/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import com.ibm.ws.logging.internal.impl.LoggingConstants;
import com.ibm.ws.logging.internal.osgi.Activator;
import com.ibm.ws.logging.internal.osgi.stackjoiner.bci.AddVersionFieldClassAdapter;
import com.ibm.ws.logging.internal.osgi.stackjoiner.bci.ThrowableClassFileTransformer;
import com.ibm.ws.logging.internal.osgi.stackjoiner.boot.templates.ThrowableProxy;
import com.ibm.ws.logging.utils.StackJoinerConfigurations;

import io.openliberty.asm.ASMHelper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;

/**
 * Stack Joiner functionality references the configuration which
 * enables the merger or joining of the multiple log records created
 * from a printStackTrace() call that results in each line of the stack
 * trace being processed as a new and unique log record.
 *
 * The configuration key follows the template "stackTraceSingleEntry" for
 * server.xml, env var and bootstrap properties. However, the functionality
 * is referred to as stack joiner.
 *
 * There-in also exists additional configuration (only env var) for the buffer size
 * of which how large a stack trace would be merge/joined. See the {@link LoggingConstants} fields
 * for more information .
 */
public class StackJoinerManager {
    
    private static final TraceComponent tc = Tr.register(StackJoinerManager.class);
    
    private MethodProxy methodProxy;
    
    private static volatile boolean isThrowableClassTransformed = false;

    public final String BOOT_DELEGATED_PACKAGE = "com.ibm.ws.boot.delegated.logging";
    
    final String TEMPLATE_CLASSES_PATH = ThrowableProxy.class.getPackage().getName().replaceAll("\\.", "/");
    
    public final String THROWABLE_PROXY_CLASS_NAME = BOOT_DELEGATED_PACKAGE + "." + ThrowableProxy.class.getSimpleName();
    
    public final String THROWABLE_PROXY_CLASS_INTERNAL_NAME = THROWABLE_PROXY_CLASS_NAME.replaceAll("\\.", "/");
    
    final  String VERSION_FIELD_NAME = "BUNDLE_VERSION";
    
    final String LOGGING_VERSION_MANIFEST_HEADER = "Liberty-Logging-Osgi-Bundle-Version";
    
    final String BASE_TRACE_SERVICE_CLASS_NAME = "com.ibm.ws.logging.internal.impl.BaseTraceService";
    
    final String BASE_TRACE_SERVICE_METHOD_NAME = "prePrintStackTrace";
    
    private final String STACK_JOIN_SERVER_XML_CONFIG_NAME = "stackTraceSingleEntry";
    
    private static Instrumentation instrumentation = null;
    private static BundleContext bundleContext = null;
    
    private static volatile StackJoinerManager instance = null;
    
    private StackJoinerManager() {};
    
    public static synchronized StackJoinerManager getInstance() {
        if (instance == null) {
            instance = new StackJoinerManager();
            instance.init();
        }
        return instance;
    }

    public void init() {
        instrumentation = Activator.getInstrumentation();
        bundleContext = Activator.getBundleContex();
    }
    
