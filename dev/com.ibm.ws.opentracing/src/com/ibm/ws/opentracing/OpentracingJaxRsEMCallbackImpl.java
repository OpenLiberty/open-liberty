/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.opentracing;

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.container.ResourceInfo;

import com.ibm.ws.jaxrs.defaultexceptionmapper.DefaultExceptionMapperCallback;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

/**
 * <p>Container filter implementation.</p>
 *
 * <p>This implementation is stateless. A single container filter is used by all applications.</p> *
 */
@Component(service = { DefaultExceptionMapperCallback.class }, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class OpentracingJaxRsEMCallbackImpl implements DefaultExceptionMapperCallback {
    private static final TraceComponent tc = Tr.register(OpentracingJaxRsEMCallbackImpl.class);
    
    public static final String EXCEPTION_KEY = OpentracingJaxRsEMCallbackImpl.class.getName() + ".Exception";

    public OpentracingJaxRsEMCallbackImpl() {
    }

    @Override
    public Map<String,Object> onDefaultMappedException(Throwable t, int statusCode, ResourceInfo resourceInfo ) {
    	if (!t.getMessage().contains("HTTP 404 Not Found"))
	        Tr.warning(tc, "OPENTRACING_UNHANDLED_JAXRS_EXCEPTION", t);
        return Collections.singletonMap(EXCEPTION_KEY, t);
    }

}
