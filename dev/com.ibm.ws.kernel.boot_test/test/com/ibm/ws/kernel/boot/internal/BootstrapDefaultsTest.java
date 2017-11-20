/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.ws.kernel.boot.SharedBootstrapConfig;

import test.common.SharedOutputManager;
import test.shared.TestUtils;

/**
 *
 */
public class BootstrapDefaultsTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    @Rule
    public TestRule outputRule = outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        TestUtils.cleanTempFiles();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        TestUtils.cleanTempFiles();
    }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testGetDefaults() throws Exception {
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        final File installDir = new File(testClassesDir, "test data/default-files/default");

        // Trick the bootstrap config into pointing to our copy of the defaults file
        SharedBootstrapConfig config = SharedBootstrapConfig.createSharedConfig(outputMgr);
        config.setInstallRoot(installDir);

        BootstrapDefaults defaults = new BootstrapDefaults(config);
        assertEquals("Kernel definition should be set to the name of the default", "kernelCore-1.0", defaults.getKernelDefinition(config));
        assertEquals("Log provider definition should be set to the name of the default", "defaultLogging-1.0", defaults.getLogProviderDefinition(config));
    }

    @Test
    public void testGetDefaultsOverride() throws Exception {
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        final File installDir = new File(testClassesDir, "test data/default-files/extension");

        // Trick the bootstrap config into pointing to our copy of the defaults file
        SharedBootstrapConfig config = SharedBootstrapConfig.createSharedConfig(outputMgr);

        // Trick the bootstrap config into thinking our os name is 'aaa'.
        Map<String, String> initProps = new HashMap<String, String>();
        initProps.put("os.name", "aaa");
        config.setInitProps(initProps);
        config.setInstallRoot(installDir);

        // Try to read the bootstrap defaults from our own internal directory.
        BootstrapDefaults defaults = new BootstrapDefaults(config);
        assertEquals("Kernel definition should be set to the test value", "kernelCoreTestA-1.0", defaults.getKernelDefinition(config));
        assertEquals("Log provider definition should be set to the name of the default", "defaultLoggingTest-1.0", defaults.getLogProviderDefinition(config));
        assertEquals("Kernel extension definition should be set to the test value", "aaaExtensions-1.0", defaults.getOSExtensionDefinition(config));
    }

    @Test
    public void testMissingKernelDefinition() throws Exception {
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        final File installDir = new File(testClassesDir, "test data/default-files/missing");

        // Trick the bootstrap config into pointing to our copy of the defaults file
        SharedBootstrapConfig config = SharedBootstrapConfig.createSharedConfig(outputMgr);
        config.setInstallRoot(installDir);

        // Try to read the bootstrap defaults from our own internal directory.
        BootstrapDefaults defaults = new BootstrapDefaults(config);
        assertNull("No value should have been found for kernel version", defaults.getKernelDefinition(config)); // this will throw
        assertNull("No value should have been found for log provider", defaults.getLogProviderDefinition(config));
    }

    @Test
    public void testMissingKernelDefinitionOverrideProperty() throws Exception {
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        final File installDir = new File(testClassesDir, "test data/default-files/missing");

        // Trick the bootstrap config into pointing to our copy of the defaults file
        SharedBootstrapConfig config = SharedBootstrapConfig.createSharedConfig(outputMgr);
        config.setInstallRoot(installDir);

        // Set the kernel etc using properties
        String kernelDef = "kernel_1.x";
        String operatingSystemExtensionsDef = "extension_1.x";
        String logProviderDef = "logging_1.x";

        Map<String, String> initProps = new HashMap<String, String>();
        initProps.put(BootstrapDefaults.BOOTPROP_KERNEL, kernelDef);
        initProps.put(BootstrapDefaults.BOOTPROP_OS_EXTENSIONS, operatingSystemExtensionsDef);
        initProps.put(BootstrapDefaults.BOOTPROP_LOG_PROVIDER, logProviderDef);
        config.setInitProps(initProps);

        // Try to read the bootstrap defaults from our own internal directory.
        BootstrapDefaults defaults = new BootstrapDefaults(config);

        assertEquals("Property was set: Kernel definition should equal provided property value",
                     kernelDef, defaults.getKernelDefinition(config));
        assertEquals("Property was set: OS extensions definition should equal provided property value",
                     operatingSystemExtensionsDef, defaults.getOSExtensionDefinition(config));
        assertEquals("Property was set: Log provider definition should equal provided property value",
                     logProviderDef, defaults.getLogProviderDefinition(config));
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
            assertEquals(names.get(osName), BootstrapDefaults.getNormalizedOperatingSystemName(osName));
        }
    }
}
