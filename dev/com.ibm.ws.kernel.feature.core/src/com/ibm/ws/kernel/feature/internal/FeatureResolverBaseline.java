/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.internal.util.BiTransformer;
import com.ibm.ws.kernel.feature.internal.util.LazySupplierImpl;
import com.ibm.ws.kernel.feature.internal.util.RepoXML;
import com.ibm.ws.kernel.feature.internal.util.Transformer;
import com.ibm.ws.kernel.feature.internal.util.VerifyData;
import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyCase;
import com.ibm.ws.kernel.feature.internal.util.VerifyDelta;
import com.ibm.ws.kernel.feature.internal.util.VerifyEnv;
import com.ibm.ws.kernel.feature.internal.util.VerifyXML;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Repository;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Selector;

//@formatter:off
public class FeatureResolverBaseline {

//    static {
//        printPath(VerifyEnv.REPO_PROPERTY_NAME, VerifyEnv.REPO_FILE_NAME);
//        printPath(VerifyEnv.RESULTS_SINGLETON_PROPERTY_NAME, VerifyEnv.RESULTS_SINGLETON_FILE_NAME);
//        printPath(VerifyEnv.DURATIONS_SINGLETON_PROPERTY_NAME, VerifyEnv.DURATIONS_SINGLETON_FILE_NAME);
//        printPath(VerifyEnv.RESULTS_SERVLET_PROPERTY_NAME, VerifyEnv.RESULTS_SERVLET_FILE_NAME);
//        printPath(VerifyEnv.DURATIONS_SERVLET_PROPERTY_NAME, VerifyEnv.DURATIONS_SERVLET_FILE_NAME);
//        printPath(VerifyEnv.RESULTS_SERVLET_MP_PROPERTY_NAME, VerifyEnv.RESULTS_SERVLET_MP_FILE_NAME);
//        printPath(VerifyEnv.DURATIONS_SERVLET_MP_PROPERTY_NAME, VerifyEnv.DURATIONS_SERVLET_MP_FILE_NAME);
//    }

    public static void printPath(String tag, String path) {
        String absPath = ((path == null) ? null : (new File(path)).getAbsolutePath());
        System.out.println(tag + ": [ " + absPath + " ]");
    }

    //

    @Trivial
    protected static void trace(String message) {
//        System.out.println("FeatureResolverBaseline: trace: " + message);
        FeatureResolverImpl.trace(message);
    }

    @Trivial
    protected static void error(String message, Object... parms) {
//        System.out.println("FeatureResolverBaseline: error: " + message);
//        for ( Object parm : parms ) {
//            System.out.println("FeatureResolverBaseline: error:   [ " + parm + " ]");
//        }
        FeatureResolverImpl.error(message, parms);
    }

    @Trivial
    protected static void info(String message, Object... parms) {
//        System.out.println("FeatureResolverBaseline: info: " + message);
//        for ( Object parm : parms ) {
//            System.out.println("FeatureResolverBaseline: info:   [ " + parm + " ]");
//        }
        FeatureResolverImpl.info(message, parms);
    }

    //

    public static final String WAS_LIBERTY_TAIL = "_WL";

    private String adjustFileName(String fileName) {
        if ( fileName == null ) {
            return null;

        } else if ( isOpenLiberty() ) {
            return fileName;

        } else {
            int extensionIndex = fileName.lastIndexOf('.');
            if ( extensionIndex == -1 ) {
                return fileName + WAS_LIBERTY_TAIL;
            } else {
                String baseName = fileName.substring(0, extensionIndex);
                String ext = fileName.substring(extensionIndex);
                return baseName + WAS_LIBERTY_TAIL + ext;
            }
        }
    }

