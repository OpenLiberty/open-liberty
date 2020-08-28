/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi.stackjoiner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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

import com.ibm.ws.logging.internal.osgi.stackjoiner.bci.AddVersionFieldClassAdapter;
import com.ibm.ws.logging.internal.osgi.stackjoiner.bci.ThrowableClassFileTransformer;
import com.ibm.ws.logging.internal.osgi.stackjoiner.boot.templates.ThrowableProxy;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentException;

public class ThrowableProxyActivator {
	
	private ThrowableInfo throwableInfo;
	private Instrumentation inst;
	private BundleContext bundleContext;
	
	/**
     * The target package for the boot loader delegated classes.
     */
    public final static String BOOT_DELEGATED_PACKAGE = "com.ibm.ws.boot.delegated.logging";
    
    /**
     * The bundle entry path prefix to the template classes.
     */
    final static String TEMPLATE_CLASSES_PATH = ThrowableProxy.class.getPackage().getName().replaceAll("\\.", "/");
    
    /**
     * The name of the {@link ThrowableProxy} class that needs to be
     * made available on the bootstrap class loader.
     */
    public final static String THROWABLE_PROXY_CLASS_NAME = BOOT_DELEGATED_PACKAGE + "." + ThrowableProxy.class.getSimpleName();

    /**
     * The internal name of the {@link ThrowableProxy} class.
     */
    public final static String THROWABLE_PROXY_CLASS_INTERNAL_NAME = THROWABLE_PROXY_CLASS_NAME.replaceAll("\\.", "/");
    
    /**
     * The name of the field in generated classes that will contain the
     * generating bundle version.
     */
    final static String VERSION_FIELD_NAME = "BUNDLE_VERSION";

    /**
     * The proxy jar manifest header that indicates the version of the bundle
     * that created the jar.
     */
    final static String LOGGING_VERSION_MANIFEST_HEADER = "Liberty-Logging-Osgi-Bundle-Version";
    
	
    public ThrowableProxyActivator(Instrumentation inst, BundleContext bundleContext) {
    	this.inst = inst;
    	this.bundleContext = bundleContext;
    }
    
    /**
     * Activation callback from the Declarative Services runtime where the
     * component is ready for activation.
     *
     * @param bundleContext the bundleContext
     */
    protected void activate() throws Exception {    
        // Store a reference to the printStackTraceOverride method from BaseTraceService
        throwableInfo = new ThrowableInfo(inst);
        
        if (throwableInfo.isInitialized()) {
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
    	        inst.appendToBootstrapClassLoaderSearch(proxyJar);
            }
            
        	activateThrowableProxyMethodTarget();
    
			inst.addTransformer(new ThrowableClassFileTransformer(), true);
			for (Class<?> clazz : inst.getAllLoadedClasses()) {
			    if (clazz.getName().equals("java.lang.Throwable")) {
			    	try {
			            inst.retransformClasses(clazz);
			        } catch (Throwable t) {
			            t.printStackTrace();
			        }
			     }
			}
        }
    	
	}
	
	protected void deactivate() throws Exception {
        try {
        	if (throwableInfo.isInitialized())
        		deactivateThrowableProxyTarget();
        } catch (Exception e) {
            throw new Exception(e);
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
            Class<?> clazz = Class.forName(THROWABLE_PROXY_CLASS_NAME);
            Field version = ReflectionHelper.getDeclaredField(clazz, VERSION_FIELD_NAME);
            runtimeVersion = (String) version.get(null);
        } catch (Exception e) {
        }
        return runtimeVersion;
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
        File dataFile = bundleContext.getDataFile("boot-proxy-throwable.jar");

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
     * Create the jar directory entries corresponding to the specified package
     * name.
     *
     * @param jarStream the target jar's output stream
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
        reader.accept(new ClassVisitor(Opcodes.ASM7) {}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        inputStream.close();

        return reader.getClassName();
    }

    /**
     * Get the class internal name that should be used where moving the internal
     * class across packages.
     *
     * @param sourceInternalName the internal name of the template class
     * @param targetPackage the package to move the class to
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
     * Get the boot proxy jar from the current data area if the code
     * matches the current bundle version.
     *
     * @return the proxy jar iff the proxy jar exits and matches this
     *         bundle's version
     */
    JarFile getBootProxyJarIfCurrent() {
        File dataFile = bundleContext.getDataFile("boot-proxy-throwable.jar");
        if (!dataFile.exists()) {
            return null;
        }

        JarFile jarFile = null;
        try {
            jarFile = new JarFile(dataFile);
            Manifest manifest = jarFile.getManifest();
            Attributes attrs = manifest.getMainAttributes();
            String jarVersion = attrs.getValue(LOGGING_VERSION_MANIFEST_HEADER);
            if (!getCurrentVersion().equals(jarVersion)) {
                jarFile.close();
                jarFile = null;
            }
        } catch (Exception e) {
        }

        return jarFile;
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
        manifestAttributes.putValue("Created-By", "Liberty Logging Osgi Extender");
        manifestAttributes.putValue("Created-Time", DateFormat.getInstance().format(new Date()));
        manifestAttributes.putValue(LOGGING_VERSION_MANIFEST_HEADER, getCurrentVersion());

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
     * Binds this ThrowableProxyActivator class' printStackTraceOverride method to ThrowableProxy.
     *
     * @throws Exception the method invocation exception
     */
    void activateThrowableProxyMethodTarget() throws Exception {
        Method method = ReflectionHelper.getDeclaredMethod(getClass(), "printStackTraceOverride", Throwable.class, PrintStream.class);
        ReflectionHelper.setAccessible(method, true);
        findThrowableProxySetFireTargetMethod().invoke(null, this, method);
    }
    
    /**
     * Returns the Method object representing a setter for the ThrowableProxy instance variables.
     *
     * @return
     * @throws Exception
     */
    Method findThrowableProxySetFireTargetMethod() throws Exception {
        Class<?> proxyClass = Class.forName(THROWABLE_PROXY_CLASS_NAME);
        Method method = ReflectionHelper.getDeclaredMethod(proxyClass, "setFireTarget", Object.class, Method.class);
        ReflectionHelper.setAccessible(method, true);
        return method;
    }

    /**
     * Unbinds the Activator class' and its method from ThrowableProxy.
     *
     * @throws Exception
     */
    void deactivateThrowableProxyTarget() throws Exception {
        findThrowableProxySetFireTargetMethod().invoke(null, null, null);
    }

    /**
     * Invokes the static BaseTraceService.printStackTraceOverride() method from the com.ibm.ws.logging package.
     * This method is bound to the ThrowableProxy, which will be visible by the bootstrap class loader
     * 
     * @param t the
     * @param originalStream that is retrieved via ThrowableMethodAdpater 
     * 
     * @return true if printStackTraceOverride() method in BaseTraceService evaluated to true, false otherwise
     */
    public boolean printStackTraceOverride(Throwable t, PrintStream originalStream) {
        Method method = throwableInfo.getBtsMethod();
        Boolean b = Boolean.FALSE;
        try {
            b = (Boolean) method.invoke(null, t, originalStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return b;
    }
}
