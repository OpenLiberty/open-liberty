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
package com.ibm.ws.kernel.boot.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.SharedBootstrapConfig;

import test.common.SharedOutputManager;
import test.shared.Constants;
import test.shared.TestUtils;

/**
 *
 */
public class BootstrapManifestTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    @Rule
    public TestRule outputRule = outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        TestUtils.cleanTempFiles();
        TestUtils.setKernelUtilsBootstrapJar(null); // make sure jar is clear
    }

    @AfterClass
    public static void tearDownAfterClass() {
        TestUtils.cleanTempFiles();
    }

    @After
    public void tearDown() throws Exception {
        // reset the bootstrap jar...
        TestUtils.setKernelUtilsBootstrapJar(null); // make sure jar is clear
    }

    @Test
    public void testGetDefaults() throws Exception {
        BootstrapConfig config = new BootstrapConfig();

        setBootstrapJar(0); // use a real built jar
        BootstrapManifest m = new BootstrapManifest();
        assertNotNull("Bundle version should be set for the kernel.boot jar/bundle", m.getBundleVersion());
        assertEquals("Kernel definition should be set to the name of the default", "kernelCore-1.0", m.getKernelDefinition(config));
        assertEquals("Log provider definition should be set to the name of the default", "defaultLogging-1.0", m.getLogProviderDefinition(config));
    }

    @Test
    public void testMissingKernelDefinition() throws Exception {
        BootstrapConfig config = new BootstrapConfig();

        setBootstrapJar(1); // use a jar with a missing kernel definition
        BootstrapManifest m = new BootstrapManifest();
        assertNull("No value should have been found for kernel version", m.getKernelDefinition(config)); // this will throw
        assertNull("No value should have been found for log provider", m.getLogProviderDefinition(config));
    }

    @Test
    public void testMissingAttributesWithProperties() throws Exception {
        SharedBootstrapConfig config = SharedBootstrapConfig.createSharedConfig(outputMgr);

        setBootstrapJar(1); // use a jar with a missing kernel definition and log provider

        String kernelDef = "kernel_1.x";
        String operatingSystemExtensionsDef = "extension_1.x";
        String logProviderDef = "logging_1.x";

        Map<String, String> initProps = new HashMap<String, String>();
        initProps.put(BootstrapManifest.BOOTPROP_KERNEL, kernelDef);
        initProps.put(BootstrapManifest.BOOTPROP_OS_EXTENSIONS, operatingSystemExtensionsDef);
        initProps.put(BootstrapManifest.BOOTPROP_LOG_PROVIDER, logProviderDef);
        config.setInitProps(initProps);

        BootstrapManifest m = new BootstrapManifest();
        assertEquals("Property was set: Kernel definition should equal provided property value",
                     kernelDef, m.getKernelDefinition(config));
        assertEquals("Property was set: OS extensions definition should equal provided property value",
                     operatingSystemExtensionsDef, m.getOSExtensionDefinition(config));
        assertEquals("Property was set: Log provider definition should equal provided property value",
                     logProviderDef, m.getLogProviderDefinition(config));
    }

    @Test(expected = com.ibm.ws.kernel.boot.LaunchException.class)
    public void testMissingSystemPackagesList() throws Exception {
        BootstrapConfig config = new BootstrapConfig();

        // WebSphere-SystemPackages in MANIFEST.MF, points to file not present in bundle/jar
        setBootstrapJar(3);
        BootstrapManifest m = new BootstrapManifest();
        m.prepSystemPackages(config);
    }

    @Test
    public void testSystemPackages() throws Exception {
        Map<String, String> initProps = new HashMap<String, String>();

        SharedBootstrapConfig config = SharedBootstrapConfig.createSharedConfig(outputMgr);
        config.setInitProps(initProps);

        setBootstrapJar(1); // use a jar with system packages
        BootstrapManifest m = new BootstrapManifest();
        m.prepSystemPackages(config);
        //the system packages are obtained from a java.version file name
        assertNull(initProps.get(BootstrapConstants.INITPROP_OSGI_EXTRA_PACKAGE));
        assertNotNull(initProps.get(BootstrapConstants.INITPROP_OSGI_SYSTEM_PACKAGES));
    }

    @Test
    public void testSystemPackagesExtraFromExportPackage() throws Exception {
        Map<String, String> initProps = new HashMap<String, String>();

        SharedBootstrapConfig config = SharedBootstrapConfig.createSharedConfig(outputMgr);
        config.setInitProps(initProps);

        // Export-Package in MANIFEST.MF
        setBootstrapJar(2);
        BootstrapManifest m = new BootstrapManifest();
        m.prepSystemPackages(config);
        assertNotNull(initProps.get(BootstrapConstants.INITPROP_OSGI_EXTRA_PACKAGE));
        //system-packages are not null because we included mocked up properties files
        assertNotNull(initProps.get(BootstrapConstants.INITPROP_OSGI_SYSTEM_PACKAGES));
    }

    @Test
    public void testSystemPackagesJavaVersion() throws Exception {
        Map<String, String> initProps = new HashMap<String, String>();

        SharedBootstrapConfig config = SharedBootstrapConfig.createSharedConfig(outputMgr);
        config.setInitProps(initProps);

        setBootstrapJar(5); // use a jar with fake system packages that have version numbers
        BootstrapManifest m = new BootstrapManifest();
        m.prepSystemPackages(config);
        String sysPkgs = initProps.get(BootstrapConstants.INITPROP_OSGI_SYSTEM_PACKAGES);

        String javaVersion = System.getProperty("java.version");
        // strip off the update modifier
        int index = javaVersion.indexOf('_');
        index = (index == -1) ? javaVersion.indexOf('-') : index;
        javaVersion = (index == -1) ? javaVersion : javaVersion.substring(0, index);
        //validate the system packages obtained match the running java.version file name
        assertTrue("The system packages being used do not match the running java.version: "
                   + javaVersion
                   + " . This is normal if you are running the test on a version of Java that we support for running the server, but do not fully support. If we are intending to fully support a new Java version then new files are required in /com.ibm.ws.kernel.boot/resources/OSGI-OPT/websphere/system-packages_*.properties for production and /com.ibm.ws.kernel.boot_test/resources/system-packages_*.properties for test.",
                   sysPkgs.contains(javaVersion));

        String versionsToCheck = null;
        if (javaVersion.equals("1.6.0")) {
            versionsToCheck = "1.6.0";
        } else if (javaVersion.equals("1.7.0")) {
            versionsToCheck = "1.7.0,1.6.0";
        } else if (javaVersion.equals("1.8.0")) {
            versionsToCheck = "1.8.0,1.7.0,1.6.0";
        } else if (javaVersion.equals("9")) {
            versionsToCheck = "9,1.8.0,1.7.0,1.6.0";
        } else {
            fail("The running java version: " + javaVersion + " is newer than we have properties files for, system-packages udpates are required");
        }

        //validate that merging works and we have the older versions too
        assertEquals("The system-packages_*.properties files were not merged for multiple java versions.", versionsToCheck,
                     sysPkgs);
    }

    @Test
    public void testSystemPackagesFileWrongProperty() throws Exception {
        Map<String, String> initProps = new HashMap<String, String>();

        SharedBootstrapConfig config = SharedBootstrapConfig.createSharedConfig(outputMgr);
        config.setInitProps(initProps);

        setBootstrapJar(4);
        BootstrapManifest m = new BootstrapManifest();
        m.prepSystemPackages(config);
        assertNull(initProps.get(BootstrapConstants.INITPROP_OSGI_EXTRA_PACKAGE));
        assertNull(initProps.get(BootstrapConstants.INITPROP_OSGI_SYSTEM_PACKAGES));
    }

    @Test
    public void testGetNormalizedOperatingSystemName() throws Exception {
        Map<String, String> names = new HashMap<String, String>();
        names.put("AIX", "aix");
        names.put("Digital Unix", "digitalunix");
        names.put("FreeBSD", "freebsd");
        names.put("HP UX", "hpux");
        names.put("Irix", "irix");
        names.put("Linux", "linux");
        names.put("Mac OS", "macos");
        names.put("Mac OS X", "macosx");
        names.put("MPE/iX", "mpeix");
        names.put("Netware 4.11", "netware411");
        names.put("OS/2", "os2");
        names.put("OS/390", "os390");
        names.put("Solaris", "solaris");
        names.put("Windows 2000", "windows2000");
        names.put("Windows 7", "windows7");
        names.put("Windows 8", "windows8");
        names.put("Windows 95", "windows95");
        names.put("Windows 98", "windows98");
        names.put("Windows NT", "windowsnt");
        names.put("Windows NT (unknown)", "windowsntunknown");
        names.put("Windows Server 2012", "windowsserver2012");
        names.put("Windows Vista", "windowsvista");
        names.put("Windows XP", "windowsxp");
        names.put("z/OS", "zos");

        for (String osName : names.keySet()) {
            assertEquals(names.get(osName), BootstrapManifest.getNormalizedOperatingSystemName(osName));
        }
    }

    protected static void setBootstrapJar(int jarTestCase) throws Exception {
        File root = new File(Constants.TEST_DIST_DIR);
        final String filterExpr;

        switch (jarTestCase) {
            default:
            case 0:
                filterExpr = "com.ibm.ws.kernel.boot.*\\.jar";
                root = new File(Constants.BOOTSTRAP_LIB_DIR);
                break;
            case 1:
                // Manifest does not contain default kernel definition / log provider
                filterExpr = "simple_1\\.0\\.jar";
                break;
            case 2: // Fake jar with manifest that does not contain
                // framework/kernel
                // definitions
                filterExpr = "simple_2\\.0\\.jar";
                break;
            case 3:
                // Manifest references a system package list that doesn't exist
                filterExpr = "simple_3\\.0\\.jar";
                break;
            case 4:
                // the system packages file exists, but contains
                // invalid properties
                filterExpr = "simple_4\\.0\\.jar";
                break;
            case 5:
                // the system packages file exists, but contains
                // invalid properties
                filterExpr = "simple_5\\.0\\.jar";
                break;
        }

        File fileList[] = root.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(filterExpr);
            }
        });

        if (fileList == null || fileList.length < 1)
            throw new RuntimeException("Unable to find " + filterExpr + " in " + root.getName());

        TestUtils.setKernelUtilsBootstrapJar(fileList[0]); // set for the test case
    }
}
