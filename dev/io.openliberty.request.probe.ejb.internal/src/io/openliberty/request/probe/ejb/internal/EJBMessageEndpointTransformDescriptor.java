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

import com.ibm.wsspi.request.probe.bci.RequestProbeTransformDescriptor;

/**
 * Sentinel {@link RequestProbeTransformDescriptor} for Message-Driven Bean (MDB) invocations.
 *
 * <p>The class and method names are synthetic keys that will never match any
 * real loaded class. The descriptor is registered as an OSGi service purely
 * so that {@code TransformDescriptorHelper.getObjForInstrumentation()} can
 * look it up by its key, allowing {@link EJBRequestTimingCollaboratorImpl} to
 * call {@code entryHelper}/{@code exitHelper} manually without BCI injection.
 */
@Component(service = RequestProbeTransformDescriptor.class,
           property = "service.vendor=IBM")
public class EJBMessageEndpointTransformDescriptor implements RequestProbeTransformDescriptor {

    /** Event type string for MDB invocations. */
    public static final String EVENT_TYPE = "websphere.ejb.mdb";

    /** Sentinel key components — never matched by the BCI class transformer. */
    static final String CLASS_NAME = "com/ibm/ws/ejbcontainer/requesttiming/mdb";
    static final String METHOD_NAME = "invoke";
    static final String METHOD_DESC = "()V";

    /** Lookup key used by {@code TransformDescriptorHelper.getObjForInstrumentation()}. */
    public static final String TD_KEY = CLASS_NAME + METHOD_NAME + METHOD_DESC;

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    public String getMethodDesc() {
        return METHOD_DESC;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Override
    public boolean isCounter() {
        return false;
    }

    @Override
    public Object getContextInfo(Object thisInstance, Object methodArgs) {
        return new EJBContextInfoHelper(thisInstance);
    }
}
