package gr.netmechanics.jmix.azurefs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorageException;
import io.jmix.core.TimeSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AzureFileStorageTest {

    private static final String KEY_PATTERN =
        "2024/03/07/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(\\.[a-z]+)?";

    private AzureFileStorage storage;
    private BlobContainerClient containerClient;
    private BlobClient blobClient;

    @BeforeEach
    void setUp() {
        TimeSource timeSource = mock(TimeSource.class);
        when(timeSource.currentTimestamp()).thenReturn(
            Date.from(LocalDate.of(2024, 3, 7).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        storage = new AzureFileStorage("azurefs", "UseDevelopmentStorage=true", "files", 1024, 2);
        ReflectionTestUtils.setField(storage, "timeSource", timeSource);

        containerClient = mock(BlobContainerClient.class);
        blobClient = mock(BlobClient.class);
        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
    }

    private void initialiseClient() {
        @SuppressWarnings("unchecked")
        AtomicReference<BlobContainerClient> ref =
            (AtomicReference<BlobContainerClient>) ReflectionTestUtils.getField(storage, "clientReference");
        ref.set(containerClient);
    }

    @Test
    void defaultConstructorUsesAzurefsName() {
        assertThat(new AzureFileStorage().getStorageName()).isEqualTo("azurefs");
    }

    @Test
    void namedConstructorKeepsName() {
        assertThat(new AzureFileStorage("archive").getStorageName()).isEqualTo("archive");
    }

    @Test
    void refreshFailsWhenConnectionStringIsEmpty() {
        AzureFileStorage s = new AzureFileStorage("x", "", "files", 1024, 2);

        assertThatThrownBy(s::refreshBlobContainerClient)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("connectionString");
    }

    @Test
    void refreshFailsWhenContainerNameIsEmpty() {
        AzureFileStorage s = new AzureFileStorage("x", "UseDevelopmentStorage=true", "", 1024, 2);

        assertThatThrownBy(s::refreshBlobContainerClient)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("containerName");
    }

    @Test
    void saveStreamUploadsAndReturnsFileRefWithDatedKeyAndExtension() {
        initialiseClient();
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);

        FileRef ref = storage.saveStream("report.pdf",
            new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)), Map.of("k", 42));

        verify(containerClient).getBlobClient(key.capture());
        assertThat(key.getValue()).matches(KEY_PATTERN).endsWith(".pdf");
        assertThat(ref.getStorageName()).isEqualTo("azurefs");
        assertThat(ref.getPath()).isEqualTo(key.getValue());
        assertThat(ref.getFileName()).isEqualTo("report.pdf");
        assertThat(ref.getParameters()).containsEntry("k", "42");
    }

    @Test
    void saveStreamPassesConfiguredTransferOptionsAndContentType() {
        initialiseClient();

        storage.saveStream("image.png", new ByteArrayInputStream(new byte[0]), Map.of());

        ArgumentCaptor<BlobParallelUploadOptions> options = ArgumentCaptor.forClass(BlobParallelUploadOptions.class);
        verify(blobClient).uploadWithResponse(options.capture(), any(), any());
        assertThat(options.getValue().getParallelTransferOptions().getBlockSizeLong()).isEqualTo(1024L);
        assertThat(options.getValue().getParallelTransferOptions().getMaxConcurrency()).isEqualTo(2);
        assertThat(options.getValue().getHeaders().getContentType()).isEqualTo("image/png");
    }

    @Test
    void saveStreamOmitsExtensionWhenFileNameHasNone() {
        initialiseClient();
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);

        storage.saveStream("README", new ByteArrayInputStream(new byte[0]), Map.of());

        verify(containerClient).getBlobClient(key.capture());
        assertThat(key.getValue()).matches(KEY_PATTERN).doesNotContain(".");
    }

    @Test
    void saveStreamFailsWithFileStorageExceptionWhenClientNotInitialised() {
        assertThatThrownBy(() ->
            storage.saveStream("a.txt", new ByteArrayInputStream(new byte[0]), Map.of()))
            .isInstanceOf(FileStorageException.class)
            .hasMessageContaining("a.txt");
    }

    @Test
    void openStreamReturnsDownloadedContent() throws IOException {
        initialiseClient();
        doAnswer(inv -> {
            OutputStream out = inv.getArgument(0);
            out.write("hello".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(blobClient).downloadStream(any(ByteArrayOutputStream.class));

        FileRef ref = new FileRef("azurefs", "2024/03/07/abc.txt", "abc.txt");

        assertThat(storage.openStream(ref).readAllBytes()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
        verify(containerClient).getBlobClient("2024/03/07/abc.txt");
    }

    @Test
    void openStreamFailsWhenClientNotInitialised() {
        FileRef ref = new FileRef("azurefs", "p", "abc.txt");

        assertThatThrownBy(() -> storage.openStream(ref))
            .isInstanceOf(FileStorageException.class)
            .hasMessageContaining("abc.txt");
    }

    @Test
    void removeFileDeletesBlob() {
        initialiseClient();

        storage.removeFile(new FileRef("azurefs", "2024/03/07/abc.txt", "abc.txt"));

        verify(containerClient).getBlobClient("2024/03/07/abc.txt");
        verify(blobClient).delete();
    }

    @Test
    void removeFileFailsWhenClientNotInitialised() {
        FileRef ref = new FileRef("azurefs", "p", "abc.txt");

        assertThatThrownBy(() -> storage.removeFile(ref))
            .isInstanceOf(FileStorageException.class)
            .hasMessageContaining("abc.txt");
    }

    @Test
    void fileExistsDelegatesToBlob() {
        initialiseClient();
        when(blobClient.exists()).thenReturn(true);

        assertThat(storage.fileExists(new FileRef("azurefs", "p", "a.txt"))).isTrue();
    }

    @Test
    void fileExistsIsFalseWhenClientNotInitialised() {
        assertThat(storage.fileExists(new FileRef("azurefs", "p", "a.txt"))).isFalse();
        verify(containerClient, never()).getBlobClient(anyString());
    }
}
