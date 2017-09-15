/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.visitor.generator;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

import javax.servlet.jsp.tagext.TagFileInfo;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagLibraryInfo;
import javax.servlet.jsp.tagext.TagVariableInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.taglib.TagClassInfo;
import com.ibm.ws.jsp.taglib.TagLibraryCache;
import com.ibm.ws.jsp.taglib.TagLibraryInfoImpl;
import com.ibm.ws.jsp.translator.optimizedtag.OptimizedTag;
import com.ibm.ws.jsp.translator.utils.JspTranslatorUtil;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.validator.ValidateResult;
import com.ibm.wsspi.jsp.context.JspCoreContext;

public class CustomTagGenerator extends CodeGeneratorBase {
    private int nestingLevel = 0;
    private int methodNesting = 0;
    
    private boolean methodReturnBoolean = false;
    private boolean hasBody = false;
    private boolean hasJspBody = false;
    private boolean hasJspAttributes = false;
    private boolean existingTag = false;
    private boolean genTagInMethod = false;
    private boolean reuseTag = false;
    
    private String prefix = null;
    private String shortName = null;
    private String namespaceURI = null;
    
    private String baseVar = null;
    private String tagEvalVar = null;
    private String tagHandlerVar = null;
    private String tagPushBodyCountVar = null;
    
    private String pushBodyCountVar = null;
    private String savePushBodyCountVar = null;
    private String saveFragmentPushBodyCountVar = null; //PK38681 - saved value for pushbodycountvar so we can set it back after processing.  Only used for Fragments.
    private String parentTagName = null;
    
    private TagLibraryInfo tli = null;
    private TagInfo ti = null;
    private ValidateResult.CollectedTagData collectedTagData = null;
    private TagClassInfo tagClassInfo = null;
    private TagLibraryCache tagLibraryCache = null;
    private Stack tagInstanceInfoStack = null;
    
    private MethodWriter tagStartWriter = null;
    private MethodWriter tagMiddleWriter = null;
    private MethodWriter tagEndWriter = null;
    
    private List setterWriterList = null;
    
    private TagGenerator tagGenerator = null;
    private OptimizedTag optTag = null;
    
    private String pushBodyCountVarDeclOrig = null;  //PK60565
    
    protected JspConfiguration tagConfig=null;
    
