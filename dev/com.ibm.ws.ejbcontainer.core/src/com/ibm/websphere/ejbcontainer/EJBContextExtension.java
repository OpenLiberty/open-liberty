/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ejbcontainer;

import javax.ejb.EJBContext;

// for javadoc

/**
 * The <code>EJBContextExtension</code> interface may be used by an
 * EJB to invoke WebSphere-specific EJB Container services. <p>
 * 
 * An EJB may invoke the EJBContextExtension methods by casting the context
 * object passed into the EJB's setSessionContext(), setEntityContext() or
 * setMessageDrivenContext() method, to
 * com.ibm.websphere.ejbcontainer.EJBContextExtension. Typically the code in
 * setSessionContext(), setEntityContext() or setMessageDrivenContext assigns
 * the context object to a bean instance variable for later use by other
 * bean methods. <p>
 * 
 * In WebSphere, all javax.ejb.EJBContext objects also implement this interface.
 * This allows the bean to use a single 'context' instance variable (of type
 * EJBContextExtension) and be able to invoke EJB specification-defined methods
 * as well as WebSphere-defined methods on the same context object. It is also
 * possible, of course, to assign the context object to two instance variables,
 * one of type javax.ejb.EJBContext and another of type
 * com.ibm.websphere.ejbcontainer.EJBContextExtension. <p>
 * 
 * For each subinterface of javax.ejb.EJBContext, a corresponding
 * WebSphere-defined context extension interface extends both the applicable
 * subinterface of EJBContext and this EJBContextExtension interface.
 * See {@link EntityContextExtension}, {@link MessageDrivenContextExtension},
 * and {@link SessionContextExtension}. <p>
 * 
 * <b>Note: Application use of some of the methods on this interface may result
 * in behavior not
 * compliant with the official EJB specification.</b> If this is the case, the
 * documentation for that method will indicate so. <p>
 * 
 * @since WAS 6.0.2
 * @see EntityContextExtension
 * @see MessageDrivenContextExtension
 * @see SessionContextExtension
 * @ibm-api
 */

public interface EJBContextExtension
                extends EJBContext
{
    /**
     * Flushes the persistent state of all entity EJB instances
     * that have been modified in the current transaction in the current
     * server process, to persistent storage. <p>
     * 
     * An application may need to perform this action prior to executing
     * a query over persistent storage to insure all modifications to
     * entity EJB instances within the current transaction are reflected
     * in the query results. <p>
     * 
     * For EJBs packaged in a Java EE 1.3 or later module, the EJB Container will
     * automatically flush the persistent state prior to executing any "custom" finder
     * method (that is, any finder method other than findByPrimaryKey), so typically
     * the application would not need to invoke this method. (Unless the EJB
     * has explicitly disabled the flush behavior; see below.)<p>
     * 
     * However, some applications may wish to manually invoke the flush behavior
     * for those EJBs packaged in Java EE 1.2 or earlier
     * modules, or for EJBs that implement the {@link DisableFlushBeforeFind} interface. <p>
     * 
     * Although this method is an extension, its use by applications is compliant with
     * the EJB Specification -- applications are allowed to perform manual
     * cache flushes in addition to those done automatically by the container. <p>
     * 
     * @exception IllegalStateException is thrown if the bean invoking this
     *                method is not currently executing within a transaction
     *                context or if the transaction has been marked for
     *                rollback.
     * @exception javax.ejb.EJBException is thrown if an error occurs while
     *                trying to flush the EJB cache to persistent storage. <p>
     * 
     * @since WAS 6.0.2
     * @see DisableFlushBeforeFind
     */
    public void flushCache();

    /**
     * Returns true when the current thread is associated with a global
     * transaction; otherwise, returns false. <p>
     * 
     * This method is useful for container managed transaction beans
     * that may run in either a global transaction or an unspecified
     * (local) transaction context. Since the EJBContext methods
     * getRollbackOnly() and setRollbackOnly() may only be called
     * when running in a global transaction, this method may be
     * used to determine when these other methods may be called. <p>
     * 
     * @since WAS 6.0.2.19
     **/
    // d406058
    public boolean isTransactionGlobal();
} // EJBContextExtension
