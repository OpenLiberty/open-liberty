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

package org.apache.wss4j.dom.handler;

import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.dom.WSConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * This class defines the names, actions, and other string for the deployment
 * data of the WS handler.
 */
public final class WSHandlerConstants extends ConfigurationConstants {

    private WSHandlerConstants() {
        super();
    }

    /**
     * Perform no action.
     */
    public static final String NO_SECURITY = "NoSecurity";

    /**
     * This is an alternative to specifying an "action" String. This Object should be a
     * list of HandlerAction objects, which associate an "action" Integer with a corresponding
     * SecurityActionToken object. This allows for more control over keys etc. used for
     * different actions.
     */
    public static final String HANDLER_ACTIONS = "handlerActions";

    /**
     * Set the value of this parameter to true to treat passwords as binary values
     * for Username Tokens. The default value is "false".
     *
     * This is needed to properly handle password equivalence for UsernameToken
     * passwords.  Binary passwords are Base64 encoded so they can be treated as
     * strings in most places, but when the password digest is calculated or a key
     * is derived from the password, the password will be Base64 decoded before
     * being used. This is most useful for hashed passwords as password equivalents.
     */
    public static final String USE_ENCODED_PASSWORDS = "useEncodedPasswords";

    //
    // Internal storage constants
    //

    /**
     * The WSHandler stores a result <code>List</code> in this property.
     */
    public static final String RECV_RESULTS = "RECV_RESULTS";

    /**
     * internally used property names to store values inside the message context
     * that must have the same lifetime as a message (request/response model).
     */
    public static final String SEND_SIGV = "_sendSignatureValues_";

    /**
     *
     */
    public static final String SIG_CONF_DONE = "_sigConfDone_";

    /**
     * Define the parameter values to set the key identifier types. These are:
     * <ul>
     * <li><code>DirectReference</code> for {@link WSConstants#BST_DIRECT_REFERENCE}
     * </li>
     * <li><code>IssuerSerial</code> for {@link WSConstants#ISSUER_SERIAL}
     * </li>
     * <li><code>IssuerSerialQuoteFormat</code> for {@link WSConstants#ISSUER_SERIAL_QUOTE_FORMAT}
     * </li>
     * <li><code>X509KeyIdentifier</code> for {@link WSConstants#X509_KEY_IDENTIFIER}
     * </li>
     * <li><code>SKIKeyIdentifier</code> for {@link WSConstants#SKI_KEY_IDENTIFIER}
     * </li>
     * <li><code>Thumbprint</code> for {@link WSConstants#THUMBPRINT}
     * </li>
     * <li><code>EncryptedKeySHA1</code> for {@link WSConstants#ENCRYPTED_KEY_SHA1_IDENTIFIER}
     * </li>
     * </ul>
     * See {@link #SIG_KEY_ID} {@link #ENC_KEY_ID}.
     */
    private static Map<String, Integer> keyIdentifier = new HashMap<>();

    static {
        keyIdentifier.put("DirectReference", WSConstants.BST_DIRECT_REFERENCE);
        keyIdentifier.put("IssuerSerial", WSConstants.ISSUER_SERIAL);
        keyIdentifier.put("IssuerSerialQuoteFormat", WSConstants.ISSUER_SERIAL_QUOTE_FORMAT);
        keyIdentifier.put("X509KeyIdentifier", WSConstants.X509_KEY_IDENTIFIER);
        keyIdentifier.put("SKIKeyIdentifier", WSConstants.SKI_KEY_IDENTIFIER);
        keyIdentifier.put("Thumbprint", WSConstants.THUMBPRINT_IDENTIFIER);
        keyIdentifier.put("EncryptedKeySHA1", WSConstants.ENCRYPTED_KEY_SHA1_IDENTIFIER);
        keyIdentifier.put("KeyValue", WSConstants.KEY_VALUE);
    }

    /**
     * Get the key identifier type corresponding to the parameter. This is intended for internal
     * use only. Valid values for "parameter" are:
     *  - "IssuerSerial"
     *  - "IssuerSerialQuoteFormat"
     *  - "DirectReference"
     *  - "X509KeyIdentifier"
     *  - "Thumbprint"
     *  - "SKIKeyIdentifier"
     *  - "KeyValue"
     *  - "EmbeddedKeyName"
     *  - "EncryptedKeySHA1"
     *
     * @param parameter
     * @return the key identifier type corresponding to the parameter
     */
    public static Integer getKeyIdentifier(String parameter) {
        return keyIdentifier.get(parameter);
    }
}

