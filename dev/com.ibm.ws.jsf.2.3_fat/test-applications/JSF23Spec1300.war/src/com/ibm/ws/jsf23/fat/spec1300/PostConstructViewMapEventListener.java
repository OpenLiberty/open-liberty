/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
package com.ibm.ws.jsf23.fat.spec1300;

import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PostConstructViewMapEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

/**
 * A listener that listens for the PostConstructViewMapEvent.
 */
public class PostConstructViewMapEventListener implements SystemEventListener {

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.event.SystemEventListener#isListenerForSource(java.lang.Object)
     */
    @Override
    public boolean isListenerForSource(Object source) {
        boolean retVal = false;

        // According to the JavaDoc for the PostConstructViewMapEvent the source
        // for the event UIViewRoot.
        if (source instanceof UIViewRoot) {
            retVal = true;
        }
        FacesContext.getCurrentInstance().getExternalContext().log("isListenerForSource: " + retVal);

        return retVal;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.event.SystemEventListener#processEvent(javax.faces.event.SystemEvent)
     */
    @Override
    public void processEvent(SystemEvent event) {
        ExternalContext context = event.getFacesContext().getExternalContext();

        context.log("processEvent...");
        if (event instanceof PostConstructViewMapEvent) {
            context.log("PostConstructViewMapEventListener processEvent invoked for PostConstructViewMapEvent!!: " + event);
        }

    }

}
