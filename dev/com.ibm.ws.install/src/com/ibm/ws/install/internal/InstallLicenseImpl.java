/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallLicense;
import com.ibm.ws.repository.common.enums.LicenseType;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryBadDataException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.AttachmentResource;

import wlp.lib.extract.LicenseProvider;
import wlp.lib.extract.SelfExtractUtils;

public class InstallLicenseImpl implements InstallLicense {

    private static final Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);
    private static final String PROGRAM_NAME = "Program Name:";

    private final String id;
    private final LicenseType type;
    private String name;
    private String programName;
    private String information = "";
    private String agreement = "";
    private final Collection<String> features = new ArrayList<String>();

    public InstallLicenseImpl(String id, LicenseType type, AttachmentResource licenseAgreement, AttachmentResource licenseInformation,
                              AttachmentResource enLicenseInformation) throws RepositoryBackendException, RepositoryResourceException {
        this.id = id;
        this.type = type;
        if (licenseAgreement != null)
            this.agreement = getLicense(licenseAgreement.getInputStream(), true);
        if (licenseInformation != null)
            this.information = getLicense(licenseInformation.getInputStream(), false);
        if (this.programName == null && enLicenseInformation != null) {
            getProgramName(enLicenseInformation.getInputStream());
        }
    }

    public InstallLicenseImpl(String id, LicenseType type, LicenseProvider licenseProvider) {
        this(id, type, licenseProvider, true);
    }

    public InstallLicenseImpl(String id, LicenseType type, LicenseProvider licenseProvider, boolean wordWrap) {
        this.id = id;
        this.type = type == null ? (id.equalsIgnoreCase("http://www.ibm.com/licenses/wlp-featureterms-v1") ? LicenseType.UNSPECIFIED : null) : type;
        if (licenseProvider != null) {
            this.agreement = getLicense(licenseProvider.getLicenseAgreement(), true, wordWrap);
            this.information = getLicense(licenseProvider.getLicenseInformation(), false, wordWrap);
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getType() {
        return this.type == null ? "" : this.type.toString();
    }

    @Override
    public String getName() {
        return this.name == null ? "" : this.name;
    }

    @Override
    public String getProgramName() {
        return this.programName == null ? "" : this.programName;
    }

    @Override
    public String getInformation() {
        return this.information;
    }

    @Override
    public String getAgreement() {
        return this.agreement;
    }

    @Override
    public Collection<String> getFeatures() {
        return features;
    }

    @Override
    public String toString() {
        return "(" + this.id + "," +
               (this.information.length() > 16 ? this.information.substring(0, 15) + "..." : this.information) + "," +
               (this.agreement.length() > 16 ? this.agreement.substring(0, 15) + "..." : this.agreement) + ")";
    }

    private String getLicense(InputStream in, boolean isAgreement) {
        return getLicense(in, isAgreement, true);
    }

    private String getLicense(InputStream in, boolean isAgreement, boolean wordWrap) {
        if (in == null)
            return "";
        StringBuffer sb = new StringBuffer();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in, "UTF-16"));
            int i = 0;
            for (String line; (line = reader.readLine()) != null;) {
                if (isAgreement) {
                    if (isAgreement && this.name == null) {
                        this.name = line;
                    }
                } else if (this.programName == null && i < 6 && line.startsWith(PROGRAM_NAME)) {
                    this.programName = line.substring(PROGRAM_NAME.length() + 1);
                }
                if (wordWrap) {
                    InstallUtils.wordWrap(sb, line, "");
                } else {
                    sb.append(line);
                    sb.append("\n");
                }
                i++;
            }
            sb.append("\n");
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.FINEST, "InstallLicenseImpl.getLicense failed", e);
            return "";
        } catch (IOException e) {
            logger.log(Level.FINEST, "InstallLicenseImpl.getLicense failed", e);
            return "";
        } finally {
            SelfExtractUtils.tryToClose(reader);
        }
    }

    private void getProgramName(InputStream in) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in, "UTF-16"));
            int i = 0;
            for (String line; (line = reader.readLine()) != null;) {
                if (i < 6 && line.startsWith(PROGRAM_NAME)) {
                    this.programName = line.substring(PROGRAM_NAME.length() + 1);
                    return;
                }
                i++;
            }
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.FINEST, "InstallLicenseImpl.getProgramName failed", e);
        } catch (IOException e) {
            logger.log(Level.FINEST, "InstallLicenseImpl.getProgramName failed", e);
        } finally {
            SelfExtractUtils.tryToClose(reader);
        }
    }

    public void addFeature(String featureName) {
        features.add(featureName);
    }

    public static String getLicense(AttachmentResource ar) throws RepositoryBadDataException, RepositoryBackendException {
        InputStream is = ar.getInputStream();
        StringBuffer sb = new StringBuffer();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is, "UTF-16"));
            for (String line; (line = reader.readLine()) != null;) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            return "";
        } finally {
            SelfExtractUtils.tryToClose(reader);
        }
        return sb.toString();
    }
}
