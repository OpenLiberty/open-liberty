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
package com.ibm.ws.install.internal;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.asset.SubsytemEntry;
import com.ibm.ws.install.repository.download.RepositoryDownloadUtil;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.feature.internal.HashUtils;
import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.provisioning.ProductExtension;
import com.ibm.ws.kernel.provisioning.ProductExtensionInfo;
import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.connections.DirectoryRepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryBadDataException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.AttachmentResource;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.SampleResource;
import com.ibm.ws.repository.transport.client.JSONAssetConverter;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.WlpInformation;

import wlp.lib.extract.SelfExtractor;

/**
 *
 */
public class InstallUtils {
    private static final Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);
    private static final int LINE_WRAP_COLUMNS = 72;
    public static final String NEWLINE = System.getProperty("line.separator");
    public static final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    public static final String SERVER_DIR_NAME = "servers";
    public static final String SERVER_XML = "server.xml";
    public static final List<String> ALL_EDITIONS = Arrays.asList("BASE", "LIBERTY_CORE", "DEVELOPERS", "EXPRESS", "ND",
                                                                  "ZOS");
    public static final String DEFAULT_TO_EXTENSION = "default";

    public static final File getServersDir() {
        return new File(Utils.getUserDir(), SERVER_DIR_NAME);
    }

    public static final boolean isFileInInstallDirectory(File file) throws IOException {
        return (null != file) && file.getCanonicalPath().startsWith(Utils.getInstallDir().getCanonicalPath());
    }

    public static boolean serverExists(final String serverName) {
        return new File(InstallUtils.getServersDir(), serverName).isDirectory();
    }

    public static interface FileWriter {
        void writeToFile(File fileToWrite) throws IOException;

        void writeToFile(File fileToWrite, String charsetName) throws IOException;
    }

    public static class InputStreamFileWriter implements FileWriter {

        private final InputStream inputStream;
        private final String charsetName;

        public InputStreamFileWriter(InputStream inputStream) {
            this(inputStream, null);
        }

        public InputStreamFileWriter(InputStream inputStream, String charsetName) {
            this.inputStream = inputStream;
            this.charsetName = (null == charsetName || charsetName.isEmpty()) ? null : charsetName;
        }

        @Override
        public void writeToFile(File fileToWrite) throws IOException {
            if (null == charsetName) {
                write(fileToWrite);
            } else {
                writeAndConvertFile(fileToWrite, null);
            }
        }

        @Override
        public void writeToFile(File fileToWrite, String charsetName) throws IOException {
            if (null != charsetName && !charsetName.isEmpty()) {
                writeAndConvertFile(fileToWrite, charsetName);
            } else {
                writeToFile(fileToWrite);
            }
        }

        private void write(final File fileToWrite) throws IOException {
            FileOutputStream fOut = null;
            try {
                try {
                    fOut = AccessController.doPrivileged(
                                                         new PrivilegedExceptionAction<FileOutputStream>() {
                                                             @Override
                                                             public FileOutputStream run() throws FileNotFoundException {
                                                                 return new FileOutputStream(fileToWrite);
                                                             }
                                                         });
                } catch (PrivilegedActionException e) {
                    // Creating a FileInputStream can only return a FileNotFoundException
                    throw new IOException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNABLE_TO_CREATE_FILE", fileToWrite.getAbsolutePath()), e);
                }
                byte[] buffer = new byte[1024 * 4];
                int len;

                while ((len = inputStream.read(buffer)) != -1) {
                    fOut.write(buffer, 0, len);
                }

                fOut.close();
                inputStream.close();
            } finally {
                close(fOut);
                close(inputStream);
            }
        }

        private void writeAndConvertFile(File fileToWrite, String toCharsetName) throws IOException {
            Writer fWriter = null;
            BufferedReader bReader = null;

            try {
                fWriter = (null == toCharsetName) ? new OutputStreamWriter(new FileOutputStream(fileToWrite)) : new OutputStreamWriter(new FileOutputStream(fileToWrite), toCharsetName);
                bReader = (null == charsetName) ? new BufferedReader(new InputStreamReader(inputStream)) : new BufferedReader(new InputStreamReader(inputStream, charsetName));
                char[] buf = new char[1024 * 4];

                for (int read; (read = bReader.read(buf)) != -1;) {
                    fWriter.write(buf, 0, read);
                }
            } finally {
                close(fWriter);
                close(bReader);
            }
        }
    }

    public static class OutputStreamCopier implements Runnable {
        private final InputStream in;
        private final StringBuffer output;

        public OutputStreamCopier(InputStream in, StringBuffer output) {
            this.in = in;
            this.output = output;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                for (String line; (line = reader.readLine()) != null;) {
                    output.append(line);
                    output.append("\n");
                }
            } catch (IOException ex) {
                throw new Error(ex);
            }
        }
    }

    public static void close(Closeable toClose) {
        if (toClose != null) {
            try {
                toClose.close();
            } catch (IOException e) {
                // ignore we are trying to close.
            }
        }
    }

    public static void close(ZipFile zip) {
        if (zip != null) {
            try {
                zip.close();
            } catch (IOException e) {
                // ignore we are trying to close.
            }
        }
    }

    public static void delete(List<File> files) {
        List<File> directories = new ArrayList<File>();
        for (File f : files) {
            if (f.isFile()) {
                logger.log(Level.FINEST, "Deleteing file " + f.getAbsolutePath());
                delete(f);
            } else {
                directories.add(f);
            }
        }
        for (File d : directories) {
            logger.log(Level.FINEST, "Deleteing directory " + d.getAbsolutePath());
            deleteDirectory(d);
        }
    }

    public static void getAllFiles(File dir, List<File> fileList) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    getAllFiles(file, fileList);
                } else if (file.isFile()) {
                    fileList.add(file);
                }
            }
        }
    }

    public static boolean deleteDirectory(final File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if (!file.delete()) {
                            logger.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_CANNOT_DELETE_FILE",
                                                                                                  file.getAbsolutePath()));
                            file.deleteOnExit();
                        }
                    }
                }
            }

            return (directory.delete());
        }

        return false;
    }

    public static boolean mkdirs(List<File> installedFiles, File file) {
        boolean result = true;
        if (!file.exists()) {
            result = mkdirs(installedFiles, file.getParentFile());
            if (result) {
                if (file.mkdir()) {
                    if (installedFiles != null) {
                        installedFiles.add(file);
                    }
                } else {
                    result = false;
                }
            }
        }

        return result;
    }

    public static boolean setLastModifiedTime(File f, long time) {
        return f.setLastModified(time);
    }

    public static DateFormat getDateFormat() {
        String pattern;
        int patternLength;
        int endOfSecsIndex;
        // Retrieve a standard Java DateFormat object with desired format.
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
        if (formatter instanceof SimpleDateFormat) {
            // Retrieve the pattern from the formatter, since we will need to
            // modify it.
            SimpleDateFormat sdFormatter = (SimpleDateFormat) formatter;
            pattern = sdFormatter.toPattern();
            // Append milliseconds and timezone after seconds
            patternLength = pattern.length();
            endOfSecsIndex = pattern.lastIndexOf('s') + 1;
            String newPattern = pattern.substring(0, endOfSecsIndex) + ":SSS z";
            if (endOfSecsIndex < patternLength)
                newPattern += pattern.substring(endOfSecsIndex, patternLength);
            // 0-23 hour clock (get rid of any other clock formats and am/pm)
            newPattern = newPattern.replace('h', 'H');
            newPattern = newPattern.replace('K', 'H');
            newPattern = newPattern.replace('k', 'H');
            newPattern = newPattern.replace('a', ' ');
            newPattern = newPattern.trim();
            sdFormatter.applyPattern(newPattern);
            formatter = sdFormatter;
        } else {
            formatter = new SimpleDateFormat("yy.MM.dd HH:mm:ss:SSS z");
        }
        return formatter;
    }

    public static void fixLogger(PrintWriter log, String fixID, String message) {
        Date d = new Date();
        String dateString = getDateFormat().format(d);
        log.println("[" + dateString + "] " + fixID + " " + message);
        log.flush();
    }

    public static File download(File installTempDir, RepositoryResource resource) throws IOException, RepositoryBackendException, RepositoryResourceException, InstallException {
        if (!installTempDir.exists()) {
            installTempDir.mkdir();
        }
        File targetFile = null;
        if (resource.getType().equals(ResourceType.FEATURE))
            targetFile = File.createTempFile("feature", ".esa", installTempDir);
        else if (resource.getType().equals(ResourceType.IFIX))
            targetFile = File.createTempFile("ifix", ".jar", installTempDir);
        else if (resource.getType().equals(ResourceType.PRODUCTSAMPLE))
            targetFile = File.createTempFile("sample", ".jar", installTempDir);
        else if (resource.getType().equals(ResourceType.OPENSOURCE))
            targetFile = File.createTempFile("osi", ".jar", installTempDir);
        else
            return null;
        try {
            new InputStreamFileWriter(resource.getMainAttachment().getInputStream()).writeToFile(targetFile);
        } catch (RepositoryBadDataException e) {
            InstallUtils.delete(targetFile);
            throw e;
        } catch (RepositoryBackendException e) {
            InstallUtils.delete(targetFile);
            throw e;
        } catch (RepositoryResourceException e) {
            InstallUtils.delete(targetFile);
            throw e;
        }

        validateDownloaded(targetFile, resource);

        return targetFile;
    }

    public static void download(URL url, File targetFile) throws IOException {
        download(url.openStream(), targetFile);
    }

    public static void download(InputStream is, File targetFile) throws IOException {
        new InputStreamFileWriter(is).writeToFile(targetFile);
    }

    public static boolean deleteZipEntry(File zipFile, Set<String> files) throws IOException {
        File tempFile = File.createTempFile(zipFile.getName(), null);
        delete(tempFile);
        if (!zipFile.renameTo(tempFile)) {
            return false;
        }
        byte[] buf = new byte[1024];
        ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipFile));
        ZipEntry entry = zin.getNextEntry();
        while (entry != null) {
            boolean toBeDeleted = false;
            for (String f : files) {
                if (f.equals(entry.getName())) {
                    toBeDeleted = true;
                    break;
                }
            }
            if (!toBeDeleted) {
                ZipEntry newEntry = new ZipEntry(entry.getName());
                newEntry.setTime(entry.getTime());
                zout.putNextEntry(newEntry);
                int len;
                while ((len = zin.read(buf)) > 0) {
                    zout.write(buf, 0, len);
                }
            }
            entry = zin.getNextEntry();
        }
        close(zin);
        close(zout);
        delete(tempFile);
        return true;
    }

    public static String getRelativePath(File baseDir, File file) {
        String installRoot = baseDir.getAbsolutePath().replace("\\", "/");
        String fileRelativePath = file.getAbsolutePath().replace("\\", "/");
        fileRelativePath = fileRelativePath.replace(installRoot + "/", "");
        return fileRelativePath;
    }

    public static String getFileContents(File file) throws IOException {
        if (file != null) {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            byte[] s = null;
            s = new byte[(int) randomAccessFile.length()];
            randomAccessFile.readFully(s);
            randomAccessFile.close();
            return new String(s);
        }
        return null;
    }

    public static void updateFingerprint(File baseDir) throws IOException {
        java.io.FileWriter fw = null;
        try {
            // alter the content of the service.fingerprint
            File f = new File(baseDir, "lib/versions/service.fingerprint");
            fw = new java.io.FileWriter(f, false);
            String data = "" + System.nanoTime();
            fw.append(data);
        } finally {
            InstallUtils.close(fw);
        }
    }

    public static File canRename(List<File> files) {
        for (File f : files) {
            if (!!!f.renameTo(f)) {
                return f;
            }
        }
        return null;
    }

    public static boolean canRename(File file) {
        return !file.exists() || file.renameTo(file);
    }

    public static void isFileLocked(String msgKey, String fix, File f) throws InstallException {
        if (!canRename(f)) {
            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(1000);
                    logger.log(Level.FINEST, "Slept a second for the system to unlock " + f.getAbsolutePath());
                } catch (InterruptedException e) {
                }
                if (canRename(f))
                    return;
            }

            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage(msgKey, fix, f.getAbsolutePath()));
        }
    }

    public static boolean isFileSame(File targetFile, String symbolicName, Version version, String inputChecksum) {
        String targetFileName = targetFile.getAbsolutePath();
        targetFileName = targetFileName.replace("\\", "/");
        if (targetFileName.contains("/lib/features/l10n/") ||
            targetFileName.contains("/lafiles/") ||
            targetFileName.contains("/checksums/")) {
            // skip check md5
            return true;
        }
        if (inputChecksum != null) {
            try {
                String fileMD5 = HashUtils.getFileMD5String(targetFile);
                return fileMD5.equals(inputChecksum);
            } catch (IOException e) {
                logger.log(Level.FINEST, "Failed to get md5 for " + targetFile.getAbsolutePath(), e);
            }
        }
        return false;
    }

    public static class InstallCommandConsole implements CommandConsole {

        @Override
        public boolean isInputStreamAvailable() {
            return false;
        }

        @Override
        public String readMaskedText(String prompt) {
            return null;
        }

        @Override
        public String readText(String prompt) {
            return null;
        }

        @Override
        public void printInfoMessage(String message) {}

        @Override
        public void printlnInfoMessage(String message) {}

        @Override
        public void printErrorMessage(String errorMessage) {}

        @Override
        public void printlnErrorMessage(String errorMessage) {}

    }

    public static File download(RepositoryResource resource, File targetDirectory) throws IOException, RepositoryResourceException, RepositoryBackendException, InstallException {
        File targetFile = null;
        String name = null;
        if (resource.getType().equals(ResourceType.FEATURE)) {
            name = ((EsaResource) resource).getProvideFeature();
            targetFile = new File(targetDirectory, name + ".esa");
        } else if (resource.getType().equals(ResourceType.IFIX)) {
            name = resource.getName();
            targetFile = new File(targetDirectory, name + ".jar");
        } else
            return null;
        if (targetFile.exists()) {
            throw new InstallException(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.file.exists", targetFile), InstallException.IO_FAILURE);
        }
        new InputStreamFileWriter(resource.getMainAttachment().getInputStream()).writeToFile(targetFile);
        validateDownloaded(targetFile, resource);
        return targetFile;
    }

    public static Collection<String> getShortNames(Map<String, ProvisioningFeatureDefinition> installedFeatures, Collection<String> featureNames) {
        Collection<String> shortNames = new ArrayList<String>(featureNames.size());
        for (String featureName : featureNames) {
            String shortFeatureName = null;
            for (ProvisioningFeatureDefinition pfd : installedFeatures.values()) {
                String ibmShortName = InstallUtils.getShortName(pfd);
                if (ibmShortName != null && ibmShortName.equalsIgnoreCase(featureName)) {
                    shortFeatureName = ibmShortName;
                    break;
                }
            }
            shortNames.add(shortFeatureName == null ? featureName : shortFeatureName);
        }
        return shortNames;
    }

    public static void wordWrap(StringBuffer sb, String line, String indentStr) {

        if (line.length() == 0) {
            return;
        }

        // Split a more complicated line...
        String indent = "";
        for (int begin = 0; begin < line.length();) {
            // ??? Java has no wcwidth (Unicode TR#11), so we assume
            // all code points have a console width of 1.
            // ??? This code assumes all characters are BMP.

            // Does the rest of the string fit in a single line?
            if (begin + LINE_WRAP_COLUMNS >= line.length()) {
                sb.append(indent);
                sb.append(line.substring(begin));
                sb.append("\n");
                break;
            }

            // Choose a split point.
            int tryEnd = Math.min(line.length(), begin + LINE_WRAP_COLUMNS);

            // If we're in the middle of a word, find the beginning.
            int end = tryEnd;
            while (end > begin && !Character.isWhitespace(line.charAt(end - 1))) {
                end--;
            }

            // Skip preceding whitespace.
            while (end > begin && Character.isWhitespace(line.charAt(end - 1))) {
                end--;
            }

            // If we couldn't find a preceding split point, then this
            // is a really long word (e.g., a URL).  Find the end of
            // the word and add it without splitting.
            if (end == begin) {
                end = tryEnd;
                while (end < line.length() && !Character.isWhitespace(line.charAt(end))) {
                    end++;
                }
            }

            sb.append(indent);
            sb.append(line.substring(begin, end));
            sb.append("\n");
            indent = indentStr;

            // Skip whitespace and find the beginning of the next word.
            begin = end;
            while (begin < line.length() && Character.isWhitespace(line.charAt(begin))) {
                begin++;
            }
        }
    }

    public static String getShortName(ProvisioningFeatureDefinition fd) {
        String shortName = fd.getIbmShortName();
        if (shortName != null)
            return shortName;
        shortName = fd.getHeader("IBM-ShortName");
        if (shortName != null)
            return shortName;
        String symbolicName = fd.getSymbolicName();
        String prefix = "com.ibm.websphere.appserver.";
        if (symbolicName.startsWith(prefix))
            return symbolicName.substring(prefix.length());
        return null;
    }

    public static Collection<String> getFeatures(InputStream serverXMLInputStream, String xml) throws InstallException {
        Collection<String> features = new ArrayList<String>();
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(serverXMLInputStream);
            Element element = doc.getDocumentElement();
            NodeList fmList = element.getElementsByTagName("featureManager");
            for (int i = 0; i < fmList.getLength(); i++) {
                Node fm = fmList.item(i);
                Element fmElement = (Element) fm;
                NodeList fList = fmElement.getElementsByTagName("feature");
                for (int j = 0; j < fList.getLength(); j++) {
                    Node f = fList.item(j);
                    features.add(f.getTextContent().trim());
                }
            }
        } catch (Exception e) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_INVALID_SERVER_XML", xml, e.getMessage()), e, InstallException.IO_FAILURE);
        }
        return features;
    }

    /**
     * Removes the "[", "]", "," characters from the ArrayList string
     *
     * @param featureList A collection of the feature names
     * @return A string with the features separated by space
     */
    public static String getFeatureListOutput(Collection<String> featureList) {
        StringBuffer sb = new StringBuffer();
        for (String output : featureList)
            sb.append(output + " ");
        return sb.toString().trim();
    }

    public static void validateProductMatches(ProvisioningFeatureDefinition fd, File installDir) throws InstallException {
        String appliesToHeader = fd.getHeader("IBM-AppliesTo");
        @SuppressWarnings("rawtypes")
        List productMatchers = SelfExtractor.parseAppliesTo(appliesToHeader);
        String errMsg = ExceptionUtils.validateProductMatches(fd.getFeatureName(), productMatchers, installDir, false);
        if (!errMsg.isEmpty()) {
            throw ExceptionUtils.create(errMsg, InstallException.NOT_VALID_FOR_CURRENT_PRODUCT);
        }
    }

    public static String getFeatureName(File esaFile) throws InstallException {
        if (!esaFile.exists())
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_INSTALL_ESA_FILE_NOTEXIST", esaFile.getAbsolutePath()), InstallException.BAD_ARGUMENT);
        if (esaFile.isDirectory())
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_DEPOLY_DIRECTORY", esaFile.getAbsolutePath()), InstallException.BAD_ARGUMENT);
        try {
            ZipFile zip = new ZipFile(esaFile);
            SubsytemEntry subsystemEntry = new SubsytemEntry(zip);
            ZipEntry zipEntry = subsystemEntry.getSubsystemEntry();
            if (zipEntry != null) {
                SubsystemFeatureDefinitionImpl featureDefinition = new SubsystemFeatureDefinitionImpl(InstallConstants.TO_USER, zip.getInputStream(zipEntry));
                String featureName = featureDefinition.getSymbolicName();
                if (featureName != null && !featureName.isEmpty()) {
                    validateProductMatches(featureDefinition, Utils.getInstallDir());
                    return featureName;
                }
            }
        } catch (IOException e) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_INVALID_LOCAL_ESA", esaFile.getAbsolutePath()), InstallException.IO_FAILURE);
        }
        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_INVALID_LOCAL_ESA", esaFile.getAbsolutePath()), InstallException.BAD_ARGUMENT);
    }

    public static boolean contains(Collection<RepositoryResource> mrList, RepositoryResource massiveResource) {
        for (RepositoryResource mr : mrList) {
            if (massiveResource instanceof EsaResource && mr instanceof EsaResource) {
                if (((EsaResource) mr).getProvideFeature().equals(((EsaResource) massiveResource).getProvideFeature()))
                    return true;
            } else if (massiveResource instanceof SampleResource && mr instanceof SampleResource) {
                if (((SampleResource) mr).getShortName().equalsIgnoreCase(((SampleResource) massiveResource).getShortName()))
                    return true;
            }
        }
        return false;
    }

    public static void log(List<? extends RepositoryResource> mrList) {
        for (RepositoryResource mr : mrList) {
            ResourceType type = mr.getType();
            String logMsg = type + ": ";
            if (ResourceType.FEATURE.equals(type)) {
                logMsg += ((EsaResource) mr).getProvideFeature();
            } else if (ResourceType.PRODUCTSAMPLE.equals(type) || ResourceType.OPENSOURCE.equals(type)) {
                logMsg += ((SampleResource) mr).getShortName();
            }
            try {
                logMsg += " is from " + mr.getRepositoryConnection().getRepositoryLocation();
                logMsg += " at " + mr.getMainAttachment().getURL();
            } catch (Exception e) {
                logMsg += " failed to get main attachment url: " + e.getMessage();
            }
            logger.log(Level.FINEST, logMsg);
        }
    }

    public static void copyFile(final File sourceFile, final File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                try {
                    source.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
            if (destination != null) {
                try {
                    destination.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    //Java 2 security APIs for deleteOnExit
    public static void delete(final File f) {
        if (f != null && f.exists()) {
            // Why do we have to specify a return type for the run method and paramatize
            // PrivilegedExceptionAction to it, this method should have a void return type ideally.
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    if (!f.delete()) {
                        logger.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_CANNOT_DELETE_FILE",
                                                                                              f.getAbsolutePath()));
                        f.deleteOnExit();
                    }

                    return null;
                }
            });
        }
    }

    //Java 2 security APIs for FileInputStream
    public static FileInputStream getFileIputStream(final File file) throws FileNotFoundException {
        try {
            return AccessController.doPrivileged(
                                                 new PrivilegedExceptionAction<FileInputStream>() {
                                                     @Override
                                                     public FileInputStream run() throws FileNotFoundException {
                                                         return new FileInputStream(file);
                                                     }
                                                 });
        } catch (PrivilegedActionException e) {
            // Creating a FileOutputStream can only return a FileNotFoundException
            throw (FileNotFoundException) e.getCause();
        }
    }

    //Java 2 security APIs for file length
    public static long getFileLength(final File file) throws FileNotFoundException {
        try {
            return AccessController.doPrivileged(
                                                 new PrivilegedExceptionAction<Long>() {
                                                     @Override
                                                     public Long run() throws FileNotFoundException {
                                                         return file.length();
                                                     }
                                                 });
        } catch (PrivilegedActionException e) {
            // Creating a FileOutputStream can only return a FileNotFoundException
            throw (FileNotFoundException) e.getCause();
        }
    }

    //Java 2 security APIs for new URL
    public static URL getNewURL(final String string) throws MalformedURLException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {

                @Override
                public URL run() throws MalformedURLException {
                    return new URL(string);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (MalformedURLException) e.getCause();
        }
    }

    //Java 2 security APIs for new URL
    public static String getSystemProperty(final String string) throws PrivilegedActionException {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {

            @Override
            public String run() {
                return System.getProperty(string);
            }
        });

    }

    //Java 2 security APIs for file.exists()
    public static boolean isFileExist(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.exists();
            }
        });
    }

    //Java 2 security APIs for file.isDirectory()
    public static boolean isFileDirectory(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.isDirectory();
            }
        });
    }

    //Java 2 security APIs for file.mkdirs()
    public static boolean mkDirs(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.mkdirs();
            }
        });
    }

    //Java 2 security APIs for file.list()
    public static String[] list(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<String[]>() {
            @Override
            public String[] run() {
                return file.list();
            }
        });
    }

    //Java 2 security APIs for file.listFiles()
    public static File[] listFiles(final File f) {
        return AccessController.doPrivileged(new PrivilegedAction<File[]>() {
            @Override
            public File[] run() {
                return f.listFiles();
            }
        });
    }

    //Java 2 security APIs for Zipfile()
    public static ZipFile createZipFile(final File f) throws ZipException, IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<ZipFile>() {
                @Override
                public ZipFile run() throws IOException {
                    return new ZipFile(f);
                }
            });
        } catch (PrivilegedActionException e) {
            if (e.getCause() instanceof ZipException)
                throw (ZipException) e.getCause();
            throw (IOException) e.getCause();
        }
    }

    public static String getResourceId(RepositoryResource resource) {
        if (resource instanceof EsaResource) {
            EsaResource esa = (EsaResource) resource;
            String shortName = esa.getShortName();
            if (shortName != null)
                return shortName;
            return esa.getProvideFeature();
        } else if (resource instanceof SampleResource) {
            String shortName = ((SampleResource) resource).getShortName();
            if (shortName != null)
                return shortName;
        }
        return resource.getName();
    }

    public static String getResourceName(RepositoryResource resource) {
        if (resource instanceof EsaResource) {
            EsaResource esa = (EsaResource) resource;
            if (esa.getVisibility() == Visibility.INSTALL)
                return Messages.INSTALL_KERNEL_MESSAGES.getMessage("ADDON_ASSET");
            return Messages.INSTALL_KERNEL_MESSAGES.getMessage("FEATURE_ASSET");
        } else if (resource instanceof SampleResource) {
            if (resource.getType() == ResourceType.PRODUCTSAMPLE)
                return Messages.INSTALL_KERNEL_MESSAGES.getMessage("SAMPLE_ASSET");
            if (resource.getType() == ResourceType.OPENSOURCE)
                return Messages.INSTALL_KERNEL_MESSAGES.getMessage("OPENSOURCE_ASSET");
        }
        return Messages.INSTALL_KERNEL_MESSAGES.getMessage("GENERAL_ASSET");
    }

    public static void validateDownloaded(File f, RepositoryResource installResource) throws InstallException {
        if (f == null || !f.exists() || installResource == null)
            return;
        String rSHA256 = installResource.getMainAttachmentSHA256();
        logger.log(Level.FINEST, "Resource SHA256: " + rSHA256);
        if (rSHA256 != null) {
            String fSHA256 = null;
            try {
                fSHA256 = HashUtils.getFileSHA256String(f);
            } catch (IOException e) {
                String msgKey = null;
                if (installResource.getType().equals(ResourceType.FEATURE)) {
                    msgKey = "ERROR_INVALID_ESA";
                } else if (installResource.getType().equals(ResourceType.IFIX)) {
                    msgKey = "ERROR_INVALID_IFIX";
                } else if (installResource.getType().equals(ResourceType.PRODUCTSAMPLE)) {
                    msgKey = "ERROR_INVALID_SAMPLE";
                } else if (installResource.getType().equals(ResourceType.OPENSOURCE)) {
                    msgKey = "ERROR_INVALID_OPENSOURCE";
                }
                if (msgKey == null)
                    logger.log(Level.FINEST, "Invalid unknown asset: " + getResourceId(installResource));
                else
                    throw ExceptionUtils.createByKey(e, msgKey, getResourceId(installResource));
            }
            logger.log(Level.FINEST, "Downloaded file SHA256: " + fSHA256);
            if (!rSHA256.equals(fSHA256)) {
                throw ExceptionUtils.createByKey("ERROR_DOWNLOADED_ASSET_INVALID_CHECKSUM", getResourceId(installResource), InstallUtils.getResourceName(installResource));
            }
        }
    }

    public static boolean isResourceExistsInDirectory(RepositoryResource installResource, File repoDir, String mainAttachmentName, String jsonFileName) {
        String type = installResource.getType().getURLForType();
        String version = RepositoryDownloadUtil.getProductVersion(installResource);
        File assetDir = new File(new File(repoDir, type), version);
        File mainFile = new File(assetDir, mainAttachmentName);
        File jsonFile = new File(assetDir, jsonFileName);
        logger.log(Level.FINEST, "isResourceExistsInDirectory mainFile " + mainFile.getAbsolutePath() + (mainFile.exists() ? " exists" : " does not exist"));
        logger.log(Level.FINEST, "isResourceExistsInDirectory jsonFile " + jsonFile.getAbsolutePath() + (jsonFile.exists() ? " exists" : " does not exist"));
        if (!mainFile.exists() || !jsonFile.exists()) {
            return false;
        }

        String rSHA256 = installResource.getMainAttachmentSHA256();
        logger.log(Level.FINEST, "isResourceExistsInDirectory remote resource SHA256: " + rSHA256);
        if (rSHA256 == null) {
            return true;
        }

        try {
            Asset asset = JSONAssetConverter.readValue(getFileIputStream(jsonFile));
            WlpInformation info = asset.getWlpInformation();
            String fSHA256 = info.getMainAttachmentSHA256();
            logger.log(Level.FINEST, "isResourceExistsInDirectory SHA256 from local json: " + fSHA256);
            if (fSHA256 == null) {
                fSHA256 = HashUtils.getFileSHA256String(mainFile);
                logger.log(Level.FINEST, "isResourceExistsInDirectory SHA256 from local main: " + fSHA256);
            }
            return rSHA256.equals(fSHA256);
        } catch (Exception e) {
            logger.log(Level.FINEST, "isResourceExistsInDirectory failed to compare checksums", e);
        }

        return true;
    }

    public static boolean isURL(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            logger.log(Level.FINEST, "The string " + url + " is not an url.", e);
            return false;
        }
    }

    public static boolean isDirectoryBasedRepository(RepositoryResource installResource) {
        RepositoryConnection connection = installResource.getRepositoryConnection();
        return connection instanceof DirectoryRepositoryConnection;
    }

    public static File getFileDirectoryBasedRepository(RepositoryResource installResource) throws InstallException {
        RepositoryConnection connection = installResource.getRepositoryConnection();
        if (connection instanceof DirectoryRepositoryConnection) {
            AttachmentResource mainAttachment = null;
            try {
                mainAttachment = installResource.getMainAttachment();
            } catch (Exception e) {
                logger.log(Level.FINEST, "Failed to get main attachment for " + installResource.getName(), e);
                return null;
            }
            if (mainAttachment != null) {
                String mainAttachmentName = mainAttachment.getName();
                if (mainAttachmentName != null) {
                    DirectoryRepositoryConnection dirRepoConnection = (DirectoryRepositoryConnection) connection;
                    String type = installResource.getType().getURLForType();
                    String version = RepositoryDownloadUtil.getProductVersion(installResource);
                    String repoDir = dirRepoConnection.getRepositoryLocation();
                    File assetDir = new File(new File(repoDir, type), version);
                    File mainFile = new File(assetDir, mainAttachmentName);
                    logger.log(Level.FINEST, "getFileDirectoryBasedRepository mainFile " + mainFile.getAbsolutePath() + (mainFile.exists() ? " exists" : " does not exist"));
                    if (mainFile.exists()) {
                        validateDownloaded(mainFile, installResource);
                        return mainFile;
                    }
                }
            }
        }
        return null;
    }

    public static String getEditionName(String editionCode) {

        String editionCodeUpperCase = editionCode.toUpperCase();
        if (editionCodeUpperCase.equals("BASE"))
            return "";
        else if (editionCodeUpperCase.equals("BASE_ILAN"))
            return "(ILAN)";
        else if (editionCodeUpperCase.equals("DEVELOPERS"))
            return "for Developers";
        else if (editionCodeUpperCase.equals("EXPRESS"))
            return "- Express";
        else if (editionCodeUpperCase.equals("EARLY_ACCESS"))
            return "Early Access";
        else if (editionCodeUpperCase.equals("LIBERTY_CORE"))
            return "Liberty Core";
        else if (editionCodeUpperCase.equals("ND"))
            return "Network Deployment";
        else if (editionCodeUpperCase.equals("ZOS"))
            return "z/OS";
        else if (editionCodeUpperCase.equals("LIBERTY"))
            return "";
        else if (editionCodeUpperCase.startsWith("LIBERTY "))
            return editionCode.substring("LIBERTY ".length());
        else {
            return editionCode;
        }

    }

    public static boolean containsIgnoreCase(Collection<String> assetNames, String name) {
        for (String assetName : assetNames) {
            if (assetName.equalsIgnoreCase(name))
                return true;
        }
        return false;
    }

    private static void putAssetToMap(Map<String, Collection<String>> assetsMap, String key, String value) {
        Collection<String> assets = assetsMap.get(key);
        if (assets == null) {
            assets = new ArrayList<String>();
            assetsMap.put(key, assets);
        }
        assets.add(value);
    }

    private static boolean isValidExtension(String extensionId) {
        if (InstallConstants.TO_USER.equalsIgnoreCase(extensionId))
            return true;
        List<ProductExtensionInfo> extensions = ProductExtension.getProductExtensions();
        if (extensions != null) {
            for (ProductExtensionInfo extension : extensions) {
                if (extension.getProductID().equals(extensionId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Map<String, Collection<String>> getAssetsMap(Collection<String> assetNames, boolean download) throws InstallException {
        Map<String, Collection<String>> assetsMap = new HashMap<String, Collection<String>>();
        for (String assetName : assetNames) {
            if (assetName.contains(":")) {
                String[] asset = assetName.split(":", 2);
                String target = asset[0].trim();
                String aName = asset[1].trim();
                if (target.isEmpty()) {
                    if (aName.isEmpty()) {
                        logger.log(Level.FINEST, ": is an invalid asset name and is ignored.");
                    } else {
                        putAssetToMap(assetsMap, DEFAULT_TO_EXTENSION, assetName);
                    }
                } else {
                    if (aName.isEmpty()) {
                        logger.log(Level.FINEST, assetName + " is an invalid asset name and is ignored.");
                    } else {
                        if (!download && !isValidExtension(target)) {
                            throw ExceptionUtils.createByKey("ERROR_EXTENSION_FROM_ASSETID_NOT_FOUND", aName, target);
                        }
                        putAssetToMap(assetsMap, target, aName);
                    }
                }
            } else {
                putAssetToMap(assetsMap, DEFAULT_TO_EXTENSION, assetName);
            }
        }
        return assetsMap;
    }

    public static String toExtension(String target, String defaultTarget) {
        if (target.equalsIgnoreCase(DEFAULT_TO_EXTENSION))
            return defaultTarget;
        return target;
    }

}
