/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.metagen;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Wrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.resource.spi.ActivationSpec;
import javax.resource.spi.Connector;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.HintsContext;
import javax.resource.spi.work.TransactionContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.container.service.annotations.ModuleAnnotations;
import com.ibm.ws.jca.utils.Utils;
import com.ibm.ws.jca.utils.Utils.ConstructType;
import com.ibm.ws.jca.utils.exception.ResourceAdapterInstallException;
import com.ibm.ws.jca.utils.metagen.internal.InternalConstants;
import com.ibm.ws.jca.utils.metagen.internal.MetaGenConfig;
import com.ibm.ws.jca.utils.metagen.internal.MetaGenInstance;
import com.ibm.ws.jca.utils.metagen.internal.XmlFileSet;
import com.ibm.ws.jca.utils.xml.metatype.Metatype;
import com.ibm.ws.jca.utils.xml.metatype.MetatypeAd;
import com.ibm.ws.jca.utils.xml.metatype.MetatypeAdOption;
import com.ibm.ws.jca.utils.xml.metatype.MetatypeDesignate;
import com.ibm.ws.jca.utils.xml.metatype.MetatypeObject;
import com.ibm.ws.jca.utils.xml.metatype.MetatypeOcd;
import com.ibm.ws.jca.utils.xml.ra.RaActivationSpec;
import com.ibm.ws.jca.utils.xml.ra.RaAdminObject;
import com.ibm.ws.jca.utils.xml.ra.RaConfigProperty;
import com.ibm.ws.jca.utils.xml.ra.RaConnectionDefinition;
import com.ibm.ws.jca.utils.xml.ra.RaConnector;
import com.ibm.ws.jca.utils.xml.ra.RaDescription;
import com.ibm.ws.jca.utils.xml.ra.RaDisplayName;
import com.ibm.ws.jca.utils.xml.ra.RaInboundResourceAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaMessageAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaMessageListener;
import com.ibm.ws.jca.utils.xml.ra.RaOutboundResourceAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaRequiredConfigProperty;
import com.ibm.ws.jca.utils.xml.ra.RaResourceAdapter;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpConfigOption;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpIbmuiGroups;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaConfigProperty;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaConnector;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaMessageListener;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * Generates metatype for a resource adapter based on ra.xml files.
 */
public class MetatypeGenerator {
    private static final TraceComponent tc = Tr.register(MetatypeGenerator.class);

    /**
     * Super interface names that should be skipped for a resource factory's creates.objectClass service property
     */
    private static final Set<String> INTERFACE_NAMES_TO_SKIP = new HashSet<String>(Arrays.asList(
                                                                                                 BeanInfo.class.getName(),
                                                                                                 Cloneable.class.getName(),
                                                                                                 Comparable.class.getName(),
                                                                                                 Externalizable.class.getName(),
                                                                                                 Iterable.class.getName(),
                                                                                                 javax.naming.Referenceable.class.getName(),
                                                                                                 javax.resource.Referenceable.class.getName(),
                                                                                                 ResourceAdapterAssociation.class.getName(),
                                                                                                 Serializable.class.getName(),
                                                                                                 Wrapper.class.getName()));
    private final List<String> buildTimeWarnings = new LinkedList<String>();
    private Metatype primaryMetatype;
    private MetaGenConfig config;
    private String generalAdapterName = null;
    //private static JAXBContext metatypeContext = null;
    private Map<String, String> suffixOverridesByIntf; // Map of fully qualified interface name to suffix
    private Map<String, String> suffixOverridesByImpl; // Map of fully qualified implementation class name to suffix
    private Map<String, String> suffixOverridesByBoth; // Map of <fully.qualified.interface>-<fully.qualified.impl> to suffix

    private final MetaTypeFactory metaTypeFactoryService;

    ClassLoader raClassLoader = null;

    // TODOCJN need to look at all of this
    // initialize only once otherwise LinkageErrors occur.
    //static {
    //    try {
    //        metatypeContext = JAXBContext.newInstance(Metatype.class);
    //    } catch (JAXBException e) {
    //        throw new ExceptionInInitializerError(e);
    //    }
    //}

    private MetatypeGenerator(MetaTypeFactory mtpService) {
        this.metaTypeFactoryService = mtpService;
    }

    /**
     * Add classes for creates.objectClass to the metatype.
     *
     * @param MetatypeOcd   ocd metatype object class definition.
     * @param interfaceName class name with which to start
     * @throws ClassNotFoundException if the class cannot be loaded
     */
    private void addInterfaces(MetatypeOcd ocd, String interfaceName) throws ClassNotFoundException {
        Set<String> interfaceNames = new HashSet<String>();
        interfaceNames.add(interfaceName);
        StringBuilder interfacesString = new StringBuilder(interfaceName);
        LinkedList<Class<?>> interfacesToProcess = new LinkedList<Class<?>>();
        if (raClassLoader != null)
            try {
                for (Class<?> cl = raClassLoader.loadClass(interfaceName); cl != null; cl = interfacesToProcess.poll())
                    for (Class<?> ifc : cl.getInterfaces())
                        if (!INTERFACE_NAMES_TO_SKIP.contains(interfaceName = ifc.getName()) && interfaceNames.add(interfaceName)) {
                            interfacesToProcess.add(ifc);
                            interfacesString.append(',').append(interfaceName);
                        }
            } catch (ClassNotFoundException x) {
                Tr.warning(tc, "J2CA9919.class.not.found", interfaceName, generalAdapterName);
                if (!config.isRuntime())
                    buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9919.class.not.found", interfaceName, generalAdapterName));
            }

        MetatypeAd ad = new MetatypeAd(metaTypeFactoryService);
        ad.setId("creates.objectClass");
        ad.setType("String");
        ad.setDefault(interfacesString.toString());
        ad.setFinal(true);
        ad.setCardinality(interfaceNames.size());
        ad.setName("internal");
        ad.setDescription("internal use only");
        ocd.addMetatypeAd(ad);
    }

    /**
     * Generate metatype from ra.xml and wlp-ra.xml files as well as from adapter
     * annotations. See MetaGenConstants for key/values for the input HashMap of
     * configuration properties.
     *
     * @param configProps properties used to execute the generator
     * @return the generated metatype or null if the metatype was never generated
     * @throws Exception
     */
    public static Metatype generateMetatype(Map<String, Object> configProps, MetaTypeFactory mtpService) throws Exception {
        return new MetatypeGenerator(mtpService).generateMetatypeInternal(configProps);
    }

    /**
     * Generate metatype from ra.xml and wlp-ra.xml files as well as from adapter
     * annotations. See MetaGenConstants for key/values for the input HashMap of
     * configuration properties.
     *
     * @param configProps properties used to execute the generator
     * @return the generated metatype, or null if the metatype was never generate
     * @throws Exception
     */
    @Trivial
    private Metatype generateMetatypeInternal(Map<String, Object> configProps) throws Exception {
        long start = System.nanoTime();

        try {
            setup(configProps);

            buildMetatype();
            postBuild(configProps);

            // Avoid FindBugs error and log to System.out when this runs at build time
            for (String warning : buildTimeWarnings)
                PrintStream.class.getMethod("println", String.class).invoke(System.out, warning);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                long duration = (System.nanoTime() - start) / 1000000l; // milliseconds
                Tr.event(this, tc, "Metatype for resource adapter " + generalAdapterName + " generated in " + (duration / 1000.0f) + " seconds",
                         primaryMetatype == null ? null : primaryMetatype.toMetatypeString(true));
            }
        }

