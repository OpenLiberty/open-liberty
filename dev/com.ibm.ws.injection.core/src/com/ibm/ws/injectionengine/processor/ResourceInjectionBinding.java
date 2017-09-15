/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.processor;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.injectionengine.annotation.ResourceImpl;
import com.ibm.ws.injectionengine.factory.EnvEntryObjectFactory;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;

/**
 * Provides the binding information for the @ResourceAnnotation. Specifically
 * this binding objects for all the resource types. It will also know how to merge
 * the @Resource information between xml and annotation.
 *
 */
public class ResourceInjectionBinding extends InjectionBinding<Resource>
{
    private static final String CLASS_NAME = ResourceInjectionBinding.class.getName();

    private static final TraceComponent tc = Tr.register
                    (ResourceInjectionBinding.class,
                     InjectionConfigConstants.traceString,
                     InjectionConfigConstants.messageFile);

    private static final Method svResourceLookupMethod; // F743-16274.1

    static
    {
        // F743-16274.1 - If the embeddable container is being run on JDK 6 with
        // an unmodified Resource class, then using the lookup method will cause
        // errors.  Tolerate the missing method so that only customers that want
        // to use lookup need to set -Djava.endorsed.dirs.
        Method resourceLookupMethod;
        try
        {
            resourceLookupMethod = Resource.class.getMethod("lookup");
        } catch (NoSuchMethodException ex)
        {
            resourceLookupMethod = null;
        }

        svResourceLookupMethod = resourceLookupMethod;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "javax.annotation.Resource.lookup = " + resourceLookupMethod);
    }

    /**
     * Returns the result of javax.annotation.Resource.lookup, or the empty
     * string if that method is unavailable in the current JVM.
     *
     * @param resource the resource annotation
     * @return the lookup string or the empty string
     */
    private static String getResourceLookup(Resource resource) // F743-16274.1
    {
        if (svResourceLookupMethod == null)
        {
            return "";
        }

        try
        {
            return (String) svResourceLookupMethod.invoke(resource, (Object[]) null);
        } catch (Exception ex)
        {
            throw new IllegalStateException(ex);
        }
    }

    // --------------------------------------------------------------------------
    // Meta data exclusive to xml
    // --------------------------------------------------------------------------
    String ivEnvValue = null; // env-entry-value                    F743-22218.3

    String ivLink = null; // message-destination-link

    ResourceXMLType ivXMLType; // XML Element Type, when defined in XML, else null
                               // Used for error messages.                d479669

    // --------------------------------------------------------------------------
    // Meta data common between annotations and xml
    // --------------------------------------------------------------------------
    String ivLookup; // lookup parameter of annotation |
                     // lookup-name from xml                F743-21028.4
    boolean ivLookupInXml; // true when lookup-name found in XML. F743-21028.4

    // --------------------------------------------------------------------------
    // Binding meta data for saved injection bindings
    // --------------------------------------------------------------------------
    String ivBindingName; // the binding-name
    String ivBindingValue; // the env-entry value
    ResourceRefConfig ivResRefConfig; // resource-ref extensions

    // --------------------------------------------------------------------------
    // Meta data of the component for which the reference is defined
    // --------------------------------------------------------------------------
    String ivApplication; // Application name of the referencing bean,
                          // NOT the referenced bean.
    String ivModule; // Module name of the referencing bean,
                     // NOT the referenced bean.
    String ivComponent; // Component name of the referencing bean,
                        // NOT the referenced bean.                 d479669

    private boolean ivMergeCompleted = false; // Has one merge method call been successfully completed?

    /**
     * @param annotation
     * @param nameSpaceConfig
     * @throws InjectionException
     */
    public ResourceInjectionBinding(Resource annotation,
                                    ComponentNameSpaceConfiguration nameSpaceConfig)
        throws InjectionException
    {
        super(annotation, nameSpaceConfig);
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "<init> ");
        setJndiName(annotation.name());
        setInjectionClassType(annotation.type());
        ivApplication = nameSpaceConfig.getApplicationName();
        ivModule = nameSpaceConfig.getModuleName();
        ivComponent = nameSpaceConfig.getDisplayName();
        ivXMLType = ResourceXMLType.UNKNOWN;
        ivLookup = getResourceLookup(annotation); // F743-21028.4, F743-16274.1
        if (ivLookup.isEmpty()) // F91489
        {
            ivLookup = null;
        }
        ivLookupInXml = false; // F743-21028.4
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "<init> : " + this);
    }

    /**
     * @param annotation
     * @param xmlType Type of XML element (resource-ref, resource-env-ref, etc).
     * @param nameSpaceConfig
     * @throws InjectionException
     **/
    // d479669
    public ResourceInjectionBinding(Resource annotation,
                                    String injectionClassTypeName, // F743-32443
                                    String lookup, // F743-21028.4
                                    ResourceXMLType xmlType,
                                    ComponentNameSpaceConfiguration nameSpaceConfig)
        throws InjectionException
    {
        super(annotation, nameSpaceConfig);
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "<init> ");
        setJndiName(annotation.name());
        setInjectionClassType(annotation.type());
        setInjectionClassTypeName(injectionClassTypeName); // F743-32443
        ivApplication = nameSpaceConfig.getApplicationName();
        ivModule = nameSpaceConfig.getModuleName();
        ivComponent = nameSpaceConfig.getDisplayName();
        ivXMLType = xmlType;
        ivLookup = lookup; // F743-21028.4
        ivLookupInXml = (ivLookup != null); // F743-21028.4
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "<init> : " + this);
    }

    /**
     * Constructor to be used when the binding represents a env-entry
     * stanza in XML. <p>
     *
     * @param annotation the Resource annotation this binding represents
     * @param envEntry the env-entry this binding represents
     * @param nameSpaceConfig the component configuration data
     */
    // F743-22218.3
    public ResourceInjectionBinding(ResourceImpl annotation,
                                    EnvEntry envEntry,
                                    ComponentNameSpaceConfiguration nameSpaceConfig)
        throws InjectionException
    {
        super(annotation, nameSpaceConfig);

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "<init> : " + envEntry);

        setJndiName(envEntry.getName());

        ivApplication = nameSpaceConfig.getApplicationName();
        ivModule = nameSpaceConfig.getModuleName();
        ivComponent = nameSpaceConfig.getDisplayName();
        ivXMLType = ResourceXMLType.ENV_ENTRY;
        ivEnvValue = envEntry.getValue();
        ivLookup = envEntry.getLookupName();
        if (ivLookup != null) {
            ivLookup = ivLookup.trim();
        }
        ivLookupInXml = (ivLookup != null);

        Object type = getEnvEntryType(envEntry);
        if (type != null)
        {
            setEnvEntryType(annotation, type); // F743-32443
        }

        // It is an error to specify both 'env-entry-value' and 'lookup-name'.
        if (ivEnvValue != null && ivLookup != null)
        {
            // Error - conflicting env-entry-value/lookup-name values
            String elementType = "env-entry";
            Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                     ivComponent,
                     ivModule,
                     ivApplication,
                     "env-entry-value/lookup-name",
                     elementType,
                     "env-entry-name",
                     getJndiName(),
                     ivEnvValue,
                     ivLookup);
            String exMsg = "The " + ivComponent + " bean in the " + ivModule +
                           " module of the " + ivApplication + " application" +
                           " has conflicting configuration data in the XML" +
                           " deployment descriptor. Conflicting " +
                           "env-entry-value/lookup-name" + " element values exist for" +
                           " multiple " + elementType + " elements with the same " +
                           "env-entry-name" + " element value : " + getJndiName() +
                           ". The conflicting " + "env-entry-value/lookup-name" +
                           " element values are \"" + ivEnvValue + "\" and \"" +
                           ivLookup + "\".";
            throw new InjectionConfigurationException(exMsg);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "<init> : " + this);
    }

    //449021 added constructor
    /**
     * @param annotation
     * @param ref
     * @param nameSpaceConfig
     * @throws InjectionException
     */
    public ResourceInjectionBinding(Resource annotation,
                                    MessageDestinationRef ref,
                                    String lookup,
                                    ComponentNameSpaceConfiguration nameSpaceConfig)
        throws InjectionException
    {
        super(annotation, nameSpaceConfig);
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "<init> : " + ref);
        setJndiName(annotation.name());
        setInjectionClassType(annotation.type());
        setInjectionClassTypeName(ref.getType());
        ivLink = ref.getLink();
        ivApplication = nameSpaceConfig.getApplicationName();
        ivModule = nameSpaceConfig.getModuleName();
        ivComponent = nameSpaceConfig.getDisplayName();
        ivXMLType = ResourceXMLType.MESSAGE_DESTINATION_REF;
        ivLookup = lookup; // F743-21028.4
        ivLookupInXml = (ivLookup != null); // F743-21028.4
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "<init> : " + this);
    }

    /**
     * The merge method performs a couple different functions, depending on the input. If one metadata
     * source is xml and the other metadata source is from an annotation, a merge is performed per the
     * EJB 3.0 Specification rules. On the other hand, if both metadata sources are from annotations,
     * it performs a simple check that both annotations have identical metadata.
     *
     * Note that merge may be called multiple times. This requires a little tracking be done because
     * this object is also used to hold the result of the merged metadata. This means that if we have
     * already merged and have a result, subsequent merge calls do not really merge, but just check that
     * the metadata is consistent with the previous merged result.
     *
     * Also note that merge is never called unless the two metadata sources already have the same name.
     * In the case of XML name is a required field. In the case of an annotation it may be a specified
     * name or a default name.
     *
     * @param annotation - the second metadata source
     * @param instanceClass - the class containing the annotation
     * @param member - describes the injection target
     *
     * @throws InjectionException
     * @throws InjectionConfigurationException when configuration errors are detected in xml or annotations.
     */
    @Override
    public void merge(Resource annotation, Class<?> instanceClass, Member member) throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "merge : " + ivMergeCompleted);

        //  XML / Annotation merge rules from EJB 3.0 Core Contracts Spec section 16.2.3:
        //
        //  The following rules apply to how a deployment descriptor entry may override a Resource annotation:
        //
        //  - The relevant deployment descriptor entry is located based on the JNDI name used with the
        //    annotation (either defaulted or provided explicitly).
        //
        //  - The type specified in the deployment descriptor must be assignable to the type of the field or
        //    property or the type specified in the Resource annotation.
        //
        //  - The description, if specified, overrides the description element of the annotation.
        //
        //  - The injection target, if specified, must name exactly the annotated field or property method.
        //
        //  - The res-sharing-scope element, if specified, overrides the shareable element of the
        //    annotation. In general, the Application Assembler or Deployer should never change the value
        //    of this element, as doing so is likely to break the application.
        //
        //  - The res-auth element, if specified, overrides the authenticationType element of the
        //     annotation. In general, the Application Assembler or Deployer should

        // Load the XML metadata settings (ie. set during ctor time)
        Resource currentAnnotation = this.getAnnotation();

        // If this object was constructed with metadata from xml, then we are being asked to
        // merge xml and annotations, following the EJB spec rules for xml overrides of annotations.
        // However, if one merge has already been completed, we simply need to check the new annotation
        // metadata passed in to assure it matches the previous merge result.
        if ((currentAnnotation instanceof ResourceImpl) && (!ivMergeCompleted) && !isComplete())
        {
            // If Type was set from XML, and it is either the same class or a subclass of the annotation type field, then
            // it overrides the annotation.  If the types mismatch between xml and an annotation throw an error. Otherwise,
            // if there was no data from XML just use the class from the annotation.
            if (((ResourceImpl) currentAnnotation).ivIsSetType)
            {
                // check that value from xml is a subclasss, if not throw an error
                if (InjectionBinding.isClassesCompatible(annotation.type(), currentAnnotation.type())) {
                    // do nothing - xml data overrides the annotation
                }
                else
                {
                    // Error - conflicting annotation type classes detected
                    Tr.error(tc, "CONFLICTING_XML_ANNOTATION_VALUES_CWNEN0053E",
                             ivComponent,
                             ivModule,
                             ivApplication,
                             ivXMLType.type_element(),
                             "type",
                             ivXMLType,
                             "@Resource",
                             ivXMLType.name_element(),
                             "name",
                             getJndiName(),
                             currentAnnotation.type().getName(),
                             annotation.type().getName()); // d479669
                    String exMsg = "The " + ivComponent + " bean in the " +
                                   ivModule + " module of the " + ivApplication +
                                   " application has conflicting configuration data between" +
                                   " the XML deployment descriptor and source code annotations." +
                                   " Conflicting " + ivXMLType.type_element() + " element values or " +
                                   "type" + " attribute values exist for multiple " + ivXMLType +
                                   " elements or " + "@Resource" + " annotations with the same " +
                                   ivXMLType.name_element() + " element value or " + "name" +
                                   " attribute value : " + getJndiName() + ". The conflicting " +
                                   ivXMLType.type_element() + " element values or " + "type" +
                                   " attribute values are " + currentAnnotation.type().getName() +
                                   " and " + annotation.type().getName() + "."; // d479669
                    throw new InjectionConfigurationException(exMsg);
                }
            }
            else
            {
                // replace current type value, using the annotation data
                ((ResourceImpl) currentAnnotation).ivType = annotation.type();
                setInjectionClassType(annotation.type()); // d729308
            }

            // If Authentication value was set from XML then it overrides the  annotation.  Otherwise, use value from annotation.
            if (((ResourceImpl) currentAnnotation).ivIsSetAuthenticationType)
            {
                // do nothing - XML overrides the annotation
            }
            else
            {
                ((ResourceImpl) currentAnnotation).ivAuthenticationType = annotation.authenticationType();
            }

            // If shareable value was set from XML then it overrides the annotation.   Otherwise, use value from annotation.
            if (((ResourceImpl) currentAnnotation).ivIsSetShareable)
            {
                // do nothing - XML overrides the annotation
            }
            else
            {
                ((ResourceImpl) currentAnnotation).ivShareable = annotation.shareable();
            }

            // If Mapped Name value was set from XML then it overrides the annotation.  Otherwise, use value from annotation.
            if (((ResourceImpl) currentAnnotation).ivIsSetMappedName)
            {
                // do nothing - XML overrides the annotation
            }
            else
            {
                ((ResourceImpl) currentAnnotation).ivMappedName = annotation.mappedName();
            }

            // If Description value was set from XML then it overrides the annotation.  Otherwise, use value from annotation.
            if (((ResourceImpl) currentAnnotation).ivIsSetDescription)
            {
                // do nothing - XML overrides the annotation
            }
            else
            {
                ((ResourceImpl) currentAnnotation).ivDescription = annotation.description();
            }

            ivMergeCompleted = true;
        }
        else
        {
            // Otherwise, we need to validate that that two metdata sources have the exact same metadata.
            // We may have one source that is from a previous merge with xml (ie. currentAnnotation is a ResourceImpl object),
            // and the other source is from an annotation (ie. annotation is a simple Resource object).   Or, we may have a
            // case where both the currentAnnotation and annotation objects are from annotations (ie. neither was xml).   In either
            // case if the metadata does not match we log and throw a configuration error.

            if (currentAnnotation instanceof ResourceImpl)
            {
                // Here we are checking a previous merge result (ie. one source was xml) against metadata from another annotation.
                // Therefore, we only need to check for matches on fields where the xml has not already overridden the annotation value.
                if ((!((ResourceImpl) currentAnnotation).ivIsSetType) &&
                    (!(currentAnnotation.type().equals(annotation.type()))))
                {
                    // Error - conflicting resource type specified
                    Tr.error(tc, "CONFLICTING_XML_ANNOTATION_VALUES_CWNEN0053E",
                             ivComponent,
                             ivModule,
                             ivApplication,
                             ivXMLType.type_element(),
                             "type",
                             ivXMLType,
                             "@Resource",
                             ivXMLType.name_element(),
                             "name",
                             getJndiName(),
                             currentAnnotation.type().getName(),
                             annotation.type().getName()); // d479669
                    String exMsg = "The " + ivComponent + " bean in the " +
                                   ivModule + " module of the " + ivApplication +
                                   " application has conflicting configuration data between" +
                                   " the XML deployment descriptor and source code annotations." +
                                   " Conflicting " + ivXMLType.type_element() + " element values or " +
                                   "type" + " attribute values exist for multiple " + ivXMLType +
                                   " elements or " + "@Resource" + " annotations with the same " +
                                   ivXMLType.name_element() + " element value or " + "name" +
                                   " attribute value : " + getJndiName() + ". The conflicting " +
                                   ivXMLType.type_element() + " element values or " + "type" +
                                   " attribute values are " + currentAnnotation.type().getName() +
                                   " and " + annotation.type().getName() + "."; // d479669
                    throw new InjectionConfigurationException(exMsg);
                }

                if ((!((ResourceImpl) currentAnnotation).ivIsSetAuthenticationType) &&
                    (!(currentAnnotation.authenticationType().equals(annotation.authenticationType()))))
                {
                    // Error - conflicting Authentication Type specified
                    Tr.error(tc, "CONFLICTING_XML_ANNOTATION_VALUES_CWNEN0053E",
                             ivComponent,
                             ivModule,
                             ivApplication,
                             "res-auth",
                             "authenticationType",
                             ivXMLType,
                             "@Resource",
                             ivXMLType.name_element(),
                             "name",
                             getJndiName(),
                             currentAnnotation.authenticationType(),
                             annotation.authenticationType()); // d479669
                    String exMsg = "The " + ivComponent + " bean in the " +
                                   ivModule + " module of the " + ivApplication +
                                   " application has conflicting configuration data between" +
                                   " the XML deployment descriptor and source code annotations." +
                                   " Conflicting " + "res-auth" + " element values or " +
                                   "authenticationType" + " attribute values exist for multiple " +
                                   ivXMLType + " elements or " + "@Resource" +
                                   " annotations with the same " + ivXMLType.name_element() +
                                   " element value or " + "name" + " attribute value : " +
                                   getJndiName() + ". The conflicting " + "res-auth" +
                                   " element values or " + "authenticationType" +
                                   " attribute values are " + currentAnnotation.authenticationType() +
                                   " and " + annotation.authenticationType() + "."; // d479669
                    throw new InjectionConfigurationException(exMsg);
                }

                if ((!((ResourceImpl) currentAnnotation).ivIsSetShareable) &&
                    (currentAnnotation.shareable() != annotation.shareable()))
                {
                    // Error - conflicting shareable resource specified
                    Tr.error(tc, "CONFLICTING_XML_ANNOTATION_VALUES_CWNEN0053E",
                             ivComponent,
                             ivModule,
                             ivApplication,
                             "res-sharing-scope",
                             "shareable",
                             ivXMLType,
                             "@Resource",
                             ivXMLType.name_element(),
                             "name",
                             getJndiName(),
                             currentAnnotation.shareable(),
                             annotation.shareable()); // d479669
                    String exMsg = "The " + ivComponent + " bean in the " +
                                   ivModule + " module of the " + ivApplication +
                                   " application has conflicting configuration data between" +
                                   " the XML deployment descriptor and source code annotations." +
                                   " Conflicting " + "res-sharing-scope" + " element values or " +
                                   "shareable" + " attribute values exist for multiple " +
                                   ivXMLType + " elements or " + "@Resource" +
                                   " annotations with the same " + ivXMLType.name_element() +
                                   " element value or " + "name" + " attribute value : " +
                                   getJndiName() + ". The conflicting " + "res-sharing-scope" +
                                   " element values or " + "shareable" +
                                   " attribute values are " + currentAnnotation.shareable() +
                                   " and " + annotation.shareable() + "."; // d479669
                    throw new InjectionConfigurationException(exMsg);
                }

                if ((!((ResourceImpl) currentAnnotation).ivIsSetMappedName) &&
                    (!(currentAnnotation.mappedName().equals(annotation.mappedName()))))
                {
                    // Error - conflicting mapped name specified
                    Tr.error(tc, "CONFLICTING_XML_ANNOTATION_VALUES_CWNEN0053E",
                             ivComponent,
                             ivModule,
                             ivApplication,
                             "mapped-name",
                             "mappedName",
                             ivXMLType,
                             "@Resource",
                             ivXMLType.name_element(),
                             "name",
                             getJndiName(),
                             currentAnnotation.mappedName(),
                             annotation.mappedName()); // d479669
                    String exMsg = "The " + ivComponent + " bean in the " +
                                   ivModule + " module of the " + ivApplication +
                                   " application has conflicting configuration data between" +
                                   " the XML deployment descriptor and source code annotations." +
                                   " Conflicting " + "mapped-name" + " element values or " +
                                   "mappedName" + " attribute values exist for multiple " +
                                   ivXMLType + " elements or " + "@Resource" +
                                   " annotations with the same " + ivXMLType.name_element() +
                                   " element value or " + "name" + " attribute value : " +
                                   getJndiName() + ". The conflicting " + "mapped-name" +
                                   " element values or " + "mappedName" +
                                   " attribute values are " + currentAnnotation.mappedName() +
                                   " and " + annotation.mappedName() + "."; // d479669
                    throw new InjectionConfigurationException(exMsg);
                }
            }
            else
            {
                // In this case both metadata sources are from annotations and there was not a previous merge of xml with an annotation.
                // Therefore, we simply need to compare the two annotation sources to make sure they do not conflict.
                if (!(currentAnnotation.type().equals(annotation.type())))
                {
                    // Error - conflicting resource type specified
                    Tr.error(tc, "CONFLICTING_ANNOTATION_VALUES_CWNEN0054E",
                             ivComponent,
                             ivModule,
                             ivApplication,
                             "type",
                             "@Resource",
                             "name",
                             getJndiName(),
                             currentAnnotation.type().getName(),
                             annotation.type().getName()); // d479669
                    String exMsg = "The " + ivComponent + " bean in the " +
                                   ivModule + " module of the " + ivApplication +
                                   " application has conflicting configuration data" +
                                   " in source code annotations. Conflicting " +
                                   "type" + " attribute values exist for multiple " +
                                   "@Resource" + " annotations with the same " +
                                   "name" + " attribute value : " + getJndiName() +
                                   ". The conflicting " + "type" +
                                   " attribute values are " + currentAnnotation.type().getName() +
                                   " and " + annotation.type().getName() + "."; // d479669
                    throw new InjectionConfigurationException(exMsg);
                }

                if (!(currentAnnotation.authenticationType().equals(annotation.authenticationType())))
                {
                    // Error - conflicting authentication type specified
                    Tr.error(tc, "CONFLICTING_ANNOTATION_VALUES_CWNEN0054E",
                             ivComponent,
                             ivModule,
                             ivApplication,
                             "authenticationType",
                             "@Resource",
                             "name",
                             getJndiName(),
                             currentAnnotation.authenticationType(),
                             annotation.authenticationType()); // d479669
                    String exMsg = "The " + ivComponent + " bean in the " +
                                   ivModule + " module of the " + ivApplication +
                                   " application has conflicting configuration data" +
                                   " in source code annotations. Conflicting " +
                                   "authenticationType" + " attribute values exist for multiple " +
                                   "@Resource" + " annotations with the same " +
                                   "name" + " attribute value : " + getJndiName() +
                                   ". The conflicting " + "authenticationType" +
                                   " attribute values are " + currentAnnotation.authenticationType() +
                                   " and " + annotation.authenticationType() + "."; // d479669
                    throw new InjectionConfigurationException(exMsg);
                }

                if (currentAnnotation.shareable() != annotation.shareable())
                {
                    // Error - conflicting shareable resource specified
                    Tr.error(tc, "CONFLICTING_ANNOTATION_VALUES_CWNEN0054E",
                             ivComponent,
                             ivModule,
                             ivApplication,
                             "shareable",
                             "@Resource",
                             "name",
                             getJndiName(),
                             currentAnnotation.shareable(),
                             annotation.shareable()); // d479669
                    String exMsg = "The " + ivComponent + " bean in the " +
                                   ivModule + " module of the " + ivApplication +
                                   " application has conflicting configuration data" +
                                   " in source code annotations. Conflicting " +
                                   "shareable" + " attribute values exist for multiple " +
                                   "@Resource" + " annotations with the same " +
                                   "name" + " attribute value : " + getJndiName() +
                                   ". The conflicting " + "shareable" +
                                   " attribute values are " + currentAnnotation.shareable() +
                                   " and " + annotation.shareable() + "."; // d479669
                    throw new InjectionConfigurationException(exMsg);
                }

                if (!(currentAnnotation.mappedName().equals(annotation.mappedName())))
                {
                    // Error - conflicting mapped name specified
                    Tr.error(tc, "CONFLICTING_ANNOTATION_VALUES_CWNEN0054E",
                             ivComponent,
                             ivModule,
                             ivApplication,
                             "mappedName",
                             "@Resource",
                             "name",
                             getJndiName(),
                             currentAnnotation.mappedName(),
                             annotation.mappedName()); // d479669
                    String exMsg = "The " + ivComponent + " bean in the " +
                                   ivModule + " module of the " + ivApplication +
                                   " application has conflicting configuration data" +
                                   " in source code annotations. Conflicting " +
                                   "mappedName" + " attribute values exist for multiple " +
                                   "@Resource" + " annotations with the same " +
                                   "name" + " attribute value : " + getJndiName() +
                                   ". The conflicting " + "mappedName" +
                                   " attribute values are " + currentAnnotation.mappedName() +
                                   " and " + annotation.mappedName() + "."; // d479669
                    throw new InjectionConfigurationException(exMsg);
                }
            }
        }

        // If "lookup" has not been defined in XML, then use the value from
        // annotations.  However, if there are multiple annotations with the
        // same name, then the values must match.                     F743-21028.4
        // However, ignore lookup from the annotation if an env-entry-value
        // has been specified in XML.                                 F743-22218.3
        if (!ivLookupInXml && ivEnvValue == null)
        {
            String lookup = getResourceLookup(annotation); // F743-16274.1

            // An empty string is the default and not considered a specified value.
            if (lookup != null && !(lookup.equals("")))
            {
                if (!isComplete() && (ivLookup == null || ivLookup.equals("")))
                {
                    ivLookup = lookup;
                }
                else
                {
                    if (!(lookup.equals(ivLookup)))
                    {
                        // Error - conflicting lookup values between annotations
                        Tr.error(tc, "CONFLICTING_ANNOTATION_VALUES_CWNEN0054E",
                                 ivComponent,
                                 ivModule,
                                 ivApplication,
                                 "lookup",
                                 "@Resource",
                                 "name",
                                 getJndiName(),
                                 ivLookup,
                                 lookup);
                        String exMsg = "The " + ivComponent + " bean in the " +
                                       ivModule + " module of the " + ivApplication +
                                       " application has conflicting configuration data" +
                                       " in source code annotations. Conflicting " +
                                       "lookup" + " attribute values exist for multiple " +
                                       "@Resource" + " annotations with the same " +
                                       "name" + " attribute value : " + getJndiName() +
                                       ". The conflicting " + "lookup" +
                                       " attribute values are " + ivLookup +
                                       " and " + lookup + ".";
                        throw new InjectionConfigurationException(exMsg);
                    }
                }
            }
        }

        // On exit trace the merged configuration data that is contained in this object
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "merge", this);
    }

    /**
     * Extract the fields from the EnvEntry, and verify they match the values in the current
     * binding object and/or annotation exactly. If they do indeed match, add all the
     * InjectionTargets on the EnvEntry parameter to the current binding. The code takes into
     * account the possibility of duplicates InjectionTargets and will only use one in case
     * where they duplicated between the two ref definitions.
     *
     * @param envEntry
     * @throws InjectionException
     */
    public void merge(EnvEntry envEntry) throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "merge : " + envEntry);

        ResourceImpl curAnnotation = (ResourceImpl) this.getAnnotation();
        String jndiName = envEntry.getName();
        String curJndiName = curAnnotation.name();
        String mappedName = envEntry.getMappedName();
        String curMappedName = curAnnotation.mappedName();
        String value = envEntry.getValue();
        String curValue = ivEnvValue; // F743-22218.3
        Object type = getEnvEntryType(envEntry); // F743-32443
        String lookup = envEntry.getLookupName(); // F743-21028.4
        if (lookup != null) {
            lookup = lookup.trim();
        }

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(tc, "new=" + jndiName + ":" + mappedName + ":" + value + ":" + type + ":" + lookup);
            Tr.debug(tc, "cur=" + curJndiName + ":" + curMappedName + ":" + curValue + ":" + getInjectionClassTypeName() + ":" + ivLookup);
        }

        //The mappedName parameter is "optional"
        if (curAnnotation.ivIsSetMappedName && mappedName != null)
        {
            if (!curMappedName.equals(mappedName))
            {
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                         ivComponent,
                         ivModule,
                         ivApplication,
                         "mapped-name",
                         "env-entry",
                         "env-entry-name",
                         getJndiName(),
                         curMappedName,
                         mappedName); // d479669
                String exMsg = "The " + ivComponent + " bean in the " +
                               ivModule + " module of the " + ivApplication +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "mapped-name" +
                               " element values exist for multiple " + "env-entry" +
                               " elements with the same " + "env-entry-name" + " element value : " +
                               getJndiName() + ". The conflicting " + "mapped-name" +
                               " element values are " + curMappedName + " and " +
                               mappedName + "."; // d479669
                throw new InjectionConfigurationException(exMsg);
            }
        }
        else if (mappedName != null && !curAnnotation.ivIsSetMappedName)
        {
            curAnnotation.ivMappedName = mappedName;
            curAnnotation.ivIsSetMappedName = true;
        }

        //The type parameter is "optional"
        if (curAnnotation.ivIsSetType && envEntry.getTypeName() != null)
        {
            // check that value from xml is a subclasss, if not throw an error
            if (!isEnvEntryTypeCompatible(type)) // F743-32443
            {
                Class<?> curType = getInjectionClassType();
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                         ivComponent,
                         ivModule,
                         ivApplication,
                         "env-entry-type",
                         "env-entry",
                         "env-entry-name",
                         getJndiName(),
                         curType,
                         type); // d479669
                String exMsg = "The " + ivComponent + " bean in the " +
                               ivModule + " module of the " + ivApplication +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "env-entry-type" +
                               " element values exist for multiple " + "env-entry" +
                               " elements with the same " + "env-entry-name" + " element value : " +
                               getJndiName() + ". The conflicting " + "env-entry-type" +
                               " element values are " + curType + " and " +
                               type + "."; // d479669
                throw new InjectionConfigurationException(exMsg);
            }
        }
        else if (type != null && !curAnnotation.ivIsSetType)
        {
            setEnvEntryType(curAnnotation, type);
        }

        if (curValue != null && value != null)
        {
            if (!curValue.equals(value))
            {
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                         ivComponent,
                         ivModule,
                         ivApplication,
                         "env-entry-value",
                         "env-entry",
                         "env-entry-name",
                         getJndiName(),
                         curValue,
                         value); // d479669
                String exMsg = "The " + ivComponent + " bean in the " +
                               ivModule + " module of the " + ivApplication +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "env-entry-value" +
                               " element values exist for multiple " + "env-entry" +
                               " elements with the same " + "env-entry-name" + " element value : " +
                               getJndiName() + ". The conflicting " + "env-entry-value" +
                               " element values are " + curValue + " and " +
                               value + "."; // d479669
                throw new InjectionConfigurationException(exMsg);
            }
        }
        else if (value != null)
        {
            ivEnvValue = value; // F743-22218.3
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "env-value = " + ivEnvValue);
        }

        // -----------------------------------------------------------------------
        // Merge : lookup - "optional parameter                       F743-21028.4
        //
        // If present in XML, even if the empty string (""), it will override
        // any setting via annotations. An empty string would effectivly turn
        // off this setting.
        //
        // When an env-entry appears multiple times in XML, an empty string is
        // considered to be a confilct with a non-empty string, since both
        // were explicitly specified.
        // -----------------------------------------------------------------------
        if (lookup != null)
        {
            if (ivLookupInXml)
            {
                if (!lookup.equals(ivLookup))
                {
                    Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                             ivComponent,
                             ivModule,
                             ivApplication,
                             "lookup-name",
                             "env-entry",
                             "env-entry-name",
                             jndiName,
                             ivLookup,
                             lookup);
                    String exMsg = "The " + ivComponent + " bean in the " +
                                   ivModule + " module of the " + ivApplication +
                                   " application has conflicting configuration data in the XML" +
                                   " deployment descriptor. Conflicting " + "lookup-name" +
                                   " element values exist for multiple " + "env-entry" +
                                   " elements with the same " + "env-entry-name" +
                                   " element value : " + jndiName + ". The conflicting " +
                                   "lookup-name" + " element values are \"" + ivLookup +
                                   "\" and \"" + lookup + "\".";
                    throw new InjectionConfigurationException(exMsg);
                }
            }
            else
            {
                ivLookup = lookup;
                ivLookupInXml = true;
                curAnnotation.ivLookup = lookup;
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "ivLookup = " + ivLookup);
            }
        }

        // It is an error to specify both 'env-entry-value' and 'lookup-name'. F743-22218.3
        if (ivEnvValue != null && ivLookup != null)
        {
            // Error - conflicting env-entry-value/lookup-name values
            String elementType = "env-entry";
            Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                     ivComponent,
                     ivModule,
                     ivApplication,
                     "env-entry-value/lookup-name",
                     elementType,
                     "env-entry-name",
                     getJndiName(),
                     ivEnvValue,
                     ivLookup);
            String exMsg = "The " + ivComponent + " bean in the " + ivModule +
                           " module of the " + ivApplication + " application" +
                           " has conflicting configuration data in the XML" +
                           " deployment descriptor. Conflicting " +
                           "env-entry-value/lookup-name" + " element values exist for" +
                           " multiple " + elementType + " elements with the same " +
                           "env-entry-name" + " element value : " + getJndiName() +
                           ". The conflicting " + "env-entry-value/lookup-name" +
                           " element values are \"" + ivEnvValue + "\" and \"" +
                           ivLookup + "\".";
            throw new InjectionConfigurationException(exMsg);
        }

        //Loop through the InjectionTargets and call addInjectionTarget.... which
        //already accounts for duplicates (in case they duplicated some between the two ref definitions.
        List<InjectionTarget> targets = envEntry.getInjectionTargets();
        String targetName = null;
        String targetClassName = null;

        Class<?> injectionType = this.getInjectionClassType();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "targetType : " + injectionType);

        if (!targets.isEmpty())
        {
            for (InjectionTarget target : targets)
            {
                targetClassName = target.getInjectionTargetClassName();
                targetName = target.getInjectionTargetName();
                this.addInjectionTarget(injectionType, targetName, targetClassName);
            } //for loop
        } //targets !null

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "merge : " + this);
    }

    /**
     * Extract the fields from the MessageDestinationRef, and verify they match the values in the current
     * binding object and/or annotation exactly. If they do indeed match, add all the
     * InjectionTargets on the MessageDestinationRef parameter to the current binding. The code takes into
     * account the possibility of duplicates InjectionTargets and will only use one in case
     * where they duplicated between the two ref definitions.
     *
     * @param msgDestRef
     * @throws InjectionException
     */
    public void merge(MessageDestinationRef msgDestRef) throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "merge", msgDestRef);

        ResourceImpl curAnnotation = (ResourceImpl) this.getAnnotation();
        String jndiName = msgDestRef.getName();
        String curJndiName = curAnnotation.name();
        String mappedName = msgDestRef.getMappedName();
        String curMappedName = curAnnotation.mappedName();
        String typeName = msgDestRef.getType();
        //The link parameter is "optional"
        String link = msgDestRef.getLink();
        String curLink = this.ivLink;
        String lookup = msgDestRef.getLookupName(); // F743-21028.4
        if (lookup != null) {
            lookup = lookup.trim();
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "new=" + jndiName + ":" + mappedName + ":" + typeName + ":" + link + ":" + lookup);

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "cur=" + curJndiName + ":" + curMappedName + ":" + getInjectionClassTypeName() + ":" + curLink + ":" + ivLookup);

        //The mappedName parameter is "optional"
        if (curAnnotation.ivIsSetMappedName && mappedName != null)
        {
            if (!curMappedName.equals(mappedName))
            {
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                         ivComponent,
                         ivModule,
                         ivApplication,
                         "mapped-name",
                         "message-destination-ref",
                         "message-destination-ref-name",
                         getJndiName(),
                         curMappedName,
                         mappedName); // d479669
                String exMsg = "The " + ivComponent + " bean in the " +
                               ivModule + " module of the " + ivApplication +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "mapped-name" +
                               " element values exist for multiple " + "message-destination-ref" +
                               " elements with the same " + "message-destination-ref-name" +
                               " element value : " +
                               getJndiName() + ". The conflicting " + "mapped-name" +
                               " element values are " + curMappedName + " and " +
                               mappedName + "."; // d479669
                throw new InjectionConfigurationException(exMsg);
            }
        }
        else if (mappedName != null && !curAnnotation.ivIsSetMappedName)
        {
            curAnnotation.ivMappedName = mappedName;
            curAnnotation.ivIsSetMappedName = true;
        }

        setXMLType(typeName,
                   "message-destination-ref",
                   "message-destination-ref-name",
                   "message-destination-type"); // F743-32443

        //The link parameter is "optional"
        if (curLink != null && link != null)
        {
            if (!curLink.equals(link))
            {
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                         ivComponent,
                         ivModule,
                         ivApplication,
                         "message-destination-link",
                         "message-destination-ref",
                         "message-destination-ref-name",
                         getJndiName(),
                         curLink,
                         link); // d479669
                String exMsg = "The " + ivComponent + " bean in the " +
                               ivModule + " module of the " + ivApplication +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "message-destination-link" +
                               " element values exist for multiple " + "message-destination-ref" +
                               " elements with the same " + "message-destination-ref-name" + " element value : " +
                               getJndiName() + ". The conflicting " + "message-destination-link" +
                               " element values are " + curLink + " and " +
                               link + "."; // d479669
                throw new InjectionConfigurationException(exMsg);
            }
        }
        else if (link != null && curLink == null)
        {
            this.ivLink = link;
        }

        // -----------------------------------------------------------------------
        // Merge : lookup - "optional parameter                       F743-21028.4
        //
        // If present in XML, even if the empty string (""), it will override
        // any setting via annotations. An empty string would effectivly turn
        // off this setting.
        //
        // When a message-destination-ref appears multiple times in XML, an empty
        // string is considered to be a confilct with a non-empty string, since
        // both were explicitly specified.
        // -----------------------------------------------------------------------
        if (lookup != null)
        {
            if (ivLookupInXml)
            {
                if (!lookup.equals(ivLookup))
                {
                    Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                             ivComponent,
                             ivModule,
                             ivApplication,
                             "lookup-name",
                             "message-destination-ref",
                             "message-destination-ref-name",
                             jndiName,
                             ivLookup,
                             lookup);
                    String exMsg = "The " + ivComponent + " bean in the " +
                                   ivModule + " module of the " + ivApplication +
                                   " application has conflicting configuration data in the XML" +
                                   " deployment descriptor. Conflicting " + "lookup-name" +
                                   " element values exist for multiple " +
                                   "message-destination-ref" + " elements with the same " +
                                   "message-destination-ref-name" +
                                   " element value : " + jndiName + ". The conflicting " +
                                   "lookup-name" + " element values are \"" + ivLookup +
                                   "\" and \"" + lookup + "\".";
                    throw new InjectionConfigurationException(exMsg);
                }
            }
            else
            {
                ivLookup = lookup;
                ivLookupInXml = true;
                curAnnotation.ivLookup = lookup;
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "ivLookup = " + ivLookup);
            }
        }

        //Loop through the InjectionTargets and call addInjectionTarget.... which
        //already accounts for duplicates (in case they duplicated some between the two ref definitions.
        List<InjectionTarget> targets = msgDestRef.getInjectionTargets();
        String targetName = null;
        String targetClassName = null;

        Class<?> injectionType = getInjectionClassType();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "targetType : " + injectionType);

        if (!targets.isEmpty())
        {
            for (InjectionTarget target : targets)
            {
                targetClassName = target.getInjectionTargetClassName();
                targetName = target.getInjectionTargetName();
                this.addInjectionTarget(injectionType, targetName, targetClassName);
            } //for loop
        } //targets !null

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "merge", this);
    }

    /**
     * Extract the fields from the ResourceEnvRef, and verify they match the values in the current
     * binding object and/or annotation exactly. If they do indeed match, add all the
     * InjectionTargets on the ResourceEnvRef parameter to the current binding. The code takes into
     * account the possibility of duplicates InjectionTargets and will only use one in case
     * where they duplicated between the two ref definitions.
     *
     * @param resourceEnvRef
     * @throws InjectionException
     */
    public void merge(ResourceEnvRef resourceEnvRef) throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "merge", resourceEnvRef);

        ResourceImpl curAnnotation = (ResourceImpl) this.getAnnotation();
        String jndiName = resourceEnvRef.getName();
        String curJndiName = curAnnotation.name();
        String mappedName = resourceEnvRef.getMappedName();
        String curMappedName = curAnnotation.mappedName();
        String typeName = resourceEnvRef.getTypeName();
        String lookup = resourceEnvRef.getLookupName(); // F743-21028.4
        if (lookup != null) {
            lookup = lookup.trim();
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "new=" + jndiName + ":" + mappedName + ":" + typeName + ":" + lookup);

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "cur=" + curJndiName + ":" + curMappedName + ":" + getInjectionClassTypeName() + ":" + ivLookup);

        //The mappedName parameter is "optional"
        if (curAnnotation.ivIsSetMappedName && mappedName != null)
        {
            if (!curMappedName.equals(mappedName))
            {
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                         ivComponent,
                         ivModule,
                         ivApplication,
                         "mapped-name",
                         "resource-env-ref",
                         "resource-env-ref-name",
                         getJndiName(),
                         curMappedName,
                         mappedName); // d479669
                String exMsg = "The " + ivComponent + " bean in the " +
                               ivModule + " module of the " + ivApplication +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "mapped-name" +
                               " element values exist for multiple " + "resource-env-ref" +
                               " elements with the same " + "resource-env-ref-name" +
                               " element value : " +
                               getJndiName() + ". The conflicting " + "mapped-name" +
                               " element values are " + curMappedName + " and " +
                               mappedName + "."; // d479669
                throw new InjectionConfigurationException(exMsg);
            }
        }
        else if (mappedName != null && !curAnnotation.ivIsSetMappedName)
        {
            curAnnotation.ivMappedName = mappedName;
            curAnnotation.ivIsSetMappedName = true;
        }

        setXMLType(typeName,
                   "resource-env-ref",
                   "resource-env-ref-name",
                   "resource-env-ref-type"); // F743-32443

        // -----------------------------------------------------------------------
        // Merge : lookup - "optional parameter                       F743-21028.4
        //
        // If present in XML, even if the empty string (""), it will override
        // any setting via annotations. An empty string would effectivly turn
        // off this setting.
        //
        // When a message-destination-ref appears multiple times in XML, an empty
        // string is considered to be a confilct with a non-empty string, since
        // both were explicitly specified.
        // -----------------------------------------------------------------------
        if (lookup != null)
        {
            if (ivLookupInXml)
            {
                if (!lookup.equals(ivLookup))
                {
                    Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                             ivComponent,
                             ivModule,
                             ivApplication,
                             "lookup-name",
                             "resource-env-ref",
                             "resource-env-ref-name",
                             jndiName,
                             ivLookup,
                             lookup);
                    String exMsg = "The " + ivComponent + " bean in the " +
                                   ivModule + " module of the " + ivApplication +
                                   " application has conflicting configuration data in the XML" +
                                   " deployment descriptor. Conflicting " + "lookup-name" +
                                   " element values exist for multiple " +
                                   "resource-env-ref" + " elements with the same " +
                                   "resource-env-ref-name" +
                                   " element value : " + jndiName + ". The conflicting " +
                                   "lookup-name" + " element values are \"" + ivLookup +
                                   "\" and \"" + lookup + "\".";
                    throw new InjectionConfigurationException(exMsg);
                }
            }
            else
            {
                ivLookup = lookup;
                ivLookupInXml = true;
                curAnnotation.ivLookup = lookup;
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "ivLookup = " + ivLookup);
            }
        }

        //Loop through the InjectionTargets and call addInjectionTarget.... which
        //already accounts for duplicates (in case they duplicated some between the two ref definitions.
        List<InjectionTarget> targets = resourceEnvRef.getInjectionTargets();
        String targetName = null;
        String targetClassName = null;

        Class<?> injectionType = getInjectionClassType();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "targetType : " + injectionType);

        if (!targets.isEmpty())
        {
            for (InjectionTarget target : targets)
            {
                targetClassName = target.getInjectionTargetClassName();
                targetName = target.getInjectionTargetName();
                this.addInjectionTarget(injectionType, targetName, targetClassName);
            } //for loop
        } //targets !null

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "merge", this);
    }

    /**
     * Extract the fields from the ResourceRef, and verify they match the values in the current
     * binding object and/or annotation exactly. If they do indeed match, add all the
     * InjectionTargets on the ResourceRef parameter to the current binding. The code takes into
     * account the possibility of duplicates InjectionTargets and will only use one in case
     * where they duplicated between the two ref definitions.
     *
     * @param resourceRef
     * @throws InjectionException
     */
    public void merge(ResourceRef resourceRef) throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "merge", resourceRef);

        ResourceImpl curAnnotation = (ResourceImpl) this.getAnnotation();
        String jndiName = resourceRef.getName();
        String curJndiName = curAnnotation.name();
        String mappedName = resourceRef.getMappedName();
        String curMappedName = curAnnotation.mappedName();
        String typeName = resourceRef.getType();
        boolean curShareable = curAnnotation.shareable();
        int resAuthType = resourceRef.getAuthValue();
        int resSharingScope = resourceRef.getSharingScopeValue();
        boolean shareable = resSharingScope == ResourceRef.SHARING_SCOPE_SHAREABLE;

        AuthenticationType authenticationType =
                        ResourceProcessor.convertAuthToEnum(resAuthType); // d543514
        AuthenticationType curAuthenticationType = curAnnotation.authenticationType();
        String lookup = resourceRef.getLookupName(); // F743-21028.4
        if (lookup != null) {
            lookup = lookup.trim();
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "new=" + jndiName + ":" + mappedName + ":" + authenticationType + ":" + shareable + ":" + typeName + ":" + lookup);

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "cur=" + curJndiName + ":" + curMappedName + ":" + curAuthenticationType + ":" + curShareable + ":" + getInjectionClassTypeName() + ":" + ivLookup);

        //The mappedName parameter is "optional"
        if (curAnnotation.ivIsSetMappedName && mappedName != null)
        {
            if (!curMappedName.equals(mappedName))
            {
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                         ivComponent,
                         ivModule,
                         ivApplication,
                         "mapped-name",
                         "resource-ref",
                         "res-ref-name",
                         getJndiName(),
                         curMappedName,
                         mappedName); // d479669
                String exMsg = "The " + ivComponent + " bean in the " +
                               ivModule + " module of the " + ivApplication +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "mapped-name" +
                               " element values exist for multiple " + "resource-ref" +
                               " elements with the same " + "res-ref-name" +
                               " element value : " +
                               getJndiName() + ". The conflicting " + "mapped-name" +
                               " element values are " + curMappedName + " and " +
                               mappedName + "."; // d479669
                throw new InjectionConfigurationException(exMsg);
            }
        }
        else if (mappedName != null && !curAnnotation.ivIsSetMappedName)
        {
            curAnnotation.ivMappedName = mappedName;
            curAnnotation.ivIsSetMappedName = true;
        }

        setXMLType(typeName,
                   "resource-ref",
                   "res-ref-name",
                   "res-type"); // F743-32443

        //The authenticationType parameter is "optional".
        if (curAnnotation.ivIsSetAuthenticationType && resAuthType != ResourceRef.AUTH_UNSPECIFIED)
        {
            if (!(curAnnotation.authenticationType() == authenticationType))
            {
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                         ivComponent,
                         ivModule,
                         ivApplication,
                         "res-auth",
                         "resource-ref",
                         "res-ref-name",
                         getJndiName(),
                         curAnnotation.authenticationType(),
                         authenticationType); // d479669
                String exMsg = "The " + ivComponent + " bean in the " +
                               ivModule + " module of the " + ivApplication +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "res-auth" +
                               " element values exist for multiple " + "resource-ref" +
                               " elements with the same " + "res-ref-name" + " element value : " +
                               getJndiName() + ". The conflicting " + "res-auth" +
                               " element values are " + curAnnotation.authenticationType() +
                               " and " + authenticationType + "."; // d479669
                throw new InjectionConfigurationException(exMsg);
            }
        }
        else if (resAuthType != ResourceRef.AUTH_UNSPECIFIED && !curAnnotation.ivIsSetAuthenticationType)
        {
            curAnnotation.ivAuthenticationType = authenticationType; // d543514
            curAnnotation.ivIsSetAuthenticationType = true;
        }

        //The resSharingScope parameter is "optional".
        if (curAnnotation.ivIsSetShareable && resSharingScope != ResourceRef.SHARING_SCOPE_UNSPECIFIED)
        {
            if (!(curAnnotation.shareable() == shareable))
            {
                Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                         ivComponent,
                         ivModule,
                         ivApplication,
                         "res-sharing-scope",
                         "resource-ref",
                         "res-ref-name",
                         getJndiName(),
                         curAnnotation.shareable(),
                         shareable); // d479669
                String exMsg = "The " + ivComponent + " bean in the " +
                               ivModule + " module of the " + ivApplication +
                               " application has conflicting configuration data in the XML" +
                               " deployment descriptor. Conflicting " + "res-sharing-scope" +
                               " element values exist for multiple " + "resource-ref" +
                               " elements with the same " + "res-ref-name" + " element value : " +
                               getJndiName() + ". The conflicting " + "res-sharing-scope" +
                               " element values are " + curAnnotation.shareable() +
                               " and " + shareable + "."; // d479669
                throw new InjectionConfigurationException(exMsg);
            }
        }
        else if (resSharingScope != ResourceRef.SHARING_SCOPE_UNSPECIFIED && !curAnnotation.ivIsSetShareable)
        {
            curAnnotation.ivShareable = shareable;
            curAnnotation.ivIsSetShareable = true;
        }

        // -----------------------------------------------------------------------
        // Merge : lookup - "optional parameter                       F743-21028.4
        //
        // If present in XML, even if the empty string (""), it will override
        // any setting via annotations. An empty string would effectivly turn
        // off this setting.
        //
        // When a message-destination-ref appears multiple times in XML, an empty
        // string is considered to be a confilct with a non-empty string, since
        // both were explicitly specified.
        // -----------------------------------------------------------------------
        if (lookup != null)
        {
            if (ivLookupInXml)
            {
                if (!lookup.equals(ivLookup))
                {
                    Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                             ivComponent,
                             ivModule,
                             ivApplication,
                             "lookup-name",
                             "resource-ref",
                             "res-ref-name",
                             jndiName,
                             ivLookup,
                             lookup);
                    String exMsg = "The " + ivComponent + " bean in the " +
                                   ivModule + " module of the " + ivApplication +
                                   " application has conflicting configuration data in the XML" +
                                   " deployment descriptor. Conflicting " + "lookup-name" +
                                   " element values exist for multiple " +
                                   "resource-ref" + " elements with the same " +
                                   "res-ref-name" +
                                   " element value : " + jndiName + ". The conflicting " +
                                   "lookup-name" + " element values are \"" + ivLookup +
                                   "\" and \"" + lookup + "\".";
                    throw new InjectionConfigurationException(exMsg);
                }
            }
            else
            {
                ivLookup = lookup;
                ivLookupInXml = true;
                curAnnotation.ivLookup = lookup;
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "ivLookup = " + ivLookup);
            }
        }

        //Loop through the InjectionTargets and call addInjectionTarget.... which
        //already accounts for duplicates (in case they duplicated some between the two ref definitions.
        List<InjectionTarget> targets = resourceRef.getInjectionTargets();
        String targetName = null;
        String targetClassName = null;

        Class<?> injectionType = loadClass(resourceRef.getType());
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "targetType : " + injectionType);

        if (!targets.isEmpty())
        {
            for (InjectionTarget target : targets)
            {
                targetClassName = target.getInjectionTargetClassName();
                targetName = target.getInjectionTargetName();
                this.addInjectionTarget(injectionType, targetName, targetClassName);
            } //for loop
        } //targets !null

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "merge", this);
    }

    @Override
    public void mergeSaved(InjectionBinding<Resource> injectionBinding) // d681743
    throws InjectionException
    {
        ResourceInjectionBinding resourceBinding = (ResourceInjectionBinding) injectionBinding;
        Resource resourceBindingAnn = resourceBinding.getAnnotation();
        Resource ann = getAnnotation();

        mergeSavedValue(getInjectionClassTypeName(), resourceBinding.getInjectionClassTypeName(), "type"); // F88163
        mergeSavedValue(ivLink, resourceBinding.ivLink, "message-destination-link");
        mergeSavedValue(ivLookup, resourceBinding.ivLookup, "lookup");
        mergeSavedValue(ann.authenticationType(), resourceBindingAnn.authenticationType(), "authentication-type");
        mergeSavedValue(ann.shareable(), resourceBindingAnn.shareable(), "shareable");
        mergeSavedValue(ivBindingName, resourceBinding.ivBindingName, "binding-name");
        mergeSavedValue(ivBindingValue, resourceBinding.ivBindingValue, "value");

        // The "XML type" can only be specified in XML, so we can only detect
        // mismatches if both bindings are specified in XML.  (The same is true
        // for env-entry-value, so the ivBindingValue check requires either the
        // env-entry-value or corresponding binding to match.)  We don't want
        // errors to be non-deterministic, but we don't know if we'll see, across
        // multiple components, whether the "saved" binding will annotation or
        // XML, which has an effect on subsequent bindings, so we skip validation
        // altogether.

        // Check bindings and extensions for conflicts.                     F88163
        List<ResourceRefConfig.MergeConflict> conflicts = ivResRefConfig.compareBindingsAndExtensions(resourceBinding.ivResRefConfig);
        ResourceRefConfig.MergeConflict primaryConflict = null;
        for (ResourceRefConfig.MergeConflict conflict : conflicts)
        {
            // ResourceRefConfig is only created for an actual resource-ref.  We
            // already checked binding-name above, so avoid spurious conflicts.
            if (!conflict.getAttributeName().equals("binding-name"))
            {
                primaryConflict = conflict;
                Tr.error(tc, "CONFLICTING_REFERENCES_CWNEN0062E",
                         ivComponent,
                         resourceBinding.ivComponent,
                         ivModule,
                         ivApplication,
                         conflict.getAttributeName(),
                         getJndiName(),
                         conflict.getValue1(),
                         conflict.getValue2());
            }
        }

        if (primaryConflict != null)
        {
            throw new InjectionConfigurationException("The " + getJndiName() +
                                                      " reference has conflicting values for the " + primaryConflict.getAttributeName() +
                                                      " attribute: " + primaryConflict.getValue1() +
                                                      " and " + primaryConflict.getValue2());
        }
    }

    @Override
    public void setInjectionClassType(Class<?> type)
                    throws InjectionException
    {
        // d661640 - Translate primitive types from annotated fields/methods to
        // their boxed equivalents.
        if (type != null && type.isPrimitive())
        {
            if (type == boolean.class)
            {
                type = Boolean.class;
            }
            else if (type == byte.class)
            {
                type = Byte.class;
            }
            else if (type == char.class)
            {
                type = Character.class;
            }
            else if (type == short.class)
            {
                type = Short.class;
            }
            else if (type == int.class)
            {
                type = Integer.class;
            }
            else if (type == long.class)
            {
                type = Long.class;
            }
            else if (type == float.class)
            {
                type = Float.class;
            }
            else if (type == double.class)
            {
                type = Double.class;
            }
        }

        super.setInjectionClassType(type);
    }

    /**
     * Sets the injection type as specified in XML.
     *
     * @param typeName the type name specified in XML
     * @param element the XML ref element
     * @param nameElement the XML name element in the ref element
     * @param typeElement the XML type element in the ref element
     * @throws InjectionConfigurationException
     */
    private void setXMLType(String typeName, String element, String nameElement, String typeElement) // F743-32443
    throws InjectionConfigurationException
    {
        if (ivNameSpaceConfig.getClassLoader() == null)
        {
            setInjectionClassTypeName(typeName);
        }
        else
        {
            Class<?> type = loadClass(typeName);
            //The type parameter is "optional"
            if (type != null)
            {
                ResourceImpl curAnnotation = (ResourceImpl) getAnnotation();
                if (curAnnotation.ivIsSetType)
                {
                    Class<?> curType = getInjectionClassType();
                    // check that value from xml is a subclasss, if not throw an error
                    Class<?> mostSpecificClass = mostSpecificClass(type, curType);
                    if (mostSpecificClass == null)
                    {
                        Tr.error(tc, "CONFLICTING_XML_VALUES_CWNEN0052E",
                                 ivComponent,
                                 ivModule,
                                 ivApplication,
                                 typeElement,
                                 element,
                                 nameElement,
                                 getJndiName(),
                                 curType,
                                 type); // d479669
                        String exMsg = "The " + ivComponent +
                                       " bean in the " + ivModule +
                                       " module of the " + ivApplication +
                                       " application has conflicting configuration data in the XML" +
                                       " deployment descriptor. Conflicting " + typeElement +
                                       " element values exist for multiple " + element +
                                       " elements with the same " + nameElement + " element value : " +
                                       getJndiName() + ". The conflicting " + typeElement +
                                       " element values are " + curType + " and " +
                                       type + "."; // d479669
                        throw new InjectionConfigurationException(exMsg);
                    }
                    curAnnotation.ivType = mostSpecificClass;
                }
                else
                {
                    curAnnotation.ivType = type;
                    curAnnotation.ivIsSetType = true;
                }
            }
        }
    }

    /**
     * Checks if the specified type is compatible for merging with the type
     * that has already specified for this binding.
     *
     * @param type a type object returned from {@link #getEnvEntryType}
     */
    private boolean isEnvEntryTypeCompatible(Object newType) // F743-32443
    {
        Class<?> curType = getInjectionClassType();
        if (curType == null)
        {
            return true;
        }

        return isClassesCompatible((Class<?>) newType, getInjectionClassType());
    }

    /**
     * Sets the type of this binding.
     *
     * @param annotation the merged data
     * @param type a type object returned from {@link #getEnvEntryType}
     */
    public void setEnvEntryType(ResourceImpl annotation, Object type) // F743-32443
    throws InjectionException
    {
        if (type instanceof String)
        {
            setInjectionClassTypeName((String) type);
        }
        else
        {
            Class<?> classType = (Class<?>) type;
            annotation.ivType = classType;
            annotation.ivIsSetType = true;
            setInjectionClassType(classType);
        }
    }

    private static final Class<?>[] ENV_ENTRY_TYPES = {
                                                       String.class,
                                                       Integer.class,
                                                       Boolean.class,
                                                       Double.class,
                                                       Byte.class,
                                                       Short.class,
                                                       Long.class,
                                                       Float.class,
                                                       Character.class,
                                                       Class.class, // F743-25853.1
    };

    /**
     * Returns the Class specified on the env-entry-type element or null.
     *
     * @param envEntry the in memory representation of the env-entry
     */
    // F743-22218.3
    Object getEnvEntryType(EnvEntry envEntry)
                    throws InjectionConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        Object type = null;

        String typeName = envEntry.getTypeName();
        if (typeName != null)
        {
            for (Class<?> typeClass : ENV_ENTRY_TYPES)
            {
                if (typeName.equals(typeClass.getName()))
                {
                    type = typeClass;
                    break;
                }
            }

            if (type == null)
            {
                if (ivNameSpaceConfig.getClassLoader() == null)
                {
                    // F743-32443 - We don't have a class loader, so we can't
                    // validate the type.  Store it as a string for now;
                    // EnvEntryEnumSerializable will validate it later when used.
                    type = typeName;
                }
                else
                {
                    Class<?> classType = loadClass(typeName);
                    if (classType == null || !classType.isEnum())
                    {
                        Tr.error(tc, "INVALID_ENV_ENTRY_TYPE_CWNEN0064E",
                                 envEntry.getName(), ivModule, ivApplication, typeName);
                        throw new InjectionConfigurationException("A type, which is not valid, has been specified for the " +
                                                                  envEntry.getName() + " simple environment entry in the " +
                                                                  ivModule + " module of the " + ivApplication +
                                                                  " application: '" + typeName + "'.");
                    }

                    type = classType;
                }
            }
        }
        // d654504
        else
        {
            // Default to type of Object, to avoid later NPE when checking to
            // see if the specified injection type is compatible with the
            // variable we are injecting into.
            if (isTraceOn && tc.isDebugEnabled())
            {
                Tr.debug(tc, "EnvEntry XML type is not set.");
            }
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "env-entry-type = " +
                         ((type == null) ? "null" : type.getClass().getName()));

        return type;
    }

    /**
     * Checks if the type of this binding is the same as the specified type.
     *
     * @param resolverType the type used for resolving.
     * @return
     */
    private boolean isEnvEntryType(Class<?> resolverType) // F743-32443
    {
        Class<?> injectType = getInjectionClassType();
        return injectType == null ? resolverType.getName().equals(getInjectionClassTypeName())
                        : resolverType == injectType;
    }

    /**
     * Converts the env-entry-value, if specified, to the proper object
     * type to be injected or returned on lookup. <p>
     *
     * Null is returned if either an env-entry-value has not been
     * specified, or the value could not be converted to the type
     * of the env-entry.
     */
    // F743-22218.3
    protected final Object resolveEnvEntryValue(String value) // F743-29779
    throws InjectionConfigurationException
    {
        // -----------------------------------------------------------------------
        // If an env-entry-value was not provided, then there is nothing to
        // convert/resolve... just return null.  This env-entry should just
        // be ignored if there is also no lookup value, or a warning logged
        // if there are also no injection targets.
        // -----------------------------------------------------------------------
        if (value == null) {
            return null;
        }

        // -----------------------------------------------------------------------
        // EJB 1.1 allows env entries to be one of the following java types.
        // Construct the right type of object to bind.
        // -----------------------------------------------------------------------
        Object injectionObject = null;

        if (isEnvEntryType(String.class))
        {
            injectionObject = value;
        }
        else if (isEnvEntryType(Integer.class))
        {
            try
            {
                injectionObject = Integer.valueOf(value);
            } catch (NumberFormatException e)
            {
                // this should not happen
                FFDCFilter.processException(e, CLASS_NAME + ".resolveEnvEntryValue",
                                            "337", this, new Object[] { value });
                Tr.warning(tc, "NUMBER_FORMAT_EXCEPTION_CWNEN0013W",
                           getJndiName(), value, e);
                if (isValidationFailable()) // fail if enabled          F743-14449
                {
                    throw new InjectionConfigurationException("The " + value + " value for the " + getJndiName() +
                                                              " simple environment entry in the " + ivModule +
                                                              " module of the " + ivApplication + " application" +
                                                              " cannot be converted to an integer.", e);
                }
            }
        }
        else if (isEnvEntryType(Boolean.class))
        {
            injectionObject = Boolean.valueOf(value);

            // If the "Boolean" is false, then make sure the correct
            // syntax was really provided, as anything but "true"
            // will result in false. d320818
            if (!((Boolean) injectionObject).booleanValue() &&
                !value.equalsIgnoreCase("false"))
            {
                Tr.warning(tc, "INVALID_BOOLEAN_FORMAT_CWNEN0014W", getJndiName(), value);
                if (isValidationFailable()) // fail if enabled          F743-14449
                {
                    throw new InjectionConfigurationException("The " + value + " value for the " + getJndiName() +
                                                              " simple environment entry in the " + ivModule +
                                                              " module of the " + ivApplication + " application" +
                                                              " is not a proper boolean value.");
                }
            }
        }
        else if (isEnvEntryType(Double.class))
        {
            try
            {
                injectionObject = Double.valueOf(value);
            } catch (NumberFormatException e)
            {
                // this should not happen
                FFDCFilter.processException(e, CLASS_NAME + ".resolveEnvEntryValue",
                                            "369", this, new Object[] { value });
                Tr.warning(tc, "NUMBER_FORMAT_EXCEPTION_CWNEN0013W",
                           getJndiName(), value, e);
                if (isValidationFailable()) // fail if enabled          F743-14449
                {
                    throw new InjectionConfigurationException("The " + value + " value for the " + getJndiName() +
                                                              " simple environment entry in the " + ivModule +
                                                              " module of the " + ivApplication + " application" +
                                                              " cannot be converted to a double.", e);
                }
            }
        }
        else if (isEnvEntryType(Byte.class))
        {
            try
            {
                injectionObject = Byte.valueOf(value);
            } catch (NumberFormatException e)
            {
                // this should not happen
                FFDCFilter.processException(e, CLASS_NAME + ".resolveEnvEntryValue",
                                            "386", this, new Object[] { value });
                Tr.warning(tc, "NUMBER_FORMAT_EXCEPTION_CWNEN0013W",
                           getJndiName(), value, e);
                if (isValidationFailable()) // fail if enabled          F743-14449
                {
                    throw new InjectionConfigurationException("The " + value + " value for the " + getJndiName() +
                                                              " simple environment entry in the " + ivModule +
                                                              " module of the " + ivApplication + " application" +
                                                              " cannot be converted to a byte.", e);
                }
            }
        }
        else if (isEnvEntryType(Short.class))
        {
            try
            {
                injectionObject = Short.valueOf(value);
            } catch (NumberFormatException e)
            {
                // this should not happen
                FFDCFilter.processException(e, CLASS_NAME + ".resolveEnvEntryValue",
                                            "403", this, new Object[] { value });
                Tr.warning(tc, "NUMBER_FORMAT_EXCEPTION_CWNEN0013W",
                           getJndiName(), value, e);
                if (isValidationFailable()) // fail if enabled          F743-14449
                {
                    throw new InjectionConfigurationException("The " + value + " value for the " + getJndiName() +
                                                              " simple environment entry in the " + ivModule +
                                                              " module of the " + ivApplication + " application" +
                                                              " cannot be converted to a short.", e);
                }
            }
        }
        else if (isEnvEntryType(Long.class))
        {
            try
            {
                injectionObject = Long.valueOf(value);
            } catch (NumberFormatException e)
            {
                // this should not happen
                FFDCFilter.processException(e, CLASS_NAME + ".resolveEnvEntryValue",
                                            "366", this, new Object[] { value });
                Tr.warning(tc, "NUMBER_FORMAT_EXCEPTION_CWNEN0013W",
                           getJndiName(), value, e);
                if (isValidationFailable()) // fail if enabled          F743-14449
                {
                    throw new InjectionConfigurationException("The " + value + " value for the " + getJndiName() +
                                                              " simple environment entry in the " + ivModule +
                                                              " module of the " + ivApplication + " application" +
                                                              " cannot be converted to a long.", e);
                }
            }
        }
        else if (isEnvEntryType(Float.class))
        {
            try
            {
                injectionObject = Float.valueOf(value);
            } catch (NumberFormatException e)
            {
                // this should not happen
                FFDCFilter.processException(e, CLASS_NAME + ".resolveEnvEntryValue",
                                            "437", this, new Object[] { value });
                Tr.warning(tc, "NUMBER_FORMAT_EXCEPTION_CWNEN0013W",
                           getJndiName(), value, e);
                if (isValidationFailable()) // fail if enabled          F743-14449
                {
                    throw new InjectionConfigurationException("The " + value + " value for the " + getJndiName() +
                                                              " simple environment entry in the " + ivModule +
                                                              " module of the " + ivApplication + " application" +
                                                              " cannot be converted to a float.", e);
                }
            }
        }
        else if (isEnvEntryType(Character.class))
        {
            try
            {
                injectionObject = Character.valueOf(value.charAt(0));
            } catch (Throwable e)
            {
                // this should not happen
                FFDCFilter.processException(e, CLASS_NAME + ".resolveEnvEntryValue",
                                            "454", this, new Object[] { value });
                Tr.warning(tc, "THROWABLE_WHILE_CONSTRUCTING_JAVA_COMP_ENV_CWNEN0015W",
                           getJndiName(), value, e);
                if (isValidationFailable()) // fail if enabled          F743-14449
                {
                    throw new InjectionConfigurationException("The " + value + " value for the " + getJndiName() +
                                                              " simple environment entry in the " + ivModule +
                                                              " module of the " + ivApplication + " application" +
                                                              " cannot be converted to a character.", e);
                }
            }
        }

        // -----------------------------------------------------------------------
        // The following data types were added in EJB 3.1 specification
        // -----------------------------------------------------------------------

        else if (isEnvEntryType(Class.class)) // F743-25853.1
        {
            if (ivNameSpaceConfig.getClassLoader() == null)
            {
                // F743-32443 - We don't have a class loader, so use a Reference.
                injectionObject = EnvEntryObjectFactory.createClassReference(getJndiName(), ivNameSpaceConfig, value); // d680699
            }
            else
            {
                injectionObject = loadClass(value);
            }
        }
        else if (ivNameSpaceConfig.getClassLoader() == null)
        {
            String className = getInjectionClassTypeName();
            if (className == null) // d701200
            {
                if (isValidationLoggable()) // F50309.6
                {
                    invalidEnvEntryType(null);
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "missing env-entry-type: ", toString());
                }
            }

            // F743-32443 - We don't have a class loader, so use a Reference.
            injectionObject = EnvEntryObjectFactory.createEnumReference(getJndiName(), ivNameSpaceConfig,
                                                                        getInjectionClassTypeName(), value); // d680699
        }
        else
        {
            Class<?> type = getInjectionClassType();
            if (type != null && type.isEnum()) // F743-25853
            {
                try
                {
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    Object enumValue = Enum.valueOf((Class<? extends Enum>) type, value);
                    injectionObject = enumValue;
                } catch (IllegalArgumentException ex)
                {
                    FFDCFilter.processException(ex, CLASS_NAME + ".resolveEnvEntryValue",
                                                "1946", this, new Object[] { value });
                    Tr.error(tc, "INVALID_ENUM_IDENTIFIER_CWNEN0063E",
                             getJndiName(), ivModule, ivApplication, type.getName(), value);
                    throw new InjectionConfigurationException("CWNEN0063E: The " + getJndiName() + " simple environment entry in the " +
                                                              ivModule + " module of the " + ivApplication +
                                                              " application, which is the " + type.getName() +
                                                              " Enum type, is not set to a valid Enum identifier: '" +
                                                              value + "'.");
                }
            }

            // -----------------------------------------------------------------------
            // When type is not supported.... log a warning
            // -----------------------------------------------------------------------

            else
            {
                // This occurs if an @Resource annotation is applied to a field or
                // method that is not a primitive type, and then tried to override
                // that with an env-entry in XML. To be consistent with legacy
                // support, juat a warning is logged, and any value specified will
                // be ignored.                                             F743-22218.3
                invalidEnvEntryType(type == null ? null : type.toString()); // d701200
            }
        }

        return injectionObject;
    }

    private void invalidEnvEntryType(String typeName) // d701200
    throws InjectionConfigurationException
    {
        Tr.warning(tc, "INVALID_TYPE_IN_JAVA_COMP_ENV_CWNEN0016W",
                   typeName, getJndiName(), ivComponent, ivModule);
        if (isValidationFailable()) // fail if enabled          F743-14449
        {
            throw new InjectionConfigurationException("The " + typeName + " type, which is not valid, has been specified for the " +
                                                      getJndiName() + " simple environment entry in the " +
                                                      ivComponent + " component in the " +
                                                      ivModule + " module of the " + ivApplication +
                                                      " application.");
        }
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

        Class<?> type = getInjectionClassType();
        if (type != null && type != Object.class) {
            sb.append(", type=").append(type.getName());
        } else {
            String typeName = getInjectionClassTypeName();
            if (typeName != null) {
                sb.append(", typeName=").append(typeName);
            }
        }

        Resource ann = getAnnotation();
        if (ann != null) {
            sb.append(", auth-type=").append(ann.authenticationType());
            sb.append(", shareable=").append(ann.shareable());
        }
        if (ivLink != null) {
            sb.append(", msg-link=").append(ivLink);
        }
        if (ivEnvValue != null) { // F743-22218.3
            sb.append(", env-entry-value=").append(ivEnvValue);
        }
        if (ivLookup != null) {
            sb.append(", lookup=").append(ivLookup);
        }

        return sb.append(']').toString();
    }
}
