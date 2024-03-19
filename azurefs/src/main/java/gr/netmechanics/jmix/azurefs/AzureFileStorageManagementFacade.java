package gr.netmechanics.jmix.azurefs;

import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageLocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;


/**
 * @author Panos Bariamis (pbaris)
 */
@ManagedResource(description = "Manages Azure file storage client", objectName = "jmix.azurefs:type=AzureFileStorage")
@Component("azurefs_AzureFileStorageManagementFacade")
public class AzureFileStorageManagementFacade {
    @Autowired
    protected FileStorageLocator fileStorageLocator;

    @ManagedOperation(description = "Refresh Azure file storage client")
    public String refreshAzureClient() {
        FileStorage fileStorage = fileStorageLocator.getDefault();
        if (fileStorage instanceof AzureFileStorage) {
            ((AzureFileStorage) fileStorage).refreshBlobContainerClient();
            return "Refreshed successfully";
        }
        return "Not an Azure file storage - refresh attempt ignored";
    }

    @ManagedOperation(description = "Refresh Azure file storage client by storage name")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "storageName", description = "Storage name"),
        @ManagedOperationParameter(name = "connectionString", description = "Azure storage connection string"),
        @ManagedOperationParameter(name = "containerName", description = "Azure storage container name")})
    public String refreshAzureClient(final String storageName, final String connectionString, final String containerName) {
        FileStorage fileStorage = fileStorageLocator.getByName(storageName);
        if (fileStorage instanceof AzureFileStorage azFileStorage) {
            azFileStorage.setConnectionString(connectionString);
            azFileStorage.setContainerName(containerName);
            azFileStorage.refreshBlobContainerClient();
            return "Refreshed successfully";
        }
        return "Not an Azure file storage - refresh attempt ignored";
    }

    @ManagedOperation(description = "Refresh Azure file storage client by storage name")
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "storageName", description = "Storage name"),
        @ManagedOperationParameter(name = "connectionString", description = "Azure storage connection string"),
        @ManagedOperationParameter(name = "containerName", description = "Azure storage container name"),
        @ManagedOperationParameter(name = "blockSize", description = "The block size (chunk size) to transfer at a time"),
        @ManagedOperationParameter(name = "maxConcurrency",
            description = "The maximum number of parallel requests that will be issued at any given time as a part of a single parallel transfer")})
    public String refreshAzureClient(final String storageName, final String connectionString,
                                     final String containerName, final int blockSize, final int maxConcurrency) {
        FileStorage fileStorage = fileStorageLocator.getByName(storageName);
        if (fileStorage instanceof AzureFileStorage azFileStorage) {
            azFileStorage.setConnectionString(connectionString);
            azFileStorage.setContainerName(containerName);
            azFileStorage.setBlockSize(blockSize);
            azFileStorage.setMaxConcurrency(maxConcurrency);
            azFileStorage.refreshBlobContainerClient();
            return "Refreshed successfully";
        }
        return "Not an Azure file storage - refresh attempt ignored";
    }
}
