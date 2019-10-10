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

public class InstallRhelTest extends InstallUtilityToolTest{
    private static final Class<?> c = InstallRhelTest.class;
    public static File openLib = new File("/var/lib/openliberty");
    public static boolean openLibExists = openLib.exists();
    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        Assume.assumeTrue(isLinuxRhel());
        //Assume.assumeTrue(ConnectedToIMRepo);
        if (openLibExists) {
            logger.info("/var/lib/openliberty found. OpenLiberty is Installed");
            setupEnv();
            createServerEnv();
        }
        else {
            logger.info("OpenLiberty did not install successfully");
        }
    }

    @AfterClass
    public static void cleanup() throws Exception {
       if (openLibExists) {
           if (isLinuxRhel()){
               final String METHOD_NAME = "cleanup";
               entering(c, METHOD_NAME);
               cleanupEnv();
               exiting(c, METHOD_NAME);
           }
           else {
               logger.info("This machine is not Rhel");
           }
       }
       else {
           logger.info("OpenLiberty did not install successfully");
       }
    }

    /*@Test
    public void testJavaInstall() throws Exception {
        
        if (openLibExists) {
            String METHOD_NAME = "testJavaInstall";
            entering(c, METHOD_NAME);

            String[] param1s = { "install", "-y", "jre" }; //any java works
            ProgramOutput po = runCommand(METHOD_NAME, "sudo yum", param1s);

            if (po.getReturnCode() != 0){
                String[] paramAs = { "whatprovides", "java" };
                ProgramOutput poA = runCommand("checkAvailableJavaPackages", "sudo yum", paramAs);
            }

            assertEquals("Expected exit code", 0, po.getReturnCode()); //if already installed, exit is 0
            exiting(c, METHOD_NAME);
	}
        else {
            logger.info("OpenLiberty did not install successfully");
        }
    }*/

    @Test
    public void testVerifyRpmInstall() throws Exception {
        
        if (openLibExists) {
            String METHOD_NAME = "testVerifyRpmInstall";
            entering(c, METHOD_NAME);

            String[] param1s = { "-qi", "openliberty" };
            ProgramOutput po = runCommand(METHOD_NAME, "rpm", param1s);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            exiting(c, METHOD_NAME);
	}
        else {
            logger.info("OpenLiberty did not install successfully");
        }
    }

    /*@Test
    public void testServerStartStopRpm() throws Exception {
        
        if (openLibExists) {
            String METHOD_NAME = "testServerStartStopRpm";
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
        else {
            logger.info("OpenLiberty did not install successfully");
        }
    }*/

    @Test
    public void testUninstallRpm() throws Exception {
        
        if (openLibExists) {   
            String METHOD_NAME = "testUninstallRpm";
            entering(c, METHOD_NAME);

            String[] param1s = { "remove", "-y", "openliberty" };
            ProgramOutput po = runCommand(METHOD_NAME, "sudo yum", param1s);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            exiting(c, METHOD_NAME);
	 }
        else {
            logger.info("OpenLiberty did not install successfully");
        }
    }
}
