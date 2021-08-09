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

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.osgi.service.event.Event;

public class EventMatcher extends TypeSafeMatcher<Event> {

    @Factory
    public static Matcher<Event> anEventMatching(Event event) {
        return new EventMatcher(event);
    }

    private Event event;

    public EventMatcher(Event event) {
        this.event = event;
    }

    public boolean matchesSafely(Event item) {
        if (event == item) {
            return true;
        }
        if (event != null && item == null) {
            return false;
        }
        if (event == null && item != null) {
            return false;
        }
        if (!event.getTopic().equals(item.getTopic())) {
            return false;
        }

        // Only validate the properties in the event we were populated with
        List<String> itemPropertyNames = Arrays.asList(item.getPropertyNames());
        for (String name : event.getPropertyNames()) {
            if (!itemPropertyNames.contains(name)) {
                return false;
            }

            Object value = event.getProperty(name);
            if (value.getClass().isArray()) {
                List<Object> eventValue = Arrays.asList((Object[]) value);
                List<Object> itemValue = Arrays.asList((Object[]) item.getProperty(name));
                if (!eventValue.equals(itemValue)) {
                    return false;
                }
            } else if (!event.getProperty(name).equals(item.getProperty(name))) {
                return false;
            }
        }

        return true;
    }

    public void describeTo(Description description) {
        description.appendText("an event matching ").appendValue(event).
                        appendText(" with properties ").appendValue(Arrays.asList(event.getPropertyNames()));
    }
}
