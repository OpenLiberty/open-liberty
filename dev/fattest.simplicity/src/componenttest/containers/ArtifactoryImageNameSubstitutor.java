/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
package componenttest.containers;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import componenttest.containers.substitution.LibertyImageNameSubstitutor;

/**
 * Moved to componenttest.containers.LibertyImageNameSubstitutor
 * TODO remove this class once all references are updated
 */
@Deprecated
public class ArtifactoryImageNameSubstitutor extends ImageNameSubstitutor {

    @Override
    public DockerImageName apply(final DockerImageName original) {
        return new LibertyImageNameSubstitutor().apply(original);
    }

    @Override
    protected String getDescription() {
        return "ArtifactoryImageNameSubstitutor is deprecated";
    }
}
