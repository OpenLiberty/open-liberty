/*******************************************************************************
 * Copyright (c) 2014,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.ws.http.internal.VirtualHostImpl.EndpointState;
import com.ibm.ws.http.internal.VirtualHostImpl.RegistrationHolder;

/**
 *
 */
public class VirtualHostImplTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");

    final Mockery context = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    final BundleContext mockBundleContext = context.mock(BundleContext.class);

    @Rule
    public TestRule rule = outputMgr;

    @Test
    public void testChanged() {
        VirtualHostImpl vi = new VirtualHostImpl();
        RegistrationHolder rh = new RegistrationHolder(mockBundleContext, vi);

        String[] one = new String[] { "one" };
        String[] two = new String[] { "one", "two" };
        List<String> empty = Collections.emptyList();
        List<String> oneList = Arrays.asList(one);
        List<String> otherList = Arrays.asList("three");

        Assert.assertFalse("If both are empty, there is no change", rh.changed(null, null));
        Assert.assertFalse("If both are empty, there is no change", rh.changed(null, empty));
        Assert.assertFalse("If both are empty, there is no change", rh.changed(new String[] {}, null));
        Assert.assertFalse("If both are empty, there is no change", rh.changed(new String[] {}, empty));

        Assert.assertTrue("Changed if array is emtpy and list is not", rh.changed(null, oneList));
        Assert.assertTrue("Changed if list is empty and array is not", rh.changed(one, null));

        Assert.assertTrue("Changed if the array and list have different sizes", rh.changed(two, oneList));

        Assert.assertTrue("Changed if the array and list have different sizes", rh.changed(one, otherList));

    }

    @Test
    public void testRegenerateAliases() {
        final VirtualHostImpl vi = new VirtualHostImpl();
        final ComponentContext mockComponentContext = context.mock(ComponentContext.class);
        final ServiceRegistration mockRegistration = context.mock(ServiceRegistration.class);
        final HttpEndpointImpl mockEndpoint = context.mock(HttpEndpointImpl.class);

        Map<String, Object> map = new HashMap<String, Object>();

        context.checking(new Expectations() {
            {
                allowing(mockComponentContext).getBundleContext();
                will(returnValue(mockBundleContext));

                allowing(mockBundleContext).registerService(with(any(Class.class)), with(vi), with(any(Dictionary.class)));
                will(returnValue(mockRegistration));
            }
        });

        map.put("id", "default_host");
        vi.activate(mockComponentContext, map);

        VirtualHostConfig vhc = new VirtualHostConfig(vi, map);
        vhc.regenerateAliases();
        Assert.assertTrue("No listening endpoints, list of aliases should be empty", vhc.getHostAliases().isEmpty());

        EndpointState state = new EndpointState("*", 8080, -1);
        vi.myEndpoints.put(mockEndpoint, state);

        vhc.regenerateAliases();
        Assert.assertEquals("One listening endpoint, there should be two aliases (include *:-1)", 2, vhc.getHostAliases().size());

        vi.myEndpoints.remove(mockEndpoint);
        vhc.regenerateAliases();
        Assert.assertTrue("No listening endpoints, list of aliases should be empty", vhc.getHostAliases().isEmpty());
    }
}
