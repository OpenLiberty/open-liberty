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
package com.ibm.ws.security.wim.util;

import java.util.Comparator;

/**
 * The comparator used to compare the length of Strings
 */
public class StringLengthComparator implements Comparator<String> {

    /**
     * @see java.util.Comparator#compare(Object, Object)
     */
    @Override
    public int compare(String s1, String s2) {
        int l1 = s1.length();
        int l2 = s2.length();

        return l2 - l1;
    }
}
