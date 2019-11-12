package com.ibm.ws.install.internal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallException;

public class ArtifactDownloader {

    private final int PROGRESS_CHUNK = 500000;

    private final int BUFFER_SIZE = 10000;

    private final String appName = "artifactDownloader"; //TODO replace with openliberty?

    private final String appVersion = "1.0.0"; //TODO replace with openliberty version?

    private final List<File> downloadedFiles = new ArrayList<File>();

    private final Logger logger = InstallLogUtils.getInstallLogger();

    private final ProgressBar progressBar = ProgressBar.getInstance();
    private final boolean isWindows = (System.getProperty("os.name").toLowerCase()).indexOf("win") >= 0;
    private static Map<String, String> envMap = null;

    public void synthesizeAndDownloadFeatures(List<String> mavenCoords, String dLocation, String repo) throws InstallException {
        info("Establishing a connection to the configured Maven repository ...\n" +
             "This process might take several minutes to complete."); //STATE_CONTACTING_MAVEN_REPO
        checkValidProxy();
        configureProxyAuthentication();
        configureAuthentication();
        updateProgress(progressBar.getMethodIncrement("establishConnection"));
        info("Successfully connected to the configured repository."); //STATE_MAVEN_REPO_CONNECTION_SUCCESSFUL
        downloadedFiles.clear();
        int repoResponseCode;
        repo = FormatUrlSuffix(repo);
        try {
            repoResponseCode = ArtifactDownloaderUtils.exists(repo, envMap);
        } catch (IOException e) {
            throw new InstallException(e.getMessage());
            //throw ExceptionUtils.createByKey("ERROR_FAILED_TO_CONNECT_MAVEN"); //ERROR_FAILED_TO_CONNECT_MAVEN
        }
        ArtifactDownloaderUtils.checkResponseCode(repoResponseCode, repo);
        List<String> featureURLs = ArtifactDownloaderUtils.acquireFeatureURLs(mavenCoords, repo);
        List<String> missingFeatures;
        dLocation = FormatPathSuffix(dLocation);
        try {
            missingFeatures = ArtifactDownloaderUtils.getMissingFiles(featureURLs, envMap);
        } catch (IOException e) {
            throw new InstallException(e.getMessage());
        }
        if (!missingFeatures.isEmpty()) {
            List<String> missingFeatureList = new ArrayList<String>();
            for (String f : missingFeatures) {
                if (f.endsWith(".esa")) {
                    missingFeatureList.add(ArtifactDownloaderUtils.getFileNameFromURL(f));
                }
            }
            throw ExceptionUtils.createByKey("ERROR_FAILED_TO_DOWNLOAD_ASSETS_FROM_REPO", missingFeatureList, "feature(s)", repo);
        } else {
            for (String coords : mavenCoords) {
                synthesizeAndDownload(coords, "esa", dLocation, repo, false);
                synthesizeAndDownload(coords, "pom", dLocation, repo, false);
                updateProgress(progressBar.getMethodIncrement("downloadArtifact"));
                fine("Finished downloading artifact: " + coords);
            }
        }
    }