    public static void generate(FeatureResolverImpl resolver, Repository repository,
                                Set<String> allowedMultiple,
                                Collection<ProvisioningFeatureDefinition> kernelFeatures) {

        FeatureResolverBaseline baseline =
            new FeatureResolverBaseline(resolver, repository);

        String repoFileName = VerifyEnv.REPO_FILE_NAME;
        if (repoFileName != null) {
            baseline.writeRepository(baseline.adjustFileName(repoFileName));
        }

        String singletonFileName = VerifyEnv.RESULTS_SINGLETON_FILE_NAME;
        if (singletonFileName != null) {
            String durationsFileName = VerifyEnv.DURATIONS_SINGLETON_FILE_NAME;

            baseline.generateSingleton(allowedMultiple,
                                       kernelFeatures,
                                       baseline.adjustFileName(singletonFileName),
                                       baseline.adjustFileName(durationsFileName));
        }

        String servletFileName = VerifyEnv.RESULTS_SERVLET_FILE_NAME;
        if (servletFileName != null) {
            String durationsFileName = VerifyEnv.DURATIONS_SERVLET_FILE_NAME;

            baseline.generatePairs(allowedMultiple,
                                   kernelFeatures,
                                   "Servlet and versionless (no MP)",
                                   "Servlet", "Versionless (no MP)",
                                   getServletFeatures(repository),
                                   getVersionlessFeatures(repository, INCLUDE_EE, !INCLUDE_MP),
                                   baseline.adjustFileName(servletFileName),
                                   baseline.adjustFileName(durationsFileName));
        }
    }

    //

    private FeatureResolverBaseline(FeatureResolverImpl resolver, Repository repository) {
        this.resolver = resolver;
        this.repository = repository;
    }

    //

    private final FeatureResolverImpl resolver;

    public FeatureResolverImpl getResolver() {
        return resolver;
    }

    //

    private final Repository repository;

    public Repository getRepository() {
        return repository;
    }

    private static final String WAS_LIBERTY_FEATURE_NAME = "apiDiscovery-1.0";

    private boolean isOpenLiberty() {
        return (getRepository().getFeature(WAS_LIBERTY_FEATURE_NAME) == null);
    }

    private static final String SERVLET_VERSIONLESS_PREFIX = "servlet-";

    private static List<String> getServletFeatures(Repository repository) {
        List<String> servletFeatures = new ArrayList<>();
        for ( ProvisioningFeatureDefinition featureDef : repository.getFeatures() ) {
            String featureName = featureDef.getIbmShortName();
            if ( (featureName != null) && featureName.startsWith(SERVLET_VERSIONLESS_PREFIX) ) {
                servletFeatures.add(featureName);
            }
        }
        Collections.sort(servletFeatures);
        return servletFeatures;
    }

    /** Common prefix of public versionless features.  This is used by both EE and MP. */
    private static final String VERSIONLESS_PREFIX = "io.openliberty.versionless.";
    /** Prefix of public microprofile versionless features. */
    private static final String VERSIONLESS_MP_PREFIX = "io.openliberty.versionless.mp";
    /** Common prefix of public WebSphere Liberty versionless features. */
    private static final String VERSIONLESS_PREFIX2 = "com.ibm.websphere.appserver.versionless.";

    private static final boolean INCLUDE_EE = true;
    private static final boolean INCLUDE_MP = true;

    private static List<String> getVersionlessFeatures(Repository repository,
                                                       boolean includeEE, boolean includeMP) {

//        System.out.println("Selecting versionless features:");

        List<String> versionlessFeatures = new ArrayList<>();
        for ( ProvisioningFeatureDefinition featureDef : repository.getFeatures() ) {
            String featureName = featureDef.getSymbolicName();
            if ( !featureName.startsWith(VERSIONLESS_PREFIX) && !featureName.startsWith(VERSIONLESS_PREFIX2) ) {
//                System.out.println("Skip: Missing prefix [ " + featureName + " ]");
                continue;
            }

            String platformName = featureDef.getPlatformName();
            if ( (platformName != null) && platformName.equals("jakartaee-11.0")) {
                continue; // Skip EE11 for now.
            }

            String addReason;
            if ( includeEE && includeMP ) {
                addReason = "Include EE and MP";
            } else if (includeEE && !featureName.startsWith(VERSIONLESS_MP_PREFIX)) {
                addReason = "Include EE";
            } else if (includeMP && featureName.startsWith(VERSIONLESS_MP_PREFIX)) {
                addReason = "Include MP";
            } else {
                addReason = null;
            }

            if ( addReason == null ) {
//                System.out.println("Skip: Not selected [ " + featureName + " ]");
            } else {
//                System.out.println("Add: Selected [ " + addReason + " ] [ " + featureName + " ]");
                versionlessFeatures.add(featureName);
            }
        }

        Collections.sort(versionlessFeatures);

//        System.out.println("Selected versionless features: [ " + versionlessFeatures.size() + " ]");
        return versionlessFeatures;
    }

