/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jsf.container.fat.tests.CDIFlowsTests;
import com.ibm.ws.jsf.container.fat.tests.ClassloadingTest;
import com.ibm.ws.jsf.container.fat.tests.ErrorPathsTest;
import com.ibm.ws.jsf.container.fat.tests.JSF22BeanValidationTests;
import com.ibm.ws.jsf.container.fat.tests.JSF22FlowsTests;
import com.ibm.ws.jsf.container.fat.tests.JSF22StatelessViewTests;
import com.ibm.ws.jsf.container.fat.tests.JSF23CDIGeneralTests;
import com.ibm.ws.jsf.container.fat.tests.JSF23WebSocketTests;
import com.ibm.ws.jsf.container.fat.tests.JSFContainerTest;

@RunWith(Suite.class)
@SuiteClasses({
                JSFContainerTest.class,
                JSF22FlowsTests.class,
                CDIFlowsTests.class,
                JSF22StatelessViewTests.class,
                JSF22BeanValidationTests.class,
                ErrorPathsTest.class,
                ClassloadingTest.class,
                JSF23CDIGeneralTests.class,
                JSF23WebSocketTests.class
})

public class FATSuite {
    public static final String MOJARRA_API_IMP = "publish/files/mojarra/javax.faces-2.3.3.jar";
    public static final String MYFACES_API = "publish/files/myfaces/myfaces-api-2.3.2.jar";
    public static final String MYFACES_IMP = "publish/files/myfaces/myfaces-impl-2.3.2.jar";

    public static WebArchive addMojarra(WebArchive app) throws Exception {
        return app.addAsLibraries(new File("publish/files/mojarra/").listFiles());
    }

    public static WebArchive addMyFaces(WebArchive app) throws Exception {
        return app.addAsLibraries(new File("publish/files/myfaces/").listFiles()).addAsLibraries(new File("publish/files/myfaces-libs/").listFiles());
    }

}
