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
package com.ibm.ws.config.schemagen.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinitionImpl;
import com.ibm.ws.config.xml.internal.metatype.ExtendedObjectClassDefinition;
import com.ibm.ws.config.xml.internal.metatype.ExtendedObjectClassDefinitionImpl;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
class TypeBuilder {

    private static final TraceComponent tc = Tr.register(TypeBuilder.class, SchemaGenConstants.TR_GROUP, SchemaGenConstants.NLS_PROPS);

    private final boolean ignoreErrors;
    private final Set<String> ignoredPids;
    private final Locale locale;

    private final Map<String, OCDType> ocdTypeMap;
    private final Map<String, OCDTypeReference> pidTypeMap;
    private final Map<String, OCDTypeReference> internalPidTypeMap;
    private final Map<String, List<OCDType>> aliasMap;
    // list of OCDs that have parentPid set
    private final Set<OCDType> children = new HashSet<OCDType>();

    private final boolean isRuntime;

    public TypeBuilder(boolean ignoreErrors, Set<String> ignoredPids, Locale locale, boolean isRuntime) {
        this.ignoreErrors = ignoreErrors;
        this.ignoredPids = ignoredPids;
        this.locale = (locale == null) ? Locale.getDefault() : locale;
        // Tooling wants order (especially attribute order) preserved from metatypes
        // LinkedHashMaps and LinkedHashSets iterate in insertion order.
        // We may not actually need _Linked_ for all of these, but we can afford it since schemagen isn't a runtime operation
        this.ocdTypeMap = new LinkedHashMap<String, OCDType>();
        this.pidTypeMap = new LinkedHashMap<String, OCDTypeReference>();
        this.internalPidTypeMap = new LinkedHashMap<String, OCDTypeReference>();
        this.aliasMap = new LinkedHashMap<String, List<OCDType>>();
        this.isRuntime = isRuntime;
    }

    private void buildTypes(MetaTypeInformation metatype, String[] pids, boolean factoryPids) {
        if (pids == null) {
            return;
        }
        for (String pid : pids) {
            if (ignoredPids != null && ignoredPids.contains(pid)) {
                continue;
            }
            ObjectClassDefinition ocd = metatype.getObjectClassDefinition(pid, locale.toString());

            OCDType type = null;

            if (!!!ocdTypeMap.containsKey(pid)) {
                Bundle bundle = metatype.getBundle();
                String bundleLocation = (bundle != null) ? bundle.getLocation() : null;
                ExtendedObjectClassDefinition extOCD = ExtendedObjectClassDefinitionImpl.newExtendedObjectClassDefinition(ocd, bundleLocation);
                type = new OCDType(metatype, pid, extOCD);
                ocdTypeMap.put(pid, type);

                // childAlias names are not added to the alias map.
                String aliasName = extOCD.getAlias();
                if (aliasName != null) {
                    addAlias(aliasName, type);
                }

                processExtensions(pid, type);

                if (factoryPids) {
                    type.setHasFactoryReference(true);
                }

                boolean isIBMFinalSetForIdField = processAttributes(ocd);
                if (isIBMFinalSetForIdField) {
                    type.setHasFactoryReference(false);
                    type.setHasIBMFinalWithDefault(true);
                }

                if ("internal".equalsIgnoreCase(ocd.getName())) {
                    OCDTypeReference existingType = pidTypeMap.get(pid);
                    if (existingType == null) {
                        internalPidTypeMap.put(pid, new OCDTypeReference(type, factoryPids));
                    } else {
                        error("schemagen.duplicate.pid", pid);
                        continue;
                    }
                } else {

                    OCDTypeReference existingType = pidTypeMap.get(pid);
                    if (existingType == null) {
                        pidTypeMap.put(pid, new OCDTypeReference(type, factoryPids));
                    } else {
                        error("schemagen.duplicate.pid", pid);
                        continue;
                    }
                }
            } else {
                // TODO we have two OCDs with the same pid. This is most likely to happen if we applied an iFix so picking one is ok, not ideal, but ok.
            }
        }
    }

