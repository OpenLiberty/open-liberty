/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import app.web.MyServlet;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Example Shrinkwrap FAT project:
 * <li> Uses minimal ant scripting, so that the bucket can be run using the green
 * 'play' button in an eclipse IDE or with the traditional build-test.xml via ant.
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant, and is a small downside to factoring out ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that no @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 1.7)
public class SimpleBatchTest extends FATServletClient {

    @Server("CDIBatch")
    @TestServlet(servlet = MyServlet.class, path = "implicit/MyServlet")
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive implicit = ShrinkWrap.create(WebArchive.class, "implicit.war")//
                        .addPackages(true, "app");
        addBatchJob(implicit, "cdi.xml");

        // Write the WebArchive to 'publish/servers/<server>/apps' and print the contents
        ShrinkHelper.exportAppToServer(server1, implicit);

        server1.startServer();
    }

    /**
     * @param implicit archive
     * @param jslName Batch Job JSL name
     */
    private static void addBatchJob(WebArchive implicit, String jslName) {
        Log.info(SimpleBatchTest.class, "addBatchJob", "Adding jslName = " + jslName);
        String resourceDir = "test-applications/implicit/resources/";
        String batchJobsDir = "classes/META-INF/batch-jobs/";
        implicit.addAsWebInfResource(new File(resourceDir + jslName), batchJobsDir + jslName);
    }
}
