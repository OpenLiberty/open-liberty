/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.jbatch.jms.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Session;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BatchJmsMessageHelperTest {

    /**
     * Mock environment.
     */
    private Mockery mockery = null;
    Session mockSession = null;
    MapMessage mockMessage = null;

    /**
     * Create the mockery environment for each test. Setting up a new mockery
     * environment for each test helps isolate Expectation sets, making it easier to
     * debug when some Expectation fails and all the Expectations are dumped
     * to the error log.
     */
    @Before
    public void before() {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        mockSession = mockery.mock(Session.class);
        mockMessage = mockery.mock(MapMessage.class);
    }

    /**
     * There are alternative ways to do this.
     * 1) Use @RunWith(JMock.class) (deprecated)
     * 2) Declare the field: @Rule public final JUnitRuleMockery context = new JUnitRuleMockery();
     * (this version of Junit is not in our codebase).
     * 
     * Doing it the manual way for now.
     */
    @After
    public void after() {
        mockery.assertIsSatisfied();
    }

    /**
     * TODO mock J2EEName and re-able test
     */
//    @Ignore
//    public void testGetSetAmcNameToMessage() {
//        try {
//
//            final AmcName amc = AmcName.parse("SimpleBatchJob");
//
//            mockery.checking(new Expectations() {
//                {
//                    oneOf(mockSession).createMapMessage();
//                    will(returnValue(mockMessage));
//
//                    oneOf(mockMessage).setStringProperty(with(equal(BatchJmsConstants.PROPERTY_NAME_APP_NAME)),
//                                                         with(equal((amc.getApplicationName()))));
//                    oneOf(mockMessage).setStringProperty(with(equal(BatchJmsConstants.PROPERTY_NAME_MODULE_NAME)),
//                                                         with(equal((amc.getModuleName()))));
//                    oneOf(mockMessage).setStringProperty(with(equal(BatchJmsConstants.PROPERTY_NAME_COMP_NAME)),
//                                                         with(equal((amc.getComponentName()))));
//
//                    oneOf(mockMessage).getStringProperty(with(equal(BatchJmsConstants.PROPERTY_NAME_APP_NAME)));
//                    will(returnValue(amc.getApplicationName()));
//                    oneOf(mockMessage).getStringProperty(with(equal(BatchJmsConstants.PROPERTY_NAME_MODULE_NAME)));
//                    will(returnValue(amc.getModuleName()));
//                    oneOf(mockMessage).getStringProperty(with(equal(BatchJmsConstants.PROPERTY_NAME_COMP_NAME)));
//                    will(returnValue(amc.getComponentName()));
//                }
//            });
//
//            MapMessage jmsMsg = mockSession.createMapMessage();
//
//            BatchJmsMessageHelper.setJ2eeNameToMessage(jmsMsg, amc);
//
//            assertEquals(amc, BatchJmsMessageHelper.getJ2EENameFromJmsMessage(jmsMsg));
//
//        } catch (JMSException e1) {
//            // TODO Auto-generated catch block
//            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
//            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
//            e1.printStackTrace();
//        }
//    }

    @Test
    public void testSetGetJobParametersOnJmsMessage() {

        Properties allParameters = new Properties();
        allParameters.put("Prop1", "value1");
        allParameters.put("Prop2", "value2");
        allParameters.put("Prop3", "value3");
        allParameters.put(BatchJmsConstants.PROPERTY_NAME_JOB_OPERATION, "Start");

        Properties userParameters = new Properties();
        userParameters.put("Prop1", "value1");
        userParameters.put("Prop2", "value2");
        userParameters.put("Prop3", "value3");

        try {
            mockery.checking(new Expectations() {
                {
                    oneOf(mockSession).createMapMessage();
                    will(returnValue(mockMessage));

                    oneOf(mockMessage).setString(with(equal("Prop1")), with(equal(("value1"))));
                    oneOf(mockMessage).setString(with(equal("Prop2")), with(equal(("value2"))));
                    oneOf(mockMessage).setString(with(equal("Prop3")), with(equal(("value3"))));
                    oneOf(mockMessage).setString(with(equal(BatchJmsConstants.PROPERTY_NAME_JOB_OPERATION)), with(equal(("Start"))));

                    oneOf(mockMessage).getMapNames();
                    will(returnEnumeration("Prop1", "Prop2", "Prop3", BatchJmsConstants.PROPERTY_NAME_JOB_OPERATION));

                    oneOf(mockMessage).getString(with(equal("Prop1")));
                    will(returnValue(("value1")));
                    oneOf(mockMessage).getString(with(equal("Prop2")));
                    will(returnValue(("value2")));
                    oneOf(mockMessage).getString(with(equal("Prop3")));
                    will(returnValue(("value3")));

                    //return value should not contain the internal property
                }
            });

            MapMessage jmsMsg = mockSession.createMapMessage();

            BatchJmsMessageHelper.setJobParametersToJmsMessageBody(allParameters, jmsMsg);
            Properties output = BatchJmsMessageHelper.getJobParametersFromJmsMessage(jmsMsg);

            assertEquals(userParameters, output);

        } catch (JMSException e1) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e1.printStackTrace();
        }
    }

    /**
     * Verifying that no parameters maps to an empty java.util.Properties object
     * rather than a null.
     * 
     * @throws Exception
     */
    @Test
    public void testGetEmptyJobParametersWithoutMapOnJmsMessage() throws Exception {

        try {
            mockery.checking(new Expectations() {
                {
                    oneOf(mockSession).createMapMessage();
                    will(returnValue(mockMessage));
                    exactly(2).of(mockMessage).getMapNames();
                    will(returnEnumeration(new String[0]));
                }
            });
        } catch (JMSException e1) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e1.printStackTrace();
        }

        MapMessage jmsMsg = mockSession.createMapMessage();
        assertFalse(jmsMsg.getMapNames().hasMoreElements());
        Properties jobParams = BatchJmsMessageHelper.getJobParametersFromJmsMessage(jmsMsg);
        // This is the most important assertion
        assertEquals("Expecting empty parameters", new Properties(), jobParams);
    }

    @Test
    public void testSetNullJobParametersOnJmsMessage() {

        Properties allParameters = null;
        Properties userParameters = null;
        try {
            mockery.checking(new Expectations() {
                {
                    oneOf(mockSession).createMapMessage();
                    will(returnValue(mockMessage));
                }
            });

            MapMessage jmsMsg = mockSession.createMapMessage();

            BatchJmsMessageHelper.setJobParametersToJmsMessageBody(userParameters, jmsMsg);

        } catch (JMSException e1) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e1.printStackTrace();
        }
    }

    @Test
    public void testIsValidJmsStringPropertyKey() {
        assertTrue(BatchJmsMessageHelper.isValidJmsStringPropertyKey("thisIsValidString"));
        assertTrue(BatchJmsMessageHelper.isValidJmsStringPropertyKey("underscore_is_valid"));
        assertTrue(BatchJmsMessageHelper.isValidJmsStringPropertyKey("dollar$isValid"));
        assertTrue(BatchJmsMessageHelper.isValidJmsStringPropertyKey("digit123isValid"));

        assertTrue(BatchJmsMessageHelper.isValidJmsStringPropertyKey("_underscoreFirstLetterIsValid"));
        assertTrue(BatchJmsMessageHelper.isValidJmsStringPropertyKey("$dollarSignFirstLetterIsValid"));
        assertTrue(BatchJmsMessageHelper.isValidJmsStringPropertyKey("reallyreallylongString_reallyreallylongString_reallyreallylongString_reallyreallylongString_reallyreallylongString_reallyreallylongString_reallyreallylongString_reallyreallylongString"));

        assertFalse(BatchJmsMessageHelper.isValidJmsStringPropertyKey("1digitFirstLetterIsInvalid"));
        assertFalse(BatchJmsMessageHelper.isValidJmsStringPropertyKey("dot.is.invalid"));
        assertFalse(BatchJmsMessageHelper.isValidJmsStringPropertyKey("NULL"));
        assertFalse(BatchJmsMessageHelper.isValidJmsStringPropertyKey("TRUE"));
        assertFalse(BatchJmsMessageHelper.isValidJmsStringPropertyKey("FALSE"));
        assertFalse(BatchJmsMessageHelper.isValidJmsStringPropertyKey("NOT"));
        assertFalse(BatchJmsMessageHelper.isValidJmsStringPropertyKey("AND"));
        assertFalse(BatchJmsMessageHelper.isValidJmsStringPropertyKey("OR"));
        assertFalse(BatchJmsMessageHelper.isValidJmsStringPropertyKey("BETWEEN"));
        assertFalse(BatchJmsMessageHelper.isValidJmsStringPropertyKey("LIKE"));
        assertFalse(BatchJmsMessageHelper.isValidJmsStringPropertyKey("IN"));
        assertFalse(BatchJmsMessageHelper.isValidJmsStringPropertyKey("ESCAPE"));
    }
}
