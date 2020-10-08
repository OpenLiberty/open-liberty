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
package com.ibm.ws.microprofile.appConfig.converters.test;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class ConvertersTestServlet extends FATServlet {
    public static final String DYNAMIC_REFRESH_INTERVAL_PROP_NAME = "microprofile.config.refresh.rate";

    /**
     * Test that a simple converter can be registered for a user type
     *
     * @throws Exception
     */
    @Test
    public void testConverters() throws Exception {
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();

        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        MySource s1 = new MySource();

        s1.put("p1", "setting1=customV1,setting2=customV2");
        s1.put("p2", "value1=customV3,value2=customV4");
        s1.put("p3", "attr1=customV5,attr2=customV6");

        b.withSources(s1);
        b.addDefaultSources();

        Converter<CustomPropertyObject1> c1 = new MyConverter();
        Converter<CustomPropertyObject1> c2 = new MyConverter();
        Converter<CustomPropertyObject1> c3 = new MyConverter();
        //TODO converters added while string constructors are disabled
        Converter<CustomPropertyObject2> c4 = new MyConverter2();
        Converter<CustomPropertyObject3> c5 = new MyConverter3();

        b.withConverters(c1, c2);
        b.withConverters(c3);
        b.withConverters(c4, c5);

        Config config = b.build();

        try {
            CustomPropertyObject1 other1 = new CustomPropertyObject1("customV1", "customV2");
            TestUtils.assertContains(config, "p1", other1);
            CustomPropertyObject2 other2 = new CustomPropertyObject2("customV3", "customV4");
            TestUtils.assertContains(config, "p2", other2);
            CustomPropertyObject3 other3 = new CustomPropertyObject3("customV5", "customV6");
            TestUtils.assertContains(config, "p3", other3);

        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
    }

    /**
     * Test support for duplicate converters
     *
     * @throws Exception
     */
    @Test
    public void testMultipleSameTypeConverters() throws Exception {
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();

        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        MySource s1 = new MySource();

        s1.put("p1", "setting1=customV1,setting2=customV2");
        s1.put("p2", "value1=customV3,value2=customV4");
        s1.put("p3", "attr1=customV5,attr2=customV6");

        b.withSources(s1);
        b.addDefaultSources();

        Converter<CustomPropertyObject1> c1 = new MyConverter();
        Converter<CustomPropertyObject1> c2 = new MyConverter1_2();
        //TODO converter added while string constructors are disabled
        Converter<CustomPropertyObject2> c3 = new MyConverter2();

        b.withConverters(c1, c2, c3);

        Config c = b.build();

        CustomPropertyObject1 other1 = new CustomPropertyObject1("Optional", "orElse");
        CustomPropertyObject1 cpo1 = c.getOptionalValue("p1", CustomPropertyObject1.class).orElse(other1);

        CustomPropertyObject2 other2 = new CustomPropertyObject2("Optional", "orElse");
        CustomPropertyObject2 cpo2 = c.getOptionalValue("p2", CustomPropertyObject2.class).orElse(other2);

        boolean passed = cpo1.setting1.equals("customV1") && cpo1.setting2.equals("customV2")
                         && cpo2.value1.equals("customV3") && cpo2.value2.equals("customV4");

        if (!passed) {
            StringBuffer result = new StringBuffer();
            result.append("FAILED: ");
            Iterable<String> names = c.getPropertyNames();
            for (String name : names) {
                result.append("\n" + name + "=" + c.getValue(name, String.class));
            }
            fail(result.toString());
        }
    }

    /**
     * Test support for discovered converters
     *
     * @throws Exception
     */
    @Test
    public void testDiscoveredConverters() throws Exception {
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();
        b.addDefaultSources();
        b.addDiscoveredConverters();
        // The implementation for the above will add the default and discovered converters
        // (this is the intended behaviour regardless of any javadoc vagaries)
        //
        // com.ibm.ws.microprofile.appConfig-1.0_fat/test-applications/converters.war/resources/META-INF/services/org.eclipse.microprofile.config.spi.Converter
        // Has:
        // com.ibm.ws.microprofile.appConfig.converters.test.DiscoveredConverterDog
        // com.ibm.ws.microprofile.appConfig.converters.test.DiscoveredConverterCat
        //
        // com.ibm.ws.microprofile.appConfig-1.0_fat/test-applications/converters.war/resources/META-INF/microprofile-config.properties
        // Has:
        //                dog=sound=whoof
        //                cat=sound=meoow
        // (there is no need to escape an '=' if it is not in the key)

        Config c = b.build();

        Dog otherDog = null;
        Cat otherCat = null;
        try {
            otherDog = new Dog("sound=quack");
            otherCat = new Cat("sound=moo");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Now we can actually do the discovered converter magic...
        Dog dog = c.getOptionalValue("dog", Dog.class).orElse(otherDog);
        Cat cat = c.getOptionalValue("cat", Cat.class).orElse(otherCat);

        boolean discoveredConverters = false;

        discoveredConverters = dog != null && cat != null && dog.sound.equals("whoof") && cat.sound.equals("meoow");
        boolean passed = discoveredConverters;

        if (!passed) {
            StringBuffer result = new StringBuffer();
            result.append("FAILED: ");
            Iterable<String> names = c.getPropertyNames();
            for (String name : names) {
                result.append("\n" + name + "=" + c.getValue(name, String.class));
            }
            fail(result.toString());
        }
    }

}