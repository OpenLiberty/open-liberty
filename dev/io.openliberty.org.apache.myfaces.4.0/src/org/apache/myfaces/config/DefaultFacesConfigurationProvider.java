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
package org.apache.myfaces.config;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import org.apache.myfaces.config.annotation.AnnotationConfigurator;
import org.apache.myfaces.config.element.FacesConfig;
import org.apache.myfaces.config.impl.FacesConfigUnmarshallerImpl;
import org.apache.myfaces.util.lang.ClassUtils;
import org.apache.myfaces.spi.FacesConfigResourceProvider;
import org.apache.myfaces.spi.FacesConfigResourceProviderFactory;
import org.apache.myfaces.spi.FacesConfigurationProvider;
import org.apache.myfaces.spi.ServiceProviderFinderFactory;
import org.xml.sax.SAXException;

import jakarta.faces.FacesException;
import jakarta.faces.FactoryFinder;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.webapp.FacesServlet;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.faces.application.ApplicationConfigurationPopulator;
import jakarta.faces.application.ViewHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.myfaces.config.element.FacesFlowDefinition;
import org.apache.myfaces.config.element.facelets.FaceletTagLibrary;
import org.apache.myfaces.config.impl.element.FacesConfigImpl;
import org.apache.myfaces.config.impl.element.FacesFlowDefinitionImpl;
import org.apache.myfaces.config.impl.element.FacesFlowReturnImpl;
import org.apache.myfaces.config.impl.element.NavigationCaseImpl;
import org.apache.myfaces.util.lang.FastWriter;
import org.apache.myfaces.util.WebConfigParamUtils;
import org.apache.myfaces.spi.FaceletConfigResourceProvider;
import org.apache.myfaces.spi.FaceletConfigResourceProviderFactory;
import org.apache.myfaces.spi.ServiceProviderFinder;
import org.apache.myfaces.view.facelets.compiler.TagLibraryConfigUnmarshallerImpl;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * 
 * @author Leonardo Uribe
 * @since 2.0.3
 */
public class DefaultFacesConfigurationProvider extends FacesConfigurationProvider
{

    private static final String STANDARD_FACES_CONFIG_RESOURCE = "META-INF/standard-faces-config.xml";

    private static final String DEFAULT_FACES_CONFIG = "/WEB-INF/faces-config.xml";

    private static final Set<String> FACTORY_NAMES = new HashSet<String>();
    static
    {
        FACTORY_NAMES.add(FactoryFinder.APPLICATION_FACTORY);
        FACTORY_NAMES.add(FactoryFinder.CLIENT_WINDOW_FACTORY);
        FACTORY_NAMES.add(FactoryFinder.EXCEPTION_HANDLER_FACTORY);
        FACTORY_NAMES.add(FactoryFinder.EXTERNAL_CONTEXT_FACTORY);
        FACTORY_NAMES.add(FactoryFinder.FACELET_CACHE_FACTORY);
        FACTORY_NAMES.add(FactoryFinder.FACES_CONTEXT_FACTORY);
        FACTORY_NAMES.add(FactoryFinder.FLASH_FACTORY);
        FACTORY_NAMES.add(FactoryFinder.FLOW_HANDLER_FACTORY);
        FACTORY_NAMES.add(FactoryFinder.LIFECYCLE_FACTORY);
        FACTORY_NAMES.add(FactoryFinder.RENDER_KIT_FACTORY);
        FACTORY_NAMES.add(FactoryFinder.TAG_HANDLER_DELEGATE_FACTORY);
        FACTORY_NAMES.add(FactoryFinder.PARTIAL_VIEW_CONTEXT_FACTORY);
        FACTORY_NAMES.add(FactoryFinder.VISIT_CONTEXT_FACTORY);
        FACTORY_NAMES.add(FactoryFinder.VIEW_DECLARATION_LANGUAGE_FACTORY);
        FACTORY_NAMES.add(FactoryFinder.SEARCH_EXPRESSION_CONTEXT_FACTORY);
    }

    private static final Logger log = Logger.getLogger(DefaultFacesConfigurationProvider.class.getName());

    private FacesConfigUnmarshaller<? extends FacesConfig> _unmarshaller;
    
    private AnnotationConfigurator _annotationConfigurator;

