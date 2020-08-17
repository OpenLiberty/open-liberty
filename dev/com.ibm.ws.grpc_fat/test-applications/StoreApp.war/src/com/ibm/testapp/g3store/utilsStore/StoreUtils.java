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
package com.ibm.testapp.g3store.utilsStore;

public class StoreUtils {

    public static boolean isBlank(String str) {
        boolean isBlank = false;

        if (str == null || str.trim().length() == 0) {
            isBlank = true;
        }
        return isBlank;
    }

    public static boolean isBlank(Object obj) {
        boolean isBlank = false;

        if (obj == null) {
            isBlank = true;
        }
        if (obj instanceof String) {
            if (((String) obj).trim().length() == 0) {
                isBlank = true;
            }
        }
        return isBlank;
    }

}
