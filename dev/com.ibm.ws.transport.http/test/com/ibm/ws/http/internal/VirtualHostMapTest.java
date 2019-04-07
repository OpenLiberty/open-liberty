/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.internal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.http.internal.VirtualHostMap.RequestHelper;
import com.ibm.ws.staticvalue.StaticValue;
import com.ibm.wsspi.http.HttpContainer;
import com.ibm.wsspi.http.VirtualHost;
import com.ibm.wsspi.http.VirtualHostListener;

import test.common.SharedOutputManager;

/**
 *
 */
public class VirtualHostMapTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");

    final Mockery context = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    final ComponentContext mockComponentContext = context.mock(ComponentContext.class);
    final BundleContext mockBundleContext = context.mock(BundleContext.class);
    final HttpEndpointImpl mockEndpoint = context.mock(HttpEndpointImpl.class);
    final ServiceRegistration<VirtualHost> mockVHostReg = context.mock(ServiceRegistration.class, "VirtualHostRegistration");
    final ServiceReference<VirtualHost> mockVHostRef = context.mock(ServiceReference.class, "VirtualHostReference");

    @Rule
    public TestRule mockRule = new TestRule() {
        @Override
        public Statement apply(final Statement stmt, final Description desc) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    // run the test
                    stmt.evaluate();
                    context.assertIsSatisfied();
                }
            };
        }
    };

    @Rule
    public TestRule rule = outputMgr;

    @Before
    public void setUp() throws Exception {
        context.checking(new Expectations() {
            {
                allowing(mockEndpoint).getName();
                will(returnValue("mockEndpoint"));

                allowing(mockVHostReg).getReference();
                will(returnValue(mockVHostRef));
            }
        });
        // Clear the alternateHostSelector and defaultHost
        Field f = VirtualHostMap.class.getDeclaredField("alternateHostSelector");
        f.setAccessible(true);
        f.set(null, StaticValue.createStaticValue(null));

        f = VirtualHostMap.class.getDeclaredField("defaultHost");
        f.setAccessible(true);
        f.set(null, StaticValue.createStaticValue(null));
    }

    @After
    public void tearDown() throws Exception {
        // Registration with HttpEndpointList is manual in these tests, clear the list.
        Assert.assertFalse("Test cleanup: list of registered endpoints should be empty", HttpEndpointList.getInstance().iterator().hasNext());
    }

    public void setUpOneActivation() {
        context.checking(new Expectations() {
            {
                one(mockComponentContext).getBundleContext();
                will(returnValue(mockBundleContext));

                one(mockBundleContext).registerService(with(any(Class.class)), with(any(VirtualHostImpl.class)), with(any(Dictionary.class)));
                will(returnValue(mockVHostReg));

                one(mockVHostReg).unregister();
            }
        });
    }

    @Test
    public void testActivateDefaultHostAliases() {
        setUpOneActivation();
        VirtualHostImpl vhost = new VirtualHostImpl();

        // If there are aliases defined for the default virtual host,
        // it stops acting like the "catch-all" host, and starts acting
        // like any other virtual host..
        vhost.activate(mockComponentContext, buildMap("default_host", Arrays.asList(new String[] { "*" })));
        Assert.assertFalse("isDefaultHost should return false (hostAlias defined)", vhost.getActiveConfig().isDefaultHost());

        TestRequestHelper helper80 = new TestRequestHelper("a.b.c", 80);

        // we should find the default_host by looking for port 80 (per alias)
        VirtualHostImpl vh = VirtualHostMap.findVirtualHost("endpoint", helper80);
        Assert.assertNotNull("VirtualHost should be retrieved by port.. ", vh);
        Assert.assertSame("Should retrieve default_host for port ", vhost, vh);

        context.checking(new Expectations() {
            {
                one(mockVHostRef).getProperty("aliases");
                will(returnValue(new String[] { "*:80" }));

                one(mockVHostRef).getProperty("httpsAlias");
                will(returnValue(null));

                one(mockVHostRef).getProperty("endpointReferences");
                will(returnValue(null));

                one(mockVHostReg).setProperties(with(any(Dictionary.class)));
            }
        });
        // Now, modify the default host configuration to remove the host alias,
        // and try to disable it. Should get a warning that you can't.
        Map<String, Object> map = buildMap("default_host", null);
        map.put("enabled", false);
        vhost.modified(map);
        Assert.assertTrue("Should see a warning message: can not disable default host", outputMgr.checkForMessages("CWWKT0024W:.*"));
        Assert.assertTrue("isDefaultHost should return true (no hostAlias defined)", vhost.getActiveConfig().isDefaultHost());
        vh = VirtualHostMap.findVirtualHost("endpoint", helper80);
        Assert.assertSame("VirtualHost should be retrieved by port 80 (catch-all)", vhost, vh);

        vhost.deactivate(mockComponentContext, 0);
    }

    @Test
    public void testActivateNoAliases() {
        setUpOneActivation();
        VirtualHostImpl vhost = new VirtualHostImpl();

        // pass through: should give a warning.
        // There are no aliases defined at all, and this is not the default host..
        vhost.activate(mockComponentContext, buildMap("vhost1", null));
        Assert.assertTrue("Should see a warning about missing hostAliases", outputMgr.checkForMessages("CWWKT0025W"));
        vhost.deactivate(mockComponentContext, 0);
    }

    @Test
    public void testActivateBadWildcardNoPort() {
        setUpOneActivation();
        VirtualHostImpl vhost = new VirtualHostImpl();

        // activate with a bad alias, that one bad alias should be removed,
        // so we should end up with no aliases and another warning..
        vhost.activate(mockComponentContext, buildMap("vhost1", Arrays.asList(new String[] { "*bad" })));
        Assert.assertTrue("Should see a warning about bad use of wildcard", outputMgr.checkForMessages("CWWKT0026W"));
        Assert.assertTrue("Should see a warning about missing hostAliases", outputMgr.checkForMessages("CWWKT0025W"));
        vhost.deactivate(mockComponentContext, 0);
    }

    @Test
    public void testActivateMultipleAliases() {
        setUpOneActivation();
        setUpOneActivation();
        setUpOneActivation();
        VirtualHostImpl vhost = new VirtualHostImpl();
        VirtualHostImpl vhost2 = new VirtualHostImpl();
        VirtualHostImpl vhost3 = new VirtualHostImpl();

        // activate with a bad alias, that one bad alias should be removed,
        // so we should end up with no aliases and another warning..
        vhost.activate(mockComponentContext, buildMap("vhost1", Arrays.asList(new String[] { "*:80" })));
        vhost2.activate(mockComponentContext, buildMap("vhost2", Arrays.asList(new String[] { "myhost:80" })));
        vhost3.activate(mockComponentContext, buildMap("vhost2", Arrays.asList(new String[] { "other:80" })));
        Assert.assertFalse("Should NOT see a warning about duplicate aliases", outputMgr.checkForMessages("CWWKT0027W"));
        Assert.assertFalse("Should NOT see a warning about missing hostAliases", outputMgr.checkForMessages("CWWKT0025W"));
        vhost.deactivate(mockComponentContext, 0);
        vhost2.deactivate(mockComponentContext, 0);
        vhost3.deactivate(mockComponentContext, 0);
    }

    @Test
    public void testActivateDuplicateAliases() {
        setUpOneActivation();
        setUpOneActivation();
        VirtualHostImpl vhost = new VirtualHostImpl();
        VirtualHostImpl vhost2 = new VirtualHostImpl();

        // activate with a bad alias, that one bad alias should be removed,
        // so we should end up with no aliases and another warning..
        vhost.activate(mockComponentContext, buildMap("vhost1", Arrays.asList(new String[] { "myhost:80" })));
        vhost2.activate(mockComponentContext, buildMap("vhost2", Arrays.asList(new String[] { "myhost:80" })));
        Assert.assertTrue("Should see a warning about duplicate aliases", outputMgr.checkForMessages("CWWKT0027W"));
        vhost.deactivate(mockComponentContext, 0);
        vhost2.deactivate(mockComponentContext, 0);
    }

    @Test
    public void testActivateDuplicateWildcardAliases() {
        setUpOneActivation();
        setUpOneActivation();
        VirtualHostImpl vhost = new VirtualHostImpl();
        VirtualHostImpl vhost2 = new VirtualHostImpl();

        // activate with a bad alias, that one bad alias should be removed,
        // so we should end up with no aliases and another warning..
        vhost.activate(mockComponentContext, buildMap("vhost1", Arrays.asList(new String[] { "*:80" })));
        vhost2.activate(mockComponentContext, buildMap("vhost2", Arrays.asList(new String[] { "*:80" })));
        Assert.assertTrue("Should see a warning about duplicate aliases", outputMgr.checkForMessages("CWWKT0027W"));
        vhost.deactivate(mockComponentContext, 0);
        vhost2.deactivate(mockComponentContext, 0);
    }

    @Test
    public void testAddRemoveVirtualHost() {
        setUpOneActivation();
        VirtualHostImpl vhost = new VirtualHostImpl();

        // add the wildcard alias for the vhost..
        vhost.activate(mockComponentContext, buildMap("vhost1", Arrays.asList(new String[] { "*" })));

        context.checking(new Expectations() {
            {
                one(mockVHostRef).getProperty("aliases");
                will(returnValue(new String[] { "*:80" }));

                one(mockVHostRef).getProperty("endpointReferences");
                will(returnValue(null));

                one(mockVHostRef).getProperty("httpsAlias");
                will(returnValue(null));

                one(mockVHostReg).setProperties(with(any(Dictionary.class)));
            }
        });

        // update the vhost configuration: should remove the good alias,
        // and then define a bad alias, that one bad alias should be removed,
        // so we should end up with no aliases and another warning..
        vhost.modified(buildMap("vhost1", Arrays.asList(new String[] { "*bad" })));
        Assert.assertTrue("Should see a warning about bad use of wildcard", outputMgr.checkForMessages("CWWKT0026W"));
        Assert.assertTrue("Should see a warning about missing hostAliases", outputMgr.checkForMessages("CWWKT0025W"));
        vhost.deactivate(mockComponentContext, 0);
    }

    @Test
    public void testVirtualHostAllowFromEndpoint() {
        setUpOneActivation();
        VirtualHostImpl vhost = new VirtualHostImpl();
        final HttpEndpointImpl mockEP1 = context.mock(HttpEndpointImpl.class, "allowEP1");
        final HttpEndpointImpl mockEP2 = context.mock(HttpEndpointImpl.class, "allowEP2");

        // define two allowFromEndpoints
        final ArrayList<String> ep = new ArrayList<String>();
        ep.add("allowEP1");
        ep.add("allowEP2");
        vhost.activate(mockComponentContext, buildMapWithEndpoint("vhost1", Arrays.asList(new String[] { "*" }), ep));
        Assert.assertTrue("VirtualHost should return allowEP1 and allowEP2 allowedEndpoints)",
                          vhost.getAllowedFromEndpoints().containsAll(ep));
        context.checking(new Expectations() {
            {
                allowing(mockVHostRef).getProperty("aliases");
                will(returnValue(new String[] { "*:80" }));
                allowing(mockVHostRef).getProperty("endpointReferences");
                will(returnValue(ep));
                allowing(mockVHostRef).getProperty("httpsAlias");
                will(returnValue(null));
                allowing(mockEP1).getPid();
                will(returnValue("allowEP1"));
                allowing(mockEP2).getPid();
                will(returnValue("allowEP2"));
            }
        });

        // update the vhost configuration: should remove one allowFromEndpoint,
        // so we should end up with only one allowFromEndpoint
        ep.remove("allowEP2");
        vhost.modified(buildMapWithEndpoint("vhost1", Arrays.asList(new String[] { "*" }), ep));
        Assert.assertTrue("VirtualHost should return only one allowEP1 allowedEndpoints)", vhost.getAllowedFromEndpoints().containsAll(ep));

        vhost.listenerStarted(mockEP1, vhost.getActiveConfig(), "localhost", 9000, false);
        vhost.listenerStarted(mockEP2, vhost.getActiveConfig(), "localhost", 8000, false);

        Assert.assertTrue("Virtual host should only have one endpoint", vhost.myEndpoints.size() == 1);
        Assert.assertTrue("getURL should use allowEP1 endpoint", "http://localhost:9000/ctxRoot".equals(vhost.getUrlString("/ctxRoot", false)));
        vhost.deactivate(mockComponentContext, 0);
    }

    @Test
    public void testAddRemoveVirtualHostNoChange() {
        setUpOneActivation();
        VirtualHostImpl vhost = new VirtualHostImpl();

        // register the vhost with an alias
        vhost.activate(mockComponentContext, buildMap("vhost1", Arrays.asList(new String[] { "*" })));

        context.checking(new Expectations() {
            {
                one(mockVHostRef).getProperty("aliases");
                will(returnValue(new String[] { "*:80" }));
                one(mockVHostRef).getProperty("endpointReferences");
                will(returnValue(null));
                one(mockVHostRef).getProperty("httpsAlias");
                will(returnValue(null));
            }
        });
        // update the vhost configuration with the identical alias list. This should
        // not result in a call to update the service properties..
        vhost.modified(buildMap("vhost1", Arrays.asList(new String[] { "*" })));
        vhost.deactivate(mockComponentContext, 0);
    }

    @Test
    public void testAddDisableVirtualHost() {
        setUpOneActivation();
        VirtualHostImpl vhost = new VirtualHostImpl();

        // register the vhost with an alias
        vhost.activate(mockComponentContext, buildMap("vhost1", Arrays.asList(new String[] { "*" })));

        // update the vhost configuration with the identical configuration,
        // but toggle the enabled flag..
        Map<String, Object> newMap = buildMap("vhost1", Arrays.asList(new String[] { "*" }));
        newMap.put(HttpServiceConstants.ENABLED, false);
        vhost.modified(newMap);

        vhost.deactivate(mockComponentContext, 0);
    }

    @Test
    public void testAddRemoveWildcardEndpoint() {
        setUpOneActivation();
        setUpOneActivation();
        context.checking(new Expectations() {
            {
                allowing(mockEndpoint).getResolvedHostName();
                will(returnValue("a.b.c"));

                allowing(mockEndpoint).getPid();
                will(returnValue("endpoint.pid"));

                allowing(mockEndpoint).getListeningHttpPort();
                will(returnValue(8080));

                allowing(mockEndpoint).getListeningSecureHttpPort();
                will(returnValue(-1));

                one(mockVHostRef).getProperty("aliases");
                will(returnValue(new String[] { "a.b.c:8080" }));

                one(mockVHostRef).getProperty("endpointReferences");
                will(returnValue(null));

                allowing(mockVHostRef).getProperty("httpsAlias");
                will(returnValue(null));
            }
        });

        TestRequestHelper helper8080 = new TestRequestHelper("a.b.c", 8080);
        TestRequestHelper wildcard8080 = new TestRequestHelper("d.e.f", 8080);

        // no vhosts, so this doesn't call anywhere
        HttpEndpointList.registerEndpoint(mockEndpoint);
        VirtualHostMap.notifyStarted(mockEndpoint, "*", 8080, false);
        Assert.assertNull("VirtualHost should not be retrieved by port.. (no virtual hosts)", VirtualHostMap.findVirtualHost("endpoint", helper8080));

        VirtualHostImpl vhost = new VirtualHostImpl();
        VirtualHostImpl defaulthost = new VirtualHostImpl();

        // now activate a vhost, which will trigger some actions when it is activated
        // register the vhost with an alias
        vhost.activate(mockComponentContext, buildMap("vhost1", Arrays.asList(new String[] { "a.b.c:8080" })));
        Assert.assertSame("VirtualHost should be retrieved by port.. ", vhost, VirtualHostMap.findVirtualHost("endpoint", helper8080));

        context.checking(new Expectations() {
            {
                exactly(2).of(mockVHostRef).getProperty("aliases");
                will(returnValue(null));
                exactly(2).of(mockVHostRef).getProperty("endpointReferences");
                will(returnValue(null));
                exactly(2).of(mockVHostReg).setProperties(with(any(Dictionary.class)));
            }
        });

        // add the default host after the other host (might happen..)
        defaulthost.activate(mockComponentContext, buildMap("default_host", Arrays.asList(new String[0])));
        Assert.assertSame("VirtualHost should be retrieved by port.. ", defaulthost, VirtualHostMap.findVirtualHost("endpoint", wildcard8080));

        // remove the default host before the other host (might happen..)
        defaulthost.deactivate(mockComponentContext, 0);
        Assert.assertNull("VirtualHost should not be retrieved by port.. (no default virtual host)", VirtualHostMap.findVirtualHost("endpoint", wildcard8080));

        // Stop the endpoint before deactivate: the virtual host will still be in the map..
        VirtualHostMap.notifyStopped(mockEndpoint, "*", 8080, false);
        Assert.assertNotNull("VirtualHost is still associated with the port.. (listener stopped)", VirtualHostMap.findVirtualHost("endpoint", helper8080));

        // now deactivate..
        vhost.deactivate(mockComponentContext, 0);
        Assert.assertNull("VirtualHost should not be retrieved by port.. (no virtual hosts)", VirtualHostMap.findVirtualHost("endpoint", helper8080));
        HttpEndpointList.unregisterEndpoint(mockEndpoint);
    }

    @Test
    public void testAddRemoveEndpoint() {
        setUpOneActivation();
        setUpOneActivation();
        setUpOneActivation();
        final HttpEndpointImpl mockEndpointABC = context.mock(HttpEndpointImpl.class, "endpoint-a.b.c");
        final HttpEndpointImpl mockEndpointDEF = context.mock(HttpEndpointImpl.class, "endpoint-d.e.f");

        context.checking(new Expectations() {
            {
                allowing(mockEndpointABC).getName();
                will(returnValue("endpoint-a.b.c"));
                allowing(mockEndpointDEF).getName();
                will(returnValue("endpoint-d.e.f"));

                allowing(mockEndpointABC).getResolvedHostName();
                will(returnValue("a.b.c"));
                // registration of virtual host service w/ httpsAlias
                allowing(mockEndpointABC).getHostName();
                will(returnValue("a.b.c"));
                allowing(mockEndpointABC).getPid();
                will(returnValue("a.b.c.pid"));

                // initialization of vhost discriminator (check known endpoints for active listeners)
                allowing(mockEndpointABC).getListeningHttpPort();
                will(returnValue(-1));

                // 1) initialization of vhost discriminator
                // 2) registration of virtual host service w/ httpsAlias
                // 3) notification when vhost is removed
                allowing(mockEndpointABC).getListeningSecureHttpPort();
                will(returnValue(8443));

                // registration of virtual host service w/ httpsAlias
                allowing(mockEndpointDEF).getHostName();
                will(returnValue("d.e.f"));
                allowing(mockEndpointDEF).getPid();
                will(returnValue("d.e.f.pid"));

                allowing(mockVHostRef).getProperty("aliases");
                will(returnValue(new String[] { "a.b.c:8443" }));

                allowing(mockVHostRef).getProperty("endpointReferences");
                will(returnValue(null));

                allowing(mockVHostRef).getProperty("httpsAlias");
                will(returnValue(null));

                allowing(mockVHostReg).setProperties(with(any(Dictionary.class)));
            }
        });

        TestRequestHelper helper8443 = new TestRequestHelper("a.b.c", 8443);
        TestRequestHelper other8443 = new TestRequestHelper("d.e.f", 8443);
        TestRequestHelper default8443 = new TestRequestHelper("g.h.i", 8443);

        // Set up a pre-existing endpoint
        HttpEndpointList.registerEndpoint(mockEndpointABC);

        VirtualHostImpl vhost = new VirtualHostImpl();
        VirtualHostImpl vhost2 = new VirtualHostImpl();
        VirtualHostImpl defaulthost = new VirtualHostImpl();

        // register virtual hosts with aliases...
        vhost.activate(mockComponentContext, buildMap("vhost1", Arrays.asList(new String[] { "a.b.c:8443" })));
        vhost2.activate(mockComponentContext, buildMap("vhost2", Arrays.asList(new String[] { "d.e.f:8443" })));
        // This puts the default_host in as a regular virtual host, not as the default/catch-all.
        defaulthost.activate(mockComponentContext, buildMap("default_host", Arrays.asList(new String[] { "*:8443" })));

        // This endpoint is in the list of registered endpoints already..
        VirtualHostMap.notifyStarted(mockEndpointABC, "a.b.c", 8443, true);
        Assert.assertSame("VirtualHost should be retrieved by port.. ", vhost, VirtualHostMap.findVirtualHost("endpoint", helper8443));

        VirtualHostMap.notifyStarted(mockEndpointDEF, "d.e.f", 8443, true);
        Assert.assertSame("VirtualHost should be retrieved by port.. ", vhost2, VirtualHostMap.findVirtualHost("endpoint", other8443));

        VirtualHostMap.notifyStopped(mockEndpointABC, "a.b.c", 8443, true);
        VirtualHostMap.notifyStopped(mockEndpointDEF, "d.e.f", 8443, true);

        vhost.deactivate(mockComponentContext, 0);
        vhost2.deactivate(mockComponentContext, 0);
        defaulthost.deactivate(mockComponentContext, 0);

        HttpEndpointList.unregisterEndpoint(mockEndpointABC);
        HttpEndpointList.unregisterEndpoint(mockEndpointDEF);
    }

    @Test
    public void testAddRemoveDefaultHostAndEndpoint() throws Exception {
        setUpOneActivation();
        context.checking(new Expectations() {
            {
                allowing(mockEndpoint).getPid();
                will(returnValue("endpoint.pid"));

                allowing(mockEndpoint).getResolvedHostName();
                will(returnValue("a.b.c"));

                allowing(mockEndpoint).getListeningHttpPort();
                will(returnValue(8080));

                allowing(mockVHostRef).getProperty("aliases");
                will(returnValue(null));

                allowing(mockVHostRef).getProperty("endpointReferences");
                will(returnValue(null));

                allowing(mockVHostRef).getProperty("httpsAlias");
                will(returnValue(null));

                allowing(mockVHostReg).setProperties(with(any(Dictionary.class)));

                allowing(mockEndpoint).getListeningSecureHttpPort();
                will(returnValue(-1));
            }
        });

        TestRequestHelper helper8080 = new TestRequestHelper("a.b.c", 8080);
        TestRequestHelper helper8443 = new TestRequestHelper("a.b.c", 8443);

        // Clear the alternateHostSelector..
        Field f = VirtualHostMap.class.getDeclaredField("alternateHostSelector");
        f.setAccessible(true);
        f.set(null, StaticValue.createStaticValue(null));

        HttpEndpointList.registerEndpoint(mockEndpoint);
        VirtualHostImpl vhost = new VirtualHostImpl();

        // register the vhost with an alias FIRST
        vhost.activate(mockComponentContext, buildMap("default_host", Arrays.asList(new String[] { "*:8080" })));

        VirtualHostMap.notifyStarted(mockEndpoint, "*", 8080, false);
        Assert.assertNotNull("VirtualHost should be retrieved by port.. ", VirtualHostMap.findVirtualHost("endpoint", helper8080));

        VirtualHostMap.notifyStarted(mockEndpoint, "*", 8443, true);
        Assert.assertNull("VirtualHost should not be retrieved by port.. (explicit bind to 8080)", VirtualHostMap.findVirtualHost("endpoint", helper8443));

        VirtualHostMap.notifyStopped(mockEndpoint, "*", 8080, false);
        vhost.deactivate(mockComponentContext, 0);
        VirtualHostMap.notifyStopped(mockEndpoint, "*", 8443, true);

        Assert.assertNull("VirtualHost should not be retrieved by port.. (listener stopped)", VirtualHostMap.findVirtualHost("endpoint", helper8080));
        Assert.assertNull("VirtualHost should not be retrieved by port.. (listener stopped)", VirtualHostMap.findVirtualHost("endpoint", helper8443));

        HttpEndpointList.unregisterEndpoint(mockEndpoint);
    }

    @Test
    public void testAddRemoveMultipleVHosts() throws Exception {

        final VirtualHostImpl vhost1 = new VirtualHostImpl();
        final VirtualHostImpl vhost2 = new VirtualHostImpl();
        final VirtualHostImpl vhost3 = new VirtualHostImpl();

        final VirtualHostListener mockListener = context.mock(VirtualHostListener.class);
        final ServiceReference<VirtualHostListener> mockListenRef = context.mock(ServiceReference.class, "VirtualHostListener");
        final HttpContainer mockContainer = context.mock(HttpContainer.class);

        context.checking(new Expectations() {
            {
                exactly(3).of(mockListenRef).getProperty("service.id");
                will(returnValue(6L));

                exactly(3).of(mockListenRef).getProperty("service.ranking");
                will(returnValue(6L));

                exactly(3).of(mockComponentContext).locateService("listener", mockListenRef);
                will(returnValue(mockListener));

                exactly(3).of(mockComponentContext).getBundleContext();
                will(returnValue(mockBundleContext));

                exactly(3).of(mockBundleContext).registerService(with(any(Class.class)), with(any(VirtualHostImpl.class)), with(any(Dictionary.class)));
                will(returnValue(mockVHostReg));

                exactly(3).of(mockVHostReg).unregister();

                allowing(mockVHostRef).getProperty("aliases");
                will(returnValue(null));

                allowing(mockVHostRef).getProperty("endpointReferences");
                will(returnValue(null));

                allowing(mockVHostRef).getProperty("httpsAlias");
                will(returnValue(null));

                allowing(mockVHostReg).setProperties(with(any(Dictionary.class)));

                allowing(mockEndpoint).getHostName();
                will(returnValue("*"));

                allowing(mockEndpoint).getListeningHttpPort();
                will(returnValue(-1));

                allowing(mockEndpoint).getListeningSecureHttpPort();
                will(returnValue(8443));

                allowing(mockEndpoint).getResolvedHostName();
                will(returnValue("a.b.c"));

                allowing(mockEndpoint).getPid();
                will(returnValue("a.b.c.pid"));

                one(mockListener).contextRootAdded("/x", vhost1);
                one(mockListener).contextRootRemoved("/x", vhost1);
                one(mockListener).contextRootAdded("/y", vhost2);
                one(mockListener).contextRootRemoved("/y", vhost2);
                one(mockListener).contextRootAdded("/z", vhost3);
                one(mockListener).contextRootRemoved("/z", vhost3);
            }
        });

        TestRequestHelper helper8443 = new TestRequestHelper("a.b.c", 8443);

        System.out.println("## 1 ");
        VirtualHostMap.dump(System.out);
        HttpEndpointList.registerEndpoint(mockEndpoint);

        System.out.println("## 2");
        VirtualHostMap.notifyStarted(mockEndpoint, "a.b.c", 8443, true);

        // activate + notifyStarted --> notification to listener of x added
        System.out.println("## 3");
        vhost1.setListener(mockListenRef);

        System.out.println("## 4");
        vhost1.activate(mockComponentContext, buildMap("default_host", null));

        System.out.println("## 5");
        vhost1.addContextRoot("/x", mockContainer);
        VirtualHostMap.dump(System.out);
        Assert.assertTrue("Should see message about addition of context root x", outputMgr.checkForMessages("CWWKT0016I.*https://a.b.c:8443/x"));
        VirtualHostImpl vh = VirtualHostMap.findVirtualHost("endpoint", helper8443);
        Assert.assertNotNull("VirtualHost should be retrieved by port.. ", vh);
        Assert.assertSame("Should retrieve default_host for port ", vhost1, vh);

        System.out.println("## 6");
        // activate of wildcard vhost means removal of port from default host: there is only one endpoint,
        // which means we see a notification of removal of context x
        vhost2.activate(mockComponentContext, buildMap("vhost2", Arrays.asList(new String[] { "*:8443" })));
        VirtualHostMap.dump(System.out);
        Assert.assertTrue("Should see message about removal of context root x", outputMgr.checkForMessages("CWWKT0017I.*https://a.b.c:8443/x"));
        vh = VirtualHostMap.findVirtualHost("endpoint", helper8443);
        Assert.assertNotNull("VirtualHost should be retrieved by port.. ", vh);
        Assert.assertSame("Should retrieve vhost2 for port ", vhost2, vh);

        System.out.println("## 7");
        vhost3.activate(mockComponentContext, buildMap("vhost3", Arrays.asList(new String[] { "a.b.c:8443" })));

        System.out.println("## 8");
        VirtualHostMap.dump(System.out);

        System.out.println("## 9");
        vhost2.addContextRoot("/y", mockContainer);
        System.out.println("## 10");
        vhost2.setListener(mockListenRef);

        System.out.println("## 11");
        vhost3.addContextRoot("/z", mockContainer);
        System.out.println("## 12");
        vhost3.setListener(mockListenRef);

        System.out.println("## 13");
        VirtualHostMap.notifyStopped(mockEndpoint, "a.b.c", 8443, true);

        System.out.println("## 14");
        vhost3.deactivate(mockComponentContext, 0);
        System.out.println("## 15");
        vhost2.deactivate(mockComponentContext, 0);
        System.out.println("## 16");
        vhost1.deactivate(mockComponentContext, 0);
        Assert.assertNull("VirtualHost should not be retrieved by port.. (endpoint/vhosts stopped)", VirtualHostMap.findVirtualHost("endpoint", helper8443));

        System.out.println("## 17");
        HttpEndpointList.unregisterEndpoint(mockEndpoint);
    }

    @Test
    public void testThreadedAddRemoveMultipleVHosts() throws Exception {
        final CountDownLatch start = new CountDownLatch(2);
        final CountDownLatch middle = new CountDownLatch(2);
        final CountDownLatch stop = new CountDownLatch(2);
        final CountDownLatch allDone = new CountDownLatch(2);

        final VirtualHostImpl vhost1 = new VirtualHostImpl();
        final VirtualHostImpl vhost2 = new VirtualHostImpl();
        final VirtualHostImpl vhost3 = new VirtualHostImpl();

        final VirtualHostListener mockListener = context.mock(VirtualHostListener.class);
        final ServiceReference<VirtualHostListener> mockListenRef = context.mock(ServiceReference.class, "VirtualHostListener");
        final HttpContainer mockContainer = context.mock(HttpContainer.class);

        final ConcurrentLinkedQueue<Throwable> throwables = new ConcurrentLinkedQueue<Throwable>();
        HttpEndpointList.registerEndpoint(mockEndpoint);

        context.checking(new Expectations() {
            {
                allowing(mockListenRef).getProperty("service.id");
                will(returnValue(6L));

                allowing(mockListenRef).getProperty("service.ranking");
                will(returnValue(6L));

                allowing(mockComponentContext).locateService("listener", mockListenRef);
                will(returnValue(mockListener));

                allowing(mockComponentContext).getBundleContext();
                will(returnValue(mockBundleContext));

                atLeast(1).of(mockEndpoint).getHostName();
                will(returnValue("*"));

                atLeast(1).of(mockEndpoint).getPid();
                will(returnValue("a.b.c.pid"));

                one(mockEndpoint).getListeningHttpPort();
                will(returnValue(-1));

                atLeast(1).of(mockEndpoint).getListeningHttpPort();
                will(returnValue(8080));

                one(mockEndpoint).getListeningSecureHttpPort();
                will(returnValue(-1));

                atLeast(3).of(mockEndpoint).getListeningSecureHttpPort();
                will(returnValue(8443));

                atLeast(2).of(mockEndpoint).getResolvedHostName();
                will(returnValue("a.b.c"));

                exactly(3).of(mockBundleContext).registerService(with(any(Class.class)), with(any(VirtualHostImpl.class)), with(any(Dictionary.class)));
                will(returnValue(mockVHostReg));

                exactly(3).of(mockVHostReg).unregister();

                allowing(mockVHostReg).getReference();
                will(returnValue(mockVHostRef));

                allowing(mockVHostRef).getProperty("aliases");
                will(returnValue(null));

                allowing(mockVHostRef).getProperty("endpointReferences");
                will(returnValue(null));

                allowing(mockVHostRef).getProperty("endpoints");
                will(returnValue(null));

                allowing(mockVHostRef).getProperty("httpsAlias");
                will(returnValue(null));

                allowing(mockVHostReg).setProperties(with(any(Dictionary.class)));

                // The x context root is added to the default host.
                // There may be more messages about this context being added or removed
                // depending on when vhost2 (*:8443) is added/removed relative to
                // the other port (*:8080).
                atLeast(1).of(mockListener).contextRootAdded("/x", vhost1);
                atLeast(1).of(mockListener).contextRootRemoved("/x", vhost1);

                // We now notify context root listeners even if there are zero associated 
                // ports; that can cause additional (new) notifications for /y and /z 
                atLeast(1).of(mockListener).contextRootAdded("/y", vhost2);
                atLeast(1).of(mockListener).contextRootRemoved("/y", vhost2);

                atLeast(1).of(mockListener).contextRootAdded("/z", vhost3);
                atLeast(1).of(mockListener).contextRootRemoved("/z", vhost3);
            }
        });

        // Clear the alternateHostSelector..
        Field f = VirtualHostMap.class.getDeclaredField("alternateHostSelector");
        f.setAccessible(true);
        f.set(null, StaticValue.createStaticValue(null));

        final Runnable r1 = new Runnable() {
            @Override
            public void run() {
                try {
                    vhost1.setListener(mockListenRef);
                    vhost3.setListener(mockListenRef);

                    waitFor(start);

                    // Addition/removal of VirtualHosts
                    vhost1.activate(mockComponentContext, buildMap("default_host", null));
                    sleep(5);

                    vhost1.addContextRoot("/x", mockContainer);
                    // vhost3 is a specific host on the http port (shared with default_host, which has the wildcard)
                    Thread.yield();
                    vhost3.activate(mockComponentContext, buildMap("vhost3", Arrays.asList(new String[] { "a.b.c:8080" })));
                    sleep(5);

                    // vhost2 is wildcard on the secure port, steals the https host from default_host entirely.
                    vhost2.activate(mockComponentContext, buildMap("vhost2", Arrays.asList(new String[] { "*:8443" })));
                    Thread.yield();
                    vhost2.addContextRoot("/y", mockContainer);
                    sleep(5);

                    vhost3.addContextRoot("/z", mockContainer);
                    Thread.yield();
                    vhost2.setListener(mockListenRef);

                    waitFor(middle);
                    waitFor(stop);
                    vhost3.deactivate(mockComponentContext, 0);
                    Thread.yield();
                    vhost2.deactivate(mockComponentContext, 0);
                    Thread.yield();
                    vhost1.deactivate(mockComponentContext, 0);
                } catch (Throwable t) {
                    t.printStackTrace();
                    throwables.add(t);
                } finally {
                    // make sure other thread isn't stuck, and allow test to finish
                    start.countDown();
                    stop.countDown();
                    allDone.countDown();
                }
            }
        };

        Thread t1 = new Thread(r1, "t1");
        t1.setDaemon(true); // allow JVM to exit if we deadlock...
        t1.start();

        final Runnable r2 = new Runnable() {
            @Override
            public void run() {
                try {
                    // Addition/removal of HttpEndpoint ports..
                    waitFor(start);

                    HttpEndpointList.registerEndpoint(mockEndpoint);
                    VirtualHostMap.notifyStarted(mockEndpoint, "a.b.c", 8443, true);
                    Thread.yield();

                    VirtualHostMap.notifyStarted(mockEndpoint, "a.b.c", 8080, false);
                    sleep(2);

                    VirtualHostMap.notifyStopped(mockEndpoint, "a.b.c", 8080, false);

                    sleep(5);
                    VirtualHostMap.notifyStarted(mockEndpoint, "a.b.c", 8080, false);

                    waitFor(middle);
                    // want to be stable when looking up virtual hosts just so we can make sure
                    // that we can assert a definite answer..
                    VirtualHostImpl result = VirtualHostMap.findVirtualHost("endpoint", new TestRequestHelper("e.f", 8443));
                    Assert.assertSame("VirtualHost 2 should be retrieved with wildcard host", vhost2, result);

                    result = VirtualHostMap.findVirtualHost("endpoint", new TestRequestHelper("e.f", 8080));
                    Assert.assertSame("Default host (vhost1) should be retrieved: no exact hostname match", vhost1, result);
                    result = VirtualHostMap.findVirtualHost("endpoint", new TestRequestHelper("a.b.c", 8080));
                    Assert.assertSame("vhost3should be retrieved with hostname match", vhost3, result);

                    waitFor(stop);
                    VirtualHostMap.notifyStopped(mockEndpoint, "a.b.c", 8080, false);
                    Thread.yield();
                    VirtualHostMap.notifyStopped(mockEndpoint, "a.b.c", 8443, true);
                    HttpEndpointList.unregisterEndpoint(mockEndpoint);
                } catch (Throwable t) {
                    t.printStackTrace();
                    throwables.add(t);
                } finally {
                    // make sure other thread isn't stuck, and allow test to finish
                    start.countDown();
                    stop.countDown();
                    allDone.countDown();
                }
            }
        };

        Thread t2 = new Thread(r2, "t2");
        t2.setDaemon(true); // allow JVM to exit if we deadlock...
        t2.start();

        // Wait at most 10 minutes for the above to sort itself out (which is a ridiculous amount of time)
        allDone.await(10, TimeUnit.MINUTES);
        if (allDone.getCount() > 0) {
            // well, someone got hung up along the way
            System.out.println("TIMEOUT... Thread 1: ");
            printStackTrace(t1.getStackTrace());
            System.out.println("TIMEOUT... Thread 2: ");
            printStackTrace(t2.getStackTrace());

            t1.interrupt();
            t2.interrupt();
            Assert.fail("Timeout waiting for test to complete");
        }
        HttpEndpointList.unregisterEndpoint(mockEndpoint);
        Assert.assertTrue("List of throwables should be empty: " + throwables, throwables.isEmpty());
    }

    private void printStackTrace(StackTraceElement[] ste) {
        if (ste != null) {
            for (StackTraceElement s : ste) {
                System.out.println("\tat " + s);
            }
        }
    }

    private void waitFor(CountDownLatch c) {
        try {
            c.countDown();
            System.out.println(Thread.currentThread().getName() + " waiting for " + c);
            c.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail("Interrupted for some reason while waiting for both threads to be created");
        }
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    private Map<String, Object> buildMap(String name, List<String> aliases) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", name);
        map.put(HttpServiceConstants.VHOST_HOSTALIAS, aliases);
        map.put(HttpServiceConstants.ENABLED, true);

        return map;
    }

    private Map<String, Object> buildMapWithEndpoint(String name, List<String> aliases, Collection<String> allowFromEndpoints) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", name);
        map.put(HttpServiceConstants.VHOST_HOSTALIAS, aliases);
        map.put(HttpServiceConstants.VHOST_ALLOWED_ENDPOINT, allowFromEndpoints);
        map.put(HttpServiceConstants.ENABLED, true);

        return map;
    }

    static class TestRequestHelper implements RequestHelper {
        final String host;
        final int port;

        TestRequestHelper(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public String getRequestedHost() {
            return host;
        }

        @Override
        public int getRequestedPort() {
            return port;
        }

        @Override
        public String toString() {
            return host + ":" + port;
        }
    }
}
