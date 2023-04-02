/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;

public class ArtifactDownloader implements AutoCloseable {

    private final int PROGRESS_CHUNK = 500000;

    private final int BUFFER_SIZE = 10000;

    private final String appName = "artifactDownloader"; //TODO replace with openliberty?

    private final String appVersion = "1.0.0"; //TODO replace with openliberty version?

    private final List<File> downloadedFiles;

    private final static Logger logger = InstallLogUtils.getInstallLogger();

    private final ProgressBar progressBar;

    private static Map<String, Object> envMap = null;

    private final ExecutorService executor;

    private Map<String, File> mavenCoordMap = null;

    ArtifactDownloader() {
        this.downloadedFiles = Collections.synchronizedList(new ArrayList<>());
        this.progressBar = ProgressBar.getInstance();
        this.executor = Executors.newFixedThreadPool(ArtifactDownloaderUtils.getNumThreads());
        this.mavenCoordMap = new Hashtable<>();
    }

    private Future<String> submitDownloadRequest(String coords, String fileType, String dLocation, MavenRepository repository) {
        return executor.submit(() -> {
            synthesizeAndDownload(coords, fileType, dLocation, repository, false);
            return coords + "." + fileType;
        });
    }

    public Set<String> getMissingFeaturesFromRepo(List<String> mavenCoords, MavenRepository repository) throws InstallException {
        info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_CONTACTING_MAVEN_REPO"));

        String repo = FormatUrlSuffix(repository.getRepositoryUrl());
        Map<String, String> URLtoMavenCoordMap = new HashMap<>();
        ArtifactDownloaderUtils.acquireFeatureURLs(mavenCoords, repo, URLtoMavenCoordMap);
        List<String> missingFeaturesURLs;
        Set<String> missingCoords = new HashSet<String>();

        if (!testConnection(repository)) {
            throw ExceptionUtils.createByKey("ERROR_FAILED_TO_CONNECT_MAVEN");
        }

        updateProgress(progressBar.getMethodIncrement("establishConnection"));
        info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_MAVEN_REPO_CONNECTION_SUCCESSFUL"));

        try {
            missingFeaturesURLs = ArtifactDownloaderUtils.getMissingFiles(URLtoMavenCoordMap.keySet(), envMap);
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new InstallException(e.getMessage());
        }

        if (!missingFeaturesURLs.isEmpty()) {
            List<String> missingFeatureList = new ArrayList<String>(); //for debugging
            for (String f : missingFeaturesURLs) {
                String featureName = ArtifactDownloaderUtils.getFileNameFromURL(f);
                String featureCoord = URLtoMavenCoordMap.get(f);
                missingFeatureList.add(featureName);
                missingCoords.add(featureCoord);
            }
            fine("The remote repository \"" + repo.toString() + "\" is missing the following artifacts: " + missingFeatureList.toString());

        }
        return missingCoords;
    }

    public void synthesizeAndDownloadFeatures(List<String> mavenCoords, String dLocation, MavenRepository repository) throws InstallException {
        final List<Future<?>> futures = new ArrayList<>();
        // we have downloaded mavenCoords.length * 2 (esa and pom) amount of features.
        double individualSize = progressBar.getMethodIncrement("downloadArtifacts") / (2 * mavenCoords.size());
        info(Messages.INSTALL_KERNEL_MESSAGES.getMessage("MSG_BEGINNING_DOWNLOAD_FEATURES"));
        for (String coords : mavenCoords) {
            Future<?> future1 = submitDownloadRequest(coords, "esa", dLocation, repository);
            futures.add(future1);
            Future<?> future2 = submitDownloadRequest(coords, "pom", dLocation, repository);
            futures.add(future2);
        }

        while (!futures.isEmpty()) {
            Iterator<Future<?>> iter = futures.iterator();
            try {
                while (iter.hasNext()) {
                    Future<?> future = iter.next();
                    if (future.isDone()) {
                        String downloadedCoords;
                        downloadedCoords = (String) future.get();
                        // update progress bar, drain the downloadArtifacts total size
                        updateProgress(individualSize);
                        fine("Finished downloading artifact: " + downloadedCoords);
                        iter.remove();
                    }
                }
                fine("Remaining artifacts: " + futures.size());
                Thread.sleep(ArtifactDownloaderUtils.THREAD_SLEEP);
            } catch (InterruptedException | ExecutionException e) {
                throw new InstallException(e.getMessage());
            }
        }
        progressBar.manuallyUpdate();

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
        String urlLocation;

        String[] checksumFormats = new String[3];

        checksumFormats[0] = "MD5";
        checksumFormats[1] = "SHA1";
        checksumFormats[2] = "SHA256";

        dLocation = FormatPathSuffix(dLocation);
        String repo = FormatUrlSuffix(repository.getRepositoryUrl());

        urlLocation = ArtifactDownloaderUtils.getUrlLocation(repo, mavenCoords) + "." + filetype;

        try {
            if (individualDownload) {
                if (!testConnection(repository)) {
                    throw ExceptionUtils.createByKey("ERROR_FAILED_TO_CONNECT_MAVEN");
                }

                if (ArtifactDownloaderUtils.fileIsMissing(urlLocation, envMap)) {
                    throw ExceptionUtils.createByKey("ERROR_FAILED_TO_DOWNLOAD_ASSETS_FROM_REPO", ArtifactDownloaderUtils.getfilename(mavenCoords), filetype + " file",
                                                     repository.toString()); //ERROR_FAILED_TO_DOWNLOAD_ASSETS_FROM_MAVEN_REPO
                }
            }

            download(mavenCoords, filetype, urlLocation, dLocation, checksumFormats, repository);

        } catch (IOException e) {
            throw new InstallException(e.getMessage());
        }

    }

