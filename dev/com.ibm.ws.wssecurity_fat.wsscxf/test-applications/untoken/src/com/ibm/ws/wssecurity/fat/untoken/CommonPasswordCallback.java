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

//from CD-Open package basicplcy.wssecfvt.test;
package com.ibm.ws.wssecurity.fat.untoken;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;

/**
 */
public class CommonPasswordCallback implements CallbackHandler {

    private final Map<String, String> passwords = new HashMap<String, String>();

    public CommonPasswordCallback() {
        passwords.put("user1", "security");
        passwords.put("user2", "security");
        passwords.put("test1", "test1");
        passwords.put("test2", "test2");
        passwords.put("test3", "test3");
        passwords.put("test4", "test4");
        passwords.put("admin", "admin");
        passwords.put("joe", "joe");
        passwords.put("user1pw", "pw8cbh"); // Its password is different from server.xml

        passwords.put("Alice", "ecilA");
        passwords.put("Frank", "knarF");
        passwords.put("abcd", "dcba");
        passwords.put("alice", "password");
        passwords.put("bob", "password");
        passwords.put("cxfca", "password");
    }

    /**
     * Here, we attempt to get the password from the private
     * alias/passwords map.
     */
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];

            String pass = passwords.get(pc.getIdentifier());
            if (pass != null) {
                pc.setPassword(pass);
                return;
            }
        }
    }

    /**
     * Add an alias/password pair to the callback mechanism.
     */
    public void setAliasPassword(String alias, String password) {
        passwords.put(alias, password);
    }
}
