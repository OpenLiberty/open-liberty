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
 * The BoundaryStatistic interface specifies standard measurements of the upper and
 * lower limits of the value of an attribute.
 */
public interface BoundaryStatistic extends Statistic {

    /*
     * Returns the upper limit of the value of this attribute.
     */
    public long getUpperBound();

    /*
     * Returns the lower limit of the value of this attribute.
     */
    public long getLowerBound();

}
