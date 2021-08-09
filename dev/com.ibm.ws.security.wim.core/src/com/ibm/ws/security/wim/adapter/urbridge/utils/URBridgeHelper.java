/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.urbridge.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.websphere.security.wim.Service;

public class URBridgeHelper {

    //Map to store object per domain
    private static List<String> personAccountTypeList = Collections.synchronizedList(new ArrayList<String>(1));

    //Map to store object per domain
    private static List<String> groupAccountTypeList = Collections.synchronizedList(new ArrayList<String>(1));

    public URBridgeHelper() {
        super();
    }

    public static void mapSupportedEntityTypeList(List<String> entityTypes) {
        for (int i = 0; i < entityTypes.size(); i++) {
            String name = entityTypes.get(i);
            if (Service.DO_PERSON_ACCOUNT.equalsIgnoreCase(name)) {
                personAccountTypeList.add(name);
            }
            if (Service.DO_GROUP.equalsIgnoreCase(name)) {
                groupAccountTypeList.add(name);
            }
        }
    }

    /**
     * Gets The Person Account List for current domain
     *
     * @return
     */
    public static String getPersonAccountType() {
        return personAccountTypeList.get(0);
    }

    /**
     * Gets the group account List for current domain
     *
     * @return
     */
    public static String getGroupAccountType(){
        return (String)groupAccountTypeList.get(0);
    }
}