    //

    /**
     * Write the feature repository to the specified file.
     *
     * @param repoFileName The file which is to be written.
     */
    private void writeRepository(String repoFileName) {
        File repoFile = new File(repoFileName);
        String repoFilePath = repoFile.getAbsolutePath();

        info("Writing feature repository to [ " + repoFilePath + " ] ...");

        try {
            RepoXML.write(new File(repoFileName), getRepository());
            info("Writing feature repository to [ " + repoFilePath + " ] ... done");

        } catch (Exception e) {
            // FFDC
            error("Error writing feature repository to [ " + repoFilePath + " ]");
        }
    }

    //

    public static interface CaseGenerator {
        String getDescription();
        List<LazySupplierImpl<VerifyCase>> generate();
    }

    private void writeCases(CaseGenerator generator,
                            String resultsFileName,
                            String durationsFileName) {

        info(generator.getDescription());

        File resultsFile = new File(resultsFileName);
        String resultsFilePath = resultsFile.getAbsolutePath();

        info("Resolving ...");
        List<LazySupplierImpl<VerifyCase>> cases = generator.generate();
        info("Resolving ... done");

        info("Writing to [ " + resultsFilePath + " ] ...");
        try {
            VerifyXML.write(resultsFile, cases);
            info("Writing to [ " + resultsFilePath + " ] ... done");
        } catch ( Exception e ) {
            // FFDC
            error("Failed writing to [ " + resultsFilePath + " ] ...");
        }

        if ( durationsFileName != null ) {
            File durationsFile = new File(durationsFileName);
            String durationsFilePath = durationsFile.getAbsolutePath();

            info("Writing durations to [ " + durationsFilePath + " ] ...");
            try {
                VerifyXML.writeDurations(durationsFile, cases);
                info("Writing durations to [ " + durationsFilePath + " ] ... done");
            } catch ( Exception e ) {
                // FFDC
                error("Failed writing durations to [ " + durationsFilePath + " ] ...");
            }
        }

        info(generator.getDescription() + " ... done");
    }

    private void generateSingleton(final Set<String> allowedMultiple,
                                   final Collection<ProvisioningFeatureDefinition> kernelFeatures,
                                   String resultsFileName,
                                   String durationsFileName) {

        CaseGenerator generator = new CaseGenerator() {
            @Override
            public String getDescription() {
                return "Test suite: All public features as singletons";
            }

            @Override
            public List<LazySupplierImpl<VerifyCase>> generate() {
                return generateSingleton(allowedMultiple, kernelFeatures);
            }
        };

        writeCases(generator, resultsFileName, durationsFileName);
    }

    /**
     * Perform resolutions using the supplied parameters.  Write
     * the results.
     *
     * @param allowedMultiple Control parameters: When non-null, allow
     *     multiple features.
     * @param kernelFeatures Kernel features to be used to perform the
     *     resolution.
     * @param resultsFileName File which is to receive the resolution
     *     results.
     * @param durationsFileName File which is to receive the resolution
     *     times.
     */
    private void generatePairs(final Set<String> allowedMultiple,
                               final Collection<ProvisioningFeatureDefinition> kernelFeatures,
                               final String description,
                               final String element0Description,
                               final String element1Description,
                               final List<String> elements0,
                               final List<String> elements1,
                               String resultsFileName,
                               String durationsFileName) {

        CaseGenerator generator = new CaseGenerator() {
            @Override
            public String getDescription() {
                return "Test suite: " + description;
            }

            @Override
            public List<LazySupplierImpl<VerifyCase>> generate() {
                return generatePairs(allowedMultiple, kernelFeatures,
                                     element0Description, element1Description,
                                     elements0, elements1);
            }
        };

        writeCases(generator, resultsFileName, durationsFileName);
    }

