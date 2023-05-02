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
package org.apache.ws.security;

import java.security.Key;

import javax.security.auth.callback.Callback;

import org.w3c.dom.Element;
/**
 * This interface serves as a way to to mitigate the migration from WSS4J 1.6 to 2.0 as required by
 * the migration of CXF from 2.6.2 to 3.4. We modify it to be a interface rather than a class in order 
 * for the 2.0 version, which has a different package name, to implement it. Meaning that users running an application
 * with a custom callback handler based off of the org.apache.ws.security.WSPasswordCallback can continue running their
 * applications and have the updated code. 
 */
// Liberty Change: Interface
public interface WSPasswordCallback extends Callback {

    /**
     * An unknown usage. Never used by the WSS4J implementation and should be treated
     * as an error.
     */
    public static final int UNKNOWN = 0;

    /**
     * DECRYPT usage is used when the calling code needs a password to get the private key of
     * this identifier (alias) from a keystore. This is only used for the inbound case of
     * decrypting a session (symmetric) key, and not for the case of getting a private key to
     * sign the message. The CallbackHandler must set the password via the setPassword(String)
     * method.
     */
    public static final int DECRYPT = 1;

    /**
     * USERNAME_TOKEN usage is used to obtain a password for either creating a Username Token,
     * or for validating it. It is also used for the case of deriving a key from a Username Token.
     * The CallbackHandler must set the password via the setPassword(String) method.
     */
    public static final int USERNAME_TOKEN = 2;

    /**
     * SIGNATURE usage is used on the outbound side only, to get a password to get the private
     * key of this identifier (alias) from a keystore. The CallbackHandler must set the password
     * via the setPassword(String) method.
     */
    public static final int SIGNATURE = 3;

    /**
     * SECURITY_CONTEXT_TOKEN usage is for the case of when we want the CallbackHandler to
     * supply the key associated with a SecurityContextToken. The CallbackHandler must set
     * the key via the setKey(byte[]) method.
     */
    public static final int SECURITY_CONTEXT_TOKEN = 6;

    /**
     * CUSTOM_TOKEN usage is used for the case that we want the CallbackHandler to supply a
     * token as a DOM Element. For example, this is used for the case of a reference to a
     * SAML Assertion or Security Context Token that is not in the message. The CallbackHandler
     * must set the token via the setCustomToken(Element) method.
     */
    public static final int CUSTOM_TOKEN = 7;

    /**
     * SECRET_KEY usage is used for the case that we want to obtain a secret key for encryption
     * or signature on the outbound side, or for decryption or verification on the inbound side.
     * The CallbackHandler must set the key via the setKey(byte[]) method.
     */
    public static final int SECRET_KEY = 9;

    /**
     * PASSWORD_ENCRYPTOR_PASSWORD usage is used to return the password used with a PasswordEncryptor
     * implementation to decrypt encrypted passwords stored in Crypto properties files
     */
    public static final int PASSWORD_ENCRYPTOR_PASSWORD = 10;
    
    public String getIdentifier();
    
    public void setIdentifier(String ident);
    
    public void setPassword(String passwd);
    
    public String getPassword();
    
    public void setKey(byte[] secret);
    
    public void setKey(Key key);
    
    public byte[] getKey();
    
    public Key getKeyObject();
    
    public int getUsage();
    
    public String getType();
    
    public Element getCustomToken();
    
    public void setCustomToken(Element customToken);
    
    public String getAlgorithm();
    
    public void setAlgorithm(String algorithm);
    
    public Element getKeyInfoReference();
    
    public void setKeyInfoReference(Element keyInfoReference);
    

}
