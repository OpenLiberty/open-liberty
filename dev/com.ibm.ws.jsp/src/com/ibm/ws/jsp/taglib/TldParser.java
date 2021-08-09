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
package com.ibm.ws.jsp.taglib;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.jsp.tagext.FunctionInfo;
import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagFileInfo;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagVariableInfo;
import javax.servlet.jsp.tagext.VariableInfo;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfigurationManager;
import com.ibm.ws.jsp.inputsource.JspInputSourceContainerImpl;
import com.ibm.ws.jsp.translator.JspTranslator;
import com.ibm.ws.jsp.translator.JspTranslatorFactory;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.tagfilescan.TagFileScanResult;
import com.ibm.ws.jsp.translator.visitor.xml.ParserFactory;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.resource.JspInputSource;


public class TldParser extends DefaultHandler { 

	static protected Logger logger;
	static protected Level logLevel = Level.FINEST;
	private static final String CLASS_NAME="com.ibm.ws.jsp.taglib.TldParser";
	static {
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}

    public static final String TAGLIB_DTD_PUBLIC_ID_11 = "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.1//EN";
    public static final String TAGLIB_DTD_RESOURCE_PATH_11 = "/javax/servlet/jsp/resources/web-jsptaglibrary_1_1.dtd";
    
    public static final String TAGLIB_DTD_PUBLIC_ID_12 = "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.2//EN";
    public static final String TAGLIB_DTD_RESOURCE_PATH_12 = "/javax/servlet/jsp/resources/web-jsptaglibrary_1_2.dtd";
    
    public static final String XMLSCHEMA_DTD_PUBLIC_ID = "-//W3C//DTD XMLSCHEMA 200102//EN";
    public static final String XMLSCHEMA_DTD_RESOURCE_PATH = "/javax/servlet/resources/XMLSchema.dtd";
    
    public static final String DATATYPES_DTD_PUBLIC_ID = "datatypes";
    public static final String DATATYPES_DTD_RESOURCE_PATH = "/javax/servlet/resources/datatypes.dtd";
    
    public static final String TAGLIB_XSD_SYSTEM_ID_20 = "web-jsptaglibrary_2_0.xsd";
    public static final String TAGLIB_XSD_RESOURCE_PATH_20 = "/javax/servlet/jsp/resources/web-jsptaglibrary_2_0.xsd";
    
    public static final String J2EE14_XSD_SYSTEM_ID = "j2ee_1_4.xsd";
    public static final String J2EE14_XSD_RESOURCE_PATH = "/javax/servlet/resources/j2ee_1_4.xsd";
    
    public static final String XML_XSD_SYSTEM_ID = "http://www.w3.org/2001/xml.xsd";
    public static final String XML_XSD_RESOURCE_PATH = "/javax/servlet/resources/xml.xsd";
    
    public static final String WEB_SERVICE_CLIENT_XSD_SYSTEM_ID = "http://www.ibm.com/webservices/xsd/j2ee_web_services_client_1_1.xsd";
    public static final String WEB_SERVICE_CLIENT_XSD_RESOURCE_PATH = "/javax/servlet/resources/j2ee_web_services_client_1_1.xsd";
    
    protected static final int TAGLIB_ELEMENT = 1;
    protected static final int TAG_ELEMENT = 2;
    protected static final int TAGFILE_ELEMENT = 3;
    protected static final int FUNCTION_ELEMENT = 4;
    protected static final int ATTRIBUTE_ELEMENT = 5;
    protected static final int DEFERRED_VALUE_ELEMENT = 6;
    // DON'T FORGET TO ADD NEW ELEMENT TYPES TO elementTypes array!:
    protected static String[] elementTypes = {"TAGLIB_ELEMENT","TAG_ELEMENT","TAGFILE_ELEMENT","FUNCTION_ELEMENT","ATTRIBUTE_ELEMENT","DEFERRED_VALUE_ELEMENT"};
    
    protected int currentElement = 0;

    protected JspCoreContext ctxt = null;
    protected JspConfigurationManager configManager = null;
    protected ClassLoader classloader = null;
        
