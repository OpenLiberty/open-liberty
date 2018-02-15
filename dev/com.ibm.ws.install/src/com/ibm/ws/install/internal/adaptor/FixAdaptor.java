/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.internal.adaptor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.internal.Product;
import com.ibm.ws.install.internal.asset.FixAsset;
import com.ibm.ws.product.utility.extension.IFixUtils;
import com.ibm.ws.product.utility.extension.ifix.xml.BundleFile;
import com.ibm.ws.product.utility.extension.ifix.xml.Bundles;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.product.utility.extension.ifix.xml.LibertyProfileMetadataFile;
import com.ibm.ws.product.utility.extension.ifix.xml.UpdatedFile;

public class FixAdaptor {

    private static Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);

    public static int install(Product product, FixAsset fixAsset) throws IOException, InstallException {

        File java = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        final String[] runtimeCmd = { java.getAbsolutePath(), "-jar", fixAsset.getJarPath(), product.getInstallDir().getAbsolutePath() };
        logger.log(Level.FINEST, java.getAbsolutePath() + " -jar " + fixAsset.getJarPath() + " " + product.getInstallDir().getAbsolutePath());
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(runtimeCmd, null, product.getInstallDir());

        StringBuffer stdout = new StringBuffer();
        Thread stderrCopier = new Thread(new InstallUtils.OutputStreamCopier(process.getErrorStream(), stdout));
        stderrCopier.start();
        new InstallUtils.OutputStreamCopier(process.getInputStream(), stdout).run();
        try {
            stderrCopier.join();
            process.waitFor();
            if (process.exitValue() != 0) {
                // TO DO
                // throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_INSTALL_FIX", fixAsset.id));
            }
        } catch (InterruptedException e) {
            throw new InstallException(e.getMessage(), e.getCause(), InstallException.RUNTIME_EXCEPTION);
        } finally {
            logger.log(Level.FINEST, "stdout/stderr: " + stdout.toString());
            logger.log(Level.FINEST, "exit code: " + process.exitValue());
        }
        return process.exitValue();
    }

    public static void uninstallFix(IFixInfo fixInfo, File baseDir, List<File> filesRestored) throws ParserConfigurationException, IOException, SAXException {
        FileInputStream fis = null;
        boolean success = false;
        try {
            Set<UpdatedFile> updatedFiles = fixInfo.getUpdates().getFiles();
            for (UpdatedFile file : updatedFiles) {
                File filePath = new File(baseDir, file.getId());
                InstallUtils.delete(filePath);
            }
            File backupFile = new File(baseDir, "/lib/fixes/" + fixInfo.getId() + "_" + fixInfo.getVersion() + ".zip");
            if (backupFile.exists() && backupFile.length() > 0) {
                ZipFile backupZip = new ZipFile(backupFile);
                Enumeration<? extends ZipEntry> entries = backupZip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry ze = entries.nextElement();
                    long timeStamp = ze.getTime();

                    File restoreFile = new File(baseDir, ze.getName());
                    extract(restoreFile, backupZip.getInputStream(ze));
                    if (restoreFile.exists()) {
                        logger.log(Level.FINEST, "uninstall " + fixInfo.getId() + " restored " + restoreFile.getAbsolutePath());
                        filesRestored.add(restoreFile);
                        InstallUtils.setLastModifiedTime(new File(baseDir, ze.getName()), timeStamp);
                    } else {
                        logger.log(Level.FINEST, "uninstall " + fixInfo.getId() + " failed to restore " + restoreFile.getAbsolutePath());
                    }
                }
                backupZip.close();
            }
            success = true;
        } finally {
            InstallUtils.close(fis);
            if (success) {
                InstallUtils.delete(new File(baseDir, "/lib/fixes/" + fixInfo.getId() + "_" + fixInfo.getVersion() + ".xml"));
                InstallUtils.delete(new File(baseDir, "/lib/fixes/" + fixInfo.getId() + "_" + fixInfo.getVersion() + ".zip"));
                InstallUtils.delete(new File(baseDir, "/lib/fixes/" + fixInfo.getId() + "_" + fixInfo.getVersion() + ".lpmf"));
            }

        }
    }

    public static Set<LibertyProfileMetadataFile> getInstalledLibertyProfileMetadataFiles(File baseDir) {
        return IFixUtils.getInstalledLibertyProfileMetadataFiles(baseDir, new InstallUtils.InstallCommandConsole());
    }

    public static Set<IFixInfo> getInstalledIFixes(File baseDir) {
        return IFixUtils.getInstalledIFixes(baseDir, new InstallUtils.InstallCommandConsole());
    }

    public static void removeFilesFromBackup(File baseDir, Collection<File> fileSet) throws ZipException, IOException {
        Set<IFixInfo> ifixInfoSet = getInstalledIFixes(baseDir);
        HashSet<String> fileEntrySet = new HashSet<String>();
        for (File f : fileSet) {
            if (f != null) {
                fileEntrySet.add(InstallUtils.getRelativePath(baseDir, f));
            }
        }
        for (IFixInfo iFixInfo : ifixInfoSet) {
            File backupFile = new File(baseDir, "/lib/fixes/" + iFixInfo.getId() + "_" + iFixInfo.getVersion() + ".zip");
            if (backupFile.exists()) {
                InstallUtils.deleteZipEntry(backupFile, fileEntrySet);
            }
        }
    }

    public static Set<IFixInfo> getUninstallFixInfo(File baseDir, Collection<File> fileSet) {
        HashSet<IFixInfo> fixInfo = new HashSet<IFixInfo>();
        Set<IFixInfo> ifixInfoSet = getInstalledIFixes(baseDir);
        for (IFixInfo iFixInfo : ifixInfoSet) {
            Set<UpdatedFile> updateFiles = iFixInfo.getUpdates().getFiles();
            HashSet<String> updateFileSet = new HashSet<String>();
            HashSet<String> toBeRemovedFileSet = new HashSet<String>();
            // Get the fix files which are going to be removed by the feature uninstallation
            for (UpdatedFile updatedFile : updateFiles) {
                updateFileSet.add(updatedFile.getId());
                for (File file : fileSet) {
                    if (file != null && updatedFile.getId().equals(InstallUtils.getRelativePath(baseDir, file))) {
                        toBeRemovedFileSet.add(updatedFile.getId());
                        break;
                    }
                }
            }
            // Only consider the ifix which updates the uninstalling feature
            if (toBeRemovedFileSet.size() > 0) {
                // Determine the remaining fix files exist or not.  If the remaining fix files are already removed, add the fix to uninstall.
                updateFileSet.removeAll(toBeRemovedFileSet);
                boolean uninstallFix = true;
                if (updateFileSet.size() > 0) {
                    for (String updateFile : updateFileSet) {
                        if (new File(baseDir, updateFile).exists()) {
                            uninstallFix = false;
                            break;
                        }
                    }
                }
                if (uninstallFix) {
                    fixInfo.add(iFixInfo);
                }
            }
        }
        return fixInfo;
    }

    public static HashMap<String, File> getBundleFiles(File baseDir, Set<String> symbolicNames) {
        HashMap<String, File> bundleFileList = new HashMap<String, File>();
        Set<LibertyProfileMetadataFile> lpmf = getInstalledLibertyProfileMetadataFiles(baseDir);
        for (LibertyProfileMetadataFile chklpmf : lpmf) {
            Bundles bundles = chklpmf.getBundles();
            if (bundles != null) {
                List<BundleFile> bundleEntries = bundles.getBundleFiles();
                if (bundleEntries != null) {
                    for (BundleFile bundleFile : bundleEntries) {
                        for (String symbolicName : symbolicNames) {
                            if (symbolicName.equals(bundleFile.getSymbolicName())) {
                                bundleFileList.put(symbolicName, new File(baseDir, bundleFile.getId()));
                            }
                        }
                    }
                }
            }
        }
        return bundleFileList;
    }

    private static void extract(File targetFile, InputStream zis) throws IOException {
        byte[] buf = new byte[4096];
        OutputStream os = new BufferedOutputStream(new FileOutputStream(targetFile, false));
        try {
            for (int read; (read = zis.read(buf)) != -1;) {
                os.write(buf, 0, read);
            }
        } finally {
            InstallUtils.close(os);
        }
    }

    private static void isFileLocked(String fix, File f) throws InstallException {
        InstallUtils.isFileLocked("ERROR_UNINSTALL_FIX_FILE_LOCKED", fix, f);
    }

    public static void preCheck(IFixInfo fixInfo, File baseDir) throws InstallException {
        Set<UpdatedFile> updatedFiles = fixInfo.getUpdates().getFiles();
        for (UpdatedFile file : updatedFiles) {
            File filePath = new File(baseDir, file.getId());
            isFileLocked(fixInfo.getId(), filePath);
        }
        File backupFile = new File(baseDir, "/lib/fixes/" + fixInfo.getId() + "_" + fixInfo.getVersion() + ".zip");
        if (backupFile.exists() && backupFile.length() > 0) {
            ZipFile backupZip = null;
            try {
                backupZip = new ZipFile(backupFile);
                Enumeration<? extends ZipEntry> entries = backupZip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry ze = entries.nextElement();
                    isFileLocked(fixInfo.getId(), new File(baseDir, ze.getName()));
                }
            } catch (ZipException e) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNINSTALL_FIX_INVALID_META_DATA", fixInfo.getId()));
            } catch (IOException e) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNINSTALL_FIX_INVALID_META_DATA", fixInfo.getId()));
            } finally {
                if (backupZip != null) {
                    try {
                        backupZip.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        isFileLocked(fixInfo.getId(), new File(baseDir, "/lib/fixes/" + fixInfo.getId() + "_" + fixInfo.getVersion() + ".xml"));
        isFileLocked(fixInfo.getId(), new File(baseDir, "/lib/fixes/" + fixInfo.getId() + "_" + fixInfo.getVersion() + ".zip"));
        isFileLocked(fixInfo.getId(), new File(baseDir, "/lib/fixes/" + fixInfo.getId() + "_" + fixInfo.getVersion() + ".lpmf"));
    }
}
