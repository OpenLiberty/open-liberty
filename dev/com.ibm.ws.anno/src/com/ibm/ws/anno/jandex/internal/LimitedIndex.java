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

package com.ibm.ws.anno.jandex.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;


public final class LimitedIndex{


    final Map<DotName, ClassInfo> classes;


    LimitedIndex(Map<DotName,ClassInfo> classes){
        this.classes = classes;
    }

    public Set<DotName> classNames(){
        return classes.keySet();
    }

    public Collection<ClassInfo> classes(){
        return classes.values();
    }

    public Collection<ClassInfo> getKnownClasses(){
        return classes();
    }

    public ClassInfo getClass(DotName className){
        return classes.get(className);
    }
    
}

