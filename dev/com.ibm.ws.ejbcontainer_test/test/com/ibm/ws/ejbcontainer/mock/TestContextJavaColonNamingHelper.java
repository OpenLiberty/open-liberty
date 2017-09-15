/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mock;

import javax.ejb.EJBContext;

import com.ibm.ws.ejbcontainer.osgi.internal.naming.ContextJavaColonNamingHelper;

public class TestContextJavaColonNamingHelper extends ContextJavaColonNamingHelper {

    private boolean ejbContextActive = false;
    private final EJBContext ejbContext;

    public TestContextJavaColonNamingHelper(EJBContext context) {
        ejbContext = context;
    }

    public void setEjbContextActive(boolean ejbContextActive) {
        this.ejbContextActive = ejbContextActive;
    }

    @Override
    protected boolean isEJBContextActive() {
        return ejbContextActive;
    }

    @Override
    protected EJBContext getEJBContext() {
        return ejbContext;
    }
}
