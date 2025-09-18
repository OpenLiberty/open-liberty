/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.visibility.tests.vistest.framework;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InAppClient;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InAppClientAsAppClientLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InAppClientAsEjbLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InAppClientAsWarLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InAppClientLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InCommonLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InEarLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InEjb;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InEjbAppClientLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InEjbAsAppClientLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InEjbAsEjbLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InEjbAsWarLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InEjbLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InEjbWarLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InNonLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InOverrideLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InPrivateLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InRuntimeExtRegular;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InRuntimeExtSeeApp;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InStandaloneWar;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InStandaloneWarLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InWar;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InWar2;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InWarAppClientLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InWarLib;
import com.ibm.ws.cdi.visibility.tests.vistest.qualifiers.InWarWebinfLib;
import com.ibm.ws.kernel.service.util.ServiceCaller;

/**
 * Holds the visibility test implementation
 */
public class VisTester {

    private static final ServiceCaller<CDIService> cdiServiceCaller = new ServiceCaller(VisTester.class, CDIService.class);

    /**
     * The full set of qualifiers to test
     */
    public static final Set<Annotation> QUALIFIERS;

    static {
        Set<Annotation> qualifiers = new HashSet<Annotation>();

        // Construct the set of literal qualifiers that we need to check the visibility of

        // Ignore all these warnings, this is the recommended way to get Annotation objects for doing a lookup
        // See CDI 5.6.3
        class AnyQualifier extends AnnotationLiteral<Any> implements Any {};
        qualifiers.add(new AnyQualifier());

        class InEjbQualifier extends AnnotationLiteral<InEjb> implements InEjb {};
        qualifiers.add(new InEjbQualifier());

        class InAppClientQualifier extends AnnotationLiteral<InAppClient> implements InAppClient {};
        qualifiers.add(new InAppClientQualifier());

        class InWarQualifier extends AnnotationLiteral<InWar> implements InWar {};
        qualifiers.add(new InWarQualifier());

        class InEjbLibQualifier extends AnnotationLiteral<InEjbLib> implements InEjbLib {};
        qualifiers.add(new InEjbLibQualifier());

        class InWarLibQualifier extends AnnotationLiteral<InWarLib> implements InWarLib {};
        qualifiers.add(new InWarLibQualifier());

        class InWarWebinfLibQualifier extends AnnotationLiteral<InWarWebinfLib> implements InWarWebinfLib {};
        qualifiers.add(new InWarWebinfLibQualifier());

        class InAppClientLibQualifier extends AnnotationLiteral<InAppClientLib> implements InAppClientLib {};
        qualifiers.add(new InAppClientLibQualifier());

        class InEjbWarLibQualifier extends AnnotationLiteral<InEjbWarLib> implements InEjbWarLib {};
        qualifiers.add(new InEjbWarLibQualifier());

        class InEjbAppClientLibQualifier extends AnnotationLiteral<InEjbAppClientLib> implements InEjbAppClientLib {};
        qualifiers.add(new InEjbAppClientLibQualifier());

        class InWarAppClientLibQualifier extends AnnotationLiteral<InWarAppClientLib> implements InWarAppClientLib {};
        qualifiers.add(new InWarAppClientLibQualifier());

        class InEarLibQualifier extends AnnotationLiteral<InEarLib> implements InEarLib {};
        qualifiers.add(new InEarLibQualifier());

        class InNonLibQualifier extends AnnotationLiteral<InNonLib> implements InNonLib {};
        qualifiers.add(new InNonLibQualifier());

        class InEjbAsEjbLibQualifier extends AnnotationLiteral<InEjbAsEjbLib> implements InEjbAsEjbLib {};
        qualifiers.add(new InEjbAsEjbLibQualifier());

        class InEjbAsWarLibQualifier extends AnnotationLiteral<InEjbAsWarLib> implements InEjbAsWarLib {};
        qualifiers.add(new InEjbAsWarLibQualifier());

        class InEjbAsAppClientLibQualifier extends AnnotationLiteral<InEjbAsAppClientLib> implements InEjbAsAppClientLib {};
        qualifiers.add(new InEjbAsAppClientLibQualifier());

        class InAppClientAsEjbLibQualifier extends AnnotationLiteral<InAppClientAsEjbLib> implements InAppClientAsEjbLib {};
        qualifiers.add(new InAppClientAsEjbLibQualifier());

        class InAppClientAsWarLibQualifier extends AnnotationLiteral<InAppClientAsWarLib> implements InAppClientAsWarLib {};
        qualifiers.add(new InAppClientAsWarLibQualifier());

        class InAppClientAsAppClientLibQualifier extends AnnotationLiteral<InAppClientAsAppClientLib> implements InAppClientAsAppClientLib {};
        qualifiers.add(new InAppClientAsAppClientLibQualifier());

        class InWar2Qualifier extends AnnotationLiteral<InWar2> implements InWar2 {};
        qualifiers.add(new InWar2Qualifier());

        class InCommonLibQualifier extends AnnotationLiteral<InCommonLib> implements InCommonLib {};
        qualifiers.add(new InCommonLibQualifier());

        class InPrivateLibQualifier extends AnnotationLiteral<InPrivateLib> implements InPrivateLib {};
        qualifiers.add(new InPrivateLibQualifier());

        class InOverrideLibQualifier extends AnnotationLiteral<InOverrideLib> implements InOverrideLib {};
        qualifiers.add(new InOverrideLibQualifier());

        class InRuntimeExtRegularQualifier extends AnnotationLiteral<InRuntimeExtRegular> implements InRuntimeExtRegular {};
        qualifiers.add(new InRuntimeExtRegularQualifier());

        class InRuntimeExtSeeAppQualifier extends AnnotationLiteral<InRuntimeExtSeeApp> implements InRuntimeExtSeeApp {};
        qualifiers.add(new InRuntimeExtSeeAppQualifier());

        class InStandaloneWarQualifier extends AnnotationLiteral<InStandaloneWar> implements InStandaloneWar {};
        qualifiers.add(new InStandaloneWarQualifier());

        class InStandaloneWarLibQualifier extends AnnotationLiteral<InStandaloneWarLib> implements InStandaloneWarLib {};
        qualifiers.add(new InStandaloneWarLibQualifier());

        QUALIFIERS = Collections.unmodifiableSet(qualifiers);
    }

    /**
     * This is the actual visibility test implementation.
     * <p>
     * TestingBeans should call this method in their doTest method, passing in their bean manager.
     *
     * @param beanManager the BeanManager to test
     * @return the visibility test result
     */
    public static String doTest(BeanManager beanManager) {

        StringBuilder sb = new StringBuilder();

        for (Annotation qualifier : QUALIFIERS) {
            Set<Bean<?>> beans = beanManager.getBeans(TargetBean.class, qualifier);
            beans.size();
            sb.append(qualifier.annotationType().getSimpleName());
            sb.append("\t");
            sb.append(beans.size());
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * @param location
     * @return
     */
    public static Annotation getQualiferForLocation(String location) {
        for (Annotation qualifier : QUALIFIERS) {
            if (qualifier.annotationType().getSimpleName().equals(location)) {
                return qualifier;
            }
        }
        throw new RuntimeException("No qualifier for location: " + location);
    }

    public static Optional<String> getModuleForClass(Class<?> clazz) {
        Optional<Optional<J2EEName>> result = cdiServiceCaller.run(cdiService -> {
            return cdiService.getModuleNameForClass(clazz);
        });
        return result.orElseThrow(() -> new RuntimeException("Failed to get CDI Service"))
                     .map(J2EEName::toString);
    }
}
