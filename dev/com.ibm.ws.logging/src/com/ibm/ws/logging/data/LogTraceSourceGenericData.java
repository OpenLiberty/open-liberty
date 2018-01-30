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
public class LogTraceSourceGenericData extends GenericData {
    private Integer LevelValue;

    /**
     *
     */
    public LogTraceSourceGenericData() {
        super();
        // TODO Auto-generated constructor stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.logging.data.GenericData#addPair(java.lang.String, java.lang.String)
     */
    @Override
    public void addPair(String key, String value) {
        // TODO Auto-generated method stub
        super.addPair(key, value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.logging.data.GenericData#addPair(java.lang.String, java.lang.Number)
     */
    @Override
    public void addPair(String key, Number value) {
        // TODO Auto-generated method stub
        super.addPair(key, value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.logging.data.GenericData#addPairs(com.ibm.ws.logging.data.KeyValuePairList)
     */
    @Override
    public void addPairs(KeyValuePairList kvps) {
        // TODO Auto-generated method stub
        super.addPairs(kvps);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.logging.data.GenericData#getPairs()
     */
    @Override
    public ArrayList<Pair> getPairs() {
        // TODO Auto-generated method stub
        return super.getPairs();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.logging.data.GenericData#getSourceType()
     */
    @Override
    public String getSourceType() {
        // TODO Auto-generated method stub
        return super.getSourceType();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.logging.data.GenericData#setSourceType(java.lang.String)
     */
    @Override
    public void setSourceType(String sourceType) {
        // TODO Auto-generated method stub
        super.setSourceType(sourceType);
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

}
