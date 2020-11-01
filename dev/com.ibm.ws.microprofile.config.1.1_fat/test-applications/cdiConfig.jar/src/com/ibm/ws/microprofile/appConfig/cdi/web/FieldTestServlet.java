/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.web;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.cdi.beans.RequestScopedConfigFieldInjectionBean;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfig20EE8;

import componenttest.annotation.SkipForRepeat;

@SuppressWarnings("serial")
@WebServlet("/field")
public class FieldTestServlet extends AbstractBeanServlet {

    @Inject
    RequestScopedConfigFieldInjectionBean configBean;

    /** {@inheritDoc} */
    @Override
    public Object getBean() {
        return configBean;
    }

    @Test
    public void testGenericInstance() throws Exception {
        test("GENERIC_INSTANCE_KEY", "This is a random string");
    }

    @Test
    public void testGenericProvider() throws Exception {
        test("GENERIC_PROVIDER_KEY", "This is a random string");
    }

    @Test
    public void testSimpleString() throws Exception {
        test("SIMPLE_KEY1", "VALUE1");
    }

    @Test
    public void testDefault() throws Exception {
        test("SIMPLE_KEY2", "VALUE2");
    }

    @Test
    public void testNamed() throws Exception {
        test("SIMPLE_KEY3", "VALUE3");
    }

    @Test
    public void testOPTIONAL() throws Exception {
        test("OPTIONAL_KEY", "http://www.ibm.com");
    }

    @Test
    public void testOPTIONAL_NULL() throws Exception {
        test("OPTIONAL_NULL_KEY", "null");
    }

    @Test
    public void testOPTIONAL_MISSING() throws Exception {
        test("OPTIONAL_MISSING_KEY", "null");
    }

    @Test
    public void testDiscovered() throws Exception {
        test("DISCOVERED_KEY", "DISCOVERED_VALUE");
    }

    @Test
    @SkipForRepeat(RepeatConfig20EE8.ID) // TODO: The intended behaviour for this is not defined in the MP Config spec. It may be covered by the answer to this: https://github.com/eclipse/microprofile-config/issues/608
    public void testNullWithDefault() throws Exception {
        test("NULL_WITH_DEFAULT_KEY", "null");
    }

    @Test
    public void testDog() throws Exception {
        test("DOG_KEY", "A Black Dog called Bob");
    }

    /**
     * This tests that Injecting the MISSING_KEY, which is not defined in a config source, will return the defined defaultValue for MISSING_KEY.
     */
    @Test
    public void testMISSING_KEY_WITH_DEFAULT_VALUE() throws Exception {
        test("MISSING_KEY", "DEFAULT_VALUE");
    }

    /**
     * This is a good test to test the pizza converter functioning correctly. The key exists in the config source and is correctly converted to a pizza object.
     *
     * @throws Exception
     */
    @Test
    public void testGoodPizza() throws Exception {
        test("PIZZA_GOOD_KEY", "9 inch ham pizza");
    }

}
