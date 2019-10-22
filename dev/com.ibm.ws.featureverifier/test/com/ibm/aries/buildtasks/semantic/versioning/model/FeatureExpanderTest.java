/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.aries.buildtasks.semantic.versioning.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.ibm.ws.featureverifier.internal.FeatureExpander;
import com.ibm.ws.featureverifier.internal.FeatureInfoUtils;

public class FeatureExpanderTest {

    /**
     * Test util, creates a Feature info given the supplied info.
     * 
     * @param name
     * @param version
     * @param visibility
     * @param isSingleton
     * @param nestedFeatures
     * @return
     */
    private FeatureInfo createDebugFeature(String name, String version, String visibility, boolean isSingleton, String... nestedFeatures) {
        FeatureInfo fi = new FeatureInfo(name, version, visibility, name, new HashMap<String, String>());
        fi.singleton = isSingleton;
        fi.contentFeatures = new HashMap<String, String>();
        if (nestedFeatures != null) {
            for (String nested : nestedFeatures) {
                if (nested.indexOf('#') == -1) {
                    fi.contentFeatures.put(nested, null);
                } else {
                    String parts[] = nested.split("#");
                    fi.contentFeatures.put(parts[0], parts[1]);
                }
            }
        }
        return fi;
    }

    /**
     * Test util, creates a list of feature info's given string descriptions of the features. <p>
     * Format: name%version%visibility%isSingleton(%nestedfeature(#toleratesString))* <p>
     * eg. fish-1.0%1.0.0%public%true%nestedfeature-1.0#2.0%anothernested-1.0%yetanothernested-1.0#1.1
     * 
     * @param featureStrings
     * @return
     */
    private List<FeatureInfo> createTestFeatureInfos(String... featureStrings) {
        List<FeatureInfo> result = new ArrayList<FeatureInfo>();
        for (String f : featureStrings) {
            String parts[] = f.split("\\%");
            List<String> nestedInfo = null;
            if (parts.length > 4) {
                nestedInfo = new ArrayList<String>();
                for (int i = 4; i < parts.length; i++) {
                    nestedInfo.add(parts[i]);
                }
            }
            FeatureInfo fi = createDebugFeature(parts[0], parts[1], parts[2], Boolean.valueOf(parts[3]), nestedInfo == null ? null : nestedInfo.toArray(new String[] {}));
            result.add(fi);
        }
        return result;
    }

    /**
     * Test util, creates a list of constrained features given string descriptions of the features & constraints. <p>
     * Format: name%version%visibility%isSingleton(%singletonID*singletonChoice))* <p>
     * eg. fish-1.0%1.0.0%public%true%nestedfeature*nestedfeature-1.0 <p>
     * prefix singletonID with ## if singleton is private.<br>
     * prefix singletonChoice with ## if choice is preferred.<br>
     * 
     * @param expansionStrings
     * @return
     */
    private List<ResolvedConstrainedFeature> createExpansion(String... expansionStrings) {
        List<ResolvedConstrainedFeature> result = new ArrayList<ResolvedConstrainedFeature>();
        for (String e : expansionStrings) {
            String parts[] = e.split("\\%");
            FeatureInfo base = createDebugFeature(parts[0], parts[1], parts[2], Boolean.valueOf(parts[3]));
            Map<SingletonSetId, SingletonChoice> choices = new HashMap<SingletonSetId, SingletonChoice>();
            if (parts.length > 4) {
                for (int i = 4; i < parts.length; i++) {
                    String selection = parts[i];
                    String choiceAndBoundSingleton[] = selection.split("\\*");

                    boolean setIsPrivate = false;
                    String setIdName = choiceAndBoundSingleton[0];
                    if (setIdName.startsWith("##")) {
                        setIsPrivate = true;
                        setIdName = setIdName.substring(2);
                    }
                    SingletonSetId setId = new SingletonSetId(setIdName + "-0.0", setIsPrivate);

                    boolean choiceIsPreferred = false;
                    String choiceName = choiceAndBoundSingleton[1];
                    if (choiceName.startsWith("##")) {
                        choiceIsPreferred = true;
                        choiceName = choiceName.substring(2);
                    }
                    SingletonChoice choice = new SingletonChoice(choiceName, choiceIsPreferred);
                    choices.put(setId, choice);
                }
            }
            ResolvedConstrainedFeature rcf = new ResolvedConstrainedFeature(base, choices);
            result.add(rcf);
        }
        return result;
    }

