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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Formatter;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.eclipse.equinox.region.Region;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class BundleFactory extends ManifestFactory {
    private static final TraceComponent tc = Tr.register(BundleFactory.class);
    private static final String EXTRAS_SHA_HEADER = "IBM-Extras-SHA";
    private static final String SHA_ALGORITHM = "SHA-256";
    private BundleContext bundleContext;
    private String bundleLocationPrefix = "VirtualBundle@";
    private String bundleLocation = null;
    private Region region = null;
    private String defaultInstance = null;
    private String metatypeXML = null;
    private final List<ServiceComponentDeclaration> components = new ArrayList<ServiceComponentDeclaration>();

    // Leaving this commented out main method in the code
    // to serve as an example of how to use this factory
    // with a service component declaration.
    //    public static void main(String[] args) throws IOException {
    //        final ServiceComponentDeclaration scd = new ServiceComponentDeclaration()
    //                        .name("SharedLibrary." + "JUNIT_LIB")
    //                        .impl(SharedLibraryImpl.class)
    //                        .pid("999")
    //                        .provide(SharedLibrary.class)
    //                        .require(new ServiceReferenceDeclaration()
    //                                        .name("fileset1")
    //                                        .provides(Fileset.class)
    //                                        .target("(id=JUNIT_FILES)")
    //                                        .bind("addFileset")
    //                                        .unbind("removeFileset"))
    //                        .require(new ServiceReferenceDeclaration()
    //                                        .name("fileset2")
    //                                        .provides(Fileset.class)
    //                                        .target("(id=ANT_JUNIT_FILES)")
    //                                        .bind("addFileset")
    //                                        .unbind("removeFileset"));
    //        System.out.println(scd);
    //        final InputStream in = new BundleFactory()
    //                        .setBundleName("WAS Shared Library Bundle for JUNIT_LIB")
    //                        .setBundleSymbolicName("com.ibm.wsspi.library.JUNIT_LIB")
    //                        .importPackages("com.ibm.ws.classloading", "com.ibm.wsspi.library", "com.ibm.ws.library.internal;shared-library=true")
    //                        .setBundleLocationPrefix("SharedLibrary@")
    //                        .setBundleLocation("JUNIT_LIB")
    //                        .declare(scd)
    //                        .getBundleInputStream();
    //        File jar = File.createTempFile("test", ".jar");
    //        OutputStream out = new FileOutputStream(jar);
    //        byte[] buffer = new byte[1024];
    //        int bytesRead;
    //        while (0 <= (bytesRead = in.read(buffer))) {
    //            out.write(buffer, 0, bytesRead);
    //        }
    //        out.close();
    //        System.out.println("Jar created: " + jar.getAbsolutePath());
    //    }

    public BundleFactory setBundleContext(BundleContext ctx) {
        bundleContext = ctx;
        return this;
    }

    public BundleFactory setBundleLocationPrefix(String prefix) {
        bundleLocationPrefix = prefix;
        return this;
    }

    public BundleFactory setBundleLocation(String loc) {
        bundleLocation = loc;
        return this;
    }

    public BundleFactory setRegion(Region region) {
        this.region = region;
        return this;
    }

    public BundleFactory setDefaultInstance(String defaultInstance) {
        this.defaultInstance = defaultInstance;
        return this;
    }

    public BundleFactory setMetatypeXML(String metatypeXML) {
        this.metatypeXML = metatypeXML;
        return this;
    }

    public BundleFactory declare(ServiceComponentDeclaration component) {
        components.add(component);
        return this;
    }

    public Bundle createBundle() {
        final String methodName = "createBundle(): ";
        if (bundleContext == null)
            throw new IllegalStateException("bundleContext == null");
        if (bundleLocation == null)
            throw new IllegalStateException("bundleLocation == null");
        try {
            String location = bundleLocationPrefix + bundleLocation;

            declareServiceComponents();
            computeExtrasSha();
            Manifest m = createManifest();

            Bundle bundle = bundleContext.getBundle(location);
            if (bundle != null) {
                if (sameManifests(m, bundle.getHeaders(""))) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName + "Using previously installed bundle", bundle.getLocation(), bundle.getBundleId());
                    return bundle;
                }
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + "Updating previously installed bundle", bundle.getLocation(), bundle.getBundleId());
            }

            InputStream in = getBundleInputStream(m);
            if (bundle != null) {
                try {
                    bundle.stop();
                } catch (BundleException be) {
                    // we really don't care much of we fail to stop the bundle
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Exception while stopping bundle", be);
                }
                // Need to be sure to place the bundle in the correct region before updating.
                // A region may change if the API configuration has changed for the gateway bundle
                // which would place the bundle in a different region to access API.
                if (region != null) {
                    Region oldRegion = region.getRegionDigraph().getRegion(bundle);
                    if (!region.equals(oldRegion)) {
                        oldRegion.removeBundle(bundle);
                        region.addBundle(bundle);
                    }
                }
                bundle.update(in);
                return bundle;
            } else {
                if (region != null) {
                    return region.installBundleAtLocation(location, in);
                } else {
                    return bundleContext.installBundle(location, in);
                }
            }
        } catch (BundleException e) {
            throw new DynamicBundleException("Failed to create bundle", e);
        }

    }

    private void computeExtrasSha() {
        if (components.isEmpty() && defaultInstance == null && metatypeXML == null) {
            return;
        }
        try {
            MessageDigest shaDigest = MessageDigest.getInstance(SHA_ALGORITHM);
            for (ServiceComponentDeclaration component : components) {
                shaDigest.update(component.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (defaultInstance != null) {
                shaDigest.update(defaultInstance.getBytes(StandardCharsets.UTF_8));
            }
            if (metatypeXML != null) {
                shaDigest.update(metatypeXML.getBytes(StandardCharsets.UTF_8));
            }
            addAttributeValues(EXTRAS_SHA_HEADER, getHexSHA(shaDigest));
        } catch (NoSuchAlgorithmException e) {
            // auto FFDC
        }
    }

    static String getHexSHA(MessageDigest digest) {
        Formatter hexFormat = new Formatter();
        for (byte b : digest.digest()) {
            hexFormat.format("%02x", b); //$NON-NLS-1$
        }
        return hexFormat.toString();
    }

    /**
     * @param m the jar new manifest
     * @param headers previous bundle headers
     * @return
     */
    private boolean sameManifests(Manifest m, Dictionary<String, String> headers) {
        Attributes mainAttrs = m.getMainAttributes();
        if (mainAttrs.size() != headers.size()) {
            return false;
        }
        for (Entry<Object, Object> entry : mainAttrs.entrySet()) {
            if (!entry.getValue().equals(headers.get(entry.getKey().toString()))) {
                return false;
            }
        }
        return true;
    }

    private InputStream getBundleInputStream(Manifest manifest) {
        try {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            JarOutputStream jarOut = new JarOutputStream(bytesOut, manifest);

            // NOTE: if any more entries are added here them make a corresponding update to computeExtrasSha

            // write each component
            for (ServiceComponentDeclaration component : components) {
                jarOut.putNextEntry(new JarEntry(component.getFileName()));
                jarOut.write(component.toString().getBytes(StandardCharsets.UTF_8));
                jarOut.closeEntry();
            }

            if (defaultInstance != null) {
                jarOut.putNextEntry(new JarEntry("OSGI-INF/wlp/defaultInstances.xml"));
                jarOut.write(defaultInstance.getBytes(StandardCharsets.UTF_8));
                jarOut.closeEntry();
            }

            if (metatypeXML != null) {
                jarOut.putNextEntry(new JarEntry("OSGI-INF/metatype/metatype.xml"));
                jarOut.write(metatypeXML.getBytes(StandardCharsets.UTF_8));
                jarOut.closeEntry();
            }

            jarOut.flush();
            jarOut.close();
            bytesOut.close();
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesOut.toByteArray());
            return bytesIn;
        } catch (IOException e) {
            throw new DynamicBundleException("Could not create in-memory jar file for dynamic bundle", e);
        }
    }

    private void declareServiceComponents() {
        ArrayList<String> componentList = new ArrayList<String>();
        for (ServiceComponentDeclaration component : components)
            componentList.add(component.getFileName());
        declareServiceComponents(componentList);
    }

    @Override
    public BundleFactory setManifestVersion(String manifestVersion) {
        return (BundleFactory) super.setManifestVersion(manifestVersion);
    }

    @Override
    public BundleFactory setBundleName(String bundleName) {
        return (BundleFactory) super.setBundleName(bundleName);
    }

    @Override
    public BundleFactory setBundleVersion(Version bundleVersion) {
        return (BundleFactory) super.setBundleVersion(bundleVersion);
    }

    @Override
    public BundleFactory setBundleVendor(String vendor) {
        return (BundleFactory) super.setBundleVendor(vendor);
    }

    @Override
    public BundleFactory setBundleDescription(String desc) {
        return (BundleFactory) super.setBundleDescription(desc);
    }

    @Override
    public BundleFactory setBundleManifestVersion(String bundleManifestVersion) {
        return (BundleFactory) super.setBundleManifestVersion(bundleManifestVersion);
    }

    @Override
    public BundleFactory setBundleSymbolicName(String bundleSymbolicName) {
        return (BundleFactory) super.setBundleSymbolicName(bundleSymbolicName);
    }

    @Override
    public BundleFactory setLazyActivation(boolean lazy) {
        return (BundleFactory) super.setLazyActivation(lazy);
    }

    @Override
    public BundleFactory importPackages(String... packages) {
        return (BundleFactory) super.importPackages(packages);
    }

    @Override
    public BundleFactory requireBundles(String... bundles) {
        return (BundleFactory) super.requireBundles(bundles);
    }

    @Override
    public BundleFactory dynamicallyImportPackages(String... packages) {
        return (BundleFactory) super.dynamicallyImportPackages(packages);
    }

    @Override
    public BundleFactory declareServiceComponents(String... components) {
        return (BundleFactory) super.declareServiceComponents(components);
    }

    @Override
    public BundleFactory addAttributeValues(String name, Object... values) {
        return (BundleFactory) super.addAttributeValues(name, values);
    }

    @Override
    public BundleFactory importPackages(Iterable<String> packages) {
        return (BundleFactory) super.importPackages(packages);
    }

    @Override
    public BundleFactory requireBundles(Iterable<String> bundles) {
        return (BundleFactory) super.requireBundles(bundles);
    }

    @Override
    public BundleFactory dynamicallyImportPackages(Iterable<String> packages) {
        return (BundleFactory) super.dynamicallyImportPackages(packages);
    }

    @Override
    public BundleFactory declareServiceComponents(Iterable<String> components) {
        return (BundleFactory) super.declareServiceComponents(components);
    }

    @Override
    public BundleFactory addManifestAttribute(String name, Iterable<? extends Object> values) {
        return (BundleFactory) super.addManifestAttribute(name, values);
    }
}