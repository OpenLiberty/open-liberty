/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import java.io.Externalizable;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.ManagedBean;
import javax.ejb.DependsOn;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.LocalHome;
import javax.ejb.MessageDriven;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Schedule;
import javax.ejb.Schedules;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.ejb.TimedObject;
import javax.ejb.Timeout;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.osgi.BeanRuntime;
import com.ibm.ws.ejbcontainer.osgi.EJBRuntimeVersion;
import com.ibm.ws.ejbcontainer.osgi.MDBRuntime;
import com.ibm.ws.ejbcontainer.osgi.ManagedBeanRuntime;
import com.ibm.ws.ejbcontainer.osgi.SessionBeanRuntime;
import com.ibm.ws.javaee.dd.commonbnd.Interceptor;
import com.ibm.ws.javaee.dd.ejb.ActivationConfig;
import com.ibm.ws.javaee.dd.ejb.ComponentViewableBean;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.Entity;
import com.ibm.ws.javaee.dd.ejb.Interceptors;
import com.ibm.ws.javaee.dd.ejb.Session;
import com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd;
import com.ibm.ws.javaee.dd.ejbext.EJBJarExt;
import com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd;
import com.ibm.ws.metadata.ejb.BeanInitData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.AnnotationValue;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.info.MethodInfo;
import com.ibm.wsspi.anno.service.AnnotationService_Service;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = ModuleInitDataFactory.class)
public class ModuleInitDataFactory {
    private static final TraceComponent tc = Tr.register(ModuleInitDataFactory.class);

    /**
     * Mapping from {@link EnterpriseBean#getKindValue} to the ejb-jar.xml element name.
     */
    private static final String[] KIND_ELEMENT_DISPLAY_NAMES;

    static {
        KIND_ELEMENT_DISPLAY_NAMES = new String[3];
        KIND_ELEMENT_DISPLAY_NAMES[EnterpriseBean.KIND_SESSION] = "<session>";
        KIND_ELEMENT_DISPLAY_NAMES[EnterpriseBean.KIND_MESSAGE_DRIVEN] = "<message-driven>";
        KIND_ELEMENT_DISPLAY_NAMES[EnterpriseBean.KIND_ENTITY] = "<entity>";
    }

    /**
     * Mapping from {@link BeanInitData#ivType} to annotation display names.
     */
    private static final String[] TYPE_TO_ANNOTATION_DISPLAY_NAMES;

    static {
        TYPE_TO_ANNOTATION_DISPLAY_NAMES = new String[8];
        TYPE_TO_ANNOTATION_DISPLAY_NAMES[InternalConstants.TYPE_STATELESS_SESSION] = "@Stateless";
        TYPE_TO_ANNOTATION_DISPLAY_NAMES[InternalConstants.TYPE_STATEFUL_SESSION] = "@Stateful";
        TYPE_TO_ANNOTATION_DISPLAY_NAMES[InternalConstants.TYPE_SINGLETON_SESSION] = "@Singleton";
        TYPE_TO_ANNOTATION_DISPLAY_NAMES[InternalConstants.TYPE_MESSAGE_DRIVEN] = "@MessageDriven";
    }

    /**
     * Mapping from {@link BeanInitData#ivType} to session-type values in ejb-jar.xml.
     */
    private static final String[] TYPE_TO_SESSION_TYPE_VALUES;

    static {
        TYPE_TO_SESSION_TYPE_VALUES = new String[5];
        TYPE_TO_SESSION_TYPE_VALUES[InternalConstants.TYPE_STATELESS_SESSION] = "Stateless";
        TYPE_TO_SESSION_TYPE_VALUES[InternalConstants.TYPE_STATEFUL_SESSION] = "Stateful";
        TYPE_TO_SESSION_TYPE_VALUES[InternalConstants.TYPE_SINGLETON_SESSION] = "Singleton";
    }

    /**
     * Mapping from {@link Session#getSessionTypeValue} + 1 (to properly handle {@link Session#SESSION_TYPE_UNSPECIFIED}) to {@link BeanInitData#ivType}.
     */
    private static final int[] SESSION_TYPE_TO_BEAN_TYPE;

    static {
        SESSION_TYPE_TO_BEAN_TYPE = new int[4];
        SESSION_TYPE_TO_BEAN_TYPE[1 + Session.SESSION_TYPE_UNSPECIFIED] = InternalConstants.TYPE_UNKNOWN;
        SESSION_TYPE_TO_BEAN_TYPE[1 + Session.SESSION_TYPE_STATEFUL] = InternalConstants.TYPE_STATEFUL_SESSION;
        SESSION_TYPE_TO_BEAN_TYPE[1 + Session.SESSION_TYPE_STATELESS] = InternalConstants.TYPE_STATELESS_SESSION;
        SESSION_TYPE_TO_BEAN_TYPE[1 + Session.SESSION_TYPE_SINGLETON] = InternalConstants.TYPE_SINGLETON_SESSION;
    }

    /**
     * Mapping from {@link Entity#getPersistenceTypeValue()} to {@link BeanInitData#ivType}.
     */
    private static final int[] ENTITY_PERSISTENCE_TYPE_TO_BEAN_TYPE;

    static {
        ENTITY_PERSISTENCE_TYPE_TO_BEAN_TYPE = new int[2];
        ENTITY_PERSISTENCE_TYPE_TO_BEAN_TYPE[Entity.PERSISTENCE_TYPE_BEAN] = InternalConstants.TYPE_BEAN_MANAGED_ENTITY;
        ENTITY_PERSISTENCE_TYPE_TO_BEAN_TYPE[Entity.PERSISTENCE_TYPE_CONTAINER] = InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY;
    }

    /**
     * Mapping from {@link Entity#getCMPVersionValue()} + 1 (to properly handle {@link Entity#CMP_VERSION_UNSPECIFIED}) to {@link BeanInitData#ivCMPVersion}.
     */
    private static final int[] ENTITY_CMP_VERSION_TO_BEAN_CMP_VERSION;

    static {
        ENTITY_CMP_VERSION_TO_BEAN_CMP_VERSION = new int[3];
        ENTITY_CMP_VERSION_TO_BEAN_CMP_VERSION[1 + Entity.CMP_VERSION_UNSPECIFIED] = InternalConstants.CMP_VERSION_UNKNOWN;
        ENTITY_CMP_VERSION_TO_BEAN_CMP_VERSION[1 + Entity.CMP_VERSION_1_X] = InternalConstants.CMP_VERSION_1_X;
        ENTITY_CMP_VERSION_TO_BEAN_CMP_VERSION[1 + Entity.CMP_VERSION_2_X] = InternalConstants.CMP_VERSION_2_X;
    }

    private static final Version DEFAULT_VERSION = EJBRuntimeVersion.VERSION_3_1;

