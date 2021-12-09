/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

/**
 * Update all OpenAPI-UI bundles
 */
public class OpenAPIUIBundlesUpdater {

    private static final TraceComponent tc = Tr.register(OpenAPIUIBundlesUpdater.class);

    /**
     * List of OpenAPI-UI bundles
     */
    private static final Set<String> openAPIUIBundleNames = new HashSet<String>();

    private static boolean stopping = false;

    static {
        openAPIUIBundleNames.add("com.ibm.ws.microprofile.openapi.ui");
    }

    synchronized static void updateResources(Map<String, Object> resourcesToUpdate, boolean isRestoreDefaults) throws IOException, BundleException {
        if (resourcesToUpdate == null || resourcesToUpdate.isEmpty()) {
            return;
        }

        //Retrieve all OpenAPI-UI Bundles from the BundleContext
        final Set<Bundle> allOpenAPIUIBundles = getOpenAPIUIBundles();

        //this will block until all bundles have started
        boolean result = waitForBundlesToStart(allOpenAPIUIBundles);
        if (!result) {
            //wait failed with an exception, do nothing
            return;
        }

        // Update the OpenAPI-UI Bundles
        Iterator<Bundle> itr = allOpenAPIUIBundles.iterator();
        InputStream updatedBundleStream = null;
        while (itr.hasNext()) {
            Bundle openAPIUIBundle = itr.next();
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "About to process bundle : " + getBundleDescription(openAPIUIBundle));
            }

