/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.utils.tck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.TCKJarInfo;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;

public class TCKResultInfoTest {

    @Mock
    LibertyServer server;

    private static final TCKJarInfo VERSION_1_0_0 = new TCKJarInfo();

    @BeforeClass
    public static void setup() {
        VERSION_1_0_0.version = "1.0.0";
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getFullSpecNameTest() {
        TCKResultsInfo microprofile = new TCKResultsInfo(Type.MICROPROFILE, "Config", null, VERSION_1_0_0);
        TCKResultsInfo jakarta = new TCKResultsInfo(Type.JAKARTA, "Data", null, VERSION_1_0_0);

        assertEquals("MicroProfile Config 1.0.0", microprofile.getFullSpecName());
        assertEquals("Jakarta Data 1.0.0", jakarta.getFullSpecName());
    }

    @Test
    public void getSpecNameForURLTest() throws Exception {
        TCKResultsInfo microprofile = new TCKResultsInfo(Type.MICROPROFILE, "Fault Tolerance", null, VERSION_1_0_0);
        TCKResultsInfo jakarta = new TCKResultsInfo(Type.JAKARTA, "Dependency Injection", null, VERSION_1_0_0);

        assertEquals("fault-tolerance", getSpecNameForURL().invoke(microprofile));
        assertEquals("dependency-injection", getSpecNameForURL().invoke(jakarta));
    }

    @Test
    public void getSpecURLTest() {
        TCKResultsInfo microprofile = new TCKResultsInfo(Type.MICROPROFILE, "Fault Tolerance", null, VERSION_1_0_0);
        TCKResultsInfo jakarta = new TCKResultsInfo(Type.JAKARTA, "Data", null, VERSION_1_0_0);

        assertEquals("https://github.com/eclipse/microprofile-fault-tolerance/tree/1.0.0", microprofile.getSpecURL());
        assertEquals("https://jakarta.ee/specifications/data/1.0.0", jakarta.getSpecURL());
    }

    @Test
    public void getTCKURLTest() {
        TCKResultsInfo microprofile = new TCKResultsInfo(Type.MICROPROFILE, "Fault Tolerance", null, VERSION_1_0_0);
        TCKResultsInfo jakartaWithOutVersion = new TCKResultsInfo(Type.JAKARTA, "Data", null, VERSION_1_0_0);
        TCKResultsInfo jakartaWithVersion = new TCKResultsInfo(Type.JAKARTA, "Dependency Injection", null, VERSION_1_0_0);
        jakartaWithVersion.withPlatformVersion("10");

        assertEquals("https://repo1.maven.org/maven2/org/eclipse/microprofile/fault-tolerance/microprofile-fault-tolerance-tck/1.0.0/microprofile-fault-tolerance-tck-1.0.0.jar",
                     microprofile.getTCKURL());
        assertEquals("https://download.eclipse.org/ee4j/data/jakartaee/promoted/eftl/data-tck-1.0.0.zip",
                     jakartaWithOutVersion.getTCKURL());
        assertEquals("https://download.eclipse.org/ee4j/dependency-injection/jakartaee10/promoted/eftl/dependency-injection-tck-1.0.0.zip",
                     jakartaWithVersion.getTCKURL());
    }

    @Test
    public void getFilenameTest() {
        when(server.getOpenLibertyVersion()).thenReturn("24.0.0.9");

        TCKResultsInfo microprofile = new TCKResultsInfo(Type.MICROPROFILE, "Fault Tolerance", server, VERSION_1_0_0);
        TCKResultsInfo jakartaWithoutQualifiers = new TCKResultsInfo(Type.JAKARTA, "Data", server, VERSION_1_0_0);
        TCKResultsInfo jakartaWithQualifiers = new TCKResultsInfo(Type.JAKARTA, "Dependency Injection", server, VERSION_1_0_0);
        jakartaWithQualifiers.withQualifiers(new String[] {"full", "web", "core", "nosql", "persistence"});

        microprofile = when(spy(microprofile).getJavaMajorVersion()).thenReturn("17").getMock();
        jakartaWithoutQualifiers = when(spy(jakartaWithoutQualifiers).getJavaMajorVersion()).thenReturn("19").getMock();
        jakartaWithQualifiers = when(spy(jakartaWithQualifiers).getJavaMajorVersion()).thenReturn("21").getMock();

        assertEquals("24.0.0.9-MicroProfile-Fault-Tolerance-1.0.0-Java17-TCKResults.adoc", microprofile.getFilename());
        assertEquals("24.0.0.9-Jakarta-Data-1.0.0-Java19-TCKResults.adoc", jakartaWithoutQualifiers.getFilename());
        assertEquals("24.0.0.9-Jakarta-Dependency-Injection-1.0.0-full-web-core-nosql-persistence-Java21-TCKResults.adoc", jakartaWithQualifiers.getFilename());
    }

    @Test
    public void getDirectoryNameTest() {
        // Ensure no repeat actions are active (left over from other unit tests)
        while (RepeatTestFilter.isAnyRepeatActionActive()) {
            RepeatTestFilter.deactivateRepeatAction();
        }

        TCKResultsInfo testResult;

        // No replacement action
        RepeatTestFilter.activateRepeatAction(FeatureReplacementAction.NO_REPLACEMENT());
        testResult = new TCKResultsInfo(Type.MICROPROFILE, "Fault Tolerance", null, VERSION_1_0_0);
        assertEquals("TCK_Results_Certifications", testResult.getDirectoryName());
        RepeatTestFilter.deactivateRepeatAction();

        // Single replacement action
        RepeatTestFilter.activateRepeatAction(FeatureReplacementAction.EE11_FEATURES());
        testResult = new TCKResultsInfo(Type.MICROPROFILE, "Fault Tolerance", null, VERSION_1_0_0);
        assertEquals("TCK_Results_Certifications_EE11_FEATURES", testResult.getDirectoryName());
        RepeatTestFilter.deactivateRepeatAction();

        // Multiple replacement actions
        RepeatTestFilter.activateRepeatAction(FeatureReplacementAction.EE10_FEATURES());
        RepeatTestFilter.activateRepeatAction(FeatureReplacementAction.BETA_OPTION());
        testResult = new TCKResultsInfo(Type.MICROPROFILE, "Fault Tolerance", null, VERSION_1_0_0);
        assertEquals("TCK_Results_Certifications_EE10_FEATURES_BETA_JVM_OPTIONS", testResult.getDirectoryName());
        RepeatTestFilter.deactivateRepeatAction();
        RepeatTestFilter.deactivateRepeatAction();

        // Manual add/remove features
        FeatureReplacementAction action = new FeatureReplacementAction();
        action.removeFeatures(Stream.of("jpa-2.1", "jpa-2.2", "persistence-3.0", "persistence-3.1").collect(Collectors.toSet()));
        action.addFeature("persistence-3.2");
        RepeatTestFilter.activateRepeatAction(action);
        testResult = new TCKResultsInfo(Type.MICROPROFILE, "Fault Tolerance", null, VERSION_1_0_0);
        assertEquals("TCK_Results_Certifications_remove_persistence-3.1-jpa-2.2-jpa-2.1-persistence-3.0_add_persistence-3.2", testResult.getDirectoryName());
        RepeatTestFilter.deactivateRepeatAction();

        // Ensure we cleaned up after ourselves
        assertFalse(RepeatTestFilter.isAnyRepeatActionActive());
    }

    private Method getSpecNameForURL() throws Exception {
        Method getSpecNameForURL = TCKResultsInfo.class.getDeclaredMethod("getSpecNameForURL");
        getSpecNameForURL.setAccessible(true);
        return getSpecNameForURL;
    }
}