    private void download(String mavenCoords, String filetype, String urlLocation, String dLocation, String[] checksumFormats,
                          MavenRepository mavenRepository) throws IOException, InstallException {
        try {
            URI uriLoc = new URI(urlLocation);
            File fileLoc = new File(urlLocation.replace(mavenRepository.toString(), dLocation));
            String filename = ArtifactDownloaderUtils.getFileNameFromURL(urlLocation);

            downloadInternal(uriLoc, fileLoc, mavenRepository);

            downloadedFiles.add(fileLoc);
            if (filetype.equals("esa")) {
                mavenCoordMap.put(mavenCoords, fileLoc);
            }
            boolean someChecksumExists = false;
            boolean checksumFail = false;
            boolean checksumSuccess = false;
            ConcurrentHashMap<String, String> checkSumCache = new ConcurrentHashMap<>();

            for (String checksumFormat : checksumFormats) {
                if (!checksumSuccess) {
                    if (checksumIsAvailable(urlLocation, checksumFormat, checkSumCache)) {
                        someChecksumExists = true;
                        if (isIncorrectChecksum(fileLoc.getAbsolutePath(), urlLocation, checksumFormat, checkSumCache)) {
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
                    ArtifactDownloaderUtils.deleteFiles(downloadedFiles, dLocation, fileLoc);
                    downloadedFiles.clear();
                    mavenCoordMap.clear();
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

    private boolean checksumIsAvailable(String urlLocation, String checksumFormat, ConcurrentHashMap<String, String> checkSumCache) {
        boolean result = true;
        try {
            if (checkSumCache.containsKey(checksumFormat))
                return true;
            String checkSum = ArtifactDownloaderUtils.getPrimaryChecksum(urlLocation, checksumFormat);
            checkSumCache.put(checksumFormat, checkSum);
        } catch (IOException e) {
            result = false;
        }
        return result;
    }

    private boolean isIncorrectChecksum(String localFile, String urlLocation, String checksumFormat,
                                        ConcurrentHashMap<String, String> checkSumCache) throws NoSuchAlgorithmException {
        boolean result = false;
        String checksumLocal;
        try {
            checksumLocal = ArtifactDownloaderUtils.getChecksum(localFile, checksumFormat);
        } catch (IOException e) {
            return true;
        }
        String checksumOrigin = checkSumCache.get(checksumFormat);
        if (checksumOrigin == null || !checksumLocal.equals(checksumOrigin)) {
            result = true;
        }
        return result;
    }

    private void configureProxyAuthentication() throws InstallException {
        //set up basic auth HTTP proxy tunnel
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");

        if (envMap.get("https.proxyUser") != null) {
            final String encodedPassword = formatAndCheckRepositoryPassword((String) envMap.get("https.proxyPassword"));
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication((String) envMap.get("https.proxyUser"), PasswordUtil.passwordDecode(encodedPassword).toCharArray());
                }
            });
        } else if (envMap.get("http.proxyUser") != null) {
            final String encodedPassword = formatAndCheckRepositoryPassword((String) envMap.get("http.proxyPassword"));
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication((String) envMap.get("http.proxyUser"), PasswordUtil.passwordDecode(encodedPassword).toCharArray());
                }
            });
        }
    }