    protected SAXParser saxParser = null;
    
    protected TagLibraryInfoImpl tli = null;
    
    protected List <TagInfo>tags = new ArrayList<TagInfo>();
    protected String tagName = null;
    protected String tagDescription = null;
    protected String tagClassName = null;
    protected String teiClassName = null;
    protected String bodyContent = TagInfo.BODY_CONTENT_JSP;
    protected String displayName = null;
    protected String smallIcon = null;
    protected String largeIcon = null;
    protected boolean dynamicAttributes = false;
    
    protected List <TagAttributeInfo>attributes = new ArrayList<TagAttributeInfo>();
    protected String attributeName = null;
    protected boolean required = false;
    protected String type = null;
    protected boolean reqTime = false;
    protected boolean fragment = false;
    protected boolean deferredValue = false, deferredMethod = false;  //jsp2.1ELwork
    protected String expectedType = null;
    protected String methodSignature = null;
    
    protected List <TagVariableInfo>variables = new ArrayList<TagVariableInfo>();
    protected String nameGiven = null;
    protected String nameFromAttribute = null;
    protected String variableClassName = "java.lang.String";
    protected boolean declare = true;
    protected int scope = VariableInfo.NESTED;
    
    protected List <TagFileInfo>tagFiles = new ArrayList<TagFileInfo>();
    protected String tagFileName = null;
    protected String path = null;
    
    protected List <FunctionInfo>functions = new ArrayList<FunctionInfo>();
    protected String functionName = null;
    protected String functionClass = null;
    protected String functionSignature = null;
    
    protected String validatorClass = null;
    protected HashMap <String,String>validatorInitParams = null;
    protected String paramName = null;
    protected String paramValue = null;
    
    protected StringBuffer chars = null;
    
    protected List <String>eventListenerList = new ArrayList<String>();
    
    protected String tldLocation = null;
    
    public ArrayList<ParsedTagElement> parsedTagElements;
    public ArrayList<ParsedTagFileElement> parsedTagFileElements;
    public HashMap<String, HashMap<String, String>> tagLibValidators;
    
