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
package io.openliberty.microprofile.config.internal_fat.apps.converter;

import static org.junit.Assert.fail;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.config.internal_fat.apps.TestUtils;

@SuppressWarnings("serial")
@WebServlet("/")
public class ConvertersTestServlet extends FATServlet {
    public static final String DYNAMIC_REFRESH_INTERVAL_PROP_NAME = "microprofile.config.refresh.rate";

    /**
     * Test what happens when a converter raises an exception for mpConfig2.0 and above
     *
     * @throws Exception
     */
    @Test
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