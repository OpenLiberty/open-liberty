/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.generaltests.listeners;

import javax.faces.application.Application;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

/**
 * A SystemEventListener that logs some information for the PostConstructApplicationEvent.
 */
public class PostConstructApplicationEventListener implements SystemEventListener {

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.event.SystemEventListener#isListenerForSource(java.lang.Object)
     */
    @Override
    public boolean isListenerForSource(Object arg0) {
        boolean retVal = false;

        if (arg0 instanceof Application) {
            retVal = true;
        }

        return retVal;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.event.SystemEventListener#processEvent(javax.faces.event.SystemEvent)
     */
    @Override
    public void processEvent(SystemEvent arg0) {
        // Use the SystemEvent.getFacesContext() method new to JSF 2.3.
        arg0.getFacesContext().getExternalContext().log("PostConstructApplicationEventListener processEvent invoked!!");
    }

}
