/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.security.internal;

import javax.ejb.EJBAccessException;

/**
 *
 */
public class EJBAccessDeniedException extends EJBAccessException {

    /**
     * Create a new EJBAccessDeniedException with an empty description string. <p>
     */
    public EJBAccessDeniedException() {

    }

    /**
     * Create a new EJBAccessDeniedException with the associated string description. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     */
    public EJBAccessDeniedException(String s) {

        super(s);

    }
}
