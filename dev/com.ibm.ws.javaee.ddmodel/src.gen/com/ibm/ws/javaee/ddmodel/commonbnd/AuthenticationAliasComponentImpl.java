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
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.commonbnd;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

@Component(configurationPid = "com.ibm.ws.javaee.dd.commonbnd.AuthenticationAlias",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = "service.vendor = IBM")
public class AuthenticationAliasComponentImpl implements com.ibm.ws.javaee.dd.commonbnd.AuthenticationAlias {
    private Map<String, Object> configAdminProperties;
    private com.ibm.ws.javaee.dd.commonbnd.AuthenticationAlias delegate;
    protected java.lang.String name;

    @Activate
    protected void activate(Map<String, Object> config) {
        this.configAdminProperties = config;
        name = (java.lang.String) config.get("name");
    }

    @Reference
    org.osgi.service.cm.ConfigurationAdmin configAdmin;

    private String getIDForPID(String pid) {
        try {
            String filter = com.ibm.wsspi.kernel.service.utils.FilterUtils.createPropertyFilter(org.osgi.framework.Constants.SERVICE_PID, name);
            org.osgi.service.cm.Configuration[] configs = configAdmin.listConfigurations(filter);
            if (configs == null || configs.length == 0)
                return null;
            return (String) configs[0].getProperties().get("id");
        } catch (java.io.IOException e) {
            e.getCause();
        } catch (org.osgi.framework.InvalidSyntaxException e) {
            e.getCause();
        }
        return null;
    }

    @Override
    public java.lang.String getName() {
        String id = getIDForPID(name);
        if (delegate == null) {
            return id == null ? null : id;
        } else {
            return id == null ? delegate.getName() : id;
        }
    }

    public Map<String, Object> getConfigAdminProperties() {
        return this.configAdminProperties;
    }

    public void setDelegate(com.ibm.ws.javaee.dd.commonbnd.AuthenticationAlias delegate) {
        this.delegate = delegate;
    }
}
