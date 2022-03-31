/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.featureverifier.internal.tests;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import io.openliberty.featureverifier.internal.buildtasks.semantic.versioning.model.FeatureInfo;
import io.openliberty.featureverifier.internal.buildtasks.semantic.versioning.model.NewFeatureInfo;
import io.openliberty.featureverifier.internal.buildtasks.semantic.versioning.model.VersionedEntity;

/**
 * This test runs the same tests as FeatureInfoTest, but using the new-style manifests (which should
 * give the same answers)
 */
public class NewFeatureInfoTest extends FeatureInfoTest {

    @Override
    protected FeatureInfo createFromManifest(
                                             Map<VersionedEntity, String> devJars, String installDir,
                                             File manifest) throws IOException {
        FeatureInfo i = NewFeatureInfo.createFromManifest(manifest, installDir, null, devJars);
        return i;
    }

    @Override
    protected String getManifestFileExtension() {
        return ".feature";
    }
}
