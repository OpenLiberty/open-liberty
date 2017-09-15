/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.sib;

public interface SIBDestinationReliabilityType {

    String BEST_EFFORT_NONPERSISTENT = "BEST_EFFORT_NONPERSISTENT";
    int BEST_EFFORT_NONPERSISTENT_VALUE = 0;

    String EXPRESS_NONPERSISTENT = "EXPRESS_NONPERSISTENT";
    int EXPRESS_NONPERSISTENT_VALUE = 1;

    String RELIABLE_NONPERSISTENT = "RELIABLE_NONPERSISTENT";
    int RELIABLE_NONPERSISTENT_VALUE = 2;

    String RELIABLE_PERSISTENT = "RELIABLE_PERSISTENT";
    int RELIABLE_PERSISTENT_VALUE = 3;

    String ASSURED_PERSISTENT = "ASSURED_PERSISTENT";
    int ASSURED_PERSISTENT_VALUE = 4;
}