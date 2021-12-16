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
package com.ibm.ws.javaee.ddmodel.ejbext;

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class EJBJarExtType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.ejbext.EJBJarExt, DDParser.RootParsable {
    private static final TraceComponent tc = Tr.register(EJBJarExtType.class);

    public EJBJarExtType(String ddPath) {
        this(ddPath, false);
    }

    public EJBJarExtType(String ddPath, boolean xmi) {
        this.xmi = xmi;
        this.deploymentDescriptorPath = ddPath;
    }

    private final String deploymentDescriptorPath;
    private DDParser.ComponentIDMap idMap;
    protected final boolean xmi;
    private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType xmiRef;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.ejbext.EnterpriseBeanType, com.ibm.ws.javaee.dd.ejbext.EnterpriseBean> enterpriseBeans;
    com.ibm.ws.javaee.ddmodel.StringType version;

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.ejbext.EnterpriseBean> getEnterpriseBeans() {
        if (enterpriseBeans != null) {
            return enterpriseBeans.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.lang.String getVersion() {
        return xmi ? "XMI" : version != null ? version.getValue() : null;
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
            java.util.Map<String, com.ibm.ws.javaee.dd.ejbext.EnterpriseBean> beans = new java.util.HashMap<String, com.ibm.ws.javaee.dd.ejbext.EnterpriseBean>(getEnterpriseBeans().size());
            for (com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean : getEnterpriseBeans()) {
                com.ibm.ws.javaee.dd.ejbext.EnterpriseBean existing = beans.put(bean.getName(), bean);
                if (existing != null) {
                    throw new DDParser.ParseException(Tr.formatMessage(tc, "found.duplicate.ejbname", parser.getDeploymentDescriptorPath(), existing.getName()));
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
            com.ibm.ws.javaee.ddmodel.ejbext.SessionType enterpriseBeans = new com.ibm.ws.javaee.ddmodel.ejbext.SessionType();
            parser.parse(enterpriseBeans);
            this.addEnterpriseBean(enterpriseBeans);
            return true;
        }
        if (!xmi && "message-driven".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.ejbext.MessageDrivenType enterpriseBeans = new com.ibm.ws.javaee.ddmodel.ejbext.MessageDrivenType();
            parser.parse(enterpriseBeans);
            this.addEnterpriseBean(enterpriseBeans);
            return true;
        }
        if (xmi && "ejbExtensions".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.ejbext.EnterpriseBeanType enterpriseBeans;
            String xmiType = parser.getAttributeValue("http://www.omg.org/XMI", "type");
            if (xmiType == null) {
                throw new DDParser.ParseException(parser.requiredAttributeMissing("xmi:type"));
            } else if (xmiType.endsWith(":SessionExtension") && "ejbext.xmi".equals(parser.getNamespaceURI(xmiType.substring(0, xmiType.length() - ":SessionExtension".length())))) {
                enterpriseBeans = new com.ibm.ws.javaee.ddmodel.ejbext.SessionType(true);
            } else if (xmiType.endsWith(":MessageDrivenExtension") && "ejbext.xmi".equals(parser.getNamespaceURI(xmiType.substring(0, xmiType.length() - ":MessageDrivenExtension".length())))) {
                enterpriseBeans = new com.ibm.ws.javaee.ddmodel.ejbext.MessageDrivenType(true);
            } else {
                return false;
            }
            parser.parse(enterpriseBeans);
            this.addEnterpriseBean(enterpriseBeans);
            return true;
        }
        return false;
    }

    void addEnterpriseBean(com.ibm.ws.javaee.ddmodel.ejbext.EnterpriseBeanType enterpriseBeans) {
        if (this.enterpriseBeans == null) {
            this.enterpriseBeans = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.ejbext.EnterpriseBeanType, com.ibm.ws.javaee.dd.ejbext.EnterpriseBean>();
        }
        this.enterpriseBeans.add(enterpriseBeans);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("ejbJar", xmiRef);
        diag.describeIfSet("version", version);
        diag.describeIfSet(xmi ? "ejbExtensions" : "enterpriseBeans", enterpriseBeans);
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }
}
