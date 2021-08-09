/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.weld.jsf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.File;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *  These tests look for changes in the FacesUrlTransformer & ConversationAwareViewHandler classes.
 *  If the checksum changes, then the latest classes should investigated and ported over to com.ibm.ws.jsfContainer
 *  See issue https://github.com/OpenLiberty/open-liberty/issues/14524
 */
public class WeldJSFCheckSumTest {

    File bundle;

    String bundleFileName = "io.openliberty.org.jboss.weld4.jar";

    String pathToFacesUrlTransformer = "org/jboss/weld/module/jsf/FacesUrlTransformer.class";
    String pathToConversationAwareViewHandler= "org/jboss/weld/module/jsf/ConversationAwareViewHandler.class";

    @Test
    public void testCheckSumForFacesUrlTransformer() {
      long expectedCheckSum  =  1873715018L; // Current as of Nov 12th, 2020
      long actualCheckSum = getChecksum(getPathToWeldBundle(), pathToFacesUrlTransformer);
      assertEquals("The checksum for " + pathToFacesUrlTransformer + " has changed!\n" +
                    "Whatever change was made to this file needs to be investigated and replicated in\n" +
                    "com.ibm.ws.jsfContainer/src/com/ibm/ws/jsf/container/cdi/FacesUrlTransformer.java\n" +
                    "and then the checksum for this test method can be updated to " + actualCheckSum, expectedCheckSum, actualCheckSum);
    }

    @Test
  	public void testCheckSumForConversationAwareViewHandler() {
      long expectedCheckSum  =  1674841844L; // Current as of Nov 12th, 2020
      long actualCheckSum = getChecksum(getPathToWeldBundle(), pathToConversationAwareViewHandler);
      assertEquals("The checksum for " + pathToConversationAwareViewHandler + " has changed!\n" +
                    "Whatever change was made to this file needs to be investigated and replicated in\n" +
                    "com.ibm.ws.jsfContainer/src/com/ibm/ws/jsf/container/cdi/IBMViewHandlerProxy.java\n" +
                    "and then the checksum for this test method can be updated to " + actualCheckSum, expectedCheckSum, actualCheckSum);
  	}

    private long getChecksum(File f, String path) {
        try (JarFile jar = new JarFile(f)) {
            JarEntry clazz = jar.getJarEntry(path);
            assertNotNull("Did not find class at file path " + f.getAbsolutePath() + "" + path, clazz);
            return clazz.getCrc();
        } catch (Exception e){
          fail("Exception thrown! " + e.toString());
          return 0;
        }
    }

    private File getPathToWeldBundle(){
      if(bundle == null){
        String projectRoot = System.getProperty("user.dir");
        bundle = new File(projectRoot + File.separator + "build" + File.separator + "libs" + File.separator + bundleFileName);
      }
      return bundle;
    }

}
