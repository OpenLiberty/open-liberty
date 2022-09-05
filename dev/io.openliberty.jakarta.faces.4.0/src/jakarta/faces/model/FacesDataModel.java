/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package jakarta.faces.model;

import jakarta.enterprise.util.AnnotationLiteral;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.inject.Qualifier;

/**
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Qualifier
public @interface FacesDataModel
{
    public java.lang.Class<?> forClass() default Object.class;

    /**
     * @since 4.0
     */
    public static final class Literal extends AnnotationLiteral<FacesDataModel> implements FacesDataModel
    {
        private static final long serialVersionUID = 1L;

        public static final Literal INSTANCE = of(Object.class);

        private final Class<?> forClass;

        public static Literal of(Class<?> forClass)
        {
            return new Literal(forClass);
        }

        private Literal(Class<?> forClass)
        {
            this.forClass = forClass;
        }

        @Override
        public Class<?> forClass()
        {
            return forClass;
        }
    }
}
