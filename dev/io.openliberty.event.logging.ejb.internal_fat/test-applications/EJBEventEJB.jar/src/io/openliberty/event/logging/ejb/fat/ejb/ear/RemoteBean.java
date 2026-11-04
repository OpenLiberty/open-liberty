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

import javax.ejb.LocalHome;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;

/**
 * Stateless session bean with an EJB 3.x remote business interface and EJB 2.x
 * remote and local home interfaces.
 *
 * <ul>
 * <li>{@link RemoteBeanRemote#doRemote()} — EJB 3.x remote; produces websphere.ejb.remote.</li>
 * <li>{@link RemoteBeanObject#doRemoteObject()} — EJB 2.x remote component object;
 * produces websphere.ejb.remote.</li>
 * <li>{@code home.create()} — NOT logged (home methods are not timed).</li>
 * <li>{@link RemoteBeanLocalObject#doLocalObject()} — NOT logged (local invocations
 * are not timed).</li>
 * <li>{@code localHome.create()} — NOT logged.</li>
 * </ul>
 *
 * Context info (EAR): EJBEventApp | EJBEventEJB | RemoteBean | &lt;method&gt;
 */
@Stateless
@RemoteHome(RemoteBeanHome.class)
@LocalHome(RemoteBeanLocalHome.class)
public class RemoteBean implements RemoteBeanRemote {

    private static final Logger log = Logger.getLogger(RemoteBean.class.getName());

    @Override
    public String doRemote() {
        log.info("RemoteBean.doRemote() invoked");
        return "remote";
    }

    /** Throws a checked application exception; still produces a websphere.ejb.remote event. */
    @Override
    public String doRemoteFail() throws EJBEventTestException {
        log.info("RemoteBean.doRemoteFail() invoked");
        throw new EJBEventTestException("expected failure from RemoteBean.doRemoteFail");
    }

    /** Throws an unchecked exception; still produces a websphere.ejb.remote event. */
    @Override
    public String doRemoteSystemFail() {
        log.info("RemoteBean.doRemoteSystemFail() invoked");
        throw new IllegalStateException("expected failure from RemoteBean.doRemoteSystemFail");
    }

    /**
     * Business method exposed via the EJB 2.x remote component interface.
     * Called as {@code home.create().doRemoteObject()}.
     */
    public String doRemoteObject() {
        log.info("RemoteBean.doRemoteObject() invoked");
        return "remoteObject";
    }

    /**
     * Business method exposed via the EJB 2.x local component interface.
     * Called as {@code localHome.create().doLocalObject()}.
     */
    public String doLocalObject() {
        log.info("RemoteBean.doLocalObject() invoked");
        return "localHome";
    }
}
