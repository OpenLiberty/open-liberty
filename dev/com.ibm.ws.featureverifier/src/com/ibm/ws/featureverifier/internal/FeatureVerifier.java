/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.featureverifier.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.aries.util.io.IOUtils;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.xml.sax.SAXException;

import com.ibm.aries.buildtasks.semantic.versioning.EmptyClassVisitor;
import com.ibm.aries.buildtasks.semantic.versioning.SemanticVersioningClassVisitor;
import com.ibm.aries.buildtasks.semantic.versioning.SerialVersionClassVisitor;
import com.ibm.aries.buildtasks.semantic.versioning.model.FeatureInfo;
import com.ibm.aries.buildtasks.semantic.versioning.model.FeatureInfo.ApiSpiJarKey;
import com.ibm.aries.buildtasks.semantic.versioning.model.FrameworkInfo;
import com.ibm.aries.buildtasks.semantic.versioning.model.PackageContent;
import com.ibm.aries.buildtasks.semantic.versioning.model.PkgInfo;
import com.ibm.aries.buildtasks.semantic.versioning.model.VersionedEntity;
import com.ibm.aries.buildtasks.semantic.versioning.model.VersionedEntityMap;
import com.ibm.aries.buildtasks.semantic.versioning.model.XMLBaseLineUtils;
import com.ibm.aries.buildtasks.semantic.versioning.model.decls.ClassDeclaration;
import com.ibm.aries.buildtasks.semantic.versioning.model.decls.FieldDeclaration;
import com.ibm.aries.buildtasks.semantic.versioning.model.decls.MethodDeclaration;
import com.ibm.ws.featureverifier.internal.PackageComparator.VersionResult;
import com.ibm.ws.featureverifier.internal.XmlErrorCollator.ReportType;
import com.ibm.ws.featureverifier.internal.querymode.FeatureQueryMode;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

public class FeatureVerifier implements BundleActivator {

    private static final String ALL_DONE_TXT = "AllDone.txt";
    private static final String FRAMEWORK_XML = "framework.xml";
    private static final String NEWFRAMEWORK_XML_ZIP = "newframework.xml.zip";
    private static final String FRAMEWORK_XML_ZIP = "framework.xml.zip";
    private static final String GLOBALCONFIG_XML_FILENAME = "ignore.xml";
    private static final String MASTER_XML_FILENAME = "master.xml";
    private static final String ERROR_XML = "error.xml";
    private static final String JUNIT_XML = "junit.xml";

    //the kernel is special, it's always present, but not actually in the installedFeatures list.
    public static final String[] KERNEL_FEATURES = {
                                                     "com.ibm.websphere.appserver.kernel-1.0",
                                                     "com.ibm.websphere.appserver.kernelCore-1.0",
                                                     "com.ibm.websphere.appserver.os400.extensions-1.0",
                                                     "com.ibm.websphere.appserver.zos.extensions-1.0",
                                                     "com.ibm.websphere.appserver.zos.no.console.extensions-1.0",
                                                     "com.ibm.websphere.appserver.logging-1.0",
                                                     "com.ibm.websphere.appserver.binary.logging-1.0" };

