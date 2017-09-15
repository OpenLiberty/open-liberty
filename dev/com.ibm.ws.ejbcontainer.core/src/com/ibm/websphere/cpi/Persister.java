/*******************************************************************************
 * Copyright (c) 1998, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cpi;

import javax.ejb.DuplicateKeyException;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.EntityBeanO;

/**
 * The persister interface seen by the container.
 */
public interface Persister {

    /**
     * Create the specified EntityBean in the persistent
     * store associated with this persister.
     * 
     * The persister assumes that the given EntityBean has
     * all its container managed fields set correctly.
     * 
     * @exception DuplicateKeyException thrown if an entity
     *                bean with the same primary key exists in the persistent
     *                store.
     */

    public void create(javax.ejb.EntityBean eb)
                    throws Exception;

    /**
     * Remove the specified EntityBean from the persistent
     * store associated with this persister.
     * 
     * @exception Exception will be thrown if the bean does not
     *                exist in the persistent store.
     */
    public void remove(javax.ejb.EntityBean eb) throws Exception;

    /**
     * Load the given EntityBean with the persistent
     * state associated with the given primary key.
     * 
     * @param eb the EntityBean to load.
     * 
     * @param primaryKey the Object containing the
     *            primary key that identifies the persistent state to load.
     * 
     * @param forUpdate a boolean indicating if this bean
     *            being loaded for update or not.
     */
    public void load(javax.ejb.EntityBean eb, Object primaryKey,
                     boolean forUpdate)
                    throws Exception;

    /**
     * Refresh the given EntityBean with the persistent
     * state associated with the primary key defined in the given
     * EntityBean.
     * 
     * @param eb the EntityBean to refresh.
     * 
     * @param forUpdate a boolean indicating if this bean
     *            being refreshed for update or not.
     */

    public void refresh(javax.ejb.EntityBean eb, boolean forUpdate)
                    throws Exception;

    /**
     * Set the given EntityBean's container managed fields
     * from the given data from persistent storage and given primary key.
     * 
     * @param eb the EntityBean to hydrate.
     * 
     * @param storageData an Object containing the data
     *            from the perisistent store to use in filling in the
     *            container managed fields.
     * 
     * @param key an Object containing the primary key of
     *            the bean being hydrated.
     */

    public void hydrate(javax.ejb.EntityBean eb, Object storageData,
                        Object pkey)
                    throws Exception;

    public Object getPrimaryKey(Object beanData) throws Exception;

    /**
     * Extract and store the CMP fields of the
     * given EntityBean in the persistent store.
     * 
     * @param eb the EntityBean to store.
     * 
     */
    public void store(javax.ejb.EntityBean eb) throws Exception;

    /**
     * Set the home associated with this persister instance.
     * 
     * @param h The PersisterHome associated with this persister.
     *            PersisterHome provides services to the persister
     *            related to this bean's home.
     */
    public void setHome(PersisterHome h);

    /**
     * Return true if the backend supports obtaining an update lock
     * on query statements. It's a consequence of the existing
     * design that this method is exposed in this
     * interface; ideally. the persister should make decisions internally
     * based on the capability of its backend. However, this cannot
     * be changed without code gen changes ... so we have to wait for
     * Aquila.
     */
    public boolean dbSupportsSelectForUpdate()
                    throws Exception;

    /**
     * Must be called when storing a CMP EntityBean and pessimistic
     * concurrency control is used. This method determines if a potential
     * lost update or deadlock scenario has occured. If so, this method
     * must log a warning message so that bean provider knows what to
     * change to avoid these potential problems.
     * <p>
     * <b>pre-conditions</b>
     * <ol>
     * <li> the beanId parameter is for a CMP EntityBean.
     * <li> pessimistic concurrency control is being used.
     * </ul>
     * 
     * @param beanId is the BeanId for the CMP EntityBean.
     *            See pre-conditions 1 and 2.
     * @param loadedForUpdate must be boolean true if the CMP EntityBean
     *            was loaded for update (eg. FOR UPDATE keywords used on SELECT).
     */
    public void checkCMPStoreOperation(BeanId beanId, boolean loadedForUpdate)
                    throws Exception; /* p111132.1 */

    /**
     * Set context data currently associated with the calling thread
     * to a specified ContainerManagedBeanO object that will be used
     * by the getDataFromCache and putDataIntoCache methods. This method
     * can be called with a null reference to clear the context data.
     * <p>
     * NOTE: this data cache is only used for EJB 1.1 support of
     * optimistic concurrency control.
     */
    public void setContextData(EntityBeanO beanO); /* p111083.3 */

    /**
     * Return data previously cached by the putDataInCache method.
     * <p>
     * NOTE: this data cache is only used for EJB 1.1 support of
     * optimistic concurrency control.
     */
    public Object[] getDataFromCache(); /* p111083.3 */

    /**
     * Put data from a CMP entity bean into the data cache for the bean.
     * <p>
     * NOTE: this data cache is only used for EJB 1.1 support of
     * optimistic concurrency control. The data put into the cache
     * is an object array used to store all the data needed for
     * the where predicate on a SQL update statement.
     */
    public void putDataIntoCache(Object[] objectArray); /* p111083.3 */

}
