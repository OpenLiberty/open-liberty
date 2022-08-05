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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.handler.MessageContext.Scope;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.jaxws.handler.soap.SOAPMessageContextImpl;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceModelUtil;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.osgi.EJBContainer;
import com.ibm.ws.jaxws.ejb.internal.JaxWsEJBConstants;
import com.ibm.wsspi.ejbcontainer.WSEJBEndpointManager;

/**
 *
 */
public class EJBPreInvokeInterceptor extends AbstractPhaseInterceptor<SoapMessage> {

    private final Class<?> implClass;

    private final J2EEName j2EEName;

    private final EJBContainer ejbContainer;

    private final Method[] methods;

    private EJBJaxWsWebEndpoint ejbJaxWsWebEndpoint;

    private volatile boolean handlerChainInitializationRequired = true;

    private final Object handlerInitializationLock = new Object() {};

    private static final TraceComponent tc = Tr.register(EJBPreInvokeInterceptor.class);

    public EJBPreInvokeInterceptor(J2EEName j2EEName, Class<?> implClass, EJBContainer ejbContainer, List<Method> methods) {
        super(Phase.PRE_PROTOCOL_FRONTEND);

        this.implClass = implClass;
        this.j2EEName = j2EEName;
        this.ejbContainer = ejbContainer;
        this.methods = methods == null ? null : methods.toArray(new Method[0]);

    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        Exchange exchange = message.getExchange();

        WSEJBEndpointManager endpointManager = null;
        Method targetMethod = null;
        try {
            if (implClass.isAnnotationPresent(WebServiceProvider.class)) {
                endpointManager = ejbContainer.createWebServiceEndpointManager(j2EEName, Provider.class);
                targetMethod = getProviderMethod(implClass);
            } else {
                endpointManager = ejbContainer.createWebServiceEndpointManager(j2EEName, methods);
                targetMethod = getTargetMethod(implClass, message);
            }

            if (targetMethod == null) {
                throw new IllegalStateException("Invocation target method should not be null.");
            }
        } catch (EJBConfigurationException e) {
            throw new IllegalStateException(e);
        }

        //ejb pre-invoke
        Object instance = null;
        WrappedMessageContext ctx = new WrappedMessageContext(message, Scope.APPLICATION);
        try {
            instance = endpointManager.ejbPreInvoke(targetMethod, ctx);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Execute ejbPreInvoke successfully in EJBPreInvokeInterceptor handleMessage: ", instance);
            }

            //While creating the handler instances, the ReferenceContext from the host EJB Bean is required for injecting the resources
            //in the handlers. But, by default, EJB Bean is lazy initialized, if we create the handlers in the initialization of the router servlet,
            //the target EJB bean may not be initialized, so the injection event will not be published, and then no chance to set the RefereceContext
            //TODO Need a better way for handling this
            if (handlerChainInitializationRequired) {
                synchronized (handlerInitializationLock) {
                    if (handlerChainInitializationRequired) {
                        ejbJaxWsWebEndpoint.initializeHandlers(message);
                        handlerChainInitializationRequired = false;
                    }
                }
            }

            // save the instance in exchange so that the EJBMethodInvoker could retrieve.
            exchange.put(JaxWsEJBConstants.EJB_INSTANCE, instance);
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        } finally {
            // make sure we always set this if ejbPreInvoke was at least attempted
            exchange.put(JaxWsEJBConstants.WS_EJB_ENDPOINT_MANAGER, endpointManager);
        }
    }

    @Override
    public void handleFault(SoapMessage message) {
        try {
            Exchange exchange = message.getExchange();

            // retrieve the ejb endpoint manager
            WSEJBEndpointManager endpointManager = (WSEJBEndpointManager) exchange.get(JaxWsEJBConstants.WS_EJB_ENDPOINT_MANAGER);

            if (endpointManager != null) {
                endpointManager.ejbPostInvoke();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Execute ejbPostInvoke successfully in EJBPreInvokeInterceptor handleFault");
                }
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception occurred when attempting ejbPostInvoke: " + e.getMessage());
            }
            // throw new IllegalStateException(e);
        }
    }

    protected Method getProviderMethod(final Class<?> providerClass) {
        Method providerMethod = null;
        Method[] methods = AccessController.doPrivileged(new PrivilegedAction<Method[]>() {
            @Override
            public Method[] run() {
                return providerClass.getMethods();
            }
        });

        if (methods != null) {
            for (Method method : methods) {
                if (method.getName().equals("invoke")) {
                    providerMethod = method;
                    break;
                }
            }
        }

        return providerMethod;
    }

    private Method getTargetMethod(Class<?> implClass, SoapMessage message) {
        setupBindingOperationInfo(message);
        BindingOperationInfo bop = message.getExchange().getBindingOperationInfo();
        if (bop == null) {
            return null;
        }
        return getMethod(message, bop);
    }

    private void setupBindingOperationInfo(SoapMessage message) {
        Exchange exch = message.getExchange();
        if (exch.get(BindingOperationInfo.class) == null) {
            //need to know the operation to determine if oneway
            QName opName = getOpQName(message);
            if (opName == null) {
                return;
            }
            BindingOperationInfo bop = ServiceModelUtil.getOperationForWrapperElement(exch, opName, false);
            if (bop == null) {
                bop = ServiceModelUtil.getOperation(exch, opName);
            }
            if (bop != null) {
                exch.put(BindingOperationInfo.class, bop);
                exch.put(OperationInfo.class, bop.getOperationInfo());
                if (bop.getOutput() == null) {
                    exch.setOneWay(true);
                }
            }
        }
    }

    private QName getOpQName(SoapMessage message) {
        SOAPMessageContextImpl sm = new SOAPMessageContextImpl(message);
        try {
            SOAPMessage msg = sm.getMessage();
            if (msg == null) {
                return null;
            }
            SOAPBody body = SAAJUtils.getBody(msg);
            if (body == null) {
                return null;
            }
            org.w3c.dom.Node nd = body.getFirstChild();
            while (nd != null && !(nd instanceof org.w3c.dom.Element)) {
                nd = nd.getNextSibling();
            }
            if (nd != null) {
                return new QName(nd.getNamespaceURI(), nd.getLocalName());
            }
            //Fix for CTS Defect 174209
            Collection<BindingOperationInfo> boi = message.getExchange().getEndpoint().getEndpointInfo().getBinding().getOperations();
            if (boi.size() > 0) {
                return boi.iterator().next().getName();
            }
        } catch (SOAPException e) {
            //ignore, nothing we can do
        }
        return null;
    }

    private Method getMethod(Message message, BindingOperationInfo operation) {
        MethodDispatcher md = (MethodDispatcher) message.getExchange().get(Service.class).get(MethodDispatcher.class.getName());
        return md.getMethod(operation);
    }

    /**
     * @param ejbJaxWsWebEndpoint the ejbJaxWsWebEndpoint to set
     */
    public void setEjbJaxWsWebEndpoint(EJBJaxWsWebEndpoint ejbJaxWsWebEndpoint) {
        this.ejbJaxWsWebEndpoint = ejbJaxWsWebEndpoint;
    }
}
