/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package componenttest.aries;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.ibm.websphere.simplicity.WebServer;
import com.ibm.ws.topology.exceptions.TopologyException;
import com.ibm.ws.topology.helper.OSGiApplication;
import com.ibm.ws.topology.helper.SecurityConfigurator;
import com.ibm.ws.topology.helper.WebClient;
import com.ibm.ws.topology.helper.impl.WASSecurityConfigurator;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 */
public class TopologyImpl implements AriesTopologyForLiberty {
    private static List<String> _expectedBundles = new ArrayList<String>();
    private final LibertyServer _server;
    private final Logger _LOGGER;
    private String _IBRDirectory = "wabIBR";

    private final Hashtable<String, String> _installedApps = new Hashtable<String, String>();

    public TopologyImpl(Logger log, String serverName) throws Exception {
        _LOGGER = log;
        _server = LibertyServerFactory.getLibertyServer(serverName);
        writeUrlFile();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws Exception
     */
    @Override
    public void startLibertyServer() throws Exception {
        _server.startServer();
    }

    @Override
    public Logger getLogger() {
        return _LOGGER;
    }

    /**
     * Write the URL file for use by the framework
     * 
     * @throws TopologyException
     */
    private void writeUrlFile() throws TopologyException {
        File addressDir = new File(getServerRoot() + "/testarea/test");
        addressDir.mkdirs();
        File address = new File(addressDir, "address.url");
        try {
            FileWriter fw = new FileWriter(address);
            fw.write(_server.getHostname() + ":" + getDefaultWCPort());
            fw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    public int getDefaultWCPort() {
        return _server.getHttpDefaultPort();
    }

    /** {@inheritDoc} */
    @Override
    public int getAdminWCPort() {
        return _server.getHttpDefaultPort();
    }

    /** {@inheritDoc} */
    @Override
    public int getDefaultWCSecurePort() {
        return _server.getHttpDefaultSecurePort();
    }

    /** {@inheritDoc} */
    @Override
    public String getHost() throws TopologyException {
        return _server.getHostname();
    }

    /**
     * This method checks the status of the tests for completion every second
     * unitl the timeout is exceeded.
     * 
     * @param customTimeout - the time until we give up in ms
     * @return boolean indicating if all tests completed
     */
    @Override
    public boolean checkForComponentTestFinish(long customTimeout) throws InterruptedException,
                    TopologyException {
        return checkForComponentTestFinish(customTimeout, false);
    }

    private boolean loggingReady() throws TopologyException, IOException {
        boolean loggingReady = false;
        List<String> actualFinishedBundles = checkFinished();
        if (actualFinishedBundles.contains("com.ibm.componenttest.logging")) {
            _LOGGER.log(Level.INFO, "Logging bundle is ready.");
            loggingReady = true;
        }
        return loggingReady;
    }

    private boolean testBundleStatus() throws TopologyException, IOException {
        boolean testsComplete = false;
        List<String> actualFinishedBundles = checkFinished();
        if (actualFinishedBundles.isEmpty()) {
            _LOGGER.log(Level.INFO, "No completed bundles");
        } else {
            _LOGGER.log(Level.INFO, "Finished bundles: " + actualFinishedBundles);
        }
        List<String> expectedFinishedBundles = getExpected();
        if (allBundlesFinished(actualFinishedBundles, expectedFinishedBundles)) {
            _LOGGER.log(Level.INFO, "All expected bundles have finished.");
            testsComplete = true;
        }

        return testsComplete;
    }

    private List<String> getExpected() {
        return _expectedBundles;
    }

    private boolean allBundlesFinished(List<String> finished, List<String> expected) {
        Collections.sort(finished);
        Collections.sort(expected);

        return finished.equals(expected);
    }

    private List<String> checkFinished() throws TopologyException, IOException {
        HttpURLConnection conn = getConnection();
        List<String> finishedBundles = null;
        //timeout if we don't get any data back in 1 second
        //the IOException thrown on timeout will be caught and
        //we will try again to get the data
        conn.setReadTimeout(1000);

        InputStream urlIS = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        String csList = null;
        try {
            urlIS = conn.getInputStream();
            isr = new InputStreamReader(urlIS);
            br = new BufferedReader(isr);
            csList = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null)
                br.close();
            if (urlIS != null)
                urlIS.close();
        }

        //if the line we read isn't empty get the data
        if (!!!(csList == null || csList.equals(""))) {

            //remove the opening [ and closing ]
            csList = csList.substring(1, csList.length() - 1);
            String[] finishedBundlesArray = csList.split(", ");

            finishedBundles = new ArrayList<String>(finishedBundlesArray.length);
            for (String s : finishedBundlesArray) {
                finishedBundles.add(s);
            }
        }

        if (finishedBundles == null) {
            //if null return an empty list
            finishedBundles = new ArrayList<String>(0);
        }

        return finishedBundles;
    }

    private HttpURLConnection getConnection() throws IOException {
        String urlAddress = "http://" + _server.getHostname() + ":" + getDefaultWCPort()
                            + "/ComponentTestStatusServlet/";
        return getConnection(urlAddress);
    }

    @Override
    public HttpURLConnection getConnection(String urlAddress) throws IOException {
        HttpURLConnection cnxn = null;
        URL url = new URL(urlAddress);
        cnxn = (HttpURLConnection) url.openConnection();
        if (cnxn == null) {
            throw new IOException(
                            "No connection to component test status servlet");
        }
        cnxn.setRequestMethod("GET");
        cnxn.setAllowUserInteraction(false);
        cnxn.setDoInput(true);
        return cnxn;
    }

    /** {@inheritDoc} */
    @Override
    public OSGiApplication installEBA(String EBAName, String filename) throws TopologyException {
        // TODO Auto-generated method stub
        return installEBA(EBAName, filename, null);
    }

    private void copyStream(InputStream is, OutputStream os) throws IOException {
        copyStream(is, os, true);
    }

    private void copyStream(InputStream is, OutputStream os, boolean closeOutput) throws IOException {
        byte[] buf = new byte[8096];
        int len = 0;
        while ((len = is.read(buf)) > 0) {
            os.write(buf, 0, len);
        }

        if (closeOutput) {
            os.close();
        }
        is.close();
    }

    private void extractFile(ZipFile source, String fileToExtract, String extractTarget) throws IOException {
        ZipEntry entry = source.getEntry(fileToExtract);
        if (entry == null) {
            _LOGGER.log(Level.WARNING, "Could not find " + fileToExtract + " inside " + source.getName());
        }
        InputStream is = source.getInputStream(entry);
        File f = new File(extractTarget);
        f.getParentFile().mkdirs();
        if (!f.getParentFile().exists()) {
            throw new IOException("Unable to create directory " + f.getParent());
        }
        FileOutputStream warOS = new FileOutputStream(extractTarget);
        copyStream(is, warOS);
        _LOGGER.log(Level.INFO, "extracted " + fileToExtract + " to " + extractTarget);
    }

    private void injectFilesIntoArchive(String sourceArchiveFileName, String targetArchiveFileName,
                                        InjectionEntry entry, String tmpDirName) throws IOException {
        File tmpDir = new File(tmpDirName);
        if (!tmpDir.exists()) {
            if (!tmpDir.mkdirs()) {
                _LOGGER.log(Level.WARNING, "Failed to create tmp dir " + tmpDirName);
                return;
            }
        }

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(targetArchiveFileName));
        try {
            _LOGGER.log(Level.INFO, "Reading " + sourceArchiveFileName);

            ZipFile sourceFile = new ZipFile(sourceArchiveFileName);
            Enumeration<? extends ZipEntry> enumWarContents = sourceFile.entries();
            while (enumWarContents.hasMoreElements()) {
                ZipEntry nextEntry = enumWarContents.nextElement();

                // Don't copy files that we are overriding
                if (!(entry.containsFile(nextEntry.getName()))) {
                    zos.putNextEntry(new JarEntry(nextEntry.getName()));
                    // dont copy dirs
                    if (!nextEntry.isDirectory()) {
                        copyStream(sourceFile.getInputStream(nextEntry), zos, false);
                    }
                }
            }

            // Deal with any nested entries
            Enumeration<String> enumNested = entry.getEnumNestedEntries();
            while (enumNested.hasMoreElements()) {
                String nestedFile = enumNested.nextElement();
                _LOGGER.log(Level.INFO, "Nested " + nestedFile);
                String extractTarget = tmpDirName + nestedFile;
                extractFile(sourceFile, nestedFile, extractTarget + ".tmp");
                injectFilesIntoArchive(extractTarget + ".tmp", tmpDirName + nestedFile, entry.getUpdatedNestedEntry(nestedFile),
                                       tmpDirName + nestedFile + "tmp/");
                ZipEntry ze = new ZipEntry(nestedFile);
                zos.putNextEntry(ze);
                File fileToInject = new File(extractTarget);
                _LOGGER.log(Level.INFO, "reading nested file from " + fileToInject.getAbsolutePath());
                FileInputStream manifestIS = new FileInputStream(fileToInject);
                copyStream(manifestIS, zos, false);
            }

            // Go through any files that need updating
            Enumeration<String> enumDestinations = entry.getEnumFilesToUpdate();
            while (enumDestinations.hasMoreElements()) {
                String destinationForFile = enumDestinations.nextElement();
                String fileNameToInject = entry.getUpdatedFile(destinationForFile);
                ZipEntry ze = new ZipEntry(destinationForFile);
                zos.putNextEntry(ze);
                File fileToInject = new File(fileNameToInject);
                _LOGGER.log(Level.INFO, "reading file from " + fileToInject.getAbsolutePath());
                FileInputStream manifestIS = new FileInputStream(fileToInject);
                copyStream(manifestIS, zos, false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            zos.close();
        }
        _LOGGER.log(Level.INFO, "finished creating " + targetArchiveFileName);
    }

    @Override
    public OSGiApplication installEBA(String EBAName, String filename, Properties props) throws TopologyException {
        _LOGGER.log(Level.INFO, "Installing " + EBAName + " from " + filename);

        if (null == props) {
            props = new Properties();
        }

        String EBAToCopyToLiberty = filename;

        if (null != props && props.size() > 0) {
            InjectionEntry entry = (InjectionEntry) props.get(InjectionEntry.INJECTION_PROP);

            if (null != entry) {
                String tmpDirName = _server.pathToAutoFVTTestFiles + "/tmp/";

                try {

                    injectFilesIntoArchive(_server.pathToAutoFVTTestFiles + "/" + filename, tmpDirName + filename, entry, tmpDirName);

                    // autoFVT/lib/tmp/<fileName> Note we don't use newEBA here as the "copyToLibertyServer"
                    // method uses the autoFVT/lib directory as its base rather than the current directory
                    // (autoFVT).
                    EBAToCopyToLiberty = "tmp/" + filename;
                } catch (IOException ioe) {

                    ioe.printStackTrace();

                }
            }

        }

        try {
            _LOGGER.log(Level.INFO, "Install file " + EBAToCopyToLiberty);
            String installTo = props.getProperty("location");
            if (null == installTo) {
                installTo = "dropins";
            }

            boolean eitherOr = props.containsKey("update_or_install");
            Integer waitFor = (Integer) props.get("wait_for_install");
            Integer updateWait = (Integer) props.get("wait_for_update");

            //very quick check to see whats there before copying file in.. 
            int initialInstallCount = _server.waitForMultipleStringsInLog(999, "CWWKZ0001I.* " + EBAName, 1000);
            _LOGGER.log(Level.INFO, "Initial count of  " + initialInstallCount + " installed hits for <" + EBAName + "> (test wanted " + String.valueOf(waitFor) + ")");
            int initialUpdateCount = _server.waitForMultipleStringsInLog(999, "CWWKZ0003I.* " + EBAName, 1000);
            _LOGGER.log(Level.INFO, "Initial count of  " + initialUpdateCount + "   updated hits for <" + EBAName + "> (test wanted " + String.valueOf(updateWait) + ")");
            int initialEitherCount = _server.waitForMultipleStringsInLog(999, "(CWWKZ0001I|CWWKZ0003I).* " + EBAName, 1000);
            _LOGGER.log(Level.INFO, "Initial count of  " + initialEitherCount + "  combined hits for <" + EBAName + "> ");

            _server.copyFileToLibertyServerRoot(installTo, EBAToCopyToLiberty);

            if (null != waitFor) {
                if (waitFor > 0) {
                    int wantedCount = initialInstallCount + waitFor;
                    String pattern = "CWWKZ0001I.* " + EBAName;

                    if (eitherOr) {
                        pattern = "(CWWKZ0001I|CWWKZ0003I).* " + EBAName;
                        wantedCount = initialEitherCount + waitFor; //waitFor & updateWait SHOULD match if using eitheror
                        _LOGGER.log(Level.INFO, "Using either/or pattern of " + pattern + " and wanted of " + wantedCount);
                    }

                    int hits = _server.waitForMultipleStringsInLog(wantedCount, pattern, 60000);

                    if (hits != wantedCount) {
                        _LOGGER.log(Level.SEVERE, "Found " + hits + " installed hits but expected " + wantedCount + " for <" + EBAName + ">");
                        assertEquals("Incorrect number of install messages found ", wantedCount, hits);
                    }
                }
            } else {
                assertNotNull("The application " + EBAName + "  did not appear to have started.",
                              _server.waitForStringInLog("CWWKZ0001I.* " + EBAName, 60000));
            }

            if (null != updateWait) {
                //skip the either/or case, since would have been handled above.
                if (updateWait > 0 && !eitherOr) {
                    int wantedCount = initialUpdateCount + updateWait;
                    int hits = _server.waitForMultipleStringsInLog(wantedCount, "CWWKZ0003I.* " + EBAName, 60000);
                    if (hits != wantedCount) {
                        _LOGGER.log(Level.SEVERE, "Found " + hits + " update hits but expected " + wantedCount + " for <" + EBAName + ">");
                        assertEquals("Incorrect number of update messages found ", wantedCount, hits);
                    }
                }
            }

            _LOGGER.log(Level.INFO, "Install complete");
            _installedApps.put(EBAName, installTo + "/" + filename);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public OSGiApplication installEBAContainingWAR(String EBAName, String filename, Properties props)
                    throws TopologyException {
        return installEBA(EBAName, filename, props);
    }

    @Override
    public void uninstallEBA(String EBAName, String filename) throws TopologyException {
        uninstallEBA(EBAName, filename, new Properties());
    }

    /** {@inheritDoc} */
    @Override
    public void uninstallEBA(String EBAName, String filename, Properties uninstallProps) throws TopologyException {
        try {
            if (_installedApps.get(EBAName) != null) {
                _LOGGER.log(Level.INFO, "Uninstalling " + EBAName);
                int startHits = 0;
                boolean waitFor = uninstallProps.containsKey("wait_for_uninstall");
                if (waitFor) {
                    startHits = _server.waitForMultipleStringsInLog(999, "CWWKZ0009I.* " + EBAName, 1000);
                    _LOGGER.log(Level.INFO, " - Initial count for uninstall of " + EBAName + " was " + startHits);
                }
                _server.deleteFileFromLibertyServerRoot(_installedApps.get(EBAName));
                if (waitFor) {
                    int endHits = _server.waitForMultipleStringsInLog((startHits + 1), "CWWKZ0009I.* " + EBAName, 60000);
                    _LOGGER.log(Level.INFO, " - Final count for uninstall of " + EBAName + " was " + endHits);
                    if (endHits != (startHits + 1)) {
                        _LOGGER.log(Level.SEVERE, "Found " + endHits + " hits but expected " + (startHits + 1));
                    }
                } else {
                    assertNotNull("The application " + EBAName + "did not appear to have been stopped after deletion.",
                                  _server.waitForStringInLog("CWWKZ0009I.* " + EBAName, 60000));
                }
                _installedApps.remove(EBAName);
                _LOGGER.log(Level.INFO, "Uninstall complete for " + EBAName);
            } else {
                _LOGGER.log(Level.INFO, "Unable to satisfy request to uninstall EBAName " + EBAName + " as it is not known in the current installedApps map");
            }
        } catch (Throwable e) {
            _LOGGER.log(Level.SEVERE, "Uninstall failed for " + EBAName);
            e.printStackTrace();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void installIntoInternalRepository(String bundleName) throws Exception {
        _LOGGER.log(Level.INFO, "Installing " + bundleName + " to the IBR ");

        try {
            _server.copyFileToLibertyServerRoot(_IBRDirectory, bundleName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** {@inheritDoc} */
    @Override
    public SecurityConfigurator getSecurityConfigurator() {
        return new WASSecurityConfigurator(this);
    }

    /**
     * --------------------------------------------------------------------------------------------------
     * Methods from AriesTopologyForLiberty interface
     * --------------------------------------------------------------------------------------------------
     */
    @Override
    public void setIBR(String IBRDir) {
        _IBRDirectory = IBRDir;
    }

    @Override
    public LibertyServer getServer() {
        return _server;
    }

    @Override
    public String getServerRoot() {
        return _server.getServerRoot();
    }

    @Override
    public void addExpectedBundle(String bundleName) {
        _expectedBundles.add(bundleName);
    }

    @Override
    public void addExpectedBundle(String bundleName, int nTimes) {
        for (int i = 0; i < nTimes; i++) {
            _expectedBundles.add(bundleName);
        }
    }

    /**
     * This version is internal and takes a boolean for the special case of checking whether we are
     * ready to being a component test (i.e. the server is started).
     * 
     * @param customTimeout
     * @param checkingReady
     * @return boolean indicating if all tests completed
     * @throws InterruptedException
     * @throws TopologyException
     */
    public boolean checkForComponentTestFinish(long customTimeout, boolean checkingReady)
                    throws InterruptedException, TopologyException {
        long startTime = System.currentTimeMillis();
        boolean testsComplete = false;
        _LOGGER.log(Level.INFO, "Expecting bundles: " + _expectedBundles);
        //loop until the tests are completed or we have exceeded the timeout
        while (!testsComplete && ((customTimeout - (System.currentTimeMillis() - startTime) > 0))) {
            try {
                if (checkingReady) {
                    //in this special case we are checking that the server is ready
                    testsComplete = loggingReady();
                } else {
                    testsComplete = testBundleStatus();
                }
            } catch (IOException e) {
                _LOGGER.log(Level.WARNING, "IOException contacting status servlet: " + e.getMessage());
                /*
                 * we catch IOExceptions within the loop because we might just be too early
                 * for the servlet to be ready, so we move on to the next iteration and try again
                 */
            }
            //if we're going round the loop again then pause for 1 s so
            // we don't keep hammering the servlet
            if (!testsComplete)
                Thread.sleep(1000);
        }
        return testsComplete;
    }

    /**
     * This method checks if we are ready to start installing and running component tests.
     * It does this by checking if the commong logging bundle has reported as as finished.
     * The logging bundle reports finished when its init-method of loggingActive is run.
     * 
     * @param customTimeout
     * @throws Exception
     */
    @Override
    public void checkReady(long customTimeout) throws InterruptedException, TopologyException {
        checkForComponentTestFinish(customTimeout, true);
    }

    /**
     * --------------------------------------------------------------------------------------------------
     * UNIMPLEMENTED METHODS
     * --------------------------------------------------------------------------------------------------
     */

    /** {@inheritDoc} */
    @Override
    public void addExtensions(String arg0, String... arg1) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String adminConfigSave() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void applyUpdates(String arg0, String arg1, SaveConfig arg2) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void clearBundleCache() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void clearBundleCache_Repositories() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String copyFileToTestDir(String arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String copyRemoteFileToLocal(String arg0, String arg1) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void createDerbyDatabase(String arg0, String arg1, String arg2, boolean arg3, String arg4, File arg5) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void createPhysicalDatabase(String arg0, File arg1) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean createWASProfile(String arg0, String arg1, String arg2, String arg3, String arg4) throws TopologyException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void createj2cAuthData(String arg0, String arg1, String arg2) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteWASProfile(String arg0) throws TopologyException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean disableJava2Security() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void disableServletCaching(String arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean doesConfigFileExist(String arg0) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean enableJava2Security() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void enableServletCaching(String arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String executeConsoleCommand(String arg0) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String executeWsadminCommand(String arg0) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String executeWsadminCommand(String arg0, boolean arg1) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String executeWsadminScript(File arg0) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String executeWsadminScript(File arg0, Properties arg1) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String executeWsadminScript(File arg0, String... arg1) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String generateMigrationPortDefProperties() throws TopologyException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String getCUUpdateStatus(String arg0) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String getCellName() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public File getNodeMetadataFile() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String getNodeName() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public int getRMIPort() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public int getSIBEndpoint() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public File getServerLogs() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String getSystemExternalBundleRepository() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String getTestDir() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public File getTestLog(String arg0) throws TopologyException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String getWASInstallRoot() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public String getWASProfileRoot() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getWASProfiles() throws TopologyException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public WebClient getWebClient() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public int getWebServerPort() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public WebServer getWebServerToUse() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void initWebServer() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public OSGiApplication installEBAContainingWARDefaultBindings(String arg0, String arg1, Properties arg2) throws TopologyException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean installEar(String arg0, String arg1, Properties arg2) throws TopologyException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void installServlet(String arg0, String arg1, String arg2, String arg3, String arg4) throws TopologyException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean iszos() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public List<String> listApps() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean migrationCleanup(File arg0, String arg1, String arg2) throws TopologyException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean migrationSetup(File arg0, String arg1, String arg2, String arg3, String arg4, String arg5, String arg6, boolean arg7) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void postTestingTidyUp() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean putNodeMetadataFile(String arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void refreshCache() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean remoteFileExists(String arg0) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void removeExtensions(String arg0, String... arg1) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void restart() throws TopologyException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void restartBLA(String arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean runPostMigration(File arg0, String arg1, String arg2, String arg3, String arg4) throws TopologyException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void setSystemProperty(String arg0, String arg1) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void setTraceOff() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void setTraceOn(Properties arg0) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void setupSystemExternalBundleRepository() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void startBLA(String arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean stop() throws TopologyException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void stopBLA(String arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void syncNodes() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean sysoutContains(String arg0, String arg1) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean uninstallEar(String arg0) throws TopologyException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void uninstallServlet(String arg0) throws TopologyException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBundleVersions(String arg0, Object... arg1) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void updateServerPolicy() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public boolean webserverPluginContains(String arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void zOSAddNodeNameGlobal() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    /** {@inheritDoc} */
    @Override
    public void zOSRemoveNodeNameGlobal() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

}
