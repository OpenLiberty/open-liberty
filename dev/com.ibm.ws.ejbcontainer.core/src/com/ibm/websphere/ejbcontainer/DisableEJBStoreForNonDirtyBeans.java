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
 * Marker interface to indicate that an entity bean that implements it,
 * should not have its ejbStore() method invoked if the persistent state
 * of the bean has not been modified during the current transaction. <p>
 * 
 * <b>Note: Enabling this option results in behavior not compliant with the
 * official EJB specification.</b> <p>
 * 
 * Normally, during transaction commit processing and prior to executing
 * EJB application-specific "custom" finder methods, the EJB Container
 * will invoke the ejbStore()
 * container callback method on all entity beans enlisted in the transaction.
 * This marker interface provides a mechanism to avoid these calls to
 * ejbStore() when the persistent state of the entity bean has not been
 * modified within the scope of the transaction. <p>
 * 
 * DisableEJBStoreForNonDirtyBeans may be useful to applications which use
 * EJB 2.x container managed persistence and also have significant code in
 * their ejbStore() container callback method. If the application-provided
 * ejbStore() method does not need to be called when the persistent state
 * of the entity bean has not been modified, then the application may obtain
 * a performance improvement by avoiding the call to ejbStore(). This may
 * be achieved by declaring the entity bean implementation to implement this
 * interface. <p>
 * 
 * This interface is generally not useful when the application-provided
 * ejbStore() method is empty. Entity beans that have not been modified
 * will not be updated in the database regardless of this interface, so
 * the only performance savings would be for the call overhead to the
 * empty ejbStore() method. <p>
 * 
 * Implementing this interface on an entity bean causes equivalent behavior
 * as setting the disableEJBStoreForNonDirtyBeans field for that bean in the
 * IBM deployment descriptor extensions (ibm-ejb-jar-ext.xmi) to "true", or
 * as setting that bean's
 * com/ibm/websphere/ejbcontainer/disableEJBStoreForNonDirtyBeans
 * EJB environment variable to "true". For scenarios where many beans extend
 * a single root class, it can be more convenient to have that class
 * implement this marker interface than to individually set the environment
 * variable or deployment descriptor extension field on each of the beans.
 * Setting either the deployment descriptor extension or the environment
 * variable to a value of "false" has no effect; a "false" value does not
 * override the presence of the marker interface. <p>
 * 
 * Note: the DisableEJBStoreForNonDirtyBeans interface is only useful for
 * entity beans with EJB 2.x container managed persistence. It has no effect
 * for entity beans with bean managed or EJB 1.x container managed
 * persistence. <p>
 * 
 * @since WAS 6.0.2
 * @ibm-api
 **/

public interface DisableEJBStoreForNonDirtyBeans
{
    // Empty interface, used only as a marker.
}
