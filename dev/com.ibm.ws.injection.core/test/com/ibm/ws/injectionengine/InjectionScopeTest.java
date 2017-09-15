/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wsspi.injectionengine.InjectionScope;

public class InjectionScopeTest
{
    @Test
    public void testSize()
    {
        Assert.assertEquals(InjectionScope.values().length, InjectionScope.size());
    }

    @Test
    public void testMatch()
    {
        Assert.assertNull(InjectionScope.match(""));
        Assert.assertNull(InjectionScope.match("x"));
        Assert.assertNull(InjectionScope.match("java:x"));

        Assert.assertNull(InjectionScope.match("java:comp"));
        Assert.assertEquals(InjectionScope.COMP, InjectionScope.match("java:comp/"));
        Assert.assertEquals(InjectionScope.COMP, InjectionScope.match("java:comp/x"));
        Assert.assertNull(InjectionScope.match("java:compx"));
        Assert.assertNull(InjectionScope.match("java:compx/"));

        Assert.assertNull(InjectionScope.match("java:module"));
        Assert.assertEquals(InjectionScope.MODULE, InjectionScope.match("java:module/"));
        Assert.assertNull(InjectionScope.match("java:modulex"));
        Assert.assertNull(InjectionScope.match("java:modulex/"));

        Assert.assertNull(InjectionScope.match("java:app"));
        Assert.assertEquals(InjectionScope.APP, InjectionScope.match("java:app/"));
        Assert.assertEquals(InjectionScope.APP, InjectionScope.match("java:app/x"));
        Assert.assertNull(InjectionScope.match("java:appx"));
        Assert.assertNull(InjectionScope.match("java:appx/"));

        Assert.assertNull(InjectionScope.match("java:global"));
        Assert.assertEquals(InjectionScope.GLOBAL, InjectionScope.match("java:global/"));
        Assert.assertEquals(InjectionScope.GLOBAL, InjectionScope.match("java:global/x"));
        Assert.assertNull(InjectionScope.match("java:globalx"));
        Assert.assertNull(InjectionScope.match("java:globalx/"));
    }

    @Test
    public void testNormalize()
    {
        Assert.assertEquals("", InjectionScope.normalize(""));
        Assert.assertEquals("x", InjectionScope.normalize("x"));
        Assert.assertEquals("java:x", InjectionScope.normalize("java:x"));
        Assert.assertEquals("java:comp", InjectionScope.normalize("java:comp"));
        Assert.assertEquals("java:compx", InjectionScope.normalize("java:compx"));
        Assert.assertEquals("java:comp/x", InjectionScope.normalize("java:comp/x"));
        Assert.assertEquals("java:comp/env", InjectionScope.normalize("java:comp/env"));
        Assert.assertEquals("java:comp/envx", InjectionScope.normalize("java:comp/envx"));
        Assert.assertEquals("", InjectionScope.normalize("java:comp/env/"));
        Assert.assertEquals("x", InjectionScope.normalize("java:comp/env/x"));
        Assert.assertEquals("java:module/env/x", InjectionScope.normalize("java:module/env/x"));
        Assert.assertEquals("java:app/env/x", InjectionScope.normalize("java:app/env/x"));
        Assert.assertEquals("java:global/env/x", InjectionScope.normalize("java:global/env/x"));
    }

    @Test
    public void testDenormalize()
    {
        Assert.assertEquals("java:comp/env/", InjectionScope.denormalize(""));
        Assert.assertEquals("java:comp/env/x", InjectionScope.denormalize("x"));
        Assert.assertEquals("java:", InjectionScope.denormalize("java:"));
        Assert.assertEquals("java:x", InjectionScope.denormalize("java:x"));
        Assert.assertEquals("java:comp/env/x", InjectionScope.denormalize("java:comp/env/x"));
    }
}
