/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.converters.test.libertyTests;

import static org.junit.Assert.fail;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.converters.test.Child;
import com.ibm.ws.microprofile.appConfig.converters.test.CustomPropertyObject1;
import com.ibm.ws.microprofile.appConfig.converters.test.MakeSubclassConverter;
import com.ibm.ws.microprofile.appConfig.converters.test.MyBadConverter;
import com.ibm.ws.microprofile.appConfig.converters.test.MySource;
import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/libertyConvertersTestServlet")
public class LibertyConvertersTestServlet extends FATServlet {
    public static final String DYNAMIC_REFRESH_INTERVAL_PROP_NAME = "microprofile.config.refresh.rate";

    /**
     * Test support for different type and subclass converters
     *
     * Behaviour for subclass converters is not defined in the MicroProfile Config specification.
     *
     * @throws Exception
     */
    @Test
    public void testConverterSubclass() throws Exception {
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();
        b.withConverters(new MakeSubclassConverter<>());
        MySource source = new MySource("key1", "value1");
        b.withSources(source);
        Config config = b.build();
        Child child = config.getValue("key1", Child.class);
        if (!"value1".equals(child.getValue())) {
            fail("FAILED" + child.getValue());
        }
    }

    /**
     * Test what happens when a converter raises an exception
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" }) // FFDC only thrown in mpConfig <2.0
    public void testConverterExceptions() throws Exception {

        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();

        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        MySource s1 = new MySource();

        s1.put("p1", "setting1=customV1,setting2=customV2");
        s1.put("p2", "value1=customV3,value2=customV4");
        s1.put("p3", "attr1=customV5,attr2=customV6");

        b.withSources(s1);
        b.addDefaultSources();

        Converter<CustomPropertyObject1> c1 = new MyBadConverter();

        b.withConverters(c1);

        Config c = b.build();

        try {
            @SuppressWarnings("unused")
            CustomPropertyObject1 p1 = c.getValue("p1", CustomPropertyObject1.class);
            fail("FAILED: IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            TestUtils.assertEquals("Converter throwing intentional exception", e.getMessage());
        }

    }

}