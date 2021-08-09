/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.event.internal;

import java.util.ArrayDeque;
import java.util.Deque;

import com.ibm.websphere.event.Event;

public class CurrentEvent {

    private final static ThreadLocal<Deque<EventImpl>> currentEvent = new ThreadLocal<Deque<EventImpl>>()
    {
        public Deque<EventImpl> initialValue()
    {
        return new ArrayDeque<EventImpl>();
    }
    };

    public static Event get() {
        return currentEvent.get().peekFirst();
    }

    static void push(EventImpl event) {
        currentEvent.get().addFirst(event);
    }

    static EventImpl pop() {
        return currentEvent.get().pollFirst();
    }
}