            if (isRestoreDefaults) {
                String customHeader = getResource(openAPIUIBundle, CustomCSSWABUpdater.PATH_CSS_CUSTOM_HEADER);
                String defaultHeader = getResource(openAPIUIBundle, CustomCSSWABUpdater.PATH_CSS_DEFAULT_HEADER);
                if (defaultHeader != null && defaultHeader.equals(customHeader)) {
                    if (OpenAPIUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "Not updating the bundle as it is already in default state : " + getBundleDescription(openAPIUIBundle));
                    }
                    continue;
                }
            }

            try {
                //Grab the bundle resources and make the changes
                updatedBundleStream = getUpdatedBundleStream(openAPIUIBundle, resourcesToUpdate);
                if (updatedBundleStream == null) {
                    continue;
                }
                openAPIUIBundle.update(updatedBundleStream);
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Updated bundle : " + getBundleDescription(openAPIUIBundle));
                }
            } finally {
                FileUtils.tryToClose(updatedBundleStream);
                if (openAPIUIBundle.getState() != Bundle.ACTIVE && openAPIUIBundle.getState() != Bundle.STARTING) {
                    if (OpenAPIUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "Bundle is not active: " + getBundleDescription(openAPIUIBundle));
                    }
                    openAPIUIBundle.start();
                }
            }
        }
    }

    private static boolean verifyRefs(ServiceReference<Bundle>[] refs, Set<String> expectedBundles) {
        if (refs == null) {
            return false;
        }
        int foundBundles = 0;
        for (ServiceReference<Bundle> ref : refs) {
            String bundleKey = (String) ref.getProperty("web.module.key");
            String bundleName = bundleKey.substring(0, bundleKey.indexOf('#'));
            if (expectedBundles.contains(bundleName))
                foundBundles++;
        }
        if (foundBundles == expectedBundles.size()) {
            return true;
        } else {
            return false;
        }
    }

    private static Set<Bundle> getOpenAPIUIBundles() {
        Set<Bundle> openAPIUIBundles = new HashSet<Bundle>();
        BundleContext bundleContext = FrameworkUtil.getBundle(OpenAPIUIBundlesUpdater.class).getBundleContext();
        if (bundleContext != null) {
            for (Bundle aBundle : bundleContext.getBundles()) {
                String bundleName = aBundle.getSymbolicName();
                if (openAPIUIBundleNames.contains(bundleName)) {
                    openAPIUIBundles.add(aBundle);
                    if (OpenAPIUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "Found a OpenAPI-UI bundle: " + bundleName);
                    }
                }
            }
        }

        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Found " + openAPIUIBundles.size() + " OpenAPI-UI bundles in bundleContext=" + bundleContext);
        }
        return openAPIUIBundles;
    }

    @Trivial
    private static String getBundleDescription(Bundle bundle) {
        return (bundle == null ? "NULL" : "Bundle Name=" + bundle.getSymbolicName() + " : ID=" + bundle.getBundleId() + " : State=" + bundle.getState());
    }

    private static InputStream getUpdatedBundleStream(Bundle bundle, Map<String, Object> resourcesToUpdate) throws IOException {

        if (bundle == null || resourcesToUpdate == null || resourcesToUpdate.isEmpty()) {
            return null;
        }

        Set<String> resourceKeys = resourcesToUpdate.keySet();
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bytesOut);

        int totalEntries = 0, nonDirEntries = 0;
        Enumeration<URL> enumer = bundle.findEntries("/", "*", true);
        while (enumer.hasMoreElements()) {
            URL aURL = enumer.nextElement();
            String path = aURL.getPath();
            if (path.startsWith("/") && !path.equals("/")) {
                path = path.substring(1);
            }

            if (!resourceKeys.contains(path)) {
                totalEntries++;
                ZipEntry anEntry = new ZipEntry(path);
                zipOut.putNextEntry(anEntry);
                if (!anEntry.isDirectory()) {
                    nonDirEntries++;
                    writeStreamToZip(aURL.openStream(), zipOut);
                }
                zipOut.closeEntry();
            } else {
                if (OpenAPIUtils.isDumpEnabled(tc)) {
                    Tr.dump(tc, "Not processing resource as it'll be overwritten later : " + path);
                }
            }
        }

        //Update resources
        Iterator<String> itr = resourceKeys.iterator();
        while (itr.hasNext()) {
            String path = itr.next();
            totalEntries++;
            ZipEntry entry = new ZipEntry(path);
            zipOut.putNextEntry(entry);
            if (!entry.isDirectory()) {
                nonDirEntries++;
                // Special case when restoring custom CSS, replace the default name with the actual CSS info
                if (path.equals(CustomCSSWABUpdater.PATH_CSS_CUSTOM_HEADER)) {
                    Object resourceValue = resourcesToUpdate.get(path);
                    if (resourceValue instanceof String &&
                        ((String) resourceValue).equals(CustomCSSWABUpdater.PATH_CSS_DEFAULT_HEADER)) {
                        String contents = getResource(bundle, CustomCSSWABUpdater.PATH_CSS_DEFAULT_HEADER);
                        resourcesToUpdate.put(path, contents);
                    }
                }
                processResource(path, resourcesToUpdate.get(path), zipOut);
            }
            zipOut.closeEntry();
        }

        if (OpenAPIUtils.isDebugEnabled(tc)) {
            Tr.debug(tc, "Zip total Entries = " + totalEntries + " : Non-dir entries " + nonDirEntries);
        }

        zipOut.close();
        bytesOut.close();
        return new ByteArrayInputStream(bytesOut.toByteArray());
    }

    private static void processResource(String resourcePath, Object resourceContents, ZipOutputStream zos) throws UnsupportedEncodingException, IOException {
        if (resourceContents != null) {
            if (resourceContents instanceof String) {
                zos.write(((String) resourceContents).getBytes(StandardCharsets.UTF_8));
                if (OpenAPIUtils.isDebugEnabled(tc)) {
                    Tr.debug(tc, "Processed (String) resource at " + resourcePath);
                }
                return;
            } else if (resourceContents instanceof File) {
                File aFile = (File) resourceContents;
                if (FileUtils.fileExists(aFile) && FileUtils.fileIsFile(aFile)) {
                    writeStreamToZip(FileUtils.getInputStream(aFile), zos);
                    if (OpenAPIUtils.isDebugEnabled(tc)) {
                        Tr.debug(tc, "Processed (File) resource at " + resourcePath);
                    }
                    return;
                } else {
                    if (OpenAPIUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "File is not valid : " + aFile.getAbsolutePath());
                    }
                }
            } else if (resourceContents instanceof URL) {
                writeStreamToZip(OpenAPIUtils.getUrlAsStream((URL) resourceContents, null), zos);
                if (OpenAPIUtils.isDebugEnabled(tc)) {
                    Tr.debug(tc, "Processed (URL) resource at " + resourcePath);
                }
                return;
            }
        }
        if (OpenAPIUtils.isDebugEnabled(tc)) {
            if (tc.isDumpEnabled()) {
                Tr.dump(tc, "Resource not processed  : resourcePath=" + resourcePath + " : resourceContents=" + resourceContents);
            } else {
                Tr.debug(tc, "Resource not processed  : resourcePath=" + resourcePath);
            }
        }
    }

    // Return as a string the contents of a file in the bundle.
    private static String getResource(Bundle myBundle, String resourcePath) {
        if (myBundle == null)
            return null;
        String bundleShortDescription = getBundleDescription(myBundle);
        StringBuilder responseString = new StringBuilder();
        URL bundleResource = myBundle.getResource(resourcePath);
        if (bundleResource != null) {
            BufferedReader br = null;
            try { // read the requested resource from the bundle
                br = new BufferedReader(new InputStreamReader(bundleResource.openConnection().getInputStream(), StandardCharsets.UTF_8));
                while (br.ready()) {
                    responseString.append(br.readLine());
                }
                br.close();
            } catch (Exception e) { // shouldn't happen
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Exception trying to read resource at " + resourcePath + " from bundle " + bundleShortDescription);
                }
            }

        } else {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Unexpected error getting resource from WAB bundle.");
            }
        }
        return responseString.toString();
    }

    @Trivial
    private static void writeStreamToZip(InputStream stream, ZipOutputStream zos) throws IOException {
        if (stream == null || zos == null) {
            return;
        }

        try {
            int length;
            byte[] buf = new byte[8192];
            while ((length = stream.read(buf)) != -1) {
                zos.write(buf, 0, length);
            }
        } finally {
            FileUtils.tryToClose(stream);
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean waitForBundlesToStart(Set<Bundle> openAPIUIBundles) {
        boolean waitComplete = false;
        Set<String> expectedBundleNames = new HashSet<String>();
        for (Bundle bundle : openAPIUIBundles) {
            expectedBundleNames.add(bundle.getSymbolicName());
        }

        try {
            BundleContext bundleContext = FrameworkUtil.getBundle(OpenAPIUIBundlesUpdater.class).getBundleContext();
            ServiceReference<Bundle>[] refs = (ServiceReference<Bundle>[]) bundleContext.getServiceReferences(Bundle.class.getName(), "(installed.wab.contextRoot=*)");
            waitComplete = verifyRefs(refs, expectedBundleNames);
            while (!waitComplete) {
                if (stopping) {
                    if (OpenAPIUtils.isDebugEnabled(tc)) {
                        Tr.debug(tc, "Stopped waiting for OpenAPI bundles because the server is shutting down");
                    }
                    return false;
                }
                refs = (ServiceReference<Bundle>[]) bundleContext.getServiceReferences(Bundle.class.getName(), "(installed.wab.contextRoot=*)");
                waitComplete = verifyRefs(refs, expectedBundleNames);
                if (!waitComplete) {
                    //sleep UI bundle update thread to prevent getting services refs all the time
                    Thread.sleep(500);
                }
            }
        } catch (Exception e) {
            waitComplete = false;
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed waiting for OpenAPI bundles before update failed with :", new Object[] { e.getMessage() });
            }
        }
        return waitComplete;
    }

    public static void serverStopping() {
        stopping = true;
    }
}
