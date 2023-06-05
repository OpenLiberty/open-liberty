/*******************************************************************************
 * Copyright (c) 2015-2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.iiop.server;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;

import java.rmi.Remote;

import javax.rmi.CORBA.Tie;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManager;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantRetentionPolicyValue;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.transport.iiop.RemoteClientUtil;
import com.ibm.ws.transport.iiop.spi.ORBRef;

/**
 * Extend this class to create a self-publishing singleton object to be invoked over IIOP.
 * It will write out its IOR to the log. Use {@link RemoteClientUtil} from test code to retrieve
 * a service from a server log.
 *
 * @param <I> the extending class e.g. <code> class MyTestImpl extends TestSingleton&lt;MyTestImpl&gt;{} </code>
 */
public abstract class AbstractRemoteService<I extends AbstractRemoteService<I> & Remote> {

    private volatile ORBRef orbRef;
    private ORB orb;
    private final Class<?> serviceInterface = this.getClass().getInterfaces()[0];
    private final String serviceName = serviceInterface.getSimpleName();
    private org.omg.CORBA.Object thisRef;

    @Reference(cardinality = MANDATORY, // ensure this service has an ORBRef before activation
               policy = STATIC,         // do not change the ORBRef while this service is active
               unbind = "unsetORBRef")
    protected final void setORBRef(ORBRef orbRef) {
        this.orbRef = orbRef;
    }

    protected final void unsetORBRef(ORBRef orbRef) {
        if (this.orbRef == orbRef)
            this.orbRef = null;
    }

    ORB getOrb() {
        return orb;
    }

    @Activate
    protected final void activate() throws Exception {
        System.out.println("### Activating " + serviceName);
        this.orb = orbRef.getORB();
        Tie tie = getTie();
        POA myPoa = createMyPoa();
        Servant servant = (Servant) tie;
        myPoa.activate_object_with_id(serviceName.getBytes(), servant);
        thisRef = myPoa.servant_to_reference(servant);
        String ref = orb.object_to_string(thisRef);
        System.out.println("### Exported " + serviceName + ": " + ref);
    }

    @SuppressWarnings("unchecked")
    private Tie getTie() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        //TODO fix to use Util.getTie(this)
        String packageName = this.getClass().getPackage().getName();
        String tieName = "_" + this.getClass().getSimpleName() + "_Tie";
        Class<?> tieClass = Class.forName(packageName + '.' + tieName);
        Tie tie = (Tie) tieClass.newInstance();
        tie.setTarget((I) this);
        return tie;
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
        POA tPOA = rootPoa.create_POA(serviceName + "POA", manager, myPolicy); //leaving out the manager causes a new manager with all new random ports to be created.
        return tPOA;
    }

    org.omg.CORBA.Object getThisRef() {
        return thisRef;
    }
}
