package gr.netmechanics.jmix.azurefs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageException;
import io.jmix.core.FileTypesHelper;
import io.jmix.core.TimeSource;
import io.jmix.core.UuidProvider;
import io.jmix.core.annotation.Internal;
import io.jmix.core.common.util.Preconditions;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author Panos Bariamis (pbaris)
 */
@Internal
@Component("azurefs_FileStorage")
public class AzureFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(AzureFileStorage.class);
    private static final String DEFAULT_STORAGE_NAME = "azure";

    @Autowired
    private AzureFileStorageProperties properties;

    @Autowired
    private TimeSource timeSource;

    private final AtomicReference<BlobContainerClient> clientReference = new AtomicReference<>();
    private final String storageName;

    private boolean useConfigurationProperties = true;
    private String connectionString;
    private String containerName;

    public AzureFileStorage() {
        this(DEFAULT_STORAGE_NAME);
    }

    public AzureFileStorage(String storageName) {
        this.storageName = storageName;
    }

    /**
     * Optional constructor that allows you to override {@link AzureFileStorageProperties}.
     */
    public AzureFileStorage(String storageName, String connectionString, String containerName) {
        this.useConfigurationProperties = false;
        this.storageName = storageName;
        this.connectionString = connectionString;
        this.containerName = containerName;
    }

    @EventListener
    public void initBlobContainerClient(ApplicationStartedEvent event) {
        refreshBlobContainerClient();
    }

    private void refreshProperties() {
        if (useConfigurationProperties) {
            this.connectionString = properties.getConnectionString();
            this.containerName = properties.getContainerName();
        }
    }

    private void refreshBlobContainerClient() {
        refreshProperties();
        Preconditions.checkNotEmptyString(connectionString, "connectionString must not be empty");
        Preconditions.checkNotEmptyString(containerName, "containerName must not be empty");

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();

        try {
            clientReference.set(blobServiceClient.createBlobContainer(containerName));

        } catch (BlobStorageException e) {
            // The container may already exist, so don't throw an error
            if (!e.getErrorCode().equals(BlobErrorCode.CONTAINER_ALREADY_EXISTS)) {
                log.warn(e.getErrorCode().toString());

            } else {
                clientReference.set(blobServiceClient.getBlobContainerClient(containerName));
            }
        }
    }

    @Override
    public String getStorageName() {
        return storageName;
    }

    @Override
    public FileRef saveStream(final String fileName, final InputStream inputStream) {
        String fileKey = createFileKey(fileName);

        try {
            final byte[] data = IOUtils.toByteArray(inputStream);
            Preconditions.checkNotNullArgument(data, "File content is null");

            BlobClient blobClient = clientReference.get().getBlobClient(fileKey);
            blobClient.upload(new ByteArrayInputStream(data), data.length, true);

            Optional.of(FileTypesHelper.getMIMEType(fileName)).ifPresent(mimeType ->
                blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(mimeType)));

            return new FileRef(getStorageName(), fileKey, fileName);

        } catch (NullPointerException | IOException e) {
            String message = String.format("Could not save file %s.", fileName);
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
    }

    private String createFileKey(String fileName) {
        return createDateDir() + "/" + createUuidFilename(fileName);
    }

    private String createDateDir() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timeSource.currentTimestamp());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        return String.format("%d/%s/%s", year,
            StringUtils.leftPad(String.valueOf(month), 2, '0'),
            StringUtils.leftPad(String.valueOf(day), 2, '0'));
    }

    private String createUuidFilename(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        if (StringUtils.isNotEmpty(extension)) {
            return UuidProvider.createUuid() + "." + extension;
        } else {
            return UuidProvider.createUuid().toString();
        }
    }

    @Override
    public InputStream openStream(final FileRef reference) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            clientReference.get()
                .getBlobClient(reference.getPath())
                .download(out);

            return new ByteArrayInputStream(out.toByteArray());

        } catch (NullPointerException e) {
            String message = String.format("Could not load file %s.", reference.getFileName());
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
    }

    @Override
    public void removeFile(final FileRef reference) {
        try {
            clientReference.get()
                .getBlobClient(reference.getPath())
                .delete();

        } catch (NullPointerException e) {
            String message = String.format("Could not delete file %s.", reference.getFileName());
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, message);
        }
    }

    @Override
    public boolean fileExists(final FileRef reference) {
        BlobContainerClient client = clientReference.get();
        return client != null && client.getBlobClient(reference.getPath()).exists();
    }
}
