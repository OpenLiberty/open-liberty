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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.translator.resource.ResourcesImpl;
import com.ibm.ws.jsp.translator.utils.JspId;
import com.ibm.ws.jsp.translator.utils.SmapGenerator;
import com.ibm.ws.jsp.translator.utils.SmapStratum;
import com.ibm.ws.jsp.translator.visitor.JspVisitor;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.JspVisitorResult;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorUsage;
import com.ibm.ws.jsp.translator.visitor.generator.GenerateJspResult;
import com.ibm.ws.jsp.translator.visitor.generator.GenerateTagFileResult;
import com.ibm.ws.jsp.translator.visitor.validator.ValidateResult; // 245645.1
import com.ibm.wsspi.jsp.context.JspCoreContext;

public class SmapVisitor extends JspVisitor {
    private SmapGenerator smapGenerator = null;
    private SmapStratum smapStratum = null;
    private List fileList = new ArrayList();
    private ResourcesImpl generatedFiles = null;
    private Map cdataJspIdMap = null;
    private int serviceMethodLineNumber=0;
    private int tagMethodLineNumber=0; //PM12658
    //private boolean firstTime=true;
    private Map customTagMethodJspIdMap = null; //232818
    private String validateResultId = null; // 245645.1
    
    public SmapVisitor(JspVisitorUsage visitorUsage,
                       JspConfiguration jspConfiguration, 
                       JspCoreContext context, 
                       HashMap resultMap,
                       JspVisitorInputMap inputMap) throws JspCoreException {
        super(visitorUsage, jspConfiguration, context, resultMap, inputMap);
        smapGenerator = new SmapGenerator();
        smapStratum = new SmapStratum("JSP");
        if (inputMap.containsKey("JspFiles")) {
            generatedFiles = (ResourcesImpl)inputMap.get("JspFiles");
            validateResultId = "JspValidate"; // 245645.1
        }
        else if (inputMap.containsKey("TagFileFiles")) {
            generatedFiles = (ResourcesImpl)inputMap.get("TagFileFiles");
            validateResultId = "TagFileValidate"; // 245645.1
        }
        smapGenerator.setOutputFileName(generatedFiles.getGeneratedSourceFile().getName());
        cdataJspIdMap = (Map)inputMap.get("CdataJspIdMap");
        // 232818 Start
        GenerateJspResult genJspResult=(GenerateJspResult)resultMap.get("JspGenerate"); 
        if (genJspResult != null) {
        	serviceMethodLineNumber=genJspResult.getServiceMethodLineNumber();
        	customTagMethodJspIdMap = genJspResult.getCustomTagMethodJspIdMap();
        }
        else {
            GenerateTagFileResult genTagFileResult=(GenerateTagFileResult)resultMap.get("TagFileGenerate");
            tagMethodLineNumber= genTagFileResult.getTagMethodLineNumber();  //PM12658
            customTagMethodJspIdMap = genTagFileResult.getCustomTagMethodJspIdMap();
        }
        // 232818 End
    }  

    private void addStartLineInfo(Element jspElement) {
        JspId jspId = new JspId(jspElement.getAttributeNS(Constants.JSP_NAMESPACE, "id"));
        if (fileList.contains(jspId.getFilePath()) == false) {
            smapStratum.addFile(jspId.getFileName(), jspId.getFilePath());
            fileList.add(jspId.getFilePath()); //227804
        }
        if (jspId.getStartGeneratedLineNum() > 0) {
        	//227804
            smapStratum.addLineData(jspId.getStartSourceLineNum(), 
                                    jspId.getFileName(), 
                                    1,
                                    jspId.getStartGeneratedLineNum(), 
									jspId.getStartGeneratedLineCount());
        }
    }   
     
    private void addEndLineInfo(Element jspElement) {
        JspId jspId = new JspId(jspElement.getAttributeNS(Constants.JSP_NAMESPACE, "id"));
        
        if (jspId.getEndGeneratedLineNum() > 0) {
            //227804
            smapStratum.addLineData(jspId.getEndSourceLineNum(), 
                                    jspId.getFileName(),
                                    1,
                                    jspId.getEndGeneratedLineNum(), 
                                    jspId.getEndGeneratedLineCount());
        }
    }
    
    public JspVisitorResult getResult() throws JspCoreException {
        smapGenerator.addStratum(smapStratum, true);
        JspOptions jspOptions = (JspOptions)inputMap.get("JspOptions");
        if (jspOptions.isKeepGenerated()) {
            FileOutputStream fos = null;
            PrintWriter so = null;
            try {
                File outSmap = new File(generatedFiles.getClassFile().getPath() + ".smap");
                fos = new FileOutputStream(outSmap);
                so = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"));
                so.print(smapGenerator.getString());
            }
            catch (IOException e) {
                throw new JspCoreException(e);
            }
            finally {
                if (so != null) {
                    so.close();
                }
                if (fos != null) {
                    try {
                        fos.close();
                    }
                    catch (IOException e) {}
                }
            }
        }
        SmapVisitorResult result = new SmapVisitorResult(visitorUsage.getJspVisitorDefinition().getId(), smapGenerator);
        return (result);
    }
    
