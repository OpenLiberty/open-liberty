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
package javax.faces.validator;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFValidator;

import javax.faces.component.PartialStateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * <p>
 *   <strong>RegexValidator</strong> is a {@link javax.faces.validator.Validator}
 *   that checks the value of the corresponding component against specified
 *   pattern using Java regular expression syntax.
 *
 *   The regular expression syntax accepted by the RegexValidator class is
 *   same as mentioned in class {@link java.util.regex.Pattern} in package
 *   <code>java.util.regex</code>.
 * </p>
 *
 * <p>
 *   The following algorithm is implemented:
 * </p>
 *
 * <ul>
 *   <li>If the passed value is <code>null</code>, exit immediately.</li>
 *   <li>
 *     If the passed value is not a String, exit with a {@link #NOT_MATCHED_MESSAGE_ID}
 *     error message.
 *   </li>
 *   <li>
 *     If no pattern has been set, or pattern resolves to <code>null</code> or an
 *     empty String, throw a {@link javax.faces.validator.ValidatorException}
 *     with a {@link #PATTERN_NOT_SET_MESSAGE_ID} message.
 *   </li>
 *   <li>
 *     If pattern is not a valid regular expression, according to the rules as defined
 *     in class {@link java.util.regex.Pattern}, throw a {@link ValidatorException}
 *     with a (@link #MATCH_EXCEPTION_MESSAGE_ID} message.
 *   </li>
 *   <li>
 *     If a <code>pattern</code> property has been configured on this
 *     {@link javax.faces.validator.Validator}, check the passed value against this pattern.
 *     If value does not match pattern throw a {@link ValidatorException}
 *     containing a {@link #NOT_MATCHED_MESSAGE_ID} message.
 *   </li>
 * </ul>
 *
 * @since 2.0
 */
@JSFValidator(
    name="f:validateRegex",
    bodyContent="empty",
    tagClass="org.apache.myfaces.taglib.core.ValidateRegexTag")
@JSFJspProperty(
    name="binding",
    returnType = "javax.faces.validator.RegexValidator",
    longDesc = "A ValueExpression that evaluates to a RegexValidator.")
public class RegexValidator implements Validator, PartialStateHolder
{

    /**
     * Converter ID, as defined by the JSF 2.0 specification.
     */
    public static final String VALIDATOR_ID = "javax.faces.RegularExpression";

    /**
     * This message ID is used when the pattern is <code>null</code>, or an empty String.
     */
    public static final String PATTERN_NOT_SET_MESSAGE_ID = "javax.faces.validator.RegexValidator.PATTERN_NOT_SET";

    /**
     * This message ID is used when the passed value is not a String, or when
     * the pattern does not match the passed value.
     */
    public static final String NOT_MATCHED_MESSAGE_ID = "javax.faces.validator.RegexValidator.NOT_MATCHED";

    /**
     * This message ID is used when the pattern is not a valid regular expression, according
     * to the rules as defined in class {@link java.util.regex.Pattern}
     */
    public static final String MATCH_EXCEPTION_MESSAGE_ID = "javax.faces.validator.RegexValidator.MATCH_EXCEPTION";

    //TODO: Find a better place for such a common constant
    private static final String EMPTY_STRING = "";

    private String pattern;

    private boolean isTransient = false;

    // VALIDATE
    /** {@inheritDoc} */
    public void validate(FacesContext context,
                         UIComponent component,
                         Object value)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }
        if (component == null)
        {
            throw new NullPointerException("component");
        }

        if (value == null)
        {
            return;
        }
        if (!(value instanceof String))
        {
            throw new ValidatorException(_MessageUtils.getErrorMessage(context, NOT_MATCHED_MESSAGE_ID, null));
        }

        String string = (String) value;

        Pattern thePattern;
        if (pattern == null
         || pattern.equals(EMPTY_STRING))
        {
            throw new ValidatorException(_MessageUtils.getErrorMessage(context, PATTERN_NOT_SET_MESSAGE_ID, null));
        }

        try
        {
            thePattern = Pattern.compile(pattern);
        }
        catch (PatternSyntaxException pse)
        {
            throw new ValidatorException(_MessageUtils.getErrorMessage(context, MATCH_EXCEPTION_MESSAGE_ID, null));
        }

        if (!thePattern.matcher(string).matches())
        {
            //TODO: Present the patternExpression in a more user friendly way
            Object[] args = {thePattern, _MessageUtils.getLabel(context, component)};
            throw new ValidatorException(_MessageUtils.getErrorMessage(context, NOT_MATCHED_MESSAGE_ID, args));
        }
    }

    // RESTORE & SAVE STATE

    /** {@inheritDoc} */
    public Object saveState(FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }

        if (!initialStateMarked())
        {
            return pattern;
        }
        return null;
    }

    /** {@inheritDoc} */
    public void restoreState(FacesContext context, Object state)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }

        if (state != null)
        {
            //Since pattern is required, if state is null
            //nothing has changed
            this.pattern = (String) state;
        }
    }

    // SETTER & GETTER

    /** {@inheritDoc} */
    public boolean isTransient()
    {
        return isTransient;
    }

    /** {@inheritDoc} */
    public void setTransient(boolean isTransient)
    {
        this.isTransient = isTransient;
    }

    /**
     * The Regular Expression property to validate against. This property must be a ValueExpression
     * that resolves to a String in the format of the java.util.regex patterns.
     *
     * @param pattern a ValueExpression that evaluates to a String that is the regular expression pattern
     */
    public void setPattern(String pattern)
    {
        //TODO: Validate input parameter
        this.pattern = pattern;
        clearInitialState();
    }

    /**
     * Return the ValueExpression that yields the regular expression pattern when evaluated.
     *
     * @return The pattern.
     */
    @JSFProperty(required = true)
    public String getPattern()
    {
        return this.pattern;
    }

    private boolean _initialStateMarked = false;

    public void clearInitialState()
    {
        _initialStateMarked = false;
    }

    public boolean initialStateMarked()
    {
        return _initialStateMarked;
    }

    public void markInitialState()
    {
        _initialStateMarked = true;
    }
    
    @JSFProperty(faceletsOnly=true)
    @SuppressWarnings("unused")
    private Boolean isDisabled()
    {
        return null;
    }
    
    @JSFProperty(faceletsOnly=true)
    @SuppressWarnings("unused")
    private String getFor()
    {
        return null;
    }
}
