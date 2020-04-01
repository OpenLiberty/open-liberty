/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.processor;

import static com.ibm.wsspi.injectionengine.InjectionConfigConstants.EE5Compatibility;

import java.lang.reflect.Member;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.ManagedBean;
import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;
import javax.annotation.Resources;
import javax.naming.Reference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.injectionengine.AbstractInjectionEngine;
import com.ibm.ws.injectionengine.InjectionScopeData;
import com.ibm.ws.injectionengine.annotation.ResourceImpl;
import com.ibm.ws.javaee.dd.common.Describable;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionEngineAccessor;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionScope;
import com.ibm.wsspi.injectionengine.InternalInjectionEngineAccessor;
import com.ibm.wsspi.injectionengine.ObjectFactoryInfo;
import com.ibm.wsspi.injectionengine.factory.IndirectJndiLookupReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.MBLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResAutoLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResRefReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResourceInfo;
import com.ibm.wsspi.injectionengine.factory.ResourceInfoRefAddr;

/**
 * Provides processing for the Resource annotation and corresponding
 * stanzas in XML ( env-entry, resource-ref, resource-env-ref, and
 * message-destination-ref ). <p>
 */
public class ResourceProcessor extends InjectionProcessor<Resource, Resources> {
    private static final String CLASS_NAME = ResourceProcessor.class.getName();
    private static final TraceComponent tc = Tr.register(ResourceProcessor.class, InjectionConfigConstants.traceString, InjectionConfigConstants.messageFile);

    private ResourceRefConfigList ivResRefList; // F48603.7

    /**
     * Map of Resource Reference Bindings (<ejbBindings>) configured
     * for the resource references of the component.
     **/
    // d446270
    private Map<String, String> ivResRefBindings;
    /**
     * Map of Resource Environment Reference Bindings (<ejbBindings>) configured
     * for the resource environment references of the component.
     **/
    // d446270
    private Map<String, String> ivResEnvRefBindings;
    /**
     * Map of Message Destination Reference Bindings (<ejbBindings>) configured
     * for the message destination references of the component.
     **/
    // d446270
    private Map<String, String> ivMsgDestRefBindings;

    /**
     * Map of env-entry names to values as configured by the deployer.
     */
    private Map<String, String> ivEnvEntryValues; // F743-29779

    /**
     * Map of env-entry names to bindings as configured by the deployer.
     */
    private Map<String, String> ivEnvEntryBindings; // F743-29779

    /**
     * Naming Reference Factory to be used when an ejb-ref or EJB annotation
     * has a binding override. Cached here for performance.
     **/
    // d446270
    private IndirectJndiLookupReferenceFactory ivIndirectLookupFactory;

    /**
     * Naming Reference Factory to be used when a resource-ref or @Resource annotation
     * has an binding. Cached here for performance.
     **/
    // d446270
    private ResRefReferenceFactory ivResRefRefFactory;

    /**
     * Naming Reference Factory to be used when a resource-ref or @Resource annotation
     * does not have a binding. Cached here for performance.
     **/
    // d455334
    private ResAutoLinkReferenceFactory ivResAutoLinkRefFactory;

    /**
     * Naming Reference Factory to be used when a resource-ref or Resource
     * annotation identifies a type with @ManagedBean.
     **/
    // F743-34301
    private MBLinkReferenceFactory ivManagedBeanRefFactory;

    public ResourceProcessor() {
        super(Resource.class, Resources.class);
    }

    @Override
    public void initProcessor() {
        ComponentNameSpaceConfiguration fCompNSConfig = ivNameSpaceConfig;

        // Set local variables; held in this processor for performance.    d446270
        ivIndirectLookupFactory = fCompNSConfig.getResIndirectJndiLookupReferenceFactory();
        ivResRefRefFactory = fCompNSConfig.getResRefReferenceFactory();
        ivResAutoLinkRefFactory = fCompNSConfig.getResAutoLinkReferenceFactory(); // d455334
        ivManagedBeanRefFactory = fCompNSConfig.getMBLinkReferenceFactory(); // F743-34301
        ivResRefBindings = fCompNSConfig.getResourceRefBindings();
        ivResEnvRefBindings = fCompNSConfig.getResourceEnvRefBindings();
        ivMsgDestRefBindings = fCompNSConfig.getMsgDestRefBindings();
        ivEnvEntryValues = fCompNSConfig.getEnvEntryValues(); // F743-29779
        ivEnvEntryBindings = fCompNSConfig.getEnvEntryBindings(); // F743-29779

        ivResRefList = fCompNSConfig.getResourceRefConfigList();
        if (ivResRefList == null) {
            ivResRefList = InternalInjectionEngineAccessor.getInstance().createResourceRefConfigList();
        }
    }

    /**
     * Processes all of tha XML entries that correspond to the @Resource
     * annotation. This includes the following:
     *
     * - <env-entry>
     * - <resource-ref>
     * - <resource-env-ref>
     * - <message-destination-ref>
     *
     * @see InjectionProcessor#processXML()
     */
    @Override
    public void processXML() throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processXML");

        // Process all of the corresponding XML types in no particular
        // order other than it is the same as in prior releases.

        List<? extends EnvEntry> envEntries = ivNameSpaceConfig.getEnvEntries();
        if (envEntries != null) {
            processXMLEnvEntries(envEntries);
        }

        List<? extends ResourceRef> resourceRefs = ivNameSpaceConfig.getResourceRefs();
        if (resourceRefs != null) {
            processXMLResourceRefs(resourceRefs);
        }

        List<? extends ResourceEnvRef> resourceEnvRefs = ivNameSpaceConfig.getResourceEnvRefs();
        if (resourceEnvRefs != null) {
            processXMLResourceEnvRefs(resourceEnvRefs);
        }

        List<? extends MessageDestinationRef> msgDestRefs = ivNameSpaceConfig.getMsgDestRefs();
        if (msgDestRefs != null) {
            processXMLMsgDestRefs(msgDestRefs);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processXML");
    }

    @Override
    public InjectionBinding<Resource> createInjectionBinding(Resource annotation,
                                                             Class<?> instanceClass,
                                                             Member member,
                                                             String jndiName) throws InjectionException {
        return new ResourceInjectionBinding(annotation, ivNameSpaceConfig);
    }

    @Override
    public void resolve(InjectionBinding<Resource> binding) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resolve : " + binding);

        ResourceInjectionBinding resourceBinding = (ResourceInjectionBinding) binding;
        String refJndiName = resourceBinding.getJndiName();
        String refFullJndiName = InjectionScope.denormalize(refJndiName);
        Class<?> injectType = resourceBinding.getInjectionClassType();
        String injectTypeName = resourceBinding.getInjectionClassTypeName();
        ResourceXMLType xmlType = resourceBinding.ivXMLType;