    private static final String REFERENCE_ANNOTATION_SERVICE = "annoService";
    private static final String REFERENCE_J2EE_NAME_FACTORY = "j2eeNameFactory";
    private static final String REFERENCE_SESSION_BEAN_RUNTIME = "sessionBeanRuntime";
    private static final String REFERENCE_MDB_RUNTIME = "mdbRuntime";
    private static final String REFERENCE_MANAGED_BEAN_RUNTIME = "managedBeanRuntime";

    private final AtomicServiceReference<AnnotationService_Service> annoServiceSRRef = new AtomicServiceReference<AnnotationService_Service>(REFERENCE_ANNOTATION_SERVICE);
    private final AtomicServiceReference<J2EENameFactory> j2eeNameFactorySRRef = new AtomicServiceReference<J2EENameFactory>(REFERENCE_J2EE_NAME_FACTORY);
    private final AtomicServiceReference<SessionBeanRuntime> sessionBeanRuntimeSRRef = new AtomicServiceReference<SessionBeanRuntime>(REFERENCE_SESSION_BEAN_RUNTIME);
    private final AtomicServiceReference<MDBRuntime> mdbRuntimeSRRef = new AtomicServiceReference<MDBRuntime>(REFERENCE_MDB_RUNTIME);
    private final AtomicServiceReference<ManagedBeanRuntime> managedBeanRuntimeSRRef = new AtomicServiceReference<ManagedBeanRuntime>(REFERENCE_MANAGED_BEAN_RUNTIME);
    private Version runtimeVersion = DEFAULT_VERSION;

    @Activate
    protected void activate(ComponentContext context) {
        annoServiceSRRef.activate(context);
        j2eeNameFactorySRRef.activate(context);
        sessionBeanRuntimeSRRef.activate(context);
        mdbRuntimeSRRef.activate(context);
        managedBeanRuntimeSRRef.activate(context);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        annoServiceSRRef.deactivate(context);
        j2eeNameFactorySRRef.deactivate(context);
        sessionBeanRuntimeSRRef.deactivate(context);
        mdbRuntimeSRRef.deactivate(context);
        managedBeanRuntimeSRRef.deactivate(context);
    }

