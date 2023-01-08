/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi.internal.core.events.app;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/event")
public class CDIEventTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private Event<TestEvent> testEvent;

    @Inject
    private EventObserver eventObserver;

    @Test
    public void testEvent() {
        assertThat(eventObserver.getValues(), hasSize(0));

        testEvent.fire(new TestEvent("testValue"));

        assertThat(eventObserver.getValues(), contains("testValue"));
    }
}
