/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//We modified this file to backport a small change from later releases of weld to the 2.4.8 branch, closing the properties stream in a final block.

package org.jboss.weld.config;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BootstrapConfiguration;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.configuration.spi.ExternalConfiguration;
import org.jboss.weld.exceptions.IllegalStateException;
import org.jboss.weld.logging.BeanLogger;
import org.jboss.weld.logging.ConfigurationLogger;
import org.jboss.weld.resources.WeldClassLoaderResourceLoader;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.resources.spi.ResourceLoadingException;
import org.jboss.weld.security.GetSystemPropertyAction;
import org.jboss.weld.util.Preconditions;
import org.jboss.weld.util.collections.ImmutableMap;
import org.jboss.weld.util.reflection.Reflections;

/**
 * Represents an immutable per-deployment Weld configuration.
 *
 * <p>
 * Each property may be set in three different sources (by priority in descending order):
 * </p>
 * <ol>
 * <li>In a properties file named `weld.properties`</li>
 * <li>As a system property</li>
 * <li>By a bootstrap configuration provided by an integrator</li>
 * </ol>
 *
 * <p>
 * For backwards compatibility there are some obsolete sources:
 * </p>
 * <ul>
 * <li>properties files <code>org.jboss.weld.executor.properties</code> and <code>org.jboss.weld.bootstrap.properties</code> are also loaded for some
 * configuration keys,</li>
 * <li>some system properties with obsolete keys are considered</li>
 * </ul>
 *
 * <p>
 * If a configuration key is set in multiple sources (e.g. as a system property and in a <code>weld.properties</code> file), the value from the source with
 * higher priority is taken, other values are ignored.
 * </p>
 *
 * <p>
 * If a configuration key is set multiple times in the same source (e.g. different <code>weld.properties</code> files) and the values are different, the
 * container automatically detects the problem and treats it as a deployment problem.
 * </p>
 *
 * <p>
 * Unsupported configuration keys are ignored. If an invalid value is set, the container automatically detects the problem and treats it as a deployment
 * problem.
 * </p>
 *
 * @author Martin Kouba
 * @see BootstrapConfiguration
 * @see ConfigurationKey
 */
@SuppressWarnings("deprecation")
public class WeldConfiguration implements Service {

    public static final String CONFIGURATION_FILE = "weld.properties";

    private static final String EXECUTOR_CONFIGURATION_FILE = "org.jboss.weld.executor.properties";

    private static final String BOOTSTRAP_CONFIGURATION_FILE = "org.jboss.weld.bootstrap.properties";

    private static final String SYSTEM_PROPETIES = "system properties";

    private static final String OBSOLETE_SYSTEM_PROPETIES = "obsolete system properties";

    private static final String EXTERNAL_CONFIGURATION_CLASS_NAME = "org.jboss.weld.configuration.spi.ExternalConfiguration";

    private static final String UNSAFE_PROXIES_MARKER = "META-INF/org.jboss.weld.enableUnsafeProxies";

    private final Map<ConfigurationKey, Object> properties;

    private final File proxyDumpFilePath;

    private final Pattern proxyIgnoreFinalMethodsPattern;

    /**
     *
     * @param services
     * @param deployment
     */
    public WeldConfiguration(ServiceRegistry services, Deployment deployment) {
        Preconditions.checkArgumentNotNull(deployment, "deployment");
        this.properties = init(services, deployment);
        this.proxyDumpFilePath = initProxyDumpFilePath();
        this.proxyIgnoreFinalMethodsPattern = initProxyIgnoreFinalMethodsPattern();
        StringBuilder logOuputBuilder = new StringBuilder("{");
        String prefix = "";
        for (Entry<ConfigurationKey, Object> entry: properties.entrySet()) {
            logOuputBuilder.append(prefix);
            prefix = ", ";
            logOuputBuilder.append(entry.getKey().get()).append("=").append(entry.getValue());
        }
        logOuputBuilder.append("}");
        ConfigurationLogger.LOG.configurationInitialized(logOuputBuilder.toString());
    }

    /**
     *
     * @param key
     * @return the property for the given key
     * @throws IllegalStateException If the property type does not match the required type
     */
    public String getStringProperty(ConfigurationKey key) {
        return getProperty(key, String.class);
    }

    /**
     *
     * @param key
     * @return the property for the given key
     * @throws IllegalStateException If the property type does not match the required type
     */
    public Boolean getBooleanProperty(ConfigurationKey key) {
        return getProperty(key, Boolean.class);
    }

    /**
     *
     * @param key
     * @return the property for the given key
     * @throws IllegalStateException If the property type does not match the required type
     */
    public Long getLongProperty(ConfigurationKey key) {
        return getProperty(key, Long.class);
    }

