/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.auth.callback;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.jaas.common.callback.AuthenticationHelper;
import com.ibm.wsspi.security.auth.callback.WSAppContextCallback;

/**
 * <p>
 * The <code>WSCallbackHandlerImpl</code> allows authentication data to be pushed to
 * the login module, i.e. without prompting for the data.
 * </p>
 * 
 * <p>
 * Supported <code>Callback</code>s:
 * <ul>
 * <li><code>javax.security.auth.callback.NameCallback</code></li>
 * <li><code>javax.security.auth.callback.PasswordCallback</code></li>
 * <li><code>com.ibm.websphere.security.auth.callback.WSCredTokenCallbackImpl</code></li>
 * <li><code>com.ibm.websphere.security.auth.callback.WSRealmNameCallbackImpl</code></li>
 * <li><code>com.ibm.wsspi.security.auth.callback.WSAppContextCallback</code></li>
 * </ul>
 * </p>
 * 
 * @ibm-api
 * @author IBM Corporation
 * @version 1.0
 * @see javax.security.auth.callback.NameCallback
 * @see javax.security.auth.callback.PasswordCallback
 * @see com.ibm.websphere.security.auth.callback.WSRealmNameCallbackImpl
 * @see com.ibm.websphere.security.callback.WSCredTokenCallbackImpl
 * @see com.ibm.wsspi.security.auth.callback.WSAppContextCallback
 * @since 1.0
 * @ibm-spi
 */
@SuppressWarnings("unchecked")
public class WSCallbackHandlerImpl implements CallbackHandler {

    private final static TraceComponent tc = Tr.register(WSCallbackHandlerImpl.class);

    private String userName;
    private String realm;
    private String password;
    private byte[] credToken;
    private java.util.Map appContext;

    /**
     * <p>
     * Push the username and password to login module.
     * </p>
     * 
     * @param userName The user name of the principal.
     * @param password The password in clear text.
     */
    public WSCallbackHandlerImpl(String userName, @Sensitive String password) {
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
     * <p>
     * In the client container, the realm is ignored and instead the
     * realm from the target server is used.
     * </p>
     * 
     * @param userName The user name of the principal.
     * @param realmName The realm name, if different from the current realm; ignored on the client container
     * @param password The password in clear text.
     */
    public WSCallbackHandlerImpl(String userName, String realmName, @Sensitive String password) {
        this.userName = userName;
        this.password = password;
        this.realm = realmName;
    }

    /**
     * <p>
     * Push the username and password to login module. The realmName is only
     * needed if it's different from the current realm. If you intend to login
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
     * <p>
     * In the client container, the realm is ignored and instead the
     * realm from the target server is used.
     * </p>
     * 
     * @param userName The user name of the principal.
     * @param realmName The realm name, if different from the current realm; ignored on the client container
     * @param password The password in clear text.
     * @param appContext A java.util.Map containing naming properties to validate userid/password.
     */
    public WSCallbackHandlerImpl(String userName, String realmName, @Sensitive String password, java.util.Map appContext) {
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
    public WSCallbackHandlerImpl(@Sensitive byte[] credToken) {
        this.credToken = AuthenticationHelper.copyCredToken(credToken);
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

        if ((callbacks == null) || ((len = callbacks.length) == 0)) {
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer();
            sb.append("{ ");
            for (int i = 0; i < len; i++) {
                sb.append(callbacks[i].getClass().getName());
                if (i < (len - 1)) {
                    sb.append(", ");
                }
            }
            sb.append(" }");
            Tr.debug(tc, "handle(callbacks = \"" + sb.toString() + "\")");
        }

        for (int i = 0; i < len; i++) {
            Callback c = callbacks[i];

            if (c instanceof javax.security.auth.callback.NameCallback) {
                ((javax.security.auth.callback.NameCallback) c).setName(userName);
            } else if (c instanceof javax.security.auth.callback.PasswordCallback) {
                ((javax.security.auth.callback.PasswordCallback) c).setPassword((password == null) ? null : password.toCharArray());
            } else if (c instanceof WSCredTokenCallbackImpl) {
                ((WSCredTokenCallbackImpl) c).setCredToken(credToken);
            } else if (c instanceof WSRealmNameCallbackImpl) {
                ((WSRealmNameCallbackImpl) c).setRealmName(realm);
            } else if (c instanceof WSAppContextCallback) {
                ((WSAppContextCallback) c).setContext(appContext);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Un-use handle(callbacks = \"" + callbacks[i].getClass().getName() + "\")");
                }
            }
        }
    }

}
