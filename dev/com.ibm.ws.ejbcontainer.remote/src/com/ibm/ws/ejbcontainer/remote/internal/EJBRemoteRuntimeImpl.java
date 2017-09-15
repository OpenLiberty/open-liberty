/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.rmi.PortableRemoteObject;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.BindingIteratorHolder;
import org.omg.CosNaming.BindingListHolder;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.CosNaming.NamingContextPackage.AlreadyBound;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantRetentionPolicyValue;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.AdapterAlreadyExists;
import org.omg.PortableServer.POAPackage.InvalidPolicy;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSRemoteWrapper;
import com.ibm.ejs.container.EJSWrapper;
import com.ibm.ejs.container.RemoteAsyncResult;
import com.ibm.ejs.container.WrapperManager;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils;
import com.ibm.ws.ejbcontainer.osgi.EJBRemoteRuntime;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.transport.iiop.spi.ORBRef;
import com.ibm.ws.transport.iiop.spi.RemoteObjectReplacer;
import com.ibm.ws.transport.iiop.spi.ServerPolicySource;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component
public class EJBRemoteRuntimeImpl implements EJBRemoteRuntime, RemoteObjectReplacer {
    /**
     * The primary POA name. This name must not be changed or else existing
     * serialized stubs will fail.
     */
    private static final String POA_NAME = "EJB";

    /**
     * The POA name used for POA results. Activated objects in this POA are
     * only valid for the lifetime of the server.
     */
    private static final String ASYNC_RESULT_POA_NAME = "AsyncResult";

    /**
     * The context name beneath the root context that is used to hold all EJBs.
     */
    private static final String EJB_NAMING_CONTEXT_NAME = "ejb";

    /**
     * The context name beneath {@link #EJB_NAMING_CONTEXT_NAME} that is used to
     * hold EJBs from deployed applications.
     */
    private static final String EJB_GLOBAL_NAMING_CONTEXT_NAME = "global";

    private static final String REFERENCE_ORB = "orbRef";
    private static final String REFERENCE_POLICIES = "serverPolicySourceRef";

    private static final String ASYNC_RESULT_TYPE_ID = CORBA_Utils.getRemoteTypeId(RemoteAsyncResult.class);

    private final AtomicServiceReference<ORBRef> orbRefSR = new AtomicServiceReference<ORBRef>(REFERENCE_ORB);
    private final AtomicServiceReference<ServerPolicySource> serverPolicySource = new AtomicServiceReference<ServerPolicySource>(REFERENCE_POLICIES);

    private POA ejbAdapter;
    private POA asyncResultAdapter;
    private NamingContext rootNamingContext;

    @Reference(service = LibertyProcess.class, target = "(wlp.process.type=server)")
    protected void setLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    @Reference(name = REFERENCE_ORB, service = ORBRef.class, target = "(id=defaultOrb)")
    protected void setOrbRef(ServiceReference<ORBRef> ref) {
        orbRefSR.setReference(ref);
    }

    @Reference(name = REFERENCE_POLICIES, service = ServerPolicySource.class)
    protected void setServerPolicySource(ServiceReference<ServerPolicySource> ref) {
        serverPolicySource.setReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        orbRefSR.activate(cc);
        serverPolicySource.activate(cc);
    }

    @Deactivate
    protected synchronized void deactivate(ComponentContext cc) {
        if (ejbAdapter != null) {
            ejbAdapter.destroy(false, false);
        }

        orbRefSR.deactivate(cc);
        serverPolicySource.deactivate(cc);
    }

    private static class BindingData {
        final BeanMetaData beanMetaData;
        final String[] contextNames;
        final NamingContext[] contexts;
        final List<String> bindingNames = new ArrayList<String>();

        BindingData(BeanMetaData bmd, String[] contextNames) {
            this.beanMetaData = bmd;
            this.contextNames = contextNames;
            this.contexts = new NamingContext[contextNames.length];
        }

        @Override
        public String toString() {
            return super.toString() + '[' + beanMetaData.j2eeName +
                   ", contexts=" + Arrays.asList(contextNames) +
                   ']';
        }
    }

    @Override
    public Object createBindingData(BeanMetaData bmd, String appLogicalName, String moduleLogicalName) {
        String[] contextNames;
        if (appLogicalName == null) {
            contextNames = new String[] { EJB_NAMING_CONTEXT_NAME, EJB_GLOBAL_NAMING_CONTEXT_NAME, moduleLogicalName };
        } else {
            contextNames = new String[] { EJB_NAMING_CONTEXT_NAME, EJB_GLOBAL_NAMING_CONTEXT_NAME, appLogicalName, moduleLogicalName };
        }

        return new BindingData(bmd, contextNames);
    }

    @Override
    public void bind(Object bindingDataObject, int interfaceIndex, String interfaceName) {
        BindingData bindingData = (BindingData) bindingDataObject;
        String bindingName = bindingData.beanMetaData.enterpriseBeanName + '!' + interfaceName;
        bind(bindingData, interfaceIndex, interfaceName, bindingName);
    }

