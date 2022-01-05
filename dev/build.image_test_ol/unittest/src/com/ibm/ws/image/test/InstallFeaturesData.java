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
package com.ibm.ws.image.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstallFeaturesData {

    protected static List<String> doubletonList(String e1, String e2) {
        List<String> result = new ArrayList<>(2);
        result.add(e1);
        result.add(e2);
        return result;
    }
    
    protected static final List<Object[]> TEST_DATA;

    static {
        List<Object[]> testData = new ArrayList<Object[]>(4);

        testData.add( new Object[] { "one",    Collections.singletonList("sipServlet-1.1") } );
        testData.add( new Object[] { "two",    doubletonList("jaxb-2.2", "servlet-3.0") } );
        testData.add( new Object[] { "twoDep", doubletonList("jsp-2.2", "servlet-3.0") } );
        testData.add( new Object[] { "raw",    InstallFeaturesTest.RAW_FEATURES } );

        TEST_DATA = testData;
    }
}
