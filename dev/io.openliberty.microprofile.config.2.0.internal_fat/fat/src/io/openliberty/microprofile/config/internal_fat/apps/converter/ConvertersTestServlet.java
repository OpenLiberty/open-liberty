/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.config.internal_fat.apps.TestUtils;
import io.openliberty.microprofile.config.internal_fat.apps.converter.converters.MyBadConverter;

@SuppressWarnings("serial")
@RequestScoped
@WebServlet("/")
public class ConvertersTestServlet extends FATServlet {

    public static final String DUPLICATE_CONVERTERS_KEY_1 = "key1";
    public static final String DUPLICATE_CONVERTERS_KEY_2 = "key2";
    public static final String DUPLICATE_CONVERTERS_KEY_3 = "key3";

    @Inject
    @ConfigProperty(name = DUPLICATE_CONVERTERS_KEY_1)
    MyTypeWithMultipleConverters value1;

    @Inject
    @ConfigProperty(name = DUPLICATE_CONVERTERS_KEY_2)
    MyTypeWithMultipleConverters value2;

    @Inject
    @ConfigProperty(name = DUPLICATE_CONVERTERS_KEY_3)
    MyTypeWithMultipleConverters value3;

    /**
     * If multiple Converters are registered for the same Type with the same priority, the result should be deterministic.
     *
     * For mpConfig > 1.4, the first of the duplicate converters registered will be used.
     */
    @Test
    public void testDuplicateConverters() throws Exception {
        assertEquals("Output from Converter 1", value1.toString());
        assertEquals("Output from Converter 1", value2.toString());
        assertEquals("Output from Converter 1", value3.toString());
    }

    /**
     * Test what happens when a converter raises an exception for mpConfig > 1.4.
     *
     * @throws Exception
     */
    @Test
    public void testConverterExceptions() throws Exception {

        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();

        CustomSource s1 = new CustomSource();
        s1.put("aKey", "aValue");
        b.withSources(s1);
        b.addDefaultSources();

        Converter<CustomType> c1 = new MyBadConverter();
        b.withConverters(c1);

        Config c = b.build();

        try {
            @SuppressWarnings("unused")
            CustomType p1 = c.getValue("aKey", CustomType.class);
            fail("FAILED: IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            TestUtils.assertEquals("Converter throwing intentional exception", e.getMessage());
        }
    }

}