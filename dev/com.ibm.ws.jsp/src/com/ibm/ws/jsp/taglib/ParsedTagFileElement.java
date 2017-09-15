/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.taglib;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.jsp.tagext.TagFileInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfigurationManager;
import com.ibm.ws.jsp.translator.JspTranslator;
import com.ibm.ws.jsp.translator.JspTranslatorFactory;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.tagfilescan.TagFileScanResult;
import com.ibm.wsspi.jsp.context.translation.JspTranslationContext;
import com.ibm.wsspi.jsp.resource.JspInputSource;

/**
 *
 */
public class ParsedTagFileElement {
    
    String path;
    String tagFileName;
    
    static final protected Logger logger = Logger.getLogger("com.ibm.ws.jsp");
    static final protected Level logLevel = Level.FINEST;
    private static final String CLASS_NAME="com.ibm.ws.jsp.taglib.ParsedTagFileElement";
        
    public ParsedTagFileElement(String path, String tagFileName) {
        this.path = path;
        this.tagFileName = tagFileName;
    }
    
    @FFDCIgnore({JspCoreException.class})
    public TagFileInfo createTagFileObject(TagLibraryInfoImpl tli, JspTranslationContext ctxt, JspInputSource tagFileInputSource, JspOptions jspOptions, JspConfigurationManager configManager) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)) {
            logger.logp(Level.FINEST, CLASS_NAME, "createTagFileObject", "about to do createTagFileObject for = ["+path+" "+tagFileName+"]");
        }
        JspTranslator jspTranslator;
        TagFileInfo tfi = null;
        try {
            jspTranslator = JspTranslatorFactory.getFactory().createTranslator(TagLibraryCache.TAGFILE_SCAN_ID, 
                                                                                             tagFileInputSource, 
                                                                                             ctxt, 
                                                                                             configManager.createJspConfiguration(),
                                                                                             jspOptions, // 396002
                                                                                             new HashMap());
            JspVisitorInputMap  inputMap = new JspVisitorInputMap();
            inputMap.put("TagLibraryInfo", tli);
            inputMap.put("TagFileName", tagFileName);
            inputMap.put("TagFilePath", path);
            HashMap results = jspTranslator.processVisitors(inputMap);
            TagFileScanResult result = (TagFileScanResult)results.get("TagFileScan");
            tfi = new TagFileInfo(tagFileName, path, result.getTagInfo());
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "createTagFileObject", "Finished createTagFileObject for  = ["+path+" "+tagFileName+"]  TagFileInfo tfi= ["+tfi+"]");
            }
        } catch (JspCoreException e) {
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME, "createTagFileObject", "exception creating a tag-file object", e);
            }
        }
        return tfi;
    }

}
