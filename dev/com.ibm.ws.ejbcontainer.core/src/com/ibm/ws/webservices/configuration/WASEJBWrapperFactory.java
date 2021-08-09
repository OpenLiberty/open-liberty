/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webservices.configuration;

/**
 * This class will be the factory interace that will be implemented by
 * classes wishing to return an instance of a class that is a wrapper for
 * web service implementations within an EJB module. This will be registered
 * with the WASWebServicesBind class and will be available via a static method
 * on that class. The EJB container will need the wrapper class in order for
 * the web services code to make any invocation within the EJB container.
 */

public interface WASEJBWrapperFactory {

    public Class getEJBWrapperClass();

}
