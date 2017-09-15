/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import wlp.lib.extract.platform.Platform;
import wlp.lib.extract.platform.PlatformUtils;

/**
 *
 */
public class PlatformTest {
    private static final File testDir = new File("build/unittest/platformTests").getAbsoluteFile();

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void beforeClass() throws Exception {
        Assert.assertTrue("mkdir -p " + testDir.getAbsolutePath(), testDir.mkdirs() || testDir.isDirectory());
        System.out.println("beforeClass() testDir set to " + testDir.getAbsolutePath());
    }

    @AfterClass
    public static void cleanUp() {
        System.out.println("Clean up testDir directory...");
        if (testDir.exists())
            deleteDir(testDir);
    }

    @Test
    public void testSetChmod() throws Exception {
        if (!Platform.isWindows()) {
            File tempFile = new File(testDir, "chmodTest");
            assertTrue("Unable to create temp file", tempFile.createNewFile());

            try {
                PlatformUtils.chmod(new String[] { tempFile.getAbsolutePath() }, "+x");
            } catch (Exception e) {
                fail("chmod failed with error: " + e.getMessage());
            } finally {
                delete(tempFile);
            }
        }
    }

    @Test
    public void testGetUmask() throws Exception {
        if (!Platform.isWindows()) {
            int umask = 512;
            try {
                umask = PlatformUtils.getUmask();
            } catch (Exception e) {
                fail("Get umask failed with error: " + e.getMessage());
            }

            assertTrue("Unable to get umask", (umask >= 0 && umask < 512));
        }
    }

    @Test
    public void testSetExtattr() throws Exception {
        if (Platform.isZOS()) {
            File tempFile = new File(testDir, "extattrTest");
            assertTrue("Unable to create temp file", tempFile.createNewFile());

            try {
                PlatformUtils.extattr(new String[] { tempFile.getAbsolutePath() }, "+alps");
            } catch (Exception e) {
                fail("extattr failed with error: " + e.getMessage());
            } finally {
                delete(tempFile);
            }
        }
    }

    private static void delete(File file) {
        if (!file.exists()) {
            return;
        }

        File[] files = file.listFiles();
        if (files != null) {
            for (File child : files) {
                delete(child);
            }
        }

        System.out.println("Deleting " + file.getAbsolutePath());
        Assert.assertTrue("delete " + file, file.delete());
    }

    private static void deleteDir(File dir) {
        if (dir.exists()) {
            System.out.println("rm -rf " + dir.getAbsolutePath());
            delete(dir);
        }
    }
}
