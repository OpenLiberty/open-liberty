/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.suite;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DDNonValidTests extends CommonTests {

    public static final String[] ALLOWED_ERRORS =
        new String[] { "CWWKC2276E", "CWWKC2277E" };

    @BeforeClass
    public static void setUp() throws Exception {
        CommonTests.commonSetUp(
            DDNonValidTests.class,
            "server_app.xml",
            CommonTests.setUpTestApp);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        CommonTests.commonTearDown(
            DDNonValidTests.class,
            CommonTests.tearDownTestApp,
            ALLOWED_ERRORS);
    }

    //

    public static final CommonTests.ErrorTest WEB_BND_EXTRA =
        new CommonTests.ErrorTest(
            "<web-bnd moduleName=\"ServletTest\">",
            "<web-bnd moduleName=\"ServletTest_Extra\">",
            "_EWB",
            "No error for extra web-bnd",
            "CWWKC2277E.*");

    @Test
    public void testExtraWebBndModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, WEB_BND_EXTRA);
    }

    public static final CommonTests.ErrorTest WEB_BND_UNNAMED =
        new CommonTests.ErrorTest(
            "<web-bnd moduleName=\"ServletTest\">",
            "<web-bnd>",
            "_UWB",
            "No error for unnamed web-bnd",
            "CWWKC2276E.*web-bnd.*");

    @Test
    public void testUnnamedWebBndModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, WEB_BND_UNNAMED);
    }

    public static final CommonTests.ErrorTest WEB_EXT_EXTRA =
        new CommonTests.ErrorTest(
            "<web-ext moduleName=\"ServletTest\"",
            "<web-ext moduleName=\"ServletTest_Extra\"",
            "_EWX",
            "No error for extra web-ext",
            "CWWKC2277E.*");

    @Test
    public void testExtraWebExtModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, WEB_EXT_EXTRA);
    }

    public static final CommonTests.ErrorTest WEB_EXT_UNNAMED =
        new CommonTests.ErrorTest(
            "<web-ext moduleName=\"ServletTest\"",
            "<web-ext",
            "_UWX",
            "No error for unnamed web-ext",
            "CWWKC2276E.*web-ext.*");

    @Test
    public void testUnnamedWebExtModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, WEB_EXT_UNNAMED);
    }

    //

    public static final CommonTests.ErrorTest EJB_BND_EXTRA =
        new CommonTests.ErrorTest(
            "<ejb-jar-bnd moduleName=\"EJBTest\">",
            "<ejb-jar-bnd moduleName=\"EJBTest_Extra\">",
            "_EEB",
            "No error for extra ejb-bnd",
            "CWWKC2277E.*");

    @Test
    public void testExtraEJBBndModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, EJB_BND_EXTRA);
    }

    public static final CommonTests.ErrorTest EJB_BND_UNNAMED =
        new CommonTests.ErrorTest(
            "<ejb-jar-bnd moduleName=\"EJBTest\">",
            "<ejb-jar-bnd>",
            "_UEB",
            "No error for unnamed ejb-bnd",
            "CWWKC2276E.*ejb-jar-bnd.*");

    @Test
    public void testUnnamedEJBBndModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, EJB_EXT_UNNAMED);
    }

    public static final CommonTests.ErrorTest EJB_EXT_EXTRA =
        new CommonTests.ErrorTest(
            "<ejb-jar-ext moduleName=\"EJBTest\">",
            "<ejb-jar-ext moduleName=\"EJBTest_Extra\">",
            "_EEX",
            "No error for extra ejb-ext",
            "CWWKC2277E.*");

    @Test
    public void testExtraEJBExtModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, EJB_EXT_EXTRA);
    }

    public static final CommonTests.ErrorTest EJB_EXT_UNNAMED =
        new CommonTests.ErrorTest(
            "<ejb-jar-ext moduleName=\"EJBTest\">",
            "<ejb-jar-ext>",
            "_UEX",
            "No error for unnamed ejb-ext",
            "CWWKC2276E.*ejb-jar-ext.*");

    @Test
    public void testUnnamedEJBExtModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, EJB_EXT_UNNAMED);
    }
}
