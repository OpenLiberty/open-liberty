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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.taglib.TagLibraryCache;
import com.ibm.ws.jsp.translator.visitor.JspVisitor;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorUsage;
import com.ibm.ws.jsp.translator.visitor.validator.ValidateResult;
import com.ibm.wsspi.jsp.context.JspCoreContext;

public abstract class GenerateVisitor extends JspVisitor {
	
    protected Stack writerStack = new Stack();
    protected HashMap generatorMap = new HashMap();
    protected HashMap jspElementMap = new HashMap();
    protected JavaCodeWriter writer = null;
    protected TagLibraryCache tagLibraryCache = null;
    protected ArrayList methodWriterList = null;
    protected FragmentHelperClassWriter fragmentHelperClassWriter = null;
    protected HashMap persistentData = new HashMap();
    protected String validateResultId = null;
    protected String filePath = null;
    protected JspOptions jspOptions = null;
    private int customTagNesting = -1;
    
    public GenerateVisitor(JspVisitorUsage visitorUsage,
                           JspConfiguration jspConfiguration, 
                           JspCoreContext context, 
                           HashMap resultMap,
                           JspVisitorInputMap inputMap,
                           String validateResultId) 
        throws JspCoreException {
        super(visitorUsage, jspConfiguration, context, resultMap, inputMap);
        this.validateResultId = validateResultId;
        tagLibraryCache = (TagLibraryCache)inputMap.get("TagLibraryCache");
        jspOptions = (JspOptions)inputMap.get("JspOptions");
        methodWriterList = new ArrayList();
    }
    
    protected void createWriter(String filePath, String className, Map customTagMethodJspIdMap) throws JspCoreException { //232818
        this.filePath = filePath;
        try {
            Map cdataJspIdMap = (Map)inputMap.get("CdataJspIdMap");
            writer = new JavaFileWriter(filePath, jspElementMap, cdataJspIdMap, customTagMethodJspIdMap, jspOptions.getJavaEncoding()); //232818
        }
        catch (UnsupportedEncodingException e) {
            throw new JspCoreException(e);
        }
        catch (FileNotFoundException e) {
            throw new JspCoreException(e);
        }
        catch (IOException e) {
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
    
    protected void closeWriter() throws JspCoreException {
        try {
            writer.close();
        }
        catch (java.io.IOException e) {
            throw new JspCoreException(e);
        }
    }

    protected void visitJspRootStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new RootGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }

    protected void visitJspRootEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspTextStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {            
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new TextGenerator();
            }          
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitJspTextEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitIncludeDirectiveStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new IncludeDirectiveGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitIncludeDirectiveEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspDeclarationStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new DeclarationGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitJspDeclarationEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspExpressionStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new ExpressionGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitJspExpressionEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspScriptletStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new ScriptletGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }  
    
