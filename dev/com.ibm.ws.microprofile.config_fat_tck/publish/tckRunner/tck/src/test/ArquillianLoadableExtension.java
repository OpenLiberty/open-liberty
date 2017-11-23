package com.ibm.ws.microprofile.config.test.tck;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * We can register a class that can amend the vanilla wars that the TCK
 * tests create.
 */
public class ArquillianLoadableExtension implements LoadableExtension {
    @Override
    public void register(ExtensionBuilder extensionBuilder) {
        System.out.println("WLP: Adding Extension");
        extensionBuilder.service(ApplicationArchiveProcessor.class, ArchiveProcessor.class);
    }
}