    public void init(JspCoreContext ctxt,
                     Element element, 
                     ValidateResult validatorResult,
                     JspVisitorInputMap inputMap,
                     ArrayList methodWriterList,
                     FragmentHelperClassWriter fragmentHelperClassWriter, 
                     HashMap persistentData,
                     JspConfiguration jspConfiguration,
                     JspOptions jspOptions) 
        throws JspCoreException {
        tagConfig = jspConfiguration.createClonedJspConfiguration();
        super.init(ctxt, element, validatorResult, inputMap, methodWriterList, fragmentHelperClassWriter, persistentData, tagConfig, jspOptions);
        tagLibraryCache = (TagLibraryCache)inputMap.get("TagLibraryCache");
        prefix = element.getPrefix();
        if (prefix == null) // 245645.1
            prefix = ""; // 245645.1
        shortName = element.getLocalName();
        namespaceURI = element.getNamespaceURI();
        if (namespaceURI.startsWith("urn:jsptld:")) {
            namespaceURI = namespaceURI.substring(namespaceURI.indexOf("urn:jsptld:")+11);
        }
        else if (namespaceURI.startsWith("urn:jsptagdir:")) {
            namespaceURI = namespaceURI.substring(namespaceURI.indexOf("urn:jsptagdir:")+14);                        
        }
        tli = (TagLibraryInfo)validatorResult.getTagLibMap().get(namespaceURI);
        if (tli.getRequiredVersion()!=null) {
            tagConfig.setJspVersion(tli.getRequiredVersion());
        }
        ti = tli.getTag(shortName);
        if (ti == null) {
            TagFileInfo tfi = tli.getTagFile(shortName);
            ti = tfi.getTagInfo();
        }
        tagClassInfo = tagLibraryCache.getTagClassInfo(ti);
        Node parentNode = element.getParentNode();
        
        while (parentNode != null) {
            if (parentNode instanceof Element) {
                Element parentElement = (Element)parentNode;
                if (parentElement.getTagName().equals(element.getTagName())){
                    nestingLevel++; 
                }
            }
            parentNode = parentNode.getParentNode();    
        }
        
        hasBody = JspTranslatorUtil.hasBody(element);
        hasJspBody = JspTranslatorUtil.hasJspBody(element);
        hasJspAttributes = hasChildJspElement(Constants.JSP_ATTRIBUTE_TYPE);
        
        collectedTagData = validatorResult.getCollectedTagData(element);
        tagInstanceInfoStack = (Stack)persistentData.get("tagInstanceInfoStack");
        if (tagInstanceInfoStack == null) {
            tagInstanceInfoStack = new Stack();
            persistentData.put("tagInstanceInfoStack", tagInstanceInfoStack);
        }
        
        if (persistentData.get("methodNesting") == null) {
            persistentData.put("methodNesting", new Integer(0));
        }
        
        if (collectedTagData.isScriptless() && !collectedTagData.hasScriptingVars()) {
            genTagInMethod = true;
        }

        if (tagClassInfo.implementsSimpleTag() == false) {
            if (jspOptions.isUsePageTagPool()) {
                reuseTag = true;
            }
            else if (jspOptions.isUseThreadTagPool()) {
                if (isTagFile == false) {
                    reuseTag = true;
                }
            }
        }
        
        if (reuseTag) {
            HashMap existTagMap = (HashMap)persistentData.get("existTagMap");
            if (existTagMap == null) {
                existTagMap = new HashMap();
                persistentData.put("existTagMap", existTagMap);
            }
            String existingVarName = (String)existTagMap.get(shortName+collectedTagData.getVarNameSuffix());
            if (existingVarName != null) {
                if (isExistingTagInStack("_jspx_th_" + existingVarName) == false) {
                    baseVar = existingVarName;
                    existingTag = true;    
                }
                else {
                    baseVar = createTagVarName(ti.getTagName(), prefix, shortName);
                }
            }
            else {
                baseVar = createTagVarName(ti.getTagName(), prefix, shortName);
                existTagMap.put(shortName+collectedTagData.getVarNameSuffix(), baseVar);
            }
            //247815 Start
            if (persistentData.get("InitTaglibLookupWriter") == null) {
                InitTaglibLookupWriter initTaglibLookupWriter = new InitTaglibLookupWriter(jspOptions.isUseThreadTagPool());
                persistentData.put("InitTaglibLookupWriter", initTaglibLookupWriter);
                methodWriterList.add(initTaglibLookupWriter);
            }
            if (persistentData.get("CleanupTaglibLookupWriter") == null) {
                CleanupTaglibLookupWriter cleanupTaglibLookupWriter = new CleanupTaglibLookupWriter(jspOptions.isUseThreadTagPool());
                persistentData.put("CleanupTaglibLookupWriter", cleanupTaglibLookupWriter);
                methodWriterList.add(cleanupTaglibLookupWriter);
            }
            //247815 End
        }
        else {        
            baseVar = createTagVarName(ti.getTagName(), prefix, shortName);
        }
        
        tagEvalVar = "_jspx_eval_" + baseVar;
        tagHandlerVar = "_jspx_th_" + baseVar;
        tagPushBodyCountVar = "_jspx_push_body_count_" + baseVar;
        
    }

