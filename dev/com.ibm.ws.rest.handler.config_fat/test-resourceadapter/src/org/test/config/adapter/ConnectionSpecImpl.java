/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.config.adapter;

import javax.resource.cci.ConnectionSpec;
import javax.resource.spi.AdministeredObject;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.ConnectionRequestInfo;

@AdministeredObject(adminObjectInterfaces = ConnectionSpec.class)
public class ConnectionSpecImpl implements ConnectionRequestInfo, ConnectionSpec {
    @ConfigProperty
    private Long connectionTimeout;

    Class<?>[] interfaces = new Class<?>[] { javax.resource.cci.Connection.class, javax.resource.cci.ConnectionMetaData.class, javax.resource.cci.Interaction.class };

    @ConfigProperty(confidential = true)
    private String password;

    @ConfigProperty(defaultValue = "false")
    private Boolean readOnly;

    @ConfigProperty
    private String userName;

    public Long getConnectionTimeout() {
        return connectionTimeout;
    }

    public String getPassword() {
        return password;
    }

    public String getUserName() {
        return userName;
    }

    public Boolean isReadOnly() {
        return readOnly;
    }

    public void setConnectionTimeout(Long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setUserName(String user) {
        this.userName = user;
    }
}
