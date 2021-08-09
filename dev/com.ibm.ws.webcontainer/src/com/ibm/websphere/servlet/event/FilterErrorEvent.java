/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.event;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

/**
 * Event that reports a filter error.
 * 
 * @ibm-api
 */

public class FilterErrorEvent extends FilterEvent {

	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	private Throwable _error;

	/**
	 * @param source
	 * @param filterConfig
	 */
	public FilterErrorEvent(Object source, FilterConfig filterConfig, Throwable error) {
		super(source, filterConfig);
		_error = error;
	}
	
    /**
     * Returns the top-level error.
     */
    public Throwable getError() {
        return _error;
    }

    /**
     * Get the original cause of the error.
     * Use of ServletExceptions by the engine to rethrow errors
     * can cause the original error to be buried within one or more
     * exceptions.  This method will sift through the wrapped ServletExceptions
     * to return the original error.
     */
    public Throwable getRootCause() {
        Throwable root = getError();
        while(true) {
            if(root instanceof ServletException) {
                ServletException se = (ServletException)_error;
                Throwable seRoot = se.getRootCause();
                if(seRoot == null) {
                    return root;
                }
                else if(seRoot.equals(root)) {//prevent possible recursion
                    return root;
                }
                else {
                    root = seRoot;
                }
            }
            else {
                return root;
            }
        }
    }


}
