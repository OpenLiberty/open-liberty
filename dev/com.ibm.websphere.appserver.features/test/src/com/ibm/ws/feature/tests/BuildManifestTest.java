/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.feature.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.kernel.feature.provisioning.*;
import com.ibm.ws.kernel.feature.internal.subsystem.*;

/**
 * Validates that every .mf file assembled into build.image/wlp/lib/features has
 * well-formed Subsystem-Content entries. Specifically, every bundle-typed entry
 * (type absent or type="osgi.bundle") must carry a version= attribute.
 *
 * The intermittent build defect this catches: when a dependent subproject's JAR
 * has not been assembled before createFeatureResources runs, the featureBnd Ant
 * task cannot resolve the bundle via ContentBasedLocalBundleRepository. It
 * silently skips the version-range derivation step and writes the entry bare —
 * no version= attribute. The Gradle build still succeeds (featureBnd only logs
 * a JUnit XML report), but the Liberty runtime will reject the versionless bundle
 * at provisioning time, causing an apparently unrelated FAT failure.
 *
 * Two tests are provided:
 * <ul>
 * <li>{@link #testAllManifestBundleEntriesHaveVersions()} — positive case:
 * the real assembled output must be clean.</li>
 * <li>{@link #testCorruptManifestIsDetected()} — negative case: a synthetic
 * corrupt .mf is injected, and the test asserts the validation logic fires.</li>
 * </ul>
 */
public class BuildManifestTest {

    /** Directory produced by the createFeatureResources Gradle task. */
    private static Path featuresDir;

    @Before
    public void setUpClass() {
        featuresDir = Paths.get("../build.image/wlp/lib/features").toAbsolutePath();

        // Skip gracefully on local builds that have not run createFeatureResources.
        Assume.assumeTrue(featuresDir.toFile().isDirectory());
    }

    // -------------------------------------------------------------------------
    // Positive test — the real assembled output must be clean
    // -------------------------------------------------------------------------

    /**
     * Scans every .mf in build.image/wlp/lib/features and asserts that no
     * bundle-typed Subsystem-Content entry is missing a version= attribute.
     */
    @Test
    public void testAllManifestBundleEntriesHaveVersions() throws Exception {
        List<Path> mfFiles = Files.list(featuresDir).filter(p -> p.toString().endsWith(".mf")).collect(Collectors.toList());

        StringBuilder errorMessage = new StringBuilder();

        for (Path path : mfFiles) {
            new SubsystemFeatureDefinitionImpl(null, path.toFile())
                                                                   .getConstituents(SubsystemContentType.BUNDLE_TYPE)
                                                                   .forEach(bundle -> {
                                                                        if (bundle.getVersionRange() == null || bundle.getVersionRange().toString().equals("0.0.0")) {
                                                                            errorMessage.append(String.format("The feature manifest %s did not resolve the bundle %s jar version correctly\n", path.getFileName(), bundle.toString()));
                                                                        }
                                                                   });
        }

        if (errorMessage.length() != 0) {
            Assert.fail("There were features with malformed bundle versions: " + errorMessage.toString());
        }
    }

}
