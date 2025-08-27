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
package org.apache.cxf.ws.security.wss4j;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Validator;

public class CXFRequestData extends RequestData {

    private static Map<QName, String> validatorKeys = new HashMap<>();

    static {
        validatorKeys.put(WSConstants.SAML_TOKEN, SecurityConstants.SAML1_TOKEN_VALIDATOR);
        validatorKeys.put(WSConstants.SAML2_TOKEN, SecurityConstants.SAML2_TOKEN_VALIDATOR);
        validatorKeys.put(WSConstants.USERNAME_TOKEN, SecurityConstants.USERNAME_TOKEN_VALIDATOR);
        validatorKeys.put(WSConstants.SIGNATURE, SecurityConstants.SIGNATURE_TOKEN_VALIDATOR);
        validatorKeys.put(WSConstants.TIMESTAMP, SecurityConstants.TIMESTAMP_TOKEN_VALIDATOR);
        validatorKeys.put(WSConstants.BINARY_TOKEN, SecurityConstants.BST_TOKEN_VALIDATOR);
        validatorKeys.put(WSConstants.SECURITY_CONTEXT_TOKEN_05_02, SecurityConstants.SCT_TOKEN_VALIDATOR);
        validatorKeys.put(WSConstants.SECURITY_CONTEXT_TOKEN_05_12, SecurityConstants.SCT_TOKEN_VALIDATOR);
    }

    public CXFRequestData() {
    }

    public Validator getValidator(QName qName) throws WSSecurityException {
        String key = validatorKeys.get(qName);
        if (key != null && this.getMsgContext() != null) {
            Object o = ((SoapMessage)this.getMsgContext()).getContextualProperty(key);
            try {
                if (o instanceof Validator) {
                    return (Validator)o;
                } else if (o instanceof Class) {
                    return (Validator)((Class<?>)o).getDeclaredConstructor().newInstance();
                } else if (o instanceof String) {
                    return (Validator)ClassLoaderUtils.loadClass(o.toString(),
                                                                 CXFRequestData.class)
                        .getDeclaredConstructor().newInstance();
                } else if (o != null) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                                  "Cannot load Validator: " + o);
                }
            } catch (RuntimeException t) {
                throw t;
            } catch (Exception ex) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
            }
        }
        return super.getValidator(qName);
    }
}

