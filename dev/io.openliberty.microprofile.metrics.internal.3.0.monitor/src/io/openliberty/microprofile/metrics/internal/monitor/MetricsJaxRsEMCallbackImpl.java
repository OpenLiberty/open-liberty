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
package io.openliberty.microprofile.metrics.internal.monitor;

import com.ibm.ws.jaxrs.defaultexceptionmapper.DefaultExceptionMapperCallback;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.container.ResourceInfo;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(service = {
		DefaultExceptionMapperCallback.class }, configurationPolicy = ConfigurationPolicy.IGNORE, property = {
				"service.vendor=IBM" })
public class MetricsJaxRsEMCallbackImpl  implements DefaultExceptionMapperCallback {

	
	public static final String EXCEPTION_KEY = MetricsJaxRsEMCallbackImpl.class.getName() + ".Exception";
	
	
	public synchronized static Counter registerOrRetrieveRESTUnmappedExceptionMetric(String fullyQualifiedClassName, String methodSignature) {
		MetricRegistry baseMetricRegistry = sharedMetricRegistry.getOrCreate(MetricRegistry.Type.BASE.getName());
		
		Metadata metadata = Metadata.builder().withType(MetricType.COUNTER).withName("REST.request.unmappedException.total").withDescription("REST.request.unmappedException.description").withDisplayName("Total Unmapped Exception Requests").build();
		
		Tag classTag = new Tag("class", fullyQualifiedClassName);
		Tag methodTag = new Tag("method", methodSignature);
		
		Counter counter = baseMetricRegistry.counter(metadata, classTag, methodTag);
		
		ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
		
		sharedMetricRegistry.associateMetricIDToApplication(new MetricID(metadata.getName(), classTag, methodTag), cmd.getJ2EEName().getApplication(), baseMetricRegistry);
		
		return counter;
	}
	
	@Override
	public Map<String, Object> onDefaultMappedException(Throwable t, int statusCode, ResourceInfo resourceInfo) {
		Map.Entry<String, String> classXmethod = resolveSimpleTimerClassMethodTags(resourceInfo);

		registerOrRetrieveRESTUnmappedExceptionMetric(classXmethod.getKey() ,classXmethod.getValue()).inc();
		
		return Collections.singletonMap(EXCEPTION_KEY, t);
	}

	static SharedMetricRegistries sharedMetricRegistry;
	
	@Reference
	public void getSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistry) {
		MetricsJaxRsEMCallbackImpl.sharedMetricRegistry = sharedMetricRegistry;
	}
	
	
	private Map.Entry<String, String> resolveSimpleTimerClassMethodTags(ResourceInfo resourceInfo) {
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
			throw new IllegalStateException("The following values are either null or empty - fullyQualifiedClassName: "
					+ fullyQualifiedClassName + " fullMethodSignature: " + fullMethodSignature);
		}

		return new AbstractMap.SimpleEntry<String, String>(fullyQualifiedClassName, fullMethodSignature);

	}

}
