/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.request.probe.ejb.internal;

import org.osgi.service.component.annotations.Component;

import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBMethodInterface;
import com.ibm.ws.ejbcontainer.EJBRequestData;
import com.ibm.ws.ejbcontainer.EJBRequestTimingCollaborator;
import com.ibm.wsspi.request.probe.bci.TransformDescriptorHelper;

/**
 * OSGi DS component that implements {@link EJBRequestTimingCollaborator} and drives
 * requestTiming probe events for all four EJB request types.
 *
 * <p>On {@code preInvoke}, classifies the request type, then calls
 * {@link TransformDescriptorHelper#entryHelper} in the same way that BCI-injected
 * bytecode would — using the sentinel descriptor's class name, method name, method
 * descriptor, event type, and TD key. The {@code EJBRequestData} is passed as
 * {@code thisInstance} so that {@link EJBContextInfoHelper} can extract bean identity.
 * On {@code postInvoke}, calls {@link TransformDescriptorHelper#exitHelper} using
 * the {@code TransformDescriptorHelper} instance returned as the preInvoke cookie,
 * mirroring the BCI exit injection.
 *
 * <p>EJB type discrimination (async checked first, taking priority over interface type):
 * <ul>
 * <li>async ({@code isAsynchMethod()}) — any interface → {@code websphere.ejb.async}</li>
 * <li>{@code MESSAGE_ENDPOINT} → {@code websphere.ejb.mdb}</li>
 * <li>{@code REMOTE} → {@code websphere.ejb.remote}</li>
 * <li>{@code TIMER} → {@code websphere.ejb.timer}</li>
 * </ul>
 * All other interface types (LOCAL non-async, LOCAL_HOME, HOME, SERVICE_ENDPOINT)
 * are not timed and {@code preInvoke} returns {@code null}.
 */
@Component(service = EJBRequestTimingCollaborator.class,
           property = "service.vendor=IBM")
public class EJBRequestTimingCollaboratorImpl implements EJBRequestTimingCollaborator<TransformDescriptorHelper> {

    private static final TraceComponent tc = Tr.register(EJBRequestTimingCollaboratorImpl.class);

    // Descriptor arrays: { className, methodName, methodDesc, eventType, tdKey }.
    // Shared and read-only; avoids allocating a new array per request.
    private static final String[] ASYNC_DESCRIPTOR = {
                                                       EJBAsyncTransformDescriptor.CLASS_NAME,
                                                       EJBAsyncTransformDescriptor.METHOD_NAME,
                                                       EJBAsyncTransformDescriptor.METHOD_DESC,
                                                       EJBAsyncTransformDescriptor.EVENT_TYPE,
                                                       EJBAsyncTransformDescriptor.TD_KEY
    };

    private static final String[] MDB_DESCRIPTOR = {
                                                     EJBMessageEndpointTransformDescriptor.CLASS_NAME,
                                                     EJBMessageEndpointTransformDescriptor.METHOD_NAME,
                                                     EJBMessageEndpointTransformDescriptor.METHOD_DESC,
                                                     EJBMessageEndpointTransformDescriptor.EVENT_TYPE,
                                                     EJBMessageEndpointTransformDescriptor.TD_KEY
    };

    private static final String[] REMOTE_DESCRIPTOR = {
                                                        EJBRemoteTransformDescriptor.CLASS_NAME,
                                                        EJBRemoteTransformDescriptor.METHOD_NAME,
                                                        EJBRemoteTransformDescriptor.METHOD_DESC,
                                                        EJBRemoteTransformDescriptor.EVENT_TYPE,
                                                        EJBRemoteTransformDescriptor.TD_KEY
    };

    private static final String[] TIMER_DESCRIPTOR = {
                                                       EJBTimerTransformDescriptor.CLASS_NAME,
                                                       EJBTimerTransformDescriptor.METHOD_NAME,
                                                       EJBTimerTransformDescriptor.METHOD_DESC,
                                                       EJBTimerTransformDescriptor.EVENT_TYPE,
                                                       EJBTimerTransformDescriptor.TD_KEY
    };

    @Override
    public TransformDescriptorHelper preInvoke(EJBRequestData request) throws Exception {
        // Classify the request — mirrors the BCI transformer choosing which descriptor
        // to instrument based on the registered transform descriptor map.
        String[] descriptor = selectDescriptor(request);
        if (descriptor == null) {
            return null;
        }

        // descriptor = { className, methodName, methodDesc, eventType, tdKey }
        // This mirrors exactly what BCI would inject at method entry:
        //   TransformDescriptorHelper tdh = new TransformDescriptorHelper();
        //   tdh.entryHelper(className, methodName, methodDesc, eventType, tdKey, this, args);
        TransformDescriptorHelper tdh = new TransformDescriptorHelper();
        tdh.entryHelper(descriptor[0], descriptor[1], descriptor[2],
                        descriptor[3], descriptor[4],
                        request, null);
        return tdh;
    }

    @Override
    public void postInvoke(EJBRequestData request, TransformDescriptorHelper tdh) throws Exception {
        // Mirrors BCI exit injection:
        //   tdh.exitHelper();
        if (tdh != null) {
            tdh.exitHelper();
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Returns a five-element string array {@code {className, methodName, methodDesc,
     * eventType, tdKey}} identifying the sentinel descriptor for this request, or
     * {@code null} if this request type is not timed.
     *
     * <p>Async is checked first so that an async remote invocation is classified
     * as async rather than remote.
     */
    private static String[] selectDescriptor(EJBRequestData request) {
        EJBMethodInfoImpl methodInfo = (EJBMethodInfoImpl) request.getEJBMethodMetaData();
        if (methodInfo == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "selectDescriptor: no EJB method metadata; request not timed");
            }
            return null;
        }

        // Async takes priority over interface type
        if (methodInfo.isAsynchMethod()) {
            return ASYNC_DESCRIPTOR;
        }

        EJBMethodInterface methodInterface = methodInfo.getEJBMethodInterface();
        if (methodInterface == EJBMethodInterface.MESSAGE_ENDPOINT) {
            return MDB_DESCRIPTOR;
        } else if (methodInterface == EJBMethodInterface.REMOTE) {
            return REMOTE_DESCRIPTOR;
        } else if (methodInterface == EJBMethodInterface.TIMER) {
            return TIMER_DESCRIPTOR;
        }

        // Other EJB method types are in the scope of other requests and not timed
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "selectDescriptor: request not timed; method interface = "
                         + methodInterface
                         + ", bean = " + methodInfo.getBeanClassName()
                         + ", method = " + methodInfo.getMethodName());
        }
        return null;
    }
}