    private void processExtensions(String pid, OCDType type) {
        ExtendedObjectClassDefinition ocd = type.getObjectClassDefinition();

        type.setSupportsExtensions(ocd.supportsExtensions());

        String parentPid = ocd.getParentPID();
        if (parentPid != null) {
            // it's a child configuration
            if ((type.getAliasName() == null) && (type.getChildAliasName() == null)) {
                error("schemagen.alias.required", pid);
                return;
            }
            if (type.isInternal()) {
                return;
            }
            type.setParentPids(SchemaGenConstants.COMMA_PATTERN.split(parentPid));
            children.add(type);
        }

        String localization = ocd.getLocalization();
        if (localization != null) {
            ResourceBundle resourceBundle = getResourceBundle(type.metatype.getBundle(), localization);
            type.setResourceBundle(resourceBundle);
        }
    }

    private void error(String message, Object... args) {
        if (!isRuntime) {
            // If the generator is being run by the JMX MBean, there may be references
            // that point to bundles that are not included in the runtime. In this case,
            // we don't want to report errors here.
            Tr.error(tc, message, args);
        }
        if (!ignoreErrors) {
            throw new RuntimeException("Error during schema generation");
        }
    }

    public void buildStart() {}

    public void build(MetaTypeInformation metatype) {
        buildTypes(metatype, metatype.getPids(), false);
        buildTypes(metatype, metatype.getFactoryPids(), true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Bundle mb = metatype.getBundle();
            if (mb != null) {
                Tr.debug(tc, "Currently processing metatype from bundle " + mb.getSymbolicName() + "_" + mb.getVersion());
            }
            Tr.debug(tc, "Pids processed: " + Arrays.toString(metatype.getPids()));
            Tr.debug(tc, "Factory Pids processed: " + Arrays.toString(metatype.getFactoryPids()));
        }
    }

    public void buildComplete() {
        processExtends();
        buildNestedTypeInfo();
        processPidReferences();
    }

    private void processExtends() {
        for (OCDType type : ocdTypeMap.values()) {
            String parentPid = type.getObjectClassDefinition().getExtends();
            if (parentPid != null) {
                OCDType parentType = ocdTypeMap.get(parentPid);
                if (parentType != null) {
                    parentType.addExtension(type);
                }
            }
        }
    }

    private void buildNestedTypeInfo() {
        for (OCDType child : children) {
            if (!child.getHasFactoryReference() && !child.getHasIBMFinalWithDefault()) {
                error("schemagen.invalid.child", child.getObjectClassDefinition().getID());
                continue;
            }
            String[] parentPids = child.getParentPids();
            if (parentPids.length == 1 && parentPids[0].length() == 0) {
                // add child to all supported parents
                for (Map.Entry<String, OCDTypeReference> entry : pidTypeMap.entrySet()) {
                    OCDTypeReference ocdReference = entry.getValue();
                    OCDType parent = ocdReference.getOCDType();
                    if (parent.getSupportsExtensions()) {
                        parent.addChild(child);
                    }
                }
            } else {
                // add child to a set of supported parents
                for (String parentPid : parentPids) {
                    OCDTypeReference ocdReference = pidTypeMap.get(parentPid);
                    if (ocdReference == null) {
                        // if the reference is null, we may be extending an internal PID, so we need to check that map.
                        ocdReference = internalPidTypeMap.get(parentPid);
                        if (ocdReference == null) {
                            error("schemagen.invalid.parent", parentPid, child.getObjectClassDefinition().getID());
                            continue;
                        }
                    }

                    OCDType parent = ocdReference.getOCDType();

                    if (!parent.getSupportsExtensions()) {
                        error("schemagen.noextensions", parentPid, child.getObjectClassDefinition().getID());
                    }

                    parent.addChild(child);
                }
            }
        }
    }

