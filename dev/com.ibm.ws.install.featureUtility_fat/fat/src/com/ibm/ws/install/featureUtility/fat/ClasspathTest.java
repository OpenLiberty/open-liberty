/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.ws.install.featureUtility.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.install.InstallException;

import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

public class ClasspathTest extends FeatureUtilityToolTest {

	private static final Class<?> c = ClasspathTest.class;
    private static final String SERVLET_5_0_CLASS_NAME = "com.ibm.wsspi.servlet.session.IBMSessionExt";
    private static final String EJBLITE_3_2_CLASS_NAME = "javax.ejb.spi.EJBContainerProvider";


	@BeforeClass
	public static void beforeClassSetup() throws Exception {
		String methodName = "beforeClassSetup";
        /* Enable tests only if running on a zOS machine, otherwise skip class */
		Assume.assumeTrue(!isZos);
		Log.entering(c, methodName);
		deleteFeaturesAndLafilesFolders("beforeClassSetup");
		replaceWlpProperties("24.0.0.8");
		
		copyFileToMinifiedRoot("etc",
			    "publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");
		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", mavenLocalRepo2);
		    ProgramOutput po = runFeatureUtility(methodName, new String[] {"installFeature", "servlet-5.0", "ejbLite-3.2", "--verify=skip"});
		assertTrue(po.getStdout().contains("All features were successfully installed"));
		Log.exiting(c, methodName);
	}

	@Before
	public void beforeSetUp() throws Exception {
		String methodName = "beforeSetUp"; 
	}

	@After
	public void afterCleanUp() throws Exception {		
		deleteUsrExtFolder("afterCleanUp");
		deleteEtcFolder("afterCleanUp");
	}

	@AfterClass
	public static void cleanUp() throws Exception {
		deleteFeaturesAndLafilesFolders("afterCleanUp");
		if (!isZos) {
			resetOriginalWlpProps();
		}
	}

	 private File getClasspathOutputJar() {
	        return new File(minifiedRoot + "/classpath.jar");
	    }

	    private void assertLoadClass(ClassLoader cl, String className) {
	        try {
	            cl.loadClass(className);
	        } catch (ClassNotFoundException e) {
	            Assert.fail("Expected to loadClass " + className);
	        }
	    }

	    private void assertNotLoadClass(ClassLoader cl, String className) {
	        try {
	            Assert.assertNull("unexpected successful loadClass " + className, cl.loadClass(className));
	        } catch (ClassNotFoundException e) {
	            Log.info(c, "assertNotLoadClass", "caught expected " + e);
	        }
	    }

	    private Manifest dumpManifest(File file) throws Exception {
	        try (JarFile jar = new JarFile(file)) {
	            Manifest manifest = jar.getManifest();
	            Assert.assertNotNull("expected manifest in " + file, manifest);

	            ByteArrayOutputStream out = new ByteArrayOutputStream();
	            manifest.write(out);

	            String manifestContents = out.toString();
	            Log.info(c, "dumpManifest", "META-INF/MANIFEST.MF contents of " + file);
	            Log.info(c, "dumpManifest", manifestContents);
	            return manifest;
	        }
	    }

