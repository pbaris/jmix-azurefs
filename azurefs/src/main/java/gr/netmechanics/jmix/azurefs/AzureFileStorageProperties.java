package gr.netmechanics.jmix.azurefs;

import com.azure.storage.blob.models.ParallelTransferOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @author Panos Bariamis (pbaris)
 */
@ConfigurationProperties(prefix = "jmix.azurefs")
@ConstructorBinding
public class AzureFileStorageProperties {

    /**
     * Azure Storage account name.
     */
    private final String connectionString;

    /**
     * Azure Storage container name.
     */
    private final String containerName;

    /**
     * The block size (chunk size) to transfer at a time.
     */
    private final long blockSize;

    /**
     * The maximum number of parallel requests that will be issued at any given time as a part of a single parallel transfer.
     */
    private final int maxConcurrency;

    public AzureFileStorageProperties(final String connectionString,
                                      final String containerName,
                                      @DefaultValue("1048576") int blockSize, // 1ΜΒ
                                      @DefaultValue("2") int maxConcurrency) {
        this.connectionString = connectionString;
        this.containerName = containerName;
        this.blockSize = blockSize;
        this.maxConcurrency = maxConcurrency;
    }

    /**
     * @see #connectionString
     */
    public String getConnectionString() {
        return connectionString;
    }

    /**
     * @see #containerName
     */
    public String getContainerName() {
        return containerName;
    }

    /**
     * @see #blockSize
     * @see ParallelTransferOptions#setBlockSizeLong(java.lang.Long)
     */
    public long getBlockSize() {
        return blockSize;
    }

    /**
     * @see #maxConcurrency
     * @see ParallelTransferOptions#setMaxConcurrency(java.lang.Integer)
     */
    public int getMaxConcurrency() {
        return maxConcurrency;
    }
}
