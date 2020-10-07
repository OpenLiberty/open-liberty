/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.servlets;

/**
 * Base interface for any type of URI.
 * 
 * <p>The only feature common to all URIs is that they can be cloned
 */
public interface BaseURI {

    /**
     * Returns a clone of this URI.
     * @param isProtected defines if the new instance must be from the protected class
     * @return URI a clone of this URI object
     */
    Object clone(boolean isProtected);
    
}
