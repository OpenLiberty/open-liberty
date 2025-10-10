/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.condition;

import org.osgi.service.condition.Condition;

/**
 * The start phase condition can be used by a bundle that has a component that should be enabled
 * at a different start-phase (OSGi start level) than the bundle containing the component.
 * Note that the containing bundle must be active before any component from the bundle can
 * activate. This generally means that a component will likely want to use a start-phase
 * equal to or greater than its containing bundle.
 * This is useful when a component needs to be activated at a later start-phase to allow other
 * bundles to activate (and their components are allowed to activate) before the component cant
 * activate.
 * <p>
 * There are filter constants defined that allow a bundle to use to target specific start-phases.
 * For example, a component can use the following filter to activate at start-phase CONTAINER_LATE or
 * later:
 *
 * <pre>
 * {@code @Component}
 * {@code @SatisfyingConditionTarget(CONTAINER_LATE_FILTER)}
 * public class MyComponent {}
 * </pre>
 *
 * Or with a {@code @Reference} like the following
 *
 * <pre>
 * {@code @Activate}
 * public MyComponent({@code @Reference(target = CONTAINER_LATE_FILTER)} Condition containerLate){}
 * </pre>
 */
public interface StartPhaseCondition {
    /**
     * The ID of the condition service that indicates the current liberty start-phase.
     */
    static final String ID_NAME = "io.openliberty.start.phase";

    /**
     * The condition filter that returns true if the start-phase is {@code >=} {@link StartPhase#PREPARE}
     */
    static final String PREPARE_FILTER = "(&(" + Condition.CONDITION_ID + "=" + StartPhaseCondition.ID_NAME + ")(" + StartPhaseCondition.ID_NAME
                                         + ">=PREPARE))";
    /**
     * The condition filter that returns true if the start-phase is {@code >=} {@link StartPhase#SERVICE_EARLY}
     */

    static final String SERVICE_EARLY_FILTER = "(&(" + Condition.CONDITION_ID + "=" + StartPhaseCondition.ID_NAME + ")(" + StartPhaseCondition.ID_NAME
                                               + ">=SERVICE_EARLY))";
    /**
     * The condition filter that returns true if the start-phase is {@code >=} {@link StartPhase#SERVICE}
     */
    static final String SERVICE_FILTER = "(&(" + Condition.CONDITION_ID + "=" + StartPhaseCondition.ID_NAME + ")(" + StartPhaseCondition.ID_NAME
                                         + ">=SERVICE))";
    /**
     * The condition filter that returns true if the start-phase is {@code >=} {@link StartPhase#SERVICE_LATE}
     */
    static final String SERVICE_LATE_FILTER = "(&(" + Condition.CONDITION_ID + "=" + StartPhaseCondition.ID_NAME + ")(" + StartPhaseCondition.ID_NAME
                                              + ">=SERVICE_LATE))";

    /**
     * The condition filter that returns true if the start-phase is {@code >=} {@link StartPhase#CONTAINER_EARLY}
     */
    static final String CONTAINER_EARLY_FILTER = "(&(" + Condition.CONDITION_ID + "=" + StartPhaseCondition.ID_NAME + ")(" + StartPhaseCondition.ID_NAME
                                                 + ">=CONTAINER_EARLY))";
    /**
     * The condition filter that returns true if the start-phase is {@code >=} {@link StartPhase#CONTAINER}
     */
    static final String CONTAINER_FILTER = "(&(" + Condition.CONDITION_ID + "=" + StartPhaseCondition.ID_NAME + ")(" + StartPhaseCondition.ID_NAME
                                           + ">=CONTAINER))";
    /**
     * The condition filter that returns true if the start-phase is {@code >=} {@link StartPhase#CONTAINER_LATE}
     */
    static final String CONTAINER_LATE_FILTER = "(&(" + Condition.CONDITION_ID + "=" + StartPhaseCondition.ID_NAME + ")(" + StartPhaseCondition.ID_NAME
                                                + ">=CONTAINER_LATE))";

    /**
     * The condition filter that returns true if the start-phase is {@code >=} {@link StartPhase#APPLICATION_EARLY}
     */
    static final String APPLICATION_EARLY_FILTER = "(&(" + Condition.CONDITION_ID + "=" + StartPhaseCondition.ID_NAME + ")(" + StartPhaseCondition.ID_NAME
                                                   + ">=APPLICATION_EARLY))";
    /**
     * The condition filter that returns true if the start-phase is {@code >=} {@link StartPhase#APPLICATION}
     */
    static final String APPLICATION_FILTER = "(&(" + Condition.CONDITION_ID + "=" + StartPhaseCondition.ID_NAME + ")(" + StartPhaseCondition.ID_NAME
                                             + ">=APPLICATION))";
    /**
     * The condition filter that returns true if the start-phase is {@code >=} {@link StartPhase#APPLICATION_LATE}
     */
    static final String APPLICATION_LATE_FILTER = "(&(" + Condition.CONDITION_ID + "=" + StartPhaseCondition.ID_NAME + ")(" + StartPhaseCondition.ID_NAME
                                                  + ">=APPLICATION_LATE))";
    /**
     * The condition filter that returns true if the start-phase is {@code >=} {@link StartPhase#ACTIVE}
     */
    static final String ACTIVE_FILTER = "(&(" + Condition.CONDITION_ID + "=" + StartPhaseCondition.ID_NAME + ")(" + StartPhaseCondition.ID_NAME
                                        + ">=ACTIVE))";

    /**
     * Represents a start-phase for Liberty
     */
    public enum StartPhase {
        PREPARE(7),
        SERVICE_EARLY(8),
        SERVICE(9),
        SERVICE_LATE(10),
        CONTAINER_EARLY(11),
        CONTAINER(12),
        CONTAINER_LATE(13),
        APPLICATION_EARLY(17),
        APPLICATION(18),
        APPLICATION_LATE(19),
        ACTIVE(20);

        final int level;

        private StartPhase(int level) {
            this.level = level;
        }

        public int level() {
            return level;
        }

        public StartPhase previous() {
            if (this == PREPARE) {
                return null;
            }
            return values()[this.ordinal() - 1];
        }

        public StartPhase next() {
            if (this == ACTIVE) {
                return null;
            }
            return values()[this.ordinal() + 1];
        }

        public static StartPhase getByActiveLevel(int frameworkActiveLevel) {
            // because of gaps between phases we just do a less-than next phase check
            // to get the current phase starting at the lowest phase level
            if (frameworkActiveLevel < SERVICE_EARLY.level) {
                return PREPARE;
            }
            if (frameworkActiveLevel < SERVICE.level) {
                return SERVICE_EARLY;
            }
            if (frameworkActiveLevel < SERVICE_LATE.level) {
                return SERVICE;
            }
            if (frameworkActiveLevel < CONTAINER_EARLY.level) {
                return SERVICE_LATE;
            }
            if (frameworkActiveLevel < CONTAINER.level) {
                return CONTAINER_EARLY;
            }
            if (frameworkActiveLevel < CONTAINER_LATE.level) {
                return CONTAINER;
            }
            if (frameworkActiveLevel < APPLICATION_EARLY.level) {
                return CONTAINER_LATE;
            }
            if (frameworkActiveLevel < APPLICATION.level) {
                return APPLICATION_EARLY;
            }
            if (frameworkActiveLevel < APPLICATION_LATE.level) {
                return APPLICATION;
            }
            if (frameworkActiveLevel < ACTIVE.level) {
                return APPLICATION_LATE;
            }

            return ACTIVE;
        }
    }
}
