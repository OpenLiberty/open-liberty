/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.injection.xml.ejbo;

/**
 * Basic Stateless Bean implementation for testing ejb-link of EJB Injection via XML
 **/
public class OtherIdenticalBean {
    private static final String CLASS_NAME = OtherIdenticalBean.class.getName();

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

    public OtherIdenticalBean() {
        // intentionally blank
    }
}
