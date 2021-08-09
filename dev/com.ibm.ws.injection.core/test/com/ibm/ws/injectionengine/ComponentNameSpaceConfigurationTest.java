/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;

public class ComponentNameSpaceConfigurationTest
{
    @Test
    public void testToDumpString()
    {
        for (int i = 0; i < 4; i++)
        {
            ComponentNameSpaceConfiguration compNSConfig = new ComponentNameSpaceConfiguration(null, null);
            if ((i & 1) != 0)
            {
                compNSConfig.setResourceRefs(Collections.<ResourceRef> emptyList());
            }
            if ((i & 2) != 0)
            {
                compNSConfig.setResourceRefBindings(Collections.<String, String> emptyMap());
            }
            compNSConfig.toDumpString();
        }

        for (int i = 0; i < 16; i++)
        {
            ComponentNameSpaceConfiguration compNSConfig = new ComponentNameSpaceConfiguration(null, null);
            if ((i & 1) != 0)
            {
                compNSConfig.setEJBRefs(Collections.<EJBRef> emptyList());
            }
            if ((i & 2) != 0)
            {
                compNSConfig.setEJBLocalRefs(Collections.<EJBRef> emptyList());
            }
            if ((i & 7) == 4)
            {
                compNSConfig.setJNDIEnvironmentRefs(EJBRef.class, Collections.<EJBRef> emptyList());
            }
            if ((i & 8) != 0)
            {
                compNSConfig.setEJBRefBindings(Collections.<String, String> emptyMap());
            }
            compNSConfig.toDumpString();
        }

        for (int i = 0; i < 4; i++)
        {
            ComponentNameSpaceConfiguration compNSConfig = new ComponentNameSpaceConfiguration(null, null);
            if ((i & 1) != 0)
            {
                compNSConfig.setEnvEntries(Collections.<EnvEntry> emptyList());
            }
            if ((i & 2) != 0)
            {
                compNSConfig.setEnvEntryValues(Collections.<String, String> emptyMap());
            }
            compNSConfig.toDumpString();
        }
    }

