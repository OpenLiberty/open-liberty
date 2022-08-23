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
package org.apache.myfaces.util;

import org.apache.myfaces.util.lang.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.faces.context.ExternalContext;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.myfaces.util.lang.ClassUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class WebXmlParser
{
    private static final Logger LOGGER = Logger.getLogger(WebXmlParser.class.getName());

    private static final String ERROR_PAGE_EXCEPTION_TYPE_EXPRESSION =
            "*[local-name() = 'error-page']/*[local-name() = 'exception-type']";
    private static final String LOCATION_EXPRESSION =
            "*[local-name() = 'location']";
    private static final String ERROR_CODE_500_LOCATION_EXPRESSION =
            "*[local-name() = 'error-page'][*[local-name() = 'error-code'] = '500'] / *[local-name() = 'location']";
    private static final String ERROR_PAGE_NO_CODE_AND_TYPE_EXPRESSION =
            "*[local-name() = 'error-page'][not(*[local-name() = 'error-code']) and not"
            + "(*[local-name() = 'exception-type'])]/*[local-name() = 'location']";

    private static final String KEY_ERROR_PAGES = WebXmlParser.class.getName() + ".errorpages";
    
    private WebXmlParser()
    {
    }

    /**
     * Parses the web.xml and web-fragements.xml for error pages.
     * "null" as key represents the default error page. Otherwise the key is the exception class.
     * 
     * @param context
     * @return 
     */
    public static Map<String, String> getErrorPages(ExternalContext context)
    {
        // it would be nicer if the cache would probably directly in DefaultWebConfigProvider
        // as its currently the only caller of the method
        // however it's recreated every request, we have to refactor the SPI thing a bit probably.
        Map<String, String> cached = (Map<String, String>) context.getApplicationMap().get(KEY_ERROR_PAGES);
        if (cached != null)
        {
            return cached;
        }
        
        Map<String, String> webXmlErrorPages = getWebXmlErrorPages(context);
        Map<String, String> webFragmentXmlsErrorPages = getWebFragmentXmlsErrorPages(context);

        Map<String, String> errorPages = webXmlErrorPages;
        if (errorPages == null)
        {
            errorPages = webFragmentXmlsErrorPages;
        }
        else if (webFragmentXmlsErrorPages != null)
        {
            for (Map.Entry<String, String> entry : webFragmentXmlsErrorPages.entrySet())
            {
                if (!errorPages.containsKey(entry.getKey()))
                {
                    errorPages.put(entry.getKey(), entry.getValue());
                }
            }
        }

        context.getApplicationMap().put(KEY_ERROR_PAGES, errorPages);
        
        return errorPages;
    }

    private static Map<String, String> getWebXmlErrorPages(ExternalContext context)
    {
        try
        {
            Document webXml = toDocument(context.getResource("/WEB-INF/web.xml"));
            
            if (webXml == null)
            {
                // Quarkus
                webXml = toDocument(ClassUtils.getCurrentLoader(WebXmlParser.class).getResource("META-INF/web.xml"));
            }
            
            if (webXml != null)
            {
                return parseErrorPages(webXml.getDocumentElement());
            }
        }
        catch (Throwable e)
        {
            LOGGER.log(Level.SEVERE, "Could not load or parse web.xml", e);
        }

        return null;
    }

    private static Map<String, String> getWebFragmentXmlsErrorPages(ExternalContext context)
    {
        Map<String, String> webFragmentXmlsErrorPages = null;

        try
        {
            Enumeration<URL> webFragments = ClassUtils.getContextClassLoader()
                    .getResources("META-INF/web-fragment.xml");
            while (webFragments.hasMoreElements())
            {
                try
                {
                    URL url = webFragments.nextElement();
                    Document webFragmentXml = toDocument(url);
                    if (webFragmentXml != null)
                    {
                        if (webFragmentXmlsErrorPages == null)
                        {
                            webFragmentXmlsErrorPages = parseErrorPages(webFragmentXml.getDocumentElement());
                        }
                        else
                        {
                            Map<String, String> temp = parseErrorPages(webFragmentXml.getDocumentElement());
                            for (Map.Entry<String, String> entry : temp.entrySet())
                            {
                                if (!webFragmentXmlsErrorPages.containsKey(entry.getKey()))
                                {
                                    webFragmentXmlsErrorPages.put(entry.getKey(), entry.getValue());
                                }
                            }
                        }
                    }
                }
                catch (Throwable e)
                {
                    LOGGER.log(Level.SEVERE, "Could not load or parse web-fragment.xml", e);
                }
            }
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, "Could not get web-fragment.xml from ClassLoader", e);
        }

        return webFragmentXmlsErrorPages;
    }

    private static Document toDocument(URL url) throws Exception
    {
        InputStream is = null;

        try
        {
            // web.xml is optional
            if (url == null)
            {
                return null;
            }

            is = url.openStream();

            if (is == null)
            {
                return null;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setIgnoringComments(true);

            try
            {
                factory.setFeature("http://xml.org/sax/features/namespaces", false);
                factory.setFeature("http://xml.org/sax/features/validation", false);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            }
            catch (Throwable e)
            {
                LOGGER.warning("DocumentBuilderFactory#setFeature not implemented. Skipping...");
            }

            boolean absolute = false;
            try
            {
                absolute = url.toURI().isAbsolute();
            }
            catch (URISyntaxException e)
            {
                // noop
            }

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document;

            if (absolute)
            {
                InputSource source = new InputSource(url.toExternalForm());
                source.setByteStream(is);
                document = builder.parse(source);
            }
            else
            {
                document = builder.parse(is);
            }

            return document;
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    // ignore
                }
            }
        }
    }

    private static Map<String, String> parseErrorPages(Element webXml) throws Exception
    {
        Map<String, String> errorPages = new HashMap<>();

        XPath xpath = XPathFactory.newInstance().newXPath();

        NodeList exceptionTypes = (NodeList) xpath.compile(ERROR_PAGE_EXCEPTION_TYPE_EXPRESSION)
                .evaluate(webXml, XPathConstants.NODESET);

        for (int i = 0; i < exceptionTypes.getLength(); i++)
        {
            Node node = exceptionTypes.item(i);

            String exceptionType = node.getTextContent().trim();
            String key = Throwable.class.getName().equals(exceptionType) ? null : exceptionType;

            String location = xpath.compile(LOCATION_EXPRESSION).evaluate(node.getParentNode()).trim();

            if (!errorPages.containsKey(key))
            {
                errorPages.put(key, location);
            }
        }

        if (!errorPages.containsKey(null))
        {
            String defaultLocation = xpath.compile(ERROR_CODE_500_LOCATION_EXPRESSION).evaluate(webXml).trim();

            if (StringUtils.isBlank(defaultLocation))
            {
                defaultLocation = xpath.compile(ERROR_PAGE_NO_CODE_AND_TYPE_EXPRESSION).evaluate(webXml).trim();
            }

            if (!StringUtils.isBlank(defaultLocation))
            {
                errorPages.put(null, defaultLocation);
            }
        }

        return errorPages;
    }
    
}
