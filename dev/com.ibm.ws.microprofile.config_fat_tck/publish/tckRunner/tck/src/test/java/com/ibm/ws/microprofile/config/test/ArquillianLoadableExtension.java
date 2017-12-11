package com.ibm.ws.microprofile.config.test;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * We register an extension class with Arquillian that amends the vanilla tck's wars
 * to add any libraries that are not in the default Liberty environment
 */
public class ArquillianLoadableExtension implements LoadableExtension {
	@Override
	public void register(ExtensionBuilder extensionBuilder) {
		System.out.println("WLP: Adding Extension com.ibm.ws.microprofile.config.test.ArchiveProcessor");
		extensionBuilder.service(ApplicationArchiveProcessor.class, ArchiveProcessor.class);
	}
}