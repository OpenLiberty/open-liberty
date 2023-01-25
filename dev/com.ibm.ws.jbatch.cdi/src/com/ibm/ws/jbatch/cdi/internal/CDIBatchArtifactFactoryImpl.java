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
package com.ibm.ws.jbatch.cdi.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.jbatch.container.cdi.BatchXMLMapper;
import com.ibm.jbatch.container.cdi.CDIBatchArtifactFactory;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Note: BatchCDIInjectionExtension - gets control on BeforeBeanDiscovery event (i.e
 * when CDI is starting up, before it scans for beans). Injects BatchProducerBean
 * into the CDI framework.
 *
 * BatchProducerBean - provides @Producer methods for @BatchProperty, JobContext and StepContext.
 *
 * Use CDI to load batch artifacts such as JobContext, StepContext, BatchProperties,
 * and any other CDI beans configured by the app.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class CDIBatchArtifactFactoryImpl implements CDIBatchArtifactFactory {

    private final static Logger logger = Logger.getLogger(CDIBatchArtifactFactoryImpl.class.getName());

    /**
     * Use CDI to load the artifact with the given ID.
     * 
     * @return the loaded artifact; or null if CDI is not enabled for the app.
     */
    @Override
    public Object load(String batchId) {

        Object loadedArtifact;

        loadedArtifact = getArtifactById(batchId);

        if (loadedArtifact != null) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("load: batchId: " + batchId
                              + ", artifact: " + loadedArtifact
                              + ", artifact class: " + loadedArtifact.getClass().getCanonicalName());
            }
        }

        return loadedArtifact;
    }

    /**
     * @return the CDI bean reference for the given id
     */
    private Object getArtifactById(String id) {

        BeanManager bm = getBeanManager();

        Bean bean = (bm != null) ? getBeanById(bm, id) : null;

        return (bean != null) ? bm.getReference(bean, bean.getBeanClass(), bm.createCreationalContext(bean)) : null;
    }

    /**
     * @return the BeanManager, located at java:comp/BeanManager, if one is installed
     *         (meaning the app uses CDI).
     */
    @FFDCIgnore(NameNotFoundException.class)
    protected BeanManager getBeanManager() {
        // Catch exceptions so that we can failover to non-CDI loading.  Hopefully the FFDC will still be of some use.
        try {
            InitialContext initialContext = new InitialContext();
            return (BeanManager) initialContext.lookup("java:comp/BeanManager");
        } catch (NameNotFoundException nnfe) {
            return null;
        } catch (NamingException ne) {
            return null;
        }
    }

    /**
     * @param id Either the EL name of the bean, or its fully qualified class name.
     * 
     * @return the bean for the given artifact id.
     */
    protected Bean<?> getBeanById(BeanManager bm, String id) {

        Bean<?> match = getUniqueBeanByBeanName(bm, id);

        if (match == null) {
            match = getUniqueBeanForBatchXMLEntry(bm, id);
        }

        if (match == null) {
            match = getUniqueBeanForClassName(bm, id);
        }

        return match;
    }

    /**
     * Use the given BeanManager to lookup a unique CDI-registered bean
     * with bean name equal to 'batchId', using EL matching rules.
     * 
     * @return the bean with the given bean name, or 'null' if there is an ambiguous resolution
     */
    protected Bean<?> getUniqueBeanByBeanName(BeanManager bm, String batchId) {
        Bean<?> match = null;

        // Get all beans with the given EL name (id).  EL names are applied via @Named.
        // If the bean is not annotated with @Named, then it does not have an EL name
        // and therefore can't be looked up that way.  
        Set<Bean<?>> beans = bm.getBeans(batchId);

        try {
            match = bm.resolve(beans);
        } catch (AmbiguousResolutionException e) {
            return null;
        }
        return match;
    }

    /**
     * Use the given BeanManager to lookup a unique CDI-registered bean
     * with bean class equal to the batch.xml entry mapped to be the batchId parameter
     * 
     * @return the bean with the given className. It returns null if there are zero matches or if there is no umabiguous resolution (i.e. more than 1 match)
     */
    @FFDCIgnore(BatchCDIAmbiguousResolutionCheckedException.class)
    protected Bean<?> getUniqueBeanForBatchXMLEntry(BeanManager bm, String batchId) {
        ClassLoader loader = getContextClassLoader();
        BatchXMLMapper batchXMLMapper = new BatchXMLMapper(loader);
        Class<?> clazz = batchXMLMapper.getArtifactById(batchId);
        if (clazz != null) {
            try {
                return findUniqueBeanForClass(bm, clazz);
            } catch (BatchCDIAmbiguousResolutionCheckedException e) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("getBeanForBatchXML: BatchCDIAmbiguousResolutionCheckedException: " + e.getMessage());
                }
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Use the given BeanManager to lookup the set of CDI-registered beans with
     * the given class name.
     * 
     * @return the bean with the given className. It returns null if there are zero matches or if there is no umabiguous resolution (i.e. more than 1 match)
     */
    @FFDCIgnore({ ClassNotFoundException.class, BatchCDIAmbiguousResolutionCheckedException.class })
    protected Bean<?> getUniqueBeanForClassName(BeanManager bm, String className) {
        // Ignore exceptions since will just failover to another loading mechanism
        try {
            Class<?> clazz = getContextClassLoader().loadClass(className);
            return findUniqueBeanForClass(bm, clazz);
        } catch (BatchCDIAmbiguousResolutionCheckedException e) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("getBeanForClassName: BatchCDIAmbiguousResolutionCheckedException: " + e.getMessage());
            }
            return null;
        } catch (ClassNotFoundException cnfe) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("getBeanForClassName: ClassNotFoundException for " + className + ": " + cnfe);
            }
            return null;
        }
    }

    /**
     * @return the bean within the given set whose class matches the given clazz.
     * @throws BatchCDIAmbiguousResolutionCheckedException if more than one match is found
     */
    protected Bean<?> findUniqueBeanForClass(BeanManager beanManager, Class<?> clazz) throws BatchCDIAmbiguousResolutionCheckedException {
        Bean<?> match = null;
        Set<Bean<?>> beans = beanManager.getBeans(clazz);
        if (beans == null || beans.isEmpty()) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("In findBeanForClass: found empty set or null for class: " + clazz);
            }
            return null;
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("In findBeanForClass: found non-empty set: " + beans + " for class: " + clazz);
        }
        for (Bean<?> bean : beans) {
            if (bean.getBeanClass().equals(clazz)) {
                if (match != null) {
                    // Not sure if this can happen but being cautious in case we're missing a subtle CDI use case.
                    throw new BatchCDIAmbiguousResolutionCheckedException("Found both bean = " + match + ", and also bean = " + bean + " with beanClass = " + bean.getBeanClass());
                } else {
                    match = bean;
                }
            }
        }
        return match;
    }

    /**
     * @return thread context classloader
     */
    protected ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    private class BatchCDIAmbiguousResolutionCheckedException extends Exception {
        public BatchCDIAmbiguousResolutionCheckedException() {
            super();
        }

        public BatchCDIAmbiguousResolutionCheckedException(String message) {
            super(message);
        }

        public BatchCDIAmbiguousResolutionCheckedException(String message, Throwable cause) {
            super(message, cause);
        }

        public BatchCDIAmbiguousResolutionCheckedException(Throwable cause) {
            super(cause);
        }
    }

}
