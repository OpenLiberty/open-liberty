/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.ldap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.Bootstrap;
import componenttest.topology.utils.PrivHelper;

/**
 *
 */
public class LocalLdapServer {

    private final String instanceName;
    private Process localLdapInstance;
    private BufferedReader in;

    private static final Class<?> c = LocalLdapServer.class;
    private static final String APACHE_DS_HOME = PrivHelper.getProperty("apache.ds.home");
    private static final String OS_NAME = PrivHelper.getProperty("os.name");

    /**
     * Sets the name of the instance
     */
    public LocalLdapServer(String instanceName) {
        this.instanceName = instanceName;
    }

    /**
     * Starts the local Ldap instance
     */
    public void start() {
        String method = "start";
        Log.entering(c, method);
        Log.info(c, method, "Using in-Memory LDAP");
        Log.info(c, method, "Starting the " + instanceName + " instance of Apache DS");

        // Get machine java.home using bootstrap
        Bootstrap b = null;
        try {
            b = Bootstrap.getInstance();
        } catch (Exception e1) {
            Log.error(c, method, e1, "Error while Getting the ootstrap instance");
        }
        String hostName = b.getValue("hostName");
        String machineJavaHome = b.getValue(hostName + ".JavaHome");
        Log.info(c, method, "machineJavaHome from Bootstrap : " + machineJavaHome);

        String JAVA_COMMAND = machineJavaHome + "/bin/java";

        String ADS_CONTROLS = "-Dapacheds.controls=org.apache.directory.api.ldap.codec.controls.cascade.CascadeFactory,org.apache.directory.api.ldap.codec.controls.manageDsaIT.ManageDsaITFactory,org.apache.directory.api.ldap.codec.controls.search.entryChange.EntryChangeFactory,org.apache.directory.api.ldap.codec.controls.search.pagedSearch.PagedResultsFactory,org.apache.directory.api.ldap.codec.controls.search.persistentSearch.PersistentSearchFactory,org.apache.directory.api.ldap.codec.controls.search.subentries.SubentriesFactory,org.apache.directory.api.ldap.extras.controls.ppolicy_impl.PasswordPolicyFactory,org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncDoneValueFactory,org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncInfoValueFactory,org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncRequestValueFactory,org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncStateValueFactory";
        String ADS_EXTENDED_OPERATIONS = "-Dapacheds.extendedOperations=org.apache.directory.api.ldap.extras.extended.ads_impl.cancel.CancelFactory,org.apache.directory.api.ldap.extras.extended.ads_impl.certGeneration.CertGenerationFactory,org.apache.directory.api.ldap.extras.extended.ads_impl.gracefulShutdown.GracefulShutdownFactory,org.apache.directory.api.ldap.extras.extended.ads_impl.storedProcedure.StoredProcedureFactory,org.apache.directory.api.ldap.extras.extended.ads_impl.gracefulDisconnect.GracefulDisconnectFactory";
        String ADS_CLASSPATH = APACHE_DS_HOME + "/lib/apacheds-service-2.0.0-M15.jar";

        String ADS_TDS_LOG4J_CONFIG = "-Dlog4j.configuration=file:" + APACHE_DS_HOME + "/instances/" + instanceName + "/conf/log4j.properties";
        String ADS_TDS_LOG_DIR = "-Dapacheds.log.dir=" + APACHE_DS_HOME + "/instances/" + instanceName + "/log";
        String ADS_TDS_INSTANCE_HOME = APACHE_DS_HOME + "/instances/" + instanceName;

        // Add "" to directory inputs if OS is windows
        if (OS_NAME.toLowerCase().startsWith("win")) {
            ADS_CLASSPATH = "\"" + APACHE_DS_HOME + "/lib/apacheds-service-2.0.0-M15.jar\"";
            ADS_TDS_LOG4J_CONFIG = "-Dlog4j.configuration=file:\"" + APACHE_DS_HOME + "/instances/" + instanceName + "/conf/log4j.properties\"";
            ADS_TDS_LOG_DIR = "-Dapacheds.log.dir=\"" + APACHE_DS_HOME + "/instances/" + instanceName + "/log\"";
            ADS_TDS_INSTANCE_HOME = "\"" + APACHE_DS_HOME + "/instances/" + instanceName + "\"";
        }
        List<String> params = java.util.Arrays.asList(JAVA_COMMAND, ADS_CONTROLS, ADS_EXTENDED_OPERATIONS, ADS_TDS_LOG4J_CONFIG, ADS_TDS_LOG_DIR, "-cp", ADS_CLASSPATH,
                                                      "org.apache.directory.server.UberjarMain", ADS_TDS_INSTANCE_HOME);
        Log.info(c, method, instanceName + " params : " + params.toString());

        // Start the Apache DS instance
        try {
            ProcessBuilder pb = new ProcessBuilder(params);
            pb.redirectErrorStream(true);
            localLdapInstance = pb.start();
        } catch (IOException e3) {
            Log.error(c, method, e3, "Error while starting the " + instanceName + " instance of apache DS");
            try {
                throw new Exception("Error while starting the " + instanceName + " instance of apache DS");
            } catch (Exception e) {
                Log.error(c, method, e3, "Error while throwing new exception");
            }
        }

        in = new BufferedReader(new InputStreamReader(localLdapInstance.getInputStream()));

        try {
            String line = null;
            long time = System.currentTimeMillis();
            long timeout = time + 120000; //wait max two minutes for Apache DS to start
            while (System.currentTimeMillis() <= timeout) {
                line = in.readLine();
                if (line != null) {
                    Log.info(c, method, line);
                    //check if we had the last line of Apache DS output
                    if (line.trim().equals("|_|")) {
                        //started, break the loop
                        break;
                    } else {
                        //wasn't the output we expected, but wasn't end of stream
                        //so just continue
                        continue;
                    }
                } else {
                    //reached the end of the stream, wait 1 second before trying again
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.error(c, "start", e, "Error occored while Thread.sleep()");
                    }
                }
            }
        } catch (IOException e2) {
            Log.error(c, "start", e2, "Error occored while reading the buffered reader");
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                Log.error(c, "start", e, "Error occored while closing the buffered reader");
            }
        }

        Log.exiting(c, method);
    }

    /**
     * Stops the local Ldap instance
     */
    public void stop() {
        String method = "stop";
        Log.entering(c, method);
        // Stop the ldap instance started.
        Log.info(c, method, "Stopping the " + instanceName + " instance of Apache DS");
        localLdapInstance.destroy();
        Log.exiting(c, method);
    }
}
