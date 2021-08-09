/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.ejbcontainer.jakarta.test.tools;

import javax.security.auth.login.LoginContext;

import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;

/**
 * FATSecurityHelper provides access to WAS security functionality.
 * This helper class is used to allow EJB container FAT test cases
 * to use websphere application server classes and methods that are not
 * available for applications to use.
 */
public abstract class FATSecurityHelper {
    public static LoginContext login(String user, String pass) throws Exception {
        LoginContext lCtx = new LoginContext(JaasLoginConfigConstants.APPLICATION_WSLOGIN, new WSCallbackHandlerImpl(user, pass));
        lCtx.login();

        return lCtx;
    }
}
