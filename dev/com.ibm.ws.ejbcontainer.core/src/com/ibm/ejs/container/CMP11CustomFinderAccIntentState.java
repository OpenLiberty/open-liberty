/*******************************************************************************
 * Copyright (c) 1997, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

public class CMP11CustomFinderAccIntentState {
    String methodName;
    boolean customFinderWithUpdateIntent;
    boolean readOnlyAttr;

    public CMP11CustomFinderAccIntentState(String mName,
                                           boolean cfwupdateintent,
                                           boolean readonly)
    {
        methodName = mName;
        customFinderWithUpdateIntent = cfwupdateintent;
        readOnlyAttr = readonly;
    }

    public boolean isCustomFinderWithUpdateIntent() {
        return customFinderWithUpdateIntent;
    }

    public boolean isReadOnly() {
        return readOnlyAttr;
    }

    public String getCustomFinderMethodname() {
        return methodName;
    }

    public String toString() {
        return "[" + methodName + " RO " + String.valueOf(readOnlyAttr) + " CFRO " + String.valueOf(customFinderWithUpdateIntent) + "]";
    }
}
