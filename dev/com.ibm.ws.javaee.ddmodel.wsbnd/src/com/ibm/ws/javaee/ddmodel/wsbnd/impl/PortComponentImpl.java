/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.wsbnd.impl;

import java.util.Map;

import javax.xml.namespace.QName;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.ws.javaee.ddmodel.wsbnd.Port;
import com.ibm.ws.javaee.ddmodel.wsbnd.Properties;
import com.ibm.ws.javaee.ddmodel.wsbnd.WebserviceEndpoint;
import com.ibm.ws.javaee.ddmodel.wsbnd.internal.StringUtils;
import com.ibm.ws.javaee.ddmodel.wsbnd.internal.WsBndConstants;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

@Component(configurationPid = "com.ibm.ws.javaee.ddmodel.wsbnd.Port",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = "service.vendor = IBM")
public class PortComponentImpl implements Port {

    private String namespace;
    private String userName;
    private String sslRef;
    private SerializableProtectedString password;
    private String address;
    private String name;
    private String keyAlias;

    private Properties properties;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               name = WebserviceEndpoint.PROPERTIES_ELEMENT_NAME,
               target = WsBndConstants.ID_UNBOUND)
    protected void setProperties(Properties value) {
        this.properties = value;
    }

    protected void unsetProperties(Properties value) {
        this.properties = null;
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        namespace = (String) config.get(Port.NAMESPACE_ATTRIBUTE_NAME);
        name = (String) config.get(Port.NAME_ATTRIBUTE_NAME);
        address = (String) config.get(Port.ADDRESS_ATTRIBUTE_NAME);
        Object pwd = config.get(Port.PASSWORD_ATTRIBUTE_NAME);
        if (pwd != null)
            password = (SerializableProtectedString) pwd;
        sslRef = (String) config.get(Port.SSL_REF_ATTRIBUTE_NAME);
        userName = (String) config.get(Port.USER_NAME_ATTRIBUTE_NAME);
        keyAlias = (String) config.get(Port.ALIAS_ATTRIBUTE_NAME);
    }

    @Override
    public QName getPortQName() {
        return StringUtils.buildQName(getNamespace(), getName());
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public ProtectedString getPassword() {
        return password == null ? null : new ProtectedString(password.getChars());
    }

    @Override
    public String getSSLRef() {
        return sslRef;
    }

    @Override
    public String getKeyAlias() {
        return keyAlias;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties == null ? null : properties.getAttributes();
    }
}
