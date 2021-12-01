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
    
    public static final String[] ALLOWED_ERRORS = new String[] {
            "CWWKC2276E", // module name not specified
            "CWWKC2277E", // module name not found
            "CWWKC2278E"  // module name duplicated
    };

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
    
    // These text fragments are targets for substitution:

    public static final String APP_BND =
        "<application-bnd>";
    public static final String APP_BND_DUPE =
        "<application-bnd/>" + "\n" +
        "<application-bnd>";

    public static final String APP_EXT =
        "<application-ext";
    public static final String APP_EXT_DUPE =
        "<application-ext/>" + "\n" +
        "<application-ext";

    public static final String WEB_BND =
        "<web-bnd moduleName=\"ServletTest\">";
    public static final String WEB_BND_UNFOUND =
        "<web-bnd moduleName=\"ServletTest_NotFound\">";
    public static final String WEB_BND_UNNAMED =
        "<web-bnd>";
    public static final String WEB_BND_DUPE =
        "<web-bnd moduleName=\"ServletTest\"/>" + "\n" +
        "<web-bnd moduleName=\"ServletTest\">";
    
    public static final String WEB_EXT =
        "<web-ext moduleName=\"ServletTest\"";
    public static final String WEB_EXT_UNFOUND =
        "<web-ext moduleName=\"ServletTest_NotFound\"";
    public static final String WEB_EXT_UNNAMED =
        "<web-ext";
    public static final String WEB_EXT_DUPE =
        "<web-ext moduleName=\"ServletTest\"/>" + "\n" +
        "<web-ext moduleName=\"ServletTest\"";

    public static final String EJB_BND =
        "<ejb-jar-bnd moduleName=\"EJBTest\">";
    public static final String EJB_BND_UNFOUND =
        "<ejb-jar-bnd moduleName=\"EJBTest_NotFound\">";
    public static final String EJB_BND_UNNAMED =
        "<ejb-jar-bnd>";
    public static final String EJB_BND_DUPE =
        "<ejb-jar-bnd moduleName=\"EJBTest\"/>" + "\n" +
        "<ejb-jar-bnd moduleName=\"EJBTest\">";

    public static final String EJB_EXT =
        "<ejb-jar-ext moduleName=\"EJBTest\">";
    public static final String EJB_EXT_UNFOUND =
        "<ejb-jar-ext moduleName=\"EJBTest_NotFound\">";
    public static final String EJB_EXT_UNNAMED =
        "<ejb-jar-ext>";
    public static final String EJB_EXT_DUPE =
        "<ejb-jar-ext moduleName=\"EJBTest\"/>" + "\n" +
        "<ejb-jar-ext moduleName=\"EJBTest\">";

    public static final String WS_BND =
        "<webservices-bnd moduleName=\"ServletTest\">";
    public static final String WS_BND_UNFOUND =
        "<webservices-bnd moduleName=\"ServletTest_NotFound\">";
    public static final String WS_BND_UNNAMED =
        "<webservices-bnd>";
    public static final String WS_BND_DUPE =
        "<webservices-bnd moduleName=\"ServletTest\"/>" + "\n" +
        "<webservices-bnd moduleName=\"ServletTest\">";

    // These text fragments are not used as targets for substitution:

    // "<web-bnd moduleName=\"ServletTestNoBnd\">"
    // "<web-ext moduleName=\"ServletTestNoBnd\""
    // "<ejb-jar-bnd moduleName=\"EJBTestNoBnd\">"
    // "<ejb-jar-ext moduleName=\"EJBTestNoBnd\">"
    // <webservices-bnd moduleName="ServletTestNoBnd">
    
    public static final CommonTests.ErrorTest APP_BND_DUPE_TEST =
        new CommonTests.ErrorTest(
            APP_BND, APP_BND_DUPE,
            "_DAB",
            "Duplicate application-bnd test",
            "CWWKC2278E.*application-bnd.*");

    // @Test // Duplicate application-bnd elements are not currently detected.
    public void testDupeAppBnd() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, APP_BND_DUPE_TEST);
    }
    
    public static final CommonTests.ErrorTest APP_EXT_DUPE_TEST =
        new CommonTests.ErrorTest(
            APP_EXT, APP_EXT_DUPE,
            "_DAE",
            "Duplicate application-ext test",
            "CWWKC2278E.*application-ext.*");

    // @Test // Duplicate application-ext elements are not currently detected.
    public void testDupeAppExt() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, APP_EXT_DUPE_TEST);
    }

    //

    // E CWWKC2277E: Cannot find the [ServletTest_NotFound] 'moduleName' attributes specified in the 'web-bnd' bindings and extension configuration elements of the Test.ear application.  The application module names are [ServletTestNoBnd, EJBTest, EJBTestNoBnd, ServletTest].

    public static final CommonTests.ErrorTest WEB_BND_UNFOUND_TEST =
        new CommonTests.ErrorTest(
            WEB_BND, WEB_BND_UNFOUND,
            "_EWB",
            "Unnamed web-bnd test",
            "CWWKC2277E.*web-bnd.*");

    // @Test // Unmatched module names are not currently detected.
    public void testUnfoundWebBndModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, WEB_BND_UNFOUND_TEST);
    }

    // E CWWKC2276E: The 'moduleName' attribute is missing from one or more 'web-bnd' bindings and extension configuration elements of the Test.ear application.

    public static final CommonTests.ErrorTest WEB_BND_UNNAMED_TEST =
        new CommonTests.ErrorTest(
            WEB_BND, WEB_BND_UNNAMED,
            "_UWB",
            "Unnamed web-bnd test",
            "CWWKC2276E.*web-bnd.*");

    @Test
    public void testUnnamedWebBndModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, WEB_BND_UNNAMED_TEST);
    }

    // E CWWKC2278E: Duplicate [ServletTest] 'moduleName' attributes are specified in the 'web-bnd' bindings and extension configuration elements of the Test.ear application.
    
    public static final CommonTests.ErrorTest WEB_BND_DUPE_TEST =
        new CommonTests.ErrorTest(
            WEB_BND, WEB_BND_DUPE,
            "_DWB",
            "Duplicated web-bnd test",
            "CWWKC2278E.*web-bnd.*");

    @Test
    public void testDupeWebBndModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, WEB_BND_DUPE_TEST);
    }

    public static final CommonTests.ErrorTest WEB_EXT_UNFOUND_TEST =
        new CommonTests.ErrorTest(
            WEB_EXT, WEB_EXT_UNFOUND,
            "_EWX",
            "Unfound web-ext test",
            "CWWKC2277E.*web-ext.*");

    // E CWWKC2277E: Cannot find the [ServletTest_NotFound] 'moduleName' attributes specified in the 'web-ext' bindings and extension configuration elements of the Test.ear application.  The application module names are [ServletTestNoBnd, EJBTest, EJBTestNoBnd, ServletTest].
    
    // @Test // Unmatched module names are not currently detected.
    public void testUnfoundWebExtModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, WEB_EXT_UNFOUND_TEST);
    }

    public static final CommonTests.ErrorTest WEB_EXT_UNNAMED_TEST =
        new CommonTests.ErrorTest(
            WEB_EXT, WEB_EXT_UNNAMED,
            "_UWX",
            "Unnamed web-ext test",
            "CWWKC2276E.*web-ext.*");

    // E CWWKC2276E: The 'moduleName' attribute is missing from one or more 'web-ext' bindings and extension configuration elements of the Test.ear application.

    @Test
    public void testUnnamedWebExtModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, WEB_EXT_UNNAMED_TEST);
    }

    public static final CommonTests.ErrorTest WEB_EXT_DUPE_TEST =
        new CommonTests.ErrorTest(
            WEB_EXT, WEB_EXT_DUPE,
            "_DWX",
            "Duplicated web-ext test",
            "CWWKC2278E.*web-ext.*");

    // E CWWKC2278E: Duplicate [ServletTest] 'moduleName' attributes are specified in the 'web-ext' bindings and extension configuration elements of the Test.ear application.

    @Test
    public void testDupeWebExtModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, WEB_EXT_DUPE_TEST);
    }
    
    //

    public static final CommonTests.ErrorTest EJB_BND_UNFOUND_TEST =
        new CommonTests.ErrorTest(
            EJB_BND, EJB_BND_UNFOUND,
            "_EEB",
            "Unfound ejb-jar-bnd test",
            "CWWKC2277E.*ejb-jar-bnd.*");

    // E CWWKC2277E: Cannot find the [EJBTest_NotFound] 'moduleName' attributes specified in the 'ejb-jar-bnd' bindings and extension configuration elements of the Test.ear application.  The application module names are [ServletTestNoBnd, EJBTest, EJBTestNoBnd, ServletTest].

    // @Test // Unmatched module names are not currently detected.
    public void testUnfoundEJBBndModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, EJB_BND_UNFOUND_TEST);
    }

    public static final CommonTests.ErrorTest EJB_BND_UNNAMED_TEST =
        new CommonTests.ErrorTest(
            EJB_BND, EJB_BND_UNNAMED,
            "_UEB",
            "Unnamed ejb-jar-bnd test",
            "CWWKC2276E.*ejb-jar-bnd.*");

    // E CWWKC2276E: The 'moduleName' attribute is missing from one or more 'ejb-jar-bnd' bindings and extension configuration elements of the Test.ear application.

    @Test
    public void testUnnamedEJBBndModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, EJB_BND_UNNAMED_TEST);
    }

    public static final CommonTests.ErrorTest EJB_BND_DUPE_TEST =
        new CommonTests.ErrorTest(
            EJB_BND, EJB_BND_DUPE,
            "_DEB",
            "Duplicate ejb-jar-bnd test",
            "CWWKC2278E.*ejb-jar-bnd.*");

    // E CWWKC2278E: Duplicate [EJBTest] 'moduleName' attributes are specified in the 'ejb-jar-bnd' bindings and extension configuration elements of the Test.ear application.
    
    @Test
    public void testDupeEJBBndModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, EJB_BND_DUPE_TEST);
    }
    
    public static final CommonTests.ErrorTest EJB_EXT_UNFOUND_TEST =
        new CommonTests.ErrorTest(
            EJB_EXT, EJB_EXT_UNFOUND,
            "_EEX",
            "Unfound ejb-jar-ext test",
            "CWWKC2277E.*ejb-jar-ext.*");

    // E CWWKC2277E: Cannot find the [EJBTest_NotFound] 'moduleName' attributes specified in the 'ejb-jar-ext' bindings and extension configuration elements of the Test.ear application.  The application module names are [ServletTestNoBnd, EJBTest, EJBTestNoBnd, ServletTest].

    // @Test // Unmatched module names are not currently detected.
    public void testUnfoundEJBExtModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, EJB_EXT_UNFOUND_TEST);
    }

    public static final CommonTests.ErrorTest EJB_EXT_UNNAMED_TEST =
        new CommonTests.ErrorTest(
            EJB_EXT, EJB_EXT_UNNAMED,
            "_UEX",
            "Unnamed ejb-jar-ext test",
            "CWWKC2276E.*ejb-jar-ext.*");

    // E CWWKC2276E: The 'moduleName' attribute is missing from one or more 'ejb-jar-ext' bindings and extension configuration elements of the Test.ear application.

    @Test
    public void testUnnamedEJBExtModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, EJB_EXT_UNNAMED_TEST);
    }

    public static final CommonTests.ErrorTest EJB_EXT_DUPE_TEST =
        new CommonTests.ErrorTest(
            EJB_EXT, EJB_EXT_DUPE,
            "_DEX",
            "ejb-jar-ext duplication test",
            "CWWKC2278E.*ejb-jar-ext.*");

    // E CWWKC2278E: Duplicate [EJBTest] 'moduleName' attributes are specified in the 'ejb-jar-ext' bindings and extension configuration elements of the Test.ear application.
    
    @Test
    public void testDupeEJBExtModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, EJB_EXT_DUPE_TEST);
    }
    
    //

    public static final CommonTests.ErrorTest WS_BND_UNFOUND_TEST =
        new CommonTests.ErrorTest(
            WS_BND, WS_BND_UNFOUND,
            "_ESB",
            "Unfound webservices-bnd test",
            "CWWKC2277E.*webservices-bnd.*");

    // @Test // The Webservices processing is not being triggered. ??
    public void testUnfoundWSBndModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, WS_BND_UNFOUND_TEST);
    }

    public static final CommonTests.ErrorTest WS_BND_UNNAMED_TEST =
        new CommonTests.ErrorTest(
            WS_BND, WS_BND_UNNAMED,
            "_USB",
            "Unnamed webservices-bnd test",
            "CWWKC2276E.*webservices-bnd.*");

    // @Test // The Webservices processing is not being triggered. ??
    public void testUnnamedWSBndModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, WS_BND_UNNAMED_TEST);
    }

    public static final CommonTests.ErrorTest WS_BND_DUPE_TEST =
        new CommonTests.ErrorTest(
            WS_BND, WS_BND_DUPE,
            "_DSB",
            "Duplicate webservices-bnd test",
            "CWWKC2278E.*webservices-bnd.*");

    // @Test // The Webservices processing is not being triggered. ??
    public void testDupeWSBndModuleName() throws Exception {
        CommonTests.errorTest(DDNonValidTests.class, WS_BND_DUPE_TEST);
    }    
}
