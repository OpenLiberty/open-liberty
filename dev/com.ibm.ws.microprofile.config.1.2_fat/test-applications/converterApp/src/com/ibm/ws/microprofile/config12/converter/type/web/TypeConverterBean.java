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
package com.ibm.ws.microprofile.config12.converter.type.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.ws.microprofile.config12.converter.type.beans.ChildType;
import com.ibm.ws.microprofile.config12.converter.type.beans.ParentType;
import com.ibm.ws.microprofile.config12.converter.type.converters.ChildTypeConverter;

@RequestScoped
public class TypeConverterBean {

//    @Inject
//    @ConfigProperty(name = "list1")
//    List<ChildType> list1;

    /**
     * Tests the SPI that allows a lambda to be added as a converter
     *
     * @throws Exception
     */
    public void lambdaConverterTest() throws Exception {

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();

        Converter<ChildType> converter = s -> new ChildType(s, "LambdaConverter");

        builder.withConverter(ChildType.class, 9000, converter);

        Config config = builder.build();
        ChildType myObject = config.getValue("key1", ChildType.class);

        assertEquals("value1", myObject.getValue());
        assertEquals("LambdaConverter", myObject.getConverter());
    }

    /**
     * Tests the SPI that allows the type of a converter to be forced
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void forcedTypeConverterTest() throws Exception {

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();

        //SubTypeConverter does not have the generic type set. It actually converts to ChildType but we are going to set it as a converter for ParentType
        builder.withConverter(ParentType.class, 9000, new ChildTypeConverter());

        Config config = builder.build();
        ParentType myObject = config.getValue("key1", ParentType.class);

        assertEquals("value1", myObject.getValue());
        assertEquals("SubTypeConverter", myObject.getConverter());
        assertTrue("myObject is not a ChildType. It is a " + myObject.getClass(), myObject instanceof ChildType);
    }

//    /**
//     * Tests that a List can be converted
//     *
//     * @throws Exception
//     */
//    public void listConverterTest() throws Exception {
//        assertEquals(5, list1.size());
//        assertEquals("value1", list1.get(0));
//        assertEquals("value2", list1.get(1));
//        assertEquals("value3", list1.get(2));
//        assertEquals("value4", list1.get(3));
//        assertEquals("value5", list1.get(4));
//    }

}
