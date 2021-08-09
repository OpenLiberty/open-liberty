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

public class WpdDouble extends WpdDataImpl {
    private static final long serialVersionUID = 2706590952848330822L;
    private double value;

    // constructor:
    public WpdDouble(int id, long time, double value) {
        super(id, time);
        this.value = value;
    }

    public double getDoubleValue() {
        return value;
    }

    public String toXML() {
        String res = PmiConstants.XML_DOUBLE + PmiConstants.XML_ID + id
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
        if (!(other instanceof WpdDouble)) {
            System.err.println("WpdDouble.combine: wrong type. WpdDouble is needed!");
        } else {
            WpdDouble otherDouble = (WpdDouble) other;
            value += otherDouble.getDoubleValue();
        }
    }
}
