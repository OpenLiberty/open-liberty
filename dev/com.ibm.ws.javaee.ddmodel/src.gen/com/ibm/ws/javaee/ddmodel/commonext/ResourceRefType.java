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
package com.ibm.ws.javaee.ddmodel.commonext;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class ResourceRefType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.commonext.ResourceRef {
    public ResourceRefType() {
        this(false);
    }

    public ResourceRefType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.ddmodel.StringType name;
    private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType resourceRef;
    com.ibm.ws.javaee.dd.commonext.ResourceRef.IsolationLevelEnum isolation_level;
    com.ibm.ws.javaee.dd.commonext.ResourceRef.ConnectionManagementPolicyEnum connection_management_policy;
    com.ibm.ws.javaee.ddmodel.IntegerType commit_priority;
    com.ibm.ws.javaee.dd.commonext.ResourceRef.BranchCouplingEnum branch_coupling;

    @Override
    public java.lang.String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public boolean isSetIsolationLevel() {
        return isolation_level != null;
    }

    @Override
    public com.ibm.ws.javaee.dd.commonext.ResourceRef.IsolationLevelEnum getIsolationLevel() {
        return isolation_level;
    }

    @Override
    public boolean isSetConnectionManagementPolicy() {
        return connection_management_policy != null;
    }

    @Override
    public com.ibm.ws.javaee.dd.commonext.ResourceRef.ConnectionManagementPolicyEnum getConnectionManagementPolicy() {
        return connection_management_policy;
    }

    @Override
    public boolean isSetCommitPriority() {
        return commit_priority != null;
    }

    @Override
    public int getCommitPriority() {
        return commit_priority != null ? commit_priority.getIntValue() : 0;
    }

    @Override
    public boolean isSetBranchCoupling() {
        return branch_coupling != null;
    }

    @Override
    public com.ibm.ws.javaee.dd.commonext.ResourceRef.BranchCouplingEnum getBranchCoupling() {
        return branch_coupling;
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
            if ((xmi ? "isolationLevel" : "isolation-level").equals(localName)) {
                this.isolation_level = parser.parseEnumAttributeValue(index, com.ibm.ws.javaee.dd.commonext.ResourceRef.IsolationLevelEnum.class);
                return true;
            }
            if ((xmi ? "connectionManagementPolicy" : "connection-management-policy").equals(localName)) {
                this.connection_management_policy = xmi ? parseXMIConnectionManagementPolicyEnumAttributeValue(parser, index) : parser.parseEnumAttributeValue(index, com.ibm.ws.javaee.dd.commonext.ResourceRef.ConnectionManagementPolicyEnum.class);
                return true;
            }
            if ((xmi ? "commitPriority" : "commit-priority").equals(localName)) {
                this.commit_priority = parser.parseIntegerAttributeValue(index);
                return true;
            }
            if ((xmi ? "branchCoupling" : "branch-coupling").equals(localName)) {
                this.branch_coupling = xmi ? parseXMIBranchCouplingEnumAttributeValue(parser, index) : parser.parseEnumAttributeValue(index, com.ibm.ws.javaee.dd.commonext.ResourceRef.BranchCouplingEnum.class);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (xmi && "resourceRef".equals(localName)) {
            this.resourceRef = new com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType("resourceRef", parser.crossComponentDocumentType);
            parser.parse(resourceRef);
            com.ibm.ws.javaee.dd.common.ResourceRef referent = this.resourceRef.resolveReferent(parser, com.ibm.ws.javaee.dd.common.ResourceRef.class);
            if (referent == null) {
                DDParser.unresolvedReference("resourceRef", this.resourceRef.getReferenceString());
            } else {
                this.name = parser.parseString(referent.getName());
            }
            return true;
        }
        return false;
    }

    private static com.ibm.ws.javaee.dd.commonext.ResourceRef.ConnectionManagementPolicyEnum parseXMIConnectionManagementPolicyEnumAttributeValue(DDParser parser, int index) throws DDParser.ParseException {
        String value = parser.getAttributeValue(index);
        if ("Default".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.ResourceRef.ConnectionManagementPolicyEnum.DEFAULT;
        }
        if ("Aggressive".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.ResourceRef.ConnectionManagementPolicyEnum.AGGRESSIVE;
        }
        if ("Normal".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.ResourceRef.ConnectionManagementPolicyEnum.NORMAL;
        }
        throw new DDParser.ParseException(parser.invalidEnumValue(value, "Default", "Aggressive", "Normal"));
    }

    private static com.ibm.ws.javaee.dd.commonext.ResourceRef.BranchCouplingEnum parseXMIBranchCouplingEnumAttributeValue(DDParser parser, int index) throws DDParser.ParseException {
        String value = parser.getAttributeValue(index);
        if ("Loose".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.ResourceRef.BranchCouplingEnum.LOOSE;
        }
        if ("Tight".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.ResourceRef.BranchCouplingEnum.TIGHT;
        }
        throw new DDParser.ParseException(parser.invalidEnumValue(value, "Loose", "Tight"));
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        if (xmi) {
            diag.describeIfSet("resourceRef", resourceRef);
        } else {
            diag.describeIfSet("name", name);
        }
        diag.describeEnumIfSet(xmi ? "isolationLevel" : "isolation-level", isolation_level);
        diag.describeEnumIfSet(xmi ? "connectionManagementPolicy" : "connection-management-policy", connection_management_policy);
        diag.describeIfSet(xmi ? "commitPriority" : "commit-priority", commit_priority);
        diag.describeEnumIfSet(xmi ? "branchCoupling" : "branch-coupling", branch_coupling);
    }
}
