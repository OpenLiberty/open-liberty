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
        String contents = "";

        String privateAuth = "\t\t\"" + registry + "\": {\n" +
                             "\t\t\t\"auth\": \"" + authToken + "\",\n"
                             + "\t\t\t\"email\": null\n" + "\t\t}";
        if (configFile.exists()) {
            Log.info(c, m, "Config already exists at: " + configFile.getAbsolutePath());
            for (String line : Files.readAllLines(configFile.toPath()))
                contents += line + '\n';

            logConfigContents(m, "Original contents", contents);
            int authsIndex = contents.indexOf("\"auths\"");
            boolean replacedAuth = false;

            if (contents.contains(registry)) {
                Log.info(c, m, "Config already contains the private registry: " + registry);
                int registryIndex = contents.indexOf(registry, authsIndex);
                int authIndex = contents.indexOf("\"auth\":", registryIndex);
                int authIndexEnd = contents.indexOf(',', authIndex) + 1;
                String authSubstring = contents.substring(authIndex, authIndexEnd);
                if (authSubstring.contains(authToken)) {
                    Log.info(c, m, "Config already contains the correct auth token for registry: " + registry);
                    return configFile;
                } else {
                    replacedAuth = true;
                    Log.info(c, m, "Replacing auth token for registry: " + registry);
                    contents = contents.replace(authSubstring, "\"auth\": \"" + authToken + "\",");
                }
            }

            if (authsIndex >= 0 && !replacedAuth) {
                Log.info(c, m, "Other auths exist. Need to add auth token for registry: " + registry);
                int splitAt = contents.indexOf('{', authsIndex);
                String firstHalf = contents.substring(0, splitAt + 1);
                String secondHalf = contents.substring(splitAt + 1);
                contents = firstHalf + '\n' + privateAuth + ",\n" + secondHalf;
            } else if (!replacedAuth) {
                Log.info(c, m, "No auths exist. Adding auth block");
                int splitAt = contents.indexOf('{');
                String firstHalf = contents.substring(0, splitAt + 1);
                String secondHalf = contents.substring(splitAt + 1);
                String delimiter = secondHalf.contains(":") ? "," : "";
                contents = firstHalf + "\n\t\"auths\": {\n" + privateAuth + "\n\t}" + delimiter + secondHalf;
            }
        } else {
            configDir.mkdirs();
            Log.info(c, m, "Generating a private registry config file at: "
                           + configFile.getAbsolutePath());
            contents = "{\n\t\"auths\": {\n" + privateAuth + "\n\t}\n}";
        }
        logConfigContents(m, "New config.json contents are", contents);
        configFile.delete();
        writeFile(configFile, contents);
        return configFile;
    }

    //   UTILITY METHODS

    private static void writeFile(File outFile, String content) {
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
     * Log the contents of a config file that may contain authentication data which should be redacted.
     *
     * @param method
     * @param msg
     * @param contents
     */
    private static void logConfigContents(String method, String msg, String contents) {
        String sanitizedContents = contents.replaceAll("\"auth\": \".*\"", "\"auth\": \"****Token Redacted****\"");
        Log.info(c, method, msg + ":\n" + sanitizedContents);
    }
}
