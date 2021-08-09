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
package com.ibm.ws.jsp.configuration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.configuration.JspConfigPropertyGroup;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.webcontainer.util.URIMatcher;

public class JspConfigurationManager {  
    protected boolean isServlet24OrHigher = false;
    protected boolean isServlet25OrHigher = false;
    protected URIMatcher uriMatcher = null;
    protected List jspExtensionList = new ArrayList();
    boolean JCDIEnabled;

	private static Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.configuration.JspConfigurationManager";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}       
    
    public JspConfigurationManager(List jspPropertyGroups, boolean isServlet24, boolean isServlet24_or_higher, List addtlJspFileExtensions, boolean JCDIEnabled) 
        throws JspCoreException {
        this.isServlet24OrHigher = isServlet24_or_higher;
        this.isServlet25OrHigher = isServlet24OrHigher && !isServlet24;
        uriMatcher = new URIMatcher();
        List urlPatterns = new ArrayList();
        
        // first add url mappings from web.xml jsp properties group
        for (Iterator itr = jspPropertyGroups.iterator(); itr.hasNext();) {
            JspConfigPropertyGroup jspPropertyGroup = (JspConfigPropertyGroup)itr.next();
            for (Iterator itr2 = jspPropertyGroup.getUrlPatterns().iterator(); itr2.hasNext();) {
                String urlPattern = (String)itr2.next();
                if (urlPatterns.contains(urlPattern) == false) {
                    urlPatterns.add(urlPattern);
                    try {
                        uriMatcher.put(urlPattern, jspPropertyGroup);
                    }
                    catch (Exception e) {
						logger.logp(Level.WARNING, CLASS_NAME, "JspConfigurationManager", "Failed to add url pattern [" + urlPattern +"] to match list.", e);
                    }
                }
                else {
                    throw new JspCoreException("jsp.error.dup.url.pattern", new Object[] {urlPattern});    
                }
                if (jspExtensionList.contains(urlPattern) == false) {
                    boolean newExtFound = true;
                    for (int i = 0; i < Constants.STANDARD_JSP_EXTENSIONS.length; i++) {
                        if (urlPattern.equals(Constants.STANDARD_JSP_EXTENSIONS[i])) {
                            newExtFound = false;
                            break;                                    
                        }
                    }
                    if (newExtFound) {
                        jspExtensionList.add(urlPattern);    
                    }
                }
            }
        }
		// then add additional extensions defined via ibm custom extensions.
		for (Iterator itr = addtlJspFileExtensions.iterator(); itr.hasNext();) {
			String urlPattern = (String) itr.next();
			if (urlPattern.startsWith("*.")) {
				if (jspExtensionList.contains(urlPattern) == false) {
					boolean newExtFound = true;
					for (int i = 0; i < Constants.STANDARD_JSP_EXTENSIONS.length; i++) {
						if (urlPattern.equals(Constants.STANDARD_JSP_EXTENSIONS[i])) {
							newExtFound = false;
							break;                                    
						}
					}
					if (newExtFound) {
						jspExtensionList.add(urlPattern);    
					}
				}
			}
		}
		this.JCDIEnabled = JCDIEnabled;
    }
    
