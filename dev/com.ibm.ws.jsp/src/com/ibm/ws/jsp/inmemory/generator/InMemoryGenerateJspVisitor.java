/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.inmemory.generator;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.inmemory.resource.InMemoryJspResources;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorUsage;
import com.ibm.ws.jsp.translator.visitor.generator.FragmentHelperClassWriter;
import com.ibm.ws.jsp.translator.visitor.generator.GenerateJspVisitor;
import com.ibm.wsspi.jsp.context.JspCoreContext;

public class InMemoryGenerateJspVisitor extends GenerateJspVisitor {
    
    public InMemoryGenerateJspVisitor(JspVisitorUsage visitorUsage,
                                       JspConfiguration jspConfiguration, 
                                       JspCoreContext context, 
                                       HashMap resultMap,
                                       JspVisitorInputMap inputMap) 
                                       throws JspCoreException {
        super(visitorUsage, jspConfiguration, context, resultMap, inputMap);
    }

    protected void createWriter(String filePath, String className, Map customTagMethodJspIdMap) throws JspCoreException { //232818
        this.filePath = filePath;
        InMemoryJspResources jspFiles = (InMemoryJspResources)inputMap.get("JspFiles");
        Map cdataJspIdMap = (Map)inputMap.get("CdataJspIdMap");
        try {
        	((CharArrayWriter)jspFiles.getGeneratedSourceWriter()).reset();
        	writer = new InMemoryWriter(jspFiles.getGeneratedSourceWriter(), jspElementMap, cdataJspIdMap, customTagMethodJspIdMap);
		} catch (IOException e) {
            throw new JspCoreException(e);
		}
        fragmentHelperClassWriter = new FragmentHelperClassWriter(className);
        boolean reuseTags = false;
        if (jspOptions.isUsePageTagPool() ||
            jspOptions.isUseThreadTagPool()) {
            reuseTags = true;                
        }
        fragmentHelperClassWriter.generatePreamble(reuseTags);
        persistentData.put("pushBodyCountVarDeclarationBase", "_jspx_push_body_count"); // defect 363508
    }
    
}
