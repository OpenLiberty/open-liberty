/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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
package com.ibm.ws.wlp.repository.esa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

/**
 * Unit tests for {@link EsaDescriptionHtmlGenerator}, focussing on
 * {@link EsaDescriptionHtmlGenerator#getMavenCoordinates(ProvisioningFeatureDefinition)}
 * and the Maven-coordinates HTML block produced by
 * {@link EsaDescriptionHtmlGenerator#generateDescriptionHtml}.
 */
public class EsaDescriptionHtmlGeneratorTest {

    /** Relative path used when the test JVM is launched from the project directory. */
    private static final String TEST_DATA_DIR = "test/test data";

    private static ProvisioningFeatureDefinition loadFeature(String fileName) throws IOException {
        File mf = new File(TEST_DATA_DIR, fileName);
        return new SubsystemFeatureDefinitionImpl(null, mf);
    }

    // -----------------------------------------------------------------------
    // getMavenCoordinates – returns correct coordinates
    // -----------------------------------------------------------------------

    @Test
    public void getMavenCoordinates_twoDistinctCoordinates() throws IOException {
        ProvisioningFeatureDefinition feature = loadFeature("featureWithMavenCoordinates.mf");

        List<String> coords = EsaDescriptionHtmlGenerator.getMavenCoordinates(feature);

        assertEquals("Expected exactly two distinct maven coordinates", 2, coords.size());
        assertTrue("Expected grpc coordinate", coords.contains("io.grpc:grpc-api:1.0.0"));
        assertTrue("Expected cdi coordinate", coords.contains("jakarta.cdi:cdi-api:3.0.1"));
    }

    @Test
    public void getMavenCoordinates_emptyWhenNoCoordinatesDeclared() throws IOException {
        ProvisioningFeatureDefinition feature = loadFeature("featureWithoutMavenCoordinates.mf");

        List<String> coords = EsaDescriptionHtmlGenerator.getMavenCoordinates(feature);

        assertTrue("Expected empty list when no mavenCoordinates attributes present", coords.isEmpty());
    }

    @Test
    public void getMavenCoordinates_deduplicatesIdenticalCoordinates() throws IOException {
        ProvisioningFeatureDefinition feature = loadFeature("featureWithDuplicateMavenCoordinates.mf");

        List<String> coords = EsaDescriptionHtmlGenerator.getMavenCoordinates(feature);

        assertEquals("Expected exactly one entry after deduplication", 1, coords.size());
        assertEquals("io.grpc:grpc-api:1.69.0", coords.get(0));
    }

    // -----------------------------------------------------------------------
    // generateDescriptionHtml – Maven section present / absent
    // -----------------------------------------------------------------------

    @Test
    public void generateDescriptionHtml_containsMavenSectionWhenCoordinatesPresent() throws IOException {
        ProvisioningFeatureDefinition feature = loadFeature("featureWithMavenCoordinates.mf");
        EsaDescriptionHtmlGenerator gen = new EsaDescriptionHtmlGenerator();

        String html = gen.generateDescriptionHtml(feature, false, null);

        assertTrue("HTML should contain maven coordinates heading",
                html.contains("ibm-wasdev-feature-maven-title"));
        assertTrue("HTML should contain maven coordinates content div",
                html.contains("ibm-wasdev-feature-maven-content"));
        assertTrue("HTML should contain grpc groupId",
                html.contains("io.grpc"));
        assertTrue("HTML should contain grpc artifactId",
                html.contains("grpc-api"));
        assertTrue("HTML should contain grpc version",
                html.contains("1.0.0"));
        assertTrue("HTML should contain cdi artifactId",
                html.contains("cdi-api"));
        assertTrue("HTML should use provided scope",
                html.contains("provided"));
    }

    @Test
    public void generateDescriptionHtml_noMavenSectionWhenNoCoordinates() throws IOException {
        ProvisioningFeatureDefinition feature = loadFeature("featureWithoutMavenCoordinates.mf");
        EsaDescriptionHtmlGenerator gen = new EsaDescriptionHtmlGenerator();

        String html = gen.generateDescriptionHtml(feature, false, null);

        assertFalse("HTML should NOT contain maven coordinates heading when none declared",
                html.contains("ibm-wasdev-feature-maven-title"));
        assertFalse("HTML should NOT contain maven coordinates content div when none declared",
                html.contains("ibm-wasdev-feature-maven-content"));
    }

    @Test
    public void generateDescriptionHtml_dedupedCoordinatesInHtml() throws IOException {
        ProvisioningFeatureDefinition feature = loadFeature("featureWithDuplicateMavenCoordinates.mf");
        EsaDescriptionHtmlGenerator gen = new EsaDescriptionHtmlGenerator();

        String html = gen.generateDescriptionHtml(feature, false, null);

        // The coordinate should appear exactly once as a groupId entry
        int count = countOccurrences(html, "io.grpc");
        assertEquals("Deduplicated coordinate groupId should appear exactly once", 1, count);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private static int countOccurrences(String text, String target) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(target, idx)) != -1) {
            count++;
            idx += target.length();
        }
        return count;
    }
}
