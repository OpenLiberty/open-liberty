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
package com.ibm.ws.jsf22.fat.viewaction.phaselistener;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

public class RenderResponsePhaseListener implements PhaseListener {

    private static final long serialVersionUID = 1L;

    private PhaseId phaseId = PhaseId.RENDER_RESPONSE;

    public void beforePhase(PhaseEvent event) {
        FacesContext.getCurrentInstance().addMessage(null,
                                                     new FacesMessage("PhaseListener Message: PhaseId.getName(): " + getPhaseId().getName() + " PhaseId.phaseIdValueOf(): "
                                                                      + getPhaseId().phaseIdValueOf("RENDER_RESPONSE")));
    }

    public void afterPhase(PhaseEvent event) {}

    public PhaseId getPhaseId() {
        return phaseId;
    }
}