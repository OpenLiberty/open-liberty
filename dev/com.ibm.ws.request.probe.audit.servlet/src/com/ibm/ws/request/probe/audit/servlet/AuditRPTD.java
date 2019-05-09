/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.probe.audit.servlet;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.request.probe.bci.RequestProbeTransformDescriptor;
import com.ibm.wsspi.request.probe.bci.TransformDescriptorHelper;

/**
 *
 */
@Component(service = { RequestProbeTransformDescriptor.class },
                name = "com.ibm.ws.security.audit.rptd",
                configurationPolicy = ConfigurationPolicy.IGNORE,
                property = "service.vendor=IBM",
                immediate = true)
public class AuditRPTD implements RequestProbeTransformDescriptor {

    TransformDescriptorHelper bob;

    @Activate
    protected void activate(ComponentContext cc) {}

    @Deactivate
    protected void deactivate(ComponentContext cc) {}

	private static final TraceComponent tc = Tr.register(AuditRPTD.class, "requestProbe",
			"com.ibm.ws.request.probe.internal.resources.LoggingMessages");

	private static final String classToInstrument = "com/ibm/ws/security/audit/Audit";
	private static final String methodToInstrument = "audit";
	private static final String descOfMethod = "(Lcom/ibm/ws/security/audit/Audit$EventID;[Ljava/lang/Object;)V";
    private static final String requestProbeType = "websphere.security.audit.test";

    /** {@inheritDoc} */
    @Override
    public boolean isCounter() {
        return true;
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
    	return methodArgs;
//    	ArrayList contextObjects = new ArrayList<Object>(2);
//        if(methodArgs!=null){
//            Object[] params = null;
//            if (Object[].class.isInstance(methodArgs)) {
//                params = (Object[]) methodArgs;
//                //Look at First Argument which would be "SQL" here.
//                contextObjects.add(params[3]);
//                contextObjects.add(params[4]);
//            }           
//        }
    }
}
