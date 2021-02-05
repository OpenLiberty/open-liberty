/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal;

import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Hashtable;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;

import test.common.SharedOutputManager;

public class SAMLResponseTAICushionTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static final HttpServletResponse response = common.getServletResponse();
    private static final HttpServletRequest request = common.getServletRequest();
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final SsoRequest ssoRequest = common.getSsoRequest();
    private static final Saml20Token saml20Token = common.getSso20Token();

    private static final SAMLResponseTAI responseTAI = new mockSAMLResponseTAI();
    private static final Subject subject = new Subject();
    private static final long lReCushionTime = 2000; // 2 seconds
    private static final Hashtable<String, Object> customProperties = new Hashtable<String, Object>();

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");

        //Prepare the subject
        Set<Object> objects = subject.getPrivateCredentials();
        objects.add(saml20Token);
        objects.add(customProperties);

        mockery.checking(new Expectations() {
            {
                allowing(ssoRequest).getSsoConfig();
                will(returnValue(ssoConfig));

                allowing(ssoConfig).getReAuthnCushion(); //
                will(returnValue(lReCushionTime)); //
                allowing(ssoConfig).isReAuthnOnAssertionExpire(); // default value
                will(returnValue(true)); //

            }
        });
    }

    @AfterClass
    public static void tearDown() {
        mockery.assertIsSatisfied();
        outputMgr.trace("*=all=disabled");
    }

    @After
    public void after() {
        SAMLResponseTAI.setTheActivatedSsoSamlServiceRef(SAMLResponseTAI.respSsoSamlServiceRef);
    }

    @Test
    public void testValidateSubject_expired() {
        long lCurrentTime = (new Date()).getTime();
        final Date expiredDate = new Date(lCurrentTime + 1000);
        // will expired after 1 second. But reCushionTime is 2 seconds. It will be considered expired
        mockery.checking(new Expectations() {
            {
                one(saml20Token).getSamlExpires();
                will(returnValue(expiredDate)); // return an epired time from now
            }
        });

        // This is a dummy statement, since the unit test will not reach the statement to access the session expired time.
        // But this will make sure that the saml20Token timeExpired does expire
        customProperties.put(Constants.SP_COOKIE_AND_SESSION_NOT_ON_OR_AFTER, new Long(lCurrentTime + 1000 + lReCushionTime));
        // will expired after 1 second plus the recushiontime is 2 seconds. It will not expired

        Boolean result = responseTAI.validateSubject(subject, request, response, ssoRequest);
        assertTrue("Expected to receive a false value but it was not received.", !result);
    }

    @Test
    public void testValidateSubject_SessionExpired() {
        long lCurrentTime = (new Date()).getTime();
        final Date expiredDate = new Date(lCurrentTime + 1000 + lReCushionTime); // will expired after 1 second plus the reCushion time
        mockery.checking(new Expectations() {
            {
                one(saml20Token).getSamlExpires();
                will(returnValue(expiredDate)); // return an epired time from now
            }
        });

        customProperties.put(Constants.SP_COOKIE_AND_SESSION_NOT_ON_OR_AFTER, new Long(lCurrentTime + 1000));
        // will expired after 1 second. But the recushiontime is 2 seconds. It will considered as expired

        Boolean result = responseTAI.validateSubject(subject, request, response, ssoRequest);
        assertTrue("Expected to receive a false value but it was not received.", !result);
    }

    @Test
    public void testValidateSubject_NotExpired() {
        long lCurrentTime = (new Date()).getTime();
        final Date expiredDate = new Date(lCurrentTime + 1000 + lReCushionTime); // will expired after 1 second plus the reCushion time
        mockery.checking(new Expectations() {
            {
                one(saml20Token).getSamlExpires();
                will(returnValue(expiredDate)); // return an epired time from now
            }
        });

        customProperties.put(Constants.SP_COOKIE_AND_SESSION_NOT_ON_OR_AFTER, new Long(lCurrentTime + 1000 + lReCushionTime));
        // will expired after 1 second plus the recushiontime is 2 seconds. It will not expired

        Boolean result = responseTAI.validateSubject(subject, request, response, ssoRequest);
        assertTrue("Expected to receive a true value but it was not received.", result);
    }

    static class mockSAMLResponseTAI extends SAMLResponseTAI {
        public boolean bTestRemoveSpCookie = false;

        @Override
        void removeInvalidSpCookie(HttpServletRequest req, HttpServletResponse resp, SsoRequest samlRequest) {
            if (bTestRemoveSpCookie) {
                super.removeInvalidSpCookie(req, resp, samlRequest);
            }
        }

    }
}