    /**
     *
     * @param key
     * @return the property for the given key
     * @throws IllegalStateException If the property type does not match the required type
     */
    public Integer getIntegerProperty(ConfigurationKey key) {
        return getProperty(key, Integer.class);
    }

    /**
     *
     * @return the path or <code>null</code> if the generated bytecode should not be dumped
     * @see ConfigurationKey#PROXY_DUMP
     */
    public File getProxyDumpFilePath() {
        return proxyDumpFilePath;
    }

    /**
     *
     * @param className
     * @return <code>true</code> if the final methods declared on the given type should be ignored, <code>false</code> otherwise
     * @see ConfigurationKey#PROXY_IGNORE_FINAL_METHODS
     */
    public boolean isFinalMethodIgnored(String className) {
        return proxyIgnoreFinalMethodsPattern != null ? proxyIgnoreFinalMethodsPattern.matcher(className).matches() : false;
    }

    @Override
    public void cleanup() {
        if (properties != null) {
            properties.clear();
        }
    }

    /**
     * Merge two maps of configuration properties. If the original contains a mapping for the same key, the new mapping is ignored.
     *
     * @param original
     * @param toMerge
     */
    static void merge(Map<ConfigurationKey, Object> original, Map<ConfigurationKey, Object> toMerge, String mergedSourceDescription) {
        for (Entry<ConfigurationKey, Object> entry : toMerge.entrySet()) {
            Object existing = original.get(entry.getKey());
            if (existing != null) {
                ConfigurationLogger.LOG.configurationKeyAlreadySet(entry.getKey().get(), existing, entry.getValue(), mergedSourceDescription);
            } else {
                original.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     *
     * @param key
     * @param requiredType
     * @throws IllegalStateException If the configuration property type does not match the required type
     */
    static void checkRequiredType(ConfigurationKey key, Class<?> requiredType) {
        if (!key.isValidValueType(requiredType)) {
            throw ConfigurationLogger.LOG.configurationPropertyTypeMismatch(key.getDefaultValue().getClass(), requiredType, key.get());
        }
    }

    /**
     *
     * @param key
     * @return the string value of the system property or <code>null</code>
     */
    static String getSystemProperty(String key) {
        try {
            return AccessController.doPrivileged(new GetSystemPropertyAction(key));
        } catch (Throwable ignore) {
            return null;
        }
    }

    private Map<ConfigurationKey, Object> init(ServiceRegistry services, Deployment deployment) {

        // 1. Properties files
        // weld.properties
        Map<ConfigurationKey, Object> properties = readFileProperties(findPropertiesFiles(deployment, CONFIGURATION_FILE));
        // org.jboss.weld.bootstrap.properties
        merge(properties,
                readObsoleteFileProperties(
                        findPropertiesFiles(deployment, BOOTSTRAP_CONFIGURATION_FILE),
                        ImmutableMap.<String, ConfigurationKey> builder().put("concurrentDeployment", ConfigurationKey.CONCURRENT_DEPLOYMENT)
                                .put("preloaderThreadPoolSize", ConfigurationKey.PRELOADER_THREAD_POOL_SIZE).build()), BOOTSTRAP_CONFIGURATION_FILE);
        // org.jboss.weld.executor.properties
        merge(properties,
                readObsoleteFileProperties(
                        findPropertiesFiles(deployment, EXECUTOR_CONFIGURATION_FILE),
                        ImmutableMap.<String, ConfigurationKey> builder().put("threadPoolSize", ConfigurationKey.EXECUTOR_THREAD_POOL_SIZE)
                                .put("threadPoolDebug", ConfigurationKey.EXECUTOR_THREAD_POOL_DEBUG)
                                .put("threadPoolType", ConfigurationKey.EXECUTOR_THREAD_POOL_TYPE)
                                .put("threadPoolKeepAliveTime", ConfigurationKey.EXECUTOR_THREAD_POOL_KEEP_ALIVE_TIME).build()), EXECUTOR_CONFIGURATION_FILE);

        // META-INF/org.jboss.weld.enableUnsafeProxies
        if (!findPropertiesFiles(deployment, UNSAFE_PROXIES_MARKER).isEmpty()) {
            merge(properties, ImmutableMap.<ConfigurationKey, Object>of(ConfigurationKey.RELAXED_CONSTRUCTION, true), UNSAFE_PROXIES_MARKER);
        }

        // 2. System properties
        merge(properties, getSystemProperties(), SYSTEM_PROPETIES);
        merge(properties, getObsoleteSystemProperties(), OBSOLETE_SYSTEM_PROPETIES);

        // 3. Integrator SPI
        // ExternalConfiguration.getConfigurationProperties() map has precedence
        merge(properties, processExternalConfiguration(getExternalConfigurationOptions(services)), "ExternalConfiguration");
        merge(properties, processBootstrapConfiguration(services.get(BootstrapConfiguration.class)), BootstrapConfiguration.class.getSimpleName());

        return properties;
    }

    private File initProxyDumpFilePath() {
        String dumpPath = getStringProperty(ConfigurationKey.PROXY_DUMP);
        if (!dumpPath.isEmpty()) {
            File tmp = new File(dumpPath);
            if (!tmp.isDirectory() && !tmp.mkdirs()) {
                BeanLogger.LOG.directoryCannotBeCreated(tmp.toString());
                return null;
            } else {
                return tmp;
            }
        }
        return null;
    }

    private Pattern initProxyIgnoreFinalMethodsPattern() {
        String ignore = getStringProperty(ConfigurationKey.PROXY_IGNORE_FINAL_METHODS);
        if (!ignore.isEmpty()) {
            return Pattern.compile(ignore);
        }
        return null;
    }

    private Set<URL> findPropertiesFiles(Deployment deployment, String fileName) {
        Set<ResourceLoader> resourceLoaders = new HashSet<ResourceLoader>();
        Set<URL> files = new HashSet<URL>();

        ResourceLoader deploymentResourceLoader = deployment.getServices().get(ResourceLoader.class);
        if (deploymentResourceLoader != null) {
            resourceLoaders.add(deploymentResourceLoader);
        }

        for (BeanDeploymentArchive archive : deployment.getBeanDeploymentArchives()) {
            ResourceLoader resourceLoader = archive.getServices().get(ResourceLoader.class);
            if (resourceLoader == null) {
                ConfigurationLogger.LOG.resourceLoaderNotSpecifiedForArchive(archive);
                continue;
            }
            resourceLoaders.add(resourceLoader);
        }
        for (ResourceLoader resourceLoader : resourceLoaders) {
            URL file = resourceLoader.getResource(fileName);
            if (file != null) {
                files.add(file);
            }
        }
        return files;
    }

    /**
     * Iterate through the {@link ConfigurationKey#values()} and try to get a system property for every key. The value is automatically converted - a runtime
     * exception may be thrown during conversion.
     *
     * @return all the properties set as system properties
     */
    private Map<ConfigurationKey, Object> getSystemProperties() {
        Map<ConfigurationKey, Object> found = new EnumMap<ConfigurationKey, Object>(ConfigurationKey.class);
        for (ConfigurationKey key : ConfigurationKey.values()) {
            String property = getSystemProperty(key.get());
            if (property != null) {
                processKeyValue(found, key, property);
            }
        }
        return found;
    }

    /**
     * Try to get a system property for obsolete keys. The value is automatically converted - a runtime exception may be thrown during conversion.
     *
     * @return all the properties whose system property keys were different in previous versions
     */
    private Map<ConfigurationKey, Object> getObsoleteSystemProperties() {
        Map<ConfigurationKey, Object> found = new EnumMap<ConfigurationKey, Object>(ConfigurationKey.class);
        String concurrentDeployment = getSystemProperty("org.jboss.weld.bootstrap.properties.concurrentDeployment");
        if (concurrentDeployment != null) {
            processKeyValue(found, ConfigurationKey.CONCURRENT_DEPLOYMENT, concurrentDeployment);
            found.put(ConfigurationKey.CONCURRENT_DEPLOYMENT, ConfigurationKey.CONCURRENT_DEPLOYMENT.convertValue(concurrentDeployment));
        }
        String preloaderThreadPoolSize = getSystemProperty("org.jboss.weld.bootstrap.properties.preloaderThreadPoolSize");
        if (preloaderThreadPoolSize != null) {
            found.put(ConfigurationKey.PRELOADER_THREAD_POOL_SIZE, ConfigurationKey.PRELOADER_THREAD_POOL_SIZE.convertValue(preloaderThreadPoolSize));
        }
        return found;
    }

    /**
     * Read the set of property files. Keys and Values are automatically validated and converted.
     *
     * @param resourceLoader
     * @return all the properties from the weld.properties file
     */
    private Map<ConfigurationKey, Object> readFileProperties(Set<URL> files) {
        Map<ConfigurationKey, Object> found = new EnumMap<ConfigurationKey, Object>(ConfigurationKey.class);
        for (URL file : files) {
            ConfigurationLogger.LOG.readingPropertiesFile(file);
            Properties fileProperties = loadProperties(file);
            for (String name : fileProperties.stringPropertyNames()) {
                processKeyValue(found, name, fileProperties.getProperty(name));
            }
        }
        return found;
    }

    private Map<ConfigurationKey, Object> readObsoleteFileProperties(Set<URL> files, Map<String, ConfigurationKey> nameToKeyMap) {
        if (files.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<ConfigurationKey, Object> found = new EnumMap<ConfigurationKey, Object>(ConfigurationKey.class);
        for (URL file : files) {
            ConfigurationLogger.LOG.readingPropertiesFile(file);
            Properties fileProperties = loadProperties(file);
            for (String name : fileProperties.stringPropertyNames()) {
                ConfigurationKey key = nameToKeyMap.get(name);
                if (key != null) {
                    processKeyValue(found, key, fileProperties.getProperty(name));
                } else {
                    ConfigurationLogger.LOG.unsupportedConfigurationKeyFound(name + " in " + fileProperties);
                }
            }
        }
        return found;
    }

    private Map<ConfigurationKey, Object> processBootstrapConfiguration(BootstrapConfiguration bootstrapConfiguration) {
        if (bootstrapConfiguration != null) {
            Map<ConfigurationKey, Object> found = new EnumMap<ConfigurationKey, Object>(ConfigurationKey.class);
            processKeyValue(found, ConfigurationKey.CONCURRENT_DEPLOYMENT, bootstrapConfiguration.isConcurrentDeploymentEnabled());
            processKeyValue(found, ConfigurationKey.PRELOADER_THREAD_POOL_SIZE, bootstrapConfiguration.getPreloaderThreadPoolSize());
            processKeyValue(found, ConfigurationKey.NON_PORTABLE_MODE, bootstrapConfiguration.isNonPortableModeEnabled());
            return found;
        }
        return Collections.emptyMap();
    }

    private Map<ConfigurationKey, Object> processExternalConfiguration(Map<String, Object> externalConfiguration) {
        Map<ConfigurationKey, Object> found =  new EnumMap<ConfigurationKey, Object>(ConfigurationKey.class);
        for (Entry<String, Object> entry : externalConfiguration.entrySet()) {
            processKeyValue(found, entry.getKey(), entry.getValue());
        }
        return found;
    }

    private Map<String, Object> getExternalConfigurationOptions(ServiceRegistry services) {
        // to stay compatible with older SPI versions we first check if ExternalConfiguration is available before using the class
        if (Reflections.isClassLoadable(EXTERNAL_CONFIGURATION_CLASS_NAME, WeldClassLoaderResourceLoader.INSTANCE)) {
            final ExternalConfiguration externalConfiguration = services.get(ExternalConfiguration.class);
            if (externalConfiguration != null) {
                return externalConfiguration.getConfigurationProperties();
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Process the given key and value. First validate the value and check if there's no different value for the same key in the same source - invalid and
     * different values are treated as a deployment problem.
     *
     * @param properties
     * @param key
     * @param value
     */
    private void processKeyValue(Map<ConfigurationKey, Object> properties, ConfigurationKey key, Object value) {
        if (value instanceof String) {
            value = key.convertValue((String) value);
        }
        if (key.isValidValue(value)) {
            Object previous = properties.put(key, value);
            if (previous != null && !previous.equals(value)) {
                throw ConfigurationLogger.LOG.configurationKeyHasDifferentValues(key.get(), previous, value);
            }
        } else {
            throw ConfigurationLogger.LOG.invalidConfigurationPropertyValue(value, key.get());
        }
    }

    /**
     * Process the given string key and value. First try to convert the <code>stringKey</code> - unsupported keys are ignored. Then delegate to
     * {@link #processKeyValue(Map, ConfigurationKey, Object)}.
     *
     * @param properties
     * @param stringKey
     * @param value
     */
    private void processKeyValue(Map<ConfigurationKey, Object> properties, String stringKey, Object value) {
        ConfigurationKey key = ConfigurationKey.fromString(stringKey);
        if (key != null) {
            processKeyValue(properties, key, value);
        } else {
            ConfigurationLogger.LOG.unsupportedConfigurationKeyFound(stringKey);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getProperty(ConfigurationKey key, Class<T> requiredType) {
        checkRequiredType(key, requiredType);
        Object property = properties.get(key);
        return (T) (property != null ? property : key.getDefaultValue());
    }

    private Properties loadProperties(URL url) {
        Properties properties = new Properties();
        try {
            //IBM changes begin here.
            InputStream propertiesStream = url.openStream();
            try {
                properties.load(propertiesStream);
            } finally {
                propertiesStream.close();
            }
            //IBM changes end here.
        } catch (IOException e) {
            throw new ResourceLoadingException(e);
        }
        return properties;
    }
}

