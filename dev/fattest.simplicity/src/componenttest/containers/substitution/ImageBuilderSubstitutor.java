/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers.substitution;

import java.util.Objects;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import componenttest.containers.ImageHelper;
import componenttest.containers.registry.InternalRegistry;

/**
 * An ImageNameSubstitutor for images built by the {@link componenttest.containers.ImageBuilder}
 * Built images are not kept in a public registry and are typically cached locally
 * or within an internal registry on the network.
 */
public class ImageBuilderSubstitutor extends ImageNameSubstitutor {
    // The repository where all Open Liberty images will be cached
    private static final String REPOSITORY_PREFIX = "openliberty/testcontainers/";

    private final String registry;

    private ImageBuilderSubstitutor() {
        if (InternalRegistry.instance().isRegistryAvailable()) {
            this.registry = InternalRegistry.instance().getRegistry();
        } else {
            this.registry = "localhost";
        }
    }

    @Override
    public DockerImageName apply(final DockerImageName original) {
        Objects.requireNonNull(original);

        if (!original.getRegistry().isEmpty()) {
            throw new IllegalArgumentException("DockerImageName with the registry " + original.getRegistry() +
                                               " cannot be substituted with registry " + registry);
        }

        if (ImageHelper.isSyntheticImage(original) || ImageHelper.isCommittedImage(original)) {
            throw new IllegalArgumentException("DockerImageName " + original.asCanonicalNameString() +
                                               " cannot be substituted because it is synthetic or a commit hash.");
        }

        if (original.getRepository().startsWith(REPOSITORY_PREFIX)) {
            return original.withRegistry(registry);
        } else {
            return original.withRepository(REPOSITORY_PREFIX + original.getRepository()).withRegistry(registry);
        }
    }

    @Override
    protected String getDescription() {
        return "ImageBuilderSubstitutor with registry " + registry;
    }

    // Hide instance method from parent class.
    // which will choose the ImageNameSubstitutor based on environment.
    private static ImageBuilderSubstitutor instance;

    public static synchronized ImageNameSubstitutor instance() {
        if (Objects.isNull(instance)) {
            instance = new ImageBuilderSubstitutor();
        }
        return instance;
    }
}
