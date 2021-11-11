package gr.netmechanics.jmix.azurefs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

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

    public AzureFileStorageProperties(final String connectionString, final String containerName) {
        this.connectionString = connectionString;
        this.containerName = containerName;
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
}