    private static enum Change {
        MODIFIED_MINOR, MODIFIED_MAJOR
    };

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        try {
            //load the exclusion filter etc..
            loadGlobalConfig(context);

            //learn about system packages..
            readSystemPackages(context);

            //attempt to load up the old baseline, if one is present..
            FrameworkInfo baseline = null;
            try {
                //don't load baseline if in no compare mode.
                if (GlobalConfig.getEditionNameToTestAllFeaturesForWithoutComparison() == null) {
                    //Try and load the xml representing the baseline,
                    baseline = loadBaseXml(context);
                }
            } catch (Exception e) {
                //ignore for now.
                baseline = null;
            }

            System.out.println("Liberty Introspection starting... ");

            if (GlobalConfig.GenerateGlobalConfigMode) {
                System.out.println("Generate GlobalConfig Mode engaged. ");
            }

            if (GlobalConfig.ApiSpiReviewMode) {
                System.out.println("API/SPI Review Mode engaged. ");
            }

            //we plan to ask liberty for the installed features list,
            //then hunt & load ALL feature manifests, then we can
            //treat our loaded manifests as a kind of reference db that we
            //will query using the features installed in liberty.

            //once we have the details for the installed features, we can
            //reason over their api/spi to discover the packages each feature
            //exposes, and can check if the set of api/spi for a given feature
            //is exported by content within the feature, or from elsewhere.

            //start by obtaining the features currently configured in the runtime
            Set<String> featureSet = FeatureProvisionerWrapper.obtainInstalledFeatures(context);

            //the kernel is special, it's always present, but not actually in the installedFeatures list.
            featureSet.addAll(Arrays.asList(KERNEL_FEATURES));

            System.out.println("Featureset " + featureSet);

            if (GlobalConfig.GenerateGlobalConfigMode) {
                //create a new GlobalConfig file for this server based on messages available in master config
                WsLocationAdmin wla = context.getService(context.getServiceReference(WsLocationAdmin.class));
                String outputDirString = wla.resolveString(WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR);
                File outputDir = new File(outputDirString);
                File frameworkLog = new File(outputDir, GLOBALCONFIG_XML_FILENAME);
                File masterConfig = new File(outputDir, MASTER_XML_FILENAME);
                GlobalConfig.generateConfigFile(frameworkLog, masterConfig, featureSet);
                return;
            }

            //now hunt down all the features liberty knows about
            //we only process lib and platform dir.
            List<FeatureInfo> allInstalledFeatures = obtainAllFeaturesFromDisk(context);

            //trim the list of features we'll process back to just those currently installed.
            //this conveniently has the side-effect of removing any singletons not selected for
            //this server config. Which makes our task of aggregating features for api checking
            //much simpler.
            List<FeatureInfo> featuresToProcess = trimFeatureList(featureSet, allInstalledFeatures);

            //new trick.. ignore missing features when told to.
            if (GlobalConfig.ignoreMissingFeatures && baseline != null) {
                baseline = filterBaselineByActiveFeatures(baseline, featuresToProcess);
            }

            VersionedEntityMap<PkgInfo, FeatureInfo> packageToFeature = new VersionedEntityMap<PkgInfo, FeatureInfo>();
            Map<String, FeatureInfo> featureNameToFeatureInfo = new TreeMap<String, FeatureInfo>();
            FeatureBundleRepository featureBundleRepository = new FeatureBundleRepository();

            //build up the global maps we'll use to speed things up..
            buildIndexes(featuresToProcess, packageToFeature, featureNameToFeatureInfo, featureBundleRepository);

            //build up the aggregates
            for (FeatureInfo f : featuresToProcess) {
                f.collectAggregateInformation(featureNameToFeatureInfo);
            }

            if (GlobalConfig.getSetGenerate()) {
                System.out.println("SetGeneration Mode enabled : " + String.valueOf(GlobalConfig.getSetGenerateEditionName()) + " :: "
                                   + String.valueOf(GlobalConfig.getSetGenerateConfigPrefix()));

                Collection<FeatureInfo> startingSetForSetGeneration = new HashSet<FeatureInfo>();
                if (GlobalConfig.getSetGenerateEditionName() != null) {
                    //we can now use edition names to figure out which features to process..
                    String editionName = GlobalConfig.getSetGenerateEditionName();
                    startingSetForSetGeneration.addAll(FeatureInfoUtils.filterFeaturesByEdition(editionName, allInstalledFeatures, context, !GlobalConfig.ignoreMissingFeatures));
                } else {
                    startingSetForSetGeneration = allInstalledFeatures;
                }
                List<FeatureInfo> featuresToUseInSetGenerate = trimFeatureListForSetGenerate(startingSetForSetGeneration);

                //dump out the server configs...
                FeatureSetGenerator.generateFeatureSets(featuresToUseInSetGenerate, context);
            }

            if (GlobalConfig.getEditionNameToTestAllFeaturesForWithoutComparison() != null) {
                String editionName = GlobalConfig.getEditionNameToTestAllFeaturesForWithoutComparison();
                System.out.println("Global NoCompare Mode enabled : " + editionName);
                Collection<FeatureInfo> setForNoCompareTests = new HashSet<FeatureInfo>();
                setForNoCompareTests.addAll(FeatureInfoUtils.filterFeaturesByEdition(editionName, allInstalledFeatures, context, !GlobalConfig.ignoreMissingFeatures));
                if ((GlobalConfig.ignoreMissingFeatures)) {
                    GlobalConfig.restrictUnusedReportingToFeatureSet(setForNoCompareTests);
                }
                NoCompareFeatureValidator.testFeaturesWithoutBaselineCompare(setForNoCompareTests, allInstalledFeatures);
                return;
            }

            if (GlobalConfig.getQueryMode() != null) {
                FeatureQueryMode fmq = new FeatureQueryMode(GlobalConfig.getQueryMode(), allInstalledFeatures, context);
                return;
            }

            //now scan the current runtime, to build up a new baseline.
            FrameworkInfo newBaseLine = generateNewBaseline(context, packageToFeature, featureNameToFeatureInfo, featureBundleRepository);

            if (baseline != null) {
                //some basic validation
                //we only put this out to console, as it's interesting, but not anything we'd ever fail or warn a build over.
                //(Also handy during development if you clone newframework.zip to framework.zip the figures should match).
                System.out.println("Bundles size  -    Baseline : " + baseline.getBundleToPkgs().keySet().size());
                System.out.println("                NewBaseline : " + newBaseLine.getBundleToPkgs().keySet().size());
                System.out.println("Features size -    Baseline : " + baseline.getFeatureInfos().size());
                System.out.println("                NewBaseline : " + newBaseLine.getFeatureInfos().size());
                System.out.println("Packages size -    Baseline : " + baseline.getPkgInfo().keySet().size());
                System.out.println("                NewBaseline : " + newBaseLine.getPkgInfo().keySet().size());

                //start by evaulating if the feature set is still the same..
                Collection<FeatureInfo> baselinefeatures = new ArrayList<FeatureInfo>();
                baselinefeatures.addAll(baseline.getFeatureInfos());
                baselinefeatures.removeAll(newBaseLine.getFeatureInfos());
                if (!baselinefeatures.isEmpty()) {
                    //features have been removed.. these are errors.
                    for (FeatureInfo feature : baselinefeatures) {
                        XmlErrorCollator.setCurrentFeature(feature.getName());
                        XmlErrorCollator.setNestedFeature(feature.getName());
                        XmlErrorCollator.addReport("Feature: " + feature.getName(),
                                                   null,
                                                   ReportType.ERROR,
                                                   //summary
                                                   "Missing Feature " + feature.getName(),
                                                   //shortText
                                                   "[MISSING_FEATURE " + feature.getName() + "]",
                                                   //reason
                                                   "Feature " + feature.getName() + " " + feature.getVersion()
                                                                                                  + " is not found in the new build. Has the feature been deleted? or is the build incomplete?");
                        System.out.println("ERROR: feature " + feature.getName() + " " + feature.getVersion() + " is no longer found in the new build.");
                    }
                }
                Collection<FeatureInfo> newbaselinefeatures = new ArrayList<FeatureInfo>();
                newbaselinefeatures.addAll(newBaseLine.getFeatureInfos());
                newbaselinefeatures.removeAll(baseline.getFeatureInfos());
                if (!newbaselinefeatures.isEmpty()) {
                    //features have been added, these are infos.
                    for (FeatureInfo feature : newbaselinefeatures) {
                        XmlErrorCollator.setCurrentFeature(feature.getName());
                        XmlErrorCollator.setNestedFeature(feature.getName());
                        XmlErrorCollator.addReport("Feature: " + feature.getName(),
                                                   null,
                                                   ReportType.INFO,
                                                   //summary
                                                   "New Feature " + feature.getName(),
                                                   //shortText
                                                   "[NEW_FEATURE " + feature.getName() + "]",
                                                   //reason
                                                   "Feature " + feature.getName() + " " + feature.getVersion()
                                                                                              + " is not known to the baseline, no compatibility testing will be possible for this feature.");

                        System.out.println("INFO: feature " + feature.getName() + " " + feature.getVersion() + " is not known to the baseline.");

                        if (GlobalConfig.ApiSpiReviewMode) {
                            generateNewFeatureApiSpiReviewDetail(newBaseLine, feature);
                        }
                    }
                }

                //record if the feature sets are the same, it can be handy to be able to check this quickly
                //in the logs later..
                if (baselinefeatures.isEmpty() && newbaselinefeatures.isEmpty()) {
                    System.out.println("INFO: feature sets matched between baseline and build.");
                }

                //now for the features that match.. we'll do our compares.
                Collection<FeatureInfo> matchingfeatures = new ArrayList<FeatureInfo>();
                matchingfeatures.addAll(baseline.getFeatureInfos()); //add all from baseline..
                matchingfeatures.removeAll(baselinefeatures); //remove all no-longer-founds
                if (!matchingfeatures.isEmpty()) { //remaining set is the overlap.
                    //compare each feature present in build & baseline.
                    for (FeatureInfo matchingFeature : matchingfeatures) {
                        compareFeature(baseline, newBaseLine, matchingFeature);
                    }
                } else {
                    //no overlap.
                    System.out.println("ERROR: feature sets between baseline & build had NO overlap");
                    XmlErrorCollator.addProcessingIssue(ReportType.ERROR, "Feature sets between baseline & build had NO overlap, no compatibility testing is possible.");
                }
            }
        } catch (Throwable e) {
            //We don't expect exceptions, but when we get them, we'll need the info in the logs..
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace(System.out);
            try {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                XmlErrorCollator.addProcessingIssue(ReportType.ERROR, sw.toString());
            } catch (Exception e2) {
                //not much we can do here!!
            }
        } finally {
            if (GlobalConfig.getQueryMode() == null) {
                try {
                    System.out.println("==== XML DUMP ====");
                    XmlErrorCollator.dumpRecordsToSysout();
                    XmlErrorCollator.newDumpRecordsToXML(getErrorXMLFile(context), getJUnitFile(context));
                } finally {
                    //once we've done our work, create a marker file to say we're all done.
                    createExitMarker(context);
                }
            }
        }
    }

    /**
     * Loads a stored ignore list of issues deemed acceptable in the current build.
     *
     * @param context
     * @return
     * @throws IOException
     */
    private void loadGlobalConfig(BundleContext context) throws Exception {
        WsLocationAdmin wla = context.getService(context.getServiceReference(WsLocationAdmin.class));
        String outputDirString = wla.resolveString(WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR);
        File outputDir = new File(outputDirString);
        File frameworkLog = new File(outputDir, GLOBALCONFIG_XML_FILENAME);
        System.out.println("Attempting to load global config from " + frameworkLog.getAbsolutePath() + " exists? " + frameworkLog.exists());
        GlobalConfig.setConfigFile(frameworkLog);
    }

    private final Set<String> systemPackages = new HashSet<String>();
    private final Set<String> systemExports = new HashSet<String>();

    /** Populates systemPackages and systemExports. */
    private void readSystemPackages(BundleContext context) {
        String syspackages = context.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
        System.out.println("@@syspkg:  " + syspackages);
        String syspackagesex = context.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
        System.out.println("@@syspkgex:  " + syspackagesex);

        List<NameValuePair> sp = ManifestHeaderProcessor.parseExportString(syspackages);
        List<NameValuePair> spex = ManifestHeaderProcessor.parseExportString(syspackages);
        for (NameValuePair nvp : sp) {
            String name = nvp.getName();
            systemPackages.add(name);
        }
        for (NameValuePair nvp : spex) {
            String name = nvp.getName();
            systemPackages.add(name);
        }

        //normally this would be a bad idea.. but bundle 0 has to exist, and has to be resolved,
        //or we wouldn't be here..
        BundleWiring bw = context.getBundle(0).adapt(BundleWiring.class);
        List<BundleCapability> bcs = bw.getCapabilities(BundleRevision.PACKAGE_NAMESPACE);
        for (BundleCapability bc : bcs) {
            String pkgName = bc.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE).toString();
            systemExports.add(pkgName);
            //String pkgVersion = bc.getAttributes().get(Constants.VERSION_ATTRIBUTE).toString();
        }
    }

    /**
     * Loads a stored framework baseline from disk, to allow for comparisons.
     *
     * @param context
     * @return
     * @throws IOException
     */
    private FrameworkInfo loadBaseXml(BundleContext context) throws IOException {
        WsLocationAdmin wla = context.getService(context.getServiceReference(WsLocationAdmin.class));
        String outputDirString = wla.resolveString(WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR);
        File outputDir = new File(outputDirString);
        File frameworkLog = new File(outputDir, FRAMEWORK_XML_ZIP);
        if (frameworkLog.exists()) {
            System.out.println("Framework Log : " + frameworkLog.getAbsolutePath());
            try {
                return XMLBaseLineUtils.loadBaseXML(frameworkLog);
            } catch (Exception e) {
                XmlErrorCollator.addProcessingIssue(ReportType.ERROR, "Unable to load baseline framework log " + frameworkLog.getAbsolutePath());
                System.out.println("ERROR: unable to load old framework log.. ");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            XmlErrorCollator.addProcessingIssue(ReportType.ERROR, "No existing baseline framework log found at " + frameworkLog.getAbsolutePath());
            System.out.println("WARNING: No framework log found at " + frameworkLog.getAbsolutePath() + " will produce a new one for this release");
            return null;
        }
    }

    /**
     * Scans the lib/features dir and lib/platform dir for feature manifests,
     * and builds up FeatureInfo instances for each feature found.
     *
     * @param context
     * @return
     * @throws IOException
     */
    private List<FeatureInfo> obtainAllFeaturesFromDisk(BundleContext context) throws IOException {
        //Obtain the lib directory.. this is almost a nice way to do it ;p
        WsLocationAdmin wla = context.getService(context.getServiceReference(WsLocationAdmin.class));
        String installDirString = wla.resolveString(WsLocationConstants.SYMBOL_INSTALL_DIR);
        File installDir = new File(installDirString);

        //now hunt down all the manifests
        List<File> manifestsToProcess = new ArrayList<File>();
        File libDir = new File(installDir, "lib");
        File featureDir = new File(libDir, "features");
        File libmanifests[] = featureDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toUpperCase().endsWith(".MF");
            }
        });
        manifestsToProcess.addAll(Arrays.asList(libmanifests));
        File platformDir = new File(libDir, "platform");
        File platformmanifests[] = platformDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toUpperCase().endsWith(".MF");
            }
        });
        manifestsToProcess.addAll(Arrays.asList(platformmanifests));

        //now build a map of symbolic name/versions to jar paths under dev..
        //we'll need this for all those content lines that do type=jar with a version range.
        File devDir = new File(installDir, "dev");
        Map<VersionedEntity, String> devJars = collectDevJars(devDir, installDir);

        //hey.. apparently some dev jars are actually in lib.. this may be historical due
        //to the old apichecker, but thanks to that, we need to look there too.
        Map<VersionedEntity, String> libJars = collectDevJars(libDir, installDir);
        Map<VersionedEntity, String> allJars = new HashMap<VersionedEntity, String>();
        allJars.putAll(devJars);
        allJars.putAll(libJars);
        if (allJars.size() != libJars.size() + devJars.size()) {
            StringBuffer error = new StringBuffer();
            error.append("Duplicate symbolicName / versioned jar found in lib and also in dev .. this is bad.. Feature checker cannot reason over content when it is present in multiple locations. ");
            System.out.println("Adding lib jars to dev jars caused " + (libJars.size() + devJars.size() - allJars.size()) + " entries to be overwritten..");
            Set<VersionedEntity> intersection = new HashSet<VersionedEntity>(libJars.keySet());
            intersection.retainAll(devJars.keySet());
            for (VersionedEntity inBoth : intersection) {
                error.append("{ key='" + inBoth + "', lib='" + libJars.get(inBoth) + "', dev='" + devJars.get(inBoth) + "'}");
            }
            throw new IllegalStateException(error.toString());
        }

        List<FeatureInfo> allInstalledFeatures = new ArrayList<FeatureInfo>();
        for (File manifest : manifestsToProcess) {
            FeatureInfo fi = FeatureInfo.createFromManifest(manifest, installDirString, context, allJars);

            //is this feature to be ignored totally ?
            if (!GlobalConfig.isFeatureToBeIgnoredInRuntime(fi.getName())) {
                allInstalledFeatures.add(fi);
            }
        }

        return allInstalledFeatures;
    }

    private Map<VersionedEntity, String> collectDevJars(File devDir, File installDir) throws IOException {
        Set<File> jars = new HashSet<File>();
        collectJarsUnderPath(devDir, jars);
        Map<VersionedEntity, String> results = new HashMap<VersionedEntity, String>();
        for (File j : jars) {
            boolean foundName = false;
            JarFile jf = new JarFile(j);
            try {
                Manifest m = jf.getManifest();
                if (m != null) {
                    //eg.
                    //Bundle-SymbolicName: com.ibm.ws.org.osgi.core.4.2.0
                    //Bundle-Version: 1.0.1.201410061245
                    String symbname = m.getMainAttributes().getValue("Bundle-SymbolicName");
                    if (symbname != null) {
                        NameValuePair name = ManifestHeaderProcessor.parseBundleSymbolicName(symbname);
                        String version = m.getMainAttributes().getValue("Bundle-Version");
                        VersionedEntity ve = new VersionedEntity(name.getName(), version);
                        String path = j.getCanonicalPath().substring(installDir.getCanonicalPath().length() + 1);

                        if (File.separatorChar != '/') {
                            //windows system..
                            path = path.replace('\\', '/');
                        }
                        foundName = true;
                        int beforeCount = results.size();
                        String old = results.put(ve, path);
                        int afterCount = results.size();
                        System.out.println("Using map to remember dev jar " + ve + " with " + path);
                        if (old != null || beforeCount == afterCount) {
                            System.out.println("ERROR: adding entry to map displaced existing entry " + old);
                        }
                    }
                }
            } finally {
                jf.close();
            }
            if (!foundName) {
                System.out.println("ERROR: dev jar " + j.getAbsolutePath() + " had no manifest");
            }
        }
        return results;
    }

    private void collectJarsUnderPath(File devDir, Set<File> jars) {
        for (File f : devDir.listFiles()) {
            if (f.isDirectory()) {
                collectJarsUnderPath(f, jars);
            } else {
                if (f.getName().toLowerCase().endsWith(".jar")) {
                    jars.add(f);
                }
            }
        }
    }

    /**
     * Removes protected, test and ignored features from the feature list.
     *
     * @param featureSet
     * @param allInstalledFeatures
     * @return
     */
    private List<FeatureInfo> trimFeatureList(Set<String> featureSet, List<FeatureInfo> allInstalledFeatures) {
        List<FeatureInfo> featuresToProcess = new ArrayList<FeatureInfo>();
        for (FeatureInfo f : allInstalledFeatures) {
            if (featureSet.contains(f.getName()) || featureSet.contains(f.getShortName())) {
                if (!f.getName().startsWith("protected") && !f.getTestFeature() && !GlobalConfig.isFeatureToBeIgnoredInRuntime(f.getName())) {
                    featuresToProcess.add(f);
                }
            }
        }
        return featuresToProcess;
    }

    /**
     * @param baseline
     * @param featuresToProcess
     * @return
     */
    private FrameworkInfo filterBaselineByActiveFeatures(FrameworkInfo baseline, List<FeatureInfo> featuresToProcess) {
        FrameworkInfo retval = new FrameworkInfo();
        List<FeatureInfo> basefeatures = baseline.getFeatureInfos();
        List<FeatureInfo> retained = new ArrayList<FeatureInfo>();
        for (FeatureInfo f : basefeatures) {
            if (featuresToProcess.contains(f)) {
                retained.add(f);
            } else {
                System.out.println("Ignoring " + f.getName() + " due to ignoreMissingFeatures flag");
                GlobalConfig.addFeatureToIgnore(f);
            }
        }
        retval.setBundleToPkgs(baseline.getBundleToPkgs());
        retval.setPkgInfo(baseline.getPkgInfo());
        retval.setFeatureInfos(retained);
        return retval;
    }

    /**
     * Builds the package->feature map and featurename->feature map by processing the list of featureInfos.
     *
     * @param featureInfos
     * @param packageToFeature
     * @param featureNameToFeatureInfo
     */
    private void buildIndexes(List<FeatureInfo> featureInfos, VersionedEntityMap<PkgInfo, FeatureInfo> packageToFeature, Map<String, FeatureInfo> featureNameToFeatureInfo,
                              FeatureBundleRepository fbr) {
        for (FeatureInfo f : featureInfos) {
            if (GlobalConfig.isFeatureToBeIgnoredInRuntime(f.getName())) {
                continue;
            }

            featureNameToFeatureInfo.put(f.getName(), f);
            fbr.addFeature(f);
            if (f.getShortName() != null) {
                featureNameToFeatureInfo.put(f.getShortName(), f);
            }
            for (Map.Entry<PkgInfo, Set<Map<String, String>>> api : f.getLocalAPI().entrySet()) {
                packageToFeature.merge(api.getKey(), f);
            }
            for (Map.Entry<PkgInfo, Set<Map<String, String>>> spi : f.getLocalSPI().entrySet()) {
                packageToFeature.merge(spi.getKey(), f);
            }

        }
        System.out.println("Known APISPI: " + packageToFeature.keySet());
    }

    /**
     * Removes protected, test and ignored features from the feature list.
     *
     * @param startingSetForSetGeneration
     * @return
     */
    private List<FeatureInfo> trimFeatureListForSetGenerate(Collection<FeatureInfo> startingSetForSetGeneration) {
        List<FeatureInfo> featuresToUseInSetGenerate = new ArrayList<FeatureInfo>();
        for (FeatureInfo f : startingSetForSetGeneration) {
            if (!f.getName().startsWith("protected") && !f.getTestFeature() && !GlobalConfig.featuresToIgnoreDuringSetGeneration().contains(f.getName())) {
                featuresToUseInSetGenerate.add(f);
            } else {
                System.out.println("Ignoring feature " + f.getName() + " during generation excludedByConfig?"
                                   + (GlobalConfig.featuresToIgnoreDuringSetGeneration().contains(f.getName())) + " testFeature?" + f.getTestFeature() + " protected?"
                                   + f.getName().startsWith("protected"));

            }
        }
        return featuresToUseInSetGenerate;
    }

    /**
     * Scan the current runtime, and generate a new baseline xml.
     *
     * Basic validation is performed during the scan, looking for
     * - split packages
     * - duplicate identical packages
     * - duplicate inconsistent packages
     * - declared api/spi packages found with no content
     * - declared api/spi packages not found in runtime
     *
     * @param context
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private FrameworkInfo generateNewBaseline(BundleContext context,
                                              VersionedEntityMap<PkgInfo, FeatureInfo> packageToFeature,
                                              Map<String, FeatureInfo> featureNameToFeatureInfo,
                                              FeatureBundleRepository featureBundleRepository) throws IOException, SAXException, ParserConfigurationException {

        FrameworkInfo newBaseLine = new FrameworkInfo();

        //Map<PkgInfo, PackageContent> pkgToPkgContent = new TreeMap<PkgInfo, PackageContent>();
        VersionedEntityMap<PkgInfo, PackageContent> pkgToPkgContent = new VersionedEntityMap<PkgInfo, PackageContent>();
        Map<VersionedEntity, Set<PkgInfo>> bundleToPkgs = new TreeMap<VersionedEntity, Set<PkgInfo>>();

        //although in normal usage we only want to compare our current build against
        //an existing baseline, we'll also always create a new baseline that represents
        //the current build.

        //We'll store that as XML.. get a writer that we can use to store the info..
        PrintWriter pw = getBaselineXMLWriter(context);
        pw.println("<baseline timestamp=\"" + System.currentTimeMillis() + "\">");

        //work through each bundle in the running framework, to write out the baseline
        //info for every package that ever could be api/spi.
        Bundle[] installedBundles = context.getBundles();
        processPackagesExportedFromBundles(installedBundles, pw, featureNameToFeatureInfo, packageToFeature, pkgToPkgContent, bundleToPkgs, featureBundleRepository);

        //write out the feature / package set information.
        processFeatures(pw, featureNameToFeatureInfo);

        pw.println("</baseline>");
        pw.flush();
        pw.close();

        //now build up the new baseline structures..
        Set<FeatureInfo> featureInfos;
        featureInfos = new HashSet<FeatureInfo>();
        for (FeatureInfo f : featureNameToFeatureInfo.values()) {
            //if("public".equals(f.getVisibility()) || "protected".equals(f.getVisibility())){
            featureInfos.add(f);
            System.out.println("featureinfos : " + f.hashCode() + " " + f.getName());
            //}
        }
        List<FeatureInfo> features = new ArrayList<FeatureInfo>(featureInfos);

        newBaseLine.setFeatureInfos(features);
        newBaseLine.setPkgInfo(pkgToPkgContent);
        newBaseLine.setBundleToPkgs(bundleToPkgs);

        return newBaseLine;
    }

    /**
     * Obtain a writer to store the new baseline xml to.
     *
     * @param context
     * @return
     * @throws IOException
     */
    private PrintWriter getBaselineXMLWriter(BundleContext context) throws IOException {
        WsLocationAdmin wla = context.getService(context.getServiceReference(WsLocationAdmin.class));
        String outputDirString = wla.resolveString(WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR);
        File outputDir = new File(outputDirString);
        File frameworkLog = new File(outputDir, NEWFRAMEWORK_XML_ZIP);
        System.out.println("Framework Log : " + frameworkLog.getAbsolutePath());

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(frameworkLog));
        ZipEntry ze = new ZipEntry(FRAMEWORK_XML);
        zos.putNextEntry(ze);
        OutputStreamWriter osw = new OutputStreamWriter(zos);
        return new PrintWriter(osw);
    }

    /**
     * Processes all the bundles passed, and evaluates their exported packages, looking for packages
     * the features know of as api/spi.
     *
     * Creates the package content and stores that in the pkginfo->packagecontent map.
     *
     * @param installedBundles set of bundles to process from the runtime
     * @param pw the printwriter to output xml representing the found packages to.
     * @param featureNameToFeatureInfoMap a prepopulated map of feature name -> feature info, used to obtain info on all features.
     * @param packageToFeatureMap a prepopulated map of pkginfo->feature info, storing features (from featureNameToFeatureInfo value set) that declare the key as api/spi
     * @param pkgToPkgContent a map to be updated with PackageContent instances for each Package processed as api/spi
     * @param bundleToPkgs a map to be updated with with info on which bundles supplied which packages.
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private void processPackagesExportedFromBundles(Bundle[] installedBundles,
                                                    PrintWriter pw,
                                                    Map<String, FeatureInfo> featureNameToFeatureInfoMap,
                                                    VersionedEntityMap<PkgInfo, FeatureInfo> packageToFeatureMap,
                                                    VersionedEntityMap<PkgInfo, PackageContent> pkgToPkgContent,
                                                    Map<VersionedEntity, Set<PkgInfo>> bundleToPkgs,
                                                    FeatureBundleRepository featureBundleRepository) throws IOException, SAXException, ParserConfigurationException {

        //used to track if we have seen packages before from other bundles..
        Map<PkgInfo, Set<Bundle>> packageToBundle = new HashMap<PkgInfo, Set<Bundle>>();
        //used to build up the xml we'll write out at the end of this method.
        Map<PkgInfo, String> packageToXml = new TreeMap<PkgInfo, String>();

        //as we step thru the packages, bundle by bundle.. lets make note if we've seen all the packages for each feature.
        Map<FeatureInfo, Set<PkgInfo>> featureAPI = new HashMap<FeatureInfo, Set<PkgInfo>>();
        Map<FeatureInfo, Set<PkgInfo>> featureSPI = new HashMap<FeatureInfo, Set<PkgInfo>>();
        //we'll use a map of feature info -> set of pkg info, to track the pkgs we have seen.
        //we'll remove from the value set of each feature as we encounter packages,
        //then afterward, we'll end up with the 'missing' packages for all features.
        for (FeatureInfo f : featureNameToFeatureInfoMap.values()) {
            featureAPI.put(f, new HashSet<PkgInfo>(f.getLocalAPI().keySet()));
            featureSPI.put(f, new HashSet<PkgInfo>(f.getLocalSPI().keySet()));
        }
        //Additionally, we build a map of the package versions seen for each api/spi
        //if we see multiple matches
        Map<FeatureInfo, Map<PkgInfo, Set<PkgInfo>>> featureAPISeen = new HashMap<FeatureInfo, Map<PkgInfo, Set<PkgInfo>>>();
        Map<FeatureInfo, Map<PkgInfo, Set<PkgInfo>>> featureSPISeen = new HashMap<FeatureInfo, Map<PkgInfo, Set<PkgInfo>>>();

        for (Bundle bundle : installedBundles) {
            //fragments will show up in the installed bundle list, but we cannot load classes via them, we can
            //only load them via the hosts, so effectively we evaluate fragment contributed content as if it were
            //contained within the host bundles.
            //
            //this means changes within a fragment may invalidate the versioning of the host.
            //
            if (bundle.getHeaders("").get(Constants.FRAGMENT_HOST) != null) {
                continue;
            }

            //use the bundlewiring api to obtain the exported packages for this bundle.
            BundleWiring bw = bundle.adapt(BundleWiring.class);
            if (bw == null) {
                //System.out.println("INFO: unable to obtain bundle wiring for bundle "+bundle.getSymbolicName()+" "+bundle.getVersion());
                //XmlErrorCollator.addProcessingIssue(ReportType.INFO, "Unable to evaluate packages exported by bundle "+bundle.getSymbolicName()+" "+bundle.getVersion()+" because it was unable to be adapted to bundle wiring.");
                //continue;
                System.out.println("INFO: falling back to header reading for bundle " + bundle.getSymbolicName());
            }

            //bundlewiring can contain duplicate packages.. use a set to remove the dupes
            //we use a pkginfo object that declares equals & hashcode to allow us to treat
            //packages as unique versioned instances.
            Set<PkgInfo> dedupedPkgs = new HashSet<PkgInfo>();
            //we'll also need to build up and store the packages from this bundle that are api/spi
            Set<PkgInfo> apispiPkgs = new HashSet<PkgInfo>();

            if (bw == null) {
                //we couldn't obtain bundle wiring for the bundle (lazy bundle?)
                //but never mind.. we can use the headers to figure out which packages the bundle claims to export.
                //it's not as ideal as using bundlewiring, but it will do for now.
                Dictionary<String, String> headers = bundle.getHeaders("");
                String exports = headers.get(Constants.EXPORT_PACKAGE);
                List<NameValuePair> exportedPackages = ManifestHeaderProcessor.parseExportString(exports);
                for (NameValuePair pkg : exportedPackages) {
                    Map<String, String> attribs = pkg.getAttributes();
                    String version = null;
                    if (attribs != null && attribs.containsKey(Constants.VERSION_ATTRIBUTE)) {
                        version = attribs.get(Constants.VERSION_ATTRIBUTE);
                    }
                    PkgInfo p = new PkgInfo(pkg.getName(), version, bundle.getSymbolicName(), bundle.getVersion().toString(), null);
                    dedupedPkgs.add(p);
                }
            } else {
                //obtain all exported packages.. even if not wired up.
                //
                //gets packages only from this bundle..
                // bw.getRevision().getCapabilities(BundleRevision.PACKAGE_NAMESPACE)
                //
                //gets packages from this bundle, and attached fragments, will omit packages
                //that are exported, and also imported, if this bundle has chosen to import the package
                //from elsewhere..
                // bw.getCapabilities(BundleRevision.PACKAGE_NAMESPACE)
                //
                List<BundleCapability> bcs = bw.getCapabilities(BundleRevision.PACKAGE_NAMESPACE);
                if (bcs == null) {
                    System.out.println("INFO: no active wiring for bundle " + bundle.getSymbolicName() + " " + bundle.getVersion());
                    XmlErrorCollator.addProcessingIssue(ReportType.INFO, "Unable to evaluate packages exported by bundle " + bundle.getSymbolicName() + " " + bundle.getVersion()
                                                                         + " because it had no active wiring in the framework.");
                } else {
                    for (BundleCapability bc : bcs) {
                        String pkgName = bc.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE).toString();
                        String pkgVersion = bc.getAttributes().get(Constants.VERSION_ATTRIBUTE).toString();
                        String mandatory = null;
                        if (bc.getDirectives() != null) {
                            mandatory = bc.getDirectives().get(Constants.MANDATORY_DIRECTIVE);
                        }
                        if (mandatory != null && mandatory.length() > 0) {
                            //ignore this package..
                            System.out.println("INFO: Ignoring package " + pkgName + "@" + pkgVersion + " due to mandatory directive " + mandatory);
                        } else {
                            dedupedPkgs.add(new PkgInfo(pkgName.trim(), pkgVersion.trim(), bundle.getSymbolicName(), bundle.getVersion().toString(), bw));
                        }

                        //if we want to identify which packages come from fragments.. (no need today).
                        //if(!bw.getRevision().equals(bc.getRevision())){
                        //      System.out.println("Package "+pkgName+" came from a fragment.. ");
                        //}

                    }
                }
            }

            //we've now got a nice set of all the packages from this bundle.. but most of them will be
            //stuff we're not interested in.. process the set, and filter by stuff exported as api/spi by
            //features.
            for (PkgInfo p : dedupedPkgs) {

                //is this a package to skip entirely..
                if (GlobalConfig.isPackageToBeIgnoredInRuntime(p, p.getFromBundle() + "@" + p.getFromBundleVersion(), null)
                    || GlobalConfig.isPackageToBeIgnoredInRuntime(p, p.getFromBundle(), null)) {
                    System.out.println("* Skipping package " + p + " globally, as requested by config.");
                    continue;
                }

                String pname = p.getName();
                //if it's in the packageToFeature map, then the package is exported as api/spi.
                if (packageToFeatureMap.containsKey(p)) {
                    Set<FeatureInfo> features = packageToFeatureMap.get(p);
                    //just a sanity check, the maps should all be consistent, if not
                    //then the code has changed since it was written.
                    //If a package is in the 'packageToFeature' map, then the set
                    //of features for that package should NOT be empty.
                    if (features.isEmpty()) {
                        System.out.println("ERROR: missing features for package " + pname);
                        XmlErrorCollator.addProcessingIssue(ReportType.ERROR, "Internal map is inconsistent, package " + p
                                                                              + " exists in package-to-feature map, but has no known features.");
                    }

                    //there can be multiple features for any given package, as nothing
                    //prevents multiple features exporting the same package as api/spi
                    boolean isApiOrSpi = false;
                    boolean wasSkipped = false;
                    Set<String> types = new HashSet<String>();
                    String apiOrSpi = "";
                    Set<String> featuresInvolved = new HashSet<String>();
                    for (FeatureInfo f : features) {

                        //is this a package to skip for this feature?
                        if (GlobalConfig.isPackageToBeIgnoredInRuntime(p, p.getFromBundle() + "@" + p.getFromBundleVersion(), f.getName())
                            || GlobalConfig.isPackageToBeIgnoredInRuntime(p, p.getFromBundle(), f.getName())) {
                            System.out.println("* Skipping package " + p + " for feature " + f.getName() + " as configured by config.");
                            wasSkipped = true;
                            continue;
                        }

                        XmlErrorCollator.setCurrentFeature(f.getName());
                        XmlErrorCollator.setNestedFeature(f.getName());
                        PkgInfo nullVersioned = new PkgInfo(p.getName(), null, p.getFromBundle(), p.getFromBundleVersion(), null);
                        if (f.getLocalAPI().containsKey(p)) {
                            isApiOrSpi = true;

                            PkgInfo apiDeclKey = nullVersioned;
                            //basic assumption here that there will only be one version of any given package declared as api/spi for any given feature.
                            for (PkgInfo z : f.getLocalAPI().keySet()) {
                                if (z.getName() == apiDeclKey.getName()) {
                                    apiDeclKey = z;
                                }
                            }

                            Set<Map<String, String>> pattribs = f.getLocalAPI().get(p);
                            for (Map<String, String> attribmap : pattribs) {
                                if (attribmap.containsKey("type")) {
                                    types.add(attribmap.get("type"));
                                }
                            }

                            if (!apiOrSpi.contains("api")) {
                                if (apiOrSpi.isEmpty()) {
                                    apiOrSpi = "api";
                                } else {
                                    apiOrSpi = apiOrSpi + ",api";
                                }
                            }

                            //remove this one as seen from the set.
                            featureAPI.get(f).remove(p);
                            featureAPI.get(f).remove(nullVersioned);

                            if (!featureAPISeen.containsKey(f)) {
                                featureAPISeen.put(f, new HashMap<PkgInfo, Set<PkgInfo>>());
                            }
                            if (!featureAPISeen.get(f).containsKey(apiDeclKey)) {
                                featureAPISeen.get(f).put(apiDeclKey, new HashSet<PkgInfo>());
                            }
                            featureAPISeen.get(f).get(apiDeclKey).add(p);

                            featuresInvolved.add(f.getName());
                        }
                        if (f.getLocalSPI().containsKey(p)) {
                            isApiOrSpi = true;

                            PkgInfo spiDeclKey = nullVersioned;
                            //basic assumption here that there will only be one version of any given package declared as api/spi for any given feature.
                            for (PkgInfo z : f.getLocalAPI().keySet()) {
                                if (z.getName() == spiDeclKey.getName()) {
                                    spiDeclKey = z;
                                }
                            }

                            //remove this one as seen from the set.
                            featureSPI.get(f).remove(p);
                            featureSPI.get(f).remove(nullVersioned);

                            if (!featureSPISeen.containsKey(f)) {
                                featureSPISeen.put(f, new HashMap<PkgInfo, Set<PkgInfo>>());
                            }
                            if (!featureSPISeen.get(f).containsKey(spiDeclKey)) {
                                featureSPISeen.get(f).put(spiDeclKey, new HashSet<PkgInfo>());
                            }
                            featureSPISeen.get(f).get(spiDeclKey).add(p);
                            if (featureSPISeen.get(f).get(spiDeclKey).size() > 1) {
                                XmlErrorCollator.addReport(f.getName(),
                                                           bundle.getSymbolicName() + "@" + bundle.getVersion(),
                                                           XmlErrorCollator.ReportType.ERROR,
                                                           //summary
                                                           "Multiple Versions of SPI Package " + spiDeclKey,
                                                           //shortText
                                                           "[MULTIPLE_SPI_PACKAGE_VERSION " + spiDeclKey + "]",
                                                           //reason
                                                           "Feature " + f.getName() + " has an SPI declaration of " + spiDeclKey + " that matches multiple versions "
                                                                                                                + featureSPISeen.get(f).get(spiDeclKey) + " in the runtime.");
                            }

                            Set<Map<String, String>> pattribs = f.getLocalSPI().get(p);
                            for (Map<String, String> attribmap : pattribs) {
                                if (attribmap.containsKey("type")) {
                                    types.add(attribmap.get("type"));
                                }
                            }

                            if (!apiOrSpi.contains("spi")) {
                                if (apiOrSpi.isEmpty()) {
                                    apiOrSpi = "spi";
                                } else {
                                    apiOrSpi = apiOrSpi + ",spi";
                                }
                            }

                            featuresInvolved.add(f.getName());
                        }
                    }
                    //if we confirmed the package as api/spi, lets process it.
                    if (isApiOrSpi) {
                        apispiPkgs.add(p);

                        XmlErrorCollator.setCurrentFeature(featuresInvolved);
                        XmlErrorCollator.setNestedFeature((String) null);

                        processPackage(packageToBundle, packageToXml,
                                       pkgToPkgContent, bundle, p, features, types, apiOrSpi);

                    } else {
                        //we know this package is a match for an api/spi declaration, yet we have ended up here,
                        //if we skipped the package because config told us to, this is not an issue, but otherwise that
                        //means somehow the package is in the package-to-feature map, but none of the features claimed the package.
                        if (!wasSkipped) {
                            XmlErrorCollator.addProcessingIssue(ReportType.ERROR,
                                                                "Internal map is inconsistent, package "
                                                                                  + p
                                                                                  + " exists in package-to-feature map, but none of the features in its value set declared it as api or spi.");
                        }
                    }
                } else {
                    //skip this package, as we do not know it as api spi.
                }
            }
            //remember the api/spi packages for this bundle.
            if (!apispiPkgs.isEmpty()) {
                bundleToPkgs.put(new VersionedEntity(bundle.getSymbolicName(), bundle.getVersion().toString()), apispiPkgs);
            }
        }
        //check if any seen api mapped to multiple candidates..
        for (Map.Entry<FeatureInfo, Map<PkgInfo, Set<PkgInfo>>> e : featureAPISeen.entrySet()) {
            FeatureInfo f = e.getKey();
            Map<PkgInfo, Set<PkgInfo>> declToSeen = e.getValue();
            XmlErrorCollator.setCurrentFeature(f.getName());
            XmlErrorCollator.setNestedFeature(f.getName());
            for (Map.Entry<PkgInfo, Set<PkgInfo>> seenPackage : declToSeen.entrySet()) {
                if (seenPackage.getValue().size() > 1) {
                    PkgInfo apiDeclKey = seenPackage.getKey();
                    StringBuffer fromInfo = new StringBuffer();
                    for (PkgInfo p : seenPackage.getValue()) {
                        if (fromInfo.length() != 0) {
                            fromInfo.append(", ");
                        }
                        fromInfo.append("{Package:");
                        fromInfo.append(p.toString());
                        fromInfo.append(" fromBundle ");
                        fromInfo.append(p.getFromBundle());
                        fromInfo.append("@");
                        fromInfo.append(p.getFromBundleVersion());
                        if (p.getOwner() != null) {
                            BundleWiring bw = p.getOwner();
                            Bundle b = bw.getBundle();
                            Set<FeatureInfo> declaredBy = featureBundleRepository.getFeaturesForBundle(b.getSymbolicName(), b.getVersion());
                            Set<String> names = new HashSet<String>();
                            for (FeatureInfo declarer : declaredBy) {
                                names.add(declarer.getName());
                            }
                            fromInfo.append(" declaredBy: ");
                            fromInfo.append(names);
                        }
                    }

                    if (featureAPISeen.get(f).get(apiDeclKey).size() > 1) {
                        XmlErrorCollator.addReport(
                                                   f.getName(),
                                                   f.getName(),
                                                   XmlErrorCollator.ReportType.ERROR,
                                                   //summary
                                                   "Multiple Versions of API Package " + apiDeclKey,
                                                   //shortText
                                                   "[MULTIPLE_API_PACKAGE_VERSION " + apiDeclKey + "]",
                                                   //reason
                                                   "Feature " + f.getName() + " has an API declaration of " + apiDeclKey + " that matches multiple versions "
                                                                                                        + seenPackage.getValue() + " in the runtime. Packages came from ["
                                                                                                        + fromInfo + "]");
                    }
                }
            }
        }
        //same again for spi..
        for (Map.Entry<FeatureInfo, Map<PkgInfo, Set<PkgInfo>>> e : featureSPISeen.entrySet()) {
            FeatureInfo f = e.getKey();
            Map<PkgInfo, Set<PkgInfo>> declToSeen = e.getValue();
            XmlErrorCollator.setCurrentFeature(f.getName());
            XmlErrorCollator.setNestedFeature(f.getName());
            for (Map.Entry<PkgInfo, Set<PkgInfo>> seenPackage : declToSeen.entrySet()) {
                if (seenPackage.getValue().size() > 1) {
                    PkgInfo spiDeclKey = seenPackage.getKey();
                    StringBuffer fromInfo = new StringBuffer();
                    for (PkgInfo p : seenPackage.getValue()) {
                        if (fromInfo.length() != 0) {
                            fromInfo.append(", ");
                        }
                        fromInfo.append("{Package:");
                        fromInfo.append(p.toString());
                        fromInfo.append(" fromBundle ");
                        fromInfo.append(p.getFromBundle());
                        fromInfo.append("@");
                        fromInfo.append(p.getFromBundleVersion());
                        if (p.getOwner() != null) {
                            BundleWiring bw = p.getOwner();
                            Bundle b = bw.getBundle();
                            Set<FeatureInfo> declaredBy = featureBundleRepository.getFeaturesForBundle(b.getSymbolicName(), b.getVersion());
                            Set<String> names = new HashSet<String>();
                            for (FeatureInfo declarer : declaredBy) {
                                names.add(declarer.getName());
                            }
                            fromInfo.append(" declaredBy: ");
                            fromInfo.append(names);
                        }
                    }

                    XmlErrorCollator.addReport(
                                               f.getName(),
                                               f.getName(),
                                               XmlErrorCollator.ReportType.ERROR,
                                               //summary
                                               "Multiple Versions of SPI Package " + spiDeclKey,
                                               //shortText
                                               "[MULTIPLE_SPI_PACKAGE_VERSION " + spiDeclKey + "]",
                                               //reason
                                               "Feature " + f.getName() + " has an SPI declaration of " + spiDeclKey + " that matches multiple versions " + seenPackage.getValue()
                                                                                                    + " in the runtime. Packages came from [" + fromInfo + "]");
                }
            }
        }
        System.out.println("INFO: Package scan complete.");

        //that's all the packages processed.. lets see if we failed to find any ?
        reportMissingPackages(featureAPI, "ibm-api");
        reportMissingPackages(featureSPI, "ibm-spi");

        //now all the data is in the package map..
        //write it out to the framework baseline xml file via the writer.
        pw.println(" <packages>");
        for (Map.Entry<PkgInfo, String> e : packageToXml.entrySet()) {
            pw.print(e.getValue());
        }
        pw.println(" </packages>");
    }

    /**
     * Processes a Package identified as api or spi.
     *
     * Uses the packageToBundleMap and packageToXmlMap to maintain state across package compares.
     * If packages are already known to the maps, then they are evaluated to ensure they are binary compatible.
     *
     * Each package not met before is processed into xml for storage in the baseline being created,
     * and into a PackageContent instance that can be used for comparison later.
     * (The xml is sufficient to recreate the PackageContent instance)
     *
     * @param packageToBundleMap map of packages to bundles that we have seen during this run of package processing
     * @param packageToXmlMap map of packages to package-xml that we are building during this run of package processing
     * @param pkgToPkgContent map of packages to package-content that we are building during this run of package processing
     * @param bundle the bundle this particular package came from
     * @param p the package we are processing
     * @param features the features that declared this package as api/spi
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private void processPackage(Map<PkgInfo, Set<Bundle>> packageToBundleMap,
                                Map<PkgInfo, String> packageToXmlMap,
                                VersionedEntityMap<PkgInfo, PackageContent> pkgToPkgContent,
                                Bundle bundle, PkgInfo p,
                                Set<FeatureInfo> features,
                                Set<String> types,
                                String apiOrSpi) throws IOException, SAXException, ParserConfigurationException {

        //have we ever seen this package before?
        if (!packageToBundleMap.containsKey(p)) {
            //no? create a new set to hold bundles exporting the package.
            packageToBundleMap.put(p, new HashSet<Bundle>());
        }
        //add this bundle to the set for this package.
        packageToBundleMap.get(p).add(bundle);

        //if there are more than 1 in the set, this isn't the first time
        //we've seen this package, which means another bundle we've seen
        //exported it already.
        if (packageToBundleMap.get(p).size() > 1) {
            //we already met this package.. if it doesn't match the others in the set
            //it's a split package.. we need to warn about these.
            //(we only compare this package, against the first in the set, in theory there
            // could be N subsets of split packages, but we expect there to be none, so
            // do not write the code to handle the subset identification ;p)
            String newlyEncounteredPackageXml = obtainXmlForPackage(bundle, p, false);
            String alreadyEncounteredPackageXml = packageToXmlMap.get(p);

            PackageContent newlyEncounteredPackageContent = XMLBaseLineUtils.processSinglePackageXml(newlyEncounteredPackageXml);
            PackageContent alreadyEncounteredPackageContent = XMLBaseLineUtils.processSinglePackageXml(alreadyEncounteredPackageXml);

            System.out.println("    Already Known: " + alreadyEncounteredPackageContent.getClasses().size() + " classes");
            System.out.println("Newly Encountered: " + newlyEncounteredPackageContent.getClasses().size() + " classes");

            //compare the 2 package contents, to check if the packages are compatible.
            //if they are compatible, it's just a warning, that the package is present in the runtime twice from
            //different places. If they are incompatible, it's an error, as we have a package exposed as api/spi
            //with content that alters depending on which bundle supplies it, this makes the api/spi inconsistent.
            PackageComparator pc = new PackageComparator();
            List<VersionResult> errors = pc.compareSplitDupePackage(newlyEncounteredPackageContent, alreadyEncounteredPackageContent, types, types, features, apiOrSpi);
            ReportType level = ReportType.WARNING;
            if (errors != null) {
                level = ReportType.ERROR;
            }
            //the packages didn't match.. this is hopefully just a split package
            //although it could be 2 bundles trying to define the same package with different content
            //at the same version..
            System.out.println(level.toString() + ": Split/mismatched package " + p + " in " + packageToBundleMap.get(p));

            //need to report this against each feature that had this as api/spi..
            for (FeatureInfo topDeclarer : features) {
                Set<String> actualDeclarerAPI = topDeclarer.getAggregateAPIDeclarers().get(p);
                Set<String> actualDeclarerSPI = topDeclarer.getAggregateAPIDeclarers().get(p);
                XmlErrorCollator.setCurrentFeature(topDeclarer.getName());

                String causes = "";
                if (errors != null) {
                    causes += "\nPackage differences : \n";
                    for (VersionResult v : errors) {
                        if (v.majorChange.isChange())
                            causes += "Major differences:\n" + v.majorChange.getReason() + "\n";
                        if (v.minorChange.isChange())
                            causes += "Minor differences:\n" + v.minorChange.getReason() + "\n";
                    }
                }

                if (actualDeclarerAPI != null && !actualDeclarerAPI.isEmpty()) {
                    XmlErrorCollator.setNestedFeature(actualDeclarerAPI);
                    XmlErrorCollator.addReport("Package: " + p,
                                               packageToBundleMap.get(p).toString(),
                                               ReportType.ERROR,
                                               //summary
                                               "Split or Incompatible API Package " + p,
                                               //shortText
                                               "[SPLIT_OR_INCOMPATIBLE_API_PACKAGE " + p + "]",
                                               //reason
                                               "When evaluating new build ibm-api packages, checker found a split or mismatched package " + p + " exported from bundles "
                                                                                                + packageToBundleMap.get(p) + causes);

                }
                if (actualDeclarerSPI != null && !actualDeclarerSPI.isEmpty()) {
                    XmlErrorCollator.setNestedFeature(actualDeclarerSPI);
                    XmlErrorCollator.addReport("Package: " + p,
                                               packageToBundleMap.get(p).toString(),
                                               ReportType.ERROR,
                                               //summary
                                               "Split or Incompatible SPI Package " + p,
                                               //shortText
                                               "[SPLIT_OR_INCOMPATIBLE_SPI_PACKAGE " + p + "]",
                                               //reason
                                               "When evaluating new build ibm-spi packages, checker found a split or mismatched package " + p + " exported from bundles "
                                                                                                + packageToBundleMap.get(p) + causes);
                }
            }
        }

        //having dealt with & warned about split packages, we assume now
        //that there'll only be one version of any given versioned-package, and
        //store the xml for that version.
        if (!packageToXmlMap.containsKey(p)) {
            //the xml for all classes in the package, even those not contained locally to this bundle
            String allXml = obtainXmlForPackage(bundle, p, false);
            //the xml for all classes in the package, limited to only those local to the bundle.
            String inUseXml = obtainXmlForPackage(bundle, p, true);

            PackageContent allPkgContent = XMLBaseLineUtils.processSinglePackageXml(allXml);
            PackageContent inUsePkgContent = XMLBaseLineUtils.processSinglePackageXml(inUseXml);

            //we compare the two versions of this package seen
            // - the one from 'just' this bundle
            // - the one with content from just this bundle, and from imported packages.
            //if they differ, then this bundle is actively using content from outside itself to provide its exported packages
            //this means the shape of the package exported from this bundle may change if the provider of such a package alters
            //thus we want to warn when this scenario has occurred.
            PackageComparator pc = new PackageComparator();
            List<VersionResult> errors;
            errors = pc.compareSplitDupePackage(allPkgContent, inUsePkgContent, types, types, features, apiOrSpi);
            if (errors != null) {
                //this can occur if the bundle is partly using content from another bundle, and that other content doesnt match
                //the content this bundle would be using from itself. (eg, bundle is importing a package it exports as api/spi)
                //
                //This situation is important, as it means the shape of the api/spi varies depending on the bundles in play.
                System.out.println("WARNING: package " + p + " from bundle (" + bundle.getSymbolicName() + "@" + bundle.getVersion()
                                   + " has a mismatch of exported & in-use content.");
                String causes = "";

                causes += "\nPackage differences : \n";
                for (VersionResult v : errors) {
                    if (v.majorChange.isChange())
                        causes += "Major differences:\n" + v.majorChange.getReason() + "\n";
                    if (v.minorChange.isChange())
                        causes += "Minor differences:\n" + v.minorChange.getReason() + "\n";
                }

                //need to report this against each feature that had this as api/spi..
                for (FeatureInfo topDeclarer : features) {
                    Set<String> actualDeclarerAPI = topDeclarer.getAggregateAPIDeclarers().get(p);
                    Set<String> actualDeclarerSPI = topDeclarer.getAggregateAPIDeclarers().get(p);
                    XmlErrorCollator.setCurrentFeature(topDeclarer.getName());

                    //TODO: add content of errors to the output.

                    if (actualDeclarerAPI != null && !actualDeclarerAPI.isEmpty()) {
                        XmlErrorCollator.setNestedFeature(actualDeclarerAPI);
                        XmlErrorCollator.addReport("Package: " + p,
                                                   bundle.getSymbolicName() + "@" + bundle.getVersion(),
                                                   ReportType.WARNING,
                                                   //summary
                                                   "Import/Export API Package Difference " + p,
                                                   //shortText
                                                   "[IMPORT_EXPORT_API_PACKAGE_CONFLICT " + p + "]",
                                                   //reason
                                                   "When evaluating new build ibm-api packages, checker found a package "
                                                                                                     + p
                                                                                                     + " with a difference between exported and in-use content. This can happen if a bundle imports an exports its own packages, and is using content from a different bundle at runtime instead of its own, and that different content is incompatible with its own. The package is ibm-api and exported by bundle "
                                                                                                     + bundle.getSymbolicName() + "@" + bundle.getVersion() + causes);
                    }
                    if (actualDeclarerSPI != null && !actualDeclarerSPI.isEmpty()) {
                        XmlErrorCollator.setNestedFeature(actualDeclarerSPI);
                        XmlErrorCollator.addReport("Package: " + p,
                                                   bundle.getSymbolicName() + "@" + bundle.getVersion(),
                                                   ReportType.WARNING,
                                                   //summary
                                                   "Import/Export SPI Package Difference " + p,
                                                   //shortText
                                                   "[IMPORT_EXPORT_SPI_PACKAGE_CONFLICT " + p + "]",
                                                   //reason
                                                   "When evaluating new build ibm-spi packages, checker found a package "
                                                                                                     + p
                                                                                                     + " with a difference between exported and in-use content. This can happen if a bundle imports an exports its own packages, and is using content from a different bundle at runtime instead of its own, and that different content is incompatible with its own. The package is ibm-spi and exported by bundle "
                                                                                                     + bundle.getSymbolicName() + "@" + bundle.getVersion() + causes);
                    }
                }

                inUseXml = allXml;
                inUsePkgContent = allPkgContent;
            }

            //warn if we've met a package declared as api/spi, that has no classes, and no xsd's.
            //if the package is empty, it may be an error, and we need to report it, if the package is
            //not empty, and contains non class/xsd content, then we dont api enforce that.
            if (allPkgContent.getClasses().size() == 0 && allPkgContent.getXsds().size() == 0) {
                Collection<String> content;
                if (bundle.getBundleId() > 0) {
                    int flags = 0;
                    content = p.getOwner().listResources("/" + p.getName().replace('.', '/'), "*", flags);
                } else {
                    content = new ArrayList<String>();
                    collectResourcesFromBundleZero(bundle, p, content, "");
                }
                if (content.size() > 0) {
                    //if size is >0 then the package does have some content, we just don't care about it from an api checker perspective.
                    //System.out.println("INFO: Package "+p.getName()+" is offered by bundle "+bundle.getSymbolicName()+"@"+bundle.getVersion()+" ["+bundle.getBundleId()+"] but has no api checked content to declare. ");
                } else {
                    //if size is 0 we should warn, as it's likely a build error, or metadata error, very odd to expose empty packages.
                    System.out.println("WARNING: Package " + p.getName() + " is offered by bundle " + bundle.getSymbolicName() + "@" + bundle.getVersion() + " ["
                                       + bundle.getBundleId() + "] but has no content to declare. "
                                       + (bundle.getBundleId() == 0 ? "(Missing SPI/API attribute tag for system bundle content??)" : ""));
                    //need to report this against each feature that had this as api/spi..
                    for (FeatureInfo topDeclarer : features) {
                        Set<String> actualDeclarerAPI = topDeclarer.getAggregateAPIDeclarers().get(p);
                        Set<String> actualDeclarerSPI = topDeclarer.getAggregateAPIDeclarers().get(p);
                        XmlErrorCollator.setCurrentFeature(topDeclarer.getName());
                        if (actualDeclarerAPI != null && !actualDeclarerAPI.isEmpty()) {
                            XmlErrorCollator.setNestedFeature(actualDeclarerAPI);
                            XmlErrorCollator.addReport("Package: " + p,
                                                       bundle.getSymbolicName() + "@" + bundle.getVersion(),
                                                       ReportType.WARNING,
                                                       //summary
                                                       "Missing API Package Content for " + p,
                                                       //shortText
                                                       "[EMPTY_API_PACKAGE " + p + "]",
                                                       //reason
                                                       "When evaluating new build ibm-api packages, checker found a package "
                                                                                        + p
                                                                                        + " exported as ibm-api, but found to have no content at runtime, this may be an error."
                                                                                        + (bundle.getBundleId() == 0 ? "(Note that for System Bundle exports, there may be a missing SPI/API attribute tag on the declaring feature manifest??)" : ""));
                        }
                        if (actualDeclarerSPI != null && !actualDeclarerSPI.isEmpty()) {
                            XmlErrorCollator.setNestedFeature(actualDeclarerSPI);
                            XmlErrorCollator.addReport("Package: " + p,
                                                       bundle.getSymbolicName() + "@" + bundle.getVersion(),
                                                       ReportType.WARNING,
                                                       //summary
                                                       "Missing SPI Package Content for " + p,
                                                       //shortText
                                                       "[EMPTY_SPI_PACKAGE " + p + "]",
                                                       //reason
                                                       "When evaluating new build ibm-spi packages, checker found a package "
                                                                                        + p
                                                                                        + " exported as ibm-spi, but found to have no content at runtime, this may be an error."
                                                                                        + (bundle.getBundleId() == 0 ? "(Note that for System Bundle exports, there may be a missing SPI/API attribute tag on the declaring feature manifest??)" : ""));
                        }
                    }
                }
            }

            //remember this package for future invocations thus run.
            packageToXmlMap.put(p, inUseXml);
            pkgToPkgContent.merge(p, inUsePkgContent);
        }
    }

    /**
     * Given a package p, from Bundle bundle, generate the XML that will represent the Package in the framework.
     *
     * Resources for the package will be iterated from the Bundle passed.
     *
     * The uselocalonly flag, dictates if the resources for the package should be limited to those within the bundle
     * or also allow for resources imported from other bundles (then exported, since we only investigated exported pkgs)
     *
     * @param bundle
     * @param p
     * @param useLocalOnly
     * @return
     * @throws IOException
     */
    private String obtainXmlForPackage(Bundle bundle, PkgInfo p, boolean useLocalOnly) throws IOException {
        BundleWiring bw = bundle.adapt(BundleWiring.class);

        if (bw == null) {
            throw new RuntimeException("Error unable to obtain xml for package " + p + " from bundle " + bundle.getSymbolicName() + "@" + bundle.getVersion()
                                       + " because adapt BundleWiring returned null. BundleState is " + bundle.getState());
        }

        Collection<String> classes = null;
        Collection<String> xsds = null;

        //in pre 3.10 frameworks, listResources API is non functional against bundle zero
        //thankfully we can still reason over the content using findEntries, as bundle zero is
        //guaranteed to have a flat class space from the root (Bundle-ClassPath of '.' only)
        //in 3.10 and later frameworks, we can still use findEntries, without it being incorrect.
        if (bundle.getBundleId() > 0) {
            int flags = 0;
            if (useLocalOnly)
                flags |= BundleWiring.LISTRESOURCES_LOCAL;
            if (p.getOwner() != null) {
                classes = p.getOwner().listResources("/" + p.getName().replace('.', '/'), "*.class", flags);
                xsds = p.getOwner().listResources("/" + p.getName().replace('.', '/'), "*.xsd", flags);
            } else {
                classes = new ArrayList<String>();
                xsds = new ArrayList<String>();
                collectResourcesFromBundleZero(bundle, p, classes, ".class");
                collectResourcesFromBundleZero(bundle, p, xsds, ".xsd");
            }
        } else {
            classes = new ArrayList<String>();
            xsds = new ArrayList<String>();
            collectResourcesFromBundleZero(bundle, p, classes, ".class");
            collectResourcesFromBundleZero(bundle, p, xsds, ".xsd");
        }

        //now classes & xsds hold a collection of paths within the bundle that we need to process for this package.

        StringBuffer xml = new StringBuffer();
        xml.append(" <package symbolicName=\"" + p.getName() + "\" version=\"" + p.getVersion() + "\" fromBundle=\"" + bundle.getSymbolicName() + "\" fromBundleVersion=\""
                   + bundle.getVersion() + "\">\n");
        if (classes != null) {
            //the bundle 0 classloader is also incompatible with 'ClassLoader'
            //thankfully since we run as a bundle, (and we know we don't override
            //any bundle 0 packages..) our bundle's classloader can
            //stand in for bundle 0's.
            ClassLoader loader;
            if (bundle.getBundleId() == 0) {
                loader = this.getClass().getClassLoader();
            } else {
                loader = bw.getClassLoader();
            }

            //for each class we're going to process the inputstream of its bytes, using the classloader
            //configured above. (Either the bundles own loader, or our loader for bundle 0)
            for (String cls : classes) {

                //Visit the class using the semanticversioningclassvisitor from the original
                //apichecker code.
                ClassVisitor cw = new EmptyClassVisitor();
                SerialVersionClassVisitor sv = new SerialVersionClassVisitor(cw);
                SemanticVersioningClassVisitor cv = new SemanticVersioningClassVisitor(loader, sv);
                InputStream is = null;
                try {
                    URL entry = bundle.getResource(cls);
                    if (entry == null) {
                        if (bundle.getBundleId() == 0) {
                            entry = bundle.getEntry(cls);
                            if (entry == null) {
                                XmlErrorCollator.addProcessingIssue(ReportType.ERROR, "UNABLE TO OBTAIN entry URL for " + cls + " from system bundle " + bundle.getSymbolicName()
                                                                                      + " " + bundle.getBundleId());
                                System.out.println("ERROR: UNABLE TO OBTAIN entry URL for " + cls + " from bundle " + bundle.getSymbolicName() + " " + bundle.getBundleId());
                            }
                        } else {
                            XmlErrorCollator.addProcessingIssue(ReportType.ERROR, "UNABLE TO OBTAIN entry URL for " + cls + " from bundle " + bundle.getSymbolicName() + " "
                                                                                  + bundle.getBundleId());
                            System.out.println("ERROR: UNABLE TO OBTAIN entry URL for " + cls + " from bundle " + bundle.getSymbolicName() + " " + bundle.getBundleId());
                        }
                    } else {
                        is = entry.openStream();
                        ClassReader cr = new ClassReader(is);
                        cr.accept(cv, 0);
                        //the decl can be null if the visit determined the class to be private
                        if (cv.getClassDeclaration() != null) {
                            //add the xml for this class to the package.
                            //System.out.println(cv.getClassDeclaration().toXML());
                            xml.append(cv.getClassDeclaration().toXML());
                        }
                    }
                } catch (IOException ioe) {
                    XmlErrorCollator.addProcessingIssue(ReportType.ERROR, "The file " + cls + " in bundle [" + bundle.getBundleId() + "] " + bundle.getSymbolicName()
                                                                          + " cannot be opened to be read to create fingerprint to use for comparison.");
                    System.err.println("ERROR: The file " + cls + " in bundle [" + bundle.getBundleId() + "] " + bundle.getSymbolicName() + " cannot be opened.");
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        }
        //for xsds we store a hash to compare later.
        if (xsds != null) {
            for (String xsd : xsds) {
                String hash = getHashForXSD(xsd, p);
                xml.append("  <xsd path=\"" + xsd + "\" hash=\"" + hash + "\"/>\n");
            }
        }
        xml.append(" </package>\n");

        return xml.toString();
    }

    /**
     * Populates a set of paths present within Bundle 0 with the requested extension.
     *
     * @param bundle reference to bundle zero.
     * @param p package to look in for resources
     * @param classes set to populate
     * @param extension .class .xsd etc
     */
    private void collectResourcesFromBundleZero(Bundle bundle,
                                                PkgInfo p, Collection<String> classes, String extension) {
        String path = "/" + p.getName().replace('.', '/');
        String filter = "*" + extension;
        //System.out.println("Looking for path '"+path+"' filter '"+filter+"' on bundle id "+bundle.getBundleId());
        Enumeration<URL> urls = bundle.findEntries(path, filter, false);
        if (urls != null) {
            while (urls.hasMoreElements()) {
                URL u = urls.nextElement();
                //getPath urls start with '/' .. listResource paths do not, so trim here.
                classes.add(u.getPath().substring(1));
            }
        }
    }

    /**
     * Given a path to an xsd, and a package from a bundle, return a hash we can use to
     * check if the xsd has been modified.
     *
     * A hash will allow for some false negatives, but the risk is low.
     *
     * @param xsdResourcePath
     * @param p
     * @return
     * @throws IOException
     */
    private String getHashForXSD(String xsdResourcePath, PkgInfo p) throws IOException {
        Bundle b = p.getOwner().getBundle();
        URL xsdURL = null;
        if (b.getBundleId() > 0) {
            xsdURL = p.getOwner().getBundle().getResource(xsdResourcePath);
        } else {
            xsdURL = this.getClass().getClassLoader().getResource(xsdResourcePath);
        }

        if (xsdURL == null) {
            throw new IllegalStateException("Could not get url for xsd resource " + xsdResourcePath);
        }

        String xsdContent = readXsdFile(xsdURL.openStream());

        //for now, we'll use the hash as string content hashcode + ## + length of content
        //if we need more uniqueness, we could move to sha1

        return xsdContent.hashCode() + "##" + xsdContent.length();
    }

    /**
     * Reads an entire xsd file and returns it as a string.
     *
     * @param is
     * @return
     */
    private String readXsdFile(InputStream is) {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException ioe) {
            IOUtils.close(br);
        }
        return sb.toString();
    }

    /**
     * Checks the passed map, looking for any packages still present in the value set, any present were not found
     * during the scan of all bundle exported packages, and need to be reported as missing.
     *
     * Will only report packages declared as ibm-api or ibm-spi, thirdparty/other are not considered in scope for
     * reporting via the apichecker.
     *
     * @param featureAPISPI The set of missing packages to report
     * @param type ibm-api or ibm-spi .. used to know to check api or spi sets for attributes
     */
    private void reportMissingPackages(
                                       Map<FeatureInfo, Set<PkgInfo>> featureAPISPI,
                                       String type) {
        for (Map.Entry<FeatureInfo, Set<PkgInfo>> entry : featureAPISPI.entrySet()) {

            XmlErrorCollator.setCurrentFeature(entry.getKey().getName());
            XmlErrorCollator.setNestedFeature((String) null);

            //System.out.println("INFO: Checking type "+type+" for feature "+entry.getKey().getName()+" with set "+entry.getValue());
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                for (PkgInfo p : entry.getValue()) {
                    Set<Map<String, String>> attribs = null;
                    boolean doWeCare = false;

                    //we want to lower the serverity for thirdparty,spec. and warn for api/spi.
                    if ("ibm-api".equals(type)) {
                        attribs = entry.getKey().getLocalAPI().get(p);
                    } else if ("ibm-spi".equals(type)) {
                        //attribs = entry.getKey().getLocalSPI().get(p);
                        //spi doesn't use the type attrib, so we can start caring now.
                        doWeCare = true;
                    } else {
                        throw new IllegalArgumentException(type);
                    }
                    //System.out.println("INFO: looked up pkg "+p+" in feature "+entry.getKey().getName()+" and got back attribs of "+attribs);

                    //if it wasn't spi.. check if the type set includes 'ibm-api'
                    Set<String> types = new HashSet<String>();
                    if (!doWeCare) {
                        if (attribs != null) {
                            for (Map<String, String> m : attribs) {
                                if (m != null) {
                                    types.add(m.get("type"));
                                    if (type.equals(m.get("type"))) {
                                        doWeCare = true;
                                    }
                                }
                            }
                        }
                    }

                    //now issue message with appropriate caveats.
                    if (doWeCare) {
                        XmlErrorCollator.addReport("Feature: " + entry.getKey().getName(),
                                                   null,
                                                   ReportType.WARNING,
                                                   //summary
                                                   "Missing API/SPI Package " + p,
                                                   //shortText
                                                   "[MISSING_APISPI_PACKAGE " + p + "]",
                                                   //reason
                                                   "feature " + entry.getKey().getName() + " declared " + p + " as " + type
                                                                                         + " but the package was not seen exported by any bundle, package was declared as " + type
                                                                                         + " with types of "
                                                                                         + types);

                        System.out.println("WARNING: feature " + entry.getKey().getName() + " declared package " + p + " as " + type
                                           + " but the package was not seen exported by any bundle, package was declared as " + type + " with types of " + types);
                    } else {
                        XmlErrorCollator.addReport("Feature: " + entry.getKey().getName(),
                                                   null,
                                                   ReportType.INFO,
                                                   //summary
                                                   "Missing ThirdParty/Spec/Internal Package " + p,
                                                   //shortText
                                                   "[MISSING_3RDPARTYSPECINTERNAL_PACKAGE " + p + "]",
                                                   //reason
                                                   "feature " + entry.getKey().getName() + " declared " + p + " as " + type
                                                                                                       + " but the package was not seen exported by any bundle, package was declared as "
                                                                                                       + type + " with types of "
                                                                                                       + types
                                                                                                       + " (This message is informational, because the package is not declared to be ibm-api/ibm-spi)");
                        System.out.println("INFO: feature " + entry.getKey().getName() + " declared package " + p + " as " + type
                                           + " but the package was not seen exported by any bundle, package was declared as " + type + " with types of " + types
                                           + " (This message is informational, because the package is not declared to be ibm-api/ibm-spi)");
                    }
                }
            }
        }
    }

    /**
     * Serialize the known features as xml.
     *
     * @param pw
     * @param featureNameToFeatureInfo
     */
    private void processFeatures(PrintWriter pw, Map<String, FeatureInfo> featureNameToFeatureInfo) {
        Set<FeatureInfo> featureInfos;
        featureInfos = new TreeSet<FeatureInfo>();
        for (FeatureInfo f : featureNameToFeatureInfo.values()) {
            featureInfos.add(f);
        }
        pw.println(" <features>");
        for (FeatureInfo f : featureInfos) {
            f.toXML(pw, featureNameToFeatureInfo);
        }
        pw.println(" </features>");
    }

    /** Build a report for the api/spi review for this new feature. */
    private void generateNewFeatureApiSpiReviewDetail(FrameworkInfo newBaseLine, FeatureInfo feature) {
        StringBuffer detailReport = new StringBuffer();
        detailReport.append("Feature " + feature.getName() + " is new to this configuration.\n");
        Map<PkgInfo, Set<Map<String, String>>> aggapi = feature.getAggregateAPI();
        Map<PkgInfo, Set<String>> aggapifrom = feature.getAggregateAPIDeclarers();
        Map<PkgInfo, Set<Map<String, String>>> aggspi = feature.getAggregateSPI();
        Map<PkgInfo, Set<String>> aggspifrom = feature.getAggregateSPIDeclarers();

        if (aggapi != null && aggapi.size() > 0) {
            StringBuffer apiReport = new StringBuffer();
            HashSet<String> kernelFeatures = new HashSet<String>();
            kernelFeatures.addAll(Arrays.asList(KERNEL_FEATURES));

            apiReport.append(" API?: yes.\n");
            boolean kernelOnly = true;
            for (Map.Entry<PkgInfo, Set<Map<String, String>>> apipkg : aggapi.entrySet()) {
                boolean kernelPkg = true;
                for (String declarer : aggapifrom.get(apipkg.getKey())) {
                    if (!kernelFeatures.contains(declarer)) {
                        kernelOnly = false;
                        kernelPkg = false;
                    }
                }

                if (!kernelPkg) {
                    apiReport.append("  - " + apipkg.getKey() + " " + apipkg.getValue() + "\n");
                    apiReport.append("    declared by : " + aggapifrom.get(apipkg.getKey()) + "\n");

                    Set<PackageContent> content = newBaseLine.getPkgInfo().get(apipkg.getKey());
                    Set<ApiSpiJarKey> devJars = new HashSet<ApiSpiJarKey>();
                    Set<String> classNames = new HashSet<String>();
                    boolean entirePackageIsAPI = true;
                    for (PackageContent pc : content) {
                        Map<String, ClassDeclaration> classes = pc.getClasses();
                        for (Entry<String, ClassDeclaration> classFromPackage : classes.entrySet()) {
                            classNames.add(classFromPackage.getKey());
                            String resourceName = PackageComparator.resourceNameToExclusionTestName(classFromPackage.getKey() + ".class", true);
                            List<ApiSpiJarKey> matchedDevJars = feature.isResourceKnownToAssociatedDevJar(resourceName, false);
                            if (matchedDevJars.isEmpty()) {
                                entirePackageIsAPI = false;
                            } else {
                                devJars.addAll(matchedDevJars);
                            }
                        }
                    }
                    apiReport.append("    classes: \n");
                    for (String className : classNames) {
                        apiReport.append("     - " + className + "\n");
                    }
                    apiReport.append("    devJars: " + devJars + "\n");
                    apiReport.append("    entire Package is API? : " + entirePackageIsAPI + "\n");
                }
            }
            if (!kernelOnly) {
                detailReport.append(apiReport);
            } else {
                detailReport.append(" API?: no.\n");
            }
        }

        if (aggspi != null && aggspi.size() > 0) {
            StringBuffer spiReport = new StringBuffer();
            HashSet<String> kernelFeatures = new HashSet<String>();
            kernelFeatures.addAll(Arrays.asList(KERNEL_FEATURES));
            spiReport.append(" SPI?: yes.\n");
            boolean kernelOnly = true;

            for (Map.Entry<PkgInfo, Set<Map<String, String>>> spipkg : aggspi.entrySet()) {
                boolean kernelPkg = true;
                for (String declarer : aggapifrom.get(spipkg.getKey())) {
                    if (!kernelFeatures.contains(declarer)) {
                        kernelOnly = false;
                        kernelPkg = false;
                    }
                }

                if (!kernelPkg) {
                    spiReport.append("  - " + spipkg.getKey() + " " + spipkg.getValue());
                    spiReport.append("    declared by : " + aggspifrom.get(spipkg.getKey()));

                    Set<PackageContent> content = newBaseLine.getPkgInfo().get(spipkg.getKey());
                    Set<ApiSpiJarKey> devJars = new HashSet<ApiSpiJarKey>();
                    Set<String> classNames = new HashSet<String>();
                    boolean entirePackageIsSPI = true;
                    for (PackageContent pc : content) {
                        Map<String, ClassDeclaration> classes = pc.getClasses();
                        for (Entry<String, ClassDeclaration> classFromPackage : classes.entrySet()) {
                            classNames.add(classFromPackage.getKey());
                            String resourceName = PackageComparator.resourceNameToExclusionTestName(classFromPackage.getKey() + ".class", true);
                            List<ApiSpiJarKey> matchedDevJars = feature.isResourceKnownToAssociatedDevJar(resourceName, false);
                            if (matchedDevJars.isEmpty()) {
                                entirePackageIsSPI = false;
                            } else {
                                devJars.addAll(matchedDevJars);
                            }
                        }
                    }
                    spiReport.append("    classes: \n");
                    for (String className : classNames) {
                        spiReport.append("     - " + className + "\n");
                    }
                    spiReport.append("    devJars: " + devJars + "\n");
                    spiReport.append("    entire Package is SPI? : " + entirePackageIsSPI + "\n");
                }
            }
            if (!kernelOnly) {
                detailReport.append(spiReport);
            } else {
                detailReport.append(" SPI?: no.\n");
            }
        }

        XmlErrorCollator.addReport("Feature: " + feature.getName(),
                                   null,
                                   ReportType.ERROR,
                                   //summary
                                   "New Feature API/SPI Review detail for " + feature.getName(),
                                   //shortText
                                   "[NEW_FEATURE_API_SPI_REVIEW_DETAIL " + feature.getName() + "]",
                                   //reason
                                   detailReport.toString());
    }

    /**
     * Compares a given feature between two framework infos, and reports if binary compatibility
     * has been maintained.
     *
     * @param baseline
     * @param newBaseLine
     * @param matchingFeature
     */
    private void compareFeature(FrameworkInfo baseline,
                                FrameworkInfo newBaseLine, FeatureInfo matchingFeature) {
        FeatureInfo baselineFeature = getFeatureFromFrameworkInfo(matchingFeature, baseline);
        FeatureInfo newbaselineFeature = getFeatureFromFrameworkInfo(matchingFeature, newBaseLine);

        Map<String, Change> affectedResources = new HashMap<String, Change>();

        //have to check API and SPI =) .. it's the same structures, and pretty much same tests, so invoke common processing twice.
        compareAPISPI(baselineFeature.getAggregateAPI(), newbaselineFeature.getAggregateAPI(), baselineFeature.getAggregateAPIDeclarers(),
                      newbaselineFeature.getAggregateAPIDeclarers(), matchingFeature, baseline, newBaseLine, "api", affectedResources);
        compareAPISPI(baselineFeature.getAggregateSPI(), newbaselineFeature.getAggregateSPI(), baselineFeature.getAggregateSPIDeclarers(),
                      newbaselineFeature.getAggregateSPIDeclarers(), matchingFeature, baseline, newBaseLine, "spi", affectedResources);

        //if any resources are affected.. then we need to test the dev bundle/jar versions..
        //if not, we need to test the dev bundle/jar versions are still as they were..

        if (affectedResources != null && !affectedResources.isEmpty()) {
            System.out.println("Compare of feature " + matchingFeature.getName() + " found resources with changes...");
            for (Entry<String, Change> c : affectedResources.entrySet()) {
                System.out.println(" " + c.getValue().name() + " " + c.getKey());
            }

            //first process Major changes..
            Set<ApiSpiJarKey> processedResources = new HashSet<ApiSpiJarKey>();
            processAffectedResources(matchingFeature, baselineFeature, newbaselineFeature, affectedResources, processedResources, Change.MODIFIED_MAJOR);
            //then process the minor.. which will skip over reporting against any resources already reported with major issues.
            processAffectedResources(matchingFeature, baselineFeature, newbaselineFeature, affectedResources, processedResources, Change.MODIFIED_MINOR);
        } else {
            //no affected resources means there should be no changes to dev resource versions.

            //check all old keys are found in the new agg key set.. this allows moves..
            Map<ApiSpiJarKey, Set<FeatureInfo>> oldKeys = new HashMap<ApiSpiJarKey, Set<FeatureInfo>>();
            for (ApiSpiJarKey local : baselineFeature.getDevJarResources().keySet()) {
                Set<FeatureInfo> localSet = new HashSet<FeatureInfo>();
                localSet.add(baselineFeature);
                oldKeys.put(local, localSet);
            }
            Map<ApiSpiJarKey, Set<FeatureInfo>> newAggKeys = new HashMap<ApiSpiJarKey, Set<FeatureInfo>>();
            for (ApiSpiJarKey local : newbaselineFeature.getDevJarResources().keySet()) {
                Set<FeatureInfo> localSet = new HashSet<FeatureInfo>();
                localSet.add(newbaselineFeature);
                newAggKeys.put(local, localSet);
            }
            for (FeatureInfo f : newbaselineFeature.getAggregateFeatureSet()) {
                for (ApiSpiJarKey agg : newbaselineFeature.getDevJarResources().keySet()) {
                    if (newAggKeys.containsKey(agg)) {
                        newAggKeys.get(agg).add(f);
                    } else {
                        Set<FeatureInfo> aggSet = new HashSet<FeatureInfo>();
                        aggSet.add(f);
                        newAggKeys.put(agg, aggSet);
                    }
                }
            }

            //step through old & check each is present in new, with matching major/minor versions (we don't test micro/qualifier).
            for (Entry<ApiSpiJarKey, Set<FeatureInfo>> oldKey : oldKeys.entrySet()) {
                boolean found = false;
                String oldName = oldKey.getKey().getSymbolicName();
                for (Entry<ApiSpiJarKey, Set<FeatureInfo>> newKey : newAggKeys.entrySet()) {
                    if (newKey.getKey().getSymbolicName().equals(oldName)) {
                        found = true;
                        evaluateBundleVersion(oldKey, newKey, null, matchingFeature);
                    }
                }
                if (!found) {
                    //hmm.. so we have no changes recorded, but yet a dev resource known to this resource before is no longer present?
                    //ignore for now.. but we could choose to raise a bug for this..
                    if (GlobalConfig.getReleaseMode().equals("IFIX")) {
                        XmlErrorCollator.setCurrentFeature(matchingFeature.getName());
                        XmlErrorCollator.setNestedFeature(matchingFeature.getName());
                        XmlErrorCollator.addReport("Feature: " + matchingFeature.getName(),
                                                   null,
                                                   ReportType.ERROR,
                                                   //summary
                                                   "Missing dev resource " + oldKey.getKey().getSymbolicName(),
                                                   //shortText
                                                   "[MISSING_DEV_RESOURCE " + oldKey.getKey().getSymbolicName() + "]",
                                                   //reason
                                                   "While verifying dev resources for the current feature, the dev resource with symbolic name "
                                                                                                                       + oldKey.getKey().getSymbolicName()
                                                                                                                       + " could no longer be found associated to this feature. Old filename "
                                                                                                                       + oldKey.getKey().getFilename());

                    }
                }
            }

        }
    }

    /**
     * Utility method to retrieve a given feature info from a given framework info structure.
     *
     * Used to allow finding a feature in the baseline, given a feature from the build, or vice-versa,
     * or retrieving a feature with a constructed key but no content..
     *
     * @param feature
     * @param fi
     * @return
     */
    private FeatureInfo getFeatureFromFrameworkInfo(FeatureInfo feature, FrameworkInfo fi) {
        for (FeatureInfo f : fi.getFeatureInfos()) {
            if (f.equals(feature)) {
                return f;
            }
        }
        return null;
    }

    /**
     * Utility method to retrieve a set of given feature info from a given framework info structure.
     *
     * Used to allow finding a feature in the baseline, given a feature from the build, or vice-versa,
     * or retrieving a feature with a constructed key but no content..
     *
     * @param features string names of features to find.
     * @param fi
     * @return
     */
    private Set<FeatureInfo> getFeaturesFromFrameworkInfo(Set<String> featureNames, FrameworkInfo fi) {
        Set<FeatureInfo> results = new HashSet<FeatureInfo>();
        //System.out.println("trying to find matching features for "+featureNames);
        for (FeatureInfo f : fi.getFeatureInfos()) {
            if (featureNames.contains(f.getName())) {
                results.add(f);
                //System.out.println("match"+f.getName());
            } else {
                //System.out.println("no match "+f.getName());
            }
        }
        return results;
    }

    /**
     * Compares the API or SPI set between two features from two baselines.
     *
     * @param oldAPISPI the api or spi to compare
     * @param newAPISPI the api or spi to compare
     * @param matchingFeature the feature the api or spi is from
     * @param baseline the baseline the feature is from
     * @param newBaseLine the build the feature is from
     * @param apiOrSpi String used for messages to report issues against api or spi.
     * @param affectedResources a set to populate with paths of any resources that have been added/deleted/modified.
     */
    private void compareAPISPI(
                               Map<PkgInfo, Set<Map<String, String>>> oldAPISPI,
                               Map<PkgInfo, Set<Map<String, String>>> newAPISPI,
                               Map<PkgInfo, Set<String>> baselineAPISPIdeclarers,
                               Map<PkgInfo, Set<String>> newAPISPIdeclarers,
                               FeatureInfo matchingFeature,
                               FrameworkInfo baseline,
                               FrameworkInfo newBaseLine,
                               String apiOrSpi,
                               Map<String, Change> affectedResources) {

        XmlErrorCollator.setCurrentFeature(matchingFeature.getName());

        //check the sets of packages still match
        System.out.println("INFO: Comparing " + apiOrSpi + " for " + matchingFeature.getName());
        System.out.println("INFO:   old " + new TreeSet<PkgInfo>(oldAPISPI.keySet()));
        System.out.println("INFO:   new " + new TreeSet<PkgInfo>(newAPISPI.keySet()));

        FeatureInfo oldFeature = getFeatureFromFrameworkInfo(matchingFeature, baseline);
        FeatureInfo newFeature = getFeatureFromFrameworkInfo(matchingFeature, newBaseLine);

        //removals..
        Collection<PkgInfo> baseapi = new HashSet<PkgInfo>();
        baseapi.addAll(oldAPISPI.keySet());
        baseapi.removeAll(newAPISPI.keySet());
        if (!baseapi.isEmpty()) {
            //packages have been removed in the new baseline.. if this feature is not private.. this is an error.
            System.out.println("ERROR: packages have been removed in the new build. " + baseapi);
            for (PkgInfo p : baseapi) {
                if (!"private".equals(matchingFeature.getVisibility())) {
                    XmlErrorCollator.setNestedFeature(baselineAPISPIdeclarers.get(p));
                    XmlErrorCollator.addReport("Package: " + p.toString(),
                                               null,
                                               ReportType.ERROR,
                                               //summary
                                               "Missing Package " + p,
                                               //shortText
                                               "[MISSING_PACKAGE " + p + "]",
                                               //reason
                                               "Package " + p + " was known to the baseline as " + apiOrSpi + " for feature " + matchingFeature.getName()
                                                                              + " (declared as such by feature " + baselineAPISPIdeclarers.get(p)
                                                                              + ") but is no longer known to the new build."
                                                                              + " Has the package been deleted? or no longer exported?");
                }
                //we must remember all resources removed, as they can trigger major bundle changes..
                Set<PackageContent> basepkgcontent = baseline.getPkgInfo().get(p);
                for (PackageContent pc : basepkgcontent) {
                    for (String classKey : pc.getClasses().keySet()) {
                        String key = PackageComparator.resourceNameToExclusionTestName(classKey + ".class", true);
                        System.out.println("Evaluating if new class " + key + " is known as api/spi");
                        if (!oldFeature.isResourceKnownToAssociatedDevJar(key, false).isEmpty()) {
                            System.out.println("@@@@ADDING (major) " + key);
                            affectedResources.put(key, Change.MODIFIED_MAJOR);
                            break;
                        }
                    }
                    for (String xsdKey : pc.getXsds().keySet()) {
                        String key = PackageComparator.resourceNameToExclusionTestName(xsdKey, false);
                        System.out.println("Evaluating if new xsd " + key + " is known as api/spi");
                        if (!oldFeature.isResourceKnownToAssociatedDevJar(key, false).isEmpty()) {
                            System.out.println("@@@@ADDING (major) " + key);
                            affectedResources.put(key, Change.MODIFIED_MAJOR);
                            break;
                        }
                    }
                }
            }
        }
        //additions
        Collection<PkgInfo> buildapi = new HashSet<PkgInfo>();
        buildapi.addAll(newAPISPI.keySet());
        buildapi.removeAll(oldAPISPI.keySet());
        if (!buildapi.isEmpty()) {
            //packages have been added in the new baseline.. this is an info.
            System.out.println("INFO: packages have been added in the new build: " + buildapi);

            System.out.println("Iterating packages.. (count:" + buildapi.size() + ")");
            for (PkgInfo p : buildapi) {
                for (String declarer : newAPISPIdeclarers.get(p)) {
                    XmlErrorCollator.setNestedFeature(declarer);
                    System.out.println("Logging new package of " + p.toString() + " known by declarers of " + newAPISPIdeclarers.get(p));
                    XmlErrorCollator.addReport("Package: " + p.toString(),
                                               null,
                                               ReportType.INFO,
                                               //summary
                                               "New Package " + p,
                                               //shortText
                                               "[NEW_PACKAGE " + p + "]",
                                               //reason
                                               "Package "
                                                                          + p
                                                                          + " is known to the new build as "
                                                                          + apiOrSpi
                                                                          + " for feature "
                                                                          + declarer
                                                                          + " but was not known to the baseline for this feature. No compatibility checking will be performed for this package in this feature.");
                }

                //now test if content of p references classes outside of api/spi
                checkReferencedFieldsReturnTypesAndArgsAreAPIorSPI(newBaseLine, p);

                // if a public feature adds a bundle to itself, then new packages may be present for this bundle on the features api/spi surface.
                // we only want to enforce version changes for bundles that already had resources, so if a resource is new to the feature,
                // then we can skip adding it to affectedResources.

                //we must collect the new packages, as they can cause a minor bundle version change
                Set<PackageContent> newbasepkgcontent = newBaseLine.getPkgInfo().get(p);
                for (PackageContent pc : newbasepkgcontent) {

                    //was the bundle for this package known to the feature in the old baseline?
                    ApiSpiJarKey match = null;
                    String fromSymbolicName = pc.getPackage().getFromBundle();
                    for (Entry<ApiSpiJarKey, Map<String, Boolean>> devJar : oldFeature.getDevJarResources().entrySet()) {
                        if (devJar.getKey().getSymbolicName().equals(fromSymbolicName)) {
                            match = devJar.getKey();
                            break;
                        }
                    }
                    if (match == null) {
                        System.out.println("Skipping addition of package " + p + " to feature " + newFeature.getName() + " because package was found in " + fromSymbolicName
                                           + " which is a new dev resource to this feature");
                        continue;
                    } else {
                        System.out.println("Processing addition of package " + p + " to feature " + newFeature.getName() + " because package was found in " + fromSymbolicName
                                           + " which was previously known as resource " + match + " in the old feature " + oldFeature.getName());
                    }
                    for (String classKey : pc.getClasses().keySet()) {
                        String key = PackageComparator.resourceNameToExclusionTestName(classKey + ".class", true);
                        boolean knownPreviously = !oldFeature.isResourceKnownToAssociatedDevJar(key, false).isEmpty();
                        boolean knownNow = !newFeature.isResourceKnownToAssociatedDevJar(key, false).isEmpty();
                        System.out.println("Evaluating if new class " + key + " is known as api/spi. Known Previously? ["
                                           + knownPreviously + "] Known now? ["
                                           + knownNow + "]");
                        if (!knownPreviously && knownNow) {
                            System.out.println("@@@@ADDING (minor) " + key);
                            affectedResources.put(key, Change.MODIFIED_MINOR);
                            break;
                        }
                    }
                    for (String xsdKey : pc.getXsds().keySet()) {
                        String key = PackageComparator.resourceNameToExclusionTestName(xsdKey, false);
                        boolean knownPreviously = !oldFeature.isResourceKnownToAssociatedDevJar(key, false).isEmpty();
                        boolean knownNow = !newFeature.isResourceKnownToAssociatedDevJar(key, false).isEmpty();
                        System.out.println("Evaluating if new class " + key + " is known as api/spi. Known Previously? ["
                                           + knownPreviously + "] Known now? ["
                                           + knownNow + "]");
                        if (!knownPreviously && knownNow) {
                            System.out.println("@@@@ADDING (minor) " + key);
                            affectedResources.put(key, Change.MODIFIED_MINOR);
                            break;
                        }
                    }
                }
            }
        }

        //if added & removed are both empty, we have an identical set of packages to baseline.
        if (buildapi.isEmpty() && baseapi.isEmpty()) {
            System.out.println("INFO:  " + apiOrSpi + " package set for feature " + matchingFeature.getName() + " still matches.");
            //won't report this via xml, as it'll end up everywhere!!
        }

        //calculate intersection
        Collection<PkgInfo> matchingapi = new HashSet<PkgInfo>();
        matchingapi.addAll(oldAPISPI.keySet()); //add the baseline
        matchingapi.removeAll(baseapi); // remove the no longer founds.
        //matchingapi now holds intersection of old & new.
        if (matchingapi.isEmpty()) {
            //if the match is empty, it can either be no overlap, or no packages to start with..
            if (!(oldAPISPI.keySet().isEmpty())) {
                //no overlap at all? error.
                System.out.println("ERROR: no package overlap found for " + apiOrSpi + " between baseline and new build for feature " + matchingFeature.getName());
                System.out.println("       baseline: " + oldAPISPI.keySet());
                System.out.println("       extras: " + baseapi);
                System.out.println("       trimmed: " + matchingapi);

                for (PkgInfo p : oldAPISPI.keySet()) {
                    //only report missings for non private features..
                    if (!"private".equals(matchingFeature.getVisibility())) {
                        XmlErrorCollator.setNestedFeature(baselineAPISPIdeclarers.get(p));
                        XmlErrorCollator.addReport("Package: " + p.toString(),
                                                   null,
                                                   ReportType.ERROR,
                                                   //summary
                                                   "Missing Package " + p,
                                                   //shortText
                                                   "[MISSING_PACKAGE " + p + "]",
                                                   //reason
                                                   "Package " + p + " was known to the baseline as " + apiOrSpi + " for feature " + matchingFeature.getName()
                                                                                  + " (declared as such by feature " + baselineAPISPIdeclarers.get(p)
                                                                                  + ") but is no longer known to the new build."
                                                                                  + " Has the package been deleted? or no longer exported?");
                    }
                }

            } else {
                //original set was empty, so intersection empty is ok.
                System.out.println("INFO: Empty set of " + apiOrSpi + " for feature " + matchingFeature.getName() + " is still empty, and considered matching.");

                //no need for xml message for this one.. business as normal..
            }
        } else {
            //we have packages in the intersection of old & new to compare.
            List<PackageComparator.VersionResult> packageCompareMismatchResults = new ArrayList<PackageComparator.VersionResult>();
            for (PkgInfo p : matchingapi) {
                //we're looking up a package from the baseline package list, using an api/spi declaration
                //api/spi decls are mainly unversioned.. so that means they could match multiple packages.. each..
                Set<PackageContent> basepkgcontent = baseline.getPkgInfo().get(p);
                Set<PackageContent> newbasepkgcontent = newBaseLine.getPkgInfo().get(p);
                if (basepkgcontent == null || newbasepkgcontent == null) {
                    //sanity check, requires programming error to reach here.
                    System.out.println("ERROR locating " + p + " in the pkgInf sets.. ");
                    System.out.println(" OLD MAP:" + baseline.getPkgInfo().keySet());
                    System.out.println(" NEW MAP:" + newBaseLine.getPkgInfo().keySet());
                    System.out.println(" - old set " + basepkgcontent);
                    System.out.println(" - new set " + newbasepkgcontent);
                    XmlErrorCollator.setNestedFeature(baselineAPISPIdeclarers.get(p));
                    XmlErrorCollator.addProcessingIssue(ReportType.ERROR, "Error processing package " + p + " which was discovered to be existing as " + apiOrSpi
                                                                          + " in both baseline and build, but had no PackageContent in the maps.");
                    continue;
                }

                //now process the sets to remove any ignored package content.
                Set<PackageContent> filteredBase = new HashSet<PackageContent>();
                Set<PackageContent> filteredNewBase = new HashSet<PackageContent>();
                Set<String> aggregateFeatures = new HashSet<String>();
                for (FeatureInfo f : matchingFeature.getAggregateFeatureSet()) {
                    aggregateFeatures.add(f.getName());
                }
                for (PackageContent pc : basepkgcontent) {
                    String bundle = pc.getPackage().getFromBundle();
                    String trimmedBundle = bundle;
                    if (bundle.indexOf('@') != -1) {
                        trimmedBundle = trimmedBundle.substring(0, bundle.indexOf('@'));
                    }
                    boolean allSkip = true;
                    Set<String> declarers = new HashSet<String>(baselineAPISPIdeclarers.get(pc.getPackage()));
                    declarers.retainAll(aggregateFeatures);
                    for (String declarer : declarers) {
                        if (GlobalConfig.isPackageToBeIgnoredInBaseline(pc.getPackage(), bundle, declarer)
                            || GlobalConfig.isPackageToBeIgnoredInBaseline(pc.getPackage(), trimmedBundle, declarer)) {
                        } else {
                            allSkip = false;
                        }
                    }
                    if (!allSkip) {
                        filteredBase.add(pc);
                    } else {
                        System.out.println("* Ignoring baseline package " + pc.getPackage() + " for feature " + baselineAPISPIdeclarers.get(pc.getPackage())
                                           + " as requested by config.");
                    }
                }
                for (PackageContent pc : newbasepkgcontent) {
                    String bundle = pc.getPackage().getFromBundle();
                    String trimmedBundle = bundle;
                    if (bundle.indexOf('@') != -1) {
                        trimmedBundle = trimmedBundle.substring(0, bundle.indexOf('@'));
                    }
                    boolean allSkip = true;
                    Set<String> declarers = new HashSet<String>(newAPISPIdeclarers.get(pc.getPackage()));
                    declarers.retainAll(aggregateFeatures);
                    for (String declarer : newAPISPIdeclarers.get(pc.getPackage())) {
                        if (GlobalConfig.isPackageToBeIgnoredInRuntime(pc.getPackage(), bundle, declarer)
                            || GlobalConfig.isPackageToBeIgnoredInRuntime(pc.getPackage(), trimmedBundle, declarer)) {
                        } else {
                            allSkip = false;
                        }
                    }
                    if (!allSkip) {
                        filteredNewBase.add(pc);
                    } else {
                        System.out.println("* Ignoring runtime package " + pc.getPackage() + " for feature " + newAPISPIdeclarers.get(pc.getPackage())
                                           + " as requested by config.");
                    }
                }
                basepkgcontent = filteredBase;
                newbasepkgcontent = filteredNewBase;

                //now compare the package contents from the sets..
                if (basepkgcontent.size() == 1 && newbasepkgcontent.size() == 1) {
                    //easy case, 1 pkg before, 1 now.. compare them..
                    PackageComparator pc = new PackageComparator();
                    PackageContent oldcontent = basepkgcontent.iterator().next();
                    PackageContent newcontent = newbasepkgcontent.iterator().next();
                    doPackageCompare(oldAPISPI, newAPISPI, baselineAPISPIdeclarers, newAPISPIdeclarers, baseline, newBaseLine, apiOrSpi, packageCompareMismatchResults, p, pc,
                                     oldcontent,
                                     newcontent,
                                     affectedResources);
                } else {
                    if (basepkgcontent.size() == 0) {
                        System.out.println("INFO: package " + p + " is listed as " + apiOrSpi + " in the baseline, but had no content to compare ");
                    } else {
                        //harder case.. we matched multiple packages.. we must check they are compatible with each other..
                        PackageComparator pc = new PackageComparator();
                        Iterator<PackageContent> baseiter = basepkgcontent.iterator();
                        PackageContent oldcontent = baseiter.next();
                        boolean allgood = true;
                        while (baseiter.hasNext()) {
                            PackageContent next = baseiter.next();

                            Set<String> baseDeclarersForPackage = baselineAPISPIdeclarers.get(p);
                            Set<FeatureInfo> baseDeclaringFeatures = getFeaturesFromFrameworkInfo(baseDeclarersForPackage, baseline);

                            XmlErrorCollator.setNestedFeature(baseDeclarersForPackage);

                            //enforce api/spi integrity.
                            checkReferencedFieldsReturnTypesAndArgsAreAPIorSPI(newBaseLine, p);

                            Set<String> oldTypes = new HashSet<String>();
                            Set<String> newTypes = new HashSet<String>();
                            for (Map<String, String> oldmap : oldAPISPI.get(oldcontent.getPackage())) {
                                oldTypes.add(oldmap.get("type"));
                            }
                            for (Map<String, String> newmap : oldAPISPI.get(next.getPackage())) {
                                newTypes.add(newmap.get("type"));
                            }

                            List<VersionResult> errors = pc.compareSplitDupePackage(oldcontent, next, oldTypes, newTypes, baseDeclaringFeatures, apiOrSpi);
                            if (errors != null) {
                                allgood = false;
                                System.out.println("ERROR: package " + oldcontent.getPackage() + " (from feature " + matchingFeature.getName()
                                                   + ") is available in the baseline from bundle [" + oldcontent.getPackage().getFromBundle() + " "
                                                   + oldcontent.getPackage().getFromBundleVersion() +
                                                   "] and in the baseline from bundle [" + next.getPackage().getFromBundle() + " " + next.getPackage().getFromBundleVersion()
                                                   + "] and the instances were found to be incompatible. ");
                                XmlErrorCollator.setNestedFeature(baselineAPISPIdeclarers.get(p));
                                XmlErrorCollator.addReport("Package: " + next.getPackage().toString(),
                                                           next.getPackage().getFromBundle(),
                                                           ReportType.ERROR,
                                                           //summary
                                                           "Baseline has Multiple Versions of " + oldcontent.getPackage(),
                                                           //shortText
                                                           "[MULTIPLE_BASELINE_VERSIONS " + oldcontent.getPackage() + "]",
                                                           //reason
                                                           "Package "
                                                                                                                           + oldcontent.getPackage()
                                                                                                                           + " is available in the baseline from bundle ["
                                                                                                                           + oldcontent.getPackage().getFromBundle()
                                                                                                                           + " "
                                                                                                                           + oldcontent.getPackage().getFromBundleVersion()
                                                                                                                           +
                                                                                                                           "] and in the baseline from bundle ["
                                                                                                                           + next.getPackage().getFromBundle()
                                                                                                                           + " "
                                                                                                                           + next.getPackage().getFromBundleVersion()
                                                                                                                           + "] and the instances were found to be incompatible, this makes this package impossible to use for comparison in the baseline.");

                                //TODO: add errors onto the output.
                            }
                        }
                        Iterator<PackageContent> newbaseiter = newbasepkgcontent.iterator();
                        PackageContent newcontent = newbaseiter.next();
                        while (newbaseiter.hasNext()) {
                            PackageContent next = newbaseiter.next();

                            Set<String> newbaseDeclarersForPackage = newAPISPIdeclarers.get(p);
                            Set<FeatureInfo> newBaseDeclaringFeatures = getFeaturesFromFrameworkInfo(newbaseDeclarersForPackage, newBaseLine);

                            XmlErrorCollator.setNestedFeature(newbaseDeclarersForPackage);

                            Set<String> oldTypes = new HashSet<String>();
                            Set<String> newTypes = new HashSet<String>();
                            for (Map<String, String> oldmap : newAPISPI.get(newcontent.getPackage())) {
                                oldTypes.add(oldmap.get("type"));
                            }
                            for (Map<String, String> newmap : newAPISPI.get(next.getPackage())) {
                                newTypes.add(newmap.get("type"));
                            }

                            List<VersionResult> errors = pc.compareSplitDupePackage(newcontent, next, oldTypes, newTypes, newBaseDeclaringFeatures, apiOrSpi);
                            if (errors != null) {
                                allgood = false;
                                System.out.println("ERROR: package " + newcontent.getPackage() + " (from feature " + matchingFeature.getName()
                                                   + ") is available in the new build from bundle [" + newcontent.getPackage().getFromBundle() + " "
                                                   + oldcontent.getPackage().getFromBundleVersion() +
                                                   "] and in the new build from bundle [" + next.getPackage().getFromBundle() + " " + next.getPackage().getFromBundleVersion()
                                                   + "] and the instances were found to be incompatible. ");
                                XmlErrorCollator.setNestedFeature(newAPISPIdeclarers.get(p));
                                XmlErrorCollator.addReport("Package: " + next.getPackage().toString(),
                                                           next.getPackage().getFromBundle(),
                                                           ReportType.ERROR,
                                                           //summary
                                                           "Build has Multiple Versions of " + newcontent.getPackage(),
                                                           //shortText
                                                           "[MULTIPLE_BUILD_VERSIONS " + newcontent.getPackage() + "]",
                                                           //reason
                                                           "Package "
                                                                                                                        + newcontent.getPackage()
                                                                                                                        + " is available in the new build from bundle ["
                                                                                                                        + newcontent.getPackage().getFromBundle()
                                                                                                                        + " "
                                                                                                                        + newcontent.getPackage().getFromBundleVersion()
                                                                                                                        +
                                                                                                                        "] and in the new build from bundle ["
                                                                                                                        + next.getPackage().getFromBundle()
                                                                                                                        + " "
                                                                                                                        + next.getPackage().getFromBundleVersion()
                                                                                                                        + "] and the instances were found to be incompatible, this makes this package impossible to use for comparison in the baseline.");

                                //TODO: add errors onto the output.
                            }
                        }
                        if (!allgood) {
                            System.out.println("ERROR: unable to perform compare of " + apiOrSpi + " package " + p + " for feature " + matchingFeature.getName()
                                               + " as multiple incompatible candidates were found.");
                            //already reported as xml..
                        } else {
                            System.out.println("INFO:   multiple instances of package " + p + " found to be compatible.");
                            System.out.println("INFO:   performing compare of " + oldcontent + " against " + newcontent);
                            XmlErrorCollator.setNestedFeature(newAPISPIdeclarers.get(p));

                            doPackageCompare(oldAPISPI, newAPISPI, baselineAPISPIdeclarers, newAPISPIdeclarers, baseline, newBaseLine, apiOrSpi, packageCompareMismatchResults, p,
                                             pc,
                                             oldcontent, newcontent, affectedResources);
                        }
                    }

                }
            }
            if (packageCompareMismatchResults.size() > 0) {
                System.out.println("  ");
                System.out.println("==========================================================================================");
                System.out.println("ERROR: package mismatches occured for " + apiOrSpi + " in feature " + matchingFeature.getName());
                System.out.println("==========================================================================================");

                for (PackageComparator.VersionResult vr : packageCompareMismatchResults) {
                    if (vr.majorChange.isChange()) {
                        System.out.println("ERROR: Major change : " + vr.pkg + " found in base[" +
                                           vr.oldFeatureDeclarers + "] build[" +
                                           vr.newFeatureDeclarers + "] " +
                                           makeNice(vr.majorChange.getReason()) + " " +
                                           makeNice(vr.majorChange.getChangeClass()) + " " +
                                           makeNice(vr.majorChange.getSpecialRemarks()));
                    }
                    if (vr.minorChange.isChange()) {
                        System.out.println("ERROR: Minor change : " + vr.pkg + " found in base[" +
                                           vr.oldFeatureDeclarers + "] build[" +
                                           vr.newFeatureDeclarers + "] " +
                                           makeNice(vr.minorChange.getReason()) + " " +
                                           makeNice(vr.minorChange.getChangeClass()) + " " +
                                           makeNice(vr.minorChange.getSpecialRemarks()));
                    }
                }
                System.out.println("   ");
            } else {
                System.out.println("INFO: Feature " + matchingFeature.getName() + " has " + apiOrSpi + " that matches baseline.");
            }
        }

    }

    /**
     * @param oldAPISPI
     * @param newAPISPI
     * @param baselineAPISPIdeclarers
     * @param newAPISPIdeclarers
     * @param newBaseLine
     * @param apiOrSpi
     * @param packageCompareMismatchResults
     * @param p
     * @param pc
     * @param oldcontent
     * @param newcontent
     */
    private void doPackageCompare(Map<PkgInfo, Set<Map<String, String>>> oldAPISPI, Map<PkgInfo, Set<Map<String, String>>> newAPISPI,
                                  Map<PkgInfo, Set<String>> baselineAPISPIdeclarers, Map<PkgInfo, Set<String>> newAPISPIdeclarers, FrameworkInfo baseline,
                                  FrameworkInfo newBaseLine, String apiOrSpi,
                                  List<PackageComparator.VersionResult> packageCompareMismatchResults, PkgInfo p, PackageComparator pc, PackageContent oldcontent,
                                  PackageContent newcontent,
                                  Map<String, Change> affectedResources) {
        System.out.println("INFO:   performing compare of " + oldcontent + " (" + oldAPISPI.get(oldcontent.getPackage()) + ") against " + newcontent + " ("
                           + newAPISPI.get(newcontent.getPackage()) + ")");

        Set<String> newbaseDeclarersForPackage = newAPISPIdeclarers.get(newcontent.getPackage());
        Set<FeatureInfo> newBaseDeclaringFeatures = getFeaturesFromFrameworkInfo(newbaseDeclarersForPackage, newBaseLine);

        //Set<String> baseDeclarersForPackage = baselineAPISPIdeclarers.get(oldcontent.getPackage());
        //Set<FeatureInfo> baseDeclaringFeatures = getFeaturesFromFrameworkInfo(baseDeclarersForPackage, baseline);

        XmlErrorCollator.setNestedFeature(newbaseDeclarersForPackage);

        //enforce api/spi integrity.
        checkReferencedFieldsReturnTypesAndArgsAreAPIorSPI(newBaseLine, p);

        Set<String> oldTypes = new HashSet<String>();
        Set<String> newTypes = new HashSet<String>();
        for (Map<String, String> oldmap : oldAPISPI.get(oldcontent.getPackage())) {
            oldTypes.add(oldmap.get("type"));
        }
        for (Map<String, String> newmap : newAPISPI.get(newcontent.getPackage())) {
            newTypes.add(newmap.get("type"));
        }

        PackageComparator.VersionResult vr = pc.comparePackage(oldcontent, newcontent, oldTypes, newTypes, false, new PrintWriter(System.out),
                                                               newBaseDeclaringFeatures, apiOrSpi);

        //always store the affected resources, as if packages are changed, and versions also changed correctly, then vr.major/vr.minor will be false
        //but we must remember that changes occurred for bundle version evaluation.
        for (Entry<String, com.ibm.ws.featureverifier.internal.PackageComparator.VersionResult.Change> affRes : vr.affectedResources.entrySet()) {
            switch (affRes.getValue()) {
                case MAJOR: {
                    //ifix streams we still report major changes, as they are only allowed with POC approval..
                    System.out.println("@@@ Adding (Modified majpr during pkg compare '" + newcontent.getPackage() + "') " + affRes.getKey());
                    affectedResources.put(affRes.getKey(), Change.MODIFIED_MAJOR);
                    break;
                }
                case MINOR: {
                    if (!GlobalConfig.getReleaseMode().equals("IFIX")) {
                        System.out.println("@@@ Adding (Modified minor during pkg compare '" + newcontent.getPackage() + "') " + affRes.getKey());
                        affectedResources.put(affRes.getKey(), Change.MODIFIED_MINOR);
                    }
                    break;
                }
                default: {
                    throw new IllegalStateException("Unsupported change type " + affRes.getValue().name());
                }
            }
        }

        //if there was a change, record it.
        if (vr.majorChange.isChange() || vr.minorChange.isChange()) {
            Set<String> originalDeclarers = baselineAPISPIdeclarers.get(p);
            Set<String> newDeclarers = newAPISPIdeclarers.get(p);
            vr.oldFeatureDeclarers = originalDeclarers;
            vr.newFeatureDeclarers = newDeclarers;
            vr.pkg = p;
            packageCompareMismatchResults.add(vr);

            if (vr.majorChange.isChange()) {
                XmlErrorCollator.addReport(vr.majorChange.changeClass,
                                           newcontent.getPackage().getFromBundle(),
                                           ReportType.ERROR,
                                           //summary
                                           "Package Compare Major Issue for " + p,
                                           //shortText
                                           "[PACKAGE_COMPARE_MAJOR " + p + "]",
                                           //reason
                                           vr.majorChange.getReason());
            }
            if (vr.minorChange.isChange()) {
                //we silently igore minor package changes in ifix streams.
                if (!GlobalConfig.getReleaseMode().equals("IFIX")) {
                    XmlErrorCollator.addReport(vr.minorChange.changeClass,
                                               newcontent.getPackage().getFromBundle(),
                                               ReportType.ERROR,
                                               //summary
                                               "Package Compare Minor Issue for " + p,
                                               //shortText
                                               "[PACKAGE_COMPARE_MINOR " + p + "]",
                                               //reason
                                               vr.minorChange.getReason());
                }
            }
        }
    }

    /**
     * Scan the supplied package, using the baseline as reference, and check all
     * public/protected classes in the package have their methods / fields using
     * types that are known api/spi to that baseline.
     *
     * Note we don't check (yet) that api/spi confirmed is provided by a feature
     * that declares the supplied package as api/spi. This means we can still have
     * situations where feature A uses types from feature B that it has no relationship
     * to.
     */
    private void checkReferencedFieldsReturnTypesAndArgsAreAPIorSPI(
                                                                    FrameworkInfo newBaseLine, PkgInfo p) {
        List<String> errors = new ArrayList<String>();
        Set<PackageContent> newbasepkgcontent = newBaseLine.getPkgInfo().get(p);
        for (PackageContent pc : newbasepkgcontent) {
            for (ClassDeclaration cd : pc.getClasses().values()) {
                //is the packages for this class api/spi? (if so, remember who for)
                Set<FeatureInfo> matches = new HashSet<FeatureInfo>();
                for (FeatureInfo fi : newBaseLine.getFeatureInfos()) {
                    if (fi.getAggregateAPI().containsKey(p)
                        || fi.getAggregateSPI().containsKey(p)) {
                        matches.add(fi);
                    }
                }
                //no-one claimed the package for this class? skip it.
                if (matches.isEmpty()) {
                    continue;
                } else {
                    //so the package for the class is known to be api/spi.. but what about the class itself?
                    boolean isApi = false;
                    //have to convert from org/package/MyClass to org.package.MyClass.class
                    String className = cd.getName();
                    className = className.replace('/', '.');
                    className += ".class";
                    //ask each feature that owned this package if it knows of this class.
                    for (FeatureInfo match : matches) {
                        //and of course, we have to ask the aggregate set, because
                        //features can declare api/spi that's actually supplied from the
                        //aggregate set.
                        //exit the loop asap if we find anyone saying yes it's api/spi.
                        for (FeatureInfo agg : match.getAggregateFeatureSet()) {
                            if (!agg.isResourceKnownToAssociatedDevJar(className, false).isEmpty()) {
                                isApi = true;
                                break;
                            }
                        }
                        if (isApi)
                            break;
                    }
                    //if none of the aggregate sets for the features owning the package
                    //said this class was api/spi, then we don't process it.
                    if (!isApi) {
                        //System.out.println("@skipping class "+cd.getName()+" because it's api/spi packaged, but not known to dev jars");
                        continue;
                    }
                }

                //if we get this far, the class IS api/spi, so we need to evaluate it.
                //fields, return types, and method args.

                //check both fields and method returns/args
                for (FieldDeclaration fd : cd.getAllFields().values()) {
                    //only process public/protected fields.
                    int access = fd.getAccess();
                    if ((access & Opcodes.ACC_PUBLIC) != 0 || (access & Opcodes.ACC_PROTECTED) != 0) {
                        Type t = Type.getType(fd.getDesc());
                        Set<String> knownAs = isTypeAPISPIorJDK(newBaseLine, t);
                        //empty means unknown, not empty can mean it knew the package, not the class
                        if (knownAs.isEmpty()) {
                            errors.add("Field " + fd.getName() + " with type " + t.getClassName() + " for class " + cd.getName() + " is not known to be api/spi");
                            XmlErrorCollator.addReport(cd.getName() + "#" + fd.getName(), "",
                                                       ReportType.ERROR,
                                                       "Non API/SPI reference to type " + t.getClassName() + " made by " + cd.getName() + "." + fd.getName(),
                                                       "[NON_APISPI_REFERENCE_TO_TYPE " + t.getClassName() + "]",
                                                       "Field " + fd.getName() + " with type " + t.getClassName() + " for class " + cd.getName() + " is not known to be api/spi");
                        } else if (knownAs.contains("PACKAGE_OK_CLASS_NOT")) {
                            knownAs.remove("PACKAGE_OK_CLASS_NOT");
                            errors.add("Field " + fd.getName() + " with type " + t.getClassName() + " for class " + cd.getName() + " is in an " + knownAs
                                       + " package, but the class is not known to a devjar.");
                            XmlErrorCollator.addReport(cd.getName() + "#" + fd.getName(), "",
                                                       ReportType.ERROR,
                                                       "Non API/SPI reference to type " + t.getClassName() + " made by " + cd.getName() + "." + fd.getName(),
                                                       "[NON_APISPI_REFERENCE_TO_TYPE " + t.getClassName() + "]",
                                                       "Field " + fd.getName() + " with type " + t.getClassName() + " for class " + cd.getName() + " is in an " + knownAs
                                                                                                                  + " package, but the class is not known to a devjar.");
                        } else if (knownAs.contains("internal") && knownAs.size() == 1) {
                            errors.add("Field " + fd.getName() + " with type " + t.getClassName() + " for class " + cd.getName() + " is of a type known only as type=internal");
                            XmlErrorCollator.addReport(cd.getName() + "#" + fd.getName(), "",
                                                       ReportType.ERROR,
                                                       "API/SPI reference to internal type " + t.getClassName() + " made by " + cd.getName() + "." + fd.getName(),
                                                       "[APISPI_REFERENCE_TO_INTERNAL_TYPE " + t.getClassName() + "]",
                                                       "Field " + fd.getName() + " with type " + t.getClassName() + " for class " + cd.getName()
                                                                                                                       + " is of a type known only as type=internal");
                        }
                    }
                }
                for (Set<MethodDeclaration> smd : cd.getAllMethods().values()) {
                    for (MethodDeclaration md : smd) {
                        //only process public/protected methods.
                        int access = md.getAccess();
                        if ((access & Opcodes.ACC_PUBLIC) != 0 || (access & Opcodes.ACC_PROTECTED) != 0) {
                            Type t = Type.getReturnType(md.getDesc());
                            Set<String> knownAs = isTypeAPISPIorJDK(newBaseLine, t);
                            //empty means unknown, not empty can mean it knew the package, not the class
                            if (knownAs.isEmpty()) {
                                errors.add("Return type " + t.getClassName() + " of method " + md.getName() + " for class " + cd.getName() + " is not known to be api/spi");
                                XmlErrorCollator.addReport(cd.getName() + "#" + md.getName(), "",
                                                           ReportType.ERROR,
                                                           "Non API/SPI reference to type " + t.getClassName() + " made by " + cd.getName() + "." + md.getName(),
                                                           "[NON_APISPI_REFERENCE_TO_TYPE " + t.getClassName() + "]",
                                                           "Return type " + t.getClassName() + " of method " + md.getName() + " for class " + cd.getName()
                                                                                                                      + " is not known to be api/spi");

                            } else if (knownAs.contains("PACKAGE_OK_CLASS_NOT")) {
                                knownAs.remove("PACKAGE_OK_CLASS_NOT");
                                errors.add("Return type " + t.getClassName() + " of method " + md.getName() + " for class " + cd.getName() + " is in an " + knownAs
                                           + " package, but the class is not known to a devjar.");
                                XmlErrorCollator.addReport(cd.getName() + "#" + md.getName(), "",
                                                           ReportType.ERROR,
                                                           "Non API/SPI reference to type " + t.getClassName() + " made by " + cd.getName() + "." + md.getName(),
                                                           "[NON_APISPI_REFERENCE_TO_TYPE " + t.getClassName() + "]",
                                                           "Return type " + t.getClassName() + " of method " + md.getName() + " for class " + cd.getName() + " is in an " + knownAs
                                                                                                                      + " package, but the class is not known to a devjar.");
                            } else if (knownAs.contains("internal") && knownAs.size() == 1) {
                                errors.add("Return type " + t.getClassName() + " of method " + md.getName() + " for class " + cd.getName()
                                           + " is of a type known only as type=internal");
                                XmlErrorCollator.addReport(cd.getName() + "#" + md.getName(), "",
                                                           ReportType.ERROR,
                                                           "API/SPI reference to internal type " + t.getClassName() + " made by " + cd.getName() + "." + md.getName(),
                                                           "[APISPI_REFERENCE_TO_INTERNAL_TYPE " + t.getClassName() + "]",
                                                           "Return type " + t.getClassName() + " of method " + md.getName() + " for class " + cd.getName()
                                                                                                                           + " is of a type known only as type=internal");
                            }
                            Type a[] = Type.getArgumentTypes(md.getDesc());
                            for (Type ta : a) {
                                Set<String> knownAsa = isTypeAPISPIorJDK(newBaseLine, ta);
                                //empty means unknown, not empty can mean it knew the package, not the class
                                if (knownAsa.isEmpty()) {
                                    errors.add("Argument type " + ta.getClassName() + " of method " + md.getName() + " for class " + cd.getName() + " is not known to be api/spi");
                                    XmlErrorCollator.addReport(cd.getName() + "#" + md.getName(), "",
                                                               ReportType.ERROR,
                                                               "Non API/SPI reference to type " + ta.getClassName() + " made by " + cd.getName() + "." + md.getName(),
                                                               "[NON_APISPI_REFERENCE_TO_TYPE " + ta.getClassName() + "]",
                                                               "Argument type " + ta.getClassName() + " of method " + md.getName() + " for class " + cd.getName()
                                                                                                                           + " is not known to be api/spi");
                                } else if (knownAsa.contains("PACKAGE_OK_CLASS_NOT")) {
                                    knownAsa.remove("PACKAGE_OK_CLASS_NOT");
                                    errors.add("Argument type " + ta.getClassName() + " of method " + md.getName() + " for class " + cd.getName() + " is in an " + knownAsa
                                               + " package, but the class is not known to a devjar.");
                                    XmlErrorCollator.addReport(cd.getName() + "#" + md.getName(), "",
                                                               ReportType.ERROR,
                                                               "Non API/SPI reference to type " + ta.getClassName() + " made by " + cd.getName() + "." + md.getName(),
                                                               "[NON_APISPI_REFERENCE_TO_TYPE " + ta.getClassName() + "]",
                                                               "Argument type " + ta.getClassName() + " of method " + md.getName() + " for class " + cd.getName() + " is in an "
                                                                                                                           + knownAsa
                                                                                                                           + " package, but the class is not known to a devjar.");
                                } else if (knownAsa.contains("internal") && knownAsa.size() == 1) {
                                    errors.add("Argument type " + ta.getClassName() + " of method " + md.getName() + " for class " + cd.getName()
                                               + " is of a type known only as type=internal");
                                    XmlErrorCollator.addReport(cd.getName() + "#" + md.getName(), "",
                                                               ReportType.ERROR,
                                                               "API/SPI reference to internal type " + ta.getClassName() + " made by " + cd.getName() + "." + md.getName(),
                                                               "[APISPI_REFERENCE_TO_INTERNAL_TYPE " + ta.getClassName() + "]",
                                                               "Argument type " + ta.getClassName() + " of method " + md.getName() + " for class " + cd.getName()
                                                                                                                                + " is of a type known only as type=internal");
                                }
                            }
                        }
                    }
                }
            }
        }
        //although we log out to xml, we'll also log into console.log as its handy to review quickly
        for (String er : errors) {
            System.out.println("@@ERROR@@ " + er);
        }
    }

    private final static Set<String> primitiveTypes;
    static {
        primitiveTypes = new HashSet<String>();
        primitiveTypes.addAll(Arrays.asList(new String[] { "double", "long", "float", "int", "short", "byte", "char", "boolean", "void" }));
    }

    /**
     * return a set of identifiers if a given type is known to be api/spi
     * identifiers can include, jdk, system-package, ibm-api, ibm-spi, third-party, internal, spec etc.
     * identifiers can also include PACKAGE_OK_CLASS_NOT informing that the types package was known
     * but the type itself was not.
     * an empty set returned means the type was not known at all.
     */
    private Set<String> isTypeAPISPIorJDK(FrameworkInfo newBaseLine, Type t) {
        Set<String> type = new HashSet<String>();

        String className = t.getClassName();

        //strip off array designators..
        while (className.endsWith("[]")) {
            className = className.substring(0, className.length() - 2);
        }

        //all primitives are fine
        if (primitiveTypes.contains(className) || className.startsWith("java.")) {
            type.add("jdk");
            return type;
        }
        //default package is forbidden for api/spi classes
        if (!className.contains(".")) {
            return type;
        }

        boolean debug = false;
//      uncomment this & set to appropriate package if more info is needed
//      on decisions made.
//        if (className.equals("javax.jms.Connection")) {
//            debug = true;
//        }

        //so now lets evaluate the package.
        String packageName = className.substring(0, className.lastIndexOf('.'));

        //auto approve system packages..
        if (systemPackages.contains(packageName)) {
            type.add("system-package");
            return type;
        }

        //auto approve packages exported by system-bundle (they are sort of auto-spi)
        if (systemExports.contains(packageName)) {
            type.add("system-export");
            return type;
        }

        PkgInfo pi = new PkgInfo(packageName, null, null);
        Set<PackageContent> spc = newBaseLine.getPkgInfo().get(pi);

        if (debug) {
            System.out.println("@evaluating className:" + t.getClassName() + " package: " + packageName + " spc? " + (spc != null ? spc.size() : "null"));
        }
        if (spc.size() == 0) {
            //package was not known to be api/spi
            return type;
        } else {
            //package was.. but what about the class?
            boolean isApiSpi = false;
            for (FeatureInfo fi : newBaseLine.getFeatureInfos()) {
                Set<String> typeForThisFeature = new HashSet<String>();
                if (debug) {
                    System.out.println("@asking " + fi.getName() + " if it knows of package " + pi);
                }
                boolean doWeNeedToProcessThisOne = false;
                if (fi.getAggregateAPI().containsKey(pi)) {
                    Set<Map<String, String>> pattribs = fi.getAggregateAPI().get(pi);
                    for (Map<String, String> attribmap : pattribs) {
                        if (attribmap.containsKey("type")) {
                            String apiType = attribmap.get("type");
                            typeForThisFeature.addAll(unwrapFlattenedArray(apiType));
                            doWeNeedToProcessThisOne = true;
                        } else {
                            typeForThisFeature.add("api"); //api is the default if type is omitted
                            doWeNeedToProcessThisOne = true;
                        }
                    }
                }
                if (fi.getAggregateSPI().containsKey(pi)) {
                    typeForThisFeature.add("ibm-spi");
                    doWeNeedToProcessThisOne = true;
                }
                type.addAll(typeForThisFeature);

                if (doWeNeedToProcessThisOne) {
                    if (debug) {
                        System.out.println("@yes, " + fi.getName() + " knows package " + pi + " as " + typeForThisFeature);
                    }
                    for (FeatureInfo afi : fi.getAggregateFeatureSet()) {
                        if (!afi.isResourceKnownToAssociatedDevJar(className + ".class", debug).isEmpty()) {
                            isApiSpi = true;
                            break;
                        }
                    }
                    if (isApiSpi) {
                        if (type.size() == 1 && type.contains("internal")) {
                            if (debug)
                                System.out.println("@class was only known as [internal], so keep processing");
                        } else {
                            if (debug) {
                                System.out.println("@class was known as " + typeForThisFeature);
                            }
                            break;
                        }
                    } else {
                        if (debug) {
                            System.out.println("@package was known to feature as " + typeForThisFeature + ", but class was not known to aggregate devjar set");
                        }
                    }
                }
            }

            //if we didn't find it as api/spi, and we care about it,
            //then we'll tag the type set with a marker to say we didn't
            //find the class as api/spi.
            //we don't do this for internal as we'll never be able to
            //confirm type=internal classes as api/spi because they dont
            //have dev jars. (spec & third-party may be doable in future,
            //but are more or less out of our control to rectify)
            if (!isApiSpi) {
                if (type.contains("ibm-api") || type.contains("ibm-spi")) {
                    type.add("PACKAGE_OK_CLASS_NOT");
                }
            }

            return type;
        }
    }

    private Set<String> unwrapFlattenedArray(String value) {
        Set<String> result = new HashSet<String>();
        if (value.startsWith("[") && value.endsWith("]")) {
            String trimmed = value.substring(1, value.length() - 1);
            String parts[] = trimmed.split(",");
            for (String part : parts) {
                result.addAll(unwrapFlattenedArray(part));
            }
        } else {
            result.add(value);
        }
        return result;
    }

    /**
     * Small util to convert the xml-ified version result text into something we can use in console.log
     *
     * @param in xml escaped message string
     * @return cleaned up string for console.log
     */
    private String makeNice(String in) {
        if (in == null)
            return "";
        else
            return in.replaceAll("&#13;&#10;", "\n       ");
    }

    /**
     * @param matchingFeature
     * @param baselineFeature
     * @param newbaselineFeature
     * @param affectedResources
     */
    private void processAffectedResources(FeatureInfo matchingFeature, FeatureInfo baselineFeature, FeatureInfo newbaselineFeature, Map<String, Change> affectedResources,
                                          Set<ApiSpiJarKey> resourcesAlreadyReported, Change typeToProcess) {
        //if we have affected resources.. we need to find what makes them api/spi ..
        for (Map.Entry<String, Change> resource : affectedResources.entrySet()) {

            String resourcePath = resource.getKey();
            Change changeType = resource.getValue();

            if (changeType != typeToProcess)
                continue;

            Map<ApiSpiJarKey, Set<FeatureInfo>> newKeys = new HashMap<ApiSpiJarKey, Set<FeatureInfo>>();
            for (ApiSpiJarKey local : newbaselineFeature.isResourceKnownToAssociatedDevJar(resourcePath, false)) {
                Set<FeatureInfo> localSet = new HashSet<FeatureInfo>();
                localSet.add(newbaselineFeature);
                newKeys.put(local, localSet);
            }
            for (FeatureInfo f : newbaselineFeature.getAggregateFeatureSet()) {
                for (ApiSpiJarKey agg : f.isResourceKnownToAssociatedDevJar(resourcePath, false)) {
                    if (newKeys.containsKey(agg)) {
                        newKeys.get(agg).add(f);
                    } else {
                        Set<FeatureInfo> aggSet = new HashSet<FeatureInfo>();
                        aggSet.add(f);
                        newKeys.put(agg, aggSet);
                    }
                }
            }
            Map<ApiSpiJarKey, Set<FeatureInfo>> oldKeys = new HashMap<ApiSpiJarKey, Set<FeatureInfo>>();
            for (ApiSpiJarKey local : baselineFeature.isResourceKnownToAssociatedDevJar(resourcePath, false)) {
                Set<FeatureInfo> localSet = new HashSet<FeatureInfo>();
                localSet.add(baselineFeature);
                oldKeys.put(local, localSet);
            }
            for (FeatureInfo f : baselineFeature.getAggregateFeatureSet()) {
                for (ApiSpiJarKey agg : f.isResourceKnownToAssociatedDevJar(resourcePath, false)) {
                    if (oldKeys.containsKey(agg)) {
                        oldKeys.get(agg).add(f);
                    } else {
                        Set<FeatureInfo> aggSet = new HashSet<FeatureInfo>();
                        aggSet.add(f);
                        oldKeys.put(agg, aggSet);
                    }
                }
            }
            if (oldKeys.isEmpty() && newKeys.isEmpty()) {

                // We need an ignorable error here because there are legacy issues where we don't expose certain classes in SPI packages as SPI
                // (particularly in the web container. If these classes change we will get into this block.
                XmlErrorCollator.setCurrentFeature(matchingFeature.getName());
                XmlErrorCollator.setNestedFeature(matchingFeature.getName());
                XmlErrorCollator.addReport("Feature: " + matchingFeature.getName(),
                                           null,
                                           ReportType.ERROR,
                                           //summary
                                           "Missing dev resource " + resourcePath,
                                           //shortText
                                           "[MISSING_DEV_RESOURCE " + resourcePath + "]",
                                           //reason
                                           "Path  " + resourcePath + " is known to have changed for " + matchingFeature.getName() +
                                                                                          " but there is no associated dev resource. This error should not be ignored unless this is an issue with"
                                                                                          +
                                                                                          " a legacy resource that is part of an API/SPI package but is not exposed as API/SPI.");
            }

            ArrayList<Map.Entry<ApiSpiJarKey, Set<FeatureInfo>>> commonKeys = new ArrayList<Map.Entry<ApiSpiJarKey, Set<FeatureInfo>>>();
            List<Map.Entry<ApiSpiJarKey, Set<FeatureInfo>>> onlyInOld = new LinkedList<Map.Entry<ApiSpiJarKey, Set<FeatureInfo>>>();
            List<Map.Entry<ApiSpiJarKey, Set<FeatureInfo>>> onlyInNew = new LinkedList<Map.Entry<ApiSpiJarKey, Set<FeatureInfo>>>();

            for (Map.Entry<ApiSpiJarKey, Set<FeatureInfo>> oldKey : oldKeys.entrySet()) {
                Map.Entry<ApiSpiJarKey, Set<FeatureInfo>> match = null;
                for (Map.Entry<ApiSpiJarKey, Set<FeatureInfo>> newKey : newKeys.entrySet()) {
                    if (oldKey.getKey().getSymbolicName().equals(newKey.getKey().getSymbolicName())) {
                        match = newKey;
                        break;
                    }
                }
                if (match != null) {
                    commonKeys.add(oldKey);
                    commonKeys.add(match);
                } else {
                    onlyInOld.add(oldKey);
                }
            }
            for (Map.Entry<ApiSpiJarKey, Set<FeatureInfo>> newKey : newKeys.entrySet()) {
                boolean foundInCommon = false;
                for (int i = 0; i < (commonKeys.size() / 2); i++) {
                    if (commonKeys.get((i * 2) + 1).getKey().getSymbolicName().equals(newKey.getKey().getSymbolicName())) {
                        foundInCommon = true;
                        break;
                    }
                }
                if (!foundInCommon) {
                    onlyInNew.add(newKey);
                }
            }
            oldKeys = null;
            newKeys = null;

            if (!onlyInNew.isEmpty()) {
                if (changeType == Change.MODIFIED_MAJOR) {
                    // I think the only case where this can happen is when a jar is switched from SPI to API or vice versa. API to SPI shouldn't be
                    // allowed, SPI to API is usually fine but we should require a specific ignore here. They'll also get tons of missing package errors
                    // because the SPI/API existed in the baseline but doesn't any more.
                    XmlErrorCollator.setCurrentFeature(matchingFeature.getName());
                    XmlErrorCollator.setNestedFeature(matchingFeature.getName());
                    XmlErrorCollator.addReport("Feature: " + matchingFeature.getName(),
                                               null,
                                               ReportType.ERROR,
                                               //summary
                                               "Major changes for " + resourcePath,
                                               //shortText
                                               "[CHANGED_RESOURCE_NOT_AVAILABLE " + resourcePath + "]",
                                               //reason
                                               "The resource at location "
                                                                                                        + resourcePath
                                                                                                        + " does not appear in the baseline, but appears to have major changes. " +
                                                                                                        "This may happen if an SPI jar is changed to API or vice versa. Only SPI to API is legitimate!"
                                                                                                        +
                                                                                                        " API to SPI should not be allowed.");

                } else if (changeType == Change.MODIFIED_MINOR) {

                    //for each
                    for (Map.Entry<ApiSpiJarKey, Set<FeatureInfo>> newKey : onlyInNew) {
                        //the minor change is a resource add.. find if there was an old dev resource with the same symbname, and if known, require minor increase.
                        Map<ApiSpiJarKey, Set<FeatureInfo>> matches = findMatchBySymbolicName(newKey.getKey().getSymbolicName(), baselineFeature);
                        if (matches != null) {
                            for (Map.Entry<ApiSpiJarKey, Set<FeatureInfo>> match : matches.entrySet()) {
                                //we found a matching dev resource in the baseline.
                                //need to check if it's version has been updated appropriately.
                                Change changeToCheck = changeType;
                                if (GlobalConfig.getReleaseMode().equals("IFIX")) {
                                    changeToCheck = null; //tell the version check to enforce no change.
                                }
                                if (!resourcesAlreadyReported.contains(newKey.getKey())) {
                                    boolean ok = evaluateBundleVersion(match, newKey, changeToCheck, matchingFeature);
                                    if (!ok)
                                        resourcesAlreadyReported.add(newKey.getKey());
                                }
                            }
                        } else {
                            //we didn't find any old dev resource that matched the symbolic name for the one that's holding the resouce with a change made
                            //in the current build.

                            //the fact we had no oldKey for this affected path, means it was not known to the feature in the baseline.
                            //this means the dev resource represented by newKey is a new dev resource, and we don't need to worry about the versioning.
                        }
                    }
                }
            } else if (!onlyInOld.isEmpty()) {
                if (changeType == Change.MODIFIED_MAJOR) {
                    for (Map.Entry<ApiSpiJarKey, Set<FeatureInfo>> oldKey : onlyInOld) {
                        //a major change, where the resource path is no longer known to a dev resource in the baseline, but not in the build?
                        //sounds like the resource has been removed.. we need to check if the dev resource itself was known to the feature in the build.
                        //and check it's version.
                        Map<ApiSpiJarKey, Set<FeatureInfo>> matches = findMatchBySymbolicName(oldKey.getKey().getSymbolicName(), newbaselineFeature);
                        if (matches != null) {
                            for (Map.Entry<ApiSpiJarKey, Set<FeatureInfo>> match : matches.entrySet()) {
                                //we found a matching dev resource in the build
                                //need to check if it's version has been updated appropriately.
                                Change changeToCheck = changeType;
                                if (GlobalConfig.getReleaseMode().equals("IFIX")) {
                                    changeToCheck = null; //tell the version check to enforce no change.
                                }
                                if (!resourcesAlreadyReported.contains(match.getKey())) {
                                    boolean ok = evaluateBundleVersion(oldKey, match, changeToCheck, matchingFeature);
                                    if (!ok)
                                        resourcesAlreadyReported.add(match.getKey());
                                }
                            }
                        } else {
                            //we didn't find any new dev resource that matched the symbolic name for the one that's holding the resouce with a change made
                            //from the baseline.
                            //This kinda hints the affected path is no longer known to any dev resource for the affected feature in the new build.
                            //eg, it's not the class/xsd that's been deleted, but the entire dev resource has been, and no replacement has been added.
                            //This should be flagged as missing api/spi .. we can always add another error here =)
                            if (GlobalConfig.getReleaseMode().equals("IFIX")) {
                                XmlErrorCollator.setCurrentFeature(matchingFeature.getName());
                                XmlErrorCollator.setNestedFeature(matchingFeature.getName());
                                Set<String> features = new TreeSet<String>();
                                for (FeatureInfo f : oldKey.getValue()) {
                                    features.add(f.getName());
                                }
                                XmlErrorCollator.addReport("Feature: " + matchingFeature.getName(),
                                                           null,
                                                           ReportType.ERROR,
                                                           //summary
                                                           "Missing dev resource " + oldKey.getKey().getSymbolicName(),
                                                           //shortText
                                                           "[MISSING_DEV_RESOURCE " + oldKey.getKey().getSymbolicName() + "]",
                                                           //reason
                                                           "While processing the changes for the current feature, the dev resource with symbolic name "
                                                                                                                               + oldKey.getKey().getSymbolicName()
                                                                                                                               + " that used to contain "
                                                                                                                               + resourcePath
                                                                                                                               + " (that has been identified as having changes to "
                                                                                                                               + onlyInOld
                                                                                                                               + "), could not be found associated to this feature. Old filename "
                                                                                                                               + oldKey.getKey().getFilename()
                                                                                                                               + " was known to aggregated features " + features);
                            }

                        }
                    }
                } else {
                    //a minor change, where the resource path is known to a dev resource in the baseline, but not in the build?
                    //this shouldn't be minor.. should be major.. means the resource path has been removed entirely from this feature..
                    XmlErrorCollator.setCurrentFeature(matchingFeature.getName());
                    XmlErrorCollator.setNestedFeature(matchingFeature.getName());
                    XmlErrorCollator.addReport("Feature: " + matchingFeature.getName(),
                                               null,
                                               ReportType.ERROR,
                                               //summary
                                               "Missing resource " + resourcePath,
                                               //shortText
                                               "[MISSING_RESOURCE " + resourcePath + "]",
                                               //reason
                                               "While processing the changes for the current feature, resource " + resourcePath
                                                                                          + " was identified as having been changed, but could not be found associated to any dev resource for this feature.");
                }
            } else if (!commonKeys.isEmpty()) {
                for (int i = 0; i < (commonKeys.size() / 2); i++) {
                    Map.Entry<ApiSpiJarKey, Set<FeatureInfo>> oldKey = commonKeys.get(i * 2);
                    Map.Entry<ApiSpiJarKey, Set<FeatureInfo>> newKey = commonKeys.get((i * 2) + 1);
                    System.out.println("Evaluating common old: " + oldKey.getKey());
                    Set<String> features = new TreeSet<String>();
                    for (FeatureInfo f : oldKey.getValue()) {
                        features.add(f.getName());
                    }
                    System.out.println("Common old from : " + features);
                    System.out.println("Evaluating common new: " + newKey.getKey());
                    Set<String> nfeatures = new TreeSet<String>();
                    for (FeatureInfo f : oldKey.getValue()) {
                        nfeatures.add(f.getName());
                    }
                    System.out.println("Common new from : " + nfeatures);
                    System.out.println("Evaluating for " + matchingFeature.getName());
                    //go evaluate if the bundles concerned need updating.
                    Change changeToCheck = changeType;
                    if (GlobalConfig.getReleaseMode().equals("IFIX")) {
                        changeToCheck = null; //tell the version check to enforce no change.
                    }
                    if (!resourcesAlreadyReported.contains(newKey.getKey())) {
                        boolean ok = evaluateBundleVersion(oldKey, newKey, changeToCheck, matchingFeature);
                        if (!ok) {
                            resourcesAlreadyReported.add(newKey.getKey());
                        }
                    }
                }
            }

        }

    }

    /**
     * @param oldKey
     * @param newKey
     * @param changeType
     * @param matchingFeature
     */
    private boolean evaluateBundleVersion(Entry<ApiSpiJarKey, Set<FeatureInfo>> oldResource, Entry<ApiSpiJarKey, Set<FeatureInfo>> newResource, Change changeType,
                                          FeatureInfo forFeature) {
        boolean versionCorrect = true;
        //we want to make sure that versions have altered appropriately..
        //we only care about versions for jars/bundles the users have dependencies on

        //so if

        // oldDevJar type == bundle
        //   package used to be loaded from a bundle.
        // oldDevJar type == jar
        //   package used to be loaded from a devjar.

        //so then.. we know the type of dev resource it used to come from.. but where is it coming from now?
        //
        // easy case, same type, same symbname.. enforce version change by changeType
        // easy case, same symbname, different type, enforce version change of new type by changeType.
        String reason = null;
        Version oldVersion = Version.parseVersion(oldResource.getKey().getVersion());
        Version newVersion = Version.parseVersion(newResource.getKey().getVersion());
        if (changeType == null) {
            //when changeType is null it means we must validate the versions have not changed.
            if (oldVersion.getMajor() != newVersion.getMajor() || oldVersion.getMinor() != newVersion.getMinor()) {
                XmlErrorCollator.setCurrentFeature(forFeature.getName());
                Set<String> names = new TreeSet<String>();
                for (FeatureInfo f : newResource.getValue()) {
                    names.add(f.getName());
                }
                XmlErrorCollator.setNestedFeature(names);
                if (GlobalConfig.getReleaseMode().equals("IFIX")) {
                    versionCorrect = false;
                    XmlErrorCollator.addReport("Feature: " + forFeature.getName(),
                                               null,
                                               ReportType.ERROR,
                                               //summary
                                               "Bundle version changed in error " + newResource.getKey().getFilename(),
                                               //shortText
                                               "[BUNDLE_VERSION_CHANGED_IN_ERROR " + newResource.getKey().getFilename() + "]",
                                               //reason
                                               "Bundle "
                                                                                                                               + newResource.getKey().getSymbolicName()
                                                                                                                               + " loaded from "
                                                                                                                               + newResource.getKey().getFilename()
                                                                                                                               + " has been changed from version  "
                                                                                                                               + oldResource.getKey().getVersion()
                                                                                                                               + " to "
                                                                                                                               + newResource.getKey().getVersion()
                                                                                                                               + ". Bundles MUST NOT change version in the IFIX stream. The bundles Major/Minor version should be reverted to match the old version");
                } else {
                    versionCorrect = false;
                    XmlErrorCollator.addReport("Feature: " + forFeature.getName(),
                                               null,
                                               ReportType.ERROR,
                                               //summary
                                               "Bundle version changed in error " + newResource.getKey().getFilename(),
                                               //shortText
                                               "[BUNDLE_VERSION_CHANGED_IN_ERROR " + newResource.getKey().getFilename() + "]",
                                               //reason
                                               "No changes were made for Bundle " + newResource.getKey().getSymbolicName() + " loaded from " + newResource.getKey().getFilename()
                                                                                                                               + " yet its version has been changed from "
                                                                                                                               + oldResource.getKey().getVersion() + " to "
                                                                                                                               + newResource.getKey().getVersion()
                                                                                                                               + ". The bundles Major/Minor version should be reverted to match the old version");

                }

            }
        } else {
            versionCorrect = false;
            switch (changeType) {
                case MODIFIED_MAJOR: {
                    if (newVersion.getMajor() > oldVersion.getMajor()) {
                        versionCorrect = true;
                    }
                    reason = "Bundle " + newResource.getKey().getSymbolicName() + " known at location " + newResource.getKey().getFilename()
                             + " used to have version " + oldVersion.toString()
                             + " and is now known at version " + newVersion.toString()
                             + ". Major package changes have been made in the bundle, and it's version should be changed from " + oldVersion.getMajor() + "."
                             + oldVersion.getMinor() + ".XXX to "
                             + (oldVersion.getMajor() + 1 + ".0.XXX");
                    break;

                }
                case MODIFIED_MINOR: {
                    if ((newVersion.getMajor() > oldVersion.getMajor()) || ((newVersion.getMajor() == oldVersion.getMajor()) && (newVersion.getMinor() > oldVersion.getMinor()))) {
                        versionCorrect = true;
                    }
                    reason = "Bundle " + newResource.getKey().getSymbolicName() + " known at location " + newResource.getKey().getFilename()
                             + " used to have version " + oldVersion.toString()
                             + " and is now known at version " + newVersion.toString()
                             + ". Minor package changes have been made in the bundle, and it's version should be changed from " + oldVersion.getMajor() + "."
                             + oldVersion.getMinor()
                             + ".XXX to "
                             + oldVersion.getMajor() + "." + (oldVersion.getMinor() + 1) + ".XXX";
                    break;
                }
            }
            if (!versionCorrect) {
                XmlErrorCollator.setCurrentFeature(forFeature.getName());
                Set<String> names = new TreeSet<String>();
                for (FeatureInfo f : newResource.getValue()) {
                    names.add(f.getName());
                }
                XmlErrorCollator.setNestedFeature(names);
                XmlErrorCollator.addReport("Feature: " + forFeature.getName(),
                                           null,
                                           ReportType.ERROR,
                                           //summary
                                           "Bad bundle version" + newResource.getKey().getFilename(),
                                           //shortText
                                           "[BAD_BUNDLE_VERSION " + newResource.getKey().getFilename() + "]",
                                           //reason
                                           reason);

            }
        }
        return versionCorrect;
    }

    private Map<ApiSpiJarKey, Set<FeatureInfo>> findMatchBySymbolicName(String symbolicName, FeatureInfo base) {
        Map<ApiSpiJarKey, Set<FeatureInfo>> result = new HashMap<ApiSpiJarKey, Set<FeatureInfo>>();
        for (Entry<ApiSpiJarKey, Map<String, Boolean>> local : base.getDevJarResources().entrySet()) {
            if (local.getKey().getSymbolicName().equals(symbolicName)) {
                Set<FeatureInfo> localSet = new HashSet<FeatureInfo>();
                localSet.add(base);
                result.put(local.getKey(), localSet);
            }
        }
        for (FeatureInfo f : base.getAggregateFeatureSet()) {
            for (Entry<ApiSpiJarKey, Map<String, Boolean>> agg : f.getDevJarResources().entrySet()) {
                if (agg.getKey().getSymbolicName().equals(symbolicName)) {
                    if (result.containsKey(agg.getKey())) {
                        result.get(agg.getKey()).add(f);
                    } else {
                        Set<FeatureInfo> aggSet = new HashSet<FeatureInfo>();
                        aggSet.add(base);
                        result.put(agg.getKey(), aggSet);
                    }
                }
            }
        }
        if (result.isEmpty())
            return null;
        else
            return result;
    }

