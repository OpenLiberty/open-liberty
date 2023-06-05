/*******************************************************************************
 * Copyright 2022 Red Hat, Inc, and individual contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
/*
 * Liberty changes are enclosed by LIBERTY CHANGE START and LIBERTY CHANGE END
 */
package io.smallrye.openapi.api.util;

import io.smallrye.openapi.runtime.util.ModelUtil;

public final class VersionUtil {

    private VersionUtil() {
    }

    // LIBERTY CHANGE START
    // Hard code the version of the MP spec that is being used rather than trying to read it from the maven metadata in the API jar
    static final String MP_VERSION = "3.1";
    // LIBERTY CHANGE END

    static final String[] MP_VERSION_COMPONENTS = ModelUtil.supply(() -> {
        int suffix = MP_VERSION.indexOf('-');
        return (suffix > -1 ? MP_VERSION.substring(0, suffix) : MP_VERSION).split("\\.");
    });

    public static int compareMicroProfileVersion(String checkVersion) {
        String[] checkComponents = checkVersion.split("\\.");
        int max = Math.max(MP_VERSION_COMPONENTS.length, checkComponents.length);
        int result = 0;

        for (int i = 0; i < max; i++) {
            int mp = component(MP_VERSION_COMPONENTS, i);
            int cv = component(checkComponents, i);

            if ((result = Integer.compare(mp, cv)) != 0) {
                break;
            }
        }

        return result;
    }

    static int component(String[] components, int offset) {
        return offset < components.length ? Integer.parseInt(components[offset]) : 0;
    }
}