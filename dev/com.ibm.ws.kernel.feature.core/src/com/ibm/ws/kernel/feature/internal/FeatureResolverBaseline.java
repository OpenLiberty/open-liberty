/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
import com.ibm.ws.kernel.feature.internal.util.LazySupplier;
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

//@formatter:off
public class FeatureResolverBaseline {

    @Trivial
    protected static void trace(String message) {
        FeatureResolverImpl.trace(message);
    }

    @Trivial
    protected static void error(String message, Object... parms) {
        FeatureResolverImpl.error(message, parms);
    }

    @Trivial
    protected static void info(String message, Object... parms) {
        FeatureResolverImpl.info(message, parms);
    }

    //

    public static void generate(FeatureResolverImpl resolver, Repository repository,
                                Set<String> allowedMultiple,
                                Collection<ProvisioningFeatureDefinition> kernelFeatures) {

        FeatureResolverBaseline baseline =
            new FeatureResolverBaseline(resolver, repository);

        if (VerifyEnv.REPO_FILE_NAME != null) {
            baseline.writeRepository(VerifyEnv.REPO_FILE_NAME);
        }

        if (VerifyEnv.RESULTS_SINGLETON_FILE_NAME != null) {
            baseline.generateSingleton(allowedMultiple,
                                       kernelFeatures,
                                       VerifyEnv.RESULTS_SINGLETON_FILE_NAME,
                                       VerifyEnv.DURATIONS_SINGLETON_FILE_NAME);
        }

        if (VerifyEnv.RESULTS_SERVLET_FILE_NAME != null) {
            baseline.generateServlet(allowedMultiple,
                                     kernelFeatures,
                                     VerifyEnv.RESULTS_SERVLET_FILE_NAME,
                                     VerifyEnv.DURATIONS_SERVLET_FILE_NAME);
        }
    }

    //

    private FeatureResolverBaseline(FeatureResolverImpl resolver, Repository repository) {
        this.resolver = resolver;
        this.repository = repository;
    }

    //

    private final FeatureResolverImpl resolver;

    //

    private final Repository repository;

    private static final String SERVLET_VERSIONLESS_PREFIX = "servlet-";
    private static final String VERSIONLESS_PREFIX = "io.openliberty.versionless.";

    private List<String> getServletFeatures() {
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

    private List<String> getVersionlessFeatures() {
        List<String> versionlessFeatures = new ArrayList<>();
        for ( ProvisioningFeatureDefinition featureDef : repository.getFeatures() ) {
            String featureName = featureDef.getSymbolicName();
            if ( featureName.startsWith(VERSIONLESS_PREFIX) ) {
                versionlessFeatures.add(featureName);
            }
        }
        Collections.sort(versionlessFeatures);
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
            RepoXML.write(new File(repoFileName), repository);
            info("Writing feature repository to [ " + repoFilePath + " ] ... done");

        } catch (Exception e) {
            // FFDC
            error("Error writing feature repository to [ " + repoFilePath + " ]");
        }
    }

    //

    public static interface CaseGenerator {
        String getDescription();
        List<LazySupplier<VerifyCase>> generate();
    }

