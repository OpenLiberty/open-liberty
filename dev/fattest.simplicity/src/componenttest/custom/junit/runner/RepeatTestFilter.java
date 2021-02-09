/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

public class RepeatTestFilter {

    private static Logger log = Logger.getLogger(RepeatTestFilter.class.getName());

    /** Stack of repeat actions. The top of the stack is the most recent repeat action. */
    private static Deque<String> REPEAT_ACTION_STACK = new ArrayDeque<String>();

    public static boolean shouldRun(FrameworkMethod method) {
        //if we're not repeating then there is no point checking the SkipForRepeat annotation; always run
        if (REPEAT_ACTION_STACK.isEmpty()) {
            return true;
        }

        SkipForRepeat anno = method.getMethod().getAnnotation(SkipForRepeat.class);

        // No annotation at method or class level, so always run
        if (anno == null || anno.value().length == 0)
            return true;

        for (String action : anno.value()) {
            if (REPEAT_ACTION_STACK.contains(action)) {
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

        for (String action : anno.value()) {
            if (REPEAT_ACTION_STACK.contains(action)) {
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
    public static void activateRepeatAction(String repeatAction) {
        REPEAT_ACTION_STACK.push(repeatAction);
    }

    /**
     * Deactivate a repeat action.
     *
     * @return The action that was deactivated.
     */
    public static String deactivateRepeatAction() {
        return REPEAT_ACTION_STACK.pop();
    }

    /**
     * Get the most recent repeat action that was activated.
     *
     * @return The most recently activated repeat action.
     */
    public static String getMostRecentRepeatAction() {
        return REPEAT_ACTION_STACK.peek();
    }

    /**
     * Get a list of repeat actions in activation order.
     *
     * @return The list of repeat actions.
     */
    public static List<String> getRepeatActions() {
        List<String> actions = new ArrayList<String>();

        Iterator<String> iter = REPEAT_ACTION_STACK.descendingIterator();
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
            Iterator<String> iter = REPEAT_ACTION_STACK.descendingIterator();
            while (iter.hasNext()) {
                String action = iter.next();
                if (!"NO_MODIFICATION_ACTION".equals(action)) {
                    actions = actions + "_" + action;
                }
            }
        }

        return actions;
    }

    /**
     * Is the repeat action active?
     *
     * @param  action The repeat action to check.
     * @return        True if the repeat action is active.
     */
    public static boolean isRepeatActionActive(String action) {
        return REPEAT_ACTION_STACK.contains(action);
    }
}
