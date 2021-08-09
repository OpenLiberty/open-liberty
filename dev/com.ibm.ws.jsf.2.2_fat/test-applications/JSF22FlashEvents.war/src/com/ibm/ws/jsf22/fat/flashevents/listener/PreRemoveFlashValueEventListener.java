/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.flashevents.listener;

import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

/*
 * This is a custom SystemEventListener which is configured in the faces-config to 
 * be called during a javax.faces.event.PreRemoveFlashValueEvent event.
 * It contains a counter which will be checked after each test to ensure it was called the expected amount of times
 * However, we currently do not see this Event being called. (same behavior in Mojarra).
 */

public class PreRemoveFlashValueEventListener implements SystemEventListener {

    private int counter;

    public PreRemoveFlashValueEventListener() {
        counter = 0;
    }

    @Override
    public void processEvent(SystemEvent event) throws AbortProcessingException {
        counter++;
        FacesContext.getCurrentInstance().getExternalContext().log("PreRemoveFlashValueEvent processEvent - counter: " + counter);
    }

    @Override
    public boolean isListenerForSource(Object value) {
        return true;
    }
}