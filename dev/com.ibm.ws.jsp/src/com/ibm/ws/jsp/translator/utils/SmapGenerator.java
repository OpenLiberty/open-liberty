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
package com.ibm.ws.jsp.translator.utils;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a source map (SMAP), which serves to associate lines
 * of the input JSP file(s) to lines in the generated servlet in the
 * final .class file, according to the JSR-045 spec.
 * 
 * @author Shawn Bayern
 */
public class SmapGenerator {

    //*********************************************************************
    // Overview

    /*
     * The SMAP syntax is reasonably straightforward.  The purpose of this
     * class is currently twofold:
     *  - to provide a simple but low-level Java interface to build
     *    a logical SMAP
     *  - to serialize this logical SMAP for eventual inclusion directly
     *    into a .class file.
     */

    //*********************************************************************
    // Private state

    private String outputFileName;
    private String defaultStratum = "Java";
    private List strata = new ArrayList();
    private List embedded = new ArrayList();
    private boolean doEmbedded = true;

    //*********************************************************************
    // Methods for adding mapping data

    /**
     * Sets the filename (without path information) for the generated
     * source file.  E.g., "foo$jsp.java".
     */
    public synchronized void setOutputFileName(String x) {
        outputFileName = x;
    }

    /**
     * Adds the given SmapStratum object, representing a Stratum with
     * logically associated FileSection and LineSection blocks, to
     * the current SmapGenerator.  If <tt>default</tt> is true, this
     * stratum is made the default stratum, overriding any previously
     * set default.
     *
     * @param stratum the SmapStratum object to add
     * @param defaultStratum if <tt>true</tt>, this SmapStratum is considered
     *                to represent the default SMAP stratum unless
     *                overwritten
     */
    public synchronized void addStratum(SmapStratum stratum, boolean defaultStratum) {
        strata.add(stratum);
        if (defaultStratum)
            this.defaultStratum = stratum.getStratumName();
    }

    /**
     * Adds the given string as an embedded SMAP with the given stratum name.
     *
     * @param smap the SMAP to embed
     * @param stratumName the name of the stratum output by the compilation
     *                    that produced the <tt>smap</tt> to be embedded
     */
    public synchronized void addSmap(String smap, String stratumName) {
        embedded.add("*O " + stratumName + "\n" + smap + "*C " + stratumName + "\n");
    }

    /**
     * Instructs the SmapGenerator whether to actually print any embedded
     * SMAPs or not.  Intended for situations without an SMAP resolver.
     *
     * @param status If <tt>false</tt>, ignore any embedded SMAPs.
     */
    public void setDoEmbedded(boolean status) {
        doEmbedded = status;
    }

    //*********************************************************************
    // Methods for serializing the logical SMAP

    public synchronized String getString() {
        // check state and initialize buffer
        if (outputFileName == null)
            throw new IllegalStateException();
        StringBuffer out = new StringBuffer();

        // start the SMAP
        out.append("SMAP\n");
        out.append(outputFileName + '\n');
        out.append(defaultStratum + '\n');

        // include embedded SMAPs
        if (doEmbedded) {
            int nEmbedded = embedded.size();
            for (int i = 0; i < nEmbedded; i++) {
                out.append(embedded.get(i));
            }
        }

        // print our StratumSections, FileSections, and LineSections
        int nStrata = strata.size();
        for (int i = 0; i < nStrata; i++) {
            SmapStratum s = (SmapStratum) strata.get(i);
            out.append(s.getString());
        }

        // end the SMAP
        out.append("*E\n");

        return out.toString();
    }

    public String toString() {
        return getString();
    }
}
