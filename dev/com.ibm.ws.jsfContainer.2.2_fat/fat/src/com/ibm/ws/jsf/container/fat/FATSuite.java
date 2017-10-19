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
package com.ibm.ws.jsf.container.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                JSFContainerTest.class,
                JSF22FlowsTests.class,
                CDIFlowsTests.class,
                JSF22StatelessViewTests.class,
                JSF22BeanValidationTests.class,
                ErrorPathsTest.class,
                ConfigTest.class
})

public class FATSuite {

    public static final String MOJARRA_API = "publish/files/mojarra/jsf-api-2.2.14.jar";
    public static final String MOJARRA_IMPL = "publish/files/mojarra/jsf-impl-2.2.14.jar";

    public static WebArchive addMojarra(WebArchive app) throws Exception {
        app = app.addAsLibrary(new File(MOJARRA_API))
                        .addAsLibrary(new File(MOJARRA_IMPL));

        return app;
    }

    public static WebArchive addMyFaces(WebArchive app) throws Exception {
        app = app.addAsLibrary(new File("publish/files/myfaces/myfaces-api-2.2.12.jar"))
                        .addAsLibrary(new File("publish/files/myfaces/myfaces-impl-2.2.12.jar"))
                        .addAsLibrary(new File("publish/files/myfaces/commons-digester-1.8.jar"))
                        .addAsLibrary(new File("publish/files/myfaces/commons-collections-3.2.1.jar"))
                        .addAsLibrary(new File("publish/files/myfaces/commons-logging-1.1.3.jar"))
                        .addAsLibrary(new File("publish/files/myfaces/commons-beanutils-1.8.3.jar"));

        return app;
    }

}
