/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.fat.suite;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 *
 */
public class SharedShrinkWrapApps {

    private static WebArchive cdiConfig_war = null;

    public static JavaArchive getTestAppUtilsJar() {
        final String LIB_NAME = "testAppUtils";
        JavaArchive testAppUtils = ShrinkWrap.create(JavaArchive.class, LIB_NAME + ".jar")
                        .addPackage("com.ibm.ws.microprofile.appConfig.test.utils");
        return testAppUtils;
    }

    public static WebArchive cdiConfigServerApps() {
        final String APP_NAME = "cdiConfig";

        if (cdiConfig_war != null) {
            return (cdiConfig_war);
        }

        cdiConfig_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource"),
                                               "services/org.eclipse.microprofile.config.spi.ConfigSource")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/services/org.eclipse.microprofile.config.spi.Converter"),
                                               "services/org.eclipse.microprofile.config.spi.Converter")
                        .addAsLibrary(cdiConfigJar());

        return (cdiConfig_war);
    }

    public static JavaArchive cdiConfigJar() {
        return ShrinkWrap.create(JavaArchive.class, "cdiConfig.jar")
                        .addAsManifestResource(new File("test-applications/cdiConfig.jar/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsManifestResource(new File("test-applications/cdiConfig.jar/resources/META-INF/microprofile-config.properties"), "microprofile-config.properties")
                        .addPackages(true, "com.ibm.ws.microprofile.appConfig.cdi.beans")
                        .addPackages(true, "com.ibm.ws.microprofile.appConfig.cdi.test")
                        .addPackages(true, "com.ibm.ws.microprofile.appConfig.cdi.web");
    }

}
