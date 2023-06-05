/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package componenttest.topology.utils.tck.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import componenttest.topology.utils.tck.TCKResultsInfo.TCKJarInfo;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKUtilities;

/**
 *
 */
public class DependencyParserTest {

    @Test
    public void testMPTCKPatternMatcher() {
        String microprofile = "11:52:57,783 [INFO]    org.eclipse.microprofile.health:microprofile-health-tck:jar:1.0:compile:/Users/tevans/.m2/repository/org/eclipse/microprofile/health/microprofile-health-tck/1.0/microprofile-health-tck-1.0.jar";
        Pattern tckPattern = TCKUtilities.getTCKPatternMatcher(Type.MICROPROFILE);
        Matcher nameMatcher = tckPattern.matcher(microprofile);
        if (nameMatcher.find()) {
            String group = nameMatcher.group("group");
            String artifact = nameMatcher.group("artifact");
            String version = nameMatcher.group("version");
            String jarPath = nameMatcher.group("path");

            System.out.println(group + ":" + artifact + ":" + version + "=" + jarPath);

            assertEquals("org.eclipse.microprofile.health", group);
            assertEquals("microprofile-health-tck", artifact);
            assertEquals("1.0", version);
            assertEquals("/Users/tevans/.m2/repository/org/eclipse/microprofile/health/microprofile-health-tck/1.0/microprofile-health-tck-1.0.jar", jarPath);
        } else {
            fail("Pattern did not match");
        }
    }

    @Test
    public void testWindowsMPTCKPatternMatcher() {
        String microprofile = "23:09:37,026 [INFO]    org.eclipse.microprofile.health:microprofile-health-tck:jar:1.0:compile:C:\\Users\\jazz_build\\Build\\jazz-build-engines\\wasrtc\\EBCPROD\\build\\dev\\com.ibm.ws.microprofile.health.1.0_fat_tck\\autoFVT\\publish\\tckRunner\\apache-maven-3.8.6\\repo\\org\\eclipse\\microprofile\\health\\microprofile-health-tck\\1.0\\microprofile-health-tck-1.0.jar";
        Pattern tckPattern = TCKUtilities.getTCKPatternMatcher(Type.MICROPROFILE);
        Matcher nameMatcher = tckPattern.matcher(microprofile);
        if (nameMatcher.find()) {
            String group = nameMatcher.group("group");
            String artifact = nameMatcher.group("artifact");
            String version = nameMatcher.group("version");
            String jarPath = nameMatcher.group("path");

            System.out.println(group + ":" + artifact + ":" + version + "=" + jarPath);

            assertEquals("org.eclipse.microprofile.health", group);
            assertEquals("microprofile-health-tck", artifact);
            assertEquals("1.0", version);
            assertEquals("C:\\Users\\jazz_build\\Build\\jazz-build-engines\\wasrtc\\EBCPROD\\build\\dev\\com.ibm.ws.microprofile.health.1.0_fat_tck\\autoFVT\\publish\\tckRunner\\apache-maven-3.8.6\\repo\\org\\eclipse\\microprofile\\health\\microprofile-health-tck\\1.0\\microprofile-health-tck-1.0.jar",
                         jarPath);
        } else {
            fail("Pattern did not match");
        }
    }

    @Test
    public void testBadMPTCKPatternMatcher() {
        String microprofile = "11:52:57,784 [INFO]    org.eclipse.microprofile.health:microprofile-health-api:jar:1.0:system:/Users/tevans/Liberty/openLibertyGit/open-liberty/dev/build.image/wlp/dev/api/stable/com.ibm.websphere.org.eclipse.microprofile.health.1.0_1.0.72.jar";
        Pattern tckPattern = TCKUtilities.getTCKPatternMatcher(Type.MICROPROFILE);
        Matcher nameMatcher = tckPattern.matcher(microprofile);
        if (nameMatcher.find()) {
            fail("Pattern should not have matched");
        }
    }

