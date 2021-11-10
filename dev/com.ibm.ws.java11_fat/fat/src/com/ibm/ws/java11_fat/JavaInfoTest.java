/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.java11_fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.kernel.service.util.JavaInfo;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;

/**
 * This is really just a unit test, but we will include it in a FAT instead
 * because FATs get run on a lot of different java levels and unit tests don't
 */
@RunWith(FATRunner.class)
public class JavaInfoTest extends FATServletClient {

    private static final Class<?> c = JavaInfoTest.class;

    private static componenttest.topology.impl.JavaInfo fatJavaInfo = componenttest.topology.impl.JavaInfo.forCurrentVM();

    @BeforeClass
    public static void setup() throws Exception {
        // Log some useful info in case the test ever fails
        log(fatJavaInfo.toString());
        log("Properties dump:");
        for (Entry<Object, Object> prop : System.getProperties().entrySet())
            if (prop.getKey().toString().startsWith("java."))
                log("  " + prop.getKey() + '=' + prop.getValue());

        log("JavaInfo dump:");
        log("  vendor=" + JavaInfo.vendor());
        log("  major=" + JavaInfo.majorVersion());
        log("  minor=" + JavaInfo.minorVersion());
        log("  micro=" + JavaInfo.microVersion());
        log("  sr=" + JavaInfo.serviceRelease());
        log("  fp=" + JavaInfo.fixPack());
    }

    private static void log(String msg) {
        Log.info(c, "setup", msg);
    }

    @Test
    public void testJavaVendor() {
        assertTrue("Found an unknown java vendor java.vendor=" + System.getProperty("java.vendor"),
                   JavaInfo.Vendor.UNKNOWN != JavaInfo.vendor());

        // Verify that the Kernel's copy of JavaInfo.vendor() is consistent with the FAT framework's
        switch (JavaInfo.vendor()) {
            case IBM:
                assertEquals(componenttest.topology.impl.JavaInfo.Vendor.IBM,
                             fatJavaInfo.vendor());
                break;
            case OPENJ9:
                assertEquals(componenttest.topology.impl.JavaInfo.Vendor.OPENJ9,
                             fatJavaInfo.vendor());
                break;
            case ORACLE:
                assertEquals(componenttest.topology.impl.JavaInfo.Vendor.SUN_ORACLE,
                             fatJavaInfo.vendor());
                break;
            default:
                fail("Got unknown java vendor: " + JavaInfo.vendor());
        }
    }

    @Test
    public void testMajorVersion() {
        int major = JavaInfo.majorVersion();
        assertTrue("Java major version was not within a known range (7-19): " + major,
                   major >= 7 && major < 20);

        assertEquals(major, fatJavaInfo.majorVersion());
    }

    @Test
    public void verifyValidMinor() {
        int minor = JavaInfo.minorVersion();
        assertTrue("Java minor version was not >=0" + minor, minor >= 0);

        assertEquals(minor, fatJavaInfo.minorVersion());
    }

    @Test
    public void verifyValidMicro() {
        int micro = JavaInfo.microVersion();
        assertTrue("Java micro version was not >=0" + micro, micro >= 0);

        assertEquals(micro, fatJavaInfo.microVersion());
    }

    @Test
    public void verifyValidFixPack() {
        int fp = JavaInfo.fixPack();
        assertTrue("Java fixpack version was not >=0" + fp, fp >= 0);

        assertEquals(fp, fatJavaInfo.fixpack());
    }

    @Test
    public void verifyValidServiceRelease() {
        int sr = JavaInfo.serviceRelease();
        assertTrue("Java service release version was not >=0" + sr, sr >= 0);

        assertEquals(sr, fatJavaInfo.serviceRelease());
    }

    /**
     * This test validates that classes are available using the JavaInfo.isSystemClassAvailable
     * method. To validate this function, the test uses a Class.forName. Java classes
     * should be found by isAvailable. Application classes should not be found.
     */
    @Test
    public void verifyIsAvailable() {
        String[] classesToTest = { "com.ibm.security.auth.module.Krb5LoginModule",
                                   "com.ibm.lang.management.OperatingSystemMXBean",
                                   "com.sun.security.auth.module.Krb5LoginModule",
                                   "org.apache.xalan.processor.TransformerFactoryImpl",
                                   "com.ibm.xtq.xslt.jaxp.compiler.TransformerFactoryImpl",
                                   "org.xml.sax.ext.LexicalHandler"
        };

        for (String className : classesToTest) {
            assertEquals(className, isAvailable(className), JavaInfo.isSystemClassAvailable(className));
        }
    }

    private static boolean isAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            //No FFDC needed
            return false;
        }
    }
}
