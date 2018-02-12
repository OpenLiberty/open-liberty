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
        this.setGenData(genData);
    }

    /**
     * @return the levelValue
     */
    public Integer getLevelValue() {
        return levelValue;
    }

    /**
     * @param levelValue the levelValue to set
     */
    public void setLevelValue(Integer levelValue) {
        this.levelValue = levelValue;
    }

    /**
     * @return the genData
     */
    public GenericData getGenData() {
        return genData;
    }

    /**
     * @param genData the genData to set
     */
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
        return this.genData.getMessageID();
    }

    @Override
    public String toString() {
        return this.genData.toString();
    }

    /**
     * @return the logLevel
     */
    public String getLogLevel() {
        return logLevel;
    }

    /**
     * @param logLevel the logLevel to set
     */
    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

}
