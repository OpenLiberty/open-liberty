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
package com.ibm.ws.jsp.translator.visitor.smap;

import com.ibm.ws.jsp.translator.utils.SmapGenerator;
import com.ibm.ws.jsp.translator.visitor.JspVisitorResult;

public class SmapVisitorResult extends JspVisitorResult {
    protected SmapGenerator smapGenerator = null;
    
    public SmapVisitorResult(String jspVisitorId, SmapGenerator smapGenerator) {
        super(jspVisitorId);
        this.smapGenerator = smapGenerator;
    }
    
    public SmapGenerator getSmapGenerator() {
        return (smapGenerator);
    }
}
