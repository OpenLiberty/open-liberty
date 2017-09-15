/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.anno.util;

import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;

// Implementation intention:
//
// className -> Set<annotationName> : tell what annotations a class has
// annotationName -> Set<className> : tell what classes have an annotation

public interface Util_BidirectionalMap {

    String getHashText();

    String getHolderTag();

    String getHeldTag();

    void logState();

    void log(TraceComponent tc);

    //

    Util_Factory getFactory();

    //

    boolean IS_ENABLED = true;
    boolean IS_NOT_ENABLED = false;

    boolean getIsEnabled();

    Util_InternMap getHolderInternMap();

    Util_InternMap getHeldInternMap();

    boolean containsHolder(String holdName);

    Set<String> getHolderSet();

    boolean containsHeld(String heldName);

    Set<String> getHeldSet();

    boolean holds(String holderName, String heldName);

    Set<String> selectHeldOf(String holderName);

    Set<String> selectHoldersOf(String heldName);
}
