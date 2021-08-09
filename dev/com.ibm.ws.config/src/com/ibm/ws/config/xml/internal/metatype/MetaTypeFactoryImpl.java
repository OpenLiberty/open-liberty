/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal.metatype;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.ibm.websphere.metatype.AttributeDefinitionProperties;
import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.websphere.metatype.ObjectClassDefinitionProperties;

/**
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class MetaTypeFactoryImpl implements MetaTypeFactory {

    static final String DURATION_TYPE_NAME = "duration";
    static final String DURATION_MS_TYPE_NAME = "duration(ms)";
    static final String DURATION_S_TYPE_NAME = "duration(s)";
    static final String DURATION_M_TYPE_NAME = "duration(m)";
    static final String DURATION_H_TYPE_NAME = "duration(h)";
    static final String PID_TYPE_NAME = "pid";
    static final String LOCATION_TYPE_NAME = "location";
    static final String PASSWORD_TYPE_NAME = "password";
    static final String HASHED_PASSWORD_TYPE_NAME = "passwordHash";
    static final String LOCATION_FILE_TYPE_NAME = "location(file)";
    static final String LOCATION_DIR_TYPE_NAME = "location(dir)";
    static final String LOCATION_URL_TYPE_NAME = "location(url)";
    static final String ON_ERROR_TYPE_NAME = "onError";
    static final String TOKEN_NAME = "token";

    static final Map<String, Integer> IBM_TYPES;

    static {
        HashMap<String, Integer> types = new HashMap<String, Integer>();
        types.put(DURATION_TYPE_NAME, MetaTypeFactory.DURATION_TYPE);
        types.put(DURATION_MS_TYPE_NAME, MetaTypeFactory.DURATION_TYPE);
        types.put(DURATION_S_TYPE_NAME, MetaTypeFactory.DURATION_S_TYPE);
        types.put(DURATION_M_TYPE_NAME, MetaTypeFactory.DURATION_M_TYPE);
        types.put(DURATION_H_TYPE_NAME, MetaTypeFactory.DURATION_H_TYPE);
        types.put(PID_TYPE_NAME, MetaTypeFactory.PID_TYPE);
        types.put(LOCATION_TYPE_NAME, MetaTypeFactory.LOCATION_TYPE);
        types.put(LOCATION_FILE_TYPE_NAME, MetaTypeFactory.LOCATION_FILE_TYPE);
        types.put(LOCATION_DIR_TYPE_NAME, MetaTypeFactory.LOCATION_DIR_TYPE);
        types.put(LOCATION_URL_TYPE_NAME, MetaTypeFactory.LOCATION_URL_TYPE);
        types.put(PASSWORD_TYPE_NAME, MetaTypeFactory.PASSWORD_TYPE);
        types.put(HASHED_PASSWORD_TYPE_NAME, MetaTypeFactory.HASHED_PASSWORD_TYPE);
        types.put(ON_ERROR_TYPE_NAME, MetaTypeFactory.ON_ERROR_TYPE);
        types.put(TOKEN_NAME, MetaTypeFactory.TOKEN_TYPE);
        IBM_TYPES = Collections.unmodifiableMap(types);
    }

    protected void activate(ComponentContext ctxt) throws Exception {}

    @Override
    public Integer getIBMType(String typeName) {
        return IBM_TYPES.get(typeName);
    }

    @Override
    public AttributeDefinition createAttributeDefinition(AttributeDefinitionProperties properties) {
        return new WSAttributeDefinitionImpl(properties);
    }

    @Override
    public ObjectClassDefinition createObjectClassDefinition(ObjectClassDefinitionProperties properties,
                                                             List<AttributeDefinition> requiredAttributes, List<AttributeDefinition> optionalAttributes) {
        return new WSObjectClassDefinitionImpl(properties, requiredAttributes, optionalAttributes);
    }

}
