/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.jaxrs.impl;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;

final class Rfc3986UriValidator {
    private static final String SCHEME = "(?i)(http|https):";

    private static final String USERINFO = "([^@\\[/?#]*)";

    private static final String HOST = "([^/?#]*)";

    private static final String PATH = "([^?#]*)";

    private static final String QUERY = "([^#]*)";

    private static final String LAST = "#(.*)";

    private static final Pattern HTTP_URL = Pattern.compile("^" + SCHEME 
        + "(//(" + USERINFO + "@)?" + HOST  + ")?" + PATH
        + "(\\?" + QUERY + ")?" + "(" + LAST + ")?");

    private Rfc3986UriValidator() {
    }

    /**
     * Validate the HTTP URL according to https://datatracker.ietf.org/doc/html/rfc3986#appendix-B  
     * @param uri HTTP schemed URI to validate
     * @return "true" if URI matches RFC-3986 validation rules, "false" otherwise
     */
    public static boolean validate(final URI uri) {
        // Only validate the HTTP(s) URIs
        if (HttpUtils.isHttpScheme(uri.getScheme())) { 
            final Matcher matcher = HTTP_URL.matcher(uri.toString());
            if (matcher.matches()) {
                final String host = matcher.group(5);
                // There is no host component in the HTTP URI, it is required
                return !(StringUtils.isEmpty(host));
            } else {
                return false;
            }
        } else {
            // not HTTP URI, skipping
            return true;
        }
    }
}
