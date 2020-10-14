/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.ola;

import javax.ejb.Local;

/**
 * The local EJB interface for WOLA EJBs.  All WOLA EJBs in Liberty must
 * implement this interface.
 */
@Local
public interface ExecuteLocalBusiness {

    /**
     * Executes the WOLA EJB request.
     *
     * @param data - request data
     * @return byte[] - response data
     * 
     */
    public byte[] execute(byte[] input);
}
