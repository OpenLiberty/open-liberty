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
package com.ibm.ws.jsp.translator.visitor.tagfiledep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.jsp.tagext.TagFileInfo;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.taglib.TagLibraryCache;
import com.ibm.ws.jsp.taglib.TagLibraryInfoImpl;
import com.ibm.ws.jsp.translator.JspTranslator;
import com.ibm.ws.jsp.translator.JspTranslatorFactory;
import com.ibm.ws.jsp.translator.utils.TagFileId;
import com.ibm.ws.jsp.translator.visitor.JspVisitor;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.JspVisitorResult;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorUsage;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.resource.JspInputSource;

public class TagFileDependencyVisitor extends JspVisitor {
    static final String TAGFILE_DEPENDENCY_ID = "TagFileDependency";
    protected TagLibraryCache tagLibraryCache = null;
    protected List processedCustomTags = new ArrayList();
    protected String jspUri = null;
    
    public TagFileDependencyVisitor(JspVisitorUsage visitorUsage,
                                    JspConfiguration jspConfiguration, 
                                    JspCoreContext context, 
                                    HashMap resultMap,
                                    JspVisitorInputMap inputMap)
        throws JspCoreException {
        super(visitorUsage, jspConfiguration, context, resultMap, inputMap);
        tagLibraryCache = (TagLibraryCache)inputMap.get("TagLibraryCache");
        jspUri = (String)inputMap.get("JspUri");
    }

    public JspVisitorResult getResult() throws JspCoreException {
        TagFileDependencyResult result = new TagFileDependencyResult(visitorUsage.getJspVisitorDefinition().getId());
        return (result);
    }
	
    protected void visitCustomTagStart(Element jspElement) throws JspCoreException {
        String nodeName = jspElement.getNamespaceURI() + "_" + jspElement.getLocalName(); // 245645.1
        if (processedCustomTags.contains(nodeName) == false) {
            processedCustomTags.add(nodeName);
            String uri = jspElement.getNamespaceURI();
            String prefix = jspElement.getPrefix();
            String tagName = jspElement.getLocalName();
            
            if (uri.startsWith("urn:jsptld:")) {
                uri = uri.substring(uri.indexOf("urn:jsptld:")+11);
            }
            else if (uri.startsWith("urn:jsptagdir:")) {
                uri = uri.substring(uri.indexOf("urn:jsptagdir:")+14);                        
            }
            TagLibraryInfoImpl tli = tagLibraryCache.getTagLibraryInfo(uri, prefix, jspUri);
            if (tli == null) {
                if (jspConfiguration.isXml() == false)
                	throw new JspCoreException("jsp.error.taglib.not.found", new Object[] {uri});
                else
                    return;
            }
            TagFileInfo tfi = tli.getTagFile(tagName);
            
            if (tfi != null) {
                scanForTagFileDependencies(uri, prefix, tagName, tli, tfi);
            }
            else {
                JspOptions jspOptions = (JspOptions)inputMap.get("JspOptions");
                if (jspOptions.isTrackDependencies()) {
                    if (uri.startsWith("/WEB-INF/tags")) {
                        tli = tagLibraryCache.reloadImplicitTld(uri);
                        if (tli != null) {
                            tfi = tli.getTagFile(tagName);
                            if (tfi != null) {
                                scanForTagFileDependencies(uri, prefix, tagName, tli, tfi);
                            }
                        }
                    }
                }
            }
        }
    }

