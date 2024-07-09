/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.feature.tests;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ibm.ws.feature.tests.util.RepositoryUtil;
import com.ibm.ws.kernel.feature.internal.util.VerifyData;
import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyCase;
import com.ibm.ws.kernel.feature.internal.util.VerifyDelta;

/**
 * Baseline public singleton resolution.
 */
@RunWith(Parameterized.class)
public class BaselineVersionlessSingletonUnitTest extends BaselineResolutionUnitTest {
    /** Control parameter: Used to disable this unit test. */
    public static final boolean IS_ENABLED = BaselineResolutionEnablement.enableVersionlessSingleton;

    // Not currently used:
    //
    // BeforeClass is invoked after data() is invoked.
    //
    // But 'data' requires the locations and repository,
    // which were being setup in 'setupClass'.
    //
    // The setup steps have been moved to 'data()', and
    // have been set run at most once.

    @BeforeClass
    public static void setupClass() throws Exception {
        // doSetupClass(getServerName());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        doTearDownClass(BaselineVersionlessSingletonUnitTest.class);
    }

    public static final String DATA_FILE_PATH_OL = "publish/verify/singleton_expected.xml";
    public static final String DATA_FILE_PATH_WL = "publish/verify/singleton_expected_WL.xml";

    public static File getDataFile_OL() {
        return new File(DATA_FILE_PATH_OL);
    }

    public static File getDataFile_WL() {
        return new File(DATA_FILE_PATH_WL);
    }

    // To use change the name of parameterized tests, you say:
    //
    // @Parameters(name="namestring")
    //
    // namestring is a string, which can have the following special placeholders:
    //
    //   {index} - the index of this set of arguments. The default namestring is {index}.
    //   {0} - the first parameter value from this invocation of the test.
    //   {1} - the second parameter value
    //   and so on
    // @Parameterized.Parameters(name = "{0}")

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        doSetupClass(); // 'data()' is invoked before the '@BeforeClass' method.

        if (!IS_ENABLED) {
            return nullCases("versionless singleton baseline");
        }

        VerifyData verifyData = readData(getDataFile_OL());

        // WAS liberty adds and modifies the Open liberty cases.
        if (RepositoryUtil.isWASLiberty()) {
            int initialCount = verifyData.getCases().size();
            VerifyData verifyData_WL = readData(getDataFile_WL());
            verifyData = verifyData.add(verifyData_WL);
            int finalCount = verifyData.getCases().size();
            System.out.println("Case adjustment [ " + (finalCount - initialCount) + " ]");
        }