	    @Test
	    @Mode(TestMode.LITE)
	    public void testClasspathNoArgs() throws Exception {
	        String METHOD_NAME = "testFindAll";
	        Log.entering(c, METHOD_NAME);
	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "classpath" });
	        assertTrue("Should throw missing arguments error",
	                   po.getStdout().indexOf("CWWKF1001E:") >= 0);
	        Log.exiting(c, METHOD_NAME);
	    }

	    @Test
	    @Mode(TestMode.LITE)
	    public void testClasspathNoFeatures() throws Exception {
	        String METHOD_NAME = "testClasspathNoFeatures";
	        Log.entering(c, METHOD_NAME);
	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "classpath", getClasspathOutputJar().getAbsolutePath() });
	        assertTrue("Should throw missing options error",
	                   po.getStdout().indexOf("CWWKF1025E:") >= 0);
	        Log.exiting(c, METHOD_NAME);

	    }

	    @Test
	    @Mode(TestMode.LITE)
	    public void testClasspathEmptyFeatures() throws Exception {
	        String METHOD_NAME = "testClasspathEmptyFeatures";
	        Log.entering(c, METHOD_NAME);
	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "classpath", "--features=", getClasspathOutputJar().getAbsolutePath() });
	        assertTrue("Should throw missing options error",
	                   po.getStdout().indexOf("CWWKF1025E:") >= 0);
	        Log.exiting(c, METHOD_NAME);
	    }

	    @Test
	    @Mode(TestMode.LITE)
	    public void testClasspathInvalidFeature() throws Exception {
	        String METHOD_NAME = "testClasspathInvalidFeature";
	        Log.entering(c, METHOD_NAME);
	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "classpath", "--features=invalidFeatureName", getClasspathOutputJar().getAbsolutePath() });
	        assertTrue("Should throw feature not found error",
	                   po.getStdout().indexOf("CWWKF1026E:") >= 0);
	        Log.exiting(c, METHOD_NAME);

	    }

	    @Test
	    @Mode(TestMode.LITE)
	    public void testClasspathProtectedFeature() throws Exception {
	        String METHOD_NAME = "testClasspathProtectedFeature";
	        Log.entering(c, METHOD_NAME);
	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "classpath", "--features=com.ibm.websphere.appserver.classloading-1.0", getClasspathOutputJar().getAbsolutePath() });
	        assertTrue("Should throw feature not public error",
	                   po.getStdout().indexOf("CWWKF1027E:") >= 0);
	        Log.exiting(c, METHOD_NAME);
	    }

	    /**
	     * Only applies to Windows machine
	     *
	     * @throws Exception
	     */
	    @Test
	    @Mode(TestMode.LITE)
	    public void testClasspathInvalidDrive() throws Exception {
	        Assume.assumeTrue(File.separatorChar == '\\');
	        String METHOD_NAME = "testClasspathProtectedFeature";
	        Log.entering(c, METHOD_NAME);

	        String outputJarPath = getClasspathOutputJar().getAbsolutePath();
	        boolean installOnCDrive = outputJarPath.substring(0, 1).equalsIgnoreCase("c");
	        File badOutputJar = new File((installOnCDrive ? "D" : "C") + outputJarPath.substring(1));

	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "classpath", "--features=servlet-5.0", badOutputJar.getAbsolutePath() });
	        assertTrue("Should throw wrong drive error",
	                   po.getStdout().indexOf("CWWKF1028E:") >= 0);
	        Log.exiting(c, METHOD_NAME);
	    }

	    @Test
	    @Mode(TestMode.LITE)
	    public void testClasspathServlet50() throws Exception {
	        String METHOD_NAME = "testClasspathServlet50";
	        File output = getClasspathOutputJar();
	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "classpath", "--features=servlet-5.0", output.getAbsolutePath() });
	        dumpManifest(output);

	        try (URLClassLoader cl = new URLClassLoader(new URL[] { output.toURI().toURL() }, null)) {
	            assertEquals("Expected exit code", 0, po.getReturnCode());
	            assertLoadClass(cl, SERVLET_5_0_CLASS_NAME);
	            assertNotLoadClass(cl, EJBLITE_3_2_CLASS_NAME);
	        }
	        Assert.assertTrue(output.delete());
	    }


	    @Test
	    @Mode(TestMode.LITE)
	    public void testClasspathEJBLite32() throws Exception {
	        String METHOD_NAME = "testClasspathEJBLite32";
	        File output = getClasspathOutputJar();

	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "classpath", "--features=ejbLite-3.2", output.getAbsolutePath() });
	        dumpManifest(output);

	        try (URLClassLoader cl = new URLClassLoader(new URL[] { output.toURI().toURL() }, null)) {
	            assertEquals("Expected exit code", 0, po.getReturnCode());
	            assertNotLoadClass(cl, SERVLET_5_0_CLASS_NAME);
	            assertLoadClass(cl, EJBLITE_3_2_CLASS_NAME);
	        }
	        Assert.assertTrue(output.delete());

	    }

	    /*
	     * servlet-5.0 and ejbLite-3.2 have feature conflict. Need to find two features without conflict.
	     */
	    @Test
	    @Ignore
	    @Mode(TestMode.LITE)
	    public void testClasspathServlet50AndEJBLite32() throws Exception {
	        String METHOD_NAME = "testClasspathServlet50AndEJBLite32";
	        File output = getClasspathOutputJar();
	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "classpath", "--features=servlet-5.0,ejbLite-3.2", output.getAbsolutePath() });
	        dumpManifest(output);

	        try (URLClassLoader cl = new URLClassLoader(new URL[] { output.toURI().toURL() }, null)) {
	            assertEquals("Expected exit code", 0, po.getReturnCode());
	            assertLoadClass(cl, SERVLET_5_0_CLASS_NAME);
	            assertLoadClass(cl, EJBLITE_3_2_CLASS_NAME);
	        }
	        Assert.assertTrue(output.delete());
	    }
	    
	    /**
	     * Tests that the installUtility featureList action help displays the --productExtension option.
	     *
	     * @throws Exception
	     */
	    @Test
	    public void testClasspatFeaturesHelpDisplay() throws Exception {
	        String METHOD_NAME = "testClasspatFeaturesHelpDisplay";
	        ProgramOutput po = runFeatureUtility(METHOD_NAME, new String[] { "help", "classpath" });

	        String stdout = po.getStdout();
	        int returnCode = po.getReturnCode();
	        Log.info(c, METHOD_NAME, "Return Code: " + returnCode);
	        assertTrue("The output should contain the option --features. ", stdout.contains("--features"));

	        Log.exiting(c, METHOD_NAME);
	    }

}