    @Override
    public Object bindSystem(BeanMetaData bmd, String systemBinding) {
        String[] systemBindingPieces = systemBinding.split("/");
        String[] contextNames = Arrays.copyOf(systemBindingPieces, systemBindingPieces.length - 1);
        String bindingName = systemBindingPieces[systemBindingPieces.length - 1];

        BindingData bindingData = new BindingData(bmd, contextNames);
        bind(bindingData, -1, bmd.homeInterfaceClassName, bindingName);
        return bindingData;
    }

    private synchronized void bind(BindingData bindingData, int interfaceIndex, String interfaceName, String bindingName) {
        BeanMetaData bmd = bindingData.beanMetaData;
        POA adapter = getEjbAdapter();

        NamingContext context = bindingData.contexts[bindingData.contexts.length - 1];
        if (context == null) {
            context = rootNamingContext;
            for (int i = 0; i < bindingData.contextNames.length; i++) {
                context = bindContext(context, bindingData.contextNames[i]);
                bindingData.contexts[i] = context;
            }
        }

        org.omg.CORBA.Object object = EJBServantLocatorImpl.createBindingReference(bmd, interfaceIndex, interfaceName, adapter);
        bindObject(context, bindingName, object);
        bindingData.bindingNames.add(bindingName);
    }

    @FFDCIgnore(AlreadyBound.class)
    private NamingContext bindContext(NamingContext context, String name) {
        NameComponent[] nameComps = { new NameComponent(name, "") };
        try {
            try {
                return context.bind_new_context(nameComps);
            } catch (AlreadyBound e) {
                return NamingContextHelper.narrow(context.resolve(nameComps));
            }
        } catch (org.omg.CosNaming.NamingContextPackage.InvalidName e) {
            // We should not be passing invalid names.
            throw new IllegalStateException(e);
        } catch (NotFound e) {
            // We're only passing one name, so this should not happen.
            throw new IllegalStateException(e);
        } catch (CannotProceed e) {
            // Our name service should be local-only, so this should not happen.
            throw new IllegalStateException(e);
        }
    }

