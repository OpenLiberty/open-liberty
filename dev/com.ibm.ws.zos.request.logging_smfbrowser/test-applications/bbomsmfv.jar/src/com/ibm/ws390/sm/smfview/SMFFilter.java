/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws390.sm.smfview;

/**
 * Interface definition for Plug-Ins
 *
 */
public interface SMFFilter {

    /**
     * Called during initialization of browser.
     * 
     * @param parms The text from the PLUGIN keyword after the comma
     * @return true if initializatin is successful, false if not
     */
    public abstract boolean initialize(String parms);

    /**
     * Called for each record found. Only the record header has been parsed
     * so you can examine the type/subtype
     * 
     * @param record The record being processed
     * @return true if this record should be parsed, false if not
     */
    public abstract boolean preParse(SmfRecord record);

    /**
     * Called to parse the record
     * 
     * @param record The record to parse
     * @return The parsed record
     */
    public abstract SmfRecord parse(SmfRecord record);

    /**
     * Called after parsing is complete. This is an opportunity to format
     * the whole record, parts of it, or just examine it
     * 
     * @param record the parsed record
     */
    public abstract void processRecord(SmfRecord record);

    /**
     * Called when all records have been processed. Print summary
     * data or whatever else you like.
     *
     */
    public abstract void processingComplete();

}
