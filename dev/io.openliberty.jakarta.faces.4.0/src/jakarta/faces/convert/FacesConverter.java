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
package jakarta.faces.convert;

import jakarta.enterprise.util.AnnotationLiteral;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.inject.Qualifier;
import java.util.Objects;

/**
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Inherited
@Qualifier
public @interface FacesConverter
{
    /**
     * This attribute is an alternative for providing the &lt;converter-for-class&gt; element in XML.
     *
     * @return The class this converter must be registered for.
     */
    public Class forClass() default Object.class;

    /**
     * The converter id. Alternative for providing the &lt;converter-id&gt; element in XML.
     * @return
     */
    public String value() default "";
    
    public boolean managed() default false;

    /**
     * @since 4.0
     */
    public static final class Literal extends AnnotationLiteral<FacesConverter> implements FacesConverter
    {
        private static final long serialVersionUID = 1L;

        public static final Literal INSTANCE = of("", Object.class, false);

        private final String value;
        private final Class forClass;
        private final boolean managed;

        public static Literal of(String value, Class forClass, boolean managed)
        {
            return new Literal(value, forClass, managed);
        }

        private Literal(String value, Class forClass, boolean managed)
        {
            this.value = value;
            this.forClass = forClass;
            this.managed = managed;
        }

        @Override
        public String value() 
        {
            return value;
        }

        @Override
        public Class forClass()
        {
            return forClass;
        }

        @Override
        public boolean managed()
        {
            return managed;
        }

        @Override
        public int hashCode()
        {
            int hash = 3;
            hash = 17 * hash + Objects.hashCode(this.value);
            hash = 17 * hash + Objects.hashCode(this.forClass);
            hash = 17 * hash + (this.managed ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            final Literal other = (Literal) obj;
            if (this.managed != other.managed)
            {
                return false;
            }
            if (!Objects.equals(this.value, other.value))
            {
                return false;
            }
            return Objects.equals(this.forClass, other.forClass);
        }

        
    }
}
