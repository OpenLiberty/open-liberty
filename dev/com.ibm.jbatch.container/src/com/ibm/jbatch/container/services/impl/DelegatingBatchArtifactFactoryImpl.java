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

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.container.cdi.BatchXMLMapper;
import com.ibm.jbatch.container.cdi.CDIBatchArtifactFactory;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.util.DependencyInjectionUtility;
import com.ibm.jbatch.container.ws.JoblogUtil;
import com.ibm.jbatch.spi.services.IBatchArtifactFactory;
import com.ibm.jbatch.spi.services.IBatchConfig;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class DelegatingBatchArtifactFactoryImpl implements IBatchArtifactFactory, XMLStreamConstants {

    private final static Logger logger = Logger.getLogger(DelegatingBatchArtifactFactoryImpl.class.getName());

    /**
     * If CDI is enabled this ref will be set.
     */
    protected CDIBatchArtifactFactory cdiBatchArtifactFactory;

    /**
     * DS injection.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setCDIBatchArtifactFactory(CDIBatchArtifactFactory preferredArtifactFactory) {
        this.cdiBatchArtifactFactory = preferredArtifactFactory;
    }

    /**
     * DS un-inject.
     */
    protected void unsetCDIBatchArtifactFactory(CDIBatchArtifactFactory preferredArtifactFactory) {
        if (this.cdiBatchArtifactFactory == preferredArtifactFactory) {
            this.cdiBatchArtifactFactory = null;
        }
    }

    private static final Set<Class<?>> classIssuedMessagesSet = Collections.newSetFromMap(new WeakHashMap<Class<?>, Boolean>());

    public static synchronized void addClassToIssuedMessage(Class<?> clazz) {
        classIssuedMessagesSet.add(clazz);
    }

    public static synchronized boolean classExistsInIssuedMessage(Class<?> clazz) {
        return classIssuedMessagesSet.contains(clazz);
    }

    // Uses TCCL
    @Override
    public Object load(String batchId) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        BatchXMLMapper batchXMLMapper = new BatchXMLMapper(tccl);

        //If CDI factory is loaded, use it.
        if (cdiBatchArtifactFactory != null) {
            Object artifact = null;
            artifact = cdiBatchArtifactFactory.load(batchId);
            if (artifact != null) {
                return artifact;
            }
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "load: Artifact not referenced using CDI, will search through Batch.xml: ", tccl);
        }

        Object loadedArtifact = null;
        Class<?> clazz = batchXMLMapper.getArtifactById(batchId);

        if (clazz != null) {
            try {
                loadedArtifact = (clazz).newInstance();
            } catch (IllegalAccessException e) {
                throw new BatchContainerRuntimeException("Tried but failed to load artifact with id: " + batchId, e);
            } catch (InstantiationException e) {
                throw new BatchContainerRuntimeException("Tried but failed to load artifact with id: " + batchId, e);
            }
        }

        clazz = null;

        if (loadedArtifact == null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "load: Artifact not found in batch.xml, trying thread context classloader: ", tccl);
            }
            try {
                clazz = Thread.currentThread().getContextClassLoader().loadClass(batchId);
                if (clazz != null) {
                    loadedArtifact = clazz.newInstance();
                }
            } catch (ClassNotFoundException e) {
                throw new BatchContainerRuntimeException("Failed to load artifact with id: " + batchId, e);
            } catch (InstantiationException e) {
                throw new BatchContainerRuntimeException("Failed to load artifact with id: " + batchId, e);
            } catch (IllegalAccessException e) {
                throw new BatchContainerRuntimeException("Failed to load artifact with id: " + batchId, e);
            }
        }

        if (cdiBatchArtifactFactory != null && !classExistsInIssuedMessage(clazz)) {
            addClassToIssuedMessage(clazz);
            JoblogUtil.logToJobLogAndTrace(Level.WARNING, "cdi.ambiguous.artifact.names", null, logger);
        }

        DependencyInjectionUtility.injectReferences(loadedArtifact, ProxyFactory.getInjectionReferences());

        return loadedArtifact;
    }

    @Override
    public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {

    }

    @Override
    public void shutdown() throws BatchContainerServiceException {
        // TODO Auto-generated method stub

    }
}
