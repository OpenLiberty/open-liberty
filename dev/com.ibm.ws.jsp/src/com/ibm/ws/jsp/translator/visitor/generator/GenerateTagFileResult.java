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
package com.ibm.ws.jsp.translator.visitor.generator;

import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.jsp.translator.visitor.JspVisitorResult;

public class GenerateTagFileResult extends JspVisitorResult {
    private Map customTagMethodJspIdMap = new HashMap(); //232818
    private int tagMethodLineNumber=0; //PM12658
    public GenerateTagFileResult(String jspVisitorId) {
        super(jspVisitorId);
    }
    /**
     * @return Returns the customTagMethodJspIdMap.
     */
    public Map getCustomTagMethodJspIdMap() { //232818
        return customTagMethodJspIdMap; //232818
    }
    
    //PM12658 start
    /**
     * @return Returns the tagMethodLineNumber.
     */
    public int getTagMethodLineNumber() {
        return tagMethodLineNumber;
    }
	
    /**
     * @param tagMethodLineNumber The tagMethodLineNumber to set.
     */
    public void setTagMethodLineNumber(int tagMethodLineNumber) {
        this.tagMethodLineNumber = tagMethodLineNumber;
    }
    //PM12658 end
}
