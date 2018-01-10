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
package javax.faces.component;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFListener;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.el.EvaluationException;
import javax.faces.el.MethodBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PostValidateEvent;
import javax.faces.event.PreValidateEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;
import javax.faces.render.Renderer;
import javax.faces.validator.Validator;
import javax.faces.webapp.FacesServlet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * UICommand is a base abstraction for components that implement ActionSource.
 * <p>
 * See the javadoc for this class in the <a href="http://java.sun.com/j2ee/javaserverfaces/1.2/docs/api/index.html">JSF
 * Specification</a> for further details.
 * <p>
 */
@JSFComponent(defaultRendererType = "javax.faces.Text")
public class UIInput extends UIOutput implements EditableValueHolder
{
    public static final String COMPONENT_TYPE = "javax.faces.Input";
    public static final String COMPONENT_FAMILY = "javax.faces.Input";

    public static final String CONVERSION_MESSAGE_ID = "javax.faces.component.UIInput.CONVERSION";
    public static final String REQUIRED_MESSAGE_ID = "javax.faces.component.UIInput.REQUIRED";
    public static final String UPDATE_MESSAGE_ID = "javax.faces.component.UIInput.UPDATE";

    /**
     * Force validation on empty fields (By default is auto, which means it is only 
     * enabled when Bean Validation binaries are available on the current classpath).
     */
    @JSFWebConfigParam(defaultValue="auto", expectedValues="auto, true, false", since="2.0", group="validation")
    public static final String VALIDATE_EMPTY_FIELDS_PARAM_NAME = "javax.faces.VALIDATE_EMPTY_FIELDS";
    
    /** 
     * Submitted values are decoded as null values instead empty strings.
     * 
     * <p>Note this param is ignored for components extending from UISelectOne/UISelectMany.</p>
     **/
    @JSFWebConfigParam(defaultValue="false", expectedValues="true, false", since="2.0", group="validation")
    public static final String EMPTY_STRING_AS_NULL_PARAM_NAME
            = "javax.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL";

    /**
     * If set to true, validation is always performed when required is true.
     */
    @JSFWebConfigParam(defaultValue="false", expectedValues="true, false", since="2.3", group="validation")
    public static final String ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE 
            = "javax.faces.ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE";
    
    // our own, cached key
    private static final String MYFACES_EMPTY_VALUES_AS_NULL_PARAM_NAME =
      "org.apache.myfaces.UIInput.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL";
    
    /**
     * Extended debug info is stored under this key in the request
     * map for every UIInput component when in Development mode.
     * ATTENTION: this constant is duplicate in org.apache.myfaces.renderkit.ErrorPageWriter
     */
    private static final String DEBUG_INFO_KEY = "org.apache.myfaces.debug.DEBUG_INFO";
    
    private final static String BEAN_BEFORE_JSF_PROPERTY = "oam.beanBeforeJsf";

    private static final Validator[] EMPTY_VALIDATOR_ARRAY = new Validator[0];
    
    private _DeltaList<Validator> _validatorList;

    /**
     * Construct an instance of the UIInput.
     */
    public UIInput()
    {
        setRendererType("javax.faces.Text");
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }

    /**
     * Store the specified object as the "local value" of this component. The value-binding named "value" (if any) is
     * ignored; the object is only stored locally on this component. During the "update model" phase, if there is a
     * value-binding named "value" then this local value will be stored via that value-binding and the "local value"
     * reset to null.
     */
    @Override
    public void setValue(Object value)
    {
        FacesContext facesContext = getFacesContext();
        if (facesContext != null && facesContext.isProjectStage(ProjectStage.Development))
        {
            // extended debug-info when in Development mode
            _createFieldDebugInfo(facesContext, "localValue",
                    getLocalValue(), value, 1);
        }
        setLocalValueSet(true);
        super.setValue(value);
    }
    
    /**
     * Return the current value of this component.
     * <p>
     * If a submitted value has been converted but not yet pushed into the
     * model, then return that locally-cached value (see isLocalValueSet).
     * <p>
     * Otherwise, evaluate an EL expression to fetch a value from the model. 
     */
    public Object getValue()
    {
        if (isLocalValueSet())
        {
            return super.getLocalValue();
        }
        return super.getValue();
    }