        return primaryMetatype;
    }

    /**
     * Main entry point for building the metatype objects
     *
     * @throws UnavailableException
     * @throws InvalidPropertyException
     * @throws ResourceAdapterInternalException
     * @throws ClassNotFoundException
     */
    private void buildMetatype() throws InvalidPropertyException, UnavailableException, ResourceAdapterInternalException, ClassNotFoundException {
        MetaGenInstance instance = config.getInstance();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Building metatype for:", instance);
            Tr.debug(this, tc, "adapterName", instance.adapterName);
            Tr.debug(this, tc, "raXmlFile", instance.xmlFileSet.raXmlFile);
            Tr.debug(this, tc, "wlpRaXmlFile", instance.xmlFileSet.wlpRaXmlFile);
            Tr.debug(this, tc, "parsedXml", instance.xmlFileSet.parsedXml);
            Tr.debug(this, tc, "parsedWlpXml", instance.xmlFileSet.parsedWlpXml);
        }

        // This applies the wlp settings after all the merging is complete.
        if (instance.xmlFileSet.parsedWlpXml != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "combining ra and wlp xmls");
            }
            DeploymentDescriptorParser.combineWlpAndRaXmls(instance.adapterName, instance.xmlFileSet.parsedXml, instance.xmlFileSet.parsedWlpXml);
        }
        WlpIbmuiGroups ibmuiGroups = instance.xmlFileSet.parsedXml.getWlpIbmuiGroups();
        if (ibmuiGroups != null) {
            config.setIbmuiGroups(ibmuiGroups);
            instance.metatype.setIbmuiGroupOrder(ibmuiGroups.getGroupOrder());
        }

        instance.metatype.setOriginatingRaConnector(instance.xmlFileSet.parsedXml);
        // buildResourceAdapters(...) MUST EXECUTE FIRST!
        buildResourceAdapters(instance);
        buildConnectionFactories(instance);
        buildAdminObjects(instance);
        buildMessageListeners(instance);
        instance.markAsProcessed();
    }

    /**
     * Builds the resource adapter objects.
     *
     * @param instance the instance to build the resource adapter from
     * @throws InvalidPropertyException
     * @throws ResourceAdapterInternalException
     * @throws ClassNotFoundException
     */
    private void buildResourceAdapters(MetaGenInstance instance) throws ResourceAdapterInternalException, InvalidPropertyException, ClassNotFoundException {

        RaResourceAdapter adapter = instance.xmlFileSet.parsedXml.getResourceAdapter();

        // Enforcing JCA 1.6 spec requirement of resource adapter class, whether defined
        // via ra.xml or @Connector annotation has the
        // javax.resource.spi.ResourceAdapter interface.
        //
        // From the spec:
        //   The Connector annotation is a component-defining annotation and it can be used
        //   by the resource adapter developer to specify that the JavaBean is a resource adapter
        //   JavaBean. The Connector annotation is applied to the JavaBean class and the
        //   JavaBean class must implement the ResourceAdapter interface.
        //
        //   The element resourceadapter-class specifies the
        //   fully qualified name of a Java class that implements
        //   the javax.resource.spi.ResourceAdapter
        //   interface.
        if (adapter.getResourceAdapterClass() != null)
            validateClassImplementsInterface(adapter.getResourceAdapterClass(), ResourceAdapter.class);

        MetatypeDesignate designate = new MetatypeDesignate();
        MetatypeObject object = new MetatypeObject();
        MetatypeOcd ocd = new MetatypeOcd(metaTypeFactoryService);
        String pid;
        if (instance.adapterName.equals("wmqJms")) {
            String raAlias = "wmqJmsClient";
            pid = InternalConstants.JCA_UNIQUE_PREFIX + '.' + raAlias;
            ocd.setAlias(raAlias);
        } else {
//            String raAlias = instance.adapterName;
//            pid = InternalConstants.JCA_UNIQUE_PREFIX + ".resourceAdapter." + raAlias;
//            if (!config.get(".installedByDropins", Boolean.FALSE)) {
//                boolean isRAREmbeddedInApp = config.get(MetaGenConstants.KEY_APP_THAT_EMBEDS_RAR, null) != null;
//                ocd.setExtendsAlias(raAlias);
//                String parentPID = isRAREmbeddedInApp ? "com.ibm.ws.jca.embeddedResourceAdapter" : "wasJms".equals(instance.adapterName)
//                                                                                                   || "wmqJms".equals(instance.adapterName) ? "com.ibm.ws.jca.bundleResourceAdapter" : "com.ibm.ws.jca.resourceAdapter";
//            }
            String raAlias = "properties." + instance.adapterName;
            pid = InternalConstants.JCA_UNIQUE_PREFIX + ".resourceAdapter." + raAlias;
            if (!config.get(".installedByDropins", Boolean.FALSE)) {
                boolean isRAREmbeddedInApp = config.get(MetaGenConstants.KEY_APP_THAT_EMBEDS_RAR, null) != null;
                ocd.setChildAlias(raAlias);
                String parentPID = isRAREmbeddedInApp ? "com.ibm.ws.jca.embeddedResourceAdapter" : "wasJms".equals(instance.adapterName)
                                                                                                   || "wmqJms".equals(instance.adapterName) ? "com.ibm.ws.jca.bundleResourceAdapter" : "com.ibm.ws.jca.resourceAdapter";
                ocd.setParentPID(parentPID);
            }
        }

        designate.setInternalInformation(ConstructType.ResourceAdapter, adapter);
        designate.setFactoryPid(pid);
        object.setOcdref(pid);
        designate.setObject(object);
        instance.metatype.addDesignate(designate);

        ocd.setId(pid);
        RaConnector connector = instance.xmlFileSet.parsedXml;

        // Possible values for name
        // "Name from wlp-ra.xml"
        // "MyAdapter Properties"
        // "MyAdapter Properties, Version 1"
        // "MyAdapter (My Display Name) Properties"
        // "MyAdapter (My Display Name) Properties, Version 1"
        String displayName = adapter.getName(); // from wlp-ra.xml
        if (displayName == null) {
            for (RaDisplayName dname : connector.getDisplayName()) {
                if ("en".equalsIgnoreCase(dname.getLang())) {
                    displayName = dname.getValue();
                    break;
                }
                displayName = dname.getValue();
            }
            if (displayName == null)
                displayName = instance.adapterName + " Properties";
            else
                displayName = instance.adapterName + " (" + displayName + ") Properties";

            String version = connector.getResourceAdapterVersion();
            if (version != null && version.length() > 0)
                displayName += ", Version " + version;
        }
        ocd.setName(displayName);

        String description = adapter.getDescription(); // from wlp-ra.xml
        String descFromConnector = "";
        for (com.ibm.ws.javaee.dd.common.Description desc : connector.getDescriptions()) {
            if (desc.getLang().equals("en")) {
                descFromConnector = desc.getValue();
                break;
            }
            descFromConnector = desc.getValue();
        }
        //TODO Support multiple language descriptions in metatypes. Currently only english is supported.
        ocd.setDescription(description == null ? descFromConnector : description);
        ocd.setExtends("com.ibm.ws.jca.resourceAdapter.properties");
        object.setMatchingOcd(ocd);

        // add internal attributes to metatype
        ocd.addInternalMetatypeAd("contextService.target", config.get("contextService.target", "(service.pid=com.ibm.ws.context.manager)"));
        ocd.addInternalMetatypeAd("executorService.target", "(service.pid=com.ibm.ws.threading)");
        ocd.addInternalMetatypeAd("id", instance.adapterName);
        ocd.addInternalMetatypeAd("resourceAdapterService.target", "(id=" + instance.adapterName + ')');
        if (adapter.getResourceAdapterClass() != null)
            ocd.addInternalMetatypeAd("resourceadapter-class", adapter.getResourceAdapterClass());

        // add requiredContextProvider.target to metatype
        List<String> requiredWorkContexts = instance.metatype.getOriginatingRaConnector().getRequiredWorkContext();
        if (requiredWorkContexts != null && requiredWorkContexts.size() > 0) {
            Set<String> types = new HashSet<String>();
            types.add(HintsContext.class.getName()); // provided by JCA feature itself
            StringBuilder filter = new StringBuilder(85);
            for (int i = 0; i < requiredWorkContexts.size(); i++) {
                String workContextType = requiredWorkContexts.get(i);
                if (ExecutionContext.class.getName().equals(workContextType))
                    workContextType = TransactionContext.class.getName();
                if (types.add(workContextType))
                    filter.append("(type=").append(workContextType).append(')');
            }
            int numRequiredWorkContexts = types.size() - 1; // ignore HintsContext because JCA processes it internally
            if (numRequiredWorkContexts > 0) {
                if (numRequiredWorkContexts > 1)
                    filter.insert(0, "(|").append(')');
                ocd.addInternalMetatypeAd("requiredContextProvider.target", filter.toString());

                MetatypeAd ad = new MetatypeAd(metaTypeFactoryService);
                ad.setId("requiredContextProvider.cardinality.minimum");
                ad.setType("Integer");
                ad.setDefault(Integer.toString(numRequiredWorkContexts));
                ad.setFinal(true);
                ad.setName("internal");
                ad.setDescription("internal use only");
                ocd.addMetatypeAd(ad);
            }
        }

        RaOutboundResourceAdapter outbound = adapter.getOutboundResourceAdapter();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "outbound is:", outbound);
        if (outbound != null) {
            String transactionSupport = outbound.getTransactionSupport();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "outbound transaction support:", transactionSupport);
            if (transactionSupport != null)
                ocd.addInternalMetatypeAd("transaction-support", transactionSupport);

            // add reauthentication-support to metatype
            if (outbound.getReauthenticationSupport() != null) {
                MetatypeAd reauthSupport = new MetatypeAd(metaTypeFactoryService);
                reauthSupport.setId("reauthentication-support");
                reauthSupport.setType("Boolean");
                reauthSupport.setFinal(true);
                reauthSupport.setDefault(outbound.getReauthenticationSupport());
                reauthSupport.setName("internal");
                reauthSupport.setDescription("internal use only");
                ocd.addMetatypeAd(reauthSupport);
            }
        }

        // Add module-name to metatype
        String modName = instance.getModuleName();
        if (modName == null)
            modName = connector.getModuleName();
        // don't add if module name is not present.
        if (modName != null)
            ocd.addInternalMetatypeAd("module-name", modName);

        for (RaConfigProperty configProperty : adapter.getConfigProperties()) {
            MetatypeAd ad = convertRaConfigPropertyToMetatypeAd(instance.adapterName, configProperty, ConstructType.ResourceAdapter);
            if (ad != null && !ocd.addMetatypeAd(ad)) {
                Tr.warning(tc, "J2CA9918.attrdef.already.processed", ad.getID(), instance.adapterName);
                if (!config.isRuntime())
                    buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9918.attrdef.already.processed", ad.getID(), instance.adapterName));
            }
        }

        instance.metatype.addOcd(ocd);
    }

    /**
     * Builds the connection factory objects.
     *
     * @param instance the instance to build the connection factory from
     * @throws InvalidPropertyException
     * @throws ResourceAdapterInternalException
     */
    private void buildConnectionFactories(MetaGenInstance instance) throws ClassNotFoundException, InvalidPropertyException, ResourceAdapterInternalException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        RaResourceAdapter adapter = instance.xmlFileSet.parsedXml.getResourceAdapter();
        RaOutboundResourceAdapter outboundAdapter = adapter.getOutboundResourceAdapter();

        if (outboundAdapter == null)
            return;

        /*
         * JCA 1.6 spec schema definition
         * If any of the outbound resource adapter elements (transaction-support,
         * authentication-mechanism, reauthentication-support) is specified through
         * this element or metadata annotations, and no connection-definition is
         * specified as part of this element or through annotations, the
         * application server must consider this an error and fail deployment.
         */
        // Full profile does not validate this, so remove it for maximum
        // compatibility.  Keep the logic but commented out for illustration and
        // reference for the otherwise unused J2CA9912E.
