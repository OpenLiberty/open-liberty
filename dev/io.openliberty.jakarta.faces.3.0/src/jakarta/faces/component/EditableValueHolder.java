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
package jakarta.faces.component;

import jakarta.faces.el.MethodBinding;
import jakarta.faces.event.ValueChangeListener;
import jakarta.faces.validator.Validator;

/**
 * Defines the methods required for a component whose value can be modified by the user.
 * <p>
 * When a component implementing this interface is rendered, the value output is (in order):
 * <ul>
 * <li>The "submitted value" if non-null.
 * <li>The component's "local value" if non-null.
 * <li>The result of evaluating the value-binding expression with name "value" for this component.
 * </ul>
 * <p>
 * Rendering the submitted value if non-null allows a component to redisplay a user-provided value when validation fails
 * for the component. The submitted value is usually just the plain string extracted from the servlet request. During
 * successful validation of the component, the submitted value is converted to an appropriate datatype, and stored as
 * the component's "local value", and then the "submitted value" is immediately reset to null.
 * <p>
 * Rendering the "local value" if non-null allows a component to redisplay a page when validation fails for some other
 * component; the model can't be updated unless <i>all</i> components have passed validation. This also allows
 * components to work without a defined "value" value-binding expression. When all components validate, the update model
 * phase runs; all components with "value" value-bindings store the "local value" into the specified property then reset
 * their local value to null.
 * <p>
 * Rendering the value-binding expression named "value" allows components to display data from the user's model classes.
 * This is the most common way a component's renderer obtains the value to display.
 * 
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a> for
 * more.
 */
public interface EditableValueHolder extends ValueHolder
{
    /**
     * Get an object representing the most recent raw user input received for this component.
     * <p>
     * This is non-null only between <code>decode</code> and <code>validate</code> phases, or when validation for the
     * component has not succeeded. Once conversion and validation has succeeded, the (converted) value is stored in the
     * local "value" property of this component, and the submitted value is reset to null.
     */
    public Object getSubmittedValue();

    /**
     * Invoked during the "decode" phase of processing to inform this component what data was received from the user.
     * <p>
     * In many cases the submitted value is a plain string extracted from the current servlet request object.
     * <p>
     * In cases where a component is rendered as multiple input components (eg a calendar control with separate
     * day/month/year fields), the submittedValue may be some custom object wrapping the data. However the provided
     * object <i>must</i> be able to represent all possible user input values, not just valid ones.
     */
    public void setSubmittedValue(Object submittedValue);

    /**
     * Determine whether the value member variable of this component has been set from the converted and validated
     * "submitted value". This property is needed because EditableValueHolder components need to distinguish between the
     * value local <i>member</i> and the value <i>property</i> (which may involve a value-binding to the user model).
     */
    public boolean isLocalValueSet();

    /**
     * Specify the return value of method isLocalValueSet. This is called after the local value member has been set from
     * the converted and validated "submitted value". It is cleared after that value has been pushed to the user model
     * via the value-binding named "value".
     */
    public void setLocalValueSet(boolean localValueSet);

    /**
     * This returns false if validation has been run for this component and has failed.
     * <p>
     * It is also set to false if the validated value could not be passed to the model during the update model phase.
     * <p>
     * All input components are marked as valid during the "restore view" phase, so this will return true for components
     * whose validation has not been executed.
     */
    public boolean isValid();

    public void setValid(boolean valid);

    /**
     * Return true if this component must have a non-empty submitted value.
     * <p>
     * Note that even when a component is "required", it is not an error for some form to be submitted which does not
     * contain the component. It is only an error when the form submitted does contain the component, but there is no
     * data for the component in that request. A "submitted value" of null is set during the "decode" step to represent
     * the case where the request map has no entry corresponding to this component's id. When the decode step finds an
     * entry in the request, but the corresponding value represents "no data" (eg an empty string for a text input
     * field) then some special non-null value must be set for the "submitted value"; validation for "required" fields
     * must then check for that.
     */
    public boolean isRequired();

    /**
     * Set to true to cause validation failure when a form containing this component is submitted and there is no value
     * selected for this component.
     */
    public void setRequired(boolean required);

    /**
     * When true, the validation step for this component will also invoke any associated actionListeners. Typically such
     * listeners will call renderResponse, causing the rendering phase to begin immediately (including possible
     * navigation) without performing validation on any following components.
     */
    public boolean isImmediate();

    public void setImmediate(boolean immediate);

    /**
     * Get the single validator defined directly on this component.
     * <p>
     * In addition to this validator, there may be a list of validators associated with this component.
     * <p>
     * This validator is executed after all validators in the validator list.
     * 
     * @deprecated Use getValidators() instead.
     */
    public MethodBinding getValidator();

    /**
     * @deprecated Use addValidator(MethodExpressionValidaotr) instead.
     */
    public void setValidator(MethodBinding validatorBinding);

    /**
     * Get the single value-change defined directly on this component.
     * <p>
     * In addition to this listener, there may be a list of listeners associated with this component.
     * <p>
     * This listeners is executed after all listeners in the list.
     * 
     * @deprecated Use getValueChangeLIsteners() instead.
     */
    public MethodBinding getValueChangeListener();

    /**
     * @deprecated use addValueChangeListener(MethodExpressionValueChangeListener) instead.
     */
    public void setValueChangeListener(MethodBinding valueChangeMethod);

    public void addValidator(Validator validator);

    public Validator[] getValidators();

    public void removeValidator(Validator validator);

    public void addValueChangeListener(ValueChangeListener listener);

    public ValueChangeListener[] getValueChangeListeners();

    public void removeValueChangeListener(ValueChangeListener listener);

    /**
     * Convenience method to reset this component's value to an uninitialized state, by resetting the local value and
     * submitted values to null (ensuring that {@link #isLocalValueSet} is false), and setting "valid" to true.
     * 
     * @since 2.0
     */
    public void resetValue();

}
