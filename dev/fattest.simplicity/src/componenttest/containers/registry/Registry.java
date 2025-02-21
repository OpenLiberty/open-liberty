/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers.registry;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

import org.testcontainers.shaded.com.github.dockerjava.core.DefaultDockerClientConfig;
import org.testcontainers.utility.AuthConfigUtil;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.RegistryAuthLocator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.dockerjava.api.model.AuthConfig;
import com.ibm.websphere.simplicity.log.Log;

/**
 * Abstract class that defines a registry.
 */
public abstract class Registry {

    private static final Class<?> c = Registry.class;

    /**
     * Get the URL to this registry
     *
     * @return the registry location
     */
    public abstract String getRegistry();

    /**
     * If this registry in unavailable this method
     * will return the exception that made the registry
     * unavailable.
     *
     * @return the exception that caused the registry to be unavailable
     */
    public abstract Throwable getSetupException();

    /**
     * Returns true if the registry is available and the
     * authentication token has been added to the docker
     * configuration. Otherwise, false
     *
     * @return
     */
    public abstract boolean isRegistryAvailable();

    /**
     * Looks at the DockerImageName and determines if this registry
     * has a mirror repository for the registry of the original image
     *
     * @param  original image name
     * @return          true if the registry has a mirror, false otherwise.
     */
    public abstract boolean supportsRegistry(DockerImageName original);

    /**
     * Looks at the DockerImageName and determines if this registry
     * contains the mirror repository for the modified image.
     *
     * @param  modified image name after determining appropriate mirror
     * @return          true if the registry supports this repository, false otherwise.
     */
    public abstract boolean supportsRepository(DockerImageName modified);

    /**
     * If this registry has a mirror repository for the provided image
     * then this method will return the name of that mirror.
     * Otherwise, throws and exception.
     *
     * @see                             Registry#supportsRegistry(DockerImageName)
     * @param  original                 image name
     * @return                          the mirror repository
     * @throws IllegalArgumentException if this registry does not support the image
     */
    public abstract String getMirrorRepository(DockerImageName original) throws IllegalArgumentException;

    // SETUP METHODS

    /**
     * Searches system properties for the this registries location using the provided regsitryKey
     *
     * @param  registryKey           System property that holds the registry location
     * @return                       The registry location
     * @throws IllegalStateException If no system property was configured
     */
    protected static String findRegistry(final String registryKey) throws IllegalStateException {
        Log.info(c, "findRegistry", "Searching system property " + registryKey + " for a registry.");
        String registry = System.getProperty(registryKey);
        if (registry == null || registry.isEmpty() || registry.startsWith("${") || registry.equals("null")) {
            throw new IllegalStateException("No registry was configured. System property '" + registryKey + "' was: " + registry
                                            + ". Ensure registry properties are set on system.");
        }
        return registry;
    }

    /**
     * Searches system properties for the this registries authentication data (user/password)
     * using the provided keys, registryUserKey and registryPasswordKey.
     *
     * @param  registryUserKey       System property that holds the registry user
     * @param  registryPasswordKey   System property that holds the registry password
     * @return                       The authentication token for this registries user
     * @throws IllegalStateException If one of the system properties was not configured
     */
    protected static String generateAuthToken(final String registryUserKey, final String registryPasswordKey) throws IllegalStateException {
        final String m = "generateAuthToken";
        Log.info(c, m, "Generating registry auth token from system properties:"
                       + " [ " + registryUserKey + ", " + registryPasswordKey + "]");

        String username = System.getProperty(registryUserKey);
        String password = System.getProperty(registryPasswordKey);

        if (username == null || username.isEmpty() || username.startsWith("${")) {
            throw new IllegalStateException("No username was configured. System property '" + registryUserKey + "' was: " + username
                                            + ". Ensure properties are set on system.");
        }

        if (password == null || password.isEmpty() || password.startsWith("${")) {
            throw new IllegalStateException("No password was configured. System property '" + registryPasswordKey
                                            + " was not set. Ensure properties are set on system.");
        }

        Log.finer(c, m, "Generating registry auth token for user " + username);

        String authData = username + ':' + password;
        String authToken = Base64.getEncoder().encodeToString(authData.getBytes());

        Log.info(c, m, "Generated registry auth token starting with: " + authToken.substring(0, 4) + "....");
        return authToken;
    }

