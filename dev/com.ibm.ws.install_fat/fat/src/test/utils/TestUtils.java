package test.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.internal.Product;
import com.ibm.ws.install.internal.asset.FixAsset;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.connections.liberty.MainRepository;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;

import componenttest.topology.utils.FileUtils;

public class TestUtils {
    public static final String NEWLINE = System.getProperty("line.separator");

    private static File wlpDirs;
    public static File wlpDir;
    public static File wlpUserLibDir;
    public static File wlpUserBinDir;
    public static File wlpUserFeaturesDir;
    public static File wlpFixesDir;
    public static File wlpLibFeatures;
    public static File wlpBin;
    public static File wlpLib;
    public static File libFeaturesl10n;
    public static File libFeaturesCheckSums;
    public static File wlpEtc;
    public static File wlpUserLibDirFeatures;
    public static File wlpUserLibDirFeaturesl10n;
    public static File wlpUserLibDirFeaturesChecksums;
    public static File massiveRepoFiles;
    public static boolean connectedToRepo = true;
    public static Map<String, String> connectedRepoProperties = null;
    public static String wlpVersion = "";
    public static File wlpLibFixes;

    public static String repositoryDescriptionUrl;

    public static void setupWlpDirs() throws Exception {
        String localPath = new File(".").getCanonicalPath();
        wlpDirs = new File(localPath.endsWith("autoFVT") ? "publish/wlpDirs" : "autoFVT/publish/wlpDirs").getCanonicalFile();
        wlpDir = new File(wlpDirs, "wlp").getCanonicalFile();

        massiveRepoFiles = new File(localPath.endsWith("autoFVT") ? "publish/massiveRepo/files" : "autoFVT/publish/massiveRepo/files").getCanonicalFile();

        wlpUserLibDir = new File(wlpDir, "usr/extension/lib");
        wlpUserLibDirFeatures = new File(wlpUserLibDir, "features");
        wlpUserLibDirFeaturesl10n = new File(wlpUserLibDirFeatures, "l10n");
        wlpUserLibDirFeaturesChecksums = new File(wlpUserLibDirFeatures, "checksums");

        wlpUserBinDir = new File(wlpDir, "usr/extension/bin");
        wlpUserFeaturesDir = new File(wlpUserLibDir, "features");
        wlpFixesDir = new File(wlpDir, "lib/fixes");
        wlpBin = new File(wlpDir, "bin");
        wlpLib = new File(wlpDir, "lib");
        wlpLibFixes = new File(wlpLib, "fixes");
        wlpLibFeatures = new File(wlpLib, "features");
        libFeaturesl10n = new File(wlpLibFeatures, "l10n");
        libFeaturesCheckSums = new File(wlpLibFeatures, "checksums");
        wlpEtc = new File(wlpDir, "etc");
        Log.info(TestUtils.class, "setupWlpDirs", "wlpDirs: " + wlpDirs.getCanonicalPath());
        Log.info(TestUtils.class, "setupWlpDirs", "wlpDevelopersDir: " + wlpDir.getCanonicalPath());
        Log.info(TestUtils.class, "setupWlpDirs", "wlpDevelopersUserLibDir: " + wlpUserLibDir.getCanonicalPath());
        Log.info(TestUtils.class, "setupWlpDirs", "wlpDevelopersUserFeaturesDir: " + wlpUserFeaturesDir.getCanonicalPath());
        Log.info(TestUtils.class, "setupWlpDirs", "wlpFixesDir: " + wlpFixesDir.getCanonicalPath());
        FileInputStream fis = new FileInputStream(new File(wlpDir, "lib/versions/WebSphereApplicationServer.properties"));
        Properties p = new Properties();
        p.load(fis);
        Log.info(TestUtils.class, "setupWlpDirs", "com.ibm.websphere.productEdition: " + p.get("com.ibm.websphere.productEdition"));
        Log.info(TestUtils.class, "setupWlpDirs", "com.ibm.websphere.productVersion: " + p.get("com.ibm.websphere.productVersion"));
        wlpVersion = "" + p.get("com.ibm.websphere.productVersion");
        Log.info(TestUtils.class, "setupWlpDirs", "Set wlpVersion to: " + wlpVersion);
        fis.close();
    }

