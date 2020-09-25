/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse License v1.0
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
    String NAMESPACE_ATTRIBUTE_NAME = "namespace";
    String NAME_ATTRIBUTE_NAME = "name";
    String ADDRESS_ATTRIBUTE_NAME = "address";
    String USER_NAME_ATTRIBUTE_NAME = "username";
    String PASSWORD_ATTRIBUTE_NAME = "password";
    String SSL_REF_ATTRIBUTE_NAME = "ssl-ref";
    String ALIAS_ATTRIBUTE_NAME = "key-alias";
    String PROPERTIES_ELEMENT_NAME = "properties";

    QName getPortQName();
    String getNamespace();
    String getName();
    String getAddress();
    String getUserName();
    ProtectedString getPassword();
    String getSSLRef();
    String getKeyAlias();

    Map<String, String> getProperties();
}
