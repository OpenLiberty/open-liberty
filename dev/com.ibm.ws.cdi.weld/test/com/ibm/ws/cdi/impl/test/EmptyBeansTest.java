/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.impl.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;

import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.bootstrap.spi.Scanning;
import org.junit.Test;

import com.ibm.ws.cdi.impl.weld.WebSphereCDIDeploymentImpl;

/**
 *
 */
public class EmptyBeansTest {

    @Test
    public void testIsEmpty() {
        assertTrue(WebSphereCDIDeploymentImpl.isEmpty(BeansXml.EMPTY_BEANS_XML));
    }

    @Test
    public void testNullURL() {
        assertTrue(WebSphereCDIDeploymentImpl.isEmpty(new TestBeansXml(null)));
    }

    @Test
    public void testEmptyFile() {
        URL url = TestBeansXml.class.getResource("zero_bytes.xml");
        assertNotNull(url);
        assertTrue(WebSphereCDIDeploymentImpl.isEmpty(new TestBeansXml(url)));
    }

    @Test
    public void testTrashFile() {
        URL url = TestBeansXml.class.getResource("trash.xml");
        assertNotNull(url);
        assertFalse(WebSphereCDIDeploymentImpl.isEmpty(new TestBeansXml(url)));
    }

    private static class TestBeansXml implements BeansXml {

        private final URL url;

        public TestBeansXml(URL url) {
            this.url = url;
        }

        /** {@inheritDoc} */
        @Override
        public URL getUrl() {
            return this.url;
        }

        //Don't care about the rest of these methods here

        /** {@inheritDoc} */
        @Override
        public BeanDiscoveryMode getBeanDiscoveryMode() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public List<Metadata<String>> getEnabledAlternativeClasses() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public List<Metadata<String>> getEnabledAlternativeStereotypes() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public List<Metadata<String>> getEnabledDecorators() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public List<Metadata<String>> getEnabledInterceptors() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Scanning getScanning() {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public String getVersion() {
            // TODO Auto-generated method stub
            return null;
        }

    }
}
