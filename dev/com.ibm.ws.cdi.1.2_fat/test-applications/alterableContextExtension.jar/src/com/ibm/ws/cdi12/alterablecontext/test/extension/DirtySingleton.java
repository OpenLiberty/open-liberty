/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.alterablecontext.test.extension;

import java.util.LinkedList;
import java.util.List;

public class DirtySingleton {
    private static List<String> strings = new LinkedList<String>();

    public static List<String> getStrings() {
        return strings;

    }

    public static void addString(String s) {
        strings.add(s);
    }
}