    private static Comparator<ProvisioningFeatureDefinition> COMPARE_SYMBOLIC =
        new Comparator<ProvisioningFeatureDefinition>() {

        /**
         * Compare two features by their symbolic name.
         * (Not all public features have a short name.)
         *
         * Use a case insensitive comparison.
         *
         * @param def1 A feature definition which is to be compared.
         * @param def2 Another feature definition which is to be
         *
         * @return The features compared by their symbolic name.
         */
        @Override
        public int compare(ProvisioningFeatureDefinition def1,
                           ProvisioningFeatureDefinition def2) {
            return def1.getSymbolicName().compareToIgnoreCase(def2.getSymbolicName());
        }
    };

    /**
     * Select feature definitions which match a specified predicate.
     *
     * Select from all of the features of a repository.
     *
     * See {@link Repository#getFeatures()) and {@link #select(Selector, List)}.
     *
     * @param repository A feature repository.
     * @param selector   A feature selector. If null, all features are selected.
     *
     * @return All feature definitions which match the predicate.
     */
    static List<ProvisioningFeatureDefinition> select(Repository repository, Selector<ProvisioningFeatureDefinition> selector) {
        return select(repository.getFeatures(), selector);
    }

    /**
     * Select feature definitions which match a specified predicate.
     *
     * @param defs     Feature definitions from which to select.
     * @param selector A feature selector. If null, all features are selected.
     *
     * @return All feature definitions which match the predicate.
     */
    public static List<ProvisioningFeatureDefinition> select(List<ProvisioningFeatureDefinition> defs, Selector<ProvisioningFeatureDefinition> selector) {
        List<ProvisioningFeatureDefinition> selected = new ArrayList<>(defs.size());
        for (ProvisioningFeatureDefinition def : defs) {
            if ((selector == null) || selector.test(def)) {
                selected.add(def);
            }
        }
        return selected;
    }

    /**
     * Generate resolution results for the specified parameters.  Generate
     * results for each single public feature and for each supported process
     * type of that feature.
     *
     * @param allowedMultiple Control parameters: When non-null, allow
     *     multiple features.
     * @param kernelFeatures Kernel features to be used to perform the
     *     resolution.
     *
     * @return A list of (lazy) case data.
     */
    private List<LazySupplierImpl<VerifyCase>> generateSingleton(final Set<String> allowedMultiple,
                                                                 final Collection<ProvisioningFeatureDefinition> kernelFeatures) {

        Selector<ProvisioningFeatureDefinition> selector =
                        RepoXML.featureSelector(RepoXML.IS_PUBLIC_FEATURE,
                                                !RepoXML.IS_VERSIONLESS_FEATURE,
                                                !RepoXML.IS_TEST_FEATURE);

        List<ProvisioningFeatureDefinition> featureDefs = select(getRepository(), selector);

        int numDefs = featureDefs.size();
        ProvisioningFeatureDefinition[] defsArray = featureDefs.toArray( new ProvisioningFeatureDefinition[numDefs]);
        Arrays.sort(defsArray, COMPARE_SYMBOLIC);

        final Transformer<ProvisioningFeatureDefinition, VerifyCase> createResult =
            new Transformer<ProvisioningFeatureDefinition, VerifyCase>() {
                @Override
                public VerifyCase apply(ProvisioningFeatureDefinition rootDef) {
                    return createSingletonResult(allowedMultiple, kernelFeatures, rootDef);
                }
        };

        List<LazySupplierImpl<VerifyCase>> cases = new ArrayList<>(numDefs);

        for (ProvisioningFeatureDefinition def : featureDefs ) {
            final ProvisioningFeatureDefinition useDef = def;
            cases.add( new LazySupplierImpl<VerifyCase>() {
                @Override
                public VerifyCase produce() {
                    return createResult.apply(useDef);
                }
            });
        }

        return cases;
    }

