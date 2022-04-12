/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wlp.feature.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.osgi.framework.VersionRange;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.version.Version;

import com.ibm.aries.buildtasks.junit.JunitReportWriter;
import com.ibm.aries.buildtasks.junit.TestCase;
import com.ibm.aries.buildtasks.junit.TestSuite;
import com.ibm.aries.buildtasks.junit.TestSuites;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;

/**
 * This implements the ESA/feature generation. It takes a bnd file and generates an ESA and feature manifest.
 *
 */
public class FeatureBnd extends Task {

    private File bnd;
    private File workspaceDir;
    private File dir;
    private Map<String, Edition> editionData = new HashMap<String, FeatureBnd.Edition>();
    private ContentBasedLocalBundleRepository cblbr;
    private Edition defaultEdition = new Edition();
    private File junitErrors;
    private String buildType;
    private String createFor;
    private File esaDir;
    private boolean createESA;
    private String manifestFileProperty = "feature.manifest.file";
    private String featureSymbolicNameProperty = "feature.symbolic.name";
    private String licensePathProperty = "license.path";
    private String licenseTypeProperty = "license.type";
    /**  */
    private static final String IBM_APP_FORCE_RESTART = "IBM-App-ForceRestart";
    /**  */
    private static final String IBM_FEATURE_VERSION = "IBM-Feature-Version";
    /**  */
    private static final String IBM_INSTALL_POLICY = "IBM-Install-Policy";
    /** */
    public static final String IBM_PROVISION_CAPABILITY = "IBM-Provision-Capability";
    /**  */
    private static final String IBM_SHORT_NAME = "IBM-ShortName";
    
    private static final String IBM_LICENSE_INFORMATION = "IBM-License-Information";
    
    private static final String IBM_LICENSE_AGREEMENT = "IBM-License-Agreement";
    /**  */
    private static final String OSGI_SUBSYSTEM_FEATURE = "osgi.subsystem.feature";
    /**  */
    private static final String SUBSYSTEM_DESCRIPTION = "Subsystem-Description";
    /**  */
    private static final String SUBSYSTEM_LOCALIZATION = "Subsystem-Localization";
    /**  */
    private static final String SUBSYSTEM_MANIFEST_VERSION = "Subsystem-ManifestVersion";
    /**  */
    private static final String SUBSYSTEM_NAME = "Subsystem-Name";
    /**  */
    public static final String SUBSYSTEM_SYMBOLIC_NAME = "Subsystem-SymbolicName";
    /**  */
    private static final String SUBSYSTEM_TYPE = "Subsystem-Type";
    /**  */
    private static final String SUBSYSTEM_VERSION = "Subsystem-Version";
    
    /** If true, a conflict during feature resolution involving this feature disables all content. Default is true. */
    private static final String WLP_DISABLE_ONCONFLICT = "WLP-DisableAllFeatures-OnConflict";
    
    
    private static final FilenameFilter LI_FILES = new PrefixFilter("LI_");
    
    private static final FilenameFilter LA_FILES = new PrefixFilter("LA_");
    
    private static class PrefixFilter implements FilenameFilter {
        private String prefix;
        
        public PrefixFilter(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith(prefix);
        }
    }

    /** 
     * This class represents data about an edition.
     */
    public class Edition {
        private String validEditions;
        private String licenseURL;
        private File licensePath;
        private String licenseType;
        public String version;
        public String displayVersion;

        public void setBaseEdition(String baseEdition) {
            editionData.put(baseEdition, this);
        }

        public void setValidEditions(String validEditions) {
            if (validEditions != null && validEditions.isEmpty()) {
                // Treat an empty string as null
                validEditions = null;
            }
            this.validEditions = validEditions;
        }

        public void setLicenseURL(String lic) {
            licenseURL = lic;
        }

        public void setLicensePath(File lic) {
            licensePath = lic;
        }

        public void setLicenseType(String lic) {
            licenseType = lic;
        }

        public void setVersion(String v) {
            version = v;
        }

        public void setDisplayVersion(String displayVersion) {
            this.displayVersion = displayVersion;
        }


        public String getValidEditions() {
            return validEditions == null ? defaultEdition.validEditions : validEditions;
        }

        public String getLicenseURL() {
            return licenseURL == null ? defaultEdition.licenseURL : licenseURL;
        }

        public File getLicensePath() {
            return licensePath == null ? defaultEdition.licensePath : licensePath;
        }

