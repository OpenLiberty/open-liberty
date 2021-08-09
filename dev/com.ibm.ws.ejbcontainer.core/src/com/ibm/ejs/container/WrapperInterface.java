/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

/**
 * <code>WrapperInterface</code> defines the legal values for the interface
 * type associated with {@link EJSWrapperBase} subclasses/implementations. <p>
 * 
 * <code>WrapperInterface</code> is a java enumeration type used for compile
 * time checking of valid values. The values are the objects themselves, and
 * there is a single instance for each value, so == checking should be performed
 * rather than equals() comparisons. <p>
 * 
 * <DL>
 * <DT>The valid interfaces an EJB wrapper may belong to are:
 * <DD>{@link #REMOTE} <DD>{@link #HOME} <DD>{@link #LOCAL} <DD>{@link #LOCAL_HOME} <DD>{@link #SERVICE_ENDPOINT} <DD>{@link #MESSAGE_LISTENER} <DD>{@link #TIMED_OBJECT} <DD>
 * {@link #BUSINESS_LOCAL} <DD>{@link #BUSINESS_REMOTE} <DD>{@link #BUSINESS_RMI_REMOTE} <DD>{@link #LIFECYCLE_INTERCEPTOR} </DL> <p>
 * 
 * <code>WrapperInterface</code> is similar to {@link MethodInterface},
 * but more granular. There is a <code>WrapperInterface</code> for every
 * posible interface type, whereas a method may be associated with multiple
 * interface types, so MethodInterface has fewer values. For example, when a
 * method is associated with the 'Remote' interfaces, it is associated with
 * both the Component and Business Remote interfaces. A 'wrapper' however,
 * corresponds directly to either the Component or Business Remote interface,
 * and NOT both. <p>
 * 
 * @see MethodInterface
 * @see EJSWrapperBase
 */
public enum WrapperInterface
{
    REMOTE(RemoteExceptionMappingStrategy.INSTANCE, true), // EJB 2.x Remote Component Interface
    HOME(RemoteExceptionMappingStrategy.INSTANCE, true), // EJB 2.x Remote Home Interface
    LOCAL(LocalExceptionMappingStrategy.INSTANCE, false), // EJB 2.x Local Component Interface
    LOCAL_HOME(LocalExceptionMappingStrategy.INSTANCE, false), // EJB 2.x Local Home Interface
    SERVICE_ENDPOINT(RemoteExceptionMappingStrategy.INSTANCE, false), // WebService Endpoint Interface
    MESSAGE_LISTENER(LocalExceptionMappingStrategy.INSTANCE, false), // Message Listener Interface
    TIMED_OBJECT(LocalExceptionMappingStrategy.INSTANCE, false), // TimedObject Interface
    BUSINESS_LOCAL(BusinessExceptionMappingStrategy.INSTANCE, false), // EJB 3.0 Business Local Interface
    BUSINESS_REMOTE(BusinessExceptionMappingStrategy.INSTANCE, false), // EJB 3.0 Business Remote Interface
    BUSINESS_RMI_REMOTE(RemoteExceptionMappingStrategy.INSTANCE, true), // EJB 3.0 Business java.rmi.Remote Interface
    LIFECYCLE_INTERCEPTOR(BusinessExceptionMappingStrategy.INSTANCE, false); // Lifecycle interceptor callbacks

    final transient ExceptionMappingStrategy ivExceptionStrategy;
    final boolean ivORB;

    WrapperInterface(ExceptionMappingStrategy exceptionStrategy, boolean orb)
    {
        ivExceptionStrategy = exceptionStrategy;
        ivORB = orb;
    }
}
