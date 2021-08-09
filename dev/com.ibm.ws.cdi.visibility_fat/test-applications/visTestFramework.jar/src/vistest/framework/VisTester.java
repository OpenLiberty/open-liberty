/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package vistest.framework;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;

import vistest.qualifiers.InAppClient;
import vistest.qualifiers.InAppClientAsAppClientLib;
import vistest.qualifiers.InAppClientAsEjbLib;
import vistest.qualifiers.InAppClientAsWarLib;
import vistest.qualifiers.InAppClientLib;
import vistest.qualifiers.InEarLib;
import vistest.qualifiers.InEjb;
import vistest.qualifiers.InEjbAppClientLib;
import vistest.qualifiers.InEjbAsAppClientLib;
import vistest.qualifiers.InEjbAsEjbLib;
import vistest.qualifiers.InEjbAsWarLib;
import vistest.qualifiers.InEjbLib;
import vistest.qualifiers.InEjbWarLib;
import vistest.qualifiers.InNonLib;
import vistest.qualifiers.InWar;
import vistest.qualifiers.InWar2;
import vistest.qualifiers.InWarAppClientLib;
import vistest.qualifiers.InWarLib;
import vistest.qualifiers.InWarWebinfLib;

/**
 * Holds the visibility test implementation
 */
public class VisTester {

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
}
