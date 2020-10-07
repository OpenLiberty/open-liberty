/*******************************************************************************n * Copyright (c) 2020 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License v1.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-v10.htmln *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.fat.tests.repeataction;

import com.ibm.websphere.simplicity.config.EJBContainerElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.rules.repeater.RepeatTestAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class RepeatOnError implements RepeatTestAction {
    private static final Class<?> c = RepeatOnError.class;
    String serverName;

    public enum EJBONERROR {
        WARN, FAIL, IGNORE
    };

    public EJBONERROR onErrorAction = EJBONERROR.WARN;

    public RepeatOnError(EJBONERROR ejbOnError, String serverName) {
        onErrorAction = ejbOnError;
        this.serverName = serverName;
    }

    /**
     * Invoked by the FAT framework to test if the action should be applied or not.
     * If a RepeatTestAction is disabled, it ought to log a message indicating why.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Invoked by the FAT framework to perform setup steps before repeating the tests.
     */
    @Override
    public void setup() throws Exception {
        String m = "setup";
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
     * Used to identify the RepeatTestAction and used in conjunction with @SkipForRepat
     */
    @Override
    public String getID() {
        switch (onErrorAction) {
            case FAIL:
                return "EJBCBOnErr_FAIL";
            case IGNORE:
                return "EJBCBOnErr_IGNORE";
            case WARN:
            default:
                return "EJBCBOnErr_WARN";
        }
    }

}
