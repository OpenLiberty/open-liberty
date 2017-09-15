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
package com.ibm.ejs.container;

/**
 * A <code>LocalBeanWrapper</code> implementation will subclass an EJB and
 * interposes calls to the container before and after every method call on the
 * EJB. The actual method calls will be directed to a managed EJB instance,
 * and will not be on the wrapper instance itself. <p>
 * 
 * The <code>LocalBeanWrapper</code> implementation is designed to contain a
 * minimum amount of state. Its only state (in addition to any state inherited
 * from the EJB implementation) is a reference to an instance of
 * BusinessLocalWrapper, which inherits from {@link EJSWrapperBase} and
 * provides the normal wrapper state, which consists of a reference to the
 * container it is in and a <code>BeanId</code> instance that defines the
 * identity of its associated EJB. Note, it does not directly maintain a
 * reference to the EJB. It relies on the container to supply it with the
 * appropriate EJB. <p>
 * 
 * Since the EJB 3.1 no-interface view does no implement either the EJBObject
 * or EJBLocalObject interfaces, the LocalBeanWrapper has no methods
 * to implement, unlike the wrapper classes for the EJB 2.1 component
 * interface wrappers. The generated no-interface view implementations
 * could implement no interface, but this class serves as a 'marker' interface,
 * so that internal EJBContainer code can easily distiguish when an object
 * is a no-interface wrapper. <p>
 * 
 * NOTE: The generated no-interface view implementation to a bean
 * extends the EJB instance. For that reason, LocalBeanWrapper MUST NOT
 * implement any methods. Otherwise, there would be a potential conflict
 * between the methods on the bean's local business interface and those
 * on LocalBeanWrapper. <p>
 **/
public interface LocalBeanWrapper
{
    // --------------------------------------------------------------------------
    // Intentionally contains no additonal state or methods.
    // Used as a marker interface to distinguish No-Interface wrappers.
    // --------------------------------------------------------------------------
}
