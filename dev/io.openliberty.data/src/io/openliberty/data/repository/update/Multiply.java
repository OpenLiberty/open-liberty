/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.repository.update;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.openliberty.data.repository.Count;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.Select;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Save;

/**
 * <p>Annotates a repository method parameter to indicate multiplying
 * the corresponding entity attribute by the parameter's value.</p>
 *
 * <p>Example query:</p>
 *
 * <pre>
 * boolean adjustPriceByRate({@literal @Multiply("price")} float rate,
 *                           long productId);
 * </pre>
 *
 * <p>Example usage:</p>
 *
 * <pre>
 * successful = products.adjustPriceByRate(1.05f, productId);
 * </pre>
 *
 * <p>Do not annotate a method parameter with more than one annotation
 * from the {@link io.openliberty.data.repository.update} package.</p>
 *
 * <p>A parameter that is annotated with {@code Multiply} must not be used in
 * combination with {@link Count}, {@link Delete}, {@link Exists}, {@link Insert},
 * {@link OrderBy}, {@link Query}, {@link Save}, or {@link Select} annotations
 * or on {@code countBy}, {@code deleteBy}, {@code existsBy},
 * {@code find...By}, or {@code updateBy} methods.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Multiply {
    /**
     * <p>The name of the entity attribute to multiply by the parameter's value.</p>
     *
     * <p>The default indicates to infer the entity attribute name from the method
     * parameter name, which requires compiling with the {@code -parameters}
     * compiler option that makes parameter names available at run time.</p>
     *
     * @return the name of the entity attribute to multiply by the parameter's value.
     */
    String value() default "";
}
