/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.fat;

import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServerFactory;

public class ProductUtilToolTest extends ProductToolTestCommon {

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        setupEnv(LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.fat.util.tool"));
        setupProductExtensions(SETUP_ALL_PROD_EXTS);

    }

    /**
     * Test that ws-productutil.jar version will display information for all installed products.
     * This includes core, product extensions in default usr location and other installed product extensions.
     * 
     * @throws Exception
     */
    @Test
    public void testProdUtilToolPrintProductVersionForAllProductsInstalled() throws Exception {
        testPrintProductVersionForAllProductsInstalled(javaExc,
                                                       new String[] { "-jar", installRoot + "/bin/tools/ws-productutil.jar", "version" },
                                                       installRoot);
    }

    /**
     * Test that ws-productutil.jar featureInfo will display a list of all installed features.
     * This includes core, product extensions in default usr location and other installed product extensions.
     * 
     * @throws Exception
     */
    @Test
    public void testProdUtilToolPrintFeatureInfoForAllProductsInstalled() throws Exception {
        testPrintFeatureInfoForAllProductsInstalled(javaExc,
                                                    new String[] { "-jar", installRoot + "/bin/tools/ws-productutil.jar", "featureInfo" },
                                                    installRoot);
    }
}