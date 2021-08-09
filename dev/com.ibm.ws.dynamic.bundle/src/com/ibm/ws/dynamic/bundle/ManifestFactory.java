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
package com.ibm.ws.dynamic.bundle;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentConstants;

public class ManifestFactory {
    private static final String MANIFEST_ATTRIBUTE_SEPARATOR = ",";

    private String manifestVersion = "1.0";
    private String bundleName = null;
    private String bundleVersion = Version.emptyVersion.toString();
    private String bundleVendor = "IBM";
    private String bundleDescription = "A dynamic bundle";
    private String bundleManifestVersion = "2";
    private String bundleSymbolicName = null;
    private Iterable<String> importPackageList = null;
    private Iterable<String> requireBundleList = null;
    private Iterable<String> dynamicImportPackageList = null;
    private Iterable<String> serviceComponentList = null;
    private boolean lazyActivation;
    private final Map<String, Iterable<? extends Object>> otherAttributes = new HashMap<String, Iterable<? extends Object>>();

    public ManifestFactory setManifestVersion(String manifestVersion) {
        this.manifestVersion = manifestVersion;
        return this;
    }

    public ManifestFactory setBundleName(String bundleName) {
        this.bundleName = bundleName;
        return this;
    }

    public ManifestFactory setBundleVersion(Version bundleVersion) {
        this.bundleVersion = bundleVersion.toString();
        return this;
    }

    public ManifestFactory setBundleVendor(String vendor) {
        this.bundleVendor = vendor;
        return this;
    }

    public ManifestFactory setBundleDescription(String desc) {
        this.bundleDescription = desc;
        return this;
    }

    public ManifestFactory setBundleManifestVersion(String bundleManifestVersion) {
        this.bundleManifestVersion = bundleManifestVersion;
        return this;
    }

    public ManifestFactory setBundleSymbolicName(String bundleSymbolicName) {
        this.bundleSymbolicName = bundleSymbolicName;
        return this;
    }

    public ManifestFactory importPackages(String... packages) {
        return importPackages(Arrays.asList(packages));
    }

    public ManifestFactory requireBundles(String... bundles) {
        return requireBundles(Arrays.asList(bundles));
    }

    public ManifestFactory dynamicallyImportPackages(String... packages) {
        return dynamicallyImportPackages(Arrays.asList(packages));
    }

    public ManifestFactory declareServiceComponents(String... components) {
        return declareServiceComponents(Arrays.asList(components));
    }

    public ManifestFactory addAttributeValues(String name, Object... values) {
        return addManifestAttribute(name, Arrays.asList(values));
    }

    public ManifestFactory importPackages(Iterable<String> packages) {
        importPackageList = packages;
        return this;
    }

    public ManifestFactory requireBundles(Iterable<String> bundles) {
        requireBundleList = bundles;
        return this;
    }

    public ManifestFactory dynamicallyImportPackages(Iterable<String> packages) {
        dynamicImportPackageList = packages;
        return this;
    }

    public ManifestFactory declareServiceComponents(Iterable<String> components) {
        serviceComponentList = components;
        return this;
    }

    public ManifestFactory addManifestAttribute(String name, Iterable<? extends Object> values) {
        otherAttributes.put(name, values);
        return this;
    }

    public ManifestFactory setLazyActivation(boolean lazy) {
        lazyActivation = lazy;
        return this;
    }

    public final Manifest createManifest() {
        if (bundleName == null)
            throw new IllegalStateException("bundleName = null");
        if (bundleSymbolicName == null)
            throw new IllegalStateException("bundleSymbolicName = null");
        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, manifestVersion);
        attrs.putValue(Constants.BUNDLE_NAME, bundleName);
        attrs.putValue(Constants.BUNDLE_VERSION, bundleVersion);
        attrs.putValue(Constants.BUNDLE_VENDOR, bundleVendor);
        attrs.putValue(Constants.BUNDLE_DESCRIPTION, bundleDescription);
        attrs.putValue(Constants.BUNDLE_MANIFESTVERSION, bundleManifestVersion);
        attrs.putValue(Constants.BUNDLE_SYMBOLICNAME, bundleSymbolicName);
        putValueList(attrs, Constants.IMPORT_PACKAGE, importPackageList);
        putValueList(attrs, Constants.REQUIRE_BUNDLE, requireBundleList);
        putValueList(attrs, Constants.DYNAMICIMPORT_PACKAGE, dynamicImportPackageList);
        putValueList(attrs, ComponentConstants.SERVICE_COMPONENT, serviceComponentList);
        if (lazyActivation) {
            attrs.putValue(Constants.BUNDLE_ACTIVATIONPOLICY, Constants.ACTIVATION_LAZY);
        }
        for (Entry<String, Iterable<? extends Object>> entry : otherAttributes.entrySet())
            putValueList(attrs, entry.getKey(), entry.getValue());
        return manifest;
    }

    private static <T> void putValueList(Attributes attrs, String name, Iterable<T> list) {
        if (list == null)
            return;
        attrs.putValue(name, join(list, MANIFEST_ATTRIBUTE_SEPARATOR));
    }

    /**
     * Join several objects into a string
     */
    static <T> String join(Iterable<T> elems, String delim) {
        if (elems == null)
            return "";
        StringBuilder result = new StringBuilder();
        for (T elem : elems)
            result.append(elem).append(delim);
        if (result.length() > 0)
            result.setLength(result.length() - delim.length());
        return result.toString();
    }

}
