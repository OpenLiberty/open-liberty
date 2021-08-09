/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal.schema;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

import com.ibm.ws.config.xml.internal.XMLConfigConstants;

/**
 * Fake bundle for TypeBuilder to look up resources
 * support for ibmui:localization in OCD
 */
class SchemaBundle implements Bundle {

    private Map<String, URL> entries;
    private final Attributes headers;
    private final String location;
    private final String productName;

    public void addEntry(String key, URL url) {
        if (entries == null) {
            entries = new HashMap<String, URL>();
        }
        entries.put(key, url);
    }

    public SchemaBundle(JarFile jar, Map<String, URL> entries, String productName) throws IOException {
        this.entries = entries;
        this.headers = jar.getManifest().getMainAttributes();
        this.location = jar.getName();
        this.productName = productName;
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public void start(int options) throws BundleException {}

    @Override
    public void start() throws BundleException {}

    @Override
    public void stop(int options) throws BundleException {}

    @Override
    public void stop() throws BundleException {}

    @Override
    public void update(InputStream input) throws BundleException {}

    @Override
    public void update() throws BundleException {}

    @Override
    public void uninstall() throws BundleException {}

    @Override
    public Dictionary<String, String> getHeaders() {
        return null;
    }

    @Override
    public long getBundleId() {
        return 0;
    }

    @Override
    public String getLocation() {
        // The location format being returned must match the format in Provisioner and BundleList.
        String taggedLocation = XMLConfigConstants.BUNDLE_LOC_REFERENCE_TAG + location;
        String productNameInfo = (productName != null && !productName.isEmpty()
                        && !productName.equals(XMLConfigConstants.CORE_PRODUCT_NAME)) ?
                        (XMLConfigConstants.BUNDLE_LOC_PROD_EXT_TAG + productName + ":") : "";

        return XMLConfigConstants.BUNDLE_LOC_FEATURE_TAG + productNameInfo + taggedLocation;
    }

    @Override
    public ServiceReference<?>[] getRegisteredServices() {
        return null;
    }

    @Override
    public ServiceReference<?>[] getServicesInUse() {
        return null;
    }

    @Override
    public boolean hasPermission(Object permission) {
        return false;
    }

    @Override
    public URL getResource(String name) {
        return null;
    }

    @Override
    public Dictionary<String, String> getHeaders(String locale) {
        return null;
    }

    @Override
    public String getSymbolicName() {
        String name = headers.getValue(Constants.BUNDLE_SYMBOLICNAME);
        int index;
        if (name != null && (index = name.indexOf(';')) != -1) {
            name = name.substring(0, index);
        }
        return name;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return null;
    }

    @Override
    public Enumeration<String> getEntryPaths(String path) {
        return null;
    }

    @Override
    public URL getEntry(String path) {
        if (entries == null)
            return null;
        return entries.get(path);
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
        return null;
    }

    @Override
    public BundleContext getBundleContext() {
        return null;
    }

    @Override
    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
        return null;
    }

    @Override
    public Version getVersion() {
        String v = headers.getValue(Constants.BUNDLE_VERSION);
        if (v == null) {
            return Version.emptyVersion;
        }
        return Version.parseVersion(v);
    }

    @Override
    public <A> A adapt(Class<A> type) {
        return null;
    }

    @Override
    public File getDataFile(String filename) {
        return null;
    }

    @Override
    public int compareTo(Bundle o) {
        return 0;
    }
}