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
package com.ibm.ws.jsf22.fat.flashevents.factory;

import javax.faces.FacesWrapper;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.context.FlashWrapper;

/*
 * This is a custom Flash implementation which was created by the TestFlashFactory
 */

public class TestFlashImpl extends FlashWrapper implements FacesWrapper<Flash> {

    private final Flash parent;

    public TestFlashImpl(Flash parent) {
        this.parent = parent;
        getEC().log("TestFlashImpl constructor");

    }

    @Override
    public Flash getWrapped() {
        return parent;
    }

    private ExternalContext getEC() {
        return FacesContext.getCurrentInstance().getExternalContext();
    }
}