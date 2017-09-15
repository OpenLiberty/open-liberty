/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.remote.server;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.rmi.CORBA.Tie;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManager;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantRetentionPolicyValue;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.clientcontainer.remote.common.ClientEJBFactory;
import com.ibm.ws.clientcontainer.remote.common.ClientSupport;
import com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService;
import com.ibm.ws.container.service.naming.NamingConstants;
import com.ibm.ws.container.service.naming.RemoteJavaColonNamingHelper;
import com.ibm.ws.container.service.naming.RemoteObjectInstance;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.transport.iiop.spi.ORBRef;
import com.ibm.ws.transport.iiop.spi.ServerPolicySource;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

/**
 *
 */
@Component(service = {}, configurationPolicy = ConfigurationPolicy.IGNORE)
public class ClientSupportImpl implements ClientSupport {

    private static final TraceComponent tc = Tr.register(ClientSupportImpl.class);
    private static final String CLIENT_SUPPORT_POA_NAME = SERVICE_NAME + "POA";
    private static final String REFERENCE_REMOTE_JAVA_COLON_NAMING_HELPERS = "remoteJavaColonNamingHelpers";
    private static final String REFERENCE_EJB_CLIENT_FACTORY = "clientEJBFactory";

    // used only for java:global lookups when no CMD is available -  to avoid the JNDI naked thread exception
    private static final ComponentMetaData DUMMY_CMD = new ComponentMetaData() {

        @Override
        public String getName() {
            return "ClientSupportImpl#DUMMY_CMD";
        }

        @Override
        public void setMetaData(MetaDataSlot slot, Object metadata) {}

        @Override
        public Object getMetaData(MetaDataSlot slot) {
            return null;
        }

        @Override
        public void release() {}

        @Override
        public ModuleMetaData getModuleMetaData() {
            return null;
        }

        @Override
        public J2EEName getJ2EEName() {
            return null;
        }

    };

    @Reference(target = "(id=defaultOrb)")
    private ORBRef orbRef;
    private ORB orb;
    private org.omg.CORBA.Object thisRef;
    @Reference
    private MetaDataIdentifierService metadataIdService;
    @Reference
    private SerializationService serializationService;

    private final AtomicServiceReference<ClientEJBFactory> ejbFactoryRef = new AtomicServiceReference<ClientEJBFactory>(REFERENCE_EJB_CLIENT_FACTORY);

    @Reference
    private ServerPolicySource serverPolicySource;

