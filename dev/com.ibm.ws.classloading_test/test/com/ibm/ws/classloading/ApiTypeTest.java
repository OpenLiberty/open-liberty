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

    @Test
    public void testApiTypePlusMinusSetParsingValid() {
        assertEquals(set(SPEC, IBMAPI, API, THIRDPARTY, STABLE), ApiType.createApiTypeSet("+third-party"));
        assertEquals(set(SPEC, IBMAPI, THIRDPARTY, STABLE), ApiType.createApiTypeSet("+third-party, -api"));
        assertEquals(set(SPEC, IBMAPI, THIRDPARTY, STABLE), ApiType.createApiTypeSet("-api, +third-party"));
        assertEquals(set(IBMAPI, API, STABLE), ApiType.createApiTypeSet("-spec"));
        assertEquals(set(SPEC, API, STABLE), ApiType.createApiTypeSet("-ibm-api"));
        assertEquals(set(SPEC, IBMAPI, STABLE), ApiType.createApiTypeSet("-api"));
        assertEquals(set(SPEC, IBMAPI, API), ApiType.createApiTypeSet("-stable"));
        assertEquals(set(SPEC, IBMAPI, API, STABLE), ApiType.createApiTypeSet("+spec"));
        assertEquals(set(SPEC, IBMAPI, API, STABLE), ApiType.createApiTypeSet("+ibm-api"));
        assertEquals(set(SPEC, IBMAPI, API, STABLE), ApiType.createApiTypeSet("+api"));
        assertEquals(set(SPEC, IBMAPI, API, STABLE), ApiType.createApiTypeSet("+stable"));
        assertEquals(set(IBMAPI, API, THIRDPARTY, STABLE), ApiType.createApiTypeSet("-spec,+third-party"));
        assertEquals(set(IBMAPI, API, THIRDPARTY), ApiType.createApiTypeSet("-spec,+third-party, -stable"));
        assertEquals(set(IBMAPI, API, STABLE), ApiType.createApiTypeSet("-spec"));
        assertEquals(set(API, STABLE), ApiType.createApiTypeSet("-spec,-ibm-api"));
        assertEquals(set(STABLE), ApiType.createApiTypeSet("-spec,-ibm-api,-api"));
        assertEquals(set(), ApiType.createApiTypeSet("-spec,-ibm-api,-api,-stable"));
        assertEquals(set(), ApiType.createApiTypeSet("-spec,-ibm-api,-api,-stable,-third-party"));
        assertEquals(set(SPEC, IBMAPI, API, STABLE), ApiType.createApiTypeSet("-third-party"));
    }

    @Test
    public void testApiTypePlusMinusSetParsingInValid() {
        assertEquals(set(), ApiType.createApiTypeSet("-spec,-spec"));
        assertEquals(set(), ApiType.createApiTypeSet("+spec,+spec"));
        assertEquals(set(), ApiType.createApiTypeSet("+api, third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("third-party, +api"));
        assertEquals(set(), ApiType.createApiTypeSet("-api, third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("third-party, -api"));
        assertEquals(set(), ApiType.createApiTypeSet("-api, spec"));
        assertEquals(set(), ApiType.createApiTypeSet("spec, -api"));
        assertEquals(set(), ApiType.createApiTypeSet("+third-party,-third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("-third-party,+third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("third-party,+third-party,-third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("third-party,-third-party,+third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("api,+third-party,-third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("api,-third-party,+third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("api,+third-party,-spec"));
        assertEquals(set(), ApiType.createApiTypeSet("api,-third-party,+spec"));
        assertEquals(set(), ApiType.createApiTypeSet("+third-partttty"));
        assertEquals(set(), ApiType.createApiTypeSet("-third-partttty"));
        assertEquals(set(), ApiType.createApiTypeSet("third-party,+third-party,-api"));
        assertEquals(set(), ApiType.createApiTypeSet("third-party,-third-party,+api"));
        assertEquals(set(), ApiType.createApiTypeSet("third-party,-api,+third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("-api,third-party,+third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("-third-partttty"));
        assertEquals(set(), ApiType.createApiTypeSet("+third-partttty"));
        assertEquals(set(), ApiType.createApiTypeSet("spec ibm-api,random", "junk,api, third-party -third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("spec ibm-api,random", "junk,api, third-party +third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("+api, third-partty"));
        assertEquals(set(), ApiType.createApiTypeSet("-api, third-partty"));
        assertEquals(set(), ApiType.createApiTypeSet("+third-party, -api, -ibm-api, fun"));
        assertEquals(set(), ApiType.createApiTypeSet("+api, third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("-api, third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("+api, +third-party", "+api, third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("-api, +third-party", "-api, third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("spec ibm-api,random", "junk,api, +third-party"));
        assertEquals(set(), ApiType.createApiTypeSet("spec +ibm-api,random", "junk,api, third-party"));
    }

    private static Set<ApiType> set(ApiType... types) {
        return new HashSet<ApiType>(Arrays.asList(types));
    }
}
