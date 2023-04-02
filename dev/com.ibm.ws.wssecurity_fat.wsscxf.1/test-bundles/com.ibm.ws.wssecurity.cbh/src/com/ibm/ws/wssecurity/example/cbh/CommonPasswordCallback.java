/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.wssecurity.example.cbh;

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

import org.apache.ws.security.WSPasswordCallback;

/**
 */
public class CommonPasswordCallback implements CallbackHandler {

    private final Map<String, String> userPasswords = new HashMap<String, String>();

    private final Map<String, String> signaturePasswords = new HashMap<String, String>();

    private final Map<String, String> encryptPasswords = new HashMap<String, String>();

    //
    //  This class will be instanced in every WSSecurity related request.
    //  So, be careful about the performance.
    //  In fat, we may need the ID and password to be changed every time.
    //  But may not be in a product environment
    //
    public CommonPasswordCallback() {
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
                                System.out.println("A String is not handled in CommonPasswordCallback:'" + strTmp + "'");
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
        System.out.println("From CommonPasswordCallback.java, using the callbacks:" + callbacks);
        for (int i = 0; i < callbacks.length; i++) {
            WSPasswordCallback pwcb = (WSPasswordCallback) callbacks[i];
            String id = pwcb.getIdentifier();
            System.out.println("From CommonPasswordCallback.java: id:" + id);
            String pass = null;

            switch (pwcb.getUsage()) {
                //2/2021    
                //case WSPasswordCallback.USERNAME_TOKEN_UNKNOWN:  // deprecated
                case WSPasswordCallback.UNKNOWN:
                case WSPasswordCallback.USERNAME_TOKEN:

                    System.out.println("From CommonPasswordCallback.java: Getting userPassword id:" + id);
                    pass = userPasswords.get(id);
                    if (pass != null) {
                        pwcb.setPassword(pass);
                        return;
                    }
                    break;

                case WSPasswordCallback.DECRYPT:
                    System.out.println("From CommonPasswordCallback.java: Getting decryptPassword id:" + id);
                    pass = encryptPasswords.get(id);
                    if (pass != null) {
                        System.out.println("From CommonPasswordCallback.java: encryptPassword:" + pass);
                        pwcb.setPassword(pass);
                        return;
                    }

                    break;

                case WSPasswordCallback.SIGNATURE:

                    System.out.println("CommonPasswordCallback: Getting signaturePassword id:" + id);
                    pass = signaturePasswords.get(id);
                    if (pass != null) {
                        pwcb.setPassword(pass);
                        System.out.println("From CommonPasswordCallback.java: signaturePassword:" + pass);
                        return;
                    }
                    break;
                default:
                    System.out.println("From CommonPasswordCallback.java: Password not handled id:" + id + "  type:" + pwcb.getUsage());
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