    private void bindObject(NamingContext context, String name, org.omg.CORBA.Object object) {
        NameComponent[] nameComps = { new NameComponent(name, "") };
        try {
            context.bind(nameComps, object);
        } catch (org.omg.CosNaming.NamingContextPackage.InvalidName e) {
            // We should not be passing invalid names.
            throw new IllegalStateException(e);
        } catch (NotFound e) {
            // We're only passing one name, so this should not happen.
            throw new IllegalStateException(e);
        } catch (AlreadyBound e) {
            // This thread is starting the application, so this should not happen.
            throw new IllegalStateException(e);
        } catch (CannotProceed e) {
            // Our name service should be local-only, so this should not happen.
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized void unbindAll(Object bindingDataObject) {
        BindingData bindingData = (BindingData) bindingDataObject;

        NamingContext context = bindingData.contexts[bindingData.contexts.length - 1];
        if (context != null) {
            for (String name : bindingData.bindingNames) {
                unbind(context, name);
            }
        }

        for (int i = bindingData.contextNames.length - 1; i >= 0 && !hasBindings(bindingData.contexts[i]); i--) {
            NamingContext parentContext = i == 0 ? rootNamingContext : bindingData.contexts[i - 1];
            unbind(parentContext, bindingData.contextNames[i]);
        }
    }

    private boolean hasBindings(NamingContext context) {
        BindingListHolder blh = new BindingListHolder();
        BindingIteratorHolder bih = new BindingIteratorHolder();
        context.list(1, blh, bih);
        return blh.value.length != 0;
    }

    private void unbind(NamingContext context, String name) {
        NameComponent[] nameComps = { new NameComponent(name, "") };
        try {
            context.unbind(nameComps);
        } catch (org.omg.CosNaming.NamingContextPackage.InvalidName e) {
            // We should not be passing invalid names.
        } catch (NotFound e) {
            // We're only passing one name, so this should not happen.
        } catch (CannotProceed e) {
            // Our name service should be local-only, so this should not happen.
        }
    }

    private synchronized POA getAsyncResultAdapter() {
        if (asyncResultAdapter == null) {
            getEjbAdapter();
        }
        return asyncResultAdapter;
    }

    private synchronized POA getEjbAdapter() {
        if (ejbAdapter == null) {
            ORBRef orbRef = orbRefSR.getServiceWithException();
            ORB orb = orbRef.getORB();
            if (orb == null) {
                throw new IllegalStateException("The orb is not available");
            }

            try {
                rootNamingContext = NamingContextHelper.narrow(orb.resolve_initial_references("NameService"));
            } catch (InvalidName e) {
                throw new IllegalStateException(e);
            }

            POA poa = orbRef.getPOA();
            createPOA(poa);
        }
        return ejbAdapter;
    }

    private void createPOA(POA rootAdapter) {

        EJSContainer container = EJSContainer.getDefaultContainer();
        if (container == null) {
            throw new IllegalStateException("EJBContainer not available");
        }
        WrapperManager wrapperManager = container.getWrapperManager();

        List<Policy> policies = new ArrayList<Policy>();
        try {
            serverPolicySource.getService().addConfiguredPolicies(policies, orbRefSR.getServiceWithException());
        } catch (Exception e) { //TODO figure out what exceptions can occur
            throw new IllegalStateException(e);
        }

        // We need to act on preinvoke/postinvoke.
        policies.add(rootAdapter.create_servant_retention_policy(ServantRetentionPolicyValue.NON_RETAIN));
        policies.add(rootAdapter.create_request_processing_policy(RequestProcessingPolicyValue.USE_SERVANT_MANAGER));
        // An EJB can exist beyond the lifespan of the container.
        policies.add(rootAdapter.create_lifespan_policy(LifespanPolicyValue.PERSISTENT));
        // The container is responsible for assigning all IDs.
        policies.add(rootAdapter.create_id_assignment_policy(IdAssignmentPolicyValue.USER_ID));

        POA adapter;
        try {
            adapter = rootAdapter.create_POA(POA_NAME, rootAdapter.the_POAManager(), policies.toArray(new Policy[policies.size()]));
        } catch (AdapterAlreadyExists e) {
            throw new IllegalStateException(e);
        } catch (InvalidPolicy e) {
            throw new IllegalStateException(e);
        }

        POA asyncResultAdapter;

        boolean success = false;
        try {
            activatePOA(adapter, wrapperManager);
            asyncResultAdapter = createRemoteAsyncPOA(adapter);
            success = true;
        } finally {
            if (!success) {
                adapter.destroy(false, false);
                adapter = null;
                asyncResultAdapter = null;
            }
        }

        this.ejbAdapter = adapter;
        this.asyncResultAdapter = asyncResultAdapter;
    }

    private void activatePOA(POA adapter, WrapperManager wrapperManager) {
        try {
            adapter.set_servant_manager(new EJBServantLocatorImpl(wrapperManager));
        } catch (WrongPolicy e) {
            throw new IllegalStateException(e);
        }

        try {
            adapter.the_POAManager().activate();
        } catch (AdapterInactive e) {
            throw new IllegalStateException(e);
        }
    }

    private POA createRemoteAsyncPOA(POA adapter) {
        List<Policy> policies = new ArrayList<Policy>();
        try {
            serverPolicySource.getService().addConfiguredPolicies(policies, orbRefSR.getServiceWithException());
        } catch (Exception e) { //TODO figure out what exceptions can occur
            throw new IllegalStateException(e);
        }
        POA asyncResultAdapter;
        try {
            asyncResultAdapter = adapter.create_POA(ASYNC_RESULT_POA_NAME, adapter.the_POAManager(), policies.toArray(new Policy[policies.size()]));
        } catch (AdapterAlreadyExists e) {
            throw new IllegalStateException(e);
        } catch (InvalidPolicy e) {
            throw new IllegalStateException(e);
        }

        try {
            asyncResultAdapter.the_POAManager().activate();
        } catch (AdapterInactive e) {
            throw new IllegalStateException(e);
        }

        return asyncResultAdapter;
    }

    @Override
    public Object getReference(EJSRemoteWrapper remoteObject) {
        return EJBServantLocatorImpl.getReference(remoteObject, getEjbAdapter());
    }

    @Override
    public byte[] activateAsyncResult(Servant servant) {
        try {
            return getAsyncResultAdapter().activate_object(servant);
        } catch (ServantAlreadyActive e) {
            throw new IllegalStateException(e);
        } catch (WrongPolicy e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public RemoteAsyncResult getAsyncResultReference(byte[] oid) {
        org.omg.CORBA.Object ref = getAsyncResultAdapter().create_reference_with_id(oid, ASYNC_RESULT_TYPE_ID);
        return (RemoteAsyncResult) PortableRemoteObject.narrow(ref, RemoteAsyncResult.class);
    }

    @Override
    public void deactivateAsyncResult(byte[] oid) {
        try {
            getAsyncResultAdapter().deactivate_object(oid);
        } catch (ObjectNotActive e) {
            throw new IllegalStateException(e);
        } catch (WrongPolicy e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    @Sensitive
    public Object replaceRemoteObject(Object obj) {
        // Generated EJBHome.create() returns EJSHome.createWrapper, which must
        // return an EJSWrapper per its method signature for ABI compatibility,
        // but we actually want to return a POA reference or else the tie's call
        // to Util.writeRemoteObject will automatically export the object via
        // RMI, which doesn't let us control the generated ID.  (On tWAS,
        // EJSWrapperCommon.registerServant exports the object with a specific
        // key, and we just hope it doesn't get unexported before it can be
        // returned back to the ORB.)
        //
        // Alternatively, we could change the generated homes to use a different
        // EJSHome.createWrapperReference method that returns the remote
        // reference as an Object, but it's less confusing if the generated
        // homes are identical to tWAS.
        if (obj instanceof EJSWrapper) {
            return EJBServantLocatorImpl.getReference((EJSRemoteWrapper) obj, getEjbAdapter());
        }
        return null;
    }
}
