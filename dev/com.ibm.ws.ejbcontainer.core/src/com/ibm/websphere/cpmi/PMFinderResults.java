/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cpmi;

/**
 * PMFinderResults provides the EJB Container a mechanism for notifying
 * the Persitence Manager of how it will use finder method result
 * collections. <p>
 * 
 * If the result collection returned by Persistence Manager implements
 * this interface, then EJB Container will invoke the methods defined
 * to inform the PM of the collection usage patterns.
 **/
public interface PMFinderResults
{
    /**
     * Load intent hint indicating the bean associated with each primary key
     * will be loaded immediately after calling next() on the collection to
     * obtain the key.
     **/
    public final int LOAD_IMMEDIATE = 0;

    /**
     * Load intent hint indicating the bean associated with each primary key
     * will be loaded at a later time after calling next() on the collection to
     * obtain the key.
     **/
    public final int LOAD_DEFERRED = 1;

    /**
     * Load intent hint indicating the bean associated with each primary key
     * will never be loaded after calling next() on the collection to
     * obtain the key.
     **/
    public final int LOAD_NEVER = 2;

    /**
     * Indicates the beginning of a series of (one or more) iterations of the
     * PM finder result collection (the collection of keys). The argument "intent"
     * sets the result collection load hint to be in effect for all iterations in
     * the series. The value indicates when the beans (keys) iterated over will
     * be loaded by Container. The valid values are: <p>
     * 
     * LOAD_IMMEDIATE : Bean will be loaded immediately after obtaining the
     * key (via "next()"). Container will follow each call
     * to "next()" with a call to "ejbLoad(key)" for the key
     * returned by "next()". No other call to "next()"
     * involving this transaction, whether for the same
     * ResultCollection or a different one within this
     * transaction, may occur between the next/ejbLoad pair.
     * One other PM method related to this key value,
     * "getHomeForKey(key)", may be called between the pair. <p>
     * 
     * LOAD_DEFERRED : Bean will be loaded some time after obtaining the key.
     * Other calls to "next()" may occur before the ejbLoad
     * for the returned key. For example, "next()" may be
     * called repeatedly during iteration, but "ejbLoad()"
     * may be deferred until the bean itself is accessed. <p>
     * 
     * LOAD_NEVER : Bean will never be loaded after obtaining the key.
     * No subsequent call to "getHomeForKey(key)" will be
     * made. Therefore, there is no need to keep the data
     * for this bean. It is likely that the returned key
     * will become input to a "findByPrimaryKey(key)" or
     * "ejbLoad()" call in a subsequent transaction. <p>
     * 
     * @param loadIntent the load intent of the EJBContainer for the elements
     *            iterated. Will be set to LOAD_IMMEDIATE,
     *            LOAD_DEFERRED or LOAD_NEVER.
     **/
    public void beginResultsProcessing(int loadIntent);

    /**
     * Indicates the end of a series of (one or more) iterations begun by
     * "beginResultsProcessing(int loadIntent)". <p>
     * 
     * The implementer should return the state of this collection to its
     * default loading behavior (if necessary). <p>
     **/
    public void endResultsProcessing();

}
