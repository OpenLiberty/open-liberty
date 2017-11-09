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

import java.io.FileNotFoundException;
import org.apache.myfaces.config.annotation.AnnotationConfigurator;
import org.apache.myfaces.config.element.FacesConfig;
import org.apache.myfaces.config.impl.digester.DigesterFacesConfigUnmarshallerImpl;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.spi.FacesConfigResourceProvider;
import org.apache.myfaces.spi.FacesConfigResourceProviderFactory;
import org.apache.myfaces.spi.FacesConfigurationProvider;
import org.apache.myfaces.spi.ServiceProviderFinderFactory;
import org.xml.sax.SAXException;

import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.context.ExternalContext;
import javax.faces.webapp.FacesServlet;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.StringReader;
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
import javax.faces.application.ApplicationConfigurationPopulator;
import javax.faces.application.ViewHandler;
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
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.config.element.FacesFlowDefinition;
import org.apache.myfaces.config.element.facelets.FaceletTagLibrary;
import org.apache.myfaces.config.impl.digester.elements.FacesConfigImpl;
import org.apache.myfaces.config.impl.digester.elements.FacesFlowDefinitionImpl;
import org.apache.myfaces.config.impl.digester.elements.FacesFlowReturnImpl;
import org.apache.myfaces.config.impl.digester.elements.NavigationCaseImpl;
import org.apache.myfaces.shared.util.FastWriter;
import org.apache.myfaces.shared.util.WebConfigParamUtils;
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
    
    //private static final String META_INF_SERVICES_RESOURCE_PREFIX = "META-INF/services/";

    private static final String DEFAULT_FACES_CONFIG = "/WEB-INF/faces-config.xml";

    private static final Set<String> FACTORY_NAMES = new HashSet<String>();
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
    
    /**
     * Set of .taglib.xml files, separated by ';' that should be loaded by facelet engine.
     */
    @JSFWebConfigParam(since = "2.0",
            desc = "Set of .taglib.xml files, separated by ';' that should be loaded by facelet engine.",
            deprecated = true)
    private final static String PARAM_LIBRARIES_DEPRECATED = "facelets.LIBRARIES";

    private final static String[] PARAMS_LIBRARIES = {ViewHandler.FACELETS_LIBRARIES_PARAM_NAME,
        PARAM_LIBRARIES_DEPRECATED};

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
            _unmarshaller = new DigesterFacesConfigUnmarshallerImpl(ectx);
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
            if (log.isLoggable(Level.INFO))
            {
                log.info("Reading standard config " + STANDARD_FACES_CONFIG_RESOURCE);
            }
            
            FacesConfig facesConfig = getUnmarshaller(ectx).getFacesConfig(stream, STANDARD_FACES_CONFIG_RESOURCE);
            stream.close();
            return facesConfig;
        }
        catch (IOException e)
        {
            throw new FacesException(e);
        }
        catch (SAXException e)
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
            org.apache.myfaces.config.impl.digester.elements.FacesConfigImpl facesConfig
                    = new org.apache.myfaces.config.impl.digester.elements.FacesConfigImpl();
            org.apache.myfaces.config.impl.digester.elements.FactoryImpl factory
                    = new org.apache.myfaces.config.impl.digester.elements.FactoryImpl();
            
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

                    if (factoryName.equals(FactoryFinder.APPLICATION_FACTORY))
                    {
                        factory.addApplicationFactory(className);
                    } 
                    else if(factoryName.equals(FactoryFinder.EXCEPTION_HANDLER_FACTORY)) 
                    {
                        factory.addExceptionHandlerFactory(className);
                    } 
                    else if (factoryName.equals(FactoryFinder.EXTERNAL_CONTEXT_FACTORY))
                    {
                        factory.addExternalContextFactory(className);
                    } 
                    else if (factoryName.equals(FactoryFinder.FACES_CONTEXT_FACTORY))
                    {
                        factory.addFacesContextFactory(className);
                    } 
                    else if (factoryName.equals(FactoryFinder.LIFECYCLE_FACTORY))
                    {
                        factory.addLifecycleFactory(className);
                    } 
                    else if (factoryName.equals(FactoryFinder.RENDER_KIT_FACTORY))
                    {
                        factory.addRenderkitFactory(className);
                    } 
                    else if(factoryName.equals(FactoryFinder.TAG_HANDLER_DELEGATE_FACTORY)) 
                    {
                        factory.addTagHandlerDelegateFactory(className);
                    } 
                    else if (factoryName.equals(FactoryFinder.PARTIAL_VIEW_CONTEXT_FACTORY))
                    {
                        factory.addPartialViewContextFactory(className);
                    } 
                    else if(factoryName.equals(FactoryFinder.VISIT_CONTEXT_FACTORY)) 
                    {
                        factory.addVisitContextFactory(className);
                    } 
                    else if(factoryName.equals(FactoryFinder.VIEW_DECLARATION_LANGUAGE_FACTORY)) 
                    {
                        factory.addViewDeclarationLanguageFactory(className);
                    }
                    else if(factoryName.equals(FactoryFinder.FLASH_FACTORY)) 
                    {
                        factory.addFlashFactory(className);
                    }
                    else if(factoryName.equals(FactoryFinder.FLOW_HANDLER_FACTORY)) 
                    {
                        factory.addFlowHandlerFactory(className);
                    }
                    else if(factoryName.equals(FactoryFinder.CLIENT_WINDOW_FACTORY)) 
                    {
                        factory.addClientWindowFactory(className);
                    }
                    else if(factoryName.equals(FactoryFinder.FACELET_CACHE_FACTORY)) 
                    {
                        factory.addFaceletCacheFactory(className);
                    }
                    else if(factoryName.equals(FactoryFinder.SEARCH_EXPRESSION_CONTEXT_FACTORY)) 
                    {
                        factory.addSearchExpressionContextFactory(className);
                    }
                    else
                    {
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
        List<FacesConfig> appConfigResources = new ArrayList<FacesConfig>();
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
                InputStream stream = null;
                try
                {
                    stream = openStreamWithoutCache(url);
                    if (log.isLoggable(Level.INFO))
                    {
                        log.info("Reading config : " + url.toExternalForm());
                    }
                    appConfigResources.add(getUnmarshaller(ectx).getFacesConfig(stream, url.toExternalForm()));
                    //getDispenser().feed(getUnmarshaller().getFacesConfig(stream, entry.getKey()));
                }
                finally
                {
                    if (stream != null)
                    {
                        stream.close();
                    }
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
        List<FacesConfig> appConfigResources = new ArrayList<FacesConfig>();
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
                InputStream stream = ectx.getResourceAsStream(systemId);
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
                //getDispenser().feed(getUnmarshaller().getFacesConfig(stream, systemId));
                stream.close();
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
            FacesConfig webAppConfig = null;
            // web application config
            if (MyfacesConfig.getCurrentInstance(ectx).isValidateXML())
            {
                URL url = ectx.getResource(DEFAULT_FACES_CONFIG);
                if (url != null)
                {
                    validateFacesConfig(ectx, url);
                }
            }
            InputStream stream = ectx.getResourceAsStream(DEFAULT_FACES_CONFIG);
            if (stream != null)
            {
                if (log.isLoggable(Level.INFO))
                {
                    log.info("Reading config /WEB-INF/faces-config.xml");
                }
                webAppConfig = getUnmarshaller(ectx).getFacesConfig(stream, DEFAULT_FACES_CONFIG);
                //getDispenser().feed(getUnmarshaller().getFacesConfig(stream, DEFAULT_FACES_CONFIG));
                stream.close();
            }
            return webAppConfig;
        }
        catch (IOException e)
        {
            throw new FacesException(e);
        }
        catch (SAXException e)
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
        List<String> configFilesList = new ArrayList<String>();
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
                List<FacesConfig> facesConfigList = new ArrayList<FacesConfig>();
                List<Document> documentList = new ArrayList<Document>();
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
                // memory buffer and then loads it using commons digester.
                // TODO: Find a better way without write the DOM and read it again and without
                // rewrite commons-digester parser!.
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

                            StringReader xmlReader = new StringReader(xmlAsWriter.toString());
                            FacesConfig facesConfig = getUnmarshaller(ectx).getFacesConfig(
                                xmlReader);
                            facesConfigList.add(facesConfig);
                        }
                        catch (IOException ex)
                        {
                            log.log(Level.SEVERE, "Error while reading faces-config from populator", ex);
                        }
                        catch (SAXException ex)
                        {
                            log.log(Level.SEVERE, "Error while reading faces-config from populator", ex);
                        }
                        catch (TransformerConfigurationException ex)
                        {
                            log.log(Level.SEVERE, "Error while reading faces-config from populator", ex);
                        }
                        catch (TransformerException ex)
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
        List<FacesConfig> configFilesList;
        Set<String> directoryPaths = ectx.getResourcePaths("/");
        if (directoryPaths == null)
        {
            return Collections.emptyList();
        }
        configFilesList = new ArrayList<FacesConfig>();
        
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
                        String filePath = webDirPath+flowName+"-flow.xml";
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
                String filePath = dirPath+flowName+"-flow.xml";
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
                //URL url = ectx.getResource(systemId);
                //if (url != null)
                //{
                    validateFacesConfig(ectx, url);
                //}
            }            
        }
        catch (IOException e)
        {
            throw new FacesException(e);
        }
        catch (SAXException e)
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
                //URL startNodeUrl = ectx.getResource(startNodePath);
                //if (startNodeUrl != null)
                //{
                flow.setStartNode(startNodePath);
                //}
                
                // There is a default return node with name [flow-name]-return and 
                // that by default points to an outer /[flow-name]-return outcome
                FacesFlowReturnImpl returnNode = new FacesFlowReturnImpl();
                returnNode.setId(flowName+"-return");
                NavigationCaseImpl returnNavCase = new NavigationCaseImpl();
                returnNavCase.setFromOutcome("/"+flowName+"-return");
                returnNode.setNavigationCase(returnNavCase);
                flow.addReturn(returnNode);
                
                facesConfig.addFacesFlowDefinition(flow);
                return facesConfig;
            }

            if (log.isLoggable(Level.INFO))
            {
                log.info("Reading config " + systemId);
            }
            
            FacesConfigImpl facesConfig = (FacesConfigImpl) 
                getUnmarshaller(ectx).getFacesConfig(pbstream, systemId);
            
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
            //getDispenser().feed(getUnmarshaller().getFacesConfig(stream, systemId));
        }
        catch (IOException e)
        {
            throw new FacesException(e);
        }
        catch (SAXException e)
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
        List<FacesConfig> facesConfigFilesList = new ArrayList<FacesConfig>();
        
        String param = WebConfigParamUtils.getStringInitParameter(externalContext, PARAMS_LIBRARIES);
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
                        org.apache.myfaces.config.impl.digester.elements.FacesConfigImpl config = 
                            new org.apache.myfaces.config.impl.digester.elements.FacesConfigImpl();
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
                        org.apache.myfaces.config.impl.digester.elements.FacesConfigImpl config = 
                            new org.apache.myfaces.config.impl.digester.elements.FacesConfigImpl();
                        config.addFaceletTagLibrary(tl);
                        facesConfigFilesList.add(config);
                    }
                    if (log.isLoggable(Level.FINE))
                    {
                        //log.fine("Added Library from: " + urls[i]);
                        log.fine("Added Library from: " + url);
                    }
                }
                catch (Exception e)
                {
                    //log.log(Level.SEVERE, "Error Loading Library: " + urls[i], e);
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