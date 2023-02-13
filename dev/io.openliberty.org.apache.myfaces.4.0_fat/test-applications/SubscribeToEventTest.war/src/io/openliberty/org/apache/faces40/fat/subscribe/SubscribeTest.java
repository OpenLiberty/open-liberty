/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.subscribe;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.event.ComponentSystemEventListener;
import jakarta.faces.event.PreRenderViewEvent;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;
import jakarta.inject.Named;

/**
 * A test class that runs the applicable tests on the server to ensure SPEC behavior change for UIComponent#subscribeToEvent()
 */
@Named(value = "subscribeTest")
@RequestScoped
public class SubscribeTest {

    public String getTestResults() {
        //Test event
        final Class<PreRenderViewEvent> event = PreRenderViewEvent.class;
        final ComponentSystemEventListener myListener = new MyTestListener();

        //Get context - shouldn't ever return null, but test just in case.
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return "false - FacesContext null";
        }

        //Get UIComponent - shouldn't ever return null, but test just in case.
        UIComponent cc = UIComponent.getCurrentComponent(ctx);
        if (cc == null) {
            return "false - UIComponent null";
        }

        try {
            //UIComponent#getListenersForEventClass() may never return null
            testGetListenersForEventClass(cc, event);

            //UIComponent#subscribeToEvent() must precheck if given listener is not already installed
            for (int i = 1; i < 4; i++) {
                try {
                    testSubscribeToEvent(cc, event, myListener);
                } catch (AssertionError ae) {
                    throw new AssertionError("Failed testSubscribeToEvent after " + i + " attempt(s) with message: " + ae.getLocalizedMessage());
                }
            }

            //UIComponent#unsubscribeToEvent should remove listeners
            testUnsubscribeToEvent(cc, event, myListener);
        } catch (AssertionError ae) {
            return Boolean.toString(false) + " - " + ae.getLocalizedMessage();
        }

        return Boolean.toString(true);
    }

    /**
     * Ensure UIComponent has no system event listeners for a given event and does not return null.
     *
     * @param cc - the UI Component
     * @param event - the SystemEvent class
     * @throws AssertionError if test fails
     */
    private void testGetListenersForEventClass(UIComponent cc, Class<? extends SystemEvent> event) throws AssertionError {
        List<SystemEventListener> subscribeList = cc.getListenersForEventClass(event);
        if (subscribeList == null || !subscribeList.isEmpty()) {
            throw new AssertionError("testGetListenersForEventClass subscribeList was either null, or not empty: " + subscribeList);
        }
    }

    /**
     * Ensure UIComponent checks to make sure listener does not duplicate install.
     *
     * @param cc - the UIComponent object
     * @param event - the SystemEvent class
     * @param listener - the ComponentSystemEventListener object
     * @throws AssertionError if test fails
     */
    private void testSubscribeToEvent(UIComponent cc, Class<? extends SystemEvent> event, ComponentSystemEventListener listener) throws AssertionError {
        cc.subscribeToEvent(event, listener);
        List<SystemEventListener> subscribeList = cc.getListenersForEventClass(event);
        if (subscribeList == null || subscribeList.size() != 1) {
            throw new AssertionError("testSubscribeToEvent subscribeList was either null, or contained more than one listener: " + subscribeList);
        }
    }

    /**
     * For completeness check to make sure listener is unsubscribed successfully.
     *
     * @param cc - the UIComponent object
     * @param event - the SystemEvent class
     * @param listener - the ComponentSystemEventListener object
     * @throws AssertionError if test fails
     */
    private void testUnsubscribeToEvent(UIComponent cc, Class<? extends SystemEvent> event, ComponentSystemEventListener listener) throws AssertionError {
        cc.unsubscribeFromEvent(event, listener);
        List<SystemEventListener> subscribeList = cc.getListenersForEventClass(event);
        if (subscribeList == null || !subscribeList.isEmpty()) {
            throw new AssertionError("testUnsubscribeToEvent subscribeList was either null, or not empty: " + subscribeList);
        }
    }

    /**
     * Fake listener for use in testing
     */
    public static class MyTestListener implements ComponentSystemEventListener {
        @Override
        public void processEvent(ComponentSystemEvent arg0) throws AbortProcessingException {
            //DO NOTHING
        }
    }

}
