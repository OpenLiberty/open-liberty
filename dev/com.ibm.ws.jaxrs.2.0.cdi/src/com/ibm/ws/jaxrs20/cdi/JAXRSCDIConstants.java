/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi;

/**
 *
 */
public class JAXRSCDIConstants {

    public final static String TR_GROUP = "com.ibm.ws.jaxrs20.cdi";
    public final static String TR_RESOURCE_BUNDLE = "com.ibm.ws.jaxrs20.cdi.internal.resources.JAXRSCDIMessages";
    public final static String JDNI_STRING = "java:comp/BeanManager";
    public final static String REQUEST_SCOPE = "javax.enterprise.context.RequestScoped";
    public final static String APPLICATION_SCOPE = "javax.enterprise.context.ApplicationScoped";
    public final static String SESSION_SCOPE = "javax.enterprise.context.SessionScoped";
    public final static String DEPENDENT_SCOPE = "javax.enterprise.context.Dependent";

}
