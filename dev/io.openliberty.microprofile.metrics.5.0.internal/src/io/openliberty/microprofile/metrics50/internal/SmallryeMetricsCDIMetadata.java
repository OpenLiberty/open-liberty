/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.metrics50.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.VersionRange;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.library.Library;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import io.openliberty.microprofile.metrics50.helper.Util;
import io.openliberty.smallrye.metrics.cdi.adapters.LegacyMetricsExtensionAdapter;
import jakarta.enterprise.inject.spi.Extension;

@Component(service = { CDIExtensionMetadata.class, SmallryeMetricsCDIMetadata.class },
        configurationPid = "com.ibm.ws.microprofile.metrics", configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true, property = { "service.vendor=IBM" })
public class SmallryeMetricsCDIMetadata implements CDIExtensionMetadata {

    private static final TraceComponent tc = Tr.register(SmallryeMetricsCDIMetadata.class);
    private ClassLoadingService classLoadingService;
    private volatile Library sharedLib = null;
    private static boolean isSuccesfulActivation = true;

    /**
     * Used by SharedMetricRegistries to figure out if it should provide any
     * MetricRegistries. Since we cannot stop activation, this is a flag to use to
     * "stop" it from emitting dramatic runtime errors.
     *
     * @return boolean did SmallryeMetricsCDIMetadata activate properly
     */
    public boolean isSuccesfulActivation() {
        return isSuccesfulActivation;
    }

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) throws Exception {

        File smallRyeMetricsJarFile;
        try {
            smallRyeMetricsJarFile = resolveSmallRyeMetricsJar();
        } catch (FileNotFoundException e) {
            // TODO: specific ERROR/WARNING?
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, e.getMessage());
            }
            isSuccesfulActivation = false;
            return;
        }

        List<File> classPath = new ArrayList<File>();
        classPath.add(smallRyeMetricsJarFile);

        boolean isSharedLibAvailable = (sharedLib != null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && isSharedLibAvailable) {
            Tr.debug(tc, "Library " + sharedLib.id()
                    + "provided for the mpMetrics feature. The mpMetrics feature will not use the default embedded Micrometer Core and Micrometer Prometheus Registry.");
        }

        /*
         * No Library Reference detected. Will use default embedded Micrometer
         * libraries.
         */
        if (!isSharedLibAvailable) {
            try {
                File micrometerJarFile = resolveMicrometerJar();
                classPath.add(micrometerJarFile);
            } catch (FileNotFoundException e) {
                // TODO: specific ERROR/WARNING?
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, e.getMessage());
                }
                isSuccesfulActivation = false;
                return;
            }
        }

        try {
            ClassLoader bundleAddOnCL = createBundleAddOnClassLoader(classPath, isSharedLibAvailable);
            Util.BUNDLE_ADD_ON_CLASSLOADER = bundleAddOnCL;
            loadSmallRyeClasses(bundleAddOnCL);
        } catch (IllegalStateException e) {// for createBundleAddOnClassLoader()
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not create bundle add on class loader.");
            }
            // TODO: Make an actual message.
            Tr.error(tc, "UNABLE TO INITIALIZE MICROPROFILE METRICS 5.0 FEATURE DUE TO: " + e);
            isSuccesfulActivation = false;
        } catch (ClassNotFoundException e) { // for loadSmallRyeClasses()
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "SmallRye Metric classes could not be loaded.");
            } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && isSharedLibAvailable) {
                Tr.debug(tc, "SmallRye Metric classes could not be loaded from provided Library Reference.");
            }

            // TODO: Make actual message
            Tr.error(tc, "UNABLE TO INITIALIZE MICROPROFILE METRICS 5.0 FEATURE DUE TO: " + e);
            isSuccesfulActivation = false;
        } catch (NoClassDefFoundError e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                /*
                 * Probably unable to to load Prometheus registry classes (error with shared
                 * library).
                 */
                Tr.debug(tc, "Unable to load necessary classes for SmallRye Metrics");
            }

            // TODO: Make actual message
            Tr.error(tc, "UNABLE TO INITIALIZE MICROPROFILE METRICS 5.0 FEATURE DUE TO: " + e);
            isSuccesfulActivation = false;
            /*
             * Trying to throw an exception to stop activation does not work. OSGI will keep
             * trying to restart service. This will result in multiple console outputs.
             * throw new Exception("Unable to initialize MicroProfile Metrics 5.0 feature");
             */
        }
    }

    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        /*
         * Might as well not register a CDI extension.
         */
        if (!isSuccesfulActivation) {
            return null;
        }
        Set<Class<? extends Extension>> extensions = new HashSet<Class<? extends Extension>>();
        extensions.add(LegacyMetricsExtensionAdapter.class);
        return extensions;
    }

    @Reference(name = "classLoadingService", service = ClassLoadingService.class)
    protected void setClassLoadingService(ClassLoadingService ref) throws Exception {
        classLoadingService = ref;
    }

    protected void unsetClassLoadingService() {
        classLoadingService = null;
    }

    @Reference(name = "sharedLib", service = Library.class, cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, target = "(id=unbound)")
    protected void setSharedLib(Library ref) throws Exception {
        sharedLib = ref;
    }

    protected void unsetSharedLib(Library ref) {
        sharedLib = null;
    }

    private File resolveSmallRyeMetricsJar() throws FileNotFoundException {
        ContentBasedLocalBundleRepository cblb = BundleRepositoryRegistry.getInstallBundleRepository();
        VersionRange vr = VersionRange.valueOf(VersionRange.LEFT_CLOSED + "1,5.0.100" + VersionRange.RIGHT_CLOSED);
        /*
         * Sometimes.. the first call returns a null file. Debug shows that the class
         * loader was unable to load the ContentBasedLocalBundleRepository
         */
        cblb.selectBundle("lib/", "io.openliberty.smallrye.metrics", vr);
        File f = cblb.selectBundle("lib/", "io.openliberty.smallrye.metrics", vr);

        if (f == null) {
            throw new FileNotFoundException("Could not load the io.openliberty.smallrye.metrics JAR");
        }
        return f;
    }

    private File resolveMicrometerJar() throws FileNotFoundException {
        ContentBasedLocalBundleRepository cblb = BundleRepositoryRegistry.getInstallBundleRepository();
        VersionRange vr = VersionRange.valueOf(VersionRange.LEFT_CLOSED + "1,5.0.100" + VersionRange.RIGHT_CLOSED);
        /*
         * Sometimes.. the first call returns a null file. Debug shows that the class
         * loader was unable to load the ContentBasedLocalBundleRepository
         */
        cblb.selectBundle("lib/", "io.openliberty.micrometer", vr);
        File f = cblb.selectBundle("lib/", "io.openliberty.micrometer", vr);
        if (f == null) {
            throw new FileNotFoundException("Could not load the io.openliberty.micrometer JAR");
        }
        return f;
    }

    private ClassLoader createBundleAddOnClassLoader(List<File> classPath, boolean isSharedLibAvailable)
            throws IllegalStateException {
        ClassLoader retClassLoader = null;
        ClassLoader classCL = this.getClass().getClassLoader();

        /*
         * Create ClassLoaderIdentity
         */
        ClassLoaderConfiguration clConfig = classLoadingService.createClassLoaderConfiguration();
        ClassLoaderIdentity cid = classLoadingService.createIdentity("smallrye", "metrics");
        clConfig.setId(cid);

        /*
         * Attaches shared library to bundle add-on classloader
         */
        if (isSharedLibAvailable) {
            clConfig.addSharedLibraries(sharedLib.id());
        }

        retClassLoader = classLoadingService.createBundleAddOnClassLoader(classPath, classCL, clConfig);
        if (retClassLoader == null) {
            throw new IllegalStateException("MpMetrics was unable to create the requisite BundleAddOnClassLoader");
        }
        return retClassLoader;
    }

    private void loadSmallRyeClasses(ClassLoader classLoader) throws ClassNotFoundException, NoClassDefFoundError {

        /*
         * Loading SmallRye Metric CDI related classes
         */

        Util.SR_LEGACY_METRIC_REGISTRY_EXTENSION_CLASS = Class
                .forName("io.smallrye.metrics.legacyapi.LegacyMetricsExtension", true, classLoader);

        Class<?> metricRegistryProducerClass = Class.forName("io.smallrye.metrics.MetricRegistryProducer", true,
                classLoader);
        Util.SR_METRIC_REGISTRY_PRODUCER_CLASS = metricRegistryProducerClass;

        Class<?> metricProducerClass = Class.forName("io.smallrye.metrics.MetricProducer", true, classLoader);
        Util.SR_METRICS_PRODUCER_CLASS = metricProducerClass;

        /*
         * Loading the other SmallRye Metric classes
         */
        Class<?> sharedMetricRegistriesClass = Class.forName("io.smallrye.metrics.SharedMetricRegistries", true,
                classLoader);
        Util.SR_SHARED_METRIC_REGISTRIES_CLASS = sharedMetricRegistriesClass;

        Class<?> LegacyMetricRegistryClass = Class.forName("io.smallrye.metrics.legacyapi.LegacyMetricRegistryAdapter",
                true, classLoader);
        Util.SR_LEGACY_METRIC_REGISTRY_CLASS = LegacyMetricRegistryClass;

        Class<?> MetricRequestHandlerClass = Class.forName("io.smallrye.metrics.MetricsRequestHandler", true,
                classLoader);
        Util.SR_METRICS_REQUEST_HANDLER_CLASS = MetricRequestHandlerClass;

        Class<?> restResponderInterface = Class.forName("io.smallrye.metrics.MetricsRequestHandler$Responder", true,
                classLoader);
        Util.SR_REST_RESPONDER_INTERFACE = restResponderInterface;

        Class<?> ApplicationNameResolverFuncInterface = Class
                .forName("io.smallrye.metrics.setup.ApplicationNameResolver", true, classLoader);
        Util.SR_APPLICATION_NAME_RESOLVER_INTERFACE = ApplicationNameResolverFuncInterface;

    }
}
