/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client;

import javax.security.auth.Subject;

/**
 *
 */

public interface MpJwtPropagation {

    /*
     * Retrieve the JsonWebToken from the subject and return it as a String
     */
    public String getJsonWebTokenPrincipal(Subject subject);

}
