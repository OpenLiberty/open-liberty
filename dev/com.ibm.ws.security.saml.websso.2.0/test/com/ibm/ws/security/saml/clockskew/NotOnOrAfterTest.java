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
package com.ibm.ws.security.saml.clockskew;

import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.SAML20_AUTHENTICATION_FAIL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.SubjectConfirmation;
import org.opensaml.saml2.core.SubjectConfirmationData;

import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.acs.AssertionValidator;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class NotOnOrAfterTest extends AssertionValidator {

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();
    private static final BasicMessageContext context = common.getBasicMessageContext();
    private static final Assertion assertion = common.getAssertion();
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final Subject subject = common.getSubject();
    private static final SubjectConfirmation subjectConfirmation = common.getSubjectConfirmation();
    private static final SubjectConfirmationData subjectConfirmationData = common.getSubjectConfirmationData();
    private static final ForwardRequestInfo requestInfo = common.getRequestInfo();
    private static final SsoSamlService ssoService = common.getSsoService();
    private static final HttpServletRequest request = common.getServletRequest();
    private static final NameID nameId = common.getNameId();
    private static final List<SubjectConfirmation> listSubjectConfirmation = mockery.mock(List.class, "listSubjectConfirmation");
    private static final Iterator<SubjectConfirmation> iterator = mockery.mock(Iterator.class, "iterator");

    private static NotOnOrAfterTest validator;
    private static DateTime notOnOrAfter;
    private static final DateTime systemTime = new DateTime(2015, 9, 30, 12, 0, 0, 0); // Date 2015/09/30 12:00:00:00
    private static final long systemTimeMilliseconds = systemTime.getMillis();
    private static final long FIVE_MIN = 300000l;
    private static final long FOUR_MIN = 240000l;
    private static final String SAML_REQUESTINFO_ID = "response to id";
    private static final String SERVER_PROVIDER_ID = "edu";
    private static final String SERVER_NAME = "mx-gdl";
    private static final int SERVER_PORT = 8010;
    private static final String SERVER_PROTOCOL = "http";
    private static final String RECIPIENT_URL = SERVER_PROTOCOL + "://" + SERVER_NAME + ":" + SERVER_PORT + "/ibm/saml20/" + SERVER_PROVIDER_ID + "/acs";

    static {
        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(0l));
                allowing(ssoConfig).getSpHostAndPort();
                will(returnValue(null));
            }
        });
    }

    public NotOnOrAfterTest() {
        super(context, assertion);
    }

    @BeforeClass
    public static void setUp() {
        mockery.checking(new Expectations() {
            {
                allowing(context).getSsoConfig();
                will(returnValue(ssoConfig));
                allowing(context).getCachedRequestInfo();
                will(returnValue(requestInfo));
                allowing(context).getExternalRelayState();
                will(returnValue(null));
                allowing(context).getHttpServletRequest();
                will(returnValue(request));
                allowing(context).getSsoService();
                will(returnValue(ssoService));
                allowing(context).setSubjectNameIdentifier(nameId);

                allowing(assertion).getSubject();
                will(returnValue(subject));

                allowing(subject).getSubjectConfirmations();
                will(returnValue(listSubjectConfirmation));

                allowing(listSubjectConfirmation).iterator();
                will(returnValue(iterator));

                allowing(subjectConfirmation).getMethod();
                will(returnValue(SubjectConfirmation.METHOD_BEARER));
                allowing(subjectConfirmation).getSubjectConfirmationData();
                will(returnValue(subjectConfirmationData));

                allowing(subjectConfirmationData).getNotBefore();
                will(returnValue(null));

                allowing(subjectConfirmationData).getInResponseTo();
                will(returnValue(SAML_REQUESTINFO_ID));

                allowing(requestInfo).getInResponseToId();
                will(returnValue(SAML_REQUESTINFO_ID));

                allowing(ssoService).getProviderId();
                will(returnValue(SERVER_PROVIDER_ID));

                allowing(request).getServerName();
                will(returnValue(SERVER_NAME));
                allowing(request).getServerPort();
                will(returnValue(SERVER_PORT));
                allowing(request).getScheme();
                will(returnValue(SERVER_PROTOCOL));
                allowing(request).isSecure();
                will(returnValue(true));

                allowing(subjectConfirmationData).getRecipient();
                will(returnValue(RECIPIENT_URL));

                allowing(subject).getNameID();
                will(returnValue(nameId));
            }
        });
    }

    @AfterClass
    public static void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @After
    public void after() {
        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(0l));
            }
        });
    }

    /**
     * Set the notOnOrAfter attribute to null.
     * Set the clockskew to zero.
     * Since the notOnOrAfter attribute is null, a SamlException is thrown.
     */
    @Test
    public void testNullNotOnOrAfter() {
        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(0l));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(subjectConfirmation));

                one(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(null));
            }
        });

        try {
            validator = new NotOnOrAfterTest();

            validator.verifySubject();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    /**
     * Set the notOnOrAfter attribute and the system time with the same value.
     * Set the clockskew to 5 minutes.
     * The notBefore value plus the clockskew is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeCurrentTime_ClockSkewSetTo5Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        notOnOrAfter = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FIVE_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(subjectConfirmation));

                atMost(2).of(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(notOnOrAfter));
            }
        });

        try {
            validator = new NotOnOrAfterTest();

            validator.verifySubject();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    /**
     * Set the notOnOrAfter attribute and the system time with the same value.
     * Set the clockskew to zero.
     * The notBefore value plus zero is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeCurrentTime_ClockSkewSetToZero() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        notOnOrAfter = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(0l));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(subjectConfirmation));

                atMost(2).of(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(notOnOrAfter));
            }
        });

        try {
            validator = new NotOnOrAfterTest();

            validator.verifySubject();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    /**
     * Set the notOnOrAfter attribute 1 second before the system time.
     * Set the clockskew to zero.
     * The notOnOrAfter value plus zero is one second before the system time, therefore a SamlException is thrown.
     */
    @Test
    public void testFakeCurrentTimeMinus1Sec_ClockSkewSetToZero() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        notOnOrAfter = new DateTime(systemTime.minus(1000));

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(0l));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(subjectConfirmation));

                atMost(3).of(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(notOnOrAfter));
            }
        });

        try {
            validator = new NotOnOrAfterTest();

            validator.verifySubject();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    /**
     * Set the notOnOrAfter attribute 4 minutes before the system time.
     * Set the clockskew to 5 minutes.
     * The notOnOrAfter value plus the clockskew is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeCurrentTimeMinus4Min_ClockSkewSetTo5Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        notOnOrAfter = new DateTime(systemTimeMilliseconds).minus(FOUR_MIN);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FIVE_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(subjectConfirmation));

                atMost(2).of(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(notOnOrAfter));
            }
        });

        try {
            validator = new NotOnOrAfterTest();

            validator.verifySubject();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    /**
     * Set the notOnOrAfter attribute 4 minutes after the system time.
     * Set the clockskew to 5 minutes.
     * The notOnOrAfter value plus the clockskew is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeCurrentTimePlus4Min_ClockSkewSetTo5Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        notOnOrAfter = new DateTime(systemTimeMilliseconds).plus(FOUR_MIN);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FIVE_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(subjectConfirmation));

                atMost(2).of(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(notOnOrAfter));
            }
        });

        try {
            validator = new NotOnOrAfterTest();

            validator.verifySubject();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    /**
     * Set the system time 4 minutes before the notOnOrAfter attribute.
     * Set the clockskew to 5 minutes.
     * The notOnOrAfter value plus the clockskew is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeSystemTimeMinus4Min_ClockSkewSetTo5Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTime.minus(FOUR_MIN).getMillis());
        notOnOrAfter = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FIVE_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(subjectConfirmation));

                atMost(2).of(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(notOnOrAfter));
            }
        });

        try {
            validator = new NotOnOrAfterTest();

            validator.verifySubject();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    /**
     * Set the system time 4 minutes after the notOnOrAfter attribute.
     * Set the clockskew to 5 minutes.
     * The notOnOrAfter value plus the clockskew is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeSystemTimePlus4Min_ClockSkewSetTo5Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTime.plus(FOUR_MIN).getMillis());
        notOnOrAfter = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FIVE_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(subjectConfirmation));

                atMost(2).of(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(notOnOrAfter));
            }
        });

        try {
            validator = new NotOnOrAfterTest();

            validator.verifySubject();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    /**
     * Set the notOnOrAfter attribute 5 minutes before the system time.
     * Set the clockskew to 4 minutes.
     * The notOnOrAfter value plus the clockskew is before the system time, therefore a SamlException is thrown.
     */
    @Test
    public void testFakeCurrentTimeMinus5Min_ClockSkewSetTo4Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        notOnOrAfter = new DateTime(systemTimeMilliseconds).minus(FIVE_MIN);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FOUR_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(subjectConfirmation));

                atMost(3).of(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(notOnOrAfter));
            }
        });

        try {
            validator = new NotOnOrAfterTest();

            validator.verifySubject();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    /**
     * Set the notOnOrAfter attribute 5 minutes after the system time.
     * Set the clockskew to 4 minutes.
     * The notOnOrAfter value plus the clockskew is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeCurrentTimePlus5Min_ClockSkewSetTo4Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        notOnOrAfter = new DateTime(systemTimeMilliseconds).plus(FIVE_MIN);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FOUR_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(subjectConfirmation));

                atMost(2).of(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(notOnOrAfter));
            }
        });

        try {
            validator = new NotOnOrAfterTest();

            validator.verifySubject();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    /**
     * Set the system time 5 minutes before the notOnOrAfter attribute.
     * Set the clockskew to 4 minutes.
     * The notOnOrAfter value plus the clockskew is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeSystemTimeMinus5Min_ClockSkewSetTo4Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTime.minus(FIVE_MIN).getMillis());
        notOnOrAfter = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FOUR_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(subjectConfirmation));

                atMost(2).of(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(notOnOrAfter));
            }
        });

        try {
            validator = new NotOnOrAfterTest();

            validator.verifySubject();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    /**
     * Set the system time 5 minutes after the notOnOrAfter attribute.
     * Set the clockskew to 4 minutes.
     * The notOnOrAfter value plus the clockskew is before the system time, therefore a SamlException is thrown.
     */
    @Test
    public void testFakeSystemTimePlus5Min_ClockSkewSetTo4Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTime.plus(FIVE_MIN).getMillis());
        notOnOrAfter = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FOUR_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(subjectConfirmation));

                atMost(3).of(subjectConfirmationData).getNotOnOrAfter();
                will(returnValue(notOnOrAfter));
            }
        });

        try {
            validator = new NotOnOrAfterTest();

            validator.verifySubject();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }
}
