/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.management.j2ee.statistics;

/**
 * The EJBStats interface specifies statistics provided by all EJB component types.
 */
public interface EJBStats extends Stats {

    /*
     * Returns the number of times the beans create method was called.
     */
    public CountStatistic getCreateCount();

    /*
     * Returns the number of times the beans remove method was called.
     */
    public CountStatistic getRemoveCount();

}
