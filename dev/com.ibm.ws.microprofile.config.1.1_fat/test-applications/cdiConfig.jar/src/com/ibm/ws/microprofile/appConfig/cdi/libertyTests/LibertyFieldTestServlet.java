/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.libertyTests;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.cdi.web.AbstractBeanServlet;

@SuppressWarnings("serial")
@WebServlet("/libertyField")
public class LibertyFieldTestServlet extends AbstractBeanServlet {

    @Inject
    LibertyRequestScopedConfigFieldInjectionBean configBean;

    /** {@inheritDoc} */
    @Override
    public Object getBean() {
        return configBean;
    }

    /**
     * Deriving converters from parent classes is extra functionality beyond the MP specification, hence don't test this against mpConfig > 1.4
     */
    @Test
    public void testParent() throws Exception {
        test("PARENT_KEY", "Child: parent");
    }

    /**
     * Deriving converters from child classes is extra functionality beyond the MP specification, hence don't test this against mpConfig > 1.4
     *
     * The Animal class has two converters, one for each child class: Converter<Ant> and Converter<Dog>.
     *
     * The former is registered first and therefore will be used.
     */
    @Test
    public void testAnimal() throws Exception {
        test("ANIMAL_KEY", "A Red Ant called Bob");
    }

    /**
     * This test is to test the PIZZA_KEY will be return a pizza value with null deliberately as no size is specified in the key. No default value should be used.
     *
     * TODO: There is an ongoing discussion for the intended behaviour for this in MP Config: https://github.com/eclipse/microprofile-config/issues/608
     */
    @Test
    public void testPizza() throws Exception {
        test("PIZZA_KEY", "null");
    }

    /**
     * This test is to return null as the key is missing from the config source and the default value was set to "", which should be converted to a null value.
     */
    @Test
    public void testPartialPizza() throws Exception {
        test("PIZZA_MISSING_KEY", "null");
    }

    /**
     * If multiple Converters are registered for the same Type with the same priority, the result should be deterministic.
     *
     * For mpConfig < 2.0, the last duplicate converter in org.eclipse.microprofile.config.spi.Converter will be used.
     */
    @Test
    public void testDuplicateConverters() throws Exception {
        test("DUPLICATE_CONVERTERS_KEY_1", "Output from Converter 2");
        test("DUPLICATE_CONVERTERS_KEY_2", "Output from Converter 2");
        test("DUPLICATE_CONVERTERS_KEY_3", "Output from Converter 2");
    }

}
