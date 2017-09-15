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
package com.ibm.ws.wsoc;

/**
 *
 */
public interface PathParamData extends Cloneable {
    /**
     * Returns the annotation value
     * 
     * @return the Annotation value
     */
    String getAnnotationValue();

    /**
     * Sets the PathParam annotation value.
     * 
     * @param name the PathParam annotation value
     */
    void setAnnotationValue(String annotationValue);

    /**
     * Returns index of the parameter in the method. First parameter has index 0
     * 
     * @return int index of the parameter in the method
     */
    int getParamIndex();

    /**
     * Sets the index of the parameter in the method. First parameter has index 0
     * 
     * @param index of the parameter in the method
     */
    void setParamIndex(int index);

    /**
     * Returns parameter type @PathParam
     * 
     * @return Class parameter type
     */
    public Class<?> getParamType();

    /**
     * parameter type of @PathParam annotation
     * 
     * @param Class parameter type
     */
    public void setParamType(Class<?> paramType);

    /**
     * Whether @PathParam value matched a ServerEndpoint uri path segment or not
     * 
     * @param boolean
     */
    public boolean isURISegmentMatched();

}
