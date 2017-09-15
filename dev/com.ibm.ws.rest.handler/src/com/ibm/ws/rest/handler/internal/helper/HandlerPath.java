/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.internal.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class represents a registered path from a RESTHandler. These paths may contain variable placeholders, such as /myURL/{var1}/abc.
 * 
 * The "length" of a registered path represents the amount of characters outside of a variable boundary {..}
 * 
 * Example: /myURL/other/abc has length of 16, while /myURL/{var1}/abc has length of 11.
 */
public class HandlerPath {

    private static final TraceComponent tc = Tr.register(HandlerPath.class);

    final String registeredPath;
    final public Pattern pattern;
    int length;
    final boolean hidden;

    /**
     * Assuming that the container will not pass any null or empty path's
     */
    public HandlerPath(String registeredPath) {
        this(registeredPath, false);
    }

    /**
     * Assuming that the container will not pass any null or empty path's
     */
    public HandlerPath(String registeredPath, boolean hidden) {
        this.hidden = hidden;

        //Remove possible trailing slashes
        if (registeredPath.charAt(registeredPath.length() - 1) == '/') {
            registeredPath = registeredPath.substring(0, registeredPath.length() - 1);
        }

        this.registeredPath = registeredPath;
        this.length = -1; //lazily calculated

        //Check if we have a variable in our path
        if (registeredPath.contains("{")) {
            //Yes, so compile a pattern
            pattern = processPattern(registeredPath);
        } else {
            //No, so leave pattern at null
            this.pattern = null;
        }
    }

    public boolean containsVariable() {
        return pattern != null;
    }

    public String getRegisteredPath() {
        return registeredPath;
    }

    public int length() {
        if (length == -1) {
            if (pattern == null) {
                length = registeredPath.length();
            } else {
                //need to compute the length of characters that aren't inside the variable {..} sections,
                //so we remove all the variable sections and then count the characters
                length = registeredPath.replaceAll("(\\{\\w*\\})", "").length();
            }
        }

        return length;
    }

    public boolean matches(String url) {
        if (pattern == null) {
            //Our registered root did not contain variables.
            //First check: the incoming URL must start with our registered root
            if (!url.startsWith(registeredPath)) {
                return false;
            }

            //Second check: immediate characters allowed are only '/' (for subpath) or '?' (for query param)
            // Grabbing the charAt here is safe because if we were an exact length match,
            // we would have been caught in the initial direct hit check.
            char nextChar = url.charAt(registeredPath.length());
            if (nextChar != '/' && nextChar != '?') {
                return false;
            }

            return true;

        } else {
            //Matching against our compiled pattern
            return pattern.matcher(url).matches();
        }
    }

    /**
     * This method assume that the incoming matchedPath returns "true" for a call to matches(String)
     */
    public Map<String, String> mapVariables(String matchedPath) {
        if (pattern != null) {
            //For each variable inside our registered path we will need to map it to the corresponding value in the matchedURL
            Map<String, String> map = new HashMap<String, String>();

            //Build matchers so we can use their matched groups
            Matcher matcherValue = pattern.matcher(matchedPath);
            Matcher matcherKey = pattern.matcher(registeredPath);
            matcherValue.matches();
            matcherKey.matches();

            if (matcherValue.groupCount() != matcherKey.groupCount()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Handler path matched expression groups did not match. Matched path: " + matchedPath + " | Registered path:" + registeredPath);
                }
            }

            //Group(0) is the entire expression, so skip that one
            for (int i = 1; i <= matcherValue.groupCount(); i++) {
                String key = matcherKey.group(i);
                //Remote leading and trailing backets {..}
                key = key.substring(1, key.length() - 1);

                String value = matcherValue.group(i);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Mapping key: " + key + " to value: " + value);
                }

                //Insert into map
                map.put(key, value);
            }

            return map;
        }

        return null;
    }

    private Pattern processPattern(String registered) {
        //Replace the variable tokens with a regex that will catch the variable, and will also enforce
        //that only "/" or "?" can immediately follow the registered URL.
        String patternRegex = registered.replaceAll("(\\{\\w+\\})", "(.+)") + "[\\?/]?.*";

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Generated regex: " + patternRegex);
        }

        //Compile pattern
        return Pattern.compile(patternRegex);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof HandlerPath)) {
            return false;
        }

        HandlerPath otherPath = (HandlerPath) obj;

        return (registeredPath == otherPath.registeredPath) || (registeredPath.equals(otherPath.registeredPath));
    }

    @Override
    public int hashCode() {
        return registeredPath.hashCode();
    }

    public boolean isHidden() {
        return hidden;
    }
}
