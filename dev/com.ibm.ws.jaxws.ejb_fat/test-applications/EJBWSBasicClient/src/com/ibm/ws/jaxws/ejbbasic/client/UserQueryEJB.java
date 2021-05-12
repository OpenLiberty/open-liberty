/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.ejbbasic.client;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.Stateless;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.Addressing;

@Stateless(name = "UseQueryEJBBean")
public class UserQueryEJB implements UserQueryEJBInterface {

    @Addressing
    @WebServiceRef(value = UserQueryService.class, wsdlLocation = "WEB-INF/wsdl/UserQueryService.wsdl")
    private UserQuery userQuery;

    private String serverName;

    @Override
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
    }

    private String serverPort;

    private static final long MAX_ASYNC_WAIT_TIME = 30 * 1000;

    protected void setEndpointAddress(BindingProvider bindingProvider, String endpointPath) throws Exception {
        if (serverName == null || serverPort == null) {
            throw new Exception("serverName and serverPort can not be null");
        }

        bindingProvider.getRequestContext().put("allowNonMatchingToDefaultSoapAction", true);
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                "http://" + serverName + ":" + serverPort + "/" + endpointPath);
    }

    @Override
    public String getUserAsyncHandler(final String name) {
        final StringBuffer result = new StringBuffer(128);
        try {
            setEndpointAddress((BindingProvider) userQuery, "EJBWSBasic/UserQueryService");
            Future<?> future = userQuery.getUserAsync(name, new AsyncHandler<GetUserResponse>() {
                @Override
                public void handleResponse(Response<GetUserResponse> response) {
                    try {
                        User user = response.get().getReturn();
                        if (user == null) {
                            result.append("FAILED Expected user instance is not returned");
                        } else if (name != null && !name.equals(user.getName())) {
                            result.append("FAILED Expected user instance with name" + name + " is not returned");
                        } else {
                            result.append("PASS");
                        }
                    } catch (Exception e) {
                        result.append("FAILED " + e.getMessage());
                    }
                }
            });
            long curWaitTime = 0;
            Object lock = new Object();

            while (!future.isDone() && curWaitTime < MAX_ASYNC_WAIT_TIME) {
                synchronized (lock) {
                    try {
                        lock.wait(50L);
                    } catch (InterruptedException e) {
                    }
                }
                curWaitTime += 50;
            }

            if (!future.isDone()) {

                result.append("FAILED the getUser is not returned after the timeout " + MAX_ASYNC_WAIT_TIME);
                //dump in case of time out happens to see what is going on threads

                Util.dumpJvmState();
            }
        } catch (UserNotFoundException_Exception e) {
            result.append("FAILED Unexpected UserNotFoundException is thrown" + e.getMessage());
        } catch (Exception e) {
            result.append("FAILED Unexpected Exception is thrown" + e.getMessage());
        }

        return result.toString();
    }

    @Override
    public String getUserAsyncResponse(String name) {

        final StringBuffer result = new StringBuffer(128);

        try {
            setEndpointAddress((BindingProvider) userQuery, "EJBWSBasic/UserQueryService");
            Response<GetUserResponse> response = userQuery.getUserAsync(name);

            long curWaitTime = 0;
            Object lock = new Object();

            while (!response.isDone() && curWaitTime < MAX_ASYNC_WAIT_TIME) {
                synchronized (lock) {
                    try {
                        lock.wait(50L);
                    } catch (InterruptedException e) {
                    }
                }
                curWaitTime += 50;
            }

            if (!response.isDone()) {
                result.append("FAILED Response is not received after waiting " + MAX_ASYNC_WAIT_TIME);
                return result.toString();
            }

            User user = response.get().getReturn();

            if (user == null) {
                result.append("FAILED Expected user instance is not returned");
            } else if (name != null && !name.equals(user.getName())) {
                result.append("FAILED Expected user instance with name" + name + " is not returned");
            } else {
                result.append("PASS");
            }
        } catch (UserNotFoundException_Exception e) {
            result.append("FAILED Unexpected UserNotFoundException is thrown" + e.getMessage());
        } catch (InterruptedException e) {
            result.append("FAILED Unexpected InterruptedException is thrown" + e.getMessage());
        } catch (ExecutionException e) {
            result.append("FAILED Unexpected ExecutionException is thrown" + e.getMessage());
        } catch (Exception e) {
            result.append("FAILED Unexpected Exception is thrown" + e.getMessage());
        }

        return result.toString();
    }

}
