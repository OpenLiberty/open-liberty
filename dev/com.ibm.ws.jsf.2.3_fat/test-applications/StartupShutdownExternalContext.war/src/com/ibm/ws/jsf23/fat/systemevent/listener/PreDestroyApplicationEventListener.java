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
package com.ibm.ws.jsf23.fat.systemevent.listener;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

/**
 * SystemEventListener for the PreDestoryApplicationEvent
 */
public class PreDestroyApplicationEventListener implements SystemEventListener {

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.event.SystemEventListener#isListenerForSource(java.lang.Object)
     */
    @Override
    public boolean isListenerForSource(Object arg0) {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.event.SystemEventListener#processEvent(javax.faces.event.SystemEvent)
     */
    @Override
    public void processEvent(SystemEvent arg0) {
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();

        // getRealPath()
        ec.log("JSF23: PreDestroyApplicationEvent getRealPath test: "
               + ec.getRealPath("index.xhtml"));
    }

}