    protected void scanForTagFileDependencies(String uri,
                                              String prefix,
                                              String tagName,
                                              TagLibraryInfoImpl tli,
                                              TagFileInfo tfi)
        throws JspCoreException {
        ArrayList tagFileDependencies = (ArrayList)inputMap.get("TagFileDependencies");
        TagFileId tagFileId = new TagFileId(prefix, uri, tagName);
        if (tagFileDependencies.contains(tagFileId) == false) {
            tagFileDependencies.add(tagFileId);
                
            JspInputSource tagFileInputSource = context.getJspInputSourceFactory().copyJspInputSource(tli.getInputSource(), tfi.getPath());
            JspOptions jspOptions = (JspOptions)inputMap.get("JspOptions"); // 396002
            JspConfiguration tagFileDepConfig = jspConfiguration.createEmptyJspConfiguration();
            if (tli!=null) { //I think this should always be the case
                if (tli.getRequiredVersion()!=null) {
                    tagFileDepConfig.setJspVersion(tli.getRequiredVersion());
                }
            }
            JspTranslator jspTranslator = JspTranslatorFactory.getFactory().createTranslator(TAGFILE_DEPENDENCY_ID, 
                                                                                             tagFileInputSource,
                                                                                             context, 
                                                                                             tagFileDepConfig,
                                                                                             jspOptions,  // 396002
                                                                                             tagLibraryCache.getImplicitTagLibPrefixMap());
                
                
            JspVisitorInputMap  tagFileInputMap = new JspVisitorInputMap();
            tagFileInputMap.put("TagLibraryCache", inputMap.get("TagLibraryCache"));
            tagFileInputMap.put("TagFileDependencies", tagFileDependencies);
            jspOptions = (JspOptions)inputMap.get("JspOptions");
            tagFileInputMap.put("JspOptions", jspOptions);
            
            HashMap results = jspTranslator.processVisitors(tagFileInputMap);
        }
    }
    
    protected void visitJspRootStart(Element jspElement) throws JspCoreException {}
    protected void visitPageDirectiveStart(Element jspElement) throws JspCoreException {}
    protected void visitJspGetPropertyStart(Element jspElement) throws JspCoreException {}
    protected void visitJspForwardStart(Element jspElement) throws JspCoreException {}
    protected void visitJspPluginStart(Element jspElement) throws JspCoreException {}
    protected void visitIncludeDirectiveStart(Element jspElement) throws JspCoreException {}
    protected void visitJspSetPropertyStart(Element jspElement) throws JspCoreException {}
    protected void visitJspIncludeStart(Element jspElement) throws JspCoreException {}
    protected void visitJspAttributeStart(Element jspElement) throws JspCoreException {}
    protected void visitJspElementStart(Element jspElement) throws JspCoreException {}
    protected void visitJspBodyStart(Element jspElement) throws JspCoreException {}
    protected void visitJspInvokeStart(Element jspElement) throws JspCoreException {}
    protected void visitJspDoBodyStart(Element jspElement) throws JspCoreException {}
    protected void visitTagDirectiveStart(Element jspElement) throws JspCoreException {}
    protected void visitAttributeDirectiveStart(Element jspElement) throws JspCoreException {}
    protected void visitVariableDirectiveStart(Element jspElement) throws JspCoreException {}
    protected void visitJspParamsStart(Element jspElement) throws JspCoreException {}
    protected void visitJspFallbackStart(Element jspElement) throws JspCoreException {}
    protected void visitJspParamStart(Element jspElement) throws JspCoreException {}
    protected void visitJspUseBeanStart(Element jspElement) throws JspCoreException {}
    protected void visitJspExpressionStart(Element jspElement) throws JspCoreException {}
    protected void visitJspScriptletStart(Element jspElement) throws JspCoreException {}
    protected void visitJspDeclarationStart(Element jspElement) throws JspCoreException {}
    protected void visitJspTextStart(Element jspElement) throws JspCoreException {}
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
            TagLibraryInfoImpl tli = tagLibraryCache.getTagLibraryInfo(uri, "", jspUri);
    
            if (tli != null) {
                System.out.println("tli for " + uri + " found");
                jspElement.setPrefix("");
                visitCustomTagStart(jspElement);
            }
        }
        // 245645.1 End
    }

    protected void visitJspParamEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspGetPropertyEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspRootEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspFallbackEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspUseBeanEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspForwardEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspPluginEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspSetPropertyEnd(Element jspElement) throws JspCoreException {}
    protected void visitIncludeDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspExpressionEnd(Element jspElement) throws JspCoreException {}
    protected void visitPageDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspIncludeEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspScriptletEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspDeclarationEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspTextEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspParamsEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspAttributeEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspElementEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspBodyEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspInvokeEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspDoBodyEnd(Element jspElement) throws JspCoreException {}
    protected void visitTagDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitAttributeDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitVariableDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitCustomTagEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspOutputEnd(Element jspElement) throws JspCoreException {}
    protected void visitUninterpretedTagEnd(Element jspElement) throws JspCoreException {}
    
    protected void visitCDataTag(CDATASection cdata) throws JspCoreException {}
}
