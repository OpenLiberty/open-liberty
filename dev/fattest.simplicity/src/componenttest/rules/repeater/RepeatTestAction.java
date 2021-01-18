/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

import org.junit.runners.model.Statement;

/**
 * API for repeat test actions.
 */
public interface RepeatTestAction {
    /**
     * Tell if this repeat action is enabled.
     * 
     * When enabled, {@link #setup()} will be invoked immediately
     * before running tests with the repeat action.  Additionally,
     * {@link componenttest.custom.junit.runner.RepeatTestFilter#activateRepeatAction(String)}
     * will be invoked with the repeat test action.  That informs
     * test filtering of the active repeat action.
     *
     * If a repeat action is not enabled, a log message should be emitted
     * which explains why the action is not enabled.
     * 
     * @return True or false telling if this repeat action is enabled. 
     */
    boolean isEnabled();

    /**
     * Invoked by the FAT framework to perform setup steps before repeating the tests.
     * 
     * @throws Exception Thrown in case test setup fails.
     */
    void setup() throws Exception;

    /**
     * Answer the identifier of this repeat action.  Used by
     * {@link componenttest.annotation.SkipForRepeat} to specify what repeat
     * test action is to be skipped.
     * 
     * @return The identifier of this repeat action.
     */
    String getID();
    
    /**
     * Evaluate a statement in the context of this repeat action.
     *
     * @param statement The statement to evaluate.
     * 
     * @throws Throwable Thrown by the statement.
     */
    void evaluate(Statement statement) throws Throwable;
}
