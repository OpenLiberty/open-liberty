/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.kernel.feature.internal.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

/**
 * Unit tests for the {@code writeMavenCoordinates} logic added to {@link FeatureList}.
 * <p>
 * The production method is private, so these tests exercise the same logic through
 * a local mirror ({@link #collectMavenCoordinates}) and validate the XML output
 * produced by {@link FeatureListWriter#writeTextElement} round-trips.
 */
public class FeatureListMavenCoordinatesTest {

    /** Relative path used when the test JVM is launched from the project directory. */
    private static final String TEST_DATA_DIR = "test/test data";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static ProvisioningFeatureDefinition loadFeature(String fileName) throws IOException {
        File mf = new File(TEST_DATA_DIR, fileName);
        return new SubsystemFeatureDefinitionImpl(null, mf);
    }

    /**
     * Mirrors the exact logic of {@code FeatureList#writeMavenCoordinates} so that
     * the extraction logic can be verified in isolation.
     */
    static List<String> collectMavenCoordinates(ProvisioningFeatureDefinition fd) {
        LinkedHashSet<String> seen = new LinkedHashSet<String>();
        for (FeatureResource resource : fd.getConstituents(null)) {
            String coordinate = resource.getAttributes().get("mavenCoordinates");
            if (coordinate != null && !coordinate.isEmpty() && coordinate.split(":").length == 3) {
                seen.add(coordinate);
            }
        }
        return new ArrayList<>(seen);
    }

    private static FeatureListOptions buildOptions(File outputFile) {
        FeatureListOptions opts = new FeatureListOptions();
        opts.setOutputFile(outputFile.getAbsolutePath());
        return opts;
    }

    private static int countOccurrences(String text, String target) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(target, idx)) != -1) {
            count++;
            idx += target.length();
        }
        return count;
    }

    // -----------------------------------------------------------------------
    // collectMavenCoordinates – extraction logic unit tests
    // -----------------------------------------------------------------------

    @Test
    public void collectMavenCoordinates_twoDistinctCoords() throws IOException {
        ProvisioningFeatureDefinition fd = loadFeature("featureWithMavenCoords.mf");

        List<String> coords = collectMavenCoordinates(fd);

        assertEquals("Expected exactly two distinct maven coordinates", 2, coords.size());
        assertTrue("Expected grpc coordinate", coords.contains("io.grpc:grpc-api:1.69.0"));
        assertTrue("Expected cdi coordinate", coords.contains("jakarta.cdi:cdi-api:3.0.1"));
    }

    @Test
    public void collectMavenCoordinates_emptyWhenNoneDeclared() throws IOException {
        ProvisioningFeatureDefinition fd = loadFeature("featureWithoutMavenCoords.mf");

        List<String> coords = collectMavenCoordinates(fd);

        assertTrue("Expected empty list when no mavenCoordinates attributes present", coords.isEmpty());
    }

    @Test
    public void collectMavenCoordinates_deduplicatesIdentical() throws IOException {
        ProvisioningFeatureDefinition fd = loadFeature("featureWithDupMavenCoords.mf");

        List<String> coords = collectMavenCoordinates(fd);

        assertEquals("Expected exactly one entry after deduplication", 1, coords.size());
        assertEquals("io.grpc:grpc-api:1.69.0", coords.get(0));
    }

    // -----------------------------------------------------------------------
    // FeatureListWriter XML output – write-then-read integration tests
    // -----------------------------------------------------------------------

    @Test
    public void featureListWriter_writesMavenCoordinatesElements() throws Exception {
        ProvisioningFeatureDefinition fd = loadFeature("featureWithMavenCoords.mf");
        List<String> coords = collectMavenCoordinates(fd);

        File outFile = tmp.newFile("featureList.xml");
        FeatureListUtils utils = new FeatureListUtils(buildOptions(outFile));
        FeatureListWriter writer = new FeatureListWriter(utils);

        utils.writeStartDocument();
        utils.startFeatureInfo("test", "lib/features", null);
        writer.startFeature("feature", "test-maven-coords-1.0");
        for (String coord : coords) {
            writer.writeTextElement("mavenCoordinates", coord);
        }
        writer.endFeature();
        utils.endFeatureInfo();
        utils.writeEndDocument();

        String xml = new String(Files.readAllBytes(outFile.toPath()), StandardCharsets.UTF_8);

        assertTrue("XML should contain grpc mavenCoordinates element",
                xml.contains("<mavenCoordinates>io.grpc:grpc-api:1.69.0</mavenCoordinates>"));
        assertTrue("XML should contain cdi mavenCoordinates element",
                xml.contains("<mavenCoordinates>jakarta.cdi:cdi-api:3.0.1</mavenCoordinates>"));
    }

    @Test
    public void featureListWriter_noMavenCoordinatesWhenNoneDeclared() throws Exception {
        ProvisioningFeatureDefinition fd = loadFeature("featureWithoutMavenCoords.mf");
        List<String> coords = collectMavenCoordinates(fd);

        File outFile = tmp.newFile("featureList-no-maven.xml");
        FeatureListUtils utils = new FeatureListUtils(buildOptions(outFile));
        FeatureListWriter writer = new FeatureListWriter(utils);

        utils.writeStartDocument();
        utils.startFeatureInfo("test", "lib/features", null);
        writer.startFeature("feature", "test-no-maven-1.0");
        for (String coord : coords) {
            writer.writeTextElement("mavenCoordinates", coord);
        }
        writer.endFeature();
        utils.endFeatureInfo();
        utils.writeEndDocument();

        String xml = new String(Files.readAllBytes(outFile.toPath()), StandardCharsets.UTF_8);

        assertFalse("XML should NOT contain mavenCoordinates element when none declared",
                xml.contains("<mavenCoordinates>"));
    }

    @Test
    public void featureListWriter_deduplicatedCoordinateAppearsOnce() throws Exception {
        ProvisioningFeatureDefinition fd = loadFeature("featureWithDupMavenCoords.mf");
        List<String> coords = collectMavenCoordinates(fd);

        File outFile = tmp.newFile("featureList-dup.xml");
        FeatureListUtils utils = new FeatureListUtils(buildOptions(outFile));
        FeatureListWriter writer = new FeatureListWriter(utils);

        utils.writeStartDocument();
        utils.startFeatureInfo("test", "lib/features", null);
        writer.startFeature("feature", "test-dup-maven-1.0");
        for (String coord : coords) {
            writer.writeTextElement("mavenCoordinates", coord);
        }
        writer.endFeature();
        utils.endFeatureInfo();
        utils.writeEndDocument();

        String xml = new String(Files.readAllBytes(outFile.toPath()), StandardCharsets.UTF_8);

        int count = countOccurrences(xml, "<mavenCoordinates>io.grpc:grpc-api:1.69.0</mavenCoordinates>");
        assertEquals("Deduplicated coordinate should appear exactly once in XML", 1, count);
    }
}
