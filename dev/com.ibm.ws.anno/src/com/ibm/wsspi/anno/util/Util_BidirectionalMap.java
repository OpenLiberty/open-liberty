/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

package com.ibm.wsspi.anno.util;

import java.util.Set;
import java.util.logging.Logger;

// Implementation intention:
//
// className -> Set<annotationName> : tell what annotations a class has
// annotationName -> Set<className> : tell what classes have an annotation

public interface Util_BidirectionalMap {

    String getHashText();

    String getHolderTag();
    String getHeldTag();

    void logState();
    void log(Logger logger);

    //

    @Deprecated
    boolean IS_ENABLED = true;

    @Deprecated
    boolean IS_NOT_ENABLED = false;

    @Deprecated
    boolean getEnabled();

    //

    Util_Factory getFactory();

    //

    Util_InternMap getHolderInternMap();
    Util_InternMap getHeldInternMap();

    //

    boolean containsHolder(String holdName);
    Set<String> getHolderSet();

    boolean containsHeld(String heldName);
    Set<String> getHeldSet();

    boolean isEmpty();

    boolean holds(String holderName, String heldName);

    Set<String> selectHeldOf(String holderName);
    Set<String> selectHoldersOf(String heldName);

    Set<String> i_selectHeldOf(String i_holderName);
    Set<String> i_selectHoldersOf(String i_heldName);
}
