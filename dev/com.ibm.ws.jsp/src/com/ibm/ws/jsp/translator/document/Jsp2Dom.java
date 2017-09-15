/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.document;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.resource.JspInputSource;

public class Jsp2Dom {
    static protected Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.document.Jsp2Dom";
    static{
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }
    static final String DEFAULT_ENCODING = "ISO-8859-1";
    static final String CONVERTED_DEFAULT_ENCODING = com.ibm.wsspi.webcontainer.util.EncodingUtils.getJvmConverter(DEFAULT_ENCODING);

    private Stack directoryStack = null;
    private Stack dependencyStack = null;
    private List dependencyList = null;
    private Map cdataJspIdMap = null;
    private Map implicitTagLibMap = null;
    private JspInputSource jspInputSource = null;
    private String resolvedRelativeURL = null;
    private JspCoreContext context = null;
    private JspConfiguration jspConfiguration = null;
    private JspOptions jspOptions = null;   //396002
    private Boolean parentIsXml = null;
    private boolean isXml=false;
    String sourceEnc = null;
    boolean isEncodingSpecifiedInProlog = false;
    boolean isBomPresent = false;
    private boolean isDefaultPageEncoding=false;
    private boolean isServlet25=false;

    /**
     * Method Jsp2Dom.
     * 
     * This constructor is used for top level jsps and tag files. 
     * The pageEncoding is obtained from the web.xml jsp 
     * configuration.
     * 
     * @param jspFileName
     * @param jarUrl
     * @param dfactory
     * @param context
     * @param jspConfiguration
     */
    public Jsp2Dom(JspInputSource jspInputSource,
                   JspCoreContext context,
                   JspConfiguration jspConfiguration,
                   JspOptions jspOptions,              //396002
                   Map implicitTagLibMap)  {
        this.jspInputSource = jspInputSource;
        this.context = context;
        this.jspConfiguration = jspConfiguration;
        this.directoryStack = new Stack();
        directoryStack.push("/");
        this.dependencyStack = new Stack();
        this.dependencyList = new ArrayList();
        this.jspOptions = jspOptions;				//396002
        this.cdataJspIdMap = new HashMap();
        resolveBaseDir();
        this.implicitTagLibMap = implicitTagLibMap;
        this.isServlet25 = jspConfiguration.getServletVersion().equals("2.5");
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
            logger.logp(Level.FINER, CLASS_NAME, "Jsp2Dom", "Jsp2Dom 1 this.jspInputSource: ["+this.jspInputSource.getRelativeURL()+"]");
        } 
    }

    /**
     * Method Jsp2Dom.
     * 
     * This constructor is used for static includes. 
     * The pageEncoding is passed in from the including jsp
     * via the provided jspConfiguration object. 
     * 
     * @param jspFileName
     * @param jarUrl
     * @param dfactory
     * @param context
     * @param directoryStack
     * @param jspConfiguration
     * @param dependencyStack
     * @param dependencyList
     */
    public Jsp2Dom(JspInputSource jspInputSource,
                   JspCoreContext context,
                   Stack directoryStack,
                   JspConfiguration jspConfiguration,
                   JspOptions jspOptions,     //396002
                   Stack dependencyStack,
                   List dependencyList,
                   Map cdataJspIdMap,
                   Map implicitTagLibMap,
                   boolean parentIsXml)  {
        this.jspInputSource = jspInputSource;
        this.context = context;
        this.jspConfiguration = jspConfiguration;
        this.jspOptions = jspOptions;    	//396002
        this.directoryStack = directoryStack;
        this.dependencyStack = dependencyStack;
        this.dependencyList = dependencyList;
        this.cdataJspIdMap = cdataJspIdMap;
        this.implicitTagLibMap = implicitTagLibMap;
        this.parentIsXml = Boolean.valueOf(parentIsXml);
        resolveBaseDir();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
            logger.logp(Level.FINER, CLASS_NAME, "Jsp2Dom", "Jsp2Dom 2 this.jspInputSource: ["+this.jspInputSource.getRelativeURL()+"]");
        } 
    }
    
    /**
     * Method getJspDocument.
     * According to the spec a JSP document can be identified as such in three ways:
     * 
     * 1. If there is a <jsp-property-group> that explicitly indicates, through the <is-xml>
     * element, whether a given file is a JSP document, then that indication overrides
     * any other determination. Otherwise,
     *  
     * 2. If there is no explicit association and the extension is .jspx, then the file is a JSP
     * document, otherwise,
     * 
     * 3. If the file is explicitly or implicitly identified as a JSP page and the top element
     * is a jsp:root element then the file is identified as a JSP document. This behavior
     * provides backwards compatibility with JSP 1.2.
     * 
     * @return Document
     * @throws JspCoreException
     */
    public Document getJspDocument() throws JspCoreException {
        Document document = null;
        /* The isXmlSpecified flag should only be set for top level jsps. */
        /* statically included jsps and tag files will alway have this set to false */
        if (jspInputSource.isXmlDocument()) {
            document = new JspDocumentConverter(jspInputSource,
                                                resolvedRelativeURL,
                                                context,
                                                directoryStack,
                                                jspConfiguration,
                                                jspOptions,
                                                dependencyStack,
                                                dependencyList,
                                                cdataJspIdMap,
                                                implicitTagLibMap).convert();
            jspConfiguration.setIsXml(true);                                                    
        }
        else {
        	getSyntaxAndEncoding();
            if ((isXml && isEncodingSpecifiedInProlog)){	// removed || isBomPresent check since that is now covered in getSyntaxAndEncoding()
                /*
                 * Make sure the encoding explicitly specified in the XML
                 * prolog (if any) matches that in the JSP config element
                 * (if any).
                 */
                if (jspConfiguration.getPageEncoding() != null && !jspConfiguration.getPageEncoding().equalsIgnoreCase(sourceEnc)
                    && (!jspConfiguration.getPageEncoding().toUpperCase().startsWith("UTF-16") || !sourceEnc.toUpperCase().startsWith("UTF-16"))) {	// see http://unicode.org/faq/utf_bom.html#36 for info on UTF-16
                    throw new JspCoreException("jsp.error.prolog_config_encoding_mismatch", new Object[] { sourceEnc,jspConfiguration.getPageEncoding()});
                }
            }
             
            if(!isDefaultPageEncoding && !isXml){
            	jspConfiguration.setResponseEncoding(sourceEnc);
            }
            
            if (isXml) {
                document = parseToDom();	            	
            }
            else {
                document = getJspDocumentAsJspPage();
            }
        }
        directoryStack.pop();
        return (document);
    }
    
    private void getSyntaxAndEncoding() throws JspCoreException {
        InputStream is = null;
        BufferedReader br = null;
        try {
	        isXml = false;
	
	        /*
	         * 'true' if the syntax (XML or standard) of the file is given
	         * from external information: either via a JSP configuration element,
	         * the ".jspx" suffix, or the enclosing file (for included resources)
	         */
	        boolean isExternal = false;
	
	        /*
	         * Indicates whether we need to revert from temporary usage of
	         * "ISO-8859-1" back to "UTF-8"
	         */
	        boolean revert = false;
	        //need to get config for this resource in case it is included and has a different is-xml or pageEncoding
	        JspConfiguration specificConfiguration;
	        if (!jspOptions.isReusePropertyGroupConfigOnInclude()) {
	            specificConfiguration =  jspConfiguration.getConfigManager().getConfigurationForUrl(resolvedRelativeURL);
	            //going to update pageEncoding so as to not have a problem later
	            jspConfiguration.setPageEncoding(specificConfiguration.getPageEncoding());
	        } else {
	            specificConfiguration = jspConfiguration;//old way, do not get new configuration for included url
	        }
	        //local variable to get config for potential prelude/coda
	        if (specificConfiguration.isXmlSpecified()) {
	            // If <is-xml> is specified in a <jsp-property-group>, it is used.
	            isXml = specificConfiguration.isXml();
	            jspConfiguration.setIsXml(isXml);//need to set the value in the main jspConfiguration object
	            isExternal = true;
	        } else if (jspInputSource.getRelativeURL().endsWith(".jspx")
	                || jspInputSource.getRelativeURL().endsWith(".tagx")) {
	            isXml = true;
	            isExternal = true;
                //436188 need to set the value in the configuration to true
                jspConfiguration.setIsXml(true);
	        }
	        if (isExternal && !isXml) {
	            // JSP (standard) syntax. Use encoding specified in jsp-config
	            // if provided.
	            sourceEnc = jspConfiguration.getPageEncoding();
	            if (sourceEnc != null) {
	                return;
	            }
	            // We don't know the encoding, so use BOM to determine it
	            sourceEnc = "ISO-8859-1";
	        } else {
	            // XML syntax or unknown, (auto)detect encoding ...
	            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
	                logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "about to call detectXMLEncoding(), sourceEnc = {0}", new Object[] {sourceEnc});
	                logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "about to call detectXMLEncoding(), isXml = {0}", new Object[] {isXml});
	            } 
	    		detectXMLEncoding();    
	    		// If isBomPresent = true and isXml = fales then sourceEnc was determined by a byte order mark. 
	            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
	                logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "back from detectXMLEncoding(), sourceEnc = {0}", new Object[] {sourceEnc});
	                logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "back from detectXMLEncoding(), isXml = {0}", new Object[] {isXml});
	            } 
	
	            if (!isXml && sourceEnc.equalsIgnoreCase("UTF-8")) {
	                /*
	                 * We don't know if we're dealing with XML or standard syntax.
	                 * Therefore, we need to check to see if the page contains
	                 * a <jsp:root> element.
	                 *
	                 * We need to be careful, because the page may be encoded in
	                 * ISO-8859-1 (or something entirely different), and may
	                 * contain byte sequences that will cause a UTF-8 converter to
	                 * throw exceptions. 
	                 *
	                 * It is safe to use a source encoding of ISO-8859-1 in this
	                 * case, as there are no invalid byte sequences in ISO-8859-1,
	                 * and the byte/character sequences we're looking for (i.e.,
	                 * <jsp:root>) are identical in either encoding (both UTF-8
	                 * and ISO-8859-1 are extensions of ASCII).
	                 */
	                sourceEnc = "ISO-8859-1";
	                revert = true;
		            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
		                logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "Just reverted sourceEnc, sourceEnc = {0}", new Object[] {sourceEnc});
		            } 
	            }
	        }
	        if (isXml) {
	            // (This implies 'isExternal' is TRUE.)
	            // We know we're dealing with a JSP document (via JSP config or
	            // ".jspx" suffix), so we're done.
	            return;
	        }
	
	        /*
	         * At this point, 'isExternal' or 'isXml' is FALSE.
	         * Search for jsp:root action, in order to determine if we're dealing 
	         * with XML or standard syntax (unless we already know what we're 
	         * dealing with, i.e., when 'isExternal' is TRUE and 'isXml' is FALSE).
	         * No check for XML prolog, since nothing prevents a page from
	         * outputting XML and still using JSP syntax (in this case, the 
	         * XML prolog is treated as template text).
	         */
	        String savedEncoding=sourceEnc;
	        is = getInputStream();
	        try {
	        	br = new BufferedReader(new InputStreamReader(is, sourceEnc), Constants.DEFAULT_BUFFER_SIZE);
	            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
	                logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "created BufferedReader with sourceEnc = {0}", new Object[] {sourceEnc});
	            } 
	        }
	        catch (IOException e) {
	            throw new JspCoreException(e);
	        }
	        if (!isExternal) {
	            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
	                logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "!isExternal about to call hasJspRoot()");
	            } 
	            if (hasJspRoot(br)) {
	                if (revert) {
	                    sourceEnc = "UTF-8";
	                }
	                isXml = true;
	                //512075 need to set the value in the configuration to true
	                jspConfiguration.setIsXml(true);
	                return;
	            } else {
	                if (revert && isBomPresent) {
	                    sourceEnc = "UTF-8";
	                }
	                isXml = false;
	            }
	            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
	                logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "after hasJspRoot(), sourceEnc = {0}", new Object[] {sourceEnc});
	                logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "after hasJspRoot(), isXml = {0}", new Object[] {isXml});
	            } 
	        }
	
	        /*
	         * At this point, we know we're dealing with JSP syntax.
	         * If an XML prolog is provided, it's treated as template text.
	         * Determine the page encoding from the page directive, unless it's
	         * specified via JSP config.
	         */
	        String sourceEncFromConfig = null;	//store pageEncoding set by jsp configuration elements
	        String sourceEncFromDir = null; //store pageEncoding set by page directives

	        	        
	        // check for  sourceEnc from jsp configuration elements
            sourceEncFromConfig = jspConfiguration.getPageEncoding();
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "from jspConfiguration, sourceEnc = {0}", new Object[] {sourceEncFromConfig});
            } 
            
            // check for pageEncoding from page directives
         	try {
				br.reset();
	            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
	                logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "called br.reset()");
	            } 
			} catch (IOException e) {
		        try {
		        	is.close();
		        	br.close();
		        	is = getInputStream();
		        	br = new BufferedReader(new InputStreamReader(is, savedEncoding), Constants.DEFAULT_BUFFER_SIZE);
		            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
		                logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "created new BufferedReader with savedEncoding = {0}", new Object[] {savedEncoding});
		            } 
		        }
		        catch (IOException ioe) {
		            throw new JspCoreException(ioe);
		        }
			}
			sourceEncFromDir = savedEncoding; // defect 410785
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "calling getPageEncodingForJspSyntax(br) with sourceEnc = {0}", new Object[] {sourceEncFromDir});
            } 
            sourceEncFromDir = getPageEncodingForJspSyntax(br);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "back from getPageEncodingForJspSyntax(br), sourceEnc = {0}", new Object[] {sourceEncFromDir});
            } 
            
            // we have now checked for pageEncoding values from both jsp configuration elements and from page directives.
            // now check for conflicts and set the value per the spec algorithm
            if(isBomPresent){
            	//use the value already set for sourceEnc earlier
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                    logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "BOM is present, attempt to use page encoding from BOM, sourceEnc = {0}", new Object[] {sourceEnc});
                } 
            	// check for conflicts between BOM value and jsp config.  
            	if(sourceEncFromConfig!=null && !sourceEncFromConfig.equalsIgnoreCase(sourceEnc)
            			&& (!sourceEncFromConfig.toUpperCase().startsWith("UTF-16") || !sourceEnc.toUpperCase().startsWith("UTF-16"))){	 // see http://unicode.org/faq/utf_bom.html#36 for info on UTF-16
            		throw new JspCoreException("jsp.error.prolog_config_encoding_mismatch", new Object[] { sourceEnc,sourceEncFromConfig});
            	}
            	//check for conflicts between BOM value and page directives
            	if(sourceEncFromDir!=null && !sourceEncFromDir.equalsIgnoreCase(sourceEnc)
            			&& (!sourceEncFromDir.toUpperCase().startsWith("UTF-16") || !sourceEnc.toUpperCase().startsWith("UTF-16"))){	// see http://unicode.org/faq/utf_bom.html#36 for info on UTF-16
            		throw new JspCoreException("jsp.error.prolog_config_encoding_mismatch", new Object[] { sourceEnc,sourceEncFromDir});
            	}            
            }else{   	//!isBomPresent     
            	if (sourceEncFromConfig!=null){
            		sourceEnc = sourceEncFromConfig;
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                        logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "Attempt to use page encoding from config, sourceEnc = {0}", new Object[] {sourceEnc});
                    } 
                    // check for conflicts between page directives and config elements.  do not throw exception for conflicts if app is servlet 2.4 or earlier to maintain customer compatiblity
            		if(sourceEncFromDir!=null &&!sourceEncFromConfig.equalsIgnoreCase(sourceEncFromDir) && isServlet25){
            			throw new JspCoreException("jsp.error.encoding.mismatch.config.pageencoding", new Object[] { sourceEncFromConfig,sourceEncFromDir});
            		}            		
            	}else if(sourceEncFromDir!=null){	//not config setting, use the page attribute value if available
            		sourceEnc = sourceEncFromDir;
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                        logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "Attempt to use page encoding from page directives, sourceEnc = {0}", new Object[] {sourceEnc});
                    } 
            	}else{	// no config setting or page attribute value, default to "ISO-8859-1" per JSP spec
            		sourceEnc = "ISO-8859-1";
            		isDefaultPageEncoding = true;
            		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
            			logger.logp(Level.FINER, CLASS_NAME, "getSyntaxAndEncoding", "sourceEnc was null, setting to default, sourceEnc = {0}", new Object[] {sourceEnc});
            		} 
            	}
            }
	        
        } finally {
            if (br != null){
                try {
                    br.close();    
                }
                catch (IOException e) {}
            }
            if (is != null){
                try {
                    is.close();    
                }
                catch (IOException e) {}
            }
        }
	}
    
	private boolean hasJspRoot(BufferedReader br) throws JspCoreException {
        boolean hasRoot=false;
        JspEncodingScanner scanner = null;
            
        // create reader with sourceEnc, determined earlier
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
            logger.logp(Level.FINER, CLASS_NAME, "hasJspRoot", "Using Encoding = {0}", new Object[] {sourceEnc});
        } 
        scanner = new JspEncodingScanner(br);
        scanner.scan();
        
        hasRoot=scanner.jspRootFound();
        return (hasRoot);
    }

	
	private String getPageEncodingForJspSyntax(BufferedReader br) throws JspCoreException {
        JspEncodingScanner scanner = null;
        String retEncoding=null;

	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
	        logger.logp(Level.FINER, CLASS_NAME, "getPageEncodingForJspSyntax", " using Encoding = {0}", new Object[] {sourceEnc});
	    } 
	    scanner = new JspEncodingScanner(br);
	    scanner.scan(); // defect 410785
	    if (scanner.getEncoding() != null && scanner.getEncoding().equals(DEFAULT_ENCODING) == false) {
	        
	        /* The scan has found an encoding. Use this to create a new BufferedReader */
	        
	        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
	            logger.logp(Level.FINER, CLASS_NAME, "getPageEncodingForJspSyntax", "Scanned Encoding = {0}", new Object[] {scanner.getEncoding()});
	        } 
	        retEncoding = com.ibm.wsspi.webcontainer.util.EncodingUtils.getJvmConverter(scanner.getEncoding());
	        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
	            logger.logp(Level.FINER, CLASS_NAME, "getPageEncodingForJspSyntax", "Converted Scanned Encoding = {0}", new Object[] {retEncoding});
	        } 
	    }
	    else {
	        if (CONVERTED_DEFAULT_ENCODING.equals(DEFAULT_ENCODING) == false) {
	            
	            /* The Converted Default Encoding is different from the standard one */
	            /* Close the old one and reopen using the converted one */
	            retEncoding=CONVERTED_DEFAULT_ENCODING;
	            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
	                logger.logp(Level.FINER, CLASS_NAME, "getPageEncodingForJspSyntax", "Converted Default Encoding = {0}", new Object[] {retEncoding});
	            } 
	        }
	    }
        return (retEncoding);
    }
	

	private Document getJspDocumentAsJspPage() throws JspCoreException {
        Document document = null;
        InputStream is = null;
        BufferedReader br = null;
        try {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                logger.logp(Level.FINER, CLASS_NAME, "getJspDocumentAsJspPage", " creating BufferedReader with encoding = {0}", new Object[] {sourceEnc});
            } 
            is = getInputStream();
            
            String brEnc;
            if(!sourceEnc.toUpperCase().startsWith("UTF-16")){
            	brEnc = sourceEnc;
            }else{
            	brEnc = "UTF-16";
            }
            br = new BufferedReader(new InputStreamReader(is, brEnc), Constants.DEFAULT_BUFFER_SIZE);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                logger.logp(Level.FINER, CLASS_NAME, "getJspDocumentAsJspPage", " CREATED BufferedReader with encoding = {0}", new Object[] {sourceEnc});
            } 
            JspPageParser jspPageParser = new JspPageParser(br,
                                                            jspInputSource,
                                                            resolvedRelativeURL,
                                                            context,
                                                            directoryStack,
                                                            jspConfiguration,
                                                            jspOptions,        //396002
                                                            dependencyStack,
                                                            dependencyList,
                                                            cdataJspIdMap,
                                                            implicitTagLibMap);
			try{
				document = jspPageParser.parse();                                                               
			}
            catch (JspCoreException jce){
				throw new JspCoreException(jspPageParser.buildLineNumberMessage(jce.getLocalizedMessage()));
			}                
        }
        catch (IOException e) {
            throw new JspCoreException(e);
        }
        finally {
            if (br != null){
                try {
                    br.close();    
                }
                catch (IOException e) {}
            }
            if (is != null){
                try {
                    is.close();    
                }
                catch (IOException e) {}
            }
        }
        return (document);
    }
    
    protected Document parseToDom() throws JspCoreException {
        Document document = null;
        InputStream is = getInputStream();
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
            logger.logp(Level.FINER, CLASS_NAME, "Jsp2Dom", "parseToDom 1 this.jspInputSource: ["+this.jspInputSource.getRelativeURL()+"]");
        } 
        try {
            InputSource inputSource = new InputSource(is);
            JspDocumentParser jspDocumentParser = new JspDocumentParser(jspInputSource,
                                                                        resolvedRelativeURL,
                                                                        context, 
                                                                        jspConfiguration,
                                                                        jspOptions, // 396002
                                                                        directoryStack,
                                                                        dependencyStack,
                                                                        dependencyList,
                                                                        cdataJspIdMap,
                                                                        implicitTagLibMap,
                                                                        isBomPresent,
                                                                        isEncodingSpecifiedInProlog,
                                                                        sourceEnc);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                logger.logp(Level.FINER, CLASS_NAME, "Jsp2Dom", "parseToDom 1 jspDocumentParser: ["+jspDocumentParser+"]");
            } 
			try{
	            document = jspDocumentParser.parse(inputSource);
			}
            catch (JspCoreException jce){
				throw new JspCoreException(jspDocumentParser.buildLineNumberMessage(jce.getLocalizedMessage()));
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
        return (document);
    }

	private void detectXMLEncoding() throws JspCoreException {
        Object[] ret;
		try {
	        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
	            logger.logp(Level.FINER, CLASS_NAME, "Jsp2Dom", "detectXMLEncoding about to call XMLEncodingDetector: ["+this.jspInputSource.getRelativeURL()+"]");
	        } 
			ret = XMLEncodingDetector.getEncoding(jspInputSource);
	        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
	            logger.logp(Level.FINER, CLASS_NAME, "Jsp2Dom", "detectXMLEncoding back from XMLEncodingDetector: ["+ret+"]");
	        } 
		} catch (IOException e1) {
            throw new JspCoreException(e1);
		}
        this.sourceEnc = (String) ret[0];
        if (((Boolean) ret[1]).booleanValue()) {
            this.isEncodingSpecifiedInProlog = true;
        }
        if (((Boolean) ret[2]).booleanValue()) {
            this.isBomPresent = true;
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
            logger.logp(Level.FINER, CLASS_NAME, "Jsp2Dom", "detectXMLEncoding back from XMLEncodingDetector  sourceEnc: ["+this.sourceEnc+"]");
            logger.logp(Level.FINER, CLASS_NAME, "Jsp2Dom", "detectXMLEncoding back from XMLEncodingDetector  isEncodingSpecifiedInProlog: ["+this.isEncodingSpecifiedInProlog+"]");
            logger.logp(Level.FINER, CLASS_NAME, "Jsp2Dom", "detectXMLEncoding back from XMLEncodingDetector  isBomPresent: ["+this.isBomPresent+"]");
        }
	}

    protected InputStream getInputStream() throws JspCoreException{
        InputStream is = null;
        try {
            is = jspInputSource.getInputStream();
        }
        catch (IOException e) {
            String msg = JspCoreException.getMsg("jsp.error.failed.to.find.resource", new Object[] {jspInputSource.getRelativeURL()});
            throw new JspCoreException(msg, new FileNotFoundException (msg));
        }
        return is;
    }

    private void resolveBaseDir() {
        String relativeURL = jspInputSource.getRelativeURL();
        if (relativeURL.charAt(0) != '/') {
            resolvedRelativeURL = (String) directoryStack.peek() + relativeURL;
            jspInputSource = context.getJspInputSourceFactory().copyJspInputSource(jspInputSource, resolvedRelativeURL);
        }
        else {
            resolvedRelativeURL = relativeURL;     
        }
        String baseDir = resolvedRelativeURL.substring(0, resolvedRelativeURL.lastIndexOf("/") + 1);
        directoryStack.push(baseDir);
    }
    
    public List getDependencyList() {
        return (dependencyList);
    }

    public Map getCdataJspIdMap() {
        return (cdataJspIdMap);
    }
}
