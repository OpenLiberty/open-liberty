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
package com.ibm.ws.install.internal.cmdline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallKernel;
import com.ibm.ws.install.InstallKernelFactory;
import com.ibm.ws.install.RepositoryConfigUtils;
import com.ibm.ws.install.internal.InstallKernelImpl;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.boot.cmdline.ExitCode;
import com.ibm.ws.kernel.feature.internal.cmdline.NLS;
import com.ibm.ws.kernel.feature.internal.cmdline.ReturnCode;
import com.ibm.ws.repository.resources.EsaResource;

/**
 * This API contains methods to execute the Find Action.
 */
public class ExeFindAction implements ActionHandler {

    static final protected Logger logger = InstallLogUtils.getInstallLogger();

    /**
     * Handles the Find action.
     * {@inheritDoc}
     */
    @Override
    public ExitCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args) {
        logger.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_STABILIZING_FEATUREMANAGER", "find") + "\n");

        InstallKernel installKernel = InstallKernelFactory.getInstance();
        installKernel.setUserAgent(InstallConstants.FEATURE_MANAGER);

        //Load the repository properties instance from properties file
        try {
            Properties repoProperties = RepositoryConfigUtils.loadRepoProperties();
            if (repoProperties != null) {
                //Set the repository properties instance in Install Kernel
                installKernel.setRepositoryProperties(repoProperties);
            }
        } catch (InstallException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return InstallExecutor.returnCode(e.getRc());
        }

        String searchStr = args.getPositionalArguments().get(0);
        boolean viewInfo = args.getOption("viewinfo") != null;
        try {
            logger.log(Level.INFO, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_SEARCHING"));
            logger.log(Level.INFO, "");

            List<EsaResource> esas = ((InstallKernelImpl) installKernel).queryFeatures(searchStr);
            if (esas.isEmpty()) {
                logger.log(Level.INFO, NLS.getMessage("find.no.feature"));
            } else {
                Collections.sort(esas, new Comparator<EsaResource>() {
                    @Override
                    public int compare(EsaResource er1, EsaResource er2) {
                        return getName(er1).compareTo(getName(er2));
                    }
                });
                InstallUtils.log(esas);
                for (EsaResource esa : esas) {
                    showESA(esa, viewInfo);
                }
            }
        } catch (InstallException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return InstallExecutor.returnCode(e.getRc());
        }
        return ReturnCode.OK;
    }

    private String getName(EsaResource esa) {
        String shortName = esa.getShortName();
        if (shortName != null && !shortName.isEmpty())
            return shortName;
        return esa.getProvideFeature();
    }

    private void showESA(EsaResource esa, boolean viewInfo) {
        if (!viewInfo) {
            logger.log(Level.INFO, getName(esa) + " : " + esa.getName());
            return;
        }
        logger.log(Level.INFO, getName(esa));
        logger.log(Level.INFO, NLS.getMessage("find.view.info.name", esa.getName()));
        logger.log(Level.INFO, NLS.getMessage("find.view.info.symbolic.name", esa.getProvideFeature()));
        String description = esa.getShortDescription();
        if (description != null && !description.isEmpty()) {
            description = NLS.getMessage("find.view.info.description", description);
            StringBuffer sb = new StringBuffer();
            BufferedReader reader = new BufferedReader(new StringReader(description));
            try {
                for (String line; (line = reader.readLine()) != null;) {
                    InstallUtils.wordWrap(sb, line, "        ");
                }
                logger.log(Level.INFO, sb.toString());
            } catch (IOException e) {
                logger.log(Level.SEVERE, description);
            }
        }
        Collection<String> requiredFeatures = esa.getRequireFeature();
        if (requiredFeatures != null && !requiredFeatures.isEmpty()) {
            logger.log(Level.INFO, NLS.getMessage("find.view.info.enabled.by"));
            for (String requiredFeature : requiredFeatures) {
                logger.log(Level.INFO, "        " + requiredFeature);
            }
        }
        logger.log(Level.INFO, "");
    }
}
