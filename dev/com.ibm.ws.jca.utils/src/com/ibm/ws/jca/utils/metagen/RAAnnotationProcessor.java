/*******************************************************************************
 * Copyright (c) 2014,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.metagen;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.resource.spi.Activation;
import javax.resource.spi.AdministeredObject;
import javax.resource.spi.AuthenticationMechanism;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.Connector;
import javax.resource.spi.ConnectionDefinitions;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.SecurityPermission;
import javax.resource.spi.TransactionSupport;
import javax.resource.spi.work.WorkContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annotations.ModuleAnnotations;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jca.utils.xml.ra.RaActivationSpec;
import com.ibm.ws.jca.utils.xml.ra.RaAdminObject;
import com.ibm.ws.jca.utils.xml.ra.RaAuthenticationMechanism;
import com.ibm.ws.jca.utils.xml.ra.RaConfigProperty;
import com.ibm.ws.jca.utils.xml.ra.RaConnectionDefinition;
import com.ibm.ws.jca.utils.xml.ra.RaConnector;
import com.ibm.ws.jca.utils.xml.ra.RaDescription;
import com.ibm.ws.jca.utils.xml.ra.RaDisplayName;
import com.ibm.ws.jca.utils.xml.ra.RaInboundResourceAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaMessageAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaMessageListener;
import com.ibm.ws.jca.utils.xml.ra.RaOutboundResourceAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaResourceAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaSecurityPermission;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;

/**
 * Process annotations in the resource adapter classes and combine with the definitions in the
 * ra.xml/wlp-ra.xml to create the combined definitions that will be processed to create the metatype
 * for the resource adapter.
 */
public class RAAnnotationProcessor {

    private static final TraceComponent tc = Tr.register(RAAnnotationProcessor.class);

    private final RaConnector deploymentDescriptor;
    private final String adapterName;
    private final ClassLoader raClassLoader;
    private final ModuleAnnotations raAnnotations;
    private final List<String> raClassNames;

    private final LinkedList<Class<?>> connectorClasses = new LinkedList<Class<?>>();
    private final LinkedList<Class<?>> activationClasses = new LinkedList<Class<?>>();
    private final LinkedList<Class<?>> connDefClasses = new LinkedList<Class<?>>();
    private final LinkedList<Class<?>> connDefsClasses = new LinkedList<Class<?>>();
    private final LinkedList<Class<?>> adminObjectClasses = new LinkedList<Class<?>>();

    /**
     * Sort the resource adapter classes into lists for each type of annotation and a catch all list
     * for classes that don't have the resource adapter annotations.
     *
     * @param adapterName   A name the resource adapter can be identified with
     * @param dd            The deployment descriptor is represented by a RaConnector constructed from ra.xml connector entry or null if there is no ra.xml
     * @param raClassLoader Class loader provided by caller to load the resource adapter classes
     * @param raClassNames  List of all the classes provided with the resource adapter
     * @throws ResourceAdapterInternalException
     */
    public RAAnnotationProcessor(String adapterName, RaConnector dd, ClassLoader raClassLoader, ModuleAnnotations raAnnotations,
                                 List<String> raClassNames) throws ResourceAdapterInternalException {
        this.deploymentDescriptor = dd;
        this.adapterName = adapterName;
        this.raClassLoader = raClassLoader;
        this.raAnnotations = raAnnotations;
        this.raClassNames = raClassNames;
    }

    private void findAnnotatedClasses() throws ResourceAdapterInternalException {
        if (raAnnotations == null) {
            findAnnotatedClassesUsingReflection();
        } else {
            findAnnotatedClassesUsingTargets();
        }
    }