    /**
     * Generate resolution results for the specified parameters.  Generate
     * results for each single public feature and for each supported process
     * type of that feature.
     *
     * @param allowedMultiple Control parameters: When non-null, allow
     *     multiple features.
     * @param kernelFeatures Kernel features to be used to perform the
     *     resolution.
     *
     * @return A list of (lazy) case data.
     */
    private List<LazySupplierImpl<VerifyCase>> generatePairs(
        final Set<String> allowedMultiple,
        final Collection<ProvisioningFeatureDefinition> kernelFeatures,
        final String element0Desc, final String element1Desc,
        List<String> elements0, List<String> elements1) {

        int num0 = elements0.size();
        int num1 = elements1.size();

        info(element0Desc + " features [ " + num0 + " ]");
        for ( String feature0 : elements0 ) {
            info("  [ " + feature0 + " ]");
        }

        info(element1Desc + " features [ " + num1 + " ]");
        for ( String feature : elements1 ) {
            info("  [ " + feature + " ]");
        }

        final BiTransformer<String, String, VerifyCase> createResult =
            new BiTransformer<String, String, VerifyCase>() {
                @Override
                public VerifyCase apply(String feature0, String feature1) {
                    return createResult(allowedMultiple,
                                               kernelFeatures,
                                               element0Desc, element1Desc,
                                               feature0, feature1);
                }
        };

        info("Total cases [ " + num0 * num1 + " ]");

        List<LazySupplierImpl<VerifyCase>> cases = new ArrayList<>(num0 * num1);

        for ( String feature0 : elements0 ) {
            for ( String feature1 : elements1) {
                final String useFeature0 = feature0;
                final String useFeature1 = feature1;
                cases.add( new LazySupplierImpl<VerifyCase>() {
                    @Override
                    public VerifyCase produce() {
                        return createResult.apply(useFeature0, useFeature1);
                    }
                });
            }
        }

        return cases;
    }

    /**
     * Perform resolution then convert the resolution result to a verification
     * case.
     *
     * @param allowedMultiple Control parameters: When non-null, allow
     *     multiple features.
     * @param kernelFeatures Kernel features to be used to perform the
     *     resolution.
     * @param feature0 A versioned servlet feature name.
     * @param feature1 A versionless feature.
     *
     * @return The resolution result converted into a verification case.
     */
    private VerifyCase createResult(Set<String> allowedMultiple,
                                    Collection<ProvisioningFeatureDefinition> kernelFeatures,
                                    String feature0Desc, String feature1Desc,
                                    String feature0, String feature1) {

        Set<String> preResolved = Collections.emptySet();
        Set<String> profiles = Collections.emptySet();

        List<String> rootFeatures = new ArrayList<>(2);
        rootFeatures.add(feature0);
        rootFeatures.add(feature1);

        info("Creating test result ... ");
        info("  " + feature0Desc + " feature [ " + feature0 + " ]");
        info("  " + feature1Desc + " feature [ " + feature1 + " ]");

        long startTimeNs = VerifyData.getTimeNs();

        Result resultWithKernel = getResolver().doResolve(getRepository(),
                                                          kernelFeatures, rootFeatures, preResolved,
                                                          allowedMultiple, EnumSet.allOf(ProcessType.class),
                                                          profiles);

        Collection<ProvisioningFeatureDefinition> emptyDefs = Collections.emptySet();
        Result resultWithoutKernel = getResolver().doResolve(getRepository(),
                                                             emptyDefs, rootFeatures, preResolved,
                                                             allowedMultiple, EnumSet.allOf(ProcessType.class),
                                                             profiles);

        long endTimeNs = VerifyData.getTimeNs();
        long durationNs = endTimeNs - startTimeNs;

        info("Creating test result ... done [ " + Long.toString(durationNs) + " ns ]");

        boolean allowMultiple = (allowedMultiple != null);

        String name = feature0 + "_" + feature1 + (allowMultiple ? "_n" : "");

        String description = "Feature pair [ " + feature0 + ", " + feature1 + " ]" +
                               (allowMultiple ? " [ Multiple ]" : "");

        return asCase(name, description,
                      allowedMultiple,
                      kernelFeatures, rootFeatures,
                      resultWithKernel, resultWithoutKernel,
                      durationNs);
    }

