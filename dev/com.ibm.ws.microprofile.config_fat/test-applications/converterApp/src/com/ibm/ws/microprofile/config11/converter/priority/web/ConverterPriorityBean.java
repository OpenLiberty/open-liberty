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
package com.ibm.ws.microprofile.config11.converter.priority.web;

import static org.junit.Assert.assertEquals;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.config11.converter.priority.beans.MyObject;
import com.ibm.ws.microprofile.config11.converter.priority.converters.Priority50Converter;

@RequestScoped
public class ConverterPriorityBean {

    @Inject
    @ConfigProperty(name = "key1")
    String key1;

    @Inject
    @ConfigProperty(name = "key1")
    MyObject myObject;

    /**
     * Just a basic test that the key/value pair exists
     *
     * @throws Exception
     */
    public void noConversionTest() throws Exception {
        assertEquals("value1", key1);
    }

    /**
     * Test that the @Priority annotation on the discovered converters works
     *
     * @throws Exception
     */
    public void converterPriorityTest() throws Exception {
        assertEquals("value1", myObject.getValue());
        assertEquals("Priority3000Converter", myObject.getConverter());
    }

    /**
     * Tests the SPI that allows the priority to be set
     *
     * @throws Exception
     */
    public void converterPrioritySPITest() throws Exception {

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        builder.addDiscoveredConverters(); //one of the discovered converters has a priority annotation of 3000 and should be the one used
        builder.withConverters(new Priority50Converter());//should use the annotation on the converter (50)

        Config config = builder.build();
        MyObject myObject2 = config.getValue("key1", MyObject.class);

        assertEquals("value1", myObject2.getValue());
        assertEquals("Priority3000Converter", myObject2.getConverter());
    }

}