    protected void visitCustomTagStart(Element jspElement) throws JspCoreException {

        // 232818 Start
        JspId jspId = new JspId(jspElement.getAttributeNS(Constants.JSP_NAMESPACE, "id"));
        // begin: 258459    Smap fails when compiling JSP examples    WAS.jsp    
        if (fileList.contains(jspId.getFilePath()) == false) {
            smapStratum.addFile(jspId.getFileName(), jspId.getFilePath());
            fileList.add(jspId.getFilePath()); //227804
        }
        // end: 258459    Smap fails when compiling JSP examples    WAS.jsp    

        String customTagMethodJspId = (String)customTagMethodJspIdMap.get(jspElement);
        if (customTagMethodJspId != null) {
        	String syntaxLineNum = customTagMethodJspId.substring(0, customTagMethodJspId.indexOf(','));
            String lineCount = customTagMethodJspId.substring(customTagMethodJspId.indexOf(',')+1);
            smapStratum.addLineData(jspId.getStartSourceLineNum(), 
                                    jspId.getFileName(), 
                                    1,
                                    Integer.valueOf(syntaxLineNum).intValue(), 
                                    Integer.valueOf(lineCount).intValue());
        }
        // 232818 End
        // begin: 258459    Smap fails when compiling JSP examples    WAS.jsp    
        //addStartLineInfo(jspElement);
        if (jspId.getStartGeneratedLineNum() > 0) {
        	//227804
            smapStratum.addLineData(jspId.getStartSourceLineNum(), 
                                    jspId.getFileName(), 
                                    1,
                                    jspId.getStartGeneratedLineNum(), 
									jspId.getStartGeneratedLineCount());
        }
        // end: 258459    Smap fails when compiling JSP examples    WAS.jsp    
        
    }
    
    protected void visitJspRootStart(Element jspElement) throws JspCoreException {
        // 232818 Start
        JspId jspId = new JspId(jspElement.getAttributeNS(Constants.JSP_NAMESPACE, "id"));
        if (fileList.contains(jspId.getFilePath()) == false) {
            smapStratum.addFile(jspId.getFileName(), jspId.getFilePath());
            fileList.add(jspId.getFilePath()); //227804
        }
        if (serviceMethodLineNumber > 0) {
            smapStratum.addLineData(1, 
                    jspId.getFileName(), 
                    1,
                    serviceMethodLineNumber, 
                    1);
        } else if (tagMethodLineNumber > 0) { //PM12658 start
            smapStratum.addLineData(1, 
                    jspId.getFileName(), 
                    1,
                    tagMethodLineNumber, 
                    1);
        	
        }
        //PM12658 end
        // 232818 End
   	}
    
    protected void visitPageDirectiveStart(Element jspElement) throws JspCoreException {}
    
    protected void visitJspGetPropertyStart(Element jspElement) throws JspCoreException {
        addStartLineInfo(jspElement);
    }
    
    protected void visitJspForwardStart(Element jspElement) throws JspCoreException {
        addStartLineInfo(jspElement);
    }
    
    protected void visitJspPluginStart(Element jspElement) throws JspCoreException {
        addStartLineInfo(jspElement);
    }
    
    protected void visitIncludeDirectiveStart(Element jspElement) throws JspCoreException {}
    
    protected void visitJspSetPropertyStart(Element jspElement) throws JspCoreException {
        addStartLineInfo(jspElement);
    }
    
    protected void visitJspIncludeStart(Element jspElement) throws JspCoreException {
        addStartLineInfo(jspElement);
    }
    
    protected void visitJspAttributeStart(Element jspElement) throws JspCoreException {
        addStartLineInfo(jspElement);
    }
    
    protected void visitJspElementStart(Element jspElement) throws JspCoreException {
        addStartLineInfo(jspElement);
    }
    
    protected void visitJspBodyStart(Element jspElement) throws JspCoreException {
        addStartLineInfo(jspElement);
    }
    
    protected void visitJspInvokeStart(Element jspElement) throws JspCoreException {
        addStartLineInfo(jspElement);
    }
    
    protected void visitJspDoBodyStart(Element jspElement) throws JspCoreException {
        addStartLineInfo(jspElement);
    }
    
    protected void visitTagDirectiveStart(Element jspElement) throws JspCoreException {}
    
    protected void visitAttributeDirectiveStart(Element jspElement) throws JspCoreException {}
    
    protected void visitVariableDirectiveStart(Element jspElement) throws JspCoreException {}
    
    protected void visitJspParamsStart(Element jspElement) throws JspCoreException {}
    
    protected void visitJspFallbackStart(Element jspElement) throws JspCoreException {
        addStartLineInfo(jspElement);
    }
    
    protected void visitJspParamStart(Element jspElement) throws JspCoreException {
        addStartLineInfo(jspElement);
    }
    
    protected void visitJspUseBeanStart(Element jspElement) throws JspCoreException {
        addStartLineInfo(jspElement);
    }
    