    public static void removeWlpDirs() {
        if (wlpDirs != null) {
            FileUtils.recursiveDelete(wlpDirs);
            if (wlpDirs.exists())
                Log.info(TestUtils.class, "removeWlpDirs", wlpDirs.getAbsolutePath() + " cannot be removed.");
            else
                Log.info(TestUtils.class, "removeWlpDirs", wlpDirs.getAbsolutePath() + " was removed.");
        }
    }

    public static void verifyFirstLine(File f, String expectedFirstLine) throws MalformedURLException, IOException {
        InputStream is = f.toURI().toURL().openConnection().getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader rd = new BufferedReader(isr);
        assertEquals(expectedFirstLine, rd.readLine());
        rd.close();
        isr.close();
        is.close();
    }

    /**
     * If <installed> is true, asserts that file paths in the <fileList> exists in the wlp directory. Else asserts that the file paths do not exist
     *
     * @param fileList An array of file paths
     * @param installed Boolean that sets the check to existing files or non existing files.
     */
    public static void checkInstalledFiles(String[] fileList, boolean installed) {
        if (installed) {
            ArrayList<String> missingFiles = new ArrayList<String>();
            for (String fileName : fileList) {
                if (!(new File(TestUtils.wlpDir, fileName).exists()))
                    missingFiles.add(fileName);
            }
            assertTrue("Files List : " + missingFiles.toString() + " should exist", missingFiles.isEmpty());
        } else {
            ArrayList<String> extraFiles = new ArrayList<String>();
            for (String fileName : fileList) {
                if (new File(TestUtils.wlpDir, fileName).exists())
                    extraFiles.add(fileName);
            }
            assertTrue("Files List : " + extraFiles.toString() + " should not exist", extraFiles.isEmpty());
        }
    }

    public static void verifyContains(List<String> strings, String[] expected) {
        for (String e : expected) {
            boolean notFound = true;
            for (String s : strings) {
                if (s.contains(e)) {
                    notFound = false;
                }
            }
            if (notFound)
                fail("\"" + e + "\" is missing in the stdout or stderr:" + NEWLINE + toString(strings, NEWLINE));
        }
    }

    private static String toString(List<String> list, String sep) {
        StringBuilder output = new StringBuilder();

        if (!list.isEmpty()) {
            Iterator<String> it = list.iterator();
            output.append(it.next());

            while (it.hasNext()) {
                output.append(sep);
                output.append(it.next());
            }
        }

        return output.toString();
    }

    public static void verifyNotContains(List<String> strings, String[] expected) {
        for (String s : expected) {
            if (strings.contains(s))
                fail("\"" + s + "\" appears in the stdout or stderr.");
        }
    }

    public static class OutputStreamCopier implements Runnable {
        private final InputStream in;
        private final List<String> output;

