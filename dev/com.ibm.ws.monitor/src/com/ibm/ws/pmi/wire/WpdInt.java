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

public class WpdInt extends WpdDataImpl {
    private static final long serialVersionUID = -557634123546071193L;
    private int value;

    // constructor:
    public WpdInt(int id, long time, int value) {
        super(id, time);
        this.value = value;
    }

    public int getIntValue() {
        return value;
    }

    public String toXML() {
        String res = PmiConstants.XML_INT + PmiConstants.XML_ID + id
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
        if (!(other instanceof WpdInt)) {
            System.err.println("WpdInt.combine: wrong type. WpdInt is needed!");
        } else {
            WpdInt otherInt = (WpdInt) other;
            value += otherInt.getIntValue();
        }
    }
}
