// IBM Confidential
//
// OCO Source Materials
//
// Copyright IBM Corp. 2013
//
// The source code for this program is not published or otherwise divested 
// of its trade secrets, irrespective of what has been deposited with the 
// U.S. Copyright Office.
//
// Change Log:
//  Date       pgmr       reason   Description
//  --------   -------    ------   ---------------------------------
//  01/07/03   jitang	  d155877  create
//  03/10/03   jitang     d159967  Fix some java doc problem
//  ----------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.core;

/**
 * This class represents the connection handle states. A connection handle can
 * be in one of the following three states: active, inactive and closed.<p>
 */
public interface HandleStates {
    /** Connection handle state constants. */
    public static final int
                    ACTIVE = 0,
                    INACTIVE = 1,
                    CLOSED = 2;

    /** List of Connection handle state names. */
    public static final String[] STATE_STRINGS = new String[]
    { "ACTIVE", "INACTIVE", "CLOSED" };
}