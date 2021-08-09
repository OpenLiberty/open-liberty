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

public interface PmiModuleAggregate {
    /**
     * Add a child instance
     */
    public void add(PmiModule instance);

    public void remove(PmiModule instance);

    /**
     * Add a list of SpdData from child
     */
    //public void add(SpdData[] dataList);

    public void remove(SpdData[] dataList);

    public boolean remove(SpdData data);

    /**
     * May need other remove methods in the future
     */
}