    private void createTagGenerator() {
        if (jspOptions.isUseOptimizedTags()) {
            optTag = tagLibraryCache.getOptimizedTag(namespaceURI, ((TagLibraryInfoImpl)tli).getTlibversion(), shortName);
            if (optTag != null) {
                tagGenerator = new OptimizedTagGenerator(optTag,
                                                         tagPushBodyCountVar,      
                                                         nestingLevel,
                                                         isTagFile,
                                                         hasBody,
                                                         hasJspBody,
                                                         tagHandlerVar,
                                                         element,
                                                         tagLibraryCache,
                                                         tagConfig,
                                                         ctxt,
                                                         tagClassInfo,
                                                         ti,
                                                         persistentData,
                                                         collectedTagData,
                                                         fragmentHelperClassWriter,
                                                         jspOptions);
                                                         
                if (tagInstanceInfoStack.size() > 0) {
                    TagInstanceInfo tagInstanceInfo = (TagInstanceInfo)tagInstanceInfoStack.peek();
                    parentTagName = tagInstanceInfo.getTagHandlerVar();
                    tagGenerator.setParentTagInstanceInfo(tagInstanceInfo);
                }
            
                if (((OptimizedTagGenerator)tagGenerator).optimizePossible()) {
                    if (genTagInMethod)
                        genTagInMethod = optTag.canGenTagInMethod((OptimizedTagGenerator)tagGenerator);
                    
                    if (existingTag) {
                        existingTag = false;                            
                        baseVar = createTagVarName(ti.getTagName(), prefix, shortName);
                        tagEvalVar = "_jspx_eval_" + baseVar;
                        tagHandlerVar = "_jspx_th_" + baseVar;
                        tagPushBodyCountVar = "_jspx_push_body_count_" + baseVar;
                        ((OptimizedTagGenerator)tagGenerator).tagHandlerVar = tagHandlerVar;
                        ((OptimizedTagGenerator)tagGenerator).tagPushBodyCountVar = tagPushBodyCountVar; 
                    }
                }
                else {
                    tagGenerator = null;
                }
            }
        }
        
        if (tagGenerator == null) {
            if (tagClassInfo.implementsSimpleTag()) {
                tagGenerator =
                    new SimpleTagGenerator(
                        nestingLevel,
                        isTagFile,
                        hasBody,
                        hasJspBody,
                        tagHandlerVar,
                        element,
                        tagLibraryCache,
                        tagConfig,
                        ctxt,
                        tagClassInfo,
                        ti,
                        persistentData,
                        collectedTagData,
                        fragmentHelperClassWriter,
                        jspOptions,
                        inputMap);
            }
            else {
                tagGenerator =
                    new ClassicTagGenerator(
                        reuseTag,
                        genTagInMethod,
                        existingTag,
                        baseVar,
                        tagEvalVar,
                        tagPushBodyCountVar,
                        nestingLevel,
                        isTagFile,
                        hasBody,
                        hasJspBody,
                        tagHandlerVar,
                        element,
                        tagLibraryCache,
                        tagConfig,
                        ctxt,
                        tagClassInfo,
                        ti,
                        persistentData,
                        collectedTagData,
                        fragmentHelperClassWriter,
                        jspOptions,
                        inputMap);// jsp2.1work
            }
            
            if (tagInstanceInfoStack.size() > 0) {
                TagInstanceInfo tagInstanceInfo = (TagInstanceInfo)tagInstanceInfoStack.peek();
                parentTagName = tagInstanceInfo.getTagHandlerVar();
                tagGenerator.setParentTagInstanceInfo(tagInstanceInfo);
            }
        }
    }
    
