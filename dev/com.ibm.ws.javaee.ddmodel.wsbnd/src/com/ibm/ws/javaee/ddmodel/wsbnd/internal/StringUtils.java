/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.wsbnd.internal;

import javax.xml.namespace.QName;

/**
 *
 */
public class StringUtils {

    public static String getEJBServiceRefKey(String serviceRefName, String componentName) {
        return new StringBuilder().append(componentName.trim()).append(".").append(serviceRefName.trim()).toString();
    }

    /**
     * Check whether the target string is empty
     *
     * @param str
     * @return true if the string is null or the length is zero after trimming spaces.
     */
    public static boolean isEmpty(String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }

        int len = str.length();
        for (int x = 0; x < len; ++x) {
            if (str.charAt(x) > ' ') {
                return false;
            }
        }

        return true;
    }

    /**
     * remove the blank characters in the left and right for a given value.
     *
     * @param value
     * @return
     */
    public final static String trim(String value) {
        String result = null;
        if (null != value) {
            result = value.trim();
        }

        return result;
    }

    /**
     * build the qname with the given, and make sure the namespace is ended with "/" if specified.
     *
     * @param portNameSpace
     * @param portLocalName
     * @return
     */
    public static QName buildQName(String namespace, String localName) {
        String namespaceURI = namespace;
        if (!isEmpty(namespace) && !namespace.trim().endsWith("/")) {
            namespaceURI += "/";
        }

        return new QName(namespaceURI, localName);
    }
}
