/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.event.internal.adapter;

import static com.ibm.ws.event.internal.adapter.EventMatcher.anEventMatching;

import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

public class BundleEventAdapterTest {

    @Rule
    public final JUnitRuleMockery context = new JUnitRuleMockery();

    @Test
    public void testBundleChangedInstalled() {
        testBundleEvent(BundleEvent.INSTALLED, "INSTALLED");
    }

    @Test
    public void testBundleChangedStarted() {
        testBundleEvent(BundleEvent.STARTED, "STARTED");
    }

    @Test
    public void testBundleChangedStopped() {
        testBundleEvent(BundleEvent.STOPPED, "STOPPED");
    }

    @Test
    public void testBundleChangedUpdated() {
        testBundleEvent(BundleEvent.UPDATED, "UPDATED");
    }

    @Test
    public void testBundleChangedUninstalled() {
        testBundleEvent(BundleEvent.UNINSTALLED, "UNINSTALLED");
    }

    @Test
    public void testBundleChangedResolved() {
        testBundleEvent(BundleEvent.RESOLVED, "RESOLVED");
    }

    @Test
    public void testBundleChangedUnesolved() {
        testBundleEvent(BundleEvent.UNRESOLVED, "UNRESOLVED");
    }

    @Test
    public void testUnknownBundleEvent() {
        final Bundle bundle = context.mock(Bundle.class);
        context.checking(new Expectations() {
            {
            }
        });

        final EventAdmin eventAdmin = context.mock(EventAdmin.class);
        context.checking(new Expectations() {
            {
            }
        });

        BundleEventAdapter adapter = new BundleEventAdapter(eventAdmin);
        adapter.bundleChanged(new BundleEvent(Integer.MIN_VALUE, bundle));
    }

    private void testBundleEvent(int bundleEventId, String bundleEventName) {
        final Bundle bundle = context.mock(Bundle.class);
        context.checking(new Expectations() {
            {
                allowing(bundle).getSymbolicName();
                will(returnValue("test.bundle.symbolic.name"));
                allowing(bundle).getBundleId();
                will(returnValue(Long.MAX_VALUE));
            }
        });

        final String topic = "org/osgi/framework/BundleEvent/" + bundleEventName;
        final Map<String, Object> eventProperties = new HashMap<String, Object>();
        eventProperties.put(EventConstants.BUNDLE_ID, Long.valueOf(Long.MAX_VALUE));
        eventProperties.put(EventConstants.BUNDLE_SYMBOLICNAME, "test.bundle.symbolic.name");
        eventProperties.put(EventConstants.BUNDLE, bundle);
        final Event event = new Event(topic, eventProperties);

        final EventAdmin eventAdmin = context.mock(EventAdmin.class);
        context.checking(new Expectations() {
            {
                oneOf(eventAdmin).postEvent(with(anEventMatching(event)));
            }
        });

        BundleEventAdapter adapter = new BundleEventAdapter(eventAdmin);
        adapter.bundleChanged(new BundleEvent(bundleEventId, bundle));
    }
}
