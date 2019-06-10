package io.smallrye.reactive.messaging.impl;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory.*;

/**
 * Implementation of config used to configured the different messaging provider / connector.
 */
public class ConnectorConfig implements Config {

  private final String prefix;
  private final Config overall;

  private final String name;
  private final String connector;

  ConnectorConfig(String prefix, Config overall, String channel) {
    this.prefix = Objects.requireNonNull(prefix, "the prefix must not be set");
    this.overall = Objects.requireNonNull(overall, "the config must not be set");
    this.name = Objects.requireNonNull(channel, "the channel name must be set");

    Optional<String> value = overall.getOptionalValue(channelKey(CONNECTOR_ATTRIBUTE), String.class);
    this.connector = value
      .orElseGet(() -> overall.getOptionalValue(channelKey("type"), String.class) // Legacy
      .orElseThrow(() -> new IllegalArgumentException("Invalid channel configuration - " +
        "the `connector` attribute must be set for channel `" + name + "`")
      ));

    // Detect invalid channel-name attribute
    for (String key : overall.getPropertyNames()) {
      if ((channelKey(CHANNEL_NAME_ATTRIBUTE)).equalsIgnoreCase(key)) {
        throw new IllegalArgumentException("Invalid channel configuration -  the `channel-name` attribute cannot be used" +
          " in configuration (channel `" + name + "`)");
      }
    }
  }

  private String channelKey(String keyName) {
    return prefix + name + "." + keyName;
  }

  private String connectorKey(String keyName) {
    return CONNECTOR_PREFIX + connector + "." + keyName;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getValue(String propertyName, Class<T> propertyType) {
    if (CHANNEL_NAME_ATTRIBUTE.equalsIgnoreCase(propertyName)) {
      return (T) name;
    }
    if (CONNECTOR_ATTRIBUTE.equalsIgnoreCase(propertyName)  || "type".equalsIgnoreCase(propertyName)) {
      return (T) connector;
    }

    // First check if the channel configuration contains the desired attribute.
    try {
      return overall.getValue(channelKey(propertyName), propertyType);
    } catch (NoSuchElementException e) {
      // If not, check the connector configuration
      try {
        return overall.getValue(connectorKey(propertyName), propertyType);
      } catch (NoSuchElementException e2) {
        // Catch the exception to provide a more meaningful error messages.
        throw new NoSuchElementException("Cannot find attribute `" + propertyName + "` for channel `" + name + "`. " +
          "Has been tried: " + channelKey(propertyName) + " and " + connectorKey(propertyName));
      }
    }
  }

  @Override
  public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
    if (CHANNEL_NAME_ATTRIBUTE.equalsIgnoreCase(propertyName)) {
      return Optional.of((T) name);
    }
    if (CONNECTOR_ATTRIBUTE.equalsIgnoreCase(propertyName)  || "type".equalsIgnoreCase(propertyName)) {
      return Optional.of((T) connector);
    }
    // First check if the channel configuration contains the desired attribute.
    Optional<T> maybe = overall.getOptionalValue(channelKey(propertyName), propertyType);
    return maybe.isPresent() ?
      maybe
      : overall.getOptionalValue(connectorKey(propertyName), propertyType);
  }

  @Override
  public Iterable<String> getPropertyNames() {
    // Compute the keys form the connector config
    Set<String> strings = StreamSupport.stream(overall.getPropertyNames().spliterator(), false)
      .filter(s -> s.startsWith(CONNECTOR_PREFIX + connector + "."))
      .map(s -> s.substring((CONNECTOR_PREFIX + connector + ".").length()))
      .collect(Collectors.toSet());

    StreamSupport.stream(overall.getPropertyNames().spliterator(), false)
      .filter(s -> s.startsWith(prefix + name + "."))
      .map(s -> s.substring((prefix + name + ".").length()))
      .forEach(strings::add);

    strings.add(CHANNEL_NAME_ATTRIBUTE);
    return strings;
  }

  @Override
  public Iterable<ConfigSource> getConfigSources() {
    return overall.getConfigSources();
  }
}
