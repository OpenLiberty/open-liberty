/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.api.statistics;

import java.math.BigInteger;
import java.util.Date;

/**
 * This interface represents access to the data for a single named statistical
 * counter within the component. More information on statistics and how to use
 * them are in the <code>OAuthStatistics</code> interface.
 * 
 * @see OAuthStatistics
 */
public interface OAuthStatistic {

    /**
     * Returns the statistical counter name
     * 
     * @return the statistical counter name
     */
    public String getName();

    /**
     * Returns the total number of transactions since startup for this named
     * statistic
     * 
     * @return the total number of transactions since startup for this named
     *         statistic
     */
    public long getCount();

    /**
     * Returns the sum of all transaction times for all transactions since
     * startup for this named statistic
     * 
     * @return the sum of all transaction times for all transactions since
     *         startup for this named statistic
     */
    public BigInteger getElapsedTime();

    /**
     * Returns the timestamp when this statistic was retrieved
     * 
     * @return the timestamp when this statistic was retrieved
     */
    public Date getTimestamp();

}
