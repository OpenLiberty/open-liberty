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

public class GenerateJspResult extends JspVisitorResult {
    private Map customTagMethodJspIdMap = new HashMap(); //232818
    private int serviceMethodLineNumber=0;
    public GenerateJspResult(String jspVisitorId) {
        super(jspVisitorId);
    }
    
	/**
	 * @return Returns the serviceMethodLineNumber.
	 */
	public int getServiceMethodLineNumber() {
		return serviceMethodLineNumber;
	}
	/**
	 * @param serviceMethodLineNumber The serviceMethodLineNumber to set.
	 */
	public void setServiceMethodLineNumber(int serviceMethodLineNumber) {
		this.serviceMethodLineNumber = serviceMethodLineNumber;
	}
	/**
	 * @return Returns the customTagMethodJspIdMap.
	 */
	public Map getCustomTagMethodJspIdMap() { //232818
		return customTagMethodJspIdMap; //232818
	}
}
