/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.monitor.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentException;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.monitor.internal.bci.ProbeMethodAdapter;
import com.ibm.ws.monitor.internal.bci.remap.AddVersionFieldClassAdapter;
import com.ibm.ws.monitor.internal.boot.templates.ClassAvailableProxy;
import com.ibm.ws.monitor.internal.boot.templates.ProbeProxy;

/**
 * Component that is responsible for generating and installing the
 * bootstrap class loader proxy for the probes infrastructure.
 * <p>
 * The component depends on the {@link Instrumentation} service.
 */
public class MonitoringProxyActivator {

    /**
     * The target package for the boot loader delegated classes.
     */
    public final static String BOOT_DELEGATED_PACKAGE = "com.ibm.ws.boot.delegated.monitoring";

    /**
     * The name of the {@link ProbeProxy} class that needs to be made
     * available on the bootstrap class loader.
     */
    public final static String PROBE_PROXY_CLASS_NAME = BOOT_DELEGATED_PACKAGE + "." + ProbeProxy.class.getSimpleName();

    /**
     * The internal name of the {@link ProbeProxy} class.
     */
    public final static String PROBE_PROXY_CLASS_INTERNAL_NAME = PROBE_PROXY_CLASS_NAME.replaceAll("\\.", "/");

    /**
     * The name of the {@link ClassAvailableProxy} class that needs to be
     * made available on the bootstrap class loader.
     */
    public final static String CLASS_AVAILABLE_PROXY_CLASS_NAME = BOOT_DELEGATED_PACKAGE + "." + ClassAvailableProxy.class.getSimpleName();

    /**
     * The internal name of the {@link ClassAvailableProxy} class.
     */
    public final static String CLASS_AVAILABLE_PROXY_CLASS_INTERNAL_NAME = CLASS_AVAILABLE_PROXY_CLASS_NAME.replaceAll("\\.", "/");

    /**
     * The bundle entry path prefix to the template classes.
     */
    final static String TEMPLATE_CLASSES_PATH = ProbeProxy.class.getPackage().getName().replaceAll("\\.", "/");

    /**
     * The name of the field in generated classes that will contain the
     * generating bundle version.
     */
    final static String VERSION_FIELD_NAME = "BUNDLE_VERSION";

    /**
     * The proxy jar manifest header that indicates the version of the bundle
     * that created the jar.
     */
    final static String MONITORING_VERSION_MANIFEST_HEADER = "Liberty-Monitoring-Bundle-Version";

    /**
     * The bundle context of the monitoring bundle.
     */
    final BundleContext bundleContext;

    /**
     * The instrumentation agent hook that we are dependent on.
     */
    final Instrumentation instrumentation;

    /**
     * Monitoring probe manager that is responsible for actually firing probes.
     */
    final ProbeManagerImpl probeManagerImpl;

    /**
     * Construct a new proxy activator.
     *
     * @param bundleContext    the {@link BundleContext} of the owning bundle
     * @param probeManagerImpl the owning {@link ProbeManagerImpl}
     * @param instrumentation  the java {@link Instrumentation} service reference
     */
    MonitoringProxyActivator(BundleContext bundleContext, ProbeManagerImpl probeManagerImpl, Instrumentation instrumentation) {
        this.bundleContext = bundleContext;
        this.probeManagerImpl = probeManagerImpl;
        this.instrumentation = instrumentation;
    }

    /**
     * Activate this declarative services component. Bundles that are
     * currently active will be examined for monitoring metadata and
     * registered as appropriate.
     *
     * @throws Exception if an error occurs during proxy setup
     */
    protected void activate() throws Exception {
        // If the ProbeProxy class is available, check it's version
        String runtimeVersion = getRuntimeClassVersion();
        if (runtimeVersion != null && !runtimeVersion.equals(getCurrentVersion())) {
            // TODO: Use a compatibility check instead
            throw new IllegalStateException("Incompatible proxy code (version " + runtimeVersion + ")");
        }

        // Find or create the proxy jar if the runtime code isn't loaded
        if (runtimeVersion == null) {
            JarFile proxyJar = getBootProxyJarIfCurrent();
            if (proxyJar == null) {
                proxyJar = createBootProxyJar();
            }
            instrumentation.appendToBootstrapClassLoaderSearch(proxyJar);
        }

        // Hook up the proxies
        activateProbeProxyTarget();
        activateClassAvailableProxyTarget();
    }

    /**
     * Deactivate this declarative services component.
     *
     * @throws ComponentException
     */
    protected void deactivate() throws ComponentException {
        try {
            deactivateClassAvailableProxyTarget();
            deactivateProbeProxyTarget();
        } catch (Exception e) {
            throw new ComponentException(e);
        }
    }

