/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.equinox.module;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * A class to encapsulate the way we embed our own 'location' into loose bundles by
 * creating an in memory jar file holding a properties file, with the location embedded in it.
 * <br>
 * This allows us to associate a location of our choosing for bundles that we install, rather than
 * relying on the one that OSGi will use.
 */
public class ModuleLocationUtils {
    private static final String BUNDLE_PROPERTY_ENTRY_NAME = "LooseBundleProps.properties";
    private static final String BUNDLE_LOCATION = "bundleLocation";

    /**
     * Use this method to obtain the location stored within a loose bundle, created via the {@link getInputStreamForLooseModule} method.
     * <p>
     * Note calling this method with bundlefiles built over empty inputstreams, or over marker files
     * will break badly. Marker files are used for nested archives.
     * 
     * @param bundleFile
     * @return Location if found, or null if not.
     */
    @FFDCIgnore(IOException.class)
    public static String getLocationFromBundleFile(BundleFile bundleFile) {
        BundleEntry be = bundleFile.getEntry(BUNDLE_PROPERTY_ENTRY_NAME);
        if (be != null) {
            Properties p = new Properties();
            try {
                p.load(be.getInputStream());
                return p.getProperty(BUNDLE_LOCATION);
            } catch (IOException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Use this method to obtain an inputstream to use when installing a loose bundle.
     * <br>
     * The location passed, will be the location passed within the ModuleInfo
     * when invoking the ModuleContainerFinder later.
     * 
     * @param location
     * @return InputStream for OSGi to use when installing the new loose bundle
     * @throws IOException if an error occurs creating the inputstream..
     */
    public static InputStream getInputStreamForLooseModule(String location) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jos = new JarOutputStream(baos);
        ZipEntry looseBundlePropsEntry = new ZipEntry(BUNDLE_PROPERTY_ENTRY_NAME);
        jos.putNextEntry(looseBundlePropsEntry);
        Properties looseProps = new Properties();
        looseProps.setProperty(BUNDLE_LOCATION, location);
        looseProps.store(jos, "LooseBundle");
        jos.close();
        return new ByteArrayInputStream(baos.toByteArray());
    }
}