        public OutputStreamCopier(InputStream in, List<String> lines) {
            this.in = in;
            this.output = lines;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                boolean inEval = false;
                int carryover = 0;

                for (String line; (line = reader.readLine()) != null;) {
                    // Filter empty lines and sh -x trace output.
                    if (inEval) {
                        System.out.println("(trace eval) " + line);
                        if (line.trim().equals("'")) {
                            inEval = false;
                        }
                    } else if (line.equals("+ eval '")) {
                        inEval = true;
                        System.out.println("(trace eval) " + line);
                    } else if (carryover > 0) {
                        carryover--;
                        System.out.println("(trace) " + line);
                    } else if (line.startsWith("+") || line.equals("'")) {
                        int index = 0;
                        index = line.indexOf("+", index + 1);
                        while (index != -1) {
                            index = line.indexOf("+", index + 1);
                            carryover++;
                        }
                        System.out.println("(trace) " + line);
                    } else if (!line.isEmpty()) {
                        synchronized (output) {
                            output.add(line);
                        }
                        System.out.println(line);
                    }
                }
            } catch (IOException ex) {
                throw new Error(ex);
            }
        }
    }

    public static String currentTime() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        return sdf.format(cal.getTime());
    }

    public static void testRepositoryConnection() {
        try {
            String localPath = new File(".").getCanonicalPath();
            File massiveRepoPath = new File(localPath.endsWith("autoFVT") ? "publish/massiveRepo" : "autoFVT/publish/massiveRepo");
            File[] repoPropsFiles = massiveRepoPath.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".props");
                }
            });
            //Randomly ordered the files, to balance the work load for the servers
            Arrays.sort(repoPropsFiles, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return (int) Math.round(Math.random() * 10 - 5);
                }
            });

            String userId;
            String password;
            String url;
            String apiKey;
            String softlayerUserId;
            String softlayerPassword;

            /*
             * use existing connection to create login first, set connectedToRepo for results;
             */
            if (connectedRepoProperties != null) {
                userId = connectedRepoProperties.get("userId");
                password = connectedRepoProperties.get("password");
                url = connectedRepoProperties.get("repository.url");
                apiKey = connectedRepoProperties.get("apiKey");
                softlayerUserId = connectedRepoProperties.get("softlayerUserId");
                softlayerPassword = connectedRepoProperties.get("softlayerPassword");

                RestRepositoryConnection loginInfo = new RestRepositoryConnection(userId, password, apiKey, url, softlayerUserId, softlayerPassword);
                Log.info(TestUtils.class, "testRepositoryConnection", "Connecting to apikey = " + loginInfo.getApiKey() + " repository url = " + loginInfo.getRepositoryUrl());
                connectedToRepo = loginInfo.isRepositoryAvailable() && isRepoEnabled(loginInfo);
                if (connectedToRepo) {
                    Log.info(TestUtils.class, "testRepositoryConnection", "Repository connection is OK.");
                } else {
                    Log.info(TestUtils.class, "testRepositoryConnection", "Cannot connect to the repository");
                }
            }

            //fail over part
            if (connectedRepoProperties == null || connectedToRepo == false) {
                for (File f : repoPropsFiles) {
                    Properties p = new Properties();
                    p.load(new FileInputStream(f));

                    // Load basic properties, set to null if blank
                    userId = p.getProperty("userId");
                    password = p.getProperty("password");
                    url = p.getProperty("repository.url");
                    apiKey = p.getProperty("apiKey");
                    softlayerUserId = p.getProperty("softlayerUserId");
                    softlayerPassword = p.getProperty("softlayerPassword");

                    RestRepositoryConnection loginInfo = new RestRepositoryConnection(userId, password, apiKey, url, softlayerUserId, softlayerPassword);
                    Log.info(TestUtils.class, "testRepositoryConnection",
                             "Connecting to apikey = " + loginInfo.getApiKey() + " repository url = " + loginInfo.getRepositoryUrl());
                    connectedToRepo = loginInfo.isRepositoryAvailable() && isRepoEnabled(loginInfo);
                    if (connectedToRepo) {
                        Log.info(TestUtils.class, "testRepositoryConnection", "Repository connection is OK.");
                        repositoryDescriptionUrl = f.getCanonicalFile().toURI().toURL().toString();
                        connectedRepoProperties = new HashMap<String, String>();
                        connectedRepoProperties.put("userId", userId);
                        connectedRepoProperties.put("password", password);
                        connectedRepoProperties.put("repository.url", url);
                        connectedRepoProperties.put("apiKey", apiKey);
                        connectedRepoProperties.put("softlayerUserId", softlayerUserId);
                        connectedRepoProperties.put("softlayerPassword", softlayerPassword);
                        return;
                    } else {
                        Log.info(TestUtils.class, "testRepositoryConnection", "Cannot connect to the repository");
                    }
                }
            }

        } catch (Exception e) {
            Log.info(TestUtils.class, "testRepositoryConnection", "Cannot connect to the repository : " + e.getMessage());
            connectedToRepo = false;
        } finally {
            MainRepository.clearCachedRepoProperties();
        }
    }

    public static Boolean isRepoEnabled(RestRepositoryConnection lie) {
        return isRepoEnabled(new RepositoryConnectionList(lie));
    }

    //If IFIX 8550-disable-repository exist and it's in PUBLISHED state, then it's in disabled state
    public static Boolean isRepoEnabled(RepositoryConnectionList login) {
        try {
            ResourceType type = ResourceType.IFIX;
            for (RepositoryResourceImpl msf : (Collection<RepositoryResourceImpl>) login.getAllResources(type)) {
                if (msf.getName().equals("8550-disable-repository")
                    && msf.getState().equals(State.PUBLISHED))
                    return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void cleanLogger() {
        Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);
        Handler[] handlers = logger.getHandlers();
        int i = 0;
        for (Handler h : handlers) {
            if (h instanceof ConsoleHandler) {
                if (i > 0)
                    logger.removeHandler(h);
                i++;
            }
        }
    }

    public static boolean noLogger() {
        Handler[] handlers = Logger.getLogger(InstallConstants.LOGGER_NAME).getHandlers();
        return handlers.length == 0;
    }

    public static void copyFile(File originalFile, File destFile) {
        copyFile(originalFile, destFile, false);
    }

    /**
     * This method copies the original file to the new destination.
     *
     * @param originalFile - The file to copy
     * @param destFile - The location of the new file.
     */
    public static void copyFile(File originalFile, File destFile, boolean appendToDest) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(originalFile);
            fos = new FileOutputStream(destFile, appendToDest);

            byte[] bytes = new byte[4096];
            int readBytes;
            while ((readBytes = fis.read(bytes)) >= 0) {
                fos.write(bytes, 0, readBytes);
            }
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static File packageIfixToVersion(String ifixID, String VERSION) throws Exception {//expecting version to have dots
        String localPath = new File(".").getCanonicalPath();
        File localIfixRepo = new File(localPath.endsWith("autoFVT") ? "publish/massiveRepo/localRepo" : "autoFVT/publish/massiveRepo/localRepo").getCanonicalFile();
        Log.info(TestUtils.class, "packageIfixToVersion", "localIfixRepo=" + localIfixRepo.getAbsolutePath());
        String VERSIONNODOTS = VERSION.replace(".", "");
        String TOLERANCE = "[" + VERSION + "," + VERSION.substring(0, VERSION.length() - 1) + (Integer.parseInt(VERSION.substring(VERSION.length() - 1)) + 1) + ")";
        String jarName = VERSIONNODOTS + "-wlp-archive-" + ifixID;
        String name = jarName + "_" + VERSION;
        if (new File(localIfixRepo, jarName + ".jar").exists()) {
            System.out.println("Ifix " + ifixID + " already exists at version= " + VERSION);
            return new File(localIfixRepo, jarName + ".jar");
        } else {

            String timeStamp = currentTime().replace(":", "").replace(" ", "");
            File workingDir = new File(localIfixRepo, "tempDir-" + timeStamp);
            Log.info(TestUtils.class, "packageIfixToVersion", "working dir:" + workingDir.getAbsolutePath().toString());
            workingDir.mkdirs();

            File template = new File(localIfixRepo, ifixID + ".jar");
            File jar = new File(workingDir, ifixID + ".jar");
            File mf = new File(workingDir + File.separator + "META-INF", "MANIFEST.MF");
            File xml = new File(workingDir + File.separator + "lib" + File.separator + "fixes", "xml.xml");
            File lpmf = new File(workingDir + File.separator + "lib" + File.separator + "fixes", "lpmf.lpmf");

            //1. extract zip containing jar, mf, xml and lpmf files, confirm successful extraction
            unzip(template, workingDir);

            Log.info(TestUtils.class, "packageIfixToVersion", "template MF=" + mf.getAbsolutePath() + "    existence=" + mf.exists());
            assertTrue("mf doesn't exist", mf.exists());
            Log.info(TestUtils.class, "packageIfixToVersion", "template XML=" + xml.getAbsolutePath() + "    existence=" + xml.exists());
            assertTrue("xml doesn't exist", xml.exists());
            Log.info(TestUtils.class, "packageIfixToVersion", "template LPMF=" + lpmf.getAbsolutePath() + "    existence=" + lpmf.exists());
            assertTrue("lpmf doesn't exist", lpmf.exists());

            //2. modify files
            //A. add versions
            String mfText = readTextFile(mf);
            mfText = mfText.replaceAll("VERSIONDOTS", VERSION);
            //clear file
            writeLineToFile(mf, "", false, false);
            //write new lines
            writeLineToFile(mf, mfText.trim(), false, false);

            String xmlText = readTextFile(xml);
            xmlText = xmlText.replaceAll("VERSIONDOTS", VERSION);
            xmlText = xmlText.replaceAll("VERSIONNODOTS", VERSIONNODOTS);
            xmlText = xmlText.replaceAll("TOLERANCE", TOLERANCE);
            //clear file
            writeLineToFile(xml, "", false, false);
            //write new lines
            writeLineToFile(xml, xmlText, false, false);

            //B. rename files
            File newXml = new File(workingDir + File.separator + "lib" + File.separator + "fixes", name + ".xml");
            File newLpmf = new File(workingDir + File.separator + "lib" + File.separator + "fixes", name + ".lpmf");

            xml.renameTo(newXml);
            lpmf.renameTo(newLpmf);

            //3. update jar with new mf, xml and lpmf
            System.out.println("java home property=" + System.getProperty("java.home"));
            String jarExecute = locateJarExe();
            System.out.println("jar command path=" + jarExecute);
            assertFalse("jar executable cannot be found in java_home or the bin directory in its parent folder", jarExecute.equalsIgnoreCase("bad path"));
            //String jarExecute = "C:\\java_1.7\\sdk\\bin\\jar";
            String flags = "cvmf";
            String mfPath = "META-INF" + File.separator + "MANIFEST.MF";

            File[] workingDirFiles = workingDir.listFiles();
            String[] jarCmdArray = { jarExecute, flags, mfPath, localIfixRepo + File.separator + jarName + ".jar" };

            final String[] runtimeCmd = generateJarCommand(workingDirFiles, jarCmdArray);
            System.out.println("cmd=" + Arrays.toString(runtimeCmd).replace("[", "").replace("]", "").replace(",", ""));

            int rc = runJarCmd(workingDir, runtimeCmd);
            System.out.println("rc=" + rc);
            assertTrue("ifix package rc=" + rc, rc == 0);

            //4. set jar executable
            File newJar = new File(localIfixRepo, jarName + ".jar");
            newJar.setExecutable(true);

            //5. remove workingDir
            recursiveDirDelete(workingDir);

            return newJar;
        }
    }

    private static String locateJarExe() {
        File javaHome = new File(System.getProperty("java.home"));
        System.out.println("original jar home=" + javaHome.toString());
        String[] pathParts = javaHome.toString().split(File.separator + File.separator);//split path up by file.separator, needs escape character of another File.seperator

        System.out.println("TRY CURRENT JAVA HOME FOR JAR");
        File[] original = javaHome.listFiles();
        for (File a : original) {
            if (a.getName().split("\\.")[0].equalsIgnoreCase("jar")) {//ignore .exe extensions
                System.out.println("FOUND JAR IN CURRENT JAR HOME");
                return a.toString();
            }
        }
        System.out.println("JAR NOT IN CURRENT JAVA HOME");
        String newPath = javaHome.toString().substring(0, javaHome.toString().length() - 3) + "bin";//remove /jre, add /bin to look for jar exe
        System.out.println("NEW PATH=" + newPath);
        File[] newFileList = new File(newPath).listFiles();
        for (File b : newFileList) {
            if (b.getName().split("\\.")[0].equalsIgnoreCase("jar")) {//ignore .exe extensions so we are just checking for a file named "jar"
                System.out.println("FOUND JAR IN NEW PATH");
                return b.toString();
            }
        }
        System.out.println("JAR NOT IN NEW PATH");
        return "bad path";
    }

    private static String[] generateJarCommand(File[] workingDirFiles, String[] jarCmdArray) {
        int length = workingDirFiles.length + jarCmdArray.length;
        String[] rtnArray = new String[length];
        int count = 0;
        for (String a : jarCmdArray) {
            rtnArray[count] = a;
            System.out.println(count + " Adding " + a + " to command Array");
            count++;
        }
        for (File b : workingDirFiles) {
            rtnArray[count] = b.getName() + File.separator;
            System.out.println(count + " Adding " + b.getName() + " to command Array");
            count++;
        }
        return rtnArray;
    }

    private static int runJarCmd(File dir, String[] runtimeCmd1) throws IOException, InstallException {
        System.out.println("Executing command=" + Arrays.toString(runtimeCmd1));
        System.out.println("java.home=" + System.getProperty("java.home"));
        int exitVal = -555555555;
        try {
            Runtime runtime = Runtime.getRuntime();

            Process process = runtime.exec(runtimeCmd1, null, dir);

            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            System.out.println("START");
            while ((line = input.readLine()) != null) {
                System.out.println(line);
            }
            exitVal = process.waitFor();
            System.out.println("FINISH");
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        return exitVal;
    }

    //writes a line to a file
    public static boolean writeLineToFile(File filePath, String lineToWrite, Boolean newLine, Boolean appendToFile) throws IOException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filePath, appendToFile));
            if (newLine) {
                writer.write("\n" + lineToWrite);
            } else {
                writer.write(lineToWrite);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
                return true;
            } catch (Exception e) {

            }
        }
        return false;
    }

    public static String readTextFile(File txtFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(txtFile.toString()));
        String txtLine = "";
        if (txtFile.exists()) {
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    sb.append('\n');
                    line = br.readLine();
                }
                txtLine = sb.toString().trim();
            } finally {
                br.close();
            }
            return txtLine;
        } else {
            return "File does not exist";
        }
    }

    private static void unzip(File fSourceZip, File temp) {

        try {
            ZipFile zipFile = new ZipFile(fSourceZip);
            Enumeration e = zipFile.entries();

            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                File destinationFilePath = new File(temp, entry.getName());

                //create directories if required.
                destinationFilePath.getParentFile().mkdirs();

                //if the entry is directory, leave it. Otherwise extract it.
                if (entry.isDirectory()) {
                    continue;
                } else {
                    //System.out.println("Extracting " + destinationFilePath);

                    /*
                     * Get the InputStream for current entry
                     * of the zip file using
                     *
                     * InputStream getInputStream(Entry entry) method.
                     */
                    BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));

                    int b;
                    byte buffer[] = new byte[1024];

                    /*
                     * read the current entry from the zip file, extract it
                     * and write the extracted file.
                     */
                    FileOutputStream fos = new FileOutputStream(destinationFilePath);
                    BufferedOutputStream bos = new BufferedOutputStream(fos, 1024);

                    while ((b = bis.read(buffer, 0, 1024)) != -1) {
                        bos.write(buffer, 0, b);
                    }

                    //flush the output stream and close it.
                    bos.flush();
                    bos.close();

                    //close the input stream.
                    bis.close();
                }
            }
        } catch (IOException ioe) {
            System.out.println("IOError :" + ioe);
        }

    }

    //recursively deletes contents of a directory
    public static void recursiveDirDelete(File file) throws IOException {

        if (file.isDirectory()) {

            //directory is empty, then delete it
            if (file.list().length == 0) {

                file.delete();
                // System.out.println("Directory is deleted : " + file.getAbsolutePath());

            } else {

                //list all the directory contents
                String files[] = file.list();

                for (String temp : files) {
                    //construct the file structure
                    File fileDelete = new File(file, temp);

                    //recursive delete
                    recursiveDirDelete(fileDelete);
                }

                //check the directory again, if empty then delete it
                if (file.list().length == 0) {
                    file.delete();
                    //  System.out.println("Directory is deleted : "  + file.getAbsolutePath());
                }
            }

        } else {
            //if file, then delete it
            file.delete();
            //System.out.println("File is deleted : " + file.getAbsolutePath());
        }
    }

    public static int localIfixInstall(String jarName, String ifixID, String version) throws Exception {
        String libFixes = "lib" + File.separator + "fixes";
        System.out.println("libFixes=" + libFixes);
        File generatedIfixPath = packageIfixToVersion(jarName, version);
        System.out.println("generatedIfixPath=" + generatedIfixPath.toString());

        Product product = new Product(TestUtils.wlpDir);
        FixAsset fixAsset = new FixAsset(ifixID, generatedIfixPath, true);
        return install(product, fixAsset);
    }

    public static int install(Product product, FixAsset fixAsset) throws IOException, InstallException {
        Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);

        File java = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        final String[] runtimeCmd = { java.getAbsolutePath(), "-jar", fixAsset.getJarPath(), product.getInstallDir().getAbsolutePath() };
        logger.log(Level.FINE, java.getAbsolutePath() + " -jar " + fixAsset.getJarPath() + " " + product.getInstallDir().getAbsolutePath());
        logger.log(Level.FINE, "Fix Adaptor Install Command= " + Arrays.toString(runtimeCmd));
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
                // throw new InstallException(InstallUtils.getMessage("ERROR_FAILED_TO_INSTALL_FIX", fixAsset.id));
            }
        } catch (InterruptedException e) {
            throw new InstallException(e.getMessage(), e.getCause(), InstallException.RUNTIME_EXCEPTION);
        } finally {
            logger.log(Level.FINE, "stdout/stderr: " + stdout.toString());
            logger.log(Level.FINE, "exit code: " + process.exitValue());
        }
        return process.exitValue();
    }

    public static void cleanupFiles(List<File> files) {
        for (File f : files) {
            if (f.exists()) {
                if (f.isDirectory())
                    FileUtils.recursiveDelete(f);
                else
                    InstallUtils.delete(f);
            }
        }
    }

    public static void verifyFiles(List<File> files, boolean exists) {
        for (File f : files) {
            assertEquals(f.getAbsolutePath() + " should " + (exists ? "exist" : "not exist"), exists, f.exists());
        }
    }
}
