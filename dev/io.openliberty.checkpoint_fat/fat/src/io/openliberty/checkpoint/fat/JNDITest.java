/*******************************************************************************
 * (c) Copyright IBM Corporation 2022.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.JNDIEntry;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import jndiApp.JNDIservlet;
import jndiApp.JNDIResourceServlet;

@RunWith(FATRunner.class)
@CheckpointTest
public class JNDITest {

    public static final String APP_NAME = "jndiApp";

    @Server("jndiServer")
    public static LibertyServer server;

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        server.deleteAllDropinApplications();
        WebArchive webappWar = ShrinkWrap.create(WebArchive.class, "JNDIApplication.war")
        		.addClass(JNDIservlet.class)
        		.addClass(JNDIResourceServlet.class);
        ShrinkHelper.exportAppToServer(server, webappWar, DeployOptions.OVERWRITE);
    }

    @Test
    public void testJNDIlookupNoEntry() throws Exception {

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START);
        server.startServer();
        server.stopServer();

        ServerConfiguration config = server.getServerConfiguration();
        JNDIEntry defaultEntry = new JNDIEntry();
        defaultEntry.setJndiName("jndi/value");
        defaultEntry.setValue("default value");
        config.getJndiEntryElements().add(defaultEntry);
        server.updateServerConfiguration(config);

        server.checkpointRestore();

        HttpUtils.findStringInUrl(server, "jndiApp/servlet", "default value");
        HttpUtils.findStringInUrl(server, "jndiApp/resource", "default value");
        
    }
    
    @Test
    public void testJNDIlookup() throws Exception {
    	
        ServerConfiguration preConfig = server.getServerConfiguration();
        JNDIEntry defaultEntry = new JNDIEntry();
        defaultEntry.setJndiName("jndi/value");
        defaultEntry.setValue("default value");
        defaultEntry.setId("jndiEntry");
        preConfig.getJndiEntryElements().add(defaultEntry);
        server.updateServerConfiguration(preConfig);
        
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer();
        
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, "jndiApp/servlet", "default value");   
        HttpUtils.findStringInUrl(server, "jndiApp/resource", "default value");      
        server.stopServer();

        ServerConfiguration config = server.getServerConfiguration();
        config.getJndiEntryElements().getById("jndiEntry").setValue("alternate value");
        server.updateServerConfiguration(config);

        server.checkpointRestore();

        HttpUtils.findStringInUrl(server, "jndiApp/servlet", "alternate value");
        HttpUtils.findStringInUrl(server, "jndiApp/resource", "alternate value");   
    
    }

    @After
    public void stopServer() throws Exception {
        server.stopServer();
        ServerConfiguration config = server.getServerConfiguration();
        config.getJndiEntryElements()
                        .removeIf((JNDIEntry entry) -> entry.getJndiName().equals("jndi/value"));
        server.updateServerConfiguration(config);
    }

}
