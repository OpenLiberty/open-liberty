/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.jmx.internal;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.RequiredModelMBean;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class CXFModelMBean extends RequiredModelMBean {
    private static final TraceComponent tc = Tr.register(CXFModelMBean.class);

    private static final String UNSUPPORTED_OPERATION_BUS_SHUTDOWN = "shutdown";
    private static final String UNSUPPORTED_OPERATION_WORK_QUEUE_MANAGER_SHUTDOWN = "shutdown";
    private static final String UNSUPPORTED_OPERATION_ENDPOINT_DESTROY = "destroy";

    private Object managedResourceLocal = null;

    /**
     * @throws MBeanException
     * @throws RuntimeOperationsException
     */
    public CXFModelMBean() throws MBeanException, RuntimeOperationsException {
        super();
    }

    @Override
    public void setManagedResource(Object mr, String mr_type)
                    throws MBeanException, RuntimeOperationsException,
                    InstanceNotFoundException, InvalidTargetObjectTypeException {
        super.setManagedResource(mr, mr_type);

        // Need to record this reference so that later we can know which kind of 
        // managed resource is being invoked. Cannot access super class for this
        // private attribute, so has to record locally.
        managedResourceLocal = mr;
    }

    @Override
    public Object invoke(String opName, Object[] opArgs, String[] sig)
                    throws MBeanException, ReflectionException {

        // For Endpoint MBean, the operation "destory" should not be allowed
        // since it will cause shutdown of liberty application.
        if ((managedResourceLocal != null) && (managedResourceLocal instanceof org.apache.cxf.endpoint.ManagedEndpoint)
            && (opName.equalsIgnoreCase(UNSUPPORTED_OPERATION_ENDPOINT_DESTROY)))
        {
            throw new MBeanException(null, Tr.formatMessage(tc, "err.unsupported.jmx.operation", opName));
        }

        // For Bus MBean, the operations "shutdown" should not be allowed also
        if ((managedResourceLocal != null) && (managedResourceLocal instanceof org.apache.cxf.bus.ManagedBus)
            && (opName.equalsIgnoreCase(UNSUPPORTED_OPERATION_BUS_SHUTDOWN)))
        {
            throw new MBeanException(null, Tr.formatMessage(tc, "err.unsupported.jmx.operation", opName));
        }

        // For Work Queue Manager MBean, the operations "shutdown" should not be allowed also
        if ((managedResourceLocal != null) && (managedResourceLocal instanceof org.apache.cxf.bus.managers.WorkQueueManagerImplMBeanWrapper)
            && (opName.equalsIgnoreCase(UNSUPPORTED_OPERATION_WORK_QUEUE_MANAGER_SHUTDOWN)))
        {
            throw new MBeanException(null, Tr.formatMessage(tc, "err.unsupported.jmx.operation", opName));
        }

        return super.invoke(opName, opArgs, sig);
    }
}
