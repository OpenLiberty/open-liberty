/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.data;

import java.util.ArrayList;

/**
 *
 */
public class LogTraceData {
    private Integer levelValue;
    private GenericData genData;
    private String logLevel;

    public LogTraceData(GenericData genData) {
        setGenData(genData);
    }

    public Integer getLevelValue() {
        return levelValue;
    }

    public void setLevelValue(Integer levelValue) {
        this.levelValue = levelValue;
    }

    public GenericData getGenData() {
        return genData;
    }

    public void setGenData(GenericData genData) {
        this.genData = genData;
    }

    public ArrayList<Pair> getPairs() {
        return genData.getPairs();
    }

    public String getSourceType() {
        return genData.getSourceType();
    }

    public String getMessageID() {
        return genData.getMessageID();
    }

    @Override
    public String toString() {
        return genData.toString();
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

}
