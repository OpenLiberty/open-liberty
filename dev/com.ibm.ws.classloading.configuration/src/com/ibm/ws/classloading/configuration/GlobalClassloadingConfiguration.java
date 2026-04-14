/*******************************************************************************
 * Copyright (c) 2013, 2026 IBM Corporation and others.
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
package com.ibm.ws.classloading.configuration;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.service.util.JavaInfo;

@Component(service = GlobalClassloadingConfiguration.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE,
           configurationPid = "com.ibm.ws.classloading.global", property = "service.vendor=IBM")
public class GlobalClassloadingConfiguration {
    private static final String CLASSLOADING_APP_PARENT_PROP = "io.openliberty.classloading.app.parent";
    private static final String CLASSLOADING_APP_PARENT_PACKAGES_PROP = "io.openliberty.classloading.app.parent.packages";

    private static final String USE_JAR_URLS_KEY = "useJarUrls";
    private static final String LIBRARY_PRECEDENCE_KEY = "libraryPrecedence";
    static final TraceComponent tc = Tr.register(GlobalClassloadingConfiguration.class);

    static final boolean java9Plus = JavaInfo.majorVersion() >= 9;
    static final ClassLoader platformClassLoader = getPlatformClassLoader();

    private static ClassLoader getPlatformClassLoader() {
        ClassLoader result = null;
        if (java9Plus) {
            try {
                Method getPlatformClassLoader = ClassLoader.class.getMethod("getPlatformClassLoader"); //$NON-NLS-1$
                result = (ClassLoader) getPlatformClassLoader.invoke(null);
            } catch (Throwable t) {
                // auto FFDC here; it should never happen
            }
        }
        if (result == null) {
            // Try everything possible to not fail;

            // Here we make the assumption that the system class loader parent is the platform loader.
            // NOTE: On Java 8 the default system class loader has a parent called the extension loader.
            // This is not identical to the Java 9+ platform loader but we decided to use it here
            // instead of hiding it completely on Java 8 when PLATFORM is used.
            // In the future we could introduce a BOOT option that bypasses the platform/ext loaders altogether.
            ClassLoader systemParent = null;
            ClassLoader system = ClassLoader.getSystemClassLoader();
            if (system != null) {
                systemParent = system.getParent();
            }
            result = systemParent != null ? systemParent : new ClassLoader(null) {
            };
        }
        return result;
    }

    private final AtomicBoolean issuedLibPrecedenceBetaMessage = new AtomicBoolean(false);
    private final AtomicBoolean issuedClassLoaderParentBetaMessage = new AtomicBoolean(false);
    private final AtomicBoolean issuedClassLoaderParentPackagesBetaMessage = new AtomicBoolean(false);
    private volatile boolean useJarUrls = false;
    private volatile LibraryPrecedence libraryPrecedence = LibraryPrecedence.afterApp;
    private volatile JVMPackages jvmPackages = new JVMPackages(null, null, null, null);

    @Activate
    protected void activate(BundleContext context, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "activate: java9Plus=" + java9Plus + " platformClassLoader=" + platformClassLoader);
        }
        // Note these bootstrap props could become proper metatype config attributes at some point.
        String parentProp = getPropAndIssueBetaMessages(context, CLASSLOADING_APP_PARENT_PROP, issuedClassLoaderParentBetaMessage);
        String parentPackagesProp = getPropAndIssueBetaMessages(context, CLASSLOADING_APP_PARENT_PACKAGES_PROP, issuedClassLoaderParentPackagesBetaMessage);
        if (!java9Plus) {
            // for Java 8 we will not support parent packages
            if (parentPackagesProp != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "activate: Ignoring the " + CLASSLOADING_APP_PARENT_PACKAGES_PROP + " value on Java 8: " + parentPackagesProp);
                }
                parentPackagesProp = null;
            }
        }
        Set<String> mandatoryPackages = findSystemMandatoryPackages(context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION));
        jvmPackages = new JVMPackages(parentProp, parentPackagesProp, context.getProperty(BootstrapConstants.INITPROP_BOOT_PACKAGES), mandatoryPackages);
        modified(properties);
    }

    /**
     * @param parentPackagesProp
     * @return
     */
    private Set<String> findSystemMandatoryPackages(Bundle systemBundle) {
        if (!java9Plus) {
            // On Java 8 we always delegate to the parent loader;
            // We can avoid doing the system bundle wiring check for mandatory packages here.
            return Collections.emptySet();
        }
        BundleWiring systemWiring = systemBundle == null ? null : systemBundle.adapt(BundleWiring.class);
        if (systemWiring == null) {
            return Collections.emptySet();
        }
        // we only expect one package: javax.transaction.xa
        Set<String> result = new HashSet<>(1);
        for (BundleCapability export : systemWiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE)) {
            if (export.getDirectives().get(AbstractWiringNamespace.CAPABILITY_MANDATORY_DIRECTIVE) != null) {
                // We don't care what the mandatory directive value is.  If it is anything but null
                // we add the package name to the mandatory packages to avoid filtering it.
                result.add((String) export.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
            }
        }
        if (result.size() == 1) {
            // optimize the expected case with only javax.transaction.xa
            result = Collections.singleton(result.iterator().next());
        }
        return result;

    }

    private static String getPropAndIssueBetaMessages(BundleContext context, String propKey, AtomicBoolean issueBetaMessage) {
        String propValue = context.getProperty(propKey);
        if (propValue == null) {
            return null;
        }
        if (ProductInfo.getBetaEdition()) {
            if (issueBetaMessage.compareAndSet(false, true)) {
                Tr.info(tc, "BETA: The beta property '" + propKey + "' is being used with a value: " + propValue);
            }
        } else {
            if (issueBetaMessage.compareAndSet(false, true)) {
                Tr.info(tc, "BETA: The beta property '" + propKey + "' is only allowed when using the beta.");
            }
            // ignore value
            return null;
        }
        return propValue;
    }

    @Modified
    protected void modified(Map<String, Object> props) {
        this.useJarUrls = (Boolean) props.get(USE_JAR_URLS_KEY);
        LibraryPrecedence checkValue = LibraryPrecedence.valueOf((String) props.get(LIBRARY_PRECEDENCE_KEY));
        if (checkValue == LibraryPrecedence.beforeApp) {
            if (!ProductInfo.getBetaEdition()) {
                checkValue = LibraryPrecedence.afterApp;
                if (issuedLibPrecedenceBetaMessage.compareAndSet(false, true)) {
                    Tr.info(tc, "BETA: The attribute '" + LIBRARY_PRECEDENCE_KEY + "' can only be used with the Open Liberty BETA.");
                }
            } else {
                // Running beta exception, issue message if we haven't already issued one for this class
                if (issuedLibPrecedenceBetaMessage.compareAndSet(false, true)) {
                    Tr.info(tc, "BETA: The attribute '" + LIBRARY_PRECEDENCE_KEY + "' is being used with the value '" + checkValue + "'");
                }
            }
        }
        libraryPrecedence = checkValue;
    }

    /**
     * @return
     */
    public boolean useJarUrls() {
        return useJarUrls;
    }

    public enum LibraryPrecedence {
        beforeApp,
        afterApp
    }

    public LibraryPrecedence libraryPrecedence() {
        return libraryPrecedence;
    }

    public static class JVMPackages {
        private static enum ParentConfig {
            PLATFORM,
            SYSTEM
        }

        private final ParentConfig parentConfig;
        private final ClassLoader parentCL;
        private final Set<String> parentPackages;
        private final Set<String> mandatoryPackages;

        public JVMPackages(String parentProp, String parentPackagesProp, String bootPackages, Set<String> mandatoryPackages) {
            Set<String> extraPackages = null;
            if (parentPackagesProp != null) {
                extraPackages = new HashSet<String>();
                // an empty string means no extra packages; use an empty set.
                if (!parentPackagesProp.isEmpty()) {
                    String[] extraPackagesArray = parentPackagesProp.split(",");
                    for (String pkg : extraPackagesArray) {
                        extraPackages.add(pkg.trim());
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "JVMPackages: configured parent packages value: " + extraPackages);
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "JVMPackages: configured with empty parent packages value.");
                    }
                }
            }
            ParentConfig result = ParentConfig.SYSTEM;
            try {
                result = parentProp == null ? ParentConfig.SYSTEM : ParentConfig.valueOf(parentProp);
            } catch (IllegalArgumentException e) {
                // auto FFDC
                result = ParentConfig.SYSTEM;
            }
            parentConfig = result;
            parentCL = parentConfig == ParentConfig.PLATFORM ? GlobalClassloadingConfiguration.platformClassLoader : ClassLoader.getSystemClassLoader();
            parentPackages = discoverParentPackages(parentConfig, bootPackages, extraPackages);
            this.mandatoryPackages = mandatoryPackages == null ? Collections.emptySet() : mandatoryPackages;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,
                         "JVMPackages: parentConfig=" + parentConfig + " parentCL=" + parentCL + " parentPackages=" + parentPackages + " mandatoryPackages=" + mandatoryPackages);
            }
        }

        /**
         * @param extraPackages
         * @param context
         * @return
         */
        private static Set<String> discoverParentPackages(ParentConfig parentConfig, String bootPackages, Set<String> extraPackages) {
            // NOTE: returning a null set will cause nothing to be filtered;
            //       returning an empty set will cause everything to be filtered;
            //       returning a non-empty set will cause everything not in the set to be filtered.
            if (usingBootclasspathJVMOption()) {
                // if using -Xbootclasspath we cannot filter anything
                return null;
            }
            if (parentConfig == ParentConfig.PLATFORM) {
                // A strange case where the user configured extra packages with PLATFORM;
                // It is arguable that this should not be allowed since we do not believe
                // there would be any extra packages that the system bundle didn't provide.
                // By default extraPackages will be null.

                // We will filter every package when PLATFORM is used since there
                // is no need to do this double delegation (empty set).
                // If the user configures extra packages with PLATFORM then these packages
                // WILL be delegated to platform.  This provides a sort of out if things are
                // not working as expected from the gateway bundle loader delegation.
                if (java9Plus) {
                    Set<String> result = extraPackages == null ? Collections.emptySet() : extraPackages;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "discoverParentPackages: Java 9+ using PLATFORM with extraPackages=" + result);
                    }
                    return result;
                }
                // On Java8 we do not have Java APIs to get the exact set of packages available
                // in the platform so by default we filter nothing (null).  The extraPackages option
                // allows an out if the user wants to control what packages we delegate to platform for on Java 8.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "discoverParentPackages: Java 8 using PLATFORM with extraPackages=" + extraPackages);
                }
                return extraPackages;
            }

            if (bootPackages == null) {
                // If we don't know what the boot packages are then we cannot filter anything.
                // This probably would be a bug.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "discoverParentPackages: returning null because bootPackages==null");
                }
                return null;
            }

            if (!java9Plus && extraPackages == null) {
                // On Java 8 we know the system bundle does not get configured for ALL available
                // packages available in the platform.  If the user did not configure extra packages
                // then we cannot filter anything (null)
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "discoverParentPackages: returning null because Java 8 is being used.");
                }
                return null;
            }

            Set<String> result = null;
            if (isDefaultClassPath() || extraPackages != null) {
                result = addPackages(bootPackages, extraPackages);
                // probably shouldn't hardcode this; but easier to just always add it and does no harm
                result.add("com.ibm.ws.kernel.instrument");
            }

            return result;
        }

        /**
         * @return
         */
        private static boolean usingBootclasspathJVMOption() {
            for (String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (jvmArg.startsWith("-Xbootclasspath")) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @param jvmClasspath
         * @return
         */
        private static boolean isDefaultClassPath() {
            // Finding the JVM classpath is not straight forward.
            // There is the java.class.path property but that only will
            // include the ws-server.jar from using the java -jar option
            // in the server script. We first check the java.class.path
            // value.  If it contains anything other than ws-server.jar
            // then we indicate this is not the default;
            String javaClassPathProp = System.getProperty("java.class.path");
            if (javaClassPathProp == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "isDefaultClassPath returning false with no java.class.path value.");
                }
                // weird case; maybe using module path? regardless we report this is not the default
                return false;
            }
            // do a simple file check on the path assuming it is a single path value
            File wsServerJarFile = new File(javaClassPathProp);
            if (!wsServerJarFile.exists() || !"ws-server.jar".equals(wsServerJarFile.getName())) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "isDefaultClassPath returning false with unrecognized path: " + javaClassPathProp);
                }
                return false;
            }

            try {
                // Unfortunately the java.class.path prop does not contain any JARs from java agents
                Set<URL> systemManifests = new HashSet<>(Collections.list(ClassLoader.getSystemClassLoader().getResources("META-INF/MANIFEST.MF")));
                // Remove any manifests from the platform (this may no longer be needed on Java 9+ because platform has only modules (with no manifests)
                systemManifests.removeAll(Collections.list(platformClassLoader.getResources("META-INF/MANIFEST.MF")));
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "isDefaultClassPath found the following manifests on the JVM classpath: " + systemManifests);
                }
                for (URL url : systemManifests) {
                    // This is using fuzzy in matching.  We simply check for the expected names in the path.
                    // This isn't perfect matching since someone could use the following strings
                    // somewhere in a component of their paths on the class path.  But that seems highly unlikely.
                    // Trying to avoid more complicated checking of the exact paths here by using contains checks
                    String path = url.getPath();
                    if (path.contains("ws-server.jar")) {
                        continue;
                    }
                    if (path.contains("ws-javaagent.jar")) {
                        continue;
                    }
                    if (path.contains("com.ibm.ws.kernel.boot_")) {
                        continue;
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "isDefaultClassPath returning false with unrecognized path: " + path);
                    }
                    // detected non-default path
                    return false;
                }
            } catch (IOException e) {
                // this shouldn't happen for getResources calls; treat as non-default and auto-ffdc
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "isDefaultClassPath returning false with IOException: " + e.getMessage());
                }
                return false;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "isDefaultClassPath returning true");
            }
            return true;
        }

        private static Set<String> addPackages(String packages, Set<String> packageSet) {
            if (packageSet == null) {
                packageSet = new HashSet<String>();
            }
            ManifestElement[] bootPackageElements;
            try {
                // NOTE: the header name used in parseHeaders doesn't matter; Add-Packages is made up.
                // The only use of it would be for the exception message
                bootPackageElements = ManifestElement.parseHeader("Add-Packages", packages);
            } catch (BundleException e) {
                // auto FFDC; treat the class path as unknown
                return packageSet;
            }
            if (bootPackageElements != null) {
                for (ManifestElement p : bootPackageElements) {
                    packageSet.add(p.getValue());
                }
            }
            return packageSet;
        }

        public ClassLoader delegate() {
            return parentCL;
        }

        @FFDCIgnore(ClassNotFoundException.class)
        public Class<?> loadClass(String className, boolean throwException) throws ClassNotFoundException {
            if (filterPackageClass(className)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "loadClass: filtered class load from gateway parent: " + className);
                }
                if (throwException) {
                    throw new ClassNotFoundException(className);
                } else {
                    return null;
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "loadClass: loading class from gateway parent: " + className);
            }
            try {
                return parentCL.loadClass(className);
            } catch (ClassNotFoundException e) {
                if (throwException) {
                    throw e;
                }
            }
            return null;
        }

        private boolean filterPackageClass(String className) {
            if (parentPackages == null) {
                // filter nothing
                return false;
            }
            int lastDot = className.lastIndexOf('.');
            if (lastDot > 0) {
                String packageName = className.substring(0, lastDot);
                return filterPackage(packageName);
            }
            // must be the default package; don't filter
            return false;
        }

        private boolean filterPackageResource(String resName) {
            if (parentPackages == null) {
                // filter nothing
                return false;
            }
            int begin = ((resName.length() > 1) && (resName.charAt(0) == '/')) ? 1 : 0;
            int end = resName.lastIndexOf('/'); /* index of last slash */
            if (end > begin) {
                String packageName = resName.substring(begin, end).replace('/', '.');
                return filterPackage(packageName);
            }
            // must be the default package; don't filter
            return false;
        }

        private boolean filterPackage(String packageName) {
            if (mandatoryPackages.contains(packageName)) {
                // never filter mandatory packages
                return false;
            }
            if (parentPackages.isEmpty()) {
                // filter everything else
                return true;
            }
            // or filter anything else not in the parentPackages set
            return !parentPackages.contains(packageName);
        }

        public URL getResource(String resName) {
            if (filterPackageResource(resName)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getResource: filtered get resource from gateway parent: " + resName);
                }
                return null;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getResource: getting resource from gateway parent: " + resName);
            }
            return parentCL.getResource(resName);
        }

        public Enumeration<URL> getResources(String resName) throws IOException {
            if (filterPackageResource(resName)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getResources: filtered get resources from gateway parent: " + resName);
                }
                return Collections.emptyEnumeration();
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getResources: getting resources from gateway parent: " + resName);
            }
            return parentCL.getResources(resName);
        }
    }

    public JVMPackages jvmPackages() {
        return jvmPackages;
    }
}