        public String getLicenseType() {
            return licenseType == null ? defaultEdition.licenseType : licenseType;
        }

        public String getVersion() {
            return version == null ? defaultEdition.version : version;
        }
        
        public String getDisplayVersion() {
            if (displayVersion == null) {
                if (defaultEdition.displayVersion == null) {
                    return getVersion();
                } else {
                    return defaultEdition.displayVersion;
                }
            } else {
                return displayVersion;
            }
        }
    }

    public void execute() {
        // if there is no provided bnd file assume a feature.bnd file in the base dir
        if (bnd == null) {
            bnd = new File(getProject().getBaseDir(), "feature.bnd");
        }

        // For tracking validation issues so we output a junit.xml.
        TestSuite suite = new TestSuite();

        // Feature build does the Bnd tool related work for us.
        FeatureBuilder builder = new FeatureBuilder();
        try {
            if (workspaceDir != null) {
                builder.setProperty("workspace", workspaceDir.getAbsolutePath());
            }

            // tell the builder where the bnd is
            builder.setProperties(bnd);
            checkBuilder(builder);

            // tell bnd where the manifest should be
            builder.setProperty(Constants.MANIFEST, "OSGI-INF/SUBSYSTEM.MF");
            checkBuilder(builder);

            // look to see if symbolic name is set, if it isn't then we look at the file name and if it has periods
            // in it we just use the file name minus the extension.
            String featureSymbolicName = builder.getProperty("symbolicName");
            if (featureSymbolicName == null) {
                String name = bnd.getName();
                int index = name.lastIndexOf('.');
                if (index == -1) {
                    throw new BuildException("feature.bnd must include a symbolic name");
                }
                name = name.substring(0, index);
                if (name.indexOf('.') == -1) {
                    throw new BuildException("feature.bnd must include a symbolicName");
                }
                featureSymbolicName = name;
            }
            if (featureSymbolicNameProperty != null && !featureSymbolicNameProperty.isEmpty()) {
                getProject().setProperty(featureSymbolicNameProperty, featureSymbolicName);
            }
            // look for a visibility and blow up if we don't have one.
            String visibility = builder.getProperty("visibility");
            if (visibility == null) {
                throw new BuildException("feature.bnd must include visibility");
            }

            // Look for the edition and kind
            String edition = builder.getProperty("edition");
            String kind = builder.getProperty("kind");
            
            //Weed out kinds that aren't recognized. Only ga, beta, noship is understood.
            if (!!!("ga".equals(kind) || "beta".equals(kind) || "noship".equals(kind))) {
                throw new BuildException("feature.bnd contains an unrecognized kind property: " + kind); 
            }
            
            // if we are creating features for the beta ensure we generate things for beta
            // applicability irrespective of the value of the edition property in the bnd.
            boolean beta = "beta".equals(createFor);
            Edition editionDataObject;
            if (beta) {
                editionDataObject = editionData.get("beta");
            } else {
                editionDataObject = editionData.get(edition);
            }
            
            if (editionDataObject == null) {
                throw new BuildException(featureSymbolicName + " targets edition " + edition + " which isn't known to the task");
            }
            
            builder.setProperty("version", editionDataObject.getDisplayVersion());
            checkBuilder(builder);

            // start processing for include resources. Start by looking for the existing
            // -includeresources and setup the builder so we augment (not replace) that.
            StringBuilder resources = new StringBuilder();
            String includeResources = builder.get(Constants.INCLUDERESOURCE);
            // we need comma separation, so we use a boolean to indicate if it is needed
            // default it to false so first time we don't add one, but once it is set to
            // true we add one. Do this here because if there is a -includresources we
            // need one here.
            boolean addComma = false;
            if (includeResources != null) {
                resources.append(builder.get(Constants.INCLUDERESOURCE));
                addComma = true;
            }

            // work out the applies to and license headers and add them to the builder.
            generateAppliesToAndLicense(builder, edition, beta, resources, addComma);
            addComma = true;

            // get the Subsystem content header.
            Parameters content = builder.getSubsystemContent();
            Properties checksums = new Properties();

            // for each file in -files if it exists add it to the SubsystemContent
            for (Map.Entry<String, Attrs> file : builder.getFiles()) {
                File f = new File(dir, file.getKey());
                Attrs attributes = file.getValue();
                attributes.put("type", "file");
                attributes.put("location:", file.getKey());
                content.put(f.getName(), attributes);
            }

            // for each file in -features add it to the Subsystem-Content
            for (Map.Entry<String, Attrs> feature : builder.getFeatures()) {
                Attrs attributes = feature.getValue();
                attributes.put("type", "osgi.subsystem.feature");
                content.put(feature.getKey(), attributes);
            }

            // for each file in -jars add it to the subsystem content setting type to jar.
            for (Map.Entry<String, Attrs> c : builder.getJars()) {
                Attrs attributes = c.getValue();
                attributes.put("type", "jar");
                // check the location attribute and if lib/ needs to be added, add it.
                addLibFolderIfRequired(attributes);
                content.put(c.getKey(), attributes);
            }

            // for each file in -bundles add it to the subsystem content.
            for (Map.Entry<String, Attrs> c : builder.getBundles()) {
                Attrs attributes = c.getValue();
                // check the location attribute and if lib/ needs to be added, add it.
                addLibFolderIfRequired(attributes);
                content.put(c.getKey(), attributes);
            }

            // Now we go through all the entries in Subsystem-Content.
            for (Map.Entry<String, Attrs> c : content.entrySet()) {
                String name = c.getKey();
                Attrs parameters = c.getValue();
                // if type isn't set we assume it is an osgi.bundle
                String type = parameters.get("type", "osgi.bundle");

                // we ignore features at this point.
                if (!!!"osgi.subsystem.feature".equals(type)) {
                    // Create a test case to capture any validation issues we might find.
                    TestCase tc = new TestCase();
                    suite.add(tc);
                    tc.setClassName(featureSymbolicName);
                    tc.setName(type + "." + name);
                    // get the location (default to lib/ if not present)
                    String location = parameters.get("location:", "lib/").trim();

                    if (addComma) {
                        resources.append(',');
                        addComma = false;
                    }

                    // Get the version and default to 0 if it isn't set. 0 matches everything
                    String version = parameters.getVersion();
                    if (version == null) {
                        version = "0";
                    }

                    // Select a file using the bundle repo for the install. This may return null
                    // if it isn't an OSGi bundle
                    File bundleFile = cblbr.selectBundle(location, name, new VersionRange(version));

                    if (bundleFile == null) {
                        // no OSGi bundle here, so look to see if the file exists and use that
                        bundleFile = new File(dir, location);
                        if (!!!(bundleFile.isFile() && bundleFile.exists())) {
                            bundleFile = null;
                        }
                    }

                    // Assume we got a file in prior steps
                    if (bundleFile != null) {
                        try {
                            // get hold of the bundle version to generate a matching range for the subsystem-content
                            JarFile file = new JarFile(bundleFile);
                            Manifest m = file.getManifest();
                            String v = m.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
                            if (v != null) {
                                String vRange = generateVersionRange(v);

                                parameters.put(Constants.VERSION_ATTRIBUTE, vRange);
                            }
                        } catch (Exception e) {
                            // do nothing, probably not a jar
                        }

                        // get the absolute path.
                        String path = bundleFile.getAbsolutePath();
                        String inESA;
                        path = path.replaceAll("\\\\", "/");
                        // look to see if it is a bundle, if it is it goes in root of ESA, otherwise it doesn't.
                        if ("osgi.bundle".equals(type)) {
                            inESA = bundleFile.getName();
                        } else {
                            inESA = path.substring(path.lastIndexOf("wlp/"));
                        }
                        // update the -includeresources for the file. Pattern is location in esa=path on disk
                        resources.append(inESA);
                        resources.append('=');
                        resources.append(path);
                        // next time round the loop we need to add a comma.
                        addComma = true;

                        // need to store the checksum away.
                        checksums.put(inESA, MD5Utils.getFileMD5String(bundleFile));
                    } else {
                        tc.setFailure("Should have a file matching " + name + " and location " + location);
                    }
                }
            }

            builder.setSubsystemContent(content);
            checkBuilder(builder);
            
            String superseded = builder.getProperty("superseded-by");
            
            if (superseded != null) {
                Parameters p = builder.getParameters(SUBSYSTEM_SYMBOLIC_NAME);
                Attrs attributes = p.entrySet().iterator().next().getValue();
                attributes.put("superseded", "true");
                attributes.put("superseded-by", superseded);
                builder.setSubsystemSymbolicName(p);
                checkBuilder(builder);
            }

            // check  WLP_DISABLE_ONCONFLICT header for non auto-features
            if (builder.getProperty(IBM_PROVISION_CAPABILITY) == null) {
                String disableOnConflict = builder.getProperty(WLP_DISABLE_ONCONFLICT);
                if (disableOnConflict != null)
                    checkValue(suite, builder, WLP_DISABLE_ONCONFLICT, "true", "false");
                else {
                    builder.setProperty(WLP_DISABLE_ONCONFLICT, "true");
                }
                checkBuilder(builder);
            }

            //I think this method isn't doing what is intended. This basically means output to both wlp/lib/features
            //and build/subsystem.mf because this method is called twice. Once with createFor==buildType, and once without.
            
            File manifest;
            boolean saveJunitReport = false;
            Map.Entry<String, Attrs> featureParams = builder.getSubsystemSymbolicName();
            if (featureParams == null) {
                throw new BuildException("getSubsystemSymbolicName is null");
            }
            String featureName = featureParams.getKey();
            if (createFor.equals(buildType)) {
                saveJunitReport = true;
                manifest = new File(dir, "lib/features/" + featureName + ".mf");
            } else {
                manifest = new File(getProject().getBaseDir(), "build/subsystem.mf");
            }
            
            // Set the ANT property pointing to the manifest so it can be used later on in the ANT process
            if (manifestFileProperty != null && !manifestFileProperty.isEmpty()) {
                getProject().setProperty(manifestFileProperty, manifest.getAbsolutePath());
            }

            resources.append(',');
            resources.append("OSGI-INF/SUBSYSTEM.MF=");
            resources.append(manifest.getAbsolutePath());

            builder.setProperty(Constants.INCLUDERESOURCE, resources.toString());
            builder.writeManifest(manifest);
            builder.setProperty(Constants.NOMANIFEST, "ignore");
            builder.setProperty(Constants.POM, "false");
            checkBuilder(builder);

            validateFeature(builder, suite);

            // if we upgrade BND we can do this instead of the code above
//			builder.setProperty(Constants.MANIFEST_NAME, "OSGI-INF/SUBSYSTEM.MF");

            //Where we control what goes into which shipment type.
            //For a GA release, we only want to build GA features.
            //However, for a beta release we want GA and BETA features both.
            if (createESA && (kind.equals(createFor) || "ga".equals(kind))) {
                Jar jar = builder.build();
                checksums.put("OSGI-INF/SUBSYSTEM.MF", MD5Utils.getFileMD5String(manifest));
                jar.putResource("OSGI-INF/checksums.cs", new PropertiesResource(checksums));
                File file = new File(esaDir, featureSymbolicName + ".esa");
                jar.write(file);
                log("Created " + file);
            }
            
            TestSuites suites = new TestSuites();
            suites.add(suite);
            suite.setName(featureName + " feature packaging tests");
            
            if (suites.getErrors() + suites.getFailures() > 0 ) {
                if (saveJunitReport) {
                    JunitReportWriter.persist(suites, junitErrors);
                }
                log("Found " + suites.getErrors() + " errors and " + suites.getFailures() + " failures");
            } else {
                log("No feature issues found");
            }
        } catch (Exception e1) {
            throw new BuildException(e1.getMessage(), e1);
        }
    }

