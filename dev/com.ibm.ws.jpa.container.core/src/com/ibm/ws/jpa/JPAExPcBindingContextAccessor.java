/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa;

/**
 * Extend-scoped persistence context binding context accessor interface. <p>
 * 
 * An EJB Container implementation must be provided for this interface,
 * providing a mechanism to determine the current extended-scoped persistence
 * context binding context active on the thread. <p>
 */
public interface JPAExPcBindingContextAccessor
{
    /**
     * Returns the binding context for the currently active extended-scoped
     * persistence context for the thread of execution. Null will be returned
     * if an extended-scoped persistence context is not currently active. <p>
     * 
     * @return binding context for currently active extended-scoped
     *         persistence context.
     */
    public JPAExPcBindingContext getExPcBindingContext();

    /**
     * Constructs an EJBException with the specified detailed message. <p>
     * 
     * Allows the JPA code to have no dependencies on classes in
     * the javax.ejb package. <p>
     */
    // d741678
    public RuntimeException newEJBException(String msg);
}