    public void activate() {
        // Already activated, nothing to do
        if (isThrowableClassTransformed) {
            return;
        }

        /*
         * If the instrumentation and bundle context objects have not been resolved the
         * first time, try to re-init().
         */
        if (instrumentation == null || bundleContext == null) {
            init();
            if (instrumentation == null || bundleContext == null) {
                //Unlikely to happen
                Tr.debug(tc, "Can not activate Stack Joiner functionality. Unable to resolve Instrumentation and Bundle Context references.");
                return;
            }
        }

        if (methodProxy == null) {
            /*
             * Create a methodProxy of the printStackTraceOverride method within the
             * BaseTraceService Class
             */
            methodProxy = new MethodProxy(instrumentation, BASE_TRACE_SERVICE_CLASS_NAME,
                    BASE_TRACE_SERVICE_METHOD_NAME, Throwable.class, PrintStream.class);
            if (!methodProxy.isInitialized()) {
                methodProxy = null;
                return;
            }
        }
        
        String runtimeVersion = getRuntimeClassVersion();
        ThrowableClassFileTransformer tcfTransformer = null;
        try {
            // Find or create the proxy jar if the runtime code isn't loaded
            if (runtimeVersion == null || (runtimeVersion != null && !runtimeVersion.equals(getCurrentVersion()))) {
                JarFile proxyJar = getBootProxyJarIfCurrent();
                if (proxyJar == null) {
                    proxyJar = createBootProxyJar();  //Exception can be thrown here 
                }
                instrumentation.appendToBootstrapClassLoaderSearch(proxyJar);
            }

            bindThrowableProxyMethodTarget(); //Exception can be thrown here 
            tcfTransformer = new ThrowableClassFileTransformer();

            instrumentation.addTransformer(tcfTransformer, true);
            instrumentation.retransformClasses(Throwable.class); // something to replace retransfornm // Exception can be thrown here 
            isThrowableClassTransformed = true;
        } catch (Exception e) {
            FFDCFilter.processException(e, this.getClass().getCanonicalName() + ".activate", "1");
            e.printStackTrace();
        } finally {
            if (tcfTransformer != null) {
                instrumentation.removeTransformer(tcfTransformer);
            }
        }
    }

    public void deactivate() {
        try {
            if (isThrowableClassTransformed) {
                instrumentation.retransformClasses(Throwable.class); // Transform Throwable.class back to its original
                                                                     // format
                isThrowableClassTransformed = false;
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, this.getClass().getCanonicalName() + ".deactivate", "2");
            e.printStackTrace();
        }
    }
    
    
    public synchronized void resolveStackJoinFeature(Map<String, Object> serverConfigMap) {
        boolean isStackJoinerEnabled = false;
        
        /*
         * Check the last stackJoin configuration (i.e. "stackTraceSingleEntry" )value from LogProviderImpl.
         *  Retrieved by calling StackJoinerConfiguration which accesses the configuration value
         * last set in BaseTraceService.
         * 
         * FYI: The updated(Dictionary) call in LoggingConfigurationService
         * calls TrConfigurator.update() which ultimately issues a call to the
         * TrDelegate.update(LogProviderConfig config) (i.e. BaseTraceService's update)
         * which will update the stackJoin field to the "new" or maintain the existing
         * state of stackJoin
         * 
         * 
         * If this is the first time this is called then it will be checking for the env
         * var or bootstrap properties value (if it has been set).
         * 
         * FYI: env var: WLP_LOGGING_STACK_TRACE_SINGLE_ENTRY bootstrap prop:
         * com.ibm.ws.logging.stackTraceSingleEntry
         * 
         * Any subsequent calls as noted above will retrieve the existing configuration
         * value that had been "last set".
         * 
         * The logic after this is to read the "new" (or non-modifed) configuration value from
         * a server.xml update.
         * 
         */
        isStackJoinerEnabled = StackJoinerConfigurations.stackJoinerEnabled();

        if (serverConfigMap != null) {
            Object o = serverConfigMap.get(STACK_JOIN_SERVER_XML_CONFIG_NAME);
            if (o != null) {
                isStackJoinerEnabled = (Boolean) o;
            }
        }
        
        if (isStackJoinerEnabled) {
            activate();
        } else {
            deactivate();
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
        reader.accept(new ClassVisitor(ASMHelper.getCurrentASM()) {}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
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
        } 
        catch (Exception e) {
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
    void bindThrowableProxyMethodTarget() throws Exception {
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
     * Invokes the static BaseTraceService.printStackTraceOverride() method from the com.ibm.ws.logging package.
     * This method is bound to the ThrowableProxy, which will be visible by the bootstrap class loader
     * 
     * @param t the
     * @param originalStream that is retrieved via ThrowableMethodAdpater 
     * 
     * @return true if printStackTraceOverride() method in BaseTraceService evaluated to true, false otherwise
     */
    public boolean printStackTraceOverride(Throwable t, PrintStream originalStream) {
        Method method = methodProxy.getMethodProxy();
        Boolean b = false;
        try {
            b = (Boolean) method.invoke(null, t, originalStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return b;
    }
   
}