    public synchronized JspConfiguration getConfigurationForUrl(String url) {
    	List matches = uriMatcher.matchAll(url);

		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
			logger.logp(Level.FINEST, CLASS_NAME, "getConfigurationForUrl", "begin creation of new JspConfiguration for url = [{0}] uris matched = [{1}]", new Object[]{ url, Integer.valueOf(matches.size())});
		}
        JspConfiguration configuration = new JspConfiguration(this);
        if (matches.size() > 0) {
            if (isServlet24OrHigher) configuration.setElIgnored(false); // initial value may be overridden
            if (!isServlet25OrHigher) configuration.setDeferredSyntaxAllowedAsLiteral(true);
            configuration.setServletVersion(getServletVersion());
            for (Iterator itr = matches.iterator(); itr.hasNext(); ) {
                JspConfigPropertyGroup propertyGroup = (JspConfigPropertyGroup)itr.next();
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
                    logger.logp(Level.FINEST, CLASS_NAME, "getConfigurationForUrl", "handling config url pattern(s) " +  propertyGroup.getUrlPatterns() );
                }
                if(propertyGroup.getIncludeCodas()!=null)		        
                    configuration.addIncludeCodas(propertyGroup.getIncludeCodas());

                if(propertyGroup.getElIgnored()!=null)
                    configuration.setElIgnored( Boolean.valueOf(propertyGroup.getElIgnored()));

                /*
                    if(propertyGroup.getElIgnoredSetTrueInPropGrp()!=null)
                        configuration.setElIgnoredSetTrueInPropGrp(((Boolean)propertyGroup.setElIgnoredSetTrueInPropGrp()).booleanValue());
                 */

                if(propertyGroup.getIsXml()!=null)
                    configuration.setIsXml(Boolean.valueOf(propertyGroup.getIsXml()));

                if(propertyGroup.getPageEncoding()!=null)
                    configuration.setPageEncoding(propertyGroup.getPageEncoding());

                if(propertyGroup.getIncludePreludes()!=null)
                    configuration.addIncludePreludes(propertyGroup.getIncludePreludes());

                if(propertyGroup.getScriptingInvalid()!=null)
                    configuration.setScriptingInvalid(Boolean.valueOf(propertyGroup.getScriptingInvalid()));

                // jsp2.1work
                if(propertyGroup.getTrimDirectiveWhitespaces()!=null){
                    configuration.setTrimDirectiveWhitespaces(Boolean.valueOf(propertyGroup.getTrimDirectiveWhitespaces()));
                    configuration.setTrimDirectiveWhitespaces((Boolean.valueOf(propertyGroup.getTrimDirectiveWhitespaces())).toString());
                }

                // jsp2.1ELwork
                if(propertyGroup.getDeferredSyntaxAllowedAsLiteral()!=null){
                    configuration.setDeferredSyntaxAllowedAsLiteral(Boolean.valueOf(propertyGroup.getDeferredSyntaxAllowedAsLiteral()));
                    configuration.setDeferredSyntaxAllowedAsLiteral(Boolean.valueOf(propertyGroup.getDeferredSyntaxAllowedAsLiteral()).toString());
                }
                //jsp2.1MR2work
                if(propertyGroup.getDefaultContentType()!=null)
                    configuration.setDefaultContentType(propertyGroup.getDefaultContentType());

                //jsp2.1MR2work
                if(propertyGroup.getBuffer()!=null)
                    configuration.setBuffer(propertyGroup.getBuffer());

                //jsp2.1MR2work
                if(propertyGroup.getErrorOnUndeclaredNamespace()!=null)
                    configuration.setErrorOnUndeclaredNamespace(Boolean.valueOf(propertyGroup.getErrorOnUndeclaredNamespace()));


                /* Liberty: JspConfigProperty is not used in Liberty. All values are in JspConfigPropertyGroup.
                for (Iterator pgitr = propertyGroup.iterator(); pgitr.hasNext();) {
                    JspConfigProperty property = (JspConfigProperty)pgitr.next();
                    switch (property.getType()) {
                        case JspConfigProperty.CODA_TYPE:
                            configuration.addIncludeCoda((String)property.getValue());
                            break;
                        case JspConfigProperty.EL_IGNORED_TYPE:
                            configuration.setElIgnored(((Boolean)property.getValue()).booleanValue());
                            break;
                        case JspConfigProperty.EL_IGNORED_SET_TRUE_TYPE:
                            configuration.setElIgnoredSetTrueInPropGrp(((Boolean)property.getValue()).booleanValue());
                            break;
                        case JspConfigProperty.IS_XML_TYPE:
                            configuration.setIsXml(((Boolean)property.getValue()).booleanValue());
                            break;
                        case JspConfigProperty.PAGE_ENCODING_TYPE:
                            configuration.setPageEncoding((String)property.getValue());
                            break;
                        case JspConfigProperty.PRELUDE_TYPE:
                            configuration.addIncludePrelude((String)property.getValue());
                            break;
                        case JspConfigProperty.SCRIPTING_INVALID_TYPE:
                            configuration.setScriptingInvalid(((Boolean)property.getValue()).booleanValue());
                            break;
                        // jsp2.1work
                        case JspConfigProperty.TRIM_DIRECTIVE_WHITESPACES_TYPE:
                            configuration.setTrimDirectiveWhitespaces(((Boolean)property.getValue()).booleanValue());
                            configuration.setTrimDirectiveWhitespaces(((Boolean)property.getValue()).toString());
                            break;
                            // jsp2.1ELwork
                        case JspConfigProperty.DEFERRED_SYNTAX_ALLOWED_AS_LITERAL_TYPE:
                            configuration.setDeferredSyntaxAllowedAsLiteral(((Boolean)property.getValue()).booleanValue());
                            configuration.setDeferredSyntaxAllowedAsLiteral(((Boolean)property.getValue()).toString());
                            break;
                        //jsp2.1MR2work
                        case JspConfigProperty.DEFAULT_CONTENT_TYPE:
                            configuration.setDefaultContentType((String)property.getValue());
                            break;
                        //jsp2.1MR2work
                        case JspConfigProperty.BUFFER:
                            configuration.setBuffer((String)property.getValue());
                            break;
                        //jsp2.1MR2work
                        case JspConfigProperty.ERROR_ON_UNDECLARED_NAMESPACE:
                            configuration.setErrorOnUndeclaredNamespace(((Boolean)property.getValue()).booleanValue());
                            break;
                    }
                }
                 */

            }
        }
        else {
            if (isServlet24OrHigher)
                configuration.setElIgnored(false);
            if (!isServlet25OrHigher) configuration.setDeferredSyntaxAllowedAsLiteral(true);
            configuration.setServletVersion(getServletVersion());
        }
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
			logger.logp(Level.FINEST, CLASS_NAME, "getConfigurationForUrl", "complete creation of new JspConfiguration for url = [{0}] {1}", new Object[]{url, configuration.toString()});
		}

        return (configuration);
    }
    
    public synchronized JspConfiguration getConfigurationForStaticInclude(String url, JspConfiguration parentConfig) {
        List matches = uriMatcher.matchAll(url);
        
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
			logger.logp(Level.FINEST, CLASS_NAME, "getConfigurationForStaticInclude", "begin creation of new StaticIncludeJspConfiguration for url = [{0}] uris matched = [{1}] (config inherited from parent)", new Object[]{ url, Integer.valueOf(matches.size())});
		}
		StaticIncludeJspConfiguration configuration = new StaticIncludeJspConfiguration(parentConfig);
        
        if (matches.size() > 0) {
            for (Iterator itr = matches.iterator(); itr.hasNext(); ) {
                JspConfigPropertyGroup propertyGroup = (JspConfigPropertyGroup)itr.next();
                if(propertyGroup.getPageEncoding()!=null)
                    configuration.setPageEncoding(propertyGroup.getPageEncoding());
                /* Liberty: JspConfigProperty is not used in Liberty. All values are in JspConfigPropertyGroup.
                for (Iterator pgitr = propertyGroup.iterator(); pgitr.hasNext();) {
                    JspConfigProperty property = (JspConfigProperty)pgitr.next();
                    if (property.getType() == JspConfigProperty.PAGE_ENCODING_TYPE) {
                        configuration.setPageEncoding((String)property.getValue());
                    }
                }
                */
            }
        }
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
			logger.logp(Level.FINEST, CLASS_NAME, "getConfigurationForStaticInclude", "complete creation of new StaticIncludeJspConfiguration for url = [{0}] {1}", new Object[]{url, configuration.toString()});
		}
        return (configuration);
    }
    
    public List getJspExtensionList() {
        return jspExtensionList;
    }
    
    public JspConfiguration createJspConfiguration() {
        JspConfiguration newConfiguration = new JspConfiguration(this); 
        newConfiguration.setServletVersion(getServletVersion());
        return newConfiguration;
    }
    
    public boolean isJCDIEnabled() {
        return JCDIEnabled;
    }
    
    public void setJCDIEnabled(boolean b) {
        JCDIEnabled=b;
    }
    
    private String getServletVersion() {
        String s;
        if (!isServlet24OrHigher) {
            s="2.3";
        } else if (!isServlet25OrHigher) {
            s="2.4";
        } else {
            s="2.5";
        }
        return s;
    }
}
