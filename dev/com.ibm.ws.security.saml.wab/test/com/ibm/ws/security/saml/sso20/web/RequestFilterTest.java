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
package com.ibm.ws.security.saml.sso20.web;

import static org.junit.Assert.fail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.saml.Constants.EndpointType;
import com.ibm.ws.security.saml.Constants.SamlSsoVersion;
import com.ibm.ws.security.saml.SsoRequest;

import junit.framework.Assert;
import test.common.SharedOutputManager;

/**
 * Unit test the {@link com.ibm.ws.security.saml.sso20.web.RequestFilter} class.
 */
public class RequestFilterTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    public static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final States STATE = mockery.states("test-execution");

    //Mock objects
    private static final HttpServletRequest HTTPSERVLETREQUEST_MCK = mockery.mock(HttpServletRequest.class);
    private static final HttpServletResponse HTTPSERVLETRESPONSE_MCK = mockery.mock(HttpServletResponse.class);
    private static final FilterChain FILTERCHAIN_MCK = mockery.mock(FilterChain.class);

    //Constants
    private static final String REGEX = "^/(\\w*)/(acs|samlmetadata)$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private static final String ACS_URL = "/34324d_sdsfsd/acs";
    private static final Matcher ACS_MATCHER = PATTERN.matcher(ACS_URL);
    private static final EndpointType EXPECTED_ACS_ENDPOINT_TYPE_FROM_URL = EndpointType.ACS;

    private static final String SAMLMETADATA_URL = "/34324d_sdsfsd/samlmetadata";
    private static final Matcher SAMLMETADATA_MATCHER = PATTERN.matcher(SAMLMETADATA_URL);
    private static final EndpointType EXPECTED_SAMLMETADATA_ENDPOINT_TYPE_FROM_URL = EndpointType.SAMLMETADATA;

    private static final String REQUEST_URL = "/34324d_sdsfsd/REQUEST";
    private static final Matcher REQUEST_MATCHER = Pattern.compile("^/(\\w*)/(REQUEST)$").matcher(REQUEST_URL);
    private static final EndpointType EXPECTED_REQUEST_ENDPOINT_TYPE_FROM_URL = EndpointType.REQUEST;

    private static final String EXPECTED_PROVIDER_NAME_FROM_URL = "34324d_sdsfsd";

    private static final SamlSsoVersion EXPECTED_SAML_VERSION = SamlSsoVersion.SAMLSSO20;

    private static final String SAML20_INVALID_ENDPOINT_TYPE_URL = "";

    private static RequestFilter requestFilter;

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
        requestFilter = new RequestFilter();

        //Initializing matchers
        ACS_MATCHER.matches();
        SAMLMETADATA_MATCHER.matches();
        REQUEST_MATCHER.matches();
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");

        mockery.assertIsSatisfied();
    }

    @Test
    public void doFilterShouldDoNothingIfResponseIsCommited() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(HTTPSERVLETREQUEST_MCK).getPathInfo();
                    will(returnValue(ACS_URL));

                    atMost(2).of(HTTPSERVLETRESPONSE_MCK).isCommitted();
                    will(returnValue(true));
                }
            });

            requestFilter.doFilter(HTTPSERVLETREQUEST_MCK, HTTPSERVLETRESPONSE_MCK, FILTERCHAIN_MCK);
        } catch (Exception e) {
            fail("Unexpected exception was thrown: " + e);
        }
    }

    @Test
    public void doFilterShouldSendResponseErrorIfNoMatcherIsRetrievedFromAnEnpointRequestOfServletRequest() {
        STATE.become("doFilterShouldDoNothingIfResponseIsCommited");
        try {
            mockery.checking(new Expectations() {
                {
                    atLeast(2).of(HTTPSERVLETREQUEST_MCK).getPathInfo();
                    will(returnValue(SAML20_INVALID_ENDPOINT_TYPE_URL));
                    when(STATE.is("doFilterShouldDoNothingIfResponseIsCommited"));

                    one(HTTPSERVLETRESPONSE_MCK).sendError(with(any(Integer.class)), with(any(String.class)));
                }
            });

            requestFilter.doFilter(HTTPSERVLETREQUEST_MCK, HTTPSERVLETRESPONSE_MCK, FILTERCHAIN_MCK);
        } catch (Exception e) {
            fail("Unexpected exception was thrown: " + e);
        }
        STATE.become("test-execution");
    }

    @Test
    public void doFilterShouldSetEndpointRequestIfRequestIsNotCommited() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(HTTPSERVLETREQUEST_MCK).getPathInfo();
                    will(returnValue(ACS_URL));

                    atMost(2).of(HTTPSERVLETRESPONSE_MCK).isCommitted();
                    will(returnValue(false));

                    one(HTTPSERVLETREQUEST_MCK).setAttribute(with(any(String.class)), with(any(SsoRequest.class)));
                    one(FILTERCHAIN_MCK).doFilter(with(any(HttpServletRequest.class)), with(any(HttpServletResponse.class)));
                }
            });

            requestFilter.doFilter(HTTPSERVLETREQUEST_MCK, HTTPSERVLETRESPONSE_MCK, FILTERCHAIN_MCK);
        } catch (Exception e) {
            fail("Unexpected exception was thrown: " + e);
        }
    }

    @Test
    public void getEndpointTypeFromUrlShouldReturnAKnownEndpointTypeAccordingToItsMatcher() {
        Assert.assertEquals(EXPECTED_ACS_ENDPOINT_TYPE_FROM_URL, requestFilter.getEndpointTypeFromUrl(ACS_MATCHER));
        Assert.assertEquals(EXPECTED_SAMLMETADATA_ENDPOINT_TYPE_FROM_URL, requestFilter.getEndpointTypeFromUrl(SAMLMETADATA_MATCHER));
        Assert.assertEquals(EXPECTED_REQUEST_ENDPOINT_TYPE_FROM_URL, requestFilter.getEndpointTypeFromUrl(REQUEST_MATCHER));
    }

    @Test
    public void getProviderNameFromUrlTestShoulReturnAProviderNameAccordingToItsMatcher() {
        Assert.assertEquals(EXPECTED_PROVIDER_NAME_FROM_URL, requestFilter.getProviderNameFromUrl(ACS_MATCHER));
    }

    @Test
    public void getSamlVersionShouldReturnSAMlSSO20() {
        Assert.assertEquals(EXPECTED_SAML_VERSION, requestFilter.getSamlVersion());
    }
}
