/*******************************************************************************
 * Copyright (c) 2013,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.osgi.service.metatype.AttributeDefinition;

import com.ibm.websphere.config.ConfigEvaluatorException;
import com.ibm.websphere.config.ConfigUpdateException;
import com.ibm.websphere.config.WSConfigurationHelper;
import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.xml.internal.ConfigEvaluator.EvaluationResult;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 *
 */
public class WSConfigurationHelperImpl implements WSConfigurationHelper {

    private final MetaTypeRegistry metatypeRegistry;
    private final ConfigEvaluator configEvaluator;
    private final BundleProcessor bundleProcessor;

    public WSConfigurationHelperImpl(MetaTypeRegistry registry, ConfigEvaluator ce, BundleProcessor bundleProcessor) {
        this.metatypeRegistry = registry;
        this.configEvaluator = ce;
        this.bundleProcessor = bundleProcessor;
    }

    @Override
    public Dictionary<String, Object> getMetaTypeDefaultProperties(String factoryPid) throws ConfigEvaluatorException {
        RegistryEntry registry = metatypeRegistry.getRegistryEntry(factoryPid);
        if (registry == null) {
            return null;
        }

        ConfigElement element = new SimpleElement(factoryPid);
        EvaluationResult result = configEvaluator.evaluate(element, registry, "", true);
        //Since we are just "evaluating" to get the defaults from metatype, we don't check for invalid result due to missing values.
        return result.getProperties();

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.config.WSConfigurationHelper#addDefaultConfiguration(java.lang.String, java.util.Dictionary)
     */
    @Override
    public void addDefaultConfiguration(String pid, Dictionary<String, String> properties) throws ConfigUpdateException {
        bundleProcessor.addDefaultConfiguration(pid, properties);
    }

    @Override
    public void addDefaultConfiguration(InputStream defaultConfig) throws ConfigUpdateException {
        bundleProcessor.addDefaultConfiguration(defaultConfig);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.config.WSConfigurationHelper#removeDefaultConfiguration(java.lang.String)
     */
    @Override
    public boolean removeDefaultConfiguration(String pid) throws ConfigUpdateException {
        return bundleProcessor.removeDefaultConfiguration(pid);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.config.WSConfigurationHelper#removeDefaultConfiguration(java.lang.String, java.lang.String)
     */
    @Override
    public boolean removeDefaultConfiguration(String pid, String id) throws ConfigUpdateException {
        return bundleProcessor.removeDefaultConfiguration(pid, id);
    }

    @Override
    @Trivial
    public Object convert(String pid, String attributeID, String strVal) {
        RegistryEntry ent = metatypeRegistry.getRegistryEntryByPidOrAlias(pid);
        if (ent != null) {
            Map<String, ExtendedAttributeDefinition> attributeMap;
            attributeMap = ent.getAttributeMap();
            if (attributeMap != null) {
                ExtendedAttributeDefinition ad = attributeMap.get(attributeID);
                if (ad != null) {
                    // The following is copied from ConfigEvaluator, excluding pid types, which are handled differently
                    int type = ad.getType();
                    try {
                        if (type == AttributeDefinition.BOOLEAN) {
                            return Boolean.valueOf(strVal);
                        } else if (type == AttributeDefinition.BYTE) {
                            return Byte.valueOf(strVal);
                        } else if (type == AttributeDefinition.CHARACTER) {
                            return Character.valueOf(strVal.charAt(0));
                        } else if (type == AttributeDefinition.DOUBLE) {
                            return Double.valueOf(strVal);
                        } else if (type == AttributeDefinition.FLOAT) {
                            return Float.valueOf(strVal);
                        } else if (type == AttributeDefinition.INTEGER) {
                            return Integer.valueOf(strVal);
                        } else if (type == AttributeDefinition.LONG) {
                            return Long.valueOf(strVal);
                        } else if (type == AttributeDefinition.SHORT) {
                            return Short.valueOf(strVal);
                        } else if (type == MetaTypeFactory.DURATION_TYPE) {
                            return MetatypeUtils.evaluateDuration(strVal, TimeUnit.MILLISECONDS);
                        } else if (type == MetaTypeFactory.DURATION_S_TYPE) {
                            return MetatypeUtils.evaluateDuration(strVal, TimeUnit.SECONDS);
                        } else if (type == MetaTypeFactory.DURATION_M_TYPE) {
                            return MetatypeUtils.evaluateDuration(strVal, TimeUnit.MINUTES);
                        } else if (type == MetaTypeFactory.DURATION_H_TYPE) {
                            return MetatypeUtils.evaluateDuration(strVal, TimeUnit.HOURS);
                        } else if (ad.isObscured() || type == MetaTypeFactory.PASSWORD_TYPE || type == MetaTypeFactory.HASHED_PASSWORD_TYPE) {
                            return new SerializableProtectedString(strVal.toCharArray());
                        } else if (type == MetaTypeFactory.ON_ERROR_TYPE) {
                            return Enum.valueOf(OnError.class, strVal.trim().toUpperCase());
                        } else if (type == MetaTypeFactory.TOKEN_TYPE) {
                            return MetatypeUtils.evaluateToken(strVal);
                        }
                    } catch (Throwable x) {
                        // auto FFDC, and value is returned without conversion
                    }
                }
            }
        }
        return strVal;
    }

    @Override
    @Trivial
    public Integer getMetaTypeAttributeCardinality(String pid, String attributeID) {
        return metatypeRegistry.getAttributeCardinality(pid, attributeID);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.config.WSConfigurationHelper#getMetaTypeAttributeName(java.lang.String, java.lang.String)
     */
    @Override
    public String getMetaTypeAttributeName(String pid, String attributeID) {
        return metatypeRegistry.getAttributeName(pid, attributeID);
    }

    @Override
    public String getMetaTypeElementName(String pid) {
        return metatypeRegistry.getElementName(pid);
    }

    @Override
    public boolean registryEntryExists(String pid) {
        return metatypeRegistry.getRegistryEntryByPidOrAlias(pid) != null ? true : false;
    }

    @Override
    public String aliasFor(String pid, String baseAlias) {
        RegistryEntry ent = metatypeRegistry.getRegistryEntryByPidOrAlias(pid);
        String alias = ent.getAlias();
        if (alias != null)
            return alias;

        String extendsAlias = ent.getExtendsAlias();
        if (extendsAlias == null)
            return baseAlias;
        if (extendsAlias.startsWith("!"))
            return extendsAlias.substring(1);
        return baseAlias == null ? extendsAlias : baseAlias + "." + extendsAlias;
    }
}
