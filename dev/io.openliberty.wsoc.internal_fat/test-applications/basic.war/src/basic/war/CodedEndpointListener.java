/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013, 2019 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.websocket.DeploymentException;
import javax.websocket.Extension;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import basic.jar.JarAnnotatedEndpoint;
import basic.war.configurator.ConfiguratorEndpointConfig;
import basic.war.configurator.ExtensionServerEP;

public class CodedEndpointListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent event) {

    }

    @Override
    public void contextInitialized(ServletContextEvent event) {

        ServletContext servletContext = event.getServletContext();
        ServerContainer websocketServerContainer = (ServerContainer) servletContext.getAttribute("javax.websocket.server.ServerContainer");

        try {

            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.ByteArrayEndpointConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.ByteBufferEndpointConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.InputStreamEndpointConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.ReaderEndpointConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.TextEndpointConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.PartialTextEndpointConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.PartialTextEndpointConfig2());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.PartialTextSenderEndpointConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.PartialTextWithSendingEmbeddedPingEndpoint());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.AsyncTextEndpointConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.CodingEndpointConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.CloseEndpointConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.CloseEndpointOnOpenConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.OnErrorEndpointConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.MsgHandlerInheritanceConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.TextPathParamEndpointConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.ProgrammaticMaxMessageSizeConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig.TextQueryParmsEndpointConfig());

            // Configurator / Extension / Handshake Tests
            websocketServerContainer.addEndpoint(new ConfiguratorEndpointConfig.CodedModifyHandshakeEndpointConfig());
            websocketServerContainer.addEndpoint(createFakeExtensionEndpoint());

            websocketServerContainer.addEndpoint(JarAnnotatedEndpoint.class);

        } catch (DeploymentException e) {
            System.out.println("CodedEndpointListener: contextInitialized: caught DeploymentException of: " + e);
        }
    }

    private ServerEndpointConfig createFakeExtensionEndpoint() {

        // Add a method for using configurator/builder directory ... I don't think it is different at all, but doesn't hurt
        List<Extension> extensionList = new ArrayList<Extension>(1);
        extensionList.add(new Extension() {

            @Override
            public String getName() {
                return "TestExtension";
            }

            @Override
            public List<Parameter> getParameters() {
                return Collections.emptyList();
            }

        });
        return ServerEndpointConfig.Builder.create(ExtensionServerEP.ConfiguredTextEndpoint.class,
                                                   "/programmaticExtension").extensions(extensionList).configurator(new ExtensionServerEP.ServerConfigurator()).build();
    }
}
