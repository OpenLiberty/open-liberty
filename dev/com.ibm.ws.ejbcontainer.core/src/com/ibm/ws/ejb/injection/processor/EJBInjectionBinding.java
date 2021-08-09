/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
import java.rmi.Remote;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.naming.CompositeName;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.rmi.PortableRemoteObject;

import org.omg.CORBA.portable.ObjectImpl;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ejbcontainer.AmbiguousEJBReferenceException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejb.injection.annotation.EJBImpl;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
import com.ibm.wsspi.injectionengine.RecursiveInjectionException;
import com.ibm.wsspi.injectionengine.factory.IndirectJndiLookupReferenceFactory;

/**
 * EJB specific Injection Binding. <p>
 *
 * This injection binding holds all of the information necessary to
 * properly bind ejb references, resolve them (including ejb-link
 * and auto-link) and inject them. <p>
 *
 * EJB references defined via annotations (@EJB) and xml (ejb-ref
 * and ejb-local-ref) are both supported. <p>
 **/
public class EJBInjectionBinding extends InjectionBinding<EJB>
{
    private static final String CLASS_NAME = EJBInjectionBinding.class.getName();

    private static final TraceComponent tc = Tr.register
                    (EJBInjectionBinding.class,
                     InjectionConfigConstants.traceString,
                     InjectionConfigConstants.messageFile);

    // --------------------------------------------------------------------------
    // Meta data common between annotations and xml
    // --------------------------------------------------------------------------
    boolean ivBeanInterface; // true if beanInterface or remote or local
                             // has been specified                    F743-32443
    String ivBeanName; // beanName parameter of annotation |
                       // ejb-link from xml
    String ivLookup; // lookup parameter of annotation |
                     // lookup-name from xml                F743-21028.4
    boolean ivBeanNameOrLookupInXml; // F743-21028.4
    Class<?> ivBeanNameClass; // class with beanName annotation         d638111.1

    // --------------------------------------------------------------------------
    // Meta data specific to xml
    // --------------------------------------------------------------------------
    int ivBeanType; // EJBRef.getTypeValue()
    boolean ivHomeInterface; // true if home or local-home (XML)      F743-32443
    boolean ivEjbLocalRef; // true if ejb-local-ref (XML)
    boolean ivEjbRef; // true if ejb-ref (i.e. a remote ref in XML)

    // --------------------------------------------------------------------------
    // Meta data of the component for which the reference is defined
    // --------------------------------------------------------------------------
    String ivApplication; // Application name of the referencing bean,
                          // NOT the referenced bean.
    String ivModule; // Module name of the referencing bean,
                     // NOT the referenced bean.
    ClassLoader ivClassLoader; // Application class loader.               d464232

    // --------------------------------------------------------------------------
    // Meta data when a binding is used instead of ejb-link / auto-link.  d452621
    // --------------------------------------------------------------------------
    String ivBindingName; // the user-specified binding
    String ivBoundToJndiName; // the user-specified binding or lookup value
    IndirectJndiLookupReferenceFactory ivIndirectLookupFactory;

    public EJBInjectionBinding(String jndiName, ComponentNameSpaceConfiguration compNSConfig)
    {
        super(null, compNSConfig);
        setJndiName(jndiName);
    }

    /**
     * Constructor for use with an @EJB annotation. <p>
     *
     * This constructor will extract the necessary information from an @EJB
     * annotation and the current component's configuration to properly
     * perform binding into the java:comp namespace, instance resolution,
     * and injection. <p>
     *
     * @param ejb the @EJB annotation
     * @param compNSConfig the current components name space configuration
     *
     * @throws InjectionException when a problem is detected with the
     *             annotation.
     **/
    public EJBInjectionBinding(EJB ejb, String jndiName, ComponentNameSpaceConfiguration compNSConfig) // F743-33811
    throws InjectionException
    {
        super(ejb, compNSConfig);

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "<init> : " + ejb);

        setJndiName(jndiName); // F743-33811

        ivBeanName = ejb.beanName();
        if (ivBeanName != null && ivBeanName.equals("")) // d429866.2
            ivBeanName = null;
        ivLookup = ejb.lookup(); // F743-21028.4
        if (ivLookup != null && ivLookup.equals(""))
            ivLookup = null;
        ivBeanType = EJBRef.TYPE_UNSPECIFIED; // unknown for annotations

        ivApplication = compNSConfig.getApplicationName();
        ivModule = compNSConfig.getModuleName();
        ivClassLoader = compNSConfig.getClassLoader(); // d464232

        setBindingName(); // d681743

        // It is an error to specify both 'beanName' and 'lookup'.    F743-21028.4
        // Unless a binding has been given.                                d661212
        if (ivBeanName != null && ivLookup != null && ivBindingName == null)
        {
            // Error - conflicting beanName/lookup values
            String component = compNSConfig.getDisplayName();
            Tr.error(tc, "CONFLICTING_ANNOTATION_VALUES_CWNEN0054E",
                     new Object[] { component,
                                   ivModule,
                                   ivApplication,
                                   "beanName/lookup",
                                   "@EJB",
                                   "name",
                                   getJndiName(),
                                   ivBeanName,
                                   ivLookup });
            String exMsg = "The " + component + " bean in the " +
                           ivModule + " module of the " + ivApplication +
                           " application has conflicting configuration data" +
                           " in source code annotations. Conflicting " +
                           "beanName/lookup" + " attribute values exist for multiple " +
                           "@EJB" + " annotations with the same " +
                           "name" + " attribute value : " + getJndiName() +
                           ". The conflicting " + "beanName/lookup" +
                           " attribute values are " + ivBeanName +
                           " and " + ivLookup + ".";
            throw new InjectionConfigurationException(exMsg);
        }

