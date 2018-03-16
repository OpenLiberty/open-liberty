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
package com.ibm.ws.microprofile.config13.serverXML.web;

import static org.junit.Assert.assertEquals;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

@RequestScoped
public class ServerXMLBean {

    @Inject
    @ConfigProperty(name = "serverXMLKey2")
    String serverXMLKey2;

    /**
     * Just a basic test that the key/value pair exists
     *
     * @throws Exception
     */
    public void appPropertiesTest() throws Exception {
        assertEquals("serverXMLValue2", serverXMLKey2);
    }

    /**
     * Tests the that the appProperties in the server.xml are available via the SPI
     *
     * Also tests that where there are duplicate keys, the last one wins
     *
     * @throws Exception
     */
    public void appPropertiesSPITest() throws Exception {

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        builder.addDiscoveredConverters();

        Config config = builder.build();
        String serverXMLValue1 = config.getValue("serverXMLKey1", String.class);
        String serverXMLValue2 = config.getValue("serverXMLKey2", String.class);
        String serverXMLValue3 = config.getValue("serverXMLKey3", String.class);
        String serverXMLValue4 = config.getValue("serverXMLKey4", String.class);

        assertEquals("serverXMLValue1b", serverXMLValue1);
        assertEquals("serverXMLValue2", serverXMLValue2);
        assertEquals("serverXMLValue3", serverXMLValue3);
        assertEquals("serverXMLValue4", serverXMLValue4);
    }

}