    /**
     * Perform resolution then convert the resolution result to a verification
     * case.
     *
     * @param allowedMultiple Control parameters: When non-null, allow
     *     multiple features.
     * @param kernelFeatures Kernel features to be used to perform the
     *     resolution.
     * @param featureDef A single public feature used as the root resolution
     *     feature.
     *
     * @return The resolution result converted into a verification case.
     */
    private VerifyCase createSingletonResult(Set<String> allowedMultiple,
                                             Collection<ProvisioningFeatureDefinition> kernelFeatures,
                                             ProvisioningFeatureDefinition featureDef) {

        Collection<String> rootFeatures = Collections.singleton(featureDef.getSymbolicName());
        Set<String> preResolved = Collections.emptySet();
        Set<String> profiles = Collections.emptySet();

        info("Creating singleton test result ... ");

        long startTimeNs = VerifyData.getTimeNs();

        Result resultWithKernel = getResolver().doResolve(getRepository(),
                                                          kernelFeatures, rootFeatures, preResolved,
                                                          allowedMultiple, EnumSet.allOf(ProcessType.class),
                                                          profiles);

        Collection<ProvisioningFeatureDefinition> emptyDefs = Collections.emptySet();
        Result resultWithoutKernel = getResolver().doResolve(getRepository(),
                                                             emptyDefs, rootFeatures, preResolved,
                                                             allowedMultiple, EnumSet.allOf(ProcessType.class),
                                                             profiles);

        long endTimeNs = VerifyData.getTimeNs();
        long durationNs = endTimeNs - startTimeNs;

        info("Creating singleton test result ... done [ " + Long.toString(durationNs) + " ns ]");

        boolean allowMultiple = (allowedMultiple != null);

        String name = featureDef.getSymbolicName() + (allowMultiple ? "_n" : "");
        String description = "Singleton [ " + featureDef.getSymbolicName() + " ]" +
                             (allowMultiple ? " [ Multiple ]" : "");

        List<String> rootFeatureNames = Collections.singletonList( featureDef.getSymbolicName() );

        return asCase(name, description,
                      allowedMultiple,
                      kernelFeatures, rootFeatureNames,
                      resultWithKernel, resultWithoutKernel,
                      durationNs);
    }

    /**
     * Create a verification case from resolution parameters and from
     * the result of resolving those parameters.
     *
     * @param name The name to assign to the case.
     * @param description The description to assign to the case.
     * @param allowedMultiple Control parameters: When non-null, allow
     *     multiple features.
     * @param kernelFeatures Kernel features to be used to perform the
     *     resolution.
     * @param rootFeatures The root feature names which were resolved.
     * @param resultWithKernel The feature resolution result, using kernel features.
     * @param resultWithKernel The feature resolution result, without using kernel features.
     * @param durationNS The resolution time, in nano-seconds.
     *
     * @return A verification case created from the resolution parameters and
     *     the resolution result.
     */
    public VerifyCase asCase(String name, String description,
                             Set<String> allowedMultiple,
                             Collection<ProvisioningFeatureDefinition> kernelFeatures,
                             Collection<String> rootFeatures,
                             Result resultWithKernel, Result resultWithoutKernel,
                             long durationNs) {

        // For now, only handle the distinction between null and an empty set.
        boolean allowMultiple = (allowedMultiple != null);

        VerifyCase verifyCase = new VerifyCase(name, description, allowMultiple);

        verifyCase.durationNs = durationNs;

        for ( ProvisioningFeatureDefinition kernelDef : kernelFeatures ) {
            verifyCase.input.addKernel(kernelDef.getSymbolicName());
        }
        for ( String rootFeature: rootFeatures ) {
            verifyCase.input.addRoot(rootFeature);
        }

        verifyCase.output.copy(resultWithKernel);

        verifyCase.kernelAdjust(VerifyDelta.ORIGINAL_USED_KERNEL,
                                resultWithoutKernel.getResolvedFeatures(),
                                !VerifyDelta.UPDATED_USED_KERNEL);

        return verifyCase;
    }
}
//@formatter:on
