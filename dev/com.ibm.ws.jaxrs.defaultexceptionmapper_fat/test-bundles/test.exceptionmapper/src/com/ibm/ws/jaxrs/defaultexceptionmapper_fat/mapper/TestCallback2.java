/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.defaultexceptionmapper_fat.mapper;

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.container.ResourceInfo;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.jaxrs.defaultexceptionmapper.DefaultExceptionMapperCallback;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class TestCallback2 implements DefaultExceptionMapperCallback {

    public static final String TEST_CALLBACK2_HEADER = "TestCallback2";
    public static final String TEST_CALLBACK2_VALUE = "TestCallback2Value";

    @Override
    public Map<String, Object> onDefaultMappedException(Throwable throwable, int statusCode, ResourceInfo resourceInfo) {
        return Collections.singletonMap(TEST_CALLBACK2_HEADER, TEST_CALLBACK2_VALUE);
    }

}
