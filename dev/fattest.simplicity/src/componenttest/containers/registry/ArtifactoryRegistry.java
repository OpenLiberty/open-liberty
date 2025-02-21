/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers.registry;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;

import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.log.Log;

/**
 * This class maintains the Artifactory registry information.
 * The registry name, user, and token is provided via system properties.
 * The registry auth token is constructed at runtime.
 */
public class ArtifactoryRegistry extends Registry {

    private static final Class<?> c = ArtifactoryRegistry.class;

    /**
     * Manual override that will allow builds or users to pull from the default registry instead of Artifactory.
     */
    private static final String FORCE_EXTERNAL = "fat.test.artifactory.force.external.repo";

    /**
     * Expect this to be set on remote build machines, and local build machines that want to test against
     * remote docker hosts.
     */
    private static final String REGISTRY = "fat.test.artifactory.docker.server";
    private static final String REGISTRY_USER = "fat.test.artifactory.download.user";
    private static final String REGISTRY_PASSWORD = "fat.test.artifactory.download.token";

    private static final String DEFAULT_REGISTRY = ""; //Blank registry is the default setting

    private static final HashMap<String, String> REGISTRY_MIRRORS = new HashMap<>();
    static {
        REGISTRY_MIRRORS.put("docker.io", "wasliberty-docker-remote"); //Only for verified images
        REGISTRY_MIRRORS.put("ghcr.io", "wasliberty-ghcr-docker-remote");
        REGISTRY_MIRRORS.put("icr.io", "wasliberty-icr-docker-remote");
        REGISTRY_MIRRORS.put("mcr.microsoft.com", "wasliberty-mcr-docker-remote");
        REGISTRY_MIRRORS.put("public.ecr.aws", "wasliberty-aws-docker-remote");
//        MIRRORS.put("quay.io", "wasliberty-quay-docker-remote"); TODO
    }

    private static File configDir = new File(System.getProperty("user.home"), ".docker");

    private String registry = ""; //Blank registry is the default setting
    private String authToken;
    private boolean isArtifactoryAvailable;
    private Throwable setupException;

    //Singleton class
    private static ArtifactoryRegistry instance;

    public static ArtifactoryRegistry instance() {
        if (instance == null) {
            instance = new ArtifactoryRegistry();
            Log.info(c, "instance", instance.toString());
        }
        return instance;
    }

    private ArtifactoryRegistry() {
        // Priority 0: If forced external do not attempt to initialize Artifactory registry
        if (Boolean.getBoolean(FORCE_EXTERNAL)) {
            String message = "System property [ " + FORCE_EXTERNAL + " ] was set to true, "
                             + "make Artifactory registry unavailable.";
            registry = DEFAULT_REGISTRY;
            isArtifactoryAvailable = false;
            setupException = new IllegalStateException(message);
            return;
        }

        // Priority 1: Is there an Artifactory registry configured?
        try {
            registry = findRegistry(REGISTRY);
        } catch (Throwable t) {
            registry = DEFAULT_REGISTRY;
            isArtifactoryAvailable = false;
            setupException = t;
            return;
        }

        // Priority 2: Can we authenticate to the Artifactory registry?
        String generatedAuthToken = null;
        String foundAuthToken = null;

        try {
            foundAuthToken = findAuthToken(registry);
        } catch (Throwable t) {
            setupException = t;
        }

        try {
            generatedAuthToken = generateAuthToken(REGISTRY_USER, REGISTRY_PASSWORD);
        } catch (Throwable t) {
            setupException = t.initCause(setupException);
        }

        // Could not generate auth token nor find auth token, give up all hope
        if (Objects.isNull(generatedAuthToken) && Objects.isNull(foundAuthToken)) {
            isArtifactoryAvailable = false;
            return;
        }

        // We could not generate an auth token, but we found an auth token
        // -- assume the found auth token will work.
        if (Objects.isNull(generatedAuthToken) && !Objects.isNull(foundAuthToken)) {
            authToken = foundAuthToken;
            isArtifactoryAvailable = true;
            return;
        }

        // We generated an auth token
        Objects.requireNonNull(generatedAuthToken);

        // -- but did not find any auth token.
        // -- Create it by persisting the generated auth token
        if (Objects.isNull(foundAuthToken)) {
            try {
                persistAuthToken(registry, generatedAuthToken, configDir);
                authToken = generatedAuthToken;
                isArtifactoryAvailable = true;
                return;
            } catch (Throwable t) {
                isArtifactoryAvailable = false;
                setupException = t.initCause(setupException);
                return;
            }
        }

        // -- and found an auth token.
        Objects.requireNonNull(foundAuthToken);

        // Was the generated auth token the same as the found auth token?
        boolean matchingTokens = generatedAuthToken.equals(foundAuthToken);

        // -- Yes, leave the config alone.
        if (matchingTokens) {
            authToken = generatedAuthToken;
            isArtifactoryAvailable = true;
            return;
        }

        // -- No, update it by persisting the generated auth token.
        try {
            persistAuthToken(registry, generatedAuthToken, configDir);
            authToken = generatedAuthToken;
            isArtifactoryAvailable = true;
            return;
        } catch (Throwable t) {
            isArtifactoryAvailable = false;
            setupException = t.initCause(setupException);
            return;
        }

    }

    @Override
    public String getRegistry() {
        return registry;
    }

    @Override
    public Throwable getSetupException() {
        return setupException;
    }

    @Override
    public boolean isRegistryAvailable() {
        return isArtifactoryAvailable;
    }

    @Override
    public boolean supportsRegistry(DockerImageName original) {
        return REGISTRY_MIRRORS.containsKey(original.getRegistry());
    }

    @Override
    public boolean supportsRepository(DockerImageName modified) {
        return REGISTRY_MIRRORS.values()
                        .stream()
                        .filter(mirror -> modified.getRepository().startsWith(mirror))
                        .findAny()
                        .isPresent();
    }

    @Override
    public String getMirrorRepository(DockerImageName original) throws IllegalArgumentException {
        if (supportsRegistry(original)) {
            return REGISTRY_MIRRORS.get(original.getRegistry());
        } else {
            throw new IllegalArgumentException("The Artifactory registry does not have a mirror for the registry " + original.getRegistry());
        }
    }

    /**
     * Generates a temporary copy of the config.json file and returns the file.
     * TODO drop support for this
     */
    @Deprecated
    public File generateTempDockerConfig(String registry) throws Exception {
        if (authToken == null) {
            throw new IllegalStateException("Auth token was not available", setupException);
        }

        File configDir = new File(System.getProperty("java.io.tmpdir"), ".docker");
        File configFile = persistAuthToken(registry, authToken, configDir);

        Log.info(c, "generateTempDockerConfig", "Creating a temporary docker configuration file at: " + configFile.getAbsolutePath());

        return configFile;
    }
}
