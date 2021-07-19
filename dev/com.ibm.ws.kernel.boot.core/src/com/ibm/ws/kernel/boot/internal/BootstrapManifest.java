/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 * Contain the informations in bootstrap jar's manifest
 */
public class BootstrapManifest {

    static final String BUNDLE_VERSION = "Bundle-Version";
    static final String JAR_PROTOCOL = "jar";

    /** prefix for system-package files */
    static final String SYSTEM_PKG_PREFIX = "OSGI-OPT/websphere/system-packages_";

    /** suffix for system-package files */
    static final String SYSTEM_PKG_SUFFIX = ".properties";

    /**
     * Manifest header designating packages that should be exported into the
     * framework by this jar
     */
    static final String MANIFEST_EXPORT_PACKAGE = "Export-Package";

    private static BootstrapManifest instance = null;

    private final Attributes manifestAttributes;

    public static BootstrapManifest readBootstrapManifest(boolean libertyBoot) throws IOException {
        BootstrapManifest manifest = instance;
        if (manifest == null) {
            manifest = instance = new BootstrapManifest(libertyBoot);
        }
        return manifest;
    }

    /** Clean up: allow garbage collection to clean up resources we don't need post-bootstrap */
    public static void dispose() {
        instance = null;
    }

    protected BootstrapManifest() throws IOException {
        this(false);
    }

    /**
     * In the case of liberty boot the manifest is discovered
     * by looking up the jar URL for this class.
     *
     * @param libertyBoot enables liberty boot
     * @throws IOException if here is an error reading the manifest
     */
    protected BootstrapManifest(boolean libertyBoot) throws IOException {
        manifestAttributes = libertyBoot ? getLibertyBootAttributes() : getAttributesFromBootstrapJar();
    }

    private static Attributes getAttributesFromBootstrapJar() throws IOException {
        JarFile jf = null;
        try {
            jf = new JarFile(KernelUtils.getBootstrapJar());
            Manifest mf = jf.getManifest();
            return mf.getMainAttributes();
        } catch (IOException e) {
            throw e;
        } finally {
            Utils.tryToClose(jf);
        }
    }

    private static Attributes getLibertyBootAttributes() {
        JarFile jf = getLibertBootJarFile();
        Manifest mf;
        try {
            mf = jf.getManifest();
            return mf.getMainAttributes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            Utils.tryToClose(jf);
        }
    }

    private static JarFile getLibertBootJarFile() {
        // here we assume we can lookup our own .class resource to find the JarFile
        return getJarFile(BootstrapManifest.class.getResource(BootstrapManifest.class.getSimpleName() + ".class"));
    }

    private static JarFile getJarFile(URL url) {
        if (JAR_PROTOCOL.equals(url.getProtocol())) {
            try {
                URLConnection conn = url.openConnection();
                if (conn instanceof JarURLConnection) {
                    return ((JarURLConnection) conn).getJarFile();
                }
            } catch (IOException e) {
                throw new IllegalStateException("No jar file found: " + url, e);
            }
        }
        throw new IllegalArgumentException("Not a jar URL: " + url);
    }

    /**
     * @return the bundleVersion
     */
    public String getBundleVersion() {
        return manifestAttributes.getValue(BUNDLE_VERSION);
    }

    /**
     * @param bootProps
     * @throws IOException
     */
    public void prepSystemPackages(BootstrapConfig bootProps) {
        // Look for _extra_ system packages
        String packages = bootProps.get(BootstrapConstants.INITPROP_OSGI_EXTRA_PACKAGE);

        // Look for exported packages in manifest: append to bootstrap packages
        String mPackages = manifestAttributes.getValue(MANIFEST_EXPORT_PACKAGE);
        if (mPackages != null) {
            packages = (packages == null) ? mPackages : packages + "," + mPackages;

            // save new "extra" packages
            if (packages != null)
                bootProps.put(BootstrapConstants.INITPROP_OSGI_EXTRA_PACKAGE, packages);
        }

    }
}
