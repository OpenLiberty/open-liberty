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
package com.ibm.ws.javaee.ddmodel.ejbbnd;

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class EJBJarBndType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd, DDParser.RootParsable {
    private static final TraceComponent tc = Tr.register(EJBJarBndType.class);

    public static class DefaultCMPConnectionFactoryXMIIgnoredType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable {
        public DefaultCMPConnectionFactoryXMIIgnoredType() {
            this(false);
        }

        public DefaultCMPConnectionFactoryXMIIgnoredType(boolean xmi) {
            this.xmi = xmi;
        }

        protected final boolean xmi;

        @Override
        public boolean isIdAllowed() {
            return xmi;
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
            return true;
        }

        @Override
        public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        }
    }

    public static class DefaultDatasourceXMIIgnoredType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable {
        public DefaultDatasourceXMIIgnoredType() {
            this(false);
        }

        public DefaultDatasourceXMIIgnoredType(boolean xmi) {
            this.xmi = xmi;
        }

        protected final boolean xmi;

        @Override
        public boolean isIdAllowed() {
            return xmi;
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
            return true;
        }

        @Override
        public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        }
    }

    public EJBJarBndType(String ddPath) {
        this(ddPath, false);
    }

    public EJBJarBndType(String ddPath, boolean xmi) {
        this.xmi = xmi;
        this.deploymentDescriptorPath = ddPath;
    }

    private final String deploymentDescriptorPath;
    private DDParser.ComponentIDMap idMap;
    protected final boolean xmi;
    private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType xmiRef;
    com.ibm.ws.javaee.ddmodel.StringType version;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.ejbbnd.EnterpriseBeanType, com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean> enterpriseBeans;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.InterceptorType, com.ibm.ws.javaee.dd.commonbnd.Interceptor> interceptor;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationType, com.ibm.ws.javaee.dd.commonbnd.MessageDestination> message_destination;
    DefaultCMPConnectionFactoryXMIIgnoredType defaultCMPConnectionFactory;
    DefaultDatasourceXMIIgnoredType defaultDatasource;
    com.ibm.ws.javaee.ddmodel.StringType currentBackendId;

    @Override
    public java.lang.String getVersion() {
        return xmi ? "XMI" : version != null ? version.getValue() : null;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean> getEnterpriseBeans() {
        if (enterpriseBeans != null) {
            return enterpriseBeans.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.Interceptor> getInterceptors() {
        if (interceptor != null) {
            return interceptor.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.MessageDestination> getMessageDestinations() {
        if (message_destination != null) {
            return message_destination.getList();
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

        {
            java.util.Map<String, com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean> beans = new java.util.HashMap<String, com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean>(getEnterpriseBeans().size());
            for (com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean bean : getEnterpriseBeans()) {
                com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean existing = beans.put(bean.getName(), bean);
                if (existing != null) {
                    throw new DDParser.ParseException(Tr.formatMessage(tc, "found.duplicate.ejbname", parser.getDeploymentDescriptorPath(), existing.getName()));
                }
            }
        }

        {
            java.util.Map<String, com.ibm.ws.javaee.dd.commonbnd.Interceptor> interceptors = new java.util.HashMap<String, com.ibm.ws.javaee.dd.commonbnd.Interceptor>(getInterceptors().size());
            for (com.ibm.ws.javaee.dd.commonbnd.Interceptor interceptor : getInterceptors()) {
                com.ibm.ws.javaee.dd.commonbnd.Interceptor existing = interceptors.put(interceptor.getClassName(), interceptor);
                if (existing != null) {
                    throw new DDParser.ParseException(Tr.formatMessage(tc, "found.duplicate.attribute.value", parser.getDeploymentDescriptorPath(), "<interceptor>", "class", existing.getClassName()));
                }
            }
        }

        {
            java.util.Map<String, com.ibm.ws.javaee.dd.commonbnd.MessageDestination> messageDestinations = new java.util.HashMap<String, com.ibm.ws.javaee.dd.commonbnd.MessageDestination>(getMessageDestinations().size());
            for (com.ibm.ws.javaee.dd.commonbnd.MessageDestination messageDestination : getMessageDestinations()) {
                com.ibm.ws.javaee.dd.commonbnd.MessageDestination existing = messageDestinations.put(messageDestination.getName(), messageDestination);
                if (existing != null) {
                    throw new DDParser.ParseException(Tr.formatMessage(tc, "found.duplicate.attribute.value", parser.getDeploymentDescriptorPath(), "<message-destination>", "name", existing.getName()));
                }
            }
        }
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if (!xmi && "version".equals(localName)) {
                this.version = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "currentBackendId".equals(localName)) {
                this.currentBackendId = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        if (xmi && "http://www.omg.org/XMI".equals(nsURI)) {
            if ("version".equals(localName)) {
                // Allowed but ignored.
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (xmi && "ejbJar".equals(localName)) {
            xmiRef = new com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType("ejbJar", com.ibm.ws.javaee.dd.ejb.EJBJar.class);
            parser.parse(xmiRef);
            return true;
        }
        if (!xmi && "session".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.ejbbnd.SessionType enterpriseBeans = new com.ibm.ws.javaee.ddmodel.ejbbnd.SessionType();
            parser.parse(enterpriseBeans);
            this.addEnterpriseBean(enterpriseBeans);
            return true;
        }
        if (!xmi && "message-driven".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.ejbbnd.MessageDrivenType enterpriseBeans = new com.ibm.ws.javaee.ddmodel.ejbbnd.MessageDrivenType();
            parser.parse(enterpriseBeans);
            this.addEnterpriseBean(enterpriseBeans);
            return true;
        }
        if (xmi && "ejbBindings".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.ejbbnd.EnterpriseBeanType enterpriseBeans;
            String xmiType = parser.getAttributeValue("http://www.omg.org/XMI", "type");
            if (xmiType == null) {
                enterpriseBeans = new com.ibm.ws.javaee.ddmodel.ejbbnd.SessionType(true);
            } else if (xmiType.endsWith(":MessageDrivenBeanBinding") && "ejbbnd.xmi".equals(parser.getNamespaceURI(xmiType.substring(0, xmiType.length() - ":MessageDrivenBeanBinding".length())))) {
                enterpriseBeans = new com.ibm.ws.javaee.ddmodel.ejbbnd.MessageDrivenType(true);
            } else {
                return false;
            }
            parser.parse(enterpriseBeans);
            this.addEnterpriseBean(enterpriseBeans);
            return true;
        }
        if (!xmi && "interceptor".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.InterceptorType interceptor = new com.ibm.ws.javaee.ddmodel.commonbnd.InterceptorType();
            parser.parse(interceptor);
            this.addInterceptor(interceptor);
            return true;
        }
        if (!xmi && "message-destination".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationType message_destination = new com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationType();
            parser.parse(message_destination);
            this.addMessageDestination(message_destination);
            return true;
        }
        if (xmi && "defaultCMPConnectionFactory".equals(localName)) {
            DefaultCMPConnectionFactoryXMIIgnoredType defaultCMPConnectionFactory = new DefaultCMPConnectionFactoryXMIIgnoredType(xmi);
            parser.parse(defaultCMPConnectionFactory);
            this.defaultCMPConnectionFactory = defaultCMPConnectionFactory;
            return true;
        }
        if (xmi && "defaultDatasource".equals(localName)) {
            DefaultDatasourceXMIIgnoredType defaultDatasource = new DefaultDatasourceXMIIgnoredType(xmi);
            parser.parse(defaultDatasource);
            this.defaultDatasource = defaultDatasource;
            return true;
        }
        return false;
    }

    void addEnterpriseBean(com.ibm.ws.javaee.ddmodel.ejbbnd.EnterpriseBeanType enterpriseBeans) {
        if (this.enterpriseBeans == null) {
            this.enterpriseBeans = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.ejbbnd.EnterpriseBeanType, com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean>();
        }
        this.enterpriseBeans.add(enterpriseBeans);
    }

    void addInterceptor(com.ibm.ws.javaee.ddmodel.commonbnd.InterceptorType interceptor) {
        if (this.interceptor == null) {
            this.interceptor = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.InterceptorType, com.ibm.ws.javaee.dd.commonbnd.Interceptor>();
        }
        this.interceptor.add(interceptor);
    }

    void addMessageDestination(com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationType message_destination) {
        if (this.message_destination == null) {
            this.message_destination = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.MessageDestinationType, com.ibm.ws.javaee.dd.commonbnd.MessageDestination>();
        }
        this.message_destination.add(message_destination);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("ejbJar", xmiRef);
        diag.describeIfSet("version", version);
        diag.describeIfSet("currentBackendId", currentBackendId);
        diag.describeIfSet(xmi ? "ejbBindings" : "enterpriseBeans", enterpriseBeans);
        diag.describeIfSet("interceptor", interceptor);
        diag.describeIfSet("message-destination", message_destination);
        diag.describeIfSet("defaultCMPConnectionFactory", defaultCMPConnectionFactory);
        diag.describeIfSet("defaultDatasource", defaultDatasource);
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }
}