    public void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException {
        if (tagGenerator == null) {
            createTagGenerator();
        }
                
        TagInstanceInfo tagInstanceInfo = new TagInstanceInfo(tagHandlerVar, ti, optTag);
        tagInstanceInfoStack.push(tagInstanceInfo);
        
        methodNesting =  ((Integer)persistentData.get("methodNesting")).intValue();
        
        if (writer instanceof FragmentHelperClassWriter.FragmentWriter) {
            //PK38681
            //Since the Fragment will use the pushBodyCountVar of _jspx_push_body_count, we need to set this in the persistentData so nested tags will use this if needed.
            //If there is no previous value set in persistentData, do not set anything as it should be null.
            saveFragmentPushBodyCountVar = (String)persistentData.get("pushBodyCountVar");
            if (saveFragmentPushBodyCountVar!=null) {
                persistentData.put("pushBodyCountVar", "_jspx_push_body_count");
            }
            //PK38681
            //PK60565 start 
            //Since the FragmentWriter is generated in a new method in the compiled code, we are saving pushBodyCountVarDeclaration and pushing null.  We will restore pushBodyCountVarDeclaration later.
            pushBodyCountVarDeclOrig = (String)persistentData.get("pushBodyCountVarDeclaration");
            persistentData.put("pushBodyCountVarDeclaration", null);
            //PK60565 end

            tagGenerator.setIsInFragment(true);
        }
        
        switch (section) {
            case CodeGenerationPhase.IMPORT_SECTION: {
                tagGenerator.generateImports(writer);
                break;
            }

            case CodeGenerationPhase.CLASS_SECTION: {
                tagGenerator.generateDeclarations(writer);
                break;
            }
            
            case CodeGenerationPhase.METHOD_INIT_SECTION: {
                generateTempScriptingVars(writer);
                tagGenerator.generateInitialization(writer);
                break;
            }
                
            case CodeGenerationPhase.METHOD_SECTION: {
                if (genTagInMethod) {
                    if (methodNesting > 0) 
                        methodReturnBoolean = true;
                    persistentData.put("methodNesting", new Integer(++methodNesting));
                }
                
                pushBodyCountVar = (String)persistentData.get("pushBodyCountVar");
                
                tagStartWriter = tagGenerator.generateTagStart();
                setterWriterList = tagGenerator.generateSetters();
              
                // defect 363508 begin
                persistentData.put("pushBodyCountVarArgument"+tagGenerator.hashCode(),null);
                if (pushBodyCountVar != null) {
                    if (genTagInMethod) {
	                    persistentData.put("pushBodyCountVarArgument"+tagGenerator.hashCode(),pushBodyCountVar);
	                }
                }
                // defect 363508 end
                //PK60565 
                //We already did this if this is a FragmentHelper, so don't do again.
                //If it's a TryCatchFinally, we want to be able to restore the original value later.
                if (tagClassInfo.implementsTryCatchFinally() && !(writer instanceof FragmentHelperClassWriter.FragmentWriter)) {
                    pushBodyCountVarDeclOrig = (String)persistentData.get("pushBodyCountVarDeclaration");               
                    persistentData.put("pushBodyCountVarDeclaration", null);
                } //PK60565 end
                
                tagMiddleWriter = tagGenerator.generateTagMiddle();
                persistentData.put("pushBodyCountVarArgument"+tagGenerator.hashCode(),null); // defect 363508
                
                
                if (tagClassInfo.implementsTryCatchFinally()) {
                    savePushBodyCountVar = pushBodyCountVar;
                    pushBodyCountVar = tagPushBodyCountVar;
                    persistentData.put("pushBodyCountVar", pushBodyCountVar);
                }
                break;
            }
                
            case CodeGenerationPhase.FINALLY_SECTION: {
                tagGenerator.generateFinally(writer);
                break;
            }
        }
    }
    
    public void endGeneration(int section, JavaCodeWriter writer)  throws JspCoreException {
        methodNesting =  ((Integer)persistentData.get("methodNesting")).intValue();
        if (writer instanceof FragmentHelperClassWriter.FragmentWriter) {
            tagGenerator.setIsInFragment(true);
        }
        
        switch (section) {
            case CodeGenerationPhase.METHOD_SECTION: {
                if (tagClassInfo.implementsTryCatchFinally()) {
                    pushBodyCountVar = savePushBodyCountVar;
                    persistentData.put("pushBodyCountVar", pushBodyCountVar);
                }                
                
                tagEndWriter = tagGenerator.generateTagEnd();
                
                assembleTagFromWriters(writer);                    
                break;
            }
        }
        tagInstanceInfoStack.pop();
        //PK38681
        //Replace original value for pushBodyCountVar in persistentData now that we are done with the Fragment.
        if (writer instanceof FragmentHelperClassWriter.FragmentWriter) {
            persistentData.put("pushBodyCountVar", saveFragmentPushBodyCountVar);
            //PK60565 want to restore pushBodyCountVarDeclaration
            persistentData.put("pushBodyCountVarDeclaration", pushBodyCountVarDeclOrig);
            //PK60565 end
        }
        //PK60565 want to restore pushBodyCountVarDeclaration
        if (tagClassInfo.implementsTryCatchFinally()) {
            persistentData.put("pushBodyCountVarDeclaration", pushBodyCountVarDeclOrig);
        } //PK60565 end

        //PK38681
    }
    
