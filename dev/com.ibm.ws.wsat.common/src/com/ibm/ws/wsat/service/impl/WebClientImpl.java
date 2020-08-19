/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.service.impl;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.soap.AddressingFeature;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.impl.AddressingPropertiesImpl;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.wsat.common.impl.WSATEndpoint;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.WebClient;
import com.ibm.ws.wsat.tm.impl.TranManagerImpl;
import com.ibm.ws.wsat.webservice.client.CoordinatorPortType;
import com.ibm.ws.wsat.webservice.client.CoordinatorService;
import com.ibm.ws.wsat.webservice.client.ParticipantPortType;
import com.ibm.ws.wsat.webservice.client.ParticipantService;
import com.ibm.ws.wsat.webservice.client.RegistrationPortType;
import com.ibm.ws.wsat.webservice.client.RegistrationService;
import com.ibm.ws.wsat.webservice.client.SSLClientInterceptor;
import com.ibm.ws.wsat.webservice.client.wsat.Notification;
import com.ibm.ws.wsat.webservice.client.wscoor.RegisterResponseType;
import com.ibm.ws.wsat.webservice.client.wscoor.RegisterType;

/**
 * This class contains the client code for invoking the WS-AT and WS-Coord
 * protocol web services.
 */
public class WebClientImpl extends WebClient {

    private static final String CLASS_NAME = WebClientImpl.class.getName();
    private static final TraceComponent TC = Tr.register(WebClientImpl.class);

    private static final HandlerImpl handlerService = HandlerImpl.getInstance();
    private static TranManagerImpl tranService = TranManagerImpl.getInstance();

    private static final AddressingFeature wsAddrFeat = new AddressingFeature(true);

    private final WSATEndpoint toEpr;
    private final WSATEndpoint fromEpr;

    public WebClientImpl(WSATEndpoint toEpr, WSATEndpoint fromEpr) {
        this.toEpr = toEpr;
        this.fromEpr = fromEpr;
    }

    /*
     * WS-Coor register
     */
    @Override
    public EndpointReferenceType register(final EndpointReferenceType participant) throws WSATException {
        return invoke(new Callable<EndpointReferenceType>() {
            @Override
            public EndpointReferenceType call() throws Exception {
                RegistrationService regService = new RegistrationService();
                RegistrationPortType port = getPort(regService, RegistrationPortType.class);
                setTimeouts(port);

                RegisterType regParm = new RegisterType();
                regParm.setParticipantProtocolService(participant);
                regParm.setProtocolIdentifier(Constants.WS_AT_PROTOCOL);
                RegisterResponseType regResp = port.registerOperation(regParm);
                return regResp.getCoordinatorProtocolService();
            }
        });
    }

    /*
     * WS-AT participant request
     */
    @Override
    public void prepare() throws WSATException {
        invoke(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                ParticipantService partService = new ParticipantService();
                ParticipantPortType port = getPort(partService, ParticipantPortType.class);
                setTimeouts(port);

                Notification parm = new Notification();
                port.prepareOperation(parm);
                return null;
            }
        });
    }

    @Override
    public void rollback() throws WSATException {
        invoke(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                ParticipantService partService = new ParticipantService();
                ParticipantPortType port = getPort(partService, ParticipantPortType.class);
                setTimeouts(port);

                Notification parm = new Notification();

                port.rollbackOperation(parm);
                return null;
            }
        });
    }

    @Override
    public void commit() throws WSATException {
        invoke(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                ParticipantService partService = new ParticipantService();
                ParticipantPortType port = getPort(partService, ParticipantPortType.class);
                setTimeouts(port);

                Notification parm = new Notification();
                port.commitOperation(parm);
                return null;
            }
        });
    }

    /*
     * WS-AT coordinator responses
     */
    @Override
    public void prepared() throws WSATException {
        invoke(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                CoordinatorService coordService = new CoordinatorService();
                CoordinatorPortType port = getPort(coordService, CoordinatorPortType.class);
                setTimeouts(port);

                Notification parm = new Notification();
                port.preparedOperation(parm);
                return null;
            }
        });
    }

    @Override
    public void readOnly() throws WSATException {
        invoke(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                CoordinatorService coordService = new CoordinatorService();
                CoordinatorPortType port = getPort(coordService, CoordinatorPortType.class);
                setTimeouts(port);

                Notification parm = new Notification();
                port.readOnlyOperation(parm);
                return null;
            }
        });
    }

    @Override
    public void aborted() throws WSATException {
        invoke(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                CoordinatorService coordService = new CoordinatorService();
                CoordinatorPortType port = getPort(coordService, CoordinatorPortType.class);
                setTimeouts(port);

                Notification parm = new Notification();
                port.abortedOperation(parm);
                return null;
            }
        });
    }

    @Override
    public void committed() throws WSATException {
        invoke(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                CoordinatorService coordService = new CoordinatorService();
                CoordinatorPortType port = getPort(coordService, CoordinatorPortType.class);
                setTimeouts(port);

                Notification parm = new Notification();
                port.committedOperation(parm);
                return null;
            }
        });
    }

    // Invoke the web service call in a wrapper that sets the thread context class loader
    // to something that seems to keep jaxws/apache CXF happy.  Not sure why we need to do
    // this but it seems some threads (maybe worker threads processing inbound web service
    // calls?) do not get all the necessary jaxws classes on their classpath.
    private <T> T invoke(final Callable<T> action) throws WSATException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<T>() {
                @Override
                public T run() throws Exception {
                    ClassLoader saveLoader = Thread.currentThread().getContextClassLoader();
                    ClassLoader localLoader = tranService.getThreadClassLoader(WebClientImpl.class);
                    try {
                        Thread.currentThread().setContextClassLoader(localLoader);
                        handlerService.setWsatCall(true);
                        return action.call();
                    } finally {
                        handlerService.setWsatCall(false);
                        Thread.currentThread().setContextClassLoader(saveLoader);
                        tranService.destroyThreadClassLoader(localLoader);
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            throw new WSATException(Tr.formatMessage(TC, "WEB_SERVICE_ERROR_CWLIB0204"), e.getException());
        }
    }

    // Obtain the web service port for a given service
    private <T> T getPort(Service service, Class<T> portType) {
        T port = service.getPort(toEpr.getWsEpr(), portType, wsAddrFeat);

        if (fromEpr != null) {
            // TODO: According to the WS-AT spec, section 8 we should set the wsa:From header to
            // indicate the sender and the wsa:replyTo should be http://www.w3.org/2005/08/addressing/none.
            // However, tWAS seems to expect the replyTo to be set (and it uses it when sending protocol
            // responses), so we had better set replyTo, as inter-op with tWAS is our prime use-case.

            AddressingProperties wsAddr = new AddressingPropertiesImpl();
            //wsAddr.setFrom(fromEpr.getEndpointReference());
            wsAddr.setReplyTo(fromEpr.getEndpointReference());
            ((BindingProvider) port).getRequestContext().put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, wsAddr);
        }
        if (toEpr.isSecure()) {
            Client c = ClientProxy.getClient(port);
            c.getOutInterceptors().add(new SSLClientInterceptor());
        }

        return port;
    }
}
