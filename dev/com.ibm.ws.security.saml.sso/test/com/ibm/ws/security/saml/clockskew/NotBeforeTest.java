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
import org.opensaml.saml2.core.AudienceRestriction;
import org.opensaml.saml2.core.Condition;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.OneTimeUse;

import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.acs.AssertionValidator;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;

public class NotBeforeTest extends AssertionValidator {

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();
    private static final BasicMessageContext<?, ?, ?> context = common.getBasicMessageContext();
    private static final Assertion assertion = common.getAssertion();
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final Conditions conditions = common.getConditions();
    private static final Condition condition = common.getCondition();

    @SuppressWarnings("unchecked")
    private static final List<AudienceRestriction> listAudienceRestriction = mockery.mock(List.class, "listAudienceRestriction");
    @SuppressWarnings("unchecked")
    private static final List<Condition> listCondition = mockery.mock(List.class, "listCondition");
    @SuppressWarnings("unchecked")
    private static final Iterator<Condition> iterator = mockery.mock(Iterator.class, "iterator");

    private static NotBeforeTest validator;
    private static DateTime notBefore;
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

    public NotBeforeTest() {
        super(context, assertion);
    }

    @BeforeClass
    public static void setUp() {
        mockery.checking(new Expectations() {
            {
                allowing(context).getSsoConfig();
                will(returnValue(ssoConfig));

                allowing(assertion).getConditions();
                will(returnValue(conditions));

                allowing(conditions).getAudienceRestrictions();
                will(returnValue(listAudienceRestriction));

                allowing(listAudienceRestriction).size();
                will(returnValue(1));

                allowing(conditions).getNotOnOrAfter();
                will(returnValue(null));
                allowing(conditions).getConditions();
                will(returnValue(listCondition));

                allowing(listCondition).iterator();
                will(returnValue(iterator));

                allowing(condition).getElementQName();
                will(returnValue(OneTimeUse.DEFAULT_ELEMENT_NAME));
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
     * Set the notBefore attribute and the system time with the same value.
     * Set the clockskew to 5 minute.
     * The notBefore value minus the clockskew is not after the system time, therefore verification is successful.
     */
    @Test
    public void testFakeCurrentTime_ClockSkewSetTo5Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        notBefore = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FIVE_MIN));

                atMost(2).of(conditions).getNotBefore();
                will(returnValue(notBefore));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(condition));
                one(iterator).hasNext();
                will(returnValue(false));
            }
        });

        try {
            validator = new NotBeforeTest();

            validator.verifyConditions();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the notBefore attribute and the system time with the same value.
     * Set the clockskew to zero.
     * The notBefore value minus zero is not after the system time, therefore verification is successful.
     */
    @Test
    public void testFakeCurrentTime_ClockSkewSetToZero() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        notBefore = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(0l));

                atMost(2).of(conditions).getNotBefore();
                will(returnValue(notBefore));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(condition));
                one(iterator).hasNext();
                will(returnValue(false));
            }
        });

        try {
            validator = new NotBeforeTest();

            validator.verifyConditions();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the notBefore attribute 1 second after the system time.
     * Set the clockskew to zero.
     * The notBefore value minus zero is one second after the system time, therefore a SamlException is thrown.
     */
    @Test
    public void testFakeCurrentTimePlus1Sec_ClockSkewSetToZero() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        notBefore = new DateTime(systemTime.plus(1000));

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(0l));

                atMost(3).of(conditions).getNotBefore();
                will(returnValue(notBefore));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(condition));
                one(iterator).hasNext();
                will(returnValue(false));
            }
        });

        try {
            validator = new NotBeforeTest();

            validator.verifyConditions();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    /**
     * Set the notBefore attribute 4 minutes before the system time.
     * Set the clockskew to 5 minutes.
     * The notBefore value minus the clockskew is not after the system time, therefore verification is successful.
     */
    @Test
    public void testFakeCurrentTimeMinus4Min_ClockSkewSetTo5Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        notBefore = new DateTime(systemTimeMilliseconds).minus(FOUR_MIN);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FIVE_MIN));

                atMost(2).of(conditions).getNotBefore();
                will(returnValue(notBefore));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(condition));
                one(iterator).hasNext();
                will(returnValue(false));
            }
        });

        try {
            validator = new NotBeforeTest();

            validator.verifyConditions();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the notBefore attribute 4 minutes after the system time.
     * Set the clockskew to 5 minutes.
     * The notBefore value minus the clockskew is not after the system time, therefore verification is successful.
     */
    @Test
    public void testFakeCurrentTimePlus4Min_ClockSkewSetTo5Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        notBefore = new DateTime(systemTimeMilliseconds).plus(FOUR_MIN);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FIVE_MIN));

                atMost(2).of(conditions).getNotBefore();
                will(returnValue(notBefore));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(condition));
                one(iterator).hasNext();
                will(returnValue(false));
            }
        });

        try {
            validator = new NotBeforeTest();

            validator.verifyConditions();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the system time 4 minutes before the notBefore attribute.
     * Set the clockskew to 5 minutes.
     * The notBefore value minus the clockskew is not after the system time, therefore verification is successful.
     */
    @Test
    public void testFakeSystemTimeMinus4Min_ClockSkewSetTo5Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTime.minus(FOUR_MIN).getMillis());
        notBefore = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FIVE_MIN));

                atMost(2).of(conditions).getNotBefore();
                will(returnValue(notBefore));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(condition));
                one(iterator).hasNext();
                will(returnValue(false));
            }
        });

        try {
            validator = new NotBeforeTest();

            validator.verifyConditions();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the system time 4 minutes after the notBefore attribute.
     * Set the clockskew to 5 minutes.
     * The notBefore value minus the clockskew is not after the system time, therefore verification is successful.
     */
    @Test
    public void testFakeSystemTimePlus4Min_ClockSkewSetTo5Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTime.plus(FOUR_MIN).getMillis());
        notBefore = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FIVE_MIN));

                atMost(2).of(conditions).getNotBefore();
                will(returnValue(notBefore));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(condition));
                one(iterator).hasNext();
                will(returnValue(false));
            }
        });

        try {
            validator = new NotBeforeTest();

            validator.verifyConditions();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the notBefore attribute 5 minutes before the system time.
     * Set the clockskew to 4 minutes.
     * The notBefore value minus the clockskew is not after the system time, therefore verification is successful.
     */
    @Test
    public void testFakeCurrentTimeMinus5Min_ClockSkewSetTo4Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        notBefore = new DateTime(systemTimeMilliseconds).minus(FIVE_MIN);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FOUR_MIN));

                atMost(2).of(conditions).getNotBefore();
                will(returnValue(notBefore));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(condition));
                one(iterator).hasNext();
                will(returnValue(false));
            }
        });

        try {
            validator = new NotBeforeTest();

            validator.verifyConditions();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    /**
     * Set the notBefore attribute 5 minutes after the system time.
     * Set the clockskew to 4 minutes.
     * The notBefore value minus the clockskew is after the system time, therefore a SamlException is thrown.
     */
    @Test
    public void testFakeCurrentTimePlus5Min_ClockSkewSetTo4Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTimeMilliseconds);
        notBefore = new DateTime(systemTimeMilliseconds).plus(FIVE_MIN);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FOUR_MIN));

                atMost(3).of(conditions).getNotBefore();
                will(returnValue(notBefore));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(condition));
                one(iterator).hasNext();
                will(returnValue(false));
            }
        });

        try {
            validator = new NotBeforeTest();

            validator.verifyConditions();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    /**
     * Set the system time 5 minutes before the notBefore attribute.
     * Set the clockskew to 4 minutes.
     * The notBefore value minus the clockskew is after the system time, therefore a SamlException is thrown.
     */
    @Test
    public void testFakeSystemTimeMinus5Min_ClockSkewSetTo4Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTime.minus(FIVE_MIN).getMillis());
        notBefore = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FOUR_MIN));

                atMost(3).of(conditions).getNotBefore();
                will(returnValue(notBefore));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(condition));
                one(iterator).hasNext();
                will(returnValue(false));
            }
        });

        try {
            validator = new NotBeforeTest();

            validator.verifyConditions();
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    /**
     * Set the system time 5 minutes after the notBefore attribute.
     * Set the clockskew to 4 minutes.
     * The notBefore value minus the clockskew is not after the system time, therefore verification is successful.
     */
    @Test
    public void testFakeSystemTimePlus5Min_ClockSkewSetTo4Min() {
        DateTimeUtils.setCurrentMillisFixed(systemTime.plus(FIVE_MIN).getMillis());
        notBefore = new DateTime(systemTimeMilliseconds);

        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getClockSkew();
                will(returnValue(FOUR_MIN));

                atMost(3).of(conditions).getNotBefore();
                will(returnValue(notBefore));

                one(iterator).hasNext();
                will(returnValue(true));
                one(iterator).next();
                will(returnValue(condition));
                one(iterator).hasNext();
                will(returnValue(false));
            }
        });

        try {
            validator = new NotBeforeTest();

            validator.verifyConditions();
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }
}