    @Test
    public void testJakartaTCKPatternMatcher() {
        String jakarta = "11:52:57,783 [INFO]    jakarta.json:jakarta.json-tck-tests:jar:2.1.0:compile:/Users/tevans/.m2/repository/jakarta/json/jakarta.json-tck-tests/2.1.0/jakarta.json-tck-tests-2.1.0.jar";
        Pattern tckPattern = TCKUtilities.getTCKPatternMatcher(Type.JAKARTA);
        Matcher nameMatcher = tckPattern.matcher(jakarta);
        if (nameMatcher.find()) {
            String group = nameMatcher.group("group");
            String artifact = nameMatcher.group("artifact");
            String version = nameMatcher.group("version");
            String jarPath = nameMatcher.group("path");

            System.out.println(group + ":" + artifact + ":" + version + "=" + jarPath);

            assertEquals("jakarta.json", group);
            assertEquals("jakarta.json-tck-tests", artifact);
            assertEquals("2.1.0", version);
            assertEquals("/Users/tevans/.m2/repository/jakarta/json/jakarta.json-tck-tests/2.1.0/jakarta.json-tck-tests-2.1.0.jar", jarPath);
        } else {
            fail("Pattern did not match");
        }
    }

    @Test
    public void testJakartaTCKPatternMatcher2() {
        String jakarta = "13:38:19,095 [INFO]    jakarta.json.bind:jakarta.json.bind-tck:jar:3.0.0:test:/Users/tevans/.m2/repository/jakarta/json/bind/jakarta.json.bind-tck/3.0.0/jakarta.json.bind-tck-3.0.0.jar";
        Pattern tckPattern = TCKUtilities.getTCKPatternMatcher(Type.JAKARTA);
        Matcher nameMatcher = tckPattern.matcher(jakarta);
        if (nameMatcher.find()) {
            String group = nameMatcher.group("group");
            String artifact = nameMatcher.group("artifact");
            String version = nameMatcher.group("version");
            String jarPath = nameMatcher.group("path");

            System.out.println(group + ":" + artifact + ":" + version + "=" + jarPath);

            assertEquals("jakarta.json.bind", group);
            assertEquals("jakarta.json.bind-tck", artifact);
            assertEquals("3.0.0", version);
            assertEquals("/Users/tevans/.m2/repository/jakarta/json/bind/jakarta.json.bind-tck/3.0.0/jakarta.json.bind-tck-3.0.0.jar", jarPath);
        } else {
            fail("Pattern did not match");
        }
    }

    @Test
    public void testGetTCKJarInfo() throws IOException {
        List<String> dependencyOutput = loadDependencyFile("dependency.txt");
        TCKJarInfo info = TCKUtilities.parseTCKDependencies(Type.MICROPROFILE, dependencyOutput);
        assertNotNull(info);
        assertEquals("org.eclipse.microprofile.health", info.group);
        assertEquals("microprofile-health-tck", info.artifact);
        assertEquals("1.0", info.version);
        assertEquals("/Users/tevans/.m2/repository/org/eclipse/microprofile/health/microprofile-health-tck/1.0/microprofile-health-tck-1.0.jar", info.jarPath);
    }

    @Test
    public void testWindowsGetTCKJarInfo() throws IOException {
        List<String> dependencyOutput = loadDependencyFile("windowsDependency.txt");
        TCKJarInfo info = TCKUtilities.parseTCKDependencies(Type.MICROPROFILE, dependencyOutput);
        assertNotNull(info);
        assertEquals("org.eclipse.microprofile.health", info.group);
        assertEquals("microprofile-health-tck", info.artifact);
        assertEquals("1.0", info.version);
        assertEquals("C:\\Users\\jazz_build\\Build\\jazz-build-engines\\wasrtc\\EBCPROD\\build\\dev\\com.ibm.ws.microprofile.health.1.0_fat_tck\\autoFVT\\publish\\tckRunner\\apache-maven-3.8.6\\repo\\org\\eclipse\\microprofile\\health\\microprofile-health-tck\\1.0\\microprofile-health-tck-1.0.jar",
                     info.jarPath);
    }

    private static final List<String> loadDependencyFile(String name) throws IOException {
        List<String> result = new ArrayList<>();
        try (InputStream is = DependencyParserTest.class.getResourceAsStream(name)) {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                result.add(line);
            }
        }
        return result;
    }
}
