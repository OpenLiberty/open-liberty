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

import com.ibm.websphere.pmi.*;

public class WpdLong extends WpdDataImpl {
    private static final long serialVersionUID = -669579073691504835L;
    private long value;

    // constructor:
    public WpdLong(int id, long time, long value) {
        super(id, time);
        this.value = value;
    }

    public long getLongValue() {
        return value;
    }

    public String toXML() {
        String res = PmiConstants.XML_LONG + PmiConstants.XML_ID + id
                     + PmiConstants.XML_TIME + time + PmiConstants.XML_VALUE + value
                     + PmiConstants.XML_ENDTAG;
        return res;
    }

    public String toString() {
        return "Data Id=" + id + " time=" + time + " value=" + value;
    }

    public void combine(WpdData other) {
        if (other == null)
            return;
        if (!(other instanceof WpdLong)) {
            System.err.println("WpdLong.combine: wrong type. WpdLong is needed!");
        } else {
            WpdLong otherLong = (WpdLong) other;
            value += otherLong.getLongValue();
        }
    }
}
