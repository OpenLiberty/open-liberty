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
package javax.faces.event;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.MethodNotFoundException;
import javax.faces.component.StateHolder;
import javax.faces.context.FacesContext;

/**
 * See Javadoc of <a href="https://javaserverfaces.dev.java.net/nonav/docs/2.0/javadocs/javax/faces/event/
 * MethodExpressionValueChangeListener.html">JSF Specification</a>
 */
public class MethodExpressionValueChangeListener implements ValueChangeListener, StateHolder
{

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
    private static final Object[] EMPTY_PARAMS = new Object[0];
    
    private MethodExpression methodExpressionOneArg;
    private MethodExpression methodExpressionZeroArg;
    private boolean isTransient = false;

    /** Creates a new instance of MethodExpressionValueChangeListener */
    public MethodExpressionValueChangeListener()
    {
        // constructor for state-saving 
    }

    public MethodExpressionValueChangeListener(MethodExpression methodExpressionOneArg)
    {
        this.methodExpressionOneArg = methodExpressionOneArg;
        
        _createZeroArgsMethodExpression(methodExpressionOneArg); 
    }

    public MethodExpressionValueChangeListener(MethodExpression methodExpressionOneArg,
                                               MethodExpression methodExpressionZeroArg)
    {
        this.methodExpressionOneArg = methodExpressionOneArg;
        if (methodExpressionZeroArg != null) 
        {
            this.methodExpressionZeroArg = methodExpressionZeroArg;
        }
        else
        {
            _createZeroArgsMethodExpression(methodExpressionOneArg);
        }
    }

    public void processValueChange(ValueChangeEvent event) throws AbortProcessingException
    {
        try
        {
            try
            {
                // call to the one argument MethodExpression
                Object[] params = new Object[] { event };
                methodExpressionOneArg.invoke(getElContext(), params);
            }
            catch (MethodNotFoundException mnfe)
            {
                // call to the zero argument MethodExpression
                methodExpressionZeroArg.invoke(getElContext(), EMPTY_PARAMS);
            }
        }
        catch (ELException e)
        {
            // "... If that fails for any reason, throw an AbortProcessingException,
            // including the cause of the failure ..."
            // -= Leonardo Uribe =- after discussing this topic on MYFACES-3199, the conclusion is the part is an advice
            // for the developer implementing a listener in a method expressions that could be wrapped by this class.
            // The spec wording is poor but, to keep this coherently with ExceptionHandler API,
            // the spec and code on UIViewRoot we need:
            // 2a) "exception is instance of APE or any of the causes of the exception are an APE, 
            // DON'T publish ExceptionQueuedEvent and terminate processing for current event".
            // 2b) for any other exception publish ExceptionQueuedEvent and continue broadcast processing.
            Throwable cause = e.getCause();
            AbortProcessingException ape = null;
            if (cause != null)
            {
                do
                {
                    if (cause instanceof AbortProcessingException)
                    {
                        ape = (AbortProcessingException) cause;
                        break;
                    }
                    cause = cause.getCause();
                }
                while (cause != null);
            }
            
            if (ape != null)
            {
                // 2a) "exception is instance of APE or any of the causes of the exception are an APE, 
                // DON'T publish ExceptionQueuedEvent and terminate processing for current event".
                // To do this throw an AbortProcessingException here, later on UIViewRoot.broadcastAll,
                // this exception will be received and stored to handle later.
                throw ape;
            }
            //for any other exception publish ExceptionQueuedEvent and continue broadcast processing.
            throw e;
            //Throwable cause = e.getCause();
            //if (cause == null)
            //{
            //    cause = e;
            //}
            //if (cause instanceof AbortProcessingException)
            //{
            //    throw (AbortProcessingException) cause;
            //}
            //else
            //{
            //    throw new AbortProcessingException(cause);
            //}
        }
    }

    public void restoreState(FacesContext context, Object state)
    {
        methodExpressionOneArg = (MethodExpression) ((Object[]) state)[0];
        methodExpressionZeroArg = (MethodExpression) ((Object[]) state)[1];
    }

    public Object saveState(FacesContext context)
    {
        return new Object[] {methodExpressionOneArg, methodExpressionZeroArg};
    }

    public void setTransient(boolean newTransientValue)
    {
        isTransient = newTransientValue;
    }

    public boolean isTransient()
    {
        return isTransient;
    }
    
    private ELContext getElContext()
    {
        return getFacesContext().getELContext();
    }
    
    private FacesContext getFacesContext()
    {
        return FacesContext.getCurrentInstance();
    }
    
    /**
     * Creates a {@link MethodExpression} with no params and with the same Expression as 
     * param <code>methodExpression</code>
     * <b>WARNING!</b> This method creates new {@link MethodExpression} with expressionFactory.createMethodExpression.
     * That means is not decorating MethodExpression passed as parameter -
     * support for EL VariableMapper will not be available!
     * This is a problem when using facelets and <ui:decorate/> with EL params (see MYFACES-2541 for details).
     */
    private void _createZeroArgsMethodExpression(MethodExpression methodExpression)
    {
        ExpressionFactory expressionFactory = getFacesContext().getApplication().getExpressionFactory();

        this.methodExpressionZeroArg = expressionFactory.createMethodExpression(getElContext(), 
                  methodExpression.getExpressionString(), Void.class, EMPTY_CLASS_ARRAY);
    }

}
