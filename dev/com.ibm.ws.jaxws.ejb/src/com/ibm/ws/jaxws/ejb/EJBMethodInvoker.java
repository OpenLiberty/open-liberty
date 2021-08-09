/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.ejb;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import javax.xml.ws.handler.MessageContext.Scope;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.AbstractJAXWSMethodInvoker;
import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Exchange;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxws.ejb.internal.JaxWsEJBConstants;
import com.ibm.wsspi.ejbcontainer.WSEJBEndpointManager;

/**
 *
 */
public class EJBMethodInvoker extends AbstractJAXWSMethodInvoker {

    public EJBMethodInvoker() {
        super(null);
    }

    @Override
    @FFDCIgnore(Fault.class)
    protected Object invoke(Exchange exchange, Object serviceObject, Method method, List<Object> params) {
        // retrieve the ejb instance
        Object instance = exchange.get(JaxWsEJBConstants.EJB_INSTANCE);

        // retrieve the ejb endpoint manager
        WSEJBEndpointManager endpointManager = (WSEJBEndpointManager) exchange.get(JaxWsEJBConstants.WS_EJB_ENDPOINT_MANAGER);

        // set up the webservice request context 
        WrappedMessageContext ctx = new WrappedMessageContext(exchange.getInMessage(), Scope.APPLICATION);

        //remove handler stuff and set them into a map
        Map<String, Object> handlerScopedStuff = removeHandlerProperties(ctx);

        WebServiceContextImpl.setMessageContext(ctx);

        try {
            Object result = super.invoke(exchange, instance, method, params);
            //update the webservice response context
            updateWebServiceContext(exchange, ctx);
            return result;
        } catch (Fault fault) {
            //get chance to copy over customer's header
            updateHeader(exchange, ctx);

            Throwable ejbInvokeException = endpointManager.setException(fault.getCause());
            boolean checkedException = isCheckedException(method, fault.getCause());
            throw createFault(ejbInvokeException, method, params, checkedException);
        } finally {
            //clear the WebServiceContextImpl's ThreadLocal variable
            WebServiceContextImpl.clear();

            //add back the handler stuff from the map
            addHandlerProperties(ctx, handlerScopedStuff);
        }
    }

    protected boolean isCheckedException(Method method, Throwable ex) {
        if (!(ex instanceof Exception) || (ex instanceof RemoteException)) {
            return false;
        }

        Class<?> exceptionClass = ex.getClass();
        Class<?>[] checkedExceptions = method.getExceptionTypes();

        for (Class<?> checkedClass : checkedExceptions) {
            if (checkedClass.isAssignableFrom(exceptionClass)) {
                return true;
            }
        }

        return false;
    }

    /**
     * This invokes the target operation. We override this method to deal with the
     * fact that the 'serviceObject' is actually an EJB wrapper class. We need
     * to get an equivalent method on the 'serviceObject' class in order to invoke
     * the target operation.
     */
    @Override
    protected Object performInvocation(Exchange exchange, final Object serviceObject, Method m,
                                       Object[] paramArray) throws Exception {
        // This retrieves the appropriate method from the wrapper class
        m = serviceObject.getClass().getMethod(m.getName(), m.getParameterTypes());
        return super.performInvocation(exchange, serviceObject, m, paramArray);
    }

    @Override
    public Object getServiceObject(Exchange context) {
        return null;
    }

    @Override
    public void releaseServiceObject(Exchange ex, Object obj) {}
}
