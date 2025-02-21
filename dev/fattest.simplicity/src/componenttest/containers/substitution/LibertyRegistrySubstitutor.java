/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers.substitution;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.registry.ArtifactoryRegistry;
import componenttest.containers.registry.InternalRegistry;
import componenttest.containers.registry.Registry;

/**
 * Prepends the appropriate registry for the known mirrored image name.
 * It is expected for this substitution to be applied AFTER the LibertyMirrorSubstitutor.
 * These two substitutions are split because we expect the LibertyMirrorSubstitutor
 * to always produce the same results, whereas this substitution is locale dependent.
 *
 * Examples:
 *
 * <pre>
 * - Already mirrored
 *   - Input: wasliberty-[department]-remote/docker/library/postgres:17.0-alpine
 *   - Output: [supported-registry]/wasliberty-[department]-remote/docker/library/postgres:17.0-alpine
 * - Using ArtifactoryRegistry
 *   - Input: wasliberty-aws-docker-remote/docker/library/postgres:17.0-alpine
 *   - Output: [artifactory-registry]/wasliberty-aws-docker-remote/docker/library/postgres:17.0-alpine
 * - Using InternalRegistry (for mirrored images)
 *   - Input: wasliberty-internal-docker-remote/testcontainers/ryuk:0.9.0
 *   - Output: [internal-registry]/wasliberty-internal-docker-remote/testcontainers/ryuk:0.9.0
 * - Using InternalRegistry (for built images)
 *   - Input: wasliberty-internal-docker-local/openliberty/testcontainers/prostgres-init/17.0-alpine
 *   - Output: [internal-registry]/wasliberty-internal-docker-local/openliberty/testcontainers/prostgres-init/17.0-alpine
 * - When ArtifactoryRegistry nor InternalRegistry support image
 *   - Input: [unsupported-registry]/repository/image:1.0
 *   - Output: IllegalStateException
 * </pre>
 */
public class LibertyRegistrySubstitutor extends ImageNameSubstitutor {

    private static final Class<?> c = LibertyRegistrySubstitutor.class;

    @Override
    public DockerImageName apply(final DockerImageName mirrored) {
        final String m = "apply";

        Registry artifactory = ArtifactoryRegistry.instance();
        Registry internal = InternalRegistry.instance();

        // Artifactory registry supports the mirrored image, but was not available
        if (artifactory.supportsRepository(mirrored) && !artifactory.isRegistryAvailable()) {
            throw new IllegalStateException("Needed to append Artifactory registry to the docker image name: " + mirrored.asCanonicalNameString()
                                            + System.lineSeparator() + "The Artfiactory registry was not available see causedBy for reason", artifactory.getSetupException());
        }

        // Internal registry supports the mirrored image, but was not available
        if (internal.supportsRepository(mirrored) && !internal.isRegistryAvailable()) {
            throw new IllegalStateException("Needed to append Internal registry to the docker image name: " + mirrored.asCanonicalNameString()
                                            + System.lineSeparator() + "The Internal registry was not available see causedBy for reason", internal.getSetupException());
        }

        // Mirrored image name already has a registry, this indicates that a developer
        // may have introduced an unsupported registry, throw an error so they know to fix it.
        if (!mirrored.getRegistry().isEmpty()) {
            throw new IllegalStateException("The mirrored image name " + mirrored.asCanonicalNameString() + " already has a registry."
                                            + System.lineSeparator() + "We cannot append another registry, "
                                            + "this usually indicates the registry of the original image is currently unsupported.");
        }

        final String registry;
        final String reason;

        if (artifactory.supportsRepository(mirrored)) {
            registry = artifactory.getRegistry();
            reason = "The Artifactory registry supports the mirrored image name " + mirrored.asCanonicalNameString();
        } else if (internal.supportsRepository(mirrored)) {
            registry = internal.getRegistry();
            reason = "The Internal registry supports the mirrored image name " + mirrored.asCanonicalNameString();
        } else {
            throw new IllegalStateException("Neither the Artifactory nor Internal registries supported the mirrrored image name " +
                                            mirrored.asCanonicalNameString() + " this likely indicates a new mirrored repository has not been " +
                                            "fully implemented within our test infrastructure.");
        }

        DockerImageName result = mirrored;
        result = result.withRegistry(registry);

        Log.finer(c, m, "Applied substitution because: " + reason);
        Log.finer(c, m, mirrored.asCanonicalNameString() + " --> " + result.asCanonicalNameString());
        return result;
    }

    @Override
    protected String getDescription() {
        return "LibertyRegistrySubstitutor";
    }

}