    public JavaCodeWriter getWriterForChild(int section, Node childElement) throws JspCoreException {
        JavaCodeWriter writerForChild = tagGenerator.getWriterForChild(section, childElement);
        return (writerForChild);
    }
    
    public boolean isTagDependent() {
        boolean tagDependent = false;
        
        if (ti.getBodyContent() != null && 
            ti.getBodyContent().equalsIgnoreCase(TagInfo.BODY_CONTENT_TAG_DEPENDENT)) {
            tagDependent = true;
        }
        return (tagDependent);
    }
    
    private void assembleTagFromWriters(JavaCodeWriter writer) throws JspCoreException {
        JavaCodeWriter tagWriter = writer;
        tagWriter.println();

        MethodWriter methodWriter = null;
        
        //PK65013 - start
        String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
        if (isTagFile && jspOptions.isModifyPageContextVariable()) {
            pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
        }
        //PK65013 - end

        if (genTagInMethod) {
            methodWriter = new MethodWriter();
            String tagMethod = null;
            
            if (reuseTag) {
                tagMethod = createMethodName(ti.getTagName(), prefix, shortName);
            }
            else {
                tagMethod = "_jspx_meth_" + baseVar;
            }
            
            //232818
            tagWriter.println("/* ElementId[" + element.hashCode() + "] ctmb */");

            tagWriter.print("if (");
            tagWriter.print(tagMethod);
            tagWriter.print("(");
            
            if (jspOptions.isUsePageTagPool() || jspOptions.isUseThreadTagPool()) {
                tagWriter.print("_jspx_TagLookup, ");
            }
            
            if (parentTagName != null) {
                tagWriter.print(parentTagName);
                tagWriter.print(", ");
            }

            //PK65013 change pageContext variable to customizable one.
            tagWriter.print(pageContextVar);
            if (pushBodyCountVar != null) {
                tagWriter.print(", ");
                if (tagWriter instanceof FragmentHelperClassWriter.FragmentWriter) {
                    tagWriter.print("_jspx_push_body_count");
                }
                else {
                    tagWriter.print(pushBodyCountVar);
                }
            }
            tagWriter.print("))");
            tagWriter.print((methodReturnBoolean) ? " return true;" : " return;");
            tagWriter.println();

            //232818
            tagWriter.println("/* ElementId[" + element.hashCode() + "] ctme */");
            
            methodWriter.println();
            methodWriter.print("private boolean ");
            methodWriter.print(tagMethod);
            methodWriter.print("(");
            
            if (jspOptions.isUsePageTagPool() || jspOptions.isUseThreadTagPool()) {
                methodWriter.print("java.util.HashMap _jspx_TagLookup, ");
            }
            
            if (parentTagName != null) {
                methodWriter.print("javax.servlet.jsp.tagext.JspTag ");
                methodWriter.print(parentTagName);
                methodWriter.print(", ");
            }
            
            //PK65013 change pageContext variable to customizable one.
            methodWriter.print("PageContext "+pageContextVar+"");
            persistentData.put("pushBodyCountVarArgument"+tagGenerator.hashCode(),null); // defect 363508
            if (pushBodyCountVar != null) {
                methodWriter.print(", int[] ");
                methodWriter.print(pushBodyCountVar);
                persistentData.put("pushBodyCountVarArgument"+tagGenerator.hashCode(),pushBodyCountVar); // defect 363508
            }
            methodWriter.print(")");
            methodWriter.println();
            
            methodWriter.println("   throws Throwable {");

            writeDebugStartBegin(methodWriter);
            //PK65013 change pageContext variable to customizable one.
            methodWriter.println("JspWriter out = "+pageContextVar+".getOut();");
            GeneratorUtils.generateLocalVariables(methodWriter, element, pageContextVar);
            tagWriter = methodWriter;
        }
        else {
            writeDebugStartBegin(tagWriter);
        }
        
        tagWriter.printMultiLn(tagStartWriter.toString());
        for (Iterator itr = setterWriterList.iterator(); itr.hasNext();) {
            JavaCodeWriter setterWriter = (JavaCodeWriter)itr.next();
            tagWriter.printMultiLn(setterWriter.toString());
        }
        tagWriter.printMultiLn(tagMiddleWriter.toString());
        //232818 
        boolean debugEnd = false;
        if (tagGenerator.fragmentWriterUsed() == false) {
            String body = tagGenerator.getBodyWriter().toString(); //232818
            if (body.length() > 0) { //232818
            	writeDebugStartEnd(tagWriter);
                debugEnd = true; //232818
            }
            tagWriter.printMultiLn(body); //232818
            if (debugEnd) //232818
            	writeDebugEndBegin(tagWriter);
        }
        tagWriter.printMultiLn(tagEndWriter.toString());

        if (genTagInMethod) {
            if (methodNesting > 0) {
                methodWriter.println("return false;");
            }
            if (tagGenerator.fragmentWriterUsed() == false) 
                if (debugEnd) //232818
                	writeDebugEndEnd(methodWriter); //232818
                else
                    writeDebugStartEnd(methodWriter); //232818
            else 
                writeDebugStartEnd(methodWriter);
            methodWriter.println("}");
            persistentData.put("methodNesting", new Integer(--methodNesting));
    
            methodWriterList.add(methodWriter);
        }
        else {
            if (tagGenerator.fragmentWriterUsed() == false)
                if (debugEnd) //232818 
                	writeDebugEndEnd(tagWriter); //232818
                else
                    writeDebugStartEnd(tagWriter); //232818
            else 
                writeDebugStartEnd(tagWriter);
        }
        tagWriter.println();
    }
    
