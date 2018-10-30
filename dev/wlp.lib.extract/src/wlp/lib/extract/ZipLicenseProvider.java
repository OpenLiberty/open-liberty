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

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * This implementation of {@link LicenseProvider} will load the license agreement and information from a ZIP.
 */
public class ZipLicenseProvider implements LicenseProvider {

    private static LicenseProvider instance;

    public static ReturnCode buildInstance(ZipFile zipFile, String laPrefix, String liPrefix) {
        JarFile jar;
        try {
            jar = getJarFile(zipFile);
        } catch (IOException e) {
            return new ReturnCode(ReturnCode.BAD_INPUT);
        }
        ReturnCode status = ContentLicenseProvider.buildInstance(new Content.JarContent(jar), laPrefix, liPrefix);
        instance = ContentLicenseProvider.getInstance();
        return status;
    }

    /**
     * Returns the instance build by invocations of {@link #buildInstance()};
     *
     * @return
     */
    public static LicenseProvider getInstance() {
        return instance;
    }

    private static JarFile getJarFile(ZipFile zipFile) throws IOException {
        if (zipFile instanceof JarFile) {
            return (JarFile) zipFile;
        }
        if (zipFile == null) {
            return null;
        }
        return new JarFile(zipFile.getName());
    }

    // If use this method to create LicenseProvider instance, please be aware that
    // getLicenseInformation() and getLicenseName() will return null
    public static LicenseProvider createInstance(ZipFile zipFile, String laPrefix) {
        JarFile jar = null;
        try {
            jar = getJarFile(zipFile);
        } catch (IOException e) {
        }
        if (jar == null) {
            return null;
        }
        return ContentLicenseProvider.createInstance(new Content.JarContent(jar), laPrefix);
    }

    /*
     * (non-Javadoc)
     *
     * @see wlp.lib.extract.LicenseProvider#getLicenseAgreement()
     */
    @Override
    public InputStream getLicenseAgreement() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see wlp.lib.extract.LicenseProvider#getLicenseInformation()
     */
    @Override
    public InputStream getLicenseInformation() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see wlp.lib.extract.LicenseProvider#getProgramName()
     */
    @Override
    public String getProgramName() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see wlp.lib.extract.LicenseProvider#getLicenseName()
     */
    @Override
    public String getLicenseName() {
        return null;
    }

}
