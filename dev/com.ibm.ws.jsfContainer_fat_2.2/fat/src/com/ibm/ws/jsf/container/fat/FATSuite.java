/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.jsf.container.fat.tests.CDIFlowsTests;
import com.ibm.ws.jsf.container.fat.tests.ClassloadingTest;
import com.ibm.ws.jsf.container.fat.tests.ErrorPathsTest;
import com.ibm.ws.jsf.container.fat.tests.JSF22BeanValidationTests;
import com.ibm.ws.jsf.container.fat.tests.JSF22CDIGeneralTests;
import com.ibm.ws.jsf.container.fat.tests.JSF22FlowsTests;
import com.ibm.ws.jsf.container.fat.tests.JSF22StatelessViewTests;
import com.ibm.ws.jsf.container.fat.tests.JSFContainerTest;

@RunWith(Suite.class)
@SuiteClasses({
                JSFContainerTest.class,
                JSF22FlowsTests.class,
                CDIFlowsTests.class,
                JSF22StatelessViewTests.class,
                JSF22BeanValidationTests.class,
                JSF22CDIGeneralTests.class,
                ErrorPathsTest.class,
                ClassloadingTest.class
})

public class FATSuite {

    public static final String MOJARRA_API = "publish/files/mojarra/jsf-api-2.2.14.jar";
    public static final String MOJARRA_IMPL = "publish/files/mojarra/jsf-impl-2.2.14.jar";

    public static WebArchive addMojarra(WebArchive app) throws Exception {
        return app.addAsLibraries(new File("publish/files/mojarra/").listFiles());
    }

    public static WebArchive addMyFaces(WebArchive app) throws Exception {
        return app.addAsLibraries(new File("publish/files/myfaces/").listFiles());
    }

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
