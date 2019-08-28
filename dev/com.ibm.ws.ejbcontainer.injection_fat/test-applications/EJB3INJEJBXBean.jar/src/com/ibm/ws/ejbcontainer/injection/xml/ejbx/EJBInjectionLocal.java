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
 * Local interface with methods to verify Environment Injection.
 **/
public interface EJBInjectionLocal {
    /**
     * Verify EJB Injection (field or method) occurred properly.
     **/
    public String verifyEJB30Injection(int testpoint);

    /**
     * Verify No EJB Injection (field or method) occurred when
     * an method is called using an instance from the pool (sl) or cache (sf).
     **/
    public String verifyNoEJB30Injection(int testpoint);

    /**
     * Verify EJB Injection (field or method) occurred properly.
     **/
    public String verifyEJB21Injection(int testpoint);

    /**
     * Verify No EJB Injection (field or method) occurred when
     * an method is called using an instance from the pool (sl) or cache (sf).
     **/
    public String verifyNoEJB21Injection(int testpoint);

    /**
     * Verify EJB Injection (field or method) occurred properly.
     **/
    public String verifyAdvEJB30Injection(int testpoint);

    /**
     * Verify EJB Injection (field or method) occurred properly.
     **/
    public String verifyAdvEJB21Injection(int testpoint);
}
