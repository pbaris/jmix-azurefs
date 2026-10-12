package gr.netmechanics.jmix.azurefs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

class AzureFileStorageManagementFacadeTest {

    private FileStorageLocator locator;
    private AzureFileStorageManagementFacade facade;

    @BeforeEach
    void setUp() {
        locator = mock(FileStorageLocator.class);
        facade = new AzureFileStorageManagementFacade();
        ReflectionTestUtils.setField(facade, "fileStorageLocator", locator);
    }

    @Test
    void refreshesDefaultAzureStorage() {
        AzureFileStorage storage = mock(AzureFileStorage.class);
        when(locator.getDefault()).thenReturn(storage);

        assertThat(facade.refreshAzureClient()).isEqualTo("Refreshed successfully");
        verify(storage).refreshBlobContainerClient();
    }

    @Test
    void ignoresDefaultStorageThatIsNotAzure() {
        FileStorage other = mock(FileStorage.class);
        when(locator.getDefault()).thenReturn(other);

        assertThat(facade.refreshAzureClient()).startsWith("Not an Azure file storage");
    }

    @Test
    void overridesConnectionSettingsBeforeRefreshingNamedStorage() {
        AzureFileStorage storage = mock(AzureFileStorage.class);
        when(locator.getByName("archive")).thenReturn(storage);

        String result = facade.refreshAzureClient("archive", "conn", "container");

        assertThat(result).isEqualTo("Refreshed successfully");
        InOrder order = inOrder(storage);
        order.verify(storage).setConnectionString("conn");
        order.verify(storage).setContainerName("container");
        order.verify(storage).refreshBlobContainerClient();
    }

    @Test
    void overridesAllSettingsBeforeRefreshingNamedStorage() {
        AzureFileStorage storage = mock(AzureFileStorage.class);
        when(locator.getByName("archive")).thenReturn(storage);

        String result = facade.refreshAzureClient("archive", "conn", "container", 4096, 6);

        assertThat(result).isEqualTo("Refreshed successfully");
        InOrder order = inOrder(storage);
        order.verify(storage).setConnectionString("conn");
        order.verify(storage).setContainerName("container");
        order.verify(storage).setBlockSize(4096);
        order.verify(storage).setMaxConcurrency(6);
        order.verify(storage).refreshBlobContainerClient();
    }

    @Test
    void ignoresNamedStorageThatIsNotAzure() {
        FileStorage other = mock(FileStorage.class);
        when(locator.getByName("other")).thenReturn(other);

        assertThat(facade.refreshAzureClient("other", "c", "n")).startsWith("Not an Azure file storage");
        verifyNoInteractions(other);
    }
}
