/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client;

import javax.security.auth.Subject;

/**
 * Duplicate (to avoid split package issues) of same interface in
 * com.ibm.ws.jaxrs.2.0.client project.
 */
public interface MpJwtPropagation {

    /*
     * Retrieve the JsonWebToken from the subject and return it as a String
     */
    public String getJsonWebTokenPrincipal(Subject subject);

}