    /**
     * Set the "submitted value" of this component from the relevant data in the current servlet request object.
     * <p>
     * If this component is not rendered, then do nothing; no output would have been sent to the client so no input is
     * expected.
     * <p>
     * Invoke the inherited functionality, which typically invokes the renderer associated with this component to
     * extract and set this component's "submitted value".
     * <p>
     * If this component is marked "immediate", then immediately apply validation to the submitted value found. On
     * error, call context method "renderResponse" which will force processing to leap to the "render
     * response" phase as soon as the "decode" step has completed for all other components.
     */
    @Override
    public void processDecodes(FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }
        try
        {
            setCachedFacesContext(context);
            pushComponentToEL(context, this);
            if (!isRendered())
            {
                return;
            }
        }
        finally
        {
            setCachedFacesContext(null);
            popComponentFromEL(context);
        }
        super.processDecodes(context);
        try
        {
            setCachedFacesContext(context);
            pushComponentToEL(context, this);
            if (isImmediate())
            {
                //Pre validation event dispatch for component
                context.getApplication().publishEvent(context,  PreValidateEvent.class, getClass(), this);
                try
                {
                    validate(context);
                }
                catch (RuntimeException e)
                {
                    context.renderResponse();
                    throw e;
                }
                finally
                {
                    context.getApplication().publishEvent(context,  PostValidateEvent.class, getClass(), this);
                }
                if (!isValid())
                {
                    context.renderResponse();
                }
            }
        }
        finally
        {
            setCachedFacesContext(null);
            popComponentFromEL(context);
        }
    }

    @Override
    public void processValidators(FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }
        try
        {
            setCachedFacesContext(context);
            pushComponentToEL(context, this);
            if (!isRendered())
            {
                return;
            }
        }
        finally
        {
            setCachedFacesContext(null);
            popComponentFromEL(context);
        }

        //super.processValidators(context);
        
        // Call the processValidators() method of all facets and children of this UIComponent, in the order
        // determined by a call to getFacetsAndChildren().
        int facetCount = getFacetCount();
        if (facetCount > 0)
        {
            for (UIComponent facet : getFacets().values())
            {
                facet.processValidators(context);
            }
        }

        for (int i = 0, childCount = getChildCount(); i < childCount; i++)
        {
            UIComponent child = getChildren().get(i);
            child.processValidators(context);
        }

        try
        {
            setCachedFacesContext(context);
            pushComponentToEL(context, this);
            if (!isImmediate())
            {
                //Pre validation event dispatch for component
                context.getApplication().publishEvent(context,  PreValidateEvent.class, getClass(), this);
                try
                {
                    validate(context);
                }
                catch (RuntimeException e)
                {
                    context.renderResponse();
                    throw e;
                }
                finally
                {
                    context.getApplication().publishEvent(context,  PostValidateEvent.class, getClass(), this);
                }
                if (!isValid())
                {
                    context.validationFailed();
                    context.renderResponse();
                }
            }
        }
        finally
        {
            setCachedFacesContext(null);
            popComponentFromEL(context);
        }
    }

    @Override
    public void processUpdates(FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }
        try
        {
            setCachedFacesContext(context);
            pushComponentToEL(context, this);
            if (!isRendered())
            {
                return;
            }
        }
        finally
        {
            setCachedFacesContext(null);
            popComponentFromEL(context);
        }
        super.processUpdates(context);

        try
        {
            setCachedFacesContext(context);
            pushComponentToEL(context, this);
            try
            {
                updateModel(context);
            }
            catch (RuntimeException e)
            {
                context.renderResponse();
                throw e;
            }
            if (!isValid())
            {
                context.renderResponse();
            }
        }
        finally
        {
            setCachedFacesContext(null);
            popComponentFromEL(context);
        }
    }

    @Override
    public void decode(FacesContext context)
    {
        // We (re)set to valid, so that component automatically gets (re)validated
        setValid(true);
        super.decode(context);
    }

    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException
    {
        // invoke standard listeners attached to this component first
        super.broadcast(event);

        // Check if the event is applicable for ValueChangeListener
        if (event instanceof ValueChangeEvent)
        {
            // invoke the single listener defined directly on the component
            MethodBinding valueChangeListenerBinding = getValueChangeListener();
            if (valueChangeListenerBinding != null)
            {
                try
                {
                    valueChangeListenerBinding.invoke(getFacesContext(), new Object[] { event });
                }
                catch (EvaluationException e)
                {
                    Throwable cause = e.getCause();
                    if (cause != null && cause instanceof AbortProcessingException)
                    {
                        throw (AbortProcessingException) cause;
                    }
                    else
                    {
                        throw e;
                    }
                }
            }
        }
    }

    public void updateModel(FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException();
        }
        if (!isValid())
        {
            return;
        }
        if (!isLocalValueSet())
        {
            return;
        }
        ValueExpression expression = getValueExpression("value");
        if (expression == null)
        {
            return;
        }

        try
        {
            expression.setValue(context.getELContext(), getLocalValue());
            setValue(null);
            setLocalValueSet(false);
        }
        catch (Exception e)
        {
            // Enqueue an error message
            //context.getExternalContext().log(e.getMessage(), e);
            
            // Create a FacesMessage with the id UPDATE_MESSAGE_ID
            FacesMessage facesMessage = _MessageUtils.getMessage(context,
                    context.getViewRoot().getLocale(), FacesMessage.SEVERITY_ERROR, UPDATE_MESSAGE_ID,
                    new Object[] { _MessageUtils.getLabel(context, this) });
            
            // create an UpdateModelException and enqueue it since 
            // we are not allowed to throw it directly here
            // spec javadoc: The exception must not be re-thrown. This enables tree traversal to 
            // continue for this lifecycle phase, as in all the other lifecycle phases.
            UpdateModelException updateModelException = new UpdateModelException(facesMessage, e);
            ExceptionQueuedEventContext exceptionQueuedContext 
                    = new ExceptionQueuedEventContext(context, updateModelException, this, PhaseId.UPDATE_MODEL_VALUES);
            
            // spec javadoc says we should call context.getExceptionHandler().processEvent(exceptionQueuedContext),
            // which is not just syntactically wrong, but also stupid!!
            context.getApplication().publishEvent(context, ExceptionQueuedEvent.class, exceptionQueuedContext);
            
            // Set the valid property of this UIInput to false
            setValid(false);
        }
    }

    protected void validateValue(FacesContext context, Object convertedValue)
    {
        if (!isValid())
        {
            return;
        }

        // If our value is empty, check the required property
        boolean isEmpty = isEmpty(convertedValue); 

        if (isRequired() && isEmpty)
        {
            if (getRequiredMessage() != null)
            {
                String requiredMessage = getRequiredMessage();
                context.addMessage(this.getClientId(context), new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    requiredMessage, requiredMessage));
            }
            else
            {
                _MessageUtils.addErrorMessage(context, this, REQUIRED_MESSAGE_ID,
                    new Object[] { _MessageUtils.getLabel(context, this) });
            }
            setValid(false);
            return;
        }

        if (!isEmpty || shouldValidateEmptyFields(context))
        {
            _ComponentUtils.callValidators(context, this, convertedValue);
        }
    }
    
    /**
     * Checks if the <code>validate()</code> should interpret an empty
     * submitted value should be handle as <code>NULL</code>
     * 
     * @return a (cached) boolean to identify the interpretation as null
     */
    private boolean shouldInterpretEmptyStringSubmittedValuesAsNull(FacesContext context)
    {
        ExternalContext ec = context.getExternalContext();
        Boolean interpretEmptyStringAsNull
                = (Boolean)ec.getApplicationMap().get(MYFACES_EMPTY_VALUES_AS_NULL_PARAM_NAME);

        // not yet cached...
        if (interpretEmptyStringAsNull == null)
        {
            // parses the web.xml to get the "javax.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL" value
            String param = ec.getInitParameter(EMPTY_STRING_AS_NULL_PARAM_NAME);

            // evaluate the param
            interpretEmptyStringAsNull = "true".equalsIgnoreCase(param);

            // cache the parsed value
            ec.getApplicationMap().put(MYFACES_EMPTY_VALUES_AS_NULL_PARAM_NAME, interpretEmptyStringAsNull);
        }

        return interpretEmptyStringAsNull;
    }

    /**
     * <p>Return <code>true</code> if the value is an empty <code>String</code>.</p>
     */
    private boolean isEmptyString(Object value)
    {
        return ((value instanceof String) && (((String) value).length() == 0));
    }


    private boolean shouldValidateEmptyFields(FacesContext context)
    {
        ExternalContext ec = context.getExternalContext();
        Boolean validateEmptyFields = (Boolean) ec.getApplicationMap().get(VALIDATE_EMPTY_FIELDS_PARAM_NAME);

        if (validateEmptyFields == null)
        {
             String param = ec.getInitParameter(VALIDATE_EMPTY_FIELDS_PARAM_NAME);

             // null means the same as auto.
             if (param == null)
             {
                 param = "auto";
             }
             else
             {
                 // The environment variables are case insensitive.
                 param = param.toLowerCase();
             }

             if (param.equals("auto") && _ExternalSpecifications.isBeanValidationAvailable())
             {
                 validateEmptyFields = true;
             }
             else if (param.equals("true"))
             {
                 validateEmptyFields = true;
             }
             else
             {
                 validateEmptyFields = false;
             }

             // cache the parsed value
             ec.getApplicationMap().put(VALIDATE_EMPTY_FIELDS_PARAM_NAME, validateEmptyFields);
        }

        return validateEmptyFields;
    }
    
    private boolean shouldAlwaysPerformValidationWhenRequiredTrue(FacesContext context)
    {
        ExternalContext ec = context.getExternalContext();
        Boolean alwaysPerformValidationWhenRequiredTrue = (Boolean) ec.getApplicationMap().get(
                ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE);

        if (alwaysPerformValidationWhenRequiredTrue == null)
        {
             String param = ec.getInitParameter(ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE);

             // null means the same as auto.
             if (param == null)
             {
                 param = "false";
             }
             else
             {
                 // The environment variables are case insensitive.
                 param = param.toLowerCase();
             }

             if (param.equals("true"))
             {
                 alwaysPerformValidationWhenRequiredTrue = true;
             }
             else
             {
                 alwaysPerformValidationWhenRequiredTrue = false;
             }

             // cache the parsed value
             ec.getApplicationMap().put(ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE, 
                     alwaysPerformValidationWhenRequiredTrue);
        }

        return alwaysPerformValidationWhenRequiredTrue;
    }

    /**
     * Determine whether the new value is valid, and queue a ValueChangeEvent if necessary.
     * <p>
     * The "submitted value" is converted to the necessary type; conversion failure is reported as an error and
     * validation processing terminates for this component. See documentation for method getConvertedValue for details
     * on the conversion process.
     * <p>
     * Any validators attached to this component are then run, passing the converted value.
     * <p>
     * The old value of this component is then fetched (possibly involving the evaluation of a value-binding expression,
     * ie invoking a method on a user object). The old value is compared to the new validated value, and if they are
     * different then a ValueChangeEvent is queued for later processing.
     * <p>
     * On successful completion of this method:
     * <ul>
     * <li>isValid() is true
     * <li>isLocalValueSet() is true
     * <li>submittedValue is reset to null
     * <li>a ValueChangeEvent is queued if the new value != old value
     * </ul>
     */
    public void validate(FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }

        Object submittedValue = getSubmittedValue();
        if (submittedValue == null)
        {
            if (isRequired() && shouldAlwaysPerformValidationWhenRequiredTrue(context))
            {
                // continue
            }
            else
            {
                return;
            }
        }

        // Begin new JSF 2.0 requirement (INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL)
        if (shouldInterpretEmptyStringSubmittedValuesAsNull(context) && isEmptyString(submittedValue))
        {   
            // -= matzew = setSubmittedValue(null) is wrong, see:
            // https://javaserverfaces-spec-public.dev.java.net/issues/show_bug.cgi?id=671
            setSubmittedValue(null);
            submittedValue = null;
        }
        // End new JSF 2.0 requirement (INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL)

        Object convertedValue;
        try
        {
            convertedValue = getConvertedValue(context, submittedValue);
        }
        catch (ConverterException e)
        {
            String converterMessage = getConverterMessage();
            if (converterMessage != null)
            {
                context.addMessage(getClientId(context), new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        converterMessage, converterMessage));
            }
            else
            {
                FacesMessage facesMessage = e.getFacesMessage();
                if (facesMessage != null)
                {
                    context.addMessage(getClientId(context), facesMessage);
                }
                else
                {
                    _MessageUtils.addErrorMessage(context, this, CONVERSION_MESSAGE_ID,
                            new Object[] { _MessageUtils.getLabel(context, this) });
                }
            }
            setValid(false);
            return;
        }

        validateValue(context, convertedValue);

        if (!isValid())
        {
            return;
        }

        Object previousValue = getValue();
        setValue(convertedValue);
        setSubmittedValue(null);
        if (compareValues(previousValue, convertedValue))
        {
            queueEvent(new ValueChangeEvent(this, previousValue, convertedValue));
        }
    }

    /**
     * Convert the provided object to the desired value.
     * <p>
     * If there is a renderer for this component, then call the renderer's getConvertedValue method. While this can of
     * course be implemented in any way the renderer desires, it typically performs exactly the same processing that
     * this method would have done anyway (ie that described below for the no-renderer case).
     * <p>
     * Otherwise:
     * <ul>
     * <li>If the submittedValue is not a String then just return the submittedValue unconverted.
     * <li>If there is no "value" value-binding then just return the submittedValue unconverted.
     * <li>Use introspection to determine the type of the target property specified by the value-binding, and then use
     * Application.createConverter to find a converter that can map from String to the required type. Apply the
     * converter to the submittedValue and return the result.
     * </ul>
     */
    protected Object getConvertedValue(FacesContext context, Object submittedValue) throws ConverterException
    {
        Renderer renderer = getRenderer(context);
        if (renderer != null)
        {
            return renderer.getConvertedValue(context, this, submittedValue);
        }
        else if (submittedValue instanceof String)
        {
            Converter converter = _SharedRendererUtils.findUIOutputConverter(context, this);
            if (converter != null)
            {
                return converter.getAsObject(context, this, (String) submittedValue);
            }
        }
        return submittedValue;
    }

    protected boolean compareValues(Object previous, Object value)
    {
        return previous == null ? (value != null) : (!previous.equals(value));
    }

    /**
     * @since 1.2
     */
    public void resetValue()
    {
        super.resetValue();
        setSubmittedValue(null);
        setLocalValueSet(false);
        setValid(true);
    }
    
    /**
     * A boolean value that identifies the phase during which action events should fire.
     * <p>
     * During normal event processing, action methods and action listener methods are fired during the
     * "invoke application" phase of request processing. If this attribute is set to "true", these methods are fired
     * instead at the end of the "apply request values" phase.
     * </p>
     */
    @JSFProperty
    public boolean isImmediate()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.immediate, Boolean.FALSE);
    }

    public void setImmediate(boolean immediate)
    {
        getStateHelper().put(PropertyKeys.immediate, immediate );
    }

    /**
     * A boolean value that indicates whether an input value is required.
     * <p>
     * If this value is true and no input value is provided by a postback operation, then the "requiredMessage" text is
     * registered as a FacesMessage for the request, and validation fails.
     * </p>
     * <p>
     * Default value: false.
     * </p>
     */
    @JSFProperty(defaultValue = "false")
    public boolean isRequired()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.required, Boolean.FALSE);
    }

    public void setRequired(boolean required)
    {
        getStateHelper().put(PropertyKeys.required, required ); 
    }

    /**
     * Text to be displayed to the user as an error message when conversion of a submitted value to the target type
     * fails.
     * <p>
     * </p>
     */
    @JSFProperty
    public String getConverterMessage()
    {
        return (String) getStateHelper().eval(PropertyKeys.converterMessage);
    }

    public void setConverterMessage(String converterMessage)
    {
        getStateHelper().put(PropertyKeys.converterMessage, converterMessage );
    }

    /**
     * Text to be displayed to the user as an error message when this component is marked as "required" but no input
     * data is present during a postback (ie the user left the required field blank).
     */
    @JSFProperty
    public String getRequiredMessage()
    {
        return (String) getStateHelper().eval(PropertyKeys.requiredMessage);
    }

    public void setRequiredMessage(String requiredMessage)
    {
        getStateHelper().put(PropertyKeys.requiredMessage, requiredMessage );
    }

    /**
     * A method-binding EL expression which is invoked during the validation phase for this component.
     * <p>
     * The invoked method is expected to check the submitted value for this component, and if not acceptable then report
     * a validation error for the component.
     * </p>
     * <p>
     * The method is expected to have the prototype
     * </p>
     * <code>public void aMethod(FacesContext, UIComponent,Object)</code>
     * 
     * @deprecated
     */
    @SuppressWarnings("dep-ann")
    @JSFProperty(stateHolder=true, returnSignature = "void",
            methodSignature = "javax.faces.context.FacesContext,javax.faces.component.UIComponent,java.lang.Object")
    public MethodBinding getValidator()
    {
        return (MethodBinding) getStateHelper().eval(PropertyKeys.validator);
    }

    /** See getValidator.
     *  
     * @deprecated 
     */
    public void setValidator(MethodBinding validator)
    {
        getStateHelper().put(PropertyKeys.validator, validator);
    }

    /** See getValidator. */
    public void addValidator(Validator validator)
    {
        if (validator == null)
        {
            throw new NullPointerException("validator");
        }
        
        if (_validatorList == null)
        {
            //normally add user 0-3 validators: 
            _validatorList = new _DeltaList<Validator>(3);
        }

        _validatorList.add(validator);
        
        // The argument validator must be inspected for the presence of the ResourceDependency annotation.
        //_handleAnnotations(FacesContext.getCurrentInstance(), validator);
    }

    /** See getValidator. */
    public void removeValidator(Validator validator)
    {
        if (validator == null || _validatorList == null)
        {
            return;
        }

        _validatorList.remove(validator);
    }

    /** See getValidator. */
    public Validator[] getValidators()
    {
        if (_ExternalSpecifications.isBeanValidationAvailable() &&
            Boolean.TRUE.equals(this.getAttributes().containsKey(BEAN_BEFORE_JSF_PROPERTY)))
        {
            int bvIndex = -1;
            for (int i = 0; i < _validatorList.size(); i++)
            {
                Validator v = _validatorList.get(i);
                if (_BeanValidationUtils.isBeanValidator(v))
                {
                    bvIndex = i;
                    break;
                }
            }
            if (bvIndex != -1)
            {
                Validator[] array = new Validator[_validatorList.size()];
                for (int i = 0; i < _validatorList.size(); i++)
                {
                    if (i == bvIndex)
                    {
                        array[0] = _validatorList.get(i);
                        bvIndex = -1;
                    }
                    else
                    {
                        array[i+1] = _validatorList.get(i);
                    }
                }
                return array;
            }
            else
            {
                return _validatorList == null ? EMPTY_VALIDATOR_ARRAY
                        : _validatorList.toArray(new Validator[_validatorList.size()]);
            }
        }
        else
        {
            return _validatorList == null ? EMPTY_VALIDATOR_ARRAY
                    : _validatorList.toArray(new Validator[_validatorList.size()]);
        }
    }

    /**
     * Text which will be shown if validation fails.
     */
    @JSFProperty
    public String getValidatorMessage()
    {
        return (String) getStateHelper().eval(PropertyKeys.validatorMessage);
    }

    public void setValidatorMessage(String validatorMessage)
    {
        getStateHelper().put(PropertyKeys.validatorMessage, validatorMessage );
    }

    /**
     * A method which is invoked during postback processing for the current view if the submitted value for this
     * component is not equal to the value which the "value" expression for this component returns.
     * <p>
     * The phase in which this method is invoked can be controlled via the immediate attribute.
     * </p>
     * 
     * @deprecated
     */
    @JSFProperty(stateHolder=true, returnSignature = "void",
                 methodSignature = "javax.faces.event.ValueChangeEvent", clientEvent="valueChange")
    public MethodBinding getValueChangeListener()
    {
        return (MethodBinding) getStateHelper().eval(PropertyKeys.valueChangeListener);
    }

    /**
     * See getValueChangeListener.
     * 
     * @deprecated
     */
    public void setValueChangeListener(MethodBinding valueChangeListener)
    {
        getStateHelper().put(PropertyKeys.valueChangeListener, valueChangeListener);
    }

    /**
     * Specifies whether the component's value is currently valid, ie whether the validators attached to this component
     * have allowed it.
     */
    @JSFProperty(defaultValue = "true", tagExcluded = true)
    public boolean isValid()
    {
        Object value = getStateHelper().get(PropertyKeys.valid);
        if (value != null)
        {
            return (Boolean) value;        
        }
        return true; 
    }

    public void setValid(boolean valid)
    {
        // default value for valid is true, so if the intention is to save the default
        // value when nothing else was set before, don't do it. This is done in order to
        // reduce the size of the saved state of the state helper. Default values won't be
        // included in the saved state. 
        if (getStateHelper().get(PropertyKeys.valid) != null || !valid)
        {
            getStateHelper().put(PropertyKeys.valid, valid );
        }
    }

    /**
     * Specifies whether a local value is currently set.
     * <p>
     * If false, values are being retrieved from any attached ValueBinding.
     */
    @JSFProperty(defaultValue = "false", tagExcluded = true)
    public boolean isLocalValueSet()
    {
        Object value = getStateHelper().get(PropertyKeys.localValueSet);
        if (value != null)
        {
            return (Boolean) value;        
        }
        return false;
    }

    public void setLocalValueSet(boolean localValueSet)
    {
        // default value for localValueSet is false, so if the intention is to save the default
        // value when nothing else was set before, don't do it. This is done in order to
        // reduce the size of the saved state of the state helper. Default values won't be
        // included in the saved state.
        if (getStateHelper().get(PropertyKeys.localValueSet) != null || localValueSet)
        {
            getStateHelper().put(PropertyKeys.localValueSet, localValueSet );
        }
    }

    /**
     * Gets the current submitted value. This value, if non-null, is set by the Renderer to store a possibly invalid
     * value for later conversion or redisplay, and has not yet been converted into the proper type for this component
     * instance. This method should only be used by the decode() and validate() method of this component, or its
     * corresponding Renderer; however, user code may manually set it to null to erase any submitted value.
     */
    @JSFProperty(tagExcluded = true)
    public Object getSubmittedValue()
    {
        return  getStateHelper().get(PropertyKeys.submittedValue);
    }

    public void setSubmittedValue(Object submittedValue)
    {
        FacesContext facesContext = getFacesContext();
        if (facesContext != null && facesContext.isProjectStage(ProjectStage.Development))
        {
            // extended debug-info when in Development mode
            _createFieldDebugInfo(facesContext, "submittedValue",
                    getStateHelper().get(PropertyKeys.submittedValue), submittedValue, 1);
        }
        getStateHelper().put(PropertyKeys.submittedValue, submittedValue );
    }

    public void addValueChangeListener(ValueChangeListener listener)
    {
        addFacesListener(listener);
    }

    public void removeValueChangeListener(ValueChangeListener listener)
    {
        removeFacesListener(listener);
    }

    /**
     * The valueChange event is delivered when the value attribute
     * is changed.
     */
    @JSFListener(event="javax.faces.event.ValueChangeEvent")
    public ValueChangeListener[] getValueChangeListeners()
    {
        return (ValueChangeListener[]) getFacesListeners(ValueChangeListener.class);
    }

    enum PropertyKeys
    {
         immediate
        , required
        , converterMessage
        , requiredMessage
        , validator
        , validatorListSet
        , validatorMessage
        , valueChangeListener
        , valid
        , localValueSet
        , submittedValue
    }
    
    private static final Object[] INITIAL_STATE_PROPERTIES = new
            Object[]{
                UIOutput.PropertyKeys.value,
                null,
                UIInput.PropertyKeys.localValueSet,
                false,
                UIInput.PropertyKeys.submittedValue,
                null,
                UIInput.PropertyKeys.valid,
                true
            };
    
    public void markInitialState()
    {
        StateHelper helper = getStateHelper(false);
        if (helper != null && helper instanceof _DeltaStateHelper)
        {
            ((_DeltaStateHelper)helper).markPropertyInInitialState(INITIAL_STATE_PROPERTIES);
        }
        super.markInitialState();
        if (_validatorList != null)
        {
            _validatorList.markInitialState();
        }
    }
    
    public void clearInitialState()
    {
        if (initialStateMarked())
        {
            super.clearInitialState();
            if (_validatorList != null)
            {
                _validatorList.clearInitialState();
            }
        }
    }    

    @Override
    public Object saveState(FacesContext facesContext)
    {
        if (initialStateMarked())
        {
            Object parentSaved = super.saveState(facesContext);
            Object validatorListSaved = saveValidatorList(facesContext);
            if (parentSaved == null && validatorListSaved == null)
            {
                //No values
                return null;
            }
            
            Object[] values = new Object[2];
            values[0] = parentSaved;
            values[1] = validatorListSaved;
            return values;
        }
        else
        {
            Object[] values = new Object[2];
            values[0] = super.saveState(facesContext);
            values[1] = saveValidatorList(facesContext);
            return values;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void restoreState(FacesContext facesContext, Object state)
    {
        if (state == null)
        {
            return;
        }
        
        Object[] values = (Object[])state;
        super.restoreState(facesContext,values[0]);
        if (values[1] instanceof _AttachedDeltaWrapper)
        {
            //Delta
            if (_validatorList != null)
            {
                ((StateHolder)_validatorList).restoreState(facesContext,
                        ((_AttachedDeltaWrapper) values[1]).getWrappedStateObject());
            }
        }
        else if (values[1] != null || !initialStateMarked())
        {
            //Full
            _validatorList = (_DeltaList<Validator>)
                restoreAttachedState(facesContext,values[1]);
        }
    }
    
    private Object saveValidatorList(FacesContext facesContext)
    {
        PartialStateHolder holder = (PartialStateHolder) _validatorList;
        if (initialStateMarked() && _validatorList != null && holder.initialStateMarked())
        {                
            Object attachedState = holder.saveState(facesContext);
            if (attachedState != null)
            {
                return new _AttachedDeltaWrapper(_validatorList.getClass(),
                        attachedState);
            }
            //_validatorList instances once is created never changes, we can return null
            return null;
        }
        else
        {
            return saveAttachedState(facesContext,_validatorList);
        }            
    }
    
    /**
     * Returns the debug-info Map for this component.
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<Object[]>> _getDebugInfoMap()
    {
        Map<String, Object> requestMap = getFacesContext()
                .getExternalContext().getRequestMap();
        Map<String, List<Object[]>> debugInfo = (Map<String, List<Object[]>>) 
                requestMap.get(DEBUG_INFO_KEY + getClientId());
        if (debugInfo == null)
        {
            // no debug info available yet, create one and put it on the attributes map
            debugInfo = new HashMap<String, List<Object[]>>();
            requestMap.put(DEBUG_INFO_KEY + getClientId(), debugInfo);
        }
        return debugInfo;
    }
    
    /**
     * Returns the field's debug-infos from the component's debug-info Map.
     * @param field
     * @return
     */
    private List<Object[]> _getFieldDebugInfos(final String field)
    {
        Map<String, List<Object[]>> debugInfo = _getDebugInfoMap();
        List<Object[]> fieldDebugInfo = debugInfo.get(field);
        if (fieldDebugInfo == null)
        {
            // no field debug-infos yet, create them and store it in the Map
            fieldDebugInfo = new ArrayList<Object[]>();
            debugInfo.put(field, fieldDebugInfo);
        }
        return fieldDebugInfo;
    }
    
    /**
     * Creates the field debug-info for the given field, which changed
     * from oldValue to newValue.
     * 
     * @param facesContext
     * @param field
     * @param oldValue
     * @param newValue
     * @param skipStackTaceElements How many StackTraceElements should be skipped
     *                              when the calling function will be determined.
     */
    private void _createFieldDebugInfo(FacesContext facesContext,
            final String field, Object oldValue, 
            Object newValue, final int skipStackTaceElements)
    {
        if (oldValue == null && newValue == null)
        {
            // both values are null, not interesting and can
            // happen a lot in UIData with saving and restoring state
            return;
        }
        
        if (facesContext.getViewRoot() == null)
        {
            // No viewRoot set, it is creating component, 
            // so it is not possible to calculate the clientId, 
            // abort processing because the interesting part will
            // happen later.
            return;
        }
        
        if (getParent() == null || !isInView())
        {
            //Skip if no parent or is not in view
            return;
        }
        
        // convert Array values into a more readable format
        if (oldValue != null && oldValue.getClass().isArray() && Object[].class.isAssignableFrom(oldValue.getClass()))
        {
            oldValue = Arrays.deepToString((Object[]) oldValue);
        }
        if (newValue != null && newValue.getClass().isArray() && Object[].class.isAssignableFrom(newValue.getClass()))
        {
            newValue = Arrays.deepToString((Object[]) newValue);
        }
        
        // use Throwable to get the current call stack
        Throwable throwableHelper = new Throwable();
        StackTraceElement[] stackTraceElements = throwableHelper.getStackTrace();
        List<StackTraceElement> debugStackTraceElements = new LinkedList<StackTraceElement>();
        
        // + 1 because this method should also be skipped
        for (int i = skipStackTaceElements + 1; i < stackTraceElements.length; i++)
        {
            debugStackTraceElements.add(stackTraceElements[i]);
            
            if (FacesServlet.class.getCanonicalName()
                    .equals(stackTraceElements[i].getClassName()))
            {
                // stop after the FacesServlet
                break;
            }
        }
        
        // create the debug-info array
        // structure:
        //     - 0: phase
        //     - 1: old value
        //     - 2: new value
        //     - 3: StackTraceElement List
        // NOTE that we cannot create a class here to encapsulate this data,
        // because this is not on the spec and the class would not be available in impl.
        Object[] debugInfo = new Object[4];
        debugInfo[0] = facesContext.getCurrentPhaseId();
        debugInfo[1] = oldValue;
        debugInfo[2] = newValue;
        debugInfo[3] = debugStackTraceElements;
        
        // add the debug info
        _getFieldDebugInfos(field).add(debugInfo);
    }
    
    /**
     * Check if a value is empty or not. Since we don't know the class of
     * value we have to check and deal with it properly.
     * 
     * @since 2.0
     * @param value
     * @return
     */
    public static boolean isEmpty(Object value)
    {
        if (value == null)
        {
            return true;
        }
        else if (value instanceof String)
        {
            if ( ((String)value).trim().length() <= 0 )
            {
                return true;
            }
        }
        else if (value instanceof Collection)
        {
            if ( ((Collection)value).isEmpty())
            {
                return true;
            }
        }
        else if (value.getClass().isArray())
        {
            if (java.lang.reflect.Array.getLength(value) <= 0)
            {
                return true;
            }
        }
        else if (value instanceof Map)
        {
            if ( ((Map)value).isEmpty())
            {
                return true;
            }
        }
        return false;
    }

}