    private void generateTempScriptingVars(JavaCodeWriter writer) {
        ArrayList vars = (ArrayList)persistentData.get("tempScriptingVars");
        if (vars == null) {
            vars = new ArrayList();
            persistentData.put("tempScriptingVars", vars);
        }
        if (nestingLevel > 0) {
            TagVariableInfo[] tagVarInfos = ti.getTagVariableInfos();
            VariableInfo[] varInfos = ti.getVariableInfo(collectedTagData.getTagData());

            if (varInfos == null)
                varInfos = new VariableInfo[0];
                
            if (varInfos.length > 0) {
                for (int i = 0; i < varInfos.length; i++) {
                    String varName = varInfos[i].getVarName();
                    String tmpVarName = "_jspx_" + varName + "_" + nestingLevel;
                    if (!vars.contains(tmpVarName)) {
                        vars.add(tmpVarName);
                        writer.print(varInfos[i].getClassName());
                        writer.print(" ");
                        writer.print(tmpVarName);
                        writer.print(" = ");
                        writer.print(null);
                        writer.println(";");
                    }
                }
            }
            else {
                for (int i = 0; i < tagVarInfos.length; i++) {
                    String varName = tagVarInfos[i].getNameGiven();
                    if (varName == null) {
                        varName = collectedTagData.getTagData().getAttributeString(tagVarInfos[i].getNameFromAttribute());
                    }
                    else if (tagVarInfos[i].getNameFromAttribute() != null) {
                        // alias
                        continue;
                    }
                    String tmpVarName = "_jspx_" + varName + "_" + nestingLevel;
                    if (!vars.contains(tmpVarName)) {
                        vars.add(tmpVarName);
                        writer.print(tagVarInfos[i].getClassName());
                        writer.print(" ");
                        writer.print(tmpVarName);
                        writer.print(" = ");
                        writer.print(null);
                        writer.println(";");
                    }
                }
            }
        }
    }
    
        
    private String createTagVarName(String fullName, String prefix, String shortName) {
        HashMap tagVarNumbers = (HashMap)persistentData.get("tagVarNumbers");
        if (tagVarNumbers == null) {
            tagVarNumbers = new HashMap();
            persistentData.put("tagVarNumbers", tagVarNumbers);
        }
        
        if (prefix.indexOf('-') >= 0)
            prefix = GeneratorUtils.replace(prefix, '-', "$1");
        if (prefix.indexOf('.') >= 0)
            prefix = GeneratorUtils.replace(prefix, '.', "$2");

        if (shortName.indexOf('-') >= 0)
            shortName = GeneratorUtils.replace(shortName, '-', "$1");
        if (shortName.indexOf('.') >= 0)
            shortName = GeneratorUtils.replace(shortName, '.', "$2");
        if (shortName.indexOf(':') >= 0)
            shortName = GeneratorUtils.replace(shortName, ':', "$3");

        String varName = prefix + "_" + shortName + "_";
        if (tagVarNumbers.get(fullName) != null) {
            Integer i = (Integer) tagVarNumbers.get(fullName);
            varName = varName + i.intValue();
            tagVarNumbers.put(fullName, new Integer(i.intValue() + 1));
            return varName;
        }
        else {
            tagVarNumbers.put(fullName, new Integer(1));
            return varName + "0";
        }
    }
    
