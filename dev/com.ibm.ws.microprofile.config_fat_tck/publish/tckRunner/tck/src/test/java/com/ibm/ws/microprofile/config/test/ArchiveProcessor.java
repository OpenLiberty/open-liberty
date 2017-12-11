package com.ibm.ws.microprofile.config.test;

import java.io.File;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * We weave in the hamcrest jar that is used by some of the microprofile config tck tests.
 * The build.gradle file pull the hamcrest jar from maven and puts it in the lib directory
 */
public class ArchiveProcessor implements ApplicationArchiveProcessor {

	/* (non-Javadoc)
	 * @see org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor#process(org.jboss.shrinkwrap.api.Archive, org.jboss.arquillian.test.spi.TestClass)
	 */
	@Override
	public void process(Archive<?> applicationArchive, TestClass testClass) {
		if (applicationArchive instanceof WebArchive) {
			File hamcrest = new File("../../../lib/hamcrest-all-1.3.jar");
			System.out.println("WLP: Adding Jar:" + hamcrest.getAbsolutePath() + " to " + applicationArchive.getName());
			((WebArchive) applicationArchive).addAsLibraries(hamcrest);
		}
	}
}