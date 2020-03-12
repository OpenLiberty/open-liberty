/*
 * Copyright (c) 2012, 2019 Oracle and/or its affiliates and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package jakarta.el;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates a parameterized {@link ValueExpression}.
 *
 * <p>
 * A <code>LambdaExpression</code> is a representation of the Jakarta Expression Language Lambda expression syntax. It
 * consists of a list of the formal parameters and a body, represented by a {@link ValueExpression}. The body can be any
 * valid <code>Expression</code>, including another <code>LambdaExpression</code>.
 *
 * <p>
 * A <code>LambdaExpression</code> is created when an Jakarta Expression Language expression containing a Lambda
 * expression is evaluated.
 *
 * <p>
 * A <code>LambdaExpression</code> can be invoked by calling {@link LambdaExpression#invoke}, with an
 * {@link ELContext} and a list of the actual arguments. Alternately, a <code>LambdaExpression</code> can be
 * invoked without passing a <code>ELContext</code>, in which case the <code>ELContext</code> previously set by calling
 * {@link LambdaExpression#setELContext} will be used. The evaluation of the <code>ValueExpression</code> in the body
 * uses the {@link ELContext} to resolve references to the parameters, and to evaluate the lambda expression. The result
 * of the evaluation is returned.
 *
 * @see ELContext#getLambdaArgument
 * @see ELContext#enterLambdaScope
 * @see ELContext#exitLambdaScope
 */
public class LambdaExpression {

    private List<String> formalParameters = new ArrayList<>();
    private ValueExpression expression;
    private ELContext context;
    // Arguments from nesting lambdas, when the body is another lambda
    private Map<String, Object> envirArgs;

    /**
     * Creates a new LambdaExpression.
     *
     * @param formalParameters The list of String representing the formal parameters.
     * @param expression The <code>ValueExpression</code> representing the body.
     */
    public LambdaExpression(List<String> formalParameters, ValueExpression expression) {
        this.formalParameters = formalParameters;
        this.expression = expression;
        this.envirArgs = new HashMap<>();
    }

    /**
     * Set the ELContext to use in evaluating the LambdaExpression. The ELContext must to be set prior to the invocation of
     * the LambdaExpression, unless it is supplied with {@link LambdaExpression#invoke}.
     *
     * @param context The ELContext to use in evaluating the LambdaExpression.
     */
    public void setELContext(ELContext context) {
        this.context = context;
    }

    /**
     * Invoke the encapsulated Lambda expression.
     * <p>
     * The supplied arguments are matched, in the same order, to the formal parameters. If there are more arguments than the
     * formal parameters, the extra arguments are ignored. If there are less arguments than the formal parameters, an
     * <code>ELException</code> is thrown.
     * </p>
     *
     * <p>
     * The actual Lambda arguments are added to the ELContext and are available during the evaluation of the Lambda
     * expression. They are removed after the evaluation.
     * </p>
     *
     * @param elContext The ELContext used for the evaluation of the expression The ELContext set by {@link #setELContext}
     * is ignored.
     * @param args The arguments to invoke the Lambda expression. For calls with no arguments, an empty array must be
     * provided. A Lambda argument can be <code>null</code>.
     * @return The result of invoking the Lambda expression
     * @throws ELException if not enough arguments are provided
     * @throws NullPointerException is elContext is null
     */
    public Object invoke(ELContext elContext, Object... args) throws ELException {
        int i = 0;
        Map<String, Object> lambdaArgs = new HashMap<>();

        // First get arguments injected from the outter lambda, if any
        lambdaArgs.putAll(envirArgs);

        for (String fParam : formalParameters) {
            if (i >= args.length) {
                throw new ELException("Expected Argument " + fParam + " missing in Lambda Expression");
            }
            lambdaArgs.put(fParam, args[i++]);
        }

        elContext.enterLambdaScope(lambdaArgs);
        try {
            Object ret = expression.getValue(elContext);

            // If the result of evaluating the body is another LambdaExpression,
            // whose body has not been evaluated yet. (A LambdaExpression is
            // evaluated iff when its invoke method is called.) The current lambda
            // arguments may be needed in that body when it is evaluated later,
            // after the current lambda exits. To make these arguments available
            // then, they are injected into it.
            if (ret instanceof LambdaExpression) {
                ((LambdaExpression) ret).envirArgs.putAll(lambdaArgs);
            }
            return ret;
        } finally {
            elContext.exitLambdaScope();
        }


    }

    /**
     * Invoke the encapsulated Lambda expression.
     * <p>
     * The supplied arguments are matched, in the same order, to the formal parameters. If there are more arguments than the
     * formal parameters, the extra arguments are ignored. If there are less arguments than the formal parameters, an
     * <code>ELException</code> is thrown.
     * </p>
     *
     * <p>
     * The actual Lambda arguments are added to the ELContext and are available during the evaluation of the Lambda
     * expression. They are removed after the evaluation.
     * </p>
     *
     * The ELContext set by {@link LambdaExpression#setELContext} is used in the evaluation of the lambda Expression.
     *
     * @param args The arguments to invoke the Lambda expression. For calls with no arguments, an empty array must be
     * provided. A Lambda argument can be <code>null</code>.
     * @return The result of invoking the Lambda expression
     * @throws ELException if not enough arguments are provided
     */
    public Object invoke(Object... args) {
        return invoke(context, args);
    }
}
