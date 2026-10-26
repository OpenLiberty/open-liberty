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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

/**
 * Unit tests for the {@code writeMavenCoordinates} logic in {@link FeatureList}.
 * <p>
 * Tests call {@link FeatureList#collectMavenCoordinates} directly (package-private)
 * so they exercise the real production code path, not a hand-rolled copy.
 * XML output is validated via {@link FeatureListWriter} round-trips.
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
     * Builds a minimal {@link FeatureList} whose {@code features} map is populated
     * with the supplied definitions.  {@code options} and {@code utils} are null because
     * {@code collectMavenCoordinates} never accesses them.
     */
    private static FeatureList buildFeatureList(Map<String, ProvisioningFeatureDefinition> features) {
        // coreFeatures is null — extension-repository fallback is tested separately
        return new FeatureList(null, features, null, null);
    }

    /**
     * Invokes the real production {@code collectMavenCoordinates} and returns the
     * accumulated list of coordinates in insertion order.
     */
    private static List<String> collect(FeatureList fl, ProvisioningFeatureDefinition fd) {
        LinkedHashSet<String> seen = new LinkedHashSet<String>();
        fl.collectMavenCoordinates(fd, seen, new HashSet<String>());
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
    // collectMavenCoordinates – flat (single-feature) extraction tests
    // -----------------------------------------------------------------------

    @Test
    public void collectMavenCoordinates_twoDistinctCoords() throws IOException {
        ProvisioningFeatureDefinition fd = loadFeature("featureWithMavenCoords.mf");
        FeatureList fl = buildFeatureList(new HashMap<String, ProvisioningFeatureDefinition>());

        List<String> coords = collect(fl, fd);

        assertEquals("Expected exactly two distinct maven coordinates", 2, coords.size());
        assertTrue("Expected grpc coordinate", coords.contains("io.grpc:grpc-api:1.69.0"));
        assertTrue("Expected cdi coordinate", coords.contains("jakarta.cdi:cdi-api:3.0.1"));
    }

    @Test
    public void collectMavenCoordinates_emptyWhenNoneDeclared() throws IOException {
        ProvisioningFeatureDefinition fd = loadFeature("featureWithoutMavenCoords.mf");
        FeatureList fl = buildFeatureList(new HashMap<String, ProvisioningFeatureDefinition>());

        List<String> coords = collect(fl, fd);

        assertTrue("Expected empty list when no mavenCoordinates attributes present", coords.isEmpty());
    }

    @Test
    public void collectMavenCoordinates_deduplicatesIdentical() throws IOException {
        ProvisioningFeatureDefinition fd = loadFeature("featureWithDupMavenCoords.mf");
        FeatureList fl = buildFeatureList(new HashMap<String, ProvisioningFeatureDefinition>());

        List<String> coords = collect(fl, fd);

        assertEquals("Expected exactly one entry after deduplication", 1, coords.size());
        assertEquals("io.grpc:grpc-api:1.69.0", coords.get(0));
    }

    // -----------------------------------------------------------------------
    // collectMavenCoordinates – recursive sub-feature traversal tests
    //
    // Fixture chain (mirrors the real mpHealth-4.0 hierarchy):
    //   publicFeatureWithSubFeatureCoords.mf   (PUBLIC, no direct coords)
    //     └─ privateSubFeatureNoCoords.mf      (PRIVATE, no direct coords, depth 1)
    //          └─ privateDeepSubFeatureWithCoords.mf  (PRIVATE, has mavenCoordinates, depth 2)
    // -----------------------------------------------------------------------

    /**
     * Loads all three recursive-chain fixtures into a features map and returns it,
     * ready for use when constructing a {@link FeatureList}.
     */
    private static Map<String, ProvisioningFeatureDefinition> buildRecursiveFeatureMap() throws IOException {
        ProvisioningFeatureDefinition depth1 = loadFeature("privateSubFeatureNoCoords.mf");
        ProvisioningFeatureDefinition depth2 = loadFeature("privateDeepSubFeatureWithCoords.mf");

        Map<String, ProvisioningFeatureDefinition> features = new HashMap<String, ProvisioningFeatureDefinition>();
        features.put(depth1.getSymbolicName(), depth1);
        features.put(depth2.getSymbolicName(), depth2);
        return features;
    }

    @Test
    public void collectMavenCoordinates_recursesIntoPrivateSubFeature() throws IOException {
        ProvisioningFeatureDefinition publicFd = loadFeature("publicFeatureWithSubFeatureCoords.mf");
        FeatureList fl = buildFeatureList(buildRecursiveFeatureMap());

        List<String> coords = collect(fl, publicFd);

        assertEquals("Expected exactly one coordinate surfaced from private sub-feature", 1, coords.size());
        assertEquals("org.eclipse.microprofile.health:microprofile-health-api:4.0.1", coords.get(0));
    }

    @Test
    public void collectMavenCoordinates_emptyWhenSubFeatureNotInMap() throws IOException {
        // Sub-feature symbolic names are not in the features map → simulate missing resolution.
        ProvisioningFeatureDefinition publicFd = loadFeature("publicFeatureWithSubFeatureCoords.mf");
        FeatureList fl = buildFeatureList(new HashMap<String, ProvisioningFeatureDefinition>());

        List<String> coords = collect(fl, publicFd);

        assertTrue("Expected empty list when sub-features cannot be resolved", coords.isEmpty());
    }

    @Test
    public void collectMavenCoordinates_cycleGuardPreventsInfiniteLoop() throws IOException {
        // Point the public feature's sub-feature entry back at itself via the map
        // to simulate a (pathological) cycle — the visited guard must prevent stack overflow.
        ProvisioningFeatureDefinition publicFd = loadFeature("publicFeatureWithSubFeatureCoords.mf");
        ProvisioningFeatureDefinition depth1   = loadFeature("privateSubFeatureNoCoords.mf");

        Map<String, ProvisioningFeatureDefinition> features = new HashMap<String, ProvisioningFeatureDefinition>();
        // depth1 includes depth2 by symbolic name; map depth2's name back to depth1 → cycle
        features.put(depth1.getSymbolicName(), depth1);
        features.put("io.openliberty.test.recursive.private.deep-1.0", depth1); // cycle

        FeatureList fl = buildFeatureList(features);

        // Must complete without StackOverflowError
        List<String> coords = collect(fl, publicFd);
        // depth1 has no bundle mavenCoordinates so result should be empty
        assertTrue("Cycle guard should yield empty result for this fixture", coords.isEmpty());
    }

    @Test
    public void collectMavenCoordinates_coreFeaturesFallback() throws IOException {
        // Sub-feature is absent from 'features' but present in 'coreFeatures'.
        // This exercises the extension-repository code path.
        ProvisioningFeatureDefinition publicFd = loadFeature("publicFeatureWithSubFeatureCoords.mf");
        ProvisioningFeatureDefinition depth1   = loadFeature("privateSubFeatureNoCoords.mf");
        ProvisioningFeatureDefinition depth2   = loadFeature("privateDeepSubFeatureWithCoords.mf");

        // features map is empty; supply sub-features via coreFeatures
        Map<String, ProvisioningFeatureDefinition> emptyFeatures = new HashMap<String, ProvisioningFeatureDefinition>();
        Map<String, ProvisioningFeatureDefinition> coreFeatures  = new HashMap<String, ProvisioningFeatureDefinition>();
        coreFeatures.put(depth1.getSymbolicName(), depth1);
        coreFeatures.put(depth2.getSymbolicName(), depth2);

        FeatureList fl = new FeatureList(null, emptyFeatures, coreFeatures, null);

        List<String> coords = collect(fl, publicFd);

        assertEquals("coreFeatures fallback should surface nested coordinate", 1, coords.size());
        assertEquals("org.eclipse.microprofile.health:microprofile-health-api:4.0.1", coords.get(0));
    }

    // -----------------------------------------------------------------------
    // FeatureListWriter XML output – write-then-read integration tests
    // -----------------------------------------------------------------------

    @Test
    public void featureListWriter_writesMavenCoordinatesElements() throws Exception {
        ProvisioningFeatureDefinition fd = loadFeature("featureWithMavenCoords.mf");
        FeatureList fl = buildFeatureList(new HashMap<String, ProvisioningFeatureDefinition>());
        List<String> coords = collect(fl, fd);

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
        FeatureList fl = buildFeatureList(new HashMap<String, ProvisioningFeatureDefinition>());
        List<String> coords = collect(fl, fd);

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
        FeatureList fl = buildFeatureList(new HashMap<String, ProvisioningFeatureDefinition>());
        List<String> coords = collect(fl, fd);

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

    @Test
    public void featureListWriter_writesRecursivelyCollectedCoordinate() throws Exception {
        ProvisioningFeatureDefinition publicFd = loadFeature("publicFeatureWithSubFeatureCoords.mf");
        FeatureList fl = buildFeatureList(buildRecursiveFeatureMap());
        List<String> coords = collect(fl, publicFd);

        File outFile = tmp.newFile("featureList-recursive.xml");
        FeatureListUtils utils = new FeatureListUtils(buildOptions(outFile));
        FeatureListWriter writer = new FeatureListWriter(utils);

        utils.writeStartDocument();
        utils.startFeatureInfo("test", "lib/features", null);
        writer.startFeature("feature", "test-recursive-public-1.0");
        for (String coord : coords) {
            writer.writeTextElement("mavenCoordinates", coord);
        }
        writer.endFeature();
        utils.endFeatureInfo();
        utils.writeEndDocument();

        String xml = new String(Files.readAllBytes(outFile.toPath()), StandardCharsets.UTF_8);

        assertTrue("XML should contain coordinate sourced from depth-2 private sub-feature",
                xml.contains("<mavenCoordinates>org.eclipse.microprofile.health:microprofile-health-api:4.0.1</mavenCoordinates>"));
    }
}
