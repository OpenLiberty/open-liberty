/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.ejb.ear;

import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.Init;
import javax.ejb.LocalHome;
import javax.ejb.RemoteHome;
import javax.ejb.Remove;
import javax.ejb.Stateful;

/**
 * Stateful session bean used to verify that remote home create() methods are
 * NOT timed by event logging, even for stateful beans.
 *
 * <ul>
 * <li>{@link StatefulRemoteBeanRemote#doRemote()} — EJB 3.x remote business
 * interface; produces a websphere.ejb.remote event.</li>
 * <li>{@link StatefulRemoteBeanObject#doRemoteObject()} — EJB 2.x remote component
 * object method; produces a websphere.ejb.remote event.</li>
 * <li>{@link StatefulRemoteBeanRemote#doRemove()} — EJB 3.x remote business
 * {@code @Remove} method; produces a websphere.ejb.remote event.</li>
 * <li>{@code remoteHome.create()} — NOT logged (home methods are not timed).</li>
 * <li>{@code remoteObject.remove()} — EJB 2.x {@code EJBObject.remove()}; produces a
 * websphere.ejb.remote event named {@code remove}. {@code remoteHome.remove(Handle)} is
 * itself a home method and is NOT logged, but it calls {@code EJBObject.remove()}, so it
 * results in the same single {@code remove} event.</li>
 * <li>{@code getHandle()} — NOT logged (container method; no bean method is invoked).</li>
 * <li>{@link StatefulRemoteBeanLocalObject#doLocalObject()} — NOT logged
 * (local invocations are not timed).</li>
 * <li>{@code localHome.create()} — NOT logged.</li>
 * </ul>
 *
 * Context info (EAR): EJBEventApp | EJBEventEJB | StatefulRemoteBean | &lt;method&gt;
 */
@Stateful
@RemoteHome(StatefulRemoteBeanHome.class)
@LocalHome(StatefulRemoteBeanLocalHome.class)
public class StatefulRemoteBean implements StatefulRemoteBeanRemote {

    private static final Logger log = Logger.getLogger(StatefulRemoteBean.class.getName());

    @Override
    public String doRemote() {
        log.info("StatefulRemoteBean.doRemote() invoked");
        return "statefulRemote";
    }

    /**
     * EJB 3.x {@code @Remove} method exposed via the remote business interface.
     */
    @Override
    @Remove
    public void doRemove() {
        log.info("StatefulRemoteBean.doRemove() invoked");
    }

    /**
     * Business method exposed via the EJB 2.x remote component interface.
     * Called as {@code remoteHome.create().doRemoteObject()}.
     */
    public String doRemoteObject() {
        log.info("StatefulRemoteBean.doRemoteObject() invoked");
        return "statefulRemoteObject";
    }

    /**
     * Business method exposed via the EJB 2.x local component interface.
     * NOT logged — local invocations are never timed.
     */
    public String doLocalObject() {
        log.info("StatefulRemoteBean.doLocalObject() invoked");
        return "statefulLocalObject";
    }

    /**
     * Required by EJB 3.x spec (section 4.6.8): a stateful bean with a
     * legacy home interface must provide an @Init-annotated method to map
     * the home's create() to this initialisation method.
     */
    @Init
    public void ejbCreate() throws CreateException {
    }
}
