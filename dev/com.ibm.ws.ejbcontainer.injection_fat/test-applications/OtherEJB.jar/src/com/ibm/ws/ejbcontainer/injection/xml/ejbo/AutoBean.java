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

package com.ibm.ws.ejbcontainer.injection.xml.ejbo;

/**
 * Basic Stateless Bean implementation for testing auto-link of EJB Injection via XML
 **/
public class AutoBean {
    /**
     * Verify injected EJB is an auto bean
     **/
    public boolean isAuto() {
        return true;
    }

    public AutoBean() {
        // intentionally blank
    }
}
