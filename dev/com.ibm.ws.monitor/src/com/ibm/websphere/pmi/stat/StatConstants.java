/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.pmi.stat;

/**
 * Constants for PMI Stats
 * 
 * @ibm-api
 */
public interface StatConstants {
    /** Represents a set in which all statistics in the server are disabled */
    public static final String STATISTIC_SET_NONE = "none";

    /** Represents a set in which the J2EE 1.4 + some top statistics are enabled */
    public static final String STATISTIC_SET_BASIC = "basic";

    /** Represents a set in which the statistic from Basic set + some important statistics from WebSphere components are enabled */
    public static final String STATISTIC_SET_EXTENDED = "extended";

    /** WebSphere performance statistics can be enabled using sets. Set "ALL" represents a set in which all the statistics are enabled */
    public static final String STATISTIC_SET_ALL = "all";

    /** Represents a customized set that is enabled using fine-grained control */
    public static final String STATISTIC_SET_CUSTOM = "custom";
}
