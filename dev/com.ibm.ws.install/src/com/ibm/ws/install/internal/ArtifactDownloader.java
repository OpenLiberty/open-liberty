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
import java.net.HttpURLConnection;
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


public class ArtifactDownloader {

    private final int PROGRESS_CHUNK = 500000;

    private final int BUFFER_SIZE = 10000;

    private String appName = "artifactDownloader"; //TODO replace with openliberty?

    private String appVersion = "1.0.0"; //TODO replace with openliberty version?

    private List<File> downloadedFiles = new ArrayList<File>();;

    public void synthesizeAndDownloadFeatures(List<String> mavenCoords, String dLocation, String repo) {
        configureProxyAuthentication();
        configureAuthentication();
        downloadedFiles.clear();
        int repoResponseCode = ArtifactDownloaderUtils.exists(repo);
        if (!(repoResponseCode  == HttpURLConnection.HTTP_OK)) { //verify repo exists
            if (repoResponseCode == 503) {
                System.out.println("repo does not exist CWWKF1418E"); //TODO proper error message with exception
                return;
            } else if (repoResponseCode == 407) {
                System.out.println("proxy authentication failure CWWKF1367E"); //TODO proper error message with exception
                return;
            } else {
                System.out.println("failed to connect to repository error code: " + repoResponseCode); //TODO proper error message with exception (unknown cause maybe)
                return;
            }
        }

        List<String> featureURLs = ArtifactDownloaderUtils.acquireFeatureURLs(mavenCoords, repo); 
        List<String> missingFeatures = ArtifactDownloaderUtils.getMissingFiles(featureURLs);
        if (!missingFeatures.isEmpty()) { //verify that there are no missing features in the repository
            System.out.println("ERROR the repo "+ repo + " is missing the following files:\n" + missingFeatures); //TODO proper error message with exception
            return;
        } else {
            for (String coords: mavenCoords) { //download the corresponding esa and pom files for the features
                synthesizeAndDownload(coords,"esa",dLocation,repo);
                synthesizeAndDownload(coords,"pom",dLocation,repo);
            }
        }
    }



