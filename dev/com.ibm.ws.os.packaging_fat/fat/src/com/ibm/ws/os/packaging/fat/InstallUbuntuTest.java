package com.ibm.ws.os.packaging.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.simplicity.OperatingSystem;

public class InstallUbuntuTest extends InstallUtilityToolTest{
    private static final Class<?> c = InstallUbuntuTest.class;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        Assume.assumeTrue(isLinuxUbuntu());
        //Assume.assumeTrue(ConnectedToIMRepo);
        setupEnv();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        final String METHOD_NAME = "cleanup";
        entering(c, METHOD_NAME);
        cleanupEnv();
        exiting(c, METHOD_NAME);
    }

    @Test
    public void testJavaInstall() throws Exception {
       
        String METHOD_NAME = "testJavaInstall";
        entering(c, METHOD_NAME);

        String[] param1s = { "install", "-y", "default-jdk" }; //any java works
        ProgramOutput po = runCommand(METHOD_NAME, "sudo apt-get", param1s);
        assertEquals("Expected exit code", 0, po.getReturnCode()); //if already installed, exit is 0
        exiting(c, METHOD_NAME);
    }

    @Test
    public void testVerifyDebInstall() throws Exception {
       
        String METHOD_NAME = "testVerifyDebInstall";
        entering(c, METHOD_NAME);

        String[] param1s = { "-s", "openliberty" };
        ProgramOutput po = runCommand(METHOD_NAME, "dpkg", param1s);
        assertEquals("Expected exit code", 0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Should contain installed status",
                   output.indexOf("Status: install ok installed") >= 0);
        exiting(c, METHOD_NAME);
    }

    @Test
    public void testServerStartStopDeb() throws Exception {
       
        String METHOD_NAME = "testServerStartStopDeb";
        entering(c, METHOD_NAME);

        String[] param1s = { "start", "openliberty@defaultServer.service" };
        ProgramOutput po1 = runCommand(METHOD_NAME, "sudo systemctl", param1s);
        if (po1.getReturnCode() != 0){
            String[] paramAs = { "status", "openliberty@defaultServer.service", "-l" };
            ProgramOutput poA = runCommand("checkServerStatus", "sudo systemctl", paramAs);
        }
        assertEquals("Expected exit code", 0, po1.getReturnCode());

        File f = new File("/var/run/openliberty/defaultServer.pid");
        assertTrue("Server pid should exist",
                   f.exists());

        String[] param2s = { "stop", "openliberty@defaultServer.service" };
        ProgramOutput po2 = runCommand(METHOD_NAME, "sudo systemctl", param2s);
        if (po2.getReturnCode() != 0){
            String[] paramAs = { "status", "openliberty@defaultServer.service", "-l" };
            ProgramOutput poA = runCommand("checkServerStatus", "sudo systemctl", paramAs);
        }
        assertEquals("Expected exit code", 0, po2.getReturnCode());
        exiting(c, METHOD_NAME);
    }

    @Test
    public void testUninstallDeb() throws Exception {
       
        String METHOD_NAME = "testUninstallDeb";
        entering(c, METHOD_NAME);

        String[] param1s = { "remove", "-y", "openliberty" };
        ProgramOutput po = runCommand(METHOD_NAME, "sudo apt-get", param1s);
        assertEquals("Expected exit code", 0, po.getReturnCode());
        exiting(c, METHOD_NAME);
    }
}
