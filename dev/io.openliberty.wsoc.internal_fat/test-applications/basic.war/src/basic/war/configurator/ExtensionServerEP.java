/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war.configurator;

import java.util.List;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;

/**
 *
 */
public abstract class ExtensionServerEP extends Endpoint {

    public static class ConfiguredTextEndpoint extends ExtensionServerEP {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            List<Extension> extensionList = session.getNegotiatedExtensions();
            ServerEndpointConfig sec = (ServerEndpointConfig) ec;
            ServerConfigurator sc = (ServerConfigurator) sec.getConfigurator();
            try {
                session.getBasicRemote().sendText("INSTALLED" + sc.numExtensionsPassed);
                session.getBasicRemote().sendText("REQUESTED" + sc.numExtensionsRequested);
                session.getBasicRemote().sendText("NEGOTIATED" + extensionList.size());

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public static class TextEndpoint extends ExtensionServerEP {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            List<Extension> extensionList = session.getNegotiatedExtensions();
            try {
                session.getBasicRemote().sendText("NEGOTIATED" + extensionList.size());

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onClose(Session session, CloseReason reason) {

    }

    @Override
    public void onError(Session session, Throwable thr) {

    }

    public static class ServerConfigurator extends ServerEndpointConfig.Configurator {

        public int numExtensionsPassed = 0;
        public int numExtensionsRequested = 0;

        /*
         * Right now we have no extensions... so no need for tests to verify this function
         */

        @Override
        public List<Extension> getNegotiatedExtensions(List<Extension> installed,
                                                       List<Extension> requested) {
            numExtensionsPassed = installed.size();
            numExtensionsRequested = requested.size();
            return null;

        }
    }

}
