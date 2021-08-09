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

public class WpdStat extends WpdDataImpl {
    private static final long serialVersionUID = 8796793171074807073L;
    private int count;
    private double total;
    private double sumOfSquares;

    // constructor:
    public WpdStat(int id, long time, int count, double total, double sumOfSquares) {
        super(id, time);
        this.count = count;
        this.total = total;
        this.sumOfSquares = sumOfSquares;
    }

    public int getCount() {
        return count;
    }

    public double getTotal() {
        return total;
    }

    public double getSumOfSquares() {
        return sumOfSquares;
    }

    public String toXML() {
        String res = PmiConstants.XML_INT + PmiConstants.XML_ID + id
                     + PmiConstants.XML_TIME + time + PmiConstants.XML_COUNT + count
                     + PmiConstants.XML_TOTAL + total
                     + PmiConstants.XML_SUMOFSQUARES + sumOfSquares
                     + PmiConstants.XML_ENDTAG;
        return res;
    }

    public String toString() {
        return "Data Id=" + id + " time=" + time + " count=" + count
                + " total=" + total + " sumOfSquares=" + sumOfSquares;
    }

    public void combine(WpdData other) {
        if (other == null)
            return;
        if (!(other instanceof WpdStat)) {
            System.err.println("WpdStat.combine: wrong type. WpdStat is needed!");
        } else {
            WpdStat otherStat = (WpdStat) other;
            count += otherStat.getCount();
            total += otherStat.getTotal();
            sumOfSquares += otherStat.getSumOfSquares();
        }
    }
}
