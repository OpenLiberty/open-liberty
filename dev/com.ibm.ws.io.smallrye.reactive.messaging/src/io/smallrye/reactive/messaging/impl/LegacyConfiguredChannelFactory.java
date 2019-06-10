package io.smallrye.reactive.messaging.impl;

import io.smallrye.reactive.messaging.ChannelRegistry;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import java.util.Map;

/**
 * Look for stream factories and get instances.
 * This implementation use the "smallrye.messaging.source" and "smallrye.messaging.sink" prefixes.
 */
@ApplicationScoped
public class LegacyConfiguredChannelFactory extends ConfiguredChannelFactory {

  private static final String SOURCE_CONFIG_PREFIX = "smallrye.messaging.source.";
  private static final String SINK_CONFIG_PREFIX = "smallrye.messaging.sink.";

  // CDI requirement for normal scoped beans
  LegacyConfiguredChannelFactory() {
    super();
  }

  @Inject
  public LegacyConfiguredChannelFactory(@Any Instance<IncomingConnectorFactory> incomingConnectorFactories,
                                        @Any Instance<OutgoingConnectorFactory> outgoingConnectorFactories,
                                        Instance<Config> config, @Any Instance<ChannelRegistry> registry,
                                        BeanManager beanManager) {
    super(incomingConnectorFactories, outgoingConnectorFactories, config, registry, beanManager, false);
  }

  @Override
  public void initialize() {
    if (this.config == null) {
      return;
    }
    Map<String, ConnectorConfig> sourceConfiguration = extractConfigurationFor(SOURCE_CONFIG_PREFIX, config);
    Map<String, ConnectorConfig> sinkConfiguration = extractConfigurationFor(SINK_CONFIG_PREFIX, config);

    register(sourceConfiguration, sinkConfiguration);
  }
}