    private String createMethodName(String fullName, String prefix, String shortName) {
        HashMap methodNumbers = (HashMap)persistentData.get("methodNumbers");
        if (methodNumbers == null) {
            methodNumbers = new HashMap();
            persistentData.put("methodNumbers", methodNumbers);
        }
        
        if (prefix.indexOf('-') >= 0)
            prefix = GeneratorUtils.replace(prefix, '-', "$1");
        if (prefix.indexOf('.') >= 0)
            prefix = GeneratorUtils.replace(prefix, '.', "$2");

        if (shortName.indexOf('-') >= 0)
            shortName = GeneratorUtils.replace(shortName, '-', "$1");
        if (shortName.indexOf('.') >= 0)
            shortName = GeneratorUtils.replace(shortName, '.', "$2");
        if (shortName.indexOf(':') >= 0)
            shortName = GeneratorUtils.replace(shortName, ':', "$3");

        String methodName = "_jspx_meth_" + prefix + "_" + shortName + "_";
        if (methodNumbers.get(fullName) != null) {
            Integer i = (Integer) methodNumbers.get(fullName);
            methodName = methodName + i.intValue();
            methodNumbers.put(fullName, new Integer(i.intValue() + 1));
            return methodName;
        }
        else {
            methodNumbers.put(fullName, new Integer(1));
            return methodName + "0";
        }
    }
    
    private boolean hasChildJspElement(String elementName) {
        boolean hasElement = false;
        
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element childElement = (Element)child;
                if (childElement.getNamespaceURI() != null && childElement.getNamespaceURI().equals(Constants.JSP_NAMESPACE) &&
                    childElement.getLocalName().equals(elementName)) {
                    hasElement = true;
                    break;
                }
            }
        }
        
        return (hasElement);
    }
    
    private boolean isExistingTagInStack(String existingTagName) {
        boolean exists = false;
        for (Iterator itr = tagInstanceInfoStack.iterator(); itr.hasNext();) {
            TagInstanceInfo tagInstanceInfo = (TagInstanceInfo)itr.next();
            if (tagInstanceInfo.getTagHandlerVar().equals(existingTagName)) {
                exists = true;
                break;
            }
        }
        return exists;
    }
    
    class TagInstanceInfo {
        private String tagHandlerVar = null;
        private TagInfo ti = null;
        private OptimizedTag optTag = null;
        
        public TagInstanceInfo(String tagHandlerVar, TagInfo ti, OptimizedTag optTag) {
            this.tagHandlerVar = tagHandlerVar;
            this.ti = ti;
            this.optTag = optTag;
        }

        public OptimizedTag getOptTag() {
            return optTag;
        }

        public void setOptTag(OptimizedTag tag) {
            optTag = tag;
        }

        public String getTagHandlerVar() {
            return tagHandlerVar;
        }

        public void setTagHandlerVar(String string) {
            tagHandlerVar = string;
        }

        public TagInfo getTi() {
            return ti;
        }

        public void setTi(TagInfo info) {
            ti = info;
        }
    }
}
