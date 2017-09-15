/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.pmi.server;

public interface SpdDouble extends SpdData {

    // set the value
    public void set(double val);

    // increment the value by 1
    public void increment();

    // increment the value by val
    public void increment(double val);

    // decrement the value by 1
    public void decrement();

    // decrement the value by val
    public void decrement(double val);

    // combine the value of this data and other data
    public void combine(SpdDouble other);

}
