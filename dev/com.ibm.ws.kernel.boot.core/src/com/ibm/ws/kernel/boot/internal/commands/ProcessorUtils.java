/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
     * Using the method to create Loose config's Archive entry config
     *
     * @param looseConfig
     * @param looseFile
     * @param bootProps
     * @return
     * @throws IOException
     */
    public static ArchiveEntryConfig createLooseArchiveEntryConfig(LooseConfig looseConfig, File looseFile, BootstrapConfig bootProps,
                                                                   String archiveEntryPrefix) throws IOException {
        File usrRoot = bootProps.getUserRoot();
        int len = usrRoot.getAbsolutePath().length();

        String entryPath = archiveEntryPrefix + "usr" + looseFile.getAbsolutePath().substring(len);
        entryPath = entryPath.replace("\\", "/");
        entryPath = entryPath.substring(0, entryPath.length() - 4); // trim the .xml

        File archiveFile = processArchive(looseFile.getName(), looseConfig, true, bootProps);

        return new FileEntryConfig(entryPath, archiveFile);
    }

    private static File processArchive(String looseFileName, LooseConfig looseConfig, boolean isRoot, BootstrapConfig bootProps) throws IOException {
        Archive thisArchive = null;
        File archiveFile = null;

        try {
            if (isRoot) {
                archiveFile = FileUtils.createTempFile(looseFileName, null, bootProps.get(BootstrapConstants.LOC_PROPERTY_SRVTMP_DIR));
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

                if (LooseType.ARCHIVE.equals(config.type)) { // archive tag
                    File childFile = processArchive(null, config, false, bootProps);
                    thisArchive.addEntryConfig(new FileEntryConfig(config.targetInArchive, childFile));

                } else if (LooseType.DIR.equals(config.type)) {// directory tag
                    final File dir;
                    try {
                        dir = FileUtils.convertPathToFile(config.sourceOnDisk, bootProps);
                    } catch (IllegalArgumentException ex) {
                        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("warning.unableToPackageLooseConfigFileCannotResolveLocSymbol"),
                                                                config.sourceOnDisk));
                        continue;
                    }
                    DirEntryConfig dirConfig = new DirEntryConfig(config.targetInArchive, dir, true, PatternStrategy.ExcludePreference);
                    if (config.excludes != null) {
                        dirConfig.exclude(convertToRegex(config.excludes));
                    }
                    thisArchive.addEntryConfig(dirConfig);

                } else { // file tag
                    thisArchive.addEntryConfig(new FileEntryConfig(config.targetInArchive, FileUtils.convertPathToFile(config.sourceOnDisk, bootProps)));
                }
            }
            thisArchive.create();

            return archiveFile;
        } finally {
            Utils.tryToClose(thisArchive);
        }

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

        public LooseConfig() {};

        public LooseConfig(LooseType type) {
            this.type = type;
        }
    }

    private enum LooseType {
        DIR, FILE, ARCHIVE
    }

}
