/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ibm.ws.test.featurestart.utils.NamedParameterized;

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
@RunWith(NamedParameterized.class)
public class FeaturesStartTest1 extends FeaturesStartTestBase {
    public static final String SERVER_NAME_1 = "features.start.1.server";

    public static final int NUM_BUCKETS = 4;
    public static final int BUCKET_NO = 1;
    public static final int SPARSITY = 0;

    static {
        try {
            FeaturesStartTestBase.setParameters(FeaturesStartTest1.class,
                                                SERVER_NAME_1,
                                                NUM_BUCKETS, BUCKET_NO, SPARSITY);
            FeaturesStartTestBase.setupFeatures();
        } catch (Exception e) {
            throw new RuntimeException("Feature parameters failure", e);
        }
    }

    /**
     * Answer parameters for running this test. These are set
     * by the calls to {@link #setParameters}, and {@link #setupFeatures()},
     * which load feature information and set the test buckets.
     * The resulting parameters are filtered feature short names.
     *
     * @return The parameters for running this test class.
     */
    @Parameterized.Parameters()
    public static Iterable<Object[]> data() {
        return FeaturesStartTestBase.getParameters();
    }

    public FeaturesStartTest1(String shortName) {
        super(shortName);
    }

    @Test
    public void test() throws Exception {
        super.testStartFeature();
    }
}