    private void checkBuilder(FeatureBuilder builder) {
        if (!builder.isOk()) {
            throw new BuildException("feature " + bnd.getName() + " error in builder: " + builder.getErrors());
        }
    }

    private String generateVersionRange(String v) {
        Version ver = new Version(v);
        int major = ver.getMajor();
        int minor = ver.getMinor();
        int micro = ver.getMicro();
        int floor = micro - (micro % 200);
        int ceiling = floor + 200;

        StringBuilder vRange = new StringBuilder("[");
        vRange.append(major);
        vRange.append('.');
        vRange.append(minor);
        vRange.append('.');
        vRange.append(floor);
        vRange.append(',');
        vRange.append(major);
        vRange.append('.');
        vRange.append(minor);
        vRange.append('.');
        vRange.append(ceiling);
        vRange.append(')');
        return vRange.toString();
    }

    private void addLibFolderIfRequired(Attrs a) {
        String location = a.get("location:");

        if (location == null) {
            return;
        }

        boolean addLib = true;
        int libCount = 0;

        String[] locParts = location.split(",");

        for (String locPart : locParts) {
            // if we have an exact location then we don't want to add lib/. 
            if (!!!locPart.endsWith("/")) {
                addLib = false;
            }
            // increment libCount if we found lib/ this is so we don't ad lib again.
            if ("lib/".equals(locPart)) {
                libCount++;
            }
        }

        if (addLib && libCount == 0) {
            StringBuilder newLocation = new StringBuilder();
            newLocation.append(location);
            newLocation.append(",lib/");
            a.put("location:", newLocation.toString());
        }
    }

