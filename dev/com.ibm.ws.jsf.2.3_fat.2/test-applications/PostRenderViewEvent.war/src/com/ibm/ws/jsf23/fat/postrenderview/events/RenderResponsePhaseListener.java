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
package com.ibm.ws.jsf23.fat.postrenderview.events;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

/**
 * A simple PhaseListener to log messages before and after the RENDER_RESPONSE phase
 * of the JSF lifecycle.
 */
public class RenderResponsePhaseListener implements PhaseListener {

    /**  */
    private static final long serialVersionUID = 1L;

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.event.PhaseListener#afterPhase(javax.faces.event.PhaseEvent)
     */
    @Override
    public void afterPhase(PhaseEvent event) {
        event.getFacesContext().getExternalContext().log("After Render Response Phase");

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.event.PhaseListener#beforePhase(javax.faces.event.PhaseEvent)
     */
    @Override
    public void beforePhase(PhaseEvent event) {
        event.getFacesContext().getExternalContext().log("Before Render Response Phase");

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.event.PhaseListener#getPhaseId()
     */
    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RENDER_RESPONSE;
    }

}
