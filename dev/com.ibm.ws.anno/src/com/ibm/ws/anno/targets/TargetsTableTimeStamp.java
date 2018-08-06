/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.targets;

import java.util.logging.Logger;

public interface TargetsTableTimeStamp {

    String getHashText();

    void log(Logger logger);

    //

    String setName(String name);
    String getName();

    String setStamp(String stamp);
    String getStamp();
}
