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

public class EnterpriseBeanType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.ejbext.EnterpriseBean {
    public static class StructureXMIIgnoredType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable {
        public StructureXMIIgnoredType() {
            this(false);
        }

        public StructureXMIIgnoredType(boolean xmi) {
            this.xmi = xmi;
        }

        protected final boolean xmi;
        com.ibm.ws.javaee.ddmodel.BooleanType inheritenceRoot;

        @Override
        public boolean isIdAllowed() {
            return xmi;
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
            if (nsURI == null) {
                if (xmi && "inheritenceRoot".equals(localName)) {
                    this.inheritenceRoot = parser.parseBooleanAttributeValue(index);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
            return true;
        }

        @Override
        public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
            diag.describeIfSet("inheritenceRoot", inheritenceRoot);
        }
    }

    public static class InternationalizationXMIIgnoredType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable {
        public InternationalizationXMIIgnoredType() {
            this(false);
        }

        public InternationalizationXMIIgnoredType(boolean xmi) {
            this.xmi = xmi;
        }

        protected final boolean xmi;
        java.lang.String invocationLocale;

        @Override
        public boolean isIdAllowed() {
            return xmi;
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
            if (nsURI == null) {
                if (xmi && "invocationLocale".equals(localName)) {
                    this.invocationLocale = parseXMIStringAttributeValue(parser, index);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
            return true;
        }

        private static java.lang.String parseXMIStringAttributeValue(DDParser parser, int index) throws DDParser.ParseException {
            String value = parser.getAttributeValue(index);
            if ("CALLER".equals(value)) {
                return value;
            }
            if ("SERVER".equals(value)) {
                return value;
            }
            throw new DDParser.ParseException(parser.invalidEnumValue(value, "CALLER", "SERVER"));
        }

        @Override
        public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
            diag.describeEnumIfSet("invocationLocale", invocationLocale);
        }
    }

    public EnterpriseBeanType() {
        this(false);
    }

    public EnterpriseBeanType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.ddmodel.ejbext.BeanCacheType bean_cache;
    com.ibm.ws.javaee.ddmodel.commonext.LocalTransactionType local_transaction;
    com.ibm.ws.javaee.ddmodel.commonext.GlobalTransactionType global_transaction;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonext.ResourceRefType, com.ibm.ws.javaee.dd.commonext.ResourceRef> resource_ref;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.ejbext.RunAsModeType, com.ibm.ws.javaee.dd.ejbext.RunAsMode> run_as_mode;
    com.ibm.ws.javaee.ddmodel.ejbext.StartAtAppStartType start_at_app_start;
    com.ibm.ws.javaee.ddmodel.StringType name;
    private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType enterpriseBean;
    StructureXMIIgnoredType structure;
    InternationalizationXMIIgnoredType internationalization;

    @Override
    public com.ibm.ws.javaee.dd.ejbext.BeanCache getBeanCache() {
        return bean_cache;
    }

    @Override
    public com.ibm.ws.javaee.dd.commonext.LocalTransaction getLocalTransaction() {
        return local_transaction;
    }

