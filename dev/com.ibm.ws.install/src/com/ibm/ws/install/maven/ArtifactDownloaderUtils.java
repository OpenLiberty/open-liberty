package com.ibm.ws.install.maven;

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
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class ArtifactDownloaderUtils {

    private static boolean isFinished = false;

    public static List<String> getMissingFiles(List<String> featureURLs) {
        List<String> result = new ArrayList<String>();
        for (String url : featureURLs) {
            if (!(exists(url) == HttpURLConnection.HTTP_OK)) {
                result.add(url);
            }
        }
        return result;
    }

    public static int exists(String URLName) {

        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection conn;
            URL url = new URL(URLName);
            if (System.getProperty("http.proxyUser") != null) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(System.getProperty("http.proxyHost"), 8080));
                conn = (HttpURLConnection) url.openConnection(proxy);
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }
            conn.setRequestMethod("HEAD");
            int responseCode = conn.getResponseCode();
            return responseCode;
        } catch (ConnectException e) {
            System.out.println("improper proxy host"); //TODO proper error message
            e.printStackTrace();
        } catch (SocketTimeoutException e) {
            System.out.println("connection timeout: " + URLName); //TODO proper error message, maybe make this a method throw
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
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

    public static String getChecksumFromURL(URL url) {
        BufferedReader in;
        String result = "";
        try {
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                result += inputLine;
            in.close();
        } catch (IOException e) {
            System.out.println("failed to download checksum file at " + url.toString()); //TODO proper error message
            e.printStackTrace();
            return null;
        }
        return result;
    }

    public static String getMasterChecksum(String url, String format) throws MalformedURLException {
        URL urlLocation = new URL(url + "." + format.toLowerCase());
        return getChecksumFromURL(urlLocation);
    }

    public static void deleteFiles(List<File> fileList, String dLocation, String groupId, String version, String filename) {
        for (File f : fileList) {
            if (!f.delete()) {
                System.out.println("failed to delete file");
            }
        }
        File file = (new File(getFileLocation(dLocation, groupId, version, filename))).getParentFile();
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

    public static String getFileLocation(String dLocation, String groupId, String version, String filename) {
        return dLocation + groupId + version + "/" + filename;
    }

}
