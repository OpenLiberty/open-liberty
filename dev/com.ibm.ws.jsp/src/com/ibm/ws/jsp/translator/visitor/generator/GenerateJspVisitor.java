/*******************************************************************************
 * Copyright (c) 1997, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsp.translator.visitor.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.PagesVersionHandler;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.JspVisitorResult;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorUsage;
import com.ibm.ws.jsp.translator.visitor.validator.ValidateJspResult;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.resource.translation.JspResources;

public class GenerateJspVisitor extends GenerateVisitor {
    protected GenerateJspResult result = null;
    protected ArrayList interfaces = new ArrayList();
    protected String jspServletBase = Constants.JSP_SERVLET_BASE;
    protected String serviceMethodName = Constants.SERVICE_METHOD_NAME;
    protected String jspClassName = null;
    protected String jspPackageName = null;
    protected String jspSourceFileName = null;

    static private Logger logger;
    private static final String CLASS_NAME = "com.ibm.ws.jsp.translator.visitor.generator.GenerateJspVisitor";
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }

    public GenerateJspVisitor(JspVisitorUsage visitorUsage,
                              JspConfiguration jspConfiguration,
                              JspCoreContext context,
                              HashMap resultMap,
                              JspVisitorInputMap inputMap) throws JspCoreException {
        super(visitorUsage, jspConfiguration, context, resultMap, inputMap, "JspValidate");
        result = new GenerateJspResult(visitorUsage.getJspVisitorDefinition().getId());
        JspResources jspFiles = (JspResources) inputMap.get("JspFiles");
        jspClassName = jspFiles.getClassName();
        jspPackageName = jspFiles.getPackageName();
        jspSourceFileName = jspFiles.getGeneratedSourceFile().toString();
        jspSourceFileName = jspSourceFileName.replace('\\', '/'); // defect 204907
        createWriter(jspFiles.getGeneratedSourceFile().getPath(), jspClassName, result.getCustomTagMethodJspIdMap()); //232818
    }

    public JspVisitorResult getResult() throws JspCoreException {
        closeWriter();
        return result;
    }

    public void visit(Document jspDocument, int visitCount) throws JspCoreException {
        ValidateJspResult validatorResult = (ValidateJspResult) resultMap.get("JspValidate");
        boolean genSessionVariable = validatorResult.isGenSessionVariable();

        switch (visitCount) {
            case CodeGenerationPhase.IMPORT_SECTION: {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                    logger.logp(Level.FINEST, CLASS_NAME, "visit", "entering code generation phase IMPORT_SECTION");
                }
                generateImportSection(validatorResult);
                break;
            }

            case CodeGenerationPhase.CLASS_SECTION: {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                    logger.logp(Level.FINEST, CLASS_NAME, "visit", "entering code generation phase CLASS_SECTION");
                }
                generateClassSection(validatorResult);
                if (PagesVersionHandler.isPages31OrHigherLoaded()) {
                    generateIsErrorOnELFoundMethod(jspConfiguration.errorOnELNotFound());
                    generateImportGetters();
                }
                break;
            }

            case CodeGenerationPhase.STATIC_SECTION: {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                    logger.logp(Level.FINEST, CLASS_NAME, "visit", "entering code generation phase STATIC_SECTION");
                }
                generateStaticSection();
                break;
            }

            case CodeGenerationPhase.INIT_SECTION: {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                    logger.logp(Level.FINEST, CLASS_NAME, "visit", "entering code generation phase INIT_SECTION");
                }

                generateInitSection(validatorResult);
                break;
            }

            case CodeGenerationPhase.METHOD_INIT_SECTION: {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                    logger.logp(Level.FINEST, CLASS_NAME, "visit", "entering code generation phase METHOD_INIT_SECTION");
                }
                generateServiceInitSection(validatorResult, genSessionVariable);
                break;
            }

            case CodeGenerationPhase.METHOD_SECTION: {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                    logger.logp(Level.FINEST, CLASS_NAME, "visit", "entering code generation phase METHOD_SECTION");
                }

                generateServiceSection(validatorResult, genSessionVariable, jspDocument);
                break;
            }

            case CodeGenerationPhase.FINALLY_SECTION: {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
                    logger.logp(Level.FINEST, CLASS_NAME, "visit", "entering code generation phase FINALLY_SECTION");
                }
                generateFinallySection();
                break;
            }
        }

        super.visit(jspDocument, visitCount);

        if (visitCount == CodeGenerationPhase.FINALLY_SECTION) {
            writer.println("}");

            writer.println("}");
            boolean poolingTagMethodsCreated = false;
            for (Iterator itr = methodWriterList.iterator(); itr.hasNext();) {
                MethodWriter methodWriter = (MethodWriter) itr.next();
                //247815 Start
                if (methodWriter instanceof InitTaglibLookupWriter) {
                    InitTaglibLookupWriter w = (InitTaglibLookupWriter) methodWriter;
                    w.complete();
                    poolingTagMethodsCreated = true;
                } else if (methodWriter instanceof CleanupTaglibLookupWriter) {
                    CleanupTaglibLookupWriter w = (CleanupTaglibLookupWriter) methodWriter;
                    w.complete();
                }
                //247815 End
                writer.printMultiLn(methodWriter.toString());
            }

            //247815 Start
            if ((jspOptions.isUsePageTagPool() || jspOptions.isUseThreadTagPool()) && poolingTagMethodsCreated == false) {
                InitTaglibLookupWriter initTaglibLookupWriter = new InitTaglibLookupWriter(jspOptions.isUseThreadTagPool());
                initTaglibLookupWriter.complete();
                writer.printMultiLn(initTaglibLookupWriter.toString());
                CleanupTaglibLookupWriter cleanupTaglibLookupWriter = new CleanupTaglibLookupWriter(jspOptions.isUseThreadTagPool());
                cleanupTaglibLookupWriter.complete();
                writer.printMultiLn(cleanupTaglibLookupWriter.toString());
            }
            //247815 End

            if (fragmentHelperClassWriter.isUsed()) {
                fragmentHelperClassWriter.generatePostamble();
                writer.printMultiLn(fragmentHelperClassWriter.toString());
            }

            writer.println("}");

            if (jspOptions.isKeepGenerated() == true) {
                writer.println("/*");
                writer.println(jspSourceFileName + " was generated @ " + new java.util.Date());
                writer.println(GeneratorUtils.fullClassfileInformation);
                writer.println(" ");
                writer.println("********************************************************");
                writer.println("The JSP engine configuration parameters were set as follows:");
                writer.println(jspOptions.toString());
                writer.println("********************************************************");
                writer.println("The following JSP Configuration Parameters were obtained from web.xml:");
                writer.println(" ");
                writer.println("prelude list = [" + jspConfiguration.getPreludeList() + "]");
                writer.println("coda list = [" + jspConfiguration.getCodaList() + "]");
                writer.println("elIgnored = [" + jspConfiguration.elIgnored() + "]");
                writer.println("pageEncoding = [" + jspConfiguration.getPageEncoding() + "]");
                writer.println("isXML = [" + jspConfiguration.isXml() + "]");
                writer.println("scriptingInvalid = [" + jspConfiguration.scriptingInvalid() + "]");
                writer.println("trimDirectiveWhitespaces = [" + jspConfiguration.isTrimDirectiveWhitespaces() + "]"); // jsp2.1work
                writer.println("deferredSyntaxAllowedAsLiteral = [" + jspConfiguration.isDeferredSyntaxAllowedAsLiteral() + "]"); // jsp2.1ELwork

                writer.println("*/");

            }

        }

    }

    // Added for Pages 3.1's errorOnELNotFound option
    private void generateIsErrorOnELFoundMethod(boolean flag) {
        writer.println("public boolean isErrorOnELNotFound() {");
        writer.println("return " + flag + ";");
        writer.println("}");
    }

    // Added for Pages 3.1, Spec Issue 44 
    private void generateImportGetters() {
        writer.println("public java.util.List<String> getImportClassList() {");
        writer.println("return importClassList;");
        writer.println("}");
        writer.println();
        writer.println("public java.util.List<String> getImportPackageList() {");
        writer.println("return importPackageList;");
        writer.println("}");
        writer.println();
        writer.println("public java.util.List<String> getImportStaticList() {");
        writer.println("return importStaticList;");
        writer.println("}");
    }

    protected void generateImportSection(ValidateJspResult validatorResult) {
        String servletPackageName = jspPackageName;

        writer.println("package " + servletPackageName + ";");
        writer.println();

        for (int i = 0; i < Constants.STANDARD_IMPORTS.length; i++)
            writer.println("import " + (String) Constants.STANDARD_IMPORTS[i] + ";");

        writer.println();
    }

    protected void generateClassSection(ValidateJspResult validatorResult) {
        writer.println();
        writer.print("public final class " + jspClassName + " extends ");
        String extendsClass = validatorResult.getExtendsClass();
        writer.print(extendsClass == null ? jspServletBase : extendsClass);
        interfaces.add("com.ibm.ws.jsp.runtime.JspClassInformation");
        if (PagesVersionHandler.isPages31OrHigherLoaded()) {
            interfaces.add("com.ibm.ws.jsp.runtime.DirectiveInfo");
        }

        /*
         * SingleThreadModel was removed in Servlet 6.0 (Pages 3.1)
         * For Page 3.1, the jsp service method is synchronized instead (performance hit)
         * isThreadSafe is not recommended anymore.
         */

        boolean singleThreaded = validatorResult.isSingleThreaded();

        if (PagesVersionHandler.isPages30OrLowerLoaded()) {
            if (singleThreaded) {
                interfaces.add("SingleThreadModel");
            }
        } else if(PagesVersionHandler.isPages31Loaded()) { // log warning only for 3.1
            if (singleThreaded) {
                logger.logp(Level.WARNING, CLASS_NAME, "generateClassSection", "jsp.isthreadsafe.warning");
            }
        }

        if (interfaces.size() != 0) {
            writer.println();
            writer.print("     implements");

            for (int i = 0; i < interfaces.size() - 1; i++)
                writer.print(" " + interfaces.get(i) + ",");
            writer.print(" " + interfaces.get(interfaces.size() - 1));
        }

        writer.println(" {");
        writer.println();
        GeneratorUtils.generateFactoryInitialization(writer, jspConfiguration.getConfigManager().isJCDIEnabled());
        writer.println();
        GeneratorUtils.generateDependencyList(writer, validatorResult, context, jspOptions.isTrackDependencies());
        writer.println();

        // LIDB4147-24

        if (!jspOptions.isDisableResourceInjection()) { //PM06063
            GeneratorUtils.generateInjectionSection(writer);
        } //PM06063

        writer.println();

        // begin 228118: JSP container should recompile if debug enabled and jsp was not compiled in debug.
        //GeneratorUtils.generateVersionInformation(writer);
        GeneratorUtils.generateVersionInformation(writer, jspOptions.isDebugEnabled());
        // end 228118: JSP container should recompile if debug enabled and jsp was not compiled in debug.

        if (!(jspOptions.isUsePageTagPool() || jspOptions.isUseThreadTagPool())) {
            GeneratorUtils.generate_tagCleanUp_methods(writer, !jspOptions.isDisableResourceInjection()); // PH49514
        }

        GeneratorUtils.generate_finalCleanUp_method(writer, jspOptions);

        if(!jspOptions.isDisableResourceInjection()){
            GeneratorUtils.generate_tagPostConstruct_method(writer);
        }

        // PK81147 start
        // declare class variable to indicate whether _jspInit() has been called.
        writer.println();
        writer.println("private boolean _jspx_isJspInited = false;");
        writer.println();

        if (PagesVersionHandler.isPages31OrHigherLoaded()) {
            writer.println();
            writer.println("private static java.util.List<String> importPackageList = new java.util.ArrayList<String>();");
            writer.println("private static java.util.List<String> importClassList = new java.util.ArrayList<String>();");
            writer.println("private static java.util.List<String> importStaticList = new java.util.ArrayList<String>();");
            writer.println();

            // Cannot place this in the ImportGenerator since that is only run when the import directive is included in the page
            // the imports below are required for all pages 
            writer.println("static {");
            // Pages 1.10 Directive Packages java.lang.*, jakarta.servlet.*, jakarta.servlet.jsp.*, and jakarta.servlet.http.* are imported implicitly by the JSP container.
            for(String jakartaPackage : Constants.JAKARTA_STANDARD_IMPORTS) {
                writer.println("importPackageList.add(\""+ jakartaPackage + "\");");
            }
            writer.println("}");
        }

        // PK81147 end
        if (validatorResult.getInfo() != null) {
            writer.println();
            writer.println("public String getServletInfo() {");
            writer.print("return ");
            writer.print(GeneratorUtils.quote(validatorResult.getInfo()));
            writer.println(";");
            writer.print("}");
            writer.println();
        }
    }

    protected void generateStaticSection() {
        writer.println();
        writer.println("static {");
    }

    protected void generateInitSection(ValidateJspResult validatorResult) throws JspCoreException {
        writer.println("}");
        writer.println();
        GeneratorUtils.generateInitSectionCode(writer, GeneratorUtils.JSP_FILE_TYPE, jspOptions); //PM06063
        writer.println();
        GeneratorUtils.generateELFunctionCode(writer, validatorResult);
        writer.println();
    }

    protected void generateServiceInitSection(ValidateJspResult validatorResult, boolean genSessionVariable) {
        writer.println();

        /*
         * Mark recommened using synchonized, but with a performance caveat
         * https://github.com/jakartaee/pages/issues/206#issuecomment-934272204
         */
        String sync = "";
        if (PagesVersionHandler.isPages31Loaded()) {  // isThreadSafe is removed in 4.0; synchronized only applies to 3.1
            if (validatorResult.isSingleThreaded()) {
                sync = " synchronized";
            }
        }

        writer.println(
                       "public" + sync + " void "
                       + serviceMethodName
                       + "("
                       + "HttpServletRequest request, "
                       + "HttpServletResponse  response)");

        writer.println("    throws java.io.IOException, ServletException {");
        writer.println();
        result.setServiceMethodLineNumber(((JavaFileWriter) writer).getCurrentLineNumber());
        //writer.println("JspFactory _jspxFactory = null;");
        writer.println("PageContext pageContext = null;");

        GeneratorUtils.generate__jspTagList_variable(writer);

        if (genSessionVariable)
            writer.println("HttpSession session = null;");

        boolean isErrorPage = validatorResult.isErrorPage();
        if (isErrorPage) {
            writer.println("Throwable exception = org.apache.jasper.runtime.JspRuntimeLibrary.getThrowable(request);");
        }

        writer.println("ServletContext application = null;");
        writer.println("ServletConfig config = null;");
        writer.println("JspWriter out = null;");
        writer.println("Object page = this;");
        writer.println();
        writer.println("JspWriter _jspx_out = null;");
        writer.println();

        // PK81147 start
        // add check to see if the _jspInit() method has been called yet.
        //If not we need to call it in order to init the annotation helper

        writer.println();
        writer.println("if(!_jspx_isJspInited){");
        writer.println("this._jspInit();");
        writer.println("}");
        writer.println();

        // PK81147 end

        // 247815 Start
        if (jspOptions.isUsePageTagPool()) {
            writer.println("java.util.HashMap _jspx_TagLookup = initTaglibLookup();"); //247815
        } else if (jspOptions.isUseThreadTagPool()) {
            writer.println("java.util.HashMap _jspx_TagLookup = initTaglibLookup(request);"); //247815
        }
        // 247815 End
    }

    protected void generateServiceSection(ValidateJspResult validatorResult, boolean genSessionVariable, Document jspDocument) {
        writer.println();
        writer.println("try {");
        writer.println();
        //writer.println("_jspxFactory = JspFactory.getDefaultFactory();");
        String contentType = validatorResult.getContentType();

        // 221843: obtain whether autoResponseEncoding is enabled for this web module.
        boolean autoResponseEncoding = jspOptions.isAutoResponseEncoding();

        if (contentType == null) {
            if (jspConfiguration.isXml()) {
                contentType = "text/xml";
            } else {
                contentType = "text/html";
            }
        }

        if (contentType.indexOf("charset=") < 0) {
            if (jspConfiguration.isXml()) {
                // 221843: add only if autoResponseEncoding is false. Else leave it to webcontainer.
                if (autoResponseEncoding == false) {
                    contentType += ";charset=UTF-8";
                }
            } else {
                String pageEncoding = validatorResult.getPageEncoding();
                if (pageEncoding != null && pageEncoding.equals("") == false) {
                    contentType += ";charset=" + pageEncoding;
                } else {
                    //pageEncoding = jspConfiguration.getPageEncoding();
                    String responseEncoding = jspConfiguration.getResponseEncoding();
                    if (responseEncoding != null && !responseEncoding.equals("")) {
                        contentType += ";charset=" + responseEncoding;
                    }

                    /*
                     * -
                     * if (pageEncoding != null && pageEncoding.equals("") == false) {
                     * contentType += ";charset=" + pageEncoding;
                     * }
                     */
                    // 248722: remove defect 221843; spec mandates that if no charset is specified, then defer to webcontainer.
                    // section JSP.4.2 Response Character Encoding
                    else {
                        // 221843: add only if autoResponseEncoding is false. Else leave it to webcontainer.
                        /*
                         * else if(autoResponseEncoding == false){
                         * contentType += ";charset=ISO-8859-1";
                         * }
                         */
                        logger.logp(Level.FINEST, CLASS_NAME, "generateServiceSection", "JSP did not specify charset; defer to webcontainer");
                    }
                    // 248722: spec mandates that if no charset is specified, then defer to webcontainer.
                }
            }
        }

        writer.println("response.setContentType(" + writer.quoteString(contentType) + ");");

        String error = validatorResult.getError();
        int bufferSize = validatorResult.getBufferSize();
        boolean autoFlush = validatorResult.isAutoFlush();
        writer.println(
                       "pageContext = _jspxFactory.getPageContext(this, request, response, "
                       + writer.quoteString(error)
                       + ", "
                       + genSessionVariable
                       + ", "
                       + bufferSize
                       + ", "
                       + autoFlush
                       + ");");
        writer.println();
        writer.println("application = pageContext.getServletContext();");
        writer.println("config = pageContext.getServletConfig();");
        if (genSessionVariable)
            writer.println("session = pageContext.getSession();");
        writer.println("out = pageContext.getOut();");
        writer.println("_jspx_out = out;");
        writer.println();
        writer.println("pageContext.setAttribute(\"" + Constants.JSP_EXPRESSION_FACTORY_OBJECT + "\", _el_expressionfactory);");

        if (jspConfiguration.isXml()) {
            boolean omitXmlDeclaration = false;

            if (jspDocument.getElementsByTagNameNS(Constants.JSP_NAMESPACE, Constants.JSP_ROOT_TYPE).getLength() > 0) {
                omitXmlDeclaration = true;
            }
            Element outputElement = null;
            if (jspDocument.getElementsByTagNameNS(Constants.JSP_NAMESPACE, Constants.JSP_OUTPUT_TYPE).getLength() > 0) {
                outputElement = (Element) jspDocument.getDocumentElement().getElementsByTagNameNS(Constants.JSP_NAMESPACE, Constants.JSP_OUTPUT_TYPE).item(0);
                String s = outputElement.getAttribute("omit-xml-declaration");
                if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")) {
                    omitXmlDeclaration = false;
                } else if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
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
            if (outputElement != null)
                doctype = outputElement.getAttribute("doctype-root-element");
            if (outputElement != null && !doctype.equals("")) { //PK17173 end
                String docTypeRootElement = outputElement.getAttribute("doctype-root-element");
                String docTypeSystem = outputElement.getAttribute("doctype-system");
                String docTypePublic = outputElement.getAttribute("doctype-public");

                writer.print("out.write(\"<!DOCTYPE " + docTypeRootElement);
                if (docTypePublic.equals("") == false) {
                    writer.print(" PUBLIC \\\"" + docTypePublic + "\\\" \\\"" + docTypeSystem + "\\\">\\n\");");
                    writer.println();
                } else {
                    writer.print(" SYSTEM \\\"" + docTypeSystem + "\\\">\\n\");");
                    writer.println();
                }
            }
        }
    }

    protected void generateFinallySection() {
        writer.println();
        writer.println("} catch (Throwable t) {");
        writer.println("if (!(t instanceof javax.servlet.jsp.SkipPageException)){");
        writer.println("out = _jspx_out;");
        writer.println("if (out != null && out.getBufferSize() != 0)");
        writer.println("out.clearBuffer();");
        writer.println("if (pageContext != null) pageContext.handlePageException(t);");
        writer.println("}");
        // begin 242714: enhance error reporting for SkipPageException.
        writer.println("else if (t instanceof com.ibm.ws.jsp.runtime.WsSkipPageException){");
        writer.println("((com.ibm.ws.jsp.runtime.WsSkipPageException)t).printStackTraceIfTraceEnabled();");
        writer.println("}");
        // end 242714: enhance error reporting for SkipPageException.

        writer.println("} finally {");

        writer.println("this._jsp_performFinalCleanUp(_jspTagList, pageContext);");

        //247815 Start 
        if (jspOptions.isUsePageTagPool()) {
            writer.println("cleanupTaglibLookup(_jspx_TagLookup);");
        } else if (jspOptions.isUseThreadTagPool()) {
            writer.println("cleanupTaglibLookup(request, _jspx_TagLookup);");
        }
        //247815 End
    }
}
