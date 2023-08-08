/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.restfulws.mpmetrics;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs.defaultexceptionmapper.DefaultExceptionMapperCallback;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.microprofile.metrics50.SharedMetricRegistries;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;

import jakarta.ws.rs.container.ResourceInfo;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(service = { DefaultExceptionMapperCallback.class }, configurationPolicy = ConfigurationPolicy.IGNORE)
public class MetricsRestfulWsEMCallbackImpl implements DefaultExceptionMapperCallback {

    static SharedMetricRegistries sharedMetricRegistry;

    private static final TraceComponent tc = Tr.register(MetricsRestfulWsEMCallbackImpl.class);

    public static final String EXCEPTION_KEY = MetricsRestfulWsEMCallbackImpl.class.getName() + ".Exception";

    private static final String[] metricCDIBundles = { "io.openliberty.smallrye.metrics.cdi.adapters",
            "io.smallrye.metrics.legacyapi.interceptors", "io.smallrye.metrics" };

    public synchronized static Counter registerOrRetrieveRESTUnmappedExceptionMetric(String fullyQualifiedClassName,
            String methodSignature) {

        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();

        return registerOrRetrieveRESTUnmappedExceptionMetric(fullyQualifiedClassName, methodSignature,
                cmd.getJ2EEName().getApplication());
    }

    public synchronized static Counter registerOrRetrieveRESTUnmappedExceptionMetric(String fullyQualifiedClassName,
            String methodSignature, String appName) {
        MetricRegistry baseMetricRegistry = sharedMetricRegistry.getOrCreate(MetricRegistry.BASE_SCOPE);

        Metadata metadata = Metadata.builder().withName("REST.request.unmappedException.total")
                .withDescription("REST.request.unmappedException.description").build();

        Tag classTag = new Tag("class", fullyQualifiedClassName);
        Tag methodTag = new Tag("method", methodSignature);

        Counter counter = baseMetricRegistry.counter(metadata, classTag, methodTag);

        sharedMetricRegistry.associateMetricIDToApplication(new MetricID(metadata.getName(), classTag, methodTag),
                appName, baseMetricRegistry);

        return counter;
    }

    @Override
    public Map<String, Object> onDefaultMappedException(Throwable throwable, int statusCode,
            ResourceInfo resourceInfo) {
        StackTraceElement[] ste = throwable.getStackTrace();

        /*
         * If the Exception originates from the Metrics CDI Bundle we do not want to
         * count the exception.
         * 
         * Validate by checking the first element on the stack to see if it came from
         * the two packages in the Metrics CDI bundle that throws Exceptions.
         */
        if (!(ste[0].getClassName().startsWith(metricCDIBundles[0])
                || ste[0].getClassName().startsWith(metricCDIBundles[1]))) {
            Map.Entry<String, String> classXmethod = resolveClassMethodTags(resourceInfo);
            if (classXmethod != null) {
                // increment exception count
                registerOrRetrieveRESTUnmappedExceptionMetric(classXmethod.getKey(), classXmethod.getValue()).inc();
            }
        }

        // Todo: update reference to message
        Tr.warning(tc, "METRICS_UNHANDLED_RESTFULWS_EXCEPTION", throwable);

        return Collections.singletonMap(EXCEPTION_KEY, throwable);
    }

    @Reference
    public void getSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistry) {
        MetricsRestfulWsEMCallbackImpl.sharedMetricRegistry = sharedMetricRegistry;
    }

    /**
     * This method resolves the fully qualified class name and method signature
     * 
     * @param resourceInfo ResourceInfo obj which contains data regarding class and
     *                     method
     * @return Map.Entry<String, String> Fully qualified class name as the key and
     *         the method as value
     */
    private Map.Entry<String, String> resolveClassMethodTags(ResourceInfo resourceInfo) {
        Class<?> resourceClass = resourceInfo.getResourceClass();

        String fullyQualifiedClassName = null;
        String fullMethodSignature = null;

        if (resourceClass != null) {
            fullyQualifiedClassName = resourceClass.getName();
            Method resourceMethod = resourceInfo.getResourceMethod();

            Class<?>[] parameterClasses = resourceMethod.getParameterTypes();
            String parameter;
            fullMethodSignature = resourceMethod.getName();

            for (Class<?> p : parameterClasses) {
                parameter = p.getCanonicalName();
                fullMethodSignature = fullMethodSignature + "_" + parameter;
            }
        }

        if (fullMethodSignature == null || fullyQualifiedClassName == null || fullMethodSignature.isEmpty()
                || fullyQualifiedClassName.isEmpty()) {
            if (tc.isAnyTracingEnabled()) {
                Tr.debug(tc, "Could not resolve the class name or method signature class:[" + fullMethodSignature
                        + "] method:[" + fullMethodSignature + "]");
            }
            return null;
        }
        return new AbstractMap.SimpleEntry<String, String>(fullyQualifiedClassName, fullMethodSignature);
    }

}
