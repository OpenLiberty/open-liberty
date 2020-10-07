/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.check_dhe;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@Mode(TestMode.LITE)
public class VerifyDHETest {
    final Pattern gavPattern = Pattern.compile("^([\\w\\.\\-]+):([\\w\\.\\-]+):(.+)$");
    final String dheServerRootURL = "https://public.dhe.ibm.com/ibmdl/export/pub/software/olrepo";

    File ossIbmMaven = new File("./oss_ibm.maven");

    final static Set<GAVEntry> excludeSet = new HashSet<GAVEntry>();
    static {
        // If any GAV coordinate should be excluded from checking, add it here.
        // excludeSet.add(new GAVEntry("com.ibm.ws.internal.prereq.java", "java.ibmjgssprovider", "1.8.0"));
    }

    final static Set<GAVEntry> excludeFromPomCheckSet = new HashSet<GAVEntry>();
    static {
        // Some resources have no pom files online, identify those here.
        excludeFromPomCheckSet.add(new GAVEntry("com.ibm.ws.internal.prereq.java", "java.ibmjgssprovider", "1.8.0"));
        excludeFromPomCheckSet.add(new GAVEntry("com.ibm.ws.internal.prereq.java", "java.rtSunKrb5", "1.8.0"));
        excludeFromPomCheckSet.add(new GAVEntry("org.xmlunit", "xmlunit-core", "2.0.0.ibm"));
    }

//    @Test
    public void sanityTest() {
        Assert.assertTrue(ossIbmMaven.exists());

        Matcher m = gavPattern.matcher("org.apache.santuario:xmlsec:1.5.2-ibm");
        Assert.assertTrue(m.matches());

        Assert.assertEquals(3, m.groupCount());
        Assert.assertEquals("org.apache.santuario", m.group(1));
        Assert.assertEquals("xmlsec", m.group(2));
        Assert.assertEquals("1.5.2-ibm", m.group(3));

        GAVEntry ge = new GAVEntry("org.eclipse.persistence", "org.eclipse.persistence.antlr", "2.7.7-69f2c2b");
        Assert.assertEquals("https://public.dhe.ibm.com/ibmdl/export/pub/software/olrepo/org/eclipse/persistence/org.eclipse.persistence.antlr/2.7.7-69f2c2b",
                            ge.getBaseURL(dheServerRootURL));
        Assert.assertEquals("https://public.dhe.ibm.com/ibmdl/export/pub/software/olrepo/org/eclipse/persistence/org.eclipse.persistence.antlr/2.7.7-69f2c2b/org.eclipse.persistence.antlr-2.7.7-69f2c2b.jar",
                            ge.getJarURL(dheServerRootURL));
        Assert.assertEquals("https://public.dhe.ibm.com/ibmdl/export/pub/software/olrepo/org/eclipse/persistence/org.eclipse.persistence.antlr/2.7.7-69f2c2b/org.eclipse.persistence.antlr-2.7.7-69f2c2b.pom",
                            ge.getPomURL(dheServerRootURL));
    }

    @Test
    public void verifyDHE() throws Throwable {
        Set<GAVEntry> gavSet = buildGAVSet(ossIbmMaven);
        gavSet.removeAll(excludeSet);
        Assert.assertTrue(gavSet.size() > 0);

        List<java.lang.AssertionError> assertionErrorList = new ArrayList<java.lang.AssertionError>();
        List<GAVEntry> failedGAVEntries = new ArrayList<GAVEntry>();

        for (GAVEntry entry : gavSet) {
            // Verify that the artifact directory exists
            String baseURL = entry.getBaseURL(dheServerRootURL);
            System.out.println("################################################################################");
            System.out.println("Testing " + baseURL);

            URL url = new URL(baseURL);
            try {
                HttpURLConnection httpConn = HttpUtils.getHttpConnection(url, 200, 10);
                String data = HttpUtils.readConnection(httpConn);
                System.out.println(data);

                // Check that references to the jar (or war) and pom file exist
                String jarFileName = entry.getArtifactName() + "-" + entry.getVersionName() + "." + entry.getFileType();
                String expectedJarAnchor = "<a href=\"" + jarFileName + "\">" + jarFileName + "</a>";

                String pomFileName = entry.getArtifactName() + "-" + entry.getVersionName() + ".pom";
                String expectedPomAnchor = "<a href=\"" + pomFileName + "\">" + pomFileName + "</a>";

                Assert.assertTrue("DHE does not have a jar file for " + entry, data.contains(expectedJarAnchor));

                if (!excludeFromPomCheckSet.contains(entry)) {
                    Assert.assertTrue("DHE does not have a pom file for " + entry, data.contains(expectedPomAnchor));
                }
            } catch (java.lang.AssertionError ae) {
                assertionErrorList.add(ae);
                failedGAVEntries.add(entry);
            } catch (Throwable t) {
                assertionErrorList.add(new java.lang.AssertionError("Failed to read resource at " + baseURL, t));
                failedGAVEntries.add(entry);

                StringBuilder sb = new StringBuilder();
                sb.append("Failed to locate GAV asset ").append(entry).append(" at ").append(baseURL);
                sb.append("\n").append("  Error: ").append(t.toString());
                assertionErrorList.add(new java.lang.AssertionError(sb.toString()));
            }
        }

        if (failedGAVEntries.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(failedGAVEntries.size()).append(" Failed GAV Entries:\n");
            for (GAVEntry e : failedGAVEntries) {
                sb.append("  ").append(e).append("\n");
            }
            assertionErrorList.add(new java.lang.AssertionError(sb.toString()));
        }

        if (assertionErrorList.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(assertionErrorList.size() + " Failed Assertions have been detected:\n");
            for (java.lang.AssertionError ae : assertionErrorList) {
                sb.append("######################################################################\n");
                sb.append(exceptionToString(ae));
                sb.append("\n");
            }
            Assert.fail(sb.toString());
        }
    }

    protected static String exceptionToString(Throwable t) {
        CharArrayWriter caw = new CharArrayWriter();
        t.printStackTrace(new PrintWriter(caw));
        return caw.toString();
    }

    private Set<GAVEntry> buildGAVSet(File ossFile) throws FileNotFoundException, IOException {
        final HashSet<GAVEntry> retVal = new HashSet<GAVEntry>();

        try (BufferedReader br = new BufferedReader(new FileReader(ossFile))) {
            String line = br.readLine();
            while (line != null) {
                System.out.println("Reading line: " + line);
                Matcher m = gavPattern.matcher(line);
                if (m.matches()) {
                    GAVEntry entry = new GAVEntry(m.group(1), m.group(2), m.group(3));
                    retVal.add(entry);
                } else {
                    System.out.println("Line does not match regex: " + line);
                }

                line = br.readLine();
            }
        }

        return retVal;
    }
}
