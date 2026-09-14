/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config.fat.suite;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 *
 */
public class SharedShrinkWrapApps {

    public static JavaArchive getTestAppUtilsJar() {
        final String LIB_NAME = "testAppUtils";
        JavaArchive testAppUtils = ShrinkWrap.create(JavaArchive.class, LIB_NAME + ".jar")
                        .addPackage("com.ibm.ws.microprofile.appConfig.test.utils");
        return testAppUtils;
    }

}