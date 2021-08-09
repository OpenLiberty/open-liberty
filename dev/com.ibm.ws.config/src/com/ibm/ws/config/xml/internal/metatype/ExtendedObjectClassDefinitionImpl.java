/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal.metatype;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.metatype.EquinoxObjectClassDefinition;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.xml.internal.XMLConfigConstants;

/**
 *
 */
public class ExtendedObjectClassDefinitionImpl implements ExtendedObjectClassDefinition {

    //backwards "compatible".  These strings were inadvertently exposed to users as ibm:filter targets.
    //We convert them to ibm:objectClass silently so they can be seen as targets of ibm:service.
    private static final List<String> EXPOSED_FILTERS = Arrays.asList(new String[] { "com.ibm.ws.zos.connect.interceptorType",
                                                                                    "com.ibm.ws.zos.connect.dataXformType",
                                                                                    "com.ibm.ws.zos.connect.serviceType" });

    private final ObjectClassDefinition delegate;
    private final String parentPid;
    private final String alias;
    private final boolean extraProperties;
    private String action;
    private String localization;
    private final boolean supportsExtensions;
    private final boolean supportsHiddenExtensions;
    private final String extendsAlias;
    private final String extendsAttribute;
    private final String childAlias;
    private int anyCount;
    private String excludedChildren;
    private List<String> objectClass;
    private boolean requireExplicitConfiguration;

    //HOPEFULLY TEMPORARY!
    private String pid;

    private boolean beta;

    @Trivial
    public static ExtendedObjectClassDefinitionImpl newExtendedObjectClassDefinition(ObjectClassDefinition ocd, String bundleLocation) {
        if (ocd instanceof EquinoxObjectClassDefinition) {
            return new ExtendedObjectClassDefinitionImpl((EquinoxObjectClassDefinition) ocd, bundleLocation);
        } else if (ocd instanceof WSObjectClassDefinitionImpl) {
            return new ExtendedObjectClassDefinitionImpl((WSObjectClassDefinitionImpl) ocd, bundleLocation);
        }
        return new ExtendedObjectClassDefinitionImpl(ocd);
    }

    /**
     * @param ocd delegate OCD
     * @param bundleLocation bundle location to determine alias prefix
     */
    private ExtendedObjectClassDefinitionImpl(WSObjectClassDefinitionImpl ocd, String bundleLocation) {
        this.delegate = ocd;
        this.parentPid = ocd.getParentPID();
        this.alias = getAliasName(ocd.getAlias(), bundleLocation);
        this.extraProperties = false;
        this.supportsExtensions = ocd.supportsExtensions() || ocd.supportsHiddenExtensions();
        this.supportsHiddenExtensions = ocd.supportsHiddenExtensions();
        this.childAlias = ocd.getChildAlias();
        this.extendsAlias = ocd.getExtendsAlias();
        this.extendsAttribute = ocd.getExtends();
        this.objectClass = ocd.getObjectClass();
        if (ocd instanceof ExtendedObjectClassDefinition) {
            this.anyCount = ((ExtendedObjectClassDefinition) ocd).getXsdAny();
            this.excludedChildren = ((ExtendedObjectClassDefinition) ocd).getExcludedChildren();
            this.action = ((ExtendedObjectClassDefinition) ocd).getAction();
            this.requireExplicitConfiguration = !((ExtendedObjectClassDefinition) ocd).hasAllRequiredDefaults();
            this.beta = ((ExtendedObjectClassDefinition) ocd).isBeta();
        }
    }

