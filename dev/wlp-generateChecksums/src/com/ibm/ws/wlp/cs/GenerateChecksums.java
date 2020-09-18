/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wlp.cs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.apache.aries.util.manifest.ManifestProcessor;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.osgi.framework.VersionRange;

import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;

/**
 * GenerateChecksums
 */
public class GenerateChecksums extends Task {
    //ant task attributes
    private File installRoot;
    private final String featuresPath = "lib/features/";
    private final String platformPath = "lib/platform/";
    private final String checksumsDirName = "checksums";
    private boolean ignoreBinFiles = false;

    //the ext name of all the checksum files
    private static String CHECKSUM_FILE_EXT = "cs";

    //initialed from installRoot when execute
    private ContentBasedLocalBundleRepository cblbr;

    @Override
    public void execute() {

        File featuresDir = new File(installRoot, featuresPath);
        File platformDir = new File(installRoot, platformPath);
        File platformChecksums = new File(platformDir, checksumsDirName);
        File featureChecksums = new File(featuresDir, checksumsDirName);

        try {
            // generate platform blst's checksums
            generateChecksums(platformDir, platformChecksums);
            // Only generated for platform
            // generateChecksums(featuresDir, featureChecksums);

        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * @param dir
     * @param checksumsDir
     * @throws IOException
     */
    private void generateChecksums(File dir, File checksumsDir) throws IOException {
        checksumsDir.mkdirs();
        File[] mfFiles = dir.listFiles();
        for (File mfFile : mfFiles) {
            if (!mfFile.getName().endsWith(".mf")) {
                continue;
            }
            if (mfFile.getName().startsWith("build.dita.kernel")) {
                continue;
            }

            //generate the cs file from the Subsystem-Content listed in mf
            Properties props = getChecksumsForMF(mfFile);
            String[] mfChecksum = getChecksumPair(mfFile);
            props.setProperty(mfChecksum[0], mfChecksum[1]);
            String fileName = props.remove("file.name").toString();
            File csFile = new File(checksumsDir, fileName + "." + CHECKSUM_FILE_EXT);
            createFile(csFile, props);

        }
    }

    private void createFile(File file, Properties props) throws IOException {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file, false);
            props.store(out, null);
        } finally {
            FileUtils.tryToClose(out);
        }
    }

    private String[] getChecksumPair(File file) throws IOException {
        String relativePath = file.getAbsolutePath().replaceAll(Pattern.quote(installRoot.getAbsolutePath() + File.separator), "");
        return new String[] { relativePath.replaceAll("\\\\", "/"), MD5Utils.getFileMD5String(file) };
    }

    /**
     * get the checksum content for a feature
     */
    private Properties getChecksumsForMF(File mf) throws IOException {
        Properties props = new Properties();

        InputStream is = null;
        try {
            is = new FileInputStream(mf);
            Manifest manifest = ManifestProcessor.parseManifest(is);

            String fileName = manifest.getMainAttributes().getValue("Subsystem-SymbolicName");
            NameValuePair pair = ManifestHeaderProcessor.parseBundleSymbolicName(fileName);
            props.put("file.name", pair.getName());

            String contents = manifest.getMainAttributes().getValue("Subsystem-Content");

            Map<String, Map<String, String>> data = ManifestHeaderProcessor.parseImportString(contents);

            for (Map.Entry<String, Map<String, String>> entry : data.entrySet()) {

                String name = entry.getKey();
                Map<String, String> attrs = entry.getValue();
                String type = attrs.get("type");
                // exclude the feature type and jar type
                if (type != null && (type.equals("osgi.subsystem.feature") || type.equals("checksum"))) {
                    continue;
                }

                // find the jar, we don't need care about the version,
                // because only one jar exists when build
                String location = attrs.get("location:");
                File jarFile = null;

                String version = attrs.get("version");
                VersionRange range = new VersionRange(version == null ? "0" : version);
                jarFile = cblbr.selectBundle(location, name, range);

                if (jarFile == null && location != null) {
                    jarFile = new File(installRoot, location);
                }

                if (jarFile == null && (type == null || "osgi.bundle".equals(type))) {
                    throw new BuildException("The OSGi Bundle with name: " + name + ", location: " + location + ", and version " + version
                                             + " does not exist (included in manifest "
                                             + mf.getName() + ")");
                }

                // If we have a null jar, throw a helpful exception rather than an NPE on the next line
                if (jarFile == null) {
                    throw new IllegalArgumentException("Could not find a jar file for the bundle " + name);
                }

                if (!jarFile.exists()) {
                    if ((type == null || "osgi.bundle".equals(type))) {
                        throw new BuildException("The OSGi Bundle with: " + name + ", location: " + location + ", and version " + version
                                                 + " does not exist (included in manifest "
                                                 + mf.getName() + ". The jar name was calculated as " + jarFile.getAbsolutePath() + " but this file does not exist.)");
                    }
                } else {
                    // If the bundle selection gave us a directory, and we're here, something is wrong
                    // We can't - and don't want to - try and calculate an MD5 hash for a directory
                    if (jarFile.isDirectory()) {
                        throw new BuildException("The entry with name " + name + ", location " + location + ", and version range " + version + " does not exist from "
                                                 + mf.getName());
                    }
                    if (shouldGenerateChecksum(jarFile)) {
                        // if it's not a directory, carry on and calculate the jar checksum
                        String[] checksumPair = getChecksumPair(jarFile);
                        props.setProperty(checksumPair[0], checksumPair[1]);
                    }
                }

            }
        } finally {
            FileUtils.tryToClose(is);
        }
        return props;

    }

    /**
     * @param jarFile
     * @return
     */
    private boolean shouldGenerateChecksum(File jarFile) {
        boolean result = true;

        if (ignoreBinFiles) {
            // if we are ignoring the bin files get the path to this file and the path to the install root.
            String path = jarFile.getAbsolutePath();
            String otherPath = installRoot.getAbsolutePath();
            // So first of all check to see if the file is in the install/bin dir and also not in the tools dir.
            if (path.startsWith(otherPath + File.separator + "bin") && !!!"tools".equals(jarFile.getParentFile().getName())) {
                result = false;
            }
        }
        return result;
    }

    // getters and setters
    public File getInstallRoot() {
        return installRoot;
    }

    public void setInstallRoot(File installRoot) {
        this.installRoot = installRoot;
        cblbr = new ContentBasedLocalBundleRepository(installRoot, null, false);
    }

    public void setIgnoreBinFiles(boolean ignore) {
        ignoreBinFiles = ignore;
    }
}
