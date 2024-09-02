/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.suite.staging;

import static com.ibm.ws.image.test.InstallProfileTest.DO_SCHEMA_TEST;

import java.util.ArrayList;
import java.util.List;

public class InstallProfileData {
    public static final List<Object[]> TEST_DATA;

    static {
        List<Object[]> profileData = new ArrayList<>();

        profileData.add( new Object[] { "web7", "wlp-webProfile7-", ".zip", DO_SCHEMA_TEST } );        
        profileData.add( new Object[] { "ee7", "wlp-javaee7-", ".zip", !DO_SCHEMA_TEST } );
        profileData.add( new Object[] { "client7", "wlp-javaeeClient7-", ".zip", !DO_SCHEMA_TEST } );

        profileData.add( new Object[] { "kernel", "wlp-kernel-", ".zip", DO_SCHEMA_TEST } );
        profileData.add( new Object[] { "osgi", "wlp-osgi-", ".zip", DO_SCHEMA_TEST } );

        TEST_DATA = profileData;
    };    
}
