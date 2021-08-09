/*******************************************************************************
 * Copyright (c) 1998, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cpi;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Enumeration;

import javax.ejb.FinderException;

import com.ibm.ejs.container.BeanMetaData;

/**
 * A PersisterFactory creates a new Persister
 * instance for the bean described by the given metadata.
 * 
 * This class also contains some utility methods to wrap the results
 * returned by the finder into a form suitable for return to the caller.
 * These methods are a consequence of the existing
 * persister design which passes the results of finders to the home
 * for wrapping. The home will then delegate to these utility methods.
 * It would be better to internalize this wrapping in the persister
 * implementation. However, since such a change would require
 * modifications to generated code, we defer it to Aquila (and to the
 * CPI supporting EJB 2.0).
 */
public interface PersisterFactory
{
    /**
     * Create a new Persister instance for the bean
     * descibed by the given config data. <p>
     * 
     * @param bmd the BeanMetaData
     * 
     * @param persisterConfig persister configuration data
     * 
     * @return a Persister instance that may be used
     *         to persist EJB instances described by the given
     *         config data.
     * 
     * @throws CPIException thrown if the persister cannot be created.
     */
    public Persister create(BeanMetaData bmd,
                            PersisterConfigData persisterConfig)
                    throws CPIException;

    /**
     * This utility method will be called when the finder passes the
     * home the results of a find on a CMP returning an Enumeration.
     * 
     * @param txInfo (PersisterTx) is used to determine when the tx was started,
     *            so a decision can be made about lazy or greedy activation.
     * 
     * @param home (PersisterHome) provides services to activate beans
     *            or wrappers to "flesh out" the results of the finder.
     * 
     * @param finder (Finder) passed by persister, opaque to container.
     * 
     * @return an Enumeration containing the EJBObjects corresponding
     *         to the results returned by the finder.
     */
    public Enumeration wrapResultsInEnumeration(PersisterTx txInfo,
                                                PersisterHome homeInfo,
                                                Finder finder)
                    throws RemoteException, FinderException;

    /**
     * This utility method will be called when the finder passes the
     * home the results of a find on a BMP returning an Enumeration.
     * 
     * @param txInfo (PersisterTx) is used to determine when the tx was started,
     *            so a decision can be made about lazy or greedy activation.
     * 
     * @param home (PersisterHome) provides services to activate beans
     *            or wrappers to "flesh out" the results of the finder.
     * 
     * @param keys An Enumeration of the keys returned by the BMP find.
     * 
     * @return an Enumeration containing the EJBObjects corresponding
     *         to the results returned by the finder.
     */
    public Enumeration wrapResultsInEnumeration(PersisterTx txInfo,
                                                PersisterHome homeInfo,
                                                Enumeration keys)
                    throws RemoteException, FinderException;

    /**
     * This utility method will be called when the finder passes the
     * home the results of a find on a CMP returning a Collection.
     * 
     * @param txInfo (PersisterTx) is used to determine when the tx was started,
     *            so a decision can be made about lazy or greedy activation.
     * 
     * @param home (PersisterHome) provides services to activate beans
     *            or wrappers to "flesh out" the results of the finder.
     * 
     * @param finder (Finder) passed by persister, opaque to container.
     * 
     * @return a Collection containing the EJBObjects corresponding
     *         to the results returned by the finder.
     */
    public Collection wrapResultsInCollection(PersisterTx txInfo,
                                              PersisterHome homeInfo,
                                              Finder finder)
                    throws RemoteException, FinderException;

    /**
     * This utility method will be called when the finder passes the
     * home the results of a find on a BMP returning a Collection.
     * 
     * @param txInfo (PersisterTx) is used to determine when the tx was started,
     *            so a decision can be made about lazy or greedy activation.
     * 
     * @param home (PersisterHome) provides services to activate beans
     *            or wrappers to "flesh out" the results of the finder.
     * 
     * @param keys a Collection of the keys returned by the BMP find.
     * 
     * @return a Collection containing the EJBObjects corresponding
     *         to the results returned by the finder.
     */
    public Collection wrapResultsInCollection(PersisterTx txInfo,
                                              PersisterHome homeInfo,
                                              Collection keys)
                    throws RemoteException, FinderException;
}
