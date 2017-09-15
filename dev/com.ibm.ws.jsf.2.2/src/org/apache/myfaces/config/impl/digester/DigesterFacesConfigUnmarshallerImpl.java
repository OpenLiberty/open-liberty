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
package org.apache.myfaces.config.impl.digester;

import org.apache.commons.digester.Digester;
import org.apache.myfaces.config.FacesConfigUnmarshaller;
import org.apache.myfaces.config.impl.FacesConfigEntityResolver;
import org.apache.myfaces.config.impl.digester.elements.*;
import org.apache.myfaces.shared.util.ClassUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.faces.context.ExternalContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import org.apache.myfaces.config.element.FacesFlowDefinition;

/**
 * @author <a href="mailto:oliver@rossmueller.com">Oliver Rossmueller</a>
 */
public class DigesterFacesConfigUnmarshallerImpl implements FacesConfigUnmarshaller<FacesConfigImpl>
{
    private Digester digester;

    public DigesterFacesConfigUnmarshallerImpl(ExternalContext externalContext)
    {
        digester = new Digester();
        // TODO: validation set to false during implementation of 1.2
        digester.setValidating(false);
        digester.setNamespaceAware(true);
        digester.setEntityResolver(new FacesConfigEntityResolver(externalContext));
        //digester.setUseContextClassLoader(true);
        digester.setClassLoader(ClassUtils.getContextClassLoader());

        digester.addObjectCreate("faces-config", FacesConfigImpl.class);
        // 2.0 specific start
        digester.addSetProperties("faces-config", "metadata-complete", "metadataComplete");
        digester.addSetProperties("faces-config", "version", "version");
        // 2.0 specific end
        // 2.0 config ordering name start
        
        digester.addCallMethod("faces-config/protected-views/url-pattern", "addProtectedViewUrlPattern", 0);
        
        digester.addCallMethod("faces-config/name", "setName", 0);
        digester.addObjectCreate("faces-config/ordering", OrderingImpl.class);
        digester.addSetNext("faces-config/ordering", "setOrdering");
        digester.addObjectCreate("faces-config/ordering/before/name", FacesConfigNameSlotImpl.class);
        digester.addSetNext("faces-config/ordering/before/name", "addBeforeSlot");
        digester.addCallMethod("faces-config/ordering/before/name", "setName",0);        
        digester.addObjectCreate("faces-config/ordering/before/others", ConfigOthersSlotImpl.class);
        digester.addSetNext("faces-config/ordering/before/others", "addBeforeSlot");
        
        digester.addObjectCreate("faces-config/ordering/after/name", FacesConfigNameSlotImpl.class);
        digester.addSetNext("faces-config/ordering/after/name", "addAfterSlot");
        digester.addCallMethod("faces-config/ordering/after/name", "setName",0);        
        digester.addObjectCreate("faces-config/ordering/after/others", ConfigOthersSlotImpl.class);
        digester.addSetNext("faces-config/ordering/after/others", "addAfterSlot");        
        
        digester.addObjectCreate("faces-config/absolute-ordering", AbsoluteOrderingImpl.class);
        digester.addSetNext("faces-config/absolute-ordering", "setAbsoluteOrdering");
        digester.addObjectCreate("faces-config/absolute-ordering/name", FacesConfigNameSlotImpl.class);
        digester.addSetNext("faces-config/absolute-ordering/name", "addOrderSlot");
        digester.addCallMethod("faces-config/absolute-ordering/name", "setName",0);        
        digester.addObjectCreate("faces-config/absolute-ordering/others", ConfigOthersSlotImpl.class);
        digester.addSetNext("faces-config/absolute-ordering/others", "addOrderSlot");
        // 2.0 config ordering name end
        
        digester.addObjectCreate("faces-config/application", ApplicationImpl.class);
        digester.addSetNext("faces-config/application", "addApplication");
        digester.addCallMethod("faces-config/application/action-listener", "addActionListener", 0);
        digester.addCallMethod("faces-config/application/default-render-kit-id", "addDefaultRenderkitId", 0);
        digester.addCallMethod("faces-config/application/default-validators", "setDefaultValidatorsPresent");
        digester.addCallMethod("faces-config/application/default-validators/validator-id", "addDefaultValidatorId", 0);
        digester.addCallMethod("faces-config/application/message-bundle", "addMessageBundle", 0);
        digester.addCallMethod("faces-config/application/navigation-handler", "addNavigationHandler", 0);
        digester.addCallMethod("faces-config/application/partial-traversal", "addPartialTraversal", 0);
        digester.addCallMethod("faces-config/application/view-handler", "addViewHandler", 0);
        digester.addCallMethod("faces-config/application/state-manager", "addStateManager", 0);
        digester.addCallMethod("faces-config/application/property-resolver", "addPropertyResolver", 0);
        digester.addCallMethod("faces-config/application/variable-resolver", "addVariableResolver", 0);
        digester.addObjectCreate("faces-config/application/locale-config", LocaleConfigImpl.class);
        digester.addSetNext("faces-config/application/locale-config", "addLocaleConfig");
        digester.addCallMethod("faces-config/application/locale-config/default-locale", "setDefaultLocale", 0);
        digester.addCallMethod("faces-config/application/locale-config/supported-locale", "addSupportedLocale", 0);

        // 1.2 specific start
        digester.addCallMethod("faces-config/application/el-resolver", "addElResolver", 0);
        digester.addObjectCreate("faces-config/application/resource-bundle", ResourceBundleImpl.class);
        digester.addSetNext("faces-config/application/resource-bundle", "addResourceBundle");
        digester.addCallMethod("faces-config/application/resource-bundle/base-name", "setBaseName", 0);
        digester.addCallMethod("faces-config/application/resource-bundle/var", "setVar", 0);
        digester.addCallMethod("faces-config/application/resource-bundle/display-name", "setDisplayName", 0);
        // 1.2 specific end

        // 2.0 specific start
        digester.addObjectCreate("faces-config/application/system-event-listener", SystemEventListenerImpl.class);
        digester.addSetNext("faces-config/application/system-event-listener", "addSystemEventListener");
        digester.addCallMethod("faces-config/application/system-event-listener/system-event-listener-class",
                               "setSystemEventListenerClass",0);
        digester.addCallMethod("faces-config/application/system-event-listener/system-event-class",
                               "setSystemEventClass",0);
        digester.addCallMethod("faces-config/application/system-event-listener/source-class", "setSourceClass",0);
        digester.addCallMethod("faces-config/application/resource-handler", "addResourceHandler", 0);
        digester.addCallMethod("faces-config/factory/exception-handler-factory", "addExceptionHandlerFactory", 0);
        digester.addCallMethod("faces-config/factory/external-context-factory", "addExternalContextFactory", 0);
        digester.addCallMethod("faces-config/factory/view-declaration-language-factory",
                               "addViewDeclarationLanguageFactory", 0);
        digester.addCallMethod("faces-config/factory/partial-view-context-factory", "addPartialViewContextFactory", 0);
        digester.addCallMethod("faces-config/factory/tag-handler-delegate-factory", "addTagHandlerDelegateFactory", 0);
        digester.addCallMethod("faces-config/factory/visit-context-factory", "addVisitContextFactory", 0);
        // 2.0 specific end
        
        digester.addObjectCreate("faces-config/application/resource-library-contracts/contract-mapping", 
            ContractMappingImpl.class);
        digester.addSetNext("faces-config/application/resource-library-contracts/contract-mapping", 
            "addResourceLibraryContractMapping");
        digester.addCallMethod(
            "faces-config/application/resource-library-contracts/contract-mapping/url-pattern", "addUrlPattern", 0);
        digester.addCallMethod(
            "faces-config/application/resource-library-contracts/contract-mapping/contracts", "addContract", 0);
        
        // 2.1 specific start
        digester.addCallMethod("faces-config/factory/facelet-cache-factory", "addFaceletCacheFactory", 0);
        // 2.1 specific end
        // 2.2 specific start
        digester.addCallMethod("faces-config/factory/flash-factory", "addFlashFactory", 0);
        // Note there is no client-window-factory, this factory can be set only using SPI.
        digester.addCallMethod("faces-config/factory/flow-handler-factory", "addFlowHandlerFactory", 0);
        // 2.2 specific end

        digester.addObjectCreate("faces-config/factory", FactoryImpl.class);
        digester.addSetNext("faces-config/factory", "addFactory");
        digester.addCallMethod("faces-config/factory/application-factory", "addApplicationFactory", 0);
        digester.addCallMethod("faces-config/factory/faces-context-factory", "addFacesContextFactory", 0);
        digester.addCallMethod("faces-config/factory/lifecycle-factory", "addLifecycleFactory", 0);
        digester.addCallMethod("faces-config/factory/render-kit-factory", "addRenderkitFactory", 0);

        digester.addCallMethod("faces-config/component", "addComponent", 2);
        digester.addCallParam("faces-config/component/component-type", 0);
        digester.addCallParam("faces-config/component/component-class", 1);

        digester.addObjectCreate("faces-config/converter", ConverterImpl.class);
        digester.addSetNext("faces-config/converter", "addConverter");
        digester.addCallMethod("faces-config/converter/converter-id", "setConverterId", 0);
        digester.addCallMethod("faces-config/converter/converter-for-class", "setForClass", 0);
        digester.addCallMethod("faces-config/converter/converter-class", "setConverterClass", 0);
        digester.addObjectCreate("faces-config/converter/attribute", AttributeImpl.class);
        digester.addSetNext("faces-config/converter/attribute", "addAttribute");
        digester.addCallMethod("faces-config/converter/attribute/description", "addDescription", 0);
        digester.addCallMethod("faces-config/converter/attribute/display-name", "addDisplayName", 0);
        digester.addCallMethod("faces-config/converter/attribute/icon", "addIcon", 0);
        digester.addCallMethod("faces-config/converter/attribute/attribute-name", "setAttributeName", 0);
        digester.addCallMethod("faces-config/converter/attribute/attribute-class", "setAttributeClass", 0);
        digester.addCallMethod("faces-config/converter/attribute/default-value", "setDefaultValue", 0);
        digester.addCallMethod("faces-config/converter/attribute/suggested-value", "setSuggestedValue", 0);
        digester.addCallMethod("faces-config/converter/attribute/attribute-extension", "addAttributeExtension", 0);
        digester.addObjectCreate("faces-config/converter/property", PropertyImpl.class);
        digester.addSetNext("faces-config/converter/property", "addProperty");
        digester.addCallMethod("faces-config/converter/property/description", "addDescription", 0);
        digester.addCallMethod("faces-config/converter/property/display-name", "addDisplayName", 0);
        digester.addCallMethod("faces-config/converter/property/icon", "addIcon", 0);
        digester.addCallMethod("faces-config/converter/property/property-name", "setPropertyName", 0);
        digester.addCallMethod("faces-config/converter/property/property-class", "setPropertyClass", 0);
        digester.addCallMethod("faces-config/converter/property/default-value", "setDefaultValue", 0);
        digester.addCallMethod("faces-config/converter/property/suggested-value", "setSuggestedValue", 0);
        digester.addCallMethod("faces-config/converter/property/property-extension", "addPropertyExtension", 0);

        digester.addObjectCreate("faces-config/managed-bean", ManagedBeanImpl.class);
        digester.addSetProperties("faces-config/managed-bean", "eager", "eager");
        digester.addSetNext("faces-config/managed-bean", "addManagedBean");
        digester.addCallMethod("faces-config/managed-bean/description", "setDescription", 0);
        digester.addCallMethod("faces-config/managed-bean/managed-bean-name", "setName", 0);
        digester.addCallMethod("faces-config/managed-bean/managed-bean-class", "setBeanClass", 0);
        digester.addCallMethod("faces-config/managed-bean/managed-bean-scope", "setScope", 0);
        digester.addObjectCreate("faces-config/managed-bean/managed-property", ManagedPropertyImpl.class);
        digester.addSetNext("faces-config/managed-bean/managed-property", "addProperty");
        digester.addCallMethod("faces-config/managed-bean/managed-property/property-name", "setPropertyName", 0);
        digester.addCallMethod("faces-config/managed-bean/managed-property/property-class", "setPropertyClass", 0);
        digester.addCallMethod("faces-config/managed-bean/managed-property/null-value", "setNullValue");
        digester.addCallMethod("faces-config/managed-bean/managed-property/value", "setValue", 0);
        digester.addObjectCreate("faces-config/managed-bean/managed-property/map-entries", MapEntriesImpl.class);
        digester.addSetNext("faces-config/managed-bean/managed-property/map-entries", "setMapEntries");
        digester.addCallMethod("faces-config/managed-bean/managed-property/map-entries/key-class", "setKeyClass", 0);
        digester.addCallMethod("faces-config/managed-bean/managed-property/map-entries/value-class",
                               "setValueClass", 0);
        digester.addObjectCreate("faces-config/managed-bean/managed-property/map-entries/map-entry",
                                 MapEntriesImpl.Entry.class);
        digester.addSetNext("faces-config/managed-bean/managed-property/map-entries/map-entry", "addEntry");
        digester.addCallMethod("faces-config/managed-bean/managed-property/map-entries/map-entry/key", "setKey", 0);
        digester.addCallMethod("faces-config/managed-bean/managed-property/map-entries/map-entry/null-value",
                               "setNullValue");
        digester.addCallMethod("faces-config/managed-bean/managed-property/map-entries/map-entry/value", "setValue", 0);
        digester.addObjectCreate("faces-config/managed-bean/managed-property/list-entries", ListEntriesImpl.class);
        digester.addSetNext("faces-config/managed-bean/managed-property/list-entries", "setListEntries");
        digester.addCallMethod("faces-config/managed-bean/managed-property/list-entries/value-class", "setValueClass",
                               0);
        digester.addObjectCreate("faces-config/managed-bean/managed-property/list-entries/null-value",
                                 ListEntriesImpl.Entry.class);
        digester.addSetNext("faces-config/managed-bean/managed-property/list-entries/null-value", "addEntry");
        digester.addCallMethod("faces-config/managed-bean/managed-property/list-entries/null-value", "setNullValue");
        digester.addObjectCreate("faces-config/managed-bean/managed-property/list-entries/value",
                                 ListEntriesImpl.Entry.class);
        digester.addSetNext("faces-config/managed-bean/managed-property/list-entries/value", "addEntry");
        digester.addCallMethod("faces-config/managed-bean/managed-property/list-entries/value", "setValue", 0);
        digester.addObjectCreate("faces-config/managed-bean/map-entries", MapEntriesImpl.class);
        digester.addSetNext("faces-config/managed-bean/map-entries", "setMapEntries");
        digester.addCallMethod("faces-config/managed-bean/map-entries/key-class", "setKeyClass", 0);
        digester.addCallMethod("faces-config/managed-bean/map-entries/value-class", "setValueClass", 0);
        digester.addObjectCreate("faces-config/managed-bean/map-entries/map-entry", MapEntriesImpl.Entry.class);
        digester.addSetNext("faces-config/managed-bean/map-entries/map-entry", "addEntry");
        digester.addCallMethod("faces-config/managed-bean/map-entries/map-entry/key", "setKey", 0);
        digester.addCallMethod("faces-config/managed-bean/map-entries/map-entry/null-value", "setNullValue");
        digester.addCallMethod("faces-config/managed-bean/map-entries/map-entry/value", "setValue", 0);
        digester.addObjectCreate("faces-config/managed-bean/list-entries", ListEntriesImpl.class);
        digester.addSetNext("faces-config/managed-bean/list-entries", "setListEntries");
        digester.addCallMethod("faces-config/managed-bean/list-entries/value-class", "setValueClass", 0);
        digester.addObjectCreate("faces-config/managed-bean/list-entries/null-value", ListEntriesImpl.Entry.class);
        digester.addSetNext("faces-config/managed-bean/list-entries/null-value", "addEntry");
        digester.addCallMethod("faces-config/managed-bean/list-entries/null-value", "setNullValue");
        digester.addObjectCreate("faces-config/managed-bean/list-entries/value", ListEntriesImpl.Entry.class);
        digester.addSetNext("faces-config/managed-bean/list-entries/value", "addEntry");
        digester.addCallMethod("faces-config/managed-bean/list-entries/value", "setValue", 0);

        digester.addObjectCreate("faces-config/navigation-rule", NavigationRuleImpl.class);
        digester.addSetNext("faces-config/navigation-rule", "addNavigationRule");
        digester.addCallMethod("faces-config/navigation-rule/from-view-id", "setFromViewId", 0);
        digester.addObjectCreate("faces-config/navigation-rule/navigation-case", NavigationCaseImpl.class);
        digester.addSetNext("faces-config/navigation-rule/navigation-case", "addNavigationCase");
        digester.addCallMethod("faces-config/navigation-rule/navigation-case/from-action", "setFromAction", 0);
        digester.addCallMethod("faces-config/navigation-rule/navigation-case/from-outcome", "setFromOutcome", 0);
        digester.addCallMethod("faces-config/navigation-rule/navigation-case/if", "setIf", 0);
        digester.addCallMethod("faces-config/navigation-rule/navigation-case/to-view-id", "setToViewId", 0);
        digester.addObjectCreate("faces-config/navigation-rule/navigation-case/redirect", RedirectImpl.class);
        digester.addSetProperties("faces-config/navigation-rule/navigation-case/redirect", "include-view-params",
                                  "includeViewParams");
        digester.addSetNext("faces-config/navigation-rule/navigation-case/redirect", "setRedirect");
        digester.addObjectCreate("faces-config/navigation-rule/navigation-case/redirect/view-param", 
            ViewParamImpl.class);
        digester.addSetNext("faces-config/navigation-rule/navigation-case/redirect/view-param", "addViewParam");
        digester.addCallMethod("faces-config/navigation-rule/navigation-case/redirect/view-param/name", "setName",0);
        digester.addCallMethod("faces-config/navigation-rule/navigation-case/redirect/view-param/value", "setValue",0);

        digester.addObjectCreate("faces-config/navigation-rule/navigation-case/redirect/redirect-param", 
            ViewParamImpl.class);
        digester.addSetNext("faces-config/navigation-rule/navigation-case/redirect/redirect-param", 
            "addViewParam");
        digester.addCallMethod("faces-config/navigation-rule/navigation-case/redirect/redirect-param/name", 
            "setName",0);
        digester.addCallMethod("faces-config/navigation-rule/navigation-case/redirect/redirect-param/value", 
            "setValue",0);

        digester.addObjectCreate("faces-config/render-kit", RenderKitImpl.class);
        digester.addSetNext("faces-config/render-kit", "addRenderKit");
        digester.addCallMethod("faces-config/render-kit/render-kit-id", "setId", 0);
        //digester.addCallMethod("faces-config/render-kit/render-kit-class", "setRenderKitClass", 0);
        digester.addCallMethod("faces-config/render-kit/render-kit-class", "addRenderKitClass", 0);
        digester.addObjectCreate("faces-config/render-kit/renderer", RendererImpl.class);
        digester.addSetNext("faces-config/render-kit/renderer", "addRenderer");
        digester.addCallMethod("faces-config/render-kit/renderer/component-family", "setComponentFamily", 0);
        digester.addCallMethod("faces-config/render-kit/renderer/renderer-type", "setRendererType", 0);
        digester.addCallMethod("faces-config/render-kit/renderer/renderer-class", "setRendererClass", 0);
        digester.addObjectCreate("faces-config/render-kit/client-behavior-renderer", ClientBehaviorRendererImpl.class);
        digester.addSetNext("faces-config/render-kit/client-behavior-renderer", "addClientBehaviorRenderer");
        digester.addCallMethod("faces-config/render-kit/client-behavior-renderer/client-behavior-renderer-type",
                               "setRendererType", 0);
        digester.addCallMethod("faces-config/render-kit/client-behavior-renderer/client-behavior-renderer-class",
                               "setRendererClass", 0);
        
        // 2.0 behavior start
        digester.addObjectCreate("faces-config/behavior", BehaviorImpl.class);
        digester.addSetNext("faces-config/behavior", "addBehavior");
        digester.addCallMethod("faces-config/behavior/behavior-id", "setBehaviorId", 0);
        digester.addCallMethod("faces-config/behavior/behavior-class", "setBehaviorClass", 0);
        digester.addObjectCreate("faces-config/behavior/attribute", AttributeImpl.class);
        digester.addSetNext("faces-config/behavior/attribute", "addAttribute");
        digester.addCallMethod("faces-config/behavior/attribute/description", "addDescription", 0);
        digester.addCallMethod("faces-config/behavior/attribute/display-name", "addDisplayName", 0);
        digester.addCallMethod("faces-config/behavior/attribute/icon", "addIcon", 0);
        digester.addCallMethod("faces-config/behavior/attribute/attribute-name", "setAttributeName", 0);
        digester.addCallMethod("faces-config/behavior/attribute/attribute-class", "setAttributeClass", 0);
        digester.addCallMethod("faces-config/behavior/attribute/default-value", "setDefaultValue", 0);
        digester.addCallMethod("faces-config/behavior/attribute/suggested-value", "setSuggestedValue", 0);
        digester.addCallMethod("faces-config/behavior/attribute/attribute-extension", "addAttributeExtension", 0);
        digester.addObjectCreate("faces-config/behavior/property", PropertyImpl.class);
        digester.addSetNext("faces-config/behavior/property", "addProperty");
        digester.addCallMethod("faces-config/behavior/property/description", "addDescription", 0);
        digester.addCallMethod("faces-config/behavior/property/display-name", "addDisplayName", 0);
        digester.addCallMethod("faces-config/behavior/property/icon", "addIcon", 0);
        digester.addCallMethod("faces-config/behavior/property/property-name", "setPropertyName", 0);
        digester.addCallMethod("faces-config/behavior/property/property-class", "setPropertyClass", 0);
        digester.addCallMethod("faces-config/behavior/property/default-value", "setDefaultValue", 0);
        digester.addCallMethod("faces-config/behavior/property/suggested-value", "setSuggestedValue", 0);
        digester.addCallMethod("faces-config/behavior/property/property-extension", "addPropertyExtension", 0);
        // 2.0 behavior end
        
        digester.addCallMethod("faces-config/lifecycle/phase-listener", "addLifecyclePhaseListener", 0);

        digester.addCallMethod("faces-config/validator", "addValidator", 2);
        digester.addCallParam("faces-config/validator/validator-id", 0);
        digester.addCallParam("faces-config/validator/validator-class", 1);
        
        // 2.1 facelets-processing start
        digester.addObjectCreate("faces-config/faces-config-extension", FacesConfigExtensionImpl.class);
        digester.addSetNext("faces-config/faces-config-extension", "addFacesConfigExtension");
        digester.addObjectCreate("faces-config/faces-config-extension/facelets-processing", 
            FaceletsProcessingImpl.class);
        digester.addSetNext("faces-config/faces-config-extension/facelets-processing", "addFaceletsProcessing");
        digester.addCallMethod("faces-config/faces-config-extension/facelets-processing/file-extension",
                               "setFileExtension", 0);
        digester.addCallMethod("faces-config/faces-config-extension/facelets-processing/process-as", "setProcessAs", 0);

        // 2.1 facelets-processing end
        
        //MyFaces specific facelets-processing instruction.
        digester.addCallMethod("faces-config/faces-config-extension/facelets-processing/oam-compress-spaces", 
                "setOamCompressSpaces", 0);
        
        addFacesFlowRules(externalContext);
        
        
    }
    
