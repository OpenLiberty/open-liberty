/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.featurestart;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.test.featurestart.FeaturesStartTestBase;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test to verify that Open Liberty can start with every valid
 * single feature.
 *
 * Split into buckets to enable shorter builds. Notably, Windows on FYRE hardware
 * does not run in under two hours, which is the maximum allowed time for a FAT
 * bucket.
 *
 * Currently split into four buckets. The last time too much time was taken the
 * number of buckets was two. The number has been increased to four to give us
 * extra running room before a new split is necessary.
 */
@RunWith(FATRunner.class)
public class FeaturesStartTest4 extends FeaturesStartTestBase {
    public static final String SERVER_NAME_4 = "features.start.4.server";

    @Server(SERVER_NAME_4)
    public static LibertyServer server4;

    public static final int NUM_BUCKETS = 4;
    public static final int BUCKET_NO = 4;
    public static final int SPARSITY = 1;

    @BeforeClass
    public static void setUp() throws Exception {
        FeaturesStartTestBase.setParameters(FeaturesStartTest4.class,
                                            server4, SERVER_NAME_4,
                                            NUM_BUCKETS, BUCKET_NO, SPARSITY);
        FeaturesStartTestBase.setUp();
    }

    @Test
    public void testStartFeaturesPart4() throws Exception {
        FeaturesStartTestBase.testStartFeatures();
    }
}