    private void configureAuthentication(final MavenRepository repository) throws InstallException {

        if (repository.getUserId() != null && repository.getPassword() != null &&
            envMap.get("https.proxyUser") == null && envMap.get("http.proxyUser") == null) {
            final String encodedPassword = formatAndCheckRepositoryPassword(repository.getPassword());
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(repository.getUserId(), PasswordUtil.passwordDecode(encodedPassword).toCharArray());
                }
            });
        }
    }

    /**
     * @param pwd - repository password
     * @return a formated password string
     * @throws InstallException if decoding the password fails due to an unsupported algorithm or invalid password.
     */
    private String formatAndCheckRepositoryPassword(String pwd) throws InstallException {
        String crypto_algorithm = PasswordUtil.getCryptoAlgorithm(pwd);
        if (!pwd.startsWith("{")) {
            pwd = "{}" + pwd;
            logger.log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_PWD_NOT_ENCRYPTED")
                                   + InstallUtils.NEWLINE);
            return pwd;
        }

        if (PasswordUtil.passwordDecode(pwd) == null) {
            if (!PasswordUtil.isValidCryptoAlgorithm(crypto_algorithm)) {
                // don't accept unsupported crypto algorithm
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_PWD_CRYPTO_UNSUPPORTED"));
            } else {
                throw new InstallException(Messages.PASSWORD_UTIL_MESSAGES.getLogMessage("PASSWORDUTIL_CYPHER_EXCEPTION"));
            }
        }

        return pwd;
    }

    /**
     * Tests the connection of a MavenRepository. If the server returns 404, then this will return false.
     *
     * @return
     */
    protected boolean testConnection(MavenRepository repository) {
        try {
            checkValidProxy();
            configureProxyAuthentication();
            configureAuthentication(repository);
            int responseCode = ArtifactDownloaderUtils.exists(repository.getRepositoryUrl(), envMap);
            logger.fine("Response code - " + repository.getRepositoryUrl() + ":" + responseCode);
            if (responseCode != 404) {
                // repo is fine for use
                return true;
            }
        } catch (IOException | InstallException e) {
            logger.warning(repository.getRepositoryUrl() + " cannot be connected");
            logger.fine(e.getMessage());
        }
        return false;
    }

    private void downloadInternal(URI address, File destination, MavenRepository repository) throws IOException, InstallException {
        Proxy proxy;
        if (envMap.get("https.proxyHost") != null) {
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress((String) envMap.get("https.proxyHost"), Integer.parseInt((String) envMap.get("https.proxyPort"))));
        } else if (envMap.get("http.proxyHost") != null) {
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress((String) envMap.get("http.proxyHost"), Integer.parseInt((String) envMap.get("http.proxyPort"))));
        } else {
            proxy = Proxy.NO_PROXY;
        }
        URL url = address.toURL();
        URLConnection conn = url.openConnection(proxy);
        addBasicAuthentication(address, conn, repository);
        final String userAgentValue = calculateUserAgent();
        conn.setRequestProperty("User-Agent", userAgentValue);
        conn.connect();

        destination.getParentFile().mkdirs();
        File tempFile = File.createTempFile(destination.getName(), null, destination.getParentFile());
        try (InputStream in = conn.getInputStream(); OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
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

        } catch (FileNotFoundException e) {
            throw ExceptionUtils.createByKey("ERROR_FAILED_TO_DOWNLOAD_FEATURE", ArtifactDownloaderUtils.getFileNameFromURL(address.toString()),
                                             destination.toString());
        }

        if (destination.exists() || !tempFile.renameTo(destination)) {
            logger.fine("Could not rename " + tempFile.getName() + " to: " + destination.getName());
            tempFile.delete();
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
            return new PasswordAuthentication((String) envMap.get("https.proxyUser"), PasswordUtil.passwordDecode((String) envMap.get("https.proxyPassword")).toCharArray());
        }
    }

    private static class SystemPropertiesProxyHttpAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication((String) envMap.get("http.proxyUser"), PasswordUtil.passwordDecode((String) envMap.get("http.proxyPassword")).toCharArray());
        }
    }

    /*
     * Returns the list of downloaded esa files in the order provided by the resolver.
     *
     * @param featureList List of missing features in the order returned by the resolver
     *
     * @return esaFiles
     */
    public List<File> getDownloadedEsas(List<String> featureList) {
        List<File> esaFiles = new ArrayList<File>();
        for (String coord : featureList) {
            //return downloaded esa files in the same order as featureList
            File artifactPath = mavenCoordMap.get(coord);
            if (artifactPath != null) {
                esaFiles.add(artifactPath);
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

    @Override
    public void close() {
        executor.shutdown();
    }

}
