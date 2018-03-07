package com.ibm.ws.microprofile.metrics.test;

import java.io.File;

//import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Adds the whole Config implementation classes and resources to the Arqillian
 * deployment archive. This is needed to have the container pick up the beans
 * from within the impl for the TCK tests.
 *
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class ArchiveProcessor implements ApplicationArchiveProcessor {

	@Override
	public void process(Archive<?> applicationArchive, TestClass testClass) {
		if (applicationArchive instanceof WebArchive) {
			File hamcrest = new File("../../../lib/hamcrest-all-1.3.jar");
			System.out.println("WLP: Adding Jar:" + hamcrest.getAbsolutePath() );
			((WebArchive) applicationArchive).addAsLibraries(hamcrest);
		}
	}
}