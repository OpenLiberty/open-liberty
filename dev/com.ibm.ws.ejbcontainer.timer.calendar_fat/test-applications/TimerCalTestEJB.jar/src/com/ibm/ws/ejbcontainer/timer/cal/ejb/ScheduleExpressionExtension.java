/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.cal.ejb;

import javax.ejb.ScheduleExpression;

public class ScheduleExpressionExtension extends ScheduleExpression {

    private static final long serialVersionUID = 1L;

    private String ivEon;
    private String ivEra;
    private String ivPeriod;
    private String ivEpoch;
    private String ivAge;

    public String getIvEon() {
        return ivEon;
    }

    public void setIvEon(String ivEon) {
        this.ivEon = ivEon;
    }

    public String getIvEra() {
        return ivEra;
    }

    public void setIvEra(String ivEra) {
        this.ivEra = ivEra;
    }

    public String getIvPeriod() {
        return ivPeriod;
    }

    public void setIvPeriod(String ivPeriod) {
        this.ivPeriod = ivPeriod;
    }

    public String getIvEpoch() {
        return ivEpoch;
    }

    public void setIvEpoch(String ivEpoch) {
        this.ivEpoch = ivEpoch;
    }

    public String getIvAge() {
        return ivAge;
    }

    public void setIvAge(String ivAge) {
        this.ivAge = ivAge;
    }

    public ScheduleExpressionExtension() {
        this.ivEon = "uninitialized";
        this.ivEra = "uninitialized";
        this.ivPeriod = "uninitialized";
        this.ivEpoch = "uninitialized";
        this.ivAge = "uninitialized";
    }

    public ScheduleExpressionExtension(String eon, String era, String period, String epoch, String age) {

        this.ivEon = eon;
        this.ivEra = era;
        this.ivPeriod = period;
        this.ivEpoch = epoch;
        this.ivAge = age;

    }

}
