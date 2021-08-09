/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config12.converter.implicit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.config12.converter.implicit.beans.MissingStringCtorType;
import com.ibm.ws.microprofile.config12.converter.implicit.beans.StringCtorType;
import com.ibm.ws.microprofile.config12.converter.implicit.converters.StringCtorTypeConverter;

@RequestScoped
public class ImplicitConverterBean {

    @Inject
    @ConfigProperty(name = "key1")
    StringCtorType myObject;

    /**
     * Tests the implict string constructor conversion
     *
     * @throws Exception
     */
    public void stringConstructorTest() throws Exception {
        assertEquals("value1", myObject.getValue());
        assertEquals("ImplicitStringConstructor", myObject.getConverter());
    }

    /**
     * Implicit converters have a priority of 1. Check that it is not used if a higher priority converter exists.
     *
     * @throws Exception
     */
    public void overrideConverterTest() throws Exception {

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();

        //has priority 2 which should be just high enough the beat the implict string constructor
        builder.withConverters(new StringCtorTypeConverter());

        Config config = builder.build();
        StringCtorType myObject = config.getValue("key1", StringCtorType.class);

        assertEquals("value1", myObject.getValue());
        assertEquals("StringCtorTypeConverter", myObject.getConverter());
    }

    /**
     * Checks that the correct exception is thrown if the class does not have a suitable String constructor
     *
     * @throws Exception
     */
    public void stringConstructorMissingTest() throws Exception {

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();

        Config config = builder.build();
        MissingStringCtorType myObject = null;
        try {
            myObject = config.getValue("key1", MissingStringCtorType.class);
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            //expected
        }

        assertNull(myObject);
    }

}
