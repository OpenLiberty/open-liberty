/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package componenttest.custom.junit.runner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.junit.runners.model.FrameworkMethod;

import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.SkipForRepeat.MultivalueSkips;
import componenttest.rules.repeater.RepeatTestAction;

public class RepeatTestFilter {

    private static Logger log = Logger.getLogger(RepeatTestFilter.class.getName());

    //TODO RepeatTests calls activateRepeatAction() and then deactivateRepeatAction() in such a way that I believe this queue
    //will only ever have one item. If is the case then this class should be rewritten to use a relevent data structure

    /** Stack of repeat actions. The top of the stack is the most recent repeat action. */
    private static Deque<RepeatTestAction> REPEAT_ACTION_STACK = new ArrayDeque<RepeatTestAction>();

    private static boolean repeatStackContainsActionByID(String searchString) {
        for (RepeatTestAction rta : REPEAT_ACTION_STACK) {
            if (rta.getID().equals(searchString)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldRun(FrameworkMethod method) {
        //if we're not repeating then there is no point checking the SkipForRepeat annotation; always run
        if (REPEAT_ACTION_STACK.isEmpty()) {
            return true;
        }

        SkipForRepeat anno = method.getMethod().getAnnotation(SkipForRepeat.class);

        // No annotation at method or class level, so always run
        if (anno == null || anno.value().length == 0)
            return true;

        String[] skipValues = MultivalueSkips.getSkipForRepeatValues(anno.value());
        for (String action : skipValues) {
            if (repeatStackContainsActionByID(action)) {
                log.info("Skipping test method " + method.getName() + " on action " + action);
                return false;
            }
        }
        return true;

    }

    public static boolean shouldRun(Class<?> clazz) {
        //if we're not repeating then there is no point checking the SkipForRepeat annotation; always run
        if (REPEAT_ACTION_STACK.isEmpty()) {
            return true;
        }

        SkipForRepeat anno = clazz.getAnnotation(SkipForRepeat.class);

        // No annotation at class level, so always run
        if (anno == null || anno.value().length == 0)
            return true;

        FATRunner.requireFATRunner(clazz.getName());

        String[] skipValues = MultivalueSkips.getSkipForRepeatValues(anno.value());
        for (String action : skipValues) {
            if (repeatStackContainsActionByID(action)) {
                log.info("Skipping test class " + clazz.getName() + " on action " + action);
                return false;
            }
        }
        return true;
    }

    /**
     * Activate a repeat action.
     *
     * @param repeatAction The repeat action to activate.
     */
    public static void activateRepeatAction(RepeatTestAction repeatAction) {
        REPEAT_ACTION_STACK.push(repeatAction);
    }

    /**
     * Deactivate a repeat action.
     *
     * @return The action that was deactivated.
     */
    public static RepeatTestAction deactivateRepeatAction() {
        return REPEAT_ACTION_STACK.pop();
    }

    /**
     * Get the most recent repeat action that was activated.
     *
     * @return The most recently activated repeat action.
     */
    public static RepeatTestAction getMostRecentRepeatAction() {
        return REPEAT_ACTION_STACK.peek();
    }

    /**
     * Get a list of repeat actions in activation order.
     *
     * @return The list of repeat actions.
     */
    public static List<RepeatTestAction> getRepeatActions() {
        List<RepeatTestAction> actions = new ArrayList<RepeatTestAction>();

        Iterator<RepeatTestAction> iter = REPEAT_ACTION_STACK.descendingIterator();
        while (iter.hasNext()) {
            actions.add(iter.next());
        }

        return actions;
    }

    /**
     * Is any repeat action active?
     *
     * @return True if any repeat action is active.
     */
    public static boolean isAnyRepeatActionActive() {
        return !REPEAT_ACTION_STACK.isEmpty();
    }

    /**
     * Returns the active repeat actions as a string in order they were activated, left to right.
     *
     * @return The repeat actions as a string or an empty string if there are no repeat actions.
     */
    public static String getRepeatActionsAsString() {
        String actions = "";

        if (!REPEAT_ACTION_STACK.isEmpty()) {
            Iterator<RepeatTestAction> iter = REPEAT_ACTION_STACK.descendingIterator();
            while (iter.hasNext()) {
                String actionId = iter.next().getID();
                if (!"NO_MODIFICATION_ACTION".equals(actionId)) {
                    actions = actions + "_" + actionId;
                }
            }
        }

        return actions;
    }

    /**
     * Is the repeat action currently active?
     *
     * @param  actionID The repeat action to check.
     * @return          True if the repeat action (or subclass) is active.
     */
    public static boolean isRepeatActionActive(String actionID) {
        // Action subclasses are supported by adding a suffix to the ID
        if (!REPEAT_ACTION_STACK.isEmpty()) {
            Iterator<RepeatTestAction> iter = REPEAT_ACTION_STACK.descendingIterator();
            while (iter.hasNext()) {
                if (iter.next().getID().startsWith(actionID)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Is the repeat action currently active?
     *
     * @param  actionID The repeat action to check.
     * @return          True if the repeat action (or subclass) is active.
     */
    public static boolean isRepeatActionActive(RepeatTestAction action) {
        // Action subclasses are supported by adding a suffix to the ID
        if (!REPEAT_ACTION_STACK.isEmpty()) {
            Iterator<RepeatTestAction> iter = REPEAT_ACTION_STACK.descendingIterator();
            while (iter.hasNext()) {
                if (iter.next().getID().startsWith(action.getID())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Is any repeat action currently active?
     *
     * @param  actions The repeat actions to check.
     * @return         True if any of the repeat actions (or subclass) is active.
     */
    public static boolean isAnyRepeatActionActive(String... actionIDs) {
        // Action subclasses are supported by adding a suffix to the ID
        if (!REPEAT_ACTION_STACK.isEmpty()) {
            Iterator<RepeatTestAction> iter = REPEAT_ACTION_STACK.descendingIterator();
            while (iter.hasNext()) {
                RepeatTestAction currentAction = iter.next();
                for (String actionID : actionIDs) {
                    if (currentAction.getID().startsWith(actionID)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isAnyRepeatActionActive(RepeatTestAction... actions) {
        // Action subclasses are supported by adding a suffix to the ID
        if (!REPEAT_ACTION_STACK.isEmpty()) {
            Iterator<RepeatTestAction> iter = REPEAT_ACTION_STACK.descendingIterator();
            while (iter.hasNext()) {
                RepeatTestAction currentAction = iter.next();
                for (RepeatTestAction action : actions) {
                    if (currentAction.getID().startsWith(action.getID())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
