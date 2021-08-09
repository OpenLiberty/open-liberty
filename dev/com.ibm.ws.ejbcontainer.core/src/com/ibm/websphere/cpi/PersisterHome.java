/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cpi;

import javax.ejb.EJBObject;
import javax.ejb.EJBLocalObject; // d111627
import javax.ejb.FinderException;
import java.rmi.RemoteException;
import java.util.*;

/**
 * PersisterHome provides home related services to the persister.
 * 
 * The utility methods to process finder results are present because
 * they are required by the existing generated code. There is no good
 * reason why this should be so and why the home has to mediate in
 * wrapping the finder result. These methods will go away when the
 * full-fledged CPI supporting EJB 2.0 is implemented for Aquila.
 */
public interface PersisterHome
{
    /**
     * Return the <code>EJBObject</code> associated with the specified
     * primary key if it exists within the cache for this home or
     * a subclass/child home if inheritance has been defined.
     * Null will be returned if the bean cannot be found in the cache. <p>
     * 
     * For caching strategies which allow muiltiple instances of the same
     * entity bean (such as Options B and C), then a transactional lookup
     * will occur. In this situation, only beans enlisted in the current
     * transaction will be found. For caching strategies which allow only
     * a single instance of a given bean (such as Option A), a transactional
     * lookup is not perormed, and an <code>EJBObject</code> is returned if
     * the bean is found, even though it may be enlisted with another
     * transaction. The bean will not be enlisted with the current
     * transaction as a result of this method call. Basically, the bean
     * may be found if it may be enlisted with the transaction without
     * being re-loaded from the database. <p>
     * 
     * This method is primarily intended for use by the generated persister
     * code for EJB 1.1 Entity Beans, specifically when inheritance is
     * involved. It provides a mechanism for the generated persister
     * <code>findByPrimaryKey()</code> method to look in the cache
     * prior to performing a database query. When inheritance is not
     * involved, {@link #activateBean activateBean()} should be used
     * instead. <p>
     * 
     * @param primaryKey the <code>Object</code> containing the primary
     *            key of the <code>EJBObject</code> to return.
     * 
     * @return the <code>EJBObject</code> associated with the specified
     *         primary key or null if the bean cannot be found in the cache.
     * 
     * @exception RemoteException thrown if a system exception occurs while
     *                trying to locate the <code>EJBObject</code> instance
     *                corresponding to the primary key within the cache.
     **/
    // d116859
    public EJBObject getBean(Object primaryKey)
                    throws RemoteException;

    /**
     * Return the EJBObject associated with the specified
     * primary key. Do not activate the bean, just return the
     * wrapper instead. Can be used in lazy activation scenarios.
     * 
     * @param primaryKey the Object containing the primary
     *            key of the EJBObject to return.
     * 
     * @return the EJBObject associated with the specified
     *         primary key.
     * 
     * @exception FinderException thrown if a finder-specific
     *                error occurs (such as no object with corresponding
     *                primary key.
     * 
     * @exception RemoteException thrown if a system
     *                exception occurs while trying to activate the
     *                bean instance.
     */
    public EJBObject getBeanWrapper(Object primaryKey)
                    throws FinderException,
                    RemoteException;

    /**
     * Return the EJBObject associated with the specified
     * primary key after activating the bean if necessary.
     * wrapper instead. Can be used in lazy activation scenarios.
     * 
     * @param primaryKey the Object containing the primary
     *            key of the EJBObject to return.
     * 
     * @return the EJBObject associated with the specified
     *         primary key.
     * 
     * @exception FinderException thrown if a finder-specific
     *                error occurs (such as no object with corresponding
     *                primary key.
     * 
     * @exception RemoteException thrown if a system
     *                exception occurs while trying to activate the
     *                bean instance.
     */
    public EJBObject activateBean(Object primaryKey)
                    throws FinderException,
                    RemoteException;

    /**
     * Return the EJBLocalObject associated with the specified
     * primary key after activating the bean if necessary.
     * wrapper instead. Can be used in lazy activation scenarios.
     * 
     * @param primaryKey the Object containing the primary
     *            key of the EJBObject to return.
     * 
     * @return the EJBLocalObject associated with the specified
     *         primary key.
     * 
     * @exception FinderException thrown if a finder-specific
     *                error occurs (such as no object with corresponding
     *                primary key.
     * 
     * @exception RemoteException thrown if a system
     *                exception occurs while trying to activate the
     *                bean instance.
     */
    // d111627
    public EJBLocalObject activateBean_Local(Object primaryKey)
                    throws FinderException,
                    RemoteException;

