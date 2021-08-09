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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.translator.document.Jsp2Dom;
import com.ibm.ws.jsp.translator.visitor.JspVisitor;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorCollection;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorUsage;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.resource.JspInputSource;

public class JspTranslator {
	protected JspInputSource inputSource = null;
    protected Jsp2Dom jsp2Dom = null;
    protected Document jspDocument = null;
    protected JspCoreContext context = null;
    protected JspConfiguration jspConfiguration = null;
    protected JspOptions jspOptions = null;   //396002
    protected JspVisitorCollection jspVisitorCollection = null;

	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.JspTranslator";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}

    JspTranslator(JspInputSource inputSource, 
                  JspCoreContext context,
                  JspConfiguration jspConfiguration,
                  JspOptions jspOptions,		//396002
                  JspVisitorCollection jspVisitorCollection) throws JspCoreException {
        this(inputSource, context, jspConfiguration, jspOptions, jspVisitorCollection, new HashMap());                      
    }
    
    JspTranslator(JspInputSource inputSource, 
                  JspCoreContext context, 
                  JspConfiguration jspConfiguration,
                  JspOptions jspOptions,		//396002
                  JspVisitorCollection jspVisitorCollection,
                  Map implicitTagLibMap) throws JspCoreException {
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.entering("com.ibm.ws.jsp.translator.JspTranslator", "Constructor", inputSource.getRelativeURL());
		}

		this.inputSource = inputSource;                         	
        this.context = context;
        this.jspConfiguration = jspConfiguration;
        this.jspOptions = jspOptions;       //396002
        this.jspVisitorCollection = jspVisitorCollection;

        jsp2Dom = new Jsp2Dom(inputSource, context, jspConfiguration, jspOptions, implicitTagLibMap);
        jspDocument = jsp2Dom.getJspDocument();
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.exiting("com.ibm.ws.jsp.translator.JspTranslator", "Constructor", inputSource.getRelativeURL() );
		}

    }
    
    public HashMap processVisitors() 
        throws JspCoreException {
        return (processVisitors(new JspVisitorInputMap()));
    }

    public HashMap processVisitors(JspVisitorInputMap jspVisitorInput)
        throws JspCoreException {
        	
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
			logger.entering("com.ibm.ws.jsp.translator.JspTranslator", "processVisitors", inputSource.getRelativeURL());
		}
	
        jspVisitorInput.put("StaticIncludeDependencyList", jsp2Dom.getDependencyList());
        jspVisitorInput.put("CdataJspIdMap", jsp2Dom.getCdataJspIdMap());
        HashMap resultsMap = new HashMap();
        try {
            SortedMap sortedMap = new TreeMap();
            for (Iterator itr = jspVisitorCollection.getJspVisitorUsageList().iterator(); itr.hasNext();) {
                JspVisitorUsage usage = (JspVisitorUsage)itr.next();
                try {
                    Integer order = new Integer(usage.getOrder());
                    sortedMap.put(order, usage);
                }
                catch (NumberFormatException e) {
                	throw new JspTranslationException ("jsp.error.building.visitor.order",e);
                }
            }
            for (Iterator itr = sortedMap.keySet().iterator(); itr.hasNext();) {
                Integer key = (Integer)itr.next();
                JspVisitorUsage usage = (JspVisitorUsage)sortedMap.get(key);
                Class visitorClass = usage.getJspVisitorDefinition().getVisitorClass();
                Constructor constructor = visitorClass.getConstructor(new Class[] {JspVisitorUsage.class,
                                                                                   JspConfiguration.class,
                                                                                   JspCoreContext.class, 
                                                                                   HashMap.class,
                                                                                   JspVisitorInputMap.class});
                JspVisitor visitor = (JspVisitor)constructor.newInstance(new Object[] {usage,
                                                                                       jspConfiguration,
                                                                                       context, 
                                                                                       resultsMap, 
                                                                                       jspVisitorInput});
                int visits = usage.getVisits();

                for (int i = 0; i < visits; i++) {
 					if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
						logger.logp(Level.FINEST, CLASS_NAME, "processVisitors","processing " + usage.getJspVisitorDefinition().getId() + " visitor - visit["+(i+1)+"] for " + inputSource.getRelativeURL());
					}
                    visitor.visit(jspDocument, i+1);
                }
                resultsMap.put(usage.getJspVisitorDefinition().getId(), visitor.getResult());
            }
        }
        catch (NoSuchMethodException e) {
            throw new JspTranslationException(e);
        }
        catch (InstantiationException e) {
            throw new JspTranslationException(e);
        }
        catch (InvocationTargetException e) {
            throw new JspTranslationException(e);
        }
        catch (IllegalAccessException e) {
            throw new JspTranslationException(e);
        }
        finally{
      		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
				logger.exiting("com.ibm.ws.jsp.translator.JspTranslator", "processVisitors", inputSource.getRelativeURL());
			}
        }

        return (resultsMap);
    }
    
    public Document getDocument() {
    	return this.jspDocument; 
    }
    
}
