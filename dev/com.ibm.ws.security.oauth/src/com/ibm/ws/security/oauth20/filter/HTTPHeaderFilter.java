/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.filter;

import javax.servlet.http.HttpServletRequest;

public interface HTTPHeaderFilter {

    /**
     * Pass the filter string so the implementation can read any of the
     * properties
     * 
     * @param filterString -
     *            set of rules to be used by the filter
     * @return true if no problem occured during parsing of filterString false
     *         otherwise
     */
    public abstract boolean init(String s1);

    /*
     * This method has two versions to make testing with JUnit easier. This is
     * the "real" method that takes an HttpServletRequest object. It just puts
     * it in a wrapper (allowing for inserting test drivers) and then calls the
     * real code.
     */
    public abstract boolean isAccepted(HttpServletRequest req);

    /**
     * Optionally use this method to indicate that all requests to this filter
     * will be processed.
     * 
     * @param -
     *            true will cause all calls to isAccepted() to return true
     */
    public abstract void setProcessAll(boolean b);

    /*
     * There is no filter defined
     */
    public abstract boolean noFilter();

}