        // Look for an existing ResourceRefConfig.  If none exists, one will be
        // set (possibly the default) before this method returns.
        resourceBinding.ivResRefConfig = ivResRefList.findByName(refJndiName); // F88163

        // Determine if lookup name has really been specified.        F743-22218.3
        String lookupName = resourceBinding.ivLookup;
        if (lookupName != null && lookupName.equals("")) {
            lookupName = null;
        }

        // -----------------------------------------------------------------------
        // Resolve simple environment entries - nothing to resolve.
        // -----------------------------------------------------------------------

        // If the binding is for a simple environment entry (either env-entry in
        // xml, or @Resource on a primitive, primitive wrapper, or String), then
        // the lookup-name needs to be bound as a indirect jndi lookup or the
        // env-entry-value needs to be converted to the proper type to bind
        // directly into java:comp/env. Then, just return the binding (which may
        // have no value to inject).
        //
        // Note: a resource-env-ref of type 'String' will NOT go in this code,
        //       but will instead look for a binding below.                PK63562
        if (xmlType == ResourceXMLType.ENV_ENTRY ||
            (xmlType == ResourceXMLType.UNKNOWN &&
             isEnvEntryType(injectType))) {
            String boundToJndiName = null;

            // First check for a binding.
            if (ivEnvEntryBindings != null) {
                boundToJndiName = ivEnvEntryBindings.get(refJndiName);
                if (boundToJndiName == null && refJndiName != refFullJndiName) {
                    boundToJndiName = ivEnvEntryBindings.get(refFullJndiName);
                }
            }

            // If no binding, check for lookup-name.
            if (boundToJndiName == null) {
                boundToJndiName = lookupName;
            }

            // If binding-name, lookup, and lookup-name were not specified,
            // then convert the value (if present) to the type of the
            // env-entry, so it may be bound directly. This code was moved
            // here from processXMLEnvEntries because the env-entry-type
            // is optional in xml and thus the value may not be properly
            // processed until after annotation processing.            F743-22218.3
            if (boundToJndiName == null) {
                String value = null;

                // First check for a binding value.
                if (ivEnvEntryValues != null) {
                    value = ivEnvEntryValues.get(refJndiName);
                }

                // If no binding, use the value from XML.
                if (value == null) {
                    value = resourceBinding.ivEnvValue;
                }

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "resolve : env-entry - binding value : " + value);

                // Convert the String value from xml to the proper type
                Object injectionObj = resourceBinding.resolveEnvEntryValue(value);

                // May be null if value is bad or was not specified.
                if (injectionObj != null) {
                    // For EJB 1.0 compatibility only
                    if (resourceBinding.getInjectionClassType() == String.class) {
                        collectEjb10Properties(refJndiName, injectionObj);
                    }
                    binding.setObjects(injectionObj, injectionObj);
                    resourceBinding.ivBindingValue = value; // d681743
                }
            }

