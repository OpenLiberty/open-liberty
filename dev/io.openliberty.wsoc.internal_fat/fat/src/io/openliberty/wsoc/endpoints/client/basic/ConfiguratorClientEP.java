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
package io.openliberty.wsoc.endpoints.client.basic;

import java.util.List;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.Extension.Parameter;
import javax.websocket.Session;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;

/**
 *
 */
public class ConfiguratorClientEP extends Endpoint implements TestHelper {

    public WsocTestContext _wtr = null;

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.Endpoint#onOpen(javax.websocket.Session, javax.websocket.EndpointConfig)
     */
    @Override
    public void onOpen(Session sess, EndpointConfig arg1) {
        List<Extension> exts = sess.getNegotiatedExtensions();
        _wtr.addMessage("CLIENTNEGOTIATED" + exts.size());

        for (int x = 0; x < exts.size(); x++) {
            Extension e = exts.get(x);
            _wtr.addMessage(e.getName());
            if (e.getParameters() != null) {
                _wtr.addMessage("PARAMS" + e.getParameters().size());
            }
            else {
                _wtr.addMessage("NO PARAMETERS, SOMETHING WRONG");
            }

            List<Parameter> li = e.getParameters();
            for (int y = 0; y < li.size(); y++) {

                Parameter p = li.get(y);
                _wtr.addMessage(p.getName());
                _wtr.addMessage(p.getValue());

            }
            _wtr.terminateClient();
        }
        _wtr.terminateClient();

    }

    @Override
    public void addTestResponse(WsocTestContext wtr) {
        _wtr = wtr;
    }

    @Override
    public WsocTestContext getTestResponse() {
        return _wtr;
    }

}
