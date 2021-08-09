/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.spi;

/**
 *
 */
    
import javax.faces.context.ExternalContext;

import org.apache.myfaces.spi.InjectionProvider;
import org.apache.myfaces.spi.InjectionProviderException;

//
// Provide new methods which take a class parameter used for CDI 1.2 support.
// Also helper methods which take an doPostConstruct and external context for better code encapsulation.
//
public abstract class WASInjectionProvider extends InjectionProvider
{
    /**
     * CDI 1.2 injection based on a Class. CDI will create the object instance which allows
     * for constructor injection. PostConstruction is not performed.
     * @param Klass - class of object to be created
     * @return instance of ManagedObject which contains the instance of Klass.
     * @throws InjectionProviderException

     */
    public Object inject(Class Klass) throws InjectionProviderException {
        return inject(Klass, false, (ExternalContext)null);
    }

    /**
     * CDI 1.2 injection based on a Class. CDI will create the object instance which allows
     * for constructor Injection.
     * @param Klass - class of object to be created
     * @param doPostConstruct - boolean indicates whether or not to perform post construct
     * @return instance of ManagedObject which contains the instance of Klass.
     * @throws InjectionProviderException
     */
    public Object inject(Class Klass, boolean doPostConstruct) throws InjectionProviderException {
        return inject(Klass,doPostConstruct,(ExternalContext)null);
    }

    /**
     * CDI 1.2 injection based on a Class. CDI will create the object instance which allows
     * for constructor injection. If an ExternalContext is provided the object is added to   
     * a bean storage map which is used by myFaces to subsequently call preDestroy.
     * @param Klass - class of object to be created.
     * @param doPostConstruct - boolean indicates whether or not to perform post construct.
     * @param eContext - externalContext for the class to be created.
     * @return instance of ManagedObject which contains the instance of Klass.
     * @throws InjectionProviderException.
      */
    public abstract Object inject(Class Klass, boolean doPostConstruct, ExternalContext eContext) throws InjectionProviderException;
   
    /**
     * Injection based on a Object. Supports cases where CDI injection is required for objects
     * which have already been created which happens when a specific constructor is used making
     * constructor injection irrelevant.
     * @param instance - class of object to be injected.
     * @param doPostConstruct - boolean indicates whether or not to perform post construct.
     * @return instance of ManagedObject which contains the instance of Klass.
     * @throws InjectionProviderException
     */    
    public abstract Object inject(Object instance, boolean doPostConstruct) throws InjectionProviderException;
    
    /**
     * Injection based on a Object. Supports cases where CDI injection is required for objects
     * which have already been created which happens when a specific constructor is used making
     * constructor injection irrelevant.
     * @param Object - object to be injected.
     * @param doPostConstruct - boolean indicates whether or not to perform post construct.
     * @param eContext - externalContext for the class to be created.
     * @return instance of ManagedObject which contains the instance of Klass.
     * @throws InjectionProviderException
     */ 
    public abstract Object inject(Object instance, boolean doPostConstruct, ExternalContext eContext) throws InjectionProviderException;

}

