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
package com.ibm.ws.jsf22.fat.actionlistener.listener;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionListener;
import javax.faces.event.ActionListenerWrapper;

public class TestActionListenerWrapper extends ActionListenerWrapper {

    @Override
    public ActionListener getWrapped() {
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        ec.log("TestActionListenerWrapper.getWrapped()");
        return new TestActionListener();
    }
}
