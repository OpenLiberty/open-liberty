/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.view.facelets.compiler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.util.Map;

import javax.el.ELException;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.view.Location;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.Tag;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.myfaces.config.element.FaceletsProcessing;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.view.facelets.tag.TagAttributeImpl;
import org.apache.myfaces.view.facelets.tag.TagAttributesImpl;
import org.apache.myfaces.view.facelets.tag.composite.CompositeLibrary;
import org.apache.myfaces.view.facelets.tag.composite.ImplementationHandler;
import org.apache.myfaces.view.facelets.tag.composite.InterfaceHandler;
import org.apache.myfaces.view.facelets.tag.jsf.core.CoreLibrary;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Compiler implementation that uses SAX
 * 
 * @see org.apache.myfaces.view.facelets.compiler.Compiler
 * 
 * @author Jacob Hookom
 * @version $Id: SAXCompiler.java 1542444 2013-11-16 01:41:08Z lu4242 $
 */
public final class SAXCompiler extends Compiler
{

    private final static Pattern XML_DECLARATION = Pattern
            .compile("^<\\?xml.+?version=['\"](.+?)['\"](.+?encoding=['\"]((.+?))['\"])?.*?\\?>");

    private static class CompilationHandler extends DefaultHandler implements LexicalHandler
    {

        private final String alias;

        private boolean inDocument = false;

        private Locator locator;

        private final CompilationManager unit;
        
        private boolean consumingCDATA = false;
        private boolean swallowCDATAContent = false;

        public CompilationHandler(CompilationManager unit, String alias)
        {
            this.unit = unit;
            this.alias = alias;
        }

        public void characters(char[] ch, int start, int length) throws SAXException
        {
            if (this.inDocument && (!consumingCDATA || (consumingCDATA && !swallowCDATAContent)))
            {
                this.unit.writeText(new String(ch, start, length));
            }
        }

        public void comment(char[] ch, int start, int length) throws SAXException
        {
            if (this.inDocument && !unit.getFaceletsProcessingInstructions().isConsumeXMLComments())
            {
                this.unit.writeComment(new String(ch, start, length));
            }
        }

        protected TagAttributes createAttributes(Attributes attrs)
        {
            int len = attrs.getLength();
            TagAttribute[] ta = new TagAttribute[len];
            for (int i = 0; i < len; i++)
            {
                ta[i] = new TagAttributeImpl(this.createLocation(), attrs.getURI(i), attrs.getLocalName(i), attrs
                        .getQName(i), attrs.getValue(i));
            }
            return new TagAttributesImpl(ta);
        }

        protected Location createLocation()
        {
            return new Location(this.alias, this.locator.getLineNumber(), this.locator.getColumnNumber());
        }

        public void endCDATA() throws SAXException
        {
            if (this.inDocument)
            {
                if (!this.unit.getFaceletsProcessingInstructions().isConsumeCDataSections())
                {
                    this.unit.writeInstruction("]]>");
                }
                else
                {
                    this.consumingCDATA = false;
                    this.swallowCDATAContent = false;
                }
            }
        }

        public void endDTD() throws SAXException
        {
            this.inDocument = true;
        }

        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            this.unit.popTag();
        }

        public void endEntity(String name) throws SAXException
        {
        }

        public void endPrefixMapping(String prefix) throws SAXException
        {
            this.unit.popNamespace(prefix);
        }

