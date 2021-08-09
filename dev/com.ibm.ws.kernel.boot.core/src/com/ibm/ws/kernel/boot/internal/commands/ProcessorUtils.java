/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.Debug;
import com.ibm.ws.kernel.boot.archive.Archive;
import com.ibm.ws.kernel.boot.archive.ArchiveEntryConfig;
import com.ibm.ws.kernel.boot.archive.ArchiveFactory;
import com.ibm.ws.kernel.boot.archive.DirEntryConfig;
import com.ibm.ws.kernel.boot.archive.DirPattern.PatternStrategy;
import com.ibm.ws.kernel.boot.archive.FileEntryConfig;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.FileUtils;
import com.ibm.ws.kernel.boot.internal.XMLUtils;

/**
 * Provide the utilities for PackageProcessor use
 */
public class ProcessorUtils {

    /**
     * Get the loose config files from ${server.config.dir}/apps, ${server.config.dir}/dropins or ${shared.app.dir}
     *
     * @param bootProps
     * @return
     */
    public static Set<File> getLooseConfigFiles(BootstrapConfig bootProps) {
        Set<File> files = new HashSet<File>();

        List<Pattern> patterns = new ArrayList<Pattern>();
        patterns.add(Pattern.compile(".*(\\.xml)$"));
        // Add loose files under ${server.config.dir}/dropins
        File dropins = getFileFromDirectory(new File(bootProps.get(BootstrapConstants.LOC_PROPERTY_SRVCFG_DIR)), BootstrapConstants.LOC_AREA_NAME_DROP);
        if (dropins.exists()) {
            files.addAll(Arrays.asList(FileUtils.listFiles(dropins, patterns, true)));
        }
        // Add loose files under ${server.config.dir}/apps
        File apps = getFileFromDirectory(new File(bootProps.get(BootstrapConstants.LOC_PROPERTY_SRVCFG_DIR)), BootstrapConstants.LOC_AREA_NAME_APP);
        if (apps.exists()) {
            files.addAll(Arrays.asList(FileUtils.listFiles(apps, patterns, true)));
        }
        // Add loose files under ${shared.app.dir}
        File shared = new File(bootProps.get(BootstrapConstants.LOC_PROPERTY_SHARED_APP_DIR));
        if (shared.exists()) {
            files.addAll(Arrays.asList(FileUtils.listFiles(shared, patterns, true)));
        }
        return files;
    }

    /**
     * Refer to the com.ibm.ws.artifact.api.loose.internal.LooseContainerFactoryHelper.createContainer.
     * Parse the loose config file and create the looseConfig object which contains the file's content.
     *
     * @param looseFile
     * @return
     * @throws Exception
     */
    public static LooseConfig convertToLooseConfig(File looseFile) throws Exception {
        //make sure the file exists, can be read and is an xml
        if (looseFile.exists() && looseFile.isFile() && looseFile.canRead() && looseFile.getName().toLowerCase().endsWith(".xml")) {
            LooseConfig root = new LooseConfig(LooseType.ARCHIVE);

            XMLStreamReader reader = null;
            try {
                LooseConfig cEntry = root;

                Stack<LooseConfig> stack = new Stack<LooseConfig>();

                stack.push(cEntry);
                /*
                 * Normally we specifically request the system default XMLInputFactory to avoid issues when
                 * another XMLInputFactory is available on the thread context class loader. In this case it
                 * should be safe to use XMLInputFactory.newInstance() because the commands run separately
                 * from the rest of the code base.
                 */
                reader = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(looseFile));
                while (reader.hasNext() && cEntry != null) {
                    int result = reader.next();
                    if (result == XMLStreamConstants.START_ELEMENT) {
                        if ("archive".equals(reader.getLocalName())) {
                            String targetLocation = XMLUtils.getAttribute(reader, "targetInArchive");
                            if (targetLocation != null) {
                                LooseConfig pEntry = cEntry;

                                cEntry = new LooseConfig(LooseType.ARCHIVE);
                                cEntry.targetInArchive = targetLocation;

                                pEntry.children.add(cEntry);
                                //put current Loose Entry into stack
                                stack.push(cEntry);
                            }
                        } else if ("dir".equals(reader.getLocalName())) {
                            cEntry.children.add(createElement(reader, LooseType.DIR));
                        } else if ("file".equals(reader.getLocalName())) {
                            cEntry.children.add(createElement(reader, LooseType.FILE));
                        }
                    } else if (result == XMLStreamConstants.END_ELEMENT) {
                        if ("archive".equals(reader.getLocalName())) {
                            stack.pop();
                            //make sure stack isn't empty before we try and look at it
                            if (!stack.empty()) {
                                //set current entry to be the top of stack
                                cEntry = stack.peek();
                            } else {
                                cEntry = null;
                            }
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                throw e;
            } catch (XMLStreamException e) {
                throw e;
            } catch (FactoryConfigurationError e) {
                throw e;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                        reader = null;
                    } catch (XMLStreamException e) {
                        Debug.printStackTrace(e);
                    }
                }
            }
            return root;
        }
        return null;
    }