    /**
     * Return an Enumeration instance that wraps the EJBObjects
     * corresponding to given Finder's result set.
     * 
     * This helper method is used to construct the result enumeration
     * for finder methods on homes that contain container managed entity
     * beans.
     * 
     * This method is used by the persister to wrap its result set.
     * This method will become unnecessary in the CPI redesign for Aquila.
     * Its implementation is expected to delegate to the corresponding
     * utility method on the PersisterFactory.
     * 
     * @param finder The Finder encapsulating the result set returned by
     *            the find on the persister.
     * 
     * @result an Enumeration wrapping the EJBObjects corresponding to
     *         the finder's result set.
     * 
     * @exception FinderException thrown if a finder-specific
     *                error occurs (such as no object with corresponding
     *                primary key.
     * 
     * @exception RemoteException thrown if a system
     *                exception occurs while trying to activate the
     *                bean instance.
     */
    public Enumeration getEnumeration(Finder finder)
                    throws FinderException,
                    RemoteException;

    /**
     * Return an Enumeration instance that wraps the EJBObjects
     * corresponding to the enumerated keys.
     * 
     * This helper method is used to construct the result enumeration
     * for finder methods on homes that contain bean managed entity
     * beans.
     * 
     * This method is used by the persister to wrap its result set.
     * This method will become unnecessary in the CPI redesign for Aquila.
     * Its implementation is expected to delegate to the corresponding
     * utility method on the PersisterFactory.
     * 
     * @param keys The Enumeration encapsulating the primary keys returned by
     *            the find on the persister.
     * 
     * @result an Enumeration wrapping the EJBObjects corresponding to
     *         the keys.
     * 
     * @exception FinderException thrown if a finder-specific
     *                error occurs (such as no object with corresponding
     *                primary key.
     * 
     * @exception RemoteException thrown if a system
     *                exception occurs while trying to activate the
     *                bean instance.
     */
    public Enumeration getEnumeration(Enumeration keys)
                    throws FinderException,
                    RemoteException;

    /**
     * Return a Collection instance that wraps the EJBObjects
     * corresponding to given Finder's result set.
     * 
     * This helper method is used to construct the result collection
     * for finder methods on homes that contain container managed entity
     * beans.
     * 
     * This method is used by the persister to wrap its result set.
     * This method will become unnecessary in the CPI redesign for Aquila.
     * Its implementation is expected to delegate to the corresponding
     * utility method on the PersisterFactory.
     * 
     * @param finder The Finder encapsulating the result set returned by
     *            the find.
     * 
     * @result a Collection wrapping the EJBObjects corresponding to
     *         the finder's result set.
     * 
     * @exception FinderException thrown if a finder-specific
     *                error occurs (such as no object with corresponding
     *                primary key.
     * 
     * @exception RemoteException thrown if a system
     *                exception occurs while trying to activate the
     *                bean instance.
     */
    public Collection getCollection(Finder finder)
                    throws FinderException,
                    RemoteException;

    /**
     * Return a Collection instance that wraps the EJBObjects
     * corresponding to the input Collection's keys.
     * 
     * This helper method is used to construct the result enumeration
     * for finder methods on homes that contain container managed entity
     * beans.
     * 
     * This method is used by the persister to wrap its result set.
     * This method will become unnecessary in the CPI redesign for Aquila.
     * Its implementation is expected to delegate to the corresponding
     * utility method on the PersisterFactory.
     * 
     * @param keys The Collection of primary keys returned by the BMP
     *            find.
     * 
     * @result a Collection wrapping the EJBObjects corresponding to
     *         the primary key Collection which was passed in.
     * 
     * @exception FinderException thrown if a finder-specific
     *                error occurs (such as no object with corresponding
     *                primary key.
     * 
     * @exception RemoteException thrown if a system
     *                exception occurs while trying to activate the
     *                bean instance.
     */
    public Collection getCollection(Collection keys)
                    throws FinderException,
                    RemoteException;

    /**
     * Returns true if inheritance has been defined for this home; otherwise
     * returns false.
     * 
     * Inheritance is defined in the deployment descriptor as
     * 'generalizations', and indicates that the finder methods on the home
     * may return subclasses of the bean class for which this is a home.
     * 
     * This method is intended for use by the FinderHelper to determine if
     * beans need to be activated for a greedy enumerator.
     **/
    // PQ57446
    public boolean hasInheritance();

    // d112604.1 begins
    public boolean isChild(); // d112604.1

    public Object getCurrentThreadDeployedSupport(); // d112604.1

    // d112604.1 ends

    // d112604.5

    /**
     * Allow CMP 11 finder executed in lazy enumeration to acquire method context state
     * if client calls back for more records.
     */

    public void setCustomFinderAccessIntentThreadState(boolean cfwithupdateaccess,
                                                       boolean readonly,
                                                       String methodname);

    /**
     * Allow CMP 11 finder executed in lazy enumeration to reset state
     * if client calls back for more records.
     */

    public void resetCustomFinderAccessIntentContext();

    // d112604.5 ends

}
