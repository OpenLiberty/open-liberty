/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.websphere.ejbcontainer.test.tools;

import javax.security.auth.login.LoginContext;

import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;

/**
 * FATSecurityHelper provides access to WAS security functionality.
 * This helper class is used to allow EJB container FAT test cases
 * to use websphere application server classes and methods that are not
 * available for applications to use.
 */
public abstract class FATSecurityHelper
{
    public static LoginContext login(String user, String pass) throws Exception {
        LoginContext lCtx = new LoginContext(JaasLoginConfigConstants.APPLICATION_WSLOGIN, new WSCallbackHandlerImpl(user, pass));
        lCtx.login();

        return lCtx;
    }
}
