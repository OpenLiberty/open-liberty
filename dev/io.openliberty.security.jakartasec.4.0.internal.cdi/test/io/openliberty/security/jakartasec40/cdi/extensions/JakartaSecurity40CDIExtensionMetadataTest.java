/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec40.cdi.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import jakarta.enterprise.inject.spi.Extension;

/**
 * unit tests for the JakartaSecurity40CDIExtensionMetadata class.
 */
public class JakartaSecurity40CDIExtensionMetadataTest {

    private JakartaSecurity40CDIExtensionMetadata metadata;

    @Before
    public void setUp() throws Exception {
        metadata = new JakartaSecurity40CDIExtensionMetadata();
    }

    @Test
    public void testGetExtensions() {
        Set<Class<? extends Extension>> extensions = metadata.getExtensions();
        
        assertNotNull("Extensions set should not be null", extensions);
        assertEquals("Extensions set should contain exactly one entry", 1, extensions.size());
        assertTrue("Extensions set should contain JakartaSecurity40CDIExtension", 
                  extensions.contains(JakartaSecurity40CDIExtension.class));
    }

    @Test
    public void testGetBeanDefiningAnnotationClasses() {
        Set<Class<? extends Annotation>> annotations = metadata.getBeanDefiningAnnotationClasses();
        
        assertNotNull("Bean defining annotations set should not be null", annotations);
        assertEquals("Bean defining annotations set should be empty", 0, annotations.size());
    }

    @Test
    public void testApplicationBeansVisible() {
        assertTrue("Application beans should be visible", metadata.applicationBeansVisible());
    }
}