    /**
     * Determine if the boot delegated proxy is already available and, if so,
     * what its version is.
     *
     * @return the runtime version of the emitter proxy or null if the class
     *         is not currently available
     */
    @FFDCIgnore(Exception.class)
    String getRuntimeClassVersion() {
        String runtimeVersion = null;
        try {
            Class<?> clazz = Class.forName(PROBE_PROXY_CLASS_NAME);
            Field version = ReflectionHelper.getDeclaredField(clazz, VERSION_FIELD_NAME);
            runtimeVersion = (String) version.get(null);
        } catch (Exception e) {
        }
        return runtimeVersion;
    }

    /**
     * Get the boot proxy jar from the current data area if the code
     * matches the current bundle version.
     *
     * @return the proxy jar iff the proxy jar exits and matches this
     *         bundle's version
     */
    JarFile getBootProxyJarIfCurrent() {
        File dataFile = bundleContext.getDataFile("boot-proxy.jar");
        if (!dataFile.exists()) {
            return null;
        }

        JarFile jarFile = null;
        try {
            jarFile = new JarFile(dataFile);
            Manifest manifest = jarFile.getManifest();
            Attributes attrs = manifest.getMainAttributes();
            String jarVersion = attrs.getValue(MONITORING_VERSION_MANIFEST_HEADER);
            if (!getCurrentVersion().equals(jarVersion)) {
                jarFile.close();
                jarFile = null;
            }
        } catch (Exception e) {
        }

        return jarFile;
    }

    /**
     * Create a jar file that contains the proxy code that will live in the
     * boot delegation package.
     *
     * @return the jar file containing the proxy code to append to the boot
     *         class path
     *
     * @throws IOException if a file I/O error occurs
     */
    JarFile createBootProxyJar() throws IOException {
        File dataFile = bundleContext.getDataFile("boot-proxy.jar");

        // Create the file if it doesn't already exist
        if (!dataFile.exists()) {
            dataFile.createNewFile();
        }

        // Generate a manifest
        Manifest manifest = createBootJarManifest();

        // Create the file
        FileOutputStream fileOutputStream = new FileOutputStream(dataFile, false);
        JarOutputStream jarOutputStream = new JarOutputStream(fileOutputStream, manifest);

        // Add the jar path entries to reduce class load times
        createDirectoryEntries(jarOutputStream, BOOT_DELEGATED_PACKAGE);

        // Map the template classes into the delegation package and add to the jar
        Bundle bundle = bundleContext.getBundle();
        Enumeration<?> entryPaths = bundle.getEntryPaths(TEMPLATE_CLASSES_PATH);
        if (entryPaths != null) {
            while (entryPaths.hasMoreElements()) {
                URL sourceClassResource = bundle.getEntry((String) entryPaths.nextElement());
                if (sourceClassResource != null)
                    writeRemappedClass(sourceClassResource, jarOutputStream, BOOT_DELEGATED_PACKAGE);
            }
        }

        jarOutputStream.close();
        fileOutputStream.close();

        return new JarFile(dataFile);
    }

    /**
     * Create the jar directory entries corresponding to the specified package
     * name.
     *
     * @param jarStream   the target jar's output stream
     * @param packageName the target package name
     *
     * @throws IOException if an IO exception raised while creating the entries
     */
    public void createDirectoryEntries(JarOutputStream jarStream, String packageName) throws IOException {
        StringBuilder entryName = new StringBuilder(packageName.length());
        for (String str : packageName.split("\\.")) {
            entryName.append(str).append("/");
            JarEntry jarEntry = new JarEntry(entryName.toString());
            jarStream.putNextEntry(jarEntry);
        }
    }

    /**
     * Transform the proxy template class that's in this package into a class
     * that's in a package on the framework boot delegation package list.
     *
     * @return the byte array containing the updated class
     *
     * @throws IOException if an IO exception raised while processing the class
     */
    private void writeRemappedClass(URL classUrl, JarOutputStream jarStream, String targetPackage) throws IOException {
        InputStream inputStream = classUrl.openStream();
        String sourceInternalName = getClassInternalName(classUrl);
        String targetInternalName = getTargetInternalName(sourceInternalName, targetPackage);
        SimpleRemapper remapper = new SimpleRemapper(sourceInternalName, targetInternalName);

        ClassReader reader = new ClassReader(inputStream);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        ClassRemapper remappingVisitor = new ClassRemapper(writer, remapper);
        ClassVisitor versionVisitor = new AddVersionFieldClassAdapter(remappingVisitor, VERSION_FIELD_NAME, getCurrentVersion());
        reader.accept(versionVisitor, ClassReader.EXPAND_FRAMES);

        JarEntry jarEntry = new JarEntry(targetInternalName + ".class");
        jarStream.putNextEntry(jarEntry);
        jarStream.write(writer.toByteArray());
    }

