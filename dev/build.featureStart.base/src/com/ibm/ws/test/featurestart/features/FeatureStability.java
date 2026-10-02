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
package com.ibm.ws.test.featurestart.features;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class FeatureStability {

    /**
     * Read and return a table of stable features. Read from a stable
     * features resource.
     *
     * @return The table of stable features.
     *
     * @throws IOException Thrown if the table could not be read.
     */
    public static FeatureStability readStableFeatures() throws IOException {
        List<List<String>> stableFeatureData = readStableFeatureData();

        return new FeatureStability(stableFeatureData);
    }

    public static final String STABLE_FEATURES_NAME = "com/ibm/ws/test/featurestart/features/feature-stable.txt";

    // Expecting, for example:
    //
    // null acmeCA # 2.0                         #
    // null adminCenter # 1.0                    #
    // null appAuthentication 2.0 # 3.0          #
    // null appAuthorization 2.0 # 2.1           #
    // null appClientSupport 1.0 # 2.0           #
    // null appSecurity 1.0 2.0 3.0 4.0 # 5.0    #
    // null appSecurityClient # 1.0              # isClient    
    
    protected static List<List<String>> readStableFeatureData() throws IOException {
        List<List<String>> featureData = new ArrayList<>();

        Enumeration<URL> urls = FeatureStability.class.getClassLoader().getResources(STABLE_FEATURES_NAME);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            try (InputStream featuresStream = url.openStream();
                 Scanner scanner = new Scanner(featuresStream)) {

                while (scanner.hasNextLine()) {
                    List<String> data = new ArrayList<>();

                    String line = scanner.nextLine().trim();
                    if (line.isEmpty() || line.charAt(0) == '#') {
                        continue;
                    }
                    int commentOffset = line.indexOf('#');
                    if (commentOffset != -1) {
                        line = line.substring(0, commentOffset).trim();
                    }
                    if (line.isEmpty()) {
                        continue;
                    }

                    int nextStart = 0;
                    int nextSpace;
                    while ((nextSpace = line.indexOf(' ', nextStart)) != -1) {
                        String text = line.substring(nextStart, nextSpace).trim();
                        if (!text.isEmpty()) {
                            if (text.equals("null")) {
                                if ( data.size() == 0 ) {
                                    text = null;
                                } else {
                                    throw new IllegalArgumentException("Null non-weight parameter");
                                }
                            }
                            data.add(text);
                        }
                        nextStart = nextSpace + 1;
                    }

                    String text = line.substring(nextStart).trim();
                    if (!text.isEmpty()) {
                        if (text.equals("null")) {
                            throw new IllegalArgumentException("Null non-weight parameter");
                        }
                        data.add(text);
                    }

                    if ( data.size() < 2 ) {
                        throw new IllegalArgumentException("Stable feature data must include a weight and a name");
                    }

                    featureData.add(data);
                }
            }
        }

        return featureData;
    }

    //

    public static class StableFeatureBucket {
        public final String name;

        public String getName() {
            return name;
        }

        public static final int DEFAULT_WEIGHT = 1;

        public final int weight;

        public int getWeight() {
            return weight;
        }

        protected final Set<String> elements;

        public Set<String> getElements() {
            return elements;
        }

        public int getSize() {
            return getElements().size();
        }

        public void addElement(String element) {
            elements.add(name + "-" + element);
        }

        public int getWeighedSize() {
            return getSize() * getWeight();
        }

        //

        public StableFeatureBucket(String name) {
            this(name, DEFAULT_WEIGHT, null, 0);
        }

        public StableFeatureBucket(String name, List<String> elements, int elementStart) {
            this(name, DEFAULT_WEIGHT, elements, elementStart);
        }

        public StableFeatureBucket(String name, int weight, List<String> elements, int elementsStart) {
            if (name == null) {
                throw new IllegalArgumentException("Null name not allowed");
            }
            if (weight <= 0) {
                throw new IllegalArgumentException("Weight [ " + weight + " ] for [ " + name + " ] must be at least [ 1 ].");
            }

            if (elementsStart < 0) {
                throw new IllegalArgumentException("Unusable start of element data [ " + elementsStart + " ]");
            }
            int numElements = ((elements == null) ? 0 : elements.size());
            if (numElements < elementsStart) {
                throw new IllegalArgumentException("Incomplete element data [ " + elements + " ] starting at [ " + elementsStart + " ]");
            } else {
                numElements -= elementsStart;
            }

            this.name = name;
            this.weight = weight;

            if (elements == null) {
                this.elements = Collections.emptySet();
            } else {
                this.elements = new HashSet<String>(numElements);
                for (int elementOffset = elementsStart; elementOffset < elementsStart + numElements; elementOffset++) {
                    addElement(elements.get(elementOffset));
                }
            }
        }

        public StableFeatureBucket(List<String> bucketData) throws NumberFormatException {
            this(bucketData.get(1), parseWeight(bucketData.get(0)), bucketData, 2);
        }

        public static int parseWeight(String weight) throws NumberFormatException {
            if (weight == null) {
                return DEFAULT_WEIGHT;
            } else {
                return Integer.parseInt(weight);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (!(other instanceof StableFeatureBucket)) {
                return false;
            } else {
                return (this.name.equals(((StableFeatureBucket) other).name));
            }
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    public FeatureStability() {
        this(null);
    }

    public FeatureStability(List<List<String>> allBucketData) throws NumberFormatException {
        this.buckets = ((allBucketData == null)
            ? new LinkedHashSet<StableFeatureBucket>(0)
            : new LinkedHashSet<StableFeatureBucket>(allBucketData.size()));

        if (allBucketData != null) {
            int numFeatures = 0;
            for (List<String> data : allBucketData) {
                numFeatures += data.size() - 2; // Skip weight and name.
            }
            this.names = new HashSet<>(numFeatures);
            this.addBuckets(allBucketData);
            
        } else {
            this.names = new HashSet<>(0);
        }


    }

    //

    protected Set<StableFeatureBucket> buckets;

    public Set<StableFeatureBucket> getBuckets() {
        return buckets;
    }

    protected Set<String> names;

    public Set<String> getNames() {
        return names;
    }

    public boolean isStable(String shortName) {
        return getNames().contains(shortName);
    }

    public void addBucket(StableFeatureBucket bucket) {
        getBuckets().add(bucket);
        getNames().addAll(bucket.getElements());
    }

    public void addBucket(List<String> bucketData) {
        addBucket(new StableFeatureBucket(bucketData));
    }

    public void addBuckets(List<List<String>> featureData) {
        for (List<String> data : featureData) {
            addBucket(data);
        }
    }

    /**
     * Partition feature buckets.
     * 
     * Attempt to proportion buckets according to their weight. Because
     * each feature bucket is placed in a single partition element, this
     * may cause one of more of the partition elements being empty.
     * 
     * @param numElements The size of the partition.
     * 
     * @return The partitioned bucket elements.
     */
    public List<List<String>> partitionFeatureNames(int numElements) {
        if (numElements < 1) {
            throw new IllegalArgumentException("Partition size [ " + numElements + " ] must be at least [ 1 ].");
        }

        Set<StableFeatureBucket> featureBuckets = getBuckets();

        if ( numElements == 1 ) {
            // Special case: If the partition is into a single element, the weights don't
            // matter..
            
            int totalNames = 0;
            for (StableFeatureBucket featureBucket : featureBuckets) {
                totalNames += featureBucket.getSize();
            }
            List<String> firstElement = new ArrayList<>(totalNames);
            for (StableFeatureBucket featureBucket : featureBuckets) {
                firstElement.addAll(featureBucket.getElements());
            }
            return Collections.singletonList(firstElement);

        } else {
            // Weighed case. Do a best fit of the weights.
            //
            // Adjust the goal weight upwards if there is a remainder.
            // This attempts to distribute overage across the partition
            // elements, instead of all at the end.

            int totalWeight = 0;
            for (StableFeatureBucket featureBucket : featureBuckets) {
                totalWeight += featureBucket.getWeighedSize();
            }
            int goalElementWeight = totalWeight / numElements;
            if ( totalWeight % numElements != 0 ) {
                goalElementWeight++;
            }

            List<List<String>> elements = new ArrayList<>(numElements);

            int nextWeight = 0;
            List<String> nextElement = null;

            for (StableFeatureBucket featureBucket : featureBuckets) {
                if ( nextElement == null ) {
                    elements.add( nextElement = new ArrayList<>() );
                }

                nextWeight += featureBucket.getWeighedSize();
                nextElement.addAll(featureBucket.getElements());

                if (nextWeight >= goalElementWeight) {
                    nextWeight = 0;
                    nextElement = null;
                }
            }

            for (int elementNo = elements.size(); elementNo < numElements; elementNo++) {
                elements.add(new ArrayList<>(0));
            }

            return elements;
        }
    }
}
