/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.opentracing;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.opentracing.OpentracingFilterHelper;

/**
 * <p>OpenTracing client filter implementation. Handles outgoing requests and
 * incoming responses. (Contract with the server filter, which handles incoming
 * requests and outgoing responses.)</p>
 *
 * <p>Implements both {@link ClientRequestFilter} and {@link ClientResponseFilter}.</p>
 *
 * <p>This implementation is stateless. A single client filter is used by all clients.</p>
 */
@Component(immediate = true)
public class OpenTracingFilterHelper implements OpentracingFilterHelper {
    private static final TraceComponent tc = Tr.register(OpenTracingFilterHelper.class);

    //
    @Override
    public String getBuildSpanName(ClientRequestContext clientRequestContext) {
        // "The default operation name of the new Span for the outgoing request is
        // <HTTP method>"
        // https://github.com/eclipse/microprofile-opentracing/blob/master/spec/src/main/asciidoc/microprofile-opentracing.asciidoc#client-span-name
        return clientRequestContext.getMethod();
    }

    //
    @Override
    public String getBuildSpanName(ContainerRequestContext incomingRequestContext, ResourceInfo resourceInfo) {
        String methodName = "getBuildSpanName";

        String methodOperationName = OpenTracingService.getMethodOperationName(resourceInfo.getResourceMethod());
        String classOperationName = OpenTracingService.getClassOperationName(resourceInfo.getResourceMethod());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " operation names", classOperationName, methodOperationName);
        }

        // Check if this JAXRS method has @Traced(false)
        if (OpenTracingService.isNotTraced(classOperationName, methodOperationName)) {
            return null;
        }

        // If there's no Traced annotation (operationName is null) or there is a Traced annotation
        // with value=true but a default operationName, then set it to the default based on the URI.

        String operationName;

        if (OpenTracingService.hasExplicitOperationName(methodOperationName)) {
            operationName = methodOperationName;
        } else {
            // "The default operation name of the new Span for the incoming request is
            // <HTTP method>:<package name>.<class name>.<method name> [...]
            // If operationName is specified on a class, that operationName will be used
            // for all methods of the class unless a method explicitly overrides it with
            // its own operationName."
            // https://github.com/eclipse/microprofile-opentracing/blob/master/spec/src/main/asciidoc/microprofile-opentracing.asciidoc#server-span-name

            if (OpenTracingService.hasExplicitOperationName(classOperationName)) {
                operationName = classOperationName;
            } else {
                operationName = incomingRequestContext.getMethod() + ":"
                                + resourceInfo.getResourceClass().getName() + "."
                                + resourceInfo.getResourceMethod().getName();
            }
        }

        return operationName;
    }
}
