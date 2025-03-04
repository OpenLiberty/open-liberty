/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.containers.substitution;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.ImageHelper;
import componenttest.containers.ImageVerifier;
import componenttest.containers.registry.ArtifactoryRegistry;
import componenttest.containers.registry.InternalRegistry;

/**
 * An image name substituter is configured in testcontainers.properties and will transform docker image names.
 * Here we use it to apply a private mirror registry and repository prefix so that in remote builds we use an internal
 * Artifactory mirror for a number of supported Docker image registries.
 */
@SuppressWarnings("deprecation")
public class LibertyImageNameSubstitutor extends ImageNameSubstitutor {

    private static final Class<?> c = LibertyImageNameSubstitutor.class;

    private static final ImageNameSubstitutor MIRROR = new LibertyMirrorSubstitutor();
    private static final ImageNameSubstitutor REGISTRY = new LibertyRegistrySubstitutor();

    @Override
    public DockerImageName apply(final DockerImageName original) {
        final DockerImageName result;
        final String reason;

        do {
            // Priority 1: If we are using a synthetic image do not substitute nor cache
            if (ImageHelper.isSyntheticImage(original) || ImageHelper.isCommittedImage(original) || ImageHelper.isBuiltImage(original)) {
                result = original;
                reason = "Image name is known to be synthetic, prebuilt, or a commit hash, cannot use an alternative registry.";
                break;
            }

            // Priority 2a: If the image is known to only exist in an Artifactory organization
            // This is now handled directly by the MIRROR substitutor

            // Priority 2b: If the image is known to only exist in an Artifactory registry
            if (original.getRegistry() != null && original.getRegistry().contains("artifactory.swg-devops.com")) {
                throw new RuntimeException("Not all developers of Open Liberty have access to artifactory, must use a public registry.");
            }

            // Priority 3: If a public registry was explicitly set on an image, do not substitute
            // This is now handled directly by the MIRROR substitutor

            // Priority 4: Always use mirror registry if using remote docker host.
            if (DockerClientFactory.instance().isUsing(EnvironmentAndSystemPropertyClientProviderStrategy.class)) {
                ImageVerifier.collectImage(original);
                result = REGISTRY.apply(MIRROR.apply(original));
                reason = "Using a remote docker host, must use mirrored registry";
                break;
            }

            // Priority 5: System property artifactory.force.external.repo
            // This is now handled directly by the ArtifactoryRegistry

            // Priority 6: If mirror registry is available use it to avoid rate limits on other registries
            if (ArtifactoryRegistry.instance().isRegistryAvailable() && ArtifactoryRegistry.instance().supportsRegistry(original)) {
                ImageVerifier.collectImage(original);
                result = REGISTRY.apply(MIRROR.apply(original));
                reason = "Artifactory registry was available and supports " + original.getRegistry();
                break;
            }

            if (InternalRegistry.instance().isRegistryAvailable() && InternalRegistry.instance().supportsRegistry(original)) {
                ImageVerifier.collectImage(original);
                result = REGISTRY.apply(MIRROR.apply(original));
                reason = "Internal registry was available and supports " + original.getRegistry();
                break;
            }

            //default - use original
            ImageVerifier.collectImage(original);
            result = original;
            reason = "Default behavior: use default docker registry.";
        } while (false);

        Log.info(c, "apply", original.asCanonicalNameString() + " --> " + result.asCanonicalNameString()
                             + System.lineSeparator() + "Reason: " + reason);

        return result;
    }

    @Override
    protected String getDescription() {
        return "LibertyImageNameSubstitutor";
    }
}