            if (binding.getBindingObject() == null) {
                // If no binding, lookup-name, or value and this is a ManagedBean
                // with an annotation that explicitly specified a name in
                // java:comp, then use an indirect lookup into the 'caller's comp
                // namespace.                                     d707905, d710771.1
                if (boundToJndiName == null &&
                    ivNameSpaceConfig.getOwningFlow() == ComponentNameSpaceConfiguration.ReferenceFlowKind.MANAGED_BEAN &&
                    binding.getInjectionScope() == InjectionScope.COMP &&
                    !"".equals(binding.getAnnotation().name())) {
                    boundToJndiName = InjectionScope.denormalize(refJndiName); // d726563
                }

                if (boundToJndiName != null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "resolve : env-entry - binding lookup : " + boundToJndiName);

                    Reference ref = createIndirectJndiLookup(resourceBinding,
                                                             boundToJndiName);
                    binding.setObjects(null, ref);
                }
            }

            if (binding.getBindingObject() == null) {
                if (binding.getInjectionScope() != InjectionScope.COMP) {
                    InjectionScopeData isd = getNonCompInjectionScopeData(resourceBinding);
                    ResourceInjectionBinding injectableEnvEntry = (ResourceInjectionBinding) isd.getInjectableEnvEntry(refJndiName);
                    if (injectableEnvEntry != null) {
                        // Set the binding object just to allow injection, and set the
                        // binding value and binding name to allow merging.     F91489
                        // InjectionProcessor.performJavaNameSpaceBinding should not
                        // bind the object, and InjectionBinding.getInjectableObject
                        // should not use the object.
                        String bindingValue = injectableEnvEntry.ivBindingValue;
                        if (bindingValue != null) {
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "resolve : env-entry - binding injectable lookup : " + bindingValue);

                            Object injectionObj = resourceBinding.resolveEnvEntryValue(bindingValue);
                            resourceBinding.setObjects(injectionObj, injectionObj); // F91489
                            resourceBinding.ivBindingValue = bindingValue; // F91489
                        } else {
                            String bindingName = injectableEnvEntry.ivBindingName;

                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "resolve : env-entry - binding injectable lookup : " + bindingName);

                            Reference ref = createIndirectJndiLookup(resourceBinding, bindingName);
                            resourceBinding.setObjects(null, ref); // F91489
                        }
                    }
                }

                // If there are no injection targets, then the spec requires a
                // non-null value or lookup-name, so log a warning; only a warning
                // was provided for EJB 2.1 and earlier, so only a warning is done
                // now. Do not log a warning if the env-entry-value was just bad,
                // as that has already been logged.                        F743-22218.3
                if (!binding.hasAnyInjectionTargets() &&
                    binding.getBindingObject() == null &&
                    resourceBinding.ivEnvValue == null) {
                    String displayName = ivNameSpaceConfig.getDisplayName();
                    Tr.warning(tc, "UNABLE_TO_RESOLVE_THE_ENV_ENTRY_CWNEN0045W",
                               refJndiName, displayName);
                }
            } else {
                InjectionScope scope = binding.getInjectionScope();
                if (scope == InjectionScope.APP &&
                    ivNameSpaceConfig.getJ2EEName() != null &&
                    ivNameSpaceConfig.getJ2EEName().getModule() == null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "resolve : env-entry - adding injectable");

                    // This env-entry has a value, so allow other components with
                    // access to this scope to inject the env-entry.          d702893
                    // We only allow this irregularity for env-entry specified in
                    // application.xml.                                        F91489
                    InjectionScopeData isd = getNonCompInjectionScopeData(resourceBinding);
                    isd.addInjectableEnvEntry(binding);
                }
            }

            if (resourceBinding.ivResRefConfig == null) {
                resourceBinding.ivResRefConfig = InternalInjectionEngineAccessor.getInstance().getDefaultResourceRefConfig();
            }

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "resolve : " + binding);
            return;
        }

        // -----------------------------------------------------------------------
        // If an extension ObjectFactory was registered for this data type, and
        // binding overrides are NOT supported, then create a Reference for that
        // factory, and use the 'generic' info object that contains pretty much
        // all of the interesting metadata for this reference.          F623-841.1
        // -----------------------------------------------------------------------
        ObjectFactoryInfo extensionFactory = getNoOverrideObjectFactory(injectType, injectTypeName);
        if (extensionFactory != null) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "resolve : binding factory : " + extensionFactory);

            Reference ref = null;

            // d675976 - Check for disallowed attributes. d707905 - ignore mappedName

            if (extensionFactory.isAttributeAllowed("lookup")) {
                if (lookupName != null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "resolve : Binding : " + lookupName);
                    ref = createIndirectJndiLookup(resourceBinding, lookupName);
                    binding.setObjects(null, ref);
                }
            } else {
                checkObjectFactoryAttribute(resourceBinding, "lookup", resourceBinding.ivLookup, "");
            }

            checkObjectFactoryAttributes(resourceBinding, extensionFactory);

            if (ref == null) {
                ref = createExtensionFactoryReference(extensionFactory, resourceBinding);
                binding.setReferenceObject(ref, extensionFactory.getObjectFactoryClass());
            }
        }

        // -----------------------------------------------------------------------
        // Provide an IndirectJndiLookup or ResRefJndiLookup binding for
        // all other resources, except env-entry.  This information is obtained
        // from the binding file.                                          d446270
        // -----------------------------------------------------------------------
        else {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "resolve : looking for binding");

            // --------------------------------------------------------------------
            // Bind indirect lookups to resource/resource-env/message-destination
            //
            // Check for a binding in the ResourceRef, ResourceEnvRef and
            // MessageDestinationRef binding maps, and if found, run the binding
            // name through the variable map service (server only), create a
            // Ref Lookup binding, and finally add the reference to the ResRefList.
            //
            // Because of past supported behavior, a binding file match is accepted
            // for any of the different binding types. For example, a resource-ref
            // could be matched to a message-destination-ref binding.
            //
            // Finally, for java:comp/env references, the bindings are first
            // searched with the unqualified name (normalized) and if not found
            // then the full (denormalized) name is also considered.
            // --------------------------------------------------------------------
            Reference ref = resolveFromBinding(resourceBinding, refJndiName);
            if (ref == null && refJndiName != refFullJndiName) {
                ref = resolveFromBinding(resourceBinding, refFullJndiName);
            }

            // --------------------------------------------------------------------
            // Bind indirect jndi using lookup-name                    F743-21028.4
            //
            // If a binding was not specified in ibm-ejb-jar-bnd.xml... then check
            // for <lookup-name> or the lookup annotation attribute and use that
            // as the JNDI binding name.
            // --------------------------------------------------------------------
            if (ref == null && lookupName != null) {
                // The type of indirect lookup depends on whether the object is
                // managed or not. If the type is managed, then full ResRefList
                // support is required, otherwise a simple indirect lookup is all
                // that is needed. Unfortunately, this may not be easy to
                // determine; for example, when injecting into a field of type
                // Object. Because of this, the following code will only use
                // a simple indirect lookup if the reference was defined in xml
                // as something other than <resource-ref>. The more accurate the
                // checking is... the better the performance for more scenarios,
                // and the less likely some other part of WAS accidentally codes
                // to rely on the ResRefList when it shouldn't.         F743-21028.4
                if (xmlType == ResourceXMLType.RESOURCE_REF ||
                    xmlType == ResourceXMLType.UNKNOWN) {
                    // There was no binding, but check for other config by full name
                    if (resourceBinding.ivResRefConfig == null && refJndiName != refFullJndiName) {
                        resourceBinding.ivResRefConfig = ivResRefList.findByName(refFullJndiName);
                    }
                    ref = createResRefJndiLookup(resourceBinding,
                                                 refJndiName,
                                                 resourceBinding.ivLookup);
                } else {
                    ref = createIndirectJndiLookup(resourceBinding,
                                                   resourceBinding.ivLookup);
                }
            }

            // --------------------------------------------------------------------
            // Bind custom ObjectFactory for extension data types.         F623-841
            //
            // If the data type being injected is not one of the Java EE standard
            // data types for @Resource and a binding was not found for it, then
            // check to see if it is one of those data types that another component
            // /specification has provided an ObjectFactory for.  This would be
            // an extension to the Java EE spec, or for a later spec.
            // --------------------------------------------------------------------
            if (ref == null) {
                extensionFactory = getObjectFactoryInfo(injectType, injectTypeName);

                // If an ObjectFactory was registered for this data type, then
                // create a Reference for that factory, and use the 'generic'
                // info object that contains pretty much all of the interesting
                // metadata for this reference.
                if (extensionFactory != null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "resolve : binding factory : " + extensionFactory);

                    // d675976 - Check for disallowed attributes.
                    checkObjectFactoryAttributes(resourceBinding, extensionFactory);

                    ref = createExtensionFactoryReference(extensionFactory, resourceBinding);
                }
            }

            // --------------------------------------------------------------------
            // Bind MBLinkReference for ManagedBean                      F743-34301
            //
            // For injection of a ManagedBean, the target type, or the type on
            // the @Resource annotation must be the ManagedBean class, which is
            // annotated @ManagedBean.
            // --------------------------------------------------------------------
            if (ref == null && isManagedBeanRef(injectType, injectTypeName)) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "resolve : ManagedBean - using MBLink");

                // Use the MBLink Reference factory (which may be overridden) to
                // create the Reference object that will be bound into Naming and
                // resolve the object to inject.
                ref = ivManagedBeanRefFactory.createMBLinkReference(refJndiName, // d655264.1
                                                                    resourceBinding.ivApplication,
                                                                    resourceBinding.ivModule,
                                                                    ivNameSpaceConfig.getDisplayName(),
                                                                    injectTypeName);
            }

            // --------------------------------------------------------------------
            // Bind Indirect for ManagedBean resource in java:comp
            //
            // If no binding or lookup-name and this is a ManagedBean with an
            // annotation that explicitly specified a name in java:comp, then
            // use an indirect lookup into the 'caller's comp namespace.    d707905
            // --------------------------------------------------------------------
            if (ref == null &&
                ivNameSpaceConfig.getOwningFlow() == ComponentNameSpaceConfiguration.ReferenceFlowKind.MANAGED_BEAN &&
                binding.getInjectionScope() == InjectionScope.COMP) {
                if (!"".equals(binding.getAnnotation().name())) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "resolve : ManagedBean resource - binding to consumer java:comp/env");

                    ref = createIndirectJndiLookupInConsumerContext(resourceBinding);
                }
            }

            // --------------------------------------------------------------------
            // Create automatic bindings to resources.
            //
            // No binding was found for the jndiName in the binding file.  The
            // runtime might have a locally defined resource that matches this
            // name, or it might otherwise know how to automatically create a
            // binding for the reference.
            // --------------------------------------------------------------------
            if (ref == null && ivResAutoLinkRefFactory != null) {
                // There was no binding, but check for other config by full name
                if (resourceBinding.ivResRefConfig == null && refJndiName != refFullJndiName) {
                    resourceBinding.ivResRefConfig = ivResRefList.findByName(refFullJndiName);
                }
                ref = ivResAutoLinkRefFactory.createResAutoLinkReference(createResourceInfo(resourceBinding));
            }

            // TODO : After all bindings have been processed, we need to log a
            //        warning for all bindings that were not used.  Perhaps
            //        every time one is used above, it should be removed from
            //        the map, and then warn for all those left.  But, don't
            //        do warnings when WebContainer is calling multiple times
            //        until the last time.

            if (ref != null) {
                // If an extension ObjectFactory is associated with the reference,
                // then pass it along with the reference, so the binding won't
                // need to try and load the class.                        F623-841.1
                if (extensionFactory != null) {
                    binding.setReferenceObject(ref, extensionFactory.getObjectFactoryClass());
                }
                // Otherwise, just set the reference on the binding, and it will
                // load the ObjectFactory from the reference itself.
                else {
                    binding.setObjects(null, ref);
                }
            } else {
                // Add the jndiName to the missing bindings list.  The list
                // will be displayed when all InjectionBindids have been processed.
                super.ivMissingBindings.add(refJndiName); //d435329
            }
        }

        if (resourceBinding.ivResRefConfig == null) {
            resourceBinding.ivResRefConfig = InternalInjectionEngineAccessor.getInstance().getDefaultResourceRefConfig();
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resolve : " + binding);
    }

    /**
     * Bind indirect lookups to resource/resource-env/message-destination from
     * the value from the binding file.
     *
     * Check for a binding in the ResourceRef, ResourceEnvRef and MessageDestinationRef
     * binding maps, and if found, run the binding name through the variable map service
     * (server only), create a Ref Lookup binding, and finally add the reference to the
     * ResRefList.
     *
     * Because of past supported behavior, a binding file match is accepted for any of
     * the different binding types. For example, a resource-ref could be matched to a
     * message-destination-ref binding.
     *
     * @param resourceBinding the resource binding to resolve
     * @param refJndiName     normalized or denormalized ref name
     * @return the Reference object that resolves the binding or null
     * @throws InjectionException if an error occurs creating the reference
     */
    private Reference resolveFromBinding(ResourceInjectionBinding resourceBinding, String refJndiName) throws InjectionException {
        String boundToJndiName = null;
        Reference ref = null;

        // --------------------------------------------------------------------
        // Bind indirect jndi lookups to Resource-Refs
        //
        // Check for a binding in the ResourceRef Binding map, and if
        // found, run the binding name through the variable map service
        // (server only), create a ResourceRef Lookup binding, and finally
        // add the reference to the ResRefList.
        // --------------------------------------------------------------------
        if (ivResRefBindings != null) {
            boundToJndiName = ivResRefBindings.get(refJndiName);
            if (boundToJndiName != null) {
                // This call uses the variable map service, creates the
                // reference and adds to the ResRefList.
                ref = createResRefJndiLookup(resourceBinding,
                                             refJndiName,
                                             boundToJndiName);
            }
        }

        // --------------------------------------------------------------------
        // Bind indirect jndi lookups to Resource-Env-Refs
        //
        // If a binding was not found yet, try the Resource Environment Ref
        // Bindings, and if found, bind an Indirect Jndi Lookup.
        // --------------------------------------------------------------------
        if (ref == null && ivResEnvRefBindings != null) {
            boundToJndiName = ivResEnvRefBindings.get(refJndiName);
            if (boundToJndiName != null) {
                ref = createIndirectJndiLookup(resourceBinding,
                                               boundToJndiName);
            }
        }

        // --------------------------------------------------------------------
        // Bind indirect jndi lookups to Message-Destination-Refs     LI2281-25
        //
        // If a binding was not found yet, try the Message Destination Ref
        // Bindings, and if found, bind an Indirect Jndi Lookup.
        // --------------------------------------------------------------------
        if (ref == null && ivMsgDestRefBindings != null) {
            boundToJndiName = ivMsgDestRefBindings.get(refJndiName);
            if (boundToJndiName != null) {
                ref = createIndirectJndiLookup(resourceBinding,
                                               boundToJndiName);
            }
        }
        return ref;
    }

    private InjectionScopeData getNonCompInjectionScopeData(ResourceInjectionBinding binding) {
        InjectionScope scope = binding.getInjectionScope();
        MetaData md = scope == InjectionScope.APP ? ivNameSpaceConfig.getApplicationMetaData() : scope == InjectionScope.MODULE ? ivNameSpaceConfig.getModuleMetaData() : null;
        AbstractInjectionEngine injectionEngine = (AbstractInjectionEngine) InjectionEngineAccessor.getInstance();
        return injectionEngine.getInjectionScopeData(md);
    }

    /**
     * Common code to create the special indirect JNDI lookup Reference for
     * a managed resource reference, and add it to the ResRefList.
     *
     * @param binding         the specific resource injection binding being processed.
     * @param refJndiName     JNDI name of the resource reference.
     * @param boundToJndiName JNDI name to re-direct the refJndiName to.
     *
     * @return an indirect JNDI lookup Reference.
     **/
    // F743-21028.4
    private Reference createResRefJndiLookup(ResourceInjectionBinding binding,
                                             String refJndiName,
                                             String boundToJndiName) throws InjectionException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Binding resource ref " + refJndiName +
                         " to resource " + boundToJndiName);

        // -----------------------------------------------------------------------
        // Bind an indirect JNDI reference using the Resource jndi name.
        // When a lookup is performed on the ResourceRef, an indirect
        // lookup will then be performed on the jndi name of the actual
        // Resource.
        // -----------------------------------------------------------------------

        InjectionScope scope = binding.getInjectionScope();
        ResourceRefConfig resRef = getResourceRefConfig(binding, refJndiName, boundToJndiName);
        return ivResRefRefFactory.createResRefJndiLookup(ivNameSpaceConfig, scope, resRef); // F743-29417, d705480
    }

    private ResourceRefConfig getResourceRefConfig(ResourceInjectionBinding binding,
                                                   String refJndiName,
                                                   String boundToJndiName) {
        Resource resourceAnnotation = binding.getAnnotation();
        String description = resourceAnnotation.description();
        String classType = binding.getInjectionClassTypeName(); // F743-32443
        int resAuthType = resourceAnnotation.authenticationType() == AuthenticationType.CONTAINER ? ResourceRef.AUTH_CONTAINER : ResourceRef.AUTH_APPLICATION;
        int resSharingScope = resourceAnnotation.shareable() ? ResourceRef.SHARING_SCOPE_SHAREABLE : ResourceRef.SHARING_SCOPE_UNSHAREABLE;

        binding.ivBindingName = boundToJndiName; // F88163

        // Create and add a ResRef object to the ResRefList for
        // this resource reference.
        ResourceRefConfig resRef = binding.ivResRefConfig; // F88163
        if (resRef == null) {
            resRef = ivResRefList.findOrAddByName(refJndiName);
            binding.ivResRefConfig = resRef;
        }

        // F743-29779 - Use ResRefImpl setters.
        resRef.setDescription(description); //d531303.1
        resRef.setJNDIName(boundToJndiName);
        resRef.setType(classType); // d641277
        resRef.setResAuthType(resAuthType); // d641277
        resRef.setSharingScope(resSharingScope); // d641277

        return resRef;
    }

    /**
     * Common code to create the indirect JNDI lookup Reference for
     * a non-managed resource reference.
     *
     * @param binding         the specific resource injection binding being processed.
     * @param refJndiName     JNDI name of the resource reference.
     * @param boundToJndiName JNDI name to re-direct the refJndiName to.
     * @param type            the XML type of the resource reference.
     *
     * @return an indirect JNDI lookup Reference.
     **/
    // F743-21028.4
    private Reference createIndirectJndiLookup(ResourceInjectionBinding binding,
                                               String boundToJndiName) throws InjectionException {
        // -----------------------------------------------------------------------
        // Bind an indirect JNDI reference using the reference jndi name.
        // When a lookup is performed on the reference, an indirect lookup will
        // then be performed on the jndi name of the actual resource.
        // -----------------------------------------------------------------------
        String jndiName = binding.getJndiName();
        String classType = binding.getInjectionClassTypeName(); // d741827

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Binding " + binding.ivXMLType + " " + jndiName +
                         " (" + classType + ") to resource " + boundToJndiName);

        binding.ivBindingName = boundToJndiName; // F88163
        if (binding.ivResRefConfig != null) {
            binding.ivResRefConfig.setJNDIName(boundToJndiName); // F88163
        }

        return ivIndirectLookupFactory.createIndirectJndiLookup(jndiName, boundToJndiName, classType);
    }

    /**
     * Common code to create the indirect JNDI lookup Reference for
     * a resource reference that is mapped to the java:comp/env JNDI
     * context of the consuming component.
     *
     * @param binding the specific resource injection binding being processed.
     *
     * @return an indirect JNDI lookup Reference.
     **/
    private Reference createIndirectJndiLookupInConsumerContext(ResourceInjectionBinding binding) throws InjectionException {
        // -----------------------------------------------------------------------
        // Bind an indirect JNDI reference using the reference jndi name.
        // When a lookup is performed on the reference, an indirect lookup will
        // then be performed on the jndi name of the caller's resource.
        // -----------------------------------------------------------------------
        String jndiName = binding.getJndiName();
        String classType = binding.getInjectionClassTypeName();
        String boundToJndiName = jndiName.startsWith("java:") ? jndiName : "java:comp/env/" + jndiName;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Binding " + binding.ivXMLType + " " + jndiName +
                         " (" + classType + ") to resource " + boundToJndiName);

        binding.ivBindingName = boundToJndiName;
        if (binding.ivResRefConfig != null) {
            binding.ivResRefConfig.setJNDIName(boundToJndiName);
        }

        return ivIndirectLookupFactory.createIndirectJndiLookupInConsumerContext(jndiName, boundToJndiName, classType);
    }

    /**
     * Check attributes for registered ObjectFactory's.
     *
     * @param resourceBinding
     * @param extensionFactory
     * @throws InjectionConfigurationException
     */
    private void checkObjectFactoryAttributes(ResourceInjectionBinding resourceBinding,
                                              ObjectFactoryInfo extensionFactory) // d675976
                    throws InjectionConfigurationException {
        Resource resourceAnnotation = resourceBinding.getAnnotation();

        if (!extensionFactory.isAttributeAllowed("authenticationType")) {
            checkObjectFactoryAttribute(resourceBinding, "authenticationType",
                                        resourceAnnotation.authenticationType(), AuthenticationType.CONTAINER);
        }

        if (!extensionFactory.isAttributeAllowed("shareable")) {
            checkObjectFactoryAttribute(resourceBinding, "shareable",
                                        resourceAnnotation.shareable(), true);
        }
    }

    private void checkObjectFactoryAttribute(ResourceInjectionBinding resourceBinding,
                                             String attributeName,
                                             Object value,
                                             Object defaultValue) // d675976
                    throws InjectionConfigurationException {
        if (value != null && !value.equals(defaultValue)) {
            String name = resourceBinding.getJndiName();
            String injectTypeName = resourceBinding.getInjectionClassTypeName();

            Tr.error(tc, "INVALID_OBJECT_FACTORY_ATTRIBUTE_CWNEN0071E",
                     name,
                     resourceBinding.ivComponent,
                     resourceBinding.ivModule,
                     resourceBinding.ivApplication,
                     injectTypeName,
                     attributeName,
                     value);

            throw new InjectionConfigurationException("The " + name +
                                                      " reference for the " + resourceBinding.ivComponent +
                                                      " component in the " + resourceBinding.ivModule +
                                                      " module in the " + resourceBinding.ivApplication +
                                                      " application has the " + injectTypeName +
                                                      " type and a value for the " + attributeName +
                                                      " attribute that is not valid: " + value);
        }
    }

    /**
     * Creates a Reference for a binding from a registered ObjectFactory.
     *
     * @param extensionFactory the object factory info
     * @param resourceBinding  the resource binding
     * @return the reference
     */
    private Reference createExtensionFactoryReference(ObjectFactoryInfo extensionFactory,
                                                      ResourceInjectionBinding resourceBinding) {
        String className = extensionFactory.getObjectFactoryClass().getName();
        Reference ref = new Reference(resourceBinding.getInjectionClassTypeName(), className, null);

        if (extensionFactory.isRefAddrNeeded()) {
            ref.add(new ResourceInfoRefAddr(createResourceInfo(resourceBinding)));
        }

        return ref;
    }

    /**
     * Creates a ResourceInfo for a binding.
     *
     * @param resourceBinding the binding
     * @return the info
     */
    private ResourceInfo createResourceInfo(ResourceInjectionBinding resourceBinding) {
        J2EEName j2eeName = ivNameSpaceConfig.getJ2EEName();
        Resource resourceAnnotation = resourceBinding.getAnnotation();

        return new ResourceInfo(j2eeName == null ? null : j2eeName.getApplication(), j2eeName == null ? null : j2eeName.getModule(),
                        // TODO: This should be j2eeName.getComponent(), but at least
                        // SIP is known to improperly depend on this being the module
                        // name without ".war".
                        ivNameSpaceConfig.getDisplayName(), resourceBinding.getJndiName(), resourceBinding.getInjectionClassTypeName(), resourceAnnotation.authenticationType(), resourceAnnotation.shareable(), resourceBinding.ivLink, getResourceRefConfig(resourceBinding,
                                                                                                                                                                                                                                                              resourceBinding.getJndiName(),
                                                                                                                                                                                                                                                              null));
    }

    /**
     * Determines if the specified jndiName is for an EJB 1.0 environment
     * property, and if so, adds the specified injection object to the
     * EJB Environment properties with the appropriate property name. <p>
     *
     * EJB 1.0 environment properties are only for env-entries of type String.
     * This methods should not be called for other types. <p>
     *
     * Here is an example of what this would look like in xml:
     *
     * <env-entry>
     * <env-entry-name>ejb10-properties/hello</env-entry-name>
     * <env-entry-type>java.lang.String</env-entry-type>
     * <env-entry-value>howdy</env-entry-value>
     * </env-entry>
     *
     * And to access in the EJB, the code would be:
     *
     * Properties env = ejbContext.getEnvironment();
     * String value = env.getProperty("hello");
     *
     * @param jndiName        env-entry-name value
     * @param injectionObject env-entry value
     *
     * @throws InjectionException if a problem occurs parsing the jndiName
     */
    // F743-22218.3
    private void collectEjb10Properties(String jndiName, Object injectionObject) throws InjectionException {
        // For EJB 1.0 compatibility only
        // According to spec remove the first element
        // of the name (ejb10-properties)
        final String prefix = "ejb10-properties/";
        if (jndiName.startsWith(prefix)) {
            Properties envProperties = ivNameSpaceConfig.getEnvProperties();
            if (envProperties != null) {
                envProperties.put(jndiName.substring(prefix.length()), injectionObject);
            }
        }
    }

    /**
     * Returns true if the specified type is a ManagedBean. <p>
     *
     * Supports the type in class form (normal server) and String form
     * (server side client support). <p>
     *
     * @param injectType     injection type; may be null if name specified.
     * @param injectTypeName injection type; may be null if class specified.
     */
    // d702400
    private boolean isManagedBeanRef(Class<?> injectType, String injectTypeName) {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "isManagedBeanRef: " + injectType + ", " + injectTypeName);

        boolean result;

        if (injectType != null && injectType != Object.class) {
            result = injectType.isAnnotationPresent(ManagedBean.class);
        } else {
            Set<String> mbClassNames = ivNameSpaceConfig.getManagedBeanClassNames();
            result = mbClassNames != null && mbClassNames.contains(injectTypeName);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "isManagedBeanRef: " + result);
        return result;
    }

    private static String getDescription(Describable desc) {
        List<Description> descs = desc.getDescriptions();
        return descs.isEmpty() ? null : descs.get(0).getValue();
    }

    /**
     * Process : <env-entry>
     *
     * For lookup and injection of :
     * - Primitive Wrappers ( Character, Integer, Boolean,
     * Double, Byte, Short, Long, Float )
     * - String
     **/
    private void processXMLEnvEntries(List<? extends EnvEntry> envEntries) throws InjectionException {
        for (EnvEntry envEntry : envEntries) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "processing : " + envEntry);

            // If XML has previously been read and an injection binding with the same
            // jndi name has been created, get the current injection binding and merge
            // the new env entry into it.
            String jndiName = envEntry.getName();
            InjectionBinding<Resource> injectionBinding = ivAllAnnotationsCollection.get(jndiName);
            if (injectionBinding != null) {
                ((ResourceInjectionBinding) injectionBinding).merge(envEntry);
            } else {
                String mappedName = envEntry.getMappedName();
                String description = getDescription(envEntry);
                String lookup = envEntry.getLookupName(); // F743-21028.4
                if (lookup != null) {
                    lookup = lookup.trim();
                }

                // Previously, the env-entry-value was converted to the
                // env-entry-type at this time, however, the env-entry-type
                // is an optional parameter, and so may need to be obtained from
                // any corresponding annotation(s).  The conversion of the value
                // to the correct type is now done during 'resolve', after all
                // metadata has been collected.                         F743-22218.3

                List<InjectionTarget> targets = envEntry.getInjectionTargets();

                // Note: env-entry-type will be determined in binding constructor,
                //       and updated in annotation at that time.          F743-25853

                ResourceImpl resourceAnnotation = new ResourceImpl(jndiName, null, mappedName, description, lookup); // F743-21028.4
                injectionBinding = new ResourceInjectionBinding(resourceAnnotation, envEntry, ivNameSpaceConfig); // d479669
                addInjectionBinding(injectionBinding);

                // d654054
                // As part of adding an InjectionTarget, validation is done to ensure
                // that the specified injection type is compatible with the type of
                // variable being injected into.
                //
                // If the user omits the <type> setting from their XML, then the
                // EnvEntry given to us by WCCM has a null value for the type, and
                // that will cause us to NPE during the compatibility check.  To
                // avoid this, we ensure that we always have a non-null injection
                // type passed into that check, and if the user omitted the <type>
                // from the XML, then we default to java.lang.Object
                //
                // The other XML types (MessageDestRef, ResourceRef, ResourceEnvRef)
                // use the .loadTypeClass() method to ensure that we've got a valid
                // injection type.  However, we can't use that method directly, because
                // unlike the other XML types, the EnvEntry does not have a method
                // that returns the injection type as a String (which is the required
                // input to the .loadTypeClass() method).  Rather, the EnvEntry type
                // is represented by an int, which is converted into the corresponding
                // Class by the .getEnvEntryType() method...and after we're done calling
                // that, we've already got the Class, so there is no point in calling
                // the .loadTypeClass()

                // Support multiple injection targets
                if (!targets.isEmpty()) {
                    Class<?> injectionType = injectionBinding.getInjectionClassType();

                    for (InjectionTarget target : targets) {
                        String targetClassName = target.getInjectionTargetClassName();
                        String targetName = target.getInjectionTargetName();
                        injectionBinding.addInjectionTarget(injectionType, targetName, targetClassName);
                    }
                } // if targets
            } // else binding with same name not found
        } // for ivEnvEntries
    } // processXMLEnvEntries()

    /**
     * Process : <resource-ref>
     *
     * For lookup and injection of resource manager connection factories. <p>
     *
     * Includes injection and binding of the ORB. <p>
     *
     * Resource manager connection factories may be configured as shared or
     * not shared, and may have CONTAINER or APPLICATION authentication.
     **/
    private void processXMLResourceRefs(List<? extends ResourceRef> resourceRefs) throws InjectionException {
        for (ResourceRef resourceRef : resourceRefs) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "processing : " + resourceRef);

            //If XML has previously been read and an injection binding with the same
            //jndi name has been created, get the current injection binding and merge
            //the new env entry into it.
            String jndiName = resourceRef.getName();
            InjectionBinding<Resource> injectionBinding = ivAllAnnotationsCollection.get(jndiName);
            if (injectionBinding != null) {
                ((ResourceInjectionBinding) injectionBinding).merge(resourceRef);
            } else {
                Class<?> injectionType = null;
                String injectionTypeName = null;
                String targetName = null;
                String targetClassName = null;

                AuthenticationType authenticationType = convertAuthToEnum(resourceRef.getAuthValue());
                int resSharingScope = resourceRef.getSharingScopeValue();
                Boolean shareable = null;
                if (resSharingScope != ResourceRef.SHARING_SCOPE_UNSPECIFIED) {
                    shareable = resSharingScope == ResourceRef.SHARING_SCOPE_SHAREABLE;
                }

                String mappedName = resourceRef.getMappedName();
                String description = getDescription(resourceRef);
                String lookup = resourceRef.getLookupName(); // F743-21028.4
                if (lookup != null) {
                    lookup = lookup.trim();
                }

                List<InjectionTarget> targets = resourceRef.getInjectionTargets();
                try {
                    injectionTypeName = resourceRef.getType();
                    injectionType = loadTypeClass(injectionTypeName, jndiName); //d463452 d476227.1
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "targetType : " + injectionType);

                    Resource resourceAnnotation = new ResourceImpl(jndiName, injectionType == null ? Object.class : injectionType, // d701306.1
                                    authenticationType, shareable, mappedName, description, lookup); // F743-21028.4
                    injectionBinding = new ResourceInjectionBinding(resourceAnnotation, injectionTypeName, lookup, ResourceXMLType.RESOURCE_REF, ivNameSpaceConfig); // d479669
                    addInjectionBinding(injectionBinding);

                    // Support multiple injection targets
                    if (!targets.isEmpty()) {
                        for (InjectionTarget target : targets) {
                            targetClassName = target.getInjectionTargetClassName();
                            targetName = target.getInjectionTargetName();

                            // Would have been nice if an exception were thrown
                            // here if the type was NOT one of the supported types
                            // for resource-ref, but since that has not been done
                            // historically, it cannot be done now without the
                            // possibility of breaking an existing application.

                            injectionBinding.addInjectionTarget(injectionType, targetName, targetClassName);
                        }
                    }
                } catch (Exception e) {
                    FFDCFilter.processException(e, CLASS_NAME + ".processXMLResourceRefs",
                                                "898", this, new Object[] { resourceRef, jndiName, injectionBinding, targetName, targetClassName });
                    InjectionException icex;
                    icex = new InjectionException("Failed to process the XML for " + "resource-ref " + resourceRef, e);
                    Tr.error(tc, "FAILED_TO_PROCESS_XML_CWNEN0032E", "resource-ref", resourceRef);
                    throw icex;
                }
            }
        }
    }

    /**
     * Process : <resource-env-ref>
     *
     * For lookup and injection of administerd objects :
     * - EJBContext ( SessionContext or MessageDrivenContext )
     * - TimerService
     * - UserTransaction
     * - TransactionSynchronizationRegistry
     * - administered objects associated with resources
     *
     * Differs from resource references in that resource env references are
     * not shareable and do not require authentication.
     **/
    private void processXMLResourceEnvRefs(List<? extends ResourceEnvRef> resourceEnvRefs) throws InjectionException {
        for (ResourceEnvRef resourceEnvRef : resourceEnvRefs) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "processing : " + resourceEnvRef);

            //If XML has previously been read and an injection binding with the same
            //jndi name has been created, get the current injection binding and merge
            //the new env entry into it.
            String jndiName = resourceEnvRef.getName();
            InjectionBinding<Resource> injectionBinding = ivAllAnnotationsCollection.get(jndiName);
            if (injectionBinding != null) {
                ((ResourceInjectionBinding) injectionBinding).merge(resourceEnvRef);
            } else {
                Class<?> injectionType = null; // d367834.10
                String injectionTypeName = null;
                String targetName = null;
                String targetClassName = null;

                String mappedName = resourceEnvRef.getMappedName();
                String description = getDescription(resourceEnvRef);
                String lookup = resourceEnvRef.getLookupName(); // F743-21028.4
                if (lookup != null) {
                    lookup = lookup.trim();
                }

                List<InjectionTarget> targets = resourceEnvRef.getInjectionTargets();
                try {
                    injectionTypeName = resourceEnvRef.getTypeName();
                    injectionType = loadTypeClass(injectionTypeName, jndiName); // d476227 d476227.1
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "injectionType : " + injectionType);

                    Resource resourceAnnotation = new ResourceImpl(jndiName, injectionType == null ? Object.class : injectionType, // d701306.1
                                    mappedName, description, lookup); // F743-21028.4
                    injectionBinding = new ResourceInjectionBinding(resourceAnnotation, injectionTypeName, lookup, ResourceXMLType.RESOURCE_ENV_REF, ivNameSpaceConfig); // d479669
                    addInjectionBinding(injectionBinding);

                    // Support multiple injection targets
                    if (!targets.isEmpty()) {
                        for (InjectionTarget target : targets) {
                            targetClassName = target.getInjectionTargetClassName();
                            targetName = target.getInjectionTargetName();

                            // Would have been nice if an exception were thrown
                            // here if the type was NOT one of the supported types
                            // for resoruce-env-ref, but since that has not been done
                            // historically, it cannot be done now without the
                            // possibility of breaking an existing application.

                            injectionBinding.addInjectionTarget(injectionType, targetName, targetClassName);
                        }
                    }
                } catch (Exception e) {
                    FFDCFilter.processException(e, CLASS_NAME + ".processXMLResourceEnvRefs",
                                                "454", this, new Object[] { resourceEnvRef, jndiName, injectionBinding, targetName, targetClassName });
                    InjectionException icex;
                    icex = new InjectionException("Failed to process the XML for " + "resource-env-ref " + resourceEnvRef, e);
                    Tr.error(tc, "FAILED_TO_PROCESS_XML_CWNEN0032E", "resource-env-ref", resourceEnvRef);
                    throw icex;
                }
            }
        }
    }

    /**
     * Process : <message-destination-ref>
     *
     * For lookup and injection of message destination objects.
     *
     * Differs from resource references in that message destination references
     * are not shareable and do not require authentication.
     *
     * @throws InjectionException
     **/
    private void processXMLMsgDestRefs(List<? extends MessageDestinationRef> msgDestRefs) throws InjectionException {
        for (MessageDestinationRef msgDestRef : msgDestRefs) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "processing : " + msgDestRef);

            //If XML has previously been read and an injection binding with the same
            //jndi name has been created, get the current injection binding and merge
            //the new env entry into it.
            String jndiName = msgDestRef.getName();
            InjectionBinding<Resource> injectionBinding = ivAllAnnotationsCollection.get(jndiName);
            if (injectionBinding != null) {
                ((ResourceInjectionBinding) injectionBinding).merge(msgDestRef);
            } else {

                Class<?> injectionType = null; // d367834.10
                String targetName = null;
                String targetClassName = null;

                //The MessageDestinationType parameter is "optional" according to spec 16.9.1.3
                String msgDestType = msgDestRef.getType();
                String description = getDescription(msgDestRef);

                String mappedName = msgDestRef.getMappedName();
                String lookup = msgDestRef.getLookupName(); // F743-21028.4
                if (lookup != null) {
                    lookup = lookup.trim();
                }

                List<InjectionTarget> targets = msgDestRef.getInjectionTargets();

                try {
                    injectionType = loadTypeClass(msgDestType, jndiName); // d476227 d476227.1
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "targetType : " + injectionType);

                    Resource resourceAnnotation = new ResourceImpl(jndiName, injectionType == null ? Object.class : injectionType, // d701306.1
                                    mappedName, description, lookup); // F743-21028.4
                    if (msgDestRef.getLink() != null) {
                        injectionBinding = new ResourceInjectionBinding(resourceAnnotation, msgDestRef, lookup, ivNameSpaceConfig);
                        addInjectionBinding(injectionBinding);
                    } else {
                        injectionBinding = new ResourceInjectionBinding(resourceAnnotation, msgDestType, lookup, ResourceXMLType.MESSAGE_DESTINATION_REF, ivNameSpaceConfig); // d479669
                        addInjectionBinding(injectionBinding);
                    }

                    // Support multiple injection targets
                    if (!targets.isEmpty()) {
                        for (InjectionTarget target : targets) {
                            targetClassName = target.getInjectionTargetClassName();
                            targetName = target.getInjectionTargetName();

                            // Would have been nice if an exception were thrown
                            // here if the type was NOT one of the supported types
                            // for message-dest-ref, but since that has not been done
                            // historically, it cannot be done now without the
                            // possibility of breaking an existing application.

                            injectionBinding.addInjectionTarget(injectionType, targetName, targetClassName);
                        }
                    }
                } catch (Exception e) {
                    FFDCFilter.processException(e, CLASS_NAME + ".processXMLMsgDestRefs",
                                                "1077", this, new Object[] { msgDestRef, jndiName, injectionBinding, targetName, targetClassName });
                    InjectionException icex;
                    icex = new InjectionException("Failed to process the XML for " + "message-destination-ref " + msgDestRef, e); //d645991
                    Tr.error(tc, "FAILED_TO_PROCESS_XML_CWNEN0032E", "message-destination-ref", msgDestRef); //d645991
                    throw icex;
                }
            }
        }
    }

    @Override
    public String getJndiName(Resource annotation) {
        return annotation.name();
    }

    @Override
    public Resource[] getAnnotations(Resources annotation) {
        return annotation.value();
    }

    // d455334 start
    /**
     * Check if the class is one of the simple environment entry types
     * (primitive, primitive wrapper, String, Class, or Enum).
     *
     * Note: If EE5Compatibility is enabled, then Class and Enum will
     * not be considered simple environment entry types.
     *
     * @param clazz class to check for type.
     * @return true if the class is a simple environment entry type.
     **/
    private static boolean isEnvEntryType(Class<?> clazz) {
        if (clazz.isPrimitive() ||
            clazz == String.class ||
            clazz == Integer.class ||
            clazz == Long.class ||
            clazz == Boolean.class ||
            clazz == Byte.class ||
            clazz == Character.class ||
            clazz == Float.class ||
            clazz == Double.class ||
            clazz == Short.class) {
            return true;
        }

        // If EE5Compatibility has not been enabled, then Class and Enum
        // are also both env-entries starting in EJB 3.1.       F743-25853 d657801
        if (!EE5Compatibility &&
            (clazz == Class.class ||
             clazz.isEnum())) {
            return true;
        }

        return false;
    } // d455334 stop

    //d463452 added method
    /**
     * Internal method to isolate the class loading logic. <p>
     *
     * Returns the loaded type class using the component specific ClassLoader,
     * or null if the specified class name was null or the empty string, if the
     * class loader was null, or if the class fails to load. <p>
     *
     * @param className name of the class to load
     * @param refName   name of the resource reference this class is
     *                      associated with, for ras.
     **/
    private Class<?> loadTypeClass(String className, String refName) // d476227.1
                    throws InjectionConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "loadTypeClass : " + className);

        ClassLoader classLoader = ivNameSpaceConfig.getClassLoader();
        if (className == null || className.equals("") || classLoader == null) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "loadTypeClass : null");
            return null; // d701306.1
        }

        Class<?> loadedClass;

        try {
            loadedClass = classLoader.loadClass(className);
        } catch (ClassNotFoundException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".loadTypeClass",
                                        "1142", this, new Object[] { className });

            // Older applications may have inadvertantly coded 'type' values
            // on the xml reference that are invalid.... but because they
            // were ignored, and only the bindings used, the app worked
            // anyway. To allow these older applications to continue working
            // a warning will be logged for this, rather than a failure.  d476227.1

            String module = ivNameSpaceConfig.getModuleName();
            Tr.warning(tc, "RESOURCE_TYPE_NOT_FOUND_CWNEN0046W", className, refName, module); // d479669
            if (isValidationFailable()) {
                throw new InjectionConfigurationException("CWNEN0046W: The " + className +
                                                          " type specified on the resource-ref, resource-env-ref, or" +
                                                          " message-destination-ref with the " + refName +
                                                          " name in the " + module +
                                                          " module could not be loaded.");
            }

            loadedClass = null; // d701306.1
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "loadTypeClass : " + loadedClass);

        return loadedClass;
    }

    /**
     * Converts the String Authentiction type in XML to the Enum authentication
     * type of the @Resource annotation. <p>
     *
     * The Authentication type in XML must be either 'Application' or
     * 'Container', and the default is 'Container'. <p>
     *
     * If the specified resAuthType is null, the default of CONTAINER
     * will be returned. <p>
     *
     * @parm resAuthType Resource Auth type from XML (WCCM)
     *
     * @return the @Resouce AuthenticationType enum value
     **/
    // d543514
    static AuthenticationType convertAuthToEnum(int resAuthType) {
        AuthenticationType authType = AuthenticationType.CONTAINER;

        if (resAuthType == ResourceRef.AUTH_APPLICATION) {
            authType = AuthenticationType.APPLICATION;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "convertAuthToEnum : " + resAuthType + " -> " + authType);

        return authType;
    }
}
