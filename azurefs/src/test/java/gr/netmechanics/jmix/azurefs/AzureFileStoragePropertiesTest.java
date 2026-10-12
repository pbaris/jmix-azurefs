package gr.netmechanics.jmix.azurefs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class AzureFileStoragePropertiesTest {

    private static AzureFileStorageProperties bind(final Map<String, Object> values) {
        return new Binder(new MapConfigurationPropertySource(values))
            .bind("jmix.azurefs", Bindable.of(AzureFileStorageProperties.class))
            .get();
    }

    @Test
    void bindsConfiguredValues() {
        AzureFileStorageProperties props = bind(Map.of(
            "jmix.azurefs.connection-string", "UseDevelopmentStorage=true",
            "jmix.azurefs.container-name", "files",
            "jmix.azurefs.block-size", "2048",
            "jmix.azurefs.max-concurrency", "8"));

        assertThat(props.getConnectionString()).isEqualTo("UseDevelopmentStorage=true");
        assertThat(props.getContainerName()).isEqualTo("files");
        assertThat(props.getBlockSize()).isEqualTo(2048L);
        assertThat(props.getMaxConcurrency()).isEqualTo(8);
    }

    @Test
    void appliesDefaultsForBlockSizeAndConcurrency() {
        AzureFileStorageProperties props = bind(Map.of(
            "jmix.azurefs.connection-string", "UseDevelopmentStorage=true",
            "jmix.azurefs.container-name", "files"));

        assertThat(props.getBlockSize()).isEqualTo(1_048_576L);
        assertThat(props.getMaxConcurrency()).isEqualTo(2);
    }
}
