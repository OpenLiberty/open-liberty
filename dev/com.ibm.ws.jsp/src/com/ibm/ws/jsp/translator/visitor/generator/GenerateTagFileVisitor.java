/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
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
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagFileInfo;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagVariableInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.taglib.TagFileTagInfo;
import com.ibm.ws.jsp.taglib.TagLibraryInfoImpl;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.JspVisitorResult;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorUsage;
import com.ibm.ws.jsp.translator.visitor.validator.ValidateTagFileResult;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.resource.translation.TagFileResources;

public class GenerateTagFileVisitor extends GenerateVisitor {
    protected GenerateTagFileResult result = null;
    protected TagFileResources tagFileFiles = null;
  
	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.visitor.generator.GenerateTagFileVisitor";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}
    
    public GenerateTagFileVisitor(JspVisitorUsage visitorUsage,
                                  JspConfiguration jspConfiguration, 
                                  JspCoreContext context, 
                                  HashMap resultMap,
                                  JspVisitorInputMap inputMap) 
        throws JspCoreException {
        super(visitorUsage, jspConfiguration, context, resultMap, inputMap, "TagFileValidate");
        result = new GenerateTagFileResult(visitorUsage.getJspVisitorDefinition().getId());
        tagFileFiles = (TagFileResources)inputMap.get("TagFileFiles");
        createWriter(tagFileFiles.getGeneratedSourceFile().getPath(), tagFileFiles.getClassName(), result.getCustomTagMethodJspIdMap()); //232818
    }
    
    public JspVisitorResult getResult() throws JspCoreException {
        closeWriter();
        return result;
    }
    
    public void visit(Document jspDocument, int visitCount) throws JspCoreException {
        ValidateTagFileResult validatorResult = (ValidateTagFileResult)resultMap.get("TagFileValidate");
        
        switch (visitCount) {
            case CodeGenerationPhase.IMPORT_SECTION: {
				if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)) {
					logger.logp(Level.FINEST, CLASS_NAME, "visit","entering code generation phase IMPORT_SECTION");
				}
                generateImportSection(validatorResult);
                break;
            }
            
