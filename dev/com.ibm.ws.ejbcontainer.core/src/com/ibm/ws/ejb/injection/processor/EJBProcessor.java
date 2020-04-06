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
package com.ibm.ws.ejb.injection.processor;

import java.lang.reflect.Member;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.EJBs;
import javax.naming.Reference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejb.injection.annotation.EJBImpl;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionScope;
import com.ibm.wsspi.injectionengine.factory.EJBLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.IndirectJndiLookupReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.OverrideReferenceFactory;

public class EJBProcessor extends InjectionProcessor<EJB, EJBs> {
    static final TraceComponent tc = Tr.register(EJBProcessor.class,
                                                 InjectionConfigConstants.traceString,
                                                 InjectionConfigConstants.messageFile);

    /**
     * Naming Reference Factory to be used when an ejb-ref or EJB annotation
     * has a binding override. Cached here for performance.
     **/
    // d440604.2
    private IndirectJndiLookupReferenceFactory ivIndirectLookupFactory;

    /**
     * Naming Reference Factory to be used when an ejb-ref or EJB annotation
     * has an ejb-link/beanName or will use auto-link. Cached here for performance.
     **/
    // d440604.2
    private EJBLinkReferenceFactory ivEJBLinkRefFactory;

    public EJBProcessor() {
        super(EJB.class, EJBs.class);
    }

    @Override
    public void initProcessor() {
        // Set local variables; held in this processor for performance.  d440604.2
        ivIndirectLookupFactory = ivNameSpaceConfig.getIndirectJndiLookupReferenceFactory();
        ivEJBLinkRefFactory = ivNameSpaceConfig.getEJBLinkReferenceFactory();
    }

    /**
     * Processes {@link ComponentNameSpaceConfiguration#getEJBRefs} and {@link ComponentNameSpaceConfiguration#getEJBLocalRefs}.
     *
     * @throws InjectionException if an error is found processing the XML.
     **/
    // d429866
    @Override
    public void processXML() throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processXML : " + this);