    @Override
    public com.ibm.ws.javaee.dd.commonext.GlobalTransaction getGlobalTransaction() {
        return global_transaction;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonext.ResourceRef> getResourceRefs() {
        if (resource_ref != null) {
            return resource_ref.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.ejbext.RunAsMode> getRunAsModes() {
        if (run_as_mode != null) {
            return run_as_mode.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public com.ibm.ws.javaee.dd.ejbext.StartAtAppStart getStartAtAppStart() {
        return start_at_app_start;
    }

    @Override
    public java.lang.String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public void finish(DDParser parser) throws DDParser.ParseException {
        if (name == null) {
            throw new DDParser.ParseException(parser.requiredAttributeMissing("name"));
        }
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if (!xmi && "name".equals(localName)) {
                this.name = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "startEJBAtApplicationStart".equals(localName)) {
                if (this.start_at_app_start == null) {
                    this.start_at_app_start = new com.ibm.ws.javaee.ddmodel.ejbext.StartAtAppStartType(true);
                }
                this.start_at_app_start.value = parser.parseBooleanAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (xmi && "enterpriseBean".equals(localName)) {
            this.enterpriseBean = new com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType("enterpriseBean", parser.crossComponentDocumentType);
            parser.parse(enterpriseBean);
            com.ibm.ws.javaee.dd.ejb.EnterpriseBean referent = this.enterpriseBean.resolveReferent(parser, com.ibm.ws.javaee.dd.ejb.EnterpriseBean.class);
            if (referent == null) {
                DDParser.unresolvedReference("enterpriseBean", this.enterpriseBean.getReferenceString());
            } else {
                this.name = parser.parseString(referent.getName());
            }
            return true;
        }
        if ((xmi ? "beanCache" : "bean-cache").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.ejbext.BeanCacheType bean_cache = new com.ibm.ws.javaee.ddmodel.ejbext.BeanCacheType(xmi);
            parser.parse(bean_cache);
            this.bean_cache = bean_cache;
            return true;
        }
        if ((xmi ? "localTransaction" : "local-transaction").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonext.LocalTransactionType local_transaction = new com.ibm.ws.javaee.ddmodel.commonext.LocalTransactionType(xmi);
            parser.parse(local_transaction);
            this.local_transaction = local_transaction;
            return true;
        }
        if ((xmi ? "globalTransaction" : "global-transaction").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonext.GlobalTransactionType global_transaction = new com.ibm.ws.javaee.ddmodel.commonext.GlobalTransactionType(xmi);
            parser.parse(global_transaction);
            this.global_transaction = global_transaction;
            return true;
        }
        if ((xmi ? "resourceRefExtensions" : "resource-ref").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonext.ResourceRefType resource_ref = new com.ibm.ws.javaee.ddmodel.commonext.ResourceRefType(xmi);
            parser.parse(resource_ref);
            this.addResourceRef(resource_ref);
            return true;
        }
        if ((xmi ? "runAsSettings" : "run-as-mode").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.ejbext.RunAsModeType run_as_mode = new com.ibm.ws.javaee.ddmodel.ejbext.RunAsModeType(xmi);
            parser.parse(run_as_mode);
            this.addRunAsMode(run_as_mode);
            return true;
        }
        if (!xmi && "start-at-app-start".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.ejbext.StartAtAppStartType start_at_app_start = new com.ibm.ws.javaee.ddmodel.ejbext.StartAtAppStartType();
            parser.parse(start_at_app_start);
            this.start_at_app_start = start_at_app_start;
            return true;
        }
        if (xmi && "structure".equals(localName)) {
            StructureXMIIgnoredType structure = new StructureXMIIgnoredType(xmi);
            parser.parse(structure);
            this.structure = structure;
            return true;
        }
        if (xmi && "internationalization".equals(localName)) {
            InternationalizationXMIIgnoredType internationalization = new InternationalizationXMIIgnoredType(xmi);
            parser.parse(internationalization);
            this.internationalization = internationalization;
            return true;
        }
        if (xmi && "localTran".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonext.EJBLocalTranXMIType localTran = new com.ibm.ws.javaee.ddmodel.commonext.EJBLocalTranXMIType();
            parser.parse(localTran);
            this.local_transaction = localTran;
            return true;
        }
        return false;
    }

    void addResourceRef(com.ibm.ws.javaee.ddmodel.commonext.ResourceRefType resource_ref) {
        if (this.resource_ref == null) {
            this.resource_ref = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonext.ResourceRefType, com.ibm.ws.javaee.dd.commonext.ResourceRef>();
        }
        this.resource_ref.add(resource_ref);
    }

    void addRunAsMode(com.ibm.ws.javaee.ddmodel.ejbext.RunAsModeType run_as_mode) {
        if (this.run_as_mode == null) {
            this.run_as_mode = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.ejbext.RunAsModeType, com.ibm.ws.javaee.dd.ejbext.RunAsMode>();
        }
        this.run_as_mode.add(run_as_mode);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        if (xmi) {
            diag.describeIfSet("enterpriseBean", enterpriseBean);
        } else {
            diag.describeIfSet("name", name);
        }
        diag.describeIfSet(xmi ? "beanCache" : "bean-cache", bean_cache);
        diag.describeIfSet(xmi ? "localTransaction" : "local-transaction", local_transaction);
        diag.describeIfSet(xmi ? "globalTransaction" : "global-transaction", global_transaction);
        diag.describeIfSet(xmi ? "resourceRefExtensions" : "resource-ref", resource_ref);
        diag.describeIfSet(xmi ? "runAsSettings" : "run-as-mode", run_as_mode);
        if (xmi) {
            if (start_at_app_start != null) {
                start_at_app_start.describe(diag);
            }
        } else {
            diag.describeIfSet("start-at-app-start", start_at_app_start);
        }
        diag.describeIfSet("structure", structure);
        diag.describeIfSet("internationalization", internationalization);
    }
}
