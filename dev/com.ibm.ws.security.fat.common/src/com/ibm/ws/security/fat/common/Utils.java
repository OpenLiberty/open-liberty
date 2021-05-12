/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.ibm.websphere.simplicity.log.Log;

public class Utils {

    private final static Class<?> thisClass = Utils.class;

    /**
     * Randomly chooses and returns one of the provided options.
     * 
     * @param options
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getRandomSelection(List<T> options) {
        return (T) getRandomSelection(options.toArray(new Object[options.size()]));
    }

    /**
     * Randomly chooses and returns one of the provided options.
     * 
     * @param options
     * @return
     */
    public static <T> T getRandomSelection(T... options) {
        String methodName = "getRandomSelection";

        if (options == null || options.length == 0) {
            Log.info(thisClass, methodName, "No options provided to random selection, returning null");
            return null;
        }
        if (options.length == 1) {
            Log.info(thisClass, methodName, "Only one option provided to random selection, returning it: [" + options[0] + "]");
            return options[0];
        }

        Random rand = new Random();
        Integer index = rand.nextInt(options.length);

        T entry = options[index];
        Log.info(thisClass, methodName, "Chose random selection: [" + ((entry == null) ? null : entry) + "]");
        return entry;
    }

    /**
     * Randomly chooses and returns one of the provided array options.
     * 
     * @param options
     * @return
     */
    public static <T> T[] getRandomSelection(T[]... options) {
        String methodName = "getRandomSelection";

        if (options == null || options.length == 0) {
            Log.info(thisClass, methodName, "No options provided to random selection, returning null");
            return null;
        }
        if (options.length == 1) {
            Log.info(thisClass, methodName, "Only one option provided to random selection, returning it: [" + options[0] + "]");
            return options[0];
        }

        Random rand = new Random();
        Integer index = rand.nextInt(options.length);

        T[] entry = options[index];
        Log.info(thisClass, methodName, "Chose random selection: " + ((entry == null) ? null : Arrays.toString(entry)));
        return entry;
    }

    /**
     * Return value of env var
     * 
     * @param env variable name
     * @return
     */
    public static String getEnvVar(String varName) throws Exception {
        String methodName = "getEnvVar";

        Map<String, String> env = System.getenv();

        String value = env.get(varName);
        Log.info(thisClass, methodName, "Returning env var: " + varName + " with value: " + value);
        return value;
    }
}
