/*
 * Copyright (c) 2015,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package test.iiop.server;

import static java.util.Objects.requireNonNull;

import javax.rmi.PortableRemoteObject;

import org.apache.yoko.orb.spi.naming.Resolver;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.Policy;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManager;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.ServantRetentionPolicyValue;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.transport.iiop.spi.ORBRef;

import test.iiop.common.CosNamingChecker;
import test.iiop.common.NamingUtil;

@Component(immediate = true)
public class CosNamingCheckerImpl implements CosNamingChecker {
    private final ORB orb;
    private org.omg.CORBA.Object thisRef;

    @Activate
    public CosNamingCheckerImpl(@Reference ORBRef orbRef) throws Exception {
        this.orb = orbRef.getORB();
        // export this object via IIOP
        _CosNamingCheckerImpl_Tie tie = new _CosNamingCheckerImpl_Tie();
        tie.setTarget(this);

        POA myPoa = createMyPoa();
        myPoa.activate_object_with_id(CosNamingChecker.class.getSimpleName().getBytes(), tie);
        thisRef = myPoa.servant_to_reference(tie);

        // bind this object into the name service
        NamingContext ctx = NamingContextHelper.narrow(orb.resolve_initial_references("NameService"));
        ctx.rebind(NamingUtil.CNC_NAME, thisRef);
        System.out.println(NamingUtil.CNC_LOG_MSG);
    }

    private POA createMyPoa() throws Exception {
        POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
        rootPoa.the_POAManager().activate();
        Policy[] myPolicy = new Policy[]
        {
         rootPoa.create_lifespan_policy(LifespanPolicyValue.TRANSIENT),
         rootPoa.create_request_processing_policy(RequestProcessingPolicyValue.USE_ACTIVE_OBJECT_MAP_ONLY),
         rootPoa.create_servant_retention_policy(ServantRetentionPolicyValue.RETAIN),
         rootPoa.create_id_assignment_policy(IdAssignmentPolicyValue.USER_ID) //not SYSTEM_ID. apparent default
        };
        POAManager manager = rootPoa.the_POAManager();
        POA tPOA = rootPoa.create_POA(CosNamingChecker.class.getSimpleName() + "POA", manager, myPolicy); //leaving out the manager causes a new manager with all new random ports to be created.
        return tPOA;
    }

    @Override
    public void checkNameServiceIsAvailable() throws Exception {
        debugClass(org.omg.CORBA.Object.class);
        org.omg.CORBA.Object o = requireNonNull(orb.resolve_initial_references("NameService"));
        debugClass(NamingContext.class);
        debugClass(NamingContextHelper.class);
        requireNonNull(NamingContextHelper.narrow(o));
    }

    @Override
    public String getNameServiceListingFromServer() throws Exception {
        org.omg.CORBA.Object o = requireNonNull(orb.resolve_initial_references("NameService"));
        NamingContext ctx = requireNonNull(NamingContextHelper.narrow(o));
        ctx.rebind(NamingUtil.CNC_NAME, thisRef);
        NamingContext nested = requireNonNull(ctx.new_context());
        nested.bind(NamingUtil.CNC_NAME, thisRef);
        ctx.rebind_context(NamingUtil.NESTED_CTX_NAME, nested);
        ctx.rebind(NamingUtil.NESTED_CNC_NAME, thisRef);
        return NamingUtil.getNameServiceListing(ctx);
    }

    @Override
    public void bindResolvable(NameComponent[] name) throws Exception {
        org.omg.CORBA.Object o = requireNonNull(orb.resolve_initial_references("NameService"));
        NamingContext ctx = requireNonNull(NamingContextHelper.narrow(o));
        ctx.rebind(name, new Resolver() {
            @Override
            public Object resolve() {
                return thisRef;
            }
        });
        Object obj = requireNonNull(ctx.resolve(NamingUtil.CNC_RESOLVABLE_NAME));
        requireNonNull(PortableRemoteObject.narrow(obj, CosNamingChecker.class));
    }

    @Override
    public void bindResolvableThatThrows(final RuntimeException exceptionToThrow, NameComponent[] name) throws Exception {
        org.omg.CORBA.Object o = requireNonNull(orb.resolve_initial_references("NameService"));
        NamingContext ctx = requireNonNull(NamingContextHelper.narrow(o));
        ctx.rebind(name, new Resolver() {
            @Override
            public Object resolve() {
                exceptionToThrow.fillInStackTrace();
                throw exceptionToThrow;
            }
        });
        Object obj = requireNonNull(ctx.resolve(NamingUtil.CNC_RESOLVABLE_NAME));
        requireNonNull(PortableRemoteObject.narrow(obj, CosNamingChecker.class));
    }

    private static void debugClass(Class<?> clazz) {
        System.out.println("### " + clazz + " was loaded by " + clazz.getClassLoader());
    }
}