    /**
     * @param ocd delegate OCD
     * @param bundleLocation bundle location to determine alias prefix
     */
    private ExtendedObjectClassDefinitionImpl(EquinoxObjectClassDefinition extendedOcd, String bundleLocation) {
        this.delegate = extendedOcd;
        Map<String, String> extensions;
        Map<String, String> uiExtensions;

        Set<String> supportedExtensions = extendedOcd.getExtensionUris();
        if (supportedExtensions != null && supportedExtensions.contains(XMLConfigConstants.METATYPE_EXTENSION_URI)) {
            extensions = extendedOcd.getExtensionAttributes(XMLConfigConstants.METATYPE_EXTENSION_URI);
        } else {
            extensions = Collections.emptyMap();
        }
        if (supportedExtensions != null && supportedExtensions.contains(XMLConfigConstants.METATYPE_UI_EXTENSION_URI)) {
            uiExtensions = extendedOcd.getExtensionAttributes(XMLConfigConstants.METATYPE_UI_EXTENSION_URI);
        } else {
            uiExtensions = Collections.emptyMap();
        }

        this.parentPid = extensions.get(PARENT_PID_ATTRIBUTE);
        this.alias = getAliasName(extensions.get(ALIAS_ATTRIBUTE), bundleLocation);
        this.extraProperties = "true".equalsIgnoreCase(uiExtensions.get(METATYPE_EXTRA_PROPERTIES));
        this.action = extensions.get(METATYPE_ACTION_ATTRIBUTE);
        this.localization = uiExtensions.get(LOCALIZATION_ATTRIBUTE);
        this.supportsExtensions = extensions.get(SUPPORTS_EXTENSIONS_ATTRIBUTE) != null || extensions.get(SUPPORTS_HIDDEN_EXTENSIONS_ATTRIBUTE) != null;
        this.supportsHiddenExtensions = extensions.get(SUPPORTS_HIDDEN_EXTENSIONS_ATTRIBUTE) != null;
        this.extendsAlias = extensions.get(EXTENDS_ALIAS_ATTRIBUTE);
        this.extendsAttribute = extensions.get(EXTENDS_ATTRIBUTE);
        this.childAlias = extensions.get(CHILD_ALIAS_ATTRIBUTE);
        this.excludedChildren = extensions.get(EXCLUDED_CHILDREN_ATTRIBUTE);
        this.requireExplicitConfiguration = "true".equalsIgnoreCase(extensions.get(REQUIRE_EXPLICIT_CONFIGURATION));
        this.beta = "true".equalsIgnoreCase(extensions.get(BETA_ATTRIBUTE));
        for (AttributeDefinition ad : extendedOcd.getAttributeDefinitions(ALL)) {
            if (EXPOSED_FILTERS.contains(ad.getID())) {
                if (objectClass == null) {
                    objectClass = Collections.singletonList(ad.getID());
                } else {
                    if (objectClass.size() == 1) {
                        objectClass = new ArrayList<String>(objectClass);
                    }
                    objectClass.add(ad.getID());
                }
            }
        }
        String objectcl = extensions.get(OBJECT_CLASS);
        if (objectcl != null) {
            if (objectClass == null) {
                objectClass = Arrays.asList(objectcl.split("[, ]+"));
            } else {
                if (objectClass.size() == 1) {
                    objectClass = new ArrayList<String>(objectClass);
                }
                for (String service : objectcl.split("[, ]+")) {
                    if (!objectClass.contains(service)) {
                        objectClass.add(service);
                    }
                }
            }
        }
        String anyVal = extensions.get(XSD_ANY_ATTRIBUTE);
        if (anyVal != null) {
            try {
                this.anyCount = Integer.parseInt(anyVal);
            } catch (NumberFormatException nfe) {
                // ignore this and leave at 0
            }
        }

    }

    /**
     * @param delegate delegate OCD
     */
    private ExtendedObjectClassDefinitionImpl(ObjectClassDefinition delegate) {
        this.delegate = delegate;
        this.parentPid = null;
        this.alias = null;
        this.extraProperties = false;
        this.localization = null;
        this.supportsExtensions = false;
        this.supportsHiddenExtensions = false;
        this.extendsAlias = null;
        this.extendsAttribute = null;
        this.childAlias = null;
        this.excludedChildren = null;
        this.requireExplicitConfiguration = false;
    }

