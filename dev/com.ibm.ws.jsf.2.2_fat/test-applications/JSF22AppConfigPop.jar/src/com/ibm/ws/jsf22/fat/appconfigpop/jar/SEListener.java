/*
 * Copyright (c)  2015  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.appconfigpop.jar;

import javax.faces.context.FacesContext;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

public class SEListener implements SystemEventListener {

    public SEListener() {
        // Do nothing
    }

    @Override
    public boolean isListenerForSource(Object source) {

        return true;
    }

    @Override
    public void processEvent(SystemEvent event) {
        FacesContext.getCurrentInstance().getExternalContext().log("JSF22:  AOP System event listener called.");

    }
}