        List<? extends EJBRef> ejbRefs = ivNameSpaceConfig.getJNDIEnvironmentRefs(EJBRef.class);
        if (ejbRefs != null) {
            for (EJBRef ejbRef : ejbRefs) {
                //If XML has previously been read and an injection binding with the same
                //jndi name has been created, get the current injection binding and merge
                //the new EJB Ref into it.
                String jndiName = ejbRef.getName();
                InjectionBinding<EJB> injectionBinding = ivAllAnnotationsCollection.get(jndiName);
                if (injectionBinding != null) {
                    ((EJBInjectionBinding) injectionBinding).merge(ejbRef);
                } else {
                    List<Description> descs = ejbRef.getDescriptions();
                    String lookupName = ejbRef.getLookupName();
                    EJB ejbAnnotation = new EJBImpl(ejbRef.getName(), null, ejbRef.getLink(), ejbRef.getMappedName(), descs.isEmpty() ? null : descs.get(0).getValue(), lookupName != null ? lookupName.trim() : null);

                    EJBInjectionBinding ejbBinding = new EJBInjectionBinding(ejbAnnotation, ejbRef, ivNameSpaceConfig);

                    // Process any injection-targets that may be specified.    d429866.1
                    // Add all of those found to the EJBInjectionBinding.        d432816
                    List<InjectionTarget> targets = ejbRef.getInjectionTargets();

                    if (!targets.isEmpty()) {
                        for (InjectionTarget target : targets) {
                            Class<?> injectionType = ejbBinding.getInjectionClassTypeWithException();
                            String injectionName = target.getInjectionTargetName();
                            String injectionClassName = target.getInjectionTargetClassName();
                            ejbBinding.addInjectionTarget(injectionType, // d446474
                                                          injectionName,
                                                          injectionClassName);
                        }
                    }

                    // TODO : Support method injection and type compatibility
                    addInjectionBinding(ejbBinding);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processXML : " + this);
    }

    // d429866
    @Override
    public void resolve(InjectionBinding<EJB> binding) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resolve : " + binding);

        Reference ref = null;
        EJBInjectionBinding ejbBinding = (EJBInjectionBinding) binding;
        String refJndiName = ejbBinding.getJndiName();
        String interfaceName = ejbBinding.getInjectionClassTypeName();

        // -----------------------------------------------------------------------
        // First, check the ejb-ref binding map to see if a binding was provided
        // for this reference/injection. If a binding was provided, then an
        // IndirectJndiLookup Reference is bound into naming... to re-direct
        // the lookup to the 'bound' name.                               d440604.2
        // -----------------------------------------------------------------------

        String boundToJndiName = ejbBinding.ivBindingName; // d681743

        // -----------------------------------------------------------------------
        // If a JNDI name was not provided in the binding file, then also check
        // the new lookup attribute on @EJB (or lookup-name in xml).  F743-21028.4
        // -----------------------------------------------------------------------
        if (boundToJndiName == null) {
            boundToJndiName = ejbBinding.ivLookup;

            // An empty string may be 'normal' here... since it is the annotation
            // default, but also a customer may specify it in XML to disable the
            // annotation setting. If empty, use ejb-link or auto-link.
            if (boundToJndiName != null && boundToJndiName.equals("")) {
                boundToJndiName = null;
            }
        }

        // -----------------------------------------------------------------------
        // If override reference factories have been registered for this processor
        // then call each factory until one of them provides an override (i.e. a
        // non-null value). If none of the factories provide an override, then
        // fall through and perform normal resolve processing.          F1339-9050
        // -----------------------------------------------------------------------
        // F743-32443 - We can only check for override processors if we have
        // an interface name and a class loader.  Otherwise, we defer the
        // checking and use a different object factory.  See below.
        if (ivOverrideReferenceFactories != null &&
            interfaceName != null && ejbBinding.ivClassLoader != null) {
            Class<?> injectType = ejbBinding.getInjectionClassTypeWithException();
            for (OverrideReferenceFactory<EJB> factory : ivOverrideReferenceFactories) {
                // d696076 - Use J2EEName for runtime data structures.  We know
                // J2EEName is non-null with non-null app/module because injectType
                // is checked for null above, which means we have a class loader,
                // which means this is not a non-java:comp code path.
                J2EEName j2eeName = ivNameSpaceConfig.getJ2EEName();
                ref = factory.createReference(j2eeName.getApplication(),
                                              j2eeName.getModule(),
                                              ejbBinding.ivBeanName,
                                              refJndiName,
                                              injectType,
                                              boundToJndiName,
                                              ejbBinding.getAnnotation());
                if (ref != null) {
                    binding.setObjects(null, ref);

                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "resolve", binding);
                    return;
                }
            }
        }

        // -----------------------------------------------------------------------
        // If a binding was provided, then an IndirectJndiLookup Reference is
        // bound into naming... to re-direct the lookup to the 'bound' name.
        // -----------------------------------------------------------------------
        if (boundToJndiName != null) {
            if (ejbBinding.ivEjbLocalRef &&
                !boundToJndiName.startsWith("java:") &&
                !boundToJndiName.startsWith("ejblocal:")) {
                boundToJndiName = "ejblocal:".concat(boundToJndiName);
            }

            String beanInterfaceName = ejbBinding.getInjectionClassTypeName();

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "resolve : binding : " + refJndiName +
                             " -> " + boundToJndiName + " : type = " + beanInterfaceName);

            ref = ivIndirectLookupFactory.createIndirectJndiLookup(refJndiName,
                                                                   boundToJndiName,
                                                                   beanInterfaceName);
            binding.setObjects(null, ref); // F48603.4

            // Save the binding and IndirectLookupFactory.  If the binding name is
            // later found to be ambiguous, we can try to disambiguate by
            // appending the interface name.
            ejbBinding.ivBoundToJndiName = boundToJndiName;
            ejbBinding.ivIndirectLookupFactory = ivIndirectLookupFactory;

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "resolve", binding);
            return;
        }

        // -----------------------------------------------------------------------
        // Second, no binding, so this ejb ref will be resolved either through
        // ejblink/beanName or auto-link. Process the ref data and bind an
        // EJBLinkObjectFactory Reference.
        // -----------------------------------------------------------------------

        if (ejbBinding.getInjectionScope() == InjectionScope.GLOBAL) {
            // Auto-link is supported for java:global EJB refs if ejb-link/beanName
            // is present and the referenced bean is declared in the same application.
            if (ejbBinding.ivBeanName == null || ivNameSpaceConfig.getJ2EEName() == null) {
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "resolve: missing binding");

                super.ivMissingBindings.add(refJndiName);
                return;
            }
        }

        if (ejbBinding.ivBeanInterface ||
            ejbBinding.ivHomeInterface) {
            String beanInterfaceName;
            String homeInterfaceName;

            if (ejbBinding.ivHomeInterface) {
                beanInterfaceName = null;
                homeInterfaceName = interfaceName;
            } else {
                beanInterfaceName = interfaceName != null ? interfaceName : "java.lang.Object"; // d668376
                homeInterfaceName = null;
            }

            // Use the EJBLink Reference factory (which may be overridden) to
            // create the Reference object that will be bound into Naming and
            // to resolve the object to inject.                           d440604.2
            J2EEName j2eeName = ivNameSpaceConfig.getJ2EEName();
            ref = ivEJBLinkRefFactory.createEJBLinkReference(refJndiName,
                                                             j2eeName.getApplication(),
                                                             j2eeName.getModule(),
                                                             j2eeName.getComponent(),
                                                             ejbBinding.ivBeanName,
                                                             beanInterfaceName,
                                                             homeInterfaceName,
                                                             ejbBinding.ivEjbLocalRef,
                                                             ejbBinding.ivEjbRef);

            /**
             * If the injection type is specified and we have no class loader, we
             * were unable to check OverrideReferenceFactory. Wrap the reference
             * just created in another reference that will check the override
             * factories when looked up.
             */
            if (ivOverrideReferenceFactories != null &&
                interfaceName != null && ejbBinding.ivClassLoader == null) {
                throw new UnsupportedOperationException();
            }

            binding.setObjects(null, ref);
        } else {
            // This may occur if the customer has incorrectly coded either no
            // home or interface class, or the class names were coded incorrectly
            // and do not exist / cannot be loaded.                         d448539

            Tr.error(tc, "EJB_REF_OR_EJB_LOCAL_REF_IS_NOT_SPECIFIED_CORRECTLY_CWNEN0026E", refJndiName);
            throw new InjectionConfigurationException("The " + refJndiName + " Enterprise JavaBean (EJB) home and " +
                                                      "remote or local-home and local elements are either missing " +
                                                      "or could not be resolved");
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resolve", binding);
    }

    @Override
    public InjectionBinding<EJB> createInjectionBinding(EJB annotation,
                                                        Class<?> instanceClass,
                                                        Member member,
                                                        String jndiName) throws InjectionException {
        return new EJBInjectionBinding(annotation, jndiName, ivNameSpaceConfig);
    }

    /**
     * Returns the 'name' attribute of the EJB annotation. <p>
     *
     * The name attribute, if present, is the Jndi Name where the
     * injection object is bound into naming. <p>
     *
     * Although all injection annotations have a 'name' attribute,
     * the attribute is not present in the base annotation class,
     * so each subclass processor must extract the value. <p>
     *
     * @param annotation the EJB annotation to extract the name from.
     **/
    // d432816
    @Override
    public String getJndiName(EJB annotation) {
        return annotation.name();
    }

    @Override
    public EJB[] getAnnotations(EJBs annotation) {
        return annotation.value();
    }

}
