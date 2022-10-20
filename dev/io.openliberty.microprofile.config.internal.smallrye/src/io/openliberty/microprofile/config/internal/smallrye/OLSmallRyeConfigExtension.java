/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.smallrye;

import javax.enterprise.context.spi.Context;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.enterprise.inject.spi.configurator.BeanConfigurator;
import javax.enterprise.inject.spi.configurator.ObserverMethodConfigurator;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.microprofile.config.internal.smallrye.checkpoint.ConfigCheckpointState;
import io.openliberty.microprofile.config.internal.smallrye.checkpoint.ConfigCheckpointState.UnpauseRecording;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.inject.ConfigExtension;

@Component(service = WebSphereCDIExtension.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" }, immediate = true)
public class OLSmallRyeConfigExtension extends ConfigExtension implements Extension, WebSphereCDIExtension {

    private static class WrappedAfterDeploymentValidation implements AfterDeploymentValidation {

        private final AfterDeploymentValidation parent;

        WrappedAfterDeploymentValidation(AfterDeploymentValidation parent) {
            this.parent = parent;
        }

        /** {@inheritDoc} */
        @Override
        public void addDeploymentProblem(Throwable e) {
            System.out.println("***DEPLOYMENT PROBLEM***: " + e.getMessage());
            e.printStackTrace(System.out);
            System.out.println("************************");
            this.parent.addDeploymentProblem(e);
        }

    }

    private static class WrappedAfterBeanDiscovery implements AfterBeanDiscovery {

        private final AfterBeanDiscovery parent;

        WrappedAfterBeanDiscovery(AfterBeanDiscovery parent) {
            this.parent = parent;
        }

        /** {@inheritDoc} */
        @Override
        public void addBean(Bean<?> bean) {
            System.out.println("*** addBean: " + bean.getTypes() + ", " + bean.getQualifiers());
            this.parent.addBean(bean);
        }

        /** {@inheritDoc} */
        @Override
        public <T> BeanConfigurator<T> addBean() {
            System.out.println("*** addBean");
            return this.parent.addBean();
        }

        /** {@inheritDoc} */
        @Override
        public void addContext(Context arg0) {
            System.out.println("*** addContext");
            this.parent.addContext(arg0);
        }

        /** {@inheritDoc} */
        @Override
        public void addDefinitionError(Throwable arg0) {
            System.out.println("*** addDefinitionError: " + arg0);
            this.parent.addDefinitionError(arg0);
        }

        /** {@inheritDoc} */
        @Override
        public <T> ObserverMethodConfigurator<T> addObserverMethod() {
            System.out.println("*** addObserverMethod");
            return this.parent.addObserverMethod();
        }

        /** {@inheritDoc} */
        @Override
        public void addObserverMethod(ObserverMethod<?> arg0) {
            System.out.println("*** addObserverMethod: " + arg0);
            this.parent.addObserverMethod(arg0);
        }

        /** {@inheritDoc} */
        @Override
        public <T> AnnotatedType<T> getAnnotatedType(Class<T> arg0, String arg1) {
            System.out.println("*** getAnnotatedType: " + arg0 + ", " + arg1);
            return this.parent.getAnnotatedType(arg0, arg1);
        }

        /** {@inheritDoc} */
        @Override
        public <T> Iterable<AnnotatedType<T>> getAnnotatedTypes(Class<T> arg0) {
            System.out.println("*** getAnnotatedTypes: " + arg0);
            return this.parent.getAnnotatedTypes(arg0);
        }

    }

    @Override
    protected void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        System.out.println("***OLSmallRyeConfigExtension beforeBeanDiscovery***");
        super.beforeBeanDiscovery(bbd, bm);
    }

    @Override
    protected void processConfigProperties(
                                           @Observes @WithAnnotations(ConfigProperties.class) ProcessAnnotatedType<?> processAnnotatedType) {
        System.out.println("***OLSmallRyeConfigExtension processConfigProperties***");
        super.processConfigProperties(processAnnotatedType);
    }

    @Override
    protected void processConfigMappings(
                                         @Observes @WithAnnotations(ConfigMapping.class) ProcessAnnotatedType<?> processAnnotatedType) {
        System.out.println("***OLSmallRyeConfigExtension processConfigMappings***");
        super.processConfigMappings(processAnnotatedType);
    }

    @Override
    protected void processConfigInjectionPoints(@Observes ProcessInjectionPoint<?, ?> pip) {
        System.out.println("***OLSmallRyeConfigExtension processConfigInjectionPoints***");
        super.processConfigInjectionPoints(pip);
    }

    @Override
    protected void registerCustomBeans(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        System.out.println("***OLSmallRyeConfigExtension registerCustomBeans***");
        super.registerCustomBeans(new WrappedAfterBeanDiscovery(abd), bm);
    }

    @Override
    @FFDCIgnore(Throwable.class) // Ignoring Throwable because try-with-resources block adds implicit catch(Throwable) which we want to ignore
    protected void validate(@Observes AfterDeploymentValidation adv) {
        System.out.println("***OLSmallRyeConfigExtension validate***");
        // Do not record the configuration values read during the super.validate(). Start recording the values read after this method.
        // We want to record the configuration values only when the application reads it, therefore pausing it
        try (UnpauseRecording unpauseRecording = ConfigCheckpointState.pauseRecordingReads()) {
            super.validate(new WrappedAfterDeploymentValidation(adv));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
