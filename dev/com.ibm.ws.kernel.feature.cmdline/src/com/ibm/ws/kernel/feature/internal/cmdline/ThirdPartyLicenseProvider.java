/*******************************************************************************
 * Copyright (c) 2013, 2025 IBM Corporation and others.
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
package com.ibm.ws.kernel.feature.internal.cmdline;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import wlp.lib.extract.LicenseProvider;

/**
 * This implementation of {@link LicenseProvider} will obtain all of the information about the license from a Subsystem-License header value.
 */
public class ThirdPartyLicenseProvider implements LicenseProvider {

    private final String programName;
    private final String subsystemLicenseHeader;

    public ThirdPartyLicenseProvider(String programName, String subsystemLicenseHeader) {
        this.programName = programName;
        this.subsystemLicenseHeader = subsystemLicenseHeader;
    }

    /*
     * (non-Javadoc)
     * 
     * @see wlp.lib.extract.LicenseProvider#getProgramName()
     */
    @Override
    public String getProgramName() {
        return this.programName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see wlp.lib.extract.LicenseProvider#getLicenseName()
     */
    @Override
    public String getLicenseName() {
        return this.subsystemLicenseHeader;
    }

    /*
     * (non-Javadoc)
     * 
     * @see wlp.lib.extract.LicenseProvider#getLicenseAgreement()
     */
    @Override
    public InputStream getLicenseAgreement() {
        return getInputStream();
    }

    /*
     * (non-Javadoc)
     * 
     * @see wlp.lib.extract.LicenseProvider#getLicenseInformation()
     */
    @Override
    public InputStream getLicenseInformation() {
        return getInputStream();
    }

    /**
     * @return
     */
    private InputStream getInputStream() {
        return new ByteArrayInputStream(this.subsystemLicenseHeader.getBytes(StandardCharsets.UTF_16));
    }
}
