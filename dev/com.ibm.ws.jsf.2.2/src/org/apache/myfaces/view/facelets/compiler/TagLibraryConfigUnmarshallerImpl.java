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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import javax.faces.context.ExternalContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.myfaces.config.ConfigFilesXmlValidationUtils;
import org.apache.myfaces.config.element.facelets.FaceletTagLibrary;
import org.apache.myfaces.config.impl.digester.elements.facelets.FaceletBehaviorTagImpl;
import org.apache.myfaces.config.impl.digester.elements.facelets.FaceletComponentTagImpl;
import org.apache.myfaces.config.impl.digester.elements.facelets.FaceletConverterTagImpl;
import org.apache.myfaces.config.impl.digester.elements.facelets.FaceletFunctionImpl;
import org.apache.myfaces.config.impl.digester.elements.facelets.FaceletHandlerTagImpl;
import org.apache.myfaces.config.impl.digester.elements.facelets.FaceletSourceTagImpl;
import org.apache.myfaces.config.impl.digester.elements.facelets.FaceletTagImpl;
import org.apache.myfaces.config.impl.digester.elements.facelets.FaceletTagLibraryImpl;
import org.apache.myfaces.config.impl.digester.elements.facelets.FaceletValidatorTagImpl;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.view.facelets.util.ReflectionUtil;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class TagLibraryConfigUnmarshallerImpl
{
    
    public static FaceletTagLibrary create(ExternalContext externalContext, URL url) throws IOException
    {
        InputStream is = null;
        FaceletTagLibrary t = null;
        URLConnection conn = null;
        try
        {
            boolean schemaValidating = false;

            // validate XML
            if (MyfacesConfig.getCurrentInstance(externalContext).isValidateXML())
            {
                String version = ConfigFilesXmlValidationUtils.getFaceletTagLibVersion(url);
                schemaValidating = "2.0".equals(version);
                if (schemaValidating)
                {
                    ConfigFilesXmlValidationUtils.validateFaceletTagLibFile(url, externalContext, version);
                }
            }
            
            // parse file
            LibraryHandler handler = new LibraryHandler(url);
            SAXParser parser = createSAXParser(handler, externalContext, schemaValidating);
            conn = url.openConnection();
            conn.setUseCaches(false);
            is = conn.getInputStream();
            parser.parse(is, handler);
            t = handler.getLibrary();
        }
        catch (SAXException e)
        {
            IOException ioe = new IOException("Error parsing [" + url + "]: ");
            ioe.initCause(e);
            throw ioe;
        }
        catch (ParserConfigurationException e)
        {
            IOException ioe = new IOException("Error parsing [" + url + "]: ");
            ioe.initCause(e);
            throw ioe;
        }
        finally
        {
            if (is != null)
            {
                is.close();
            }
        }
        return t;
    }    
    
    private static final SAXParser createSAXParser(LibraryHandler handler, ExternalContext externalContext,
                                                   boolean schemaValidating)
            throws SAXException, ParserConfigurationException
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        if (MyfacesConfig.getCurrentInstance(externalContext).isValidateXML() && !schemaValidating)
        {
            // DTD validating
            factory.setNamespaceAware(false);
            factory.setFeature("http://xml.org/sax/features/validation", true);
            factory.setValidating(true);
        }
        else
        {
            //Just parse it and do not validate, because it is not necessary.
            factory.setNamespaceAware(true);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setValidating(false);
        }

        SAXParser parser = factory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setErrorHandler(handler);
        reader.setEntityResolver(handler);
        return parser;
    }

    private static class LibraryHandler extends DefaultHandler
    {
        private final URL source;
        
        private FaceletTagLibraryImpl library;

        private final StringBuffer buffer;

        private Locator locator;

        private String tagName;

        private String converterId;

        private String validatorId;
        
        private String behaviorId;

        private String componentType;

        private String rendererType;

        private String functionName;

        //private Class<? extends TagHandler> handlerClass;
        private String handlerClass;

        //private Class<?> functionClass;
        private String functionClass;

        private String functionSignature;
        
        private String resourceId;
       
        public LibraryHandler(URL source)
        {
            this.source = source;
            this.buffer = new StringBuffer(64);
        }

        public FaceletTagLibrary getLibrary()
        {
            return this.library;
        }
        
        private FaceletTagLibraryImpl getLibraryImpl()
        {
            if (this.library == null)
            {
                this.library = new FaceletTagLibraryImpl();
            }
            return this.library;
        }

        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            try
            {
                if ("facelet-taglib".equals(qName))
                {
                    // Nothing to do
                }                
                else if ("library-class".equals(qName))
                {
                    getLibraryImpl().setLibraryClass(this.captureBuffer());
                }
                else if ("short-name".equals(qName))
                {
                    getLibraryImpl().setShortName(this.captureBuffer());
                }
                else if ("namespace".equals(qName))
                {
                    getLibraryImpl().setNamespace(this.captureBuffer());
                }
                else if ("composite-library-name".equals(qName))
                {
                    getLibraryImpl().setCompositeLibraryName(this.captureBuffer());
                }
                else if ("component-type".equals(qName))
                {
                    this.componentType = this.captureBuffer();
                }
                else if ("renderer-type".equals(qName))
                {
                    this.rendererType = this.captureBufferEmptyNull();
                }
                else if ("tag-name".equals(qName))
                {
                    this.tagName = this.captureBuffer();
                }
                else if ("function-name".equals(qName))
                {
                    this.functionName = this.captureBuffer();
                }
                else if ("function-class".equals(qName))
                {
                    //String className = this.captureBuffer();
                    //this.functionClass = createClass(Object.class, className);
                    this.functionClass = this.captureBuffer();
                }
                else if ("description".equals(qName))
                {
                    //Not used
                }
                else if ("display-name".equals(qName))
                {
                    //Not used
                }
                else if ("icon".equals(qName))
                {
                    //Not used
                }                
                else if ("resource-id".equals(qName))
                {
                    this.resourceId = this.captureBuffer();
                }
                else
                {
                    // Make sure there we've seen a namespace element
                    // before trying any of the following elements to avoid
                    // obscure NPEs
                    if (this.library == null)
                    {
                        throw new IllegalStateException("No <namespace> element");
                    }
                    else if (this.library.getNamespace() == null)
                    {
                        throw new IllegalStateException("No <namespace> element");
                    }

                    //TagLibraryImpl impl = (TagLibraryImpl) this.library;

                    if ("tag".equals(qName))
                    {
                        if (this.handlerClass != null)
                        {
                            //impl.putTagHandler(this.tagName, this.handlerClass);
                            getLibraryImpl().addTag(
                                new FaceletTagImpl(this.tagName, 
                                    new FaceletHandlerTagImpl(this.handlerClass)) );
                            this.handlerClass = null;
                        }
                    }
                    else if ("handler-class".equals(qName))
                    {
                        //String cName = this.captureBuffer();
                        //this.handlerClass = createClass(TagHandler.class, cName);
                        this.handlerClass = this.captureBufferEmptyNull();
                    }
                    else if ("component".equals(qName))
                    {
                        if (this.handlerClass != null)
                        {
                            //impl.putComponent(this.tagName, this.componentType, this.rendererType, this.handlerClass);
                            getLibraryImpl().addTag(new FaceletTagImpl(this.tagName,
                                new FaceletComponentTagImpl(this.componentType, this.rendererType, 
                                    this.handlerClass, null)));
                            this.handlerClass = null;
                        }
                        else if (this.resourceId != null)
                        {
                            //impl.putComponentFromResourceId(this.tagName, this.resourceId);
                            getLibraryImpl().addTag(new FaceletTagImpl(this.tagName,
                                new FaceletComponentTagImpl(null, null, null, this.resourceId)));
                            this.resourceId = null;
                            this.handlerClass = null;
                        }
                        else
                        {
                            getLibraryImpl().addTag(new FaceletTagImpl(this.tagName,
                                new FaceletComponentTagImpl(this.componentType, this.rendererType, null, null)));
                            this.handlerClass = null;
                            //impl.putComponent(this.tagName, this.componentType, this.rendererType);
                        }
                    }
                    else if ("converter-id".equals(qName))
                    {
                        this.converterId = this.captureBuffer();
                    }
                    else if ("converter".equals(qName))
                    {
                        if (this.handlerClass != null)
                        {
                            //impl.putConverter(this.tagName, this.converterId, handlerClass);
                            getLibraryImpl().addTag(new FaceletTagImpl(this.tagName,
                                new FaceletConverterTagImpl(this.converterId, this.handlerClass)));
                            this.handlerClass = null;
                        }
                        else
                        {
                            //impl.putConverter(this.tagName, this.converterId);
                            getLibraryImpl().addTag(new FaceletTagImpl(this.tagName,
                                new FaceletConverterTagImpl(this.converterId)));
                        }
                        this.converterId = null;
                    }
                    else if ("validator-id".equals(qName))
                    {
                        this.validatorId = this.captureBuffer();
                    }
                    else if ("validator".equals(qName))
                    {
                        if (this.handlerClass != null)
                        {
                            //impl.putValidator(this.tagName, this.validatorId, handlerClass);
                            getLibraryImpl().addTag(new FaceletTagImpl(this.tagName,
                                new FaceletValidatorTagImpl(this.validatorId, this.handlerClass)));
                            this.handlerClass = null;
                        }
                        else
                        {
                            //impl.putValidator(this.tagName, this.validatorId);
                            getLibraryImpl().addTag(new FaceletTagImpl(this.tagName,
                                new FaceletValidatorTagImpl(this.validatorId)));
                        }
                        this.validatorId = null;
                    }
                    else if ("behavior-id".equals(qName))
                    {
                        this.behaviorId = this.captureBuffer();
                    }
                    else if ("behavior".equals(qName))
                    {
                        if (this.handlerClass != null)
                        {
                            //impl.putBehavior(this.tagName, this.behaviorId, handlerClass);
                            getLibraryImpl().addTag(new FaceletTagImpl(this.tagName,
                                new FaceletBehaviorTagImpl(this.behaviorId, this.handlerClass)));
                            this.handlerClass = null;
                        }
                        else
                        {
                            //impl.putBehavior(this.tagName, this.behaviorId);
                            getLibraryImpl().addTag(new FaceletTagImpl(this.tagName,
                                new FaceletBehaviorTagImpl(this.behaviorId)));
                        }
                        this.behaviorId = null;
                    }
                    else if ("source".equals(qName))
                    {
                        String path = this.captureBuffer();
                        URL url = new URL(this.source, path);
                        //impl.putUserTag(this.tagName, url);
                        getLibraryImpl().addTag(new FaceletTagImpl(this.tagName,
                            new FaceletSourceTagImpl(url.toString())));
                    }
                    else if ("function-signature".equals(qName))
                    {
                        this.functionSignature = this.captureBuffer();
                        getLibraryImpl().addFunction(
                            new FaceletFunctionImpl(this.functionName, this.functionClass, functionSignature));
                        //Method m = createMethod(this.functionClass, this.functionSignature);
                        //impl.putFunction(this.functionName, m);
                    }
                }
            }
            catch (Exception e)
            {
                throw new SAXParseException("Error Handling [" + this.source + "@" + this.locator.getLineNumber()
                        + "," + this.locator.getColumnNumber() + "] <" + qName + ">", locator, e);
            }
        }

        private String captureBuffer() throws Exception
        {
            String s = this.buffer.toString().trim();
            if (s.length() == 0)
            {
                throw new Exception("Value Cannot be Empty");
            }
            this.buffer.setLength(0);
            return s;
        }
        
        private String captureBufferEmptyNull() throws Exception
        {
            String s = this.buffer.toString().trim();
            if (s.length() == 0)
            {
                //if is "" just set null instead
                s = null;
            }
            this.buffer.setLength(0);
            return s;
        }      

        @SuppressWarnings("unchecked")
        private static <T> Class<? extends T> createClass(Class<T> type, String name) throws Exception
        {
            Class<? extends T> factory = (Class<? extends T>)ReflectionUtil.forName(name);
            if (!type.isAssignableFrom(factory))
            {
                throw new Exception(name + " must be an instance of " + type.getName());
            }
            return factory;
        }

        private static Method createMethod(Class<?> type, String s) throws Exception
        {
            int pos = s.indexOf(' ');
            if (pos == -1)
            {
                throw new Exception("Must Provide Return Type: " + s);
            }
            else
            {
                int pos2 = s.indexOf('(', pos + 1);
                if (pos2 == -1)
                {
                    throw new Exception("Must provide a method name, followed by '(': " + s);
                }
                else
                {
                    String mn = s.substring(pos + 1, pos2).trim();
                    pos = s.indexOf(')', pos2 + 1);
                    if (pos == -1)
                    {
                        throw new Exception("Must close parentheses, ')' missing: " + s);
                    }
                    else
                    {
                        String[] ps = s.substring(pos2 + 1, pos).trim().split(",");
                        Class<?>[] pc;
                        if (ps.length == 1 && "".equals(ps[0]))
                        {
                            pc = new Class[0];
                        }
                        else
                        {
                            pc = new Class[ps.length];
                            for (int i = 0; i < pc.length; i++)
                            {
                                pc[i] = ReflectionUtil.forName(ps[i].trim());
                            }
                        }
                        try
                        {
                            return type.getMethod(mn, pc);
                        }
                        catch (NoSuchMethodException e)
                        {
                            throw new Exception("No Function Found on type: " + type.getName() + " with signature: "
                                    + s);
                        }

                    }

                }
            }
        }

        public InputSource resolveEntity(String publicId, String systemId) throws SAXException
        {
            if ("-//Sun Microsystems, Inc.//DTD Facelet Taglib 1.0//EN".equals(publicId))
            {
                URL url = ClassUtils.getResource("org/apache/myfaces/resource/facelet-taglib_1_0.dtd");
                return new InputSource(url.toExternalForm());
            }
            return null;
        }

        public void characters(char[] ch, int start, int length) throws SAXException
        {
            this.buffer.append(ch, start, length);
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            this.buffer.setLength(0);
            if ("tag".equals(qName))
            {
                this.handlerClass = null;
                this.componentType = null;
                this.rendererType = null;
                this.tagName = null;
            }
            else if ("function".equals(qName))
            {
                this.functionName = null;
                this.functionClass = null;
                this.functionSignature = null;
            }
        }

        public void error(SAXParseException e) throws SAXException
        {
            throw new SAXException(
                    "Error Handling [" + this.source + "@" + e.getLineNumber() + "," + e.getColumnNumber() + "]", e);
        }

        public void setDocumentLocator(Locator locator)
        {
            this.locator = locator;
        }

        public void fatalError(SAXParseException e) throws SAXException
        {
            throw e;
        }

        public void warning(SAXParseException e) throws SAXException
        {
            throw e;
        }
    }

}
