/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.utils.ExternalTestService;

/**
 * This class maintains the Artifactory registry information.
 * The registry name is provided via system property.
 * The registry auth token is pulled from an internal service.
 */
public class ArtifactoryRegistry {

    private static final Class<?> c = ArtifactoryRegistry.class;

    /**
     * Expect this to be set on remote build machines, and local build machines that want to test against
     * remote docker hosts.
     */
    private static final String artifactoryRegistryKey = "fat.test.artifactory.docker.server";

    private String registry = ""; //Blank registry is the default setting
    private String authToken;
    private boolean isArtifactoryAvailable;
    private Throwable setupException;

    //Singleton class
    private static ArtifactoryRegistry instance;

    public static ArtifactoryRegistry instance() {
        if (instance == null) {
            instance = new ArtifactoryRegistry();
        }
        return instance;
    }

    private ArtifactoryRegistry() {
        // Priority 1: Is there an Artifactory registry configured?
        try {
            registry = findRegistry();
        } catch (Throwable t) {
            isArtifactoryAvailable = false;
            setupException = t;
            return;
        }

        // Priority 2: Are we able to get an auth token to Artifactory?
        try {
            authToken = requestAuthToken();
        } catch (Throwable t) {
            isArtifactoryAvailable = false;
            setupException = t;
            return;
        }

        // Finally: Attempt to generate docker configuration for Artifactory
        try {
            File configDir = new File(System.getProperty("user.home"), ".docker");
            generateDockerConfig(registry, authToken, configDir);
            isArtifactoryAvailable = true;
        } catch (Throwable t) {
            isArtifactoryAvailable = false;
            setupException = t;
        }

    }

    public String getRegistry() {
        return registry;
    }

    public Throwable getSetupException() {
        return setupException;
    }

    public boolean isArtifactoryAvailable() {
        return isArtifactoryAvailable;
    }

    //  SETUP METHODS

    private static String findRegistry() {
        Log.info(c, "findRegistry", "Searching system property " + artifactoryRegistryKey + " for an Artifactory registry.");
        String registry = System.getProperty(artifactoryRegistryKey);
        if (registry == null || registry.isEmpty() || registry.startsWith("${") || registry.equals("null")) {
            throw new IllegalStateException("No Artifactory registry configured. System property '" + artifactoryRegistryKey + "' was: " + registry
                                            + " Ensure Artifactory properties are set in gradle.startup.properties");
        }
        return registry;
    }

    private static String requestAuthToken() throws Exception {
        Log.info(c, "requestAuthToken", "Requesting Artifactory registry auth token from consul");
        String token = ExternalTestService.getProperty("docker-hub-mirror/auth-token");
        if (token == null || token.isEmpty() || token.startsWith("${")) {
            throw new IllegalStateException("No valid Artifactory registry auth token was returned from consul");
        }
        Log.info(c, "getRegistryAuthToken", "Got Artifactory registry auth token starting with: " + token.substring(0, 4) + "....");
        return token;
    }

    /**
     * Generate a config file config.json in the provided config directory if a private docker registry will be used
     * Or if a config.json already exists, make sure that the private registry is listed. If not, add
     * the private registry to the existing config
     */
    private static File generateDockerConfig(String registry, String authToken, File configDir) throws Exception {
        final String m = "generateDockerConfig";

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

                //If existing config contains correct registry and auth token combination, return original file.
                if (testExistingConfig(root, registry, authToken)) {
                    Log.info(c, m, "Config already contains the correct auth token for registry: " + registry);
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

    //   UTILITY METHODS

    /**
     * Deletes current file if it exists and writes the content to a new file
     *
     * @param outFile - The output file destination
     * @param content - The content to be written
     */
    static void writeFile(final File outFile, final String content) {
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
     * Tests to see if the existing JSON object contains the expected registry
     *
     * @param  root      - The collected JSON object
     * @param  registry  - The expected registry
     * @param  authToken - The expected authToken
     * @return           true, if the existing json object contains the registry and authToken, false otherwise.
     */
    static boolean testExistingConfig(final ObjectNode root, final String registry, final String authToken) {
        final String m = "testExistingConfig";

        try {
            if (root.isNull()) {
                return false;
            }

            if (!root.hasNonNull("auths")) {
                Log.finer(c, m, "Config does not contain the auths element");
                return false;
            }

            if (!root.get("auths").hasNonNull(registry)) {
                Log.finer(c, m, "Config does not contain the registry [ " + registry + " ] element under the auths element");
                return false;
            }

            if (!root.get("auths").get(registry).hasNonNull("auth")) {
                Log.finer(c, m, "Config does not contain the auth element under registry [ " + registry + " ] element");
                return false;
            }

            if (!root.get("auths").get(registry).get("auth").isTextual()) {
                Log.finer(c, m, "Config contains an auth element that is not textual");
                return false;
            }

            return root.get("auths").get(registry).get("auth").textValue().equals(authToken);
        } catch (Exception e) {
            //Unexpected exception log it and consider fixing the logic above to void it
            Log.error(c, m, e);
            return false;
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
    static String serializeOutput(final ObjectMapper mapper, final ObjectNode root) throws JsonProcessingException {
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
    static void logConfigContents(final String method, final String msg, final String contents) {
        String sanitizedContents = contents.replaceAll("\"auth\": \".*\"", "\"auth\": \"****Token Redacted****\"");
        Log.info(c, method, msg + ":\n" + sanitizedContents);
    }
}
