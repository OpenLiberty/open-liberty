/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.resource.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.ws.resource.ResourceRefInfo;

public class ResourceRefConfigImpl implements ResourceRefConfig, Serializable {

    private static final TraceComponent tc = Tr.register(ResourceRefConfigImpl.class);

    private static final long serialVersionUID = -6326427478067188694L;

    private static final String[] ISOLATION_LEVEL_NAMES = {
                                                           "TRANSACTION_NONE",
                                                           "TRANSACTION_READ_UNCOMMITTED",
                                                           "TRANSACTION_READ_COMMITTED",
                                                           "3",
                                                           "TRANSACTION_REPEATABLE_READ",
                                                           "5",
                                                           "6",
                                                           "7",
                                                           "TRANSACTION_SERIALIZABLE",
    };

    private static final String[] BRANCH_COUPLING_NAMES = {
                                                           "LOOSE",
                                                           "TIGHT",
    };

    private final String name;
    private String description;
    private String type;
    private int auth = ResourceRef.AUTH_APPLICATION;
    private int sharingScope = ResourceRef.SHARING_SCOPE_SHAREABLE;
    private String bindingName;
    private String loginConfigurationName;
    private transient List<ResourceRefInfo.Property> loginProperties;
    private int isolationLevel = Connection.TRANSACTION_NONE;
    private int commitPriority;
    private int branchCoupling = BRANCH_COUPLING_UNSET;

    private static final String AUTHENTICATION_ALIAS_LOGIN_NAME = "DefaultPrincipalMapping";

    ResourceRefConfigImpl(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return new StringBuilder(super.toString())
                        .append("[name=").append(name)
                        .append(", description=").append(description)
                        .append(", type=").append(type)
                        .append(", auth=").append(toStringAuth())
                        .append(", sharingScope=").append(toStringSharingScope())
                        .append(", bindingName=").append(bindingName)
                        .append(", loginConfigurationName=").append(loginConfigurationName)
                        .append(", loginProperties=").append(loginProperties)
                        .append(", isolationLevel=").append(toStringIsolationLevel())
                        .append(", commitPriority=").append(commitPriority)
                        .append(", branchCoupling=").append(toStringBranchCoupling())
                        .append("]").toString();
    }

    @Trivial
    private String toStringAuth() {
        switch (auth) {
            case ResourceRef.AUTH_CONTAINER:
                return "Container";
            case ResourceRef.AUTH_APPLICATION:
                return "Application";
            default:
                return null;
        }
    }

    @Trivial
    private String toStringSharingScope() {
        switch (sharingScope) {
            case ResourceRef.SHARING_SCOPE_SHAREABLE:
                return "Shareable";
            case ResourceRef.SHARING_SCOPE_UNSHAREABLE:
                return "Unshareable";
            default:
                return null;
        }
    }

    @Trivial
    private String toStringIsolationLevel() {
        switch (isolationLevel) {
            case Connection.TRANSACTION_NONE:
                return "TRANSACTION_NONE";
            case Connection.TRANSACTION_READ_UNCOMMITTED:
                return "TRANSACTION_READ_UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED:
                return "TRANSACTION_READ_COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ:
                return "TRANSACTION_REPEATEDABLE_READ";
            case Connection.TRANSACTION_SERIALIZABLE:
                return "TRANSACTION_SERIALIZABLE";
            default:
                return null;
        }
    }

