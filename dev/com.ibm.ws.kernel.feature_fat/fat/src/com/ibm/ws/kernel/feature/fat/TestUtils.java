/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.kernel.feature.fat;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.junit.Assert;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class TestUtils {

    /**
     * This method loads the feature.cache file as properties.
     * 
     * @param server TODO
     * @param cacheFile - The cache file to read.
     * 
     * @return - A properties object containing the properties from the feature.cache file.
     * @throws Exception
     */
    public static Properties getCacheProperties(LibertyServer server, String cacheFile) throws Exception {
        Properties cacheProps = new Properties();
        InputStream cacheStream = null;
        try {
            cacheStream = server.getFileFromLibertyServerRoot(cacheFile).openForReading();
            cacheProps.load(new BufferedReader(new InputStreamReader(cacheStream, "UTF-8")));

            // Race with the server replacing this file
            if (cacheProps.isEmpty()) {
                Thread.currentThread().sleep(1000);
                cacheStream = server.getFileFromLibertyServerRoot(cacheFile).openForReading();
                cacheProps.load(new BufferedReader(new InputStreamReader(cacheStream, "UTF-8")));
            }

            Assert.assertFalse("feature.bundle.cache should not be empty unless there was a race reading it", cacheProps.isEmpty());
        } finally {
            tryToClose(cacheStream);
        }

        return cacheProps;
    }

    /**
     * This method finds the installed features from the cache file.
     * Note this is sleazy and evil and not something that is in general good practice,
     * as the cache file contents should be able to change at any time.
     * 
     * @param cacheFile - The cache file to read.
     * @return The installed features list as a string
     * @throws Exception
     */
    public static String getInstalledFeatures(LibertyServer server, String cacheFile) throws Exception {
        InputStream cacheStream = null;
        BufferedReader reader = null;
        String installedFeatures = null;
        String line = null;
        try {
            cacheStream = server.getFileFromLibertyServerRoot(cacheFile).openForReading();
            reader = new BufferedReader(new InputStreamReader(cacheStream, "UTF-8"));
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("@=")) {
                    installedFeatures = line.substring(2);
                    break;
                }
            }
        } finally {
            tryToClose(reader);
            tryToClose(cacheStream);
        }

        return installedFeatures;
    }

    public static void tryToClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ioe) {
            }
        }
    }

    public static void makeConfigUpdateSetMark(LibertyServer server, String newServerXml) throws Exception {
        // set a new mark
        server.setMarkToEndOfLog(server.getDefaultLogFile());

        // make the configuration update
        server.setServerConfigurationFile(newServerXml);

        // wait for configuration and feature update to complete
        // these two messages can happen in any order!
        server.waitForStringInLogUsingMark("CWWKG0017I");
        server.waitForStringInLogUsingMark("CWWKF0008I");
    }

}
