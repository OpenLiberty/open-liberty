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
package com.ibm.ws.javaee.ddmodel.managedbean;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class ManagedBeanBndType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd, DDParser.RootParsable {
    public ManagedBeanBndType(String ddPath) {
        this.deploymentDescriptorPath = ddPath;
    }

    private final String deploymentDescriptorPath;
    private DDParser.ComponentIDMap idMap;
    com.ibm.ws.javaee.ddmodel.StringType version;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.InterceptorType, com.ibm.ws.javaee.dd.commonbnd.Interceptor> interceptor;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.managedbean.ManagedBeanType, com.ibm.ws.javaee.dd.managedbean.ManagedBean> managed_bean;

    @Override
    public java.lang.String getVersion() {
        return version != null ? version.getValue() : null;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.Interceptor> getInterceptors() {
        if (interceptor != null) {
            return interceptor.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.managedbean.ManagedBean> getManagedBeans() {
        if (managed_bean != null) {
            return managed_bean.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public String getDeploymentDescriptorPath() {
        return deploymentDescriptorPath;
    }

    @Override
    public Object getComponentForId(String id) {
        return idMap.getComponentForId(id);
    }

    @Override
    public String getIdForComponent(Object ddComponent) {
        return idMap.getIdForComponent(ddComponent);
    }

    @Override
    public void finish(DDParser parser) throws DDParser.ParseException {
        this.idMap = parser.idMap;
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if ("version".equals(localName)) {
                this.version = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if ("interceptor".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.InterceptorType interceptor = new com.ibm.ws.javaee.ddmodel.commonbnd.InterceptorType();
            parser.parse(interceptor);
            this.addInterceptor(interceptor);
            return true;
        }
        if ("managed-bean".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.managedbean.ManagedBeanType managed_bean = new com.ibm.ws.javaee.ddmodel.managedbean.ManagedBeanType();
            parser.parse(managed_bean);
            this.addManagedBean(managed_bean);
            return true;
        }
        return false;
    }

    void addInterceptor(com.ibm.ws.javaee.ddmodel.commonbnd.InterceptorType interceptor) {
        if (this.interceptor == null) {
            this.interceptor = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.InterceptorType, com.ibm.ws.javaee.dd.commonbnd.Interceptor>();
        }
        this.interceptor.add(interceptor);
    }

    void addManagedBean(com.ibm.ws.javaee.ddmodel.managedbean.ManagedBeanType managed_bean) {
        if (this.managed_bean == null) {
            this.managed_bean = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.managedbean.ManagedBeanType, com.ibm.ws.javaee.dd.managedbean.ManagedBean>();
        }
        this.managed_bean.add(managed_bean);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("version", version);
        diag.describeIfSet("interceptor", interceptor);
        diag.describeIfSet("managed-bean", managed_bean);
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }
}