    private void generateAppliesToAndLicense(FeatureBuilder build, String edition, boolean beta, StringBuilder resources, boolean addComma) throws IOException {
        String name = build.getProperty("IBM-ProductID", "com.ibm.websphere.appserver");
        Attrs attributes = new Attrs();
        Edition e;
        if (beta) {
            e = editionData.get("beta");
        } else {
            e = editionData.get(edition);
        }
        if (e.getValidEditions() != null) {
            attributes.put("productEdition", e.getValidEditions());
        }
        if (e.getVersion() != null) {
            attributes.put("productVersion", e.getVersion());
        }

        build.setAppliesTo(name, attributes);

        if (e.getLicenseURL() != null) {
            build.setProperty("Subsystem-License", e.getLicenseURL());
        } else {
            throw new BuildException("Unable to find url for edition " + (beta ? "beta" : edition));
        }

        if (addComma) {
            resources.append(',');
        }
        resources.append("wlp/lafiles/=");
        String licensePath = e.getLicensePath().getAbsolutePath(); 
        resources.append(licensePath);
        getProject().setProperty(licensePathProperty, licensePath);
        getProject().setProperty(licenseTypeProperty, e.getLicenseType());
        
        if (e.getLicensePath().listFiles(LI_FILES).length > 0) {
            build.setProperty(IBM_LICENSE_INFORMATION, "wlp/lafiles/LI");
        }
        
        if (e.getLicensePath().listFiles(LA_FILES).length > 0) {
            build.setProperty(IBM_LICENSE_AGREEMENT, "wlp/lafiles/LA");
        }
    }