    protected void visitJspExpressionStart(Element jspElement) throws JspCoreException {
        addStartLineInfo(jspElement);
    }
    
    protected void visitJspScriptletStart(Element jspElement) throws JspCoreException {
        addScriptingLineInfo(jspElement); //260728
    }
    
    protected void visitJspDeclarationStart(Element jspElement) throws JspCoreException {
        addScriptingLineInfo(jspElement); //260728
    }
    
    protected void visitJspTextStart(Element jspElement) throws JspCoreException {
        addStartLineInfo(jspElement);
    }
    
    protected void visitJspOutputStart(Element jspElement) throws JspCoreException {}
    
    protected void visitUninterpretedTagStart(Element jspElement) throws JspCoreException {
    	// 245645.1 Start
        String uri = jspElement.getNamespaceURI();
        if (uri != null) {
            if (uri.startsWith("urn:jsptld:")) {
                uri = uri.substring(uri.indexOf("urn:jsptld:") + 11);
            }
            else if (uri.startsWith("urn:jsptagdir:")) {
                uri = uri.substring(uri.indexOf("urn:jsptagdir:") + 14);
            }
            ValidateResult validatorResult = (ValidateResult)resultMap.get(validateResultId);
    
            if (validatorResult.getTagLibMap().get(uri) != null) {
            	visitCustomTagStart(jspElement);
            }
            else {
                addStartLineInfo(jspElement);
            }
        }
        else {
            addStartLineInfo(jspElement);
        }
        // 245645.1 End
    }

    protected void visitJspParamEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitJspGetPropertyEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitJspRootEnd(Element jspElement) throws JspCoreException {}
    
    protected void visitJspFallbackEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitJspUseBeanEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitJspForwardEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitJspPluginEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitJspSetPropertyEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitIncludeDirectiveEnd(Element jspElement) throws JspCoreException {}
    
    protected void visitJspExpressionEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitPageDirectiveEnd(Element jspElement) throws JspCoreException {}
    
    protected void visitJspIncludeEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitJspScriptletEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitJspDeclarationEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitJspTextEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitJspParamsEnd(Element jspElement) throws JspCoreException {}
    
    protected void visitJspAttributeEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitJspElementEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitJspBodyEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitJspInvokeEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitJspDoBodyEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitTagDirectiveEnd(Element jspElement) throws JspCoreException {}
    
    protected void visitAttributeDirectiveEnd(Element jspElement) throws JspCoreException {}
    
    protected void visitVariableDirectiveEnd(Element jspElement) throws JspCoreException {}
    
    protected void visitCustomTagEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitJspOutputEnd(Element jspElement) throws JspCoreException {}
    
    protected void visitUninterpretedTagEnd(Element jspElement) throws JspCoreException {
        addEndLineInfo(jspElement);
    }
    
    protected void visitCDataTag(CDATASection cdata) throws JspCoreException {
        String id = (String)cdataJspIdMap.get(new Integer(cdata.hashCode()));
        if (id != null) {
            JspId jspId = new JspId(id);
            if (fileList.contains(jspId.getFilePath()) == false) {
                smapStratum.addFile(jspId.getFileName(), jspId.getFilePath());
                fileList.add(jspId.getFilePath()); //227804
            }
            if (jspId.getStartGeneratedLineNum() > 0) {
                smapStratum.addLineData(jspId.getStartSourceLineNum(), 
                                        jspId.getFileName(), 
										jspId.getStartGeneratedLineCount(),
                                        jspId.getStartGeneratedLineNum(), 
                                        1);
                                        
            }
        }
    }

    /* New method for Defect 260728 */
    private void addScriptingLineInfo(Element jspElement) {
        JspId jspId = new JspId(jspElement.getAttributeNS(Constants.JSP_NAMESPACE, "id"));
        if (fileList.contains(jspId.getFilePath()) == false) {
            smapStratum.addFile(jspId.getFileName(), jspId.getFilePath());
            fileList.add(jspId.getFilePath()); //227804
        }
        if (jspId.getStartGeneratedLineNum() > 0) {
            CDATASection cdata = (CDATASection)jspElement.getChildNodes().item(0);
            BufferedReader reader = new BufferedReader(new StringReader(cdata.getData()));
            int skipLeadingCount = 0;
            int skipTrailingCount = 0;
            int lineCount = 0;
            boolean contentFound = false;
            try {
                for (String line = null;(line = reader.readLine()) != null;) {
                    lineCount++;
                    if (line.trim().length() < 1) {
                        if (contentFound) {
                            skipTrailingCount++;
                        }
                        else {
                            skipLeadingCount++;
                        }
                    }
                    else {
                        contentFound = true;
                        skipTrailingCount = 0;
                    }
                }
            }
            catch (IOException ex) {}
            smapStratum.addLineData(jspId.getStartSourceLineNum()+skipLeadingCount,
                                    jspId.getFileName(),
                                    lineCount - skipLeadingCount - skipTrailingCount,
                                    jspId.getStartGeneratedLineNum()+skipLeadingCount,
                                    1);
        }
    }
}