    private void addNavigationRules(ExternalContext externalContext, String prefix, String method)
    {
        digester.addObjectCreate(prefix, NavigationRuleImpl.class);
        digester.addSetNext(prefix, method);
        digester.addCallMethod(prefix+"/from-view-id", "setFromViewId", 0);
        addNavigationCases(externalContext, prefix+"/navigation-case", "addNavigationCase");
    }
    
    private void addNavigationCases(ExternalContext externalContext, String prefix, String method)
    {
        digester.addObjectCreate(prefix, NavigationCaseImpl.class);
        digester.addSetNext(prefix, method);
        digester.addCallMethod(prefix+"/from-action", "setFromAction", 0);
        digester.addCallMethod(prefix+"/from-outcome", "setFromOutcome", 0);
        digester.addCallMethod(prefix+"/if", "setIf", 0);
        digester.addCallMethod(prefix+"/to-view-id", "setToViewId", 0);
        digester.addObjectCreate(prefix+"/redirect", RedirectImpl.class);
        digester.addSetProperties(prefix+"/redirect", "include-view-params",
                                  "includeViewParams");
        digester.addSetNext(prefix+"/redirect", "setRedirect");
        digester.addObjectCreate(prefix+"/redirect/view-param", ViewParamImpl.class);
        digester.addSetNext(prefix+"/redirect/view-param", "addViewParam");
        digester.addCallMethod(prefix+"/redirect/view-param/name", "setName",0);
        digester.addCallMethod(prefix+"/redirect/view-param/value", "setValue",0);

        digester.addObjectCreate(prefix+"/redirect/redirect-param", ViewParamImpl.class);
        digester.addSetNext(prefix+"/redirect/redirect-param", "addViewParam");
        digester.addCallMethod(prefix+"/redirect/redirect-param/name", "setName",0);
        digester.addCallMethod(prefix+"/redirect/redirect-param/value", "setValue",0);
    }
    