    protected void setUnmarshaller(ExternalContext ectx, FacesConfigUnmarshaller<? extends FacesConfig> unmarshaller)
    {
        _unmarshaller = unmarshaller;
    }

    @SuppressWarnings("unchecked")
    protected FacesConfigUnmarshaller<? extends FacesConfig> getUnmarshaller(ExternalContext ectx)
    {
        if (_unmarshaller == null)
        {
            _unmarshaller = new FacesConfigUnmarshallerImpl(ectx);
        }
        return _unmarshaller;
    }
    
    protected void setAnnotationConfigurator(AnnotationConfigurator configurator)
    {
        _annotationConfigurator = configurator;
    }
    
    protected AnnotationConfigurator getAnnotationConfigurator()
    {
        if (_annotationConfigurator == null)
        {
            _annotationConfigurator = new AnnotationConfigurator();
        }
        return _annotationConfigurator;
    }

    @Override
    public FacesConfig getStandardFacesConfig(ExternalContext ectx)
    {        
        try
        {
            if (MyfacesConfig.getCurrentInstance(ectx).isValidateXML())
            {
                URL url = ClassUtils.getResource(STANDARD_FACES_CONFIG_RESOURCE);
                if (url != null)
                {
                    validateFacesConfig(ectx, url);
                }
            }
            InputStream stream = ClassUtils.getResourceAsStream(STANDARD_FACES_CONFIG_RESOURCE);
            if (stream == null)
            {
                throw new FacesException("Standard faces config " + STANDARD_FACES_CONFIG_RESOURCE + " not found");
            }
            if (log.isLoggable(Level.FINE))
            {
                log.fine("Reading standard config " + STANDARD_FACES_CONFIG_RESOURCE);
            }
            
            FacesConfig facesConfig = getUnmarshaller(ectx).getFacesConfig(stream, STANDARD_FACES_CONFIG_RESOURCE);
            stream.close();
            return facesConfig;
        }
        catch (IOException | SAXException e)
        {
            throw new FacesException(e);
        }
    }

    @Override
    public FacesConfig getAnnotationsFacesConfig(ExternalContext ectx, boolean metadataComplete)
    {
        return getAnnotationConfigurator().createFacesConfig(ectx, metadataComplete);
    }

    /**
     * This method performs part of the factory search outlined in section 10.2.6.1.
     */
    @Override
    public FacesConfig getMetaInfServicesFacesConfig(ExternalContext ectx)
    {
        try
        {
            org.apache.myfaces.config.impl.element.FacesConfigImpl facesConfig
                    = new org.apache.myfaces.config.impl.element.FacesConfigImpl();
            org.apache.myfaces.config.impl.element.FactoryImpl factory
                    = new org.apache.myfaces.config.impl.element.FactoryImpl();
            
            facesConfig.addFactory(factory);
            
            for (String factoryName : FACTORY_NAMES)
            {
                List<String> classList = ServiceProviderFinderFactory.getServiceProviderFinder(ectx)
                        .getServiceProviderList(factoryName);
                
                for (String className : classList)
                {
                    if (log.isLoggable(Level.INFO))
                    {
                        log.info("Found " + factoryName + " factory implementation: " + className);
                    }

                    switch (factoryName)
                    {
                        case FactoryFinder.APPLICATION_FACTORY:
                            factory.addApplicationFactory(className);
                            break;
                        case FactoryFinder.EXCEPTION_HANDLER_FACTORY:
                            factory.addExceptionHandlerFactory(className);
                            break;
                        case FactoryFinder.EXTERNAL_CONTEXT_FACTORY:
                            factory.addExternalContextFactory(className);
                            break;
                        case FactoryFinder.FACES_CONTEXT_FACTORY:
                            factory.addFacesContextFactory(className);
                            break;
                        case FactoryFinder.LIFECYCLE_FACTORY:
                            factory.addLifecycleFactory(className);
                            break;
                        case FactoryFinder.RENDER_KIT_FACTORY:
                            factory.addRenderkitFactory(className);
                            break;
                        case FactoryFinder.TAG_HANDLER_DELEGATE_FACTORY:
                            factory.addTagHandlerDelegateFactory(className);
                            break;
                        case FactoryFinder.PARTIAL_VIEW_CONTEXT_FACTORY:
                            factory.addPartialViewContextFactory(className);
                            break;
                        case FactoryFinder.VISIT_CONTEXT_FACTORY:
                            factory.addVisitContextFactory(className);
                            break;
                        case FactoryFinder.VIEW_DECLARATION_LANGUAGE_FACTORY:
                            factory.addViewDeclarationLanguageFactory(className);
                            break;
                        case FactoryFinder.FLASH_FACTORY:
                            factory.addFlashFactory(className);
                            break;
                        case FactoryFinder.FLOW_HANDLER_FACTORY:
                            factory.addFlowHandlerFactory(className);
                            break;
                        case FactoryFinder.CLIENT_WINDOW_FACTORY:
                            factory.addClientWindowFactory(className);
                            break;
                        case FactoryFinder.FACELET_CACHE_FACTORY:
                            factory.addFaceletCacheFactory(className);
                            break;
                        case FactoryFinder.SEARCH_EXPRESSION_CONTEXT_FACTORY:
                            factory.addSearchExpressionContextFactory(className);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected factory name " + factoryName);
                    }
                }
            }
            return facesConfig;
        }
        catch (Throwable e)
        {
            throw new FacesException(e);
        }
    }

