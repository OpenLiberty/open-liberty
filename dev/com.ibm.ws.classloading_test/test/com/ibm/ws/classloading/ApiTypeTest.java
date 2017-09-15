/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading;

import static com.ibm.wsspi.classloading.ApiType.API;
import static com.ibm.wsspi.classloading.ApiType.IBMAPI;
import static com.ibm.wsspi.classloading.ApiType.SPEC;
import static com.ibm.wsspi.classloading.ApiType.STABLE;
import static com.ibm.wsspi.classloading.ApiType.THIRDPARTY;
import static junit.framework.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.ibm.wsspi.classloading.ApiType;

public class ApiTypeTest {
    @Test
    public void testApiTypeParsing() {
        assertEquals(API, ApiType.fromString(null));
        assertEquals(null, ApiType.fromString(""));
        assertEquals(null, ApiType.fromString("ibmapi"));
        assertEquals(null, ApiType.fromString("IBMAPI"));
        assertEquals(SPEC, ApiType.fromString("spec"));
        assertEquals(IBMAPI, ApiType.fromString("ibm-api"));
        assertEquals(API, ApiType.fromString("api"));
        assertEquals(THIRDPARTY, ApiType.fromString("third-party"));
        assertEquals(STABLE, ApiType.fromString("stable"));
    }

    @Test
    public void testApiTypeSetParsing() {
        assertEquals(set(), ApiType.createApiTypeSet(""));
        assertEquals(set(), ApiType.createApiTypeSet("rubbish"));
        assertEquals(set(), ApiType.createApiTypeSet((String[]) null));
        assertEquals(set(), ApiType.createApiTypeSet(null, null));
        assertEquals(set(SPEC), ApiType.createApiTypeSet("spec"));
        assertEquals(set(SPEC, IBMAPI), ApiType.createApiTypeSet("spec,ibm-api"));
        assertEquals(set(SPEC, IBMAPI, API, THIRDPARTY, STABLE), ApiType.createApiTypeSet("spec ibm-api,random,junk,api, third-party, stable"));
        assertEquals(set(SPEC, IBMAPI, API, THIRDPARTY), ApiType.createApiTypeSet("spec ibm-api,random", "junk,api, third-party"));
    }

    private static Set<ApiType> set(ApiType... types) {
        return new HashSet<ApiType>(Arrays.asList(types));
    }
}
