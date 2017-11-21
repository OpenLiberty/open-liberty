/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.injectparameters;

import java.util.List;

public class TestUtils {

    public static String join(List<String> stringList) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String string : stringList) {
            if (!first) {
                sb.append(", ");
            } else {
                first = false;
            }
            sb.append(string);
        }
        return sb.toString();
    }
}
