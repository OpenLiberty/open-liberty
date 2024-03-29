/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/

package com.ibm.ws.jaxws.ejbbasic.client;

import java.util.List;
import java.util.concurrent.Future;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.Response;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "UserQuery", targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/")
@XmlSeeAlso( { ObjectFactory.class })
public interface UserQuery {

    /**
     * 
     * @param arg0
     * @return
     *         returns javax.xml.ws.Response<com.ibm.ws.jaxws.ejbbasic.client.GetUserResponse>
     */
    @WebMethod(operationName = "getUser")
    @RequestWrapper(localName = "getUser", targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbbasic.client.GetUser")
    @ResponseWrapper(localName = "getUserResponse", targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbbasic.client.GetUserResponse")
    public Response<GetUserResponse> getUserAsync(
                                                  @WebParam(name = "arg0", targetNamespace = "") String arg0)
                                                  throws UserNotFoundException_Exception;

    /**
     * 
     * @param arg0
     * @param asyncHandler
     * @return
     *         returns java.util.concurrent.Future<? extends java.lang.Object>
     */
    @WebMethod(operationName = "getUser")
    @RequestWrapper(localName = "getUser", targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbbasic.client.GetUser")
    @ResponseWrapper(localName = "getUserResponse", targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbbasic.client.GetUserResponse")
    public Future<?> getUserAsync(
                                  @WebParam(name = "arg0", targetNamespace = "") String arg0,
                                  @WebParam(name = "asyncHandler", targetNamespace = "") AsyncHandler<GetUserResponse> asyncHandler)
                    throws UserNotFoundException_Exception;

    /**
     * 
     * @param arg0
     * @return
     *         returns com.ibm.ws.jaxws.ejbbasic.client.User
     * @throws UserNotFoundException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getUser", targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbbasic.client.GetUser")
    @ResponseWrapper(localName = "getUserResponse", targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbbasic.client.GetUserResponse")
    public User getUser(
                        @WebParam(name = "arg0", targetNamespace = "") String arg0)
                    throws UserNotFoundException_Exception;

    /**
     * 
     * @return
     *         returns javax.xml.ws.Response<com.ibm.ws.jaxws.ejbbasic.client.ListUsersResponse>
     */
    @WebMethod(operationName = "listUsers")
    @RequestWrapper(localName = "listUsers", targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbbasic.client.ListUsers")
    @ResponseWrapper(localName = "listUsersResponse", targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbbasic.client.ListUsersResponse")
    public Response<ListUsersResponse> listUsersAsync();

    /**
     * 
     * @param asyncHandler
     * @return
     *         returns java.util.concurrent.Future<? extends java.lang.Object>
     */
    @WebMethod(operationName = "listUsers")
    @RequestWrapper(localName = "listUsers", targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbbasic.client.ListUsers")
    @ResponseWrapper(localName = "listUsersResponse", targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbbasic.client.ListUsersResponse")
    public Future<?> listUsersAsync(
                                    @WebParam(name = "asyncHandler", targetNamespace = "") AsyncHandler<ListUsersResponse> asyncHandler);

    /**
     * 
     * @return
     *         returns java.util.List<com.ibm.ws.jaxws.ejbbasic.client.User>
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "listUsers", targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbbasic.client.ListUsers")
    @ResponseWrapper(localName = "listUsersResponse", targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbbasic.client.ListUsersResponse")
    public List<User> listUsers();

}
