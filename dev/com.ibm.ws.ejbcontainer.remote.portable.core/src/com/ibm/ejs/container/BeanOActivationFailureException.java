/*******************************************************************************
 * Copyright (c) 1998, 2005 IBM Corporation and others.
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
 * This exception is thrown whenever an attempt to activate a
 * <code>BeanO</code> instance fails. <p>
 * 
 * This exception must never be thrown to an EJB client. It is intended
 * to signal a transient failure to the container. For instance, if one
 * thread has created a <code>BeanO</code> and another thread attempts to
 * invoke a method on the newly created <code>BeanO</code> it is possible
 * that the second thread's request may fail with a
 * <code>BeanOActivationFailureException</code> if the create fails. At
 * this point the container must retry the second thread's invocation
 * request on a fresh <code>BeanO</code> instance, in case the
 * <code>BeanO</code> already exists.
 * 
 */

public class BeanOActivationFailureException
                extends ContainerException
{
    private static final long serialVersionUID = 6114931973348549639L;

    /**
     * Create a new <code>BeanOActivationFailureException</code>
     * instance. <p>
     */

    public BeanOActivationFailureException() {
        super();
    } // BeanOActivationFailureException

} // BeanOActivationFailureException
