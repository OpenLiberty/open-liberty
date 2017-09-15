/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.extension.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry.BundleRepositoryHolder;
import com.ibm.ws.product.utility.extension.MD5Utils;

/**
 *
 */
public class TestUtils {

    // The pattern we use to break out the symbolic name and version from an ifix update xml element.
    private static final Pattern updateSymbolicNameVersionPattern = Pattern.compile(".*id=\"(.*?)_(.*?)\\.jar.*");
    // The pattern we use to get the attrs from the ifix xml file element.
    private static final Pattern updateAttrPattern = Pattern.compile("<file(.*)/>");

    /**
     * This method generates the ifix xml files that contain the files that are in the ifix install. This method also
     * builds the Liberty Profile Metadata files for any jars supplied.
     * 
     * @param ifixFile - The Ifix File to write to.
     * @param ifixName - The name of the ifix
     * @param updates - A Set of string containing the update file names.
     * @param ifixApars - A set of String containing the apar numbers.
     */
    public static void createIfixXML(File ifixFile, String ifixName, Set<String> updates, Set<String> ifixApars) {

        // The ifix xml buffer
        StringBuffer ifixBuffer = new StringBuffer();
        // The Liberty profile metadata buffer if it is needed.
        StringBuffer lpmfBuffer = new StringBuffer();

        ifixBuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        ifixBuffer.append("  <fix id=\"" + ifixName + "\" version=\"1.0.0\">\n");
        ifixBuffer.append("  <applicability>\n");
        ifixBuffer.append("    <offering id=\"com.ibm.websphere.BASE.v85\" tolerance=\"[8.5.1,8.5.2)\"/>\n");
        ifixBuffer.append("    <offering id=\"com.ibm.websphere.DEVELOPERS.v85\" tolerance=\"[8.5.1,8.5.2)\"/>\n");
        ifixBuffer.append("    <offering id=\"com.ibm.websphere.ND.v85\" tolerance=\"[8.5.1,8.5.2)\"/>\n");
        ifixBuffer.append("  </applicability>\n");
        ifixBuffer.append("  <categories/>\n");
        ifixBuffer.append("  <information name=\"" + ifixName + "\" version=\"8.5.1.20121128_1822\">Web application response times are very slow</information>\n");
        ifixBuffer.append("  <property name=\"com.ibm.ws.superseded.apars\" value=\"PM70625\"/>\n");
        ifixBuffer.append("  <property name=\"recommended\" value=\"false\"/>\n");
        ifixBuffer.append("  <resolves problemCount=\"" + ifixApars.size() + "\" description=\"This fix resolves APARS:\" showList=\"true\">\n");
        for (String apar : ifixApars)
            ifixBuffer.append("    <problem id=\"com.ibm.ws.apar." + apar + "\" displayId=\"" + apar + "\" description=\"" + apar + "\"/>\n");
        ifixBuffer.append("  </resolves>\n");
        ifixBuffer.append("  <updates>\n");

        lpmfBuffer.append("<libertyFixMetadata>\n");
        lpmfBuffer.append("  <bundles>\n");

        for (String update : updates) {
            ifixBuffer.append("    " + update + "\n");

            // If the current update is a jar file, then work out the symbolicname and version and write the values out to the 
            // Liberty Profile Metadata file.
            if (update.contains(".jar\"")) {
                Matcher symbolicNameMatcher = updateSymbolicNameVersionPattern.matcher(update);
                Matcher updateAttrMatcher = updateAttrPattern.matcher(update);
                if (symbolicNameMatcher.matches() && updateAttrMatcher.matches()) {
                    String updateAttrString = updateAttrMatcher.group(1);
                    String symbolicName = symbolicNameMatcher.group(1);
                    if (symbolicName.contains("/"))
                        symbolicName = symbolicName.substring(symbolicName.lastIndexOf("/") + 1);

                    lpmfBuffer.append("    <bundle " + updateAttrString + " symbolicName=\"" + symbolicName +
                                      "\" version=\"" + symbolicNameMatcher.group(2) + "\" isBaseBundle=\"false\"/>\n");
                }
            }
        }

        ifixBuffer.append("  </updates>\n");
        ifixBuffer.append("</fix>\n");

        lpmfBuffer.append("  </bundles>\n");
        lpmfBuffer.append("</libertyFixMetadata>\n");

        createFile(ifixFile, ifixBuffer, false);
        String ifixABSPath = ifixFile.getAbsolutePath();
        File lpmfFile = new File(ifixABSPath.substring(0, ifixABSPath.lastIndexOf(".")) + ".lpmf");
        createFile(lpmfFile, lpmfBuffer, false);
    }

