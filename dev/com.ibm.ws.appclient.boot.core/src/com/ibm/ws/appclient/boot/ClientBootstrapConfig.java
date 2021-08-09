/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.appclient.boot;

import java.net.URL;
import java.util.List;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.KernelUtils;

/**
 *
 */
public class ClientBootstrapConfig extends BootstrapConfig {

    /**
     * Return the root directory name of the processes.
     * 
     * @return BootstrapConstants.LOC_AREA_NAME_CLIENTS
     */
    @Override
    protected String getProcessesSubdirectory() {
        return BootstrapConstants.LOC_AREA_NAME_CLIENTS;
    }

    /**
     * Return the output directory name value set in the environment variable WLP_CLIENT_OUTPUT_DIR.
     * 
     * @return BootstrapConstants.ENV_WLP_CLIENT_OUTPUT_DIR
     */
    @Override
    protected String getOutputDirectoryEnvName() {
        return BootstrapConstants.ENV_WLP_CLIENT_OUTPUT_DIR;
    }

    @Override
    protected String getDefaultProcessName() {
        return BootstrapConstants.DEFAULT_CLIENT_NAME;
    }

    @Override
    protected String getProcessXMLFilename() {
        return BootstrapConstants.CLIENT_XML;
    }

    @Override
    protected String getProcessXMLResourcePath() {
        return "/OSGI-OPT/websphere/client/client.xml";
    }

    @Override
    protected String getErrorCreatingNewProcessMessageKey() {
        return "error.creatingNewClient";
    }

    @Override
    protected String getErrorCreatingNewProcessMkDirFailMessageKey() {
        return "error.creatingNewClientMkDirFail";
    }

    @Override
    protected String getErrorCreatingNewProcessExistsMessageKey() {
        return "error.creatingNewClientExists";
    }

    @Override
    protected String getErrorNoExistingProcessMessageKey() {
        return "error.noExistingClient";
    }

    @Override
    protected String getErrorProcessDirExistsMessageKey() {
        return "error.clientDirExists";
    }

    @Override
    protected String getErrorProcessNameCharacterMessageKey() {
        return "error.clientNameCharacter";
    }

    @Override
    protected String getInfoNewProcessCreatedMessageKey() {
        return "info.newClientCreated";
    }

    @Override
    protected String getProcessesTemplateDir() {
        return "templates/clients/";
    }

    @Override
    public String getProcessType() {
        return BootstrapConstants.LOC_PROCESS_TYPE_CLIENT;
    }

    @Override
    public void addBootstrapJarURLs(List<URL> urlList) {
        urlList.add(KernelUtils.getLocationFromClass(ClientBootstrapConfig.class));
        super.addBootstrapJarURLs(urlList);
    }

    /**
     * No need to generate server.env/client.env in the client process.
     */
    @Override
    protected ReturnCode generateServerEnv(boolean generatePassword) {
        return ReturnCode.OK;
    }
}
