/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import java.util.HashSet;
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

    public static final String STABLE_FEATURES_NAME = "/com/ibm/ws/test/featurestart/features/feature-stable.txt";

    protected static List<List<String>> readStableFeatureData() throws IOException {
        List<List<String>> featureData = new ArrayList<>();

        URL url = FeatureStability.class.getResource(STABLE_FEATURES_NAME);
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
                            text = null;
                        }
                        data.add(text);
                    }
                    nextStart = nextSpace + 1;
                }

                String text = line.substring(nextStart).trim();
                if (!text.isEmpty()) {
                    if (text.equals("null")) {
                        text = null;
                    }
                    data.add(text);
                }

                featureData.add(data);
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

    public FeatureStability(List<List<String>> bucketData) throws NumberFormatException {
        this.buckets = ((bucketData == null) ? new HashSet<StableFeatureBucket>() : new HashSet<StableFeatureBucket>(bucketData.size()));

        int numFeatures = 0;
        for (List<String> data : bucketData) {
            numFeatures += data.size() - 2; // Skip weight and name.
        }

        this.names = new HashSet<>(numFeatures);

        this.addBuckets(bucketData);
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

    public List<List<String>> partitionFeatureNames(int elements) {
        if (elements < 1) {
            throw new IllegalArgumentException("Count of partition elements [ " + elements + " ] must be at least [ 1 ].");
        }

        Set<StableFeatureBucket> featureBuckets = getBuckets();

        int nameCount = 0;
        int weighedNameCount = 0;
        for (StableFeatureBucket featureBucket : featureBuckets) {
            nameCount += featureBucket.getSize();
            weighedNameCount += featureBucket.getWeighedSize();
        }

        if (elements == 1) {
            List<String> names = new ArrayList<>(nameCount);
            for (StableFeatureBucket featureBucket : featureBuckets) {
                names.addAll(featureBucket.getElements());
            }
            return Collections.singletonList(names);
        }

        List<List<String>> nameLists = new ArrayList<>(elements);

        // Simple approximate way to
        int bucketWeight = weighedNameCount / elements;
        if (weighedNameCount % elements != 0) {
            bucketWeight++;
        }
        for (int elementNo = 0; elementNo < elements; elementNo++) {
            List<String> names;

            nameLists.add(names = new ArrayList<>());

            int nextTotalWeight = 0;

            for (StableFeatureBucket featureBucket : featureBuckets) {
                int weight = featureBucket.getWeighedSize();
                nextTotalWeight += weight;

                names.addAll(featureBucket.getElements());

                if (nextTotalWeight >= bucketWeight) {
                    nameLists.add(names = new ArrayList<>());
                }
            }
        }

        int numElements = nameLists.size();
        while (numElements < elements) {
            nameLists.add(new ArrayList<>(0));
            numElements++;
        }

        return nameLists;
    }
}