    /**
     * Test util, creates constrained feature set given string descriptions of the features & constraints. <p>
     * Format: name%version%visibility%isSingleton(%singletonID*singletonChoice))* <p>
     * eg. fish-1.0%1.0.0%public%true%nestedfeature*nestedfeature-1.0 <p>
     * prefix singletonID with ## if singleton is private.<br>
     * prefix singletonChoice with ## if choice is preferred.<br>
     * 
     * @param expansionStrings
     * @return
     */
    private ConstrainedFeatureSet createConstrainedFeatureSet(String... expansionStrings) {
        List<ResolvedConstrainedFeature> rcfs = createExpansion(expansionStrings);
        ConstrainedFeatureSet cfs = new ConstrainedFeatureSet();
        for (ResolvedConstrainedFeature rcf : rcfs) {
            //we simply blend the maps, we must not attempt to do any processing, else we'll have 2 implementations to test
            //the one in the test, and the one in the code. 
            //instead, we'll just make sure that our rcf choice maps agree re preferred/unpreferred status of choices.
            for (Map.Entry<SingletonSetId, SingletonChoice> e : rcf.choices.entrySet()) {
                if (cfs.chosenSingletons.containsKey(e.getKey()) && !cfs.chosenSingletons.get(e.getKey()).equals(e.getValue())) {
                    assertTrue("Test case error, conflicting values given for singleton choice " + e.getValue() + " and " + cfs.chosenSingletons.get(e.getKey()), false);
                }
            }
            //now we know we're ok, just blend the maps.
            cfs.chosenSingletons.putAll(rcf.choices);
            //and add this rcf to the list of features 
            cfs.features.add(rcf);
        }
        return cfs;
    }

    private List<ConstrainedFeatureSet> createConstrainedFeatureList(ConstrainedFeatureSet... items) {
        List<ConstrainedFeatureSet> results = new ArrayList<ConstrainedFeatureSet>();
        for (ConstrainedFeatureSet item : items) {
            results.add(item);
        }
        return results;
    }

    /**
     * Test util, {@link ResolvedConstrainedFeature} doesn't implement equals/hashcode in a way useful for the tests.
     * (it only compares the wrappered feature, where as the tests want to be able to store rcfs in sets to perform equality tests).
     */
    private static class ResolvedConstrainedFeatureWrapper {
        private final Map<SingletonSetId, SingletonChoice> choices;
        private final String name;
        private final String version;
        private final boolean isSingleton;

        public ResolvedConstrainedFeatureWrapper(ResolvedConstrainedFeature r) {
            this.choices = r.choices;
            this.name = r.feature.getName();
            this.version = r.feature.getVersion();
            this.isSingleton = r.feature.getSingleton();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((choices == null) ? 0 : choices.hashCode());
            result = prime * result + (isSingleton ? 1231 : 1237);
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((version == null) ? 0 : version.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ResolvedConstrainedFeatureWrapper other = (ResolvedConstrainedFeatureWrapper) obj;
            if (choices == null) {
                if (other.choices != null)
                    return false;
            } else if (!choices.equals(other.choices))
                return false;
            if (isSingleton != other.isSingleton)
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (version == null) {
                if (other.version != null)
                    return false;
            } else if (!version.equals(other.version))
                return false;
            return true;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(name);
            sb.append("@");
            sb.append(version);
            sb.append("[singleton?" + isSingleton + "]");
            sb.append("{" + choices + "}");
            return sb.toString();
        }
    }

    private static class ConstrainedFeatureSetWrapper {
        Map<SingletonSetId, SingletonChoice> choices;
        Set<String> features;

