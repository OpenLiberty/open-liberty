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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import org.jboss.weld.exceptions.WeldException;
import org.jboss.weld.metadata.TypeStore;

/**
 * {@link ReflectionCache} implementation that works around possible deadlocks in HotSpot:
 *
 * @see https://issues.jboss.org/browse/WELD-1169
 * @see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7122142
 * @see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6588239
 *
 * @author Jozef Hartinger
 *
 */
public class HotspotReflectionCache extends DefaultReflectionCache {

    private final Class<?> annotationTypeLock;

    public HotspotReflectionCache(TypeStore store) {
        super(store);
        try {
            this.annotationTypeLock = Class.forName("sun.reflect.annotation.AnnotationType");
        } catch (ClassNotFoundException e) {
            throw new WeldException(e);
        }
    }

    @Override
    protected Annotation[] internalGetAnnotations(AnnotatedElement element) {
	try{
        	if (element instanceof Class<?>) {
        	    Class<?> clazz = (Class<?>) element;
        	    if (clazz.isAnnotation()) {
        	        synchronized (annotationTypeLock) {
        	            return element.getAnnotations();
        	        }
        	    }
        	}
        	return element.getAnnotations();
    
	} catch (Exception e) {
		System.out.println("GREP");

		if (element instanceof Class) {
	            Class<?> clazz = (Class<?>) element;

			System.out.println(clazz.getCanonicalName());

		}

		System.out.println(element.toString());
		throw e;
	}
    }

}

