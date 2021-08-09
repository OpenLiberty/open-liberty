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
package com.ibm.ws.jsf23.fat.phaselistener.exception;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

/**
 * A PhaseListener implementation that throws a runtime exception in
 * the beforePhase method for the RENDER_RESPONSE phase of the JSF lifecycle
 * if the throwExceptionInPhaseListener context-parameter is set to true.
 */
public class RenderResponsePhaseListener implements PhaseListener {

    /**  */
    private static final long serialVersionUID = 1L;

    boolean throwExceptionInPhaseListener = Boolean.valueOf(FacesContext.getCurrentInstance().getExternalContext().getInitParameter("throwExceptionInPhaseListener"));

    public RenderResponsePhaseListener() {
        // do nothing
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.event.PhaseListener#afterPhase(javax.faces.event.PhaseEvent)
     */
    @Override
    public void afterPhase(PhaseEvent event) {
        if (event.getPhaseId() == PhaseId.RENDER_RESPONSE) {
            event.getFacesContext().getExternalContext().log("afterPhase: RENDER_RESPONSE");
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.event.PhaseListener#beforePhase(javax.faces.event.PhaseEvent)
     */
    @Override
    public void beforePhase(PhaseEvent event) {
        if (event.getPhaseId() == PhaseId.RENDER_RESPONSE) {
            event.getFacesContext().getExternalContext().log("afterPhase: RENDER_RESPONSE: throwExceptionInPhaseListener: " + throwExceptionInPhaseListener);
            if (throwExceptionInPhaseListener) {
                throw new RuntimeException("This is a test runtime exception beforePhase RENDER_RESPONSE!");
            }
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.event.PhaseListener#getPhaseId()
     */
    @Override
    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }

}