    /**
     * This method fixes MYFACES-208
     */
    @Override
    public List<FacesConfig> getClassloaderFacesConfig(ExternalContext ectx)
    {
        List<FacesConfig> appConfigResources = new ArrayList<>();
        try
        {
            FacesConfigResourceProvider provider = FacesConfigResourceProviderFactory.
                getFacesConfigResourceProviderFactory(ectx).createFacesConfigResourceProvider(ectx);
            
            Collection<URL> facesConfigs = provider.getMetaInfConfigurationResources(ectx);
            
            for (URL url : facesConfigs)
            {
                if (MyfacesConfig.getCurrentInstance(ectx).isValidateXML())
                {
                    validateFacesConfig(ectx, url);
                }
                
                try (InputStream stream = openStreamWithoutCache(url))
                {
                    if (log.isLoggable(Level.FINE))
                    {
                        log.fine("Reading config: " + url.toExternalForm());
                    }
                    appConfigResources.add(getUnmarshaller(ectx).getFacesConfig(stream, url.toExternalForm()));
                }
            }
        }
        catch (Throwable e)
        {
            throw new FacesException(e);
        }
        return appConfigResources;
    }

    @Override
    public List<FacesConfig> getContextSpecifiedFacesConfig(ExternalContext ectx)
    {
        List<FacesConfig> appConfigResources = new ArrayList<>();
        try
        {
            for (String systemId : getConfigFilesList(ectx))
            {
                if (MyfacesConfig.getCurrentInstance(ectx).isValidateXML())
                {
                    URL url = ectx.getResource(systemId);
                    if (url != null)
                    {
                        validateFacesConfig(ectx, url);
                    }
                }

                try (InputStream stream = ectx.getResourceAsStream(systemId))
                {
                    if (stream == null)
                    {
                        log.severe("Faces config resource " + systemId + " not found");
                        continue;
                    }

                    if (log.isLoggable(Level.INFO))
                    {
                        log.info("Reading config " + systemId);
                    }
                    appConfigResources.add(getUnmarshaller(ectx).getFacesConfig(stream, systemId));
                }
            }
        }
        catch (Throwable e)
        {
            throw new FacesException(e);
        }
        return appConfigResources;
    }
    
    @Override
    public FacesConfig getWebAppFacesConfig(ExternalContext ectx)
    {
        try
        {
            // skip parsing/validating a empty faces-config.xml to avoid warn/error logs
            try (InputStream stream = ectx.getResourceAsStream(DEFAULT_FACES_CONFIG))
            {
                if (stream != null)
                {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = stream.read(buffer)) != -1)
                    {
                        out.write(buffer, 0, length);
                    }
                    out.flush();

                    String content = new String(out.toByteArray());
                    if (content.trim().isEmpty())
                    {
                        return new FacesConfigImpl();
                    }
                }
            }

            // web application config
            if (MyfacesConfig.getCurrentInstance(ectx).isValidateXML())
            {
                URL url = ectx.getResource(DEFAULT_FACES_CONFIG);
                if (url != null)
                {
                    validateFacesConfig(ectx, url);
                }
            }

