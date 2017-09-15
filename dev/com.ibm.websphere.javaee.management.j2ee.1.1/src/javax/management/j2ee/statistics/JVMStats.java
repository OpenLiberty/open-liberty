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
 * Specifies the statistics provided by a Java VM.
 */
public interface JVMStats extends Stats {

    /*
     * Returns the amount of time the JVM has been running.
     */
    public CountStatistic getUpTime();

    /*
     * Returns the size of the JVM’s heap.
     */
    public BoundedRangeStatistic getHeapSize();

}
