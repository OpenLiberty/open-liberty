/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.provisioining;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

import com.ibm.ws.kernel.boot.internal.FileUtils;
import com.ibm.ws.kernel.provisioning.ServiceFingerprint;

/**
 *
 */
public class ServiceFingerprintTest {
    private static File workArea = new File("build/service.print/workarea/");
    private static File installDir = new File("../build.image/wlp");
    @Rule
    public TestRule sharedOutputRuleThingy = SharedOutputManager.getInstance();

    @Before
    public void setup() {
        new File(workArea, "platform").mkdirs();
        ServiceFingerprint.hasServiceBeenApplied(installDir, workArea);
        ServiceFingerprint.putInstallDir(null, installDir);
    }

    /**
     * Add a single file to the ServiceFingerprint. Then check to see if service has been applied. Since the file has not
     * changed it should not have changed.
     */
    @Test
    public void checkServiceDetectionWithNoChanges() {
        ServiceFingerprint.put(new File("resources/packages.list"));
        assertFalse("We should not have detected a file change", ServiceFingerprint.hasServiceBeenApplied(installDir, workArea));
    }

    /**
     * Add two files to the ServiceFingerprint. One doesn't change. One is deleted. Then check to see if service has been applied.
     * Since a file is deleted it should return true for the final call to hasServiceBeenApplied.
     */
    @Test
    public void checkServiceDetectionWithFileDelete() throws IOException {
        ServiceFingerprint.put(new File("resources/packages.list"));
        File tmp = File.createTempFile("test", ".txt");
        PrintStream out = new PrintStream(tmp);
        out.println("some data");
        out.close();
        ServiceFingerprint.put(tmp);
        assertFalse("We should not have detected a file change", ServiceFingerprint.hasServiceBeenApplied(installDir, workArea));
        tmp.delete();
        assertTrue("We should have detected a file change", ServiceFingerprint.hasServiceBeenApplied(installDir, workArea));
    }

    /**
     * Add two files to the ServiceFingerprint. One doesn't change. One doesn't exist when it is initially added. The
     * file is then created before checking to see if service has been applied.
     * Since a file is added it should return true for the final call to hasServiceBeenApplied.
     */
    @Test
    public void checkServiceDetectionWithFileCreation() throws IOException {
        ServiceFingerprint.put(new File("resources/packages.list"));
        File tmp = File.createTempFile("test", ".txt");
        tmp.delete();
        ServiceFingerprint.put(tmp);
        assertFalse("We should not have detected a file change", ServiceFingerprint.hasServiceBeenApplied(installDir, workArea));
        PrintStream out = new PrintStream(tmp);
        out.println("some data");
        out.close();
        assertTrue("We should have detected a file change", ServiceFingerprint.hasServiceBeenApplied(installDir, workArea));
        tmp.delete();
    }

    /**
     * Add two files to the ServiceFingerprint. One doesn't change. One does. Then check to see if service has been applied.
     * Since a file has changed it should return true for the final call to hasServiceBeenApplied.
     */
    @Test
    public void checkServiceDetectionWithFileChange() throws IOException {
        ServiceFingerprint.put(new File("resources/packages.list"));
        assertFalse("We should not have detected a file change", ServiceFingerprint.hasServiceBeenApplied(installDir, workArea));
        File tmp = File.createTempFile("test", ".txt");
        tmp.delete();
        PrintStream out = new PrintStream(tmp);
        out.println("some data");
        out.close();
        ServiceFingerprint.put(tmp);
        assertFalse("We should not have detected a file change", ServiceFingerprint.hasServiceBeenApplied(installDir, workArea));
        out = new PrintStream(tmp);
        out.println("some totally new data");
        out.close();
        assertTrue("We should have detected a file change", ServiceFingerprint.hasServiceBeenApplied(installDir, workArea));
        tmp.delete();
    }

    /**
     * Add two files to the ServiceFingerprint. One doesn't change. One is deleted. Then check to see if service has been applied.
     * Since a file is deleted it should return true for the final call to hasServiceBeenApplied.
     */
    @Test
    public void checkServiceDetectionWithFileDeleteAndClear() throws IOException {
        File tmpInstall = File.createTempFile("wlp", null);
        tmpInstall.delete();
        File tmpVersions = new File(tmpInstall, "lib/versions/");
        tmpVersions.mkdirs();
        File tmpServiceFingerprint = new File(tmpVersions, "service.fingerprint");
        PrintStream out = new PrintStream(tmpServiceFingerprint);
        out.println("some data");
        out.close();
        ServiceFingerprint.putInstallDir(null, tmpInstall);
        assertFalse("We should not have detected a file change", ServiceFingerprint.hasServiceBeenApplied(tmpInstall, workArea));
        // rename the install area
        File renameTmpInstall = new File(tmpInstall.getParentFile(), tmpInstall.getName() + "2");
        tmpInstall.renameTo(renameTmpInstall);
        assertTrue("We should have detected a file change", ServiceFingerprint.hasServiceBeenApplied(tmpInstall, workArea));
        ServiceFingerprint.clear();
        ServiceFingerprint.putInstallDir(null, renameTmpInstall);
        assertFalse("We should not have detected a file change", ServiceFingerprint.hasServiceBeenApplied(renameTmpInstall, workArea));
        FileUtils.recursiveClean(renameTmpInstall);
    }

    @After
    public void cleanup() throws FileNotFoundException, IOException {
        Properties props = new Properties();
        props.store(new FileOutputStream(new File(workArea, "platform/service.fingerprint")), null);
        ServiceFingerprint.hasServiceBeenApplied(installDir, workArea);
        new File("build/service.print/workarea/platform/service.fingerprint").delete();
    }
}