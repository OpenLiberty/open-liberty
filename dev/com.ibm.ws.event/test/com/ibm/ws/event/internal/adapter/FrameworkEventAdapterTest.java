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
import org.osgi.framework.FrameworkEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

public class FrameworkEventAdapterTest {

    @Rule
    public final JUnitRuleMockery context = new JUnitRuleMockery();

    @Test
    public void testFrameworkEventStarted() {
        testFrameworkEvent(FrameworkEvent.STARTED, "STARTED");
    }

    @Test
    public void testFrameworkEventError() {
        testFrameworkEvent(FrameworkEvent.ERROR, "ERROR");
    }

    @Test
    public void testFrameworkEventPackagesRefreshed() {
        testFrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED, "PACKAGES_REFRESHED");
    }

    @Test
    public void testFrameworkEventStartLevelChanged() {
        testFrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, "STARTLEVEL_CHANGED");
    }

    @Test
    public void testFrameworkEventWarning() {
        testFrameworkEvent(FrameworkEvent.WARNING, "WARNING");
    }

    @Test
    public void testFrameworkEventInfo() {
        testFrameworkEvent(FrameworkEvent.INFO, "INFO");
    }

    @Test
    public void testFrameworkEventUnknown() {
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

        FrameworkEventAdapter adapter = new FrameworkEventAdapter(eventAdmin);
        adapter.frameworkEvent(new FrameworkEvent(Integer.MIN_VALUE, bundle, new Throwable("Test Exception")));
    }

    private void testFrameworkEvent(int frameworkEventId, String frameworkEventName) {
        final Bundle bundle = context.mock(Bundle.class);
        context.checking(new Expectations() {
            {
                allowing(bundle).getBundleId();
                will(returnValue(Long.MAX_VALUE));
                allowing(bundle).getSymbolicName();
                will(returnValue("test.bundle.symbolic.name"));
            }
        });

        final String topic = "org/osgi/framework/FrameworkEvent/" + frameworkEventName;
        final Exception exception = new Exception("Test Exception for " + frameworkEventName);
        final Map<String, Object> eventProperties = new HashMap<String, Object>();
        eventProperties.put(EventConstants.BUNDLE_ID, Long.valueOf(Long.MAX_VALUE));
        eventProperties.put(EventConstants.BUNDLE_SYMBOLICNAME, "test.bundle.symbolic.name");
        eventProperties.put(EventConstants.BUNDLE, bundle);
        eventProperties.put(EventConstants.EXCEPTION, exception);
        eventProperties.put(EventConstants.EXCEPTION_CLASS, exception.getClass().getName());
        eventProperties.put(EventConstants.EXCEPTION_MESSAGE, exception.getMessage());
        final Event event = new Event(topic, eventProperties);

        final EventAdmin eventAdmin = context.mock(EventAdmin.class);
        context.checking(new Expectations() {
            {
                oneOf(eventAdmin).postEvent(with(anEventMatching(event)));
            }
        });
        FrameworkEventAdapter adapter = new FrameworkEventAdapter(eventAdmin);
        adapter.frameworkEvent(new FrameworkEvent(frameworkEventId, bundle, exception));
    }
}
