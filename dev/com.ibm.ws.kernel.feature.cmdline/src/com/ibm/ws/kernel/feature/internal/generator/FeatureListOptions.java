/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.generator;

import java.util.Locale;

/**
 *
 */
public class FeatureListOptions {
    private String encoding;
    private Locale locale = Locale.getDefault();
    private ReturnCode returnCode = ReturnCode.OK;
    private String outputFile;
    private String productName = ManifestFileProcessor.CORE_PRODUCT_NAME;
    private boolean includeInternals = false;

    public enum ReturnCode {
        OK(0),
        // Jump a few numbers for error return codes
        BAD_ARGUMENT(20),
        RUNTIME_EXCEPTION(21),
        PRODUCT_EXT_NOT_FOUND(26),
        PRODUCT_EXT_NOT_DEFINED(27),
        PRODUCT_EXT_NO_FEATURES_FOUND(28),

        // All "actions" should be < 0, these are not returned externally
        HELP_ACTION(-1),
        GENERATE_ACTION(-2);

        final int val;

        ReturnCode(int val) {
            this.val = val;
        }

        public int getValue() {
            return val;
        }
    }

    /**
     * @param locale the locale to set
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * @param encoding the encoding to set
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * @return the encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Saves the product extension name to process.
     * 
     * @param name The product extension name to process.
     */
    public void setProductName(String name) {
        this.productName = name;
    }

    /**
     * Retrieves the set product name.
     * 
     * @return The product name.
     */
    public String getProductName() {
        return productName;
    }

    /**
     * @param returnCode the returnCode to set
     */
    public void setReturnCode(ReturnCode returnCode) {
        this.returnCode = returnCode;
    }

    /**
     * @return the returnCode
     */
    public ReturnCode getReturnCode() {
        return returnCode;
    }

    /**
     * @param outputFile the outputFile to set
     */
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * @return the outputFile
     */
    public String getOutputFile() {
        return outputFile;
    }

    /**
     * @param includeInternals true if internals should be included in the output
     */
    public void setIncludeInternals(boolean includeInternals) {
        this.includeInternals = includeInternals;
    }

    /**
     * @return true if internals should be included in the output
     */
    public boolean getIncludeInternals() {
        return includeInternals;
    }

}
