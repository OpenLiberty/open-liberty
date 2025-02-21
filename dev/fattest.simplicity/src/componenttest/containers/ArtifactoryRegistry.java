/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers;

import java.io.File;

/**
 * Moved to componenttest.containers.registry.ArtifactoryRegistry
 * TODO remove this class once all references are updated
 */
@Deprecated
public class ArtifactoryRegistry {

    //Singleton class
    private static ArtifactoryRegistry instance;

    public static ArtifactoryRegistry instance() {
        if (instance == null) {
            instance = new ArtifactoryRegistry();
        }
        return instance;
    }

    @Deprecated
    public File generateTempDockerConfig(String registry) throws Exception {
        return componenttest.containers.registry.ArtifactoryRegistry.instance().generateTempDockerConfig(registry);
    }
}