    @Trivial
    private String toStringBranchCoupling() {
        switch (branchCoupling) {
            case BRANCH_COUPLING_UNSET:
                return "UNSET";
            case BRANCH_COUPLING_LOOSE:
                return "LOOSE";
            case BRANCH_COUPLING_TIGHT:
                return "TIGHT";
            default:
                return null;
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        if (loginProperties == null) {
            out.writeInt(0);
        } else {
            out.writeInt(loginProperties.size());
            for (ResourceRefInfo.Property loginProperty : loginProperties) {
                out.writeObject(loginProperty.getName());
                out.writeObject(loginProperty.getValue());
            }
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        int numLoginProperties = in.readInt();
        for (int i = 0; i < numLoginProperties; i++) {
            String name = (String) in.readObject();
            String value = (String) in.readObject();
            addLoginProperty(name, value);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String className) {
        type = className;
    }

    @Override
    public int getAuth() {
        return auth;
    }

    @Override
    public void setResAuthType(int auth) {
        this.auth = auth;
    }

    @Override
    public int getSharingScope() {
        return sharingScope;
    }

    @Override
    public void setSharingScope(int sharingScope) {
        this.sharingScope = sharingScope;
    }

    @Override
    public String getJNDIName() {
        return bindingName;
    }

    @Override
    public void setJNDIName(String bindingName) {
        this.bindingName = bindingName;
    }

    @Override
    public String getLoginConfigurationName() {
        return loginConfigurationName;
    }

    @Override
    public void setLoginConfigurationName(String loginConfigurationName) {
        this.loginConfigurationName = loginConfigurationName;
    }

    @Override
    public List<? extends ResourceRefInfo.Property> getLoginPropertyList() {
        if (loginProperties != null) {
            return loginProperties;
        }
        return Collections.emptyList();
    }

    @Override
    public void clearLoginProperties() {
        loginProperties = null;
    }

    @Override
    public void addLoginProperty(String name, String value) {
        if (loginProperties == null) {
            loginProperties = new ArrayList<ResourceRefInfo.Property>();
        }
        loginProperties.add(new PropertyImpl(name, value));
    }

    @Override
    public int getIsolationLevel() {
        return isolationLevel;
    }

    @Override
    public void setIsolationLevel(int isolationLevel) {
        this.isolationLevel = isolationLevel;
    }

    @Override
    public int getCommitPriority() {
        return commitPriority;
    }

    @Override
    public void setCommitPriority(int commitPriority) {
        this.commitPriority = commitPriority;
    }

    @Override
    public int getBranchCoupling() {
        return branchCoupling;
    }

    @Override
    public void setBranchCoupling(int branchCoupling) {
        this.branchCoupling = branchCoupling;
    }

    @Override
    public void mergeBindingsAndExtensions(ResourceRefConfig[] resRefs, List<MergeConflict> mergeConflicts) {
        mergeBindingsAndExtensions(resRefs, mergeConflicts, false);
    }

    public List<MergeConflict> compareBindingsAndExtensions(ResourceRefConfig resRef) {
        List<MergeConflict> mergeConflicts = new ArrayList<MergeConflict>();
        mergeBindingsAndExtensions(new ResourceRefConfig[] { this, resRef }, mergeConflicts, true);
        return mergeConflicts;
    }

    private void mergeBindingsAndExtensions(ResourceRefConfig[] resRefs, List<MergeConflict> mergeConflicts, boolean compareOnly) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        final boolean strict = compareOnly;

        // binding values
        Attribute<String> bindingNameAttr = new Attribute<String>("binding-name", mergeConflicts, strict, null);
        Attribute<String> loginConfigurationNameAttr = new Attribute<String>("custom-login-configuration", mergeConflicts, strict, null);
        Map<String, Attribute<String>> loginAttrProperties = null;

        // extension values
        Attribute<Integer> isolationLevelAttr = new Attribute<Integer>("isolation-level", mergeConflicts, strict, ISOLATION_LEVEL_NAMES);
        Attribute<Integer> commitPriorityAttr = new Attribute<Integer>("commit-priority", mergeConflicts, strict, null);
        Attribute<Integer> branchCouplingAttr = new Attribute<Integer>("branch-coupling", mergeConflicts, strict, BRANCH_COUPLING_NAMES);

        for (int i = 0; i < resRefs.length; i++) {
            ResourceRefConfig resRef = resRefs[i];
            if (resRef != null) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "merging " + i + ": " + resRef);

                // binding - binding name
                bindingNameAttr.merge(i, resRef.getJNDIName());

                // binding - login configuration name
                loginConfigurationNameAttr.merge(i, resRef.getLoginConfigurationName());

                // binding - login configuration properties
                List<? extends ResourceRefInfo.Property> loginPropertyList = resRef.getLoginPropertyList();
                if (!loginPropertyList.isEmpty()) {
                    if (loginAttrProperties == null) {
                        loginAttrProperties = new LinkedHashMap<String, Attribute<String>>();
                    }

                    if (strict && i != 0) {
                        Set<String> newNames = new HashSet<String>();
                        for (ResourceRefInfo.Property property : loginPropertyList) {
                            newNames.add(property.getName());
                        }

                        for (Map.Entry<String, Attribute<String>> entry : loginAttrProperties.entrySet()) {
                            if (!newNames.contains(entry.getKey())) {
                                entry.getValue().merge(i, null);
                            }
                        }
                    }

                    for (ResourceRefInfo.Property property : loginPropertyList) {
                        String propertyName = property.getName();

                        Attribute<String> attribute = loginAttrProperties.get(propertyName);
                        if (attribute == null) {
                            String attributeName;
                            // On liberty, according to the J2C team, the authentication alias is provided 
                            // through the login property "DefaultPrincipleMapping".  It is no longer
                            // provided via the login configuration name.
                            if (propertyName.equals(AUTHENTICATION_ALIAS_LOGIN_NAME)) {
                                attributeName = "authentication-alias";
                            } else {
                                attributeName = "custom-login-configuration " + propertyName;
                            }

                            attribute = new Attribute<String>(attributeName, mergeConflicts, strict, null);
                            loginAttrProperties.put(propertyName, attribute);

                            // If we're comparing strictly and the property wasn't
                            // previously defined, then ensure it conflicts.     F88163
                            if (strict && i != 0) {
                                attribute.merge(0, null);
                            }
                        }

                        attribute.merge(i, property.getValue());
                    }
                } else if (strict && loginAttrProperties != null) {
                    // If we're comparing strictly, then all properties conflict.
                    for (Attribute<String> attribute : loginAttrProperties.values()) {
                        attribute.merge(i, null);
                    }
                }

                // extension - isolation level
                isolationLevelAttr.merge(i, resRef.getIsolationLevel());
                // extension - commit priority
                commitPriorityAttr.merge(i, resRef.getCommitPriority());
                // extension - branch coupling
                int branchCouplingValue = resRef.getBranchCoupling();
                if (branchCouplingValue != BRANCH_COUPLING_UNSET) {
                    branchCouplingAttr.merge(i, branchCouplingValue);
                } else if (strict) {
                    branchCouplingAttr.merge(i, null);
                }
            }
        }

        if (!compareOnly) {
            // set values from bindings
            bindingName = bindingNameAttr.ivValue;
            loginConfigurationName = loginConfigurationNameAttr.ivValue;
            if (loginAttrProperties != null) {
                loginProperties = new ArrayList<ResourceRefInfo.Property>(loginAttrProperties.size());
                for (Map.Entry<String, Attribute<String>> entry : loginAttrProperties.entrySet()) {
                    loginProperties.add(new PropertyImpl(entry.getKey(), entry.getValue().ivValue));
                }
            }

            // set values from extensions
            if (isolationLevelAttr.ivValue != null) {
                isolationLevel = isolationLevelAttr.ivValue;
            }
            if (commitPriorityAttr.ivValue != null) {
                setCommitPriority(commitPriorityAttr.ivValue);
            }
            if (branchCouplingAttr.ivValue != null) {
                branchCoupling = branchCouplingAttr.ivValue;
            }
        }
    }

