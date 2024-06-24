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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.ibm.ws.feature.tests.util.RepositoryUtil;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.KernelUtils;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.internal.FeatureResolverImpl;
import com.ibm.ws.kernel.feature.internal.util.VerifyData;
import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyCase;
import com.ibm.ws.kernel.feature.internal.util.VerifyDelta;
import com.ibm.ws.kernel.feature.internal.util.VerifyXML;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;

/**
 * Feature resolution testing.
 */
public class BaselineResolutionUnitTest {

    public static void largeDashes(PrintStream stream) {
        stream.println("============================================================");
    }

    public static void smallDashes(PrintStream stream) {
        stream.println("------------------------------------------------------------");
    }

    //

    private static final List<FailureSummary> failures = new ArrayList<>(0);

    public static final class FailureSummary {
        public final String description;
        public final List<String> features;
        public final List<String> messages;

        public FailureSummary(VerifyCase inputCase, List<String> messages) {
            this.description = inputCase.description;
            this.features = new ArrayList<>(inputCase.input.roots);
            this.messages = new ArrayList<>(messages);
        }

        public FailureSummary(VerifyCase inputCase, List<String> extra, List<String> missing) {
            this.description = inputCase.description;
            this.features = new ArrayList<>(inputCase.input.roots);

            int numMessages = ((extra == null) ? 0 : extra.size()) + ((missing == null) ? 0 : missing.size());
            ArrayList<String> useMessages = new ArrayList<>(numMessages);
            if (extra != null) {
                for (String extraFeature : extra) {
                    useMessages.add("Extra [ " + extraFeature + " ]");
                }
            }
            if (missing != null) {
                for (String missingFeature : missing) {
                    useMessages.add("Missing [ " + missingFeature + " ]");
                }
            }
            this.messages = useMessages;
        }

        public void print(PrintStream output) {
            output.println("Feature resolution [ " + description + " ]:");
            output.println("  Features [ " + features + " ]");
            for (String message : messages) {
                output.println("  [ " + message + " ]");
            }
        }

        public String getMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("Feature resolution [ " + description + " ]");
            builder.append(" failed with [ ");
            builder.append(Integer.toString(messages.size()));
            builder.append(" ] errors: ");

            int errorNo = 0;
            for (String error : messages) {
                if (errorNo > 3) {
                    builder.append("...");
                    break;
                } else if (errorNo > 0) {
                    builder.append(", ");
                }

                builder.append(error);
                errorNo++;
            }