    private void addFacesFlowRules(ExternalContext externalContext)
    {
        digester.addObjectCreate("faces-config/flow-definition", FacesFlowDefinitionImpl.class);
        
        digester.addSetNext("faces-config/flow-definition", "addFacesFlowDefinition");
        digester.addSetProperties("faces-config/flow-definition", "id", "id");
        
        digester.addCallMethod("faces-config/flow-definition/start-node", "setStartNode", 0);
        digester.addCallMethod("faces-config/flow-definition/initializer", "setInitializer", 0);
        digester.addCallMethod("faces-config/flow-definition/finalizer", "setFinalizer", 0);
        
        digester.addObjectCreate("faces-config/flow-definition/view", FacesFlowViewImpl.class);
        digester.addSetNext("faces-config/flow-definition/view", "addView");
        digester.addSetProperties("faces-config/flow-definition/view", "id", "id");
        digester.addCallMethod("faces-config/flow-definition/view/vdl-document", "setVdlDocument", 0);
        
        digester.addObjectCreate("faces-config/flow-definition/switch", FacesFlowSwitchImpl.class);
        digester.addSetNext("faces-config/flow-definition/switch", "addSwitch");
        digester.addSetProperties("faces-config/flow-definition/switch", "id", "id");
        
        digester.addObjectCreate("faces-config/flow-definition/switch/default-outcome", 
            NavigationCaseImpl.class);
        digester.addSetNext("faces-config/flow-definition/switch/default-outcome", 
            "setDefaultOutcome");
        digester.addCallMethod("faces-config/flow-definition/switch/default-outcome", 
            "setFromOutcome", 0);
        
        addNavigationCases(externalContext, "faces-config/flow-definition/switch/case",
            "addNavigationCase");
        
        digester.addObjectCreate("faces-config/flow-definition/flow-return", FacesFlowReturnImpl.class);
        digester.addSetNext("faces-config/flow-definition/flow-return", "addReturn");
        digester.addSetProperties("faces-config/flow-definition/flow-return", "id", "id");
        digester.addObjectCreate("faces-config/flow-definition/flow-return/from-outcome", 
            NavigationCaseImpl.class);
        digester.addSetNext("faces-config/flow-definition/flow-return/from-outcome", 
            "setNavigationCase");
        digester.addCallMethod("faces-config/flow-definition/flow-return/from-outcome", 
            "setFromOutcome", 0);
        
        addNavigationRules(externalContext, "faces-config/flow-definition/navigation-rule", "addNavigationRule");
        
        digester.addObjectCreate("faces-config/flow-definition/flow-call", FacesFlowCallImpl.class);
        digester.addSetNext("faces-config/flow-definition/flow-call", "addFlowCall");
        digester.addSetProperties("faces-config/flow-definition/flow-call", "id", "id");
        digester.addObjectCreate("faces-config/flow-definition/flow-call/flow-reference", FacesFlowReferenceImpl.class);
        digester.addSetNext("faces-config/flow-definition/flow-call/flow-reference", "setFlowReference");        
        digester.addCallMethod("faces-config/flow-definition/flow-call/flow-reference/flow-document-id", 
                "setFlowDocumentId", 0);
        digester.addCallMethod("faces-config/flow-definition/flow-call/flow-reference/flow-id", "setFlowId", 0);
        
        digester.addObjectCreate("faces-config/flow-definition/flow-call/outbound-parameter", 
            FacesFlowParameterImpl.class);
        digester.addSetNext("faces-config/flow-definition/flow-call/outbound-parameter", 
            "addOutboundParameter");
        digester.addCallMethod("faces-config/flow-definition/flow-call/outbound-parameter/name", "setName", 0);
        digester.addCallMethod("faces-config/flow-definition/flow-call/outbound-parameter/value", "setValue", 0);
        
        digester.addObjectCreate("faces-config/flow-definition/method-call", FacesFlowMethodCallImpl.class);
        digester.addSetNext("faces-config/flow-definition/method-call", "addMethodCall");
        digester.addSetProperties("faces-config/flow-definition/method-call", "id", "id");
        digester.addCallMethod("faces-config/flow-definition/method-call/method", "setMethod", 0);
        digester.addCallMethod("faces-config/flow-definition/method-call/default-outcome", 
            "setDefaultOutcome", 0);
        digester.addObjectCreate("faces-config/flow-definition/method-call/parameter", 
            FacesFlowMethodParameterImpl.class);
        digester.addSetNext("faces-config/flow-definition/method-call/parameter", "addParameter");
        digester.addCallMethod("faces-config/flow-definition/method-call/parameter/class", "setClassName", 0);
        digester.addCallMethod("faces-config/flow-definition/method-call/parameter/value", "setValue", 0);
        
        digester.addObjectCreate("faces-config/flow-definition/inbound-parameter", 
            FacesFlowParameterImpl.class);
        digester.addSetNext("faces-config/flow-definition/inbound-parameter", "addInboundParameter");
        digester.addCallMethod("faces-config/flow-definition/inbound-parameter/name", "setName", 0);
        digester.addCallMethod("faces-config/flow-definition/inbound-parameter/value", "setValue", 0);
        
        //View Pool config
        digester.addObjectCreate("faces-config/faces-config-extension/view-pool-mapping", 
            ViewPoolMappingImpl.class);
        digester.addSetNext("faces-config/faces-config-extension/view-pool-mapping", 
            "addViewPoolMapping");
        digester.addCallMethod(
                        "faces-config/faces-config-extension/view-pool-mapping/url-pattern", "setUrlPattern", 0);
        digester.addObjectCreate("faces-config/faces-config-extension/view-pool-mapping/parameter", 
            ViewPoolParameterImpl.class);
        digester.addSetNext("faces-config/faces-config-extension/view-pool-mapping/parameter", "addParameter");
        digester.addCallMethod("faces-config/faces-config-extension/view-pool-mapping/parameter/name", "setName", 0);
        digester.addCallMethod("faces-config/faces-config-extension/view-pool-mapping/parameter/value", "setValue", 0);
    }

