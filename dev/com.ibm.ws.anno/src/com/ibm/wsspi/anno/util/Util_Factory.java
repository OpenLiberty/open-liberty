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

import com.ibm.wsspi.anno.util.Util_InternMap.ValueType;

public interface Util_Factory {
    String getHashText();

    Set<String> createIdentityStringSet();

    Util_InternMap createInternMap(ValueType valueType, String name);

    Util_BidirectionalMap createBidirectionalMap(ValueType holderType, String holderTag,
                                                 ValueType heldType, String heldTag);

    Util_BidirectionalMap createBidirectionalMap(ValueType holderType, String holderTag,
                                                 ValueType heldType, String heldTag,
                                                 boolean isEnabled);
}
