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
package com.ibm.ws.javaee.ddmodel.wsbnd;

import java.util.Map;

import javax.xml.namespace.QName;

import com.ibm.websphere.ras.ProtectedString;

public interface Port {

    public static String NAMESPACE_ATTRIBUTE_NAME = "namespace";

    public static String NAME_ATTRIBUTE_NAME = "name";

    public static String ADDRESS_ATTRIBUTE_NAME = "address";

    public static String USER_NAME_ATTRIBUTE_NAME = "username";

    public static String PASSWORD_ATTRIBUTE_NAME = "password";

    public static String SSL_REF_ATTRIBUTE_NAME = "ssl-ref";

    public static String ALIAS_ATTRIBUTE_NAME = "key-alias";

    public static String PROPERTIES_ELEMENT_NAME = "properties";

    public QName getPortQName();

    public String getNamespace();

    public String getName();

    public String getAddress();

    public String getUserName();

    public ProtectedString getPassword();

    public String getSSLRef();

    public String getKeyAlias();

    /**
     * Get all attributes defined in the properties element under port.
     * 
     * @return
     */
    public Map<String, String> getProperties();
}
