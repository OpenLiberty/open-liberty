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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.javaee.ddmodel.wsbnd.HttpPublishing;
import com.ibm.ws.javaee.ddmodel.wsbnd.WebserviceSecurity;
import com.ibm.ws.javaee.ddmodel.wsbnd.internal.WsBndConstants;

@Component(configurationPid = "com.ibm.ws.javaee.ddmodel.wsbnd.HttpPublishing",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = "service.vendor = IBM")
public class HttpPublishingComponentImpl implements HttpPublishing {

    private HttpPublishing delegate;

    protected volatile String contextRoot;

    protected volatile WebserviceSecurity webServiceSecurity;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               name = HttpPublishing.WEBSERVICE_SECURITY_ELEMENT_NAME,
               target = WsBndConstants.ID_UNBOUND)
    protected void setWebserviceSecurity(WebserviceSecurity value) {
        this.webServiceSecurity = value;
    }

    protected void unsetWebserviceSecurity(WebserviceSecurity value) {
        this.webServiceSecurity = null;
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        contextRoot = (java.lang.String) config.get(HttpPublishing.CONTEXT_ROOT_ATTRIBUTE_NAME);
    }

    @Override
    public String getContextRoot() {
        if (delegate == null) {
            return this.contextRoot;
        } else {
            return this.contextRoot == null ? delegate.getContextRoot() : this.contextRoot;
        }
    }

    @Override
    public WebserviceSecurity getWebserviceSecurity() {
        if (delegate == null) {
            return this.webServiceSecurity;
        } else {
            return this.webServiceSecurity == null ? delegate.getWebserviceSecurity() : this.webServiceSecurity;
        }
    }

    public void setDelegate(HttpPublishing value) {
        this.delegate = value;
    }
}
