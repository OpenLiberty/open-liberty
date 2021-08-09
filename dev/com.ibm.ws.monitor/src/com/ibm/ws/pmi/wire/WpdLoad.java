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

public class WpdLoad extends WpdDataImpl {
    private static final long serialVersionUID = -1608134391368458856L;
    private long createTime;
    private double currentLevel;
    private double integral;

    public WpdLoad(int id, long time, double currentLevel, double integral, long createTime) {
        super(id, time);
        this.currentLevel = currentLevel;
        this.integral = integral;
        this.createTime = createTime;
    }

    public double getLastValue() {
        return currentLevel;
    }

    public double getIntegral() {
        return integral;
    }

    public long getCreateTime() {
        return createTime;
    }

    public String toXML() {
        String res = PmiConstants.XML_INT + PmiConstants.XML_ID + id
                     + PmiConstants.XML_TIME + time
                     + PmiConstants.XML_LASTVALUE + currentLevel
                     + PmiConstants.XML_INTEGRAL + integral
                     + PmiConstants.XML_CREATETIME + createTime
                     + PmiConstants.XML_ENDTAG;
        return res;
    }

    public String toString() {
        return "Data Id=" + id + " time=" + time + " currentLevel=" + currentLevel
                + " integral=" + integral + " createTime=" + createTime;
    }

    public void combine(WpdData other) {
        if (other == null)
            return;
        if (!(other instanceof WpdLoad)) {
            System.err.println("WpdLoad.combine: wrong type. WpdLoad is needed!");
        } else {
            WpdLoad otherLoad = (WpdLoad) other;

            // accumulate currentLevel
            currentLevel += otherLoad.getLastValue();

            // Note: it is difficult to get accurate aggregation value because creatTime/timestamp
            //       may be different betweent this CpdLoad and parameter other.
            //
            // The combine method uses the following algorithm:
            // If other's timeWeight is bigger than this timeweight,
            // only count part of its value; otherwise, use other's timeWeight.
            double timeWeight = time - createTime;
            double timeWeight2 = otherLoad.getTime() - otherLoad.getCreateTime();
            if (timeWeight2 <= timeWeight)
                integral += otherLoad.getIntegral();
            else {
                integral += otherLoad.getIntegral() * (timeWeight / timeWeight2);
            }
        }
    }
}
