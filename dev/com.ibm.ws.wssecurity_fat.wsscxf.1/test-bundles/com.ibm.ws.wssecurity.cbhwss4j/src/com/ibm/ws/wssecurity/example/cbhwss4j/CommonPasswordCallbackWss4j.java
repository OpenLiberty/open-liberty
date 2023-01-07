/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.ibm.ws.wssecurity.example.cbhwss4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;

/**
 */
public class CommonPasswordCallbackWss4j implements CallbackHandler {

    private final Map<String, String> userPasswords = new HashMap<String, String>();

    private final Map<String, String> signaturePasswords = new HashMap<String, String>();

    private final Map<String, String> encryptPasswords = new HashMap<String, String>();

    //
    //  This class will be instanced in every WSSecurity related request.
    //  So, be careful about the performnace.
    //  In fat, we may need the ID and password to be changed every time.
    //  But may not be in a product environment
    //
    public CommonPasswordCallbackWss4j() {
        String strPath = System.getProperty("server.config.dir");
        File filePasswd = null;
        boolean bFileExist = false;
        if (strPath != null) {
            try {
                filePasswd = new File(strPath + "/PasswordCallBack.props");
                bFileExist = filePasswd.exists();
            } catch (Exception e) {
            }
        }
        if (bFileExist) {

            try {
                InputStreamReader inputStream = new InputStreamReader(new FileInputStream(filePasswd));
                BufferedReader dataStream = new BufferedReader(inputStream);

                String strTmp = null;
                while ((strTmp = dataStream.readLine()) != null) {
                    if (!putUserPass(userPasswords, "userPasswords.", strTmp)) {
                        if (!putUserPass(signaturePasswords, "signaturePasswords.", strTmp)) {
                            if (!putUserPass(encryptPasswords, "encryptPasswords.", strTmp)) {
                                System.out.println("A String is not handled in CommonPasswordCallbackWss4j:'" + strTmp + "'");
                            }
                        }
                    }
                }
                dataStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else { // No property file exists. Using the original UserID and password
            userPasswords.put("user1", "security");
            userPasswords.put("user2", "security");
            userPasswords.put("test1", "test1");
            userPasswords.put("test2", "test2");
            userPasswords.put("test3", "test3");
            userPasswords.put("test4", "test4");
            userPasswords.put("admin", "admin");
            userPasswords.put("joe", "joe");
            userPasswords.put("user1pw", "pw8cbh"); // Its password is different from server.xml

            userPasswords.put("Alice", "ecilA");
            userPasswords.put("Frank", "knarF");
            userPasswords.put("abcd", "dcba");
            userPasswords.put("alice", "password");
            userPasswords.put("bob", "password");
            userPasswords.put("cxfca", "password");

            // do not distinguish encrypt or signature for now
            signaturePasswords.put("x509ClientDefault", "KLibertyX509Client");
            signaturePasswords.put("x509clientdefault", "KLibertyX509Client");
            signaturePasswords.put("x509ServerDefault", "KLibertyX509Server");
            signaturePasswords.put("x509serverdefault", "KLibertyX509Server");
            signaturePasswords.put("x509ClientSecond", "KLibertyX509Client2");
            signaturePasswords.put("x509clientsecond", "KLibertyX509Client2");
            signaturePasswords.put("x509ServerSecond", "KLibertyX509Server2");
            signaturePasswords.put("x509serversecond", "KLibertyX509Server2");
            signaturePasswords.put("soapprovider", "server");
            signaturePasswords.put("soaprequester", "client");
            signaturePasswords.put("bob", "keypass");
            signaturePasswords.put("alice", "keypass");

            encryptPasswords.put("x509ClientDefault", "KLibertyX509Client");
            encryptPasswords.put("x509clientdefault", "KLibertyX509Client");
            encryptPasswords.put("x509ServerDefault", "KLibertyX509Server");
            encryptPasswords.put("x509serverdefault", "KLibertyX509Server");
            encryptPasswords.put("x509ClientSecond", "KLibertyX509Client2");
            encryptPasswords.put("x509clientsecond", "KLibertyX509Client2");
            encryptPasswords.put("x509ServerSecond", "KLibertyX509Server2");
            encryptPasswords.put("x509serversecond", "KLibertyX509Server2");
            encryptPasswords.put("bob", "keypass");
            encryptPasswords.put("alice", "keypass");

        }

    }

    /**
     * Here, we attempt to get the password from the private
     * alias/passwords map.
     */
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        System.out.println("From CommonPasswordCallbackWss4j.java, using the callbacks:" + callbacks);
        for (int i = 0; i < callbacks.length; i++) {
            WSPasswordCallback pwcb = (WSPasswordCallback) callbacks[i];
            String id = pwcb.getIdentifier();
            System.out.println("From CommonPasswordCallbackWss4j.java: id:" + id);
            String pass = null;

            switch (pwcb.getUsage()) {
                //case WSPasswordCallback.USERNAME_TOKEN_UNKNOWN:  // deprecated
                case WSPasswordCallback.UNKNOWN:
                case WSPasswordCallback.USERNAME_TOKEN:

                    System.out.println("From CommonPasswordCallbackWss4j.java: Getting userPassword id:" + id);
                    pass = userPasswords.get(id);
                    if (pass != null) {
                        pwcb.setPassword(pass);
                        return;
                    }
                    break;

                case WSPasswordCallback.DECRYPT:
                    System.out.println("From CommonPasswordCallbackWss4j.java: Getting decryptPassword id:" + id);
                    pass = encryptPasswords.get(id);
                    if (pass != null) {
                        System.out.println("From CommonPasswordCallbackWss4j.java: encryptPassword:" + pass);
                        pwcb.setPassword(pass);
                        return;
                    }

                    break;

                case WSPasswordCallback.SIGNATURE:

                    System.out.println("From CommonPasswordCallbackWss4j.java: Getting signaturePassword id:" + id);
                    pass = signaturePasswords.get(id);
                    if (pass != null) {
                        pwcb.setPassword(pass);
                        System.out.println("From CommonPasswordCallbackWss4j.java: signaturePassword:" + pass);
                        return;
                    }
                    break;
                default:
                    System.out.println("From CommonPasswordCallbackWss4j.java: Password not handled id:" + id + "  type:" + pwcb.getUsage());
                    break;
            }
        }
    }

    /**
     * Add an alias/password pair to the callback mechanism.
     */
    public void setAliasPassword(String alias, String password) {
        userPasswords.put(alias, password);
    }

    boolean putUserPass(Map<String, String> mapPasswords, String strKeyWord, String strEntry) {
        int index = strEntry.indexOf(strKeyWord);
        if (index >= 0) {
            String strTmp = strEntry.substring(index + strKeyWord.length());
            int indexEqual = strTmp.indexOf("=");
            if (indexEqual >= 0) {
                String strKey = strTmp.substring(0, indexEqual).trim();
                String strValue = strTmp.substring(indexEqual + 1).trim();
                mapPasswords.put(strKey, strValue);
                return true;
            }
        }
        return false;
    }
}