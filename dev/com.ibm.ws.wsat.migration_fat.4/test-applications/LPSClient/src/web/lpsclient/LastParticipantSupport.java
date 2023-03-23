/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.lpsclient;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "LastParticipantSupport", targetNamespace = "http://lpsservice.web/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface LastParticipantSupport {

	/**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS004FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS004FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS004FVT")
    @ResponseWrapper(localName = "WSTXLPS004FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS004FVTResponse")
    public String wstxlps004FVT();
    
	/**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS005FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS005FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS005FVT")
    @ResponseWrapper(localName = "WSTXLPS005FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS005FVTResponse")
    public String wstxlps005FVT();
    
	/**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS011FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS011FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS011FVT")
    @ResponseWrapper(localName = "WSTXLPS011FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS011FVTResponse")
    public String wstxlps011FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS106FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS106FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS106FVT")
    @ResponseWrapper(localName = "WSTXLPS106FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS106FVTResponse")
    public String wstxlps106FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS101FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS101FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS101FVT")
    @ResponseWrapper(localName = "WSTXLPS101FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS101FVTResponse")
    public String wstxlps101FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS202FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS202FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS202FVT")
    @ResponseWrapper(localName = "WSTXLPS202FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS202FVTResponse")
    public String wstxlps202FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     * @throws Exception_Exception
     */
    @WebMethod(operationName = "WSTXLPS114FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS114FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS114FVT")
    @ResponseWrapper(localName = "WSTXLPS114FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS114FVTResponse")
    public String wstxlps114FVT()
        throws Exception_Exception
    ;

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS108FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS108FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS108FVT")
    @ResponseWrapper(localName = "WSTXLPS108FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS108FVTResponse")
    public String wstxlps108FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS203FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS203FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS203FVT")
    @ResponseWrapper(localName = "WSTXLPS203FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS203FVTResponse")
    public String wstxlps203FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS204FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS204FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS204FVT")
    @ResponseWrapper(localName = "WSTXLPS204FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS204FVTResponse")
    public String wstxlps204FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS109FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS109FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS109FVT")
    @ResponseWrapper(localName = "WSTXLPS109FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS109FVTResponse")
    public String wstxlps109FVT();

    /**
     * 
     * @param arg0
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "echo", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.Echo")
    @ResponseWrapper(localName = "echoResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.EchoResponse")
    public String echo(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0);

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS113FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS113FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS113FVT")
    @ResponseWrapper(localName = "WSTXLPS113FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS113FVTResponse")
    public String wstxlps113FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS206FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS206FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS206FVT")
    @ResponseWrapper(localName = "WSTXLPS206FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS206FVTResponse")
    public String wstxlps206FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS104FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS104FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS104FVT")
    @ResponseWrapper(localName = "WSTXLPS104FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS104FVTResponse")
    public String wstxlps104FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS110FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS110FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS110FVT")
    @ResponseWrapper(localName = "WSTXLPS110FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS110FVTResponse")
    public String wstxlps110FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS201FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS201FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS201FVT")
    @ResponseWrapper(localName = "WSTXLPS201FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS201FVTResponse")
    public String wstxlps201FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS205FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS205FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS205FVT")
    @ResponseWrapper(localName = "WSTXLPS205FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS205FVTResponse")
    public String wstxlps205FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS102FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS102FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS102FVT")
    @ResponseWrapper(localName = "WSTXLPS102FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS102FVTResponse")
    public String wstxlps102FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS107FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS107FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS107FVT")
    @ResponseWrapper(localName = "WSTXLPS107FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS107FVTResponse")
    public String wstxlps107FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS207FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS207FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS207FVT")
    @ResponseWrapper(localName = "WSTXLPS207FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS207FVTResponse")
    public String wstxlps207FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     * @throws Exception_Exception
     */
    @WebMethod(operationName = "WSTXLPS103FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS103FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS103FVT")
    @ResponseWrapper(localName = "WSTXLPS103FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS103FVTResponse")
    public String wstxlps103FVT()
        throws Exception_Exception
    ;

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS105FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS105FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS105FVT")
    @ResponseWrapper(localName = "WSTXLPS105FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS105FVTResponse")
    public String wstxlps105FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS112FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS112FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS112FVT")
    @ResponseWrapper(localName = "WSTXLPS112FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS112FVTResponse")
    public String wstxlps112FVT();

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod(operationName = "WSTXLPS111FVT")
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "WSTXLPS111FVT", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS111FVT")
    @ResponseWrapper(localName = "WSTXLPS111FVTResponse", targetNamespace = "http://lpsservice.web/", className = "web.lpsclient.WSTXLPS111FVTResponse")
    public String wstxlps111FVT();

}