    protected void visitJspScriptletEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspParamStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new ParamGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    } 
    
    protected void visitJspParamEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspParamsStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new ParamsGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    } 
    
    protected void visitJspParamsEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspFallbackStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new FallBackGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitJspFallbackEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspIncludeStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new IncludeGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitJspIncludeEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspForwardStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new ForwardGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitJspForwardEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspUseBeanStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new UseBeanGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitJspUseBeanEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspGetPropertyStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new GetPropertyGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitJspGetPropertyEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspSetPropertyStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new SetPropertyGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    } 
    
    protected void visitJspSetPropertyEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspPluginStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new PluginGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitJspPluginEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitCustomTagStart(Element jspElement) throws JspCoreException {
        String uri = jspElement.getNamespaceURI();
        if (uri.startsWith("urn:jsptld:")) {
            uri = uri.substring(uri.indexOf("urn:jsptld:") + 11);
        }
        else if (uri.startsWith("urn:jsptagdir:")) {
            uri = uri.substring(uri.indexOf("urn:jsptagdir:") + 14);
        }
        ValidateResult validatorResult = (ValidateResult)resultMap.get(validateResultId);

        if (validatorResult.getTagLibMap().get(uri) != null) {
            CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
            if (generator == null) {
                if (isTagDependent(jspElement.getParentNode())) {
                    generator = new TagDependentGenerator();
                }
                else {
                    generator = new CustomTagGenerator();
                }
                initializeGenerator(jspElement, generator);
            }
            startGeneration(jspElement, generator);
            
            if (isNodeTagDependent (jspElement)) {
                 ++this.customTagNesting;
            }
            
            //}
        }
        else {
            if (jspConfiguration.isXml())
            	visitUninterpretedTagStart(jspElement);
        }
    } 
    
    protected void visitCustomTagEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
        
        if (isNodeTagDependent (jspElement)) {
             --this.customTagNesting;
        }
    }
    
    protected void visitJspAttributeStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if ((this.customTagNesting > 0) && isTagDependent (jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new AttributeGenerator();
            }
             
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitJspAttributeEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspElementStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new ElementGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitJspElementEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspBodyStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if ((this.customTagNesting > 0) && isTagDependent (jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new BodyGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitJspBodyEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspInvokeStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new InvokeGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitJspInvokeEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspDoBodyStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new DoBodyGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitJspDoBodyEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitJspOutputStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new OutputGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitJspOutputEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitUninterpretedTagStart(Element jspElement) throws JspCoreException {
        // 245645.1 Start
        String uri = jspElement.getNamespaceURI();
        if (uri == null) {
            CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
            if (generator == null) {
                if (isTagDependent(jspElement.getParentNode())) {
                    generator = new TagDependentGenerator();
                }
                else {
                    generator = new UninterpretedTagGenerator();
                }
                initializeGenerator(jspElement, generator);
            }
            startGeneration(jspElement, generator);
        }
        else {
            if (uri.startsWith("urn:jsptld:")) {
                uri = uri.substring(uri.indexOf("urn:jsptld:") + 11);
            }
            else if (uri.startsWith("urn:jsptagdir:")) {
                uri = uri.substring(uri.indexOf("urn:jsptagdir:") + 14);
            }
            ValidateResult validatorResult = (ValidateResult)resultMap.get(validateResultId);
    
            if (validatorResult.getTagLibMap().get(uri) == null) {
                CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
                if (generator == null) {
                    if (isTagDependent(jspElement.getParentNode())) {
                        generator = new TagDependentGenerator();
                    }
                    else {
                        generator = new UninterpretedTagGenerator();
                    }
                    initializeGenerator(jspElement, generator);
                }
                startGeneration(jspElement, generator);
            }
            else {
                visitCustomTagStart(jspElement);            
            }
        }
        // 245645.1 End
    }
    
    protected void visitUninterpretedTagEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    
    protected void visitCDataTag(CDATASection cdata) throws JspCoreException {
		//PK34989
		//PK94316 - added check for isTrimDirectiveWhitespaces - per the JSP 2.1 spec (Servlet Spec 2.5).
		boolean useTrim = jspOptions.isUseCDataTrim() || jspConfiguration.isTrimDirectiveWhitespaces();
		boolean process = true;
		if(useTrim)
		{
			process = cdata.getData().trim().length()> 0;
		}
		if(process)
		{ //PK34989
	        CDATAGenerator generator = (CDATAGenerator)generatorMap.get(cdata);
	        Element jspElement = (Element)cdata.getParentNode();
	        if (generator == null) {
	            generator = new CDATAGenerator(cdata);
	            ValidateResult validatorResult = (ValidateResult)resultMap.get(validateResultId);
	            if (isTagDependent(cdata.getParentNode())) {
	                Object parentGenerator = generatorMap.get (jspElement);
	                boolean ignoreEL = (parentGenerator instanceof AttributeGenerator) &&
	                     (this.customTagNesting == 0);
	                
	                generator.init(context, 
	                               jspElement, 
	                               validatorResult, 
	                               inputMap, 
	                               methodWriterList, 
	                               fragmentHelperClassWriter, 
	                               persistentData,
	                               jspConfiguration,
	                               jspOptions,
	                               !ignoreEL);
	            }
	            else {
	                generator.init(context, 
	                               jspElement, 
	                               validatorResult, 
	                               inputMap, 
	                               methodWriterList, 
	                               fragmentHelperClassWriter, 
	                               persistentData,
	                               jspConfiguration,
	                               jspOptions);
	            }
	            generatorMap.put(cdata, generator);
	        }
	        
	        CodeGenerator parentGenerator = (CodeGenerator)generatorMap.get(jspElement);
	        if (parentGenerator != null) {
	            JavaCodeWriter writerForChild = parentGenerator.getWriterForChild(visitCount, cdata);
	            if (writerForChild != null) {
	                writerStack.push(writer);
	                writer = writerForChild;
	            }
	        }
	        generator.startGeneration(visitCount, writer);
	        
	        generator.endGeneration(visitCount, writer);
	        if (parentGenerator != null) {
	            JavaCodeWriter writerForChild = parentGenerator.getWriterForChild(visitCount, cdata);
	            if (writerForChild != null) {
	                writer = (JavaCodeWriter)writerStack.pop();
	            }
	        }
		}
    }
    
    protected void visitPageDirectiveStart(Element jspElement) throws JspCoreException {
		CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
		if (generator == null) {
			if (isTagDependent(jspElement.getParentNode())) {
				generator = new TagDependentGenerator();
			}
			else {
				generator = new ImportGenerator();
			}
			initializeGenerator(jspElement, generator);
		}
		startGeneration(jspElement, generator);
    }
    protected void visitPageDirectiveEnd(Element jspElement) throws JspCoreException {
		endGeneration(jspElement);
    }
    
    /* Start Defect 206049 */
    protected void visitTagDirectiveStart(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        if (generator == null) {
            if (isTagDependent(jspElement.getParentNode())) {
                generator = new TagDependentGenerator();
            }
            else {
                generator = new ImportGenerator();
            }
            initializeGenerator(jspElement, generator);
        }
        startGeneration(jspElement, generator);
    }
    
    protected void visitTagDirectiveEnd(Element jspElement) throws JspCoreException {
        endGeneration(jspElement);
    }
    /* End Defect 206049 */
    
    protected void visitAttributeDirectiveStart(Element jspElement) throws JspCoreException {}
    protected void visitAttributeDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitVariableDirectiveStart(Element jspElement) throws JspCoreException {}
    protected void visitVariableDirectiveEnd(Element jspElement) throws JspCoreException {}
    
    protected void initializeGenerator(Element jspElement, CodeGenerator generator) throws JspCoreException {
        ValidateResult validatorResult = (ValidateResult)resultMap.get(validateResultId);
        generator.init(context, 
                       jspElement, 
                       validatorResult, 
                       inputMap, 
                       methodWriterList, 
                       fragmentHelperClassWriter, 
                       persistentData,
                       jspConfiguration,
                       jspOptions);
        generatorMap.put(jspElement, generator);
        jspElementMap.put(new Integer(jspElement.hashCode()), jspElement);
    }
    
    protected void startGeneration(Element jspElement, CodeGenerator generator) throws JspCoreException {
        Node parentNode = jspElement.getParentNode();
        if (parentNode.getNodeType() == Node.ELEMENT_NODE) {
            Element parentElement = (Element)parentNode;
            CodeGenerator parentGenerator = (CodeGenerator)generatorMap.get(parentElement);
            if (parentGenerator != null) {
                JavaCodeWriter writerForChild = parentGenerator.getWriterForChild(visitCount, jspElement);
                if (writerForChild != null) {
                    writerStack.push(writer);
                    writer = writerForChild;
                }
            }
        }
        generator.startGeneration(visitCount, writer);
    }
    
    protected void endGeneration(Element jspElement) throws JspCoreException {
        CodeGenerator generator = (CodeGenerator)generatorMap.get(jspElement);
        generator.endGeneration(visitCount, writer);
        Node parentNode = jspElement.getParentNode();
        if (parentNode.getNodeType() == Node.ELEMENT_NODE) {
            Element parentElement = (Element)parentNode;
            CodeGenerator parentGenerator = (CodeGenerator)generatorMap.get(parentElement);
            if (parentGenerator != null) {
                JavaCodeWriter writerForChild = parentGenerator.getWriterForChild(visitCount, jspElement);
                if (writerForChild != null) {
                    writer = (JavaCodeWriter)writerStack.pop();
                }
            }
        }
    }
    
    protected boolean isTagDependent(Node parent) {
        boolean tagDependent = false;
        
        if (parent != null) {
            CodeGenerator parentGenerator = (CodeGenerator)generatorMap.get(parent);
            if (parentGenerator instanceof CustomTagGenerator) {
                CustomTagGenerator customTagGenerator = (CustomTagGenerator)parentGenerator;
                tagDependent = customTagGenerator.isTagDependent();
            }
            if (tagDependent == false) {
                tagDependent = isTagDependent(parent.getParentNode());
            }
        }
        
        return tagDependent;
    }
    
    protected boolean isElementTagDependent (Element jspElement) throws JspCoreException {
         CodeGenerator codeGenerator = (CodeGenerator) generatorMap.get (jspElement);
         
         if (codeGenerator instanceof TagDependentGenerator) {
              return true;
         }
         
         if (codeGenerator instanceof CustomTagGenerator) {
              return ((CustomTagGenerator) codeGenerator).isTagDependent();
         }
         
         return false;
    }
}