    private static LooseConfig createElement(XMLStreamReader reader, LooseType type) {
        LooseConfig lEntry = null;

        Set<String> attrNames = new HashSet<String>();
        attrNames.add("targetInArchive");
        attrNames.add("sourceOnDisk");

        if (LooseType.DIR == type) {
            attrNames.add("excludes");
        }

        lEntry = XMLUtils.createInstanceByElement(reader, LooseConfig.class, attrNames);
        lEntry.type = type;
        return lEntry;
    }

    /**
     * Method to add looseConfigs in both archive and expanded format
     *
     * @param looseConfig
     * @param looseFile
     * @param bootProps
     * @param bootProps
     * @param archiveEntryPrefix
     * @param isUsr
     * @return List<ArchiveEntryConfig>
     * @throws IOException
     */
    public static List<ArchiveEntryConfig> createLooseExpandedArchiveEntryConfigs(LooseConfig looseConfig, File looseFile, BootstrapConfig bootProps,
                                                                                  String archiveEntryPrefix, boolean isUsr) throws IOException {

        List<ArchiveEntryConfig> entryConfigs = new ArrayList<ArchiveEntryConfig>();
        String entryPath = generateBaseEntryPath(looseFile, bootProps, archiveEntryPrefix, isUsr);

        //Add loose config as archive
        File archiveFile = processArchive(looseConfig, looseFile.getName(), bootProps);
        entryConfigs.add(new FileEntryConfig(entryPath, archiveFile));

        //Add expanded folder to entry path
        int lastSlash = entryPath.lastIndexOf("/") + 1;
        if (lastSlash != 0) {
            entryPath = entryPath.substring(0, lastSlash) + "expanded/" + entryPath.substring(lastSlash);
            // Make sure if using dropins, the expanded folder goes to apps folder
            if (entryPath.indexOf("dropins") != 0) {
                entryPath = entryPath.replace("dropins", "apps");
            }
        }

        Iterator<LooseConfig> configIter = looseConfig.iteration();

        while (configIter.hasNext()) {
            LooseConfig config = configIter.next();
            try {
                entryConfigs.add(processLooseConfig(entryPath, config, bootProps));
            } catch (IllegalArgumentException | FileNotFoundException ex) {
                continue;
            }
        }
        return entryConfigs;
    }

    /**
     * Creates an ArchiveEntryConfig for the LooseConfig at the given entryPath
     *
     * @param entryPath
     * @param config
     * @param bootProps
     * @return ArchiveEntryConfig
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public static ArchiveEntryConfig processLooseConfig(String entryPath, LooseConfig config, BootstrapConfig bootProps) throws IllegalArgumentException, IOException {
        if (LooseType.ARCHIVE.equals(config.type)) { // archive tag
            File archiveFile = processArchive(config, null, bootProps);
            return new FileEntryConfig(createLooseConfigEntryPath(config, entryPath), archiveFile);

        } else if (LooseType.DIR.equals(config.type)) { // directory tag
            final File dir;
            DirEntryConfig dirConfig;

            try {
                dir = FileUtils.convertPathToFile(config.sourceOnDisk, bootProps);
                dirConfig = new DirEntryConfig(createLooseConfigEntryPath(config, entryPath), dir, true, PatternStrategy.ExcludePreference);
            } catch (IllegalArgumentException | FileNotFoundException ex) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("warning.unableToPackageLooseConfigFileCannotResolveLocSymbol"),
                                                        config.sourceOnDisk));
                throw ex;
            }

            if (config.excludes != null) {
                dirConfig.exclude(convertToRegex(config.excludes));
            }
            return dirConfig;

        } else { // file tag
            return new FileEntryConfig(createLooseConfigEntryPath(config, entryPath), FileUtils.convertPathToFile(config.sourceOnDisk, bootProps));
        }
    }

    /**
     * Method creates archive file with resources defined in LooseConfig
     *
     * @param looseConfig
     * @param LooseFileName
     * @param bootProps
     * @return File
     * @throws IOException
     */
    private static File processArchive(LooseConfig looseConfig, String LooseFileName, BootstrapConfig bootProps) throws IOException {
        Archive thisArchive = null;
        File archiveFile = null;

        try {
            if (LooseFileName != null) {
                // The root archive uses the LooseFileName instead of the targetInArchive property
                archiveFile = FileUtils.createTempFile(LooseFileName, null, bootProps.get(BootstrapConstants.LOC_PROPERTY_SRVTMP_DIR));
            } else {
                String fileName = looseConfig.targetInArchive.replace("\\", "/");// Make sure always use "/"
                if (fileName.endsWith("/")) {
                    fileName = fileName.substring(0, fileName.length() - 1);
                }

                int index = fileName.lastIndexOf("/");
                if (index != -1) {
                    fileName = fileName.substring(index + 1);
                }
                archiveFile = FileUtils.createTempFile(fileName, null, bootProps.get(BootstrapConstants.LOC_PROPERTY_SRVTMP_DIR));
            }

            thisArchive = ArchiveFactory.create(archiveFile);
            Iterator<LooseConfig> configIter = looseConfig.iteration();

            while (configIter.hasNext()) {
                LooseConfig config = configIter.next();
                try {
                    thisArchive.addEntryConfig(processLooseConfig("", config, bootProps));
                } catch (IllegalArgumentException | FileNotFoundException ex) {
                    continue;
                }
            }
            thisArchive.create();

            return archiveFile;
        } finally {
            Utils.tryToClose(thisArchive);
        }
    }

