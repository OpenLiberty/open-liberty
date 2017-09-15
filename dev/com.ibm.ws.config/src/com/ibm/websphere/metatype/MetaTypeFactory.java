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
package com.ibm.websphere.metatype;

import java.util.List;

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 *
 */
public interface MetaTypeFactory {

    public static final int DURATION_TYPE = 1000;
    public static final int PID_TYPE = 1001;
    public static final int LOCATION_TYPE = 1002;
    public static final int PASSWORD_TYPE = 1003;
    public static final int DURATION_S_TYPE = 1004;
    public static final int DURATION_M_TYPE = 1005;
    public static final int DURATION_H_TYPE = 1006;
    public static final int ON_ERROR_TYPE = 1007;
    public static final int HASHED_PASSWORD_TYPE = 1008;
    public static final int LOCATION_FILE_TYPE = 1009;
    public static final int LOCATION_DIR_TYPE = 1010;
    public static final int LOCATION_URL_TYPE = 1011;
    public static final int TOKEN_TYPE = 1012;

    ObjectClassDefinition createObjectClassDefinition(ObjectClassDefinitionProperties properties,
                                                      List<AttributeDefinition> requiredAttributes, List<AttributeDefinition> optionalAttributes);

    /**
     * @param properties
     * @return
     */
    AttributeDefinition createAttributeDefinition(AttributeDefinitionProperties properties);

    /**
     * @param typeName
     * @return
     */
    Integer getIBMType(String typeName);

}