        public ConstrainedFeatureSetWrapper(ConstrainedFeatureSet s) {
            this.choices = s.chosenSingletons;
            this.features = new HashSet<String>();
            //don't use a wrappered rcf, else we'll try to evaluate the choices in the rcf when we compare cfsw's 
            for (ResolvedConstrainedFeature rcf : s.features) {
                features.add(rcf.feature.getName() + "@" + rcf.feature.getVersion() + "%" + rcf.feature.getVisibility() + "%" + rcf.feature.getSingleton());
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((choices == null) ? 0 : choices.hashCode());
            result = prime * result + ((features == null) ? 0 : features.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ConstrainedFeatureSetWrapper other = (ConstrainedFeatureSetWrapper) obj;
            if (choices == null) {
                if (other.choices != null)
                    return false;
            } else if (!choices.equals(other.choices))
                return false;
            if (features == null) {
                if (other.features != null)
                    return false;
            } else if (!features.equals(other.features))
                return false;
            return true;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Features:");
            sb.append(features);
            sb.append("BoundChoices:");
            sb.append("{" + choices + "}");
            return sb.toString();

        }

    }

    /**
     * Util for Test Util.. wrappers a list of {@link ResolvedConstrainedFeature} into a set of {@link ResolvedConstrainedFeatureWrapper}
     * 
     * @param set
     * @return
     */
    private static Set<ResolvedConstrainedFeatureWrapper> getWrapperedRCFs(Collection<ResolvedConstrainedFeature> set) {
        Set<ResolvedConstrainedFeatureWrapper> w = new HashSet<ResolvedConstrainedFeatureWrapper>();
        for (ResolvedConstrainedFeature rcf : set) {
            w.add(new ResolvedConstrainedFeatureWrapper(rcf));
        }
        return w;
    }

    /**
     * Util for Test Util.. wrappers a list of {@link ConstrainedFeatureSet} into a set of {@link ConstrainedFeatureSetWrapper}
     * 
     * @param set
     * @return
     */
    private static Set<ConstrainedFeatureSetWrapper> getWrapperedCFSs(Collection<ConstrainedFeatureSet> set) {
        Set<ConstrainedFeatureSetWrapper> w = new HashSet<ConstrainedFeatureSetWrapper>();
        for (ConstrainedFeatureSet cfs : set) {
            w.add(new ConstrainedFeatureSetWrapper(cfs));
        }
        return w;
    }

    /**
     * Test an indvidual CFS.
     * 
     * @param test
     * @param result
     */
    private void testConstrainedFeatureSetVsResults(ConstrainedFeatureSet test, ConstrainedFeatureSet result) {
        Set<ResolvedConstrainedFeatureWrapper> testFeatures = getWrapperedRCFs(test.features);
        Set<ResolvedConstrainedFeatureWrapper> resultFeatures = getWrapperedRCFs(result.features);
        if (!testFeatures.equals(resultFeatures)) {
            Set<ResolvedConstrainedFeatureWrapper> matching = new HashSet<ResolvedConstrainedFeatureWrapper>(resultFeatures);
            matching.retainAll(testFeatures);
            testFeatures.removeAll(resultFeatures);
            resultFeatures.removeAll(matching);
            assertTrue("Features missing from bucket " + testFeatures + " Features not expected in bucket " + resultFeatures, false);
        }
        assertEquals(test.chosenSingletons, result.chosenSingletons);
    }

    /**
     * Compare two lists of cfs's.. by wrappering and converting to sets.
     * 
     * @param testExpansion
     * @param results
     */
    private void testBucketsVsResults(List<ConstrainedFeatureSet> testBuckets, List<ConstrainedFeatureSet> results) {
        Set<ConstrainedFeatureSetWrapper> testSet = getWrapperedCFSs(testBuckets);
        Set<ConstrainedFeatureSetWrapper> resultSet = getWrapperedCFSs(results);
        assertEquals("List returned from test buckets contained duplicates [test error, not code error] ", testBuckets.size(), testSet.size());
        assertEquals("result set contained duplicate buckets, this should not happen (expected count of entries, actual count of unique entries)", results.size(),
                     resultSet.size());
        if (!testSet.equals(resultSet)) {
            Set<ConstrainedFeatureSetWrapper> matching = new HashSet<ConstrainedFeatureSetWrapper>(resultSet);
            matching.retainAll(testSet);
            testSet.removeAll(resultSet);
            resultSet.removeAll(matching);
            assertTrue("Buckets not found " + testSet + "  Buckets unexpected " + resultSet, false);
        }
    }

    private void testExpansionVsResults(List<ResolvedConstrainedFeature> testExpansion, List<ResolvedConstrainedFeature> results) {
        Set<ResolvedConstrainedFeatureWrapper> expansionSet = getWrapperedRCFs(testExpansion);
        Set<ResolvedConstrainedFeatureWrapper> resultSet = getWrapperedRCFs(results);
        assertEquals("List returned from test expansion contained duplicates [test error, not code error] ", testExpansion.size(), expansionSet.size());
        assertEquals("result set contained duplicate expansions, this should not happen (expected count of entries, actual count of unique entries)", results.size(),
                     resultSet.size());
        if (!expansionSet.equals(resultSet)) {
            Set<ResolvedConstrainedFeatureWrapper> matching = new HashSet<ResolvedConstrainedFeatureWrapper>(resultSet);
            matching.retainAll(expansionSet);
            expansionSet.removeAll(resultSet);
            resultSet.removeAll(matching);
            System.out.println("Expansions not found " + expansionSet + "  Expansions unexpected " + resultSet);
            assertTrue("Expansions not found " + expansionSet + "  Expansions unexpected " + resultSet, false);
        }
    }

    List<ResolvedConstrainedFeature> getPermutationsForFeatureSet(List<FeatureInfo> features) {
        Map<String, FeatureInfo> nameToFeatureMap = FeatureInfoUtils.createNameToFeatureMap(features);

        List<ResolvedConstrainedFeature> results = new ArrayList<ResolvedConstrainedFeature>();
        for (FeatureInfo f : features) {
            System.out.println("** Expanding " + f.getName());
            results.addAll(FeatureExpander.expandFeature(f, nameToFeatureMap));
        }
        return results;
    }

    List<ResolvedConstrainedFeature> getPermutationsForFeatureOne(List<FeatureInfo> features) {
        Map<String, FeatureInfo> nameToFeatureMap = new HashMap<String, FeatureInfo>();
        //we have to build our own name to feature map, as the one built by the main path code
        //will only have the features in the current config in it.
        for (FeatureInfo f : features) {
            nameToFeatureMap.put(f.getName(), f);
            if (f.getShortName() != null) {
                nameToFeatureMap.put(f.getShortName(), f);
            }
        }

        List<ResolvedConstrainedFeature> results = new ArrayList<ResolvedConstrainedFeature>();
        for (FeatureInfo f : features) {
            results.addAll(FeatureExpander.expandFeature(f, nameToFeatureMap));
            break;
        }
        return results;
    }

    @Test
    public void testSingletonChoiceExpansion() {
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%singleton-1.0#2.0",
                                                     "singleton-1.0%1.0.0%public%true",
                                                     "singleton-2.0%1.0.0%public%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%singleton*##singleton-1.0",
                                                                "feature-1.0%1.0.0%public%false%singleton*singleton-2.0",
                                                                "singleton-1.0%1.0.0%public%true%singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%public%true%singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testConstrainedByAbsenceMiddleOfChain() {
        //a(s1.0/2.0)->b->c(s1.0/2.0)
        //a(s1.0) is only expected outcome.

        List<FeatureInfo> t = createTestFeatureInfos("a-1.0%1.0.0%public%false%s-1.0#2.0%b-1.0",
                                                     "b-1.0%1.0.0%public%false%c-1.0",
                                                     "c-1.0%1.0.0%public%false%s-1.0#2.0",
                                                     "s-1.0%1.0.0%protected%true",
                                                     "s-2.0%1.0.0%protected%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        List<ResolvedConstrainedFeature> trcf = createExpansion("a-1.0%1.0.0%public%false%s*##s-1.0",
                                                                "b-1.0%1.0.0%public%false%s*##s-1.0",
                                                                "c-1.0%1.0.0%public%false%s*##s-1.0",
                                                                "c-1.0%1.0.0%public%false%s*s-2.0",
                                                                "s-1.0%1.0.0%protected%true%s*##s-1.0",
                                                                "s-2.0%1.0.0%protected%true%s*##s-2.0");

        testExpansionVsResults(trcf, results);
    }

    public void testConstrainedByMiddleOfChain() {
        //a(s1.0/2.0)->b(2.0)->c(s1.0/2.0)
        //a(s2.0) is only expected outcome.

        List<FeatureInfo> t = createTestFeatureInfos("a-1.0%1.0.0%public%false%s-1.0#2.0%b-1.0",
                                                     "b-1.0%1.0.0%public%false%s-2.0%c-1.0",
                                                     "c-1.0%1.0.0%public%false%s-1.0#2.0",
                                                     "s-1.0%1.0.0%protected%true",
                                                     "s-2.0%1.0.0%protected%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        List<ResolvedConstrainedFeature> trcf = createExpansion("a-1.0%1.0.0%public%false%s*s-2.0",
                                                                "b-1.0%1.0.0%public%false%s*##s-2.0",
                                                                "c-1.0%1.0.0%public%false%s*##s-1.0",
                                                                "c-1.0%1.0.0%public%false%s*s-2.0",
                                                                "s-1.0%1.0.0%protected%true%*s##s-1.0",
                                                                "s-2.0%1.0.0%protected%true%s*##s-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testSingletonChoiceExpansionNoExpandOption1() {
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%singleton-1.0",
                                                     "singleton-1.0%1.0.0%public%true",
                                                     "singleton-2.0%1.0.0%public%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%singleton*##singleton-1.0",
                                                                "singleton-1.0%1.0.0%public%true%singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%public%true%singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);

    }

    @Test
    public void testSingletonChoiceExpansionNoExpandOption2() {
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%singleton-2.0",
                                                     "singleton-1.0%1.0.0%public%true",
                                                     "singleton-2.0%1.0.0%public%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%singleton*##singleton-2.0",
                                                                "singleton-1.0%1.0.0%public%true%singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%public%true%singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);

    }

    @Test
    public void testSingletonChoiceExpansionNestedFeature() {
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%nestedfeature-1.0%singleton-1.0#2.0",
                                                     "nestedfeature-1.0%1.0.0%private%false%singleton-1.0#2.0",
                                                     "singleton-1.0%1.0.0%private%true",
                                                     "singleton-2.0%1.0.0%private%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        //nested has singleton child, both values tolerated by nested, and by parent.
        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%##singleton*##singleton-1.0",
                                                                "feature-1.0%1.0.0%public%false%##singleton*singleton-2.0",
                                                                "nestedfeature-1.0%1.0.0%private%false%##singleton*##singleton-1.0",
                                                                "nestedfeature-1.0%1.0.0%private%false%##singleton*singleton-2.0",
                                                                "singleton-1.0%1.0.0%private%true%##singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%##singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);

    }

    @Test
    public void testSingletonChoiceExpansionNestedFeatureGatedByRoot() {
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%nestedfeature-1.0%singleton-1.0",
                                                     "nestedfeature-1.0%1.0.0%private%false%singleton-1.0#2.0",
                                                     "singleton-1.0%1.0.0%private%true",
                                                     "singleton-2.0%1.0.0%private%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        //nested has singleton child, only 1.0 tolerated by root feature.
        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%##singleton*##singleton-1.0",
                                                                "nestedfeature-1.0%1.0.0%private%false%##singleton*##singleton-1.0",
                                                                "nestedfeature-1.0%1.0.0%private%false%##singleton*singleton-2.0",
                                                                "singleton-1.0%1.0.0%private%true%##singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%##singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testSingletonChoiceExpansionNestedFeatureGatedByRootNonDefaultOption() {
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%nestedfeature-1.0%singleton-2.0",
                                                     "nestedfeature-1.0%1.0.0%private%false%singleton-1.0#2.0",
                                                     "singleton-1.0%1.0.0%private%true",
                                                     "singleton-2.0%1.0.0%private%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        //nested has singleton child, only 2.0 tolerated by root feature.
        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%##singleton*##singleton-2.0",
                                                                "nestedfeature-1.0%1.0.0%private%false%##singleton*##singleton-1.0",
                                                                "nestedfeature-1.0%1.0.0%private%false%##singleton*singleton-2.0",
                                                                "singleton-1.0%1.0.0%private%true%##singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%##singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testSingletonChoiceExpansionNestedFeatureGatedByRootNoStatementOnSingleton() {
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%nestedfeature-1.0",
                                                     "nestedfeature-1.0%1.0.0%private%false%singleton-1.0#2.0",
                                                     "singleton-1.0%1.0.0%private%true",
                                                     "singleton-2.0%1.0.0%private%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        //nested has singleton child, no statement made by root feature.
        //variants are allowed, because singleton is private
        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%##singleton*##singleton-1.0",
                                                                "feature-1.0%1.0.0%public%false%##singleton*singleton-2.0",
                                                                "nestedfeature-1.0%1.0.0%private%false%##singleton*##singleton-1.0",
                                                                "nestedfeature-1.0%1.0.0%private%false%##singleton*singleton-2.0",
                                                                "singleton-1.0%1.0.0%private%true%##singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%##singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testSingletonChoiceExpansionNestedFeatureGatedByRootNoStatementOnSingleton2() {
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%nestedfeature-1.0",
                                                     "nestedfeature-1.0%1.0.0%private%false%singleton-1.0#2.0",
                                                     "singleton-1.0%1.0.0%public%true",
                                                     "singleton-2.0%1.0.0%public%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        //nested has singleton child, no statement made by root feature.
        //singleton-2.0 is not possible for feature, because singleton is public.
        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%singleton*##singleton-1.0",
                                                                "nestedfeature-1.0%1.0.0%private%false%singleton*##singleton-1.0",
                                                                "nestedfeature-1.0%1.0.0%private%false%singleton*singleton-2.0",
                                                                "singleton-1.0%1.0.0%private%true%singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testSingletonChoiceExpansionNestedSiblingConstraint() {
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%nestedfeature-1.0#2.0%singleton-1.0#2.0",
                                                     "nestedfeature-1.0%1.0.0%private%true%singleton-1.0",
                                                     "nestedfeature-2.0%1.0.0%private%true%singleton-2.0",
                                                     "singleton-1.0%1.0.0%private%true",
                                                     "singleton-2.0%1.0.0%private%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        System.out.println("*** " + results);

        //nested has singleton child, no statement made by root feature.
        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%##nestedfeature*##nestedfeature-1.0%##singleton*##singleton-1.0",
                                                                "feature-1.0%1.0.0%public%false%##nestedfeature*nestedfeature-2.0%##singleton*##singleton-2.0",
                                                                "nestedfeature-1.0%1.0.0%private%true%##singleton*##singleton-1.0%##nestedfeature*##nestedfeature-1.0",
                                                                "nestedfeature-2.0%1.0.0%private%true%##singleton*##singleton-2.0%##nestedfeature*##nestedfeature-2.0",
                                                                "singleton-1.0%1.0.0%private%true%##singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%##singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testSingletonChoiceExpansionNestedSiblingConstraintUnbound() {
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%nestedfeature-1.0#2.0%singleton-1.0#2.0",
                                                     "nestedfeature-1.0%1.0.0%private%true%singleton-1.0",
                                                     "nestedfeature-2.0%1.0.0%private%true%singleton-1.0#2.0",
                                                     "singleton-1.0%1.0.0%private%true",
                                                     "singleton-2.0%1.0.0%private%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        //nested has singleton child, no statement made by root feature.
        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%##nestedfeature*##nestedfeature-1.0%##singleton*##singleton-1.0",
                                                                "feature-1.0%1.0.0%public%false%##nestedfeature*nestedfeature-2.0%##singleton*##singleton-1.0",
                                                                "feature-1.0%1.0.0%public%false%##nestedfeature*nestedfeature-2.0%##singleton*singleton-2.0",
                                                                "nestedfeature-1.0%1.0.0%private%true%##singleton*##singleton-1.0%##nestedfeature*##nestedfeature-1.0",
                                                                "nestedfeature-2.0%1.0.0%private%true%##singleton*##singleton-1.0%##nestedfeature*##nestedfeature-2.0",
                                                                "nestedfeature-2.0%1.0.0%private%true%##singleton*singleton-2.0%##nestedfeature*##nestedfeature-2.0",
                                                                "singleton-1.0%1.0.0%private%true%##singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%##singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testSingletonChoiceExpansionNestedSiblingConstraintTopConstraintIgnoreSubFeatureByConstraint() {
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%nestedfeature-1.0#2.0%singleton-2.0",
                                                     "nestedfeature-1.0%1.0.0%private%true%singleton-1.0",
                                                     "nestedfeature-2.0%1.0.0%private%true%singleton-1.0#2.0",
                                                     "singleton-1.0%1.0.0%private%true",
                                                     "singleton-2.0%1.0.0%private%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        //nested has singleton child, no statement made by root feature.
        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%##nestedfeature*nestedfeature-2.0%##singleton*##singleton-2.0",
                                                                "nestedfeature-1.0%1.0.0%private%true%##singleton*##singleton-1.0%##nestedfeature*##nestedfeature-1.0",
                                                                "nestedfeature-2.0%1.0.0%private%true%##singleton*##singleton-1.0%##nestedfeature*##nestedfeature-2.0",
                                                                "nestedfeature-2.0%1.0.0%private%true%##singleton*singleton-2.0%##nestedfeature*##nestedfeature-2.0",
                                                                "singleton-1.0%1.0.0%private%true%##singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%##singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testSingletonChoiceExpansionNestedSiblingConstraintTopConstraintSubFeatureByConstraint() {
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%nestedfeature-1.0#2.0%singleton-1.0",
                                                     "nestedfeature-1.0%1.0.0%private%true%singleton-1.0",
                                                     "nestedfeature-2.0%1.0.0%private%true%singleton-1.0#2.0",
                                                     "singleton-1.0%1.0.0%private%true",
                                                     "singleton-2.0%1.0.0%private%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        //nested has singleton child, no statement made by root feature.
        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%##nestedfeature*##nestedfeature-1.0%##singleton*##singleton-1.0",
                                                                "feature-1.0%1.0.0%public%false%##nestedfeature*nestedfeature-2.0%##singleton*##singleton-1.0",
                                                                "nestedfeature-1.0%1.0.0%private%true%##singleton*##singleton-1.0%##nestedfeature*##nestedfeature-1.0",
                                                                "nestedfeature-2.0%1.0.0%private%true%##singleton*##singleton-1.0%##nestedfeature*##nestedfeature-2.0",
                                                                "nestedfeature-2.0%1.0.0%private%true%##singleton*singleton-2.0%##nestedfeature*##nestedfeature-2.0",
                                                                "singleton-1.0%1.0.0%private%true%##singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%##singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testSingletonChoiceExpansionRootFeatureNoChoice() {
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%nestedfeature-1.0%singleton-2.0",
                                                     "nestedfeature-1.0%1.0.0%private%true%singleton-1.0#2.0",
                                                     "singleton-1.0%1.0.0%private%true",
                                                     "singleton-2.0%1.0.0%private%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        System.out.println("*** " + results);

        //nested has singleton child, no statement made by root feature.
        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%##nestedfeature*##nestedfeature-1.0%##singleton*##singleton-2.0",
                                                                "nestedfeature-1.0%1.0.0%private%true%##singleton*##singleton-1.0%##nestedfeature*##nestedfeature-1.0",
                                                                "nestedfeature-1.0%1.0.0%private%true%##singleton*singleton-2.0%##nestedfeature*##nestedfeature-1.0",
                                                                "singleton-1.0%1.0.0%private%true%##singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%##singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testPrivateExpansion() {
        List<FeatureInfo> t = createTestFeatureInfos("above-1.0%1.0.0%public%false%top-1.0#2.0",
                                                     "top-2.0%1.0.0%public%true%a-1.0%b-1.0%c-1.0%d-1.0%e-1.0%f-1.0%g-1.0%h-1.0%i-1.0%j-1.0%k-1.0%l-1.0",
                                                     "top-1.0%1.0.0%public%true",
                                                     "a-1.0%1.0.0%private%false",
                                                     "b-1.0%1.0.0%private%false",
                                                     "c-1.0%1.0.0%private%false",
                                                     "d-1.0%1.0.0%private%false",
                                                     "e-1.0%1.0.0%private%false",
                                                     "f-1.0%1.0.0%private%false",
                                                     "g-1.0%1.0.0%private%false",
                                                     "h-1.0%1.0.0%private%false",
                                                     "i-1.0%1.0.0%private%false",
                                                     "j-1.0%1.0.0%private%false",
                                                     "k-1.0%1.0.0%private%false",
                                                     "l-1.0%1.0.0%private%true%"
                        );

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureOne(t);

        System.out.println("*** " + results);
    }

    @Test
    public void testSingletonChoiceExpansionRootNestedFeatureNoChoice() {
        List<FeatureInfo> t = createTestFeatureInfos("topfeature-1.0%1.0.0%private%false%feature-1.0",
                                                     "feature-1.0%1.0.0%private%true%nestedfeature-1.0%singleton-2.0",
                                                     "nestedfeature-1.0%1.0.0%private%true%singleton-1.0#2.0",
                                                     "singleton-1.0%1.0.0%private%true",
                                                     "singleton-2.0%1.0.0%private%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        System.out.println("*** " + results);

        //nested has singleton child, no statement made by root feature. 
        List<ResolvedConstrainedFeature> trcf = createExpansion("topfeature-1.0%1.0.0%private%false%##feature*##feature-1.0%##singleton*##singleton-2.0%##nestedfeature*##nestedfeature-1.0",
                                                                "feature-1.0%1.0.0%private%true%##feature*##feature-1.0%##nestedfeature*##nestedfeature-1.0%##singleton*##singleton-2.0",
                                                                "nestedfeature-1.0%1.0.0%private%true%##singleton*##singleton-1.0%##nestedfeature*##nestedfeature-1.0",
                                                                "nestedfeature-1.0%1.0.0%private%true%##singleton*singleton-2.0%##nestedfeature*##nestedfeature-1.0",
                                                                "singleton-1.0%1.0.0%private%true%##singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%##singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testSingletonNestedSiblingConstraint() {
        List<FeatureInfo> t = createTestFeatureInfos("topfeature-1.0%1.0.0%public%false%feature-1.0",
                                                     "feature-1.0%1.0.0%public%true%anothernestedfeature-1.0%nestedfeature-1.0",
                                                     "nestedfeature-1.0%1.0.0%public%true%singleton-1.0#2.0",
                                                     "anothernestedfeature-1.0%1.0.0%public%true%singleton-2.0",
                                                     "singleton-1.0%1.0.0%private%true",
                                                     "singleton-2.0%1.0.0%private%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        System.out.println("*** " + results);

        //nested has singleton child, no statement made by root feature.
        List<ResolvedConstrainedFeature> trcf = createExpansion("topfeature-1.0%1.0.0%public%false%feature*##feature-1.0%##singleton*##singleton-2.0%nestedfeature*##nestedfeature-1.0%anothernestedfeature*##anothernestedfeature-1.0",
                                                                "feature-1.0%1.0.0%public%true%feature*##feature-1.0%nestedfeature*##nestedfeature-1.0%anothernestedfeature*##anothernestedfeature-1.0%##singleton*##singleton-2.0",
                                                                "anothernestedfeature-1.0%1.0.0%public%true%##singleton*##singleton-2.0%anothernestedfeature*##anothernestedfeature-1.0",
                                                                "nestedfeature-1.0%1.0.0%public%true%##singleton*##singleton-1.0%nestedfeature*##nestedfeature-1.0",
                                                                "nestedfeature-1.0%1.0.0%public%true%##singleton*singleton-2.0%nestedfeature*##nestedfeature-1.0",
                                                                "singleton-1.0%1.0.0%private%true%##singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%##singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testSingletonNestedSiblingConstraint2() {
        //this time singleton is not private.. should deny the alt choice for top feature
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%true%anothernestedfeature-1.0%nestedfeature-1.0",
                                                     "nestedfeature-1.0%1.0.0%public%true%singleton-1.0#2.0",
                                                     "anothernestedfeature-1.0%1.0.0%public%true%singleton-2.0",
                                                     "singleton-1.0%1.0.0%public%true",
                                                     "singleton-2.0%1.0.0%public%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        System.out.println("*** " + results);

        //nested has singleton child, no statement made by root feature.
        List<ResolvedConstrainedFeature> trcf = createExpansion("anothernestedfeature-1.0%1.0.0%public%true%singleton*##singleton-2.0%anothernestedfeature*##anothernestedfeature-1.0",
                                                                "nestedfeature-1.0%1.0.0%public%true%singleton*##singleton-1.0%nestedfeature*##nestedfeature-1.0",
                                                                "nestedfeature-1.0%1.0.0%public%true%singleton*singleton-2.0%nestedfeature*##nestedfeature-1.0",
                                                                "singleton-1.0%1.0.0%private%true%singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testChildConstrainedByParentAbsence() {
        //public,private,private
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%nestedfeature-1.0",
                                                     "nestedfeature-1.0%1.0.0%private%false%singleton-1.0#2.0",
                                                     "singleton-1.0%1.0.0%private%true",
                                                     "singleton-2.0%1.0.0%private%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        System.out.println("*** " + results);

        //nested has singleton child, no statement made by root feature.
        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%##singleton*##singleton-1.0",
                                                                "feature-1.0%1.0.0%public%false%##singleton*singleton-2.0",
                                                                "nestedfeature-1.0%1.0.0%private%false%##singleton*##singleton-1.0",
                                                                "nestedfeature-1.0%1.0.0%private%false%##singleton*singleton-2.0",
                                                                "singleton-1.0%1.0.0%private%true%##singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%##singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testChildConstrainedByParentAbsence2() {
        //public,public,private
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%nestedfeature-1.0",
                                                     "nestedfeature-1.0%1.0.0%public%false%singleton-1.0#2.0",
                                                     "singleton-1.0%1.0.0%private%true",
                                                     "singleton-2.0%1.0.0%private%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        System.out.println("*** " + results);

        //nested has singleton child, no statement made by root feature.
        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%##singleton*##singleton-1.0",
                                                                "feature-1.0%1.0.0%public%false%##singleton*singleton-2.0",
                                                                "nestedfeature-1.0%1.0.0%public%false%##singleton*##singleton-1.0",
                                                                "nestedfeature-1.0%1.0.0%public%false%##singleton*singleton-2.0",
                                                                "singleton-1.0%1.0.0%private%true%##singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%##singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    @Test
    public void testChildConstrainedByParentAbsence3() {
        //public,public,public
        List<FeatureInfo> t = createTestFeatureInfos("feature-1.0%1.0.0%public%false%nestedfeature-1.0",
                                                     "nestedfeature-1.0%1.0.0%public%false%singleton-1.0#2.0",
                                                     "singleton-1.0%1.0.0%public%true",
                                                     "singleton-2.0%1.0.0%public%true");

        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(t);

        System.out.println("*** " + results);

        //nested has singleton child, no statement made by root feature.
        List<ResolvedConstrainedFeature> trcf = createExpansion("feature-1.0%1.0.0%public%false%singleton*##singleton-1.0",
                                                                "nestedfeature-1.0%1.0.0%private%false%singleton*##singleton-1.0",
                                                                "nestedfeature-1.0%1.0.0%private%false%singleton*singleton-2.0",
                                                                "singleton-1.0%1.0.0%private%true%singleton*##singleton-1.0",
                                                                "singleton-2.0%1.0.0%private%true%singleton*##singleton-2.0");

        testExpansionVsResults(trcf, results);
    }

    /**
     * backup test that doesn't rely on all the test utils above.
     */
    @Test
    public void testSimpleExpansion() {
        FeatureInfo fi = createDebugFeature("feature-1.0", "1.0.0", "public", false);
        List<FeatureInfo> featureList = new ArrayList<FeatureInfo>();
        featureList.add(fi);
        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(featureList);
        assertNotNull("result set should not be null", results);
        assertEquals("result set should have size of 1", 1, results.size());
        ResolvedConstrainedFeature rcf = results.get(0);
        assertNotNull("rcf should not be null", rcf);
        assertEquals("rcf should have same name as initial feature", "feature-1.0", rcf.feature.getName());
        assertTrue("rcf should not have choices", rcf.choices.isEmpty());
    }

    /**
     * backup test that doesn't rely on all the test utils above.
     */
    @Test
    public void testSimpleSingletonExpansion() {
        FeatureInfo fi = createDebugFeature("feature-1.0", "1.0.0", "public", true);
        List<FeatureInfo> featureList = new ArrayList<FeatureInfo>();
        featureList.add(fi);
        List<ResolvedConstrainedFeature> results = getPermutationsForFeatureSet(featureList);
        assertNotNull("result set should not be null", results);
        assertEquals("result set should have size of 1", 1, results.size());
        ResolvedConstrainedFeature rcf = results.get(0);
        assertNotNull("rcf should not be null", rcf);
        assertEquals("rcf should have same name as initial feature", "feature-1.0", rcf.feature.getName());
        assertFalse("rcf should have itself as a choice", rcf.choices.isEmpty());
        assertEquals("rcf should have itself as a choice", 1, rcf.choices.size());
        Map<SingletonSetId, SingletonChoice> keys = new HashMap<SingletonSetId, SingletonChoice>();
        keys.put(new SingletonSetId("feature-1.0", false), new SingletonChoice("feature-1.0", true));
        assertEquals("rcf should have itself as a choice", keys, rcf.choices);
    }
}
