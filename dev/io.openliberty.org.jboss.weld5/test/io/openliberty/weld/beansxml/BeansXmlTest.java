/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.weld.beansxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.URL;

import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.junit.Test;

/**
 *
 */
public class BeansXmlTest {

    private static final BeanDiscoveryMode DEFAULT_EMPTY_BEAN_DISCOVERY_MODE = BeanDiscoveryMode.ANNOTATED;

    @Test
    public void testBeansXmlEmpty() {
        WeldBootstrap bootstrap = new WeldBootstrap();
        URL url = BeansXmlTest.class.getResource("empty.xml");
        BeansXml beansXml = bootstrap.parse(url, DEFAULT_EMPTY_BEAN_DISCOVERY_MODE);
        BeanDiscoveryMode mode = beansXml.getBeanDiscoveryMode();
        assertEquals(BeanDiscoveryMode.ANNOTATED, mode);
        URL resourceUrl = beansXml.getUrl();
        assertNull(resourceUrl);
        String version = beansXml.getVersion();
        assertNull(version);
    }

    @Test
    public void testEmptyBeansXmlLegacyDefault() {
        WeldBootstrap bootstrap = new WeldBootstrap();
        URL url = BeansXmlTest.class.getResource("empty.xml");
        BeansXml beansXml = bootstrap.parse(url, BeanDiscoveryMode.ALL);
        BeanDiscoveryMode mode = beansXml.getBeanDiscoveryMode();
        assertEquals(BeanDiscoveryMode.ALL, mode);
        URL resourceUrl = beansXml.getUrl();
        assertNull(resourceUrl);
        String version = beansXml.getVersion();
        assertNull(version);
    }

    @Test
    public void testCDI10BeansXmlDefault() {
        WeldBootstrap bootstrap = new WeldBootstrap();
        URL url = BeansXmlTest.class.getResource("beans10_default.xml");
        BeansXml beansXml = bootstrap.parse(url, DEFAULT_EMPTY_BEAN_DISCOVERY_MODE);
        BeanDiscoveryMode mode = beansXml.getBeanDiscoveryMode();
        assertEquals(BeanDiscoveryMode.ANNOTATED, mode);
        URL resourceUrl = beansXml.getUrl();
        assertEquals(url, resourceUrl);
        String version = beansXml.getVersion();
        assertNull(version);
    }

    @Test
    public void testCDI40BeansXmlAll() {
        WeldBootstrap bootstrap = new WeldBootstrap();
        URL url = BeansXmlTest.class.getResource("beans40_all.xml");
        BeansXml beansXml = bootstrap.parse(url, DEFAULT_EMPTY_BEAN_DISCOVERY_MODE);
        BeanDiscoveryMode mode = beansXml.getBeanDiscoveryMode();
        assertEquals(BeanDiscoveryMode.ALL, mode);
        URL resourceUrl = beansXml.getUrl();
        assertEquals(url, resourceUrl);
        String version = beansXml.getVersion();
        assertEquals("4.0", version);
    }

    @Test
    public void testCDI40BeansXmlDefault() {
        WeldBootstrap bootstrap = new WeldBootstrap();
        URL url = BeansXmlTest.class.getResource("beans40_default.xml");
        BeansXml beansXml = bootstrap.parse(url, DEFAULT_EMPTY_BEAN_DISCOVERY_MODE);
        BeanDiscoveryMode mode = beansXml.getBeanDiscoveryMode();
        assertEquals(BeanDiscoveryMode.ANNOTATED, mode);
        URL resourceUrl = beansXml.getUrl();
        assertEquals(url, resourceUrl);
        String version = beansXml.getVersion();
        assertEquals("4.0", version);
    }

}
