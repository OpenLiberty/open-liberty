/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.xml.ejbx;

/**
 * Basic Stateless Bean implementation for testing auto-link of EJB Injection via XML
 **/
public class IdenticalBean {
    private static final String CLASS_NAME = IdenticalBean.class.getName();

    /**
     * Verify injected EJB is an identical bean
     **/
    public boolean isIdentical() {
        return true;
    }

    /**
     * Verify injected EJB is the expected bean
     **/
    public String getBeanName() {
        return CLASS_NAME;
    }

    public IdenticalBean() {
        // intentionally blank
    }
}
