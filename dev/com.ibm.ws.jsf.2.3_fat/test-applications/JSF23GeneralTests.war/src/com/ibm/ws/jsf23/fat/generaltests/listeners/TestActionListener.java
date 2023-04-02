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
package com.ibm.ws.jsf23.fat.generaltests.listeners;

import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

/**
 * An ActionListener that logs some information when invoked.
 */
public class TestActionListener implements ActionListener {

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.event.ActionListener#processAction(javax.faces.event.ActionEvent)
     */
    @Override
    public void processAction(ActionEvent arg0) throws AbortProcessingException {
        // Use the FacesEvent.getFacesContext() method new to JSF 2.3.
        arg0.getFacesContext().getExternalContext().log("TestActionListener processAction invoked!!");
    }

}
