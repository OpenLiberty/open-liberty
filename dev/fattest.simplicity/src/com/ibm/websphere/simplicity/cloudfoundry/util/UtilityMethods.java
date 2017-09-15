/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.cloudfoundry.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public final class UtilityMethods {

    public static void main(String[] args) {
        String[] result = findMatchingSubstrings("^([^\\s\"]+|\"([^\"]*)\")", "\"C:\\Program Files (x86)\\CloudFoundry\\gcf.exe\" push");
        for (String string : result) {
            System.out.println(string);
        }
    }

    public static String[] findMatchingSubstrings(String regex, String text) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        ArrayList<String> matches = new ArrayList<String>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches.toArray(new String[matches.size()]);
    }

}
