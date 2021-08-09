/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.equinox.metatype.EquinoxAttributeDefinition;
import org.eclipse.equinox.metatype.EquinoxObjectClassDefinition;

import com.ibm.ws.config.xml.internal.metatype.ExtendedObjectClassDefinition;

public class MockObjectClassDefinition extends BaseDefinition implements EquinoxObjectClassDefinition {

    private final Map<String, EquinoxAttributeDefinition> requiredAttributeMap;
    private final Map<String, EquinoxAttributeDefinition> optionalAttributeMap;

    public MockObjectClassDefinition(String id) {
        super(id);
        this.requiredAttributeMap = new HashMap<String, EquinoxAttributeDefinition>();
        this.optionalAttributeMap = new HashMap<String, EquinoxAttributeDefinition>();
    }

    public void addAttributeDefinition(EquinoxAttributeDefinition attributeDef) {
        addAttributeDefinition(attributeDef, true);
    }

    public void addAttributeDefinition(EquinoxAttributeDefinition attributeDef, boolean required) {
        if (required) {
            requiredAttributeMap.put(attributeDef.getID(), attributeDef);
        } else {
            optionalAttributeMap.put(attributeDef.getID(), attributeDef);
        }
    }

    public Map<String, EquinoxAttributeDefinition> getAttributeDefinitionMap() {
        Map<String, EquinoxAttributeDefinition> all = new HashMap<String, EquinoxAttributeDefinition>();
        all.putAll(requiredAttributeMap);
        all.putAll(optionalAttributeMap);
        return all;
    }

    @Override
    public EquinoxAttributeDefinition[] getAttributeDefinitions(int filter) {
        Collection<EquinoxAttributeDefinition> attributes;
        if (filter == EquinoxObjectClassDefinition.REQUIRED) {
            attributes = requiredAttributeMap.values();
        } else if (filter == EquinoxObjectClassDefinition.OPTIONAL) {
            attributes = optionalAttributeMap.values();
        } else {
            attributes = new HashSet<EquinoxAttributeDefinition>();
            attributes.addAll(requiredAttributeMap.values());
            attributes.addAll(optionalAttributeMap.values());
        }
        return attributes.toArray(new EquinoxAttributeDefinition[attributes.size()]);
    }

    @Override
    public InputStream getIcon(int arg0) throws IOException {
        return null;
    }

    public void setSupportsExtensions(boolean supportsExtensions) {
        setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedObjectClassDefinition.SUPPORTS_EXTENSIONS_ATTRIBUTE,
                              (supportsExtensions) ? "true" : "false");
    }

    public void setSupportsHiddenExtensions(boolean supportsHiddenExtensions) {
        setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedObjectClassDefinition.SUPPORTS_HIDDEN_EXTENSIONS_ATTRIBUTE,
                              (supportsHiddenExtensions) ? "true" : "false");
    }

    public void setObjectClass(String objectClass) {
        setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedObjectClassDefinition.OBJECT_CLASS, objectClass);
    }

    public void setParentPid(String parentPid) {
        setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedObjectClassDefinition.PARENT_PID_ATTRIBUTE, parentPid);
    }

    public void setAlias(String alias) {
        setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedObjectClassDefinition.ALIAS_ATTRIBUTE, alias);
    }

    public String getAlias() {
        return getExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedObjectClassDefinition.ALIAS_ATTRIBUTE);
    }

    public void setExtraProperties(boolean value) {
        setExtensionAttribute(XMLConfigConstants.METATYPE_UI_EXTENSION_URI, ExtendedObjectClassDefinition.METATYPE_EXTRA_PROPERTIES, String.valueOf(value));
    }

    /**
     * @param aliasName
     */
    public void setChildAlias(String aliasName) {
        setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedObjectClassDefinition.CHILD_ALIAS_ATTRIBUTE, aliasName);

    }

    public void setExtendsAlias(String aliasName) {
        setExtensionAttribute(XMLConfigConstants.METATYPE_EXTENSION_URI, ExtendedObjectClassDefinition.EXTENDS_ALIAS_ATTRIBUTE, aliasName);

    }
}