    public TldParser(JspCoreContext ctxt,
            JspConfigurationManager configManager,
            boolean validateTLDs,
            ClassLoader classloader) throws JspCoreException {
        
        this(ctxt, configManager, validateTLDs);
        if (classloader != null)
            this.classloader = classloader;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
            logger.logp(logLevel, CLASS_NAME, "TldParser(JspCoreContext, JspConfigurationManager, boolean, ClassLoader)", "ctxt= ["+ctxt+"]  configManager= ["+configManager+"] validateTLDs= ["+validateTLDs+"] classloader= ["+classloader+"]");
        }
    }

    public TldParser(JspCoreContext ctxt,
                     JspConfigurationManager configManager,
                     boolean validateTLDs) throws JspCoreException {
        this.ctxt = ctxt;
        this.configManager = configManager;
        if (ctxt!=null) {
            this.classloader = ctxt.getJspClassloaderContext().getClassLoader();
        }
        ClassLoader oldLoader = ThreadContextHelper.getContextClassLoader();
        ThreadContextHelper.setClassLoader(TldParser.class.getClassLoader());
        try {
            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
            saxFactory.setNamespaceAware(true);
            if (validateTLDs) {
                saxFactory.setFeature("http://xml.org/sax/features/validation", true);
                saxFactory.setFeature("http://apache.org/xml/features/validation/schema", true);
            }
            saxParser = saxFactory.newSAXParser();
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "TldParser(JspCoreContext, JspConfigurationManager, boolean)", "ctxt= ["+ctxt+"]  configManager= ["+configManager+"] validateTLDs= ["+validateTLDs+"] saxParser= ["+saxParser+"]");
            }
        }
        catch (ParserConfigurationException e) {
            throw new JspCoreException(e);
        }
        catch (SAXException e) {
            throw new JspCoreException(e);
        }
        finally{
            ThreadContextHelper.setClassLoader(oldLoader);
        }
    }
    
    private void reset() {
        resetTagFile();
        resetTag();
        resetFunction();
        resetVariable();
        resetAttribute();
        resetValidator();
        tags.clear();
        tagFiles.clear();
        functions.clear();
        eventListenerList.clear();
    }
    
    private void resetTagFile() {
        tagFileName = null;
        path = null;
    }
    
    private void resetTag() {
        tagName = null;
        tagClassName = null;
        teiClassName = null;
        bodyContent = TagInfo.BODY_CONTENT_JSP;
        tagDescription = null;
        displayName = null;
        smallIcon = null;
        largeIcon = null;
        dynamicAttributes = false;
        attributes.clear();
        variables.clear();
    }
    
    private void resetFunction() {
        functionClass = null;
        functionSignature = null;
        functionName = null;
    }
    
    private void resetVariable() {
        nameGiven = null;
        nameFromAttribute = null;
        variableClassName = "java.lang.String";
        declare = true;
        scope = VariableInfo.NESTED;
    }
    
    private void resetAttribute() {
        attributeName = null;
        required = false;
        type = null;
        reqTime = false;
        fragment = false;
        // jsp2.1ELwork
        deferredValue = false;
        deferredMethod = false;  
        expectedType = null;
        methodSignature = null;
    }
    
    private void resetValidator() {
        validatorClass = null;
        validatorInitParams = null;
        paramName = null;
        paramValue = null;
    }
    
    public TagLibraryInfoImpl parseTLD(JspInputSource inputSource, String tldOriginatorId) throws JspCoreException {
        this.tldLocation = inputSource.getRelativeURL();
        tli = new TagLibraryInfoImpl(tldOriginatorId, inputSource);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
            logger.logp(logLevel, CLASS_NAME, "parseTLD(JspInputSource, String)", "inputSource= ["+inputSource+"]  tldOriginatorId= ["+tldOriginatorId+"]");
        }
        try {
            if (inputSource instanceof JspInputSourceContainerImpl) {
                //TODO: figure out if anything needs to be done here
                InputStream is = inputSource.getInputStream();
                if (is!=null) {
                    parse(is);
                } else {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                        logger.logp(logLevel, CLASS_NAME, "parseTLD(JspInputSource, String)", "problem parsing tld for " + inputSource.getRelativeURL());
                    }                    
                }
            } else {
                parse(inputSource.getInputStream());
            }
        }
        catch (SAXException e) {
            tli = null;
            logParseErrorMessage(e);
            throw new JspCoreException(e);
        }
        catch (IOException e) {
            tli = null;
            logParseErrorMessage(e);
            throw new JspCoreException(e);
        }
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
            logger.logp(logLevel, CLASS_NAME, "parseTLD(JspInputSource, String)", "returning tli= ["+tli+"]");
        }
        return (tli);
    }
    
    public TagLibraryInfoImpl parseTLD(JspInputSource inputSource, InputStream is, String tldOriginatorId) throws JspCoreException {
        this.tldLocation = inputSource.getRelativeURL();
        tli = new TagLibraryInfoImpl(tldOriginatorId, inputSource);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
            logger.logp(logLevel, CLASS_NAME, "parseTLD(JspInputSource, InputStream, String)", "inputSource= ["+inputSource+"]  InputStream=["+is+"] tldOriginatorId= ["+tldOriginatorId+"]");
        }
        try {
            parse(is);
        }
        catch (SAXException e) {
            tli = null;
            logParseErrorMessage(e);
            throw new JspCoreException(e);
        }
        catch (IOException e) {
            tli = null;
            logParseErrorMessage(e);
            throw new JspCoreException(e);
        }
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
            logger.logp(logLevel, CLASS_NAME, "parseTLD(JspInputSource, InputStream, String)", "returning tli= ["+tli+"]");
        }
        return (tli);
    }
    
    private void parse(InputStream is) throws SAXException, IOException {
        reset();

        try {        
            ParserFactory.parseDocument(saxParser, is, this);
	    }
        finally {
            try {
                is.close();
            } catch (IOException e) {}
        }
    }
    
    public List<String> getEventListenerList() {
        return eventListenerList;
    }
    
    public List<String> getParsedTagsList() {
        List<String> result = new ArrayList<String>();
        if (parsedTagElements!=null) {
            for (ParsedTagElement pte:parsedTagElements) {
                result.add(pte.getTagClassName());
            }
        }
        return result;
    }
    
    public void startElement(String namespaceURI, 
                             String localName,
                             String elementName, 
                             Attributes attrs) 
        throws SAXException {
        chars = new StringBuffer();
        if (elementName.equals("taglib")) {
            currentElement = TAGLIB_ELEMENT;
            String ver=attrs.getValue("version");
            if (ver!=null) {
            	tli.setRequiredVersion(ver.trim());
            }
        }
        else if (elementName.equals("tag")) {
            currentElement = TAG_ELEMENT;
        }
        else if (elementName.equals("tag-file")) {
            currentElement = TAGFILE_ELEMENT;
        }
        else if (elementName.equals("function")) {
            currentElement = FUNCTION_ELEMENT;
        }
        else if (elementName.equals("attribute")) {
            currentElement = ATTRIBUTE_ELEMENT;
        }
        else if (elementName.equals("deferred-value")) {
            currentElement = DEFERRED_VALUE_ELEMENT;
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
            logger.logp(logLevel, CLASS_NAME, "startElement", "currentElement= ["+elementTypes[currentElement-1]+"]");
        }
    }
    
    @Trivial
    public void characters(char[] ch, int start, int length) throws SAXException {
        for (int i = 0; i < length; i++) {
            if (chars != null)
                chars.append(ch[start+i]);
        }
        /*if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
            logger.logp(logLevel, CLASS_NAME, "characters", "ch= ["+ch.toString()+"] start= ["+start+"] length= ["+length+"]");
        }*/
    }

    public void endElement(String namespaceURI,
                           String localName,
                           String elementName)
        throws SAXException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
            logger.logp(logLevel, CLASS_NAME, "endElement", "namespaceURI= ["+namespaceURI+"] localName= ["+localName+"] elementName=["+elementName+"]");
        }
        if (elementName.equals("tlibversion") || elementName.equals("tlib-version")) {
            tli.setTlibversion(chars.toString().trim());
        }
        else if (elementName.equals("jspversion") || elementName.equals("jsp-version")) {
            tli.setRequiredVersion(chars.toString().trim());
        }
        else if (elementName.equals("shortname") || elementName.equals("short-name")) {
            tli.setShortName(chars.toString().trim());
        }
        else if (elementName.equals("uri")) {
            tli.setReliableURN(chars.toString().trim());
        }
        else if (elementName.equals("info") || elementName.equals("description")) {
            switch (currentElement) {
                case TAGLIB_ELEMENT:
                    tli.setInfoString(chars.toString().trim());
                    break;
                case TAG_ELEMENT:                    
                    tagDescription = chars.toString().trim();
                    break;
                default:
                    break;
            }
        }
        else if (elementName.equals("tag")) {
            TagAttributeInfo[] tagAttributes = new TagAttributeInfo[attributes.size()];
            if (attributes.size() > 0) {
                tagAttributes = (TagAttributeInfo[])attributes.toArray(tagAttributes);
            }
            
            TagVariableInfo[] tagVariables = new TagVariableInfo[variables.size()];
            if (variables.size() > 0) {
                tagVariables = (TagVariableInfo[])variables.toArray(tagVariables);
            }

            if (classloader==null) { // only for gathering a list of classes for injection purposes
                if (parsedTagElements==null) {
                    parsedTagElements = new ArrayList<ParsedTagElement>();
                }
                parsedTagElements.add(new ParsedTagElement(tldLocation, tagName, tagClassName, bodyContent, tagDescription, tli, teiClassName, tagAttributes, displayName, smallIcon, largeIcon, tagVariables, dynamicAttributes));
                //tags.add done later in ParsedTagElement
            } else {
                TagExtraInfo tei = null;
                if (teiClassName != null) {
                            // begin  221334: check if user specified empty tag for tei-class and log warning.
                    if(teiClassName.trim().equals("")){
                            logger.logp(Level.WARNING, CLASS_NAME, "endElement", "TagExtraInfo specified in tld without a value.  tld=[" + tldLocation +"]");
                    }
                    else{
                            // end  221334: check if user specified empty tag for tei-class and log warning.        
                            try {
                                tei = (TagExtraInfo)classloader.loadClass(teiClassName).newInstance();
                            }
                            catch (Exception e) {
                                    //      begin  221334: improve error being logged for this error.
                                    String message = JspCoreException.getMsg("jsp.error.failed.load.tei.class", new Object[]{teiClassName});
                                    message+=" from "+tldLocation;
                                    logger.logp(Level.WARNING, CLASS_NAME, "endElement", message);   //PK27099
                                //throw new SAXException(message);                               //PK27099
                                    //      end  221334: improve error being logged for this error.                     
                            }
                    }
                }
                TagInfo tag = new TagInfo(tagName,
                                          tagClassName,
                                          bodyContent,
                                          tagDescription,
                                          tli,
                                          tei,
                                          tagAttributes,
                                          displayName,
                                          smallIcon,
                                          largeIcon,
                                          tagVariables,
                                          dynamicAttributes);
                                          
                tags.add(tag);
            }
            
            resetTag();
            currentElement = TAGLIB_ELEMENT;
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] TagInfo tag= ["+tagName+"]");
            }
        }
        else if (elementName.equals("tag-file")) {
            if (ctxt!=null) {
                try {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)) {
                        logger.logp(Level.FINEST, CLASS_NAME, "endElement", "about to do tagfilescan for = ["+path+" "+tagFileName+"]");
                    }
                    JspOptions jspOptions = new JspOptions(); // 396002
                	JspInputSource tagFileInputSource = ctxt.getJspInputSourceFactory().copyJspInputSource(tli.getInputSource(), path);
                    JspTranslator jspTranslator = JspTranslatorFactory.getFactory().createTranslator(TagLibraryCache.TAGFILE_SCAN_ID, 
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
                    TagFileInfo tfi = new TagFileInfo(tagFileName, path, result.getTagInfo());
                    tagFiles.add(tfi);
                    //resetTagFile();
                    //currentElement = TAGLIB_ELEMENT;
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                        logger.logp(logLevel, CLASS_NAME, "endElement", "TldParser elementName= ["+elementName+"] TagFileInfo tfi= ["+tfi+"]");
                    }
                }
                catch (JspCoreException e) {
                    throw new SAXException(e);
                }
            }
            resetTagFile();
            currentElement = TAGLIB_ELEMENT;
        }
        else if (elementName.equals("function")) {
            FunctionInfo fi = new FunctionInfo(functionName, functionClass, functionSignature);
            functions.add(fi);
            resetFunction();
            currentElement = TAGLIB_ELEMENT;
        }
        else if (elementName.equals("name")) {
            switch (currentElement) {
                case TAGFILE_ELEMENT:
                    tagFileName = chars.toString().trim();
                    break;
                case TAG_ELEMENT:                    
                    tagName = chars.toString().trim();
                    break;
                case FUNCTION_ELEMENT:                    
                    functionName = chars.toString().trim();
                    break;
                case ATTRIBUTE_ELEMENT:                    
                    attributeName = chars.toString().trim();
                    break;
                default:
                    break;
            }
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] currentElement= ["+elementTypes[currentElement-1]+"]");
            }
        }
        else if (elementName.equals("path")) {
            path = chars.toString().trim();
        }
        else if (elementName.equals("tag-class") || elementName.equals("tagclass")) {
            tagClassName = chars.toString().trim();
        }
        else if (elementName.equals("tei-class") || elementName.equals("teiclass")) {
            teiClassName = chars.toString().trim();
        }
        else if (elementName.equals("body-content") || elementName.equals("bodycontent")) {
            bodyContent = chars.toString().trim();
        }
        else if (elementName.equals("variable")) {
            TagVariableInfo tvi = new TagVariableInfo(nameGiven, 
                                                      nameFromAttribute, 
                                                      variableClassName, 
                                                      declare, 
                                                      scope);
            variables.add(tvi);
            resetVariable();                                           
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] TagVariableInfo= ["+tvi+"]");
            }
        }
        else if (elementName.equals("attribute")) {
            /* Start Defect 202537 */
             
            /* 
             * According to JSP.C-3 ("TLD Schema Element Structure - tag"), 
             * 'type' and 'rtexprvalue' must not be specified if 'fragment'
             * has been specified (this will be enforced by validating parser).
             * Also, if 'fragment' is TRUE, 'type' is fixed at
             * javax.servlet.jsp.tagext.JspFragment, and 'rtexprvalue' is
             * fixed at true. See also JSP.8.5.2.
             * 
             */
              
            if (fragment) {
                type = "javax.servlet.jsp.tagext.JspFragment";
                reqTime = true;
            }
            
            /* According to JSP spec, for static values (those determined at
             * translation time) the type is fixed at java.lang.String.
             */
            if (!reqTime) {
                type = "java.lang.String";
            }
            
            /* End Defect 202537 */
            
            if(deferredValue && expectedType==null) {
            	expectedType = "java.lang.Object";
            }
            
            // defect 420617 - comment-out the setting of type to "java.lang.Object"
            //if (type == null) {
            //	type = "java.lang.Object";
            //}
            
            /*if(deferredValue) {
            	type = "javax.el.ValueExpression";
            }
            
            if(deferredMethod) {
            	type = "javax.el.MethodExpression";
            }*/
             
            TagAttributeInfo tai = new TagAttributeInfo(attributeName, required, type, reqTime, fragment,
                                                        null, deferredValue, deferredMethod, expectedType,
                                                        methodSignature);  // jsp2.1ELWork
            attributes.add(tai);
            resetAttribute();                                                
            currentElement = TAG_ELEMENT;
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] TagAttributeInfo= ["+tai+"]");
            }
        }
        else if (elementName.equals("required")) {
            String requiredString = chars.toString().trim();
            if (requiredString.equalsIgnoreCase("yes") ||
                requiredString.equalsIgnoreCase("true")) {
                required = true;    
            }
            else {
                required = false;
            }
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] required= ["+required+"]");
            }
        }
        else if (elementName.equals("rtexprvalue")) {
            String reqTimeString = chars.toString().trim();
            if (reqTimeString.equalsIgnoreCase("yes") ||
                reqTimeString.equalsIgnoreCase("true")) {
                reqTime = true;    
            }
            else {
                reqTime = false;
            }
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] reqTime= ["+reqTime+"]");
            }
        } else if (elementName.equals("deferred-value")) {
            deferredValue = true;
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] deferredValue= ["+deferredValue+"] type= ["+type+"]");
            }
        } else if (elementName.equals("deferred-method")) {
            deferredMethod = true;
            if (methodSignature==null) {
            	methodSignature = "java.lang.Object method()";
            }         
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] deferredMethod= ["+deferredMethod+"] methodSignature= ["+methodSignature+"]");
            }
        } else if (elementName.equals("method-signature")) {
        	methodSignature = chars.toString().trim();
        	if (methodSignature != null && methodSignature.length()>0) {
                    methodSignature = methodSignature.trim();
            } else {
                methodSignature = "java.lang.Object method()";
            }
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] methodSignature= ["+methodSignature+"]");
            }
        }
        else if (elementName.equals("listener")) {
        }
        else if (elementName.equals("listener-class")) {
            eventListenerList.add(chars.toString().trim());
        }
        else if (elementName.equals("type")) {
            switch (currentElement) {
            case TAG_ELEMENT:
            case ATTRIBUTE_ELEMENT:
                type = chars.toString().trim();
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                    logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] tagDescription= ["+tagDescription+"]");
                }
                break;
            case DEFERRED_VALUE_ELEMENT:
            	expectedType = chars.toString().trim();
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                    logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] expectedType= ["+expectedType+"]");
                }
                currentElement = ATTRIBUTE_ELEMENT;
                break;
            default:
                break;
            }            
        }
        else if (elementName.equals("function-class")) {
            functionClass = chars.toString().trim();
        }
        else if (elementName.equals("function-signature")) {
            functionSignature = chars.toString().trim();
        }
        else if (elementName.equals("dynamic-attributes")) {
            String dynamicAttributesString = chars.toString().trim();
            if (dynamicAttributesString.equalsIgnoreCase("yes") ||
                dynamicAttributesString.equalsIgnoreCase("true")) {
                dynamicAttributes = true;    
            }
            else {
                dynamicAttributes = false;
            }
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] dynamicAttributes= ["+dynamicAttributes+"]");
            }
        }
        else if (elementName.equals("fragment")) {
            String fragmentString = chars.toString().trim();
            if (fragmentString.equalsIgnoreCase("yes") ||
                fragmentString.equalsIgnoreCase("true")) {
                fragment = true;    
            }
            else {
                fragment = false;
            }
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] fragment= ["+fragment+"]");
            }
        }
        else if (elementName.equals("name-given")) {
            nameGiven = chars.toString().trim();
        }
        else if (elementName.equals("name-from-attribute")) {
            nameFromAttribute = chars.toString().trim();
        }
        else if (elementName.equals("variable-class")) {
            variableClassName = chars.toString().trim();
        }
        else if (elementName.equals("declare")) {
            String declareString = chars.toString().trim();
            if (declareString.equalsIgnoreCase("yes") ||
                declareString.equalsIgnoreCase("true")) {
                declare = true;    
            }
            else {
                declare = false;
            }
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] declare= ["+declare+"]");
            }
        }
        else if (elementName.equals("scope")) {
            String strScope = chars.toString().trim();
            if (strScope.equals("AT_BEGIN"))
                scope = VariableInfo.AT_BEGIN;
            else if (strScope.equals("NESTED"))
                scope = VariableInfo.NESTED;
            else if (strScope.equals("AT_END"))
                scope =VariableInfo.AT_END;
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] scope= ["+scope+"]");
            }
        }
        else if (elementName.equals("validator")) {

        	if(validatorClass != null){
        		// begin  221334: check if user specified empty tag for validator-class and log warning.        		
        		if (validatorClass.trim().equals("")){
	        		logger.logp(Level.WARNING, CLASS_NAME, "endElement", "TagLibraryValidator specified in tld without a value.  tld=[" + tldLocation +"]");
	        	}
	        	else{
                            // end  221334: check if user specified empty tag for validator-class and log warning.
	        	    if (ctxt==null) {
	        	        //handle validatation later
        	        	if (this.tagLibValidators==null) {
        	        	    this.tagLibValidators=new HashMap<String, HashMap<String, String>>();
        	        	}
        	        	this.tagLibValidators.put(validatorClass, validatorInitParams);
	        	    } else {
	        	        
	        	        try {
		                
	        	            Class tlvClass = classloader.loadClass(validatorClass);
	        	            tli.setTabLibraryValidator(tlvClass, validatorInitParams);                 
                                } catch (Exception e) {
                                    //  begin  221334: improve error being logged for this error.
                                    String message = JspCoreException.getMsg("jsp.error.failed.load.tlv.class", new Object[]{validatorClass});
                                    logger.logp(Level.FINE, CLASS_NAME, "endElement", message, e);
                                    throw new SAXException(message);
                                    //end  221334: improve error being logged for this error.
                                } catch (NoClassDefFoundError e) {
                                    //  begin  221334: improve error being logged for this error.
                                    String message = JspCoreException.getMsg("jsp.error.failed.load.tlv.class", new Object[]{validatorClass});
                                    logger.logp(Level.FINE, CLASS_NAME, "endElement", message, e);
                                    throw new SAXException(message);
                                    //end  221334: improve error being logged for this error.
    		                }
	        	    }
	        	}
        	}
            resetValidator();
        }
        else if (elementName.equals("validator-class")) {
            validatorClass = chars.toString().trim();
        }
        else if (elementName.equals("init-param")) {
            if (validatorInitParams == null)
                validatorInitParams = new HashMap<String,String>();
            validatorInitParams.put(paramName, paramValue);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] paramName= ["+paramName+"] paramValue= ["+paramValue+"]");
            }
        }
        else if (elementName.equals("param-name")) {
            paramName = chars.toString().trim();
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] paramName= ["+paramName+"]");
            }
        }
        else if (elementName.equals("param-value")) {
            paramValue = chars.toString().trim();
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(logLevel)) {
                logger.logp(logLevel, CLASS_NAME, "endElement", "elementName= ["+elementName+"] paramValue= ["+paramValue+"]");
            }
        }
        else if (elementName.equals("small-icon")) {
            smallIcon = chars.toString().trim();
        }
        else if (elementName.equals("large-icon")) {
            largeIcon = chars.toString().trim();
        }
        else if (elementName.equals("taglib")) {
            tli.setTags(tags);
            tli.setTagFiles(tagFiles);
            tli.setFunctions(functions);
            currentElement = 0;
        }
      else if (elementName.equals("display-name")) {
          displayName = chars.toString().trim();
      }
        chars = null;
    }
    
    public InputSource resolveEntity(String publicId, String systemId)
        throws SAXException {
        InputSource isrc = null;
        String resourcePath = null;            
        if (publicId != null) {          
            if (publicId.equals(TAGLIB_DTD_PUBLIC_ID_11)) {
                resourcePath = TAGLIB_DTD_RESOURCE_PATH_11;
            }
            else if (publicId.equals(TAGLIB_DTD_PUBLIC_ID_12)) {
                resourcePath = TAGLIB_DTD_RESOURCE_PATH_12;
            }
            else if (publicId.equals(XMLSCHEMA_DTD_PUBLIC_ID)) {
                resourcePath = XMLSCHEMA_DTD_RESOURCE_PATH;
            }
            else if (publicId.equals(DATATYPES_DTD_PUBLIC_ID)) {
                resourcePath = DATATYPES_DTD_RESOURCE_PATH;
            }
        }
        else if (systemId != null) {
            if (systemId.endsWith(TAGLIB_XSD_SYSTEM_ID_20)) {
                resourcePath = TAGLIB_XSD_RESOURCE_PATH_20;
            }
            else if (systemId.endsWith(J2EE14_XSD_SYSTEM_ID)) {
                resourcePath = J2EE14_XSD_RESOURCE_PATH;
            }
            else if (systemId.equals(XML_XSD_SYSTEM_ID)) {
                resourcePath = XML_XSD_RESOURCE_PATH;
            }
            else if (systemId.equals(WEB_SERVICE_CLIENT_XSD_SYSTEM_ID)) {
                resourcePath = WEB_SERVICE_CLIENT_XSD_RESOURCE_PATH;
            }
        }
        
        if (resourcePath != null) {
            InputStream input = this.getClass().getResourceAsStream(resourcePath);
            if (input == null) {
            	//	begin  221334: improve error being logged for this error.
                throw new SAXException(JspCoreException.getMsg("jsp.error.internal.dtd.not.found") + "[" + resourcePath +"]");
            	//	end  221334: improve error being logged for this error.
            }
            isrc = new InputSource(input);
            isrc.setSystemId(systemId);
        }
        return isrc;
    }

    public void error(SAXParseException arg0) throws SAXException {
		throw arg0;
    }

    public void fatalError(SAXParseException arg0) throws SAXException {
		throw arg0;    
	}

    public void warning(SAXParseException arg0) throws SAXException {
		String origMessage= arg0.getMessage();
		String newMessage = "Parser warning during parse of Tag Library [" + tldLocation +"]";
		if (origMessage != null){
			newMessage = newMessage + ": " + origMessage;
		}
		logger.logp(Level.WARNING, CLASS_NAME, "warning", newMessage);
    }
    
    private void logParseErrorMessage(Exception e){
		String origMessage= e.getMessage();
		String newMessage = "Failed to parse Tag Library [" + tldLocation +"]";
		if (origMessage != null){
			newMessage = newMessage + ": " + origMessage;
		}
		logger.logp(Level.SEVERE, CLASS_NAME, "logParseErrorMessage", newMessage);
    }
}
