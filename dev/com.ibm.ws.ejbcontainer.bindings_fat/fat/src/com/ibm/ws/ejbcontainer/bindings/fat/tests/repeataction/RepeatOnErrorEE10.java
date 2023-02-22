/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.fat.tests.repeataction;

import com.ibm.websphere.simplicity.config.EJBContainerElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class RepeatOnErrorEE10 extends JakartaEE10Action {

    public static final String ID_EJB_ON_ERROR = JakartaEE10Action.ID + "_EjbOnErr";
    public static final String ID_FAIL = ID_EJB_ON_ERROR + "_FAIL";
    public static final String ID_IGNORE = ID_EJB_ON_ERROR + "_IGNORE";
    public static final String ID_WARN = ID_EJB_ON_ERROR + "_WARN";

    private static final Class<?> c = RepeatOnErrorEE10.class;
    String serverName;

    public EjbOnError onErrorAction = EjbOnError.WARN;

    public RepeatOnErrorEE10(EjbOnError ejbOnError) {
        onErrorAction = ejbOnError;
    }

    public static boolean isActive() {
        return RepeatTestFilter.isRepeatActionActive(ID_EJB_ON_ERROR);
    }

    public static boolean isActive(EjbOnError onErrorAction) {
        switch (onErrorAction) {
            case FAIL:
                return RepeatTestFilter.isRepeatActionActive(ID_FAIL);
            case IGNORE:
                return RepeatTestFilter.isRepeatActionActive(ID_IGNORE);
            case WARN:
                return RepeatTestFilter.isRepeatActionActive(ID_WARN);
        }
        return false;
    }

    @Override
    public JakartaEE10Action forServers(String... serverNames) {
        if (serverNames == null || serverNames.length != 1) {
            throw new IllegalStateException("Only 1 server supported by " + getClass().getSimpleName() + " repeat action");
        }
        this.serverName = serverNames[0];
        return super.forServers(serverNames);
    }

    /**
     * Invoked by the FAT framework to perform setup steps before repeating the tests.
     */
    @Override
    public void setup() throws Exception {
        String m = "setup";

        super.setup();

        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);
        if (server != null) {
            Log.info(c, m, "adding customBindings.OnError =" + onErrorAction + "to server config");

            ServerConfiguration config = server.getServerConfiguration();
            EJBContainerElement ejbElement = config.getEJBContainer();
            ejbElement.setCustomBindingsOnError(onErrorAction.toString());

            server.updateServerConfiguration(config);

        } else {
            Log.info(c, m, "server is null, not adding customBindings.OnError element");
        }
    }

    /**
     * May be invoked by test to perform cleanup steps to avoid impacting later repeat actions.
     */
    public static void cleanup(LibertyServer server) throws Exception {
        String m = "cleanup";
        if (server != null) {
            Log.info(c, m, "removing customBindings.OnError from server config");

            ServerConfiguration config = server.getServerConfiguration();
            EJBContainerElement ejbElement = config.getEJBContainer();
            ejbElement.setCustomBindingsOnError(null);

            server.updateServerConfiguration(config);

        } else {
            Log.info(c, m, "server is null, not removing customBindings.OnError element");
        }
    }

    /**
     * Used to identify the RepeatTestAction and used in conjunction with @SkipForRepat
     */
    @Override
    public String getID() {
        switch (onErrorAction) {
            case FAIL:
                return ID_FAIL;
            case IGNORE:
                return ID_IGNORE;
            case WARN:
            default:
                return ID_WARN;
        }
    }

}