    @Override
    public String toString() {
        return super.toString() + '[' + delegate.getID() + ']';
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String getName() {
        return delegate.getName();
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String getID() {
        return delegate.getID();
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String getDescription() {
        return delegate.getDescription();
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public AttributeDefinition[] getAttributeDefinitions(int filter) {
        return delegate.getAttributeDefinitions(filter);
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public InputStream getIcon(int size) throws IOException {
        return delegate.getIcon(size);
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String getAlias() {
        return this.alias;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public List<String> getObjectClass() {
        return this.objectClass;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String getParentPID() {
        return this.parentPid;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public boolean hasExtraProperties() {
        return this.extraProperties;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String getLocalization() {
        return this.localization;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public ObjectClassDefinition getDelegate() {
        return this.delegate;
    }

    /** {@inheritDoc} */
    //ONLY USED BY SCHEMA WRITER
    @Override
    @Trivial
    public Map<String, ExtendedAttributeDefinition> getAttributeMap() {
        Map<String, ExtendedAttributeDefinition> map = null;
        AttributeDefinition[] attrDefs = getAttributeDefinitions(ObjectClassDefinition.ALL);
        if (attrDefs != null) {
            map = new HashMap<String, ExtendedAttributeDefinition>();
            for (AttributeDefinition attrDef : attrDefs) {
                map.put(attrDef.getID(), new ExtendedAttributeDefinitionImpl(attrDef));
            }
        } else {
            map = Collections.emptyMap();
        }
        return map;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public boolean hasAllRequiredDefaults() {
        if (requireExplicitConfiguration) {
            return false;
        }
        AttributeDefinition[] requiredAttributes = getAttributeDefinitions(ObjectClassDefinition.REQUIRED);
        if (requiredAttributes != null) {
            for (AttributeDefinition attrDef : requiredAttributes) {
                String[] defaultValues = attrDef.getDefaultValue();
                if (defaultValues == null)
                    return false;
            }
        }
        AttributeDefinition[] attrDefs = getAttributeDefinitions(ObjectClassDefinition.ALL);
        if (attrDefs != null) {
            for (AttributeDefinition attrDef : attrDefs) {
                String[] defaultValues = attrDef.getDefaultValue();
                if (defaultValues != null && defaultValues.length > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.config.internal.services.ExtendedObjectClassDefinition#getRequiredAttributes()
     */
    @Override
    @Trivial
    public List<AttributeDefinition> getRequiredAttributes() {
        return Arrays.asList(getAttributeDefinitions(REQUIRED));

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.config.xml.internal.metatype.ExtendedObjectClassDefinition#getExtendsAlias()
     */
    @Override
    public String getExtendsAlias() {
        return extendsAlias;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String getExtends() {
        return this.extendsAttribute;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.config.internal.services.ExtendedObjectClassDefinition#getChildAlias()
     */
    @Override
    @Trivial
    public String getChildAlias() {
        return this.childAlias;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.config.internal.services.ExtendedObjectClassDefinition#supportsExtensions()
     */
    @Override
    @Trivial
    public boolean supportsExtensions() {
        return this.supportsExtensions;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.config.WSObjectClassDefinition#supportsHiddenExtensions()
     */
    @Override
    @Trivial
    public boolean supportsHiddenExtensions() {
        return this.supportsHiddenExtensions;
    }

    /**
     * Prefixes the alias with the product extension name if there is a product extension
     * associated to this OCD.
     * 
     * @param alias The alias name to process.
     * @param bundleLocation bundle location to analyze to provide prefix for the alias
     * @return The new alias possibly including a prefix based on the bundle location.
     */
    private static String getAliasName(String alias, String bundleLocation) {
        String newAlias = alias;

        if (alias != null && !alias.isEmpty()) {
            try {
                if (bundleLocation != null && !bundleLocation.isEmpty()) {
                    if (bundleLocation.startsWith(XMLConfigConstants.BUNDLE_LOC_KERNEL_TAG)) {
                        // nothing to do. The alias is returned.
                    } else if (bundleLocation.startsWith(XMLConfigConstants.BUNDLE_LOC_FEATURE_TAG)) {
                        // Check for the presence of a product extension location.
                        int index = bundleLocation.indexOf(XMLConfigConstants.BUNDLE_LOC_PROD_EXT_TAG);
                        if (index != -1) {
                            index += XMLConfigConstants.BUNDLE_LOC_PROD_EXT_TAG.length();
                            int endIndex = bundleLocation.indexOf(":", index);
                            String productName = bundleLocation.substring(index, endIndex);
                            newAlias = productName + "_" + alias;
                        }
                    } else if (bundleLocation.startsWith(XMLConfigConstants.BUNDLE_LOC_CONNECTOR_TAG)) {
                        // nothing to do. The alias is returned.
                    } else {
                        // Unknown location. Ignore the alias. If bundles are installed through fileInstall,
                        // bundle resolution should happen through the pid or factoryPid.
                        newAlias = null;
                    }
                }
            } catch (Throwable t) {
                // An exception here would be bad. Need an ffdc.
            }
        }

        return newAlias;
    }

    @Override
    public int getXsdAny() {
        return anyCount;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.config.xml.internal.metatype.ExtendedObjectClassDefinition#getExcludedChildren()
     */
    @Override
    public String getExcludedChildren() {
        return excludedChildren;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.config.xml.internal.metatype.ExtendedObjectClassDefinition#getAction()
     */
    @Override
    public String getAction() {
        return action;
    }

    //HOPEFULLY TEMPORARY!
    /**
     * @return the pid
     */
    public String getPid() {
        return pid;
    }

    /**
     * @param pid the pid to set
     */
    public void setPid(String pid) {
        this.pid = pid;
    }

    @Override
    public boolean isBeta() {
        return beta;
    }

}