    public void synthesizeAndDownload(String mavenCoords, String filetype, String dLocation, String repo) {

        String groupId = ArtifactDownloaderUtils.getGroupId(mavenCoords).replace(".", "/") + "/";
        String artifactId = ArtifactDownloaderUtils.getartifactId(mavenCoords);
        String version = ArtifactDownloaderUtils.getVersion(mavenCoords);
        new File(dLocation + groupId + version + "/").mkdirs();

        String filename = ArtifactDownloaderUtils.getfilename(mavenCoords, filetype);
        String urlLocation = ArtifactDownloaderUtils.getUrlLocation(repo, groupId, artifactId, version, filename);
        String[] checksumFormats = new String[2];
        checksumFormats[0] = "MD5";
        checksumFormats[1] = "SHA1";
        configureProxyAuthentication();
        configureAuthentication();
        try {
            download(urlLocation, dLocation, groupId, version, filename, checksumFormats);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 

    }

    private void download(String urlLocation, String dLocation, String groupId, String version, String filename, String[] checksumFormats) throws IOException {
        try {
            URI uriLoc = new URI(urlLocation);
            File fileLoc = new File(ArtifactDownloaderUtils.getFileLocation(dLocation, groupId, version, filename));

            downloadInternal(uriLoc, fileLoc);
            downloadedFiles.add(fileLoc);

            for (String checksumFormat: checksumFormats) {
                String checksumLocal = ArtifactDownloaderUtils.getChecksum(fileLoc.getAbsolutePath(), checksumFormat);
                String checksumOrigin = ArtifactDownloaderUtils.getMasterChecksum(urlLocation, checksumFormat);
                if (!checksumLocal.equals(checksumOrigin)) { // ||  filename.equals("jaxb-2.2-19.0.0.8.pom")
                    ArtifactDownloaderUtils.deleteFiles(downloadedFiles, dLocation, groupId, version, filename);
                    downloadedFiles.clear();
                    throw new IOException("ERROR discrepency between "+ checksumFormat + " checksums for file: " + filename); //TODO proper error message with exception
                }
            }

        } catch(URISyntaxException e) {
            System.out.println("url not in proper format"); //TODO proper error message with exception
            e.printStackTrace();
            return;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("no such checksum format found."); //TODO proper error message with exception
            e.printStackTrace();
            return;
        }
    }

    private void configureProxyAuthentication() {
        if (System.getProperty("http.proxyUser") != null) {
            Authenticator.setDefault(new SystemPropertiesProxyAuthenticator());
        }
    }

    private void configureAuthentication() {
        if (System.getProperty("MVNW_USERNAME") != null && System.getProperty("MVNW_PASSWORD") != null && System.getProperty("http.proxyUser") == null) {
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(System.getProperty("MVNW_USERNAME"), System.getProperty("MVNW_PASSWORD").toCharArray());
                }
            });
        }
    }


    private void downloadInternal(URI address, File destination) throws IOException {
        OutputStream out = null;
        URLConnection conn;
        InputStream in = null;
        try {
            URL url = address.toURL();
            try {
                out = new BufferedOutputStream(new FileOutputStream(destination));
            } catch (FileNotFoundException e) {
                System.out.println("failed to find/create file at destination: " + destination); //TODO proper error message with exception
                e.printStackTrace();
                return;
            }
            if (System.getProperty("http.proxyUser") != null) { //TODO consider https proxy
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(System.getProperty("http.proxyHost"), 8080));
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
                    System.out.println(".");
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
        if (!"https".equals(address.getScheme())) {
            System.out.println("WARNING Using HTTP Basic Authentication over an insecure connection to download the Maven distribution. Please consider using HTTPS.");
        }
        connection.setRequestProperty("Authorization", "Basic " + base64Encode(userInfo));
    }

    private String base64Encode(String userInfo) {
        ClassLoader loader = getClass().getClassLoader();
        try {
            Method getEncoderMethod = loader.loadClass("java.util.Base64").getMethod("getEncoder");
            Method encodeMethod = loader.loadClass("java.util.Base64$Encoder").getMethod("encodeToString", byte[].class);
            Object encoder = getEncoderMethod.invoke(null);
            return (String) encodeMethod.invoke(encoder, new Object[]{userInfo.getBytes("UTF-8")});
        } catch (Exception java7OrEarlier) {
            try {
                Method encodeMethod = loader.loadClass("javax.xml.bind.DatatypeConverter").getMethod("printBase64Binary", byte[].class);
                return (String) encodeMethod.invoke(null, new Object[]{userInfo.getBytes("UTF-8")});
            } catch (Exception java5OrEarlier) {
                throw new RuntimeException("Downloading Maven distributions with HTTP Basic Authentication is not supported on your JVM.", java5OrEarlier);
            }
        }
    }

    private String calculateUserInfo(URI uri) {

        if (System.getProperty("MVNW_USERNAME") != null && System.getProperty("MVNW_PASSWORD") != null) {
            return System.getProperty("MVNW_USERNAME") + ':' + System.getProperty("MVNW_PASSWORD");
        }
        return uri.getUserInfo();
    }

    private static class SystemPropertiesProxyAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(System.getProperty("http.proxyUser"), System.getProperty("http.proxyPassword", "").toCharArray());
        }
    }

    public List<File> getDownloadedEsas(){
        List<File> esaFiles = new ArrayList<File>();
        for (File f: downloadedFiles) {
            if (f.getName().endsWith(".esa")) {
                esaFiles.add(f);
            }
        }
        return esaFiles;
    }
    
    public List<File> getDownloadedPoms(){
        List<File> pomFiles = new ArrayList<File>();
        for (File f: downloadedFiles) {
            if (f.getName().endsWith(".pom")) {
                pomFiles.add(f);
            }
        }
        return pomFiles;
    }
    
    public List<File> getDownloadedFiles() {
        return downloadedFiles;
    }

}