    private void validateFeature(FeatureBuilder builder, TestSuite suite) {
        checkValue(suite, builder, SUBSYSTEM_MANIFEST_VERSION, "1");
        checkValue(suite, builder, IBM_FEATURE_VERSION, "2");
        checkValue(suite, builder, SUBSYSTEM_TYPE, OSGI_SUBSYSTEM_FEATURE);
        checkSet(suite, builder, SUBSYSTEM_SYMBOLIC_NAME);
        checkSet(suite, builder, SUBSYSTEM_VERSION);

        if (builder.getProperty(IBM_PROVISION_CAPABILITY) != null) {
            checkValue(suite, builder, IBM_INSTALL_POLICY, "when-satisfied", "manual");
        }

        String val = builder.getProperty(SUBSYSTEM_VERSION);

        TestCase testCase = new TestCase();
        testCase.setName(SUBSYSTEM_VERSION);
        try {
            Version.parseVersion(val);
        } catch (IllegalArgumentException e) {
            // error
            testCase.setFailure(e.toString());
        }
        suite.add(testCase);

        if ("public".equals(builder.getProperty("visibility"))) {
            String symbolicName = builder.getProperty("symbolicName");
            Properties rb = getResourceBundle(builder.getBase(), symbolicName);

            checkValue(suite, builder, SUBSYSTEM_LOCALIZATION, "OSGI-INF/l10n/" + symbolicName);
            checkNLS(suite, builder, rb, SUBSYSTEM_DESCRIPTION);
            checkSet(suite, builder, SUBSYSTEM_NAME);
            checkSet(suite, builder, IBM_SHORT_NAME);
        }

        val = builder.getProperty(IBM_APP_FORCE_RESTART);
        if (val != null) {
            testCase = new TestCase();
            testCase.setName(IBM_APP_FORCE_RESTART);
            String[] values = val.split(",");

            for (String str : values) {
                if (!!!("install".equals(str.trim()) || "uninstall".equals(str.trim()))) {
                    testCase.setFailure(IBM_APP_FORCE_RESTART + " was not set correctly. Should contain install or uninstall, Found: " + val);
                }
            }
            suite.add(testCase);
        }
    }

    public void setBnd(File theBnd) {
        bnd = theBnd;
    }

    public void setWorkspaceDir(File workspaceDir) {
        this.workspaceDir = workspaceDir;
    }

    public void setJunit(File junitReport) {
        junitErrors = junitReport;
    }

