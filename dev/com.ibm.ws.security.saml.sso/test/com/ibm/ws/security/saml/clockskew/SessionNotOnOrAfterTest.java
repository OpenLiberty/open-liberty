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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnStatement;

import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.acs.AssertionValidator;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SessionNotOnOrAfterTest extends AssertionValidator {

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();
    private static final BasicMessageContext context = common.getBasicMessageContext();
    private static final Assertion assertion = common.getAssertion();
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final List<AuthnStatement> listAuthnStatement = mockery.mock(List.class, "listAuthnStatement");
    private static final Iterator<AuthnStatement> iterator = mockery.mock(Iterator.class, "iterator");
    private static final AuthnStatement authnStatement = common.getAuthnStatement();

    private static SessionNotOnOrAfterTest validator;
    private static DateTime sessionNotOnOrAfter;
    private static final DateTime systemTime = new DateTime(2015, 9, 30, 12, 0, 0, 0); // Date 2015/09/30 12:00:00:00
    private static final long systemTimeMilliseconds = systemTime.getMillis();
    private static final long FIVE_MIN = 300000l;
    private static final long FOUR_MIN = 240000l;

    static {
        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(0l));
            }
        });
    }

    public SessionNotOnOrAfterTest() {
        super(context, assertion);
    }

    @BeforeClass
    public static void setUp() {
        mockery.checking(new Expectations() {
            {
                allowing(context).getSsoConfig();
                will(returnValue(ssoConfig));

                allowing(assertion).getAuthnStatements();
                will(returnValue(listAuthnStatement));

                allowing(listAuthnStatement).iterator();
                will(returnValue(iterator));
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
     * Set the sessionNotOnOrAfter attribute to null.
     * Set the clockskew to zero.
     * Since the sessionNotOnOrAfter attribute is null, verification is not done.
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
                will(returnValue(authnStatement));
                one(iterator).hasNext();
                will(returnValue(false));

                one(authnStatement).getSessionNotOnOrAfter();
                will(returnValue(null));
            }
        });

        try {
            validator = new SessionNotOnOrAfterTest();

            validator.verifyAuthnStatement();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the sessionNotOnOrAfter attribute and the system time with the same value.
     * Set the clockskew to 5 minutes.
     * The sessionNotOnOrAfter value plus the clockskew is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeCurrentTime_ClockSkewSetTo5Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        sessionNotOnOrAfter = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FIVE_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(authnStatement));
                one(iterator).hasNext();
                will(returnValue(false));

                atMost(2).of(authnStatement).getSessionNotOnOrAfter();
                will(returnValue(sessionNotOnOrAfter));
            }
        });

        try {
            validator = new SessionNotOnOrAfterTest();

            validator.verifyAuthnStatement();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the sessionNotOnOrAfter attribute and the system time with the same value.
     * Set the clockskew to zero.
     * The sessionNotOnOrAfter value plus zero is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeCurrentTime_ClockSkewSetToZero() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        sessionNotOnOrAfter = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(0l));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(authnStatement));
                one(iterator).hasNext();
                will(returnValue(false));

                atMost(2).of(authnStatement).getSessionNotOnOrAfter();
                will(returnValue(sessionNotOnOrAfter));
            }
        });

        try {
            validator = new SessionNotOnOrAfterTest();

            validator.verifyAuthnStatement();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the sessionNotOnOrAfter attribute 1 second before the system time.
     * Set the clockskew to zero.
     * The sessionNotOnOrAfter value plus zero is one second before the system time, therefore a SamlException is thrown.
     */
    @Test
    public void testFakeCurrentTimeMinus1Sec_ClockSkewSetToZero() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        sessionNotOnOrAfter = new DateTime(systemTime.minus(1000));

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(0l));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(authnStatement));

                atMost(3).of(authnStatement).getSessionNotOnOrAfter();
                will(returnValue(sessionNotOnOrAfter));
            }
        });

        try {
            validator = new SessionNotOnOrAfterTest();

            validator.verifyAuthnStatement();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    /**
     * Set the sessionNotOnOrAfter attribute 4 minutes before the system time.
     * Set the clockskew to 5 minutes.
     * The sessionNotOnOrAfter value plus the clockskew is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeCurrentTimeMinus4Min_ClockSkewSetTo5Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        sessionNotOnOrAfter = new DateTime(systemTimeMilliseconds).minus(FOUR_MIN);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FIVE_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(authnStatement));
                one(iterator).hasNext();
                will(returnValue(false));

                atMost(2).of(authnStatement).getSessionNotOnOrAfter();
                will(returnValue(sessionNotOnOrAfter));
            }
        });

        try {
            validator = new SessionNotOnOrAfterTest();

            validator.verifyAuthnStatement();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the sessionNotOnOrAfter attribute 4 minutes after the system time.
     * Set the clockskew to 5 minutes.
     * The sessionNotOnOrAfter value plus the clockskew is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeCurrentTimePlus4Min_ClockSkewSetTo5Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        sessionNotOnOrAfter = new DateTime(systemTimeMilliseconds).plus(FOUR_MIN);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FIVE_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(authnStatement));
                one(iterator).hasNext();
                will(returnValue(false));

                atMost(2).of(authnStatement).getSessionNotOnOrAfter();
                will(returnValue(sessionNotOnOrAfter));
            }
        });

        try {
            validator = new SessionNotOnOrAfterTest();

            validator.verifyAuthnStatement();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the system time 4 minutes before the sessionNotOnOrAfter attribute.
     * Set the clockskew to 5 minutes.
     * The sessionNotOnOrAfter value plus the clockskew is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeSystemTimeMinus4Min_ClockSkewSetTo5Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTime.minus(FOUR_MIN).getMillis());
        sessionNotOnOrAfter = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FIVE_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(authnStatement));
                one(iterator).hasNext();
                will(returnValue(false));

                atMost(2).of(authnStatement).getSessionNotOnOrAfter();
                will(returnValue(sessionNotOnOrAfter));
            }
        });

        try {
            validator = new SessionNotOnOrAfterTest();

            validator.verifyAuthnStatement();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the system time 4 minutes after the sessionNotOnOrAfter attribute.
     * Set the clockskew to 5 minutes.
     * The sessionNotOnOrAfter value plus the clockskew is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeSystemTimePlus4Min_ClockSkewSetTo5Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTime.plus(FOUR_MIN).getMillis());
        sessionNotOnOrAfter = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FIVE_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(authnStatement));
                one(iterator).hasNext();
                will(returnValue(false));

                atMost(2).of(authnStatement).getSessionNotOnOrAfter();
                will(returnValue(sessionNotOnOrAfter));
            }
        });

        try {
            validator = new SessionNotOnOrAfterTest();

            validator.verifyAuthnStatement();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the sessionNotOnOrAfter attribute 5 minutes before the system time.
     * Set the clockskew to 4 minutes.
     * The sessionNotOnOrAfter value plus the clockskew is before the system time, therefore a SamlException is thrown.
     */
    @Test
    public void testFakeCurrentTimeMinus5Min_ClockSkewSetTo4Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        sessionNotOnOrAfter = new DateTime(systemTimeMilliseconds).minus(FIVE_MIN);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FOUR_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(authnStatement));

                atMost(3).of(authnStatement).getSessionNotOnOrAfter();
                will(returnValue(sessionNotOnOrAfter));
            }
        });

        try {
            validator = new SessionNotOnOrAfterTest();

            validator.verifyAuthnStatement();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    /**
     * Set the sessionNotOnOrAfter attribute 5 minutes after the system time.
     * Set the clockskew to 4 minutes.
     * The sessionNotOnOrAfter value plus the clockskew is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeCurrentTimePlus5Min_ClockSkewSetTo4Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        sessionNotOnOrAfter = new DateTime(systemTimeMilliseconds).plus(FIVE_MIN);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FOUR_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(authnStatement));
                one(iterator).hasNext();
                will(returnValue(false));

                atMost(2).of(authnStatement).getSessionNotOnOrAfter();
                will(returnValue(sessionNotOnOrAfter));
            }
        });

        try {
            validator = new SessionNotOnOrAfterTest();

            validator.verifyAuthnStatement();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the system time 5 minutes before the sessionNotOnOrAfter attribute.
     * Set the clockskew to 4 minutes.
     * The sessionNotOnOrAfter value plus the clockskew is not before the system time, therefore verification is successful.
     */
    @Test
    public void testFakeSystemTimeMinus5Min_ClockSkewSetTo4Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTime.minus(FIVE_MIN).getMillis());
        sessionNotOnOrAfter = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FOUR_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(authnStatement));
                one(iterator).hasNext();
                will(returnValue(false));

                atMost(2).of(authnStatement).getSessionNotOnOrAfter();
                will(returnValue(sessionNotOnOrAfter));
            }
        });

        try {
            validator = new SessionNotOnOrAfterTest();

            validator.verifyAuthnStatement();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the system time 5 minutes after the sessionNotOnOrAfter attribute.
     * Set the clockskew to 4 minutes.
     * The sessionNotOnOrAfter value plus the clockskew is before the system time, therefore a SamlException is thrown.
     */
    @Test
    public void testFakeSystemTimePlus5Min_ClockSkewSetTo4Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTime.plus(FIVE_MIN).getMillis());
        sessionNotOnOrAfter = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FOUR_MIN));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(authnStatement));

                atMost(3).of(authnStatement).getSessionNotOnOrAfter();
                will(returnValue(sessionNotOnOrAfter));
            }
        });

        try {
            validator = new SessionNotOnOrAfterTest();

            validator.verifyAuthnStatement();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }
}
