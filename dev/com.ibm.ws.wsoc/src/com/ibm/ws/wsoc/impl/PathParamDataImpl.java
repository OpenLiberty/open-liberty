/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc.impl;

import com.ibm.ws.wsoc.PathParamData;

/**
 * Holds @PathParam annotation data
 */
public class PathParamDataImpl implements PathParamData, Cloneable {

    private String annotationValue;
    private int index;
    private Class<?> paramType;
    private boolean uriSegmentMatched = false;

    /**
     * @param annotation
     * @param index
     */
    public PathParamDataImpl(String annotationValue, int index, Class<?> paramType, boolean uriSegmentMatched) {
        this.annotationValue = annotationValue;
        this.index = index;
        this.setParamType(paramType);
        this.uriSegmentMatched = uriSegmentMatched;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.PathParamData#getAnnotation()
     */
    @Override
    public String getAnnotationValue() {
        return annotationValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.PathParamData#SetAnnotation(java.lang.annotation.Annotation)
     */
    @Override
    public void setAnnotationValue(String annotationValue) {
        this.annotationValue = annotationValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.PathParamData#getParamIndex()
     */
    @Override
    public int getParamIndex() {
        return index;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.PathParamData#setParamIndex(int)
     */
    @Override
    public void setParamIndex(int index) {
        this.index = index;
    }

    /**
     * @return the paramType
     */
    @Override
    public Class<?> getParamType() {
        return this.paramType;
    }

    /**
     * @param paramType the paramType to set
     */
    @Override
    public void setParamType(Class<?> paramType) {
        this.paramType = paramType;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        PathParamDataImpl clone = (PathParamDataImpl) super.clone();
        // Question: - paramType need cloning ?
        return clone;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsoc.PathParamData#isMatch()
     */
    @Override
    public boolean isURISegmentMatched() {
        return uriSegmentMatched;
    }

}