//	/**
//	 * Currently not used, kept for when/if we need to validate bundle semantic versions.
//	 * @param framework
//	 * @param f
//	 * @param combinedBundles
//	 */
//	private void findBundlesForFeature(FrameworkBundleRepository framework, FeatureInfo f,
//			Map<String, Set<VersionRange>> combinedBundles) {
//		//now build the set of bundles that 'could' be in play due to this feature
//		//'could' because multiple matches can be present, and the framework will only
//		//select one, but we need to apichk each of them..
//		Map<Long,Bundle> bundlesForFeature = new HashMap<Long,Bundle>();
//		for(Map.Entry<String, Set<VersionRange>> e : combinedBundles.entrySet()){
//			for(VersionRange vr : e.getValue()){
//				String vrString = vr==null?null:vr.toString();
//				Collection<Bundle> matches = framework.matchBundles(e.getKey(), vrString);
//				if(matches!=null && matches.size()>0){
//					for(Bundle b: matches){
//						bundlesForFeature.put(b.getBundleId(), b);
//					}
//				}else{
//					//System.out.println(" - No match for requested bundle "+e.getKey()+" "+e.getValue()+" requested by ("+ f.symbolicName+") in this runtime");
//				}
//			}
//		}
//		List<Long> ids = new ArrayList<Long>();
//		ids.addAll(bundlesForFeature.keySet());
//		Collections.sort(ids);
////		System.out.println(" - Live (Candidate) Bundles for ("+ f.symbolicName +"):");
////		for(Long id : ids){
////			Bundle b = bundlesForFeature.get(id);
////			System.out.println("   ["+b.getBundleId()+"] "+b.getSymbolicName()+" "+b.getVersion());
////		}
//	}
    /**
     * Obtain a file to store the error/issue xml to.
     *
     * @param context
     * @return
     * @throws IOException
     */
    private File getErrorXMLFile(BundleContext context) throws IOException {
        WsLocationAdmin wla = context.getService(context.getServiceReference(WsLocationAdmin.class));
        String outputDirString = wla.resolveString(WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR);
        File outputDir = new File(outputDirString);
        File xmlLog = new File(outputDir, ERROR_XML);
        return xmlLog;
    }

    /**
     * Obtain a file to store the junit xml into.
     *
     * @param context
     * @return
     * @throws IOException
     */
    private File getJUnitFile(BundleContext context) throws IOException {
        WsLocationAdmin wla = context.getService(context.getServiceReference(WsLocationAdmin.class));
        String outputDirString = wla.resolveString(WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR);
        File outputDir = new File(outputDirString);
        File xmlLog = new File(outputDir, JUNIT_XML);
        return xmlLog;
    }

    /**
     * Create a marker file that ant can use to know we have completed our processing, and it's
     * safe to shut the server down.
     *
     * @param context
     * @throws IOException
     */
    private void createExitMarker(BundleContext context) throws IOException {
        WsLocationAdmin wla = context.getService(context.getServiceReference(WsLocationAdmin.class));
        String outputDirString = wla.resolveString(WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR);
        File outputDir = new File(outputDirString);
        File frameworkLog = new File(outputDir, ALL_DONE_TXT);
        FileWriter fw = new FileWriter(frameworkLog);
        try {
            fw.append("Done " + System.currentTimeMillis());
            fw.append("   ");
            if (XmlErrorCollator.getNeedToPublish()) {
                fw.append("REPORT");
            }
            fw.flush();
        } finally {
            fw.close();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {}

}
