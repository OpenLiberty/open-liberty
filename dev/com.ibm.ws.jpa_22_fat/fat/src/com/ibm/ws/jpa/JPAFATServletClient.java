/**
 *
 */
package com.ibm.ws.jpa;

import java.util.Set;

import com.ibm.websphere.simplicity.config.FeatureManager;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.PrivHelper;

/**
 *
 */
public class JPAFATServletClient extends FATServletClient {
    protected static void handleJava2SecurityWorkaround(LibertyServer server) throws Exception {
        if (Boolean.parseBoolean(PrivHelper.getProperty("global.java2.sec", "true"))) {
            System.out.println("JAG: True");
            ServerConfiguration sc = server.getServerConfiguration();
            FeatureManager fm = sc.getFeatureManager();
            Set<String> features = fm.getFeatures();
            features.add("jaxb-2.2");
            server.updateServerConfiguration(sc);
            server.saveServerConfiguration();
        } else {
            System.out.println("JAG: False");
        }
    }
}
