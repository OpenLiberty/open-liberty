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
package com.ibm.ws.cache.util;

public class AssertUtility {

    //---------------------------------------------------------------------
    // DynaCache Component assert checking - only call from within CTORs
    //---------------------------------------------------------------------
    static public boolean assertCheck(boolean assertRanOnce, Object clazz) {
        if ( !assertRanOnce ) {
            assert assertRanOnce = true;
            if ( assertRanOnce ) {
                System.out.println("A S S E R T S  A R E  A C T I V E  IN: "+clazz.getClass()+"@@"+clazz.hashCode() );
            }
        }
        return true;
    }


}


