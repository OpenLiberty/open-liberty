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
package com.ibm.ws.session.utils;

/**
 * This interface allows the session manager creator to plug in his own
 * implementation of the Session ID Generator. The default id generator
 * generates
 * ids that are compatible with the base HTTP Session manager in WebSphere.
 * <p>
 * 
 * @author Aditya Desai
 * 
 */
public interface IIDGenerator {

    /**
     * Returns a new unique session id
     * <p>
     * 
     * @return String
     */
    public String getID();
}