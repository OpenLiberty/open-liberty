/**
 *
 */
package com.ibm.ws.microprofile.faulttolerance.tck;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.microprofile.tck.Utils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This is a test class that runs the Whole Fault Tolerance TCK as one FAT test.
 * There is a detailed output on specific tests stored in: For a local build:
 * /libertyGit/open-liberty/dev/com.ibm.ws.microprofile.faulttolerance_fat_tck/build/libs/autoFVT/results/mvnOutput_TCK
 * /libertyGit/open-liberty/dev/com.ibm.ws.microprofile.faulttolerance_fat_tck/build/libs/autoFVT/results/tck/surefire-reports/index.html
 * 
 * Copy
 * /libertyGit/open-liberty/dev/com.ibm.ws.microprofile.faulttolerance_fat_tck/build/libs/autoFVT/results/tck/surefire-reports/junitreports
 * into
 * /libertyGit/open-liberty/dev/com.ibm.ws.microprofile.faulttolerance_fat_tck/build/libs/autoFVT/results/junit
 * 
 */
@RunWith(FATRunner.class)
public class FaultToleranceTckPackageTest {

	@Server("FATServer")
	public static LibertyServer server;

	@BeforeClass
	public static void setUp() throws Exception {
		server.startServer();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWWKZ0002E");
	}

	@Test
	@AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
	public void testTck() throws Exception {
		if (!Utils.init) {
			Utils.init(server);
		}
		// Everything under autoFVT/results is collected from the child build machine
		File mvnOutput = new File(Utils.home, "results/mvnOutput_TCK");
		int rc = Utils.runCmd(Utils.mvnCliTckRoot, Utils.tckRunnerDir, mvnOutput);
		// mvn returns 0 is all surefire tests and pass 1 on failure

		File src = new File(Utils.home, "results/tck/surefire-reports/junitreports");
		File tgt = new File(Utils.home, "results/junit");
		Files.walkFileTree(src.toPath(), new Utils.CopyFileVisitor(src.toPath(), tgt.toPath()));
		//		File[] listOfFiles = src.listFiles();
//		for (int i = 0; i < listOfFiles.length; i++) {
//			if (listOfFiles[i].isFile()) {
//				Utils.log("File " + listOfFiles[i].getAbsolutePath());
//				Files.copy(Paths.get(listOfFiles[i].getAbsolutePath()), tgt.toPath() , StandardCopyOption.REPLACE_EXISTING);
//			} else if (listOfFiles[i].isDirectory()) {
//				Utils.log("Directory " + listOfFiles[i].getName());
//			}
//		}
//		Utils.log("Copying from " + src.getAbsolutePath() + " to " + tgt.getAbsolutePath());
//		try {
//			Files.copy(src.toPath(), tgt.toPath(), StandardCopyOption.REPLACE_EXISTING);
//		} catch (Throwable t) {
//			Utils.log("Copy failed" + t.getMessage());
//		}

		Assert.assertTrue(
				"com.ibm.ws.microprofile.faulttolerance_fat_tck:org.eclipse.microprofile.faulttolerance.tck.FaultToleranceTckPackageTest:testTck:TCK has returned non-zero return code of: "
						+ rc + " This indicates test failure, see: ...autoFVT/results/mvn* "
						+ "and ...autoFVT/results/tck/surefire-reports/index.html",
				rc == 0);
	}

}