    /**
     * Impl class for a property
     */
    private static class PropertyImpl implements ResourceRefInfo.Property {
        private final String name;
        private final String value;

        PropertyImpl(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return '[' + name + '=' + value + ']';
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    /**
     * Maintains the merged state of a single attribute of a ResRef.
     */
    private class Attribute<T> {
        private final String ivName;
        private final List<MergeConflict> ivConflicts;
        private final boolean ivStrict;
        private final String[] ivNames;

        int ivIndex = -1;
        T ivValue;

        Attribute(String name, List<MergeConflict> conflicts, boolean strict, String[] names) {
            ivName = name;
            ivConflicts = conflicts;
            ivStrict = strict;
            ivNames = names;
        }

        private String valueToString(Object value) {
            if (value == null) {
                return "null";
            }
            if (ivNames != null) {
                return ivNames[((Integer) value)];
            }
            return String.valueOf(value);
        }

        public void merge(int index, T value) {
            if (ivStrict) {
                if (ivIndex != -1 && (value == null ? ivValue != null : !value.equals(ivValue))) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "adding strict conflict " + getName() + "/" + ivName + " for " + ivIndex + " and " + index);

                    ivConflicts.add(new MergeConflictImpl(ivName, ivIndex, valueToString(ivValue), index, valueToString(value)));
                }

                ivIndex = index;
                ivValue = value;
                return;
            }

            if (value != null) {
                if (ivValue == null) {
                    ivIndex = index;
                    ivValue = value;
                } else if (!value.equals(ivValue)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "adding conflict " + getName() + "/" + ivName + " for " + ivIndex + " and " + index);

                    ivConflicts.add(new MergeConflictImpl(ivName, ivIndex, valueToString(ivValue), index, valueToString(value)));
                }
            }
        }
    }

    /**
     * Represents a merge conflict for a single attribute.
     */
    private class MergeConflictImpl
                    implements MergeConflict {
        private final String ivAttributeName;
        private final int ivIndex1;
        private final String ivValue1;
        private final int ivIndex2;
        private final String ivValue2;

        MergeConflictImpl(String attributeName, int index1, String value1, int index2, String value2) {
            ivAttributeName = attributeName;
            ivIndex1 = index1;
            ivValue1 = value1;
            ivIndex2 = index2;
            ivValue2 = value2;
        }

        @Override
        public String toString() {
            return super.toString() + '[' + ivAttributeName +
                   ", [" + ivIndex1 + "]=" + ivValue1 +
                   " != [" + ivIndex2 + "]=" + ivValue2 +
                   ']';
        }

        @Override
        public ResourceRefConfig getResourceRefConfig() {
            return ResourceRefConfigImpl.this;
        }

        @Override
        public String getAttributeName() {
            return ivAttributeName;
        }

        @Override
        public int getIndex1() {
            return ivIndex1;
        }

        @Override
        public String getValue1() {
            return ivValue1;
        }

        @Override
        public int getIndex2() {
            return ivIndex2;
        }

        @Override
        public String getValue2() {
            return ivValue2;
        }
    }
}