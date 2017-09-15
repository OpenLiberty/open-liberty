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
package com.ibm.ws.jsp.translator.visitor.tagfilescan;

import javax.servlet.jsp.tagext.TagInfo;

import com.ibm.ws.jsp.translator.visitor.JspVisitorResult;

public class TagFileScanResult extends JspVisitorResult {
    TagInfo ti = null;
    
    public TagFileScanResult(String jspVisitorId) {
        super(jspVisitorId);
    }
    
    public TagInfo getTagInfo() {
        return (ti);
    }
    
    void setTagInfo(TagInfo ti) {
        this.ti = ti;
    }
}
