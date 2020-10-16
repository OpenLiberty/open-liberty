/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
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

public class StringUtils {

    /**
     * Convert a service reference name and component name pair into an EJB
     * service reference key value.
     * 
     * Strip leading and trailing spaces from both the service reference name
     * and the component name.  Concatenate these with a '.' between them.
     *
     * @param serviceRefName The service reference name to build into the
     *     reference value.
     * @param componentName The component name to build into the reference
     *     value.
     *
     * @return A service reference key value constructed from the service
     *     reference name and from the component name.
     */
    public static String getEJBServiceRefKey(String serviceRefName, String componentName) {
        return
            new StringBuilder()
                .append( componentName.trim() )
                .append(".")
                .append( serviceRefName.trim() )
                .toString();
    }

    /**
     * Tell whether a string value is null, empty, or all whitespace
     * characters.
     *
     * The implementation allows all special characters (characters
     * which have an ordinal value less than or equal to the space
     * character ordinal value).  This is slightly inproper, but is
     * expected to be sufficient for the XML text value which will
     * be tested.
     * 
     * @param value The string value which is to be tested.
     *
     * @return True or false telling if the string value is null,
     *     empty, or all whitespace characters.
     */
    public static boolean isEmpty(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        int numChars = value.length();
        for ( int charNo = 0; charNo < numChars; charNo++ ) {
            // Ignore white space characters ...
            // .. but also ignore special characters.
            if ( value.charAt(charNo) > ' ' ) {
                return false;
            }
        }

        return true;
    }

    /**
     * Safely trim a value: Answer null if the value is null.  Otherwise,
     * return the usual result of invoking {@link String#trim()} on the value.
     *
     * @param value The value which is to be trimmed.
     *
     * @return Null, if the value is null.  Otherwise, the trimmed value.
     */
    public final static String trim(String value) {
        if ( value != null ) {
            value = value.trim();
        }
        return value;
    }

    /**
     * Construct a qualified name from a specified namespace and local name.
     * 
     * Append a forward slash ('/') to the namespace, unless the namespace
     * already ends with a forward slash.  (Whitespace characters following the
     * slash are ignored when testing for a forward slash.  However, the
     * whitespace characters are retained in the text of the qualified name.
     *
     * Do not append a forward slash if the namespace is empty or is all
     * whitespace. 
     *
     * @param namespace The namespace to put into the qualified name.
     * @param localName The local name to put into the qualified name.
     * 
     * @return A qualified name composed of the namespace and local name.
     */
    public static QName buildQName(String namespace, String localName) {
        if ( !isEmpty(namespace) && !namespace.trim().endsWith("/") ) {
            namespace += "/";
        }
        return new QName(namespace, localName);
    }
}