    /**
     * This writes a StringBuffer out to the required file. The file is always overwritten.
     * 
     * @param fileToWrite - The file to write the buffer to.
     * @param buffer - A String buffer containing the contents of the file
     */
    public static File createFile(File fileToWrite, StringBuffer buffer) {
        return createFile(fileToWrite, buffer, false);
    }

    /**
     * This writes a StringBuffer out to the required file.
     * 
     * @param fileToWrite - The file to write the buffer to.
     * @param buffer - A String buffer containing the contents of the file
     * @param append - Whether we should be appending to the file or overwriting it.
     */
    public static File createFile(File fileToWrite, StringBuffer buffer, boolean append) {
        FileOutputStream fos = null;
        try {
            //make any non-existent dirs
            fileToWrite.getParentFile().mkdirs();

            fos = new FileOutputStream(fileToWrite, append);
            fos.write(buffer.toString().getBytes());
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        }
        return fileToWrite;
    }

    /**
     * This method deletes a file or directory. If a directory is passed it deletes the directory and
     * all subdirectories and their contents.
     * 
     * @param file - The file/Directory to delete.
     */
    public static void delete(File file) {
        // If this is a directory, then iterate over each element and recursively call this method for the child.
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    delete(child);
                }
            }
        }

        boolean deleted = file.delete();
        if (!deleted)
            System.out.println("Unable to delete " + file.getAbsolutePath());
    }

    /**
     * This method generates a hash for the requested file.
     * 
     * @param file - The file to hash
     * @return - A String containing the hash of the file.
     */
    public static String generateHash(File file) {
        String result = "";
        try {
            result = MD5Utils.getFileMD5String(file);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        return result;
    }

    /**
     * This method creates a jar which is symbolicName_version.jar which just contains a manifest. It adds ifix headers if required.
     * 
     * @param directory - The directory in which to put the jar file
     * @param symbolicName - The symbolic Name of the jar file
     * @param version - The String representation of the version
     * @param ifixJar - A boolean to indicate whether this is an ifix jar
     * @param testFix - A boolean to indicate whether this is a test ifix or not.
     * @return - The jar file.
     */
    public static File createJarFile(File directory, String symbolicName, String version, boolean ifixJar, boolean testFix) {

        JarOutputStream jos = null;
        FileOutputStream fos = null;

        File newJarFile = new File(directory, symbolicName + "_" + version + ".jar");

        try {
            fos = new FileOutputStream(newJarFile);
            jos = new JarOutputStream(fos);

            // Create the manifest for the bundle.
            String manifestFileName = "META-INF/MANIFEST.MF";
            //Generate the manifest using a StringBuffer.
            StringBuffer buffer = new StringBuffer();
            buffer.append("Manifest-Version: 1.0\n");
            buffer.append("Bundle-SymbolicName: " + symbolicName + "\n");
            buffer.append("Bundle-Version: " + version + "\n");

            if (ifixJar) {
                if (testFix) {
                    buffer.append("IBM-Test-Fixes: apar1\n");
                } else {
                    buffer.append("IBM-Interim-Fixes: apar2\n");
                }
            }
            buffer.append("Bundle-ManifestVersion: 2\n\n");

            // write out the jarEntry for the manifest.
            JarEntry jarEntry = new JarEntry(manifestFileName);
            jarEntry.setTime(System.currentTimeMillis());
            jos.putNextEntry(jarEntry);
            jos.write(buffer.toString().getBytes());
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            if (jos != null) {
                try {
                    jos.close();
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }

        }

        return newJarFile;
    }

    /**
     * This method generate a set with the vars args supplied.
     * 
     * @param elements The elements to be put in the set.
     * @return The set containing the elements.
     */
    public static <E> Set<E> generateSet(E... elements) {
        Set<E> result = new HashSet<E>();
        for (E element : elements)
            result.add(element);

        return result;
    }

    /**
     * This method creates a relative pathname based on an actual file and a toplevel dir.
     * It also transforms "\"'s to "/"'s.
     * 
     * So actual file c:\test\installroot\lib\file1.xml and topLevelDir of c:\test\installroot
     * would return a value of lib/file1.xml.
     * 
     * @param actualFile - The actual file we're creating a relative pathname for.
     * @param topLevelDir - The Top level Dir that we remove from the actual pathname.
     * @return - A relative pathname from the toplevel dir.
     */
    public static String createRelativePath(File actualFile, File topLevelDir) {
        String relativeFileName = actualFile.getAbsolutePath().substring(topLevelDir.getAbsolutePath().length() + 1);
        if (relativeFileName.contains("\\"))
            relativeFileName = relativeFileName.replace("\\", "/");

        return relativeFileName;
    }

    /**
     * This method generates a test feature with the required subsystem content headers
     * 
     * @param featureFile - The actual Feature File.
     * @param featureName - the name of the feature
     * @param subsystemContent - A Map of subsystem content.The key is the location string, and the value is the file name.
     * @return - The file for the feature.
     */
    public static File createFeature(File featureFile, String featureName, Map<String, String> subsystemContent) {
        // Create test feature
        StringBuffer featureContents = new StringBuffer();
        featureContents.append("Subsystem-ManifestVersion: 1\n");
        featureContents.append("IBM-ShortName: " + featureName + "\n");
        featureContents.append("Subsystem-Type: osgi.subsystem.feature\n");
        featureContents.append("Subsystem-SymbolicName: com.ibm.websphere.appserver." + featureName + "\n");
        featureContents.append("Subsystem-Version: 1.0.0\n");
        featureContents.append("IBM-Feature-Version: 2\n");
        boolean writeHeader = true;
        // Iterate over the subsystem content and add it to the header. We need to ensure that the 1st entry is prefixed with the header
        // name and all entries have ,\n unless it is the last entry, which just has \n.
        for (Map.Entry<String, String> entry : subsystemContent.entrySet()) {
            if (writeHeader) {
                featureContents.append("Subsystem-Content:");
                writeHeader = false;
            } else {
                featureContents.append(",\n");
            }
            String location = entry.getKey();
            String fileName = entry.getValue();
            if (fileName.endsWith(".jar")) {
                String[] jarParts = fileName.split("_");
                if (fileName.contains("static")) {
                    featureContents.append(" " + jarParts[0] + "; location:=\"" + location + "\"; type=\"jar\"");
                } else {
                    featureContents.append(" " + jarParts[0] + "; version=\"[1,1.0.100)\"");
                }
            } else {
                featureContents.append(" " + fileName + "; location:=\"" + location + "\"; type=\"file\"");
            }
        }
        featureContents.append("\n");

        return createFile(featureFile, featureContents, false);
    }

    /**
     * This method refreshes the Bundle repository that is used to identify files that are within the liberty runtime.
     * We have to refresh this because if we add a file to the runtime, it won't get picked up unless the repo is cleaned and
     * it re-reads the dirs. This is effectively bouncing the liberty server.
     * 
     * @return
     */
    public static ContentBasedLocalBundleRepository refreshBundleRepository() {

        clearBundleRepositoryHolders();
        BundleRepositoryRegistry.initializeDefaults("server1", true);
        return BundleRepositoryRegistry.getInstallBundleRepository();
    }

    /**
     * This method clears the existing repositoryHolders from the BundleRepositoryRegistry. This behaves as though we were bouncing the
     * Liberty server.
     */
    public static void clearBundleRepositoryHolders() {
        // Remove any existing and recreate the bundle repository that will work out the latest versions of the jars.
        try {
            Field repositoryHoldersField = BundleRepositoryRegistry.class.getDeclaredField("repositoryHolders");
            repositoryHoldersField.setAccessible(true);
            Map<String, BundleRepositoryHolder> m = (Map<String, BundleRepositoryHolder>) repositoryHoldersField.get(null);
            m.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(File originalFile, File destFile) {
        copyFile(originalFile, destFile, false);
    }

    /**
     * This method copies the original file to the new destination.
     * 
     * @param originalFile - The file to copy
     * @param destFile - The location of the new file.
     */
    public static void copyFile(File originalFile, File destFile, boolean appendToDest) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(originalFile);
            fos = new FileOutputStream(destFile, appendToDest);

            byte[] bytes = new byte[4096];
            int readBytes;
            while ((readBytes = fis.read(bytes)) >= 0) {
                fos.write(bytes, 0, readBytes);
            }
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
