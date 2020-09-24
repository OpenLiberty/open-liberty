/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests what can and cannot be loaded by the server's JVM classpath.
 */
public class ServerClasspathTest {

    private static final String SERVER_NAME = "com.ibm.ws.kernel.boot.classpath.fat";

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);

    private static final String[] EXPECTED_PACKAGES = { "com.ibm.ws.kernel", "java.", "javax.", "sun.",
                                                        "org.osgi.framework", "com.ibm.crypto", "com.ibm.security",
                                                        "com.ibm.misc", "com.ibm.xml", "com.ibm.nio", "com.ibm.jvm",
                                                        "org.apache.xerces", "com.ibm.Compiler", "com.ibm.oti",
                                                        "org.omg.CORBA", "com.sun", "org.xml.sax", "com.ibm.jit",
                                                        "com.ibm.jsse2", "com.ibm.lang.management", "com.ibm.tools.attach",
                                                        "com.ibm.virtualization.management", "com.ibm.wsspi.kernel",
                                                        "com.ibm.java.lang.management.internal",
                                                        "org.ietf.jgss", "jdk", // Java 9
                                                        "com.ibm.sharedclasses.spi", // Open JDK 9
                                                        "openj9"
    };

    @BeforeClass
    public static void before() throws Exception {
        JavaArchive archive = ShrinkHelper.buildJavaArchive("checkJvmAppClasspath", "com.ibm.ws.kernel.boot.app.classpath");
        ShrinkHelper.exportAppToServer(server, archive, DeployOptions.DISABLE_VALIDATION);
        server.startServer();
    }

    @AfterClass
    public static void after() throws Exception {
        server.stopServer();
    }

    @Test
    public void testJvmAppClasspath() throws Exception {
        //TODO: check logs for any packages that are not in the expected packages list
        StringBuilder unexpectedPackages = new StringBuilder();
        List<String> pkgsOnCP = server.findStringsInLogs("AppLoader can load: .*", server.getConsoleLogFile());
        Iterator<String> iter = pkgsOnCP.iterator();
        boolean allowed;
        while (iter.hasNext()) {
            allowed = false;
            String pkg = iter.next().substring("AppLoader can load: ".length());
            for (String allowedPkg : EXPECTED_PACKAGES) {
                if (pkg.startsWith(allowedPkg)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                unexpectedPackages.append(" " + pkg);
            }
        }
        assertEquals("Found unexpected packages in the server JVM's application classpath", "", unexpectedPackages.toString());
    }
}
