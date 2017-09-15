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
package com.ibm.ws.jsp.translator.visitor.validator;

import java.util.Map;

import com.ibm.ws.jsp.translator.visitor.JspVisitorResult;

public class TagCountResult extends JspVisitorResult {
    private Map countMap = null;
    
    protected TagCountResult(String jspVisitorId, Map countMap) {
        super(jspVisitorId);
        this.countMap = countMap;
    }

    public Map getCountMap() {
        return countMap;
    }
}