        // Set the 'beanInterface' of the annotation as the class being injected.
        // Even if not specified, the annotation will return the default of Object.
        setInjectionClassType(ejb.beanInterface()); // d668376

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "<init> : " + this);
    }

    /**
     * Constructor for use with an ejb-ref or ejb-local-ref entry. <p>
     *
     * This constructor will extract the necessary information from an
     * ejb-ref entry and the current component's configuration to properly
     * perform binding into the java:comp namespace, instance resolution,
     * and injection. <p>
     *
     * @param ejb the @EJB annotation
     * @param ejbRef the ejb-ref or ejb-local-ref DD object
     * @param compNSConfig the current components name space configuration
     *
     * @throws InjectionException when a problem is detected with the ejb-ref.
     **/
    public EJBInjectionBinding(EJB ejb, EJBRef ejbRef,
                               ComponentNameSpaceConfiguration compNSConfig)
        throws InjectionException
    {
        super(ejb, compNSConfig);

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "<init> : " + ejbRef);

        setJndiName(ejbRef.getName());

        ivBeanName = ejbRef.getLink();
        ivLookup = ejbRef.getLookupName(); // F743-21028.4
        if (ivLookup != null) {
            ivLookup = ivLookup.trim();
        }
        ivBeanNameOrLookupInXml = (ivBeanName != null ||
                        ivLookup != null); // F743-21028.4

        ivBeanType = ejbRef.getTypeValue();
        ivEjbLocalRef = ejbRef.getKindValue() == EJBRef.KIND_LOCAL;
        ivEjbRef = ejbRef.getKindValue() == EJBRef.KIND_REMOTE;

        ivApplication = compNSConfig.getApplicationName();
        ivModule = compNSConfig.getModuleName();
        ivClassLoader = compNSConfig.getClassLoader(); // d464232

        setBindingName(); // d681743

        // It is an error to specify both 'ejb-link' and 'lookup-name'. F743-21028.4
        // Unless a binding has been given.                             F743-33811
        if (ivBeanName != null && ivLookup != null && ivBindingName == null)
        {
            // Error - conflicting ejb-link/lookup-name values
            String component = compNSConfig.getDisplayName();
            String elementType = ivEjbRef ? "ejb-ref" : "ejb-local-ref";
            Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                     new Object[] { component,
                                   ivModule,
                                   ivApplication,
                                   "ejb-link/lookup-name",
                                   elementType,
                                   "ejb-ref-name",
                                   getJndiName(),
                                   ivBeanName,
                                   ivLookup });
            String exMsg = "The " + component + " bean in the " + ivModule +
                           " module of the " + ivApplication + " application" +
                           " has conflicting configuration data in the XML" +
                           " deployment descriptor. Conflicting " +
                           "ejb-link/lookup-name" + " element values exist for" +
                           " multiple " + elementType + " elements with the same " +
                           "ejb-ref-name" + " element value : " + getJndiName() +
                           ". The conflicting " + "ejb-link/lookup-name" +
                           " element values are \"" + ivBeanName + "\" and \"" +
                           ivLookup + "\".";
            throw new InjectionConfigurationException(exMsg);
        }

        setXMLBeanInterface(ejbRef.getHome(), ejbRef.getInterface()); // F743-32443

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "<init> : " + this);
    }

    /**
     * Sets the beanInterface as specified by XML.
     */
    private void setXMLBeanInterface(String homeInterfaceName, String interfaceName) // F743-32443
    throws InjectionException
    {
        // If a home or business interface was specified in XML, then set that as
        // the injection type.  Both may be null if the XML just provides an
        // override of an annotation to add <ejb-link>... in which case the
        // injection class type will be set when the annotation is processed.

        // For performance, there is no need to load these classes until first
        // accessed, which might be never if there is a binding file, so just the
        // name will be set here. getInjectionClassType() is then overridden to load
        // the class when needed. Prior to EJB 3.0 these interfaces were completely
        // ignored, so this behavior allows EJB 2.1 apps to continue to function
        // even if they had garbage in their deployment descriptor.        d739281
        if (homeInterfaceName != null && homeInterfaceName.length() != 0) // d668376
        {
            ivHomeInterface = true;
            setInjectionClassTypeName(homeInterfaceName);

            if (isValidationLoggable())
            {
                loadClass(homeInterfaceName, isValidationFailable());
                loadClass(interfaceName, isValidationFailable());
            }
        }
        else if (interfaceName != null && interfaceName.length() != 0) // d668376
        {
            ivBeanInterface = true;
            setInjectionClassTypeName(interfaceName);

            if (isValidationLoggable())
            {
                loadClass(interfaceName, isValidationFailable());
            }
        }
    }

    /**
     * Returns true if the user has configured a binding for this reference.
     */
    private void setBindingName() // d681743
    throws InjectionException
    {
        Map<String, String> ejbRefBindings = ivNameSpaceConfig.getEJBRefBindings();
        if (ejbRefBindings != null)
        {
            ivBindingName = ejbRefBindings.get(getJndiName());

            if (ivBindingName != null && ivBindingName.equals(""))
            {
                ivBindingName = null;
                Tr.warning(tc, "EJB_BOUND_TO_EMPTY_STRING_CWNEN0025W");
                if (isValidationFailable()) // fail if enabled       F743-14449
                {
                    InjectionConfigurationException icex = new InjectionConfigurationException
                                    ("The " + getJndiName() + " EJB reference in the " + ivModule +
                                     " module of the " + ivApplication + " application has been" +
                                     " bound to the empty string in the global Java Naming and Directory Interface (JNDI) namespace.");
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(tc, "resolve : " + icex);
                    throw icex;
                }
            }
        }
    }

    /**
     * Adds an InjectionTarget for the specified member (field or method). <p>
     *
     * Overridden here to identify the class where the beanName attribute
     * was defined on a method or field annotation. <p>
     *
     * @param member field or method reflection object.
     *
     * @throws InjectionException if a configuration problem is detected
     *             relating the member to the binding.
     */
    // d638111.1
    @Override
    public void addInjectionTarget(Member member)
                    throws InjectionException
    {
        // If the beanName attribute was found in the constructor or merge
        // method, then save the class of where it was located.
        if (ivBeanName != null && ivBeanNameClass == null) {
            ivBeanNameClass = member.getDeclaringClass();
        }

        super.addInjectionTarget(member);
    }

    /**
     * Merges the configuration information of an annotation with a binding
     * object created from previously processed XML or annotations. <p>
     *
     * The may occur when there is an XML override of an annotation, or
     * there are multiple annotations defined with the same name (i.e.
     * a multiple target injection scenario).
     *
     * This method will implement/enforce the deployment descriptor override
     * rules defined in the EJB Specification:
     * <ul>
     * <li> The relevant deployment descriptor entry is located based on the
     * JNDI name used with the annotation (either defaulted or provided
     * explicitly).
     * <li> The type specified in the deployment descriptor via the remote,
     * local, remote-home, or local-home element and any bean referenced
     * by the ejb-link element must be assignable to the type of the field
     * or property or the type specified by the beanInterface element
     * of the EJB annotation.
     * <li> The description, if specified, overrides the description element
     * of the annotation.
     * <li> The injection target, if specified, must name exactly the annotated
     * field or property method.
     * </ul>
     *
     * @param annotation the EJB annotation to be merged
     * @param instanceClass the class containing the annotation
     * @param member the Field or Method associated with the annotation;
     *            null if a class level annotation.
     **/
    // d432816
    @Override
    public void merge(EJB annotation, Class<?> instanceClass, Member member)
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "merge : " + annotation + ", " + member);

        // Calling setInjectionClassType is done for 3 reasons:
        // 1 - to validate the element is assignable as per the spec
        // 2 - to set the Injection Class Type, which may be a subclass of what
        //     is configured in XML and which will be used in resolve
        // 3 - to set the HomeInterface class, if beanInterface extends EJBHome
        //     or EJBLocalHome.
        Class<?> beanInterface = annotation.beanInterface();
        setInjectionClassType(beanInterface); // d668376

        // If neither "ejb-link" nor "lookup-name" has been defined in XML,
        // then use the value from annotations. If there are multiple annotations
        // with the same name, then the values must match.            F743-21028.4
        if (!ivBeanNameOrLookupInXml)
        {
            String beanName = annotation.beanName();
            // An empty string is the default and not considered a specified value.
            if (beanName != null && !(beanName.equals("")))
            {
                if (ivBeanName == null)
                {
                    ivBeanName = beanName;
                }
                else
                {
                    // If the beanName was found previously on another annotation,
                    // then throw an exception if the current annotation doesn't
                    // have the same beanName value. However, in WAS 7.0, overriding
                    // was allowed by a subclass... so if the current annotation is
                    // on a parent class of where beanName was found... then don't
                    // throw the exception.                                 d638111.1
                    Class<?> beanNameClass = (member != null) ? member.getDeclaringClass()
                                    : Object.class;
                    if (!ivBeanName.equals(beanName) &&
                        (ivBeanNameClass == null ||
                         ivBeanNameClass == beanNameClass ||
                        !beanNameClass.isAssignableFrom(ivBeanNameClass)))
                    {
                        // Error - conflicting beanName values between annotations
                        String component = ivNameSpaceConfig.getDisplayName();
                        Tr.error(tc, "CONFLICTING_ANNOTATION_VALUES_CWNEN0054E",
                                 new Object[] { component,
                                               ivModule,
                                               ivApplication,
                                               "beanName",
                                               "@EJB",
                                               "name",
                                               getJndiName(),
                                               ivBeanName,
                                               beanName });
                        String exMsg = "The " + component + " bean in the " +
                                       ivModule + " module of the " + ivApplication +
                                       " application has conflicting configuration data" +
                                       " in source code annotations. Conflicting " +
                                       "beanName" + " attribute values exist for multiple " +
                                       "@EJB" + " annotations with the same " +
                                       "name" + " attribute value : " + getJndiName() +
                                       ". The conflicting " + "beanName" +
                                       " attribute values are " + ivBeanName +
                                       " and " + beanName + ".";
                        throw new InjectionConfigurationException(exMsg);
                    }
                }
            }

            String lookup = annotation.lookup();
            // An empty string is the default and not considered a specified value.
            if (lookup != null && !(lookup.equals("")))
            {
                if (ivLookup == null)
                {
                    ivLookup = lookup;
                }
                else
                {
                    if (!(ivLookup.equals(lookup)))
                    {
                        // Error - conflicting lookup values between annotations
                        String component = ivNameSpaceConfig.getDisplayName();
                        Tr.error(tc, "CONFLICTING_ANNOTATION_VALUES_CWNEN0054E",
                                 new Object[] { component,
                                               ivModule,
                                               ivApplication,
                                               "lookup",
                                               "@EJB",
                                               "name",
                                               getJndiName(),
                                               ivLookup,
                                               lookup });
                        String exMsg = "The " + component + " bean in the " +
                                       ivModule + " module of the " + ivApplication +
                                       " application has conflicting configuration data" +
                                       " in source code annotations. Conflicting " +
                                       "lookup" + " attribute values exist for multiple " +
                                       "@EJB" + " annotations with the same " +
                                       "name" + " attribute value : " + getJndiName() +
                                       ". The conflicting " + "lookup" +
                                       " attribute values are " + ivLookup +
                                       " and " + lookup + ".";
                        throw new InjectionConfigurationException(exMsg);
                    }
                }
            }

            // It is an error to specify both 'beanName' and 'lookup'. F743-21028.4
            // Unless a binding has been given.                             d661212
            if (ivBeanName != null && ivLookup != null && ivBindingName == null)
            {
                // Error - conflicting beanName/lookup values
                String component = ivNameSpaceConfig.getDisplayName();
                Tr.error(tc, "CONFLICTING_ANNOTATION_VALUES_CWNEN0054E",
                         new Object[] { component,
                                       ivModule,
                                       ivApplication,
                                       "beanName/lookup",
                                       "@EJB",
                                       "name",
                                       getJndiName(),
                                       ivBeanName,
                                       ivLookup });
                String exMsg = "The " + component + " bean in the " +
                               ivModule + " module of the " + ivApplication +
                               " application has conflicting configuration data" +
                               " in source code annotations. Conflicting " +
                               "beanName/lookup" + " attribute values exist for multiple " +
                               "@EJB" + " annotations with the same " +
                               "name" + " attribute value : " + getJndiName() +
                               ". The conflicting " + "beanName/lookup" +
                               " attribute values are " + ivBeanName +
                               " and " + ivLookup + ".";
                throw new InjectionConfigurationException(exMsg);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "merge", this);
    }

    /**
     * Extract the fields from the EjbRef, and verify they match the values in the current
     * binding object and/or annotation exactly. If they do indeed match, add all the
     * InjectionTargets on the EjbRef parameter to the current binding. The code takes into
     * account the possibility of duplicates InjectionTargets and will only use one in case
     * where they duplicated between the two ref definitions.
     *
     * @param ejbRef
     * @throws InjectionException
     */
    public void merge(EJBRef ejbRef)
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "merge : " + ejbRef);

        EJBImpl curAnnotation = (EJBImpl) this.getAnnotation();
        String jndiName = ejbRef.getName();
        String curJndiName = curAnnotation.name();
        int beanType = ejbRef.getTypeValue();
        int curBeanType = this.ivBeanType;
        String homeInterfaceName = ejbRef.getHome();
        String beanInterfaceName = ejbRef.getInterface();
        String beanName = ejbRef.getLink();
        String curBeanName = ivBeanName; // 649980
        String lookup = ejbRef.getLookupName(); // F743-21028.4
        if (lookup != null) {
            lookup = lookup.trim();
        }
        String mappedName = (ejbRef.getMappedName() == null) ? "" : ejbRef.getMappedName();
        String curMappedName = curAnnotation.mappedName();
        int kind = ejbRef.getKindValue();
        boolean ejbLocalRef = kind == EJBRef.KIND_LOCAL;

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "new=" + jndiName + ":" + beanType + ":" + homeInterfaceName + "/" + beanInterfaceName + ":" + beanName + ":" + mappedName + ":" + lookup);

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "cur=" + curJndiName + ":" + curBeanType + ":" + getInjectionClassTypeName() + "," + ivHomeInterface + ":" + curBeanName + ":" + curMappedName + ":"
                         + ivLookup);

        // If there are 2 EJB Refs, but one is local, and the other remote,
        // then this is an incompatibility... and thus an error.           d479669
        if ((ivEjbRef && ejbLocalRef) ||
            (ivEjbLocalRef && kind == EJBRef.KIND_REMOTE))
        {
            Tr.error(tc, "CONFLICTING_XML_TYPES_CWNEN0051E", new Object[]
            { ivNameSpaceConfig.getDisplayName(),
             ivNameSpaceConfig.getModuleName(),
             ivNameSpaceConfig.getApplicationName(),
             "ejb-ref-name",
             jndiName,
             "ejb-ref",
             "ejb-local-ref" });
            String exMsg = "The " + ivNameSpaceConfig.getDisplayName() +
                           " bean in the " + ivNameSpaceConfig.getModuleName() +
                           " module of the " + ivNameSpaceConfig.getApplicationName() +
                           " application has conflicting configuration data in the XML" +
                           " deployment descriptor. Conflicting element types exist with the same " +
                           "ejb-ref-name" + " element value : " + jndiName +
                           ". The conflicting element types are " + "ejb-ref" + " and " +
                           "ejb-local-ref" + ".";
            throw new InjectionConfigurationException(exMsg);
        }

        //The type parameter is "optional"
        if (beanType != EJBRef.TYPE_UNSPECIFIED &&
            curBeanType != EJBRef.TYPE_UNSPECIFIED)
        {
            // check that value from xml is a subclasss, if not throw an error
            if (beanType != curBeanType)
            {
                String elementType = ejbLocalRef ? "ejb-local-ref" : "ejb-ref";
                String curBeanTypeName = curBeanType == EJBRef.TYPE_SESSION ? "Session" : "Entity";
                String beanTypeName = beanType == EJBRef.TYPE_SESSION ? "Session" : "Entity";
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E", new Object[]
                { ivNameSpaceConfig.getDisplayName(),
                 ivNameSpaceConfig.getModuleName(),
                 ivNameSpaceConfig.getApplicationName(),
                 "ejb-ref-type",
                 elementType,
                 "ejb-ref-name",
                 jndiName,
                 curBeanTypeName,
                 beanTypeName }); // d479669
                String exMsg = "The " + ivNameSpaceConfig.getDisplayName() +
                               " bean in the " + ivNameSpaceConfig.getModuleName() +
                               " module of the " + ivNameSpaceConfig.getApplicationName() +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "ejb-ref-type" +
                               " element values exist for multiple " + elementType +
                               " elements with the same " + "ejb-ref-name" + " element value : " +
                               jndiName + ". The conflicting " + "ejb-ref-type" +
                               " element values are " + curBeanTypeName + " and " +
                               beanTypeName + ".";
                throw new InjectionConfigurationException(exMsg);
            }
        }
        else if (beanType != EJBRef.TYPE_UNSPECIFIED &&
                 curBeanType == EJBRef.TYPE_UNSPECIFIED)
        {
            this.ivBeanType = beanType;
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "ivBeanType = " + this.ivBeanType);
        }

        //The homeInterface parameter is "optional"
        if (homeInterfaceName != null && homeInterfaceName.length() != 0) // d668376
        {
            if (ivClassLoader == null) // F743-32443
            {
                setInjectionClassTypeName(homeInterfaceName);
                ivHomeInterface = true;
            }
            else
            {
                Class<?> homeInterface = loadClass(homeInterfaceName, false);
                if (ivHomeInterface)
                {
                    Class<?> curHomeInterface = getInjectionClassType();
                    Class<?> mostSpecific = mostSpecificClass(homeInterface, curHomeInterface);
                    if (mostSpecific == null)
                    {
                        String elementType = ejbLocalRef ? "ejb-local-ref" : "ejb-ref";
                        String elementAttr = ejbLocalRef ? "local-home" : "home";
                        Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E", new Object[]
                        { ivNameSpaceConfig.getDisplayName(),
                         ivNameSpaceConfig.getModuleName(),
                         ivNameSpaceConfig.getApplicationName(),
                         elementAttr,
                         elementType,
                         "ejb-ref-name",
                         jndiName,
                         curHomeInterface.getName(),
                         homeInterface.getName() }); // d479669
                        String exMsg = "The " + ivNameSpaceConfig.getDisplayName() +
                                       " bean in the " + ivNameSpaceConfig.getModuleName() +
                                       " module of the " + ivNameSpaceConfig.getApplicationName() +
                                       " application has conflicting configuration data in the XML" +
                                       " deployment descriptor. Conflicting " + elementAttr +
                                       " element values exist for multiple " + elementType +
                                       " elements with the same " + "ejb-ref-name" + " element value : " +
                                       jndiName + ". The conflicting " + elementAttr +
                                       " element values are " + curHomeInterface.getName() + " and " +
                                       homeInterface.getName() + ".";
                        throw new InjectionConfigurationException(exMsg);
                    }

                    setInjectionClassType(mostSpecific);
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "ivHomeInterface = " + this.ivHomeInterface);
                }
                else
                {
                    setInjectionClassType(homeInterface);
                    ivHomeInterface = true;
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "ivHomeInterface = " + this.ivHomeInterface);
                }
            }
        }
        //The beanInterface parameter is "optional"
        else if (beanInterfaceName != null && beanInterfaceName.length() != 0) // d668376
        {
            ivBeanInterface = true;
            if (ivClassLoader == null) // F743-32443
            {
                setInjectionClassTypeName(beanInterfaceName);
            }
            else if (!ivHomeInterface)
            {
                Class<?> beanInterface = loadClass(beanInterfaceName, false);
                Class<?> curBeanInterface = getInjectionClassType();
                Class<?> mostSpecific = mostSpecificClass(beanInterface, curBeanInterface);
                if (mostSpecific == null)
                {
                    String elementType = ejbLocalRef ? "ejb-local-ref" : "ejb-ref";
                    String elementAttr = ejbLocalRef ? "local" : "remote";
                    Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E", new Object[]
                    { ivNameSpaceConfig.getDisplayName(),
                     ivNameSpaceConfig.getModuleName(),
                     ivNameSpaceConfig.getApplicationName(),
                     elementAttr,
                     elementType,
                     "ejb-ref-name",
                     jndiName,
                     curBeanInterface.getName(),
                     beanInterface.getName() }); // d479669
                    String exMsg = "The " + ivNameSpaceConfig.getDisplayName() +
                                   " bean in the " + ivNameSpaceConfig.getModuleName() +
                                   " module of the " + ivNameSpaceConfig.getApplicationName() +
                                   " application has conflicting configuration data in the XML" +
                                   " deployment descriptor. Conflicting " + elementAttr +
                                   " element values exist for multiple " + elementType +
                                   " elements with the same " + "ejb-ref-name" + " element value : " +
                                   jndiName + ". The conflicting " + elementAttr +
                                   " element values are " + curBeanInterface.getName() + " and " +
                                   beanInterface.getName() + ".";
                    throw new InjectionConfigurationException(exMsg);
                }

                setInjectionClassType(beanInterface);
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "ivBeanInterface = " + beanInterface);
            }
        }

        //The mappedName parameter is "optional"
        if (curAnnotation.ivIsSetMappedName && mappedName != null)
        {
            if (!curMappedName.equals(mappedName))
            {
                String elementType = ejbLocalRef ? "ejb-local-ref" : "ejb-ref";
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E", new Object[]
                { ivNameSpaceConfig.getDisplayName(),
                 ivNameSpaceConfig.getModuleName(),
                 ivNameSpaceConfig.getApplicationName(),
                 "mapped-name",
                 elementType,
                 "ejb-ref-name",
                 jndiName,
                 curMappedName,
                 mappedName }); // d479669
                String exMsg = "The " + ivNameSpaceConfig.getDisplayName() +
                               " bean in the " + ivNameSpaceConfig.getModuleName() +
                               " module of the " + ivNameSpaceConfig.getApplicationName() +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "mapped-name" +
                               " element values exist for multiple " + elementType +
                               " elements with the same " + "ejb-ref-name" + " element value : " +
                               jndiName + ". The conflicting " + "mapped-name" +
                               " element values are " + curMappedName + " and " +
                               mappedName + ".";
                throw new InjectionConfigurationException(exMsg);
            }
        }
        else if (mappedName != null && !curAnnotation.ivIsSetMappedName)
        {
            curAnnotation.ivMappedName = mappedName;
            curAnnotation.ivIsSetMappedName = true;
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "ivMappedName = " + mappedName);
        }

        //The beanName parameter is "optional"
        if (beanName != null && curAnnotation.ivIsSetBeanName)
        {
            if (!curBeanName.equals(beanName))
            {
                String elementType = ejbLocalRef ? "ejb-local-ref" : "ejb-ref";
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E", new Object[]
                { ivNameSpaceConfig.getDisplayName(),
                 ivNameSpaceConfig.getModuleName(),
                 ivNameSpaceConfig.getApplicationName(),
                 "ejb-link",
                 elementType,
                 "ejb-ref-name",
                 jndiName,
                 curBeanName,
                 beanName }); // d479669
                String exMsg = "The " + ivNameSpaceConfig.getDisplayName() +
                               " bean in the " + ivNameSpaceConfig.getModuleName() +
                               " module of the " + ivNameSpaceConfig.getApplicationName() +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "ejb-link" +
                               " element values exist for multiple " + elementType +
                               " elements with the same " + "ejb-ref-name" + " element value : " +
                               jndiName + ". The conflicting " + "ejb-link" +
                               " element values are " + curBeanName + " and " +
                               beanName + ".";
                throw new InjectionConfigurationException(exMsg);
            }
        }
        else if (beanName != null && curBeanName == null)
        {
            curAnnotation.ivBeanName = beanName;
            curAnnotation.ivIsSetBeanName = true;
            ivBeanName = beanName;
            ivBeanNameOrLookupInXml = true; // F743-21028.4
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "ivBeanName = " + this.ivBeanName);
        }

        // -----------------------------------------------------------------------
        // Merge : lookup - "optional parameter                       F743-21028.4
        //
        // If present in XML, even if the empty string (""), it will override
        // any setting via annotations. An empty string would effectively turn
        // off this setting, so auto-link would be used instead.
        //
        // When an ejb-ref appears multiple times in XML, an empty string is
        // considered to be a conflict with a non-empty string, since both
        // were explicitly specified.
        // -----------------------------------------------------------------------
        if (lookup != null)
        {
            if (ivLookup != null)
            {
                if (!lookup.equals(ivLookup))
                {
                    String component = ivNameSpaceConfig.getDisplayName();
                    String elementType = ejbLocalRef ? "ejb-local-ref" : "ejb-ref";
                    Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E", new Object[]
                    { component,
                     ivModule,
                     ivApplication,
                     "lookup-name",
                     elementType,
                     "ejb-ref-name",
                     jndiName,
                     ivLookup,
                     lookup });
                    String exMsg = "The " + component + " bean in the " +
                                   ivModule + " module of the " + ivApplication +
                                   " application has conflicting configuration data in the XML" +
                                   " deployment descriptor. Conflicting " + "lookup-name" +
                                   " element values exist for multiple " + elementType +
                                   " elements with the same " + "ejb-ref-name" +
                                   " element value : " + jndiName + ". The conflicting " +
                                   "lookup-name" + " element values are \"" + ivLookup +
                                   "\" and \"" + lookup + "\".";
                    throw new InjectionConfigurationException(exMsg);
                }
            }
            else
            {
                ivLookup = lookup;
                ivBeanNameOrLookupInXml = true;
                curAnnotation.ivLookup = lookup;
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "ivLookup = " + ivLookup);
            }
        }

        // It is an error to specify both 'ejb-link' and 'lookup-name'. F743-21028.4
        if (ivBeanName != null && ivLookup != null)
        {
            // Error - conflicting ejb-link/lookup-name values
            String component = ivNameSpaceConfig.getDisplayName();
            String elementType = ejbLocalRef ? "ejb-local-ref" : "ejb-ref";
            Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                     new Object[] { component,
                                   ivModule,
                                   ivApplication,
                                   "ejb-link/lookup-name",
                                   elementType,
                                   "ejb-ref-name",
                                   getJndiName(),
                                   ivBeanName,
                                   ivLookup });
            String exMsg = "The " + component + " bean in the " + ivModule +
                           " module of the " + ivApplication + " application" +
                           " has conflicting configuration data in the XML" +
                           " deployment descriptor. Conflicting " +
                           "ejb-link/lookup-name" + " element values exist for" +
                           " multiple " + elementType + " elements with the same " +
                           "ejb-ref-name" + " element value : " + getJndiName() +
                           ". The conflicting " + "ejb-link/lookup-name" +
                           " element values are \"" + ivBeanName + "\" and \"" +
                           ivLookup + "\".";
            throw new InjectionConfigurationException(exMsg);
        }

        //Loop through the InjectionTargets and call addInjectionTarget.... which
        //already accounts for duplicates (in case they duplicated some between the two ref definitions.
        List<InjectionTarget> targets = ejbRef.getInjectionTargets();

        if (!targets.isEmpty())
        {
            for (InjectionTarget target : targets)
            {
                Class<?> injectionType = this.getInjectionClassType();
                String injectionName = target.getInjectionTargetName();
                String injectionClassName = target.getInjectionTargetClassName();
                this.addInjectionTarget(injectionType, // d446474
                                        injectionName,
                                        injectionClassName);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "merge", this);
    }

    @Override
    public void mergeSaved(InjectionBinding<EJB> injectionBinding) // d681743
    throws InjectionException
    {
        EJBInjectionBinding ejbBinding = (EJBInjectionBinding) injectionBinding;

        mergeSavedValue(getInjectionClassType(), ejbBinding.getInjectionClassType(), "bean-interface");
        mergeSavedValue(ivBeanName, ejbBinding.ivBeanName, "ejb-link");
        mergeSavedValue(ivLookup, ejbBinding.ivLookup, "lookup");
        mergeSavedValue(ivBindingName, ejbBinding.ivBindingName, "binding-name");

        // ejb-ref-type (Session or Entity) and home/local-home can only be
        // specified in XML, so we can only detect mismatches if both bindings
        // are specified in XML.  We don't want errors to be non-deterministic,
        // but we don't know if we'll see, across multiple components, whether
        // the "saved" binding will annotation or XML, which has an effect on
        // subsequent bindings, so we skip validation altogether.
    }

    /**
     * Set the class of the type to be injected. This will be the most
     * 'specific' class of all the injection-targets or config data. <p>
     *
     * The EJBProcessor overrides this to determine if the injection
     * class is a Home to improve EJB-Link/Auto-Link performance. <p>
     *
     * @param injectionClassType class of the value to be injected
     *
     *            throws InjectionException when the specified injection class type
     *            conflicts with the current injection class type.
     **/
    // d432816
    @Override
    public void setInjectionClassType(Class<?> injectionClassType)
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "setInjectionClassType : " + injectionClassType);

        super.setInjectionClassType(injectionClassType);

        if (!ivHomeInterface)
        {
            if ((EJBHome.class).isAssignableFrom(injectionClassType))
            {
                ivHomeInterface = true;
                ivEjbLocalRef = false;
                ivEjbRef = true;
            }
            else if ((EJBLocalHome.class).isAssignableFrom(injectionClassType))
            {
                ivHomeInterface = true;
                ivEjbLocalRef = true;
                ivEjbRef = false;
            }
            else
            {
                ivBeanInterface = true;
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "setInjectionClassType : " + this);
    }

    /**
     * Overridden to load the injection type class if it hasn't been loaded
     * yet. Will return null if the injection type has not been specified
     * or failed to load.
     *
     * EJBInjectionBinding supports deferred loading of the injection class type.
     */
    // d739281
    @Override
    public Class<?> getInjectionClassType()
    {
        try
        {
            return getInjectionClassTypeWithException();
        } catch (InjectionException ex)
        {
            // FFDC and Tr.error or Tr.warning already logged
        }
        return null;
    }

    /**
     * Equivalent to getInjectionClassType() except a configuration exception
     * will be reported if the bean interface class fails to load. Null will
     * be returned if there is no ClassLoader associated with this binding,
     * an injection type has not been specified, or a invalid home interface
     * was specified but the failure to load is ignored for backward
     * compatibility.
     *
     * EJBInjectionBinding supports deferred loading of the injection class type.
     *
     * @throws InjectionException if the bean interface class has not been
     *             configured properly and the class fails to load.
     */
    // d739281
    public Class<?> getInjectionClassTypeWithException() throws InjectionException
    {
        Class<?> type = super.getInjectionClassType();

        if (type == null && ivClassLoader != null)
        {
            String typeName = getInjectionClassTypeName();
            if (typeName != null)
            {
                type = loadClass(typeName, !ivHomeInterface);
                setInjectionClassType(type);
            }
        }
        return type;
    }

    @Override
    public Object getInjectionObject(Object targetObject,
                                     InjectionTargetContext targetContext)
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getInjectionObject : " + this);

        Object ejb = null;
        try
        {
            ejb = super.getInjectionObject(targetObject, targetContext); // F48603.4
        } catch (Throwable ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".getInjectionObject",
                                        "516", this, new Object[] { ejb, getBindingObject() });

            if (isTraceOn && tc.isEntryEnabled())
            {
                Tr.debug(tc, "getInjectionObject : " + getBindingObject());
                Tr.exit(tc, "getInjectionObject : " + ex);
            }

            //d408351 start
            RecursiveInjectionException.RecursionDetection rd = RecursiveInjectionException.detectRecursiveInjection(ex);
            if (rd == RecursiveInjectionException.RecursionDetection.NotRecursive)
            {
                if (ex instanceof Error)
                {
                    throw (Error) ex;
                }
                if (ex instanceof RuntimeException)
                {
                    throw (RuntimeException) ex;
                }
                if (ex instanceof InjectionException)
                {
                    throw (InjectionException) ex;
                }
                throw new InjectionException(ex.toString(), ex);
            }

            if (rd != RecursiveInjectionException.RecursionDetection.RecursiveAlreadyLogged)
            {
                Tr.error(tc, "RECURSIVE_INJECTION_FAILURE_CWNEN0059E",
                         new Object[] { getJndiName(), ivBeanType, ivBeanName });
            }

            RecursiveInjectionException riex = new RecursiveInjectionException
                            ("The Injection Engine failed to inject the " + getJndiName() +
                             " binding object into the " + ivBeanType + ":" + ivBeanName +
                             " Enterprise JavaBeans (EJB) file because the attempted injection is recursive or cyclic.");
            riex.ivLogged = true;
            throw riex;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getInjectionObject : " + Util.identity(ejb));

        return ejb;
    }

    @Override
    protected Object getInjectionObjectInstance(Object targetObject, InjectionTargetContext targetContext)
                    throws Exception
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getInjectionObjectInstance : " + this);

        Object ejb;
        try
        {
            ejb = super.getInjectionObjectInstance(targetObject, targetContext); // F48603.4
        } catch (NamingException cioex) // CannotInstantiateObjectException F746994.1
        {
            // FFDCFilter.processException( ex, CLASS_NAME + ".getInjectionObject",

            // Most likely, this has occurred because a simple-binding-name has
            // been used, but the bean is not simple. Since this is an ejb-ref
            // binding and the specific interface is known, retry with the
            // swizzeled simple name.                                    d452621
            Throwable cause = cioex.getCause();
            Class<?> injectionType = getInjectionClassType();
            String beanInterfaceName = (injectionType == null) ? null : injectionType.getName();

            if (cause instanceof AmbiguousEJBReferenceException &&
                ivIndirectLookupFactory != null &&
                beanInterfaceName != null)
            {
                String boundToJndiName = ivBoundToJndiName + "#" + beanInterfaceName;
                Reference ref = ivIndirectLookupFactory.createIndirectJndiLookup(getJndiName(),
                                                                                 boundToJndiName,
                                                                                 beanInterfaceName);

                try
                {
                    ejb = ivObjectFactory.getObjectInstance(ref, null, null, null);
                } catch (Throwable ex)
                {
                    FFDCFilter.processException(ex, CLASS_NAME + ".getInjectionObject",
                                                "489", this, new Object[] { ref });
                    throw cioex;
                }

                // Since it worked, make the new settings permanent
                setObjects(null, ref);
                ivBoundToJndiName = boundToJndiName;
            }
            else
            {
                throw cioex;
            }
        }

        // If the returned EJB Object is not assignable to the injection
        // type and it is Remote, then it is likely a Stub class that
        // needs to be narrowed.                                        d447921
        // If the type has not been specified, then it just needs to be
        // type compatible with Object.                            F743-22218.3
        Class<?> injectType = getInjectionClassType();
        if ((injectType != null) &&
            (ejb instanceof ObjectImpl) && // d718020.1
            (!(injectType.isAssignableFrom(ejb.getClass()))))
        {
            // PM28563 - The context class loader should already be set to
            // ivClassLoader prior to injection or lookup, so we do not need
            // to do it ourselves.
            ejb = PortableRemoteObject.narrow(ejb, injectType);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getInjectionObjectInstance : " + Util.identity(ejb));
        return ejb;
    }

    @Override
    public Object getRemoteObject() throws NamingException {
        // While most bindings will return either a primitive value (like env-entry)
        // or a Reference that will be resolved on the client, for EJBs the _Stub
        // will be returned to the client.
        try {
            Object bean = getInjectionObject();
            if (bean instanceof Remote) {
                return bean;
            }
            NamingException nex = new NamingException("The " + getInjectionClassTypeName() + " EJB interface bound to " + getJndiName() + " is not a remote interface.");
            nex.setResolvedName(new CompositeName(getJndiName()));
            throw nex;
        } catch (InjectionException iex) {
            NamingException nex = new NamingException("Failed to create instance of EJB bound to " + getJndiName() + " : " + iex);
            nex.setResolvedName(new CompositeName(getJndiName()));
            throw nex;
        }
    }

    /**
     * Internal method to isolate the class loading logic. <p>
     *
     * Returns the loaded class using the component specific ClassLoader,
     * or null if the specified class name was null or the empty string.
     * This method always returns null if the ClassLoader is null. <p>
     *
     * If a non-null class name is specified, but fails to load, an
     * InjectionConfigurationException will be thrown if the 'required'
     * parameter is set to true; otherwise, a warning will be logged.
     *
     * @param className name of the class to load
     * @param required throw an exception if the class could not be loaded
     **/
    // d432816
    private Class<?> loadClass(String className, boolean required)
                    throws InjectionConfigurationException
    {
        if (className == null || className.equals("") || ivClassLoader == null) // F743-32443
        {
            return null;
        }

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "loadClass : " + className);

        Class<?> loadedClass = null;

        try
        {
            loadedClass = ivClassLoader.loadClass(className);
        } catch (ClassNotFoundException ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".loadClass",
                                        "575", this, new Object[] { className });

            // Prior to EJB 3.0, the classes specified for the bean interface were
            // never validated, so an app would work fine even if garbage was
            // specified.  To allow the EJB 2.1 apps to continue to function, the
            // bean interface class is only 'required' if there is not a home.
            // A warning will be logged if the class name is invalid.       d447590
            if (required)
            {
                InjectionConfigurationException icex = new InjectionConfigurationException
                                ("The " + className + " interface specified for <ejb-ref> or <ejb-local-ref> could not be found", ex);
                Tr.error(tc, "EJB_INTERFACE_IS_NOT_SPECIFIED_CORRECTLY_CWNEN0039E", className);
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "loadClass : " + icex);
                throw icex;
            }

            Tr.warning(tc, "EJB_INTERFACE_IS_NOT_SPECIFIED_CORRECTLY_CWNEN0033W", className);
            if (isValidationFailable()) // fail if enabled       F743-14449
            {
                InjectionConfigurationException icex = new InjectionConfigurationException
                                ("The " + className + " interface specified for <ejb-ref> or <ejb-local-ref> could not be found", ex);
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "loadClass : " + icex);
                throw icex;
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "loadClass : " + loadedClass);

        return loadedClass;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer(Util.identity(this));
        sb.append("[name=").append(getJndiName());

        if (ivEjbLocalRef) {
            sb.append(", local");
        }
        if (ivEjbRef) {
            sb.append(", remote");
        }
        if (ivHomeInterface) {
            sb.append(", home");
        } else if (ivBeanInterface) {
            sb.append(", beanInterface");
        }
        sb.append(", type=").append(getInjectionClassTypeName());
        if (ivBeanName != null) {
            sb.append(", beanName=").append(ivBeanName);
        }
        if (ivLookup != null) {
            sb.append(", lookup=").append(ivLookup);
        }
        if (ivBindingName != null) {
            sb.append(", binding=").append(ivBindingName);
        }

        return sb.append(']').toString();
    }
}
