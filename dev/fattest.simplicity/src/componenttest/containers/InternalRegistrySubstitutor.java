/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers;

import java.util.Objects;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Replaces the localhost registry required for custom images with an internal registry class address
 * Example: localhost/openliberty/testcontainers/postgres-init:1.0 -> [internal-host]:[internal-port]/openliberty/testcontainers/postgres-init:1.0
 *
 * TODO: For now this InternalRegistrySubstitutor is backed by Artifactory, but will be backed
 * by an alternative registry in the future.
 *
 * TODO: Unit test
 */
public class InternalRegistrySubstitutor extends ImageNameSubstitutor {
    private static final Class<?> c = InternalRegistrySubstitutor.class;

    @Override
    public DockerImageName apply(final DockerImageName original) {
        Objects.requireNonNull(original);

        if (!ImageBuilder.isBuiltImage(original)) {
            throw new RuntimeException("Can only use the InternalRegistrySubstitutor for built images using the componenttest.containers.ImageBuilder class.");
        }

        //TODO Update logic to use an alternative registry to Artifactory
        DockerImageName result = original;
        result = result.withRegistry(ArtifactoryRegistry.instance().getRegistry()); //Replace registry
        result = result.withRepository("wasliberty-infrastructure-docker" + "/" + original.getRepository()); //Prepend repository

        //TODO finer
        Log.info(c, "apply", original.asCanonicalNameString() + " --> " + result.asCanonicalNameString());

        return result;
    }

    @Override
    protected String getDescription() {
        // TODO Auto-generated method stub
        return "InternalRegistrySubstitutor";
    }

}
