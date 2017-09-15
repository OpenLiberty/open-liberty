/**
 * Copyright 2013 International Business Machines Corp.
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
package com.ibm.jbatch.container.servicesmanager;

import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.ws.BatchDispatcher;
import com.ibm.jbatch.container.ws.events.BatchEventsPublisher;
import com.ibm.jbatch.container.ws.smf.ZosJBatchSMFLogging;
import com.ibm.jbatch.spi.services.IBatchArtifactFactory;
import com.ibm.jbatch.spi.services.IBatchThreadPoolService;
import com.ibm.jbatch.spi.services.IJobXMLLoaderService;
import com.ibm.jbatch.spi.services.ITransactionManagementService;

public interface ServicesManager {
    public IPersistenceManagerService getPersistenceManagerService();

    public ITransactionManagementService getTransactionManagementService();

    public ZosJBatchSMFLogging getJBatchSMFService();

    public IBatchKernelService getBatchKernelService();

    public IJobXMLLoaderService getDelegatingJobXMLLoaderService();

    public IJobXMLLoaderService getPreferredJobXMLLoaderService();

    public IBatchThreadPoolService getThreadPoolService();

    public IBatchArtifactFactory getDelegatingArtifactFactory();

    public IBatchArtifactFactory getPreferredArtifactFactory();

    public BatchEventsPublisher getBatchEventsPublisher();

    public BatchDispatcher getBatchJmsDispatcher();
}