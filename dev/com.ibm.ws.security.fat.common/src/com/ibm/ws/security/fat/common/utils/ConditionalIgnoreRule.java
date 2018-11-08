/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ConditionalIgnoreRule implements TestRule {

    public interface IgnoreCondition {
        boolean isSatisfied();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    public @interface ConditionalIgnore {
        Class<? extends IgnoreCondition> condition();
    }

    @Override
    public Statement apply(Statement aStatement, Description aDescription) {
        Statement result = aStatement;
        if (hasConditionalIgnoreAnnotation(aDescription)) {
            IgnoreCondition condition = getIgnoreCondition(aDescription);
            if (condition.isSatisfied()) {
                result = new IgnoreStatement(condition);
            }
        }

        return result;
    }

    private static boolean hasConditionalIgnoreAnnotation(Description aDescription) {
        return aDescription.getAnnotation(ConditionalIgnore.class) != null;
    }

    private static IgnoreCondition getIgnoreCondition(Description aDescription) {
        ConditionalIgnore annotation = aDescription.getAnnotation(ConditionalIgnore.class);
        return new IgnoreConditionCreator(aDescription.getTestClass(), annotation).create();
    }

    private static class IgnoreConditionCreator {
        private final Class<?> mTestClass;
        private final Class<? extends IgnoreCondition> conditionType;

        IgnoreConditionCreator(Class<?> aTestClass, ConditionalIgnore annotation) {
            mTestClass = aTestClass;
            conditionType = annotation.condition();
        }

        IgnoreCondition create() {
            checkConditionType();
            try {
                return createCondition();
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private IgnoreCondition createCondition() throws Exception {
            IgnoreCondition result;
            if (isConditionTypeStandalone()) {
                result = conditionType.newInstance();
            } else {
                result = conditionType.getDeclaredConstructor(mTestClass).newInstance(mTestClass);
            }
            return result;
        }

        private void checkConditionType() {
            if (!isConditionTypeStandalone() && !isConditionTypeDeclaredInTarget()) {
                String msg = "Conditional class '%s' is a member class "
                        + "but was not declared inside the test case using it.\n"
                        + "Either make this class a static class, "
                        + "standalone class (by declaring it in it's own file) "
                        + "or move it inside the test case using it";
                throw new IllegalArgumentException(String.format(msg, conditionType.getName()));
            }
        }

        private boolean isConditionTypeStandalone() {
            return !conditionType.isMemberClass()
                    || Modifier.isStatic(conditionType.getModifiers());
        }

        private boolean isConditionTypeDeclaredInTarget() {
            return mTestClass.getClass().isAssignableFrom(conditionType.getDeclaringClass());
        }
    }

    private static class IgnoreStatement extends Statement {
        private final IgnoreCondition condition;

        IgnoreStatement(IgnoreCondition condition) {
            this.condition = condition;
        }

        @Override
        public void evaluate() {
            System.out.println("Ignored by " + condition.getClass().getSimpleName());
        }
    }
}
