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
package com.ibm.ws.security.saml.sso20.internal.utils;

import java.security.Security;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.saml.error.SamlException;


/**
 *
 */
public class SamlUtilTest {

    public static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final Throwable throwable = mockery.mock(Throwable.class);
    private static SamlUtil samlUtil;

    private static StackTraceElement[] stackTraceArray = new StackTraceElement[1];
    private static StackTraceElement stackTraceElement = new StackTraceElement("TestClass", "TestMethod", "TestFile", 118);

    private final String JCEPROVIDER_IBM = "IBMJCE";
    private final int iLimited = -1;

    @BeforeClass
    public static void setUp() {
        stackTraceArray[0] = stackTraceElement;
        samlUtil = new SamlUtil();
    }

    @Test
    public void hashTest() {
        samlUtil.hashCode();
    }

    @Test
    public void generateRandomTest() {
        SamlUtil.generateRandom();
    }

    @Test
    public void getRandomCatchException() {
        Security.removeProvider(JCEPROVIDER_IBM);
        SamlUtil.getRandom();
    }

    @Test
    public void generateRandomWithNumbersTest() {
        SamlUtil.generateRandom(10);
    }

    @Test
    public void cloneQNameTestWhenQnameIsNull() {
        SamlUtil.cloneQName(null);
    }

    @Test
    public void dumpStackTraceWhenLimitedIsLowerThanOne() {
        mockery.checking(new Expectations() {
            {
                one(throwable).getStackTrace();
                will(returnValue(stackTraceArray));
            }
        });
        SamlException.dumpStackTrace(throwable, iLimited);
    }
}
