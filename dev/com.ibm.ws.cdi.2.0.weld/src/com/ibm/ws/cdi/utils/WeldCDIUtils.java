/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Copyright IBM Corp. 2015, 2016
 * Some of the method implementation of filterClassesBasedOnBeansXML was borrowed from BeanDeployment.java under org.jboss.weld.bootstrap
 */

package com.ibm.ws.cdi.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.enterprise.inject.Vetoed;

import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Filter;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.metadata.FilterPredicate;
import org.jboss.weld.metadata.ScanningPredicate;
import org.jboss.weld.resources.spi.ResourceLoader;

/**
 * The original version of the is was in what is now the com.ibm.ws.cdi.shared.weld bundle.
 * It has now been split out into this 2.0 version and a corresponding 1.2 version
 */
public class WeldCDIUtils {

    /**
     * Return true if the class is vetoed or the package is vetoed
     *
     * @param type class
     * @return true if the class is vetoed or the package is vetoed, false otherwise
     */
    public static boolean isClassVetoed(Class<?> type) {
        if (type.isAnnotationPresent(Vetoed.class)) {
            return true;
        }
        return isPackageVetoed(type.getPackage());
    }

    /**
     * Return true if the package is vetoed
     *
     * @param pkg the package
     * @return ture if the package is vetoed, false otherwise
     */
    public static boolean isPackageVetoed(Package pkg) {
        return pkg != null && pkg.isAnnotationPresent(Vetoed.class);
    }

    /**
     * Filter the bean classes based on the beans.xml. This is essentially a copy of BeanDeployment.obtainClasses()
     *
     * @return the filtered classes which are not excluded by the beans.xml
     */
    public static Collection<String> filterClassesBasedOnBeansXML(BeansXml beansXml, final ResourceLoader resourceLoader, Set<String> classes) {
        if (beansXml != null && beansXml.getBeanDiscoveryMode().equals(BeanDiscoveryMode.NONE)) {
            // if the integrator for some reason ignored the "none" flag make sure we do not process the archive
            return Collections.emptySet();
        }
        Function<Metadata<Filter>, Predicate<String>> filterToPredicateFunction = new Function<Metadata<Filter>, Predicate<String>>() {

            @Override
            public Predicate<String> apply(Metadata<Filter> from) {
                return new FilterPredicate(from, resourceLoader);
            }

        };
        Collection<String> classNames;
        if (beansXml != null && beansXml.getScanning() != null) {
            Collection<Metadata<Filter>> includeFilters;
            if (beansXml.getScanning().getIncludes() != null) {
                includeFilters = beansXml.getScanning().getIncludes();
            } else {
                includeFilters = Collections.emptyList();
            }
            Collection<Metadata<Filter>> excludeFilters;
            if (beansXml.getScanning().getExcludes() != null) {
                excludeFilters = beansXml.getScanning().getExcludes();
            } else {
                excludeFilters = Collections.emptyList();
            }
            /*
             * Take a copy of the transformed collection, this means that the
             * filter predicate is only built once per filter predicate
             */
            Collection<Predicate<String>> includes;
            if (includeFilters.isEmpty()) {
                includes = Collections.emptyList();
            } else {
                includes = new ArrayList<Predicate<String>>(includeFilters.size());
                for (Metadata<Filter> includeFilter : includeFilters) {
                    includes.add(filterToPredicateFunction.apply(includeFilter));
                }
            }
            Collection<Predicate<String>> excludes;
            if (excludeFilters.isEmpty()) {
                excludes = Collections.emptyList();
            } else {
                excludes = new ArrayList<Predicate<String>>(excludeFilters.size());
                for (Metadata<Filter> excludeFilter : excludeFilters) {
                    excludes.add(filterToPredicateFunction.apply(excludeFilter));
                }
            }
            Predicate<String> filters = new ScanningPredicate<String>(includes, excludes);
            classNames = new HashSet<>();
            for (String beanClass : classes) {
                if (filters.test(beanClass)) {
                    classNames.add(beanClass);
                }
            }
        } else {
            classNames = classes;
        }
        return classNames;
    }
}