        public void fatalError(SAXParseException e) throws SAXException
        {
            if (this.locator != null)
            {
                throw new SAXException("Error Traced[line: " + this.locator.getLineNumber() + "] " + e.getMessage());
            }
            else
            {
                throw e;
            }
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException
        {
            if (this.inDocument)
            {
                this.unit.writeWhitespace(new String(ch, start, length));
            }
        }

        public InputSource resolveEntity(String publicId, String systemId) throws SAXException
        {
            String dtd = "org/apache/myfaces/resource/default.dtd";
            /*
             * if ("-//W3C//DTD XHTML 1.0 Transitional//EN".equals(publicId)) { dtd = "xhtml1-transitional.dtd"; } else
             * if (systemId != null && systemId.startsWith("file:/")) { return new InputSource(systemId); }
             */
            URL url = ClassUtils.getResource(dtd);
            return new InputSource(url.toString());
        }

        public void setDocumentLocator(Locator locator)
        {
            this.locator = locator;
        }

        public void startCDATA() throws SAXException
        {
            if (this.inDocument)
            {
                if (!this.unit.getFaceletsProcessingInstructions().isConsumeCDataSections())
                {
                    this.unit.writeInstruction("<![CDATA[");
                }
                else
                {
                    this.consumingCDATA = true;
                    this.swallowCDATAContent = this.unit.getFaceletsProcessingInstructions().isSwallowCDataContent();
                }
            }
        }

        public void startDocument() throws SAXException
        {
            this.inDocument = true;
        }

        public void startDTD(String name, String publicId, String systemId) throws SAXException
        {
            if (this.inDocument && !unit.getFaceletsProcessingInstructions().isConsumeXmlDocType())
            {
                this.unit.writeDoctype(name, publicId, systemId);
                /*
                StringBuffer sb = new StringBuffer(64);
                sb.append("<!DOCTYPE ").append(name);
                if (publicId != null)
                {
                    sb.append(" PUBLIC \"").append(publicId).append("\"");
                    if (systemId != null)
                    {
                        sb.append(" \"").append(systemId).append("\"");
                    }
                }
                else if (systemId != null)
                {
                    sb.append(" SYSTEM \"").append(systemId).append("\"");
                }
                sb.append(" >\n");
                this.unit.writeInstruction(sb.toString());
                */
            }
            this.inDocument = false;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            this.unit.pushTag(new Tag(this.createLocation(), uri, localName, qName, this.createAttributes(attributes)));
        }

        public void startEntity(String name) throws SAXException
        {
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException
        {
            this.unit.pushNamespace(prefix, uri);
        }

        public void processingInstruction(String target, String data) throws SAXException
        {
            if (this.inDocument && !this.unit.getFaceletsProcessingInstructions().isConsumeProcessingInstructions())
            {
                StringBuffer sb = new StringBuffer(64);
                sb.append("<?").append(target).append(' ').append(data).append("?>\n");
                this.unit.writeInstruction(sb.toString());
            }
        }
    }
    
    /**
     * Like CompilationHandler but does not take into account everything outside f:metadata tag 
     * 
     * @since 2.0
     */
    private static class ViewMetadataHandler extends DefaultHandler implements LexicalHandler
    {

        private final String alias;

        private boolean inDocument = false;

        private Locator locator;

        private final CompilationManager unit;
        
        private boolean inMetadata = false;
        
        private boolean consumingCDATA = false;
        private boolean swallowCDATAContent = false;

        public ViewMetadataHandler(CompilationManager unit, String alias)
        {
            this.unit = unit;
            this.alias = alias;
        }

        public void characters(char[] ch, int start, int length) throws SAXException
        {
            if (this.inDocument && inMetadata && (!consumingCDATA || (consumingCDATA && !swallowCDATAContent)))
            {
                this.unit.writeText(new String(ch, start, length));
            }
        }

        public void comment(char[] ch, int start, int length) throws SAXException
        {
            if (this.inDocument && inMetadata && !unit.getFaceletsProcessingInstructions().isConsumeXMLComments())
            {
                this.unit.writeComment(new String(ch, start, length));
            }
        }

        protected TagAttributes createAttributes(Attributes attrs)
        {
            int len = attrs.getLength();
            TagAttribute[] ta = new TagAttribute[len];
            for (int i = 0; i < len; i++)
            {
                ta[i] = new TagAttributeImpl(this.createLocation(), attrs.getURI(i), attrs.getLocalName(i), attrs
                        .getQName(i), attrs.getValue(i));
            }
            return new TagAttributesImpl(ta);
        }

        protected Location createLocation()
        {
            return new Location(this.alias, this.locator.getLineNumber(), this.locator.getColumnNumber());
        }

        public void endCDATA() throws SAXException
        {
            if (this.inDocument && inMetadata)
            {
                if (!this.unit.getFaceletsProcessingInstructions().isConsumeCDataSections())
                {
                    this.unit.writeInstruction("]]>");
                }
                else
                {
                    this.consumingCDATA = false;
                    this.swallowCDATAContent = false;
                }
            }
        }

        public void endDTD() throws SAXException
        {
            this.inDocument = true;
        }

        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            if (inMetadata)
            {
                this.unit.popTag();
            }
            if ( (CoreLibrary.NAMESPACE.equals(uri) ||
                CoreLibrary.ALIAS_NAMESPACE.equals(uri)))
            {
                if ("metadata".equals(localName))
                {
                    this.inMetadata=false;
                }
                else if (!inMetadata && "view".equals(localName))
                {
                    this.unit.popTag();
                }
            }            
        }

        public void endEntity(String name) throws SAXException
        {
        }

        public void endPrefixMapping(String prefix) throws SAXException
        {
            this.unit.popNamespace(prefix);
        }

        public void fatalError(SAXParseException e) throws SAXException
        {
            if (this.locator != null)
            {
                throw new SAXException("Error Traced[line: " + this.locator.getLineNumber() + "] " + e.getMessage());
            }
            else
            {
                throw e;
            }
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException
        {
            if (this.inDocument && inMetadata)
            {
                this.unit.writeWhitespace(new String(ch, start, length));
            }
        }

        public InputSource resolveEntity(String publicId, String systemId) throws SAXException
        {
            String dtd = "org/apache/myfaces/resource/default.dtd";
            /*
             * if ("-//W3C//DTD XHTML 1.0 Transitional//EN".equals(publicId)) { dtd = "xhtml1-transitional.dtd"; } else
             * if (systemId != null && systemId.startsWith("file:/")) { return new InputSource(systemId); }
             */
            URL url = ClassUtils.getResource(dtd);
            return new InputSource(url.toString());
        }

        public void setDocumentLocator(Locator locator)
        {
            this.locator = locator;
        }

        public void startCDATA() throws SAXException
        {
            if (this.inDocument && inMetadata)
            {
                if (!this.unit.getFaceletsProcessingInstructions().isConsumeCDataSections())
                {
                    this.unit.writeInstruction("<![CDATA[");
                }
                else
                {
                    this.consumingCDATA = true;
                    this.swallowCDATAContent = this.unit.getFaceletsProcessingInstructions().isSwallowCDataContent();
                }
            }
        }

        public void startDocument() throws SAXException
        {
            this.inDocument = true;
        }

        public void startDTD(String name, String publicId, String systemId) throws SAXException
        {
            // metadata does not require output doctype
            this.inDocument = false;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            if ( CoreLibrary.NAMESPACE.equals(uri) ||
                 CoreLibrary.ALIAS_NAMESPACE.equals(uri))
            {
                if ("metadata".equals(localName))
                {
                    this.inMetadata=true;
                }
                else if (!inMetadata && "view".equals(localName))
                {
                    this.unit.pushTag(new Tag(createLocation(), uri, localName, qName, createAttributes(attributes)));
                }
            }
            if (inMetadata)
            {
                this.unit.pushTag(new Tag(createLocation(), uri, localName, qName, createAttributes(attributes)));
            }
        }

        public void startEntity(String name) throws SAXException
        {
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException
        {
            this.unit.pushNamespace(prefix, uri);
        }

        public void processingInstruction(String target, String data) throws SAXException
        {
            if (inDocument && inMetadata && !unit.getFaceletsProcessingInstructions().isConsumeProcessingInstructions())
            {
                StringBuffer sb = new StringBuffer(64);
                sb.append("<?").append(target).append(' ').append(data).append("?>\n");
                unit.writeInstruction(sb.toString());
            }
        }        
    }
    
    /**
     * Like CompilationHandler but does not take into account everything outside cc:interface or cc:implementation tag.
     *  
     * Note inside cc:implementation it only takes into account cc:insertChildren, cc:insertFacet and cc:renderFacet,
     * all other tags, comments or text are just skipped.
     * 
     * @since 2.0.1
     */
    private static class CompositeComponentMetadataHandler extends DefaultHandler implements LexicalHandler
    {

        private final String alias;

        private boolean inDocument = false;

        private Locator locator;

        private final CompilationManager unit;
        
        private boolean inCompositeInterface = false;
        
        private boolean inCompositeImplementation = false;

        private boolean consumingCDATA = false;
        private boolean swallowCDATAContent = false;

        public CompositeComponentMetadataHandler(CompilationManager unit, String alias)
        {
            this.unit = unit;
            this.alias = alias;
        }

        public void characters(char[] ch, int start, int length) throws SAXException
        {
            if (this.inDocument && inCompositeInterface && 
                    (!consumingCDATA || (consumingCDATA && !swallowCDATAContent)))
            {
                this.unit.writeText(new String(ch, start, length));
            }
        }

        public void comment(char[] ch, int start, int length) throws SAXException
        {
            if (inDocument && inCompositeInterface && 
                    !unit.getFaceletsProcessingInstructions().isConsumeXMLComments())
            {
                this.unit.writeComment(new String(ch, start, length));
            }
        }

        protected TagAttributes createAttributes(Attributes attrs)
        {
            int len = attrs.getLength();
            TagAttribute[] ta = new TagAttribute[len];
            for (int i = 0; i < len; i++)
            {
                ta[i] = new TagAttributeImpl(this.createLocation(), attrs.getURI(i), attrs.getLocalName(i), attrs
                        .getQName(i), attrs.getValue(i));
            }
            return new TagAttributesImpl(ta);
        }

        protected Location createLocation()
        {
            return new Location(this.alias, this.locator.getLineNumber(), this.locator.getColumnNumber());
        }

        public void endCDATA() throws SAXException
        {
            if (this.inDocument && inCompositeInterface)
            {
                if (!this.unit.getFaceletsProcessingInstructions().isConsumeCDataSections())
                {
                    this.unit.writeInstruction("]]>");
                }
                else
                {
                    this.consumingCDATA = false;
                    this.swallowCDATAContent = false;
                }
            }
        }

        public void endDTD() throws SAXException
        {
            this.inDocument = true;
        }

        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            if (inCompositeInterface)
            {
                this.unit.popTag();
            }
            else if (inCompositeImplementation && 
                (CompositeLibrary.NAMESPACE.equals(uri) || CompositeLibrary.ALIAS_NAMESPACE.equals(uri)) )
            {
                if ( "insertFacet".equals(localName) ||
                     "renderFacet".equals(localName) ||
                     "insertChildren".equals(localName) || 
                     ImplementationHandler.NAME.equals(localName))
                {
                    this.unit.popTag();
                }
            }
            
            if (CompositeLibrary.NAMESPACE.equals(uri) || CompositeLibrary.ALIAS_NAMESPACE.equals(uri))
            {
                if (InterfaceHandler.NAME.equals(localName))
                {
                    this.inCompositeInterface=false;
                }
                else if (ImplementationHandler.NAME.equals(localName))
                {
                    this.inCompositeImplementation=false;
                }
            }
        }

        public void endEntity(String name) throws SAXException
        {
        }

        public void endPrefixMapping(String prefix) throws SAXException
        {
            this.unit.popNamespace(prefix);
        }

        public void fatalError(SAXParseException e) throws SAXException
        {
            if (this.locator != null)
            {
                throw new SAXException("Error Traced[line: " + this.locator.getLineNumber() + "] " + e.getMessage());
            }
            else
            {
                throw e;
            }
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException
        {
            if (this.inDocument && inCompositeInterface)
            {
                this.unit.writeWhitespace(new String(ch, start, length));
            }
        }

        public InputSource resolveEntity(String publicId, String systemId) throws SAXException
        {
            String dtd = "org/apache/myfaces/resource/default.dtd";
            /*
             * if ("-//W3C//DTD XHTML 1.0 Transitional//EN".equals(publicId)) { dtd = "xhtml1-transitional.dtd"; } else
             * if (systemId != null && systemId.startsWith("file:/")) { return new InputSource(systemId); }
             */
            URL url = ClassUtils.getResource(dtd);
            return new InputSource(url.toString());
        }

        public void setDocumentLocator(Locator locator)
        {
            this.locator = locator;
        }

        public void startCDATA() throws SAXException
        {
            if (this.inDocument && inCompositeInterface)
            {
                if (!this.unit.getFaceletsProcessingInstructions().isConsumeCDataSections())
                {
                    this.unit.writeInstruction("<![CDATA[");
                }
                else
                {
                    this.consumingCDATA = true;
                    this.swallowCDATAContent = this.unit.getFaceletsProcessingInstructions().isSwallowCDataContent();
                }
            }
        }

        public void startDocument() throws SAXException
        {
            this.inDocument = true;
        }

        public void startDTD(String name, String publicId, String systemId) throws SAXException
        {
            // metadata does not require output doctype
            this.inDocument = false;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            if (CompositeLibrary.NAMESPACE.equals(uri) || CompositeLibrary.ALIAS_NAMESPACE.equals(uri))
            {
                if (InterfaceHandler.NAME.equals(localName))
                {
                    this.inCompositeInterface=true;
                }
                else if (ImplementationHandler.NAME.equals(localName))
                {
                    this.inCompositeImplementation=true;
                }
            }
            
            if (inCompositeInterface)
            {
                this.unit.pushTag(new Tag(createLocation(), uri, localName, qName, createAttributes(attributes)));
            }
            else if (inCompositeImplementation && 
                (CompositeLibrary.NAMESPACE.equals(uri) || CompositeLibrary.ALIAS_NAMESPACE.equals(uri)))
            {
                if ("insertFacet".equals(localName)    ||
                    "renderFacet".equals(localName)    ||
                    "insertChildren".equals(localName) ||
                    ImplementationHandler.NAME.equals(localName)   )
                {
                    this.unit.pushTag(new Tag(createLocation(), uri, localName, qName, createAttributes(attributes)));
                }
            }
        }

        public void startEntity(String name) throws SAXException
        {
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException
        {
            this.unit.pushNamespace(prefix, uri);
        }

        public void processingInstruction(String target, String data) throws SAXException
        {
            if (inDocument && inCompositeInterface
                && !unit.getFaceletsProcessingInstructions().isConsumeProcessingInstructions())
            {
                StringBuffer sb = new StringBuffer(64);
                sb.append("<?").append(target).append(' ').append(data).append("?>\n");
                this.unit.writeInstruction(sb.toString());
            }
        }        
    }

    public SAXCompiler()
    {
        super();
    }

    public FaceletHandler doCompile(URL src, String alias)
            throws IOException, FaceletException, ELException, FacesException
    {
        CompilationManager mngr = null;
        InputStream is = null;
        String encoding = null;
        try
        {
            is = new BufferedInputStream(src.openStream(), 1024);
            mngr = new CompilationManager(alias, this, getFaceletsProcessingInstructions(src, alias));
            encoding = writeXmlDecl(is, mngr);
            CompilationHandler handler = new CompilationHandler(mngr, alias);
            SAXParser parser = this.createSAXParser(handler);
            parser.parse(is, handler);
        }
        catch (SAXException e)
        {
            throw new FaceletException("Error Parsing " + alias + ": " + e.getMessage(), e.getCause());
        }
        catch (ParserConfigurationException e)
        {
            throw new FaceletException("Error Configuring Parser " + alias + ": " + e.getMessage(), e.getCause());
        }
        finally
        {
            if (is != null)
            {
                is.close();
            }
        }
        return new EncodingHandler(mngr.createFaceletHandler(), encoding);
    }

    /**
     * @since 2.0
     */
    @Override
    protected FaceletHandler doCompileViewMetadata(URL src, String alias)
            throws IOException, FaceletException, ELException, FacesException
    {
        CompilationManager mngr = null;
        InputStream is = null;
        String encoding = null;
        try
        {
            is = new BufferedInputStream(src.openStream(), 1024);
            mngr = new CompilationManager(alias, this, getFaceletsProcessingInstructions(src, alias));
            encoding = getXmlDecl(is, mngr);
            final ViewMetadataHandler handler = new ViewMetadataHandler(mngr, alias);
            final SAXParser parser = this.createSAXParser(handler);
            
            if (System.getSecurityManager() != null)
            {
                try
                {
                    final InputStream finalInputStream = is;
                    AccessController.doPrivileged(new PrivilegedExceptionAction() 
                    {
                        public Object run() throws SAXException, IOException 
                        {
                            parser.parse(finalInputStream, handler);
                            return null; 
                        }
                    });
                }
                catch (PrivilegedActionException pae)
                {
                    Exception e = pae.getException();
                    if(e instanceof SAXException)
                    {
                        throw new FaceletException("Error Parsing " + alias + ": " + e.getMessage(), e.getCause());
                    } 
                    else if(e instanceof IOException)
                    {
                        throw (IOException)e;
                    }
                }
            }
            else
            {
                parser.parse(is, handler);
            }
        }
        catch (SAXException e)
        {
            throw new FaceletException("Error Parsing " + alias + ": " + e.getMessage(), e.getCause());
        }
        catch (ParserConfigurationException e)
        {
            throw new FaceletException("Error Configuring Parser " + alias + ": " + e.getMessage(), e.getCause());
        }
        finally
        {
            if (is != null)
            {
                is.close();
            }
        }
        return new EncodingHandler(mngr.createFaceletHandler(), encoding);
    }

    /**
     * @since 2.0.1
     */
    @Override
    protected FaceletHandler doCompileCompositeComponentMetadata(URL src, String alias)
            throws IOException, FaceletException, ELException, FacesException
    {
        CompilationManager mngr = null;
        InputStream is = null;
        String encoding = null;
        try
        {
            is = new BufferedInputStream(src.openStream(), 1024);
            mngr = new CompilationManager(alias, this, getFaceletsProcessingInstructions(src, alias));
            encoding = getXmlDecl(is, mngr);
            CompositeComponentMetadataHandler handler = new CompositeComponentMetadataHandler(mngr, alias);
            SAXParser parser = this.createSAXParser(handler);
            parser.parse(is, handler);
        }
        catch (SAXException e)
        {
            throw new FaceletException("Error Parsing " + alias + ": " + e.getMessage(), e.getCause());
        }
        catch (ParserConfigurationException e)
        {
            throw new FaceletException("Error Configuring Parser " + alias + ": " + e.getMessage(), e.getCause());
        }
        finally
        {
            if (is != null)
            {
                is.close();
            }
        }
        return new EncodingHandler(mngr.createFaceletHandler(), encoding);
    }
    
    @Override
    protected FaceletHandler doCompileComponent(
        String taglibURI, String tagName, Map<String, Object> attributes)
    {
        String alias = tagName;
        CompilationManager mngr = new CompilationManager(alias, this, getDefaultFaceletsProcessingInstructions());
        String prefix = "oamf"; // The prefix is only a logical name.
        mngr.pushNamespace(prefix, taglibURI);
        
        Location location = new Location(alias, 0, 0);
        int len = attributes.size();
        TagAttribute[] ta = new TagAttribute[len];
        int i = 0;
        for (Map.Entry<String, Object> entry : attributes.entrySet())
        {
            String stringValue = null;
            if (entry.getValue() instanceof ValueExpression)
            {
                stringValue = ((ValueExpression)entry.getValue()).getExpressionString();
            }
            else if (entry.getValue() instanceof MethodExpression)
            {
                stringValue = ((MethodExpression)entry.getValue()).getExpressionString();
            }
            else if (entry.getValue() != null)
            {
                stringValue = entry.getValue().toString();
            }
            ta[i] = new TagAttributeImpl(location, "", entry.getKey(), entry.getKey(), stringValue);
            i++;
        }        
        mngr.pushTag(new Tag(location, taglibURI, tagName, "oamf:"+tagName, new TagAttributesImpl(ta)));
        mngr.popTag();
        mngr.popNamespace(prefix);
        
        FaceletHandler handler = new DynamicComponentFacelet((NamespaceHandler) mngr.createFaceletHandler());
        return handler;
    }
    
    protected FaceletsProcessingInstructions getDefaultFaceletsProcessingInstructions()
    {
        return FaceletsProcessingInstructions.getProcessingInstructions(FaceletsProcessing.PROCESS_AS_XHTML, false);
    }
    
    protected FaceletsProcessingInstructions getFaceletsProcessingInstructions(URL src, String alias)
    {
        String processAs = null;
        boolean compressSpaces = false;
        for (FaceletsProcessing entry : getFaceletsProcessingConfigurations())
        {
            if (src.getPath().endsWith(entry.getFileExtension()))
            {
                processAs = entry.getProcessAs();
                compressSpaces = Boolean.valueOf(entry.getOamCompressSpaces());
                break;
            }
        }
        return FaceletsProcessingInstructions.getProcessingInstructions(processAs, compressSpaces);
    }

    protected static String writeXmlDecl(InputStream is, CompilationManager mngr) throws IOException
    {
        is.mark(128);
        String encoding = null;
        try
        {
            byte[] b = new byte[128];
            if (is.read(b) > 0)
            {
                String r = new String(b);
                Matcher m = XML_DECLARATION.matcher(r);
                if (m.find())
                {
                    if (!mngr.getFaceletsProcessingInstructions().isConsumeXmlDeclaration())
                    {
                        mngr.writeInstruction(m.group(0) + "\n");
                    }
                    if (m.group(3) != null)
                    {
                        encoding = m.group(3);
                    }
                }
            }
        }
        finally
        {
            is.reset();
        }
        return encoding;
    }
    
    protected static String getXmlDecl(InputStream is, CompilationManager mngr) throws IOException
    {
        is.mark(128);
        String encoding = null;
        try
        {
            byte[] b = new byte[128];
            if (is.read(b) > 0)
            {
                String r = new String(b);
                Matcher m = XML_DECLARATION.matcher(r);
                if (m.find() && m.group(3) != null)
                {
                    encoding = m.group(3);
                }
            }
        }
        finally
        {
            is.reset();
        }
        return encoding;
    }

    private SAXParser createSAXParser(DefaultHandler handler) throws SAXException,
            ParserConfigurationException
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
        factory.setFeature("http://xml.org/sax/features/validation", this.isValidating());
        factory.setValidating(this.isValidating());
        SAXParser parser = factory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
        reader.setErrorHandler(handler);
        reader.setEntityResolver(handler);
        return parser;
    }

}
