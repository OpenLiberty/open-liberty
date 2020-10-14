/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ejbcontainer;

import javax.ejb.MessageDrivenContext;

/**
 * The <code>MessageDrivenContextExtension</code> interface may be used by a
 * Message-Driven EJB to invoke WebSphere-specific EJB Container services. <p>
 * 
 * A Message-Driven EJB may invoke the MessageDrivenContextExtension methods
 * by casting the context object passed into the EJB's setMessageDrivenContext()
 * method, to com.ibm.websphere.ejbcontainer.MessageDrivenContextExtension.
 * Typically the code in setMessageDrivenContext() assigns the context
 * object to a bean instance variable for later use by other bean methods. <p>
 * 
 * In WebSphere, all javax.ejb.MessageDrivenContext objects also implement this
 * interface. This allows the bean to use a single 'context' instance variable
 * (of type MessageDrivenContextExtension) and be able to invoke EJB
 * specification-defined methods as well as WebSphere-defined methods on the
 * same context object. It is also possible, of course, to assign the context
 * object to two instance variables, one of type javax.ejb.MessageDrivenContext
 * and another of type
 * com.ibm.websphere.ejbcontainer.MessageDrivenContextExtension. <p>
 * 
 * <b>Note: Some of the methods on this interface may result in behavior not
 * compliant with the official EJB specification.</b> If this is the case, the
 * documentation for that method will indicate so. <p>
 * 
 * @since WAS 6.0.2
 * @see EJBContextExtension
 * @ibm-api
 */

public interface MessageDrivenContextExtension
                extends MessageDrivenContext,
                EJBContextExtension
{
    // Currently no MessageDriven specific extensions.
} // MessageDrivenContextExtension
