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
package com.ibm.websphere.security.auth;

import javax.security.auth.login.LoginException;

/**
 * This exception is thrown whenever authentication fails.
 * 
 * @author IBM
 * @version 1.0
 * @ibm-api
 */
public class WSLoginFailedException extends LoginException {

    /**
     * <p>
     * A constructor that accepts an error message. The error message can be retrieved
     * using the getMessage() API.
     * </p>
     * 
     * @param errorMessage An error message.
     */
    public WSLoginFailedException(String errorMessage) {
        super(errorMessage);
    }

}
