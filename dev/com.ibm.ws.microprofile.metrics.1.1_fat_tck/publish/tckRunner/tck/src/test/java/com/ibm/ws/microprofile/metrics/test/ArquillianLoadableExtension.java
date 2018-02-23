package com.ibm.ws.microprofile.metrics.test;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class ArquillianLoadableExtension implements LoadableExtension {
	@Override
	public void register(ExtensionBuilder extensionBuilder) {
		System.out.println("WLP: Adding Extension");
		extensionBuilder.service(ApplicationArchiveProcessor.class, ArchiveProcessor.class);
	}
}