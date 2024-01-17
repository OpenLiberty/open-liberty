/*
 * Copyright (c) 1997-2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 package org.apache.taglibs.standard.tag.common.xml;

 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.Reader;
 import java.io.StringReader;
 
 import jakarta.servlet.http.HttpServletRequest;
 import jakarta.servlet.jsp.JspException;
 import jakarta.servlet.jsp.JspTagException;
 import jakarta.servlet.jsp.PageContext;
 import jakarta.servlet.jsp.tagext.BodyTagSupport;
 import javax.xml.XMLConstants;
 import javax.xml.parsers.DocumentBuilder;
 import javax.xml.parsers.DocumentBuilderFactory;
 import javax.xml.parsers.ParserConfigurationException;
 import javax.xml.transform.TransformerConfigurationException;
 import javax.xml.transform.TransformerFactory;
 import javax.xml.transform.dom.DOMResult;
 import javax.xml.transform.sax.SAXTransformerFactory;
 import javax.xml.transform.sax.TransformerHandler;
 
 import org.apache.taglibs.standard.resources.Resources;
 import org.apache.taglibs.standard.tag.common.core.ImportSupport;
 import org.apache.taglibs.standard.tag.common.core.Util;
 import org.w3c.dom.Document;
 import org.xml.sax.EntityResolver;
 import org.xml.sax.InputSource;
 import org.xml.sax.SAXException;
 import org.xml.sax.XMLFilter;
 import org.xml.sax.XMLReader;
 import org.xml.sax.helpers.XMLReaderFactory;
 
 /**
  * <p>Support for tag handlers for &lt;parse&gt;, the XML parsing tag.</p>
  *
  * @author Shawn Bayern
  */
 public abstract class ParseSupport extends BodyTagSupport {
 
     //*********************************************************************
     // Protected state
 
     protected Object xml;                          // 'xml' attribute
     protected String systemId;                     // 'systemId' attribute
     protected XMLFilter filter;			   // 'filter' attribute
 
     //*********************************************************************
     // Private state
 
     private String var;                            // 'var' attribute
     private String varDom;			   // 'varDom' attribute
     private int scope;				   // processed 'scope' attr
     private int scopeDom;			   // processed 'scopeDom' attr
 
     // state in support of XML parsing...
     private DocumentBuilderFactory dbf;
     private DocumentBuilder db;
     private TransformerFactory tf;
     private TransformerHandler th;
 
     // To secure XML processing
     private String ACCESS_EXTERNAL_DTD_SETTING = "javax.xml.accessExternalDTD";
     private String ACCESS_EXTERNAL_SCHEMA_SETTING = "javax.xml.accessExternalSchema";
 
     //*********************************************************************
     // Constructor and initialization
 
     public ParseSupport() {
     super();
     init();
     }
 
     private void init() {
     var = varDom = null;
     xml = null;
     systemId = null;
     filter = null;
     dbf = null;
     db = null;
     tf = null;
     th = null;
     scope = PageContext.PAGE_SCOPE;
     scopeDom = PageContext.PAGE_SCOPE;
     }
 
 
     //*********************************************************************
     // Tag logic
 
     // parse 'source' or body, storing result in 'var'
     public int doEndTag() throws JspException {
       try {
     
     // set up our DocumentBuilder
         if (dbf == null) {
             dbf = DocumentBuilderFactory.newInstance();
             dbf.setNamespaceAware(true);
             dbf.setValidating(false);
             try {
                 dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                 
                 // JDK may not implement FEATURE_SECURE_PROCESSING, so force it to be secure if 
                 // neither property is set explicity.
                 String accessExternalDTD = System.getProperty("javax.xml.accessExternalDTD");
                 String accessExternalSchema = System.getProperty("javax.xml.accessExternalSchema");
   
                 if(accessExternalDTD == null){
                     dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                 }
                 if(accessExternalSchema == null){
                     dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                 }
             } catch (ParserConfigurationException e) {
                 throw new AssertionError("Parser does not support secure processing");
             }
         }
         db = dbf.newDocumentBuilder();
 
     // if we've gotten a filter, set up a transformer to support it
     if (filter != null) {
             if (tf == null)
                 tf = TransformerFactory.newInstance();
             if (!tf.getFeature(SAXTransformerFactory.FEATURE))
                 throw new JspTagException(
             Resources.getMessage("PARSE_NO_SAXTRANSFORMER"));
             try {
                 tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                 
                 // JDK may not implement FEATURE_SECURE_PROCESSING, so force it to be secure if 
                 // neither property is set explicity.
                 String accessExternalDTD = System.getProperty("javax.xml.accessExternalDTD");
                 String accessExternalSchema = System.getProperty("javax.xml.accessExternalSchema");
   
                 if(accessExternalDTD == null){
                    tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                 }
                 if(accessExternalSchema == null){
                    tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                 }
             } catch (TransformerConfigurationException e) {
                 throw new AssertionError(
                         "TransformerFactory does not support secure processing");
             }
             SAXTransformerFactory stf = (SAXTransformerFactory) tf;
             th = stf.newTransformerHandler();
     }
 
     // produce a Document by parsing whatever the attributes tell us to use
     Document d;
     Object xmlText = this.xml;
     if (xmlText == null) {
         // if the attribute was specified, use the body as 'xml'
         if (bodyContent != null && bodyContent.getString() != null)
         xmlText = bodyContent.getString().trim();
         else
         xmlText = "";
     }
     if (xmlText instanceof String)
         d = parseStringWithFilter((String) xmlText, filter);
     else if (xmlText instanceof Reader)
         d = parseReaderWithFilter((Reader) xmlText, filter);
     else
         throw new JspTagException(
             Resources.getMessage("PARSE_INVALID_SOURCE"));
 
     // we've got a Document object; store it out as appropriate
     // (let any exclusivity or other constraints be enforced by TEI/TLV)
     if (var != null)
         pageContext.setAttribute(var, d, scope);
     if (varDom != null)
         pageContext.setAttribute(varDom, d, scopeDom);
 
     return EVAL_PAGE;
       } catch (SAXException ex) {
     throw new JspException(ex);
       } catch (IOException ex) {
     throw new JspException(ex);
       } catch (ParserConfigurationException ex) {
     throw new JspException(ex);
       } catch (TransformerConfigurationException ex) {
     throw new JspException(ex);
       }
     }
 
     // Releases any resources we may have (or inherit)
     public void release() {
     init();
     }
 
 
     //*********************************************************************
     // Private utility methods
 
     /** Parses the given InputSource after, applying the given XMLFilter. */
     private Document parseInputSourceWithFilter(InputSource s, XMLFilter f)
             throws SAXException, IOException {
     if (f != null) {
             // prepare an output Document
             Document o = db.newDocument();
 
             // use TrAX to adapt SAX events to a Document object
             th.setResult(new DOMResult(o));
             XMLReader xr = XMLReaderFactory.createXMLReader();
         xr.setEntityResolver(new JstlEntityResolver(pageContext));
             //   (note that we overwrite the filter's parent.  this seems
             //    to be expected usage.  we could cache and reset the old
             //    parent, but you can't setParent(null), so this wouldn't
             //    be perfect.)
             f.setParent(xr);
             f.setContentHandler(th);
             f.parse(s);
             return o;
     } else
         return parseInputSource(s);	
     }
 
     /** Parses the given Reader after applying the given XMLFilter. */
     private Document parseReaderWithFilter(Reader r, XMLFilter f)
             throws SAXException, IOException {
     return parseInputSourceWithFilter(new InputSource(r), f);
     }
 
     /** Parses the given String after applying the given XMLFilter. */
     private Document parseStringWithFilter(String s, XMLFilter f)
             throws SAXException, IOException {
         StringReader r = new StringReader(s);
         return parseReaderWithFilter(r, f);
     }
 
     /** Parses the given Reader after applying the given XMLFilter. */
     private Document parseURLWithFilter(String url, XMLFilter f)
             throws SAXException, IOException {
     return parseInputSourceWithFilter(new InputSource(url), f);
     }
 
     /** Parses the given InputSource into a Document. */
     private Document parseInputSource(InputSource s)
         throws SAXException, IOException {
     db.setEntityResolver(new JstlEntityResolver(pageContext));
 
         // normalize URIs so they can be processed consistently by resolver
         if (systemId == null)
             s.setSystemId("jstl:");
     else if (ImportSupport.isAbsoluteUrl(systemId))
             s.setSystemId(systemId);
         else
             s.setSystemId("jstl:" + systemId);
     return db.parse(s);
     }
 
     /** Parses the given Reader into a Document. */
     private Document parseReader(Reader r) throws SAXException, IOException {
         return parseInputSource(new InputSource(r));
     }
 
     /** Parses the given String into a Document. */
     private Document parseString(String s) throws SAXException, IOException {
         StringReader r = new StringReader(s);
         return parseReader(r);
     }
 
     /** Parses the URL (passed as a String) into a Document. */
     private Document parseURL(String url) throws SAXException, IOException {
     return parseInputSource(new InputSource(url));
     }
 
     //*********************************************************************
     // JSTL-specific EntityResolver class
 
     /** Lets us resolve relative external entities. */
     public static class JstlEntityResolver implements EntityResolver {
     private final PageContext ctx;
         public JstlEntityResolver(PageContext ctx) {
             this.ctx = ctx;
         }
         public InputSource resolveEntity(String publicId, String systemId)
             throws FileNotFoundException {
 
         // pass if we don't have a systemId
         if (systemId == null)
         return null;
 
         // strip leading "jstl:" off URL if applicable
         if (systemId.startsWith("jstl:"))
         systemId = systemId.substring(5);
 
         // we're only concerned with relative URLs
         if (ImportSupport.isAbsoluteUrl(systemId))
         return null;
 
         // for relative URLs, load and wrap the resource.
         // don't bother checking for 'null' since we specifically want
         // the parser to fail if the resource doesn't exist
         InputStream s;
         if (systemId.startsWith("/")) {
             s = ctx.getServletContext().getResourceAsStream(systemId);
             if (s == null)
             throw new FileNotFoundException(
             Resources.getMessage("UNABLE_TO_RESOLVE_ENTITY",
              systemId));
         } else {
         String pagePath =
             ((HttpServletRequest) ctx.getRequest()).getServletPath();
         String basePath =
             pagePath.substring(0, pagePath.lastIndexOf("/"));
         s = ctx.getServletContext().getResourceAsStream(
               basePath + "/" + systemId);
             if (s == null)
             throw new FileNotFoundException(
             Resources.getMessage("UNABLE_TO_RESOLVE_ENTITY",
              systemId));
         }
         return new InputSource(s);
         }
     }
 
     //*********************************************************************
     // Tag attributes
 
     public void setVar(String var) {
     this.var = var;
     }
 
     public void setVarDom(String varDom) {
     this.varDom = varDom;
     }
 
     public void setScope(String scope) {
     this.scope = Util.getScope(scope);
     }
 
     public void setScopeDom(String scopeDom) {
     this.scopeDom = Util.getScope(scopeDom);
     }
 }
 