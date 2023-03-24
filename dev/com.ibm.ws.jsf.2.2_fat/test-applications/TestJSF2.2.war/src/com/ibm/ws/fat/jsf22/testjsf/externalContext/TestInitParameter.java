/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.jsf22.testjsf.externalContext;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

/**
 * will check if NPE is thrown if name is null
 */
@ManagedBean
@RequestScoped
public class TestInitParameter {

    /**  */
    private final FacesContext context = FacesContext.getCurrentInstance();

    public boolean result = false;

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
