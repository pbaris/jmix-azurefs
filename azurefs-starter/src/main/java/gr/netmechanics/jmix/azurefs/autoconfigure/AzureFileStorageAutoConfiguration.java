package gr.netmechanics.jmix.azurefs.autoconfigure;

import gr.netmechanics.jmix.azurefs.AzureFileStorageConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(AzureFileStorageConfiguration.class)
public class AzureFileStorageAutoConfiguration {
}
