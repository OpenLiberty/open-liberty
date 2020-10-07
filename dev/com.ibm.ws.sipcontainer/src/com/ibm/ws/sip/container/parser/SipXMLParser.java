/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.rules.And;
import com.ibm.ws.sip.container.rules.Condition;
import com.ibm.ws.sip.container.rules.Contains;
import com.ibm.ws.sip.container.rules.Equal;
import com.ibm.ws.sip.container.rules.Exists;
import com.ibm.ws.sip.container.rules.Not;
import com.ibm.ws.sip.container.rules.Or;
import com.ibm.ws.sip.container.rules.SubDomainOf;

/**
 * Parser for the Sip Application XML configuration. 
 */
public class SipXMLParser implements EntityResolver
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipXMLParser.class);

    /**
     * DOM Parser. 
     */
    private DocumentBuilder m_documentBuilder;

    //
    //Condition Types
    //
    private final static String AND = "and";
    private final static String OR = "or";
    private final static String NOT = "not";
    private final static String EQUAL = "equal";
    private final static String EXISTS = "exists";
    private final static String CONTAINS = "contains";
    private final static String SUBDOMAIN_OF = "subdomain-of";

    private final static String[] ALL_CONDITIONS =
        { AND, OR, NOT, EQUAL, EXISTS, CONTAINS, SUBDOMAIN_OF };

    //
    //XML Constants
    //
    private final static String DTD_DOC = "sip-app_1_0.dtd";
    private final static String DTD_LOCATION = "/javax/servlet/sip/";
    private final static String VAR = "var";
    private final static String VALUE = "value";
    private final static String IGNORE_CASE = "ignore-case";
    private static final String PATTERN = "pattern";
    private static final String SERVLET_SELECTION = "servlet-selection";
    private static final String MAIN_SERVLET = "main-servlet";
    private static final String SERVLET_NAME = "servlet-name";
    private static final String SERVLET_MAPPING = "servlet-mapping";
    private static final String SERVLET_CLASS = "servlet-class";
    private static final String SERVLET_LOAD_ON_STARTUP = "load-on-startup";
    private static final String SERVLET_INIT_PARAM = "init-param";    
    private static final String SERVLET = "servlet";
    private static final String DISPLAY_NAME = "display-name";
    private static final String APP_NAME = "app-name";
    private static final String SIP_APP = "sip-app";
    private static final String LISTENER = "listener";
    private static final String LISTENER_CLASS = "listener-class";
    private static final String SESSION_CONFIG = "session-config";
    private static final String SESSION_TIMEOUT = "session-timeout";
    private static final String CONTEXT_PARAM = "context-param";
    private static final String PARAM_NAME = "param-name";
    private static final String PARAM_VALUE = "param-value";
    private static final String PROXY_CONFIG = "proxy-config";
    private static final String SEQUENTIAL_SEARCH_TIMEOUT = "sequential-search-timeout";
    private static final String PROXY_TIMEOUT = "proxy-timeout";
    private static final String SECURITY_CONSTRAINT = "security-constraint";
    private static final String RESOURCE_COLLECTION = "resource-collection";
    private static final String RESOURCE_NAME = "resource-name";
    private static final String PROXY_AUTHENTICATION = "proxy-authentication";
    private static final String SIP_METHOD = "sip-method";
    private static final String LOGIN_CONFIG = "login-config";
    private static final String REALM_NAME = "realm-name";

    /**
     * Construct a new parser for parsing sip.xml configuration.  
     */
    public SipXMLParser() throws ParserConfigurationException
    {
        if (c_logger.isTraceEntryExitEnabled())
        {
            c_logger.traceEntry(this, "SipXMLParser");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        m_documentBuilder = factory.newDocumentBuilder();
        m_documentBuilder.setEntityResolver(this);

        if (c_logger.isTraceEntryExitEnabled())
        {
            c_logger.traceExit(this, "SipXMLParser");
        }

    }

    /**
     * Parser the given stream into a Sip Application configuration description.
     * @param classLoader Class Loader for loading application specific classes. 
     * @param in
     */
    public SipAppDesc parse(InputStream in)
        throws SAXException, IOException
    {
        if (c_logger.isTraceEntryExitEnabled())
        {
            c_logger.traceEntry(this, "parse");
        }

        SipAppDesc app = null;

        Document doc = m_documentBuilder.parse(in);
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(
                this,
                "parse",
                "Sip Document parsing completed");
        }
        
        //Read the Sip App definition from document.
        app = getSipApp(doc);

        if (null != app)
        {
            //Read servlets defintion from xml document. 
            getServletsFromXML(app, doc);

            //Attach Patterns-Rules to servlets
            getServletPatternsFromXML(app, doc);
            
            parseMainServlet(app, doc);
            

            //Get security constraints
            getSecurityConstraintsFromXML(app,doc);
            
            //Get login-config
            getLoginConigFromXML(app,doc);
            //Attach listener for timer, session and appsession events. 
            getListenersFromXML(app, doc);

            //Get the application session's time to live setting
            getAppSessionTTL(app, doc);

            // Get the context parameter from the XML file
            getContextParamFromXML(app, doc);
            
            // Get the sequencial search timeout 
            getSequetialSearchTimeout(app, doc);
            
            
        }

        return app;
    }

    /**
     * Get the sequential search timeout for proxying operations. 
     * @param app
     * @param doc
     */
    private void getSequetialSearchTimeout(SipAppDesc app, Document doc) {
        NodeList list = doc.getElementsByTagName(PROXY_CONFIG);
        if (list.getLength() == 1)
        {
        	//take the timeout parameter from the xml, for jsr289 application it should be under
        	//"proxy-timeout" for jsr116 applications it should be under "sequential-search-timeout" 
        	Node timeoutNode = getChildNode(PROXY_TIMEOUT, list.item(0));
        	if (timeoutNode == null){
        		timeoutNode = getChildNode(SEQUENTIAL_SEARCH_TIMEOUT, list.item(0));
        	}
        	
            if (null != timeoutNode)
            {
                String txt = getTextNode(timeoutNode);
                if (null != txt && txt.length() > 0)
                {
                    try
                    {
                        int timeout = Integer.parseInt(txt);
                        app.setProxyTimeout(timeout);
                        if (c_logger.isTraceDebugEnabled())
                        {
                            StringBuffer b = new StringBuffer(64);
                            b.append("Setting sequential search timeout: ");
                            b.append(timeout);
                            b.append(" Seconds, For App: ");
                            b.append(app.getApplicationName());
                            c_logger.traceDebug(
                                this,
                                "getSequetialSearchTimeout",
                                b.toString());
                        }

                    }
                    catch (NumberFormatException e)
                    {
                        if (c_logger.isErrorEnabled())
                        {
                            Object[] args = { app };
                            c_logger.error(
                                "error.parsing.proxy.timeout",
                                Situation.SITUATION_CREATE,
                                args,
                                e);
                        }

                    }
                }

            }
        }
    }

    /**
     * Get the Sessions time to live setting from xml. 
     * @param app
     * @param doc
     */
    private void getAppSessionTTL(SipAppDesc app, Document doc)
    {
        NodeList list = doc.getElementsByTagName(SESSION_CONFIG);
        if (list.getLength() == 1)
        {
            Node timeoutNode = getChildNode(SESSION_TIMEOUT, list.item(0));
            if (null != timeoutNode)
            {
                String txt = getTextNode(timeoutNode);
                if (null != txt && txt.length() > 0)
                {
                    try
                    {
                        int ttl = Integer.parseInt(txt);
                        app.setAppSessionTTL(ttl);
                        if (c_logger.isTraceDebugEnabled())
                        {
                            StringBuffer b = new StringBuffer(64);
                            b.append("Setting Expiration time: ");
                            b.append(ttl);
                            b.append(" Minutes, For App: ");
                            b.append(app.getApplicationName());
                            c_logger.traceDebug(
                                this,
                                "getAppSessionTTL",
                                b.toString());
                        }

                    }
                    catch (NumberFormatException e)
                    {
                        if (c_logger.isErrorEnabled())
                        {
                            Object[] args = { app };
                            c_logger.error(
                                "error.parsing.session.ttl",
                                Situation.SITUATION_CREATE,
                                args,
                                e);
                        }

                    }
                }

            }
        }
    }

    /**
     * Read XML document and attach listeners to Sip app. Listeners tag can 
     * add timer listeners, app session listeners and session listeners.
     * @param classLoader Class Loader for loading application specific classes.  
     * @param app
     * @param doc
     */
    private void getListenersFromXML(
        SipAppDesc app,
        Document doc)
    {
        NodeList list = doc.getElementsByTagName(LISTENER);
        for (int i = 0; i < list.getLength(); i++)
        {
            Node listenerClass = getChildNode(LISTENER_CLASS, list.item(i));
            if (listenerClass != null)
            {
                String className = getTextNode(listenerClass);
                if (c_logger.isTraceDebugEnabled())
                {
                    StringBuffer b = new StringBuffer(64);
                    b.append("Setting listener class: ");
                    b.append(className);
                    b.append(" For App: ");
                    b.append(app.getApplicationName());

                    c_logger.traceDebug(
                        this,
                        "getListenersFromXML",
                        b.toString());
                }

                app.setSipListeners(Collections.singletonList(className));
            }
            else
            {
                if (c_logger.isWarnEnabled())
                {
                    Object[] args = { app };
                    c_logger.warn(
                        "warn.getting.listeners.failed",
                        Situation.SITUATION_CREATE,
                        args);
                }

            }
        }

    }

    /**
     * Read XML document and attach pattern-rules to Sip Servlets. 
     * @param app
     * @param doc
     */
    private void getServletPatternsFromXML(SipAppDesc app, Document doc)
    {
    	NodeList servlet_mapping = doc.getElementsByTagName(SERVLET_SELECTION);
    	
    	List<Node> nodeList; 
    	if (servlet_mapping.getLength() > 0) {
    		nodeList = getAllChildNodes(new String[] {SERVLET_MAPPING}, servlet_mapping.item(0));
    	} else {
    		servlet_mapping = doc.getElementsByTagName(SERVLET_MAPPING);
    		nodeList = new ArrayList<Node>(servlet_mapping.getLength());
            for (int i = 0; i < servlet_mapping.getLength(); i++) {
            	nodeList.add(servlet_mapping.item(i));
            }
            
    	}
    	
    	Map<String, Condition> servletsPatterns = new HashMap<String, Condition>();
    	for (Node node : nodeList) {
            //Get the servlet's name
            Node servletNameNode = getChildNode(SERVLET_NAME, node);
            String servletName = getTextNode(servletNameNode);

            Node pattern = getChildNode(PATTERN, node);
            if (null != servletName && null != pattern)
            {
                Node baseCondition = getChildNode(ALL_CONDITIONS, pattern);
                Condition condition = getCondition(baseCondition);
                servletsPatterns.put(servletName, condition);
                if (c_logger.isTraceDebugEnabled())
                {
                    StringBuffer b = new StringBuffer(64);
                    b.append("Siplet name: ");
                    b.append(servletName);
                    b.append(" Condition: ");
                    b.append(condition);
                    c_logger.traceDebug(
                        this,
                        "getServletPatternsFromXML",
                        b.toString());
                }
            }
        }
    	
    	app.setServletsPatterns(servletsPatterns);
    }

    /**
     * Read XML document and attach listeners to Sip app. Listeners tag can 
     * add timer listeners, app session listeners and session listeners.
     * @param app the app description where this param are going to be saved
     * @param doc
     */
    private void getContextParamFromXML(SipAppDesc app, Document doc)
    {
        NodeList params = doc.getElementsByTagName(CONTEXT_PARAM);
        for (int i = 0; i < params.getLength(); i++)
        {
            NodeList childs = params.item(i).getChildNodes();
            String paramName = null;
            String paramValue = null;

            // Look for the parameter name and value
            for (int j = 0;
                j < childs.getLength()
                    && (paramName == null || paramValue == null);
                j++)
            {
                Node node = childs.item(j);
                if (PARAM_NAME.equalsIgnoreCase(getLocalNodeName(node)))
                {
                    paramName = getTextNode(node);
                }
                else if (PARAM_VALUE.equalsIgnoreCase(getLocalNodeName(node)))
                {
                    paramValue = getTextNode(node);
                }
            }

            if (null != paramName && null != paramValue)
            {
                app.addContextParam(paramName, paramValue);

                if (c_logger.isTraceDebugEnabled())
                {
                    StringBuffer b = new StringBuffer(64);
                    b.append("name: ");
                    b.append(paramName);
                    b.append(" ,Value:");
                    b.append(paramValue);

                    c_logger.traceDebug(
                        this,
                        "getContextParamFromXML",
                        b.toString());
                }

            }

        }
    }

    /**
     * Construct a matching condition for the given node. 
     * @return Condition
     */
    private Condition getCondition(Node base)
    {
        Condition condition = null;
        String type = getLocalNodeName(base);

        if (AND.equalsIgnoreCase(type))
        {
            List<Condition> subConditions = getSubConditions(base);
            condition = new And(subConditions);

        }
        else if (OR.equalsIgnoreCase(type))
        {
            List<Condition> subConditions = getSubConditions(base);
            condition = new Or(subConditions);
        }
        else if (NOT.equalsIgnoreCase(type))
        {
            //We really should have only one sub condition. 
            List<Condition> subConditions = getSubConditions(base);
            condition = new Not((Condition) subConditions.get(0));
        }
        else if (EQUAL.equalsIgnoreCase(type))
        {
            condition = getEqualCondition(base);
        }
        else if (EXISTS.equalsIgnoreCase(type))
        {
            condition = getExistsCondition(base);
        }
        else if (CONTAINS.equalsIgnoreCase(type))
        {
            condition = getContainsCondition(base);
        }
        else if (SUBDOMAIN_OF.equalsIgnoreCase(type))
        {
            condition = getSubdomainOfCondtion(base);
        }
        else
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { type };
                c_logger.error(
                    "error.invalid.condition.type",
                    Situation.SITUATION_CREATE,
                    args);
            }
        }

        return condition;
    }

    /**
     * Helper function - Extract SUBDOMAIN-OF condition from XML doc.  
     * @param base
     * @return Condition
     */
    private Condition getSubdomainOfCondtion(Node base)
    {
        Condition condition = null;
        String var = getTextNode(getChildNode(VAR, base));
        String value = getTextNode(getChildNode(VALUE, base));
        boolean ignoreCase = getAttribute(base, IGNORE_CASE, false);
        if (null != var && null != value)
        {
            var = var.trim();
            value = value.trim();
            condition = new SubDomainOf(var, value, ignoreCase);
        }
        else
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { "SUBDOMAIN-OF", var, value };
                c_logger.error(
                    "error.parsing.condition",
                    Situation.SITUATION_CREATE,
                    args);
            }
        }

        return condition;

    }

    /**
     * Helper function - Extract CONTAINS condition from XML doc.  
     * @param base
     * @return Condition
     */
    private Condition getContainsCondition(Node base)
    {
        Condition condition = null;
        String var = getTextNode(getChildNode(VAR, base));
        String value = getTextNode(getChildNode(VALUE, base));
        boolean ignoreCase = getAttribute(base, IGNORE_CASE, false);
        if (null != var && null != value)
        {
            var = var.trim();
            value = value.trim();
            condition = new Contains(var, value, ignoreCase);
        }
        else
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { "CONTAINS", var, value };
                c_logger.error(
                    "error.parsing.condition",
                    Situation.SITUATION_CREATE,
                    args);
            }
        }

        return condition;


    }

    /**
     * Helper function - Extract EXISTS condition from XML doc.  
     * @param base
     * @return Condition
     */
    private Condition getExistsCondition(Node base)
    {
        Condition condition = null;
        String var = getTextNode(getChildNode(VAR, base));
        if (null != var)
        {
            var = var.trim();
            condition = new Exists(var);
        }
        else
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { "EXISTS", var, "" };
                c_logger.error(
                    "error.parsing.condition",
                    Situation.SITUATION_CREATE,
                    args);
            }
        }

        return condition;
    }
    
    /**
     * Helper function - Extract EQUAL condition from XML doc.  
     * @param base
     * @return Condition
     */
    private Condition getEqualCondition(Node base)
    {
        Condition condition = null;
        String var = getTextNode(getChildNode(VAR, base));
        String value = getTextNode(getChildNode(VALUE, base));
        boolean ignoreCase = getAttribute(base, IGNORE_CASE, false);
        if (null != var && null != value)
        {
            var = var.trim();
            value = value.trim();
            condition = new Equal(var, value, ignoreCase);
        }
        else
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args = { "EQUAL", var, value };
                c_logger.error(
                    "error.parsing.condition",
                    Situation.SITUATION_CREATE,
                    args);
            }
        }

        return condition;

    }

    /**
     * Get a boolean attribute value for the specified node. If the attribute
     * does not exist, the default value will be used as the attribute value. 
     * @param node
     * @param attributeName
     * @param defaultValue
     * @return boolean
     */
    private boolean getAttribute(Node node, String attributeName,
                                 boolean defaultValue) {
        boolean rc = defaultValue;
        NamedNodeMap map = node.getAttributes();
        if (map != null) {
            Node attr = map.getNamedItem(attributeName);
            if (null != attr) {
                String value = attr.getNodeValue();
                if (value.equalsIgnoreCase("true")) {
                    rc = true;
                }
                else {
                    rc = false;
                }
            }
        }

        return rc;
    }


    /**
     * Used in the case logical connectors OR or AND to find the operators that
     * are contained in the expression. Get the list of sub conditions under the
     * specified base node.
     * 
     * @param base
     * @return List
     */
    public List<Condition> getSubConditions(Node base)
    {
        List<Condition> conditionsList = new LinkedList<Condition>();
        List<Node> nodes = getAllChildNodes(ALL_CONDITIONS, base);

        if (nodes.size() > 0)
        {

            Iterator<Node> iter = nodes.iterator();
            while (iter.hasNext())
            {
                Node subElement = iter.next();
                Condition subCondtion = getCondition(subElement);

                if (null != subCondtion)
                {
                    conditionsList.add(subCondtion);
                }
            }
        }
        else
        {
            if (c_logger.isErrorEnabled())
            {
                c_logger.error(
                    "error.parsing.and.condition",
                    Situation.SITUATION_CREATE,
                    null);
            }
        }

        return conditionsList;
    }

    /**
     * Read the Sip App defintion from document. 
     * @param doc
     * @return SipApp
     */
    private SipAppDesc getSipApp(Document doc)
    {
        SipAppDesc app = null;
        NodeList list = doc.getElementsByTagName(SIP_APP);

        if (list.getLength() == 1)
        {
        	// app-name replaces display-name for JSR289,
        	// we are trying to read both tags and prefering app-name
        	// over display name for backward compatibility
            Node displayNameNode = getChildNode(DISPLAY_NAME, list.item(0));
            Node appNameNode = getChildNode(APP_NAME, list.item(0));
            
            String display_name = null, app_name = null;
            if (appNameNode != null) {
            	app_name = getTextNode(appNameNode);
            } 
        	if (displayNameNode != null) {
                display_name = getTextNode(displayNameNode);
            }

        	
            app = new SipAppDesc(display_name, app_name);

            if (c_logger.isTraceDebugEnabled())
            {
                c_logger.traceDebug(
                    this,
                    "getSipApp",
                    "New Sip Application: " + app.getApplicationName());
            }

        }
        else
        {
            if (c_logger.isErrorEnabled())
            {
                c_logger.error(
                    "error.parsing.sip.app",
                    Situation.SITUATION_CREATE,
                    null);
            }
        }

        return app;
    }

    /**
     * Finds the FIRST child node for the given parent node that matches 
     * the given name. 
     * @param name
     * @param parent
     */
    private Node getChildNode(String name, Node parent)
    {
        Node node = null;
        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++)
        {
            if (name.equalsIgnoreCase(getLocalNodeName(list.item(i))))
            {
                node = list.item(i);
                break;
            }
        }

        return node;
    }

    /**
     * Finds the FIRST child node for the given parent node that matches 
     * one of the names in the list. 
     * @param names List of names. 
     * @param parent
     */
    private Node getChildNode(String[] names, Node parent)
    {
        Node node = null;
        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength() && node == null; i++) {
        	String nodeName = getLocalNodeName(list.item(i));
            for (int j = 0; j < names.length; j++) {
                if (names[j].equalsIgnoreCase(nodeName)) {
                    node = list.item(i);
                    break;
                }
            }
        }

        return node;
    }

    /**
     * Finds all child nodes for the given parent node that match 
     * one of the names in the list. 
     * @param names List of names. 
     * @param parent
     */
    private List<Node> getAllChildNodes(String[] names, Node parent)
    {
        List<Node> matchingList = new LinkedList<Node>();

        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++)
        {
        	String nodeName = getLocalNodeName(list.item(i));
            for (int j = 0; j < names.length; j++)
            {
                if (names[j].equalsIgnoreCase(nodeName))
                {
                    matchingList.add(list.item(i));
                }
            }
        }

        return matchingList;
    }

    /**
     * Reads Servlets defintion from XML doc. Updates the Sip App with the list 
     * of Siplets.  
     * @param app The Sip Application that contains the siplet
     * @param doc
     */
    private void getServletsFromXML(SipAppDesc app, Document doc)
    {
    	List<SipServletDesc> servletDescriptors = new LinkedList<SipServletDesc>();
        NodeList servlets = doc.getElementsByTagName(SERVLET);
        
        Map<String,String> initParams = new LinkedHashMap<String,String>();
        
        for (int i = 0; i < servlets.getLength(); i++)
        {
            NodeList childs = servlets.item(i).getChildNodes();
            
            String servletName = null;
            String servletClass = null;
            
            // The default values for load-on-startup.
    		int servletLoadOnStartup = -1;
    		boolean hasLoadOnStartupTag = false;
    		
    		// Clears the map for each servlet.
    		initParams.clear();

            //Look for the servlet name and class definiton
            for (int j = 0; j < childs.getLength(); j++)
            {
                Node node = childs.item(j);
                String name = getLocalNodeName(node);
                if (SERVLET_NAME.equalsIgnoreCase(name))
                {
                    servletName = getTextNode(node);
                }
                else if (SERVLET_CLASS.equalsIgnoreCase(name))
                {
                    servletClass = getTextNode(node);
                }
                else if (SERVLET_LOAD_ON_STARTUP.equalsIgnoreCase(name))
                {
                	try {
                		servletLoadOnStartup = Integer.parseInt(getTextNode(node));
                		
                	} catch (NumberFormatException e) {
                		
                		// <load-on-startup/> is defined without value.
                		servletLoadOnStartup = 0;
                	}
                	hasLoadOnStartupTag = true;
                	
                } 
                else if (SERVLET_INIT_PARAM.equalsIgnoreCase(name))
                {
                	getServletInitParamsFromXML(node,initParams);
                }
            }

            if (null != servletName && null != servletClass)
            {
            	SipServletDesc desc = new SipServletDesc(app, servletName, servletClass, servletLoadOnStartup, hasLoadOnStartupTag);
            	for (Map.Entry<String, String> entry: initParams.entrySet()) {
            		desc.setInitParam(entry.getKey(), entry.getValue());
            	}
            	servletDescriptors.add(desc);

                if (c_logger.isTraceDebugEnabled())
                {
                    StringBuffer b = new StringBuffer(64);
                    b.append("New Servlet: ")
                    	.append(servletName)
                    	.append(" ,Class:")
                    	.append(servletClass)
                    	.append(" , load-on-startup: ")
                    	.append(servletLoadOnStartup);
                    c_logger.traceDebug(this, "getServletsFromXML", b.toString());
                }

            }
        }
        app.addSipServlets(servletDescriptors);
    }
    
    
    /**
     * Read XML document and attach listeners to Sip app. Listeners tag can 
     * add timer listeners, app session listeners and session listeners.
     * @param initParamNode
     * @param initParams
     */
    private void getServletInitParamsFromXML(Node initParamNode, Map<String,String> initParams) {
        String paramName = null;
        String paramValue = null;
    	NodeList params = initParamNode.getChildNodes();

        for (int i = 0; i < params.getLength(); i++) {
        	Node node = params.item(i);
            if (PARAM_NAME.equalsIgnoreCase(getLocalNodeName(node))) {
            	paramName = getTextNode(node);
                    
            } else if (PARAM_VALUE.equalsIgnoreCase(getLocalNodeName(node))) {
            	paramValue = getTextNode(node);
            }

            if (null != paramName && null != paramValue) {
            	initParams.put(paramName,paramValue);

                if (c_logger.isTraceDebugEnabled()) {
                    StringBuffer b = new StringBuffer(64);
                    b.append("name: ");
                    b.append(paramName);
                    b.append(" ,Value:");
                    b.append(paramValue);

                    c_logger.traceDebug(
                        this,
                        "getServletInitParamsFromXML",
                        b.toString());
                }
                // Since the initParamNode is wrap the context of 
                // <init-param> ... </init-param> then once it get the paramater name and value, it could escape the for-loop.
                break;
            }
        }
    }    
    
    
    /**
     * Reads Servlets defintion from XML doc. Updates the Sip App with the list 
     * of Siplets.  
     * @param app The Sip Application that contains the siplet
     * @param doc
     */
    private void parseMainServlet(SipAppDesc app, Document doc)
    {
        NodeList servlet_selection = doc.getElementsByTagName(SERVLET_SELECTION);
        String value = null;
        for (int i = 0; i < servlet_selection.getLength(); i++) {
        	Node child = getChildNode(MAIN_SERVLET, servlet_selection.item(i));
        	if (child != null) {
        		value = child.getTextContent().trim();
        		break;
        	}
        }
        
        boolean setValue = true;
        
        if (value != null && !value.isEmpty()) {
        	if (app.hasMainServlet()) {
        		if (c_logger.isErrorEnabled()) {
        			c_logger.error("Application has already defined main-servlet", Situation.SITUATION_CREATE, app);
        		}
        		setValue = false;
        	}


        	if (setValue) {
        		app.setMainSipletName(value);
        	}

        }
    }
    

    /**
     * Parse security constraints
     * @param app
     * @param doc
     */
    private void getSecurityConstraintsFromXML(SipAppDesc app, Document doc){
    	NodeList constraints = doc.getElementsByTagName(SECURITY_CONSTRAINT);
    	SecurityConstraint secCon;
        for (int i = 0; i < constraints.getLength(); i++)
        {
        	if(c_logger.isTraceDebugEnabled()){
        		c_logger.traceDebug(this,"getSecurityConstraintsFromXML","found new security constraint");
        	}
        	secCon = new SecurityConstraint();
        	app.addSecurityConstraint(secCon);
        	
            NodeList childs = constraints.item(i).getChildNodes();
            String displayName = null;
            SecurityResourceCollection rc=null;
            for (int j = 0;  j < childs.getLength(); j++) {
            	Node node = childs.item(j);
            	String nodeName = getLocalNodeName(node);
            	if (DISPLAY_NAME.equalsIgnoreCase(nodeName)) {
            		displayName = getTextNode(node);
            		if(c_logger.isTraceDebugEnabled()){
                		c_logger.traceDebug(this,"getSecurityConstraintsFromXML","display-name["+displayName+"]");
                	}
            		secCon.setDisplayName(displayName);
            	}else if (PROXY_AUTHENTICATION.equalsIgnoreCase(nodeName)) {
            		if(c_logger.isTraceDebugEnabled()){
                		c_logger.traceDebug(this,"getSecurityConstraintsFromXML","proxy-authenticate enable");
                	}
            		secCon.setProxyAuthenticate(true);
            	}else if (RESOURCE_COLLECTION.equalsIgnoreCase(nodeName)) {            	
            		NodeList resourceCollection = node.getChildNodes();
            		rc=new SecurityResourceCollection(secCon);
            		SipServletDesc siplet = null;
            		String rname=null;
            		String sipletName=null;
            		String method=null;
            		for (int j1 = 0;  j1 < resourceCollection.getLength(); j1++) {
                    	Node rnodeChild = resourceCollection.item(j1);
                    	String rnodeName = getLocalNodeName(rnodeChild);
                    	if(RESOURCE_NAME.equalsIgnoreCase(rnodeName)){
                    		rname = getTextNode(rnodeChild);
                    		if(c_logger.isTraceDebugEnabled()){
                        		c_logger.traceDebug(this,"getSecurityConstraintsFromXML","resource-name["+rname+"]");
                        	}
                    	}else if (SERVLET_NAME.equalsIgnoreCase(rnodeName)){
                    		sipletName = getTextNode(rnodeChild);
                    		if(c_logger.isTraceDebugEnabled()){
                        		c_logger.traceDebug(this,"getSecurityConstraintsFromXML","servlet-name["+sipletName+"]");
                        	}
                    		siplet = app.getSipServlet(sipletName);
                    		siplet.addSecurityResourceCollection(rc);
                    		rc.addSiplet(siplet);
                    	}else if(SIP_METHOD.equalsIgnoreCase(rnodeName)){
                    		method = getTextNode(rnodeChild);
                    		if(c_logger.isTraceDebugEnabled()){
                        		c_logger.traceDebug(this,"getSecurityConstraintsFromXML","sip-method["+method+"]");
                        	}
                    		rc.addMethod(method);
                    	}                    	
            		}
            	}
            }
        }
    }
    
    /**
     * Parse security constraints
     * @param app
     * @param doc
     */
    private void getLoginConigFromXML(SipAppDesc app, Document doc){
    	NodeList constraints = doc.getElementsByTagName(LOGIN_CONFIG);
        for (int i = 0; i < constraints.getLength(); i++)
        {
            NodeList childs = constraints.item(i).getChildNodes();
            String realm = null;
            for (int j = 0;  j < childs.getLength(); j++) {
            	Node node = childs.item(j);
            	if (REALM_NAME.equalsIgnoreCase(getLocalNodeName(node))) {
            		realm = getTextNode(node);
            		if (c_logger.isTraceDebugEnabled())
                    {
                        StringBuffer b = new StringBuffer(64);
                        b.append("Login-config realm: ");
                        b.append(realm);
                        c_logger.traceDebug(this,"getLoginConfigFromXML",b.toString());
                    }
            		app.setRealm(realm);
            	}
            }
        }
    }
    
    /**
     * Helper function. Looks at the childs of the given node. Returns the first 
     * child node's value which is of TEXT type. 
     * @param node
     * @return String
     */
    private String getTextNode(Node node)
    {
        String rValue = null;
        NodeList list = node.getChildNodes();
        Node child;
        for (int i = 0; i < list.getLength(); i++)
        {
            child = list.item(i);
            if (child.getNodeType() == Node.TEXT_NODE)
            {
                rValue = child.getNodeValue();
                break;
            }
        }

        if (null != rValue)
        	rValue = rValue.trim();
        
        return rValue;
    }

   

    /**
     * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
     */
    public InputSource resolveEntity(String publicId, String systemId)
        throws SAXException, IOException
    {
        InputSource inputSource = null;
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(
                this,
                "resolveEntity",
                publicId + " " + systemId);
        }

        if (systemId.indexOf(DTD_DOC) > 0)
        {
            //Use the local DTD document and not the definition from the web 
            //that can not be reached. 
            InputStream in =
                getClass().getResourceAsStream(DTD_LOCATION + DTD_DOC);
            if (null != in)
            {
                inputSource = new InputSource(in);
            }
            else
            {
                if (c_logger.isErrorEnabled())
                {
                    c_logger.error(
                        "error.local.sip.dtd.not.found",
                        Situation.SITUATION_CREATE,
                        null);
                }
            }

        }

        return inputSource;
    }

 
    
    /**
     * Retrieve node name without schema prefix. 
     * We do not recieve any namespace data from the DOM parser, so this is a work around
     * we just trim all of the namespaces prefixes.
     * 
     * @param n Node
     * @return node name without prefix (if there is a prefix)
     */
    private String getLocalNodeName(Node n) {
    	String name = null;
    	if (n != null) {
    		name = n.getNodeName();
    		if (name != null) {
    			if (name.indexOf(':') > 0) {
    				name = name.substring(name.indexOf(':') + 1);
    			}
    		}
    	}
    	
    	return name;
    	
    }
    


}
