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

/**
 *
 */
public class LogTraceData extends GenericData {
    private Integer LevelValue;
    private GenericData genData;

    /**
     *
     */
    public LogTraceData(GenericData genData) {
        this.setGenData(genData);
        // TODO Auto-generated constructor stub
    }

    /**
    *
    */
    public LogTraceData() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @return the levelValue
     */
    public Integer getLevelValue() {
        return LevelValue;
    }

    /**
     * @param levelValue the levelValue to set
     */
    public void setLevelValue(Integer levelValue) {
        LevelValue = levelValue;
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

}
