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
package com.ibm.wsspi.webservices.handler;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *The interface GlobalHandlerMessageContext abstracts the message context that is processed by a
 * global handler in the handle method.
 */
public interface GlobalHandlerMessageContext {

    /**
     *Retrieves whether the processing is in ServerSide.
     * 
     * @return true if is in Server side; false otherwise.
     */
    public boolean isServerSide();

    /**
     *Retrieves whether the processing is in ClientSide.
     * 
     * @return true if is in Client side; false otherwise.
     */
    public boolean isClientSide();

    /**
     *Retrieves the engine type of the processing.
     * 
     * @return Engine Type of the processing, the possible value is : "JAX_WS", "JAX_RS", "ALL"
     */
    public String getEngineType();

    /**
     *Retrieves flow type of the processing.
     * 
     * @return Flow Type of the processing, the possible value is : "IN", "OUT", "INOUT"
     */
    public String getFlowType();

    /**
     *Retrieves value of the specified property.
     * 
     * @param name the name of the property to retrieve.
     * @return the value associated with the named property or null if no such property exists.
     */
    public Object getProperty(String name);

    /**
     *Associates the specified value with the specified property.
     *If there was already a value associated with this property, the old value is replaced.
     * 
     * @param name the property with which the specified value is to be associated.
     * @param value the value to be associated with the specified property.
     */
    public void setProperty(String name, Object value);

    /**
     *Retrieves all the property names available in this GlobalHandlerMessageContext object.
     * 
     * @return an iterator over all the property names in this GlobalHandlerMessageContext.
     */
    public Iterator<String> getPropertyNames();

    /**
     *Remove the specified property from this GlobalHandlerMessageContext object.
     * 
     * @param name the property names which will be removed from this GlobalHandlerMessageContext.
     */
    public void removeProperty(String name);

    /**
     *Retrieve whether this GlobalHandlerMessageContext object contains the specified property.
     * 
     * @param name the property name
     * @return true is this GlobalHandlerMessageContext object contains the specified property; false otherwise.
     */
    public boolean containsProperty(String name);

    /**
     *Retrieve HttpServletRequest from this GlobalHandlerMessageContext object
     * 
     * @return the HttpServletRequest from this GlobalHandlerMessageContext or null if no HttpServletRequest exists
     */
    public HttpServletRequest getHttpServletRequest();

    /**
     *Retrieve HttpServletResponse from this GlobalHandlerMessageContext object.
     * 
     * @return the HttpServletResponse from this GlobalHandlerMessageContext or null if no HttpServletRequest exists.
     */
    public HttpServletResponse getHttpServletResponse();

    /**
     *Using this method to adapt the GlobalHandlerMessageContext to javax.xml.ws.handler.soap.SOAPMessageContext
     *or javax.xml.ws.handler.LogicalMessageContext.
     * 
     * @param clazz the class which this GlobalHandlerMessageContext will be adpat to
     * @return The apapted messageContext or null if the clazz is none of above.
     */
    public <T> T adapt(Class<T> clazz);

}
