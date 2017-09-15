/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ejbcontainer;

/**
 * Marker interface to indicate that an entity bean that implements it, should not
 * cause other entity bean state to be flushed to the persistent store prior to
 * "custom" finder operations on this bean's home. <p>
 * 
 * <b>Note: Enabling this option results in behavior not compliant with the
 * official EJB specification.</b> <p>
 * 
 * For entity beans packaged in a Java EE 1.3 or later module, prior to the
 * execution of any application-specific "custom" finder methods (that is,
 * any finder method other than
 * findByPrimaryKey), the EJB Container automatically
 * flushes to storage the persistent state of those beans modified by the
 * current transaction, to ensure that those modifications are reflected in the
 * finder method results. For applications that are coded to ensure that any
 * such modifications will not affect the query results, or that otherwise account
 * for the modifications, a performance improvement may be obtained
 * by avoiding the flush behavior. This may be achieved by declaring the
 * entity bean implementation (or a parent class of the bean implementation)
 * to implement this interface. <p>
 * 
 * Implementing this interface on an entity bean causes equivalent behavior
 * as setting the disableFlushBeforeFind field for that bean in the
 * IBM deployment descriptor extensions (ibm-ejb-jar-ext.xmi) to "true", or
 * as setting that bean's
 * com/ibm/websphere/ejbcontainer/disableFlushBeforeFind
 * EJB environment variable to "true". For scenarios where many beans extend
 * a single root class, it can be more convenient to have that class
 * implement this marker interface than to individually set the environment
 * variable or deployment descriptor extension field on each of the beans.
 * Setting either the deployment descriptor extension or the environment
 * variable to a value of "false" has no effect; a "false" value does not
 * override the presence of the marker interface. <p>
 * 
 * For applications that require entity bean modifications to be flushed
 * to persistent storage at specific times or for specific methods,
 * this interface may be used in conjunction with the {@link EJBContextExtension#flushCache} method. <p>
 * 
 * Note: the DisableFlushBeforeFind interface may be used for entity beans
 * with either container managed or bean managed persistence; however, it has no
 * effect on entity beans packaged in a Java EE 1.2 or earlier module. <p>
 * 
 * @since WAS 6.0.2
 * @see EJBContextExtension
 * @ibm-api
 **/

public interface DisableFlushBeforeFind
{
    // Empty interface, used only as a marker.
}
