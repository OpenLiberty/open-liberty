/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.feature.tests;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import com.ibm.ws.feature.tests.util.FeatureUtil;
import com.ibm.ws.feature.tests.util.RepositoryUtil;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.internal.FeatureResolverImpl;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;

/**
 * Feature resolution testing.
 */
public class BaselineResolutionErrorCaseTest {

    @BeforeClass
    public static void setupClass() throws Exception {
        // RepositoryUtil.setupFeatures();
        RepositoryUtil.setupProfiles();

        RepositoryUtil.setupRepo("FeatureResolverTest");
    }

    @Before
    public void setupTest() throws Exception {
        doSetupResolver();
    }

    public FeatureResolverImpl resolver;

    public void doSetupResolver() throws Exception {
        resolver = new FeatureResolverImpl();
    }

    public void doClearResolver() throws Exception {
        resolver = null;
    }

    public Result resolveFeatures(Collection<String> features, Collection<String> platforms) throws Exception {
        try{
            return resolver.resolve(RepositoryUtil.getRepository(),
                                    Collections.EMPTY_LIST,
                                    features,
                                    Collections.<String> emptySet(), // pre-resolved feature names
                                    false,
                                    EnumSet.allOf(ProcessType.class),
                                    platforms);
        }
        catch (Exception e) {
            throw e;
        }
    }

    public Result oldResolveFeatures(Collection<String> features) throws Exception {
        try{
            return resolver.resolveFeatures(RepositoryUtil.getRepository(),
                                    Collections.EMPTY_LIST,
                                    features,
                                    Collections.<String> emptySet(), // pre-resolved feature names
                                    false,
                                    EnumSet.allOf(ProcessType.class));
        }
        catch (Exception e) {
            throw e;
        }
    }

    //Set root platforms to null, test to make sure getNoPlatforms gets populated
    @Test
    public void nullPlatformTests() {
        Set<String> features = new HashSet<String>();
        Set<String> platforms = null;
        features.add("servlet");

        Result result;
        try {
            result = resolveFeatures(features, platforms);

            assertEquals(result.getNoPlatformVersionless().get("com.ibm.websphere.appserver.eeCompatible"), features);
        }
        catch(Exception e){
            fail("Unexpected Exception: " + e);
        }
    }

    //test the old feature resolver call that doesn't allow for root platforms to be set, test to make sure getNoPlatforms gets populated
    @Test
    public void oldResolverWithVersionlessTest() {
        Set<String> features = new HashSet<String>();
        features.add("servlet");

        Result result;
        try {
            result = oldResolveFeatures(features);

            assertEquals(result.getNoPlatformVersionless().get("com.ibm.websphere.appserver.eeCompatible"), features);
        }
        catch(Exception e){
            fail("Unexpected Exception: " + e);
        }
    }
}