    @Reference(service = EJBRuntimeVersion.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setEJBRuntimeVersion(ServiceReference<EJBRuntimeVersion> ref) {
        runtimeVersion = Version.parseVersion((String) ref.getProperty(EJBRuntimeVersion.VERSION));
    }

    protected void unsetEJBRuntimeVersion(ServiceReference<EJBRuntimeVersion> ref) {
        runtimeVersion = DEFAULT_VERSION;
    }

    @Reference(name = REFERENCE_ANNOTATION_SERVICE, service = AnnotationService_Service.class)
    protected void setAnnoService(ServiceReference<AnnotationService_Service> ref) {
        annoServiceSRRef.setReference(ref);
    }

    protected void unsetAnnoService(ServiceReference<AnnotationService_Service> ref) {
        annoServiceSRRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_J2EE_NAME_FACTORY, service = J2EENameFactory.class)
    protected void setJ2eeNameFactory(ServiceReference<J2EENameFactory> ref) {
        j2eeNameFactorySRRef.setReference(ref);
    }

    protected void unsetJ2eeNameFactory(ServiceReference<J2EENameFactory> ref) {
        j2eeNameFactorySRRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_SESSION_BEAN_RUNTIME,
               service = SessionBeanRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setSessionBeanRuntime(ServiceReference<SessionBeanRuntime> ref) {
        sessionBeanRuntimeSRRef.setReference(ref);
    }

    protected void unsetSessionBeanRuntime(ServiceReference<SessionBeanRuntime> ref) {
        sessionBeanRuntimeSRRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_MDB_RUNTIME,
               service = MDBRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setMDBRuntime(ServiceReference<MDBRuntime> ref) {
        mdbRuntimeSRRef.setReference(ref);
    }

    protected void unsetMDBRuntime(ServiceReference<MDBRuntime> ref) {
        mdbRuntimeSRRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_MANAGED_BEAN_RUNTIME,
               service = ManagedBeanRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setManagedBeanRuntime(ServiceReference<ManagedBeanRuntime> ref) {
        managedBeanRuntimeSRRef.setReference(ref);
    }

    protected void unsetManagedBeanRuntime(ServiceReference<ManagedBeanRuntime> ref) {
        managedBeanRuntimeSRRef.unsetReference(ref);
    }

    ModuleInitDataImpl createModuleInitData(Container container,
                                            ClassLoader classLoader,
                                            String modName,
                                            String modLogicalName,
                                            String appName,
                                            AnnotationTargets_Targets annoTargets,
                                            InfoStore infoStore,
                                            boolean defaultMetadataComplete) throws EJBConfigurationException {
        SessionBeanRuntime sessionBeanRuntime = sessionBeanRuntimeSRRef.getService();
        MDBRuntime mdbRuntime = mdbRuntimeSRRef.getService();
        EJBJar ejbJar = null;
        if (sessionBeanRuntime != null || mdbRuntime != null) {
            try {
                ejbJar = container.adapt(EJBJar.class);
            } catch (UnableToAdaptException e) {
                throw new EJBConfigurationException(e);
            }
        }

        J2EENameFactory j2eeNameFactory = j2eeNameFactorySRRef.getService();
        ModuleInitDataImpl mid = new ModuleInitDataImpl(
                        modName, appName,
                        ejbJar == null ? EJBJar.VERSION_3_0 : ejbJar.getVersionID(),
                        sessionBeanRuntime,
                        mdbRuntime,
                        managedBeanRuntimeSRRef.getService());
        mid.container = container;
        mid.ivLogicalName = modLogicalName;
        mid.ivJ2EEName = j2eeNameFactory.create(mid.ivAppName, mid.ivName, null);
        mid.ivEJBJar = ejbJar;
        ModuleMergeData modMergeData = new ModuleMergeData(mid, container, annoTargets, infoStore);

        if (mid.ivEJBJar == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "mid.ivEJBJar == null");
            }
            mid.ivMetadataComplete = defaultMetadataComplete;
        } else {
            mid.ivMetadataComplete = mid.ivEJBJar.getVersionID() < EJBJar.VERSION_3_0 || mid.ivEJBJar.isMetadataComplete();

            Interceptors interceptors = mid.ivEJBJar.getInterceptors();
            if (interceptors != null) {
                // Only EJB interceptors may be defined in ejb-jar.xml
                for (com.ibm.ws.javaee.dd.ejb.Interceptor interceptor : interceptors.getInterceptorList()) {
                    mid.addEJBInterceptor(interceptor.getInterceptorClassName());
                }
            }

            for (EnterpriseBean eb : mid.ivEJBJar.getEnterpriseBeans()) {
                String name = eb.getName();
                if (modMergeData.getBeanMergeData(name) != null) {
                    Tr.error(tc, "DUPLICATE_EJB_CNTR4100E", name);
                    modMergeData.error();
                    continue;
                }

                BeanMergeData beanMergeData = modMergeData.createBeanMergeDataFromXML(name, eb.getEjbClassName());
                BeanInitData bid = beanMergeData.getBeanInitData();
                bid.ivEnterpriseBean = eb;

                switch (eb.getKindValue()) {
                    case EnterpriseBean.KIND_SESSION: {
                        Session session = (Session) eb;
                        beanMergeData.setType(SESSION_TYPE_TO_BEAN_TYPE[1 + session.getSessionTypeValue()], mid.sessionBeanRuntime);

                        processComponentViewableBean(bid, session);

                        for (String interfaceName : session.getRemoteBusinessInterfaceNames()) {
                            beanMergeData.addRemoteBusinessInterfaceName(interfaceName);
                        }

                        for (String interfaceName : session.getLocalBusinessInterfaceNames()) {
                            beanMergeData.addLocalBusinessInterfaceName(interfaceName);
                        }

                        bid.ivLocalBean = session.isLocalBean();
                        bid.ivWebServiceEndpointInterfaceName = session.getServiceEndpointInterfaceName();

                        int tranType = session.getTransactionTypeValue();
                        if (tranType != Session.TRANSACTION_TYPE_UNSPECIFIED) {
                            beanMergeData.setBeanManagedTransaction(tranType == Session.TRANSACTION_TYPE_BEAN);
                        }

                        bid.ivHasScheduleTimers = !session.getTimers().isEmpty();

                        if (session.isSetInitOnStartup()) {
                            beanMergeData.setStartup(session.isInitOnStartup());
                        }

                        com.ibm.ws.javaee.dd.ejb.DependsOn dependsOn = session.getDependsOn();
                        if (dependsOn != null) {
                            bid.ivDependsOn = new LinkedHashSet<String>(dependsOn.getEjbName());
                        }

                        if (session.getTimeoutMethod() != null) {
                            mid.ivHasTimers = true;
                        }

                        if (session.isSetPassivationCapable()) {
                            beanMergeData.setPassivationCapable(session.isPassivationCapable());
                        }
                        break;
                    }

                    case EnterpriseBean.KIND_MESSAGE_DRIVEN: {
                        com.ibm.ws.javaee.dd.ejb.MessageDriven messageDriven = (com.ibm.ws.javaee.dd.ejb.MessageDriven) eb;
                        beanMergeData.setType(InternalConstants.TYPE_MESSAGE_DRIVEN, mid.mdbRuntime);
                        bid.ivMessageListenerInterfaceName = messageDriven.getMessagingTypeName();

                        ActivationConfig activationConfig = messageDriven.getActivationConfigValue();
                        bid.ivActivationConfigProperties = new Properties();
                        if (activationConfig != null) {
                            for (com.ibm.ws.javaee.dd.ejb.ActivationConfigProperty property : activationConfig.getConfigProperties()) {
                                bid.ivActivationConfigProperties.put(property.getName(), property.getValue());
                            }
                        }

                        int tranType = messageDriven.getTransactionTypeValue();
                        if (tranType != Session.TRANSACTION_TYPE_UNSPECIFIED) {
                            beanMergeData.setBeanManagedTransaction(tranType == Session.TRANSACTION_TYPE_BEAN);
                        }

                        bid.ivHasScheduleTimers = !messageDriven.getTimers().isEmpty();

                        if (messageDriven.getTimeoutMethod() != null) {
                            mid.ivHasTimers = true;
                        }
                        break;
                    }

                    case EnterpriseBean.KIND_ENTITY: {
                        Entity entity = (Entity) eb;
                        beanMergeData.setType(ENTITY_PERSISTENCE_TYPE_TO_BEAN_TYPE[entity.getPersistenceTypeValue()], null);

                        processComponentViewableBean(bid, entity);

                        bid.ivCMPVersion = ENTITY_CMP_VERSION_TO_BEAN_CMP_VERSION[1 + entity.getCMPVersionValue()];
                        bid.ivHasScheduleTimers = false;
                        break;
                    }

                    default:
                        // Internal error.
                        throw new IllegalStateException("unknown kind " + eb.getKindValue() + ": " + eb);
                }
            }
        }

        if (!mid.ivMetadataComplete || modMergeData.isManagedBeanEnabled()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Processing annotations : mid.ivMetadataComplete=" + mid.ivMetadataComplete);
            }
            AnnotationTargets_Targets targets = modMergeData.getAnnotationTargets();
            infoStore = modMergeData.getInfoStore();

            // d95160:
            //
            // The combination of EJB and MBEAN cases have a careful interaction:
            //
            // Scans of an EJB JAR, if triggered, always set the scan policy to SEED.
            // That is, if scanning is performed (either because the EJB JAR is not
            // metadata-complete or because of a trigger from MBEAN), it is a complete
            // scan of the EJB JAR.

            Collection<String> classNames = new LinkedHashSet<String>();
            if (!mid.ivMetadataComplete && modMergeData.isEJBEnabled()) {

                // d95160: EJB always retrieves from SEED locations.

                classNames.addAll(targets.getAnnotatedClasses(Stateless.class.getName(), AnnotationTargets_Targets.POLICY_SEED));
                classNames.addAll(targets.getAnnotatedClasses(Stateful.class.getName(), AnnotationTargets_Targets.POLICY_SEED));
                classNames.addAll(targets.getAnnotatedClasses(Singleton.class.getName(), AnnotationTargets_Targets.POLICY_SEED));
                classNames.addAll(targets.getAnnotatedClasses(MessageDriven.class.getName(), AnnotationTargets_Targets.POLICY_SEED));
            }

            if (modMergeData.isManagedBeanEnabled()) {

                // d95160:
                //
                // Review notes (w/Tom Bitonti w/Tracy Burroughs):
                //
                // WEB and EJB are handled by modifying code that retrieves the targets tables.
                //
                // EJB technically uses a different rule (SEED) than WEB (which uses SEED | PARTIAL | EXCLUDED).
                // However, for EJB the PARTIAL and EXCLUDED locations are empty.  That allows this code
                // to use SEED | PARTIAL | EXCLUDED in all cases regardless of whether EJB or WEB targets
                // were obtained.

                classNames.addAll(targets.getAnnotatedClasses(ManagedBean.class.getName(), AnnotationTargets_Targets.POLICY_ALL_EXCEPT_EXTERNAL));
            }

            for (String className : classNames) {
                ClassInfo classInfo = infoStore.getDelayableClassInfo(className);
                mergeComponentDefiningAnnotations(classInfo, null, modMergeData, mid.ivMetadataComplete);
            }
        }

        for (BeanMergeData beanMergeData : modMergeData.getBeans()) {
            BeanInitData bid = beanMergeData.getBeanInitData();
            if (bid.ivClassName == null) {
                Tr.error(tc, "UNSPECIFIED_CLASS_CNTR4101E", bid.ivName);
                modMergeData.error();
                continue;
            }

            if (!mid.ivMetadataComplete && beanMergeData.getClassInfo().isArtificial()) {
                Tr.error(tc, "INVALID_CLASS_CNTR4115E", bid.ivClassName, bid.ivName);
                beanMergeData.getModuleMergeData().error();
                continue;
            }

            if (bid.ivType == InternalConstants.TYPE_UNKNOWN) {
                // Session from ejb-jar.xml did not specify session-type.  Look
                // for a component-defining annotation.
                if (!mid.ivMetadataComplete) {
                    mergeComponentDefiningAnnotations(beanMergeData.getClassInfo(), beanMergeData, modMergeData, false);
                }

                if (bid.ivType == InternalConstants.TYPE_UNKNOWN) {
                    Tr.error(tc, "UNSPECIFIED_SESSION_TYPE_CNTR4102E", bid.ivName);
                    modMergeData.error();
                    continue;
                }
            }

            switch (bid.ivType) {
                case InternalConstants.TYPE_STATELESS_SESSION:
                case InternalConstants.TYPE_STATEFUL_SESSION:
                case InternalConstants.TYPE_SINGLETON_SESSION:
                    if (!mid.ivMetadataComplete) {
                        mergeSessionInterfaces(beanMergeData);
                        mergeTransactionManagement(beanMergeData);
                        mergeSingleton(beanMergeData);
                        mergeScheduleTimers(beanMergeData);
                    }
                    break;

                case InternalConstants.TYPE_MESSAGE_DRIVEN:
                    if (!mid.ivMetadataComplete) {
                        mergeMessagingInterface(beanMergeData);
                        mergeTransactionManagement(beanMergeData);
                        mergeScheduleTimers(beanMergeData);
                    }
                    break;

                case InternalConstants.TYPE_MANAGED_BEAN:
                    bid.ivLocalBean = true;
                    bid.ivHasScheduleTimers = Boolean.FALSE;
                    beanMergeData.setBeanManagedTransaction(true);
                    break;
                default:
                    break;
            }

            beanMergeData.merge();

            Collection<String> remoteBusiness = beanMergeData.getRemoteBusinessInterfaceNames();
            Collection<String> localBusiness = beanMergeData.getLocalBusinessInterfaceNames();
            if (!remoteBusiness.isEmpty() && !localBusiness.isEmpty()) {
                Set<String> localBusinessSet = new HashSet<String>(localBusiness);
                for (String interfaceName : remoteBusiness) {
                    if (localBusinessSet.contains(interfaceName)) {
                        Tr.error(tc, "INCOMPATIBLE_INTERFACE_TYPE_CNTR4110E",
                                 bid.ivName,
                                 interfaceName);
                        beanMergeData.getModuleMergeData().error();
                    }
                }
            }
        }

        if (modMergeData.hasErrors()) {
            throw new EJBConfigurationException("Errors occurred processing EJB metadata");
        }

        try {
            if (modMergeData.isEJBEnabled()) {
                mid.ejbJarExtension = container.adapt(EJBJarExt.class);
                if (mid.ejbJarExtension != null) {
                    Map<String, com.ibm.ws.javaee.dd.ejbext.EnterpriseBean> beanExts = getExtensions(mid.ejbJarExtension.getEnterpriseBeans());

                    for (BeanMergeData beanMergeData : modMergeData.getBeans()) {
                        BeanInitDataImpl bid = beanMergeData.getBeanInitData();
                        if (bid.ivType != InternalConstants.TYPE_MANAGED_BEAN) {
                            bid.enterpriseBeanExt = beanExts.remove(bid.getName());
                        }
                    }

                    // Find all left over beans and issue warning that they didn't match up
                    for (Map.Entry<String, com.ibm.ws.javaee.dd.ejbext.EnterpriseBean> entry : beanExts.entrySet()) {
                        String elementName = entry.getValue() instanceof com.ibm.ws.javaee.dd.ejbext.Session ? "<session>" : "<message-driven>";
                        Tr.warning(tc, "ORPHAN_EXTENSION_ENTRY_CNTR4112W", elementName, mid.ivLogicalName, entry.getKey());
                    }
                }

                mid.ejbJarBinding = container.adapt(EJBJarBnd.class);
                if (mid.ejbJarBinding != null) {
                    Map<String, com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean> beanBnds = getBindings(mid.ejbJarBinding.getEnterpriseBeans());
                    for (BeanMergeData beanMergeData : modMergeData.getBeans()) {
                        BeanInitDataImpl bid = beanMergeData.getBeanInitData();
                        if (bid.ivType != InternalConstants.TYPE_MANAGED_BEAN) {
                            bid.beanBnd = beanBnds.get(bid.getName());
                        }
                    }

                    // create interceptor map for later use
                    Map<String, Interceptor> interceptorBindings = new HashMap<String, Interceptor>();
                    for (Interceptor interceptorBnd : mid.ejbJarBinding.getInterceptors()) {
                        interceptorBindings.put(interceptorBnd.getClassName(), interceptorBnd);
                    }
                    mid.ejbJarInterceptorBindings = interceptorBindings;
                }
            }

            if (modMergeData.isManagedBeanEnabled()) {
                mid.managedBeanBinding = container.adapt(ManagedBeanBnd.class);
                if (mid.managedBeanBinding != null) {
                    Map<String, com.ibm.ws.javaee.dd.managedbean.ManagedBean> managedBeanBindings =
                                    getManagedBeanBindings(mid.managedBeanBinding.getManagedBeans());

                    for (BeanMergeData beanMergeData : modMergeData.getBeans()) {
                        BeanInitDataImpl bid = beanMergeData.getBeanInitData();
                        if (bid.ivType == InternalConstants.TYPE_MANAGED_BEAN) {
                            bid.beanBnd = managedBeanBindings.get(bid.ivClassName);
                        }
                    }

                    // create interceptor map for later use
                    Map<String, Interceptor> interceptorBindings = new HashMap<String, Interceptor>();
                    for (Interceptor interceptorBnd : mid.managedBeanBinding.getInterceptors()) {
                        interceptorBindings.put(interceptorBnd.getClassName(), interceptorBnd);
                    }
                    mid.managedBeanInterceptorBindings = interceptorBindings;
                }
            }
        } catch (UnableToAdaptException e) {
            throw new EJBConfigurationException(e);
        }

        if (mid.ivHasTimers == null) {
            boolean hasTimers = false;

            for (BeanMergeData beanMergeData : modMergeData.getBeans()) {
                Boolean hasScheduleTimers = beanMergeData.getBeanInitData().ivHasScheduleTimers;
                if (hasScheduleTimers != null && hasScheduleTimers) {
                    hasTimers = true;
                    break;
                }
            }

            if (!hasTimers) {
                // First, cheaply check if any EJB class implements TimedObject.
                for (BeanMergeData beanMergeData : modMergeData.getBeans()) {
                    if (beanMergeData.getBeanInitData().ivType != InternalConstants.TYPE_MANAGED_BEAN &&
                        beanMergeData.getClassInfo().isInstanceOf(TimedObject.class)) {
                        hasTimers = true;
                        break;
                    }
                }

                // Otherwise, do an expensive check for @Timeout on all methods.
                if (!hasTimers && !mid.ivMetadataComplete) {
                    bean: for (BeanMergeData beanMergeData : modMergeData.getBeans()) {
                        if (beanMergeData.getBeanInitData().ivType != InternalConstants.TYPE_MANAGED_BEAN) {
                            for (MethodInfo methodInfo : beanMergeData.getClassInfo().getMethods()) {
                                if (methodInfo.isAnnotationPresent(Timeout.class.getName())) {
                                    hasTimers = true;
                                    break bean;
                                }
                            }
                        }
                    }
                }
            }

            mid.ivHasTimers = hasTimers;
        }

        for (BeanMergeData beanMergeData : modMergeData.getBeans()) {
            BeanInitData bid = beanMergeData.getBeanInitData();
            bid.ivJ2EEName = j2eeNameFactory.create(mid.ivAppName, mid.ivName, bid.ivName);
            mid.addBean(bid);

            if (!mid.ivMetadataComplete || bid.ivType == InternalConstants.TYPE_MANAGED_BEAN) {
                processInterceptorAnnotation(beanMergeData.getClassInfo(), bid, mid);
            }
        }

        mid.ivClassLoader = classLoader;

        return mid;
    }

    private void processComponentViewableBean(BeanInitData bid, ComponentViewableBean bean) {
        bid.ivRemoteHomeInterfaceName = bean.getHomeInterfaceName();
        bid.ivRemoteInterfaceName = bean.getRemoteInterfaceName();
        bid.ivLocalHomeInterfaceName = bean.getLocalHomeInterfaceName();
        bid.ivLocalInterfaceName = bean.getLocalInterfaceName();
    }

    private void processInterceptorAnnotation(ClassInfo classInfo, BeanInitData bid, ModuleInitDataImpl mid) {
        AnnotationInfo interceptorsInfo = classInfo.getAnnotation(javax.interceptor.Interceptors.class);
        if (interceptorsInfo != null) {
            List<? extends AnnotationValue> interceptors = interceptorsInfo.getArrayValue("value");
            if (interceptors != null && !interceptors.isEmpty()) {
                if (bid.ivType == InternalConstants.TYPE_MANAGED_BEAN) {
                    for (AnnotationValue interceptorValue : interceptors) {
                        mid.addMBInterceptor(interceptorValue.getClassNameValue());
                    }
                } else {
                    for (AnnotationValue interceptorValue : interceptors) {
                        mid.addEJBInterceptor(interceptorValue.getClassNameValue());
                    }
                }
            }
        }
    }

    private void mergeComponentDefiningAnnotations(ClassInfo classInfo, BeanMergeData beanMergeData, ModuleMergeData modData, boolean metadataComplete) {
        if (!metadataComplete && modData.isEJBEnabled()) {
            SessionBeanRuntime sessionBeanRuntime = modData.getSessionBeanRuntime();
            MDBRuntime mdbRuntime = modData.getMDBRuntime();

            AnnotationInfo statelessAnn = classInfo.getAnnotation(Stateless.class);
            if (statelessAnn != null) {
                mergeComponentDefiningAnnotation(classInfo, statelessAnn,
                                                 EnterpriseBean.KIND_SESSION,
                                                 InternalConstants.TYPE_STATELESS_SESSION,
                                                 sessionBeanRuntime,
                                                 beanMergeData, modData);
            }

            AnnotationInfo statefulAnn = classInfo.getAnnotation(Stateful.class);
            if (statefulAnn != null) {
                BeanMergeData newBeanMergeData = mergeComponentDefiningAnnotation(classInfo, statefulAnn,
                                                                                  EnterpriseBean.KIND_SESSION,
                                                                                  InternalConstants.TYPE_STATEFUL_SESSION,
                                                                                  sessionBeanRuntime,
                                                                                  beanMergeData, modData);
                if (beanMergeData == null) {
                    mergePassivationCapable(newBeanMergeData, statefulAnn);
                }
            }

            AnnotationInfo singletonAnn = classInfo.getAnnotation(Singleton.class);
            if (singletonAnn != null) {
                mergeComponentDefiningAnnotation(classInfo, singletonAnn,
                                                 EnterpriseBean.KIND_SESSION,
                                                 InternalConstants.TYPE_SINGLETON_SESSION,
                                                 sessionBeanRuntime,
                                                 beanMergeData, modData);
            }

            AnnotationInfo messageDrivenAnn = classInfo.getAnnotation(MessageDriven.class);
            if (messageDrivenAnn != null) {
                BeanMergeData newBeanMergeData = mergeComponentDefiningAnnotation(classInfo, messageDrivenAnn,
                                                                                  EnterpriseBean.KIND_MESSAGE_DRIVEN,
                                                                                  InternalConstants.TYPE_MESSAGE_DRIVEN,
                                                                                  mdbRuntime,
                                                                                  beanMergeData, modData);
                if (beanMergeData == null) {
                    mergeMessagingInterface(newBeanMergeData, messageDrivenAnn);
                    mergeActivationConfigProperties(newBeanMergeData, messageDrivenAnn);
                }
            }
        }

        // If bean merge data already exists then this is not a managed bean
        // as managed beans cannot be defined in XML.
        if (modData.isManagedBeanEnabled()) {
            AnnotationInfo managedBeanAnn = classInfo.getAnnotation(ManagedBean.class);
            if (managedBeanAnn != null &&
                !modData.containsBeanMergeDataForClass(classInfo.getName())) {
                mergeComponentDefiningAnnotation(classInfo, managedBeanAnn,
                                                 EnterpriseBean.KIND_SESSION,
                                                 InternalConstants.TYPE_MANAGED_BEAN,
                                                 modData.getManagedBeanRuntime(),
                                                 beanMergeData, modData);
            }
        }
    }

    private BeanMergeData mergeComponentDefiningAnnotation(ClassInfo classInfo,
                                                           AnnotationInfo compDefAnn,
                                                           int kind,
                                                           int type,
                                                           BeanRuntime beanRuntime,
                                                           BeanMergeData beanMergeData,
                                                           ModuleMergeData modData) {
        if (beanMergeData != null) {
            // We already have a BeanInitData, so we're just merging the type.
            mergeComponentDefiningAnnotationType(beanMergeData, type, beanRuntime, modData);
            return beanMergeData;
        }

        String name = (type == InternalConstants.TYPE_MANAGED_BEAN) ? getManagedBeansInternalEJBName(classInfo, compDefAnn) : getComponentName(classInfo, compDefAnn);
        String className = classInfo.getName();

        beanMergeData = modData.getBeanMergeData(name);
        if (beanMergeData == null) {
            beanMergeData = modData.createBeanMergeDataFromAnnotation(name, classInfo);
            beanMergeData.setTypeFromAnnotation(type, beanRuntime);
        } else {
            BeanInitData bid = beanMergeData.getBeanInitData();

            boolean validClass;
            if (bid.ivClassName == null) {
                beanMergeData.setClassNameFromAnnotation(className);
                validClass = true;
            } else if (!bid.ivClassName.equals(className)) {
                if (beanMergeData.isClassNameFromAnnotation()) {
                    Tr.error(tc, "INCOMPATIBLE_CLASS_ANN_ANN_CNTR4106E",
                             bid.ivName,
                             TYPE_TO_ANNOTATION_DISPLAY_NAMES[bid.ivType],
                             bid.ivClassName,
                             TYPE_TO_ANNOTATION_DISPLAY_NAMES[type],
                             className);
                } else {
                    Tr.error(tc, "INCOMPATIBLE_CLASS_XML_ANN_CNTR4114E",
                             bid.ivName,
                             bid.ivClassName,
                             TYPE_TO_ANNOTATION_DISPLAY_NAMES[type],
                             className);
                }
                modData.error();

                validClass = false;
            } else {
                validClass = true;
            }

            if (validClass) {
                if (bid.ivEnterpriseBean != null && kind != bid.ivEnterpriseBean.getKindValue()) {
                    Tr.error(tc, "INCOMPATIBLE_KIND_CNTR4103E",
                             bid.ivName,
                             KIND_ELEMENT_DISPLAY_NAMES[bid.ivEnterpriseBean.getKindValue()],
                             TYPE_TO_ANNOTATION_DISPLAY_NAMES[type],
                             classInfo.getName());
                    modData.error();
                } else {
                    mergeComponentDefiningAnnotationType(beanMergeData, type, beanRuntime, modData);
                }
            }
        }

        return beanMergeData;
    }

    private void mergeComponentDefiningAnnotationType(BeanMergeData beanMergeData, int type, BeanRuntime beanRuntime, ModuleMergeData modData) {
        BeanInitData bid = beanMergeData.getBeanInitData();
        if (bid.ivType == InternalConstants.TYPE_UNKNOWN) {
            beanMergeData.setTypeFromAnnotation(type, beanRuntime);
        } else if (type != bid.ivType) {
            if (beanMergeData.isTypeFromAnnotation()) {
                Tr.error(tc, "INCOMPATIBLE_ANN_TYPE_CNTR4104E",
                         bid.ivName,
                         TYPE_TO_ANNOTATION_DISPLAY_NAMES[bid.ivType],
                         TYPE_TO_ANNOTATION_DISPLAY_NAMES[type],
                         bid.ivClassName);
            } else {
                Tr.error(tc, "INCOMPATIBLE_SESSION_TYPE_CNTR4105E",
                         bid.ivName,
                         TYPE_TO_SESSION_TYPE_VALUES[bid.ivType],
                         TYPE_TO_ANNOTATION_DISPLAY_NAMES[type],
                         bid.ivClassName);
            }

            modData.error();
        }
    }

    private static String getStringValue(AnnotationInfo ann, String name) {
        AnnotationValue annValue = ann.getValue(name);
        if (annValue != null) {
            String value = annValue.getStringValue();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private String getComponentName(ClassInfo classInfo, AnnotationInfo compDefAnn) {
        String name = getManagedBeansName(classInfo);
        if (name == null) {
            name = getStringValue(compDefAnn, "name");
            if (name == null) {
                name = classInfo.getName();
                int index = Math.max(name.lastIndexOf('.'), name.lastIndexOf('$'));
                if (index != -1) {
                    name = name.substring(index + 1);
                }
            }
        }

        return name;
    }

    private String getManagedBeansName(ClassInfo classInfo) {
        AnnotationInfo ann = classInfo.getAnnotation(ManagedBean.class);
        return ann == null ? null : getStringValue(ann, "value");
    }

    /**
     * Return the ManagedBean name to be used internally by the EJBContainer.
     *
     * The internal ManagedBean name is the value on the annotation (which
     * must be unique even compared to EJBs in the module) or the class name
     * of the ManagedBean with a $ prefix. This is done for two reasons:
     *
     * 1 - when a name is not specified, our internal derived name cannot
     * conflict with other EJB names that happen to be the same.
     *
     * 2 - when a name is not specified, the managed bean is not supposed
     * to be bound into naming... the '$' will tip off the binding code.
     *
     * '$' is used for consistency with JDK synthesized names.
     */
    private String getManagedBeansInternalEJBName(ClassInfo classInfo, AnnotationInfo managedBeanAnn) {
        String name = getStringValue(managedBeanAnn, "value");
        if (name == null) {
            name = '$' + classInfo.getName();
        }
        return name;
    }

    private void mergeSessionInterfaces(BeanMergeData beanMergeData) {
        BeanInitData bid = beanMergeData.getBeanInitData();
        ClassInfo classInfo = beanMergeData.getClassInfo();

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "mergeSessionInterfaces", new Object[] { bid.ivName, bid.ivClassName });
        }

        // Implement the rules described in the EJB 3.1 specification:
        //   4.9.7 Session Bean's Business Interface
        //   4.9.8 Session Bean's No-Interface View

        // Look for @RemoteHome, unless <home/> was specified in XML.
        if (bid.ivRemoteHomeInterfaceName == null) {
            AnnotationInfo ann = classInfo.getAnnotation(RemoteHome.class);
            if (ann != null) {
                bid.ivRemoteHomeInterfaceName = ann.getClassNameValue("value");
            }
        }

        // Look for @LocalHome, unless <local-home/> was specified in XML.
        if (bid.ivLocalHomeInterfaceName == null) {
            AnnotationInfo ann = classInfo.getAnnotation(LocalHome.class);
            if (ann != null) {
                bid.ivLocalHomeInterfaceName = ann.getClassNameValue("value");
            }
        }

        // Look for @Remote/@Local on the bean class, and merge with the
        // interfaces found in ejb-jar.xml.

        // TRUE if empty @Remote is found, FALSE if empty @Local is found, or
        // null if neither empty @Remote nor empty @Local has been found.
        Boolean implementsAreRemote = null;

        for (Class<?> annClass : new Class<?>[] { Remote.class, Local.class }) {
            boolean remote = annClass == Remote.class;
            AnnotationInfo ann = classInfo.getAnnotation(annClass.asSubclass(Annotation.class));

            List<? extends AnnotationValue> interfaces = ann == null ? null : ann.getArrayValue("value");
            if (interfaces != null) {
                if (interfaces.isEmpty()) {
                    if (implementsAreRemote == null) {
                        implementsAreRemote = Boolean.valueOf(remote);
                    } else {
                        Tr.error(tc, "INCOMPATIBLE_DEFAULT_BUSINESS_INTERFACE_TYPE_CNTR4107E",
                                 classInfo.getName(),
                                 bid.ivName);
                        beanMergeData.getModuleMergeData().error();
                    }
                } else {
                    for (AnnotationValue interfaceValue : interfaces) {
                        String interfaceName = interfaceValue.getClassNameValue();
                        if (remote) {
                            beanMergeData.addRemoteBusinessInterfaceName(interfaceName);
                        } else {
                            beanMergeData.addLocalBusinessInterfaceName(interfaceName);
                        }
                    }
                }
            }
        }

        // Look for business interfaces on the implements clause.
        Set<String> eligibleInterfaceNames = getEligibleInterfaceNames(classInfo);

        if (implementsAreRemote != null && eligibleInterfaceNames.isEmpty()) {
            String annDisplayName = implementsAreRemote ? "@Remote" : "@Local";
            Tr.error(tc, "DEFAULT_IMPLEMENTS_NONE_CNTR4108E",
                     annDisplayName,
                     classInfo.getName(),
                     bid.ivName);
            beanMergeData.getModuleMergeData().error();
        }

        // "A bean class is permitted to have more than one interface. If a
        // bean class has more than one interface--excluding the interfaces
        // listed below--any business interface of the bean class must be
        // explicitly designated as a business interface of the bean by
        // means of the Local or Remote annotation on the bean class or
        // interface or in the deployment descriptor."
        for (ClassInfo interfaceInfo : classInfo.getInterfaces()) {
            String interfaceName = interfaceInfo.getName();
            if (eligibleInterfaceNames.contains(interfaceName)) {
                for (Class<?> annClass : new Class[] { Remote.class, Local.class }) {
                    boolean remote = annClass == Remote.class;
                    AnnotationInfo ann = interfaceInfo.getAnnotation(annClass.asSubclass(Annotation.class));

                    List<? extends AnnotationValue> interfaces = ann == null ? null : ann.getArrayValue("value");
                    if (interfaces != null) {
                        if (!interfaces.isEmpty()) {
                            Tr.error(tc, "IMPLEMENTS_INTERFACE_TYPE_VALUE_CNTR4111E",
                                     remote ? "@Remote" : "@Local",
                                     interfaceName,
                                     classInfo.getName(),
                                     bid.ivName);
                            beanMergeData.getModuleMergeData().error();
                        }

                        if (remote) {
                            beanMergeData.addRemoteBusinessInterfaceName(interfaceName);
                        } else {
                            beanMergeData.addLocalBusinessInterfaceName(interfaceName);
                        }
                    }
                }
            }
        }

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(tc, "checked interfaces", new Object[] { "interfaces=" + classInfo.getInterfaceNames(),
                                                             "eligibleInterfaces=" + eligibleInterfaceNames });
        }

        if (!bid.ivLocalBean) {
            bid.ivLocalBean = classInfo.isAnnotationPresent(LocalBean.class.getName());
        }

        // Determine if the EJB is a webservice endpoint as defined by JSR 224.
        bid.ivWebServiceEndpoint = classInfo.isAnnotationPresent("javax.jws.WebService") ||
                                   classInfo.isAnnotationPresent("javax.xml.ws.WebServiceProvider");

        if (beanMergeData.getRemoteBusinessInterfaceNames().isEmpty() &&
            beanMergeData.getLocalBusinessInterfaceNames().isEmpty() &&
            !bid.ivLocalBean) {
            if (eligibleInterfaceNames.isEmpty()) {
                if (bid.ivRemoteHomeInterfaceName == null &&
                    bid.ivLocalHomeInterfaceName == null &&
                    bid.ivWebServiceEndpointInterfaceName == null &&
                    !bid.ivWebServiceEndpoint) {
                    // "If the bean does not expose any other client views (Local,
                    // Remote, No-Interface, 2.x Remote Home, 2.x Local Home, Web
                    // Service) and its implements clause is empty, the bean
                    // defines a no-interface view."
                    bid.ivLocalBean = true;
                }
            } else {
                // "All business interfaces must be explicitly designated as
                // such if any of the following is true: [...] Otherwise:
                //   * If the bean class is annotated with the Remote
                //     annotation, all implemented interfaces (excluding the
                //     interfaces listed above) are assumed to be remote
                //     business interfaces of the bean.
                //   * If the bean class is annotated with the Local annotation,
                //     or if the bean class is annotated with neither the Local
                //     nor the Remote annotation, all implemented interfaces
                //     (excluding the interfaces listed above) are assumed to be
                //     local business interfaces of the bean."
                for (String interfaceName : eligibleInterfaceNames) {
                    if (implementsAreRemote != null && implementsAreRemote) {
                        beanMergeData.addRemoteBusinessInterfaceName(interfaceName);
                    } else {
                        beanMergeData.addLocalBusinessInterfaceName(interfaceName);
                    }
                }
            }
        } else if (isEmptyAnnotationIgnoresExplicitInterfaces()) {
            // For compatibility, if empty @Local or @Remote was specified and
            // there is exactly one interface on the implements clause, then add
            // it as a business interface even if business interfaces were
            // already specified in XML or if a no-interface view was specified
            // in XML or annotation.  However, for EJB 3.2 compatibility, we
            // only do so if this does not cause a conflict.
            if (implementsAreRemote != null && eligibleInterfaceNames.size() == 1) {
                String interfaceName = eligibleInterfaceNames.iterator().next();
                if (implementsAreRemote) {
                    if (!beanMergeData.getLocalBusinessInterfaceNames().contains(interfaceName)) {
                        beanMergeData.addRemoteBusinessInterfaceName(interfaceName);
                    }
                } else {
                    if (!beanMergeData.getRemoteBusinessInterfaceNames().contains(interfaceName)) {
                        beanMergeData.addLocalBusinessInterfaceName(interfaceName);
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "mergeSessionInterfaces", new Object[] { "remoteHome=" + bid.ivRemoteHomeInterfaceName,
                                                                "localHome=" + bid.ivLocalHomeInterfaceName,
                                                                "localBean=" + bid.ivLocalBean,
                                                                "remoteBusiness=" + beanMergeData.getRemoteBusinessInterfaceNames(),
                                                                "localBusiness=" + beanMergeData.getLocalBusinessInterfaceNames(),
                                                                "webServiceEndpoint=" + bid.ivWebServiceEndpoint,
                                                                "webServiceEndpointName=" + bid.ivWebServiceEndpointInterfaceName });
        }
    }

    boolean isEmptyAnnotationIgnoresExplicitInterfaces() {
        return runtimeVersion.compareTo(EJBRuntimeVersion.VERSION_3_2) < 0;
    }

    private Set<String> getEligibleInterfaceNames(ClassInfo classInfo) {
        Set<String> eligibleInterfaceNames = new LinkedHashSet<String>();
        for (String interfaceName : classInfo.getInterfaceNames()) {
            if (!isExcludedInterface(interfaceName)) {
                eligibleInterfaceNames.add(interfaceName);
            }
        }

        return eligibleInterfaceNames;
    }

    private boolean isExcludedInterface(String name) {
        // "The following interfaces are excluded when determining whether the
        // bean class has more than one interface: java.io.Serializable;
        // java.io.Externalizable; any of the interfaces defined by the
        // javax.ejb package."
        return name.equals(Serializable.class.getName()) ||
               name.equals(Externalizable.class.getName()) ||
               name.startsWith("javax.ejb.");
    }

    private void mergeTransactionManagement(BeanMergeData beanMergeData) {
        if (!beanMergeData.isSetBeanManagedTransaction()) {
            AnnotationInfo ann = beanMergeData.getClassInfo().getAnnotation(TransactionManagement.class);
            if (ann != null) {
                beanMergeData.setBeanManagedTransaction(TransactionManagementType.BEAN.name().equals(ann.getEnumValue("value")));
            }
        }
    }

    private void mergeSingleton(BeanMergeData beanMergeData) {
        BeanInitData bid = beanMergeData.getBeanInitData();

        if (!beanMergeData.isSetStartup() && beanMergeData.getClassInfo().isAnnotationPresent(Startup.class.getName())) {
            bid.ivStartup = true;
        }

        if (bid.ivDependsOn == null) {
            AnnotationInfo dependsOnAnn = beanMergeData.getClassInfo().getAnnotation(DependsOn.class);
            if (dependsOnAnn != null) {
                bid.ivDependsOn = new LinkedHashSet<String>();
                for (AnnotationValue dependsOnValue : dependsOnAnn.getArrayValue("value")) {
                    bid.ivDependsOn.add(dependsOnValue.getStringValue());
                }
            }
        }
    }

    private void mergeScheduleTimers(BeanMergeData beanMergeData) {
        BeanInitData bid = beanMergeData.getBeanInitData();
        if (bid.ivHasScheduleTimers == null || !bid.ivHasScheduleTimers) {
            boolean hasScheduleTimer = false;
            for (MethodInfo methodInfo : beanMergeData.getClassInfo().getMethods()) {
                if (methodInfo.isAnnotationPresent(Schedule.class.getName()) || methodInfo.isAnnotationPresent(Schedules.class.getName())) {
                    hasScheduleTimer = true;
                    break;
                }
            }

            bid.ivHasScheduleTimers = hasScheduleTimer;
        }
    }

    private void mergePassivationCapable(BeanMergeData beanMergeData, AnnotationInfo compDefAnn) {
        if (!beanMergeData.isSetPassivationCapable() && runtimeVersion.compareTo(EJBRuntimeVersion.VERSION_3_2) >= 0) {
            Boolean passivationCapable = compDefAnn.getBoolean("passivationCapable");
            if (passivationCapable != null) {
                beanMergeData.getBeanInitData().ivPassivationCapable = passivationCapable;
            }
        }
    }

    private void mergeMessagingInterface(BeanMergeData beanMergeData, AnnotationInfo compDefAnn) {
        BeanInitData bid = beanMergeData.getBeanInitData();
        if (bid.ivMessageListenerInterfaceName == null) {
            String className = compDefAnn.getClassNameValue("messageListenerInterface");
            if (!className.equals("java.lang.Object")) {
                bid.ivMessageListenerInterfaceName = className;
            }
        }
    }

    private void mergeMessagingInterface(BeanMergeData beanMergeData) {
        BeanInitData bid = beanMergeData.getBeanInitData();
        if (bid.ivMessageListenerInterfaceName == null) {
            // The spec does not have precise rules for determining the
            // messaging interface like it does for business interfaces for
            // session beans, but CTS at least expects interfaces on superclass
            // implements to be found (d659661.1).
            //
            // We choose to look for any class in the hierarchy that has
            // exactly one (eligible) interface on its implements clause.  This
            // is consistent with the EJB 3.0 feature pack, which only looked
            // on the implements clause of the bean class.
            for (ClassInfo superclassInfo = beanMergeData.getClassInfo(); superclassInfo != null && !superclassInfo.getName().equals(Object.class.getName()); superclassInfo = superclassInfo.getSuperclass()) {
                Set<String> eligibleInterfaceNames = getEligibleInterfaceNames(superclassInfo);
                if (!eligibleInterfaceNames.isEmpty()) {
                    if (eligibleInterfaceNames.size() == 1) {
                        bid.ivMessageListenerInterfaceName = eligibleInterfaceNames.iterator().next();
                    }

                    // Either we succeeded by finding one interface, or we
                    // failed by finding multiple.
                    break;
                }
            }
        }
    }

    private void mergeActivationConfigProperties(BeanMergeData beanMergeData, AnnotationInfo compDefAnn) {
        Properties properties = new Properties();
        for (AnnotationValue propertyValue : compDefAnn.getArrayValue("activationConfig")) {
            AnnotationInfo property = propertyValue.getAnnotationValue();
            String name = getStringValue(property, "propertyName");
            String value = getStringValue(property, "propertyValue");

            if (name != null && value != null) {
                properties.put(name, value);
            }
        }

        BeanInitData bid = beanMergeData.getBeanInitData();
        if (bid.ivActivationConfigProperties != null) {
            properties.putAll(bid.ivActivationConfigProperties);
        }

        bid.ivActivationConfigProperties = properties;
    }

    private <T extends com.ibm.ws.javaee.dd.ejbext.EnterpriseBean> Map<String, T> getExtensions(List<T> beans) {
        Map<String, T> beanMap = new HashMap<String, T>();
        for (T eb : beans) {
            beanMap.put(eb.getName(), eb);
        }

        return beanMap;
    }

    private <T extends com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean> Map<String, T> getBindings(List<T> beans) {
        Map<String, T> beanMap = new HashMap<String, T>();
        for (T eb : beans) {
            beanMap.put(eb.getName(), eb);
        }

        return beanMap;
    }

    private <T extends com.ibm.ws.javaee.dd.managedbean.ManagedBean> Map<String, T> getManagedBeanBindings(List<T> beans) {
        Map<String, T> beanMap = new HashMap<String, T>();
        for (T managedBean : beans) {
            beanMap.put(managedBean.getClazz(), managedBean);
        }

        return beanMap;
    }
}