    @Test
    public void testJNDIEnvironmentRefs()
    {
        ComponentNameSpaceConfiguration compNSConfig = new ComponentNameSpaceConfiguration(null, null);
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefs(ResourceRef.class));
        Assert.assertNull(compNSConfig.getResourceRefs());
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefs(ResourceEnvRef.class));
        Assert.assertNull(compNSConfig.getResourceEnvRefs());

        List<ResourceRef> resourceRefs = new ArrayList<ResourceRef>();
        compNSConfig.setJNDIEnvironmentRefs(ResourceRef.class, resourceRefs);
        Assert.assertSame(resourceRefs, compNSConfig.getJNDIEnvironmentRefs(ResourceRef.class));
        Assert.assertSame(resourceRefs, compNSConfig.getResourceRefs());
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefs(ResourceEnvRef.class));
        Assert.assertNull(compNSConfig.getResourceEnvRefs());
    }

    @Test
    public void testJNDIEnvironmentRefsBindings()
    {
        ComponentNameSpaceConfiguration compNSConfig = new ComponentNameSpaceConfiguration(null, null);
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefBindings(ResourceRef.class));
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefValues(ResourceRef.class));
        Assert.assertNull(compNSConfig.getResourceRefBindings());
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefBindings(ResourceEnvRef.class));
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefValues(ResourceEnvRef.class));
        Assert.assertNull(compNSConfig.getResourceEnvRefBindings());

        Map<String, String> bindings = new HashMap<String, String>();
        compNSConfig.setJNDIEnvironmentRefBindings(ResourceRef.class, bindings);
        Assert.assertSame(bindings, compNSConfig.getJNDIEnvironmentRefBindings(ResourceRef.class));
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefValues(ResourceRef.class));
        Assert.assertSame(bindings, compNSConfig.getResourceRefBindings());
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefBindings(ResourceEnvRef.class));
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefValues(ResourceEnvRef.class));
        Assert.assertNull(compNSConfig.getResourceEnvRefBindings());
    }

    @Test
    public void testJNDIEnvironmentRefsValues()
    {
        ComponentNameSpaceConfiguration compNSConfig = new ComponentNameSpaceConfiguration(null, null);
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefBindings(EnvEntry.class));
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefValues(EnvEntry.class));
        Assert.assertNull(compNSConfig.getResourceRefBindings());
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefBindings(ResourceEnvRef.class));
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefValues(ResourceEnvRef.class));
        Assert.assertNull(compNSConfig.getResourceEnvRefBindings());

        Map<String, String> bindings = new HashMap<String, String>();
        compNSConfig.setJNDIEnvironmentRefValues(EnvEntry.class, bindings);
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefBindings(EnvEntry.class));
        Assert.assertSame(bindings, compNSConfig.getJNDIEnvironmentRefValues(EnvEntry.class));
        Assert.assertSame(bindings, compNSConfig.getEnvEntryValues());
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefBindings(ResourceEnvRef.class));
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefValues(ResourceEnvRef.class));
        Assert.assertNull(compNSConfig.getResourceEnvRefBindings());
    }

    @Test
    public void testEJBRefs()
    {
        ComponentNameSpaceConfiguration compNSConfig = new ComponentNameSpaceConfiguration(null, null);
        Assert.assertNull(compNSConfig.getJNDIEnvironmentRefs(EJBRef.class));
        Assert.assertNull(compNSConfig.getEJBRefs());
        Assert.assertNull(compNSConfig.getEJBLocalRefs());

        compNSConfig = new ComponentNameSpaceConfiguration(null, null);
        List<EJBRef> ejbRefs = new ArrayList<EJBRef>();
        ejbRefs.add(TestHelper.createProxyInstance(EJBRef.class));
        compNSConfig.setEJBRefs(ejbRefs);
        Assert.assertSame(ejbRefs, compNSConfig.getJNDIEnvironmentRefs(EJBRef.class));
        Assert.assertSame(ejbRefs, compNSConfig.getEJBRefs());
        Assert.assertNull(compNSConfig.getEJBLocalRefs());

        compNSConfig = new ComponentNameSpaceConfiguration(null, null);
        List<EJBRef> ejbLocalRefs = new ArrayList<EJBRef>();
        ejbLocalRefs.add(TestHelper.createProxyInstance(EJBRef.class));
        compNSConfig.setEJBLocalRefs(ejbLocalRefs);
        Assert.assertSame(ejbLocalRefs, compNSConfig.getJNDIEnvironmentRefs(EJBRef.class));
        Assert.assertNull(compNSConfig.getEJBRefs());
        Assert.assertSame(ejbLocalRefs, compNSConfig.getEJBLocalRefs());

        compNSConfig = new ComponentNameSpaceConfiguration(null, null);
        compNSConfig.setEJBRefs(ejbRefs);
        compNSConfig.setEJBLocalRefs(ejbLocalRefs);
        List<? extends EJBRef> allEJBRefs = compNSConfig.getJNDIEnvironmentRefs(EJBRef.class);
        Assert.assertTrue(allEJBRefs.size() == 2 && allEJBRefs.contains(ejbRefs.get(0)) && allEJBRefs.contains(ejbLocalRefs.get(0)));
        Assert.assertSame(ejbRefs, compNSConfig.getEJBRefs());
        Assert.assertSame(ejbLocalRefs, compNSConfig.getEJBLocalRefs());

        compNSConfig = new ComponentNameSpaceConfiguration(null, null);
        compNSConfig.setJNDIEnvironmentRefs(EJBRef.class, allEJBRefs);
        Assert.assertSame(allEJBRefs, compNSConfig.getJNDIEnvironmentRefs(EJBRef.class));

        compNSConfig = new ComponentNameSpaceConfiguration(null, null);
        compNSConfig.setJNDIEnvironmentRefs(EJBRef.class, Collections.<EJBRef> emptyList());
        try
        {
            compNSConfig.setEJBRefs(Collections.<EJBRef> emptyList());
            Assert.fail();
        } catch (IllegalStateException ex)
        {
            // Pass.
        }
        try
        {
            compNSConfig.getEJBRefs();
            Assert.fail();
        } catch (IllegalStateException ex)
        {
            // Pass.
        }
        try
        {
            compNSConfig.setEJBLocalRefs(Collections.<EJBRef> emptyList());
            Assert.fail();
        } catch (IllegalStateException ex)
        {
            // Pass.
        }
        try
        {
            compNSConfig.getEJBLocalRefs();
            Assert.fail();
        } catch (IllegalStateException ex)
        {
            // Pass.
        }

        compNSConfig = new ComponentNameSpaceConfiguration(null, null);
        compNSConfig.setEJBRefs(Collections.<EJBRef> emptyList());
        try
        {
            compNSConfig.setJNDIEnvironmentRefs(EJBRef.class, Collections.<EJBRef> emptyList());
            Assert.fail();
        } catch (IllegalStateException ex)
        {
            // Pass.
        }

        compNSConfig = new ComponentNameSpaceConfiguration(null, null);
        compNSConfig.setEJBLocalRefs(Collections.<EJBRef> emptyList());
        try
        {
            compNSConfig.setJNDIEnvironmentRefs(EJBRef.class, Collections.<EJBRef> emptyList());
            Assert.fail();
        } catch (IllegalStateException ex)
        {
            // Pass.
        }
    }
}