    /**
     * Common code for generating the entryPath
     *
     * @param looseFile
     * @param bootProps
     * @param archiveEntryPrefix
     * @param isUsr
     * @return String
     */
    private static String generateBaseEntryPath(File looseFile, BootstrapConfig bootProps, String archiveEntryPrefix, boolean isUsr) {
        File usrRoot = bootProps.getUserRoot();
        int len = usrRoot.getAbsolutePath().length();

        String entryPath = null;
        if (archiveEntryPrefix.equalsIgnoreCase(PackageProcessor.PACKAGE_ARCHIVE_PREFIX) || !isUsr) {
            entryPath = archiveEntryPrefix + BootstrapConstants.LOC_AREA_NAME_USR + looseFile.getAbsolutePath().substring(len);
        } else {
            entryPath = archiveEntryPrefix + looseFile.getAbsolutePath().substring(len);
        }

        // Fix path separators if necessary and trim '.xml' from the end
        return entryPath.replace("\\", "/").replaceAll("/(?=/)|.xml$", "");
    }

    /**
     * Adds a leading slash to the looseConfig's targetInArchive if needed,
     * and appends it to the end of the given entry path
     *
     * @param looseConfig
     * @param entryPath
     * @return String
     */
    public static String createLooseConfigEntryPath(LooseConfig looseConfig, String entryPath) {
        if (looseConfig.targetInArchive.startsWith("/") || entryPath == "") {
            entryPath += looseConfig.targetInArchive;
        } else {
            entryPath += "/" + looseConfig.targetInArchive;
        }
        return entryPath;
    }

    /**
     * Copy from com.ibm.ws.artifact.api.loose.internal.LooseArchive
     *
     * @param excludeStr
     * @return
     */
    public static Pattern convertToRegex(String excludeStr) {
        // make all "." safe decimles then convert ** to .* and /* to /.* to make it regex
        if (excludeStr.contains(".")) {
            // regex for "." is \. - but we are converting the string to a regex string so need to escape the escape slash...
            excludeStr = excludeStr.replace(".", "\\.");
        }
        //if we exclude a dir (eg /**/) we need to remove the closing slash so our regex is /.*
        if (excludeStr.endsWith("/")) {
            excludeStr = excludeStr.substring(0, excludeStr.length() - 1);
        }
        if (excludeStr.contains("**")) {
            excludeStr = excludeStr.replace("**", ".*");
        }
        if (excludeStr.contains("/*")) {
            excludeStr = excludeStr.replace("/*", "/.*");
        }
        //need to escape the file seperators correctly, as / is a regex keychar
        if (excludeStr.contains("/")) {
            excludeStr = excludeStr.replace("/", "\\/");
        }
        //at this point we should not have any **, if we do replace with * as all * should be prefixed with a .
        if (excludeStr.contains("**")) {
            excludeStr = excludeStr.replace("**", "*");
        }
        if (excludeStr.startsWith("*")) {
            excludeStr = "." + excludeStr;
        }
        if (excludeStr.contains("[")) {
            excludeStr = excludeStr.replace("[", "\\[");
        }
        if (excludeStr.contains("]")) {
            excludeStr = excludeStr.replace("]", "\\]");
        }
        if (excludeStr.contains("-")) {
            excludeStr = excludeStr.replace("-", "\\-");
        }
        return Pattern.compile(excludeStr);

    }

    public static File getFileFromDirectory(File dir, String path) {
        StringBuilder sBuilder = new StringBuilder(dir.getAbsolutePath());
        for (String p : path.split("/")) {
            sBuilder.append(File.separator).append(p);
        }

        return new File(sBuilder.toString());
    }

    protected static class LooseConfig {
        protected String targetInArchive;
        protected String sourceOnDisk;
        protected String excludes;
        protected LooseType type;

        protected List<LooseConfig> children = new ArrayList<LooseConfig>();

        public Iterator<LooseConfig> iteration() {
            return children.iterator();
        }

        public LooseConfig() {
        };

        public LooseConfig(LooseType type) {
            this.type = type;
        }
    }

    private enum LooseType {
        DIR, FILE, ARCHIVE
    }

}
