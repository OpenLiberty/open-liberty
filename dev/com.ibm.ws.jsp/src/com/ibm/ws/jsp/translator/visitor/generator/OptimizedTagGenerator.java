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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.tagext.TagInfo;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.taglib.TagClassInfo;
import com.ibm.ws.jsp.taglib.TagLibraryCache;
import com.ibm.ws.jsp.translator.optimizedtag.OptimizedTag;
import com.ibm.ws.jsp.translator.optimizedtag.OptimizedTagContext;
import com.ibm.ws.jsp.translator.visitor.validator.ValidateResult;
import com.ibm.wsspi.jsp.context.JspCoreContext;

public class OptimizedTagGenerator extends BaseTagGenerator implements TagGenerator, OptimizedTagContext {
    protected MethodWriter tagStartWriter = null;
    protected MethodWriter tagMiddleWriter = null;
    protected MethodWriter tagEndWriter = null;
    protected JavaCodeWriter currentWriter = null;
    protected OptimizedTag optimizedTag = null;
    protected Map attrMap = new HashMap();
    protected List declaredIdList = null;
    protected String tagPushBodyCountVar = null;
    
    public OptimizedTagGenerator(OptimizedTag optimizedTag,
                                 String tagPushBodyCountVar, 
                                 int nestingLevel,
                                 boolean isTagFile,
                                 boolean hasBody,
                                 boolean hasJspBody,
                                 String tagHandlerVar,
                                 Element element,
                                 TagLibraryCache tagLibraryCache,
                                 JspConfiguration jspConfiguration,
                                 JspCoreContext ctxt,
                                 TagClassInfo tagClassInfo,
                                 TagInfo ti,
                                 Map persistentData,
                                 ValidateResult.CollectedTagData collectedTagData,
                                 FragmentHelperClassWriter fragmentHelperClassWriter,
                                 JspOptions jspOptions) {
        super(nestingLevel,
              isTagFile,
              hasBody,
              hasJspBody,
              tagHandlerVar,
              element,
              tagLibraryCache,
              jspConfiguration,
              ctxt,
              tagClassInfo,
              ti,
              persistentData,
              collectedTagData,
              fragmentHelperClassWriter,
              jspOptions);
        
        this.jspOptions = jspOptions; //PK65013
        this.optimizedTag = optimizedTag;
        this.tagPushBodyCountVar = tagPushBodyCountVar;
        
        NamedNodeMap nodeAttrs = element.getAttributes();
        for (int i = 0; i < nodeAttrs.getLength(); i++) {
            Attr attr = (Attr)nodeAttrs.item(i);
            if (attr.getName().equals("jsp:id") == false && attr.getName().startsWith("xmlns") == false) {
                attrMap.put(attr.getName(), Boolean.FALSE);
            }
        }
        
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node childNode = nl.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element)childNode;
                if (childElement.getNamespaceURI() != null && 
                    childElement.getNamespaceURI().equals(Constants.JSP_NAMESPACE) && 
                    childElement.getLocalName().equals(Constants.JSP_ATTRIBUTE_TYPE)) {
                        String name = childElement.getAttribute("name");
                        if (name.indexOf(':') != -1) {
                            name = name.substring(name.indexOf(':') + 1);
                        }
                        attrMap.put(name, Boolean.TRUE);
                }
            }
        }
        
        declaredIdList = (List)persistentData.get("declaredIdList");
        if (declaredIdList == null) {
            declaredIdList = new ArrayList();
            persistentData.put("declaredIdList", declaredIdList);
        }
    }
    
    public boolean optimizePossible() {
        return optimizedTag.doOptimization(this);
    }
    
    public MethodWriter generateTagStart() throws JspCoreException {
        tagStartWriter = new MethodWriter();
        if (hasBody) {
            
            // LIDB4147-24
            if (!jspOptions.isDisableResourceInjection()){		//PM06063
                // have CDI create and inject the managed object
                tagStartWriter.print ("com.ibm.ws.managedobject.ManagedObject " + tagHandlerVar + "_mo = ");
                tagStartWriter.print ("_jspx_iaHelper.inject(");
                tagStartWriter.print (tagClassInfo.getTagClassName() + ".class");
                tagStartWriter.println (");");
            
                // get the underlying object from the managed object
                tagStartWriter.print(tagClassInfo.getTagClassName());
                tagStartWriter.print(" ");
                tagStartWriter.print(tagHandlerVar);   
                tagStartWriter.print(" = ");           
                tagStartWriter.println("("+tagClassInfo.getTagClassName()+")"+tagHandlerVar+"_mo.getObject();"); 

            	tagStartWriter.print ("_jspx_iaHelper.doPostConstruct(");
            	tagStartWriter.print (tagHandlerVar);
            	tagStartWriter.println (");");
            	
                tagStartWriter.print ("_jspx_iaHelper.addTagHandlerToCdiMap(");
                tagStartWriter.print (tagHandlerVar + ", " + tagHandlerVar + "_mo");
                tagStartWriter.println (");");
                
            } else {
                
                // not using CDI
                tagStartWriter.print(tagClassInfo.getTagClassName());
                tagStartWriter.print(" ");
                tagStartWriter.print(tagHandlerVar);
                tagStartWriter.print(" = ");
                tagStartWriter.print("new ");
                tagStartWriter.print(tagClassInfo.getTagClassName());
                tagStartWriter.print("();");
                tagStartWriter.println();
            }
            
            tagStartWriter.println();
        }
        return tagStartWriter;
    }
    
    public MethodWriter generateTagMiddle() throws JspCoreException {
        tagMiddleWriter = new MethodWriter();
        if (tagClassInfo.implementsTryCatchFinally()) {
            tagMiddleWriter.print("int[] ");
            tagMiddleWriter.print(tagPushBodyCountVar);
            tagMiddleWriter.print(" = new int[] { 0 };");
            tagMiddleWriter.println();
        }
        return tagMiddleWriter;
    }
    
    public MethodWriter generateTagEnd() throws JspCoreException {
        generateJspAttributeSetters();
        currentWriter = tagMiddleWriter;
        optimizedTag.generateStart(this);
        tagEndWriter = new MethodWriter();
        currentWriter = tagEndWriter;
        optimizedTag.generateEnd(this);
        return tagEndWriter;
    }
    
    public void generateImports(JavaCodeWriter writer) {
        currentWriter = writer;
        optimizedTag.generateImports(this);
    }
    
    public void generateDeclarations(JavaCodeWriter writer) {
        currentWriter = writer;
        optimizedTag.generateDeclarations(this);
    }
    
    protected void generateSetterCall(String attrName,
                                      String evalAttrValue,
                                      String uri,
                                      MethodWriter setterWriter,
                                      boolean isDymanic) {
        optimizedTag.setAttribute(attrName, evalAttrValue);                                              
    }
    
    public void writeSource(String source) {
        currentWriter.println(source);
    }
    
    public void writeImport(String importId, String importSource) {
        if (declaredIdList.contains(importId) == false) {
            currentWriter.println(importSource);
            declaredIdList.add(importId);                                
        }
    }
    
    public void writeDeclaration(String declarationId, String declarationSource) {
        if (declaredIdList.contains(declarationId) == false) {
            currentWriter.println(declarationSource);                    
            declaredIdList.add(declarationId);                                
        }
    }
    
    public String createTemporaryVariable() {
        return GeneratorUtils.nextTemporaryVariableName(persistentData);
    }
    
    public boolean hasAttribute(String attrName) {
        return attrMap.containsKey(attrName);
    }

    public boolean isJspAttribute(String attrName) {
        boolean b = false;
        if (attrMap.containsKey(attrName)) {
            b = ((Boolean)attrMap.get(attrName)).booleanValue();    
        }
        return b;
    }
    
    public OptimizedTag getParent() {
        OptimizedTag parentTag = null;
        if (parentTagInstanceInfo != null) {
            parentTag = parentTagInstanceInfo.getOptTag();
        }
        return parentTag;
    }

    public void generateInitialization(JavaCodeWriter writer) {}
    public void generateFinally(JavaCodeWriter writer) {}
    
    public boolean hasBody() {
        return hasBody;
    }
    
    public boolean hasJspBody() {
        return hasJspBody;
    }
    
    //PK65013 start
    public JspOptions getJspOptions() {
        return jspOptions;
    }

    public boolean isTagFile() {
        return isTagFile;
    }
    //PK65013 end

}
