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

package com.ibm.ws.pmi.wire;

public abstract class WpdDataImpl implements WpdData {
    protected int id;
    protected long time;

    // constructor:
    public WpdDataImpl(int id, long time) {
        this.id = id;
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public int getId() {
        return id;
    }

    public abstract String toXML();

    public abstract void combine(WpdData other);
}
