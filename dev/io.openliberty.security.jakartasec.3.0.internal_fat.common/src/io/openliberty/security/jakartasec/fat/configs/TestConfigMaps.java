/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.configs;

import java.util.HashMap;
import java.util.Map;

import io.openliberty.security.jakartasec.fat.utils.Constants;

public class TestConfigMaps {

    /*************** OpenIdAuthenticationMechanismDefinition ************/
    public static Map<String, Object> getTest1() throws Exception {

        Map<String, Object> test1 = new HashMap<String, Object>();
        test1.put(Constants.CLIENT_SECRET, "myDogHasFleas");
        test1.put(Constants.CLIENT_ID, Constants.EMPTY_VALUE);
        return test1;
    }

    public static Map<String, Object> getBadClientId() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CLIENT_ID, "badCLient");
        return updatedMap;
    }

    public static Map<String, Object> getEmptyClientId() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CLIENT_ID, Constants.EMPTY_VALUE);
        return updatedMap;
    }

    public static Map<String, Object> getBadClientSecret() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CLIENT_SECRET, "badCLientSecret");
        //        updatedMap.put(Constants.CLIENT_SECRET, "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger");
        return updatedMap;
    }

    public static Map<String, Object> getEmptyClientSecret() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CLIENT_SECRET, Constants.EMPTY_VALUE);
        return updatedMap;
    }

    public static Map<String, Object> getUseSessionExpressionTrue() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.USE_SESSION_EXPRESSION, String.valueOf(true));
        return updatedMap;
    }

    public static Map<String, Object> getUseSessionExpressionFalse() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.USE_SESSION_EXPRESSION, String.valueOf(false));
        return updatedMap;
    }

    /****************** ClaimDefinitions ********************/
    public static Map<String, Object> getBadCallerNameClaim() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CALLER_NAME_CLAIM, "badCallerName");
        return updatedMap;

    }

    public static Map<String, Object> getEmptyCallerNameClaim() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CALLER_NAME_CLAIM, Constants.EMPTY_VALUE);
        return updatedMap;

    }

    public static Map<String, Object> getBadCallerGroupsClaim() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CALLER_GROUPS_CLAIM, "badCallerGroups");
        return updatedMap;

    }

    public static Map<String, Object> getEmptyCallerGroupsClaim() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.CALLER_GROUPS_CLAIM, Constants.EMPTY_VALUE);
        //        updatedMap.put(Constants.CALLER_GROUPS_CLAIM, "groups");
        return updatedMap;

    }
}