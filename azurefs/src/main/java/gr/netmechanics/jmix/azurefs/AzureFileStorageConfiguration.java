package gr.netmechanics.jmix.azurefs;

import io.jmix.core.CoreConfiguration;
import io.jmix.core.annotation.JmixModule;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Panos Bariamis (pbaris)
 */
@Configuration
@ComponentScan
@ConfigurationPropertiesScan
@JmixModule(dependsOn = CoreConfiguration.class)
public class AzureFileStorageConfiguration {
}
