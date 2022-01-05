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
import java.util.List;

public class InstallImageData {

    protected static final String PREFIX_BASE_ALL ="wlp-base-all";
    protected static final String NAME_BASE_ALL ="wlpBase";
    protected static final String[] REQUIRED_BASE_ALL =
        { ".*<feature>jsp-2.3</feature>.*" };

    protected static final String PREFIX_CORE_ALL ="wlp-core-all";
    protected static final String NAME_CORE_ALL ="wlpCore";
    protected static final String[] REQUIRED_CORE_ALL =
        { ".*<feature>jsp-2.3</feature>.*" };
    
    protected static final String PREFIX_IPLA_ALL ="wlp-developers-ipla-all";
    protected static final String NAME_IPLA_ALL ="wlpIpla";    
    protected static final String[] REQUIRED_IPLA_ALL =
        { ".*<feature>jsp-2.3</feature>.*" };
    
    protected static final String PREFIX_ND_ALL ="wlp-nd-all";
    protected static final String NAME_ND_ALL ="wlpNd";    
    protected static final String[] REQUIRED_ND_ALL =
        { ".*<feature>jsp-2.3</feature>.*" };

    protected static final List<Object[]> TEST_DATA;

    static {
        List<Object[]> testData = new ArrayList<Object[]>(4);

        testData.add( new Object[] { PREFIX_BASE_ALL, NAME_BASE_ALL, REQUIRED_BASE_ALL } );
        testData.add( new Object[] { PREFIX_CORE_ALL, NAME_CORE_ALL, REQUIRED_CORE_ALL } );
        testData.add( new Object[] { PREFIX_IPLA_ALL, NAME_IPLA_ALL, REQUIRED_IPLA_ALL } );
        testData.add( new Object[] { PREFIX_ND_ALL,   NAME_ND_ALL,   REQUIRED_ND_ALL   } );

        TEST_DATA = testData;
    }
}
