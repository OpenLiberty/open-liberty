/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.visitor.validator;


import com.ibm.ws.jsp.Constants;

public class ValidateJspResult extends ValidateResult {
    protected String extendsClass = null;
    protected String error = null;
    protected String info = null;
    protected String contentType = null;
    protected int bufferSize = Constants.DEFAULT_BUFFER_SIZE;
    protected boolean genSessionVariable = true;
    protected boolean autoFlush = true;
    protected boolean singleThreaded = false;
    protected boolean isErrorPage = false;
    
    
    public ValidateJspResult(String jspVisitorId) {
        super(jspVisitorId);
    }
    
    /**
     * Returns the autoFlush.
     * @return boolean
     */
    public boolean isAutoFlush() {
        return autoFlush;
    }

    /**
     * Returns the bufferSize.
     * @return int
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Returns the contentType.
     * @return String
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the error.
     * @return String
     */
    public String getError() {
        return error;
    }

    /**
     * Returns the extendsClass.
     * @return String
     */
    public String getExtendsClass() {
        return extendsClass;
    }

    /**
     * Returns the genSessionVariable.
     * @return boolean
     */
    public boolean isGenSessionVariable() {
        return genSessionVariable;
    }

    /**
     * Returns the info.
     * @return String
     */
    public String getInfo() {
        return info;
    }

    /**
     * Returns the isErrorPage.
     * @return boolean
     */
    public boolean isErrorPage() {
        return isErrorPage;
    }

    /**
     * Returns the singleThreaded.
     * @return boolean
     */
    public boolean isSingleThreaded() {
        return singleThreaded;
    }

    /**
     * Sets the autoFlush.
     * @param autoFlush The autoFlush to set
     */
    public void setAutoFlush(boolean autoFlush) {
        this.autoFlush = autoFlush;
    }

    /**
     * Sets the bufferSize.
     * @param bufferSize The bufferSize to set
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Sets the contentType.
     * @param contentType The contentType to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Sets the error.
     * @param error The error to set
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Sets the extendsClass.
     * @param extendsClass The extendsClass to set
     */
    public void setExtendsClass(String extendsClass) {
        this.extendsClass = extendsClass;
    }

    /**
     * Sets the genSessionVariable.
     * @param genSessionVariable The genSessionVariable to set
     */
    public void setGenSessionVariable(boolean genSessionVariable) {
        this.genSessionVariable = genSessionVariable;
    }

    /**
     * Sets the info.
     * @param info The info to set
     */
    public void setInfo(String info) {
        this.info = info;
    }

    /**
     * Sets the isErrorPage.
     * @param isErrorPage The isErrorPage to set
     */
    public void setIsErrorPage(boolean isErrorPage) {
        this.isErrorPage = isErrorPage;
    }

    /**
     * Sets the singleThreaded.
     * @param singleThreaded The singleThreaded to set
     */
    public void setSingleThreaded(boolean singleThreaded) {
        this.singleThreaded = singleThreaded;
    }
    
}
