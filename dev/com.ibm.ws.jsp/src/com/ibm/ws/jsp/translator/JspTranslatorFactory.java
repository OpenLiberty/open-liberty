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
package com.ibm.ws.jsp.translator;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorCollection;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorConfiguration;
import com.ibm.ws.jsp.translator.visitor.configuration.VisitorConfigParser;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.resource.JspInputSource;

public class JspTranslatorFactory {
	
	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.JspTranslatorFactory";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}

    protected static JspTranslatorFactory factory = null;
    protected JspVisitorConfiguration visitorConfiguration = null;
    
    static {
        try {
            factory = new JspTranslatorFactory("com/ibm/ws/jsp/translator/visitor/configuration/JspVisitorConfiguration.xml", JspTranslatorFactory.class.getClassLoader());
        }
        catch (JspCoreException e) {
			if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.SEVERE)){
				logger.logp(Level.SEVERE, CLASS_NAME, "static", "Failed to create JspTranslatorFactory",e);
			}
        }
    }
    
    public static void initialize(String configFilePath) throws JspCoreException {
        factory = new JspTranslatorFactory(configFilePath, JspTranslatorFactory.class.getClassLoader());
    }
    
    public static void initialize(InputStream is) throws JspCoreException {
        factory = new JspTranslatorFactory(is, JspTranslatorFactory.class.getClassLoader());
    }
    
    public static JspTranslatorFactory getFactory() {
        return (factory);
    }
    
    public static JspTranslatorFactory createFactory(InputStream is, ClassLoader cl) throws JspCoreException {
        return new JspTranslatorFactory(is, cl);
    }
    
    JspTranslatorFactory(String configFilePath, ClassLoader cl) throws JspCoreException {
        InputStream is = null;
        try {
            VisitorConfigParser configParser = new VisitorConfigParser(cl);
            is = this.getClass().getResourceAsStream(configFilePath);
            if (is == null) {
                is = this.getClass().getResourceAsStream("/"+configFilePath);
            }
            if (is != null) {
                visitorConfiguration = configParser.parse(is);
            }
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {}
            }
        }
    }
    
    JspTranslatorFactory(InputStream is, ClassLoader cl) throws JspCoreException {
        try {
            VisitorConfigParser configParser = new VisitorConfigParser(cl);
            visitorConfiguration = configParser.parse(is);
        }
        finally {
            try {
                is.close();
            }
            catch (IOException e) {}
        }
    }
    
    public JspTranslator createTranslator(String visitorCollectionId,
                                          JspInputSource inputSource,
                                          JspCoreContext context, 
                                          JspConfiguration jspConfiguration,
                                          JspOptions jspOptions)  //396002
        throws JspCoreException {
        return (createTranslator(visitorCollectionId, inputSource, context, jspConfiguration,jspOptions, new HashMap()));                                         
    }
    
    public JspTranslator createTranslator(String visitorCollectionId,
                                          JspInputSource inputSource,
                                          JspCoreContext context, 
                                          JspConfiguration jspConfiguration,
                                          JspOptions jspOptions,      //396002
                                          Map implicitTagLibMap) 
        throws JspCoreException {
        JspVisitorCollection visitorCollection = visitorConfiguration.getJspVisitorCollection(visitorCollectionId);
        JspTranslator translator = new JspTranslator(inputSource, 
                                                     context,
                                                     jspConfiguration, 
                                                     jspOptions,		//396002
                                                     visitorCollection,
                                                     implicitTagLibMap);
        return translator;                                                         
    }
}
