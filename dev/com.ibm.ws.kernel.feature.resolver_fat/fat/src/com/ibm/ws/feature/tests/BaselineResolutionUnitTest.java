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

import static com.ibm.ws.feature.tests.util.RepositoryUtil.getFeatureDef;
import static com.ibm.ws.feature.tests.util.RepositoryUtil.getVersionlessFeatureDef;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.ibm.ws.feature.tests.util.FeatureUtil;
import com.ibm.ws.feature.tests.util.RepositoryUtil;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.KernelUtils;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.internal.FeatureResolverImpl;
import com.ibm.ws.kernel.feature.internal.util.VerifyData;
import com.ibm.ws.kernel.feature.internal.util.VerifyData.ResultData;
import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyCase;
import com.ibm.ws.kernel.feature.internal.util.VerifyDelta;
import com.ibm.ws.kernel.feature.internal.util.VerifyXML;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
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
        public final VerifyCase inputCase;
        public final List<String> messages;

        public FailureSummary(VerifyCase inputCase, List<String> messages) {
            this.inputCase = inputCase;
            this.messages = new ArrayList<>(messages);
        }

        public void print(PrintStream output) {
            output.println("Feature resolution [ " + inputCase.description + " ]:");
            if (!inputCase.input.platforms.isEmpty()) {
                output.println("  Platforms [ " + inputCase.input.platforms + " ]");
            }
            if (!inputCase.input.roots.isEmpty()) {
                output.println("  Features [ " + inputCase.input.roots + " ]");
            }
            if (!inputCase.input.envMap.isEmpty()) {
                for (Map.Entry<String, String> envEntry : inputCase.input.envMap.entrySet()) {
                    output.println("  Environment [ " + envEntry.getKey() + "=" + envEntry.getValue() + " ]");
                }
            }
            for (String message : messages) {
                output.println("  [ " + message + " ]");
            }
        }

        public String getMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("Feature resolution [ " + inputCase.description + " ]");
            builder.append(" failed with [ ");
            builder.append(Integer.toString(messages.size()));
            builder.append(" ] errors: ");

            int errorNo = 0;
            for (String error : messages) {
                if (errorNo == 10) {
                    builder.append("\n    ...");
                    break;
                }

                builder.append("\n   ");
                builder.append(error);
                errorNo++;
            }

            return builder.toString();
        }
    }

    public static FailureSummary addFailure(VerifyCase inputCase, List<String> errors) {
        FailureSummary summary = new FailureSummary(inputCase, errors);
        failures.add(summary);
        System.out.println("Added failures [ " + failures.size() + " ]");
        return summary;
    }

    public static void printFailures(PrintStream output, Class<?> testClass) {
        if (!failures.isEmpty()) {
            largeDashes(output);
            output.println("Test class [ " + testClass.getSimpleName() + " ] failures [ " + failures.size() + " ]:");
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

        // RepositoryUtil.setupFeatures();
        RepositoryUtil.setupProfiles();

        RepositoryUtil.setupRepo(serverName);

        setupBeta();
    }

    //

    public static void setupBeta() {
        System.setProperty("com.ibm.ws.beta.edition", "true");
    }

    //

    public static void doTearDownClass(Class<?> testClass) throws Exception {
        printFailures(System.out, testClass);
        clearFailures();

        Utils.setInstallDir(null);
        KernelUtils.setBootStrapLibDir(null);
    }

    //

    /**
     * Convert versioned test data to the versionless equivalent.
     */
    public static VerifyData convertToVersionless(VerifyData verifyData) throws Exception {
        VerifyData newVerifyData = new VerifyData();

        for (VerifyCase inputCase : verifyData.getCases()) {
            VerifyCase newCase = copyForVersionless(inputCase);
            if (newCase != null) {
                newVerifyData.addCase(newCase);
            }
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
     *
     * @return The copied case. Null if the case does not support versionless.
     */
    public static VerifyCase copyForVersionless(VerifyCase inputCase) {
        // sym: prefix.short-V1.V2
        // base: prefix.short
        // shortBase: short
        // short: short-V1.V2

        String symName = inputCase.input.roots.get(0);
        String[] parts = FeatureResolverImpl.parseNameAndVersion(symName);
        String baseName = parts[0];
        String version = parts[1];

        if (version == null) {
            System.out.println("Skipping singleton [ " + symName + " ]: " + "Feature is not versioned");
            return null;
        }

        if (symName.equals("com.ibm.websphere.appserver.jcaInboundSecurity-1.0")) {
            System.out.println("Skipping [ " + symName + " ]: Conversion to versionless causes a conflict.");
            return null;
        }

        // Feature resolution [ versionless - platform javaee-6.0 - from Singleton [ com.ibm.websphere.appserver.jcaInboundSecurity-1.0 ] ] failed with [ 2 ] errors: Missing [ Resolved platforms ]: [ javaee-6.0 ] Extra [ Conflicted features ]: [ com.ibm.websphere.appserver.eeCompatible ]

        String shortBaseName = FeatureUtil.getShortName(baseName);
        String shortName = shortBaseName + "-" + version;

        String platform = null;
        String versionlessInternalSymName = null;

        String skipReason;

        ProvisioningFeatureDefinition featureDef = getFeatureDef(symName);
        if (featureDef == null) {
            skipReason = "Feature not found [ " + symName + " ]";
        } else if (RepositoryUtil.isNoShip(featureDef)) {
            skipReason = "Feature is no-ship [ " + symName + " ]";

        } else {
            platform = RepositoryUtil.getPlatformOf(featureDef);
            if (platform == null) {
                skipReason = "No platform";
            } else if (platform.startsWith("microProfile-")) {
                skipReason = "Platform [ " + platform + " ]";

            } else {
                ProvisioningFeatureDefinition versionlessDef = getFeatureDef(shortBaseName);
                if (versionlessDef == null) {
                    skipReason = "Versionless not found [ " + shortBaseName + " ]";
                } else if (RepositoryUtil.isNoShip(versionlessDef)) {
                    skipReason = "Versionless is no-ship [ " + shortBaseName + " ]";

                } else {
                    versionlessInternalSymName = RepositoryUtil.asInternalVersionlessFeatureName(symName);
                    ProvisioningFeatureDefinition internalDef = getFeatureDef(versionlessInternalSymName);
                    if (internalDef == null) {
                        String altName = RepositoryUtil.getRename(symName);
                        if (altName != null) {
                            versionlessInternalSymName = RepositoryUtil.asInternalVersionlessFeatureName(altName);
                            internalDef = getFeatureDef(versionlessInternalSymName);
                        }
                    }
                    if (internalDef == null) {
                        skipReason = "Internal versionless not found [ " + versionlessInternalSymName + " ]";
                    } else if (RepositoryUtil.isNoShip(internalDef)) {
                        skipReason = "Internal versionless is no-ship [ " + versionlessInternalSymName + " ]";

                    } else {
                        skipReason = null;
                    }
                }
            }
        }

        if (skipReason == null) {
            if (RepositoryUtil.isJDBCVersionlessException(symName, platform)) {
                skipReason = "JDBC 4.3 exception case for platform [ " + platform + " ]";
            }
        }

        if (skipReason != null) {
            System.out.println("Skipping singleton [ " + symName + " ]: " + skipReason);
            return null;
        }

        VerifyCase newCase = new VerifyCase();

        newCase.name = "versionless - " + shortName + " - from " + inputCase.name;
        newCase.description = "versionless - platform " + platform + " - from " + inputCase.description;

        newCase.durationNs = inputCase.durationNs;

        if (inputCase.input.isMultiple) {
            newCase.input.setMultiple();
        }

        for (String kernelName : inputCase.input.kernel) {
            newCase.input.addKernel(kernelName);
        }

        newCase.input.addRoot(shortBaseName);
        newCase.input.addPlatform(platform);

        newCase.output.add(ResultData.PLATFORM_RESOLVED, platform);

        // Correction for bean validation: The resolution crosses
        // a rename-boundary.  Without this correction an the test
        // fails:
        //
        // Feature resolution [ versionless - platform jakartaee-11.0 - from Singleton [ io.openliberty.beanValidation-3.1 ] ]
        // failed with [ 1 ] errors:
        // Incorrect [ Versionless resolutions ]:
        //   Key [ beanValidation ]
        //     Expected value [ beanValidation-3.1 ]
        //     Actual value [ validation-3.1 ]

        if (shortBaseName.equals("beanValidation") && shortName.equals("beanValidation-3.1")) {
            shortName = "validation-3.1";
        }
        newCase.output.putVersionlessResolved(shortBaseName, shortName);

        newCase.output.addResolved(versionlessInternalSymName);
        newCase.output.addResolved(shortBaseName);

        for (String featureName : inputCase.output.getResolved()) {
            newCase.output.addResolved(featureName);
        }

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
        return asCases(verifyData, null);
    }

    public static Collection<Object[]> asCases(VerifyData verifyData, CaseSelector selector) {
        List<? extends VerifyCase> cases = verifyData.getCases();

        List<Object[]> params = new ArrayList<>(cases.size());
        for (VerifyCase aCase : cases) {
            if ((selector == null) || selector.accept(aCase)) {
                params.add(new Object[] { aCase.name, aCase });
            }
        }
        return params;
    }

    public interface CaseSelector {
        boolean accept(VerifyCase aCase);
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
        if (getFeatureDef(rootFeature) == null) {
            String message = "Root feature [ " + rootFeature + " ]: Missing from baseline";
            return Collections.singletonList(message);
        } else {
            return null;
        }
    }

    public List<String> detectVersionlessSingletonErrors(List<String> rootFeatures) {
        String rootFeature = rootFeatures.get(0);

        if (getVersionlessFeatureDef(rootFeature) == null) {
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

        if (getFeatureDef(rootFeature0) == null) {
            String message = "Missing root feature 0 [ " + rootFeature0 + " ]";

            rootErrors = new ArrayList<>(2);
            rootErrors.add(message);
        }

        if (getFeatureDef(rootFeature1) == null) {
            String message = "Missing root feature 1 [ " + rootFeature1 + " ]";

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
                                EnumSet.allOf(ProcessType.class),
                                verifyCase.input.platforms);
    }

    protected void testBanner(VerifyCase useTestCase) {
        System.out.println("Verifying case:");
        System.out.println("  Name [ " + useTestCase.name + " ]");
        System.out.println("  Description [ " + useTestCase.description + " ]");
    }

    public void doTestResolve() throws Exception {
        VerifyCase inputCase = getTestCase();

        largeDashes(System.out);

        List<String> rootErrors = detectFeatureErrors(inputCase.input.roots);
        if (rootErrors != null) {
            testBanner(inputCase);
            FailureSummary summary = addFailure(inputCase, rootErrors);
            fail(summary.getMessage());
            return; // 'fail' never returns.
        }

        long startNs = System.nanoTime();
        Result result = resolveFeatures(inputCase, rootErrors);
        long endNs = System.nanoTime();

        VerifyCase outputCase = new VerifyCase(inputCase, result, endNs - startNs);

        List<String> missing = new ArrayList<>();
        List<String> extra = new ArrayList<>();

        VerifyDelta.ChangeMessages caseMessages = VerifyDelta.compare(RepositoryUtil.getSupplier(),
                                                                      inputCase, outputCase,
                                                                      !VerifyDelta.UPDATED_USED_KERNEL,
                                                                      extra, missing,
                                                                      getAllowedSubstitution(inputCase));

        if (!caseMessages.hasErrors() && !caseMessages.hasWarnings() && !caseMessages.hasInfo()) {
            System.out.println("Verified case [ " + inputCase.name + " ]");
            return;
        }

        testBanner(inputCase);

        if (!caseMessages.hasErrors()) {
            System.out.println("Verified");
        } else {
            System.out.println("Verification failure:");

            System.out.println("Errors [ " + caseMessages.errors.size() + " ]:");
            for (String error : caseMessages.errors) {
                System.out.println("  [ " + error + " ]");
            }
            write("Revised Case:", outputCase, System.out);
        }

        if (caseMessages.hasWarnings()) {
            System.out.println("Warnings [ " + caseMessages.warnings.size() + " ]:");
            for (String warning : caseMessages.warnings) {
                System.out.println("  [ " + warning + " ]");
            }
        }

        if (caseMessages.hasInfo()) {
            System.out.println("Info [ " + caseMessages.info.size() + " ]:");
            for (String infoMsg : caseMessages.info) {
                System.out.println("  [ " + infoMsg + " ]");
            }
        }

        if (caseMessages.hasErrors()) {
            FailureSummary summary = addFailure(inputCase, caseMessages.errors);
            fail(summary.getMessage());
            return; // 'fail' never returns.
        }
    }

    public Map<String, String[]> getAllowedSubstitutions() {
        return Collections.emptyMap();
    }

    public String[] getAllowedSubstitution(VerifyCase useTestCase) {
        return null;
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
