/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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

import org.junit.runners.model.FrameworkMethod;

import componenttest.annotation.SkipForRepeat;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Repeat test filter.
 * 
 * Note that this filter class does not extent {@link org.junit.runner.manipulation.Filter}.
 * While a similar API is defined, usage is custom to {@link FATRunner}.
 * 
 * This class maintains a static stack of active repeat actions.  (TFB: Why a stack is needed
 * is unclear.  Can repeat actions nest?)
 * 
 * When beginning a repeat action
 * (see {@link componenttest.rules.repeater.RepeatTests.CompositeRepeatTestActionStatement.evaluate()}),
 * the repeat action is pushed using {@link #activateRepeatAction(String)}.
 * At the ending of a repeat action, the repeat action is popped using
 * {link {@link #deactivateRepeatAction()}.
 * 
 * While running tests, when starting a test class, a call is made to tell if any
 * repeat filter is set on that class for the current repeat action on the test class.
 * That is, {@link #shouldRun(Class)} is invoked from {@link FATRunner#classBlock(RunNotifier)},
 * and the entire class may be skipped.
 * 
 * When starting a test method, a call is made to tell if any repeat filter is set
 * on that test method, and the test method method may be skipped.  See
 * {@link #shouldRun(FrameworkMethod)} and {@link FATRunner#methodBlock(FrameworkMethod)}.  
 */
public class RepeatTestFilter {
    private static final Class<? extends RepeatTestFilter> c = RepeatTestFilter.class;
    
    /** Stack of repeat actions. */
    private static Deque<String> REPEAT_ACTION_STACK = new ArrayDeque<String>();

    /**
     * Activate a repeat action.
     *
     * @param repeatAction The repeat action to activate.
     */
    public static void activateRepeatAction(String repeatAction) {
        REPEAT_ACTION_STACK.push(repeatAction);
    }

    /**
     * Deactivate the top-most active repeat action.
     *
     * @return The action that was deactivated.
     */
    public static String deactivateRepeatAction() {
        return REPEAT_ACTION_STACK.pop();
    }

    /**
     * Answer the top-most repeat action.
     *
     * @return The top-most repeat action.  Null if no repeat actions are active.
     */
    public static String getMostRecentRepeatAction() {
        return REPEAT_ACTION_STACK.peek();
    }

    /**
     * Tell if any repeat action is active.
     *
     * @return True of false telling if a repeat action is active.
     */
    public static boolean isAnyRepeatActionActive() {
        return !REPEAT_ACTION_STACK.isEmpty();
    }

    /**
     * Tell if a repeat action is active.
     *
     * @param action The repeat action that is to be tested.
     *
     * @return True or false telling if the repeat action is
     *     one of the active repeat actions.
     */
    public static boolean isRepeatActionActive(String action) {
        return REPEAT_ACTION_STACK.contains(action);
    }

    /**
     * Answer the stack of repeat actions as a list.  Put the
     * active repeat action first, and put additional repeat
     * actions in descending order.
     * 
     * New storage is allocated for the list.
     *
     * @return The list of repeat actions, using newly allocated
     *     storage.
     */
    public static List<String> getRepeatActions() {
        List<String> actions = new ArrayList<String>();

        Iterator<String> iter = REPEAT_ACTION_STACK.descendingIterator();
        while ( iter.hasNext() ) {
            actions.add( iter.next() );
        }

        return actions;
    }

    /**
     * Answer a print string of all repeat actions, starting with the current
     * active repeat action, and continuing in descending order.
     *
     * Answer an empty string if no repeat actions are active.
     *
     * @return A print string of all repeat actions. 
     */
    @SuppressWarnings("null")
    public static String getRepeatActionsAsString() {
        if ( REPEAT_ACTION_STACK.isEmpty() ) {
            return "";
        }

        String actionText = null;
        StringBuilder actionTextBuilder = null;
        
        Iterator<String> actions = REPEAT_ACTION_STACK.descendingIterator();
        while ( actions.hasNext() ) {
            String action = actions.next();

            if ( "NO_MODIFICATION_ACTION".equals(action) ) {
                continue;
            }

            // At 0 : (actionText == null), (actionTextBuilder == null)
            // At 1 : (actionText != null), (actionTextBuilder == null)
            // At 2+: (actionText == null), (actionTextBuilder != null)

            if ( actionText != null ) {
                actionTextBuilder = new StringBuilder(actionText);
                actionText = null;
            }

            // At 0 : (actionText == null), (actionTextBuilder == null)
            // At 1+: (actionText == null), (actionTextBuilder != null)

            if ( actionTextBuilder != null ) {
                actionTextBuilder.append('_');
                actionTextBuilder.append(action);
            } else {
                actionText = '_' + action;
            }
            
            // At 0 : (actionText != null), (actionTextBuilder == null)
            // At 1+: (actionText == null), (actionTextBuilder != null)
        }

        if ( actionText != null ) {
            return actionText;
        } else {
            return actionTextBuilder.toString();
        }
    }

    //

    /**
     * Tell if a test class should be run based on the active repeat
     * action any any {@link SkipForRepeat} annotations present on
     * the test class.
     * 
     * If a repeat action is active, the test class should be skipped if
     * a {@link SkipForRepeat} annotation is present that specifies
     * that repeat action.
     * 
     * Additional filtering is done on test methods,
     * by {@link #shouldRun(FrameworkMethod)}.
     *
     * @param testClass The test class which is to be tested.
     *
     * @return True or false telling if the test class should be run.
     */
    public static boolean shouldRun(Class<?> testClass) {
        // Not using repeat actions.  Repeat action filtering does not apply.
        if ( !isAnyRepeatActionActive() ) {
            return true;
        }

        // No skip annotation: Run for this repeat action.
        SkipForRepeat anno = testClass.getAnnotation(SkipForRepeat.class);
        if ( anno == null ) {
            return true;
        }
        
        // This should not happen.  Ignore the repeat action, but emit
        // a warning so the annotation can be fixed.
        if ( anno.value().length == 0 ) {
            Log.warning(c, "Test class " + testClass.getName() + " has empty SkipForRepeat annotation");
            return true;
        }

        // Register that the test class requires the repeat action.
        FATRunner.requireFATRunner( testClass.getName() );

        for ( String skipAction : anno.value() ) {
            if ( isRepeatActionActive(skipAction) ) {
                Log.info(c, "shouldRun", "Skipping test class " + testClass.getName() + " on action " + skipAction);
                return false;
            }
        }

        return true;
    }

    /**
     * Tell if a test method should be run based on the active repeat
     * action any any {@link SkipForRepeat} annotations present on
     * the test method.
     * 
     * If a repeat action is active, the test method should be skipped if
     * a {@link SkipForRepeat} annotation is present that specifies
     * that repeat action.
     * 
     * Class level filtering is handled separately, in {@link #shouldRun(Class)}.
     *
     * @param method The test method which is to be tested.
     *
     * @return True or false telling if the test method should be run.
     */
    public static boolean shouldRun(FrameworkMethod method) {
        // Not using repeat actions.  Repeat action filtering does not apply.
        if ( !isAnyRepeatActionActive() ) {
            return true;
        }

        SkipForRepeat anno = method.getMethod().getAnnotation(SkipForRepeat.class);

        // No skip annotation: Run for this repeat action.
        if ( anno == null ) {
            return true;
        }

        // This should not happen.  Ignore the repeat action, but emit
        // a warning so the annotation can be fixed.
        if ( anno.value().length == 0 ) {
            Log.warning(c, "Test class " + method.getName() + " of " + method.getClass().getName() + " has empty SkipForRepeat annotation");
            return true;
        }

        for ( String action : anno.value() ) {
            if ( isRepeatActionActive(action) ) {
                Log.info(c, "shouldRun", "Skipping test method " + method.getName() + " of " + method.getClass().getCanonicalName() + " on action " + action);
                return false;
            }
        }

        return true;
    }
}
