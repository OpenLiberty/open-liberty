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
package org.apache.myfaces.config.impl;

import java.io.ByteArrayInputStream;
import jakarta.faces.context.ExternalContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import jakarta.faces.FacesException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.myfaces.config.FacesConfigUnmarshaller;
import org.apache.myfaces.config.element.FacesFlowDefinition;
import org.apache.myfaces.config.impl.element.AbsoluteOrderingImpl;
import org.apache.myfaces.config.impl.element.ApplicationImpl;
import org.apache.myfaces.config.impl.element.AttributeImpl;
import org.apache.myfaces.config.impl.element.BehaviorImpl;
import org.apache.myfaces.config.impl.element.ClientBehaviorRendererImpl;
import org.apache.myfaces.config.impl.element.ComponentImpl;
import org.apache.myfaces.config.impl.element.ConfigOthersSlotImpl;
import org.apache.myfaces.config.impl.element.ContractMappingImpl;
import org.apache.myfaces.config.impl.element.ConverterImpl;
import org.apache.myfaces.config.impl.element.FaceletsProcessingImpl;
import org.apache.myfaces.config.impl.element.FaceletsTemplateMappingImpl;
import org.apache.myfaces.config.impl.element.FacesConfigExtensionImpl;
import org.apache.myfaces.config.impl.element.FacesConfigImpl;
import org.apache.myfaces.config.impl.element.FacesConfigNameSlotImpl;
import org.apache.myfaces.config.impl.element.FacesFlowCallImpl;
import org.apache.myfaces.config.impl.element.FacesFlowDefinitionImpl;
import org.apache.myfaces.config.impl.element.FacesFlowMethodCallImpl;
import org.apache.myfaces.config.impl.element.FacesFlowMethodParameterImpl;
import org.apache.myfaces.config.impl.element.FacesFlowParameterImpl;
import org.apache.myfaces.config.impl.element.FacesFlowReferenceImpl;
import org.apache.myfaces.config.impl.element.FacesFlowReturnImpl;
import org.apache.myfaces.config.impl.element.FacesFlowSwitchImpl;
import org.apache.myfaces.config.impl.element.FacesFlowViewImpl;
import org.apache.myfaces.config.impl.element.FactoryImpl;
import org.apache.myfaces.config.impl.element.LocaleConfigImpl;
import org.apache.myfaces.config.impl.element.NavigationCaseImpl;
import org.apache.myfaces.config.impl.element.NavigationRuleImpl;
import org.apache.myfaces.config.impl.element.OrderingImpl;
import org.apache.myfaces.config.impl.element.PropertyImpl;
import org.apache.myfaces.config.impl.element.RedirectImpl;
import org.apache.myfaces.config.impl.element.RenderKitImpl;
import org.apache.myfaces.config.impl.element.RendererImpl;
import org.apache.myfaces.config.impl.element.ResourceBundleImpl;
import org.apache.myfaces.config.impl.element.SystemEventListenerImpl;
import org.apache.myfaces.config.impl.element.ViewParamImpl;
import org.apache.myfaces.config.impl.element.ViewPoolMappingImpl;
import org.apache.myfaces.config.impl.element.ViewPoolParameterImpl;
import org.apache.myfaces.util.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class FacesConfigUnmarshallerImpl implements FacesConfigUnmarshaller<FacesConfigImpl>
{
    private static final Logger log = Logger.getLogger(FacesConfigUnmarshallerImpl.class.getName());

    private ExternalContext externalContext;
    
    public FacesConfigUnmarshallerImpl(ExternalContext externalContext)
    {
        this.externalContext = externalContext;
    }

    @Override
    public FacesConfigImpl getFacesConfig(String s) throws IOException, SAXException
    {
        return getFacesConfig(new ByteArrayInputStream(s.getBytes()), null);
    }

    @Override
    public FacesConfigImpl getFacesConfig(InputStream in, String systemId) throws IOException, SAXException
    {
        FacesConfigImpl facesConfig = new FacesConfigImpl();

        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            
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

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(new FacesConfigEntityResolver(externalContext));
            Document document;
            if (systemId == null)
            {
                document = builder.parse(in);
            }
            else
            {
                document = builder.parse(in, systemId);
            }
            document.getDocumentElement().normalize();
            
            onAttribute("metadata-complete", document.getDocumentElement(),
                    (v) -> { facesConfig.setMetadataComplete(v); });
            onAttribute("version", document.getDocumentElement(),
                    (v) -> { facesConfig.setVersion(v); });

            onChild("name", document.getDocumentElement(), 
                    (n) -> { facesConfig.setName(getTextContent(n)); });
            onChild("ordering", document.getDocumentElement(), 
                    (n) -> { facesConfig.setOrdering(processOrdering(n)); });
            onChild("absolute-ordering", document.getDocumentElement(), 
                    (n) -> { facesConfig.setAbsoluteOrdering(processAbsoluteOrdering(n)); });
            onChild("application", document.getDocumentElement(), 
                    (n) -> { facesConfig.addApplication(processApplication(n)); });
            onChild("factory", document.getDocumentElement(), 
                    (n) -> { facesConfig.addFactory(processFactory(n)); });
            onChild("component", document.getDocumentElement(),
                    (n) -> { facesConfig.addComponent(processComponent(n)); });
            onChild("lifecycle", document.getDocumentElement(), (n) -> {
                onChild("phase-listener", n, (cn) -> {
                    facesConfig.addLifecyclePhaseListener(getTextContent(cn)); 
                });  
            });
            onChild("validator", document.getDocumentElement(), (n) -> {
                facesConfig.addValidator(
                        firstChildTextContent("validator-id", n),
                        firstChildTextContent("validator-class", n));
            });
            onChild("render-kit", document.getDocumentElement(),
                    (n) -> { facesConfig.addRenderKit(processRenderKit(n)); });
            onChild("behavior", document.getDocumentElement(),
                    (n) -> { facesConfig.addBehavior(processBehavior(n)); });
            onChild("converter", document.getDocumentElement(),
                    (n) -> { facesConfig.addConverter(processConverter(n)); });
            onChild("protected-views", document.getDocumentElement(), (n) -> {
                onChild("url-pattern", n, (cn) -> {
                    facesConfig.addProtectedViewUrlPattern(getTextContent(cn)); 
                });  
            });
            onChild("faces-config-extension", document.getDocumentElement(),
                    (n) -> { facesConfig.addFacesConfigExtension(processFacesConfigExtension(n)); });
            onChild("navigation-rule", document.getDocumentElement(),
                    (n) -> { facesConfig.addNavigationRule(processNavigationRule(n)); });
            onChild("flow-definition", document.getDocumentElement(),
                    (n) -> { facesConfig.addFacesFlowDefinition(processFlowDefinition(n)); });
            
        }
        catch (Exception e)
        {
            throw new FacesException(e);
        }
        finally
        {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
                // ignore silently
            }
        }
          
        postProcessFacesConfig(systemId, facesConfig);

        return facesConfig;
    }

    private void postProcessFacesConfig(String systemId, FacesConfigImpl config)
    {
        for (org.apache.myfaces.config.element.Application application : config.getApplications())
        {
            for (org.apache.myfaces.config.element.LocaleConfig localeConfig : application.getLocaleConfig())
            {
                if (!localeConfig.getSupportedLocales().contains(localeConfig.getDefaultLocale()))
                {
                    localeConfig.addSupportedLocale(localeConfig.getDefaultLocale());
                }
            }
        }
        
        for (FacesFlowDefinition facesFlowDefinition : config.getFacesFlowDefinitions())
        {
            // Faces 2.2 section 11.4.3.1 says this: "... Flows are defined using the 
            // <flow-definition> element. This element must have an id attribute which uniquely 
            // identifies the flow within the scope of the Application Configuration Resource 
            // file in which the element appears. To enable multiple flows with the same id to 
            // exist in an application, the <faces-config><name> element is taken to 
            // be the definingDocumentId of the flow. If no <name> element is specified, 
            // the empty string is taken as the value for definingDocumentId. ..."
            if (config.getName() != null)
            {
                ((FacesFlowDefinitionImpl)facesFlowDefinition).setDefiningDocumentId(
                    config.getName());
            }
            else
            {
                ((FacesFlowDefinitionImpl)facesFlowDefinition).setDefiningDocumentId("");
            }
        }
    }

    protected OrderingImpl processOrdering(Node node)
    {
        OrderingImpl obj = new OrderingImpl();
        
        onChild("before", node, (n) -> {
            onChild("name", n, (cn) -> { 
                obj.addBeforeSlot(new FacesConfigNameSlotImpl(getTextContent(cn)));
            });
            onChild("others", n, (cn) -> {
                obj.addBeforeSlot(new ConfigOthersSlotImpl());
            });
        });

        onChild("after", node, (n) -> {
            onChild("name", n, (cn) -> { 
                obj.addAfterSlot(new FacesConfigNameSlotImpl(getTextContent(cn)));
            });
            onChild("others", n, (cn) -> {
                obj.addAfterSlot(new ConfigOthersSlotImpl());
            });
        });

        return obj;
    }
    
    protected AbsoluteOrderingImpl processAbsoluteOrdering(Node node)
    {
        AbsoluteOrderingImpl obj = new AbsoluteOrderingImpl();
        
        forEachChild(node, (n) -> {
            if ("name".equals(n.getLocalName()))
            {
                FacesConfigNameSlotImpl slot = new FacesConfigNameSlotImpl();
                slot.setName(getTextContent(n));
                obj.addOrderSlot(slot);
            }
            else if ("others".equals(n.getLocalName()))
            {
                obj.addOrderSlot(new ConfigOthersSlotImpl());
            }
        });

        return obj;
    }
    
    protected ApplicationImpl processApplication(Node node)
    {
        ApplicationImpl obj = new ApplicationImpl();
        
        onChild("action-listener", node, (n) -> { obj.addActionListener(getTextContent(n)); });
        onChild("message-bundle", node, (n) -> { obj.addMessageBundle(getTextContent(n)); });
        onChild("navigation-handler", node, (n) -> { obj.addNavigationHandler(getTextContent(n)); });
        onChild("view-handler", node, (n) -> { obj.addViewHandler(getTextContent(n)); });
        onChild("state-manager", node, (n) -> { obj.addStateManager(getTextContent(n)); });
        onChild("property-resolver", node, (n) -> { obj.addPropertyResolver(getTextContent(n)); });
        onChild("variable-resolver", node, (n) -> { obj.addVariableResolver(getTextContent(n)); });
        onChild("el-resolver", node, (n) -> { obj.addElResolver(getTextContent(n)); });
        onChild("resource-handler", node, (n) -> { obj.addResourceHandler(getTextContent(n)); });
        onChild("default-render-kit-id", node, (n) -> { obj.addDefaultRenderkitId(getTextContent(n)); });
        onChild("search-expression-handler", node, (n) -> { obj.addSearchExpressionHandler(getTextContent(n)); });
        onChild("search-keyword-resolver", node, (n) -> { obj.addSearchKeywordResolver(getTextContent(n)); });

        onChild("default-validators", node, (n) -> {
            obj.setDefaultValidatorsPresent();
            onChild("validator-id", n, (cn) -> {
                obj.addDefaultValidatorId(getTextContent(cn));
            });
        });
        
        onChild("locale-config", node, (n) -> {
            LocaleConfigImpl lc = new LocaleConfigImpl();
            onChild("default-locale", n, (cn) -> { lc.setDefaultLocale(getTextContent(cn)); });
            onChild("supported-locale", n, (cn) -> { lc.addSupportedLocale(getTextContent(cn)); });
            obj.addLocaleConfig(lc);
        });

        onChild("resource-bundle", node, (n) -> {
            ResourceBundleImpl rb = new ResourceBundleImpl();
            onChild("base-name", n, (cn) -> { rb.setBaseName(getTextContent(cn)); });
            onChild("var", n, (cn) -> { rb.setVar(getTextContent(cn)); });
            onChild("display-name", n, (cn) -> { rb.setDisplayName(getTextContent(cn)); });
            obj.addResourceBundle(rb);
        });
        
        onChild("system-event-listener", node, (n) -> {
            SystemEventListenerImpl sel = new SystemEventListenerImpl();
            onChild("system-event-listener-class", n, (cn) -> {
                sel.setSystemEventListenerClass(getTextContent(cn));
            });
            onChild("system-event-class", n, (cn) -> {
                sel.setSystemEventClass(getTextContent(cn));
            });
            onChild("source-class", n, (cn) -> {
                sel.setSourceClass(getTextContent(cn));
            });
            obj.addSystemEventListener(sel);
        });

        onChild("resource-library-contracts", node, (n) -> {
            onChild("contract-mapping", n, (cn) -> {
                ContractMappingImpl cm = new ContractMappingImpl();
                onChild("url-pattern", cn, (ccn) -> {
                    cm.addUrlPattern(getTextContent(ccn));
                });
                onChild("contracts", cn, (ccn) -> {
                    cm.addContract(getTextContent(ccn));
                });
                obj.addResourceLibraryContractMapping(cm);
            });
        });

        return obj;
    }
    
    protected FactoryImpl processFactory(Node node)
    {
        FactoryImpl obj = new FactoryImpl();
        
        onChild("application-factory", node, (n) -> { obj.addApplicationFactory(getTextContent(n)); });
        onChild("faces-context-factory", node, (n) -> { obj.addFacesContextFactory(getTextContent(n)); });
        onChild("lifecycle-factory", node, (n) -> { obj.addLifecycleFactory(getTextContent(n)); });
        onChild("render-kit-factory", node, (n) -> { obj.addRenderkitFactory(getTextContent(n)); });
        onChild("exception-handler-factory", node, (n) -> { obj.addExceptionHandlerFactory(getTextContent(n)); });
        onChild("external-context-factory", node, (n) -> { obj.addExternalContextFactory(getTextContent(n)); });
        onChild("view-declaration-language-factory", node, (n) -> {
            obj.addViewDeclarationLanguageFactory(getTextContent(n));
        });
        onChild("partial-view-context-factory", node, (n) -> {
            obj.addPartialViewContextFactory(getTextContent(n));
        });
        onChild("tag-handler-delegate-factory", node, (n) -> {
            obj.addTagHandlerDelegateFactory(getTextContent(n));
        });
        onChild("visit-context-factory", node, (n) -> { obj.addVisitContextFactory(getTextContent(n)); });
        onChild("search-expression-context-factory", node, (n) -> {
            obj.addSearchExpressionContextFactory(getTextContent(n));
        });
        onChild("facelet-cache-factory", node, (n) -> { obj.addFaceletCacheFactory(getTextContent(n)); });
        onChild("flash-factory", node, (n) -> { obj.addFlashFactory(getTextContent(n)); });
        onChild("flow-handler-factory", node, (n) -> { obj.addFlowHandlerFactory(getTextContent(n)); });
        onChild("client-window-factory", node, (n) -> { obj.addClientWindowFactory(getTextContent(n)); });
        
        return obj;
    }
    
    protected RenderKitImpl processRenderKit(Node node)
    {
        RenderKitImpl obj = new RenderKitImpl();

        onChild("render-kit-id", node, (n) -> { obj.setId(getTextContent(n)); });
        onChild("render-kit-class", node, (n) -> { obj.addRenderKitClass(getTextContent(n)); });

        onChild("renderer", node, (n) -> {
            RendererImpl r = new RendererImpl();
            onChild("component-family", n, (cn) -> { r.setComponentFamily(getTextContent(cn)); });
            onChild("renderer-type", n, (cn) -> { r.setRendererType(getTextContent(cn)); });
            onChild("renderer-class", n, (cn) -> { r.setRendererClass(getTextContent(cn)); });
            obj.addRenderer(r);
        });
        
        onChild("client-behavior-renderer", node, (n) -> {
            ClientBehaviorRendererImpl r = new ClientBehaviorRendererImpl();
            onChild("client-behavior-renderer-type", n, (cn) -> { r.setRendererType(getTextContent(cn)); });
            onChild("client-behavior-renderer-class", n, (cn) -> { r.setRendererClass(getTextContent(cn)); });
            obj.addClientBehaviorRenderer(r);
        });

        return obj;
    }

    protected ComponentImpl processComponent(Node node)
    {
        ComponentImpl obj = new ComponentImpl();

        onChild("component-type", node, (n) -> { obj.setComponentType(getTextContent(n)); });
        onChild("component-class", node, (n) -> { obj.setComponentClass(getTextContent(n)); });

        onChild("attribute", node, (n) -> {
            AttributeImpl a = new AttributeImpl();
            onChild("description", n, (cn) -> { a.addDescription(getTextContent(cn)); });
            onChild("display-name", n, (cn) -> { a.addDisplayName(getTextContent(cn)); });
            onChild("icon", n, (cn) -> { a.addIcon(getTextContent(cn)); });
            onChild("attribute-name", n, (cn) -> { a.setAttributeName(getTextContent(cn)); });
            onChild("attribute-class", n, (cn) -> { a.setAttributeClass(getTextContent(cn)); });
            onChild("default-value", n, (cn) -> { a.setDefaultValue(getTextContent(cn)); });
            onChild("suggested-value", n, (cn) -> { a.setSuggestedValue(getTextContent(cn)); });
            onChild("attribute-extension", n, (cn) -> { a.addAttributeExtension(getTextContent(cn)); });
            obj.addAttribute(a);
        });

        onChild("property", node, (n) -> {
            PropertyImpl p = new PropertyImpl();
            onChild("description", n, (cn) -> { p.addDescription(getTextContent(cn)); });
            onChild("display-name", n, (cn) -> { p.addDisplayName(getTextContent(cn)); });
            onChild("icon", n, (cn) -> { p.addIcon(getTextContent(cn)); });
            onChild("property-name", n, (cn) -> { p.setPropertyName(getTextContent(cn)); });
            onChild("property-class", n, (cn) -> { p.setPropertyClass(getTextContent(cn)); });
            onChild("default-value", n, (cn) -> { p.setDefaultValue(getTextContent(cn)); });
            onChild("suggested-value", n, (cn) -> { p.setSuggestedValue(getTextContent(cn)); });
            onChild("property-extension", n, (cn) -> { p.addPropertyExtension(getTextContent(cn)); });
            obj.addProperty(p);
        });

        return obj;
    }

    protected BehaviorImpl processBehavior(Node node)
    {
        BehaviorImpl obj = new BehaviorImpl();

        onChild("behavior-id", node, (n) -> { obj.setBehaviorId(getTextContent(n)); });
        onChild("behavior-class", node, (n) -> { obj.setBehaviorClass(getTextContent(n)); });
                
        onChild("attribute", node, (n) -> {
            AttributeImpl a = new AttributeImpl();
            onChild("description", n, (cn) -> { a.addDescription(getTextContent(cn)); });
            onChild("display-name", n, (cn) -> { a.addDisplayName(getTextContent(cn)); });
            onChild("icon", n, (cn) -> { a.addIcon(getTextContent(cn)); });
            onChild("attribute-name", n, (cn) -> { a.setAttributeName(getTextContent(cn)); });
            onChild("attribute-class", n, (cn) -> { a.setAttributeClass(getTextContent(cn)); });
            onChild("default-value", n, (cn) -> { a.setDefaultValue(getTextContent(cn)); });
            onChild("suggested-value", n, (cn) -> { a.setSuggestedValue(getTextContent(cn)); });
            onChild("attribute-extension", n, (cn) -> { a.addAttributeExtension(getTextContent(cn)); });
            obj.addAttribute(a);
        });
        
        onChild("property", node, (n) -> {
            PropertyImpl p = new PropertyImpl();
            onChild("description", n, (cn) -> { p.addDescription(getTextContent(cn)); });
            onChild("display-name", n, (cn) -> { p.addDisplayName(getTextContent(cn)); });
            onChild("icon", n, (cn) -> { p.addIcon(getTextContent(cn)); });
            onChild("property-name", n, (cn) -> { p.setPropertyName(getTextContent(cn)); });
            onChild("property-class", n, (cn) -> { p.setPropertyClass(getTextContent(cn)); });
            onChild("default-value", n, (cn) -> { p.setDefaultValue(getTextContent(cn)); });
            onChild("suggested-value", n, (cn) -> { p.setSuggestedValue(getTextContent(cn)); });
            onChild("property-extension", n, (cn) -> { p.addPropertyExtension(getTextContent(cn)); });
            obj.addProperty(p);
        });
        
        return obj;
    }

    protected ConverterImpl processConverter(Node node)
    {
        ConverterImpl obj = new ConverterImpl();

        onChild("converter-id", node, (n) -> { obj.setConverterId(getTextContent(n)); });
        onChild("converter-for-class", node, (n) -> { obj.setForClass(getTextContent(n)); });
        onChild("converter-class", node, (n) -> { obj.setConverterClass(getTextContent(n)); });
                
        onChild("attribute", node, (n) -> {
            AttributeImpl a = new AttributeImpl();
            onChild("description", n, (cn) -> { a.addDescription(getTextContent(cn)); });
            onChild("display-name", n, (cn) -> { a.addDisplayName(getTextContent(cn)); });
            onChild("icon", n, (cn) -> { a.addIcon(getTextContent(cn)); });
            onChild("attribute-name", n, (cn) -> { a.setAttributeName(getTextContent(cn)); });
            onChild("attribute-class", n, (cn) -> { a.setAttributeClass(getTextContent(cn)); });
            onChild("default-value", n, (cn) -> { a.setDefaultValue(getTextContent(cn)); });
            onChild("suggested-value", n, (cn) -> { a.setSuggestedValue(getTextContent(cn)); });
            onChild("attribute-extension", n, (cn) -> { a.addAttributeExtension(getTextContent(cn)); });
            obj.addAttribute(a);
        });
        
        onChild("property", node, (n) -> {
            PropertyImpl p = new PropertyImpl();
            onChild("description", n, (cn) -> { p.addDescription(getTextContent(cn)); });
            onChild("display-name", n, (cn) -> { p.addDisplayName(getTextContent(cn)); });
            onChild("icon", n, (cn) -> { p.addIcon(getTextContent(cn)); });
            onChild("property-name", n, (cn) -> { p.setPropertyName(getTextContent(cn)); });
            onChild("property-class", n, (cn) -> { p.setPropertyClass(getTextContent(cn)); });
            onChild("default-value", n, (cn) -> { p.setDefaultValue(getTextContent(cn)); });
            onChild("suggested-value", n, (cn) -> { p.setSuggestedValue(getTextContent(cn)); });
            onChild("property-extension", n, (cn) -> { p.addPropertyExtension(getTextContent(cn)); });
            obj.addProperty(p);
        });

        return obj;
    }
    
    protected FacesConfigExtensionImpl processFacesConfigExtension(Node node)
    {
        FacesConfigExtensionImpl obj = new FacesConfigExtensionImpl();
        
        onChild("facelets-processing", node, (n) -> {
            FaceletsProcessingImpl fp = new FaceletsProcessingImpl();
            onChild("file-extension", n, (cn) -> { fp.setFileExtension(getTextContent(cn)); });
            onChild("process-as", n, (cn) -> { fp.setProcessAs(getTextContent(cn)); });
            onChild("oam-compress-spaces", n, (cn) -> { fp.setOamCompressSpaces(getTextContent(cn)); });
            obj.addFaceletsProcessing(fp);
        });

        onChild("view-pool-mapping", node, (n) -> {
            ViewPoolMappingImpl vpm = new ViewPoolMappingImpl();
            onChild("url-pattern", n, (cn) -> { vpm.setUrlPattern(getTextContent(cn)); });
            onChild("parameter", n, (cn) -> {
                ViewPoolParameterImpl vpp = new ViewPoolParameterImpl();
                onChild("name", cn, (ccn) -> { vpp.setName(getTextContent(ccn)); });
                onChild("value", cn, (ccn) -> { vpp.setValue(getTextContent(ccn)); });
                vpm.addParameter(vpp);
            });
            obj.addViewPoolMapping(vpm);
        });

        onChild("facelets-template-mapping", node, (n) -> {
            FaceletsTemplateMappingImpl ftm = new FaceletsTemplateMappingImpl();
            onChild("url-pattern", n, (cn) -> { ftm.setUrlPattern(getTextContent(cn)); });
            obj.addFaceletsTemplateMapping(ftm);
        });  
        
        return obj;
    }
    
    protected NavigationRuleImpl processNavigationRule(Node node)
    {
        NavigationRuleImpl obj = new NavigationRuleImpl();
        
        onChild("from-view-id", node, (n) -> { obj.setFromViewId(getTextContent(n)); });
                
        onChild("navigation-case", node, (n) -> {
            obj.addNavigationCase(processNavigationCase(n));
        });
        
        return obj;
    }
    
    protected FacesFlowDefinitionImpl processFlowDefinition(Node node)
    {
        FacesFlowDefinitionImpl obj = new FacesFlowDefinitionImpl();
        
        onAttribute("id", node, (v) -> { obj.setId(v); });
        onChild("start-node", node, (n) -> { obj.setStartNode(getTextContent(n)); });
        onChild("initializer", node, (n) -> { obj.setInitializer(getTextContent(n)); });
        onChild("finalizer", node, (n) -> { obj.setFinalizer(getTextContent(n)); });
        
        onChild("view", node, (n) -> {
            FacesFlowViewImpl ffv = new FacesFlowViewImpl();
            onAttribute("id", n, (v) -> { ffv.setId(v); });
            onChild("vdl-document", n, (cn) -> { ffv.setVdlDocument(getTextContent(cn)); });
            obj.addView(ffv);
        });
        
        onChild("switch", node, (n) -> {
            FacesFlowSwitchImpl ffs = new FacesFlowSwitchImpl();
            onAttribute("id", n, (v) -> { ffs.setId(v); });
            onChild("case", n, (cn) -> { ffs.addNavigationCase(processNavigationCase(cn)); });
            onChild("default-outcome", n, (cn) -> {
                NavigationCaseImpl nc = new NavigationCaseImpl();
                nc.setFromOutcome(getTextContent(cn));
                ffs.setDefaultOutcome(nc);
            });
            obj.addSwitch(ffs);
        });
        
        onChild("flow-return", node, (n) -> {
            FacesFlowReturnImpl ffr = new FacesFlowReturnImpl();
            onAttribute("id", n, (v) -> { ffr.setId(v); });
            onChild("from-outcome", n, (cn) -> {
                NavigationCaseImpl nc = new NavigationCaseImpl();
                nc.setFromOutcome(getTextContent(cn));
                ffr.setNavigationCase(nc);
            });
            obj.addReturn(ffr);
        });
 
        onChild("navigation-rule", node, (n) -> {
            NavigationRuleImpl nr = new NavigationRuleImpl();
            onChild("from-view-id", n, (cn) -> { nr.setFromViewId(getTextContent(cn)); });
            onChild("navigation-case", n, (cn) -> { nr.addNavigationCase(processNavigationCase(cn)); });
            obj.addNavigationRule(nr);
        });
                
        onChild("flow-call", node, (n) -> {
            FacesFlowCallImpl ffc = new FacesFlowCallImpl();
            onAttribute("id", n, (v) -> { ffc.setId(v); });
            onChild("flow-reference", n, (cn) -> {
                FacesFlowReferenceImpl ffr = new FacesFlowReferenceImpl();
                onChild("flow-document-id", cn, (ccn) -> { ffr.setFlowDocumentId(getTextContent(ccn)); });
                onChild("flow-id", cn, (ccn) -> { ffr.setFlowId(getTextContent(ccn)); });
                ffc.setFlowReference(ffr);
            });
            onChild("outbound-parameter", n, (cn) -> {
                FacesFlowParameterImpl ffp = new FacesFlowParameterImpl();
                onChild("name", cn, (ccn) -> { ffp.setName(getTextContent(ccn)); });
                onChild("value", cn, (ccn) -> { ffp.setValue(getTextContent(ccn)); });
                ffc.addOutboundParameter(ffp);
            });
            obj.addFlowCall(ffc);
        });
        
        onChild("method-call", node, (n) -> {
            FacesFlowMethodCallImpl ffmc = new FacesFlowMethodCallImpl();
            onAttribute("id", n, (v) -> { ffmc.setId(v); });
            onChild("method", n, (cn) -> { ffmc.setMethod(getTextContent(cn)); });
            onChild("default-outcome", n, (cn) -> { ffmc.setDefaultOutcome(getTextContent(cn)); });
            onChild("parameter", n, (cn) -> {
                FacesFlowMethodParameterImpl ffmp = new FacesFlowMethodParameterImpl();
                onChild("class", cn, (ccn) -> { ffmp.setClassName(getTextContent(ccn)); });
                onChild("value", cn, (ccn) -> { ffmp.setValue(getTextContent(ccn)); });
                ffmc.addParameter(ffmp);
            });
            obj.addMethodCall(ffmc);
        });
       
        onChild("inbound-parameter", node, (n) -> {
            FacesFlowParameterImpl ffp = new FacesFlowParameterImpl();
            onChild("name", n, (cn) -> { ffp.setName(getTextContent(cn)); });
            onChild("value", n, (cn) -> { ffp.setValue(getTextContent(cn)); });
            obj.addInboundParameter(ffp);
        });
       
        return obj;
    }

    private NavigationCaseImpl processNavigationCase(Node node)
    {
        NavigationCaseImpl obj = new NavigationCaseImpl();

        onChild("from-action", node, (n) -> { obj.setFromAction(getTextContent(n)); });
        onChild("from-outcome", node, (n) -> { obj.setFromOutcome(getTextContent(n)); });
        onChild("if", node, (n) -> { obj.setIf(getTextContent(n)); });
        onChild("to-view-id", node, (n) -> { obj.setToViewId(getTextContent(n)); });
       
        onChild("redirect", node, (n) -> {
            RedirectImpl r = new RedirectImpl();
            onChild("include-view-params", n, (cn) -> { r.setIncludeViewParams("true"); });
            onChild("view-param", n, (cn) -> {
                ViewParamImpl vp = new ViewParamImpl();
                onChild("name", cn, (ccn) -> { vp.setName(getTextContent(ccn)); });
                onChild("value", cn, (ccn) -> { vp.setValue(getTextContent(ccn)); });
                r.addViewParam(vp);
            });
            onChild("redirect-param", n, (cn) -> {
                ViewParamImpl vp = new ViewParamImpl();
                onChild("name", cn, (ccn) -> { vp.setName(getTextContent(ccn)); });
                onChild("value", cn, (ccn) -> { vp.setValue(getTextContent(ccn)); });
                r.addViewParam(vp);
            });
            obj.setRedirect(r);
        });
        
        return obj;
    }
    
    
    
    
    protected void onAttribute(String name, Node node, Consumer<String> val)
    {
        if (node instanceof Element)
        {
            Element element = (Element) node;
            if (element.hasAttribute(name))
            {
                val.accept(element.getAttribute(name));
            }
        }
    }
    
    protected void forEachChild(Node node, Consumer<Node> val)
    {
        if (node.getChildNodes() != null)
        {
            for (int i = 0; i < node.getChildNodes().getLength(); i++)
            {
                Node childNode = node.getChildNodes().item(i);
                if (childNode == null)
                {
                    continue;
                }

                val.accept(childNode);
            }
        }
    }
    
    protected void onChild(String name, Node node, Consumer<Node> val)
    {
        if (node.getChildNodes() != null)
        {
            for (int i = 0; i < node.getChildNodes().getLength(); i++)
            {
                Node childNode = node.getChildNodes().item(i);
                if (childNode == null)
                {
                    continue;
                }

                if (name.equals(childNode.getLocalName()))
                {
                    val.accept(childNode);
                }
            }
        }
    }
    
    protected String firstChildTextContent(String name, Node node)
    {
        if (node.getChildNodes() != null)
        {
            for (int i = 0; i < node.getChildNodes().getLength(); i++)
            {
                Node childNode = node.getChildNodes().item(i);
                if (childNode == null)
                {
                    continue;
                }

                if (name.equals(childNode.getLocalName()))
                {
                    return childNode.getTextContent();
                }
            }
        }

        return null;
    }
    
    protected String getTextContent(Node node)
    {
        if (node == null)
        {
            return null;
        }

        String textContent = node.getTextContent();
        if (textContent != null)
        {
            textContent = textContent.trim();
        }

        return StringUtils.isBlank(textContent) ? null : textContent;
    }
}
