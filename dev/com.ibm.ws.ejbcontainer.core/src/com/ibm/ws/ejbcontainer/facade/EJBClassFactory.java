/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.facade;

/**
 * Provides the EJB Class factory required to support an EJB facade, exposing
 * another component model as an EJB. <p>
 */
public interface EJBClassFactory
{
    /**
     * Dynamically generates and loads all of the EJB interface and
     * implementation classes defined by the specified EJBConfiguration
     * parameter. <p>
     * 
     * This method may not be called until the first time the EJB is accessed. <p>
     * 
     * It is expected that the EJB interfaces and implementation classes will
     * be dynamically generated when this method is called, and loaded with
     * the application ClassLoader using the defineApplicationClass method. <p>
     * 
     * After this method has been called, the ClassLoader.loadClass method of
     * the application ClassLoader should succeed for the EJB interface and
     * implementation classes. <p>
     **/
    public void loadEJBClasses(ClassLoader moduleClassLoader,
                               EJBConfiguration ejbConfig);
}
