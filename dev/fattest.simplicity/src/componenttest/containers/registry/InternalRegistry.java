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
import java.util.HashMap;
import java.util.Objects;

import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.ImageHelper;

/**
 * This class maintains the internal registry information.
 * The registry name, user, and password is provided via system properties.
 * The registry auth token is constructed at runtime.
 */
public class InternalRegistry extends Registry {

    private static final Class<?> c = InternalRegistry.class;

    /**
     * Expect this to be set on remote build machines. Local build machines will
     * likely not be able to use this registry.
     */
    private static final String REGISTRY = "fat.test.docker.registry.server";
    private static final String REGISTRY_USER = "fat.test.docker.registry.user";
    private static final String REGISTRY_PASSWORD = "fat.test.docker.registry.password";

    private static final String DEFAULT_REGISTRY = ""; //Blank registry is the default setting

    private static final HashMap<String, String> REGISTRY_MIRRORS = new HashMap<>();
    static {
        REGISTRY_MIRRORS.put("NONE", "wasliberty-infrastructure-docker"); // images we cache (from sources like dockerhub)
//        REGISTRY_MIRRORS.put("NONE", "wasliberty-internal-docker-remote"); //TODO replace with a more standard naming scheme
        REGISTRY_MIRRORS.put("UNSUPPORTED", "wasliberty-intops-docker-local"); // TODO drop support for this local repository
        REGISTRY_MIRRORS.put("localhost", "wasliberty-internal-docker-local"); // images we build
    }

    private static File configDir = new File(System.getProperty("user.home"), ".docker");

    private String registry;
    private boolean isRegistryAvailable;
    private Throwable setupException;

    //Singleton class
    private static InternalRegistry instance;

    public static InternalRegistry instance() {
        if (instance == null) {
            instance = new InternalRegistry();
            Log.info(c, "instance", instance.toString());
        }
        return instance;
    }

    private InternalRegistry() {
        // Priority 1: Is there an Internal registry configured?
        try {
            registry = findRegistry(REGISTRY);
        } catch (Throwable t) {
            registry = DEFAULT_REGISTRY;
            isRegistryAvailable = false;
            setupException = t;
            return;
        }

        // Priority 2: Can we authenticate to the Internal registry?
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
            isRegistryAvailable = false;
            return;
        }

        // We could not generate an auth token, but we found an auth token
        // -- assume the found auth token will work.
        if (Objects.isNull(generatedAuthToken) && !Objects.isNull(foundAuthToken)) {
            isRegistryAvailable = true;
            return;
        }

        // We generated an auth token
        Objects.requireNonNull(generatedAuthToken);

        // -- but did not find any auth token.
        // -- Create it by persisting the generated auth token
        if (Objects.isNull(foundAuthToken)) {
            try {
                persistAuthToken(registry, generatedAuthToken, configDir);
                isRegistryAvailable = true;
                return;
            } catch (Throwable t) {
                isRegistryAvailable = false;
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
            isRegistryAvailable = true;
            return;
        }

        // -- No, update it by persisting the generated auth token.
        try {
            persistAuthToken(registry, generatedAuthToken, configDir);
            isRegistryAvailable = true;
            return;
        } catch (Throwable t) {
            isRegistryAvailable = false;
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
        return isRegistryAvailable;
    }

    @Override
    public boolean supportsRegistry(DockerImageName original) {
        if (ImageHelper.isCommittedImage(original) || ImageHelper.isSyntheticImage(original)) {
            return false;
        }

        String registry = original.getRegistry().isEmpty() ? "NONE" : original.getRegistry();
        return REGISTRY_MIRRORS.containsKey(registry);
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
        String registry = original.getRegistry().isEmpty() ? "NONE" : original.getRegistry();

        if (supportsRegistry(original)) {
            return REGISTRY_MIRRORS.get(registry);
        } else {
            throw new IllegalArgumentException("The Artifactory registry does not have a mirror for the registry " + registry);
        }
    }
}
