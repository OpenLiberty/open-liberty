/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.fat.tests;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.fat.util.SharedServer;

/**
 *
 */
public class TypesTest extends AbstractConfigApiTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("TypesServer");

    public TypesTest() {
        super("/types/");
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public TestName testName = new TestName();

    /** Tests that a user can retrieve properties of type boolean */
    @Test
    public void testBooleanTypes() throws Exception {
        test(testName.getMethodName());
    }

    /** Tests that a user can retrieve properties of type Integer */
    @Test
    public void testIntegerTypes() throws Exception {
        test(testName.getMethodName());
    }
}