    /**
     * Get the internal class name of the class referenced by {@code classUrl}.
     *
     * @param classUrl the URL of the class to inspect
     *
     * @return the internal class name of the class referenced by {@code classUrl}
     *
     * @throws IOException if an IO error occurs during processing
     */
    String getClassInternalName(URL classUrl) throws IOException {
        InputStream inputStream = classUrl.openStream();

        ClassReader reader = new ClassReader(inputStream);
        reader.accept(new ClassVisitor(Opcodes.ASM8) {
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        inputStream.close();

        return reader.getClassName();
    }

    /**
     * Get the class internal name that should be used where moving the internal
     * class across packages.
     *
     * @param sourceInternalName the internal name of the template class
     * @param targetPackage      the package to move the class to
     *
     * @return the target class name
     */
    String getTargetInternalName(String sourceInternalName, String targetPackage) {
        StringBuilder targetInternalName = new StringBuilder();
        targetInternalName.append(targetPackage.replaceAll("\\.", "/"));

        int lastSlashIndex = sourceInternalName.lastIndexOf('/');
        targetInternalName.append(sourceInternalName.substring(lastSlashIndex));

        return targetInternalName.toString();
    }

    /**
     * Create the {@code Manifest} for the boot proxy jar.
     *
     * @return the boot proxy jar {@code Manifest}
     */
    Manifest createBootJarManifest() {
        Manifest manifest = new Manifest();

        Attributes manifestAttributes = manifest.getMainAttributes();
        manifestAttributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        manifestAttributes.putValue("Created-By", "Liberty Monitoring Extender");
        manifestAttributes.putValue("Created-Time", DateFormat.getInstance().format(new Date()));
        manifestAttributes.putValue(MONITORING_VERSION_MANIFEST_HEADER, getCurrentVersion());

        return manifest;
    }

    /**
     * Get the host bundle's version string.
     *
     * @return the host bundle's version string
     */
    String getCurrentVersion() {
        return bundleContext.getBundle().getVersion().toString();
    }

    /**
     * Reflectively load the boot loader resident proxy and find {@code setTarget} method.
     *
     * @return the {@code setTarget} method
     *
     * @throws Exception if the {@code setTarget} method can't be resolved
     */
    Method findProbeProxySetFireProbeTargetMethod() throws Exception {
        Class<?> proxyClass = Class.forName(PROBE_PROXY_CLASS_NAME);
        Method setFireProbeTargetMethod = ReflectionHelper.getDeclaredMethod(
                                                                             proxyClass,
                                                                             "setFireProbeTarget",
                                                                             Object.class, Method.class);
        ReflectionHelper.setAccessible(setFireProbeTargetMethod, true);

        return setFireProbeTargetMethod;
    }

    /**
     * Hook up the monitoring boot proxy delegate.
     *
     * @throws Exception the method invocation exception
     */
    void activateProbeProxyTarget() throws Exception {
        Method method = ReflectionHelper.getDeclaredMethod(
                                                           probeManagerImpl.getClass(),
                                                           ProbeMethodAdapter.FIRE_PROBE_METHOD_NAME,
                                                           long.class, Object.class, Object.class, Object.class);
        ReflectionHelper.setAccessible(method, true);
        if (!Type.getMethodDescriptor(method).equals(ProbeMethodAdapter.FIRE_PROBE_METHOD_DESC)) {
            throw new IncompatibleClassChangeError("Proxy method signature does not match byte code");
        }
        findProbeProxySetFireProbeTargetMethod().invoke(null, probeManagerImpl, method);
    }

    /**
     * Clear the monitoring boot proxy delegate.
     *
     * @throws Exception the method invocation exception
     */
    void deactivateProbeProxyTarget() throws Exception {
        findProbeProxySetFireProbeTargetMethod().invoke(null, null, null);
    }

    /**
     * Reflectively locate the {@code setClassAvailableTarget} method on
     * the proxy at runtime.
     *
     * @return the {@code setProcessCandidateTarget} method instance
     *
     * @throws Exception if a reflection error occurred
     */
    Method findClassAvailableProxySetClassAvailableTargetMethod() throws Exception {
        Class<?> proxyClass = Class.forName(CLASS_AVAILABLE_PROXY_CLASS_NAME);
        Method method = ReflectionHelper.getDeclaredMethod(proxyClass, "setClassAvailableTarget", Object.class, Method.class);
        ReflectionHelper.setAccessible(method, true);
        return method;
    }

    /**
     * Activate the {@link #ClassAvailableProxy} by setting ourselves as the
     * delegate.
     *
     * @throws Exception if a reflection error occurred
     */
    void activateClassAvailableProxyTarget() throws Exception {
        Method method = ReflectionHelper.getDeclaredMethod(getClass(), "classAvailable", Class.class);
        findClassAvailableProxySetClassAvailableTargetMethod().invoke(null, this, method);
    }

    /**
     * Deactivate the {@link ClassAvailableProxy} by clearing the delegate.
     *
     * @throws Exception if a reflection error occurred
     */
    void deactivateClassAvailableProxyTarget() throws Exception {
        findClassAvailableProxySetClassAvailableTargetMethod().invoke(null, null, null);
    }

    /**
     * The {@link ClassAvailableProxy#classAvailable} delegate method.
     *
     * @param clazz the recently initialized class
     */
    public void classAvailable(Class<?> clazz) {
        probeManagerImpl.classAvailable(clazz);
    }
}
