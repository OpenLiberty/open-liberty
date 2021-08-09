/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.ras;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The <code>TraceOptions</code> annotation can be used to declare which trace
 * group a class (or classes in a package) should be asociated with. The
 * annotation can also be used to declare whether or not debug traces should be
 * cut when exceptions are explicitly thrown or caught.
 * <p>
 * For example:<br>
 *
 * <pre>
 * &#064;TraceOptions(traceGroup = &quot;MyTraceGroup&quot;, traceExceptionThrow = true)
 * public class Foo {}
 * </pre>
 *
 * will associate the class <code>Foo</code> with the <code>MyTraceGroup</code>
 * trace group and will cause debug traces to be added whenever an exception is
 * explicitly thrown. <br>
 * or:<br>
 *
 * <pre>
 * &#064;TraceOptions(traceGroups = { &quot;BarGroup&quot;, &quot;FooGroup&quot; }, messageBundle = &quot;com.ibm.bar&quot;)
 * public class Bar {}
 * </pre>
 *
 * will associate the class <code>Bar</code> with the trace groups
 * <code>BarGroup</code> and <code>FooGroup</code> if the underlying trace
 * runtime supports multiple groups. If not, only the first trace group listed
 * will be used. The message bundle "com.ibm.bar" will be used for messages.
 */
@Retention(RUNTIME)
@Target({ TYPE, PACKAGE })
public @interface TraceOptions {
    String traceGroup() default "";

    String[] traceGroups() default {};

    String messageBundle() default "";

    boolean traceExceptionThrow() default false;

    boolean traceExceptionHandling() default false;
}
