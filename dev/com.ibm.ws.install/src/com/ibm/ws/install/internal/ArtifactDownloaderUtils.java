/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import com.ibm.ws.install.InstallException;

public class ArtifactDownloaderUtils {

    private static boolean isFinished = false;

    public static List<String> getMissingFiles(List<String> featureURLs, Map<String, Object> envMap) throws IOException {
        List<String> result = new ArrayList<String>();
        for (String url : featureURLs) {
            if (!(exists(url, envMap) == HttpURLConnection.HTTP_OK)) {
                result.add(url);
            }
        }
        return result;
    }

    public static boolean fileIsMissing(String url, Map<String, Object> envMap) throws IOException {
        return !(exists(url, envMap) == HttpURLConnection.HTTP_OK);
    }

    public static int exists(String URLName, Map<String, Object> envMap) throws IOException {
        try {
            URL url = new URL(URLName);
            if (url.getProtocol().equals("https")) {
                HttpsURLConnection.setFollowRedirects(true);
                HttpsURLConnection conn;

                if (envMap.get("https.proxyHost") != null) {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress((String) envMap.get("https.proxyHost"), Integer.parseInt((String) envMap.get("https.proxyPort"))));
                    conn = (HttpsURLConnection) url.openConnection(proxy);
                } else if (envMap.get("http.proxyHost") != null) {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress((String) envMap.get("http.proxyHost"), Integer.parseInt((String) envMap.get("http.proxyPort"))));
                    conn = (HttpsURLConnection) url.openConnection(proxy);
                } else {
                    conn = (HttpsURLConnection) url.openConnection();
                }
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(10000);
                conn.connect();
                int responseCode = conn.getResponseCode();
                conn.setInstanceFollowRedirects(true);
                return responseCode;
            } else {
                HttpURLConnection.setFollowRedirects(true);
                HttpURLConnection conn;
                if (envMap.get("https.proxyHost") != null) {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress((String) envMap.get("https.proxyHost"), Integer.parseInt((String) envMap.get("https.proxyPort"))));
                    conn = (HttpURLConnection) url.openConnection(proxy);
                } else if (envMap.get("http.proxyHost") != null) {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress((String) envMap.get("http.proxyHost"), Integer.parseInt((String) envMap.get("http.proxyPort"))));
                    conn = (HttpURLConnection) url.openConnection(proxy);
                } else {
                    conn = (HttpURLConnection) url.openConnection();
                }
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(10000);
                conn.connect();
                int responseCode = conn.getResponseCode();
                conn.setInstanceFollowRedirects(true);
                return responseCode;
            }
        } catch (ConnectException e) {
            throw e;
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
    }

    public static List<String> acquireFeatureURLs(List<String> mavenCoords, String repo) {
        List<String> result = new ArrayList<String>();
        for (String coord : mavenCoords) {
            String groupId = getGroupId(coord).replace(".", "/") + "/";
            String artifactId = getartifactId(coord);
            String version = getVersion(coord);
            String esafilename = getfilename(coord, "esa");
            String pomfilename = getfilename(coord, "pom");
            result.add(getUrlLocation(repo, groupId, artifactId, version, esafilename));
            result.add(getUrlLocation(repo, groupId, artifactId, version, pomfilename));
        }
        return result;
    }

    public static String getChecksum(String filename, String format) throws NoSuchAlgorithmException, IOException {
        if (format.equals("SHA256")) {
            format = "SHA-256";
        }
        byte[] b = createChecksum(filename, format);
        String result = "";

        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static byte[] createChecksum(String filename, String format) throws IOException, NoSuchAlgorithmException {
        InputStream fis = new FileInputStream(filename);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance(format);
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    public static String getChecksumFromURL(URL url) throws IOException {
        BufferedReader in;
        String result = "";
        in = new BufferedReader(new InputStreamReader(url.openStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            result += inputLine;
        in.close();

        return result;
    }

    public static String getPrimaryChecksum(String url, String format) throws IOException {
        URL urlLocation = new URL(url + "." + format.toLowerCase());
        return getChecksumFromURL(urlLocation);
    }

    public static void deleteFiles(List<File> fileList, String dLocation, String groupId, String artifactId, String version, String filename) {
        for (File f : fileList) {
            f.delete();
        }
        File file = (new File(getFileLocation(dLocation, groupId, artifactId, version, filename))).getParentFile();
        while (!(file.toString() + "/").equals(dLocation)) {
            File[] files = file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return !name.equals(".DS_Store");
                }
            });
            File newFile = file.getParentFile();
            if (files.length == 0) {
                file.delete();
            }
            file = newFile;
        }
    }

    public static String getGroupId(String mavenCoords) {
        return mavenCoords.split(":")[0];
    }

    public static String getartifactId(String mavenCoords) {
        return mavenCoords.split(":")[1];
    }

    public static String getVersion(String mavenCoords) {
        return mavenCoords.split(":")[2];
    }

    public static String getfilename(String mavenCoords, String filetype) {
        return getartifactId(mavenCoords) + "-" + getVersion(mavenCoords) + "." + filetype;
    }

    public static String getUrlLocation(String repo, String groupId, String artifactId, String version, String filename) {
        return repo + groupId + artifactId + "/" + version + "/" + filename;
    }

    public static String getFileLocation(String dLocation, String groupId, String artifactId, String version, String filename) {
        return dLocation + groupId + artifactId + "/" + version + "/" + filename;
    }

    public static String getFileNameFromURL(String str) {
        String[] pathSplit = str.split("/");
        return pathSplit[pathSplit.length - 3];
    }

    public static void checkResponseCode(int repoResponseCode, String repo) throws InstallException {
        if (!(repoResponseCode == HttpURLConnection.HTTP_OK)) { //verify repo exists
            if (repoResponseCode == 407) {
                throw ExceptionUtils.createByKey("ERROR_TOOL_INCORRECT_PROXY_CREDENTIALS");
            } else if (repoResponseCode == 401 || repoResponseCode == 403) {
                throw ExceptionUtils.createByKey("ERROR_INVALID_MAVEN_CREDENTIALS", repo);
            } else {
                throw ExceptionUtils.createByKey("ERROR_FAILED_TO_CONNECT_MAVEN"); //503
            }
        }
    }

    public static String getMavenCoordFromPath(String coordPath, String groupID) {
        String result = coordPath;
        String[] resSplit = result.split(File.separator);
        String artifactID = resSplit[(resSplit.length - 3)];
        String version = resSplit[(resSplit.length - 2)];
        result = groupID + ":" + artifactID + ":" + version;
        return result;
    }

}
