/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal.metatype;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.metatype.EquinoxAttributeDefinition;
import org.osgi.service.metatype.AttributeDefinition;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.xml.internal.XMLConfigConstants;
import com.ibm.ws.config.xml.internal.schema.AttributeDefinitionSpecification;

/**
 *
 */
@Trivial
public class ExtendedAttributeDefinitionImpl implements ExtendedAttributeDefinition {

    private final AttributeDefinition delegate;
    private int cachedType;
    private String group;
    private boolean isFinal;
    private boolean isFlat;
    private boolean isUnique;
    private String referencePid;
    private String service;
    private String serviceFilter;
    private String rename;
    private String requiresFalse;
    private String requiresTrue;
    private String uniqueCategory;
    private String variable;
    private String copyOf;
    private boolean resolveVariables = true;
    private List<String> uiReference;
    private boolean beta;
    private boolean obscure;

    public ExtendedAttributeDefinitionImpl(AttributeDefinition ad) {
        delegate = ad;
        cachedType = ad.getType();
        if (ad instanceof EquinoxAttributeDefinition)
            initFromEquinoxAD();
        else if (ad instanceof WSAttributeDefinitionImpl)
            initFromWSAD();
    }

    private void initFromEquinoxAD() {
        EquinoxAttributeDefinition delegate = (EquinoxAttributeDefinition) this.delegate;
        Set<String> supportedExtensions = delegate.getExtensionUris();
        if (supportedExtensions != null && supportedExtensions.contains(XMLConfigConstants.METATYPE_EXTENSION_URI)) {
            Map<String, String> extensions = delegate.getExtensionAttributes(XMLConfigConstants.METATYPE_EXTENSION_URI);
            String typeStr = extensions.get(ATTRIBUTE_TYPE_NAME);
            if (typeStr != null) {
                Integer cachedType = MetaTypeFactoryImpl.IBM_TYPES.get(typeStr);
                if (cachedType != null) {
                    this.cachedType = cachedType;
                } else {
                    throw new NullPointerException("Unrecognized ibm type: '" + typeStr + "' in AD " + delegate.getID());
                }
            }
            isFinal = extensions.get(FINAL_ATTR_NAME) != null;
            isFlat = extensions.get(FLAT_ATTR_NAME) != null;
            copyOf = extensions.get(COPY_OF_ATTR_NAME);
            isUnique = extensions.get(UNIQUE_ATTR_NAME) != null;
            referencePid = extensions.get(ATTRIBUTE_REFERENCE_NAME);
            service = extensions.get(SERVICE);
            serviceFilter = extensions.get(SERVICE_FILTER);
            rename = extensions.get(RENAME_ATTR_NAME);
            uniqueCategory = extensions.get(UNIQUE_ATTR_NAME);
            variable = extensions.get(VARIABLE_ATTR_NAME);
            beta = "true".equals(extensions.get(BETA_NAME));
            obscure = extensions.get(OBSCURE_NAME) != null;

            String variableResolution = extensions.get(VARIABLE_SUBSTITUTION_NAME);
            if (variableResolution != null && FALSE.equalsIgnoreCase(variableResolution))
                resolveVariables = false;
        }
        if (supportedExtensions != null && supportedExtensions.contains(XMLConfigConstants.METATYPE_UI_EXTENSION_URI)) {
            Map<String, String> uiExtensions = delegate.getExtensionAttributes(XMLConfigConstants.METATYPE_UI_EXTENSION_URI);
            group = uiExtensions.get(GROUP_ATTR_NAME);
            requiresFalse = uiExtensions.get(REQUIRES_FALSE_ATTR_NAME);
            requiresTrue = uiExtensions.get(REQUIRES_TRUE_ATTR_NAME);
            if (uiExtensions.get(UI_REFERENCE) != null) {
                uiReference = Arrays.asList(uiExtensions.get(UI_REFERENCE).split("[, ]+"));
            }
        }
    }

    private void initFromWSAD() {
        WSAttributeDefinitionImpl delegate = (WSAttributeDefinitionImpl) this.delegate;
        isFinal = delegate.isFinal();
        isFlat = delegate.isFlat();
        copyOf = delegate.getCopyOf();
        isUnique = delegate.isUnique();
        referencePid = delegate.getReferencePid();
        service = delegate.getService();
        serviceFilter = delegate.getServiceFilter();
        uniqueCategory = delegate.getUnique();
        variable = delegate.getVariable();
    }

    @Override
    public String toString() {
        return super.toString() + '[' + delegate.getID() + ']';
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return delegate.getName();
    }

    /** {@inheritDoc} */
    @Override
    public String getID() {
        return delegate.getID();
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    /** {@inheritDoc} */
    @Override
    public int getCardinality() {
        return delegate.getCardinality();
    }

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return this.cachedType;
    }

    @Override
    public String getReferencePid() {
        return referencePid;
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public String getServiceFilter() {
        return serviceFilter;
    }

    @Override
    public boolean isBeta() {
        return beta;
    }

    @Override
    public boolean isObscured() {
        return obscure;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.config.WSAttributeDefinition#getCopyOf()
     */
    @Override
    public String getCopyOf() {
        return copyOf;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getOptionValues() {
        return delegate.getOptionValues();
    }

    /** {@inheritDoc} */
    @Override
    public String[] getOptionLabels() {
        return delegate.getOptionLabels();
    }

    /** {@inheritDoc} */
    @Override
    public String validate(String value) {
        return delegate.validate(value);
    }

    /** {@inheritDoc} */
    @Override
    public String[] getDefaultValue() {
        return delegate.getDefaultValue();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFinal() {
        return isFinal;
    }

    /** {@inheritDoc} */
    @Override
    public String getVariable() {
        return variable;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUnique() {
        return isUnique;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.config.internal.services.ExtendedAttributeDefinition#isFlat()
     */
    @Override
    public boolean isFlat() {
        return isFlat;
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueCategory() {
        return uniqueCategory;
    }

    /** {@inheritDoc} */
    @Override
    public String getRequiresTrue() {
        return requiresTrue;
    }

    /** {@inheritDoc} */
    @Override
    public String getRequiresFalse() {
        return requiresFalse;
    }

    /** {@inheritDoc} */
    @Override
    public String getGroup() {
        return group;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.config.internal.services.ExtendedAttributeDefinition#getDelegate()
     */
    @Override
    public AttributeDefinition getDelegate() {
        return this.delegate;
    }

    /** {@inheritDoc} */
    @Override
    public String getRename() {
        return rename;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getExtensionUris() {
        return delegate instanceof EquinoxAttributeDefinition ? ((EquinoxAttributeDefinition) delegate).getExtensionUris() : Collections.<String> emptySet();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getExtensions(String extensionUri) {
        return delegate instanceof EquinoxAttributeDefinition ? ((EquinoxAttributeDefinition) delegate).getExtensionAttributes(extensionUri) : Collections.<String, String> emptyMap();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.config.internal.services.ExtendedAttributeDefinition#getAttributeName()
     */
    @Override
    public String getAttributeName() {
        if (delegate instanceof AttributeDefinitionSpecification) {
            return ((AttributeDefinitionSpecification) delegate).getAttributeName();
        }
        return delegate.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o != null && o instanceof ExtendedAttributeDefinitionImpl) {
            return this.delegate == ((ExtendedAttributeDefinitionImpl) o).delegate;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode() + 1;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition#resolveVariables()
     */
    @Override
    public boolean resolveVariables() {
        return this.resolveVariables;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition#getUIReference()
     */
    @Override
    public List<String> getUIReference() {
        return uiReference;
    }

}