    private void postProcessFacesConfig(String systemId, FacesConfigImpl config)
    {
        for (org.apache.myfaces.config.element.Application application : config.getApplications())
        {
            for (org.apache.myfaces.config.element.LocaleConfig localeConfig : application.getLocaleConfig())
            {
                if (!localeConfig.getSupportedLocales().contains(localeConfig.getDefaultLocale()))
                {
                    localeConfig.getSupportedLocales().add(localeConfig.getDefaultLocale());
                }
            }
        }
        
        for (FacesFlowDefinition facesFlowDefinition : config.getFacesFlowDefinitions())
        {
            // JSF 2.2 section 11.4.3.1 says this: "... Flows are defined using the 
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
    
    public FacesConfigImpl getFacesConfig(InputStream in, String systemId) throws IOException, SAXException
    {
        InputSource is = new InputSource(in);
        is.setSystemId(systemId);

        // Fix for http://issues.apache.org/jira/browse/MYFACES-236
        FacesConfigImpl config = (FacesConfigImpl) digester.parse(is);

        postProcessFacesConfig(systemId, config);

        return config;
    }
    
    public FacesConfigImpl getFacesConfig(Reader r) throws IOException, SAXException
    {
        //InputSource is = new InputSource(in);
        //is.setSystemId(systemId);

        // Fix for http://issues.apache.org/jira/browse/MYFACES-236
        FacesConfigImpl config = (FacesConfigImpl) digester.parse(r);

        postProcessFacesConfig(null, config);

        return config;
    }

}
