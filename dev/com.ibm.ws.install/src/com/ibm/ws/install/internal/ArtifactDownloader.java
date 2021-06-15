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
package com.ibm.ws.install.internal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;

public class ArtifactDownloader {

    private final int PROGRESS_CHUNK = 500000;

    private final int BUFFER_SIZE = 10000;

    private final String appName = "artifactDownloader"; //TODO replace with openliberty?

    private final String appVersion = "1.0.0"; //TODO replace with openliberty version?

    private final List<File> downloadedFiles = new ArrayList<File>();

    private final static Logger logger = InstallLogUtils.getInstallLogger();

    private final ProgressBar progressBar = ProgressBar.getInstance();
    private static Map<String, Object> envMap = null;

    public void synthesizeAndDownloadFeatures(List<String> mavenCoords, String dLocation, MavenRepository repository) throws InstallException {
        info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_CONTACTING_MAVEN_REPO"));
        checkValidProxy();
        configureProxyAuthentication();
        configureAuthentication(repository);

        downloadedFiles.clear();
        String repo = FormatUrlSuffix(repository.getRepositoryUrl());
        List<String> featureURLs = ArtifactDownloaderUtils.acquireFeatureURLs(mavenCoords, repo);
        List<String> missingFeatures;
        dLocation = FormatPathSuffix(dLocation);
        try {
            int responseCode = ArtifactDownloaderUtils.exists(featureURLs.get(0), envMap);
            if (responseCode != 404) {
                ArtifactDownloaderUtils.checkResponseCode(responseCode, repo);
            }
        } catch (IOException e) {
            fine(e.getMessage());
            throw ExceptionUtils.createByKey("ERROR_FAILED_TO_CONNECT_MAVEN");
        }
        updateProgress(progressBar.getMethodIncrement("establishConnection"));
        info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_MAVEN_REPO_CONNECTION_SUCCESSFUL"));
        try {
            missingFeatures = ArtifactDownloaderUtils.getMissingFiles(featureURLs, envMap);
        } catch (IOException e) {
            throw new InstallException(e.getMessage());
        }
        if (!missingFeatures.isEmpty()) {

            List<String> missingFeatureList = new ArrayList<String>();
            for (String f : missingFeatures) {
                if (f.endsWith(".esa")) {
                    missingFeatureList.add(ArtifactDownloaderUtils.getFileNameFromURL(f) + ".esa");
                } else if (f.endsWith(".pom")) {
                    missingFeatureList.add(ArtifactDownloaderUtils.getFileNameFromURL(f) + ".pom");
                }
            }
            fine("The remote repository is missing the following artifacts: " + missingFeatureList.toString());
            throw ExceptionUtils.createByKey("ERROR_FAILED_TO_DOWNLOAD_ASSETS_FROM_REPO", "required", "feature(s)", repo);
        } else {
            // we have downloaded mavenCoords.length amount of features.
            double individualSize = progressBar.getMethodIncrement("downloadArtifacts") / mavenCoords.size();
            progressBar.updateMethodMap("downloadArtifact", individualSize);
            logger.info(Messages.INSTALL_KERNEL_MESSAGES.getMessage("MSG_BEGINNING_DOWNLOAD_FEATURES"));
            for (String coords : mavenCoords) {
                synthesizeAndDownload(coords, "esa", dLocation, repository, false);
                synthesizeAndDownload(coords, "pom", dLocation, repository, false);

                // update progress bar, drain the downloadArtifacts total size
                updateProgress(individualSize);
                progressBar.updateMethodMap("downloadArtifacts", progressBar.getMethodIncrement("downloadArtifacts") - individualSize);
                progressBar.manuallyUpdate();
                fine("Finished downloading artifact: " + coords);
            }
        }
    }

    /**
     * @param path
     * @return
     */
    private String FormatPathSuffix(String path) {
        String result = path;
        if (!path.endsWith(File.separator)) {
            result += File.separator;
        }
        return result;
    }

    private String FormatUrlSuffix(String url) {
        String result = url;
        if (!url.endsWith("/")) {
            result += "/";
        }
        return result;
    }