//        if (outboundAdapter.getConnectionDefinitions().isEmpty() &&
//            (outboundAdapter.getTransactionSupport() != null ||
//             !outboundAdapter.getAuthenticationMechanisms().isEmpty() ||
//            outboundAdapter.getReauthenticationSupport() != null)) {
//            // TODOCJN Should this be a warning or an exception?
//            //Tr.warning(tc, "J2CA9912.conn.defn.required", cfInterface, "<connection-defintion>", "@ConnectionDefinition");
//            //if (!config.isRuntime())
//            //    buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9912.conn.defn.required", cfInterface, "<connection-defintion>", "@ConnectionDefinition"));
//            throw new ResourceAdapterInternalException(Tr.formatMessage(tc,
//                                                                        "J2CA9912.conn.defn.required"));
//        }

        String baseExtendsAlias = instance.adapterName;
        ChildAliasSelector childAliasSelector = new ChildAliasSelector();
        Map<RaConnectionDefinition, MetatypeOcd> deferred = new HashMap<RaConnectionDefinition, MetatypeOcd>();
        Map<String, String> connectionDefinitionsValidation = new HashMap<String, String>();
        for (RaConnectionDefinition connectionFactory : outboundAdapter.getConnectionDefinitions()) {
            // Enforcing JCA 1.6 spec requirement of connection definition managed factory class, whether defined
            // via ra.xml or ConnectionDefinition/ConnectionDefinitions annotation has the
            // javax.resource.spi.ManagedConnectionFactory interface.
            //
            // From the spec:
            //   The ConnectionDefinition and ConnectionDefinitions annotations are
            //   applied to the JavaBean class and are restricted to be applied only on JavaBean
            //   classes that implement the ManagedConnectionFactory interface (see
            //   Section 5.3.2, "ManagedConnectionFactory JavaBean and Outbound
            //   Communication" on page 5-8).

            //   The element managedconnectionfactory-class specifies
            //   the fully qualified name of the Java class that
            //   implements the
            //   javax.resource.spi.ManagedConnectionFactory interface.
            validateClassImplementsInterface(connectionFactory.getManagedConnectionFactoryClass(),
                                             ManagedConnectionFactory.class);

            String cfInterface = connectionFactory.getConnectionFactoryInterface();
            String cfImpl = connectionFactory.getConnectionFactoryImplClass();
            String parentPid = connectionFactory.getConnectionFactoryParentPid();
            String cfFactoryPid = parentPid + ".properties." + baseExtendsAlias + '.' + cfInterface;
            String cfOcdRef = cfFactoryPid;

            // JCA Spec 1.6 XML Schema connectionfactory-interface-uniqueness
            //   The connectionfactory-interface element content
            //   must be unique in the outbound-resourceadapter.
            //   Multiple connection-definitions can not use the
            //   same connectionfactory-type.
            if (connectionDefinitionsValidation.get(cfInterface) == null) {
                connectionDefinitionsValidation.put(cfInterface, cfImpl);
            } else {
                // TODOCJN Should this be a warning or an exception?
                //Tr.warning(tc, "J2CA9920.duplicate.type", cfInterface, "<connection-defintion>", "@ConnectionDefinition");
                //if (!config.isRuntime())
                //    buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9920.duplicate.type", cfInterface, "<connection-defintion>", "@ConnectionDefinition"));
                throw new ResourceAdapterInternalException(Tr.formatMessage(tc,
                                                                            "J2CA9920.duplicate.type",
                                                                            cfInterface,
                                                                            "<connection-definition>",
                                                                            "@ConnectionDefinition"));
            }

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "Building connection factory: " + cfFactoryPid);

            MetatypeDesignate designate = new MetatypeDesignate();
            MetatypeObject object = new MetatypeObject();

            designate.setInternalInformation(ConstructType.ConnectionFactory, connectionFactory);
            designate.setFactoryPid(cfFactoryPid);
            object.setOcdref(cfOcdRef);
            designate.setObject(object);
            instance.metatype.getDesignates().add(designate);

            MetatypeOcd ocd = new MetatypeOcd(metaTypeFactoryService);
            ocd.setId(cfOcdRef);
            String suffixOverride = suffixOverridesByBoth.get(cfInterface + '-' + cfImpl);
            if (suffixOverride == null) {
                suffixOverride = suffixOverridesByImpl.get(cfImpl);
                if (suffixOverride == null) {
                    suffixOverride = suffixOverridesByIntf.get(cfInterface);
                    if (suffixOverride == null)
                        suffixOverride = connectionFactory.getAliasSuffix();
                }
            }
            boolean isGeneric = RaConnectionDefinition.parentPids.get(cfInterface) == null;
            if (suffixOverride == null && isGeneric)
                deferred.put(connectionFactory, ocd);
            else {
                if (suffixOverride == null || suffixOverride.length() == 0) {
                    ocd.setName(instance.adapterName + " Properties");
                    ocd.setExtendsAlias(baseExtendsAlias);
                    suffixOverride = "";
                } else {
                    ocd.setName(instance.adapterName + ' ' + suffixOverride + " Properties");
                    ocd.setExtendsAlias(baseExtendsAlias + '.' + suffixOverride);
                }
                if (isGeneric)
                    childAliasSelector.reserve(suffixOverride, ocd);
            }
            ocd.setExtends(parentPid + ".properties");
            if (connectionFactory.getName() != null)
                ocd.setName(connectionFactory.getName());
            ocd.setDescription(connectionFactory.getDescription() != null ? connectionFactory.getDescription() : ("Properties for " + cfInterface + " (" + instance.adapterName
                                                                                                                  + ')'));
            object.setMatchingOcd(ocd);

            // add connectionfactory-interface to metatype
            addInterfaces(ocd, connectionFactory.getConnectionFactoryInterface());

            // add other internal attributes
            ocd.addInternalMetatypeAd("managedconnectionfactory-class", connectionFactory.getManagedConnectionFactoryClass());
            ocd.addInternalMetatypeAd("resourceAdapterConfig.id", instance.adapterName);

            for (RaConfigProperty configProperty : connectionFactory.getConfigProperties()) {
                MetatypeAd ad = convertRaConfigPropertyToMetatypeAd(instance.adapterName, configProperty, ConstructType.ConnectionFactory);
                if (ad != null && !ocd.addMetatypeAd(ad)) {
                    Tr.warning(tc, "J2CA9918.attrdef.already.processed", ad.getID(), instance.adapterName);
                    if (!config.isRuntime())
                        buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9918.attrdef.already.processed", ad.getID(), instance.adapterName));
                }
            }

            instance.metatype.getOcds().add(ocd);
        }

        // Second pass to assign child aliases.
        for (Map.Entry<RaConnectionDefinition, MetatypeOcd> entry : deferred.entrySet()) {
            MetatypeOcd ocd = entry.getValue();
            String cfInterface = entry.getKey().getConnectionFactoryInterface();
            String cfInterfaceSimpleName = cfInterface.substring(cfInterface.lastIndexOf('.') + 1);
            String cfImpl = entry.getKey().getConnectionFactoryImplClass();
            String cfImplSimpleName = cfImpl.substring(cfImpl.lastIndexOf('.') + 1);
            ArrayList<String> preferredAliasSuffixes = new ArrayList<String>(4);
            preferredAliasSuffixes.add("");
            if (!cfInterfaceSimpleName.equals(cfImplSimpleName))
                preferredAliasSuffixes.add(cfInterfaceSimpleName);
            preferredAliasSuffixes.add(cfImplSimpleName);
            preferredAliasSuffixes.add(cfInterface);
            childAliasSelector.rank(ocd, preferredAliasSuffixes);
        }
        childAliasSelector.assign(instance.adapterName, baseExtendsAlias);
    }

    /**
     * Builds the administered object objects.
     *
     * @param instance the instance to build the administered object from
     * @throws InvalidPropertyException
     * @throws ResourceAdapterInternalException
     */
    private void buildAdminObjects(MetaGenInstance instance) throws ClassNotFoundException, InvalidPropertyException, ResourceAdapterInternalException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        RaResourceAdapter adapter = instance.xmlFileSet.parsedXml.getResourceAdapter();

        String baseExtendsAlias = instance.adapterName;

        // Map of adminObject/jmsQueue/... pid to child alias selector
        Map<String, ChildAliasSelector> childAliasSelectors = new HashMap<String, ChildAliasSelector>();

        Map<RaAdminObject, MetatypeOcd> deferred = new HashMap<RaAdminObject, MetatypeOcd>();
        Map<String, String> adminObjectsValidation = new HashMap<String, String>();

        for (RaAdminObject adminObject : adapter.getAdminObjects()) {
            String aoInterface = adminObject.getMetaAdminObjectInterface();
            String aoImpl = adminObject.getAdminObjectClass();
            String aoInterfaceAndImpl = aoInterface + '-' + aoImpl;
            String parentPid = adminObject.getParentPid();
            String aoFactoryPid = parentPid + ".properties." + baseExtendsAlias + '.' + aoInterfaceAndImpl;
            String aoOcdRef = aoFactoryPid;

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "Building admin object: " + aoFactoryPid);

            // JCA Spec 1.6, 20.4.1
            // There must be no more than one administered
            // object definition with the same interface and Class name combination in a
            // resource adapter.
            if (adminObjectsValidation.get(aoInterfaceAndImpl) == null) {
                adminObjectsValidation.put(aoInterfaceAndImpl, aoImpl);
            } else {
                // TODOCJN Should this be a warning or an exception?
                //Tr.warning(tc, "J2CA9920.duplicate.type", aoInterface, "<adminobject-interface>/<adminobject-class>", "@AdministeredObject");
                //if (!config.isRuntime())
                //    buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9920.duplicate.type", aoInterface, "<adminobject-interface>/<adminobject-class>", "@AdministeredObject"));
                throw new ResourceAdapterInternalException(Tr.formatMessage(tc,
                                                                            "J2CA9920.duplicate.type",
                                                                            aoInterface,
                                                                            "<adminobject-interface>",
                                                                            "@AdministeredObject"));
            }

            MetatypeDesignate designate = new MetatypeDesignate();
            MetatypeObject object = new MetatypeObject();

            designate.setInternalInformation(ConstructType.AdminObject, adminObject);
            designate.setFactoryPid(aoFactoryPid);
            object.setOcdref(aoOcdRef);
            designate.setObject(object);
            instance.metatype.getDesignates().add(designate);

            MetatypeOcd ocd = new MetatypeOcd(metaTypeFactoryService);
            ocd.setId(aoOcdRef);
            String suffixOverride = suffixOverridesByBoth.get(aoInterfaceAndImpl);
            if (suffixOverride == null) {
                suffixOverride = suffixOverridesByImpl.get(aoImpl);
                if (suffixOverride == null) {
                    suffixOverride = suffixOverridesByIntf.get(aoInterface);
                    if (suffixOverride == null)
                        suffixOverride = adminObject.getAliasSuffix();
                }
            }
            if (suffixOverride == null)
                deferred.put(adminObject, ocd);
            else {
                if (suffixOverride.length() == 0) {
                    ocd.setName(instance.adapterName + " Properties");
                    ocd.setExtendsAlias(baseExtendsAlias);
                } else {
                    ocd.setName(instance.adapterName + ' ' + suffixOverride + " Properties");
                    ocd.setExtendsAlias(baseExtendsAlias + '.' + suffixOverride);
                }
                ChildAliasSelector childAliasSelector = childAliasSelectors.get(parentPid);
                if (childAliasSelector == null)
                    childAliasSelectors.put(parentPid, childAliasSelector = new ChildAliasSelector());
                childAliasSelector.reserve(suffixOverride, ocd);
            }
            ocd.setExtends(parentPid + ".properties");
            if (adminObject.getName() != null)
                ocd.setName(adminObject.getName());
            ocd.setDescription(adminObject.getDescription() != null ? adminObject.getDescription() : ("Properties for " + aoInterface + " implementation " + aoImpl + " ("
                                                                                                      + instance.adapterName + ')'));
            object.setMatchingOcd(ocd);

            // add adminobject-interface to metatype
            addInterfaces(ocd, adminObject.getMetaAdminObjectInterface());

            // add other internal attributes
            ocd.addInternalMetatypeAd("adminobject-class", adminObject.getAdminObjectClass());
            ocd.addInternalMetatypeAd("resourceAdapterConfig.id", instance.adapterName);

            for (RaConfigProperty configProperty : adminObject.getConfigProperties()) {
                MetatypeAd ad = convertRaConfigPropertyToMetatypeAd(instance.adapterName, configProperty, ConstructType.AdminObject);
                if (ad != null && !ocd.addMetatypeAd(ad)) {
                    Tr.warning(tc, "J2CA9918.attrdef.already.processed", ad.getID(), instance.adapterName);
                    if (!config.isRuntime())
                        buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9918.attrdef.already.processed", ad.getID(), instance.adapterName));
                }
            }

            instance.metatype.getOcds().add(ocd);
        }

        // Second pass to identify and rank child aliases
        for (Map.Entry<RaAdminObject, MetatypeOcd> entry : deferred.entrySet()) {
            RaAdminObject adminObject = entry.getKey();
            MetatypeOcd ocd = entry.getValue();
            String parentPid = adminObject.getParentPid();
            String aoImpl = adminObject.getAdminObjectClass();
            String aoInterface = adminObject.getMetaAdminObjectInterface();
            String aoImplSimpleName = aoImpl.substring(aoImpl.lastIndexOf('.') + 1);
            String aoInterfaceSimpleName = aoInterface.substring(aoInterface.lastIndexOf('.') + 1);

            ChildAliasSelector childAliasSelector = childAliasSelectors.get(parentPid);
            if (childAliasSelector == null)
                childAliasSelectors.put(parentPid, childAliasSelector = new ChildAliasSelector());

            ArrayList<String> preferredAliasSuffixes = new ArrayList<String>(5);
            preferredAliasSuffixes.add("");
            boolean isGeneric = RaAdminObject.parentPids.get(aoInterface) == null;
            if (isGeneric && !aoInterfaceSimpleName.equals(aoImplSimpleName))
                preferredAliasSuffixes.add(aoInterfaceSimpleName);
            preferredAliasSuffixes.add(aoImplSimpleName);
            if (isGeneric) {
                preferredAliasSuffixes.add(aoInterfaceSimpleName + '-' + aoImplSimpleName);
                preferredAliasSuffixes.add(aoInterface + '-' + aoImpl);
            } else
                preferredAliasSuffixes.add(aoImpl);
            childAliasSelector.rank(ocd, preferredAliasSuffixes);
        }

        // Assign child aliases
        for (ChildAliasSelector childAliasSelector : childAliasSelectors.values())
            childAliasSelector.assign(instance.adapterName, baseExtendsAlias);
    }

    /**
     * Builds the message listener objects.
     *
     * @param instance the instance to build the message listener from
     * @throws InvalidPropertyException
     * @throws UnavailableException
     * @throws ClassNotFoundException
     * @throws ResourceAdapterInternalException
     */
    private void buildMessageListeners(MetaGenInstance instance) throws InvalidPropertyException, UnavailableException, ClassNotFoundException, ResourceAdapterInternalException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        RaResourceAdapter adapter = instance.xmlFileSet.parsedXml.getResourceAdapter();
        RaInboundResourceAdapter inboundAdapter = adapter.getInboundResourceAdapter();

        if (inboundAdapter == null)
            return;

        RaMessageAdapter messageAdapter = inboundAdapter.getMessageAdapter();

        if (messageAdapter == null)
            return;

        String baseExtendsAlias = instance.adapterName;
        ChildAliasSelector childAliasSelector = new ChildAliasSelector();
        Map<RaMessageListener, MetatypeOcd> deferred = new HashMap<RaMessageListener, MetatypeOcd>();
        Map<String, String> messageListenersValidation = new HashMap<String, String>();
        for (RaMessageListener messageListener : messageAdapter.getMessageListeners()) {
            String mlInterface = messageListener.getMessageListenerType();
            String asImpl = messageListener.getActivationSpec().getActivationSpecClass();
            if (trace && tc.isDebugEnabled()) {
                Tr.debug(tc, "message listener type: " + mlInterface);
                Tr.debug(tc, "message listener activation spec: " + asImpl);
            }

            // from jca 1.6 spec
            // 1. A resource adapter capable of message delivery to message
            // endpoints must annotate a JavaBean with the Activation annotation
            // for each supported endpoint message listener type.
            // 2. The resource adapter provider may annotate one or more JavaBeans
            // with the Activation annotation.
            // 3. The messageListeners annotation element indicates the message listener
            // type(s) supported with the ActivationSpec JavaBean.
            // from the xsd in the 1.6 spec:
            // The messagelistener-type element content must be
            // unique in the messageadapter. Several messagelisteners
            // can not use the same messagelistener-type.
            if (messageListenersValidation.get(mlInterface) == null) {
                messageListenersValidation.put(mlInterface, asImpl);
            } else {
                // TODOCJN Should this be a warning or an exception?
                //Tr.warning(tc, "J2CA9920.duplicate.type", mlInterface, "<messagelistener-type>", "@Activation");
                //if (!config.isRuntime())
                //    buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9920.duplicate.type", mlInterface, "<messagelistener-type>", "@Activation"));
                throw new ResourceAdapterInternalException(Tr.formatMessage(tc,
                                                                            "J2CA9920.duplicate.type",
                                                                            mlInterface,
                                                                            "<messagelistener-type>",
                                                                            "@Activation"));
            }

            String parentPid = messageListener.getMessageListenerParentPid();
            String mlFactoryPid = parentPid + ".properties." + baseExtendsAlias + '.' + mlInterface;
            String mlOcdRef = mlFactoryPid;

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "Building message listener: " + mlFactoryPid);

            MetatypeDesignate designate = new MetatypeDesignate();
            MetatypeObject object = new MetatypeObject();

            designate.setInternalInformation(ConstructType.MessageListener, messageListener);
            designate.setFactoryPid(mlFactoryPid);
            object.setOcdref(mlOcdRef);
            designate.setObject(object);
            instance.metatype.addDesignate(designate);

            MetatypeOcd ocd = new MetatypeOcd(metaTypeFactoryService);
            ocd.setId(mlOcdRef);
            String suffixOverride = suffixOverridesByBoth.get(mlInterface + '-' + asImpl);
            if (suffixOverride == null) {
                suffixOverride = suffixOverridesByImpl.get(asImpl);
                if (suffixOverride == null) {
                    suffixOverride = suffixOverridesByIntf.get(mlInterface);
                    if (suffixOverride == null)
                        suffixOverride = messageListener.getAliasSuffix();
                }
            }
            boolean isGeneric = RaMessageListener.parentPids.get(mlInterface) == null;
            if (suffixOverride == null && isGeneric)
                deferred.put(messageListener, ocd);
            else {
                if (suffixOverride == null || suffixOverride.length() == 0) {
                    ocd.setName(instance.adapterName + " Properties");
                    ocd.setExtendsAlias(baseExtendsAlias);
                    suffixOverride = "";
                } else {
                    ocd.setName(instance.adapterName + ' ' + suffixOverride + " Properties");
                    ocd.setExtendsAlias(baseExtendsAlias + '.' + suffixOverride);
                }
                if (isGeneric)
                    childAliasSelector.reserve(suffixOverride, ocd);
            }
            ocd.setExtends(parentPid + ".properties");
            if (messageListener.getName() != null)
                ocd.setName(messageListener.getName());
            ocd.setDescription(messageListener.getDescription() != null ? messageListener.getDescription() : ("Properties for " + mlInterface + " (" + instance.adapterName + ')'));
            object.setMatchingOcd(ocd);

            RaActivationSpec activationSpec = messageListener.getActivationSpec();

            // Enforcing JCA 1.6 spec requirement of activation spec class is either annotated
            // or has the ActivationSpec interface.
            //
            // From the spec:
            //   The JavaBean is not required to implement the javax.resource.spi.ActivationSpec interface if
            //   the JavaBean is annotated with the Activation annotation.
            // This seems incorrect with regards to not requiring that an annotated class implement the
            // interface and the JCA 1.7 spec may correct this.  Checking both non annotated and annotated
            // classes for now.
            //if (!activationSpec.isAnnotated())
            validateClassImplementsInterface(activationSpec.getActivationSpecClass(), ActivationSpec.class);

            // add internal attributes to metatype
            ocd.addInternalMetatypeAd("activationspec-class", activationSpec.getActivationSpecClass());
            ocd.addInternalMetatypeAd("resourceAdapterConfig.id", instance.adapterName);

            RaConfigProperty destinationProp = null;
            for (RaConfigProperty configProp : activationSpec.getConfigProperties()) {
                if ("destination".equals(configProp.getName())) {
                    destinationProp = configProp;
                    break;
                }
            }

            if (destinationProp != null) {
                destinationProp.isProcessed = false;

                // add destinationRef to metatype
                MetatypeAd destinationRef = new MetatypeAd(metaTypeFactoryService);
                destinationRef.setId("destinationRef");
                destinationRef.setType("String");
                destinationRef.setReferencePid("com.ibm.ws.jca.adminObject.supertype");
                destinationRef.setIbmType("pid");
                destinationRef.setCardinality(0);
                destinationRef.setRequired(Boolean.TRUE.equals(destinationProp.getRequired()));
                destinationRef.setName("Destination");
                destinationRef.setDescription("Destination");
                destinationRef.setNLSKey(destinationProp.getNLSKey()); // use wlp-ra.xml "destination" NLS key if present
                ocd.addMetatypeAd(destinationRef);
            }

            // Process activation spec required properties
            StringBuilder requiredPropNames = new StringBuilder();
            for (RaRequiredConfigProperty requiredConfigProperty : activationSpec.getRequiredConfigProperties()) {
                String requiredPropName = requiredConfigProperty.getConfigPropertyName();
                if (requiredPropNames.length() > 0)
                    requiredPropNames.append(',');
                requiredPropNames.append(requiredPropName);
                RaConfigProperty configProperty = activationSpec.getConfigPropertyById(requiredPropName);
                if (configProperty == null && instance.xmlFileSet.parsedWlpXml != null) {
                    // look for it in the wlp-ra.xml
                    WlpRaMessageListener wlpMessageListener = instance.xmlFileSet.parsedWlpXml.getMessageListener(messageListener.getMessageListenerType());
                    if (wlpMessageListener != null) {
                        WlpRaConfigProperty wlpConfigProperty = wlpMessageListener.getActivationSpec().getConfigPropertyById(requiredPropName);
                        if (wlpConfigProperty != null) {
                            configProperty = new RaConfigProperty();
                            configProperty.copyWlpSettings(wlpConfigProperty);
                            if (trace && tc.isDebugEnabled())
                                Tr.debug(this, tc, "Configuration property was found in wlp-ra.xml, not ra.xml");

                            MetatypeAd ad = convertRaConfigPropertyToMetatypeAd(instance.adapterName, configProperty, ConstructType.MessageListener);
                            if (ad != null && !ocd.addMetatypeAd(ad)) {
                                Tr.warning(tc, "J2CA9918.attrdef.already.processed", ad.getID(), instance.adapterName);
                                if (!config.isRuntime())
                                    buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9918.attrdef.already.processed", ad.getID(), instance.adapterName));
                            }
                        } else {
                            Tr.warning(tc, "J2CA9905.required.prop.missing", requiredPropName, instance.adapterName);
                            if (!config.isRuntime())
                                buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9905.required.prop.missing", requiredPropName, instance.adapterName));
                        }
                    } else {
                        Tr.warning(tc, "J2CA9905.required.prop.missing", requiredPropName, instance.adapterName);
                        if (!config.isRuntime())
                            buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9905.required.prop.missing", requiredPropName, instance.adapterName));
                    }
                } else if (configProperty != null && !configProperty.isProcessed) { // if the config property has already been processed (i.e. already converted), don't do it again
                    MetatypeAd ad = convertRaConfigPropertyToMetatypeAd(instance.adapterName, configProperty, ConstructType.MessageListener);
                    if (ad != null)
                        if (ocd.addMetatypeAd(ad))
                            configProperty.isProcessed = true;
                        else {
                            Tr.warning(tc, "J2CA9918.attrdef.already.processed", ad.getID(), instance.adapterName);
                            if (!config.isRuntime())
                                buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9918.attrdef.already.processed", ad.getID(), instance.adapterName));
                        }
                }
            }

            // Add information to the metatype about required config properties for activation
            if (requiredPropNames.length() > 0) {
                MetatypeAd ad = new MetatypeAd(metaTypeFactoryService);
                ad.setId("required-config-property");
                ad.setType("String");
                ad.setDefault(requiredPropNames.toString());
                ad.setCardinality(10000);
                ad.setFinal(true);
                ad.setName("internal");
                ad.setDescription("internal use only");
                ocd.addMetatypeAd(ad);
            }

            for (RaConfigProperty configProperty : activationSpec.getConfigProperties()) {
                MetatypeAd ad = convertRaConfigPropertyToMetatypeAd(instance.adapterName, configProperty, ConstructType.MessageListener);
                if (ad != null && !ocd.addMetatypeAd(ad)) {
                    Tr.warning(tc, "J2CA9918.attrdef.already.processed", ad.getID(), instance.adapterName);
                    if (!config.isRuntime())
                        buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9918.attrdef.already.processed", ad.getID(), instance.adapterName));
                }
            }

            instance.metatype.addOcd(ocd);
        }

        // Second pass to assign child aliases.
        for (Map.Entry<RaMessageListener, MetatypeOcd> entry : deferred.entrySet()) {
            MetatypeOcd ocd = entry.getValue();
            String mlInterface = entry.getKey().getMessageListenerType();
            String mlInterfaceSimpleName = mlInterface.substring(mlInterface.lastIndexOf('.') + 1);
            String asImpl = entry.getKey().getActivationSpec().getActivationSpecClass();
            String asImplSimpleName = asImpl.substring(asImpl.lastIndexOf('.') + 1);
            ArrayList<String> preferredAliasSuffixes = new ArrayList<String>(4);
            preferredAliasSuffixes.add("");
            if (!mlInterfaceSimpleName.equals(asImplSimpleName))
                preferredAliasSuffixes.add(mlInterfaceSimpleName);
            preferredAliasSuffixes.add(asImplSimpleName);
            preferredAliasSuffixes.add(mlInterface);
            childAliasSelector.rank(ocd, preferredAliasSuffixes);
        }
        childAliasSelector.assign(instance.adapterName, baseExtendsAlias);
    }

    private void validateClassImplementsInterface(String className, Class<?> interfaceClass) throws ResourceAdapterInternalException, ClassNotFoundException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (config.isRuntime()) {
            if (trace && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Check class " +
                                   className +
                                   " implements " + interfaceClass.getName() + " interface.");
            }
            Class<?> cl = raClassLoader.loadClass(className);
            if (trace && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "loaded class : " + cl);
            }

            boolean interfaceFound = false;
            boolean examineSuperClass = true;
            while (examineSuperClass) {
                if (lookForInterface(cl, interfaceClass)) {
                    interfaceFound = true;
                    examineSuperClass = false;
                    break;
                }
                if (examineSuperClass)
                    cl = cl.getSuperclass();
                if (cl == null)
                    examineSuperClass = false;
            }

            if (!interfaceFound) {
                // TODOCJN should this be a warning or an exception?
                //Tr.warning(tc, "J2CA9903.required.rainterface.missing",
                //           className,
                //           interfaceClass.getName()));
                //if (!config.isRuntime())
                //    buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9903.required.rainterface.missing",
                //                                           className,
                //                                           interfaceClass.getName()));
                throw new ResourceAdapterInternalException(Tr.formatMessage(tc,
                                                                            "J2CA9903.required.rainterface.missing",
                                                                            className,
                                                                            interfaceClass.getName()));
            }
        }
    }

    private boolean lookForInterface(Class<?> cl, Class<?> interfaceClass) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        Class<?>[] interfaces = cl.getInterfaces();
        if (interfaces.length == 0)
            return false;

        for (Class<?> intf : interfaces) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "Check interface " + intf.getName());
            if (intf == interfaceClass)
                return true;
            if (lookForInterface(intf, interfaceClass))
                return true;
        }
        return false;
    }

    /**
     * Converts an ra.xml config-property into a AD element for the metatype
     *
     * @param adapterName    the name of the resource adapter
     * @param configProperty the parsed ra.xml config-property to convert
     * @param cType          the construct type
     * @return the generated metatype AD object
     * @throws InvalidPropertyException
     */
    private MetatypeAd convertRaConfigPropertyToMetatypeAd(final String adapterName, RaConfigProperty configProperty, ConstructType cType) throws InvalidPropertyException {
        if (configProperty.isProcessed)
            return null;

        MetatypeAd ad_configProperty = new MetatypeAd(metaTypeFactoryService);

        String propName = configProperty.getName();
        ad_configProperty.setId(propName);
        if (configProperty.getWlpDefault() != null)
            ad_configProperty.setDefault(configProperty.getWlpDefault()); // wlp-ra.xml default overrules ra.xml default
        else if (configProperty.getDefault() != null)
            ad_configProperty.setDefault(configProperty.getDefault()); // ra.xml default
        else if (configProperty.getRequired() != null)
            ad_configProperty.setRequired(configProperty.getRequired());
        else
            ad_configProperty.setRequired(false);

        if (configProperty.getConfidential() != null && configProperty.getConfidential()
            || propName.toUpperCase().contains("PASSWORD"))
            ad_configProperty.setIbmType("password");

        if (configProperty.getIgnore() != null && configProperty.getIgnore()) {
            // hide from configuration, but honor default value if specified
            ad_configProperty.setName("internal");
            ad_configProperty.setDescription("internal use only");
            ad_configProperty.setFinal(true);
        } else {
            ad_configProperty.setName(configProperty.getName());

            String cpDesc = null;
            for (RaDescription raDesc : configProperty.getDescription()) {
                if ("en".equalsIgnoreCase(raDesc.getLang())) {
                    cpDesc = raDesc.getValue();
                    break;
                }
                cpDesc = raDesc.getValue();
            }
            if (cpDesc != null)
                ad_configProperty.setDescription(cpDesc);
            ad_configProperty.setFinal(configProperty.getIbmFinal());

            if (cType == ConstructType.ConnectionFactory || cType == ConstructType.MessageListener) {
                String name = configProperty.getName().toUpperCase();
                if ("PASSWORD".equals(name) || "USER".equals(name) || "USERNAME".equals(name))
                    // special case for connection factories - override the description for user/userName/password to discourage usage
                    if (cType == ConstructType.ConnectionFactory)
                        ad_configProperty.setRecommendAuthAliasUsage(true);
                    // special case for activation spec - omit user/userName/password from the metatype
                    else
                        return null;
            }

            ad_configProperty.setMax(configProperty.getMax());
            ad_configProperty.setMin(configProperty.getMin());

            if (configProperty.getConfigOptions() != null) {
                List<WlpConfigOption> options = configProperty.getConfigOptions();

                for (WlpConfigOption option : options) {
                    MetatypeAdOption ad_option = new MetatypeAdOption();
                    ad_option.setLabel(option.getLabel());
                    ad_option.setValue(option.getValue());
                    ad_option.setNLSKey(option.getNLSKey());
                    ad_configProperty.getOptions().add(ad_option);
                }
            }

            ad_configProperty.setIbmuiGroup(configProperty.getIbmuiGroup());
        }

        String wlpCustomType = configProperty.getWlpType();
        if (wlpCustomType == null) {
            String type = configProperty.getType();
            ad_configProperty.setType(type == null ? "String" : MetatypeAd.getTypeName(type)); // default to String if type unspecified
        } else if (MetatypeAd.TYPES.get(wlpCustomType) != null) {
            // If wlpCustomType is a normal metatype type (String, Integer, etc), set it as the type
            ad_configProperty.setType(wlpCustomType);
        } else {
            // Set type="String", ibm:type="wlpCustomType"
            ad_configProperty.setIbmType(wlpCustomType);
            ad_configProperty.setType("String");
        }

        // validation of type would be unreachable here, already done earlier

        if (configProperty.isOptionLabelNLSDisabled())
            ad_configProperty.disableOptionLabelNLS();

        ad_configProperty.setNLSKey(configProperty.getNLSKey());

        return ad_configProperty;
    }

    /**
     * Merge two metatypes together.
     *
     * @param metatype1 the metatype that will act as the base, into which the second
     *                      metatype will be merged into.
     * @param metatype2 the metatype to merge into the first
     */
    private void mergeMetatypes(Metatype metatype1, Metatype metatype2) {
        List<MetatypeDesignate> m2Designates = metatype2.getDesignates();
        for (MetatypeDesignate m2Designate : m2Designates) {
            MetatypeDesignate m1MatchingDesignate;

            if (m2Designate.getFactoryPid() != null)
                m1MatchingDesignate = metatype1.getDesignateByFactoryPid(m2Designate.getFactoryPid());
            else
                m1MatchingDesignate = metatype1.getDesignateByPid(m2Designate.getPid());

            if (m1MatchingDesignate == null) {
                // Designate pid/factoryPid not common between the two metatypes
                MetatypeOcd m2Ocd = metatype2.getOcdById(m2Designate.getObject().getOcdref());
                MetatypeOcd m1MatchingOcd = metatype1.getOcdById(m2Ocd.getId());

                if (m1MatchingOcd == null) {
                    // metatype1's designate Object ocdref doesn't map to an OCD construct in metatype1, thus it is safe to copy
                    metatype1.addDesignate(m2Designate);
                    metatype1.addOcd(m2Ocd);
                } else {
                    // metatype1 doesn't contain a matching Designate but it does contain a matching OCD
                }
            } else {
                // metatype1 contains a matching Designate
            }
        }
    }

    /**
     * Called before main generator method. Parses input configuration,
     * including parsing the ra.xml and wlp-ra.xml files if that has not already been done.
     * If this is running in the server, then the parsing will have been done during
     * connector adapting.
     *
     * @param configProps generator configuration
     * @throws IOException
     * @throws JAXBException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ClassNotFoundException
     * @throws ResourceAdapterInternalException
     * @throws UnableToAdaptException
     * @throws InstantiationException
     * @throws UnavailableException
     * @throws InvalidPropertyException
     * @throws ResourceAdapterInstallException
     */
    private void setup(Map<String, Object> configProps) throws IOException, JAXBException, SAXException, ParserConfigurationException, ClassNotFoundException, ResourceAdapterInternalException, UnableToAdaptException, InstantiationException, InvalidPropertyException, UnavailableException, ResourceAdapterInstallException {

        config = new MetaGenConfig(configProps);
        generalAdapterName = config.getInstance().adapterName;

        suffixOverridesByIntf = config.get(MetaGenConstants.KEY_SUFFIX_OVERRIDES_BY_INTERFACE, Collections.<String, String> emptyMap());
        suffixOverridesByImpl = config.get(MetaGenConstants.KEY_SUFFIX_OVERRIDES_BY_IMPL, Collections.<String, String> emptyMap());
        suffixOverridesByBoth = config.get(MetaGenConstants.KEY_SUFFIX_OVERRIDES_BY_BOTH, Collections.<String, String> emptyMap());

        if (!config.isRuntime()) {
            DeploymentDescriptorParser.init();
        } else {
            raClassLoader = config.getRarClassLoader();
        }
        // parse ra.xml and wlp-ra.xml files
        MetaGenInstance instance = config.getInstance();
        // Explicit mode means that the build time generator will be provided with the ra.xml/wlp_ra.xml files
        // RarMode mode, not runtime, means that the ra.xml/wlp.ra.xml must be obtained from the rar file
        // RarMode mode and runtime means that ra.xml/wlp_ra.xml have already been parsed and merged by
        // the connector adapter code
        // So the real indicator is, if RarMode and parsed/merged xml, then it will be in the xmlFileSet,
        // otherwise this needs to be done
        if (config.getGenerationMode() == MetaGenConfig.GenerationMode.ExplicitMode) {
            instance.xmlFileSet.parsedXml = (RaConnector) DeploymentDescriptorParser.parseResourceAdapterXml(Utils.getFileInputStreamPrivileged(instance.xmlFileSet.raXmlFile),
                                                                                                             "ra.xml", false);
            if (instance.xmlFileSet.wlpRaXmlFile != null)
                instance.xmlFileSet.parsedWlpXml = (WlpRaConnector) DeploymentDescriptorParser.parseResourceAdapterXml(Utils.getFileInputStreamPrivileged(instance.xmlFileSet.wlpRaXmlFile),
                                                                                                                       "wlp-ra.xml", false);
        } else if (config.getGenerationMode() == MetaGenConfig.GenerationMode.RarMode
        // && !config.isRuntime()   // TODOCJN remove the isRuntime check for now
        ) {
            parseResourceAdapterXmlsFromRar(instance.adapterName, instance.xmlFileSet, instance.getModuleName());
        }
    }

    /**
     * Runs after the main metatype generator method executes. If there are multiple
     * metatype files they will be combined; writes the metatype file.
     *
     * @param configProps generator configuration
     * @throws IOException
     * @throws JAXBException
     * @throws InvalidPropertyException
     */
    private void postBuild(Map<String, Object> configProps) throws IOException, JAXBException, InvalidPropertyException {
        primaryMetatype = config.getInstance().metatype;

        translateAndBuildNLSFile(); // NLS file production needs to run before writing metatype.xml
        writeMetatypeToFile();
    }

    /**
     * The module annotations. This field is only non-null for runtime processing.
     */
    ModuleAnnotations moduleAnnotations;

    /**
     * Get a list of all the classes in the connector module or rar. This list is only populated for build-time processing.
     *
     * @param containerToAdapt
     * @return
     * @throws UnableToAdaptException
     */
    List<String> classNames = new ArrayList<String>();

    // This code is used when RAR mode is used at build time.
    // TODO: Currently no component is using this option and this code does not find all .class files in
    // the RAR since it does not process the jars in a rar.
    private void getBuildtimeRarClasses(final XmlFileSet xmlFileSet) throws IOException, JAXBException, SAXException, ParserConfigurationException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // new JarFile(...) throws IOException and we need to catch it and rethrow it outside the doPriv
        final AtomicReference<IOException> exceptionRef = new AtomicReference<IOException>();
        JarFile rar = AccessController.doPrivileged(new PrivilegedAction<JarFile>() {
            @Override
            public JarFile run() {
                try {
                    return new JarFile(xmlFileSet.rarFile);
                } catch (IOException e) {
                    exceptionRef.set(e);
                    return null;
                }
            }
        });

        if (exceptionRef.get() != null)
            throw exceptionRef.get();

        try {
            Enumeration<JarEntry> entries = rar.entries();
            List<String> classNames = new ArrayList<String>(); // TODO: remove if not needed

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (!entry.isDirectory()) {
                    String fileName = entryName.substring(entry.getName().lastIndexOf('/') + 1);
                    int dotClassIndex = entryName.indexOf(".class");
                    if (fileName.endsWith(".class")) {
                        String className = entryName.replaceAll("/", "\\.");
                        classNames.add(className.substring(0, dotClassIndex));
                    } else if (fileName.equals(InternalConstants.RA_XML_FILE_NAME)) {
                        xmlFileSet.rarRaXmlFilePath = entry.getName();
                        xmlFileSet.parsedXml = (RaConnector) DeploymentDescriptorParser.parseResourceAdapterXml(rar.getInputStream(entry), InternalConstants.RA_XML_FILE_NAME,
                                                                                                                false);
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, InternalConstants.RA_XML_FILE_NAME + ": " + xmlFileSet.parsedXml);
                    } else if (fileName.equals(InternalConstants.WLP_RA_XML_FILE_NAME)) {
                        xmlFileSet.rarWlpRaXmlFilePath = entry.getName();
                        xmlFileSet.parsedWlpXml = (WlpRaConnector) DeploymentDescriptorParser.parseResourceAdapterXml(rar.getInputStream(entry),
                                                                                                                      InternalConstants.WLP_RA_XML_FILE_NAME, false);
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, InternalConstants.WLP_RA_XML_FILE_NAME + ": " + xmlFileSet.parsedWlpXml);
                    }
                }
            }
        } finally {
            rar.close();
        }
    }

    /**
     * Scans a RAR file for the wlp-/ra.xml files and parses them.
     *
     * @param adapterName the name of the resource adapter
     * @param xmlFileSet  the XmlFileSet that contains the RAR file
     * @throws IOException
     * @throws JAXBException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws ClassNotFoundException
     * @throws ResourceAdapterInternalException
     * @throws UnableToAdaptException
     * @throws UnavailableException
     * @throws InvalidPropertyException
     * @throws ResourceAdapterInstallException
     */
    private void parseResourceAdapterXmlsFromRar(final String adapterName, final XmlFileSet xmlFileSet,
                                                 final String moduleName) throws IOException, JAXBException, SAXException, ParserConfigurationException, ClassNotFoundException, ResourceAdapterInternalException, UnableToAdaptException, InvalidPropertyException, UnavailableException, ResourceAdapterInstallException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (!config.isRuntime()) {
            // TODOCJN, need to either remove the RARMode at buildtime option or add processing for jars in rars
            getBuildtimeRarClasses(xmlFileSet);

            for (String s : classNames) {
                if (trace && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "className: " + s);
                }
            }
        }

        if (trace && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "config.useAnnotations: " + config.useAnnotations());
            Tr.debug(this, tc, "config.getRarClassLoader: " + config.getRarClassLoader());
            Tr.debug(this, tc, "xmlFileSet.parsedXml: " + xmlFileSet.parsedXml);
        }

        if (config.useAnnotations()) {
            moduleAnnotations = getModuleAnnotations();
            if (trace && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "moduleAnnotations: " + moduleAnnotations);
            }

            RAAnnotationProcessor processor = new RAAnnotationProcessor(adapterName, xmlFileSet.parsedXml, config.getRarClassLoader(), moduleAnnotations, classNames);

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "processor:", processor);

            RaConnector parsedXml = processor.getProcessedConnector();
            if (xmlFileSet.parsedXml == null && !processor.isAnnotatedConnector()) {
                // not annotated connector and no resource adapter xml
                if (Connector.class.getPackage().getName().startsWith("jak"))
                    throw new ResourceAdapterInstallException(Tr.formatMessage(tc, "J2CA9944.missing.connector.jakarta", adapterName));
                else
                    throw new ResourceAdapterInstallException(Tr.formatMessage(tc, "J2CA9945.missing.connector.javax", adapterName));
            }
            xmlFileSet.parsedXml = parsedXml;
        } else {
            if (xmlFileSet.parsedXml == null) {
                // no resource adapter xml
                throw new ResourceAdapterInstallException(Tr.formatMessage(tc, "J2CA9943.missing.connector.dd", adapterName));
            }
        }

        if (config.isRuntime()) {
            // Note: this modifies the RaConnector in place, which has the
            // potential to cause problems for other consumers.  Ideally, we
            // would copy the RaConnector and update that copy, but there is
            // no convenient way to do that.
            processJavaBeanProperties(xmlFileSet.parsedXml);
        }
    }

    private ModuleAnnotations getModuleAnnotations() throws UnableToAdaptException {
        return AnnotationsBetaHelper.getModuleAnnotations(config.getRarContainer());
    }

    /**
     * Process java bean properties and merge them into configuration properties.
     */
    void processJavaBeanProperties(RaConnector connector) throws ResourceAdapterInternalException {
        RaResourceAdapter adapter = connector.getResourceAdapter();

        String adapterClassName = adapter.getResourceAdapterClass();
        if (adapterClassName != null) {
            processJavaBeanProperties(adapterClassName, adapter.getConfigProperties(), true);
        }

        RaOutboundResourceAdapter outboundAdapter = adapter.getOutboundResourceAdapter();
        if (outboundAdapter != null) {
            for (RaConnectionDefinition connectionFactory : outboundAdapter.getConnectionDefinitions()) {
                processJavaBeanProperties(connectionFactory.getManagedConnectionFactoryClass(), connectionFactory.getConfigProperties(), false);
            }
        }

        for (RaAdminObject adminObject : adapter.getAdminObjects()) {
            processJavaBeanProperties(adminObject.getAdminObjectClass(), adminObject.getConfigProperties(), false);
        }

        RaInboundResourceAdapter inboundAdapter = adapter.getInboundResourceAdapter();
        if (inboundAdapter != null) {
            RaMessageAdapter messageAdapter = inboundAdapter.getMessageAdapter();
            if (messageAdapter != null) {
                for (RaMessageListener messageListener : messageAdapter.getMessageListeners()) {
                    RaActivationSpec activationSpec = messageListener.getActivationSpec();
                    processJavaBeanProperties(activationSpec.getActivationSpecClass(), activationSpec.getConfigProperties(), false);
                }
            }
        }
    }

    /**
     * Process java bean properties and merge them into configuration properties.
     *
     * @param className       the java bean class name
     * @param propertyList    the existing property list to update
     * @param processDefaults true if java bean property default values should be merged for resource adapters
     */
    private void processJavaBeanProperties(String className,
                                           @Sensitive /* reduce trace noise */List<RaConfigProperty> propertyList,
                                           boolean processDefaults) throws ResourceAdapterInternalException {
        Class<?> klass;
        try {
            klass = config.getRarClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            e.getClass(); // findbugs
            Tr.warning(tc, "J2CA9919.class.not.found", className, generalAdapterName);
            return;
        }

        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(klass);
        } catch (IntrospectionException e) {
            throw new ResourceAdapterInternalException(e);
        }

        Set<String> beanPropNames = new HashSet<String>();
        Map<String, RaConfigProperty> propertyMap = null;
        Object bean = null;

        for (PropertyDescriptor propDesc : beanInfo.getPropertyDescriptors()) {
            // Skip read-only properties.
            if (propDesc.getWriteMethod() == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "skipping read only property", propDesc);
                continue;
            }

            String propName = toCamelCase(propDesc.getName());
            Class<?> propType = propDesc.getPropertyType();

            if (!beanPropNames.add(propName)) {
                Tr.warning(tc, "J2CA9918.attrdef.already.processed", propName, config.getInstance().adapterName);
                if (!config.isRuntime())
                    buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9918.attrdef.already.processed", propName, config.getInstance().adapterName));
                continue;
            }

            if (propType.isPrimitive()) {
                propType = MetatypeAd.getBoxedType(propType);
            } else if (!MetatypeAd.isTypeClassName(propType.getName())) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "skipping property with unsupported type", propDesc);
                continue;
            }

            // Lazily initialize the property map.
            if (propertyMap == null) {
                propertyMap = new HashMap<String, RaConfigProperty>();
                for (RaConfigProperty property : propertyList) {
                    propertyMap.put(property.getName().toUpperCase(), property);
                }
            }

            // Get or create the property
            RaConfigProperty property = propertyMap.get(propName.toUpperCase());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "processing property", propDesc, property);

            if (property == null) {
                property = new RaConfigProperty();
                property.setName(propName);
                propertyList.add(property);
            }

            // Merge the name. When case doesn't match, choose the one that matches the property descriptor
            if (!propName.equals(property.getName()))
                property.setName(propName);

            // Merge the type.
            if (property.getType() == null) {
                property.setType(propType.getName());
            }

            // Merge the default value if necessary and possible.
            if (processDefaults && property.getDefault() == null) {
                Method readMethod = propDesc.getReadMethod();
                if (readMethod != null) {
                    // Lazily instantiate the bean that will be used to obtain
                    // the default value for all properties.
                    if (bean == null) {
                        try {
                            bean = klass.getConstructor().newInstance();
                        } catch (Exception e) {
                            Throwable cause = e instanceof InvocationTargetException ? e.getCause() : e;
                            Tr.warning(tc, "J2CA9936.default.value.error", propName, className, generalAdapterName, cause.toString());
                            continue;
                        }
                    }

                    Object defaultValue;
                    try {
                        defaultValue = readMethod.invoke(bean);
                    } catch (Exception e) {
                        Throwable cause = e instanceof InvocationTargetException ? e.getCause() : e;
                        Tr.warning(tc, "J2CA9936.default.value.error", propName, className, generalAdapterName, cause.toString());
                        continue;
                    }

                    if (defaultValue != null) {
                        property.setDefault(defaultValue.toString());
                    }
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "updated property", property);
        }
    }

    /**
     * Write the generated metatype to file. If the output path already contains
     * a metatype.xml file, then it is backed up and a new metatype.xml is created
     * containing the generated code. Also, if one already exists, the two metatypes
     * (the generated and pre-existing one) will attempt to be merged.
     *
     * @throws IOException
     * @throws JAXBException
     */
    private void writeMetatypeToFile() throws IOException, JAXBException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (config.getMetatypeOutputFile() == null)
            return;

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "Output path: " + config.getMetatypeOutputFile().getAbsolutePath());

        if (config.getMetatypeInputFile() != null && Utils.doesFileExistPrivileged(config.getMetatypeInputFile())) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "Input metatype.xml found; merging with generated metatype");

            // merge the pre-existing metatype
            FileInputStream customInput = Utils.getFileInputStreamPrivileged(config.getMetatypeInputFile());
            Metatype custom = null;
            // TODOCJN customContext was added by Tim, not sure if that is correct
            //Unmarshaller customUnmarshaller = metatypeContext.createUnmarshaller();
            JAXBContext customContext = JAXBContext.newInstance(Metatype.class);
            Unmarshaller customUnmarshaller = customContext.createUnmarshaller();

            try {
                custom = (Metatype) customUnmarshaller.unmarshal(customInput);
            } finally {
                customInput.close();
            }

            mergeMetatypes(primaryMetatype, custom);
        }

        // write the final metatype.xml file
        BufferedWriter writer = new BufferedWriter(new FileWriter(config.getMetatypeOutputFile()));
        try {
            writer.write(primaryMetatype.toMetatypeString(true));
        } finally {
            writer.close();
        }
    }

    /**
     * Builds the metatype.properties file containing the generated NLS keys.
     * If the provided file doesn't contain a key, it will be added to the file
     * and a warning message will be displayed regarding the missing message.
     * If the provided file contains keys/messages that are not in the generated
     * metatype, they will not be copied over.
     *
     * @throws IOException
     */
    private void translateAndBuildNLSFile() throws IOException {
        if (!config.doTranslate())
            return;

        HashMap<String, String> originalNLSMap = new HashMap<String, String>();
        List<String> ibmuiGroups = new ArrayList<String>();
        List<String> commonKeys = new ArrayList<String>();
        boolean nlsOutputFilePresent = config.getNLSOutputFile() != null;

        // write new metatype.properties file
        StringBuilder nls = null;

        if (nlsOutputFilePresent) {
            nls = new StringBuilder();
            nls.append("# Licensed Materials - Property of IBM").append(Utils.NEW_LINE);
            nls.append("#").append(Utils.NEW_LINE);
            nls.append("# \"Restricted Materials of IBM\"").append(Utils.NEW_LINE);
            nls.append("#").append(Utils.NEW_LINE);
            nls.append("# Copyright IBM Corp. ").append(Calendar.getInstance().get(Calendar.YEAR)).append(" All Rights Reserved.").append(Utils.NEW_LINE);
            nls.append("#").append(Utils.NEW_LINE);
            nls.append("# US Government Users Restricted Rights - Use, duplication or").append(Utils.NEW_LINE);
            nls.append("# disclosure restricted by GSA ADP Schedule Contract with").append(Utils.NEW_LINE);
            nls.append("# IBM Corp.").append(Utils.NEW_LINE);
            nls.append("#").append(Utils.NEW_LINE);
            nls.append("# -------------------------------------------------------------------------------------------------").append(Utils.NEW_LINE);
            nls.append("#").append(Utils.NEW_LINE);
        }

        File nlsFile = config.getNLSInputFile();
        if (nlsFile != null) {
            // read original NLS keys/messages
            BufferedReader reader = new BufferedReader(new FileReader(nlsFile));
            String line;

            try {
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.startsWith("# ") && line.length() > 1) {
                        if (line.startsWith("#")) {
                            if (nlsOutputFilePresent)
                                nls.append(line).append(Utils.NEW_LINE);
                        } else {
                            int index = line.indexOf('=');
                            String key = line.substring(0, index);
                            String msg = line.substring(index + 1);
                            if (originalNLSMap.put(key, msg) != null) {
                                Tr.warning(tc, "J2CA9916.duplicate.nls.key", nlsFile.getCanonicalPath(), generalAdapterName, key);
                                if (!config.isRuntime())
                                    buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9916.duplicate.nls.key", nlsFile.getCanonicalPath(), generalAdapterName, key));
                            }
                        }
                    }
                }
            } finally {
                reader.close();
            }
        }

        if (nlsOutputFilePresent)
            nls.append("#").append(Utils.NEW_LINE).append(Utils.NEW_LINE);

        for (MetatypeDesignate designate : primaryMetatype.getDesignates()) {
            MetatypeOcd ocd = designate.getObject().getMatchingOcd();
            ocd.sort(true);
            String nameKey = null, descKey = null, message, name, desc;
            boolean isCommon = false;

            if (nlsOutputFilePresent)
                nls.append(Utils.NEW_LINE);

            if (designate.getChildOcdType() == ConstructType.AdminObject) {
                if (nlsOutputFilePresent)
                    nls.append("# Administered Object: ");
                nameKey = ((RaAdminObject) designate.getMatchingRaXmlObject()).getNLSKey();
            } else if (designate.getChildOcdType() == ConstructType.ConnectionFactory) {
                if (nlsOutputFilePresent)
                    nls.append("# Connection Factory: ");
                nameKey = ((RaConnectionDefinition) designate.getMatchingRaXmlObject()).getNLSKey();
            } else if (designate.getChildOcdType() == ConstructType.MessageListener) {
                if (nlsOutputFilePresent)
                    nls.append("# Message Listener: ");
                nameKey = ((RaMessageListener) designate.getMatchingRaXmlObject()).getNLSKey();
            } else if (designate.getChildOcdType() == ConstructType.ResourceAdapter) {
                if (nlsOutputFilePresent)
                    nls.append("# Resource Adapter: ");
                nameKey = ((RaResourceAdapter) designate.getMatchingRaXmlObject()).getNLSKey();
            } else if (designate.getChildOcdType() == ConstructType.Unknown) {
                if (nlsOutputFilePresent)
                    nls.append("# Other Object: ");
            }

            if (nlsOutputFilePresent)
                nls.append(designate.getDesignateID()).append(Utils.NEW_LINE);

            if (!ocd.getName().equals("internal")) {
                // generate OCD name attribute
                if (nameKey == null)
                    nameKey = ocd.getId();

                if (nameKey.startsWith("[common]")) {
                    isCommon = true;
                    nameKey = nameKey.substring("[common]".length() + 1).trim();
                }

                name = ocd.getName();
                message = originalNLSMap.get(nameKey);
                ocd.setName("%" + nameKey);
                if (isCommon) {
                    if (!commonKeys.contains(nameKey))
                        commonKeys.add(nameKey);
                } else if (message != null && !message.isEmpty()) {
                    if (nlsOutputFilePresent)
                        nls.append(nameKey).append('=').append(message).append(Utils.NEW_LINE);
                } else if (nlsOutputFilePresent && name != null && !name.isEmpty()) {
                    // nlsOutputFilePresent is checked above because using the name is only valid as a message if we are writing the output file
                    nls.append(nameKey).append('=').append(name).append(Utils.NEW_LINE);
                } else {
                    Tr.warning(tc, "J2CA9917.missing.nls.msg", nlsFile.getCanonicalPath(), generalAdapterName, nameKey);
                    if (!config.isRuntime())
                        buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9917.missing.nls.msg", nlsFile.getCanonicalPath(), generalAdapterName, nameKey));
                }

                // generate OCD description attribute
                descKey = nameKey + ".desc";
                desc = ocd.getDescription();
                message = originalNLSMap.get(descKey);
                ocd.setDescription("%" + descKey);
                if (isCommon) {
                    if (!commonKeys.contains(descKey))
                        commonKeys.add(descKey);
                } else if (message != null && !message.isEmpty()) {
                    if (nlsOutputFilePresent)
                        nls.append(descKey).append('=').append(message).append(Utils.NEW_LINE);
                } else if (nlsOutputFilePresent && desc != null && !desc.isEmpty()) {
                    // nlsOutputFilePresent is checked above because the desc is only valid as a message if we are writing the output file
                    nls.append(descKey).append('=').append(desc).append(Utils.NEW_LINE);
                } else {
                    Tr.warning(tc, "J2CA9917.missing.nls.msg", nlsFile.getCanonicalPath(), generalAdapterName, descKey);
                    if (!config.isRuntime())
                        buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9917.missing.nls.msg", nlsFile.getCanonicalPath(), generalAdapterName, descKey));
                }
            }

            String nlsUigroup = "(default)";
            for (MetatypeAd ad : ocd.getMetatypeAds()) {
                String uigroup = ad.getIbmUigroup();
                if (uigroup != null && !uigroup.isEmpty() && !uigroup.equals("(default)")) {
                    if (nlsOutputFilePresent && !nlsUigroup.equals(uigroup)) {
                        nlsUigroup = uigroup;
                        nls.append("# ").append(designate.getDesignateID()).append(" UI group '").append(uigroup).append("' AD element keys:").append(Utils.NEW_LINE);
                    }

                    WlpIbmuiGroups ibmuiGroupsObj = config.getIbmuiGroups();
                    String groupNLSKey = ibmuiGroupsObj != null ? ibmuiGroupsObj.getGroupNLSKey(uigroup) : null;

                    if (config.isIbmuiGroupScopeGlobal()) {
                        String global_groupName = (groupNLSKey != null ? groupNLSKey : uigroup) + ".name";
                        String global_groupDesc = (groupNLSKey != null ? groupNLSKey : uigroup) + ".description";

                        if (!ibmuiGroups.contains(global_groupName))
                            ibmuiGroups.add(global_groupName);
                        if (!ibmuiGroups.contains(global_groupDesc))
                            ibmuiGroups.add(global_groupDesc);
                    } else if (config.isIbmuiGroupScopeOcd()) {
                        String ocd_groupName = (groupNLSKey != null ? groupNLSKey : uigroup) + "." + ocd.getId() + ".name";
                        String ocd_groupDesc = (groupNLSKey != null ? groupNLSKey : uigroup) + "." + ocd.getId() + ".description";

                        if (!ibmuiGroups.contains(ocd_groupName))
                            ibmuiGroups.add(ocd_groupName);
                        if (!ibmuiGroups.contains(ocd_groupDesc))
                            ibmuiGroups.add(ocd_groupDesc);
                    }
                }

                name = ad.getName();
                desc = ad.getDescription();

                if (name == null || (name != null && !name.equals("internal"))) {
                    // generate AD name attribute
                    nameKey = ad.getNLSKey();
                    if (nameKey == null)
                        nameKey = ocd.getId() + "." + ad.getID();

                    if (nameKey.startsWith("[common]")) {
                        isCommon = true;
                        nameKey = nameKey.substring("[common]".length() + 1).trim();
                    } else
                        isCommon = false;

                    message = originalNLSMap.get(nameKey);
                    ad.setName("%" + nameKey);
                    if (isCommon) {
                        if (!commonKeys.contains(nameKey))
                            commonKeys.add(nameKey);
                    } else if (message != null && !message.isEmpty()) {
                        if (nlsOutputFilePresent)
                            nls.append(nameKey).append('=').append(message).append(Utils.NEW_LINE);
                    } else if (nlsOutputFilePresent && name != null && !name.isEmpty()) {
                        // nlsOutputFilePresent is checked above because the name is only valid as a message if we are writing the output file
                        nls.append(nameKey).append('=').append(name).append(Utils.NEW_LINE);
                    } else {
                        Tr.warning(tc, "J2CA9917.missing.nls.msg", nlsFile.getCanonicalPath(), generalAdapterName, nameKey);
                        if (!config.isRuntime())
                            buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9917.missing.nls.msg", nlsFile.getCanonicalPath(), generalAdapterName, nameKey));
                    }

                    // generate AD description attribute
                    descKey = nameKey + ".desc";
                    message = originalNLSMap.get(descKey);
                    ad.setDescription("%" + descKey);
                    if (isCommon) {
                        if (ad.isAuthAliasUsageRecommended())
                            descKey = descKey + "*";

                        if (!commonKeys.contains(descKey))
                            commonKeys.add(descKey);
                    } else if (message != null && !message.isEmpty()) {
                        if (nlsOutputFilePresent) {
                            if (ad.isAuthAliasUsageRecommended() && !message.contains(InternalConstants.RECOMMEND_AUTH_ALIAS_MSG))
                                nls.append(descKey).append('=').append(message).append(" (").append(InternalConstants.RECOMMEND_AUTH_ALIAS_MSG).append(')').append(Utils.NEW_LINE);
                            else
                                nls.append(descKey).append('=').append(message).append(Utils.NEW_LINE);
                        }
                    } else if (nlsOutputFilePresent && desc != null && !desc.isEmpty()) {
                        // nlsOutputFilePresent is checked above because the desc is only valid as a message if we are writing the output file
                        if (ad.isAuthAliasUsageRecommended() && !desc.contains(InternalConstants.RECOMMEND_AUTH_ALIAS_MSG))
                            nls.append(descKey).append('=').append(desc).append(" (").append(InternalConstants.RECOMMEND_AUTH_ALIAS_MSG).append(')').append(Utils.NEW_LINE);
                        else
                            nls.append(descKey).append('=').append(desc).append(Utils.NEW_LINE);
                    } else {
                        if (nlsOutputFilePresent && ad.isAuthAliasUsageRecommended())
                            nls.append(descKey).append('=').append('(').append(InternalConstants.RECOMMEND_AUTH_ALIAS_MSG).append(')').append(Utils.NEW_LINE);

                        Tr.warning(tc, "J2CA9917.missing.nls.msg", nlsFile.getCanonicalPath(), generalAdapterName, descKey);
                        if (!config.isRuntime())
                            buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9917.missing.nls.msg", nlsFile.getCanonicalPath(), generalAdapterName, descKey));
                    }
                }

                if (!ad.getOptions().isEmpty() && !ad.isOptionLabelNLSDisabled()) {
                    int labelIndex = 0;
                    for (MetatypeAdOption option : ad.getOptions()) {
                        if (!option.getValue().equals(option.getLabel())) {
                            // generate label attribute
                            String labelKey = option.getNLSKey();
                            if (labelKey == null)
                                labelKey = nameKey + ".option" + labelIndex++;

                            if (labelKey.startsWith("[common]")) {
                                isCommon = true;
                                labelKey = labelKey.substring("[common]".length() + 1).trim();
                            } else
                                isCommon = false;
                            message = originalNLSMap.get(labelKey);
                            option.setLabel('%' + labelKey);
                            if (isCommon) {
                                if (!commonKeys.contains(labelKey))
                                    commonKeys.add(labelKey);
                            } else if (message != null && !message.isEmpty()) {
                                if (nlsOutputFilePresent)
                                    nls.append(labelKey).append('=').append(message);
                            } else if (nlsOutputFilePresent)
                                nls.append(option.getLabel());

                            if (nlsOutputFilePresent)
                                nls.append(Utils.NEW_LINE);
                        }
                    }
                }
            }
        }

        if (!ibmuiGroups.isEmpty()) {
            Collections.sort(ibmuiGroups);

            if (nlsOutputFilePresent)
                nls.append(Utils.NEW_LINE).append("# UI Groups").append(Utils.NEW_LINE);

            for (String uigroup : ibmuiGroups) {
                if (nlsOutputFilePresent)
                    nls.append(uigroup).append('=');
                String message = originalNLSMap.get(uigroup);
                if (message != null && !message.isEmpty()) {
                    if (nlsOutputFilePresent)
                        nls.append(message).append(Utils.NEW_LINE);
                } else {
                    Tr.warning(tc, "J2CA9917.missing.nls.msg", nlsFile.getCanonicalPath(), generalAdapterName, uigroup);
                    if (!config.isRuntime())
                        buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9917.missing.nls.msg", nlsFile.getCanonicalPath(), generalAdapterName, uigroup));
                }
            }
        }

        if (!commonKeys.isEmpty()) {
            Collections.sort(commonKeys);

            if (nlsOutputFilePresent)
                nls.append(Utils.NEW_LINE).append("# Common Messages").append(Utils.NEW_LINE);

            for (String key : commonKeys) {
                boolean authAlias = false;
                if (key.endsWith("*")) {
                    key = key.substring(0, key.length() - 1);
                    authAlias = true;
                }

                if (nlsOutputFilePresent)
                    nls.append(key).append('=');

                String message = originalNLSMap.get(key);
                if (message != null && !message.isEmpty()) {
                    if (nlsOutputFilePresent) {
                        if (authAlias && !message.contains(InternalConstants.RECOMMEND_AUTH_ALIAS_MSG))
                            nls.append(message).append(" (").append(InternalConstants.RECOMMEND_AUTH_ALIAS_MSG).append(')').append(Utils.NEW_LINE);
                        else
                            nls.append(message).append(Utils.NEW_LINE);
                    }
                } else {
                    Tr.warning(tc, "J2CA9917.missing.nls.msg", nlsFile.getCanonicalPath(), generalAdapterName, key);
                    if (!config.isRuntime())
                        buildTimeWarnings.add(Tr.formatMessage(tc, "J2CA9917.missing.nls.msg", nlsFile.getCanonicalPath(), generalAdapterName, key));
                }
            }
        }

        if (nlsOutputFilePresent) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "Writing NLS keys/messages to file: " + config.getNLSOutputFile().getAbsolutePath());

            BufferedWriter writer = new BufferedWriter(new FileWriter(config.getNLSOutputFile()));
            try {
                writer.write(nls.toString());
            } finally {
                writer.close();
            }
        }
    }

    @Trivial
    public static final String toCamelCase(String input) {
        if (input == null
            || input.length() == 0
            || Character.isLowerCase(input.charAt(0))
            || input.length() > 1 && Character.isUpperCase(input.charAt(1))) {
            return input;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(Character.toLowerCase(input.charAt(0)));
            sb.append(input.substring(1));
            return sb.toString();
        }
    }
}