            return builder.toString();
        }
    }

    public static FailureSummary addRootFailure(VerifyCase inputCase, List<String> errors) {
        FailureSummary summary = new FailureSummary(inputCase, errors);

        failures.add(summary);
        System.out.println("Failures [ " + failures.size() + " ]");

        return summary;
    }

    public static FailureSummary addFailure(VerifyCase inputCase, List<String> extra, List<String> missing) {
        FailureSummary summary = new FailureSummary(inputCase, extra, missing);

        failures.add(summary);
        System.out.println("Failures [ " + failures.size() + " ]");

        return summary;
    }

    public static void printFailures(PrintStream output) {
        if (!failures.isEmpty()) {
            largeDashes(output);
            output.println("Failures [ " + failures.size() + " ]:");
            smallDashes(output);
            for (FailureSummary summary : failures) {
                summary.print(output);
            }
            largeDashes(output);
        }
    }

    public static void clearFailures() {
        failures.clear();
    }

    //

    public static final String SERVER_NAME = "FeatureResolverTest";

    public static String getServerName() {
        return SERVER_NAME;
    }

    //

    public static boolean didSetup;

    public static void doSetupClass() throws Exception {
        doSetupClass(getServerName());
    }

    public static void doSetupClass(String serverName) throws Exception {
        if (didSetup) {
            return;
        } else {
            didSetup = true;
        }

        RepositoryUtil.setupFeatures();
        RepositoryUtil.setupProfiles();

        RepositoryUtil.setupRepo(serverName);

        setupBeta();
    }

    //

    public static void setupBeta() {
        System.setProperty("com.ibm.ws.beta.edition", "true");
    }

    //

    public static void doTearDownClass() throws Exception {
        printFailures(System.out);
        clearFailures();

        Utils.setInstallDir(null);
        KernelUtils.setBootStrapLibDir(null);
    }

    //

    /**
     * Converts a versioned test case to an equivalent versionless test case
     *
     * @param verifyData the versioned test case
     * @return a versionless test case derived from the input case
     * @throws Exception
     */
    public static VerifyData convertToVersionless(VerifyData verifyData) throws Exception {
        VerifyData newVerifyData = new VerifyData();

        for (VerifyCase inputCase : verifyData.getCases()) {
            String symName = inputCase.input.roots.get(0);
            String platform = RepositoryUtil.getPlatformOf(symName);
            if (platform == null) {
                continue;
            }

            VerifyCase newCase = copyForVersionless(inputCase, symName, platform);
            newVerifyData.addCase(newCase);
        }

        return newVerifyData;
    }

    /**
     * Copies the case and modifies it to be an equivalent versionless case.
     * Instead of having a versioned root feature, the copy has a
     * versionless root feature and a platform. The platform is derived from
     * the versioned feature that versionless feature replaces.
     *
     * @param inputCase test case
     * @param symName symbolic feature name
     * @param platform
     * @return
     */
    public static VerifyCase copyForVersionless(VerifyCase inputCase, String symName, String platform) {
        VerifyCase newCase = new VerifyCase();
        newCase.name = "versionless " + inputCase.name;
        newCase.description = "versionless " + inputCase.description;

        newCase.durationNs = inputCase.durationNs;

        if (inputCase.input.isMultiple) {
            newCase.input.setMultiple();
        }

        if (inputCase.input.isClient) {
            newCase.input.setClient();
        } else if (inputCase.input.isServer) {
            newCase.input.setServer();
        }

        for (String kernelName : inputCase.input.kernel) {
            newCase.input.addKernel(kernelName);
        }

        for (String featureName : inputCase.output.resolved) {
            newCase.output.addResolved(featureName);
        }

        String versionlessFeatureName = RepositoryUtil.asVersionlessFeatureName(symName);
        newCase.name = "versionless - " + versionlessFeatureName + " -  derived from " + inputCase.name;
        newCase.description = "versionless - platform [" + platform + "] -  derived from " + inputCase.description;
        newCase.input.addRoot(versionlessFeatureName);
        newCase.input.addPlatform(platform);
        newCase.output.addResolved(RepositoryUtil.asInternalVersionlessFeatureName(symName));
        newCase.output.addResolved(RepositoryUtil.asShortName(symName));

        return newCase;
    }

    //

    public static Collection<Object[]> nullCases(String description) {
        return Collections.singletonList(new Object[] { description, new VerifyCase() });
    }

    public void nullResult() {
        //
    }

    public static Collection<Object[]> asCases(VerifyData verifyData) {
        List<? extends VerifyCase> cases = verifyData.getCases();

        List<Object[]> params = new ArrayList<>(cases.size());
        for (VerifyCase aCase : cases) {
            params.add(new Object[] { aCase.name, aCase });
        }
        return params;
    }

    public static VerifyData readData(File verifyDataFile) throws Exception {
        VerifyData verifyData = VerifyXML.read(verifyDataFile);
        System.out.println("Read [ " + verifyData.getCases().size() + " ]" +
                           " test cases from [ " + verifyDataFile.getCanonicalPath() + " ]");
        return verifyData;
    }

    //

    public BaselineResolutionUnitTest(String name, VerifyCase testCase) {
        this.name = name;
        this.testCase = testCase;
    }

    private final String name;
    private final VerifyCase testCase;

    public String getName() {
        return name;
    }

    public VerifyCase getTestCase() {
        return testCase;
    }

    //

    public String getPreferredPlatforms() {
        return null;
    }

    public FeatureResolverImpl resolver;

    public void doSetupResolver() throws Exception {
        resolver = new FeatureResolverImpl();

        String preferredPlatforms = getPreferredPlatforms();
        if (preferredPlatforms != null) {
            FeatureResolverImpl.setPreferredPlatforms(preferredPlatforms);
        }
    }

    public void doClearResolver() throws Exception {
        resolver = null;
    }

    public static final Collection<ProvisioningFeatureDefinition> emptyFeatureDefs = Collections.<ProvisioningFeatureDefinition> emptySet();

    public List<String> detectFeatureErrors(List<String> rootFeatures) {
        // Do nothing by default; this should be specialized
        // according to the case pattern used by the test.
        return null;
    }

    public List<String> detectSingletonErrors(List<String> rootFeatures) {
        String rootFeature = rootFeatures.get(0);

        if (RepositoryUtil.getFeatureDef(rootFeature) == null) {
            String message = "Root feature [ " + rootFeature + " ]: Missing from baseline";
            return Collections.singletonList(message);
        } else {
            return null;
        }
    }

    public List<String> detectVersionlessSingletonErrors(List<String> rootFeatures) {
        String rootFeature = rootFeatures.get(0);

        if (RepositoryUtil.getVersionlessFeatureDef(rootFeature) == null) {
            String message = "Versionless root feature [ " + rootFeature + " ]: Missing from baseline";
            RepositoryUtil.displayVersionlessFeatures();
            return Collections.singletonList(message);
        } else {
            return null;
        }
    }

    public List<String> detectPairErrors(List<String> rootFeatures) {
        String rootFeature0 = rootFeatures.get(0);
        String rootFeature1 = rootFeatures.get(1);

        List<String> rootErrors = null;

        if (RepositoryUtil.getFeatureDef(rootFeature0) == null) {
            String message = "Root feature [ " + rootFeature0 + " ]: Missing from baseline";

            rootErrors = new ArrayList<>(2);
            rootErrors.add(message);
        }

        if (RepositoryUtil.getFeatureDef(rootFeature1) == null) {
            String message = "Combination feature [ " + rootFeature1 + " ]: Missing from baseline";

            if (rootErrors == null) {
                rootErrors = Collections.singletonList(message);
            } else {
                rootErrors.add(message);
            }
        }

        return rootErrors;
    }

    public Result resolveFeatures(VerifyCase verifyCase, List<String> rootErrors) throws Exception {
        return resolver.resolve(RepositoryUtil.getRepository(),
                                RepositoryUtil.ignoreFeatures("Kernel", verifyCase.input.kernel),
                                verifyCase.input.roots,
                                Collections.<String> emptySet(), // pre-resolved feature names
                                verifyCase.input.isMultiple,
                                getProcessTypes(verifyCase),
                                verifyCase.input.platforms);
    }

    public static EnumSet<ProcessType> getProcessTypes(VerifyCase verifyCase) {
        if (verifyCase.input.isClient) {
            if (verifyCase.input.isServer) {
                return EnumSet.of(ProcessType.CLIENT, ProcessType.SERVER);
            } else {
                return EnumSet.of(ProcessType.CLIENT);
            }
        } else {
            if (verifyCase.input.isServer) {
                return EnumSet.of(ProcessType.SERVER);
            } else {
                return EnumSet.noneOf(ProcessType.class);
            }
        }
    }

    public void doTestResolve() throws Exception {
        VerifyCase testCase = getTestCase();

        largeDashes(System.out);

        System.out.println("Verifying case name[ " + testCase.name + " ]\ncase description [ " + testCase.description + " ]");

        List<String> rootErrors = detectFeatureErrors(testCase.input.roots);
        if (rootErrors != null) {
            FailureSummary summary = addRootFailure(testCase, rootErrors);
            fail(summary.getMessage());
            return; // 'fail' never returns.
        }

        System.out.println("DEBUG begin ******");
        System.out.println(testCase.input);
        System.out.println(testCase.output);
        System.out.println("DEBUG end ******");

        long startNs = System.nanoTime();
        Result result = resolveFeatures(testCase, rootErrors);
        long endNs = System.nanoTime();
        long durationNs = endNs - startNs;

        Set<String> resultFeatures = result.getResolvedFeatures();
        List<String> resolvedFeatures = new ArrayList<>(resultFeatures.size());
        resolvedFeatures.addAll(resultFeatures);

        VerifyCase outputCase = new VerifyCase(testCase, resolvedFeatures, durationNs);

        List<String> warnings = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> extra = new ArrayList<>();

        List<String> errors = VerifyDelta.compare(new RepoVisibilitySupplier(RepositoryUtil.getRepository()),
                                                  null, warnings,
                                                  testCase, outputCase,
                                                  !VerifyDelta.UPDATED_USED_KERNEL,
                                                  extra, missing);

        if ((errors == null) || errors.isEmpty()) {
            System.out.println("Verified");
        } else {
            System.out.println("Verification errors [ " + errors.size() + " ]:");
            for (String error : errors) {
                System.out.println("  [ " + error + " ]");
            }
            write("Revised Case:", outputCase, System.out);
        }

        if (!warnings.isEmpty()) {
            System.out.println("Verification warnings [ " + warnings.size() + " ]:");
            for (String warning : warnings) {
                System.out.println("  [ " + warning + " ]");
            }
        }

        if ((errors != null) && !errors.isEmpty()) {
            FailureSummary summary = addFailure(testCase, extra, missing);
            fail(summary.getMessage());
            return; // 'fail' never returns.
        }
    }

    private class RepoVisibilitySupplier implements VerifyDelta.VisibilitySupplier {
        public RepoVisibilitySupplier(FeatureResolver.Repository repo) {
            this.repo = repo;
        }

        private final FeatureResolver.Repository repo;

        @Override
        public String getVisibility(String featureName) {
            ProvisioningFeatureDefinition featureDef = repo.getFeature(featureName);
            return ((featureDef == null) ? "MISSING" : featureDef.getVisibility().toString());
        }
    }

    protected void write(String tag, VerifyCase verifyCase, PrintStream output) {
        smallDashes(output);
        System.out.println(tag);
        smallDashes(output);
        PrintWriter writer = new PrintWriter(output);

        // Do not close the writer: 'output' is often System.out, which
        // must not be closed.
        @SuppressWarnings("resource")
        VerifyXML.VerifyXMLWriter xmlWriter = new VerifyXML.VerifyXMLWriter(writer);

        try {
            writer.println(tag);
            xmlWriter.write(verifyCase);
        } finally {
            xmlWriter.flush();
        }

        smallDashes(output);
    }
}
