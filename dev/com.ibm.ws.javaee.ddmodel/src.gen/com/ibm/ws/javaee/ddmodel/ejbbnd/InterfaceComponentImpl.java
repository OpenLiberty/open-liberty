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
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.ejbbnd;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.javaee.dd.ejbbnd.Interface;

@Component(configurationPid = "com.ibm.ws.javaee.dd.ejbbnd.Interface",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = "service.vendor = IBM")
public class InterfaceComponentImpl extends InterfaceType implements com.ibm.ws.javaee.dd.ejbbnd.Interface {
    private Map<String, Object> configAdminProperties;
    private Interface delegate;

    protected java.lang.String bindingName;
    protected java.lang.String className;

    @Activate
    protected void activate(Map<String, Object> config) {
        this.configAdminProperties = config;
        bindingName = (java.lang.String) config.get("binding-name");
        className = (java.lang.String) config.get("class");

    }

    public Map<String, Object> getConfigAdminProperties() {
        return this.configAdminProperties;
    }

    public void setDelegate(Interface delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getBindingName() {
        if (delegate == null) {
            return bindingName == null ? null : bindingName;
        } else {
            return bindingName == null ? delegate.getBindingName() : bindingName;
        }
    }

    @Override
    public String getClassName() {
        if (delegate == null) {
            return className == null ? null : className;
        } else {
            return className == null ? delegate.getClassName() : className;
        }
    }
}
