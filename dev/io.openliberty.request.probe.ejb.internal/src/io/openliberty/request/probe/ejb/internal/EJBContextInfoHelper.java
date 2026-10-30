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

import java.util.Objects;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ws.ejbcontainer.EJBRequestData;
import com.ibm.ws.request.probe.bci.internal.RequestProbeConstants;
import com.ibm.wsspi.request.probe.bci.ContextInfoHelper;
import com.ibm.wsspi.requestContext.ContextInfoArray;

/**
 * Extracts EJB identity context from an {@link EJBRequestData} instance for all
 * four EJB timing event types (timer, async, remote, mdb).
 *
 * <p>The {@code thisInstance} passed by BCI (or by {@link EJBRequestTimingCollaboratorImpl})
 * is the {@link EJBRequestData} / {@code EJSDeployedSupport} for the current invocation.
 *
 * <p>Context info array layout:
 * <ol>
 * <li>[0] application name — logical EAR name, or module name for standalone modules</li>
 * <li>[1] module name — EJB module logical name (JAR name without extension)</li>
 * <li>[2] bean name — EJB component name</li>
 * <li>[3] method name — method name</li>
 * </ol>
 *
 * <p>For standalone modules (no EAR), {@code array[0]} equals {@code array[1]} because
 * the module name is used in both slots. The {@link #toString()} representation
 * suppresses the duplicate slot only in that case. This behavior is consistent with
 * the EJB specification requirements for java:global bindings.
 */
public class EJBContextInfoHelper extends ContextInfoHelper implements ContextInfoArray {

    private String[] contextInfoArray;
    private boolean standaloneModule;

    EJBContextInfoHelper(Object thisInstance) {
        super(thisInstance, null);
    }

    @Override
    public String[] getContextInfoArray() {
        if (contextInfoArray == null) {
            String appName = "";
            String moduleName = "";
            String beanName = "";
            String methodName = "";

            Object instance = getInstanceOfThisClass();
            if (instance instanceof EJBRequestData) {
                EJBRequestData request = (EJBRequestData) instance;
                EJBMethodInfoImpl methodInfo = (EJBMethodInfoImpl) request.getEJBMethodMetaData();
                if (methodInfo != null) {
                    methodName = Objects.toString(methodInfo.getMethodName(), "");

                    BeanMetaData bmd = methodInfo.getBeanMetaData();
                    if (bmd != null) {
                        beanName = Objects.toString(bmd.getName(), "");
                        moduleName = Objects.toString(bmd.getModuleLogicalName(), "");

                        String logicalAppName = bmd.getApplicationLogicalName();
                        if (logicalAppName != null) {
                            appName = logicalAppName;
                            standaloneModule = false;
                        } else {
                            // Standalone module — no EAR; use module name in the app slot
                            // so config filters based on appName="*" still match.
                            appName = moduleName;
                            standaloneModule = true;
                        }
                    }
                }
            }

            contextInfoArray = new String[] { appName, moduleName, beanName, methodName };
        }
        return contextInfoArray;
    }

    @Override
    public String toString() {
        String[] a = getContextInfoArray();
        // For standalone modules the app and module slots are identical; skip the
        // first slot to avoid redundant output (e.g. "MyModule | MyModule | MyBean | myMethod").
        // For EAR-deployed modules both slots are meaningful even if the names happen
        // to be the same, so we always start from index 0 in that case. This behavior
        // is consistent with the EJB specification requirements for java:global bindings.
        int start = standaloneModule ? 1 : 0;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < a.length; i++) {
            if (a[i] != null && !a[i].isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(RequestProbeConstants.EVENT_CONTEXT_INFO_SEPARATOR);
                }
                sb.append(a[i]);
            }
        }
        return sb.toString();
    }
}