    /**
     * <pre>
     * First, find or create a config file (config.json) in the provided configDir location.
     *
     * Then, search the config file, and perform one of the following actions for the provided registry:
     * - Append: If no auth element existed for this registry,
     *           then add a new auth element with this authToken
     * - Replace: If an auth element existed for this registry but the auth tokens do not match,
     *            then replace it with the one provided to this method.
     * - Return: If an auth element existed for this registry and the auth tokens match,
     *            then return without making any changes.
     * </pre>
     *
     * @param  registry  The registry for which an auth element of the config file.
     * @param  authToken The authentication token for an auth element of the config file.
     * @param  configDir The directory where an config.json file should be located.
     *
     * @throws Exception For issues parsing the config file.
     */
    protected static File persistAuthToken(final String registry, final String authToken, final File configDir) throws Exception {

        final String m = "persistAuthToken";

        File configFile = new File(configDir, "config.json");

        final ObjectMapper mapper = JsonMapper.builder()
                        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY) // alpha properties
                        .disable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST) // ensures new properties are not excluded from alpha
                        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS) // alpha maps
                        .enable(SerializationFeature.INDENT_OUTPUT) // use pretty printer by default
                        .defaultPrettyPrinter(
                                              // pretty printer should use tabs and new lines
                                              new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("\t", "\n")))
                        .build();
        ObjectNode root;

        //If config file already exists, read it, otherwise create a new json object
        if (configFile.exists()) {
            try {
                root = (ObjectNode) mapper.readTree(configFile);

                Log.info(c, m, "Config already exists at: " + configFile.getAbsolutePath());
                logConfigContents(m, "Original contents", serializeOutput(mapper, root));

                // If existing config contains correct registry and auth token combination, return original file.
                Optional<String> found = findExistingConfig(root, registry);
                if (found.isPresent() && found.get().equals(authToken)) {
                    Log.info(c, m, "Config already contains the correct auth token for registry: " + registry);
                    return configFile;
                }

                // Config contained registry, but not auth data, therefore do not attempt to store auth data, return original file.
                if (found.isPresent() && found.get().isEmpty()) {
                    Log.info(c, m, "Config contains the registry with no auth data, cannot persist new auth data for registry: " + registry);
                    return configFile;
                }
            } catch (Exception e) {
                Log.error(c, m, e);
                Log.warning(c, "Could not read config file, or it was malformed, recreating from scrach");
                root = mapper.createObjectNode();
            }
        } else {
            root = mapper.createObjectNode();

            configDir.mkdirs();
            Log.info(c, m, "Generating a private registry config file at: " + configFile.getAbsolutePath());
        }

        //Get existing nodes
        ObjectNode authsObject = root.has("auths") ? (ObjectNode) root.get("auths") : mapper.createObjectNode();
        ObjectNode registryObject = authsObject.has(registry) ? (ObjectNode) authsObject.get(registry) : mapper.createObjectNode();
        TextNode registryAuthObject = TextNode.valueOf(authToken); //Replace existing auth token with this one.

        //Replace nodes with updated/new configuration
        registryObject.replace("auth", registryAuthObject);
        authsObject.replace(registry, registryObject);
        root.set("auths", authsObject);

        //Output results to file
        String newContent = serializeOutput(mapper, root);
        logConfigContents(m, "New config.json contents are", newContent);
        writeFile(configFile, newContent);

        return configFile;
    }

    /**
     * If an auth token for this registry exists, return it.
     * Otherwise, throw an exception.
     *
     * @param  registry              The registry for which an auth element of the config file.
     * @return                       The auth token
     * @throws IllegalStateException If no auth element existed
     */
    protected static String findAuthToken(final String registry) throws Exception {

        final String m = "findAuthToken";

        DockerImageName tiny = DockerImageName.parse("alpine:3.17").withRegistry(registry);

        //Bad practice here, but we need to lookup the auth config without caching it at the same time.
        Method lookupUncachedAuthConfig = RegistryAuthLocator.class.getDeclaredMethod("lookupUncachedAuthConfig", String.class, DockerImageName.class);
        lookupUncachedAuthConfig.setAccessible(true);

        /**
         * Utilize the RegistryAuthLocator with a bogus DockerImageName.
         * This will attempt to locate the registry authentication in the following priority order:
         * 1. credHelpers
         * 2. credsStore
         * 3. base64 encoded credentials
         */
        AuthConfig encodedCredentials = DefaultDockerClientConfig.createDefaultConfigBuilder().build().effectiveAuthConfig(tiny.asCanonicalNameString());
        Optional<AuthConfig> credStoreOrHelper = (Optional<AuthConfig>) lookupUncachedAuthConfig.invoke(RegistryAuthLocator.instance(), tiny.getRegistry(), tiny);
        AuthConfig config = credStoreOrHelper.orElse(encodedCredentials);

        if (Objects.isNull(config)) {
            throw new IllegalStateException("Could not find a pre-existing auth for the registry: " + registry);
        } else {
            Log.info(c, m, "Found pre-existing auth config for registry: " + AuthConfigUtil.toSafeString(config));
        }

        if (Objects.nonNull(config.getAuth())) {
            return config.getAuth();
        } else if (Objects.nonNull(config.getUsername()) && Objects.nonNull(config.getPassword())) {
            String authData = config.getUsername() + ':' + config.getPassword();
            return Base64.getEncoder().encodeToString(authData.getBytes());
        } else {
            throw new RuntimeException("The pre-existing auth config has not been implemented by this method. "
                                       + AuthConfigUtil.toSafeString(config));
        }
    }

    //   UTILITY METHODS

    /**
     * Deletes current file if it exists and writes the content to a new file
     *
     * @param outFile - The output file destination
     * @param content - The content to be written
     */
    protected static void writeFile(final File outFile, final String content) {
        try {
            Files.deleteIfExists(outFile.toPath());
            Files.write(outFile.toPath(), content.getBytes());
        } catch (IOException e) {
            Log.error(c, "writeFile", e);
            throw new RuntimeException(e);
        }
        Log.info(c, "writeFile", "Wrote property to: " + outFile.getAbsolutePath());
    }

    /**
     * Searches the JSON object for the auth element for the provided registry,
     * and returns the authToken if it exists.
     *
     * @param  root     The collected JSON object
     * @param  registry The expected registry
     * @return          An optional - ofEmpty if registry does not exist, ofBlank if auth data does not exist,
     *                  of auth token if auth data does exist.
     *
     */
    protected static Optional<String> findExistingConfig(final ObjectNode root, final String registry) {
        final String m = "findExistingConfig";

        try {
            if (root.isNull()) {
                return Optional.empty();
            }

            if (!root.hasNonNull("auths")) {
                Log.info(c, m, "Config does not contain the auths element");
                return Optional.empty();
            }

            if (!root.get("auths").hasNonNull(registry)) {
                Log.info(c, m, "Config does not contain the registry [ " + registry + " ] element under the auths element");
                return Optional.empty();
            }

            if (!root.get("auths").get(registry).hasNonNull("auth")) {
                Log.info(c, m, "Config does not contain the auth element under registry [ " + registry + " ] element");
                return Optional.of("");
            }

            if (!root.get("auths").get(registry).get("auth").isTextual()) {
                Log.info(c, m, "Config contains an auth element that is not textual");
                return Optional.of("");
            }

            return Optional.of(root.get("auths").get(registry).get("auth").textValue());

        } catch (Exception e) {
            //Unexpected exception log it and consider fixing the logic above to void it
            Log.error(c, m, e);
            return Optional.empty();
        }
    }

    /**
     * Takes a modified JSON object and will serialize it to ensure proper formatting
     *
     * @param  mapper                  - The ObjectMapper to use for serialization
     * @param  root                    - The modified JSON object
     * @return                         - The serialized JSON object as a string
     * @throws JsonProcessingException - if any error occurs during serialization.
     */
    protected static String serializeOutput(final ObjectMapper mapper, final ObjectNode root) throws JsonProcessingException {
        String input = mapper.writeValueAsString(root);
        Object pojo = mapper.readValue(input, Object.class);
        String output = mapper.writeValueAsString(pojo);
        return output;
    }

    /**
     * Log the contents of a config file that may contain authentication data which should be redacted.
     *
     * @param method   - The method calling this logger
     * @param msg      - The message to output
     * @param contents - The content that needs to be sanitized
     */
    protected static void logConfigContents(final String method, final String msg, final String contents) {
        String sanitizedContents = contents.replaceAll("\"auth\" : \".*\"", "\"auth\" : \"****Token Redacted****\"");
        Log.info(c, method, msg + ":\n" + sanitizedContents);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " [getRegistry=" + this.getRegistry() + ", isRegistryAvailable=" + this.isRegistryAvailable() +
               ", getSetupException=" + this.getSetupException() + "]";
    }
}
