/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ejbcontainer;

import javax.ejb.EJBException;

/**
 * This exception is thrown when the EJB container detects that a lookup
 * of an EJB has been attempted which does not uniquely identify an EJB
 * or EJB interface. <p>
 * 
 * This may occur for the following scenarios :
 * <ul>
 * <li> A 'simple name' binding has been provided for an EJB which has
 * more than one remote or local interface, and an attempt is made
 * to lookup the EJB using the simple name. <p>
 * 
 * Since the 'simple name' does not uniquely identify a single
 * interface, it is considered ambiguous. Instead, the lookup
 * String should have '#<interface>' appended, where <interface>
 * is the desired local or remote interface.
 * 
 * <li> Default short form bindings are being used, and multiple EJBs
 * are configured to implement the same interface. <p>
 * 
 * Since the short form binding does not uniquely identify a
 * specific EJB, it is considered ambiguous. Instead, the
 * lookup should be performed using the long form binding name,
 * or a specific binding should be provided.
 * 
 * <li> ejb-link (xml) or beanName (annotation) has been configured,
 * and more than one bean with the specified name exists in the
 * application. <p>
 * 
 * Since the bean name does not uniquely identify a specific EJB,
 * it is considered ambiguous. Instead, the module name should
 * also be specified (module#beanName).
 * 
 * <li> Auto-link is used, either injecting into a field/method of
 * a specific type, or with beanInterface specified, and more
 * than one bean in the application implements the interface. <p>
 * 
 * Since the interface does not uniquely identify a specific EJB,
 * it is considered ambiguous. Instead, either ejb-link should
 * be used, or a binding provided.
 * </ul>
 **/
public class AmbiguousEJBReferenceException extends EJBException
{
    private static final long serialVersionUID = -9113527466038916053L;

    /**
     * Constructs a new AmbiguousEJBReferenceException. All fields are set
     * to null.
     **/
    public AmbiguousEJBReferenceException()
    {
        super();
    }

    /**
     * Constructs a new AmbiguousEJBReferenceException with the specified detail
     * message. All other fields are set to null.
     **/
    public AmbiguousEJBReferenceException(String detailMessage)
    {
        super(detailMessage);
    }

    /**
     * Constructs a new AmbiguousEJBReferenceException with the specified detail
     * message and cause.
     **/
    public AmbiguousEJBReferenceException(String detailMessage, Exception cause)
    {
        super(detailMessage, cause);
    }
}
