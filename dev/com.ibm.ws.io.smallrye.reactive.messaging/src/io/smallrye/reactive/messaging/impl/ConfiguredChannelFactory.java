package io.smallrye.reactive.messaging.impl;

import io.smallrye.reactive.messaging.ChannelRegistar;
import io.smallrye.reactive.messaging.ChannelRegistry;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.*;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Look for stream factories and get instances.
 */
@ApplicationScoped
public class ConfiguredChannelFactory implements ChannelRegistar {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfiguredChannelFactory.class);

  private final Instance<IncomingConnectorFactory> incomingConnectorFactories;
  private final Instance<OutgoingConnectorFactory> outgoingConnectorFactories;

  protected final Config config;
  protected final ChannelRegistry registry;

  // CDI requirement for normal scoped beans
  ConfiguredChannelFactory() {
    this.incomingConnectorFactories = null;
    this.outgoingConnectorFactories = null;
    this.config = null;
    this.registry = null;
  }

  @Inject
  public ConfiguredChannelFactory(@Any Instance<IncomingConnectorFactory> incomingConnectorFactories,
                                  @Any Instance<OutgoingConnectorFactory> outgoingConnectorFactories,
                                  Instance<Config> config, @Any Instance<ChannelRegistry> registry,
                                  BeanManager beanManager) {

    this(incomingConnectorFactories, outgoingConnectorFactories, config, registry, beanManager, true);
  }

  ConfiguredChannelFactory(@Any Instance<IncomingConnectorFactory> incomingConnectorFactories,
                           @Any Instance<OutgoingConnectorFactory> outgoingConnectorFactories,
                           Instance<Config> config, @Any Instance<ChannelRegistry> registry,
                           BeanManager beanManager, boolean logConnectors) {
    this.registry = registry.get();
    if (config.isUnsatisfied()) {
      this.incomingConnectorFactories = null;
      this.outgoingConnectorFactories = null;
      this.config = null;
    } else {
      this.incomingConnectorFactories = incomingConnectorFactories;
      this.outgoingConnectorFactories = outgoingConnectorFactories;
      if (logConnectors) {
        LOGGER.info("Found incoming connectors: {}", getConnectors(beanManager, IncomingConnectorFactory.class));
        LOGGER.info("Found outgoing connectors: {}", getConnectors(beanManager, OutgoingConnectorFactory.class));
      }
      //TODO Should we try to merge all the config?
      // For now take the first one.
      this.config = config.stream().findFirst()
        .orElseThrow(() -> new IllegalStateException("Unable to retrieve the config"));
    }
  }

  private List<String> getConnectors(BeanManager beanManager, Class<?> clazz) {
    return beanManager.getBeans(clazz).stream()
      .map(BeanAttributes::getQualifiers)
      .flatMap(set -> set.stream().filter(a -> a.annotationType().equals(Connector.class)))
      .map(annotation -> ((Connector) annotation).value())
      .collect(Collectors.toList());
  }

  static Map<String, ConnectorConfig> extractConfigurationFor(String prefix, Config root) {
    Iterable<String> names = root.getPropertyNames();
    Map<String, ConnectorConfig> configs = new HashMap<>();
    names.forEach(key -> {
      // $prefix$name.key=value (the prefix ends with a .)
      if (key.startsWith(prefix)) {
        // Extract the name
        String name = key.substring(prefix.length());
        if (name.contains(".")) { // We must remove the part after the first dot
          String tmp = name;
          name = tmp.substring(0, tmp.indexOf('.'));
          configs.put(name, new ConnectorConfig(prefix, root, name));
        } else {
          configs.put(name, new ConnectorConfig(prefix, root, name));
        }
      }
    });
    return configs;
  }

  @Override
  public void initialize() {
    if (this.config == null) {
      LOGGER.info("No MicroProfile Config found, skipping");
      return;
    }

    LOGGER.info("Stream manager initializing...");

    Map<String, ConnectorConfig> sourceConfiguration = extractConfigurationFor(ConnectorFactory.INCOMING_PREFIX, config);
    Map<String, ConnectorConfig> sinkConfiguration = extractConfigurationFor(ConnectorFactory.OUTGOING_PREFIX, config);

    register(sourceConfiguration, sinkConfiguration);
  }

  void register(Map<String, ConnectorConfig> sourceConfiguration, Map<String, ConnectorConfig> sinkConfiguration) {
    try {
      sourceConfiguration.forEach((name, conf) -> registry.register(name, createPublisherBuilder(name, conf)));
      sinkConfiguration.forEach((name, conf) -> registry.register(name, createSubscriberBuilder(name, conf)));
    } catch (RuntimeException e) {
      LOGGER.error("Unable to create the publisher or subscriber during initialization", e);
      throw e;
    }
  }

  private static String getConnectorAttribute(Config config) {
    // This method looks for connector and type.
    // The availability has been checked when the config object has been created
    return config.getValue("connector", String.class);
  }

  private PublisherBuilder<? extends Message> createPublisherBuilder(String name, Config config) {
    // Extract the type and throw an exception if missing
    String connector = getConnectorAttribute(config);

    // Look for the factory and throw an exception if missing
    IncomingConnectorFactory mySourceFactory = incomingConnectorFactories.select(ConnectorLiteral.of(connector))
      .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown connector for " + name + "."));

    return mySourceFactory.getPublisherBuilder(config);
  }

  private SubscriberBuilder<? extends Message, Void> createSubscriberBuilder(String name, Config config) {
    // Extract the type and throw an exception if missing
    String connector = getConnectorAttribute(config);

    // Look for the factory and throw an exception if missing
    OutgoingConnectorFactory mySinkFactory = outgoingConnectorFactories.select(ConnectorLiteral.of(connector))
      .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown connector for " + name + "."));

    return mySinkFactory.getSubscriberBuilder(config);
  }
}
