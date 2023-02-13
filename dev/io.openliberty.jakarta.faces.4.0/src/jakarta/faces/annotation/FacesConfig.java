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
package jakarta.faces.annotation;

import jakarta.enterprise.util.AnnotationLiteral;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

/**
 *
 */
@Qualifier
@Target(value=ElementType.TYPE)
@Retention(value=RetentionPolicy.RUNTIME)
public @interface FacesConfig
{
    @Deprecated(forRemoval = true, since = "4.0")
    @Nonbinding Version version() default Version.JSF_2_3;

    @Deprecated(forRemoval = true, since = "4.0")
    public static enum Version 
    {
        JSF_2_3
    }
    
    public static final class Literal extends AnnotationLiteral<FacesConfig> implements FacesConfig
    {
        private static final long serialVersionUID = 1L;

        public static final Literal INSTANCE = new Literal();

        @Override
        public Version version()
        {
            return Version.JSF_2_3;
        }
    }
}