    private final ConcurrentServiceReferenceSet<RemoteJavaColonNamingHelper> remoteJavaColonNamingHelpers = new ConcurrentServiceReferenceSet<RemoteJavaColonNamingHelper>(REFERENCE_REMOTE_JAVA_COLON_NAMING_HELPERS);

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.clientcontainer.remote.common.ClientSupport#getRemoteObjectInstance(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public RemoteObjectInstance getRemoteObjectInstance(String appName, String moduleName, String compName, String namespaceString,
                                                        String jndiName) throws NamingException, RemoteException {
        NamingConstants.JavaColonNamespace namespace = NamingConstants.JavaColonNamespace.fromName(namespaceString);
        ComponentMetaData cmd = getCMD(appName, moduleName, compName, namespace);

        RemoteObjectInstance roi = null;
        try {
            ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().beginContext(cmd);

            Iterator<RemoteJavaColonNamingHelper> remoteJCNHelpers = remoteJavaColonNamingHelpers.getServices();
            while (remoteJCNHelpers.hasNext()) {
                RemoteJavaColonNamingHelper helper = remoteJCNHelpers.next();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getRemoteObjectInstance - checking " + helper);
                }
                roi = helper.getRemoteObjectInstance(namespace, jndiName);
                if (roi != null)
                    break;
            }
        } finally {
            ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
        }

        return roi;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.clientcontainer.remote.common.ClientSupport#hasRemoteObjectWithPrefix(java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String)
     */
    @Override
    public boolean hasRemoteObjectWithPrefix(String appName, String moduleName, String compName, String namespaceString, String name) throws NamingException {
        boolean b = false;
        NamingConstants.JavaColonNamespace namespace = NamingConstants.JavaColonNamespace.fromName(namespaceString);
        ComponentMetaData cmd = getCMD(appName, moduleName, compName, namespace);
        try {
            ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().beginContext(cmd);

            Iterator<RemoteJavaColonNamingHelper> remoteJCNHelpers = remoteJavaColonNamingHelpers.getServices();
            while (remoteJCNHelpers.hasNext()) {
                RemoteJavaColonNamingHelper helper = remoteJCNHelpers.next();
                b = helper.hasRemoteObjectWithPrefix(namespace, name);
                if (b == true)
                    break;
            }
        } finally {
            ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
        }
        return b;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.clientcontainer.remote.common.ClientSupport#listRemoteInstances(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Collection<? extends NameClassPair> listRemoteInstances(String appName, String moduleName, String compName, String namespaceString,
                                                                   String nameInContext) throws NamingException {
        Collection<NameClassPair> allInstances = new HashSet<NameClassPair>();
        NamingConstants.JavaColonNamespace namespace = NamingConstants.JavaColonNamespace.fromName(namespaceString);
        ComponentMetaData cmd = getCMD(appName, moduleName, compName, namespace);
        try {
            ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().beginContext(cmd);
            Iterator<RemoteJavaColonNamingHelper> remoteJCNHelpers = remoteJavaColonNamingHelpers.getServices();
            while (remoteJCNHelpers.hasNext()) {
                RemoteJavaColonNamingHelper helper = remoteJCNHelpers.next();
                allInstances.addAll(helper.listRemoteInstances(namespace, nameInContext));
            }
        } finally {
            ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
        }
        return allInstances;
    }

    @FFDCIgnore(IllegalStateException.class)
    //indicates that client component metadata was not found - ok in some cases.
    private ComponentMetaData getCMD(String appName, String moduleName, String compName, NamingConstants.JavaColonNamespace namespace) throws NamingException {
        String metadataId = metadataIdService.getMetaDataIdentifier("CLIENT", appName, moduleName, compName);
        ComponentMetaData cmd;
        try {
            cmd = (ComponentMetaData) metadataIdService.getMetaData(metadataId);
        } catch (IllegalStateException ex) {
            // This indicates that the client component metadata is not available on the server.
            // This can occur if the client module is not installed on the server - if this is the case,
            // then only java:global lookups would succeed (by using a dummy CMD).
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getCMD - no component metadata found for " + metadataId);
            }
            cmd = null;
        }

        if (cmd == null && NamingConstants.JavaColonNamespace.GLOBAL.equals(namespace)) {
            cmd = DUMMY_CMD;
        }
        if (cmd == null) {
            throw new NamingException("Unable to find ComponentMetaData for " + appName + "/" + moduleName + "/" + compName);
        }
        return cmd;
    }

    @Activate
    protected void activate(ComponentContext cc) throws Exception {
        remoteJavaColonNamingHelpers.activate(cc);
        ejbFactoryRef.activate(cc);

        this.orb = orbRef.getORB();

        if (this.orb == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The orb instance is null, possibly because of an earlier failure to bind a port.");
            }
            return;
        }

        Tie tie = getTie();
        POA myPoa = createMyPoa();
        Servant servant = (Servant) tie;
        myPoa.activate_object_with_id(SERVICE_NAME.getBytes(), servant);
        thisRef = myPoa.servant_to_reference(servant);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            String ref = orb.object_to_string(thisRef);
            Tr.debug(tc, "activate - created IOR: " + ref);
        }

        org.omg.CORBA.Object o = orb.resolve_initial_references("NameService");
        NamingContextExt rootContext = NamingContextExtHelper.narrow(o);
        rootContext.bind(new NameComponent[] { new NameComponent(SERVICE_NAME, "") }, thisRef);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        //TODO: destroy my poa so that it can be re-created next time this component is activated
        // does the following code suffice?
        //thisRef._release();

        ejbFactoryRef.deactivate(cc);
        remoteJavaColonNamingHelpers.deactivate(cc);
    }

    @Trivial
    @Reference(name = REFERENCE_REMOTE_JAVA_COLON_NAMING_HELPERS, cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC)
    protected void addRemoteJavaColonNamingHelper(ServiceReference<RemoteJavaColonNamingHelper> helperSR) {
        remoteJavaColonNamingHelpers.addReference(helperSR);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addRemoteJavaColonNamingHelper - added helper " + helperSR + " -- all helpers: " + remoteJavaColonNamingHelpers);
        }
    }

    protected void removeRemoteJavaColonNamingHelper(ServiceReference<RemoteJavaColonNamingHelper> helperSR) {
        remoteJavaColonNamingHelpers.removeReference(helperSR);
    }

    private Tie getTie() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        String packageName = this.getClass().getPackage().getName();
        String tieName = "_" + this.getClass().getSimpleName() + "_Tie";
        Class<?> tieClass = Class.forName(packageName + '.' + tieName);
        Tie tie = (Tie) tieClass.newInstance();
        tie.setTarget(this);
        return tie;
    }

    private POA createMyPoa() throws Exception {
        POA rootPoa = orbRef.getPOA();
        rootPoa.the_POAManager().activate();

        List<Policy> policies = new ArrayList<Policy>();
        try {
            serverPolicySource.addConfiguredPolicies(policies, orbRef);
        } catch (Exception e) { //TODO figure out what exceptions can occur
            throw new IllegalStateException(e);
        }
        policies.add(rootPoa.create_lifespan_policy(LifespanPolicyValue.TRANSIENT));
        policies.add(rootPoa.create_request_processing_policy(RequestProcessingPolicyValue.USE_ACTIVE_OBJECT_MAP_ONLY));
        policies.add(rootPoa.create_servant_retention_policy(ServantRetentionPolicyValue.RETAIN));
        policies.add(rootPoa.create_id_assignment_policy(IdAssignmentPolicyValue.USER_ID)); //not SYSTEM_ID. apparent default

        POAManager manager = rootPoa.the_POAManager();
        POA tPOA = rootPoa.create_POA(CLIENT_SUPPORT_POA_NAME, manager, policies.toArray(new Policy[policies.size()]));
        return tPOA;
    }

    @Reference(name = REFERENCE_EJB_CLIENT_FACTORY,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setClientEJBFactory(ServiceReference<ClientEJBFactory> reference) {
        ejbFactoryRef.setReference(reference);
    }

    protected void unsetClientEJBFactory(ServiceReference<ClientEJBFactory> reference) {
        ejbFactoryRef.unsetReference(reference);
    }

    @Override
    public Set<String> getEJBRmicCompatibleClasses(String appName) throws RemoteException {
        ClientEJBFactory factory = ejbFactoryRef.getService();
        if (factory != null) {
            return factory.getRmicCompatibleClasses(appName);
        }
        throw new RemoteException("ejbRemote feature is not enabled in server process.");
    }

    @Override
    public RemoteObjectInstance createEJB(String appName, String moduleName, String beanName, String beanInterface) throws NamingException, RemoteException {
        ClientEJBFactory factory = ejbFactoryRef.getService();
        if (factory != null) {
            return factory.create(appName, moduleName, beanName, beanInterface);
        }
        throw new RemoteException("ejbRemote feature is not enabled in server process.");
    }

    @Override
    public RemoteObjectInstance createEJB(String appName, String beanName, String beanInterface) throws NamingException, RemoteException {
        ClientEJBFactory factory = ejbFactoryRef.getService();
        if (factory != null) {
            return factory.create(appName, beanName, beanInterface);
        }
        throw new RemoteException("ejbRemote feature is not enabled in server process.");
    }

    @Override
    public RemoteObjectInstance findEJBByBeanName(String appName, String beanName, String beanInterface) throws NamingException, RemoteException {
        ClientEJBFactory factory = ejbFactoryRef.getService();
        if (factory != null) {
            return factory.findByBeanName(appName, beanName, beanInterface);
        }
        throw new RemoteException("ejbRemote feature is not enabled in server process.");
    }

    @Override
    public RemoteObjectInstance findEJBByInterface(String appName, String beanInterface) throws NamingException, RemoteException {
        ClientEJBFactory factory = ejbFactoryRef.getService();
        if (factory != null) {
            return factory.findByInterface(appName, beanInterface);
        }
        throw new RemoteException("ejbRemote feature is not enabled in server process.");
    }

}
