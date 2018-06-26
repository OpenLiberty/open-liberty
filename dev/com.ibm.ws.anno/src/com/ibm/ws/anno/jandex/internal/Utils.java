/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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
 */

package com.ibm.ws.anno.jandex.internal;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Common utilities
 *
 * @author Jason T. Greene
 */
class Utils {
    private static Charset UTF8 = Charset.forName("UTF-8");

    static byte[] toUTF8(String string) {
        return string.getBytes(UTF8);
    }

    static String fromUTF8(byte[] bytes) {
        return new String(bytes, UTF8);
    }

    static <T> List<T> emptyOrWrap(List<T> list) {
        return list.size() == 0 ? Collections.<T>emptyList() : Collections.unmodifiableList(list);
    }

    static <T> Collection<T> emptyOrWrap(Collection<T> list) {
        return list.size() == 0 ? Collections.<T>emptyList() : Collections.unmodifiableCollection(list);
    }

    static <K, V> Map<K, V> emptyOrWrap(Map<K, V> map) {
        return map.size() == 0 ? Collections.<K, V>emptyMap() : Collections.unmodifiableMap(map);
    }
}