    private void processPidReferences() {
        for (Map.Entry<String, TypeBuilder.OCDTypeReference> pidEntry : pidTypeMap.entrySet()) {
            ExtendedObjectClassDefinition ocd = pidEntry.getValue().getOCDType().getObjectClassDefinition();
            for (Map.Entry<String, ExtendedAttributeDefinition> attributeEntry : ocd.getAttributeMap().entrySet()) {
                ExtendedAttributeDefinition attributeDef = attributeEntry.getValue();
                if (attributeDef.getType() == MetaTypeFactory.PID_TYPE || (attributeDef.getType() == AttributeDefinition.STRING && attributeDef.getUIReference() != null)) {
                    List<String> referencePids = new ArrayList<String>();
                    if (attributeDef.getUIReference() != null) {
                        referencePids.addAll(attributeDef.getUIReference());
                    } else {
                        if (attributeDef.getReferencePid() != null) {
                            referencePids.add(attributeDef.getReferencePid());
                        } else if (attributeDef.getService() != null) {
                            referencePids.addAll(getServiceMatches(attributeDef.getService()));
                        } else {
                            error("schemagen.bad.reference.extension", attributeDef.getID());
                            continue;
                        }
                    }

                    for (String referencePid : referencePids) {
                        OCDTypeReference ocdReference = pidTypeMap.get(referencePid);
                        if (ocdReference == null) {
                            // if the reference is null, we may be extending an internal PID, so we need to check that map.
                            ocdReference = internalPidTypeMap.get(referencePid);
                            if (ocdReference == null) {
                                error("schemagen.bad.reference.pid", referencePid);
                                continue;
                            }
                        }
                        ocdReference.getOCDType().setPidReferenced(true);
                    }
                }
            }
        }
    }

    public boolean processAttributes(ObjectClassDefinition ocd) {
        boolean response = false;

        AttributeDefinition[] attributeList = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
        for (AttributeDefinition ad : attributeList) {
            String[] defaultValue = ad.getDefaultValue();
            ExtendedAttributeDefinition ead = new ExtendedAttributeDefinitionImpl(ad);

            if (ad.getID().equalsIgnoreCase("id") && ead.isFinal() && (defaultValue != null && defaultValue.length > 0)) {
                response = true;
            }

        }
        return response;
    }

    /**
     * @param service
     * @return
     */
    protected Collection<String> getServiceMatches(String service) {
        if (service == null) {
            return Collections.emptyList();
        }
        List<String> pids = new ArrayList<String>();
        for (OCDType ocd : getTypes()) {
            List<String> objectClass = ocd.getObjectClassDefinition().getObjectClass();
            if (objectClass != null && objectClass.contains(service)) {
                pids.add(ocd.pid);
            }
        }
        return pids;
    }

    private void addAlias(String aliasName, OCDType type) {
        List<OCDType> aliases = aliasMap.get(aliasName);
        if (aliases == null) {
            aliases = new ArrayList<OCDType>();
            aliasMap.put(aliasName, aliases);
        }
        aliases.add(type);
    }

    public Collection<OCDType> getTypes() {
        return ocdTypeMap.values();
    }

    public Map<String, OCDTypeReference> getPidTypeMap() {
        return pidTypeMap;
    }

    public Map<String, OCDTypeReference> getInternalPidTypeMap() {
        return internalPidTypeMap;
    }

    public OCDTypeReference getPidType(String pid) {
        return pidTypeMap.get(pid);
    }

    public OCDTypeReference getInternalPidType(String pid) {
        return internalPidTypeMap.get(pid);
    }

    public Map<String, List<OCDType>> getAliasMap() {
        return aliasMap;
    }

    private URL findResourceBundle(Bundle bundle, String baseName, String locale) {
        String lookupName;
        if (locale != null && locale.length() > 0) {
            lookupName = baseName + "_" + locale + ".properties";
        } else {
            lookupName = baseName + ".properties";
        }
        URL rbName = bundle.getEntry(lookupName);
        if (rbName == null && locale != null) {
            int pos = locale.lastIndexOf('_');
            if (pos != -1) {
                rbName = findResourceBundle(bundle, baseName, locale.substring(0, pos));
            }
        }
        return rbName;
    }

