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

import javax.ejb.SessionContext;

/**
 * The <code>SessionContextExtension</code> interface may be used by a
 * Session EJB to invoke WebSphere-specific EJB Container services. <p>
 * 
 * A Session EJB may invoke the SessionContextExtension methods by casting
 * the context object passed into the EJB's setSessionContext() method, to
 * com.ibm.websphere.ejbcontainer.SessionContextExtension. Typically the
 * code in setSessionContext() assigns the context object to a bean
 * instance variable for later use by other bean methods. <p>
 * 
 * In WebSphere, all javax.ejb.SessionContext objects also implement this
 * interface. This allows the bean to use a single 'context' instance variable
 * (of type SessionContextExtension) and be able to invoke EJB
 * specification-defined methods as well as WebSphere-defined methods on the
 * same context object. It is also possible, of course, to assign the context
 * object to two instance variables, one of type javax.ejb.SessionContext and
 * another of type com.ibm.websphere.ejbcontainer.SessionContextExtension. <p>
 * 
 * <b>Note: Some of the methods on this interface may result in behavior not
 * compliant with the official EJB specification.</b> If this is the case, the
 * documentation for that method will indicate so. <p>
 * 
 * @since WAS 6.0.2
 * @see EJBContextExtension
 * @ibm-api
 */

public interface SessionContextExtension
                extends SessionContext,
                EJBContextExtension
{
    // Currently no Session specific extensions.
} // SessionContextExtension
