/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.jaxws.fat;

import java.util.concurrent.Future;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

@WebService(targetNamespace="http://fat.jaxws.openliberty.io",
serviceName="SimpleWebService"
)
@SOAPBinding(style = Style.RPC) 
public interface SimpleWebService {

    // Syncronous web service method
    public String simpleHello(String invoker);
    
    // Asnc method that returns a Resonse object
    @WebMethod(operationName = "simpleHello") 
    public Response simpleHelloAsync(@WebParam(name = "invoker") String invoker);
    
    // Asnc method that returns a Future object
    @WebMethod(operationName = "simpleHello") 
    public Future simpleHelloAsync(@WebParam(name = "invoker") String invoker, @WebParam(name = "asyncHandler") AsyncHandler asyncHandler);
    
    @Oneway
    public void oneWaySimpleHello(@WebParam(name = "invoker") String invoker);
}
