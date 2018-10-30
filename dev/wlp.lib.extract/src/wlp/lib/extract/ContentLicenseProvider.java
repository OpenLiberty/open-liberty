/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import wlp.lib.extract.Content.Entry;

/**
 * This implementation of {@link LicenseProvider} will load the license agreement and information from a Content.
 */
public class ContentLicenseProvider implements LicenseProvider {

    private final Entry laEntry;
    private final Entry liEntry;
    private final String pName;
    private final String lName;
    private static LicenseProvider instance;
    private static final String PROGRAM_NAME = "Program Name:";
    private static final String PROGRAM_NAME_PROGRAM_NUMBER = "Program Name (Program Number):";

    /**
     * @param zipFile    The zip file containing the license
     * @param laZipEntry The entry for the zip license agreement in the zip file
     * @param liZipEntry The entry for the zip license information in the zip file
     * @param pName      The name of the program
     * @param lName      The name of the license
     */
    private ContentLicenseProvider(Entry laEntry, Entry liEntry, String pName, String lName) {
        super();
        this.laEntry = laEntry;
        this.liEntry = liEntry;
        this.pName = pName;
        this.lName = lName;
    }

    public static ReturnCode buildInstance(Content container, String laPrefix, String liPrefix) {
        // Get the zip, LI and LA files -- we need to lift values from them
        Entry laEntry = null;
        Entry liEntry = null;
        Entry liEnglishEntry = null;
        try {
            laEntry = SelfExtractUtils.getLicenseFile(container, laPrefix);
            liEntry = SelfExtractUtils.getLicenseFile(container, liPrefix);
            liEnglishEntry = container.getEntry(liPrefix + (liPrefix.endsWith("_") ? "" : "_") + "en");
        } catch (Exception e) {
            return new ReturnCode(ReturnCode.NOT_FOUND, "licenseNotFound", new Object[] {});
        }

        if (container == null || laEntry == null || liEntry == null || liEnglishEntry == null) {
            return new ReturnCode(ReturnCode.NOT_FOUND, "licenseNotFound", new Object[] {});
        }

        // Now lift the product and license names from the license files..
        String lName = getLicenseName(container, laEntry);
        String pName = getProgramName(liEnglishEntry);
        if (pName == null || lName == null) {
            return new ReturnCode(ReturnCode.UNREADABLE, "licenseNotFound", new Object[] {});
        }
        instance = new ContentLicenseProvider(laEntry, liEntry, pName, lName);
        return ReturnCode.OK;
    }

    // If use this method to create LicenseProvider instance, please be aware that
    // getLicenseInformation() and getLicenseName() will return null
    public static LicenseProvider createInstance(Content container, String laPrefix) {
        if (container == null) {
            return null;
        }
        Entry laEntry = null;
        try {
            laEntry = SelfExtractUtils.getLicenseFile(container, laPrefix);
        } catch (Exception e) {
            return null;
        }
        String lName = getLicenseName(container, laEntry);
        if (lName == null) {
            return null;
        }
        return new ContentLicenseProvider(laEntry, null, null, lName);
    }

    private static String getLicenseName(Content container, Entry laEntry) {
        BufferedReader r = null;
        try {
            // The license name is the sixth line in the LA file
            r = new BufferedReader(new InputStreamReader(laEntry.getInputStream(), "UTF-16"));
            //read the first line
            String line = r.readLine();
            String sixTh_line = "";
            //if jar is base,core or nd, the name is in the sixth line
            if (container.getName().contains("base") || container.getName().contains("core") || container.getName().contains("nd")) {
                //after the loop, r will be the sixth line
                for (int i = 0; i < 5; i++) {
                    sixTh_line = r.readLine();
                }
                if (sixTh_line != null) {
                    //get rid of the first three characters, which is "2. "
                    int startIndex = sixTh_line.indexOf("IBM");
                    if (startIndex > 0) {
                        return sixTh_line.substring(startIndex, sixTh_line.length()).trim();
                    }
                    return sixTh_line;
                }
                return line;
            } else {
                return line;
            }
        } catch (IOException e) {
        } finally {
            SelfExtractUtils.tryToClose(r);
        }
        return null;
    }

    private static String getProgramName(Entry liEnglishEntry) {
        BufferedReader r = null;
        String line = null;
        try {
            // Look for the product name in the LI file -- within the first few lines
            int i = 0;
            r = new BufferedReader(new InputStreamReader(liEnglishEntry.getInputStream(), "UTF-16"));
            do {
                line = r.readLine();
                if (line != null) {

                    if (line.startsWith(PROGRAM_NAME)) {
                        // First two words are translated, IBM is not
                        // Program Name: IBM WebSphere Application Server Network Deployment Version 8.5
                        line = line.substring(PROGRAM_NAME.length() + 1);
                        int versionIndex = line.indexOf(" V");
                        if (versionIndex > 0) {
                            return line.substring(0, versionIndex).trim();
                        } else {
                            return line;
                        }
                    } else if (line.startsWith(PROGRAM_NAME_PROGRAM_NUMBER)) {
                        // Program Name (Program Number):
                        // IBM WebSphere Application Server Network Deployment V9.0.0.3 (Evaluation)
                        String nextLine = r.readLine();
                        if (nextLine != null) {
                            int versionIndex = nextLine.indexOf(" V");
                            if (versionIndex > 0) {
                                return nextLine.substring(0, versionIndex).trim();
                            } else {
                                return nextLine;
                            }
                        }
                    }
                }
            } while (line != null && i++ < 30);
        } catch (IOException e) {
        } finally {
            SelfExtractUtils.tryToClose(r);
        }
        return null;
    }

    /**
     * Returns the instance build by invocations of {@link #buildInstance()};
     *
     * @return
     */
    public static LicenseProvider getInstance() {
        return instance;
    }

    /*
     * (non-Javadoc)
     *
     * @see wlp.lib.extract.LicenseProvider#getLicenseAgreement()
     */
    @Override
    public InputStream getLicenseAgreement() {
        try {
            return laEntry.getInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see wlp.lib.extract.LicenseProvider#getLicenseInformation()
     */
    @Override
    public InputStream getLicenseInformation() {
        try {
            if (liEntry != null)
                return liEntry.getInputStream();
        } catch (IOException e) {
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see wlp.lib.extract.LicenseProvider#getProgramName()
     */
    @Override
    public String getProgramName() {
        return pName;
    }

    /*
     * (non-Javadoc)
     *
     * @see wlp.lib.extract.LicenseProvider#getLicenseName()
     */
    @Override
    public String getLicenseName() {
        return lName;
    }

}
