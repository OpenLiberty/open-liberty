/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.validator.fat;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.topology.utils.HttpUtils;

@RunWith(Suite.class)
@SuiteClasses({
                ValidateDataSourceTest.class,
                ValidateJCATest.class,
                ValidateJMSTest.class,
                ValidateDSCustomLoginModuleTest.class,
                ValidateOpenApiSchemaTest.class
})

public class FATSuite {

    @BeforeClass
    public static void setup() throws Exception {
        HttpUtils.trustAllCertificates();
        HttpUtils.trustAllHostnames();
        HttpUtils.setDefaultAuth("adminuser", "adminpwd");
    }
}