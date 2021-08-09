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
package com.ibm.ws.cdi.ejb.apps.managedbean;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CounterUtil {

    private static List<String> msgList = new ArrayList<String>();

    public static void addToMsgList(String str) {
        msgList.add(str);
    }

    public static List<String> getMsgList() {
        return msgList;
    }

}
