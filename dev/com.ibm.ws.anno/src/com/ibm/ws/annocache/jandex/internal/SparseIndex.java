/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
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
 * 
 * All modifications made by IBM from initial source -
 * https://github.com/wildfly/jandex/blob/master/src/main/java/org/jboss/jandex/Index.java
 * commit - f17e60cbb362ba2563e2a8128a53b3b492548393
 */
package com.ibm.ws.annocache.jandex.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public final class SparseIndex {
    public SparseIndex(Map<? extends SparseDotName, ? extends SparseClassInfo> classes){
        this.classes = classes;
    }

    //

    private final Map<? extends SparseDotName, ? extends SparseClassInfo> classes;

    public Set<? extends SparseDotName> classNames(){
        return classes.keySet();
    }

    public Collection<? extends SparseClassInfo> classes(){
        return classes.values();
    }

    public Collection<? extends SparseClassInfo> getKnownClasses(){
        return classes();
    }

    public SparseClassInfo getClass(SparseDotName className){
        return classes.get(className);
    }
}
