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
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.config.ConfigProvider;
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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
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

    private static final String SMALLRYE_METRICS_JAR_NAME = "io.openliberty.io.smallrye.metrics";
    private static final String SMALLRYE_METRICS_JAR_VERSION_RANGE = "[1.0,5.0)";
    private static final String MICROMETER_JAR_NAME = "io.openliberty.io.micrometer";
    private static final String MICROMETER_JAR_VERSION_RANGE = "[1.0,5.0)";

    private static final String LEGACY_METRICS_EXTENSION_CLASSNAME = "io.smallrye.metrics.legacyapi.LegacyMetricsExtension";
    private static final String LEGACY_METRIC_REGISTRY_ADAPTER_CLASSNAME = "io.smallrye.metrics.legacyapi.LegacyMetricRegistryAdapter";
    private static final String METRIC_REGISTRY_PRODUCER_CLASSNAME = "io.smallrye.metrics.MetricRegistryProducer";
    private static final String METRIC_PRODUCER_CLASSNAME = "io.smallrye.metrics.MetricProducer";
    private static final String SHARED_METRIC_REGISTRIES_CLASSNAME = "io.smallrye.metrics.SharedMetricRegistries";
    private static final String METRICS_REQUEST_HANDLER_CLASSNAME = "io.smallrye.metrics.MetricsRequestHandler";
    private static final String METRICS_REQUEST_HANDELER_RESPONDER_CLASSNAME = METRICS_REQUEST_HANDLER_CLASSNAME
            + "$Responder";
    private static final String APPLICATION_NAME_RESOLVER_CLASSNAME = "io.smallrye.metrics.setup.ApplicationNameResolver";

    private static final String FQ_PROMETHEUSCONFIG_PATH = "io.micrometer.prometheus.PrometheusConfig";

    private ClassLoadingService classLoadingService;
    private volatile Library sharedLib = null;
    private static boolean isSuccesfulActivation = true;
    private static AtomicBoolean isActivated = new AtomicBoolean();
    private MetricsConfig metricsConfig;

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

        if (isActivated.get())
            return;

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
        isActivated.set(true);
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

    @Reference(name = "metricsConfig", service = MetricsConfig.class)
    protected void setMetricConfig(MetricsConfig ref) throws Exception {
        metricsConfig = ref;
    }

    protected void unsetMetricConfig() {
        metricsConfig = null;
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
        File f = resolveLibJar(SMALLRYE_METRICS_JAR_NAME, SMALLRYE_METRICS_JAR_VERSION_RANGE);
        return f;
    }

    private File resolveMicrometerJar() throws FileNotFoundException {
        File f = resolveLibJar(MICROMETER_JAR_NAME, MICROMETER_JAR_VERSION_RANGE);
        return f;
    }

    private File resolveLibJar(String jarName, String jarVersionRange) throws FileNotFoundException {
        ContentBasedLocalBundleRepository cblb = BundleRepositoryRegistry.getInstallBundleRepository();
        VersionRange vr = VersionRange.valueOf(jarVersionRange);

        File f = cblb.selectBundle("lib/", jarName, vr);
        if (f == null) {
            throw new FileNotFoundException("Could not load the " + jarName + " jar");
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
        clConfig.setProtectionDomain(new ProtectionDomain(new CodeSource(null, new Certificate[] {}),
                this.getClass().getProtectionDomain().getPermissions()));
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

        checkPrometheusRegistryAvailable(classLoader);

        /*
         * Loading SmallRye Metric CDI related classes
         */

        Util.SR_LEGACY_METRIC_REGISTRY_EXTENSION_CLASS = Class.forName(LEGACY_METRICS_EXTENSION_CLASSNAME, true,
                classLoader);

        Class<?> metricRegistryProducerClass = Class.forName(METRIC_REGISTRY_PRODUCER_CLASSNAME, true, classLoader);
        Util.SR_METRIC_REGISTRY_PRODUCER_CLASS = metricRegistryProducerClass;

        Class<?> metricProducerClass = Class.forName(METRIC_PRODUCER_CLASSNAME, true, classLoader);
        Util.SR_METRICS_PRODUCER_CLASS = metricProducerClass;

        /*
         * Loading the other SmallRye Metric classes
         */
        Class<?> sharedMetricRegistriesClass = Class.forName(SHARED_METRIC_REGISTRIES_CLASSNAME, true, classLoader);
        Util.SR_SHARED_METRIC_REGISTRIES_CLASS = sharedMetricRegistriesClass;

        Class<?> LegacyMetricRegistryClass = Class.forName(LEGACY_METRIC_REGISTRY_ADAPTER_CLASSNAME, true, classLoader);
        Util.SR_LEGACY_METRIC_REGISTRY_CLASS = LegacyMetricRegistryClass;

        Class<?> MetricRequestHandlerClass = Class.forName(METRICS_REQUEST_HANDLER_CLASSNAME, true, classLoader);
        Util.SR_METRICS_REQUEST_HANDLER_CLASS = MetricRequestHandlerClass;

        Class<?> restResponderInterface = Class.forName(METRICS_REQUEST_HANDELER_RESPONDER_CLASSNAME, true,
                classLoader);
        Util.SR_REST_RESPONDER_INTERFACE = restResponderInterface;

        Class<?> ApplicationNameResolverFuncInterface = Class.forName(APPLICATION_NAME_RESOLVER_CLASSNAME, true,
                classLoader);
        Util.SR_APPLICATION_NAME_RESOLVER_INTERFACE = ApplicationNameResolverFuncInterface;

    }

    @FFDCIgnore(ClassNotFoundException.class)
    private void checkPrometheusRegistryAvailable(ClassLoader classLoader) {
        // Check if Prometheus Meter Registry is in class path
        try {
            Class.forName(FQ_PROMETHEUSCONFIG_PATH, false, classLoader);
            if (!Boolean.parseBoolean(ConfigProvider.getConfig()
                    .getOptionalValue("mp.metrics.prometheus.enabled", String.class).orElse("true"))) {
                metricsConfig.disableMetricsEndpoint();
            }
        } catch (ClassNotFoundException e) {
            metricsConfig.disableMetricsEndpoint();
        } catch (Exception e) {
            metricsConfig.disableMetricsEndpoint();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected exception encountered " + e);
            }
        }
    }
}
