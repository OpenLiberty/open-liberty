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

import componenttest.containers.substitution.LibertyMirrorSubstitutor;

/**
 * Moved to componenttest.containers.LibertyMirrorSubstitutor
 * TODO remove this class once all references are updated
 */
@Deprecated
public class ArtifactoryMirrorSubstitutor extends ImageNameSubstitutor {

    @Override
    public DockerImageName apply(final DockerImageName original) {
        return new LibertyMirrorSubstitutor().apply(original);
    }

    @Override
    protected String getDescription() {
        return "ArtifactoryMirrorSubstitutor is deprecated";
    }
}