    /**
     * @param dLocation
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

    public void synthesizeAndDownload(String mavenCoords, String filetype, String dLocation, String repo, boolean individualDownload) throws InstallException {
        configureProxyAuthentication();
        configureAuthentication();
        repo = FormatUrlSuffix(repo);
        if (individualDownload) {
            checkValidProxy();
            int repoResponseCode;
            dLocation = FormatPathSuffix(dLocation);
            try {
                repoResponseCode = ArtifactDownloaderUtils.exists(repo, envMap);
            } catch (IOException e) {
                throw new InstallException(e.getMessage());
            }
            ArtifactDownloaderUtils.checkResponseCode(repoResponseCode, repo);
        }
        String groupId = ArtifactDownloaderUtils.getGroupId(mavenCoords).replace(".", "/") + "/";
        String artifactId = ArtifactDownloaderUtils.getartifactId(mavenCoords);
        String version = ArtifactDownloaderUtils.getVersion(mavenCoords);
        new File(dLocation + groupId + artifactId + "/" + version + "/").mkdirs();

        String filename = ArtifactDownloaderUtils.getfilename(mavenCoords, filetype);
        String urlLocation = ArtifactDownloaderUtils.getUrlLocation(repo, groupId, artifactId, version, filename);
        String[] checksumFormats = new String[3];
        checksumFormats[0] = "MD5";
        checksumFormats[1] = "SHA1";
        checksumFormats[2] = "SHA256";
        try {
            if (individualDownload && ArtifactDownloaderUtils.fileIsMissing(urlLocation, envMap)) {
                throw ExceptionUtils.createByKey("ERROR_FAILED_TO_DOWNLOAD_ASSETS_FROM_REPO", ArtifactDownloaderUtils.getFileNameFromURL(urlLocation), filetype + " file", repo); //ERROR_FAILED_TO_DOWNLOAD_ASSETS_FROM_MAVEN_REPO
            } else {
                download(urlLocation, dLocation, groupId, artifactId, version, filename, checksumFormats);
            }
        } catch (IOException e) {
            throw new InstallException(e.getMessage());
        }

    }

    private void download(String urlLocation, String dLocation, String groupId, String artifactId, String version, String filename,
                          String[] checksumFormats) throws IOException, InstallException {
        try {
            URI uriLoc = new URI(urlLocation);
            File fileLoc = new File(ArtifactDownloaderUtils.getFileLocation(dLocation, groupId, artifactId, version, filename));

            downloadInternal(uriLoc, fileLoc);

            downloadedFiles.add(fileLoc);
            boolean someChecksumExists = false;
            boolean checksumFail = false;
            for (String checksumFormat : checksumFormats) {
                if (checksumIsAvailable(urlLocation, checksumFormat)) {
                    someChecksumExists = true;
                    if (isIncorrectChecksum(fileLoc.getAbsolutePath(), urlLocation, checksumFormat)) {
                        fine("Failed to validate " + checksumFormat + " checksum for file: " + filename);
                        checksumFail = true;
                    } else {
                        fine("Successfully validated " + checksumFormat + " checksum for file: " + filename);
                    }
                } else {
                    fine("Failed to find " + checksumFormat + " checksum for file: " + filename);
                }
            }
            if (someChecksumExists) {
                if (checksumFail) {
                    ArtifactDownloaderUtils.deleteFiles(downloadedFiles, dLocation, groupId, artifactId, version, filename);
                    downloadedFiles.clear();
                    throw new InstallException("Failed to validate available checksums for file: " + filename); //ERROR_CHECKSUM_FAILED_MAVEN
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
            ArtifactDownloaderUtils.getMasterChecksum(urlLocation, checksumFormat);
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
            checksumOrigin = ArtifactDownloaderUtils.getMasterChecksum(urlLocation, checksumFormat);
        } catch (IOException e) {
            return true;
        }
        if (!checksumLocal.equals(checksumOrigin)) {
            result = true;
        }
        return result;
    }

    private void configureProxyAuthentication() {
        if (envMap.get("http.proxyUser") != null) {
            Authenticator.setDefault(new SystemPropertiesProxyAuthenticator());
        }
    }

    private void configureAuthentication() {
        if (envMap.get("openliberty_feature_repository_user") != null && envMap.get("openliberty_feature_repository_password") != null
            && envMap.get("http.proxyUser") == null) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(envMap.get("openliberty_feature_repository_user"), envMap.get("openliberty_feature_repository_password").toCharArray());
                }
            });
        }
    }

    private void downloadInternal(URI address, File destination) throws IOException, InstallException {
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
            if (envMap.get("http.proxyUser") != null) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(envMap.get("http.proxyHost"), 8080));
                conn = url.openConnection(proxy);
            } else {
                conn = url.openConnection();
            }
            addBasicAuthentication(address, conn);
            final String userAgentValue = calculateUserAgent();
            conn.setRequestProperty("User-Agent", userAgentValue);
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

    private void addBasicAuthentication(URI address, URLConnection connection) throws IOException {
        String userInfo = calculateUserInfo(address);
        if (userInfo == null) {
            return;
        }
        connection.setRequestProperty("Authorization", "Basic " + base64Encode(userInfo));
    }

    private String base64Encode(String userInfo) {
        ClassLoader loader = getClass().getClassLoader();
        try {
            Method getEncoderMethod = loader.loadClass("java.util.Base64").getMethod("getEncoder");
            Method encodeMethod = loader.loadClass("java.util.Base64$Encoder").getMethod("encodeToString", byte[].class);
            Object encoder = getEncoderMethod.invoke(null);
            return (String) encodeMethod.invoke(encoder, new Object[] { userInfo.getBytes("UTF-8") });
        } catch (Exception java7OrEarlier) {
            try {
                Method encodeMethod = loader.loadClass("javax.xml.bind.DatatypeConverter").getMethod("printBase64Binary", byte[].class);
                return (String) encodeMethod.invoke(null, new Object[] { userInfo.getBytes("UTF-8") });
            } catch (Exception java5OrEarlier) {
                throw new RuntimeException("Downloading Maven distributions with HTTP Basic Authentication is not supported on your JVM.", java5OrEarlier);
            }
        }
    }

    private String calculateUserInfo(URI uri) {

        if (envMap.get("openliberty_feature_repository_user") != null && envMap.get("openliberty_feature_repository_password") != null) {
            return envMap.get("openliberty_feature_repository_user") + ':' + envMap.get("openliberty_feature_repository_password");
        }
        return uri.getUserInfo();
    }

    private static class SystemPropertiesProxyAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(envMap.get("http.proxyUser"), envMap.get("http.proxyPassword").toCharArray());
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

        String proxyPort = envMap.get("http.proxyPort");
        if (envMap.get("http.proxyUser") != null) {
            int proxyPortnum = Integer.parseInt(proxyPort);
            if (envMap.get("http.proxyHost").isEmpty()) {
                throw ExceptionUtils.createByKey("ERROR_TOOL_PROXY_HOST_MISSING");
            } else if (proxyPortnum < 0 || proxyPortnum > 65535) {
                throw ExceptionUtils.createByKey("ERROR_TOOL_INVALID_PROXY_PORT", proxyPort);
            } else if (envMap.get("http.proxyPassword").isEmpty() ||
                       envMap.get("http.proxyPassword") == null) {
                throw ExceptionUtils.createByKey("ERROR_TOOL_PROXY_PWD_MISSING");
            }
        }
    }

    private void updateProgress(double increment) {
        progressBar.updateProgress(increment);

    }

    public void setEnvMap(Map<String, String> envMap) {
        this.envMap = envMap;
    }

    public Map<String, String> getEnvMap() {
        return this.envMap;
    }

    // log message types
    private void info(String msg) {
        if (isWindows) {
            logger.info(msg);
        } else {
            System.out.print("\033[2K"); // Erase line content
            logger.info(msg);
            progressBar.display();
        }

    }

    private void fine(String msg) {
        if (isWindows) {
            logger.fine(msg);
        } else {
            System.out.print("\033[2K"); // Erase line content
            logger.fine(msg);
            progressBar.display();
        }
    }

    private void severe(String msg) {
        if (isWindows) {
            logger.severe(msg);
        } else {
            System.out.print("\033[2K"); // Erase line content
            logger.severe(msg);
            progressBar.display();
        }

    }

}
