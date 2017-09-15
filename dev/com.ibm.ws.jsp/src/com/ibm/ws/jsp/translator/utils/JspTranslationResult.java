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
package com.ibm.ws.jsp.translator.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.jsp.Constants;
import com.ibm.wsspi.jsp.compiler.JspCompilerResult;
import com.ibm.wsspi.jsp.compiler.JspLineId;
import com.ibm.wsspi.jsp.resource.translation.JspResources;

public class JspTranslationResult {
    private Map smapGeneratorMap = null;
    private Collection jspLineIds = null;
    private List tagFileDependencyList = null;
    private JspCompilerResult tagFileCompileResult = null;
    
    public JspTranslationResult() {
        jspLineIds = new ArrayList();
        tagFileDependencyList = new ArrayList();
    }
    
    void addSmapGenerator(String resourceId, SmapGenerator smapGenerator) {
        if (smapGeneratorMap == null) {
            smapGeneratorMap = new HashMap();            
        }
        smapGeneratorMap.put(resourceId, smapGenerator);    
    }
    
    public boolean hasSmap() {
        return (!(smapGeneratorMap == null));
    }
    
    void addJspLineIds(JspResources jspResources, Document document) {
        loadJspIdList(jspResources, document.getDocumentElement(), jspLineIds);
    }
    
    public Collection getJspLineIds() {
        return jspLineIds;
    }

    public SmapGenerator getSmapGenerator(String resourceId) {
        return (SmapGenerator)smapGeneratorMap.get(resourceId);
    }
    
    public List getTagFileDependencyList() {
        return tagFileDependencyList;
    }
    
    public JspCompilerResult getTagFileCompileResult() {
        return tagFileCompileResult;
    }
    
    public void setTagFileCompileResult(JspCompilerResult tagFileCompileResult) {
        this.tagFileCompileResult = tagFileCompileResult;
    }
    
    public static void loadJspIdList(JspResources jspResources, Element element, Collection jspLineIds) {
        Attr jspIdAttr = element.getAttributeNodeNS(Constants.JSP_NAMESPACE, "id");
        if (jspIdAttr != null) {
            JspId jspId = new JspId(jspIdAttr.getValue());
            JspLineId lineId = new JspLineId(jspId.getFilePath(),
                                             jspResources.getGeneratedSourceFile().getPath(), 
								    		 jspResources.getInputSource().getRelativeURL(), // defect 203009 
                                             jspId.getStartSourceLineNum(),
                                             jspId.getStartSourceColNum(),
                                             jspId.getSourceLineCount(),
                                             jspId.getEndSourceLineNum(),
                                             jspId.getEndSourceColNum(),
                                             jspId.getStartGeneratedLineNum(),
                                             jspId.getStartGeneratedLineCount(),
                                             jspId.getEndGeneratedLineNum(),
                                             jspId.getEndGeneratedLineCount());
            jspLineIds.add(lineId);                
        }
        
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n instanceof Element) {
                Element childElement = (Element) n;
                loadJspIdList(jspResources, childElement, jspLineIds);
            }
        }
    }
}
