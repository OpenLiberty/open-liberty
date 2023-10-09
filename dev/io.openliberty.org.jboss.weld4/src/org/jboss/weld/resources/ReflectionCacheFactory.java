/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.resources;

import org.jboss.weld.metadata.TypeStore;
import org.jboss.weld.util.reflection.Reflections;

public class ReflectionCacheFactory {

    private static final String HOTSPOT_MARKER = "sun.reflect.annotation.AnnotationType";

    private ReflectionCacheFactory() {
System.out.println("GREP RCF INIT");
    }

    public static ReflectionCache newInstance(TypeStore store) {
System.out.println("GREP RCF STORE");
        if (Reflections.isClassLoadable(HOTSPOT_MARKER, WeldClassLoaderResourceLoader.INSTANCE)) {
System.out.println("GREP RCF STORE HOTSPOT");
            HotspotReflectionCache hrc = new HotspotReflectionCache(store);
System.out.println("GREP RCF STORE complete");
            return hrc;
        } else {
System.out.println("GREP RCF STORE DEFAULT");
            return new DefaultReflectionCache(store);
        }
    }
}

