/*
 * Copyright 2012 International Business Machines Corp.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jbatch.container.services.impl;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.stream.StreamSource;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.jbatch.container.RASConstants;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.services.IJobXMLSource;
import com.ibm.jbatch.container.ws.BatchSubmitInvalidParametersException;
import com.ibm.jbatch.spi.services.IBatchConfig;
import com.ibm.jbatch.spi.services.IJobXMLLoaderService;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class DelegatingJobXMLLoaderServiceImpl implements IJobXMLLoaderService, RASConstants {

    private final static String CLASSNAME = DelegatingJobXMLLoaderServiceImpl.class.getName();
    private final static Logger logger = Logger.getLogger(CLASSNAME, BATCH_MSG_BUNDLE);

    /**
     * Note: currently never set.
     */
    protected IJobXMLLoaderService preferredJobXmlLoader;

    public static final String PREFIX = "META-INF/batch-jobs/";

    @Override
    public IJobXMLSource loadJSL(String id) {

        IJobXMLSource jobXML = null;

        if (preferredJobXmlLoader != null && !preferredJobXmlLoader.getClass().equals(this.getClass())) {
            jobXML = preferredJobXmlLoader.loadJSL(id);
        } else {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "No preferred job xml loader is detected in configuration");
            }
        }

        if (jobXML != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Preferred job xml loader loaded job with id " + id + ".");
            }
            return jobXML;
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Preferred job xml loader failed to load " + id + ". Defaulting to " + PREFIX);
        }

        jobXML = loadJobFromMetaInfBatchJobs(id);

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Loaded job xml with " + id + " from " + PREFIX);
        }

        return jobXML;
    }

    private IJobXMLSource loadJobFromMetaInfBatchJobs(String id) {

        ClassLoader tccl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });

        String relativePath = PREFIX + id + ".xml";

        StreamSource strSource = null;

        logger.fine("looking up batch job xml at " + relativePath);

        URL url = tccl.getResource(relativePath);

        // The lookup failed.  Check if id already ends with ".xml"...
        if (url == null && id.endsWith(".xml")) {
            String path = PREFIX + id;
            logger.fine("looking up batch job xml at " + path);
            url = tccl.getResource(path);
        }
        //Defect 191113
        //The BatchJmsEndpointListener isExceptionToProcess() method did not check for a BatchContainerRuntimeException exception
        //but instead if returned false a BatchContainerRuntimeException was thrown. A new exception class was created which
        //was used below and extends BatchContainerRuntimeException to take care of this case and process the exception properly.
        if (url != null) {
            try {
                strSource = new StreamSource(url.openStream());
            } catch (IOException e) {
                String excMessage = "IOException on URL at relativePath: " + relativePath + ",  on openStream(), message: " + e.getMessage();
                logger.fine(excMessage);
                throw new BatchSubmitInvalidParametersException(excMessage, e);
            }
        } else {
            String excMessage = "Resource not found at relativePath: " + relativePath;
            logger.log(Level.SEVERE, "jsl.not.found.batch-jobs", new Object[] { id, relativePath });
            throw new BatchSubmitInvalidParametersException(excMessage);
        }
        strSource.setSystemId(url.toExternalForm());
        JobXMLSource jslSource = new JobXMLSource(url, strSource);
        return jslSource;
    }

    @Override
    public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {

    }

    @Override
    public void shutdown() throws BatchContainerServiceException {
        // TODO Auto-generated method stub
    }

}
