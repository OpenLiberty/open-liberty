/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config12.converter.type.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.ws.microprofile.config12.converter.type.beans.ChildType;
import com.ibm.ws.microprofile.config12.converter.type.beans.MyStringObject;
import com.ibm.ws.microprofile.config12.converter.type.beans.ParentType;
import com.ibm.ws.microprofile.config12.converter.type.converters.ChildTypeConverter;

@RequestScoped
public class TypeConverterBean {

    @Inject
    @ConfigProperty(name = "list1")
    List<MyStringObject> list1;

    @Inject
    @ConfigProperty(name = "list1")
    Optional<List<MyStringObject>> list2;

    @Inject
    @ConfigProperty(name = "list1")
    Provider<Optional<List<MyStringObject>>> list3;

    @Inject
    @ConfigProperty(name = "list1")
    Set<MyStringObject> set1;

    @Inject
    @ConfigProperty(name = "unknown")
    Optional<MyStringObject> unknownOptional;

    @Inject
    @ConfigProperty(name = "unknown", defaultValue = "defaultValue")
    Optional<MyStringObject> defaultOptional;

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

    /**
     * Tests that a List can be converted
     *
     * @throws Exception
     */
    public void listConverterTest() throws Exception {
        listConverterTest(list1);
    }

    /**
     * Tests that a List can be converted
     *
     * @throws Exception
     */
    public void optionalListConverterTest() throws Exception {
        List<MyStringObject> list = list2.get();
        listConverterTest(list);
    }

    /**
     * Tests that a List can be converted
     *
     * @throws Exception
     */
    public void providerOptionalListConverterTest() throws Exception {
        List<MyStringObject> list = list3.get().get();
        listConverterTest(list);
    }

    /**
     * Tests that a List can be converted
     *
     * @throws Exception
     */
    private void listConverterTest(List<MyStringObject> list) throws Exception {
        assertEquals(5, list.size());
        assertEquals("value1", list.get(0).getValue());
        assertEquals("value2", list.get(1).getValue());
        assertEquals("value3", list.get(2).getValue());
        assertEquals("value4", list.get(3).getValue());
        assertEquals("value5", list.get(4).getValue());
    }

    /**
     * Tests that a Set can be converted
     *
     * @throws Exception
     */
    public void setConverterTest() throws Exception {
        assertEquals(5, set1.size());
        for (MyStringObject obj : set1) {
            assertTrue("value1,value2,value3,value4,value5".contains(obj.getValue()));
        }
    }

    /**
     * Tests that a Optional can be converted
     *
     * @throws Exception
     */
    public void optionalConverterTest() throws Exception {
        assertNotNull(unknownOptional);
        if (unknownOptional.isPresent()) {
            fail("optional value should have been null but was (MyStringObject): " + unknownOptional.get().getValue());
        }
    }

    /**
     * Tests that a Optional can be converted
     *
     * @throws Exception
     */
    public void defaultOptionalConverterTest() throws Exception {
        assertNotNull(defaultOptional);
        assertTrue(defaultOptional.isPresent());
        assertEquals("defaultValue", defaultOptional.get().getValue());
    }

}