    public void synthesizeAndDownload(String mavenCoords, String filetype, String dLocation, MavenRepository repository, boolean individualDownload) throws InstallException {
        configureProxyAuthentication();
        configureAuthentication(repository);
        String repo = FormatUrlSuffix(repository.getRepositoryUrl());
        dLocation = FormatPathSuffix(dLocation);
        String groupId = ArtifactDownloaderUtils.getGroupId(mavenCoords).replace(".", "/") + "/";
        String artifactId = ArtifactDownloaderUtils.getartifactId(mavenCoords);
        String version = ArtifactDownloaderUtils.getVersion(mavenCoords);
        new File(dLocation + groupId + artifactId + "/" + version + "/").mkdirs();

        String filename = ArtifactDownloaderUtils.getfilename(mavenCoords, filetype);
        String urlLocation = ArtifactDownloaderUtils.getUrlLocation(repo, groupId, artifactId, version, filename);

        if (individualDownload) {
            checkValidProxy();
            int repoResponseCode;
            try {
                repoResponseCode = ArtifactDownloaderUtils.exists(urlLocation, envMap);
                if (repoResponseCode != 404) {
                    ArtifactDownloaderUtils.checkResponseCode(repoResponseCode, repo);
                }
            } catch (Exception e) {
                fine(e.getMessage());
                throw ExceptionUtils.createByKey("ERROR_FAILED_TO_CONNECT_MAVEN");
            }
        }
        String[] checksumFormats = new String[3];
        checksumFormats[0] = "SHA256";
        checksumFormats[1] = "MD5";
        checksumFormats[2] = "SHA1";

        try {
            if (individualDownload && ArtifactDownloaderUtils.fileIsMissing(urlLocation, envMap)) {
                throw ExceptionUtils.createByKey("ERROR_FAILED_TO_DOWNLOAD_ASSETS_FROM_REPO", ArtifactDownloaderUtils.getFileNameFromURL(urlLocation), filetype + " file", repo); //ERROR_FAILED_TO_DOWNLOAD_ASSETS_FROM_MAVEN_REPO
            } else {
                download(urlLocation, dLocation, groupId, artifactId, version, filename, checksumFormats, repository);
            }
        } catch (IOException e) {
            throw new InstallException(e.getMessage());
        }

    }

    private void download(String urlLocation, String dLocation, String groupId, String artifactId, String version, String filename,
                          String[] checksumFormats, MavenRepository mavenRepository) throws IOException, InstallException {
        try {
            URI uriLoc = new URI(urlLocation);
            File fileLoc = new File(ArtifactDownloaderUtils.getFileLocation(dLocation, groupId, artifactId, version, filename));

            downloadInternal(uriLoc, fileLoc, mavenRepository);

            downloadedFiles.add(fileLoc);
            boolean someChecksumExists = false;
            boolean checksumFail = false;
            boolean checksumSuccess = false;
            for (String checksumFormat : checksumFormats) {
                if (!checksumSuccess) {
                    if (checksumIsAvailable(urlLocation, checksumFormat)) {
                        someChecksumExists = true;
                        if (isIncorrectChecksum(fileLoc.getAbsolutePath(), urlLocation, checksumFormat)) {
                            fine("Failed to validate " + checksumFormat + " checksum for file: " + filename);
                            checksumFail = true;
                        } else {
                            checksumSuccess = true;
                            fine("Successfully validated " + checksumFormat + " checksum for file: " + filename);
                        }
                    } else {
                        fine("Failed to find " + checksumFormat + " checksum for file: " + filename);
                    }
                }
            }
            if (someChecksumExists) {
                if (checksumFail) {
                    ArtifactDownloaderUtils.deleteFiles(downloadedFiles, dLocation, groupId, artifactId, version, filename);
                    downloadedFiles.clear();
                    throw ExceptionUtils.createByKey("ERROR_CHECKSUM_FAILED_MAVEN", filename);
                }
            } else {
                fine("No checksums found for file in remote repository");
            }
        } catch (URISyntaxException e) {
            throw new InstallException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new InstallException(e.getMessage());
        }
    }

    private boolean checksumIsAvailable(String urlLocation, String checksumFormat) {
        boolean result = true;
        try {
            ArtifactDownloaderUtils.getPrimaryChecksum(urlLocation, checksumFormat);
        } catch (IOException e) {
            result = false;
        }
        return result;
    }

