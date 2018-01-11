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
package com.ibm.ws.dynamic.bundle;

import java.util.ArrayList;
import java.util.List;

public class ServiceComponentDeclaration {
    private String name;
    private String implementationClass;
    private String serviceVendor = "IBM";
    private String servicePid;
    private final List<String> providedInterfaces = new ArrayList<String>();
    private final List<ServiceReferenceDeclaration> references = new ArrayList<ServiceReferenceDeclaration>();

    public ServiceComponentDeclaration name(String name) {
        this.name = name;
        return this;
    }

    public ServiceComponentDeclaration impl(Class<?> implementationClass) {
        return impl(implementationClass.getName());
    }

    public ServiceComponentDeclaration impl(String implementationClass) {
        this.implementationClass = implementationClass;
        return this;
    }

    public ServiceComponentDeclaration vendor(String serviceVendor) {
        this.serviceVendor = serviceVendor;
        return this;
    }

    public ServiceComponentDeclaration pid(String servicePid) {
        this.servicePid = servicePid;
        return this;
    }

    public ServiceComponentDeclaration provide(Class<?> iface) {
        return provide(iface.getName());
    }

    public ServiceComponentDeclaration provide(String iface) {
        providedInterfaces.add(iface);
        return this;
    }

    public ServiceComponentDeclaration require(ServiceReferenceDeclaration ref) {
        references.add(ref);
        return this;
    }

    public String getFileName() {
        return "OSGI-INF/" + name + ".xml";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='utf-8' standalone='no'?>\n");
        sb.append("<component xmlns='http://www.osgi.org/xmlns/scr/v1.3.0' configuration-policy='ignore' name='").append(name).append("'>\n");
        sb.append("  <implementation class='").append(implementationClass).append("' />\n");
        sb.append("  <service>\n");
        for (String iface : providedInterfaces)
            sb.append("    <provide interface='").append(iface).append("' />\n");
        sb.append("  </service>\n");
        sb.append("  <property name='service.vendor' value='").append(serviceVendor).append("' />\n");
        sb.append("  <property name='service.pid' value='").append(servicePid).append("' />\n");
        for (ServiceReferenceDeclaration ref : references)
            sb.append("  ").append(ref).append("\n");
        sb.append("</component>");
        return sb.toString();
    }
}