    @FFDCIgnore(IOException.class)
    private ResourceBundle getResourceBundle(Bundle bundle, String baseName) {
        URL rbName = findResourceBundle(bundle, baseName, locale.toString());
        if (rbName == null) {
            rbName = findResourceBundle(bundle, baseName, null);
        }
        if (rbName != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Loading resource bundle from " + rbName);
            }
            InputStream in = null;
            try {
                in = rbName.openStream();
                return new PropertyResourceBundle(in);
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getResourceBundle(). Exception while loading resource bundle. Message = " + e.getMessage());
                }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception ignore) {
                    }
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Resource bundle not found for " + baseName);
            }
        }
        return null;
    }

    public static class OCDTypeReference {
        private final OCDType ocdType;
        private final boolean isFactoryReference;

        public OCDTypeReference(OCDType ocdType, boolean isFactoryReference) {
            this.ocdType = ocdType;
            this.isFactoryReference = isFactoryReference;
        }

        public OCDType getOCDType() {
            return ocdType;
        }

        public String getOcdTypeName() {
            return ocdType.getTypeName();
        }

        public boolean isFactoryReference() {
            return isFactoryReference;
        }

        public boolean isInternal() {
            return ocdType.isInternal();
        }
    }

    static class OCDType {

        private final MetaTypeInformation metatype;
        private final ExtendedObjectClassDefinition ocd;
        private boolean hasFactoryReference;
        private boolean supportsExtensions;
        private String[] parentPids;
        private boolean pidReferenced;
        private Set<OCDType> children;
        private Set<OCDType> extensions;
        private List<AppInfoEntry> appInfo;
        private Set<String> groups;
        private ResourceBundle resourceBundle;
        private final String pid;
        private final List<String> excludedChildren;
        private boolean hasIBMFinalWithDefault;

        public OCDType(MetaTypeInformation metatype, String pid, ExtendedObjectClassDefinition ocd) {
            this.metatype = metatype;
            this.ocd = ocd;
            this.pid = pid;
            excludedChildren = new ArrayList<String>();

            String excludedChildrenRaw = ocd.getExcludedChildren();
            if (excludedChildrenRaw != null) {
                for (String childPid : excludedChildrenRaw.split(",")) {
                    if (childPid != null) {
                        excludedChildren.add(childPid.trim());
                    }
                }
            }
        }

        public void addExtension(OCDType type) {
            if (extensions == null) {
                extensions = new LinkedHashSet<OCDType>(); // _Linked_HashSet to preserve insertion order for tools
            }
            extensions.add(type);
        }

        public Set<OCDType> getExtensions() {
            return extensions == null ? Collections.<OCDType> emptySet() : extensions;
        }

        public boolean isInternal() {
            return "internal".equals(ocd.getName());
        }

        public ExtendedObjectClassDefinition getObjectClassDefinition() {
            return ocd;
        }

        public MetaTypeInformation getMetaTypeInformation() {
            return metatype;
        }

        public boolean getHasFactoryReference() {
            return hasFactoryReference;
        }

        public void setHasFactoryReference(boolean hasFactoryReference) {
            this.hasFactoryReference = hasFactoryReference;
        }

        public String getAliasName() {
            return ocd.getAlias();
        }

        public String getChildAliasName() {
            return ocd.getChildAlias();
        }

        public void setSupportsExtensions(boolean supportsExtensions) {
            this.supportsExtensions = supportsExtensions;
        }

        public boolean getSupportsExtensions() {
            return supportsExtensions;
        }

        public void setPidReferenced(boolean pidReferenced) {
            this.pidReferenced = pidReferenced;
        }

        /*
         * Returns true if some other type has an attribute with ibm:reference or ibm:filter to this type.
         * False, otherwise.
         */
        public boolean isPidReferenced() {
            return pidReferenced;
        }

        public void setParentPids(String[] parentPids) {
            this.parentPids = parentPids;
        }

        public String[] getParentPids() {
            return parentPids;
        }

        public boolean getHasExtraProperties() {
            return ocd.hasExtraProperties();
        }

        public String getAction() {
            return ocd.getAction();
        }

        public boolean hasParentPids() {
            return (parentPids != null && parentPids.length > 0);
        }

        public boolean getHasIBMFinalWithDefault() {
            return hasIBMFinalWithDefault;
        }

        public void setHasIBMFinalWithDefault(boolean hasIBMFinalWithDefault) {
            this.hasIBMFinalWithDefault = hasIBMFinalWithDefault;
        }

        List<String> getExcludedChildren() {
            return this.excludedChildren;
        }

        public void addChild(OCDType type) {
            if (children == null) {
                children = new LinkedHashSet<OCDType>(); // _Linked_HashSet to preserve insertion order for tools
            }

            if (!!!excludedChildren.contains(type.getTypeName())) {
                children.add(type);

                if (extensions != null) {
                    for (OCDType t : extensions) {
                        t.addChild(type);
                    }
                }
            }
        }

        public Collection<OCDType> getChildren() {
            return (children == null) ? Collections.<OCDType> emptyList() : children;
        }

        public String getTypeName() {
            return pid;
        }

        protected void setResourceBundle(ResourceBundle resourceBundle) {
            this.resourceBundle = resourceBundle;
        }

        private void initAppInfo() {
            if (appInfo == null) {
                appInfo = new ArrayList<AppInfoEntry>();
                String name = ocd.getName();
                if (name != null) {
                    appInfo.add(AppInfoEntry.createLabelTag(name, null));
                }
                if (ocd.hasExtraProperties()) {
                    appInfo.add(AppInfoEntry.createExtraPropertiesTag());
                }
            }
        }

        public void addAppInfoEntry(AppInfoEntry entry) {
            initAppInfo();
            appInfo.add(entry);
        }

        public AppInfoEntry[] getAppInfoEntries() {
            initAppInfo();
            AppInfoEntry[] entries = new AppInfoEntry[appInfo.size()];
            return appInfo.toArray(entries);
        }

        public void addGroup(String group) {
            if (groups == null) {
                groups = new LinkedHashSet<String>(); // _Linked_HashSet to preserve insertion order for tools
                groups.add(group);
                addAppInfoEntry(createGroupDeclaration(group));
            } else if (!groups.contains(group)) {
                groups.add(group);
                addAppInfoEntry(createGroupDeclaration(group));
            }
        }

        private AppInfoEntry createGroupDeclaration(String group) {
            String label = getTranslatedText(getLabelKeys(group));
            String description = getTranslatedText(getDescriptionKeys(group));
            return AppInfoEntry.createGroupDeclarationTag(group, label, description);
        }

        public String[] getLabelKeys(String prefix) {
            return new String[] { prefix + "." + ocd.getID() + ".name",
                                  prefix + ".name" };
        }

        public String[] getDescriptionKeys(String prefix) {
            return new String[] { prefix + "." + ocd.getID() + ".description",
                                  prefix + ".description" };
        }

        @FFDCIgnore(MissingResourceException.class)
        public String getTranslatedText(String... keys) {
            if (resourceBundle == null) {
                return null;
            }
            for (String key : keys) {
                try {
                    return resourceBundle.getString(key);
                } catch (MissingResourceException e) {
                    // keep trying
                }
            }
            if (keys.length == 1) {
                return keys[0];
            } else {
                return Arrays.toString(keys);
            }
        }

        /**
         * @return
         */
        public int getXsdAny() {
            return ocd.getXsdAny();
        }

        /**
         * @return
         */
        public String getExtendsAlias() {
            return ocd.getExtendsAlias();
        }

        /**
         * @return
         */
        public boolean isBeta() {
            return ocd.isBeta();
        }
    }

}
