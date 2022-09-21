/*
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.simple.externalContext.faces40;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;

/**
 * will check if NPE is thrown if name is null
 */
@Named
@RequestScoped
public class TestInitParameter {

    /**  */
    private final FacesContext context = FacesContext.getCurrentInstance();

    private boolean result = false;

    /**
     * @throws Exception
     */
    public boolean testInitParam() throws Exception {

        String badValue = null;
        ExternalContext ec = context.getExternalContext();
        try {
            ec.getInitParameter(badValue);
        } catch (NullPointerException e) {
            result = true;
        }
        return result;
    }
}
