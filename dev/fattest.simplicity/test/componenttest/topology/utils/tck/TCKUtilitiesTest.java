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
        final String FORCE_EXTERNAL_PROP = TCKUtilities.FAT_TEST_PREFIX + TCKUtilities.ARTIFACTORY_FORCE_EXTERNAL_KEY;
        final String ART_SERVER_PROP = TCKUtilities.FAT_TEST_PREFIX + TCKUtilities.ARTIFACTORY_SERVER_KEY;

        logTest(1, ART_SERVER_PROP, "null", FORCE_EXTERNAL_PROP, "null");
        System.clearProperty(ART_SERVER_PROP);
        System.clearProperty(FORCE_EXTERNAL_PROP);
        assertFalse(TCKUtilities.useArtifactory());

        logTest(2, ART_SERVER_PROP, "null", FORCE_EXTERNAL_PROP, "true");
        System.clearProperty(ART_SERVER_PROP);
        System.setProperty(FORCE_EXTERNAL_PROP, "true");
        assertFalse(TCKUtilities.useArtifactory());

        logTest(3, ART_SERVER_PROP, "null", FORCE_EXTERNAL_PROP, "false");
        System.clearProperty(ART_SERVER_PROP);
        System.setProperty(FORCE_EXTERNAL_PROP, "false");
        assertFalse(TCKUtilities.useArtifactory());

        logTest(4, ART_SERVER_PROP, "example.com", FORCE_EXTERNAL_PROP, "null");
        System.setProperty(ART_SERVER_PROP, "example.com");
        System.clearProperty(FORCE_EXTERNAL_PROP);
        assertTrue(TCKUtilities.useArtifactory());

        logTest(5, ART_SERVER_PROP, "example.com", FORCE_EXTERNAL_PROP, "true");
        System.setProperty(ART_SERVER_PROP, "example.com");
        System.setProperty(FORCE_EXTERNAL_PROP, "true");
        assertFalse(TCKUtilities.useArtifactory());

        logTest(6, ART_SERVER_PROP, "example.com", FORCE_EXTERNAL_PROP, "false");
        System.setProperty(ART_SERVER_PROP, "example.com");
        System.setProperty(FORCE_EXTERNAL_PROP, "false");
        assertTrue(TCKUtilities.useArtifactory());

        logTest(7, ART_SERVER_PROP, "", FORCE_EXTERNAL_PROP, "null");
        System.setProperty(ART_SERVER_PROP, "");
        System.clearProperty(FORCE_EXTERNAL_PROP);
        assertFalse(TCKUtilities.useArtifactory());

        logTest(8, ART_SERVER_PROP, "", FORCE_EXTERNAL_PROP, "true");
        System.setProperty(ART_SERVER_PROP, "");
        System.setProperty(FORCE_EXTERNAL_PROP, "true");
        assertFalse(TCKUtilities.useArtifactory());

        logTest(9, ART_SERVER_PROP, "", FORCE_EXTERNAL_PROP, "false");
        System.setProperty(ART_SERVER_PROP, "");
        System.setProperty(FORCE_EXTERNAL_PROP, "false");
        assertFalse(TCKUtilities.useArtifactory());

    }

    /**
     * Test method: TCKUtilities.getArtifactoryServer()
     * Variables: artifactory.download.server
     */
    @Test
    public void getArtifactoryServer() {
        final String ART_SERVER_PROP = TCKUtilities.FAT_TEST_PREFIX + TCKUtilities.ARTIFACTORY_SERVER_KEY;

        System.clearProperty(ART_SERVER_PROP);
        assertNull(TCKUtilities.getArtifactoryServer());
        assertTrue(Objects.isNull(TCKUtilities.getArtifactoryServer()));

        System.setProperty(ART_SERVER_PROP, "${env.artifactory.download.server}");
        assertNull(TCKUtilities.getArtifactoryServer());
        assertTrue(Objects.isNull(TCKUtilities.getArtifactoryServer()));

        System.setProperty(ART_SERVER_PROP, "");
        assertNull(TCKUtilities.getArtifactoryServer());
        assertTrue(Objects.isNull(TCKUtilities.getArtifactoryServer()));

        System.setProperty(ART_SERVER_PROP, "example.com");
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