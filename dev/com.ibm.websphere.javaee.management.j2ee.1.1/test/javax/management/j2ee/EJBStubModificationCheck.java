/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.management.j2ee;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.Test;

public class EJBStubModificationCheck {

    private static final String testBuildDir = System.getProperty("test.buildDir").replace("\\", "/");

    @Test
    public void testManagementUnmodified() throws IOException {
        verifyChecksum("javax/management/j2ee/Management.class", 279782729L);
    }

    @Test
    public void testManagementHomeUnmodified() throws IOException {
        verifyChecksum("javax/management/j2ee/ManagementHome.class", 2871325588L);
    }

    private void verifyChecksum(String clazz, long expectedChecksum) throws IOException {
        File buildDir = new File(testBuildDir);
        assertTrue((buildDir.exists()));
        File outputBundle = null;
        for (File f : buildDir.listFiles()) {
            if (f.getName().endsWith(".jar") && f.getName().contains("com.ibm.websphere.javaee.management.j2ee.1.1")) {
                outputBundle = f;
                break;
            }
        }
        assertNotNull(outputBundle);
        assertTrue(outputBundle.exists());

        try (JarFile jar = new JarFile(outputBundle)) {
            JarEntry nlsFile = jar.getJarEntry(clazz);
            assertNotNull(nlsFile);
            assertEquals("The class " + clazz + " appears to have been modified! Before proceeding, use JDK 8 or older to generate " +
                         "an EJB stub for this class using the 'rmic' gradle task in this project, then check in the newly generated .java stub into source control " +
                         "and then update the expected checksum in this test case.",
                         expectedChecksum, nlsFile.getCrc());

        }
    }

}
