/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.xml.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.After;
import org.junit.Test;

import com.ibm.ws.microprofile.config.dynamic.test.TestDynamicConfigSource;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

public class CustomXMLSourceTest {

    @Test
    public void testUsersPropertiesSource() throws MalformedURLException {
        GenericXMLConfigSource source = new GenericXMLConfigSource("client_config.xml", CustomXMLSourceTest.class.getClassLoader(), Integer.MAX_VALUE);
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder = builder.withConverters(new ClientXMLConverter());
        builder.withSources(source);
        Config config = builder.build();
        Iterable<String> keys = config.getPropertyNames();
        Map<String, Client> clients = new HashMap<>();
        for (String key : keys) {
            if (key.startsWith("client")) {
                Client client = config.getValue(key, Client.class);
                clients.put(key, client);
            }
        }
        assertEquals(4, clients.size());
        assertEquals("Windy Street", clients.get("client_0").getAddress().getStreet());
        assertEquals("Basingstoke", clients.get("client_1").getAddress().getCity());
        assertEquals("Mark", clients.get("client_2").getName());
        assertEquals("Jeremy", clients.get("client_3").getName());
    }

    @Test
    public void testNonExistant() {
        GenericXMLConfigSource source = new GenericXMLConfigSource("client_config.xml", CustomXMLSourceTest.class.getClassLoader(), Integer.MAX_VALUE);
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder = builder.withSources(source);
        builder = builder.withConverters(new ClientXMLConverter());
        Config config = builder.build();

        Client client = config.getOptionalValue("FAKE_KEY", Client.class).orElse(null);
        assertNull(client);
    }

    @Test
    public void testRawString() {
        GenericXMLConfigSource source = new GenericXMLConfigSource("client_config.xml", CustomXMLSourceTest.class.getClassLoader(), Integer.MAX_VALUE);
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder = builder.withSources(source);
        builder = builder.withConverters(new ClientXMLConverter());
        Config config = builder.build();

        String client = config.getValue("client_0", String.class);
        assertTrue(client.contains("<street>Windy Street</street>"));
    }

    @Test
    public void testBadConversion() {
        GenericXMLConfigSource source = new GenericXMLConfigSource("client_config.xml", CustomXMLSourceTest.class.getClassLoader(), Integer.MAX_VALUE);
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder = builder.withSources(source);
        builder = builder.withConverters(new ClientXMLConverter());
        Config config = builder.build();

        Integer client = null;
        try {
            client = config.getOptionalValue("client_0", Integer.class).orElse(null);
            fail("Exception not thrown: " + client);
        } catch (IllegalArgumentException e) {
            assertNull(client);
        }
    }

    @Test
    public void testBadConversion2() throws InterruptedException {
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 1);
        TestDynamicConfigSource source = new TestDynamicConfigSource();

        source.put("client_0", "<client><name>Tom</name><address><city>Winchester</city><street>Windy Street</street></address></client>");
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder = builder.withSources(source);
        builder = builder.withConverters(new ClientXMLConverter());
        Config config = builder.build();

        Client client1 = null;
        Client client2 = null;
        try {
            client1 = config.getValue("client_0", Client.class);
            assertNotNull(client1);
            source.put("client_0", "vfkdjhbvkjhvdf");

            Thread.sleep(1000);

            client2 = config.getValue("client_0", Client.class);
            fail("Exception not thrown: " + client2);
        } catch (IllegalArgumentException e) {
            assertNull(client2);
        }
    }

    @After
    public void resetRefresh() {
        System.setProperty(ConfigConstants.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "");
    }
}