    private void writeCases(CaseGenerator generator,
                            String resultsFileName,
                            String durationsFileName) {

        info(generator.getDescription());

        File resultsFile = new File(resultsFileName);
        String resultsFilePath = resultsFile.getAbsolutePath();

        info("Resolving ...");
        List<LazySupplier<VerifyCase>> cases = generator.generate();
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
            public List<LazySupplier<VerifyCase>> generate() {
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
    private void generateServlet(final Set<String> allowedMultiple,
                                 final Collection<ProvisioningFeatureDefinition> kernelFeatures,
                                 String resultsFileName,
                                 String durationsFileName) {

        CaseGenerator generator = new CaseGenerator() {
            @Override
            public String getDescription() {
                return "Test suite: All public features as singletons";
            }

            @Override
            public List<LazySupplier<VerifyCase>> generate() {
                return generateServlet(allowedMultiple, kernelFeatures);
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
         * @param def2 Another feature definition which is to be compared.
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
     * Generate resolution results for the specified parameters.  Generate
     * results for each single public feature and for each supported process
     * type of that feature.
     *
     * The results placed with server results then with client results.
     * Both results collections are sorted by feature short name.
     *
     * @param allowedMultiple Control parameters: When non-null, allow
     *     multiple features.
     * @param kernelFeatures Kernel features to be used to perform the
     *     resolution.
     *
     * @return A list of (lazy) case data.
     */
    private List<LazySupplier<VerifyCase>> generateSingleton(final Set<String> allowedMultiple,
                                                             final Collection<ProvisioningFeatureDefinition> kernelFeatures) {

        List<ProvisioningFeatureDefinition> publicDefs = repository.select(RepoXML.IS_PUBLIC);
        int numDefs = publicDefs.size();
        ProvisioningFeatureDefinition[] publicDefsArray = publicDefs.toArray( new ProvisioningFeatureDefinition[numDefs]);
        Arrays.sort(publicDefsArray, COMPARE_SYMBOLIC);

        List<ProvisioningFeatureDefinition> publicServerDefs = new ArrayList<>(numDefs);
        List<ProvisioningFeatureDefinition> publicClientDefs = new ArrayList<>(numDefs);

        for ( ProvisioningFeatureDefinition def : publicDefsArray ) {
            if ( RepoXML.isServer(def) ) {
                publicServerDefs.add(def);
            }
            if ( RepoXML.isClient(def) ) {
                publicClientDefs.add(def);
            }
        }

        final Transformer<ProvisioningFeatureDefinition, VerifyCase> createServerResult =
            new Transformer<ProvisioningFeatureDefinition, VerifyCase>() {
                @Override
                public VerifyCase apply(ProvisioningFeatureDefinition rootDef) {
                    return createSingletonResult(allowedMultiple,
                                                 kernelFeatures, rootDef,
                                                 ProcessType.SERVER);
                }
        };

        final Transformer<ProvisioningFeatureDefinition, VerifyCase> createClientResult =
            new Transformer<ProvisioningFeatureDefinition, VerifyCase>() {
                @Override
                public VerifyCase apply(ProvisioningFeatureDefinition rootDef) {
                    return createSingletonResult(allowedMultiple,
                                                 kernelFeatures, rootDef,
                                                 ProcessType.CLIENT);
                }
        };

        List<LazySupplier<VerifyCase>> cases = new ArrayList<>( publicServerDefs.size() + publicClientDefs.size() );

        for (ProvisioningFeatureDefinition def : publicServerDefs ) {
            final ProvisioningFeatureDefinition useDef = def;
            cases.add( new LazySupplier<VerifyCase>() {
                @Override
                public VerifyCase supply() {
                    return createServerResult.apply(useDef);
                }
            });
        }

        for (ProvisioningFeatureDefinition def : publicClientDefs ) {
            final ProvisioningFeatureDefinition useDef = def;
            cases.add( new LazySupplier<VerifyCase>() {
                @Override
                public VerifyCase supply() {
                    return createClientResult.apply(useDef);
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
     * The results placed with server results then with client results.
     * Both results collections are sorted by feature short name.
     *
     * @param allowedMultiple Control parameters: When non-null, allow
     *     multiple features.
     * @param kernelFeatures Kernel features to be used to perform the
     *     resolution.
     *
     * @return A list of (lazy) case data.
     */
    private List<LazySupplier<VerifyCase>> generateServlet(final Set<String> allowedMultiple,
                                                           final Collection<ProvisioningFeatureDefinition> kernelFeatures) {

        List<String> servletFeatures = getServletFeatures();
        List<String> versionlessFeatures = getVersionlessFeatures();

        int numServlet = servletFeatures.size();
        int numVersionless = versionlessFeatures.size();

        info("Servlet features [ " + numServlet+ " ]");
        info("Versionless features [ " + numVersionless + " ]");

        final Transformer<List<String>, VerifyCase> createResult =
            new Transformer<List<String>, VerifyCase>() {
                @Override
                public VerifyCase apply(List<String> rootFeatures) {
                    return createServletResult(allowedMultiple,
                                               kernelFeatures, rootFeatures,
                                               ProcessType.SERVER);
                }
        };

        info("Servlet cases [ " + numServlet * numVersionless + " ]");

        List<LazySupplier<VerifyCase>> servletCases = new ArrayList<>(numServlet * numVersionless);

        final List<String> rootFeatures = new ArrayList<String>(2);
        rootFeatures.add("dummy"); // Need to pre-populate the list to enable
        rootFeatures.add("dummy"); // setting specific elements.

        for ( String servletFeature : servletFeatures ) {
            rootFeatures.set(0, servletFeature);

            for ( String versionlessFeature : versionlessFeatures ) {
                rootFeatures.set(1, versionlessFeature);

                servletCases.add( new LazySupplier<VerifyCase>() {
                    @Override
                    public VerifyCase supply() {
                        return createResult.apply(rootFeatures);
                    }
                });
            }
        }

        return servletCases;
    }

    /**
     * Perform resolution then convert the resolution result to a verification
     * case.
     *
     * @param allowedMultiple Control parameters: When non-null, allow
     *     multiple features.
     * @param kernelFeatures Kernel features to be used to perform the
     *     resolution.
     * @param servletFeature A versioned servlet feature name.
     * @param versionlessFeature A versionless feature.
     * @param processType Control parameter: Sets the process type active
     *     during resolution.
     *
     * @return The resolution result converted into a verification case.
     */
    private VerifyCase createServletResult(Set<String> allowedMultiple,
                                           Collection<ProvisioningFeatureDefinition> kernelFeatures,
                                           List<String> rootFeatures,
                                           ProcessType processType) {

        Set<String> preResolved = Collections.emptySet();
        EnumSet<ProcessType> processTypes = EnumSet.of(processType);

        info("Creating servlet test result ... ");

        long startTimeNs = VerifyData.getTimeNs();

        Result resultWithKernel = resolver.doResolve(repository,
                                                     kernelFeatures, rootFeatures, preResolved,
                                                     allowedMultiple, processTypes);

        Collection<ProvisioningFeatureDefinition> emptyDefs = Collections.emptySet();
        Result resultWithoutKernel = resolver.doResolve(repository,
                                                        emptyDefs, rootFeatures, preResolved,
                                                        allowedMultiple, processTypes);

        long endTimeNs = VerifyData.getTimeNs();
        long durationNs = endTimeNs - startTimeNs;

        info("Creating servlet test result ... done [ " + Long.toString(durationNs) + " ns ]");

        boolean allowMultiple = (allowedMultiple != null);

        String servletFeature = rootFeatures.get(0);
        String versionlessFeature = rootFeatures.get(1);

        String name = servletFeature + "_" + versionlessFeature + (allowMultiple ? "_n" : "");

        String description = "Simple versionless [ " + servletFeature + ", " + versionlessFeature + " ]" +
                               (allowMultiple ? " [ Multiple ]" : "");

        return asCase(name, description,
                      allowedMultiple, processType,
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
     * @param processType Control parameter: Sets the process type active
     *     during resolution.
     *
     * @return The resolution result converted into a verification case.
     */
    private VerifyCase createSingletonResult(Set<String> allowedMultiple,
                                             Collection<ProvisioningFeatureDefinition> kernelFeatures,
                                             ProvisioningFeatureDefinition featureDef,
                                             ProcessType processType) {

        Collection<String> rootFeatures = Collections.singleton(featureDef.getSymbolicName());
        Set<String> preResolved = Collections.emptySet();
        EnumSet<ProcessType> processTypes = EnumSet.of(processType);

        info("Creating singleton test result ... ");

        long startTimeNs = VerifyData.getTimeNs();

        Result resultWithKernel = resolver.doResolve(repository,
                                                     kernelFeatures, rootFeatures, preResolved,
                                                     allowedMultiple, processTypes);

        Collection<ProvisioningFeatureDefinition> emptyDefs = Collections.emptySet();
        Result resultWithoutKernel = resolver.doResolve(repository,
                                                        emptyDefs, rootFeatures, preResolved,
                                                        allowedMultiple, processTypes);

        long endTimeNs = VerifyData.getTimeNs();
        long durationNs = endTimeNs - startTimeNs;

        info("Creating singleton test result ... done [ " + Long.toString(durationNs) + " ns ]");

        boolean allowMultiple = (allowedMultiple != null);

        String name = featureDef.getSymbolicName() +
                      "_" + processType +
                      (allowMultiple ? "_n" : "");

        String description = "Singleton [ " + featureDef.getSymbolicName() + " ]" +
                             " [ " + processType + " ]" +
                             (allowMultiple ? " [ Multiple ]" : "");

        List<String> rootFeatureNames = Collections.singletonList( featureDef.getSymbolicName() );

        return asCase(name, description,
                      allowedMultiple, processType,
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
     * @param processType Control parameter: Sets the process type active
     *     during resolution.
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
                             Set<String> allowedMultiple, ProcessType processType,
                             Collection<ProvisioningFeatureDefinition> kernelFeatures,
                             Collection<String> rootFeatures,
                             Result resultWithKernel, Result resultWithoutKernel,
                             long durationNs) {

        // For now, only handle the distinction between null and an empty set.
        boolean allowMultiple = (allowedMultiple != null);

        VerifyCase verifyCase = new VerifyCase();

        verifyCase.name = name;
        verifyCase.description = description;

        verifyCase.durationNs = durationNs;

        if ( allowMultiple ) {
            verifyCase.input.setMultiple();
        }

        if (processType == ProcessType.CLIENT) {
            verifyCase.input.setClient();
        } else if (processType == ProcessType.SERVER) {
            verifyCase.input.setServer();
        }

        for ( ProvisioningFeatureDefinition kernelDef : kernelFeatures ) {
            verifyCase.input.addKernel(kernelDef.getSymbolicName());
        }
        for ( String rootFeature: rootFeatures ) {
            verifyCase.input.addRoot(rootFeature);
        }
        for ( String resolvedFeature : resultWithKernel.getResolvedFeatures() ) {
            verifyCase.output.addResolved(resolvedFeature);
        }

        verifyCase.kernelAdjust(VerifyDelta.ORIGINAL_USED_KERNEL,
                                resultWithoutKernel.getResolvedFeatures(),
                                !VerifyDelta.UPDATED_USED_KERNEL);

        return verifyCase;
    }
}
//@formatter:on
