/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.custom.junit.runner;

import java.util.logging.Logger;

import org.junit.runners.model.FrameworkMethod;

import componenttest.annotation.SkipForRepeat;

public class RepeatTestFilter {

    private static Logger log = Logger.getLogger(RepeatTestFilter.class.getName());

    public static String CURRENT_REPEAT_ACTION = null;

    public static boolean shouldRun(FrameworkMethod method) {
        //if we're not repeating then there is no point checking the SkipForRepeat annotation; always run
        if (CURRENT_REPEAT_ACTION == null) {
            return true;
        }

        SkipForRepeat anno = method.getMethod().getAnnotation(SkipForRepeat.class);

        // No annotation at method or class level, so always run
        if (anno == null || anno.value().length == 0)
            return true;

        for (String action : anno.value()) {
            if (CURRENT_REPEAT_ACTION.equals(action)) {
                log.info("Skipping test method " + method.getName() + " on action " + action);
                return false;
            }
        }
        return true;

    }

    public static boolean shouldRun(Class<?> clazz) {
        //if we're not repeating then there is no point checking the SkipForRepeat annotation; always run
        if (CURRENT_REPEAT_ACTION == null) {
            return true;
        }

        SkipForRepeat anno = clazz.getAnnotation(SkipForRepeat.class);

        // No annotation at class level, so always run
        if (anno == null || anno.value().length == 0)
            return true;

        FATRunner.requireFATRunner(clazz.getName());

        for (String action : anno.value()) {
            if (CURRENT_REPEAT_ACTION.equals(action)) {
                log.info("Skipping test class " + clazz.getName() + " on action " + action);
                return false;
            }
        }
        return true;
    }

}
