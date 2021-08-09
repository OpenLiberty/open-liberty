/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.naming;

import org.junit.Assert;
import org.junit.Test;

public class JavaColonNamespaceTest {
    @Test
    public void testMatch() {
        for (NamingConstants.JavaColonNamespace namespace : NamingConstants.JavaColonNamespace.values()) {
            Assert.assertEquals(namespace.toString(), namespace, NamingConstants.JavaColonNamespace.match(namespace.prefix() + "x"));
        }
    }

    @Test
    public void testUnprefix() {
        for (NamingConstants.JavaColonNamespace namespace : NamingConstants.JavaColonNamespace.values()) {
            Assert.assertEquals(namespace.toString(), "x", namespace.unprefix(namespace.prefix() + "x"));
        }
    }

    @Test
    public void testIsComp() {
        for (NamingConstants.JavaColonNamespace namespace : NamingConstants.JavaColonNamespace.values()) {
            Assert.assertEquals(namespace.prefix().startsWith("java:comp/"), namespace.isComp());
        }
    }

    @Test
    public void testFromName() {
        for (NamingConstants.JavaColonNamespace namespace : NamingConstants.JavaColonNamespace.values()) {
            Assert.assertEquals(namespace.toString(), namespace, NamingConstants.JavaColonNamespace.fromName(namespace.qualifiedName()));
        }

        for (NamingConstants.JavaColonNamespace namespace : NamingConstants.JavaColonNamespace.values()) {
            NamingConstants.JavaColonNamespace ns = NamingConstants.JavaColonNamespace.fromName(namespace.prefix());
            Assert.assertNull("Expected null when calling fromName(" + namespace.prefix() + "), but got " + ns, ns);
        }
    }
}
