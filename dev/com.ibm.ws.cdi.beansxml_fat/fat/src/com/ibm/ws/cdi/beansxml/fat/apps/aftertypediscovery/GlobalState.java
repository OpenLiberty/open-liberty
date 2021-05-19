/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.fat.apps.aftertypediscovery;

import java.util.LinkedList;
import java.util.List;

public class GlobalState {

    private static List<String> output = new LinkedList<String>();

    public static List<String> getOutput() {
        System.out.println("out" + output.size());
        return output;
    }

    public static void addOutput(String s) {
        System.out.println(s);
        output.add(s);
    }

}
