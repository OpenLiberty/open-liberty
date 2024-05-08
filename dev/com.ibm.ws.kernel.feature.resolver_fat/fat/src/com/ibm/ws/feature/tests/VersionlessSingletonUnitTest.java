/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import static com.ibm.ws.feature.tests.RepositoryUtil.getRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.util.VerifyData;
import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyCase;
import com.ibm.ws.kernel.feature.internal.util.VerifyDelta;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;

import junit.framework.Assert;
import test.utils.SimpleTestCase;

/**
 * Micro profile cross platform tests.
 */
@RunWith(Parameterized.class)
public class VersionlessSingletonUnitTest extends FeatureResolutionUnitTestBase {
    // Not currently used:
    //
    // BeforeClass is invoked after data() is invoked.
    //
    // But 'data' requires the locations and repository,
    // which were being setup in 'setupClass'.
    //
    // The setup steps have been moved to 'data()', and
    // have been set run at most once.

    @AfterClass
    public static void tearDownClass() throws Exception {
        doTearDownClass();
    }

    // To use change the name of parameterized tests, you say:
    //
    // @Parameters(name="namestring")
    //
    // namestring is a string, which can have the following special placeholders:
    //
    //   {index} - the index of this set of arguments. The default namestring is {index}.
    //   {0} - the first parameter value from this invocation of the test.
    //   {1} - the second parameter value
    //   and so on
    // @Parameterized.Parameters(name = "{0}")

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        doSetupClass(); // 'data()' is invoked before the '@BeforeClass' method.

        //Test each versionless feature by itself.
        //This shouldn't resolve anything so the input and output are both singleton versionless features.
        return asCases(RepositoryUtil.getVersionlessFeatures());
    }

    SimpleTestCase testCase;

    public VersionlessSingletonUnitTest(SimpleTestCase testCase) throws Exception {
        super(testCase.getFeatureInputs().toString(), null);
        this.testCase = testCase;
    }

    @Before
    public void setupTest() throws Exception {
        doSetupResolver();
    }

    @After
    public void tearDownTest() throws Exception {
        doClearResolver();
    }

    @Override
    public List<String> detectFeatureErrors(List<String> rootFeatures) {
        return detectPairErrors(rootFeatures);
    }

    
    public Result resolveFeatures(SimpleTestCase testCase) throws Exception {
        return resolver.resolveFeatures(getRepository(),
                                        Collections.emptySet(),
                                        testCase.getFeatureInputs(),
                                        Collections.<String> emptySet(), // pre-resolved feature names
                                        Collections.emptySet(),
                                        EnumSet.allOf(ProcessType.class));
    }

    @Override
    public void doTestResolve() throws Exception {

        largeDashes(System.out);

        System.out.println("Verifying [ " + testCase.getFeatureInputs().toString() + " ]");


        long startNs = System.nanoTime();
        Result result = resolveFeatures(testCase);
        long endNs = System.nanoTime();
        long durationNs = endNs - startNs;

        Set<String> resultFeatures = result.getResolvedFeatures();
        List<String> resolvedFeatures = new ArrayList<>(resultFeatures.size());
        resolvedFeatures.addAll(resultFeatures);

        List<String> warnings = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> extra = new ArrayList<>();

        if (!warnings.isEmpty()) {
            System.out.println("Verification warnings [ " + warnings.size() + " ]:");
            for (String warning : warnings) {
                System.out.println("  [ " + warning + " ]");
            }
        }
        FeatureResolver.Repository repository = getRepository();
        Set<String> expected = new HashSet<String>(testCase.getFeatureOutputs());
        Set<String> output = new HashSet<String>();

        System.out.println("          " + output + " - Conflicts: " + result.getConflicts() + " - Missing: " + result.getMissing());

        for(String feature : result.getResolvedFeatures()){
            if(repository.getFeature(feature).getVisibility() == Visibility.PUBLIC){
                output.add(feature);
            }
        }

        Assert.assertEquals("Wrong results found.", new TreeSet<String>(expected), new TreeSet<String>(output));
    }

    @Test
    public void testResolve_Versionless_Errors() throws Exception {
        doTestResolve();
    }
}