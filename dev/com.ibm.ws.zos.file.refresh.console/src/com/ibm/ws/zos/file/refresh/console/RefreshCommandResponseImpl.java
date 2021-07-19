/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.file.refresh.console;

import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.security.keyring.saf.KeyringNotificationMBean;
import com.ibm.ws.kernel.filemonitor.FileNotificationMBean;
import com.ibm.wsspi.zos.command.processing.CommandResponses;


/**
 * An implementation of refresh command responses. It
 * produces the responses for the resfresh command handler for the keystores.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "type:String=fileRefresh" }, service = { CommandResponses.class })
public class RefreshCommandResponseImpl implements CommandResponses {

    FileNotificationMBean fnmbean;
    KeyringNotificationMBean krmbean;

    /**
     * DS method to activate this component.
     */
    @Activate
    protected void activate() {}

    /**
     * DS method to deactivate this component.
     */
    @Deactivate
    protected void deactivate() {}

    /**
     * Mbean Server instance to invoke the service
     */
    private static final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

    /** {@inheritDoc} */
    @Override
    public int getCommandResponses(String commandparams, List<String> responses) {
        // Initialize return code
        int rc = 0;
        try {
            fnmbean = JMX.newMBeanProxy(mbs, new ObjectName("WebSphere:service=com.ibm.ws.kernel.filemonitor.FileNotificationMBean"), FileNotificationMBean.class);
            krmbean = JMX.newMBeanProxy(mbs, new ObjectName("WebSphere:service=com.ibm.websphere.security.keyring.saf.KeyringNotificationMBean"), KeyringNotificationMBean.class);

            //Split the commandparams and check starting from element 2 onwards
            String[] modify_param = commandparams.split(",");

            //We have already validated "refresh,keystore" from the refreshCommand class. 
            if (modify_param.length == 2) {//If nothing else is provide refresh all keystores
                krmbean.refreshRequested(null);
                fnmbean.processSecurityChanges(null);
            } else if (modify_param.length > 2 && modify_param[2].startsWith("ID") && modify_param[2].split("=").length == 2) {
                //If there is another param refresh the specific keystore id
                String keyStoreName = modify_param[2].split("=")[1].toLowerCase();
                if (krmbean.refreshRequested(keyStoreName)) {
                    responses.add("Updated the SAF keystore " + keyStoreName);
                } else if (fnmbean.processSecurityChanges(keyStoreName)) {
                    responses.add("Updated the FILE keystore " + keyStoreName);
                } else {
                    responses.add("Unable to find the keystore " + keyStoreName);
                }
            } else if (modify_param.length > 2 && modify_param[2].equalsIgnoreCase("help")) {
                responses.add("Parameters:");
                responses.add(" ID: Provide the name of the keystore");
                responses.add(" that needs to be refreshed such as ID='name'");
            } else {
                responses.add("An invalid parameter of " + modify_param[2] + " was found.\n");
                rc = -2;
            }
        } catch (Exception e) {
            // Relying on the FFDC to debug the exception
            responses.add("Unable to process refresh command due to an error.");
            responses.add("Check logs for details");
            // RC of -1 to set modify results to ERROR in command handler
            rc = -3;
        }
        return rc;
    }

}


