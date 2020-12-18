/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;

public class TestSettings {
    static private final Class<?> thisClass = TestSettings.class;

    private Map<String, String> requestParms = null;

    public TestSettings() {}

    public TestSettings(Map<String, String> inReqParms) {
        if (inReqParms != null) {
            requestParms = new HashMap<String, String>(inReqParms);
        }
    }

    public TestSettings copyTestSettings() {
        return new TestSettings(requestParms);
    }

    public void printTestSettings() {
        String thisMethod = "printTestSettings";

        Log.info(thisClass, thisMethod, "Test Settings: ");
        Log.info(thisClass, thisMethod, "requestParms: " + requestParms);

    }

    public void setRequestParms(Map<String, String> inReqParms) {
        if (inReqParms != null) {
            requestParms = new HashMap<String, String>(inReqParms);
        } else {
            requestParms = null;
        }
    }

    public Map<String, String> getRequestParms() {
        if (requestParms != null) {
            Map<String, String> newReqParms = new HashMap<String, String>(requestParms);
            return newReqParms;
        } else {
            return null;
        }
    }

}