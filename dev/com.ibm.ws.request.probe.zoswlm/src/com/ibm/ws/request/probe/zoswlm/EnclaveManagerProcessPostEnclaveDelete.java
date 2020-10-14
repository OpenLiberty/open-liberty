/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.probe.zoswlm;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.wsspi.request.probe.bci.RequestProbeTransformDescriptor;

/**
 * Prove descriptor implementation associated to z/OS WLM's EnclaveManager.
 */
@Component(service = { RequestProbeTransformDescriptor.class }, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class EnclaveManagerProcessPostEnclaveDelete implements RequestProbeTransformDescriptor {

    private static final String classToInstrument = "com/ibm/ws/zos/wlm/internal/EnclaveManagerImpl";
    private static final String methodToInstrument = "processPostEnclaveDelete";
    private static final String descOfMethod = "(Ljava/util/HashMap;)V";
    private static final String requestProbeType = "websphere.wlm.EnclaveManagerImpl.processPostEnclaveDelete";

    /** {@inheritDoc} */
    @Override
    public boolean isCounter() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getClassName() {
        return classToInstrument;
    }

    /** {@inheritDoc} */
    @Override
    public String getMethodName() {
        return methodToInstrument;
    }

    /** {@inheritDoc} */
    @Override
    public String getMethodDesc() {
        return descOfMethod;
    }

    /** {@inheritDoc} */
    @Override
    public String getEventType() {
        return requestProbeType;
    }

    /** {@inheritDoc} */
    @Override
    public Object getContextInfo(Object thisInstance, Object methodArgs) {
        Object[] obj = (Object[]) methodArgs;
        return obj[0];
    }
}