            case CodeGenerationPhase.CLASS_SECTION: {
				if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)) {
					logger.logp(Level.FINEST, CLASS_NAME, "visit","entering code generation phase CLASS_SECTION");
				}
                generateClassSection(validatorResult);
                break;
            }
            
            case CodeGenerationPhase.STATIC_SECTION: {
				if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)) {
					logger.logp(Level.FINEST, CLASS_NAME, "visit","entering code generation phase STATIC_SECTION");
				}
                generateStaticSection();
                break;
            }
            
            case CodeGenerationPhase.INIT_SECTION: {
				if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)) {
					logger.logp(Level.FINEST, CLASS_NAME, "visit","entering code generation phase INIT_SECTION");
				}
                generateInitSection(validatorResult);
                break;
            }
            
            case CodeGenerationPhase.METHOD_INIT_SECTION: {
				if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)) {
					logger.logp(Level.FINEST, CLASS_NAME, "visit","entering code generation phase METHOD_INIT_SECTION");
				}
                generateDoTagInitSection(validatorResult);
                break;
            }
            
            case CodeGenerationPhase.METHOD_SECTION: {
				if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)) {
					logger.logp(Level.FINEST, CLASS_NAME, "visit","entering code generation phase METHOD_SECTION");
				}
                generateDoTagSection(validatorResult, jspDocument);
                break;
            }
            
            case CodeGenerationPhase.FINALLY_SECTION: {
				if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)) {
					logger.logp(Level.FINEST, CLASS_NAME, "visit","entering code generation phase FINALLY_SECTION");
				}
                generateFinallySection();
                break;
            } 
        }
        
        super.visit(jspDocument, visitCount);
        
        if (visitCount == CodeGenerationPhase.FINALLY_SECTION) {
            writer.println("}");
            writer.println("}");

			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)) {
				logger.logp(Level.FINEST, CLASS_NAME, "visit","entering code generation phase METHOD_WRITE");
			}
    
            boolean poolingTagMethodsCreated = false;
            for (Iterator itr = methodWriterList.iterator(); itr.hasNext();) {
                MethodWriter methodWriter = (MethodWriter)itr.next();
                //247815 Start
                if (methodWriter instanceof InitTaglibLookupWriter) {
                    InitTaglibLookupWriter w = (InitTaglibLookupWriter)methodWriter;
                    w.complete();
                    poolingTagMethodsCreated = true;
                }
                else if (methodWriter instanceof CleanupTaglibLookupWriter) {
                    CleanupTaglibLookupWriter w = (CleanupTaglibLookupWriter)methodWriter;
                    w.complete();
                    poolingTagMethodsCreated = true;
                }
                //247815 End
                writer.printMultiLn(methodWriter.toString());
            }

            //247815 Start
            if (jspOptions.isUsePageTagPool() && poolingTagMethodsCreated == false) {
                InitTaglibLookupWriter initTaglibLookupWriter = new InitTaglibLookupWriter(jspOptions.isUseThreadTagPool());
                initTaglibLookupWriter.complete();
                writer.printMultiLn(initTaglibLookupWriter.toString());
                CleanupTaglibLookupWriter cleanupTaglibLookupWriter =  new CleanupTaglibLookupWriter(jspOptions.isUseThreadTagPool());
                cleanupTaglibLookupWriter.complete();
                writer.printMultiLn(cleanupTaglibLookupWriter.toString());
            }
            //247815 End
            
			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)) {
				logger.logp(Level.FINEST, CLASS_NAME, "visit","entering code generation phase FRAGMENT_HELPER");
			}

            if (fragmentHelperClassWriter.isUsed()) {
                fragmentHelperClassWriter.generatePostamble();
                writer.printMultiLn(fragmentHelperClassWriter.toString());
            }
    
            writer.println("}");
        }
    }


    protected void generateImportSection(ValidateTagFileResult validatorResult) {
        TagFileInfo tfi = (TagFileInfo)inputMap.get("TagFileInfo");
        TagLibraryInfoImpl tli = (TagLibraryInfoImpl)tfi.getTagInfo().getTagLibrary();
        String tagFilePath = null;
        if (tfi.getPath().startsWith("/WEB-INF/tags")) {
            tagFilePath = tfi.getPath().substring(tfi.getPath().indexOf("/WEB-INF/tags") + 13);
        }
        else if (tfi.getPath().startsWith("/META-INF/tags")) {
            tagFilePath = tfi.getPath().substring(tfi.getPath().indexOf("/META-INF/tags") + 14);
        }
        tagFilePath = tagFilePath.substring(0, tagFilePath.lastIndexOf("/"));
        //PM70267 start
        if (tagFilePath.indexOf("-") > -1) {
            tagFilePath = com.ibm.ws.jsp.translator.utils.NameMangler.handlePackageName(tagFilePath);
            if (!tagFilePath.startsWith(".")) { 
                tagFilePath = "." + tagFilePath;
            }
        }
        //PM70267 end

        tagFilePath = tagFilePath.replace('/', '.');
        
        String tagFilePackageName = Constants.TAGFILE_PACKAGE_NAME + "." + tli.getOriginatorId();
        if (tagFilePath.length() > 0)
            tagFilePackageName += tagFilePath;
        writer.println("package " + tagFilePackageName + ";");
        writer.println();
        
        for(int i = 0; i < Constants.STANDARD_IMPORTS.length; i++)
            writer.println("import " + (String) Constants.STANDARD_IMPORTS[i] + ";");
        
        writer.println();
    }
    
    protected void generateClassSection(ValidateTagFileResult validatorResult) {
        writer.println();
        writer.print("public class " + tagFileFiles.getClassName() + " extends javax.servlet.jsp.tagext.SimpleTagSupport");
        TagFileInfo tfi = (TagFileInfo)inputMap.get("TagFileInfo");
        TagInfo ti = tfi.getTagInfo();
        if (ti.hasDynamicAttributes()) {
            writer.println();
            writer.print(" implements javax.servlet.jsp.tagext.DynamicAttributes");
        }
        
        writer.println(" {");
        GeneratorUtils.generateFactoryInitialization(writer, jspConfiguration.getConfigManager().isJCDIEnabled());
		writer.println();
    }

    protected void generateStaticSection() {
        writer.println();
        writer.println("static {");
    }
    
    protected void generateInitSection(ValidateTagFileResult validatorResult) throws JspCoreException {
        TagFileInfo tfi = (TagFileInfo)inputMap.get("TagFileInfo");
        TagInfo ti = tfi.getTagInfo();
        
        boolean aliasSeen = false;
        TagVariableInfo[] tagVars = ti.getTagVariableInfos();
        for (int i = 0; i < tagVars.length; i++) {
            if (tagVars[i].getNameFromAttribute() != null && tagVars[i].getNameGiven() != null) {
                aliasSeen = true;
                break;
            }
        }
        writer.println("}");
        writer.println();
        GeneratorUtils.generateInitSectionCode(writer, GeneratorUtils.TAG_FILE_TYPE, jspOptions);        //PM06063
        writer.println();        
        GeneratorUtils.generateELFunctionCode(writer, validatorResult);
        writer.println("private JspContext jspContext;");
        writer.println("private java.io.Writer _jspx_sout;");
        
        if (aliasSeen) {
            writer.println("public void setJspContext(JspContext ctx, java.util.Map aliasMap) {");
        }
        else {
            writer.println("public void setJspContext( JspContext ctx ) {");
        }
        writer.println("super.setJspContext(ctx);");
        writer.println("java.util.ArrayList _jspx_nested = null;");
        writer.println("java.util.ArrayList _jspx_at_begin = null;");
        writer.println("java.util.ArrayList _jspx_at_end = null;");

        for (int i = 0; i < tagVars.length; i++) {

            switch (tagVars[i].getScope()) {
                case VariableInfo.NESTED :
                    writer.println("if (_jspx_nested == null)");
                    writer.println("_jspx_nested = new java.util.ArrayList();");
                    writer.print("_jspx_nested.add(");
                    break;

                case VariableInfo.AT_BEGIN :
                    writer.println("if (_jspx_at_begin == null)");
                    writer.println("_jspx_at_begin = new java.util.ArrayList();");
                    writer.print("_jspx_at_begin.add(");
                    break;

                case VariableInfo.AT_END :
                    writer.println("if (_jspx_at_end == null)");
                    writer.println("_jspx_at_end = new java.util.ArrayList();");
                    writer.print("_jspx_at_end.add(");
                    break;
            }

            writer.print(GeneratorUtils.quote(tagVars[i].getNameGiven()));
            writer.print(");");
            writer.println();     
        }
        if (aliasSeen) {
            writer.println("this.jspContext = new org.apache.jasper.runtime.JspContextWrapper(ctx, _jspx_nested, _jspx_at_begin, _jspx_at_end, aliasMap);");
        } 
        else {
            writer.println("this.jspContext = new org.apache.jasper.runtime.JspContextWrapper(ctx, _jspx_nested, _jspx_at_begin, _jspx_at_end, null);");
        }
        writer.println("}");
        writer.println();
        writer.println("public JspContext getJspContext() {");
        writer.println("return this.jspContext;");
        writer.println("}");
        

        if (ti.hasDynamicAttributes()) {
            writer.println("private java.util.HashMap _jspx_dynamic_attrs = new java.util.HashMap();");
        }

        TagAttributeInfo[] attrInfos = ti.getAttributes();
        for (int i = 0; i < attrInfos.length; i++) {
            writer.print("private ");
            if (attrInfos[i].isFragment()) {
                writer.print("javax.servlet.jsp.tagext.JspFragment ");
            }
            else {
                /* Defect 213290 */
                writer.print(GeneratorUtils.toJavaSourceType(attrInfos[i].getTypeName()));
                writer.print(" ");
            }
            writer.print(attrInfos[i].getName());
            writer.print(";");
            writer.println();
        }
        writer.println();

        if (attrInfos != null) {
            for (int i = 0; i < attrInfos.length; i++) {
                writer.print("public ");
                if (attrInfos[i].isFragment()) {
                    writer.print("javax.servlet.jsp.tagext.JspFragment ");
                }
                else {
                    /* Defect 213290 */
                    writer.print(GeneratorUtils.toJavaSourceType(attrInfos[i].getTypeName()));
                    writer.print(" ");
                }
                writer.print(GeneratorUtils.toGetterMethod(attrInfos[i].getName()));
                writer.print(" {");
                writer.println();
                writer.print("return this.");
                writer.print(attrInfos[i].getName());
                writer.print(";");
                writer.println();
                writer.println("}");
                writer.println();

                // setter method
                writer.print("public void ");
                writer.print(GeneratorUtils.toSetterMethodName(attrInfos[i].getName()));
                if (attrInfos[i].isFragment()) {
                    writer.print("(");
                    writer.print("javax.servlet.jsp.tagext.JspFragment ");
                }
                else {
                    writer.print("(");
                    /* Defect 213290 */
                    writer.print(GeneratorUtils.toJavaSourceType(attrInfos[i].getTypeName()));
                    writer.print(" ");
                }
                writer.print(attrInfos[i].getName());
                writer.print(") {");
                writer.println();
                writer.print("this.");
                writer.print(attrInfos[i].getName());
                writer.print(" = ");
                writer.print(attrInfos[i].getName());
                writer.print(";");
                writer.println();
                //set attribute in jsp context
                writer.print("jspContext.setAttribute(");
                writer.print(GeneratorUtils.quote(attrInfos[i].getName()));
                writer.print(", ");
                writer.print(attrInfos[i].getName());
                writer.print(");");
                writer.println("}");
                writer.println();
            }
        }
        
        if (ti.hasDynamicAttributes()) {
            writer.println("public void setDynamicAttribute(String uri, String localName, Object value) throws javax.servlet.jsp.JspException {");
            writer.println("if (uri == null) _jspx_dynamic_attrs.put(localName, value);");
            writer.println("}");
        }
        
    }
    
    protected void generateDoTagInitSection(ValidateTagFileResult validatorResult) {
        TagFileInfo tfi = (TagFileInfo)inputMap.get("TagFileInfo");
        TagInfo ti = tfi.getTagInfo();
        //PK65013 - start
        String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
        if (jspOptions.isModifyPageContextVariable()) {
            pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
        }
        //PK65013 - end
        
        //PM12658 start - keep track of doTag line number for debug mode (smap generation)
        result.setTagMethodLineNumber(((JavaFileWriter)writer).getCurrentLineNumber());
        //PM12658 end
        writer.println("public void doTag() throws javax.servlet.jsp.JspException, java.io.IOException {");
        writer.println("PageContext "+pageContextVar+" = (PageContext)jspContext;");
        writer.println("javax.servlet.http.HttpServletRequest request = (javax.servlet.http.HttpServletRequest) "+pageContextVar+".getRequest();");        //PK66299
        writer.println("javax.servlet.http.HttpServletResponse response = (javax.servlet.http.HttpServletResponse) "+pageContextVar+".getResponse();");    //PK66299
        writer.println("javax.servlet.http.HttpSession session = "+pageContextVar+".getSession();");
        writer.println("javax.servlet.ServletContext application = "+pageContextVar+".getServletContext();");
        writer.println("javax.servlet.ServletConfig config = "+pageContextVar+".getServletConfig();");
        writer.println("javax.servlet.jsp.JspWriter out = jspContext.getOut();");
        writer.println();
        
        // 247815 Start
        if (jspOptions.isUsePageTagPool()) {
            writer.println("java.util.HashMap _jspx_TagLookup = initTaglibLookup();");
            writer.println();
        }
        else if (jspOptions.isUseThreadTagPool()) {
            writer.println("java.util.HashMap _jspx_TagLookup = new java.util.HashMap();");
            writer.println();
        }
        // 247815 End

        // jsp2.1ELwork
        writer.println("_jspInit(config);");
        writer.println();
        
        // defect 386311 begin
        // set current JspContext on ELContext
        writer.println("jspContext.getELContext().putContext(JspContext.class,jspContext);");
        // defect 386311 end
        boolean varMapperInit = false;
        TagAttributeInfo[] attrInfos = ti.getAttributes();
        for (int i = 0; i < attrInfos.length; i++) {
        	String attrName = attrInfos[i].getName();
        	if(attrInfos[i].isDeferredMethod()||attrInfos[i].isDeferredValue()){
        		if(!varMapperInit){
        			writer.print("javax.el.VariableMapper _jspx_varmap= jspContext.getELContext().getVariableMapper();");
        			writer.println();
        			varMapperInit = true;
        		}
        		writer.print("javax.el.ValueExpression _jspx_ve");
                writer.print(Integer.toString(i));
                writer.print(" = _jspx_varmap.setVariable(");
                writer.print(GeneratorUtils.quote(attrName));
                writer.print(", ");
                if (attrInfos[i].isDeferredMethod()) {
                    writer.print("_el_expressionfactory");
                    writer.print(".createValueExpression(");
                    writer.print(GeneratorUtils.toGetterMethod(attrName));
                    writer.print(",javax.el.MethodExpression.class)");
                } else {
                    writer.print(GeneratorUtils.toGetterMethod(attrName));
                }
                writer.print(");");
                writer.println();
                
        	}else{
        		writer.print("if (" + GeneratorUtils.toGetterMethod(attrName) + " != null) ");
                writer.print(" "+pageContextVar+".setAttribute(");
        		writer.print(GeneratorUtils.quote(attrName));
            	writer.print(", ");
            	writer.print(GeneratorUtils.toGetterMethod(attrName));
            	writer.print(");");
            	writer.println();
        	}
        }

        if (ti.hasDynamicAttributes()) {
            writer.print(pageContextVar+".setAttribute(\"");
            writer.print(((TagFileTagInfo)ti).getDynamicAttributesMapName());
            writer.print("\", _jspx_dynamic_attrs);");
            writer.println();
        }
    }
    
    protected void generateDoTagSection(ValidateTagFileResult validatorResult, Document jspDocument) {
        writer.println("try {");
        if (jspConfiguration.isXml()) {
            boolean omitXmlDeclaration = true;
            Element outputElement = null;
            
            if (jspDocument.getElementsByTagNameNS(Constants.JSP_NAMESPACE, Constants.JSP_OUTPUT_TYPE).getLength() > 0) {
                outputElement = (Element)jspDocument.getDocumentElement().getElementsByTagNameNS(Constants.JSP_NAMESPACE, Constants.JSP_OUTPUT_TYPE).item(0);
                String s = outputElement.getAttribute("omit-xml-declaration");
                if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")) {
                    omitXmlDeclaration = false;
                }
                else if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
                    omitXmlDeclaration = true;
                }
            }
            
            if (omitXmlDeclaration == false) {
                String pageEncoding = validatorResult.getPageEncoding();
                if (pageEncoding == null || pageEncoding.equals("")) {
                    pageEncoding = jspConfiguration.getPageEncoding();
                    if (pageEncoding == null || pageEncoding.equals("")) {
                        pageEncoding = "UTF-8";    
                    }
                }
                writer.println("out.write(\"<?xml version=\\\"1.0\\\" encoding=\\\"" + pageEncoding + "\\\"?>\\n\");");
            }
            
            //PK17173 begin
            String doctype = "";
            if (outputElement!=null)
            	doctype=outputElement.getAttribute("doctype-root-element");
            if (outputElement != null && !doctype.equals("")) {    //PK17173 end
                String docTypeRootElement =  outputElement.getAttribute("doctype-root-element");
                String docTypeSystem = outputElement.getAttribute("doctype-system");
                String docTypePublic = outputElement.getAttribute("doctype-public");
                
                writer.print("out.write(\"<!DOCTYPE " + docTypeRootElement);
                if (docTypePublic.equals("") == false) {
                    writer.print(" PUBLIC \\\"" + docTypePublic + "\\\" \\\"" + docTypeSystem + "\\\">\\n\");");
                    writer.println();     
                }
                else {
                    writer.print(" SYSTEM \\\"" + docTypeSystem + "\\\">\\n\");");
                    writer.println();     
                }
            }
        }
    }
    
    protected void generateFinallySection() {
        writer.println("} catch( Throwable t ) {");
        writer.println("if( t instanceof javax.servlet.jsp.SkipPageException )");
        writer.println("    throw (javax.servlet.jsp.SkipPageException) t;");
        writer.println("if( t instanceof java.io.IOException )");
        writer.println("    throw (java.io.IOException) t;");
        writer.println("if( t instanceof IllegalStateException )");
        writer.println("    throw (IllegalStateException) t;");
        writer.println("if( t instanceof javax.servlet.jsp.JspException )");
        writer.println("    throw (javax.servlet.jsp.JspException) t;");
        writer.println("throw new javax.servlet.jsp.JspException(t);");
        writer.println("} finally {");
        writer.println("jspContext.getELContext().putContext(JspContext.class,super.getJspContext());"); // 393110
        writer.println("((org.apache.jasper.runtime.JspContextWrapper) jspContext).syncEndTagFile();");
        //247815 Start
        if (jspOptions.isUsePageTagPool()) {
            writer.println("cleanupTaglibLookup(_jspx_TagLookup);");
        }
        //247815 End
    }
}
