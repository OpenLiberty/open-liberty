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
package io.openliberty.microprofile.config.internal_fat.apps.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.inject.Inject;
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

    @Inject
    DuplicateConvertersBean duplicateConvertersBean;

    /**
     * Test what happens when a converter raises an exception for mpConfig > 1.4.
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

    /**
     * If multiple Converters are registered for the same Type with the same priority, the result should be deterministic.
     *
     * For mpConfig > 1.4, the first of the duplicate converters in org.eclipse.microprofile.config.spi.Converter will be used.
     */
    @Test
    public void testDuplicateConverters() throws Exception {
        assertEquals("Output from Converter 1", duplicateConvertersBean.getDUPLICATE_CONVERTERS_KEY_1().toString());
        assertEquals("Output from Converter 1", duplicateConvertersBean.getDUPLICATE_CONVERTERS_KEY_2().toString());
        assertEquals("Output from Converter 1", duplicateConvertersBean.getDUPLICATE_CONVERTERS_KEY_3().toString());
    }

}