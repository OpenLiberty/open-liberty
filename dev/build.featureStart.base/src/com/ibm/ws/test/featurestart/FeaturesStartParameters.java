/*******************************************************************************
 * Copyright (c) 2023,2026 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;

public class FeaturesStartParameters {
    // Logging ...
    
    public static void logInfo(String m, String msg) {
        FeaturesStartTestBase.logInfo(m, msg);
    }
    
    public static void logBanner(String m) {
        FeaturesStartReporting.logBanner(m);
    }    

    //
    
    /**
     * Global test mode: Tell if tests are running in LITE mode or
     * FULL mode. When running feature start tests in LITE mode, startup
     * tests are not run on stable features. This reduces the number of
     * features tests, which considerably reduces the time taken to run
     * the feature start tests.
     * 
     * @return True or false telling if tests are running in LITE mode.
     */
    public static boolean isModeLite() {
        return ( TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.LITE );
    }
    
    // Test bucket parameters ...
    
    // Test features within a bucket, with an assigned range within
    // the features list.
    //
    // Tests run by this class are conditioned entirely on the number
    // of buckets and the bucket number.
    //
    // The server name must be updated to match the bucket parameters.

    /**
     * Global test bucket count. A single test project must exist per bucket.
     * These are currently {@link com.ibm.ws.test.featurestart.FeaturesStartTest1},
     * {@link com.ibm.ws.test.featurestart.FeaturesStartTest2},
     * {@link com.ibm.ws.test.featurestart.FeaturesStartTest3}, and
     * {@link com.ibm.ws.test.featurestart.FeaturesStartTest4}.
     */
    public static final int NUM_BUCKETS = 4;

    /**
     * Test sparsity control parameter.
     * 
     * Must be 0 or greater.
     *
     * If greater than 0, test a subset of features, 1 of every SPARSITY.
     *
     * For example, setting '10' means run every 10'th test.
     * (At least 1 test is always run.)
     *
     * Use this when testing to limit the number of features which are
     * started.
     */
    public static final int SPARSITY = 0;
    
    /**
     * Initial processing: Display test bucket information, including information
     * in regards to the selected features. Do sparsity related feature selection.
     * Setup for running tests, including the gathering of test result information,
     * including timing information.
     */
    @SuppressWarnings("unused") // The usual SPARSITY value of 0 generates unused code warnings.
    public FeaturesStartParameters(
        FeaturesStartServer server,
        FeaturesStartFeatures features, int bucketNo) {
        
        String m = "<init>";

        this.bucketNo = bucketNo;

        int[] range = getRange(features.runnableFeatures.size(), NUM_BUCKETS, bucketNo);
        this.firstFeatureNo = range[0];
        this.lastFeatureNo = range[1];

        int numCandidateFeatures = this.lastFeatureNo - this.firstFeatureNo;

        // If 'SPARSITY' was specified, adjust the number of features.
        // One of each 'SPARSITY' features is run.
        // If there are remaining features, increment the count, since
        // one of those remaining features will be run.

        int useNumFeatures; 
        if (SPARSITY > 0) {
            useNumFeatures = numCandidateFeatures / SPARSITY;
            int remainder = numCandidateFeatures % SPARSITY;
            if (remainder > 0) {
                useNumFeatures++;
            }
        } else {
            useNumFeatures = numCandidateFeatures;
        }
        this.numFeatures = useNumFeatures;

        this.bucketOutOfLevelFeatureNames = new HashSet<>(features.outOfLevelFeatureNames.size());
        for (int featureNo = firstFeatureNo; featureNo < lastFeatureNo; featureNo++) {
            String shortName = features.runnableFeatureNames.get(featureNo);
            if (features.outOfLevelFeatureNames.contains(shortName)) {
                bucketOutOfLevelFeatureNames.add(shortName);
            }
        }

        List<String> useRunFeatureNames = new ArrayList<>(useNumFeatures);
        List<String> useSkipFeatureNames = new ArrayList<>(numCandidateFeatures - useNumFeatures);

        List<Object[]> useParameters = new ArrayList<>(useNumFeatures);
        
        for (int featureNo = this.firstFeatureNo; featureNo < this.lastFeatureNo; featureNo++) {
            String shortName = features.runnableFeatureNames.get(featureNo);
            if ((SPARSITY > 0) && (((featureNo - this.firstFeatureNo) % SPARSITY) != 0)) {
                logInfo(m, "Skipping [ " + shortName + " ]: Filtered by SPARSITY");
                useSkipFeatureNames.add(shortName);
            } else {
                useRunFeatureNames.add(shortName);
                useParameters.add(new Object[] { shortName });
            }
        }

        this.runFeatureNames = useRunFeatureNames;
        this.skipFeatureNames = useSkipFeatureNames;

        this.parameters = useParameters;
        
        //
        
        logInfo(m, "Test class [ " + FeaturesStartTestBase.testClass + " ]");

        logInfo(m, "Test server [ " + server.serverGetName() + " ]");
        logInfo(m, "  Server java [ " + server.getJavaLevel() + " ]");
        logInfo(m, "  Server isZOS [ " + server.isZOS() + " ]");
        logBanner(m);

        logInfo(m, "Features [ " + features.runnableFeatures.size() + " ]");
        logInfo(m, "  Out-of-level [ " + features.outOfLevelFeatureNames.size() + " ]");

        logInfo(m, "Bucket [ " + bucketNo + " ] of [ " + NUM_BUCKETS + " ]");
        logInfo(m, "  Count [ " + this.numFeatures + " ] Out-of-level [ " + this.bucketOutOfLevelFeatureNames.size() + " ]");
        if ( numCandidateFeatures > 0 ) {
            logInfo(m, "  First [ " + firstFeatureNo + " ]: [ " + features.runnableFeatureNames.get(firstFeatureNo) + " ]");
            logInfo(m, "  Last  [ " + (lastFeatureNo - 1) + " ]: [ " + features.runnableFeatureNames.get(lastFeatureNo - 1) + " ]");
        }
        if (SPARSITY > 0) {
            logInfo(m, "  Sparsity [ " + SPARSITY + " ]");
        }
        logBanner(m);        
    }


    // Set the range for this bucket ...
    // Distribute features as evenly as possible:
    // 12 features with 4 buckets: 0..3, 3..6, 6..9. 9..12: 3, 3, 3, 3: 12
    // 11 features with 4 buckets: 0..3, 3..6, 6..9, 9..11: 3, 3, 3, 2: 11
    //  9 features with 4 buckets: 0..3, 3..5, 5..7, 7..9 : 3, 2, 2, 2: 9
    //
    // static final int[] NUM_FEATURES_RANGE = { 12, 11, 10, 9, 8 };
    // static final int[] NUM_BUCKETS_RANGE = { 2, 3, 4 };
    //
    // public static void main(String[] args) {
    //     for (int numFeatures : NUM_FEATURES_RANGE) {
    //         for (int numBuckets : NUM_BUCKETS_RANGE) {
    //             for (int bucketNo = 1; bucketNo <= numBuckets; bucketNo++) {
    //                 int[] range = getRange(numFeatures, numBuckets, bucketNo);
    //             }
    //         }
    //     }
    // }
    
    /**
     * Compute a range for a bucket within a larger range, dividing the
     * larger range as evenly as possible. Any leftover elements are
     * allocated to the initial buckets.
     *
     * @param numElements The number of elements of the overall range.
     * @param numBuckets  The number of buckets.
     * @param bucketNo The bucket number (one based).
     *
     * @return The range as a half open interval: The first offset of the range,
     *         then the last offset of the range plus one.
     */
    protected static int[] getRange(int numElements, int numBuckets, int bucketNo) {
        // A zero based bucket number is easier to compute with.
        int bucketOffset = bucketNo - 1;

        int bucketSize = numElements / numBuckets;
        int residue = numElements % numBuckets;

        // Assign the range assuming an even split (residue == 0).

        int useFirstFeatureNo = bucketOffset * bucketSize;
        int useLastFeatureNo = useFirstFeatureNo + bucketSize;

        // But there may be leftover features.
        // Allocate these one per bucket, starting with the first bucket.

        // When there is a residue, 'bucketSize' is imprecise:
        //   Bucket numbers [ 0 .. residue - 1 ] have a bucket size one greater.
        //   Bucket numbers [ residue .. bucketNo - 1 ] have the computed bucket size

        if (residue != 0) {
            if (bucketOffset < residue) {
                // In effect, add one to the bucket size.
                useFirstFeatureNo += bucketOffset;
                useLastFeatureNo += bucketOffset + 1;
            } else {
                // In effect, add one to the bucket size **for preceding buckets**
                useFirstFeatureNo += residue;
                useLastFeatureNo += residue;
            }
        }

        return new int[] { useFirstFeatureNo, useLastFeatureNo };
    }            

    /**
     * Test parameters, per the junit parameterized test pattern.
     * 
     * Each feature start test instance has a single parameter which
     * is a short feature name. For example, "mpOpenApi-1.0".
     * 
     * This data structure is provided to {@link FeaturesStartTestBase#getParameters()},
     * which is invoked by the junit test runner to create test instances.
     */
    protected List<Object[]> parameters;

    public List<Object[]> getParameters() {
        return parameters;
    }
    
    // Bucket parameters:

    public final int bucketNo;
    public final int firstFeatureNo;
    public final int lastFeatureNo;
    public final int numFeatures;
    
    // Runnable features ...

    protected final List<String> runFeatureNames;
    protected final List<String> skipFeatureNames;

    protected Set<String> bucketOutOfLevelFeatureNames;
    
    public List<String> getRunFeatureNames() {
        return runFeatureNames;
    }

    /**
     * Tell if a feature is the first runnable feature.
     * 
     * @param featureShortName A feature short name.
     * 
     * @return True or false telling if the feature is the first runnable feature.
     */
    public boolean isFirstFeature(String featureShortName) {
        return ( !runFeatureNames.isEmpty() && runFeatureNames.get(0).equals(featureShortName) );
    }

    /**
     * Tell if a feature is the last runnable feature.
     * 
     * @param featureShortName A feature short name.
     * 
     * @return True or false telling if the feature is the last runnable feature.
     */    
    public boolean isLastFeature(String featureShortName) {
        return ( !runFeatureNames.isEmpty() &&
                 runFeatureNames.get(runFeatureNames.size() - 1).equals(featureShortName) );
    }

    public List<String> getSkipFeatureNames() {
        return skipFeatureNames;
    }
}