    private boolean isIncorrectChecksum(String localFile, String urlLocation, String checksumFormat) throws NoSuchAlgorithmException {
        boolean result = false;
        String checksumLocal;
        try {
            checksumLocal = ArtifactDownloaderUtils.getChecksum(localFile, checksumFormat);
        } catch (IOException e) {
            return true;
        }
        String checksumOrigin;
        try {
            checksumOrigin = ArtifactDownloaderUtils.getPrimaryChecksum(urlLocation, checksumFormat);
        } catch (IOException e) {
            return true;
        }
        if (!checksumLocal.equals(checksumOrigin)) {
            result = true;
        }
        return result;
    }

    private void configureProxyAuthentication() {
        //set up basic auth HTTP proxy tunnel
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");

        if (envMap.get("https.proxyUser") != null) {
            Authenticator.setDefault(new SystemPropertiesProxyAuthenticator());
        } else if (envMap.get("http.proxyUser") != null) {
            Authenticator.setDefault(new SystemPropertiesProxyHttpAuthenticator());
        }
    }

    private void configureAuthentication(final MavenRepository repository) {

        if (repository.getUserId() != null && repository.getPassword() != null &&
            envMap.get("https.proxyUser") == null && envMap.get("http.proxyUser") == null) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(repository.getUserId(), repository.getPassword().toCharArray());
                }
            });
        }
    }

    /**
     * Tests the connection of a MavenRepository. If the server returns 404, then this will return false.
     *
     * @return
     */
    protected boolean testConnection(MavenRepository repository, List<String> mavenCoords) {
        configureProxyAuthentication();
        configureAuthentication(repository);
        List<String> featureURLs = ArtifactDownloaderUtils.acquireFeatureURLs(mavenCoords, repository.getRepositoryUrl());
        try {
            int responseCode = ArtifactDownloaderUtils.exists(featureURLs.get(0), envMap);
            logger.fine("Response code: " + responseCode);
            if (responseCode != 404) {
                // repo is fine for use
                return true;
            }
        } catch (IOException e) {

        }
        return false;
    }

    /**
     * Tests the connection of a MavenRepository. If the server returns 404, then this will return false.
     *
     * @return
     */
    protected boolean testConnection(MavenRepository repository) {
        configureProxyAuthentication();
        configureAuthentication(repository);
        try {
            int responseCode = ArtifactDownloaderUtils.exists(repository.getRepositoryUrl(), envMap);
            logger.fine("Response code: " + responseCode);
            if (responseCode != 404) {
                // repo is fine for use
                return true;
            }
        } catch (IOException e) {

        }
        return false;
    }

    private void downloadInternal(URI address, File destination, MavenRepository repository) throws IOException, InstallException {
        OutputStream out = null;
        URLConnection conn;
        InputStream in = null;
        try {
            URL url = address.toURL();
            try {
                out = new BufferedOutputStream(new FileOutputStream(destination));
            } catch (FileNotFoundException e) {
                throw ExceptionUtils.createByKey("ERROR_FAILED_TO_DOWNLOAD_FEATURE", ArtifactDownloaderUtils.getFileNameFromURL(address.toString()),
                                                 destination.toString());
            }

            if (envMap.get("https.proxyHost") != null) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress((String) envMap.get("https.proxyHost"), Integer.parseInt((String) envMap.get("https.proxyPort"))));
                conn = url.openConnection(proxy);
            } else if (envMap.get("http.proxyUser") != null) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress((String) envMap.get("http.proxyHost"), Integer.parseInt((String) envMap.get("http.proxyPort"))));
                conn = url.openConnection(proxy);
            } else {
                conn = url.openConnection();
            }
            addBasicAuthentication(address, conn, repository);
            final String userAgentValue = calculateUserAgent();
            conn.setRequestProperty("User-Agent", userAgentValue);
            conn.connect();
            in = conn.getInputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int numRead;
            long progressCounter = 0;
            while ((numRead = in.read(buffer)) != -1) {
                progressCounter += numRead;
                if (progressCounter / PROGRESS_CHUNK > 0) {
                    progressCounter = progressCounter - PROGRESS_CHUNK;
                }
                out.write(buffer, 0, numRead);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    private String calculateUserAgent() {

        String javaVendor = System.getProperty("java.vendor");
        String javaVersion = System.getProperty("java.version");
        String javaVendorVersion = System.getProperty("java.vm.version");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        return String.format("%s/%s (%s;%s;%s) (%s;%s;%s)", appName, appVersion, osName, osVersion, osArch, javaVendor, javaVersion, javaVendorVersion);
    }

    private void addBasicAuthentication(URI address, URLConnection conn, MavenRepository repository) throws IOException {
        String userInfo = calculateUserInfo(address, repository);
        if (userInfo == null) {
            return;
        }
        conn.setRequestProperty("Authorization", "Basic " + base64Encode(userInfo));
    }

    private String base64Encode(String userInfo) {
        try {
            return Base64.getEncoder().encodeToString(userInfo.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException encodingException) {
            throw new RuntimeException("Failed to get bytes for user info using UTF-8.", encodingException);
        }
    }

    private String calculateUserInfo(URI uri, MavenRepository repository) {
        if (repository.getUserId() != null && repository.getPassword() != null) {
            return repository.getUserId() + ":" + repository.getPassword();
        }
//        if (envMap.get("FEATURE_REPO_USER") != null && envMap.get("FEATURE_REPO_PASSWORD") != null) {
//            return (String)envMap.get("FEATURE_REPO_USER") + ':' + (String)envMap.get("FEATURE_REPO_PASSWORD");
//        }
        return uri.getUserInfo();
    }

    private static class SystemPropertiesProxyAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication((String) envMap.get("https.proxyUser"), ((String) envMap.get("https.proxyPassword")).toCharArray());
        }
    }

    private static class SystemPropertiesProxyHttpAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication((String) envMap.get("http.proxyUser"), ((String) envMap.get("http.proxyPassword")).toCharArray());
        }
    }

    public List<File> getDownloadedEsas() {
        List<File> esaFiles = new ArrayList<File>();
        for (File f : downloadedFiles) {
            if (f.getName().endsWith(".esa")) {
                esaFiles.add(f);
            }
        }
        return esaFiles;
    }

    public List<File> getDownloadedPoms() {
        List<File> pomFiles = new ArrayList<File>();
        for (File f : downloadedFiles) {
            if (f.getName().endsWith(".pom")) {
                pomFiles.add(f);
            }
        }
        return pomFiles;
    }

    public List<File> getDownloadedFiles() {
        return downloadedFiles;
    }

    public void checkValidProxy() throws InstallException {
        String protocol = null;
        if (envMap.get("https.proxyUser") != null) {
            protocol = "https";
        } else if (envMap.get("http.proxyUser") != null) {
            protocol = "http";
        }

        String proxyPort = (String) envMap.get(protocol + ".proxyPort");
        if (protocol != null) {
            int proxyPortnum = Integer.parseInt(proxyPort);
            if (((String) envMap.get(protocol + ".proxyHost")).isEmpty()) {
                throw ExceptionUtils.createByKey("ERROR_TOOL_PROXY_HOST_MISSING");
            } else if (proxyPortnum < 0 || proxyPortnum > 65535) {
                throw ExceptionUtils.createByKey("ERROR_TOOL_INVALID_PROXY_PORT", proxyPort);
            } else if (((String) envMap.get(protocol + ".proxyPassword")).isEmpty() ||
                       envMap.get(protocol + ".proxyPassword") == null) {
                throw ExceptionUtils.createByKey("ERROR_TOOL_PROXY_PWD_MISSING");
            }
        }
    }

    private void updateProgress(double increment) {
        progressBar.updateProgress(increment);

    }

    public void setEnvMap(Map<String, Object> envMap) {
        this.envMap = envMap;
    }

    public Map<String, Object> getEnvMap() {
        return this.envMap;
    }

    // log message types
    private void info(String msg) {
        logger.info(msg);

    }

    private void fine(String msg) {

        logger.fine(msg);

    }

    private void severe(String msg) {
        logger.severe(msg);

    }

}
