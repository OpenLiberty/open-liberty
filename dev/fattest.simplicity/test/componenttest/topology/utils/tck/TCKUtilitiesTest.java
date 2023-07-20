/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.utils.tck;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Objects;

import org.junit.Test;

/**
 * Tests to ensure behavior of TCKUtilities
 */
public class TCKUtilitiesTest {

    /**
     * Test method TCKUtilities.useArtifactory()
     * Variables: artifactory.download.server, artifactory.force.external.repo
     */
    @Test
    public void usingArtifactory() {
        final String forceExternalProp = TCKUtilities.FAT_TEST_PREFIX + TCKUtilities.ARTIFACTORY_FORCE_EXTERNAL_KEY;
        final String artifactoryServerProp = TCKUtilities.FAT_TEST_PREFIX + TCKUtilities.ARTIFACTORY_SERVER_KEY;

        logTest(1, artifactoryServerProp, "null", forceExternalProp, "null");
        System.clearProperty(artifactoryServerProp);
        System.clearProperty(forceExternalProp);
        assertFalse(TCKUtilities.useArtifactory());

        logTest(2, artifactoryServerProp, "null", forceExternalProp, "true");
        System.clearProperty(artifactoryServerProp);
        System.setProperty(forceExternalProp, "true");
        try {
            TCKUtilities.useArtifactory();
            fail("Should have thrown an IllegalStateException");
        } catch (IllegalStateException e) {
            //pass
        }

        logTest(3, artifactoryServerProp, "null", forceExternalProp, "false");
        System.clearProperty(artifactoryServerProp);
        System.setProperty(forceExternalProp, "false");
        assertFalse(TCKUtilities.useArtifactory());

        logTest(4, artifactoryServerProp, "example.com", forceExternalProp, "null");
        System.setProperty(artifactoryServerProp, "example.com");
        System.clearProperty(forceExternalProp);
        assertTrue(TCKUtilities.useArtifactory());

        logTest(5, artifactoryServerProp, "example.com", forceExternalProp, "true");
        System.setProperty(artifactoryServerProp, "example.com");
        System.setProperty(forceExternalProp, "true");
        assertTrue(TCKUtilities.useArtifactory());

        logTest(6, artifactoryServerProp, "example.com", forceExternalProp, "false");
        System.setProperty(artifactoryServerProp, "example.com");
        System.setProperty(forceExternalProp, "false");
        assertTrue(TCKUtilities.useArtifactory());

        logTest(7, artifactoryServerProp, "", forceExternalProp, "null");
        System.setProperty(artifactoryServerProp, "");
        System.clearProperty(forceExternalProp);
        assertFalse(TCKUtilities.useArtifactory());

        logTest(8, artifactoryServerProp, "", forceExternalProp, "true");
        System.setProperty(artifactoryServerProp, "");
        System.setProperty(forceExternalProp, "true");
        try {
            TCKUtilities.useArtifactory();
            fail("Should have thrown an IllegalStateException");
        } catch (IllegalStateException e) {
            //pass
        }

        logTest(9, artifactoryServerProp, "", forceExternalProp, "false");
        System.setProperty(artifactoryServerProp, "");
        System.setProperty(forceExternalProp, "false");
        assertFalse(TCKUtilities.useArtifactory());

    }

    /**
     * Test method: TCKUtilities.getArtifactoryServer()
     * Variables: artifactory.download.server
     */
    @Test
    public void getArtifactoryServer() {
        final String artifactoryServerProp = TCKUtilities.FAT_TEST_PREFIX + TCKUtilities.ARTIFACTORY_SERVER_KEY;

        System.clearProperty(artifactoryServerProp);
        assertNull(TCKUtilities.getArtifactoryServer());
        assertTrue(Objects.isNull(TCKUtilities.getArtifactoryServer()));

        System.setProperty(artifactoryServerProp, "${env.artifactory.download.server}");
        assertNull(TCKUtilities.getArtifactoryServer());
        assertTrue(Objects.isNull(TCKUtilities.getArtifactoryServer()));

        System.setProperty(artifactoryServerProp, "");
        assertNotNull(TCKUtilities.getArtifactoryServer());
        assertFalse(Objects.isNull(TCKUtilities.getArtifactoryServer()));
        assertTrue(TCKUtilities.getArtifactoryServer().isEmpty());

        System.setProperty(artifactoryServerProp, "example.com");
        assertNotNull(TCKUtilities.getArtifactoryServer());
        assertFalse(Objects.isNull(TCKUtilities.getArtifactoryServer()));
        assertFalse(TCKUtilities.getArtifactoryServer().isEmpty());
    }

    public void logTest(Object... ids) {
        String msg = "Test: ";
        for (Object id : ids) {
            msg += id.toString() + ", ";
        }
        System.err.println(msg);
    }

}