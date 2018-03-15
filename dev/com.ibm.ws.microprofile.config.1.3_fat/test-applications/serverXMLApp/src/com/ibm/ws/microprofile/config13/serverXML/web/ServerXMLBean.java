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
     * Tests the SPI that allows the priority to be set
     *
     * @throws Exception
     */
    public void appPropertiesSPITest() throws Exception {

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        builder.addDiscoveredConverters();

        Config config = builder.build();
        String serverXMLValue2 = config.getValue("serverXMLKey2", String.class);

        assertEquals("serverXMLValue2", serverXMLValue2);
    }

}
