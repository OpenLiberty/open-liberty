/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import componenttest.containers.substitution.LibertyRegistrySubstitutor;

/**
 * Moved to componenttest.containers.LibertyRegistrySubstitutor
 * TODO remove this class once all references are updated
 */
@Deprecated
public class ArtifactoryRegistrySubstitutor extends ImageNameSubstitutor {

    @Override
    public DockerImageName apply(DockerImageName original) {
        return new LibertyRegistrySubstitutor().apply(original);
    }

    @Override
    protected String getDescription() {
        return "ArtifactoryRegistrySubstitutor is deprecated";
    }

}