    private void findAnnotatedClassesUsingReflection() throws ResourceAdapterInternalException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        Class<?> raClass = null;
        try {
            for (String className : raClassNames) {
                raClass = raClassLoader.loadClass(className);

                if (raClass.getAnnotation(Connector.class) != null) {
                    connectorClasses.add(raClass);
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "class " + className + " has @Connector");
                } else if (raClass.getAnnotation(Activation.class) != null) {
                    activationClasses.add(raClass);
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "class " + className + " has @Activation");
                } else if (raClass.getAnnotation(ConnectionDefinition.class) != null) {
                    connDefClasses.add(raClass);
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "class " + className + " has @ConnectionDefinition");
                } else if (raClass.getAnnotation(ConnectionDefinitions.class) != null) {
                    connDefsClasses.add(raClass);
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "class " + className + " has @ConnectionDefinitions");
                } else if (raClass.getAnnotation(AdministeredObject.class) != null) {
                    adminObjectClasses.add(raClass);
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "class " + className + " has @AdministeredObject");
                }
            }
        } catch (NoClassDefFoundError e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw new ResourceAdapterInternalException(e);
        }

    }

    private void findAnnotatedClassesUsingTargets() throws ResourceAdapterInternalException {
        AnnotationTargets_Targets targets;
        try {
            targets = raAnnotations.getAnnotationTargets();
        } catch (UnableToAdaptException e) {
            throw new ResourceAdapterInternalException(e);
        }

        findAnnotatedClassesUsingTargets(targets, Connector.class, connectorClasses);
        findAnnotatedClassesUsingTargets(targets, Activation.class, activationClasses);
        findAnnotatedClassesUsingTargets(targets, ConnectionDefinition.class, connDefClasses);
        findAnnotatedClassesUsingTargets(targets, ConnectionDefinitions.class, connDefsClasses);
        findAnnotatedClassesUsingTargets(targets, AdministeredObject.class, adminObjectClasses);
    }

    private void findAnnotatedClassesUsingTargets(AnnotationTargets_Targets targets,
                                                  Class<? extends Annotation> annoClass,
                                                  List<Class<?>> classes) throws ResourceAdapterInternalException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        Set<String> classNames = targets.getAnnotatedClasses(annoClass.getName());
        for (String className : classNames) {
            try {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "class " + className + " has @" + annoClass.getSimpleName());
                classes.add(raClassLoader.loadClass(className));
            } catch (ClassNotFoundException e) {
                throw new ResourceAdapterInternalException(e);
            }
        }
    }

    /**
     * Create a RaConnector xml object and all its associated xml objects
     * that represents the combined ra.xml, wlp-ra.xml, and annotations, if any
     * that are present in the rar file.
     *
     * @return RaConnector that represents the resource adapter instance
     * @throws ResourceAdapterInternalException if any JCA spec violations are detected
     */
    public RaConnector getProcessedConnector() throws ResourceAdapterInternalException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String jcaVersion = getAdapterVersion(deploymentDescriptor);
        boolean processAnno = checkProcessAnnotations(deploymentDescriptor, jcaVersion);

        if (!processAnno) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "Skip annotation processing and return the RaConnector that was passed in");
            return deploymentDescriptor;
        }

        findAnnotatedClasses();

        // JCA 1.6 spec
        //   The implementation class name of the ResourceAdapter interface is specified in
        //   the resource adapter deployment descriptor or through the Connector annotation
        //   described in Section 18.4, “@Connector” on page 18-6.
        //
        //   It is optional for a resource adapter implementation to bundle a JavaBean class
        //   implementing the javax.resource.spi.ResourceAdapter interface (see
        //   Section 5.3.1, “ResourceAdapter JavaBean and Bootstrapping a Resource Adapter
        //   Instance” on page 5-4). In particular, a resource adapter implementation that only
        //   performs outbound communication to the EIS might not provide a JavaBean that
        //   implements the ResourceAdapter interface or a JavaBean annotated with the
        //   Connector annotation.
        //
        // If the descriptor has a resource adapter descriptor that has the name of the resource adapter class
        // then
        //   If there are one or more @Connector,
        //     then need to verify the class is annotated by only one of them or none of them
        //   If no classes are annotated with @Connector, then verify the class can be loaded
        // If there isn't a resource adapter class specified in the descriptor or there isn't a ra.xml,
        //   then verify there is only one class annotated with @Connector
        //
        // It is not necessary to locate a JavaBean that implements the ResourceAdapter interface.

        Class<?> resourceAdapterClass = null;
        if (deploymentDescriptor != null) {
            RaResourceAdapter rxRA = deploymentDescriptor.getResourceAdapter();
            if (rxRA != null) {
                String rxAdapterClassName = rxRA.getResourceAdapterClass();
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "rxAdapterClassName: ", rxAdapterClassName);
                if (rxAdapterClassName != null) {
                    // look to see if this class name is in the list of classes annotated with @Connector
                    for (Class<?> connectorClass : connectorClasses) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "connectorClass to examine: ", connectorClass);
                        if (rxAdapterClassName.equals(connectorClass.getName())) {
                            resourceAdapterClass = connectorClass;
                            if (trace && tc.isDebugEnabled())
                                Tr.debug(this, tc, "connectorClasses - resourceAdapterClass: ", resourceAdapterClass);
                            break;
                        }
                    } // end for ClassInfo : connectorClasses

                    // if an annotated class was not found, check the <resourceadapter-class> is present by loading it
                    if (resourceAdapterClass == null) {
                        try {
                            resourceAdapterClass = raClassLoader.loadClass(rxAdapterClassName);
                            if (trace && tc.isDebugEnabled())
                                Tr.debug(this, tc, "raClassLoader - resourceAdapterClass: ", resourceAdapterClass);
                        } catch (ClassNotFoundException e) {
                            throw new ResourceAdapterInternalException(Tr.formatMessage(tc, "J2CA9904.required.raclass.missing", rxAdapterClassName, adapterName), e);
                        }
                    } // end adapterClass == null
                } else { // rxAdapterClass == null, check for class annotated with @Connector
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "ra.xml does not contain a <resourceadapter-class> entry");
                }
            } else { // ra.xml does not have a <resourceadapter> entry
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "ra.xml does not contain a <resourceadapter> entry");
            }
        } else {
            // rar does not have a ra.xml
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "rar does not contain a ra.xml", resourceAdapterClass);
        }

        // If resource adapter class was not found, do @Connector annotation validation and try to get the
        // resource adapter class from there.
        if (resourceAdapterClass == null) {
            if (connectorClasses.size() == 0) {
                if (trace && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "rar does not contain a class annotated with @Connector");
                    // throw new ResourceAdapterInternalException(Tr.formatMessage(tc, "J2CA9923.connector.anno.missing", adapterName));
                }
            } else if (connectorClasses.size() > 1) {
                throw new ResourceAdapterInternalException(Tr.formatMessage(tc, "J2CA9922.multiple.connector.anno.found", adapterName));
            } else { // there is only one annotated connectorClass
                resourceAdapterClass = connectorClasses.get(0);
            }
        }

        RaConnector connector = processConnector(resourceAdapterClass, deploymentDescriptor);

        return connector;
    }

    /**
     * Process all the metatype information provided by the rar file to create
     * xml objects that can be processed to create the Liberty metatype.
     *
     * @param connectorClass - the class from <resourceadapter-class> or null if <resourceadapter-class> was not specified
     * @param rxConnector    - can be null if there wasn't an ra.xml in the rar file.
     *                           This rxConnector is the combined ra.xml and wlp-ra.xml, if any.
     * @return RaConnector representing all processed and combined metatype information
     * @throws ResourceAdapterInternalException
     */
    private RaConnector processConnector(Class<?> connectorClass, RaConnector rxConnector) throws ResourceAdapterInternalException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        RaConnector primaryConnector = new RaConnector();

        // get the parsed ra.xml file ------------------------------------------
        RaResourceAdapter rxRA = rxConnector == null ? null : rxConnector.getResourceAdapter();
        if (trace && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "rxRA", rxRA);
            if (rxRA != null)
                Tr.debug(this, tc, "rxRA.getResourceAdapterClass", rxRA.getResourceAdapterClass());
        }

        // Create empty shell --------------------------------------------------
        RaResourceAdapter primaryRA = null;

        // These will be set as needed
        RaOutboundResourceAdapter primaryOutbound = null;
        RaInboundResourceAdapter primaryInbound = new RaInboundResourceAdapter();
        RaMessageAdapter primaryMessageAdapter = new RaMessageAdapter();

        // parse annotations ---------------------------------------------------
        // Convert all the info from the annotations to xml.Ra classes
        // This info will be merged with the ra.xml info, if any
        RaConnector annoConnector = null;
        List<RaConfigProperty> annoRAConfigProperties = new LinkedList<RaConfigProperty>();
        List<RaConnectionDefinition> annoDefinitions = new LinkedList<RaConnectionDefinition>();
        List<RaMessageListener> annoMsgListeners = new LinkedList<RaMessageListener>();
        List<RaAdminObject> annoAdminObjects = new LinkedList<RaAdminObject>();

        // At this point, at most one of the classes annotated with @Connector will match
        // the class specified in ra.xml, if any, OR there will be none or only one class that is annotated
        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "Number of classes annotated with @Connector is " + connectorClasses.size());
        if (!connectorClasses.isEmpty()) {
            for (Class<?> annoConnectorClass : connectorClasses) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "annotated class is " + annoConnectorClass.getName());
                if (connectorClass != null) {
                    if (connectorClass.getName().equals(annoConnectorClass.getName())) {
                        annoConnector = getAnnotatedConnector(annoConnectorClass);
                        if (trace && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "<resourceadapter-class> set to" + connectorClass.getName());
                        }
                        annoRAConfigProperties.addAll(getAnnotatedConfigProperties(annoConnectorClass, true));
                        break;
                    }
                } else {
                    annoConnector = getAnnotatedConnector(annoConnectorClass);
                    annoRAConfigProperties.addAll(getAnnotatedConfigProperties(annoConnectorClass, true));
                }
            }
        }
        if (connectorClass != null && !connectorClasses.contains(connectorClass)) {
            // what if there is no @Connector but the class mentioned in ra.xml is annotated with @ConfigProperty
            annoRAConfigProperties.addAll(getAnnotatedConfigProperties(connectorClass, true));
        }

        for (Class<?> connDefClass : connDefClasses)
            annoDefinitions.add(getAnnotatedConnectionDefinition(connDefClass));
        for (Class<?> connDefClass : connDefsClasses)
            annoDefinitions.addAll(getAnnotatedConnectionDefinitions(connDefClass));

        for (Class<?> activationClass : activationClasses)
            annoMsgListeners.addAll(getAnnotatedMessageListeners(activationClass));

        for (Class<?> aoClass : adminObjectClasses)
            annoAdminObjects.addAll(getAnnotatedAdminObjects(aoClass));

        // begin processing ----------------------------------------------------
        if (rxRA != null) {
            // need to process/merge ra.xml and annotations
            // -> BEGIN process connector XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "processing ra.xml resource adapter");

            primaryConnector = mergeConnectors(rxConnector, annoConnector);
            // The resource adapter is created by mergeConnectors
            primaryRA = primaryConnector.getResourceAdapter();

            // The outbound connector may have been created as part of merging connectors
            if (primaryRA.getOutboundResourceAdapter() != null) {
                primaryOutbound = primaryRA.getOutboundResourceAdapter();
            } else {
                primaryOutbound = new RaOutboundResourceAdapter();
                primaryRA.setOutboundResourceAdapter(primaryOutbound);
            }
            // -> END process connector XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

            // -> BEGIN process RA config properties XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
            for (RaConfigProperty p : rxRA.getConfigProperties()) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "RA config property from xml: " + p);
            }
            primaryRA.getConfigProperties().addAll(mergeConfigProperties(rxRA.getConfigProperties(), annoRAConfigProperties));
            // <- END process RA config properties XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

            // -> BEGIN process outbound resource adapter XXXXXXXXXXXXXXXXXXXXXXXXXX
            RaOutboundResourceAdapter rxOutbound = rxRA.getOutboundResourceAdapter();
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "processing outbound connection definitions", rxOutbound);

            if (rxOutbound != null) {
                List<RaConnectionDefinition> rxDefinitions = rxOutbound.getConnectionDefinitions();
                if (!rxDefinitions.isEmpty()) {
                    // merge ra.xml and annotated connection definitions
                    primaryOutbound.getConnectionDefinitions().addAll(mergeConnectionDefinitions(rxDefinitions, annoDefinitions));
                } else {
                    // there are no connection definitions in the ra.xml, so just
                    // add the annotated connection definitions to the primary
                    primaryOutbound.getConnectionDefinitions().addAll(annoDefinitions);
                }
            } else {
                // there is no ra.xml outbound element, so just add the annotated
                // connection definitions to the primary
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "no ra.xml, primaryOutbound: " + primaryOutbound);
                if (primaryOutbound != null)
                    primaryOutbound.getConnectionDefinitions().addAll(annoDefinitions);
            }
            // <- END process outbound resource adapter XXXXXXXXXXXXXXXXXXXXXXXXXX

            // -> BEGIN process message listeners XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "processing inbound message listeners");

            RaInboundResourceAdapter rxInbound = rxRA.getInboundResourceAdapter();
            primaryInbound.setMessageAdapter(primaryMessageAdapter);
            primaryRA.setInboundResourceAdapter(primaryInbound);
            boolean messageListenersMerged = false;
            if (rxInbound != null && rxInbound.getMessageAdapter() != null) {
                List<RaMessageListener> rxMsgListeners = rxInbound.getMessageAdapter().getMessageListeners();
                if (!rxMsgListeners.isEmpty()) {
                    // merge ra.xml and annotated message listeners
                    primaryMessageAdapter.getMessageListeners().addAll(mergeMessageListeners(rxMsgListeners, (LinkedList<RaMessageListener>) annoMsgListeners));
                    messageListenersMerged = true;
                }
            }
            if (!messageListenersMerged) {
                // there is no ra.xml message listeners, so just add the annotated
                // message listeners to the primary
                primaryMessageAdapter.getMessageListeners().addAll(annoMsgListeners);
            }
            // <- END process message listeners XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

            // -> BEGIN process admin objects XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "processing resource adapter admin objects");

            List<RaAdminObject> rxAdminObjects = rxRA.getAdminObjects();
            if (!rxAdminObjects.isEmpty()) {
                // merge ra.xml and annotated admin objects
                primaryRA.getAdminObjects().addAll(mergeAdminObjects(rxAdminObjects, annoAdminObjects));
            } else {
                // there are no admin objects defined in ra.xml, so just add the annotated
                // admin objects to the primary
                primaryRA.getAdminObjects().addAll(annoAdminObjects);
            }
            // <- END process admin objects XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

        } else {
            // There is no ra.xml resource adapter, therefore just copy the annotated
            // objects, if any, into the primary

            // There should only be zero or only one class annotated with @Connector at this point
            if (trace && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "only annotations were found");
                Tr.debug(this, tc, "number of @Connector annotations: " + connectorClasses.size());
            }

            // Create empty shell --------------------------------------------------
            primaryRA = new RaResourceAdapter();
            primaryConnector.setResourceAdapter(primaryRA);

            // These will be set on the primaryRA as needed
            primaryOutbound = new RaOutboundResourceAdapter();
            primaryInbound = new RaInboundResourceAdapter();
            primaryMessageAdapter = new RaMessageAdapter();

            // -> BEGIN process resource adapter XXXXXXXXXXXXXXXXXXXXXXXXXX
            // Set resource adapter class to the annotated class
            if (annoConnector != null) {
                primaryRA.setResourceAdapterClass(annoConnector.getResourceAdapter().getResourceAdapterClass());

                if (annoConnector.getDescriptions() != null) {
                    primaryConnector.setDescription(annoConnector.getDescription());
                }

                if (annoConnector.getDisplayName() != null) {
                    primaryConnector.setDisplayName(annoConnector.getDisplayName());
                }

                if (annoConnector.getResourceAdapterVersion() != null) {
                    primaryConnector.setResourceAdapterVersion(annoConnector.getResourceAdapterVersion());
                }

                // security permissions must be copied.  It's not used but an Info
                // message will be logged that this is not supported
                if (annoConnector.getResourceAdapter().getSecurityPermissions() != null) {
                    primaryConnector.getResourceAdapter().setSecurityPermissions(annoConnector.getResourceAdapter().getSecurityPermissions());
                }

                if (annoConnector.getRequiredWorkContext() != null)
                    primaryConnector.setRequiredWorkContext(annoConnector.getRequiredWorkContext());
            }

            // copy annotated top level resource adapter config properties
            if (!annoRAConfigProperties.isEmpty())
                primaryRA.getConfigProperties().addAll(annoRAConfigProperties);
            // -> END process resource adapter XXXXXXXXXXXXXXXXXXXXXXXXXX

            // -> BEGIN process outbound resource adapter XXXXXXXXXXXXXXXXXXXXXXXXXX
            boolean outboundWasFound = false;
            // There should only be one class annotated with @Connector at this point
            if (annoConnector != null &&
                annoConnector.getResourceAdapter().getOutboundResourceAdapter() != null) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "transaction support from @Connector: " +
                                       annoConnector.getResourceAdapter().getOutboundResourceAdapter().getTransactionSupport());
                if (annoConnector.getResourceAdapter().getOutboundResourceAdapter().getTransactionSupport() != null) {
                    primaryOutbound.setTransactionSupport(annoConnector.getResourceAdapter().getOutboundResourceAdapter().getTransactionSupport());
                    outboundWasFound = true;
                }

                // Getting authenticationMechanisms since it's not added to the metatype but it is
                // needed to enforce this:
                //   JCA 1.6 spec schema definition
                //   If any of the outbound resource adapter elements (transaction-support,
                //   authentication-mechanism, reauthentication-support) is specified through
                //   this element or metadata annotations, and no connection-definition is
                //   specified as part of this element or through annotations, the
                //   application server must consider this an error and fail deployment.
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "authentication mechanism from @Connector: " +
                                       annoConnector.getResourceAdapter().getOutboundResourceAdapter().getAuthenticationMechanisms());
                if (!annoConnector.getResourceAdapter().getOutboundResourceAdapter().getAuthenticationMechanisms().isEmpty()) {
                    primaryOutbound.setAuthenticationMechanisms(annoConnector.getResourceAdapter().getOutboundResourceAdapter().getAuthenticationMechanisms());
                    outboundWasFound = true;
                }

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "reauthentication support from @Connector: " +
                                       annoConnector.getResourceAdapter().getOutboundResourceAdapter().getReauthenticationSupport());
                if (annoConnector.getResourceAdapter().getOutboundResourceAdapter().getReauthenticationSupport() != null) {
                    primaryOutbound.setReauthenticationSupport(annoConnector.getResourceAdapter().getOutboundResourceAdapter().getReauthenticationSupport());
                    outboundWasFound = true;
                }
            }

            // copy annotated connection definitions
            if (!annoDefinitions.isEmpty()) {
                primaryOutbound.getConnectionDefinitions().addAll(annoDefinitions);
                outboundWasFound = true;
            }

            if (outboundWasFound)
                primaryRA.setOutboundResourceAdapter(primaryOutbound);
            // <- END process outbound resource adapter XXXXXXXXXXXXXXXXXXXXXXXXXX

            // <- BEGIN process inbound resource adapter XXXXXXXXXXXXXXXXXXXXXXXXXX
            // copy annotated message listeners
            if (!annoMsgListeners.isEmpty()) {
                primaryMessageAdapter.getMessageListeners().addAll(annoMsgListeners);
                if (primaryRA.getInboundResourceAdapter() == null)
                    primaryRA.setInboundResourceAdapter(primaryInbound);
                primaryInbound.setMessageAdapter(primaryMessageAdapter);
                primaryRA.setInboundResourceAdapter(primaryInbound);
            }
            // <- END process inbound resource adapter XXXXXXXXXXXXXXXXXXXXXXXXXX

            // <- BEGIN process rest of resource adapter XXXXXXXXXXXXXXXXXXXXXXXXXX
            // copy annotated admin objects
            if (!annoAdminObjects.isEmpty())
                primaryRA.getAdminObjects().addAll(annoAdminObjects);
            // <- END process rest of resource adapter XXXXXXXXXXXXXXXXXXXXXXXXXX
        }

        return primaryConnector;
    }

    private String getAdapterVersion(RaConnector rxConnector) throws ResourceAdapterInternalException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // If rxConnector is null, then there was no ra.xml, which implies 1.6 or 1.7
        // (The "version" specified via @Connector is the resource adapter version, not the same
        //  as the Specs Version.)
        String jcaVersion = "1.7";
        if (rxConnector != null) {
            jcaVersion = rxConnector.getVersion().trim();
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "version from ra.xml is " + jcaVersion);
        } else {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "no ra.xml, version defaults to " + jcaVersion);
        }

        // Check for supported JCA versions
        if ((!jcaVersion.equals("2.0")) && (!jcaVersion.equals("1.7")) && (!jcaVersion.equals("1.6"))
            && (!jcaVersion.equals("1.5") && (!jcaVersion.equals("1.0"))))
            throw new ResourceAdapterInternalException(Tr.formatMessage(tc, "J2CA9934.not.a.valid.option",
                                                                        jcaVersion, "<version>, <spec-version>", "2.0, 1.7, 1.6, 1.5, 1.0"));

        return jcaVersion;
    }

    private boolean checkProcessAnnotations(RaConnector rxConnector, String jcaVersion) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // JCA 1.6+ allows for rar to not have an ra.xml, in which case annotations should be processed
        // Earlier JCA versions require an ra.xml, and annotation should not be processed
        // JCA 1.6+ metadata-complete set to true means to not process annotations
        boolean processAnno = true;
        if (rxConnector != null) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "connector jee version", jcaVersion);
            processAnno = !jcaVersion.equals("1.5") && !jcaVersion.equals("1.0");
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "processAnno based on version", processAnno);
            processAnno = processAnno ? (rxConnector.getMetadataComplete() ? false : true) : false;
            if (trace && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "metadata-complete", rxConnector.getMetadataComplete());
                Tr.debug(this, tc, "processAnno based on metadata-complete", processAnno);
            }
        }
        return processAnno;
    }

    /**
     * Combine the fields from @Connector with the matching areas in the ra.xml.
     * At a minimum a RaConnector with a RaResourceAdapter will be returned if none of these
     * fields have been specified.
     *
     * Note that only fields that are being used to create the metatype are processed.
     *
     * @param rxConnector   - must be specified and contain a RaResourceAdapter
     * @param annoConnector - null or a class that is annotated with @Connector
     * @return RaConnector - the merged RaConnector. It may contain an RaOutboundResourceAdapter
     */
    private RaConnector mergeConnectors(RaConnector rxConnector, RaConnector annoConnector) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        RaResourceAdapter rxRa = rxConnector.getResourceAdapter();

        boolean useRxOra = false;
        RaOutboundResourceAdapter rxOra = null;
        if (rxRa.getOutboundResourceAdapter() != null) {
            useRxOra = true;
            rxOra = rxRa.getOutboundResourceAdapter();
        }

        boolean useAnno = false;
        RaResourceAdapter annoRa = null;
        boolean useAnnoOra = false;
        RaOutboundResourceAdapter annoOra = null;
        if (annoConnector != null) {
            useAnno = true;
            annoRa = annoConnector.getResourceAdapter();
            if (annoRa.getOutboundResourceAdapter() != null) {
                useAnnoOra = true;
                annoOra = annoConnector.getResourceAdapter().getOutboundResourceAdapter();
            }
        }

        RaConnector connector = new RaConnector();
        RaResourceAdapter ra = new RaResourceAdapter();
        connector.setResourceAdapter(ra);
        RaOutboundResourceAdapter ora = new RaOutboundResourceAdapter();

        if (rxRa.getResourceAdapterClass() != null) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "rar.xml resource adapter class", rxRa.getResourceAdapterClass());
            connector.getResourceAdapter().setResourceAdapterClass(rxRa.getResourceAdapterClass());
        } else if (useAnno) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "annotated resource adapter class", annoRa.getResourceAdapterClass());
            // No need to check if there is a resource adapter class since this will be set to the annotated classname
            connector.getResourceAdapter().setResourceAdapterClass(annoRa.getResourceAdapterClass());
        }

        if (!rxConnector.getDescription().isEmpty()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "rar.xml description", rxConnector.getDescription());
            connector.setDescription(rxConnector.getDescription());
        } else if (useAnno && !annoConnector.getDescription().isEmpty()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "@Connector description", annoConnector.getDescription());
            connector.setDescription(annoConnector.getDescription());
        }

        if (!rxConnector.getDisplayName().isEmpty()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "rar.xml display name", rxConnector.getDisplayName());
            connector.setDisplayName(rxConnector.getDisplayName());
        } else if (useAnno && !annoConnector.getDisplayName().isEmpty()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "@Connector display name", annoConnector.getDisplayName());
            connector.setDisplayName(annoConnector.getDisplayName());
        }

        if (rxConnector.getResourceAdapterVersion() != null) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "rar.xml resource adapter version", rxConnector.getResourceAdapterVersion());
            connector.setResourceAdapterVersion(rxConnector.getResourceAdapterVersion());
        } else if (useAnno && annoConnector.getResourceAdapterVersion() != null) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "@Connector resource adapter version", annoConnector.getResourceAdapterVersion());
            connector.setResourceAdapterVersion(annoConnector.getResourceAdapterVersion());
        }

        // security permissions must be copied.  It's not used but an Info
        // message will be logged that this is not supported
        if (!rxRa.getSecurityPermissions().isEmpty()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "rar.xml resource adapter security permissions", rxRa.getSecurityPermissions());
            connector.getResourceAdapter().setSecurityPermissions(rxRa.getSecurityPermissions());
        } else if (useAnno && !annoRa.getSecurityPermissions().isEmpty()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "@Connector resource adapter security permissions", annoRa.getSecurityPermissions());
            connector.getResourceAdapter().setSecurityPermissions(annoRa.getSecurityPermissions());
        }

        // Set outbound resource adapter properties
        boolean setRaOra = false;
        if (useRxOra && rxOra.getTransactionSupport() != null) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "rar.xml transaction support", rxOra.getTransactionSupport());
            setRaOra = true;
            ora.setTransactionSupport(rxOra.getTransactionSupport());
        } else if (useAnnoOra && annoOra.getTransactionSupport() != null) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "@Connector transaction support", annoOra.getTransactionSupport());
            setRaOra = true;
            ora.setTransactionSupport(annoOra.getTransactionSupport());
        }

        // Getting authenticationMechanisms since it's not added to the metatype but it is
        // needed to enforce this:
        //   JCA 1.6 spec schema definition
        //   If any of the outbound resource adapter elements (transaction-support,
        //   authentication-mechanism, reauthentication-support) is specified through
        //   this element or metadata annotations, and no connection-definition is
        //   specified as part of this element or through annotations, the
        //   application server must consider this an error and fail deployment.
        if (useRxOra && !rxOra.getAuthenticationMechanisms().isEmpty()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "rar.xml authentication mechanisms", rxOra.getAuthenticationMechanisms());
            setRaOra = true;
            ora.setAuthenticationMechanisms(rxOra.getAuthenticationMechanisms());
        } else if (useAnnoOra && !annoOra.getAuthenticationMechanisms().isEmpty()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "@Connector authentication mechanisms", annoOra.getAuthenticationMechanisms());
            setRaOra = true;
            ora.setAuthenticationMechanisms(annoOra.getAuthenticationMechanisms());
        }

        if (useRxOra && rxOra.getReauthenticationSupport() != null) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "rar.xml reauthentication support", rxOra.getReauthenticationSupport());
            setRaOra = true;
            ora.setReauthenticationSupport(rxOra.getReauthenticationSupport());
        } else if (useAnnoOra && annoOra.getReauthenticationSupport() != null) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "@Connector reauthentication support", annoOra.getReauthenticationSupport());
            setRaOra = true;
            ora.setReauthenticationSupport(annoOra.getReauthenticationSupport());
        }

        if (setRaOra)
            ra.setOutboundResourceAdapter(ora);

        if (!rxConnector.getRequiredWorkContext().isEmpty()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "rar.xml required work context", rxConnector.getRequiredWorkContext());
            if (useAnno && !annoConnector.getRequiredWorkContext().isEmpty()) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "@Connector required work context", rxConnector.getRequiredWorkContext());
                List<String> requiredWorkContexts = new ArrayList<String>(rxConnector.getRequiredWorkContext());
                for (String requiredWorkContext : annoConnector.getRequiredWorkContext()) {
                    if (!rxConnector.getRequiredWorkContext().contains(requiredWorkContext)) {
                        requiredWorkContexts.add(requiredWorkContext);
                    }
                }
                connector.setRequiredWorkContext(requiredWorkContexts);
            } else {
                connector.setRequiredWorkContext(rxConnector.getRequiredWorkContext());
            }
        } else if (useAnno && !annoConnector.getRequiredWorkContext().isEmpty()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "@Connector required work context", annoConnector.getRequiredWorkContext());
            connector.setRequiredWorkContext(annoConnector.getRequiredWorkContext());
        }

        return connector;
    }

    /**
     * Merge admin objects
     *
     * Descriptor parsing creates one admin object class/interface pair
     * for each <admin-object> in the ra.xml/wlp-ra.xml.
     *
     * getAdminObjects creates one admin object class/anno_interface pair
     * for each annotated admin object class and the interfaces listed in the annotation.
     *
     * getAdminObjects creates one admin object class/class interfaces (zero or more) pair
     * for each annotated admin object class that has implemented interfaces listed on the class but
     * no interfaces listed in the @AdministereObject annotation.
     *
     * @param rxAdminObjects   admin objects from ra.xml
     * @param annoAdminObjects admin objects from annotations
     * @return a list of merged admin objects
     */
    private List<RaAdminObject> mergeAdminObjects(List<RaAdminObject> rxAdminObjects, List<RaAdminObject> annoAdminObjects) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // JCA 1.6 spec
        //   The adminObjectInterfaces annotation element specifies the Java type of the
        //   interface implemented by the administered object. This annotation element is
        //   optional and when this value is not provided by the resource adapter provider, the
        //   application server must use the following rules to determine the Java interfaces of
        //   the administered object:
        //
        //   - The following interfaces must be excluded while determining the Java interfaces
        //     of the administered object:
        //     - java.io.Serializable
        //     - java.io.Externalizable
        //   - If the JavaBean implements only one interface, that interface is chosen as the Java
        //     Interface implemented by the administered object
        //   - If the JavaBean class implements more than one Java interface, the resource
        //     adapter provider must explicitly state the interfaces supported by the
        //     administered object either through the adminObjectInterfaces annotation
        //     element or through the deployment descriptor. It is an error if the resource
        //     adapter provider does not use either of the two schemes to specify the Java types
        //     of the interfaces supported by the administered object.

        LinkedList<RaAdminObject> adminObjects = new LinkedList<RaAdminObject>();

        for (RaAdminObject rxAdminObject : rxAdminObjects) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "Processing rxAdminObject: " + rxAdminObject);
            RaAdminObject annoAdminObject = null;
            for (RaAdminObject aAdminObject : annoAdminObjects) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Processing aAdminObject: " + aAdminObject);
                if (rxAdminObject.getAdminObjectClass().equals(aAdminObject.getAdminObjectClass())) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "rx admin class matched anno admin class: " + rxAdminObject.getAdminObjectClass());
                    if (aAdminObject.getAdminObjectInterface() != null && !aAdminObject.getAdminObjectInterface().isEmpty()) {
                        if (rxAdminObject.getAdminObjectInterface().equals(aAdminObject.getAdminObjectInterface())) {
                            if (trace && tc.isDebugEnabled())
                                Tr.debug(this, tc, "rx admin interface matched anno admin interface, combine props: " + rxAdminObject.getAdminObjectInterface());
                            annoAdminObject = aAdminObject;
                            break;
                        }
                    } else {
                        // If ra.xml provides an interface for the admin object class that does not specify in the @AdministeredObject the interfaces
                        // for the class, then need to decide what to do with this annotation.
                        // If the class has no interfaces or multiple interfaces, then the ra.xml definition should be processed and this annotated class can
                        // be merged with the ra.xml and removed from the list of remaining annotated classes that will be processed after the
                        // ra.xml is process.
                        // If there is only one interface implemented by the class, then if it matches the interface specified on the ra.xml,
                        // then merge this with the ra.xml and remove it from the list of remaining annotated classes that will be processed.
                        // This will ensure that if there is only one interface and it doesn't match, then it will still be processed in addition to
                        // what is specified in the ra.xml
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "anno admin class does not have annotated interfaces, class interfaces are: " + aAdminObject.getImplementedAdminObjectInterfaces());
                        if (aAdminObject.getImplementedAdminObjectInterfaces().isEmpty()
                            || aAdminObject.getImplementedAdminObjectInterfaces().size() > 1
                            || (rxAdminObject.getAdminObjectInterface()).equals(aAdminObject.getImplementedAdminObjectInterfaces().get(0).getName())) {
                            annoAdminObject = aAdminObject;
                            break;
                        }
                    }
                }
            }

            if (annoAdminObject != null) {
                // remove the admin object from the list so we know at the end if
                // there are any additional annotated-only admin objects that we need
                // to copy into the primary
                annoAdminObjects.remove(annoAdminObject);

                // considering these should be the exact same in terms of attributes,
                // just process the config properties since those are the only things
                // that should be different
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "merging: " + rxAdminObject + ", " + annoAdminObject);

                RaAdminObject adminObject = new RaAdminObject();
                adminObject.setAdminObjectClass(rxAdminObject.getAdminObjectClass());
                adminObject.setAdminObjectInterface(rxAdminObject.getAdminObjectInterface());
                adminObject.getConfigProperties().addAll(mergeConfigProperties(rxAdminObject.getConfigProperties(), annoAdminObject.getConfigProperties()));
                adminObjects.add(adminObject);
            } else {
                try {
                    List<RaConfigProperty> properties = mergeConfigProperties(rxAdminObject.getConfigProperties(),
                                                                              getAnnotatedConfigProperties(raClassLoader.loadClass(rxAdminObject.getAdminObjectClass()), false));
                    rxAdminObject.getConfigProperties().clear();
                    rxAdminObject.getConfigProperties().addAll(properties);
                } catch (Exception ex) {
                    Tr.warning(tc, "J2CA9919.class.not.found", rxAdminObject.getAdminObjectClass(), adapterName);
                }
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "add rxAdminObject: " + rxAdminObject);
                adminObjects.add(rxAdminObject);
            }
        }

        if (!annoAdminObjects.isEmpty()) {
            // add the leftover annotated admin objects to the primary
            adminObjects.addAll(annoAdminObjects);
        }

        return adminObjects;
    }

    /**
     * Merge message listeners
     *
     * Create a RaMessageListener for each <messagelistener-type> in the ra.xml
     * and for each message listener type specified in messageListeners field in any classes annotated
     * with @Activation.
     *
     * A set of RaMessageListeners was created by getAnnotatedMessageListeners from the classes annotated with @Activation
     * There should be one RaMessageListener for each message listener type that was specified in the @Activation.
     * Message listeners specified in the ra.xml override those specified on classes annotated with @Activation.
     * Combine all matching listeners from the @Activation annotations with those in the ra.xml,
     * by combining the properties.
     *
     * @param rxListeners   message listeners from ra.xml
     * @param annoListeners message listeners from annotations
     * @return a list of merged message listeners
     */
    private List<RaMessageListener> mergeMessageListeners(List<RaMessageListener> rxListeners, LinkedList<RaMessageListener> annoListeners) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        LinkedList<RaMessageListener> listeners = new LinkedList<RaMessageListener>();

        @SuppressWarnings("unchecked")
        LinkedList<RaMessageListener> copyAnnoListeners = (LinkedList<RaMessageListener>) annoListeners.clone();

        // JCA 1.6 spec
        //   1. If a deployment descriptor element and one or more annotations specify
        //   information for the same unique identity (as specified by the XML schema), the
        //   information provided in the deployment descriptor overrides the value specified
        //   in the annotation.
        for (RaMessageListener rxListener : rxListeners) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "rxlistener type is: " + rxListener.getMessageListenerType());
            RaMessageListener mergedListener = null;
            RaActivationSpec activationSpec = null;
            for (RaMessageListener aListener : annoListeners) {
                if (trace && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "anno listener type is: " + aListener.getMessageListenerType());
                    Tr.debug(this, tc, "anno listener class is: " + aListener.getActivationSpec().getActivationSpecClass());
                }
                if (rxListener.getMessageListenerType().equals(aListener.getMessageListenerType())) {
                    if (trace && tc.isDebugEnabled()) {
                        Tr.debug(tc, "ra.xml listener class: " + rxListener.getActivationSpec().getActivationSpecClass());
                    }

                    // remove the message listener from the list so we know at the end if
                    // there are any additional annotated-only listeners that we need to
                    // copy into the primary
                    copyAnnoListeners.remove(aListener);

                    // ra.xml overrides @Activation, but still process the config properties
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "ra.xml already contains a message listener with type '" + rxListener.getMessageListenerType()
                                           + "'.  Combining the properties from class with @Activation annotation '" + aListener.getActivationSpec().getActivationSpecClass()
                                           + "'");

                    RaActivationSpec rxActivationSpec = rxListener.getActivationSpec();
                    RaActivationSpec annoActivationSpec = aListener.getActivationSpec();
                    if (rxActivationSpec == null)
                        throw new IllegalArgumentException(Tr.formatMessage(tc, "J2CA9924.listener.actspec.missing",
                                                                            rxListener.getMessageListenerType(),
                                                                            aListener.getActivationSpec().getActivationSpecClass()));
                    if (mergedListener == null) {
                        mergedListener = new RaMessageListener();
                        mergedListener.setMessageListenerType(rxListener.getMessageListenerType());
                        activationSpec = new RaActivationSpec();
                        activationSpec.setActivationSpecClass(rxActivationSpec.getActivationSpecClass());
                    }
                    activationSpec.getConfigProperties().addAll(mergeConfigProperties(rxActivationSpec.getConfigProperties(), annoActivationSpec.getConfigProperties()));
                }
            }
            if (mergedListener != null) {
                mergedListener.setActivationSpec(activationSpec);
                listeners.add(mergedListener);
            } else {
                RaActivationSpec rxActivationSpec = rxListener.getActivationSpec();
                try {
                    List<RaConfigProperty> properties = mergeConfigProperties(rxActivationSpec.getConfigProperties(),
                                                                              getAnnotatedConfigProperties(raClassLoader.loadClass(rxActivationSpec.getActivationSpecClass()),
                                                                                                           false));
                    rxActivationSpec.getConfigProperties().clear();
                    rxActivationSpec.getConfigProperties().addAll(properties);
                } catch (Exception e) {
                    Tr.warning(tc, "J2CA9919.class.not.found", rxActivationSpec.getActivationSpecClass(), adapterName);
                }
                listeners.add(rxListener);
            }
        }

        if (!copyAnnoListeners.isEmpty()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "remaining anno listeners after merge", copyAnnoListeners);
            // there are annotated message listeners (activations) that do not already
            // exist in the ra.xml, add them to the primary
            for (RaMessageListener msgListener : copyAnnoListeners)
                listeners.add(msgListener);
        }

        return listeners;
    }

    /**
     * Merge connection definitions
     *
     * @param rxDefinitions   connection definitions from ra.xml
     * @param annoDefinitions connection definitions from annotations
     * @return a list of the merged connection definitions
     */
    private List<RaConnectionDefinition> mergeConnectionDefinitions(List<RaConnectionDefinition> rxDefinitions, List<RaConnectionDefinition> annoDefinitions) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        LinkedList<RaConnectionDefinition> definitions = new LinkedList<RaConnectionDefinition>();

        for (RaConnectionDefinition rxDef : rxDefinitions) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc,
                         "Merging ra.xml connection definition with connection factory interface " +
                                   rxDef.getConnectionFactoryInterface());
            RaConnectionDefinition annoDef = null;
            for (RaConnectionDefinition def : annoDefinitions) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             "Compare with connection definition annotation " +
                                       def.getConnectionFactoryInterface());
                if (rxDef.getConnectionFactoryInterface().equals(def.getConnectionFactoryInterface())) {
                    annoDef = def;
                    break;
                }
            }

            if (annoDef != null) {
                // remove the connection definition from the list so we know at the end if
                // there are any additional annotated-only definitions that we need to
                // copy into the primary
                annoDefinitions.remove(annoDef);

                // ra.xml overrides @ConnectionDefinition, but still process the config properties
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             "ra.xml already contains a connection definition with the connection factory interface of '" + rxDef.getConnectionFactoryInterface()
                                       + "' thus ignoring the @ConnectionDefinition annotation on class '" + annoDef.getManagedConnectionFactoryClass());

                RaConnectionDefinition definition = new RaConnectionDefinition();
                definition.setConnectionFactoryImplClass(rxDef.getConnectionFactoryImplClass());
                definition.setConnectionFactoryInterface(rxDef.getConnectionFactoryInterface());
                definition.setConnectionImplClass(rxDef.getConnectionImplClass());
                definition.setConnectionInterface(rxDef.getConnectionInterface());
                definition.setManagedConnectionFactoryClass(rxDef.getManagedConnectionFactoryClass());
                definition.getConfigProperties().addAll(mergeConfigProperties(rxDef.getConfigProperties(), annoDef.getConfigProperties()));
                definitions.add(definition);
            } else {
                // just add the ra.xml connection definition
                try {
                    List<RaConfigProperty> properties = mergeConfigProperties(rxDef.getConfigProperties(),
                                                                              getAnnotatedConfigProperties(raClassLoader.loadClass(rxDef.getManagedConnectionFactoryClass()),
                                                                                                           false));
                    rxDef.getConfigProperties().clear();
                    rxDef.getConfigProperties().addAll(properties);
                } catch (Exception e) {
                    Tr.warning(tc, "J2CA9919.class.not.found", rxDef.getManagedConnectionFactoryClass(), adapterName);
                }
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "Add ra.xml connection definition to list of connection definitions");
                definitions.add(rxDef);
            }
        }

        if (!annoDefinitions.isEmpty()) {
            // there are annotated connection definitions that do not exist already in
            // the ra.xml, thus add them to the primary
            for (RaConnectionDefinition r : annoDefinitions) {
                if (trace && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Add annotated connection definition to list of connection definitions");
                    Tr.debug(tc, "anno ",
                             r.getConnectionFactoryInterface(),
                             r.getConnectionFactoryImplClass(),
                             r.getConnectionInterface(),
                             r.getConnectionImplClass());
                }
            }
            definitions.addAll(annoDefinitions);
        }

        return definitions;
    }

    /**
     * Merge config properties
     *
     * @param rxConfigProperties   config properties from ra.xml
     * @param annoConfigProperties config properties from annotations
     * @return a list of the merged config properties
     */
    @SuppressWarnings("unchecked")
    private List<RaConfigProperty> mergeConfigProperties(List<RaConfigProperty> rxConfigProperties, List<RaConfigProperty> annotatedConfigProperties) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        LinkedList<RaConfigProperty> configProperties = new LinkedList<RaConfigProperty>();
        List<RaConfigProperty> annoConfigProperties = null;
        if (annotatedConfigProperties != null)
            annoConfigProperties = (List<RaConfigProperty>) ((LinkedList<RaConfigProperty>) annotatedConfigProperties).clone();
        else
            annoConfigProperties = new LinkedList<RaConfigProperty>();

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc,
                     "rxConfigProperties size: " + rxConfigProperties.size());

        for (RaConfigProperty rxConfigProp : rxConfigProperties) {
            RaConfigProperty annoConfigProp = null;

            if (annoConfigProperties.isEmpty()) {
                return (List<RaConfigProperty>) ((LinkedList<RaConfigProperty>) rxConfigProperties).clone();
            } else {
                for (RaConfigProperty configProp : annoConfigProperties)
                    if (isEqual(rxConfigProp.getName(), configProp.getName())) {
                        annoConfigProp = configProp;
                        break;
                    }

                if (annoConfigProp != null) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "merging " + rxConfigProp + ", " + annoConfigProp);

                    // remove the config property from the list so we know at the end if there
                    // are any additional annotated-only config properties that we need to copy
                    // into the primary
                    annoConfigProperties.remove(annoConfigProp);

                    // merge the two config properties
                    if (rxConfigProp.getConfidential() == null)
                        rxConfigProp.setConfidential(annoConfigProp.getConfidential());

                    if (rxConfigProp.getDescription() == null || rxConfigProp.getDescription().isEmpty())
                        rxConfigProp.setDescription(annoConfigProp.getDescription());

                    if (rxConfigProp.getIgnore() == null)
                        rxConfigProp.setIgnore(annoConfigProp.getIgnore());

                    if (rxConfigProp.getSupportsDynamicUpdates() == null)
                        rxConfigProp.setSupportsDynamicUpdates(annoConfigProp.getSupportsDynamicUpdates());

                    if (rxConfigProp.getType() == null || rxConfigProp.getType().equals(""))
                        rxConfigProp.setType(annoConfigProp.getType());

                    if (rxConfigProp.getDefault() == null || rxConfigProp.getDefault().equals(""))
                        rxConfigProp.setDefault(annoConfigProp.getDefault());

                    configProperties.add(rxConfigProp);
                } else {
                    configProperties.add(rxConfigProp);
                }
            }
        }

        if (!annoConfigProperties.isEmpty()) {
            // there are annotated config properties that do not exist already in the ra.xml,
            // thus add them to the primary
            for (RaConfigProperty configProp : annoConfigProperties)
                configProperties.add(configProp);
        }

        return configProperties;
    }

    private RaConnector getAnnotatedConnector(Class<?> clazz) throws ResourceAdapterInternalException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (trace && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "class to look at for ResourceAdapter interface", clazz);
            Tr.debug(this, tc, "class name is: " + clazz.getName());
        }

        RaConnector connector = new RaConnector();
        RaResourceAdapter ra = new RaResourceAdapter();
        ra.setResourceAdapterClass(clazz.getName());
        connector.setResourceAdapter(ra);
        RaOutboundResourceAdapter ora = new RaOutboundResourceAdapter();
        ra.setOutboundResourceAdapter(ora);
        RaInboundResourceAdapter ira = new RaInboundResourceAdapter();
        ra.setInboundResourceAdapter(ira);

        if (trace && tc.isDebugEnabled())
            Tr.debug(tc, "annotated resource adapter class",
                     connector.getResourceAdapter().getResourceAdapterClass());

        // Get information from the annotation
        Connector anno = clazz.getAnnotation(Connector.class);

        LinkedList<RaDescription> description = new LinkedList<RaDescription>();
        for (String d : anno.description()) {
            RaDescription raDesc = new RaDescription();
            raDesc.setValue(d);
            description.add(raDesc);
        }

        LinkedList<RaDisplayName> displayName = new LinkedList<RaDisplayName>();
        for (String d : anno.displayName()) {
            RaDisplayName raDisplayName = new RaDisplayName();
            raDisplayName.setValue(d);
            displayName.add(raDisplayName);
        }

        String version = anno.version();

        TransactionSupport.TransactionSupportLevel transactionSupport = null;
        if (anno.transactionSupport() != null)
            transactionSupport = anno.transactionSupport();

        AuthenticationMechanism[] authMechanisms = anno.authMechanisms();

        boolean reauthenticationSupport = anno.reauthenticationSupport();

        SecurityPermission[] securityPermissions = anno.securityPermissions();

        Class<? extends WorkContext>[] requiredWorkContexts = anno.requiredWorkContexts();

        // Put annotation information into an RaConnector
        if (!description.isEmpty())
            connector.setDescription(description);

        if (!displayName.isEmpty())
            connector.setDisplayName(displayName);

        if (!version.isEmpty())
            connector.setResourceAdapterVersion(version);

        // Do not set transaction support level if the default was specified.
        // This is necessary to make the JCA spec check for no connection definitions and
        // transaction support was specified work.
        if (transactionSupport != TransactionSupport.TransactionSupportLevel.NoTransaction)
            ora.setTransactionSupport(transactionSupport.name());

        // Getting authenticationMechanisms since it's not added to the metatype but it is
        // needed to enforce this:
        //   JCA 1.6 spec schema definition
        //   If any of the outbound resource adapter elements (transaction-support,
        //   authentication-mechanism, reauthentication-support) is specified through
        //   this element or metadata annotations, and no connection-definition is
        //   specified as part of this element or through annotations, the
        //   application server must consider this an error and fail deployment.
        if (authMechanisms.length > 0) {
            List<RaAuthenticationMechanism> raAuthenticationMechanism = new LinkedList<RaAuthenticationMechanism>();
            for (AuthenticationMechanism am : authMechanisms) {
                RaAuthenticationMechanism authenticationMechanism = new RaAuthenticationMechanism();
                // Just setting the auth mechanism type, since the it's only being checked for existence
                authenticationMechanism.setAuthenticationMechanismType(am.authMechanism());
                raAuthenticationMechanism.add(authenticationMechanism);
            }
            connector.getResourceAdapter().getOutboundResourceAdapter().setAuthenticationMechanisms(raAuthenticationMechanism);
        }

        // Do not set reauthentication support if the default was specified.
        // This is necessary to make the JCA spec check for no connection definitions and
        // reauthentication support was specified work.
        if (reauthenticationSupport != false)
            connector.getResourceAdapter().getOutboundResourceAdapter().setReauthenticationSupport("true");

        if (securityPermissions.length > 0) {
            List<RaSecurityPermission> raSecurityPermissions = new LinkedList<RaSecurityPermission>();
            for (SecurityPermission sp : securityPermissions) {
                RaSecurityPermission securityPermission = new RaSecurityPermission();
                LinkedList<RaDescription> spDescription = new LinkedList<RaDescription>();
                for (String d : sp.description()) {
                    RaDescription raDesc = new RaDescription();
                    raDesc.setValue(d);
                    spDescription.add(raDesc);
                }
                securityPermission.setDescription(spDescription);
                securityPermission.setSecurityPermissionSpec(sp.permissionSpec());
                raSecurityPermissions.add(securityPermission);
            }
            connector.getResourceAdapter().setSecurityPermissions(raSecurityPermissions);
        }

        if (requiredWorkContexts.length > 0) {
            List<String> requiredWorkContext = new LinkedList<String>();
            for (Class<?> wc : requiredWorkContexts) {
                requiredWorkContext.add(wc.getName());
            }
            connector.setRequiredWorkContext(requiredWorkContext);
        }

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "Processed @Connector " + clazz.getName());

        return connector;
    }

    private List<RaAdminObject> getAnnotatedAdminObjects(Class<?> clazz) throws ResourceAdapterInternalException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        final String className = clazz.getName();
        LinkedList<RaAdminObject> adminObjects = new LinkedList<RaAdminObject>();
        final List<RaConfigProperty> configProperties = getAnnotatedConfigProperties(clazz, false);

        AdministeredObject anno = clazz.getAnnotation(AdministeredObject.class);
        Class<?>[] aoInterfaces = anno.adminObjectInterfaces();
        if (aoInterfaces.length != 0) {
            // use the interfaces defined by the annotation
            for (Class<?> aoInterface : aoInterfaces) {
                RaAdminObject adminObject = new RaAdminObject();
                adminObject.setAdminObjectClass(className);
                adminObject.setAdminObjectInterface(aoInterface.getName());
                adminObject.getConfigProperties().addAll(configProperties);
                adminObjects.add(adminObject);

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Processed admin object with class " + className + " and interface " + aoInterface.getName());
            }
        } else {
            // we need to get the interfaces from the class itself, not the annotation
            List<Class<?>> interfaces = new LinkedList<Class<?>>();
            findAllInterfacesOnAdminClass(clazz, interfaces);

            RaAdminObject adminObject = new RaAdminObject();
            adminObject.setAdminObjectClass(className);
            adminObject.setAnnAdminObjectInterfaces(interfaces);
            adminObject.getConfigProperties().addAll(configProperties);
            adminObjects.add(adminObject);

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "Processed admin object with class " + className + " and interfaces " + interfaces);
        }

        return adminObjects;
    }

    private void findAllInterfacesOnAdminClass(Class<?> clazz, List<Class<?>> interfaceList) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        Class<?>[] classInterfaces = clazz.getInterfaces();

        for (Class<?> classInterface : classInterfaces) {
            if (java.io.Serializable.class == classInterface)
                continue;
            else if (java.io.Externalizable.class == classInterface)
                continue;
            else if (javax.resource.spi.ResourceAdapterAssociation.class == classInterface)
                continue;
            else
                interfaceList.add(classInterface);
        }
        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "Interface list: " + interfaceList);

        if (clazz.getSuperclass() != null) {
            findAllInterfacesOnAdminClass(clazz.getSuperclass(), interfaceList);
        }

    }

    private List<RaMessageListener> getAnnotatedMessageListeners(Class<?> clazz) throws ResourceAdapterInternalException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        List<RaMessageListener> messageListeners = new LinkedList<RaMessageListener>();
        final String className = clazz.getName();
        final List<RaConfigProperty> configProperties = getAnnotatedConfigProperties(clazz, false);

        Activation anno = clazz.getAnnotation(Activation.class);
        Class<?>[] msgListenerClasses = anno.messageListeners();

        for (Class<?> msgListenerClass : msgListenerClasses) {
            RaMessageListener messageListener = new RaMessageListener();
            messageListener.setMessageListenerType(msgListenerClass.getName());

            RaActivationSpec activationSpec = new RaActivationSpec();
            activationSpec.setActivationSpecClass(className);
            activationSpec.getConfigProperties().addAll(configProperties);

            messageListener.setActivationSpec(activationSpec);
            messageListeners.add(messageListener);

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "Processed message listener " + msgListenerClass.getName() + " with activation spec " + className);
        }

        return messageListeners;
    }

    private RaConnectionDefinition getAnnotatedConnectionDefinition(Class<?> clazz) throws ResourceAdapterInternalException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "class to look at for ManagedConnectionFactory interface", clazz);

        RaConnectionDefinition definition = new RaConnectionDefinition();

        ConnectionDefinition anno = clazz.getAnnotation(ConnectionDefinition.class);
        String connectionFactoryImpl = anno.connectionFactoryImpl().getName();
        String connectionFactoryInterface = anno.connectionFactory().getName();
        String connectionInterface = anno.connection().getName();
        String connectionImpl = anno.connectionImpl().getName();

        definition.setConnectionFactoryImplClass(connectionFactoryImpl);
        definition.setConnectionFactoryInterface(connectionFactoryInterface);
        definition.setConnectionImplClass(connectionImpl);
        definition.setConnectionInterface(connectionInterface);
        definition.setManagedConnectionFactoryClass(clazz.getName());
        definition.getConfigProperties().addAll(getAnnotatedConfigProperties(clazz, false));

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "Processed @ConnectionDefinition " + clazz.getName());

        return definition;
    }

    private List<RaConnectionDefinition> getAnnotatedConnectionDefinitions(Class<?> clazz) throws ResourceAdapterInternalException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        final List<RaConnectionDefinition> definitions = new LinkedList<RaConnectionDefinition>();

        ConnectionDefinitions anno = clazz.getAnnotation(ConnectionDefinitions.class);
        ConnectionDefinition[] annoDefs = anno.value();

        if (annoDefs != null && annoDefs.length > 0) {
            for (ConnectionDefinition annoDef : annoDefs) {
                RaConnectionDefinition definition = new RaConnectionDefinition();
                String connectionFactoryImpl = annoDef.connectionFactoryImpl().getName();
                String connectionFactoryInterface = annoDef.connectionFactory().getName();
                String connectionInterface = annoDef.connection().getName();
                String connectionImpl = annoDef.connectionImpl().getName();

                definition.setConnectionFactoryImplClass(connectionFactoryImpl);
                definition.setConnectionFactoryInterface(connectionFactoryInterface);
                definition.setConnectionImplClass(connectionImpl);
                definition.setConnectionInterface(connectionInterface);
                definition.setManagedConnectionFactoryClass(clazz.getName());
                definition.getConfigProperties().addAll(getAnnotatedConfigProperties(clazz, false));
                definitions.add(definition);

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Processed @ConnectionDefinitions " + clazz.getName() + " : " + definition);
            }
        }

        return definitions;
    }

    /**
     * @param clazz           the class to inspect for {@code @ConfigProperty} annotations
     * @param processDefaults true if java bean property default values should be merged for resource adapters
     */
    @SuppressWarnings("unchecked")
    @FFDCIgnore(NoSuchMethodException.class)
    private <T> List<RaConfigProperty> getAnnotatedConfigProperties(Class<?> clazz,
                                                                    boolean processJavaBeanDefaults) throws ResourceAdapterInternalException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        LinkedList<RaConfigProperty> configProperties = new LinkedList<RaConfigProperty>();

        T instance = null; // initialize only is absolutely required

        try {
            // search the fields
            Field[] fields = clazz.getDeclaredFields();
            for (final Field field : fields) {
                ConfigProperty anno = field.getAnnotation(ConfigProperty.class);
                if (anno != null) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "Processing " + anno + " on " + field);

                    // @ConfigProperty annotation found, now process it
                    RaConfigProperty configProperty = new RaConfigProperty();

                    Class<?> type = field.getType();
                    if (anno.type() != Object.class && anno.type() != type) {
                        throw new IllegalStateException(Tr.formatMessage(tc, "J2CA9940.cfgprop.type.mismatch.field",
                                                                         clazz.getSimpleName(), field.getName(), anno.type().getName(), type.getName()));
                    }
                    // Full profile doesn't allow primitive types for
                    // @ConfigProperty, so don't use MetatypeAd.getBoxedType.

                    String fName = field.getName();
                    String upperCaseFieldName = Character.toUpperCase(fName.charAt(0)) + fName.substring(1);
                    String gName = "get" + upperCaseFieldName;
                    String sName = "set" + upperCaseFieldName;

                    try {
                        // check if there is a getter for the config property
                        checkGetterMethod(clazz, fName, gName, type);
                    } catch (NoSuchMethodException e) {
                        // it's possible we need to try the isProperty() form of the getter
                        boolean foundGetter = false;
                        if (type == Boolean.class) {
                            try {
                                gName = "is" + upperCaseFieldName;
                                checkGetterMethod(clazz, fName, gName, type);
                                foundGetter = true;
                            } catch (NoSuchMethodException e2) {
                            }
                        }

                        if (!foundGetter) {
                            throw new IllegalArgumentException(Tr.formatMessage(tc, "J2CA9938.cfgprop.no.getter",
                                                                                clazz.getSimpleName(), fName, type.getName()));
                        }
                    }

                    try {
                        // check if there is a setter for the config property with a matching type
                        clazz.getMethod(sName, type);
                    } catch (NoSuchMethodException e) {
                        throw new IllegalArgumentException(Tr.formatMessage(tc, "J2CA9939.cfgprop.no.setter",
                                                                            clazz.getSimpleName(), fName, type.getName()));
                    }

                    String[] descList = anno.description();
                    List<RaDescription> description = new LinkedList<RaDescription>();
                    for (String d : descList) {
                        RaDescription ra = new RaDescription();
                        ra.setValue(d);
                        description.add(ra);
                    }

                    //String description = null;
                    //if (anno.description() != null) {
                    //    String[] desc = anno.description();
                    //    StringBuilder sb = new StringBuilder();
                    //    for (int i = 0; i < desc.length; ++i) {
                    //        sb.append(desc[i]);
                    //        if (i + 1 != desc.length)
                    //            sb.append(' ');
                    //    }
                    //    description = sb.toString();
                    //}

                    String defaultValue = !anno.defaultValue().isEmpty() ? anno.defaultValue() : null;
                    if (processJavaBeanDefaults && defaultValue == null) {
                        if (instance == null)
                            instance = (T) clazz.getConstructor().newInstance();
                        AccessController.doPrivileged(new PrivilegedAction<Void>() {
                            @Override
                            public Void run() {
                                field.setAccessible(true);
                                return null;
                            }
                        });
                        Object reflectDefaultVal = field.get(instance);
                        defaultValue = reflectDefaultVal == null ? null : reflectDefaultVal.toString();
                    }

                    Boolean confidential = anno.confidential();
                    Boolean ignore = anno.ignore();
                    Boolean supportsDynamicUpdates = anno.supportsDynamicUpdates();

                    configProperty.setName(field.getName());
                    configProperty.setConfidential(confidential);
                    configProperty.setDefault(defaultValue);
                    configProperty.setDescription(description);
                    configProperty.setIgnore(ignore);
                    configProperty.setSupportsDynamicUpdates(supportsDynamicUpdates);
                    configProperty.setType(type.getName());

                    configProperties.add(configProperty);

                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Processed @ConfigProperty " + configProperty);
                }
            }

            // search the methods
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                ConfigProperty anno = method.getAnnotation(ConfigProperty.class);
                if (anno != null) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "Processing " + anno + " on " + method);

                    // @ConfigProperty annotation found, now process it
                    // make sure this is a setter
                    String mName = method.getName();
                    if (!mName.startsWith("set") || mName.length() < 4)
                        throw new IllegalArgumentException(Tr.formatMessage(tc, "J2CA9927.cfgprop.invalid.method", method.getName()));

                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length != 1)
                        throw new IllegalArgumentException(Tr.formatMessage(tc, "J2CA9942.cfgprop.invalid.parameters",
                                                                            method.getName(), Arrays.toString(paramTypes)));

                    RaConfigProperty configProperty = new RaConfigProperty();

                    // Use the method name to extract the field name it relates to
                    String upperName = mName.substring("set".length());
                    String name = Character.toLowerCase(upperName.charAt(0)) + upperName.substring(1);
                    String getterName = "get" + upperName;

                    Class<?> type = paramTypes[0];
                    if (anno.type() != Object.class && anno.type() != type) {
                        throw new IllegalStateException(Tr.formatMessage(tc, "J2CA9941.cfgprop.type.mismatch.setter",
                                                                         clazz.getSimpleName(), name, anno.type().getName(), "", type.getName()));
                    }
                    // Full profile doesn't allow primitive types for
                    // @ConfigProperty, so don't use MetatypeAd.getBoxedType.

                    Method getterMethodTemp = null;
                    try {
                        // check if there is a getter for the config property
                        getterMethodTemp = checkGetterMethod(clazz, name, getterName, type);
                    } catch (NoSuchMethodException e) {
                        // it's possible we need to try the isProperty() form of the getter
                        boolean foundGetter = false;
                        if (type == Boolean.class) {
                            getterName = "is" + upperName;
                            try {
                                getterMethodTemp = checkGetterMethod(clazz, name, getterName, type);
                                foundGetter = true;
                            } catch (NoSuchMethodException e1) {
                            }
                        }

                        if (!foundGetter) {
                            throw new IllegalArgumentException(Tr.formatMessage(tc, "J2CA9938.cfgprop.no.getter",
                                                                                clazz.getSimpleName(), name, type.getName()));
                        }
                    }

                    final Method getterMethod = getterMethodTemp;

                    String[] descList = anno.description();
                    List<RaDescription> description = new LinkedList<RaDescription>();
                    for (String d : descList) {
                        RaDescription ra = new RaDescription();
                        ra.setValue(d);
                        description.add(ra);
                    }

                    //String description = null;
                    //if (anno.description() != null) {
                    //    String[] desc = anno.description();
                    //    StringBuilder sb = new StringBuilder();
                    //    for (int i = 0; i < desc.length; ++i) {
                    //        sb.append(desc[i]);
                    //        if (i + 1 != desc.length)
                    //            sb.append(' ');
                    //    }
                    //    description = sb.toString();
                    //}

                    String defaultValue = !anno.defaultValue().isEmpty() ? anno.defaultValue() : null;
                    if (processJavaBeanDefaults && defaultValue == null) {
                        if (instance == null)
                            instance = (T) clazz.getConstructor().newInstance();
                        AccessController.doPrivileged(new PrivilegedAction<Void>() {
                            @Override
                            public Void run() {
                                getterMethod.setAccessible(true);
                                return null;
                            }
                        });
                        Object reflectDefaultVal = getterMethod.invoke(instance);
                        defaultValue = reflectDefaultVal == null ? null : reflectDefaultVal.toString();
                    }

                    Boolean ignore = anno.ignore();
                    Boolean supportsDynamicUpdates = anno.supportsDynamicUpdates();
                    Boolean confidential = anno.confidential();

                    configProperty.setName(name);
                    configProperty.setConfidential(confidential);
                    configProperty.setDefault(defaultValue);
                    configProperty.setDescription(description);
                    configProperty.setIgnore(ignore);
                    configProperty.setSupportsDynamicUpdates(supportsDynamicUpdates);
                    configProperty.setType(type.getName());

                    configProperties.add(configProperty);

                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Processed @ConfigProperty " + configProperty);
                }
            }

            // need to process all @ConfigProperty annotations in superclasses
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null)
                configProperties.addAll(getAnnotatedConfigProperties(superclass, processJavaBeanDefaults));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof ResourceAdapterInternalException)
                throw (ResourceAdapterInternalException) e;
            else
                throw new ResourceAdapterInternalException(e);
        }

        return configProperties;
    }

    private Method checkGetterMethod(Class<?> clazz, String propName, String getterName, Class<?> type) throws NoSuchMethodException {
        Method getterMethod = clazz.getMethod(getterName);
        Class<?> returnType = getterMethod.getReturnType();
        if (returnType != type) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "J2CA9937.cfgprop.invalid.return",
                                                                clazz.getSimpleName(), propName, returnType.getName(), type.getName()));
        }

        return getterMethod;
    }

    public boolean isEqual(String str1, String str2) {
        str1 = str1.substring(0, 1).toUpperCase() + str1.substring(1);
        str2 = str2.substring(0, 1).toUpperCase() + str2.substring(1);
        return str1.equals(str2);
    }

    public boolean isAnnotatedConnector() {
        return connectorClasses.size() != 0 || activationClasses.size() != 0 || connDefClasses.size() != 0 ||
               connDefsClasses.size() != 0 || adminObjectClasses.size() != 0;
    }

}
