/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.csi;

import java.util.List;
import com.ibm.websphere.csi.ActivitySessionAttribute;

public class ActivitySessionMethod
{
    private ActivitySessionAttribute asAttr;
    private List methodElements;

    public ActivitySessionMethod(ActivitySessionAttribute asAttr, List methodElements) {
        this.asAttr = asAttr;
        this.methodElements = methodElements;
    }

    public ActivitySessionAttribute getActivitySessionAttribute() {
        return asAttr;
    }

    public List getMethodElements() {
        return methodElements;
    }
}