    public void setDir(File dir) {
        this.dir = dir;
        cblbr = new ContentBasedLocalBundleRepository(dir, null, false);
    }

    public Edition createEdition() {
        return new Edition();
    }

    public Edition createDefaultEdition() {
        return defaultEdition;
    }

    private Properties getResourceBundle(File parentFile, String symbolicName) {
        File nls = new File(new File(parentFile, "resources/l10n"), symbolicName + ".properties");
        Properties props = new Properties();
        try {
            props.load(new FileReader(nls));
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        return props;
    }

    private void checkNLS(TestSuite suite, FeatureBuilder attribs, Properties rb, String name) {
        String val;
        TestCase testCase;
        val = attribs.getProperty(name);
        testCase = new TestCase();
        testCase.setName(name);
        if (val == null) {
            testCase.setFailure(name + " must be set for a public feature");
        } else if (val.charAt(0) == '%') {
            if (rb != null) {
                String value = rb.getProperty(val.substring(1));
                if (value == null) {
                    testCase.setFailure(name + " cannot be resolved using key " + val);
                }
            } else {
                testCase.setFailure("Unable to resolve resource bundle for the feature");
            }
        } else {
            testCase.setFailure(name + " must be translated for a public feature");
        }
        suite.add(testCase);
    }

    /**
     * @param suite
     * @param attribs
     * @param subsystemSymbolicName
     */
    private void checkSet(TestSuite suite, FeatureBuilder attribs, String name) {
        String val = attribs.getProperty(name);
        TestCase testCase = new TestCase();
        testCase.setName("isSet" + name);
        if (val == null) {
            testCase.setFailure(name + " was not set");
        }
        suite.add(testCase);
    }

    private void checkValue(TestSuite suite, FeatureBuilder attribs, String name, String value) {
        String val;
        TestCase testCase;
        val = attribs.getProperty(name);
        testCase = new TestCase();
        testCase.setName("equals" + name);
        if (!!!value.equals(val)) {
            testCase.setFailure(name + " was not set correctly. Expected: " + value + ", Found: " + val);
        }
        suite.add(testCase);
    }

    /**
     * @param suite
     * @param attribs
     * @param name
     * @param values
     */
    private void checkValue(TestSuite suite, FeatureBuilder attribs, String name, String... values) {
        String val;
        TestCase testCase;
        val = attribs.getProperty(name);
        testCase = new TestCase();
        testCase.setName("equals" + name);
        List<String> valuesList = Arrays.asList(values);
        if (!!!valuesList.contains(val)) {
            testCase.setFailure(name + " was not set correctly. Expected one of: " + valuesList + ", Found: " + val);
        }
        suite.add(testCase);
    }

    public void setBuildType(String buildType) {
        this.buildType = buildType;
    }

    public void setCreateFor(String createFor) {
        this.createFor = createFor;
    }

    public void setEsaDir(File esaDir) {
        this.esaDir = esaDir;
    }
    
    public void setCreateESA(boolean create) {
        createESA = create;
    }

    /**
     * States the name of the ANT property to set pointing to the manifest file generated by this task. Defaults to <code>feature.manifest.file</code>
     * @param manifestFileProperty the manifestFileProperty to set
     */
    public void setManifestFileProperty(String manifestFileProperty) {
        this.manifestFileProperty = manifestFileProperty;
    }

    /**
     * States the name of the ANT property to set pointing to the feature's symbolic name as calculated by this task. Defaults to <code>feature.symbolic.name</code>
     * @param featureSymbolicNameProperty the featureSymbolicNameProperty to set
     */
    public void setFeatureSymbolicNameProperty(String featureSymbolicNameProperty) {
        this.featureSymbolicNameProperty = featureSymbolicNameProperty;
    }

    /**
     * States the name of the ANT property to set pointing to the license path that is going used to put the license into the ESA. Defaults to <code>license.path</code>
     * @param licensePathProperty the licensePathProperty to set
     */
    public void setLicensePathProperty(String licensePathProperty) {
        this.licensePathProperty = licensePathProperty;
    }

    /**
     * States the name of the ANT property to set pointing to the license type that is used to put the license into the ESA. Defaults to <code>license.type</code>
     * @param licenseTypeProperty the licenseTypeProperty to set
     */
    public void setLicenseTypeProperty(String licenseTypeProperty) {
        this.licenseTypeProperty = licenseTypeProperty;
    }
}