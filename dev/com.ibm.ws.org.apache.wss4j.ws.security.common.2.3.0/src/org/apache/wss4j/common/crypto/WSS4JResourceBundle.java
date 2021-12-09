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

package org.apache.wss4j.common.crypto;

import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.I18n;

import com.ibm.ws.ffdc.annotation.FFDCIgnore; //Liberty code change

/**
 * ResourceBundle for WSS4J
 */
public class WSS4JResourceBundle extends ResourceBundle {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(WSS4JResourceBundle.class);

    private final ResourceBundle wss4jSecResourceBundle;
    private final ResourceBundle xmlSecResourceBundle;

    public WSS4JResourceBundle() {
        wss4jSecResourceBundle = ResourceBundle.getBundle("messages.wss4j_errors");

        ResourceBundle tmpResourceBundle;
        try {
            tmpResourceBundle =
                ResourceBundle.getBundle(Constants.exceptionMessagesResourceBundleBase,
                        Locale.getDefault(),
                        I18n.class.getClassLoader());
        } catch (MissingResourceException ex) {
            // Using a Locale of which there is no properties file.
            LOG.debug(ex.getMessage());
            // Default to en/US
            tmpResourceBundle =
                ResourceBundle.getBundle(Constants.exceptionMessagesResourceBundleBase,
                                         new Locale("en", "US"), I18n.class.getClassLoader());
        }
        xmlSecResourceBundle = tmpResourceBundle;
    }

    @Override
    @FFDCIgnore(MissingResourceException.class) //Liberty code change
    protected Object handleGetObject(String key) {
        Object value = null;
        try {
            value = wss4jSecResourceBundle.getObject(key);
        } catch (MissingResourceException e) {
            try {
                value = xmlSecResourceBundle.getObject(key);
            } catch (MissingResourceException ex) { //NOPMD
                //ignore
            }
        }
        return value;
    }

    @Override
    public Enumeration<String> getKeys() {
        throw new UnsupportedOperationException("getKeys not supported");
    }


}
