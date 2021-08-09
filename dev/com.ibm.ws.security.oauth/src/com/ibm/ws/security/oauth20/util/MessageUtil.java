/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.util;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class MessageUtil {
    private static Map<String, MessageUtil> instances = new HashMap<String, MessageUtil>();

    private Map<Locale, ResourceBundle> map = new HashMap<Locale, ResourceBundle>();
    private String bundleName = null;

    private MessageUtil(String bundleName) {
        this.bundleName = bundleName;
    }

    public static MessageUtil getInstance(String bundleName) {
        MessageUtil util = instances.get(bundleName);
        if (util == null) {
            synchronized (MessageUtil.class) {
                util = instances.get(bundleName);
                if (util == null) {
                    util = new MessageUtil(bundleName);
                    instances.put(bundleName, util);
                }
            }
        }
        return util;
    }

    public String getMessage(String key, Locale locale, Object[] args) {
        ResourceBundle rb = map.get(locale);
        if (rb == null) {
            synchronized (this) {
                rb = map.get(locale);
                if (rb == null) {
                    rb = ResourceBundle.getBundle(bundleName, locale);
                    map.put(locale, rb);
                }
            }
        }
        return MessageFormat.format(rb.getString(key), args);
    }

    public String getMessage(String key, Object[] args) {
        Locale locale = Locale.getDefault();
        return getMessage(key, locale, args);
    }
}
