/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.suite;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.RepeatTests;

/**
 * Only run tests with certain annotations on certain test runs.
 * <p>
 * Designed to be used to allow you to put all your tests under one test class and use {@link RepeatTests} to run it for several feature versions, but only run a subset of tests
 * for some features.
 */
public class AnnotationFilter implements TestRule {

    private final List<Class<? extends Annotation>> requiredAnnotations;
    private final List<BooleanSupplier> predicates = new ArrayList<>();

    private AnnotationFilter(List<Class<? extends Annotation>> annotations) {
        this.requiredAnnotations = annotations;
    }

    @SafeVarargs
    public static AnnotationFilter requireAnnotations(Class<? extends Annotation>... annotations) {
        return new AnnotationFilter(Arrays.asList(annotations));
    }

    public AnnotationFilter forRepeats(String... repeatIds) {
        HashSet<String> repeatIdSet = new HashSet<>(Arrays.asList(repeatIds));
        predicates.add(() -> repeatIdSet.contains(RepeatTestFilter.CURRENT_REPEAT_ACTION));
        return this;
    }

    public AnnotationFilter forAllRepeatsExcept(String... repeatIds) {
        HashSet<String> repeatIdSet = new HashSet<>(Arrays.asList(repeatIds));
        predicates.add(() -> !repeatIdSet.contains(RepeatTestFilter.CURRENT_REPEAT_ACTION));
        return this;
    }

    public AnnotationFilter inModes(TestMode... mode) {
        HashSet<TestMode> modeSet = new HashSet<>(Arrays.asList(mode));
        predicates.add(() -> modeSet.contains(TestModeFilter.FRAMEWORK_TEST_MODE));
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Statement apply(Statement statement, Description description) {

        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                boolean checkAnnotation = true;
                for (BooleanSupplier predicate : predicates) {
                    checkAnnotation &= predicate.getAsBoolean();
                }

                if (checkAnnotation && !hasAnnotations(description)) {
                    Assume.assumeTrue(false); // Failed assumption skips the test
                }

                statement.evaluate();
            }
        };
    }

    private boolean hasAnnotations(Description desc) {
        for (Class<? extends Annotation> a : requiredAnnotations) {
            if (desc.getAnnotation(a) != null) {
                return true;
            }
        }
        return false;
    }
}
