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
package jakarta.faces.validator;

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
public @interface FacesValidator
{
    /**
     * The value of this annotation attribute is taken to be the <i>validator-id</i> with which instances of this class
     * of component can be instantiated by calling 
     * {@link jakarta.faces.application.Application#createValidator(java.lang.String)}.
     */
    public String value() default "";
    
    /**
     * If it is true, the validator is added to default list calling  
     * {@link jakarta.faces.application.Application#addDefaultValidatorId(java.lang.String)}.
     */
    boolean isDefault() default false;
    
    public boolean managed() default false;

    /*
     * @since 4.0
     */
    public static final class Literal extends AnnotationLiteral<FacesValidator> implements FacesValidator
    {
        private static final long serialVersionUID = 1L;

        public static final Literal INSTANCE = of("", false, false);

        private final String value;
        private final boolean isDefault;
        private final boolean managed;

        public static Literal of(String value, boolean isDefault, boolean managed)
        {
            return new Literal(value, isDefault, managed);
        }

        private Literal(String value, boolean isDefault, boolean managed)
        {
            this.value = value;
            this.isDefault = isDefault;
            this.managed = managed;
        }

        @Override
        public String value()
        {
            return value;
        }

        @Override
        public boolean isDefault()
        {
            return isDefault;
        }

        @Override
        public boolean managed()
        {
            return managed;
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 59 * hash + Objects.hashCode(this.value);
            hash = 59 * hash + (this.isDefault ? 1 : 0);
            hash = 59 * hash + (this.managed ? 1 : 0);
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
            if (this.isDefault != other.isDefault)
            {
                return false;
            }
            if (this.managed != other.managed)
            {
                return false;
            }
            return Objects.equals(this.value, other.value);
        }
        
        
    }
}