        return asCases(convertToVersionless(verifyData));
    }

    public BaselineVersionlessSingletonUnitTest(String name, VerifyCase testCase) throws Exception {
        super(name, testCase);
    }

    @Before
    public void setupTest() throws Exception {
        doSetupResolver();
    }

    @After
    public void tearDownTest() throws Exception {
        doClearResolver();
    }

    @Override
    public List<String> detectFeatureErrors(List<String> rootFeatures) {
        return detectVersionlessSingletonErrors(rootFeatures);
    }

    @Test
    public void baseline_versionlessSingletonTest() throws Exception {
        if (!IS_ENABLED) {
            nullResult();
            return;
        }
        doTestResolve();
    }

    //@formatter:off

    protected static final String[][] ALLOWED_SUBSTITUTIONS = {
        // Simple removal

        { "com.ibm.websphere.appserver.jcaInboundSecurity-1.0",
          "io.openliberty.versionless.jcaInboundSecurity", "javaee-6.0",
          "com.ibm.websphere.appserver.eeCompatible-7.0", null },

        { "io.openliberty.appAuthentication-3.1",
          "io.openliberty.versionless.appAuthentication", "jakartaee-11.0",
          "io.openliberty.internal.versionless.appAuthentication-3.1", null },

        { "io.openliberty.appAuthorization-3.0",
          "io.openliberty.versionless.appAuthorization", "jakartaee-11.0",
          "io.openliberty.internal.versionless.appAuthorization-3.0", null },

        // eeCompatible version change

        { "com.ibm.websphere.appserver.javaMail-1.5",
          "io.openliberty.versionless.javaMail", "javaee-7.0",
          "com.ibm.websphere.appserver.eeCompatible-6.0", "com.ibm.websphere.appserver.eeCompatible-7.0" },

        { "com.ibm.websphere.appserver.jdbc-4.1",
          "io.openliberty.versionless.jdbc", "javaee-7.0",
          "com.ibm.websphere.appserver.eeCompatible-6.0", "com.ibm.websphere.appserver.eeCompatible-7.0" },

        { "com.ibm.websphere.appserver.jdbc-4.2",
          "io.openliberty.versionless.jdbc", "javaee-8.0",
          "com.ibm.websphere.appserver.eeCompatible-6.0", "com.ibm.websphere.appserver.eeCompatible-8.0" },

        { "com.ibm.websphere.appserver.servlet-3.1",
          "io.openliberty.versionless.servlet", "javaee-7.0",
          "com.ibm.websphere.appserver.eeCompatible-6.0", "com.ibm.websphere.appserver.eeCompatible-7.0" },

        { "com.ibm.websphere.appserver.wasJmsClient-2.0",
          "io.openliberty.versionless.wasJmsClient", "javaee-7.0",
          "com.ibm.websphere.appserver.eeCompatible-6.0", "com.ibm.websphere.appserver.eeCompatible-7.0" },

        { "com.ibm.websphere.appserver.websocket-1.1",
          "io.openliberty.versionless.websocket", "javaee-7.0",
          "com.ibm.websphere.appserver.eeCompatible-6.0", "com.ibm.websphere.appserver.eeCompatible-7.0" },

        // feature rename across versions

        { "io.openliberty.appAuthentication-2.0",
          "io.openliberty.versionless.appAuthentication", "jakartaee-9.1",
          "io.openliberty.internal.versionless.appAuthentication-2.0", "io.openliberty.internal.versionless.jaspic-2.0" },
        { "io.openliberty.appAuthentication-3.0",
          "io.openliberty.versionless.appAuthentication", "jakartaee-10.0",
          "io.openliberty.internal.versionless.appAuthentication-3.0", "io.openliberty.internal.versionless.jaspic-3.0" },

        { "io.openliberty.appAuthorization-2.0",
          "io.openliberty.versionless.appAuthorization", "jakartaee-9.1",
          "io.openliberty.internal.versionless.appAuthorization-2.0", "io.openliberty.internal.versionless.jacc-2.0" },
        { "io.openliberty.appAuthorization-2.1",
           "io.openliberty.versionless.appAuthorization", "jakartaee-10.0",
           "io.openliberty.internal.versionless.appAuthorization-2.1", "io.openliberty.internal.versionless.jacc-2.1" },

        { "io.openliberty.connectors-2.0",
          "io.openliberty.versionless.connectors", "jakartaee-9.1",
          "io.openliberty.internal.versionless.connectors-2.0", "io.openliberty.internal.versionless.jca-2.0" },
        { "io.openliberty.connectors-2.1",
          "io.openliberty.versionless.connectors", "jakartaee-10.0",
          "io.openliberty.internal.versionless.connectors-2.1", "io.openliberty.internal.versionless.jca-2.1" },

        { "io.openliberty.connectorsInboundSecurity-2.0",
          "io.openliberty.versionless.connectorsInboundSecurity", "jakartaee-9.1",
          "io.openliberty.internal.versionless.connectorsInboundSecurity-2.0", "io.openliberty.internal.versionless.jcaInboundSecurity-2.0" },

        { "io.openliberty.enterpriseBeans-4.0",
          "io.openliberty.versionless.enterpriseBeans", "jakartaee-9.1",
          "io.openliberty.internal.versionless.enterpriseBeans-4.0", "io.openliberty.internal.versionless.ejb-4.0" },
        { "io.openliberty.enterpriseBeansHome-4.0",
          "io.openliberty.versionless.enterpriseBeansHome", "jakartaee-9.1",
          "io.openliberty.internal.versionless.enterpriseBeansHome-4.0", "io.openliberty.internal.versionless.ejbHome-4.0" },
        { "io.openliberty.enterpriseBeansLite-4.0",
          "io.openliberty.versionless.enterpriseBeansLite", "jakartaee-9.1",
          "io.openliberty.internal.versionless.enterpriseBeansLite-4.0", "io.openliberty.internal.versionless.ejbLite-4.0" },
        { "io.openliberty.enterpriseBeansRemote-4.0",
          "io.io.openliberty.versionless.enterpriseBeansRemote", "jakartaee-9.1",
          "openliberty.internal.versionless.enterpriseBeansRemote-4.0", "io.openliberty.internal.versionless.ejbRemote-4.0" },

        { "io.openliberty.expressionLanguage-4.0",
          "io.openliberty.versionless.expressionLanguage", "jakartaee-9.1",
          "io.openliberty.internal.versionless.expressionLanguage-4.0", "io.openliberty.internal.versionless.el-4.0" },
        { "io.openliberty.expressionLanguage-5.0",
          "io.openliberty.versionless.expressionLanguage", "jakartaee-10.0",
          "io.openliberty.internal.versionless.expressionLanguage-5.0", "io.openliberty.internal.versionless.el-5.0" },
        { "io.openliberty.expressionLanguage-6.0",
          "io.openliberty.versionless.expressionLanguage", "jakartaee-11.0",
          "io.openliberty.internal.versionless.expressionLanguage-6.0", "io.openliberty.internal.versionless.el-6.0" },

        { "io.openliberty.faces-3.0",
          "io.openliberty.versionless.faces", "jakartaee-9.1",
          "io.openliberty.internal.versionless.faces-3.0", "io.openliberty.internal.versionless.jsf-3.0" },
        { "io.openliberty.faces-4.0",
          "io.openliberty.versionless.faces", "jakartaee-10.0",
          "io.openliberty.internal.versionless.faces-4.0", "io.openliberty.internal.versionless.jsf-4.0" },
        { "io.openliberty.faces-4.1",
          "io.openliberty.versionless.faces", "jakartaee-11.0",
          "io.openliberty.internal.versionless.faces-4.1", "io.openliberty.internal.versionless.jsf-4.1" },

        { "io.openliberty.mail-2.0",
          "io.openliberty.versionless.mail", "jakartaee-9.1",
          "io.openliberty.internal.versionless.mail-2.0", "io.openliberty.internal.versionless.javaMail-2.0" },
        { "io.openliberty.mail-2.1",
          "io.openliberty.versionless.mail", "jakartaee-10.0",
          "io.openliberty.internal.versionless.mail-2.1", "io.openliberty.internal.versionless.javaMail-2.1" },

        { "io.openliberty.messaging-3.0",
          "io.openliberty.versionless.messaging", "jakartaee-9.1",
          "io.openliberty.internal.versionless.messaging-3.0", "io.openliberty.internal.versionless.jms-3.0" },
        { "io.openliberty.messaging-3.1",
          "io.openliberty.versionless.messaging", "jakartaee-10.0",
          "io.openliberty.internal.versionless.messaging-3.1", "io.openliberty.internal.versionless.jms-3.1" },
        { "io.openliberty.messagingClient-3.0",
          "io.openliberty.versionless.messagingClient", "jakartaee-9.1",
          "io.openliberty.internal.versionless.messagingClient-3.0", "io.openliberty.internal.versionless.wasJmsClient-3.0" },
        { "io.openliberty.messagingSecurity-3.0",
          "io.openliberty.versionless.messagingSecurity", "jakartaee-9.1",
          "io.openliberty.internal.versionless.messagingSecurity-3.0", "io.openliberty.internal.versionless.wasJmsSecurity-3.0" },
        { "io.openliberty.messagingServer-3.0",
          "io.openliberty.versionless.messagingServer", "jakartaee-9.1",
          "io.openliberty.internal.versionless.messagingServer-3.0", "io.openliberty.internal.versionless.wasJmsServer-3.0" },

        { "io.openliberty.pages-3.0",
          "io.openliberty.versionless.pages", "jakartaee-9.1",
          "io.openliberty.internal.versionless.pages-3.0", "io.openliberty.internal.versionless.jsp-3.0" },
        { "io.openliberty.pages-3.1",
          "io.openliberty.versionless.pages", "jakartaee-10.0",
          "io.openliberty.internal.versionless.pages-3.1", "io.openliberty.internal.versionless.jsp-3.1" },
        { "io.openliberty.pages-4.0",
          "io.openliberty.versionless.pages", "jakartaee-11.0",
          "io.openliberty.internal.versionless.pages-4.0", "io.openliberty.internal.versionless.jsp-4.0" },

        { "io.openliberty.persistence-3.0",
          "io.openliberty.versionless.persistence", "jakartaee-9.1",
          "io.openliberty.internal.versionless.persistence-3.0", "io.openliberty.internal.versionless.jpa-3.0" },
        { "io.openliberty.persistence-3.1",
          "io.openliberty.versionless.persistence", "jakartaee-10.0",
          "io.openliberty.internal.versionless.persistence-3.1", "io.openliberty.internal.versionless.jpa-3.1" },

        { "io.openliberty.restfulWS-3.0",
          "io.openliberty.versionless.restfulWS", "jakartaee-9.1",
          "io.openliberty.internal.versionless.restfulWS-3.0", "io.openliberty.internal.versionless.jaxrs-3.0" },
        { "io.openliberty.restfulWS-3.1",
          "io.openliberty.versionless.restfulWS", "jakartaee-10.0",
          "io.openliberty.internal.versionless.restfulWS-3.1", "io.openliberty.internal.versionless.jaxrs-3.1" },
        { "io.openliberty.restfulWS-4.0",
          "io.openliberty.versionless.restfulWS", "jakartaee-11.0",
          "io.openliberty.internal.versionless.restfulWS-4.0", "io.openliberty.internal.versionless.jaxrs-4.0" },
        { "io.openliberty.restfulWSClient-3.0",
          "io.openliberty.versionless.restfulWSClient", "jakartaee-9.1",
          "io.openliberty.internal.versionless.restfulWSClient-3.0", "io.openliberty.internal.versionless.jaxrsClient-3.0" },
        { "io.openliberty.restfulWSClient-3.1",
          "io.openliberty.versionless.restfulWSClient", "jakartaee-10.0",
          "io.openliberty.internal.versionless.restfulWSClient-3.1", "io.openliberty.internal.versionless.jaxrsClient-3.1" },
        { "io.openliberty.restfulWSClient-4.0",
          "io.openliberty.versionless.restfulWSClient", "jakartaee-11.0",
          "io.openliberty.internal.versionless.restfulWSClient-4.0", "io.openliberty.internal.versionless.jaxrsClient-4.0" },

        { "io.openliberty.xmlBinding-3.0",
          "io.openliberty.versionless.xmlBinding", "jakartaee-9.1",
          "io.openliberty.internal.versionless.xmlBinding-3.0", "io.openliberty.internal.versionless.jaxb-3.0" },
        { "io.openliberty.xmlBinding-4.0",
          "io.openliberty.versionless.xmlBinding", "jakartaee-10.0",
          "io.openliberty.internal.versionless.xmlBinding-4.0", "io.openliberty.internal.versionless.jaxb-4.0" },

        { "io.openliberty.xmlWS-3.0",
          "io.openliberty.versionless.xmlWS", "jakartaee-9.1",
          "io.openliberty.internal.versionless.xmlWS-3.0", "io.openliberty.internal.versionless.jaxws-3.0" },
        { "io.openliberty.xmlWS-4.0",
          "io.openliberty.versionless.xmlWS", "jakartaee-10.0",
          "io.openliberty.internal.versionless.xmlWS-4.0", "io.openliberty.internal.versionless.jaxws-4.0" }
    };

    protected static final Map<String, String[]> allowedSubstitutions;

    static {
        Map<String, String[]> useSubstitutions = new HashMap<>(ALLOWED_SUBSTITUTIONS.length);
        for ( String[] substitution : ALLOWED_SUBSTITUTIONS ) {
            VerifyDelta.putSubstitution(substitution, useSubstitutions);
        }
        allowedSubstitutions = useSubstitutions;
    }

    @Override
    public Map<String, String[]> getAllowedSubstitutions() {
        return allowedSubstitutions;
    }

    @Override
    public String[] getAllowedSubstitution(VerifyCase testCase) {
        String vFeature = testCase.input.roots.get(0); // Must be versionless; must be a singleton
        String platform = testCase.input.platforms.get(0); // Must be a singleton.

        return VerifyDelta.getSubstitution(vFeature, platform, getAllowedSubstitutions());
    }

    //@formatter:on
}
