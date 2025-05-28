/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.annocache.util;

import java.util.Set;
import java.util.logging.Logger;

// Implementation intention:
//
// className -> Set<annotationName> : tell what annotations a class has
// annotationName -> Set<className> : tell what classes have an annotation

public interface Util_BidirectionalMap extends com.ibm.wsspi.anno.util.Util_BidirectionalMap {
    String getHashText();

    String getHolderTag();
    String getHeldTag();

    void logState();
    void log(Logger logger);

    //

    Util_Factory getFactory();

    boolean getIsEnabled();
    
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
