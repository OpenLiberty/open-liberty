/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.jsx.tags;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

public class CallTagTEI extends TagExtraInfo {
    public VariableInfo[] getVariableInfo(TagData data) {
        if (data.getAttributeString("id") != null) {
            VariableInfo info1 = new VariableInfo(data.getAttributeString("id"), "String", true, VariableInfo.AT_END);
            VariableInfo[] info = { info1 };
            return info;
        }
        else {
            VariableInfo[] info = {};
            return info;
        }
    }
}
