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

package com.ibm.ws.security.client.fat.callbackhandler;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.ibm.websphere.security.auth.callback.WSCredTokenCallbackImpl;
import com.ibm.websphere.security.auth.callback.WSRealmNameCallbackImpl;
import com.ibm.ws.security.client.fat.Constants;
import com.ibm.wsspi.security.auth.callback.WSAppContextCallback;

public class ProgrammaticLoginCallbackHandlerNullPwd implements CallbackHandler {

    private String userName;
    private String realm;
    private String password;
    private byte[] credToken;
    private Map appContext;

    public ProgrammaticLoginCallbackHandlerNullPwd() { }

    /**
     * <p>
     * Push the username and password to login module.
     * </p>
     * 
     * @param userName The user name of the principal.
     * @param password The password in clear text.
     */
    public ProgrammaticLoginCallbackHandlerNullPwd(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    /**
     * <p>
     * Push the username and password to login module. The realmName is only
     * need if it's different from the current realm. If you intend to login
     * to the current realm, you do not need to specify a realm or you can
     * specify the current realm.
     * </p>
     * 
     * @param userName The user name of the principal.
     * @param realmName The realm name, if different from the current realm
     * @param password The password in clear text.
     */
    public ProgrammaticLoginCallbackHandlerNullPwd(String userName, String realmName, String password) {
        this.userName = userName;
        this.password = password;
        this.realm = realmName;
    }

    /**
     * <p>
     * Push the username and password to login module. The realmName is only
     * need if it's different from the current realm. If you intend to login
     * to the current realm, you do not need to specify a realm or you can
     * specify the current realm.
     * </p>
     * 
     * <p>
     * If the realm you specify is different from the current realm and you
     * want to validate the userid/password at the new target realm during
     * this login, then you must specify a java.util.Map which contains the
     * following properites:
     * javax.naming.Context.PROVIDER_URL
     * Example: "corbaloc:iiop:myhost.mycompany.com:2809"
     * javax.naming.Context.INITIAL_CONTEXT_FACTORY
     * Example: "com.ibm.websphere.naming.WsnInitialContextFactory"
     * Note: If the target server is not a WebSphere server, then do not
     * specify this information since it will not validate successfully. Also,
     * the target server must support CSIv2 in order for the userid/password
     * to be successfully sent.
     * </p>
     * 
     * @param userName The user name of the principal.
     * @param realmName The realm name, if different from the current realm
     * @param password The password in clear text.
     * @param appContext A java.util.Map containing naming properties to validate userid/password.
     */
    public ProgrammaticLoginCallbackHandlerNullPwd(String userName, String realmName, String password, Map appContext) {
        this.userName = userName;
        this.password = password;
        this.realm = realmName;
        this.appContext = appContext;
    }

    /**
     * <p>
     * Push Credential Token to login module. The Credential Token should be treated as
     * an opaque object. The credential token must be in the format recognized by
     * WebSphere Secure Association Service.
     * </p>
     * 
     * @param credToken The credential token.
     */
    public ProgrammaticLoginCallbackHandlerNullPwd(byte[] credToken) {
        return;
    }

    /**
     * <p>
     * This implementation of <code>CallbackHandler</code> pushes the data specified in the
     * constructor to the login module.
     * </p>
     * 
     * @param callbacks An array of <code>Callback</code> objects provided by the underlying
     *            security service which contains the information
     *            requested to be retrieved or displayed.
     * @exception IOException
     *                If an input or output error occurs.
     * @exception UnsupportedCallbackException
     *                If the implementation of this method does not support one or more of the
     *                <code>Callback</code>s specified in the callbacks parameter.
     */
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        int len = 0;

    	userName = Constants.USER_2;
    	password = null;
        System.out.println("Hard coding userName to " + userName + " and password to " + password);

        if ((callbacks == null) || ((len = callbacks.length) == 0)) {
            return;
        }

        StringBuffer sb = new StringBuffer();
        sb.append("{ ");
        for (int i = 0; i < len; i++) {
            sb.append(callbacks[i].getClass().getName());
            if (i < (len - 1)) {
                sb.append(", ");
            }
        }
        sb.append(" }");
        System.out.println("handle(callbacks = \"" + sb.toString() + "\")");

        for (int i = 0; i < len; i++) {
            Callback c = callbacks[i];

            if (c instanceof javax.security.auth.callback.NameCallback) {
                System.out.println("Setting name: " + userName);
                ((javax.security.auth.callback.NameCallback) c).setName(userName);
            } else if (c instanceof javax.security.auth.callback.PasswordCallback) {
                System.out.println("Setting password: " + password);
                ((javax.security.auth.callback.PasswordCallback) c).setPassword((password == null) ? null : password.toCharArray());
            } else if (c instanceof WSCredTokenCallbackImpl) {
                System.out.println("Setting cred token: " + credToken);
                ((WSCredTokenCallbackImpl) c).setCredToken(credToken);
            } else if (c instanceof WSRealmNameCallbackImpl) {
                System.out.println("Setting realm: " + realm);
                ((WSRealmNameCallbackImpl) c).setRealmName(realm);
            } else if (c instanceof WSAppContextCallback) {
                System.out.println("Setting app context");
                ((WSAppContextCallback) c).setContext(appContext);
            } else {
                System.out.println("Un-use handle(callbacks = \"" + c.getClass().getName() + "\")");
            }
        }
    }

}
