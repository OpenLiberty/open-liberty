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
 * Attempts to substitute a registry for a known remote mirror in an alternative registry.
 * It is expected for the result of this substitution to be cached for build automation,
 * or to passed along to the LibertyRegistrySubstitutor at test runtime.
 *
 * Examples:
 *
 * <pre>
 * - Already mirrored
 *   - Input: wasliberty-[department]-local/docker/library/postgres:17.0-alpine
 *   - Output: wasliberty-[department]-remote/docker/library/postgres:17.0-alpine
 *   - TODO reject this state once Artfiactory local registries are no longer supported in WebSphere Liberty
 * - Using ArtifactoryRegistry
 *   - Input: public.ecr.aws/docker/library/postgres:17.0-alpine
 *   - Output: wasliberty-aws-docker-remote/docker/library/postgres:17.0-alpine
 * - Using InternalRegistry (for mirrored images)
 *   - Input: testcontainers/ryuk:0.9.0
 *   - Output: wasliberty-internal-docker-remote/testcontainers/ryuk:0.9.0
 * - Using InternalRegistry (for built images)
 *   - Input: localhost/openliberty/testcontainers/postgres-init/17.0-alpine
 *   - Output wasliberty-internal-docker-local/openliberty/testcontainers/prostgres-init/17.0-alpine
 * - When ArtifactoryRegistry nor InternalRegistry support image
 *   - Input: [unsupported-registry]/repository/image:1.0
 *   - Output: [unsupported-registry]/repository/image:1.0
 *   - Note: this may lead to a failure down the line
 * </pre>
 */
public class LibertyMirrorSubstitutor extends ImageNameSubstitutor {

    private static final Class<?> c = LibertyMirrorSubstitutor.class;

    private static final String MIRROR_PREFIX = "wasliberty-";

    @Override
    public DockerImageName apply(final DockerImageName original) {
        final String m = "apply";

        final String repository;
        final String reason;

        // Docker image was already defined in a mirror (only valid for WebSphere Liberty tests)
        if (original.getRepository().startsWith(MIRROR_PREFIX)) {
            return original; // Already in a mirror, return original
        }

        Registry artifactory = ArtifactoryRegistry.instance();
        Registry internal = InternalRegistry.instance();

        if (artifactory.supportsRegistry(original)) {
            repository = artifactory.getMirrorRepository(original) + "/" + original.getRepository();
            reason = "Artifactory has a mirror for: " + original.getRegistry();
        } else if (internal.supportsRegistry(original)) {
            repository = internal.getMirrorRepository(original) + "/" + original.getRepository();
            reason = "Internal registry has a mirror for: " + original.getRegistry();
        } else {
            return original; //No mirror available, therefore return original
        }

        DockerImageName result = original;
        result = result.withRegistry(""); //Remove registry
        result = result.withRepository(repository); //Substitute repository

        Log.finer(c, m, "Applied substitution because: " + reason);
        Log.finer(c, m, original.asCanonicalNameString() + " --> " + result.asCanonicalNameString());
        return result;
    }

    @Override
    protected String getDescription() {
        return "LibertyMirrorSubstitutor";
    }
}