            try (InputStream stream = ectx.getResourceAsStream(DEFAULT_FACES_CONFIG))
            {
                if (stream != null)
                {
                    if (log.isLoggable(Level.INFO))
                    {
                        log.info("Reading config /WEB-INF/faces-config.xml");
                    }
                    return getUnmarshaller(ectx).getFacesConfig(stream, DEFAULT_FACES_CONFIG);
                }
            }

            return null;
        }
        catch (IOException | SAXException e)
        {
            throw new FacesException(e);
        }

    }

    private InputStream openStreamWithoutCache(URL url) throws IOException
    {
        URLConnection connection = url.openConnection();
        connection.setUseCaches(false);
        return connection.getInputStream();
    }

    private List<String> getConfigFilesList(ExternalContext ectx)
    {
        String configFiles = ectx.getInitParameter(FacesServlet.CONFIG_FILES_ATTR);
        List<String> configFilesList = new ArrayList<>();
        if (configFiles != null)
        {
            StringTokenizer st = new StringTokenizer(configFiles, ",", false);
            while (st.hasMoreTokens())
            {
                String systemId = st.nextToken().trim();

                if (DEFAULT_FACES_CONFIG.equals(systemId))
                {
                    if (log.isLoggable(Level.WARNING))
                    {
                        log.warning(DEFAULT_FACES_CONFIG + " has been specified in the "
                                + FacesServlet.CONFIG_FILES_ATTR
                                + " context parameter of "
                                + "the deployment descriptor. This will automatically be removed, "
                                + "if we wouldn't do this, it would be loaded twice.  See JSF spec 1.1, 10.3.2");
                    }
                }
                else
                {
                    configFilesList.add(systemId);
                }
            }
        }
        return configFilesList;
    }
    
    private void validateFacesConfig(ExternalContext ectx, URL url) throws IOException, SAXException
    {
        String version = ConfigFilesXmlValidationUtils.getFacesConfigVersion(url);
        if ("1.2".equals(version) || "2.0".equals(version) || "2.1".equals(version) 
            || "2.2".equals(version) || "2.3".equals(version))
        {
            ConfigFilesXmlValidationUtils.validateFacesConfigFile(url, ectx, version);
        }
    }

    @Override
    public List<FacesConfig> getApplicationConfigurationResourceDocumentPopulatorFacesConfig(ExternalContext ectx)
    {
        ServiceProviderFinder spff = ServiceProviderFinderFactory.getServiceProviderFinder(ectx);
        ServiceLoader<ApplicationConfigurationPopulator> instances = 
            spff.load(ApplicationConfigurationPopulator.class);
        if (instances != null)
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // namespace aware
            factory.setNamespaceAware(true);
            // no validation
            factory.setValidating(false);
            try
            {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                factory.setXIncludeAware(false);
                factory.setExpandEntityReferences(false);
            }
            catch (Throwable e)
            {
                log.log(Level.WARNING, "DocumentBuilderFactory#setFeature not implemented. Skipping...", e);
            }
            DocumentBuilder builder = null;
            DOMImplementation domImpl = null;
            try
            {
                builder = factory.newDocumentBuilder();
                domImpl = builder.getDOMImplementation();
            }
            catch (ParserConfigurationException ex)
            {
                log.log(Level.SEVERE, "Cannot create dom document builder, skipping it", ex);
            }
            
            if (builder != null)
            {
                List<FacesConfig> facesConfigList = new ArrayList<>();
                List<Document> documentList = new ArrayList<>();
                for (ApplicationConfigurationPopulator populator : instances)
                {
                    // Spec says "... For each implementation, create a fresh org.w3c.dom.Document 
                    // instance, configured to be in the XML namespace of the
                    // application configuration resource format. ..."
                    Document document = domImpl.createDocument(
                        "http://java.sun.com/xml/ns/javaee", "faces-config", null);
                    //Document document = builder.newDocument();
                    populator.populateApplicationConfiguration(document);
                    documentList.add(document);
                }
                
                // Parse document. This strategy construct the faces-config.xml in a
                // memory buffer and then loads it.
                Transformer trans = null;
                try
                {
                    trans = TransformerFactory.newInstance().newTransformer();
                    trans.setOutputProperty(OutputKeys.INDENT, "no");
                    trans.setOutputProperty(OutputKeys.METHOD, "xml");
                    trans.setOutputProperty(OutputKeys.VERSION, "1.0");
                    trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                }
                catch (TransformerConfigurationException ex)
                {
                    Logger.getLogger(DefaultFacesConfigurationProvider.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (trans != null)
                {
                    FastWriter xmlAsWriter = new FastWriter();
                    for (int i = 0; i < documentList.size(); i++)
                    {
                        Document document = documentList.get(i);
                        xmlAsWriter.reset();
                        try
                        {
                            DOMSource source = new DOMSource(document);
                            StreamResult result = new StreamResult(xmlAsWriter);

                            trans.transform(source, result);

                            FacesConfig facesConfig = getUnmarshaller(ectx).getFacesConfig(xmlAsWriter.toString());
                            facesConfigList.add(facesConfig);
                        }
                        catch (IOException | SAXException | TransformerException ex)
                        {
                            log.log(Level.SEVERE, "Error while reading faces-config from populator", ex);
                        }
                    }
                    return facesConfigList;
                }
                else
                {
                    log.log(Level.SEVERE, "Cannot create xml transformer, skipping it");
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    public List<FacesConfig> getFacesFlowFacesConfig(ExternalContext ectx)
    {
        Set<String> directoryPaths = ectx.getResourcePaths("/");
        if (directoryPaths == null)
        {
            return Collections.emptyList();
        }
        
        List<FacesConfig> configFilesList = new ArrayList<>();
        List<String> contextSpecifiedList = getConfigFilesList(ectx);
        
        for (String dirPath : directoryPaths)
        {
            if (dirPath.equals("/WEB-INF/"))
            {
                // Look on /WEB-INF/<flow-Name>/<flowName>-flow.xml
                Set<String> webDirectoryPaths = ectx.getResourcePaths(dirPath);
                for (String webDirPath : webDirectoryPaths)
                {
                    if (webDirPath.endsWith("/") && 
                        !webDirPath.equals("/WEB-INF/classes/"))
                    {
                        String flowName = webDirPath.substring(9, webDirPath.length() - 1);
                        String filePath = webDirPath + flowName + "-flow.xml";
                        if (!contextSpecifiedList.contains(filePath))
                        {
                            try
                            {
                                URL url = ectx.getResource(filePath);
                                if (url != null)
                                {
                                    FacesConfig fc = parseFacesConfig(ectx, filePath, url);
                                    if (fc != null)
                                    {
                                        configFilesList.add(fc);
                                    }
                                }
                            }
                            catch (MalformedURLException ex)
                            {
                            }
                        }
                    }
                }
            }
            else if (!dirPath.startsWith("/META-INF") && dirPath.endsWith("/"))
            {
                // Look on /<flowName>/<flowName>-flow.xml
                String flowName = dirPath.substring(1, dirPath.length() - 1);
                String filePath = dirPath + flowName + "-flow.xml";
                if (!contextSpecifiedList.contains(filePath))
                {
                    try
                    {
                        URL url = ectx.getResource(filePath);
                        if (url != null)
                        {
                            FacesConfig fc = parseFacesConfig(ectx, filePath, url);
                            if (fc != null)
                            {
                                configFilesList.add(fc);
                            }
                        }
                    }
                    catch (MalformedURLException ex)
                    {
                    }
                }
            }
        }
        
        return configFilesList;
    }
    
    private FacesConfig parseFacesConfig(ExternalContext ectx, String systemId, URL url)
    {
        try
        {
            if (MyfacesConfig.getCurrentInstance(ectx).isValidateXML())
            {
                validateFacesConfig(ectx, url);
            }            
        }
        catch (IOException | SAXException e)
        {
            throw new FacesException(e);
        }

        InputStream stream = ectx.getResourceAsStream(systemId);
        PushbackInputStream pbstream = new PushbackInputStream(stream, 10);
        try
        {
            if (stream == null)
            {
                log.severe("Faces config resource " + systemId + " not found");
                return null;
            }
            String flowName = systemId.substring(systemId.lastIndexOf('/')+1, systemId.lastIndexOf("-flow.xml"));
            int c = pbstream.read();
            if (c != -1)
            {
                pbstream.unread(c);
            }
            else
            {
                // Zero lenght, if that so the flow definition must be implicitly derived. 
                // See JSF 2.2 section 11.4.3.3
                // 
                FacesConfigImpl facesConfig = new FacesConfigImpl();
                FacesFlowDefinitionImpl flow = new FacesFlowDefinitionImpl();
                flow.setId(flowName);
                // In this case the defining document id is implicit associated
                flow.setDefiningDocumentId(systemId);
                
                String startNodePath = systemId.substring(0, systemId.lastIndexOf('/')+1)+flowName+".xhtml";
                flow.setStartNode(startNodePath);
                
                // There is a default return node with name [flow-name]-return and 
                // that by default points to an outer /[flow-name]-return outcome
                FacesFlowReturnImpl returnNode = new FacesFlowReturnImpl();
                returnNode.setId(flowName+"-return");
                NavigationCaseImpl returnNavCase = new NavigationCaseImpl();
                returnNavCase.setFromOutcome('/' +flowName+"-return");
                returnNode.setNavigationCase(returnNavCase);
                flow.addReturn(returnNode);
                
                facesConfig.addFacesFlowDefinition(flow);
                return facesConfig;
            }

            if (log.isLoggable(Level.INFO))
            {
                log.info("Reading config " + systemId);
            }
            
            FacesConfigImpl facesConfig = (FacesConfigImpl) getUnmarshaller(ectx).getFacesConfig(pbstream, systemId);
            
            // Set default start node when it is not present.
            for (FacesFlowDefinition definition : facesConfig.getFacesFlowDefinitions())
            {
                if (flowName.equals(definition.getId()))
                {
                    FacesFlowDefinitionImpl flow = (FacesFlowDefinitionImpl) definition;
                    if (flow.getStartNode() == null)
                    {
                        String startNodePath = systemId.substring(0, 
                            systemId.lastIndexOf('/')+1)+flowName+".xhtml";
                        flow.setStartNode(startNodePath);
                    }
                }
            }
            return facesConfig;
        }
        catch (IOException | SAXException e)
        {
            throw new FacesException(e);
        }
        finally
        {
            if (stream != null)
            {
                try
                {
                    stream.close();
                }
                catch (IOException ex)
                {
                    // No op
                }
            }
        }
    }

    @Override
    public List<FacesConfig> getFaceletTaglibFacesConfig(ExternalContext externalContext)
    {
        List<FacesConfig> facesConfigFilesList = new ArrayList<>();
        
        String param = WebConfigParamUtils.getStringInitParameter(externalContext,
                ViewHandler.FACELETS_LIBRARIES_PARAM_NAME);
        if (param != null)
        {
            for (String library : param.split(";"))
            {
                try
                {
                    URL src = externalContext.getResource(library.trim());
                    if (src == null)
                    {
                        throw new FileNotFoundException(library);
                    }
                    
                    FaceletTagLibrary tl = TagLibraryConfigUnmarshallerImpl.create(externalContext, src);
                    if (tl != null)
                    {
                        org.apache.myfaces.config.impl.element.FacesConfigImpl config = 
                            new org.apache.myfaces.config.impl.element.FacesConfigImpl();
                        config.addFaceletTagLibrary(tl);
                        facesConfigFilesList.add(config);
                    }
                    if (log.isLoggable(Level.FINE))
                    {
                        log.fine("Successfully loaded library: " + library);
                    }
                }
                catch (IOException e)
                {
                    log.log(Level.SEVERE, "Error Loading library: " + library, e);
                }
            }
        }
        
        try
        {
            FaceletConfigResourceProvider provider = FaceletConfigResourceProviderFactory.
                getFacesConfigResourceProviderFactory(externalContext).
                    createFaceletConfigResourceProvider(externalContext);
            Collection<URL> urls = provider.getFaceletTagLibConfigurationResources(externalContext);
            for (URL url : urls)
            {
                try
                {
                    FaceletTagLibrary tl = TagLibraryConfigUnmarshallerImpl.create(externalContext, url);
                    if (tl != null)
                    {
                        org.apache.myfaces.config.impl.element.FacesConfigImpl config = 
                            new org.apache.myfaces.config.impl.element.FacesConfigImpl();
                        config.addFaceletTagLibrary(tl);
                        facesConfigFilesList.add(config);
                    }
                    if (log.isLoggable(Level.FINE))
                    {
                        log.fine("Added Library from: " + url);
                    }
                }
                catch (Exception e)
                {
                    log.log(Level.SEVERE, "Error Loading Library: " + url, e);
                }
            }
        }
        catch (IOException e)
        {
            log.log(Level.SEVERE, "Compiler Initialization Error", e);
        }
        return facesConfigFilesList;
    }
}