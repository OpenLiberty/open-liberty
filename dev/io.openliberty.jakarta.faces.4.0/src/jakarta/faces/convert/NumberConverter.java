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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Currency;
import java.util.Locale;

import jakarta.el.ValueExpression;
import jakarta.faces.FacesException;
import jakarta.faces.component.PartialStateHolder;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFConverter;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;
import org.apache.myfaces.core.api.shared.MessageUtils;
import org.apache.myfaces.core.api.shared.lang.Assert;

/**
 * This tag creates a number formatting converter and associates it
 * with the nearest parent UIComponent.
 * 
 * Unless otherwise specified, all attributes accept static values or EL expressions.
 * 
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
@JSFConverter(name = "f:convertNumber", bodyContent = "empty")
@JSFJspProperty(
    name="binding", 
    returnType = "jakarta.faces.convert.NumberConverter",
    longDesc = "A ValueExpression that evaluates to a NumberConverter.")
public class NumberConverter implements Converter, PartialStateHolder
{
    public static final String CONVERTER_ID = "jakarta.faces.Number";
    public static final String STRING_ID = "jakarta.faces.converter.STRING";
    public static final String CURRENCY_ID = "jakarta.faces.converter.NumberConverter.CURRENCY";
    public static final String NUMBER_ID = "jakarta.faces.converter.NumberConverter.NUMBER";
    public static final String PATTERN_ID = "jakarta.faces.converter.NumberConverter.PATTERN";
    public static final String PERCENT_ID = "jakarta.faces.converter.NumberConverter.PERCENT";

    private String _currencyCode;
    private String _currencySymbol;
    private Locale _locale;
    private int _maxFractionDigits;
    private int _maxIntegerDigits;
    private int _minFractionDigits;
    private int _minIntegerDigits;
    private String _pattern;
    private String _type = "number";
    private boolean _groupingUsed = true;
    private boolean _integerOnly = false;
    private boolean _transient;

    private boolean _maxFractionDigitsSet;
    private boolean _maxIntegerDigitsSet;
    private boolean _minFractionDigitsSet;
    private boolean _minIntegerDigitsSet;

    public NumberConverter()
    {
    }

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String value)
    {
        Assert.notNull(facesContext, "facesContext");
        Assert.notNull(uiComponent, "uiComponent");

        if (value == null)
        {
            return null;
        }
        
        value = value.trim();
        if (value.length() < 1)
        {
            return null;
        }
        
        NumberFormat format = getNumberFormat(facesContext);
        format.setParseIntegerOnly(_integerOnly);

        DecimalFormat df = (DecimalFormat)format;

        // The best we can do in this case is check if there is a ValueExpression
        // with a BigDecimal as returning type , and if that so enable BigDecimal parsing
        // to prevent loss in precision, and do not break existing examples (since
        // in those cases it is expected to return Double). See MYFACES-1890 and TRINIDAD-1124
        // for details
        ValueExpression valueExpression = uiComponent.getValueExpression("value");
        Class<?> destType = null;
        if (valueExpression != null)
        {
            destType = valueExpression.getType(facesContext.getELContext());
            if (destType != null
                && (BigDecimal.class.isAssignableFrom(destType) || BigInteger.class.isAssignableFrom(destType)))
            {
                df.setParseBigDecimal(true);
            }
        }

        DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
        boolean changed = false;
        if(dfs.getGroupingSeparator() == '\u00a0')
        {
            dfs.setGroupingSeparator(' ');
            df.setDecimalFormatSymbols(dfs);
            value = value.replace('\u00a0', ' ');
            changed = true;
        }

        formatCurrency(format);

        try
        {
            return parse(value, format, destType);
        }
        catch (ParseException e)
        {
            if(changed)
            {
                dfs.setGroupingSeparator('\u00a0');
                df.setDecimalFormatSymbols(dfs);
            }
            try
            {
                return parse(value, format, destType);
            }
            catch (ParseException pe)
            {
                if (getPattern() != null)
                {
                    throw new ConverterException(MessageUtils.getErrorMessage(facesContext,
                            PATTERN_ID,
                            new Object[]{value, "$###,###", MessageUtils.getLabel(facesContext, uiComponent)}));
                }
                else if (getType().equals("number"))
                {
                    throw new ConverterException(MessageUtils.getErrorMessage(facesContext,
                            NUMBER_ID,
                            new Object[]{value, format.format(21),
                                         MessageUtils.getLabel(facesContext, uiComponent)}));
                }
                else if (getType().equals("currency"))
                {
                    throw new ConverterException(MessageUtils.getErrorMessage(facesContext,
                            CURRENCY_ID,
                            new Object[]{value, format.format(42.25),
                                         MessageUtils.getLabel(facesContext, uiComponent)}));
                }
                else if (getType().equals("percent"))
                {
                    throw new ConverterException(MessageUtils.getErrorMessage(facesContext,
                            PERCENT_ID,
                            new Object[]{value, format.format(.90),
                                         MessageUtils.getLabel(facesContext, uiComponent)}));
                }
            }
        }
        
        return null;
    }

    private Object parse(String value, NumberFormat format, Class<?> destType)
        throws ParseException
    {
        Object parsed = null;
        
        ParsePosition parsePosition = new ParsePosition(0);
        if (destType == BigInteger.class)
        {
            parsed = ((BigDecimal) format.parse(value, parsePosition)).toBigInteger();
        }
        else
        {
            parsed = format.parse(value, parsePosition);
        }
        
        if (parsePosition.getIndex() != value.length())
        {
            throw new ParseException(value, parsePosition.getIndex());
        }

        return parsed;
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent uiComponent, Object value)
    {
        Assert.notNull(facesContext, "facesContext");
        Assert.notNull(uiComponent, "uiComponent");

        if (value == null)
        {
            return "";
        }

        if (value instanceof String)
        {
            return (String) value;
        }

        NumberFormat format = getNumberFormat(facesContext);
        format.setGroupingUsed(_groupingUsed);
        if (_maxFractionDigitsSet)
        {
            format.setMaximumFractionDigits(_maxFractionDigits);
        }
        if (_maxIntegerDigitsSet)
        {
            format.setMaximumIntegerDigits(_maxIntegerDigits);
        }
        if (_minFractionDigitsSet)
        {
            format.setMinimumFractionDigits(_minFractionDigits);
        }
        if (_minIntegerDigitsSet)
        {
            format.setMinimumIntegerDigits(_minIntegerDigits);
        }
        formatCurrency(format);
        try
        {
            return format.format(value);
        }
        catch (Exception e)
        {
            throw new ConverterException(MessageUtils.getErrorMessage(facesContext, STRING_ID,
                    new Object[]{value,MessageUtils.getLabel(facesContext, uiComponent)}),e);
        }
    }

    private NumberFormat getNumberFormat(FacesContext facesContext)
    {
        Locale locale = _locale != null ? _locale : facesContext.getViewRoot().getLocale();

        if (_pattern == null && _type == null)
        {
            throw new ConverterException("Cannot get NumberFormat, either type or pattern needed.");
        }

        // pattern
        if (_pattern != null)
        {
            return new DecimalFormat(_pattern, new DecimalFormatSymbols(locale));
        }

        // type
        if (_type.equals("number"))
        {
            return NumberFormat.getNumberInstance(locale);
        }
        else if (_type.equals("currency"))
        {
            return NumberFormat.getCurrencyInstance(locale);
        }
        else if (_type.equals("percent"))
        {
            return NumberFormat.getPercentInstance(locale);
        }
        throw new ConverterException("Cannot get NumberFormat, illegal type " + _type);
    }

    private void formatCurrency(NumberFormat format)
    {
        if (_currencyCode == null && _currencySymbol == null)
        {
            return;
        }

        boolean useCurrencyCode;
        useCurrencyCode = _currencyCode != null;

        if (useCurrencyCode)
        {
            // set Currency
            try
            {
                format.setCurrency(Currency.getInstance(_currencyCode));
            }
            catch (Exception e)
            {
                throw new ConverterException("Unable to get Currency instance for currencyCode " + _currencyCode);
            }
        }
        else if (format instanceof DecimalFormat)
        {
            DecimalFormat dFormat = (DecimalFormat)format;
            DecimalFormatSymbols symbols = dFormat.getDecimalFormatSymbols();
            symbols.setCurrencySymbol(_currencySymbol);
            dFormat.setDecimalFormatSymbols(symbols);
        }
    }

    // STATE SAVE/RESTORE
    @Override
    public void restoreState(FacesContext facesContext, Object state)
    {
        if (state != null)
        {
            Object values[] = (Object[]) state;
            _currencyCode = (String) values[0];
            _currencySymbol = (String) values[1];
            _locale = (Locale) values[2];
            Integer value = (Integer) values[3];
            _maxFractionDigits = value != null ? value : 0;
            value = (Integer) values[4];
            _maxIntegerDigits = value != null ? value : 0;
            value = (Integer) values[5];
            _minFractionDigits = value != null ? value : 0;
            value = (Integer) values[6];
            _minIntegerDigits = value != null ? value : 0;
            _pattern = (String) values[7];
            _type = (String) values[8];
            _groupingUsed = (Boolean) values[9];
            _integerOnly = (Boolean) values[10];
            _maxFractionDigitsSet = (Boolean) values[11];
            _maxIntegerDigitsSet = (Boolean) values[12];
            _minFractionDigitsSet = (Boolean) values[13];
            _minIntegerDigitsSet = (Boolean) values[14];
        }
    }

    @Override
    public Object saveState(FacesContext facesContext)
    {
        if (!initialStateMarked())
        {
            Object values[] = new Object[15];
            values[0] = _currencyCode;
            values[1] = _currencySymbol;
            values[2] = _locale;
            values[3] = _maxFractionDigitsSet ? Integer.valueOf(_maxFractionDigits) : null;
            values[4] = _maxIntegerDigitsSet ? Integer.valueOf(_maxIntegerDigits) : null;
            values[5] = _minFractionDigitsSet ? Integer.valueOf(_minFractionDigits) : null;
            values[6] = _minIntegerDigitsSet ? Integer.valueOf(_minIntegerDigits) : null;
            values[7] = _pattern;
            values[8] = _type;
            values[9] = _groupingUsed ? Boolean.TRUE : Boolean.FALSE;
            values[10] = _integerOnly ? Boolean.TRUE : Boolean.FALSE;
            values[11] = _maxFractionDigitsSet ? Boolean.TRUE : Boolean.FALSE;
            values[12] = _maxIntegerDigitsSet ? Boolean.TRUE : Boolean.FALSE;
            values[13] = _minFractionDigitsSet ? Boolean.TRUE : Boolean.FALSE;
            values[14] = _minIntegerDigitsSet ? Boolean.TRUE : Boolean.FALSE;
            return values;
        }
        return null;
    }

    // GETTER & SETTER
    
    /**
     * ISO 4217 currency code
     * 
     */
    @JSFProperty
    public String getCurrencyCode()
    {
        return _currencyCode != null ?
               _currencyCode :
               getDecimalFormatSymbols().getInternationalCurrencySymbol();
    }

    public void setCurrencyCode(String currencyCode)
    {
        _currencyCode = currencyCode;
        clearInitialState();
    }

    /**
     * The currency symbol used to format a currency value.  Defaults
     * to the currency symbol for locale.
     * 
     */
    @JSFProperty
    public String getCurrencySymbol()
    {
        return _currencySymbol != null ?
               _currencySymbol :
               getDecimalFormatSymbols().getCurrencySymbol();
    }

    public void setCurrencySymbol(String currencySymbol)
    {
        _currencySymbol = currencySymbol;
        clearInitialState();
    }

    /**
     * Specifies whether output will contain grouping separators.  Default: true.
     * 
     */
    @JSFProperty(deferredValueType="java.lang.Boolean")
    public boolean isGroupingUsed()
    {
        return _groupingUsed;
    }

    public void setGroupingUsed(boolean groupingUsed)
    {
        _groupingUsed = groupingUsed;
        clearInitialState();
    }

    /**
     * Specifies whether only the integer part of the input will be parsed.  Default: false.
     * 
     */
    @JSFProperty(deferredValueType="java.lang.Boolean")
    public boolean isIntegerOnly()
    {
        return _integerOnly;
    }

    public void setIntegerOnly(boolean integerOnly)
    {
        _integerOnly = integerOnly;
        clearInitialState();
    }

    /**
     * The name of the locale to be used, instead of the default as
     * specified in the faces configuration file.
     * 
     */
    @JSFProperty(deferredValueType="java.lang.Object")
    public Locale getLocale()
    {
        if (_locale != null)
        {
            return _locale;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getViewRoot().getLocale();
    }

    public void setLocale(Locale locale)
    {
        _locale = locale;
        clearInitialState();
    }

    /**
     * The maximum number of digits in the fractional portion of the number.
     * 
     */
    @JSFProperty(deferredValueType="java.lang.Integer")
    public int getMaxFractionDigits()
    {
        return _maxFractionDigits;
    }

    public void setMaxFractionDigits(int maxFractionDigits)
    {
        _maxFractionDigitsSet = true;
        _maxFractionDigits = maxFractionDigits;
        clearInitialState();
    }

    /**
     * The maximum number of digits in the integer portion of the number.
     * 
     */
    @JSFProperty(deferredValueType="java.lang.Integer")
    public int getMaxIntegerDigits()
    {
        return _maxIntegerDigits;
    }

    public void setMaxIntegerDigits(int maxIntegerDigits)
    {
        _maxIntegerDigitsSet = true;
        _maxIntegerDigits = maxIntegerDigits;
        clearInitialState();
    }

    /**
     * The minimum number of digits in the fractional portion of the number.
     * 
     */
    @JSFProperty(deferredValueType="java.lang.Integer")
    public int getMinFractionDigits()
    {
        return _minFractionDigits;
    }

    public void setMinFractionDigits(int minFractionDigits)
    {
        _minFractionDigitsSet = true;
        _minFractionDigits = minFractionDigits;
        clearInitialState();
    }

    /**
     * The minimum number of digits in the integer portion of the number.
     * 
     */
    @JSFProperty(deferredValueType="java.lang.Integer")
    public int getMinIntegerDigits()
    {
        return _minIntegerDigits;
    }

    public void setMinIntegerDigits(int minIntegerDigits)
    {
        _minIntegerDigitsSet = true;
        _minIntegerDigits = minIntegerDigits;
        clearInitialState();
    }

    /**
     * A custom Date formatting pattern, in the format used by java.text.SimpleDateFormat.
     * 
     */
    @JSFProperty
    public String getPattern()
    {
        return _pattern;
    }

    public void setPattern(String pattern)
    {
        _pattern = pattern;
        clearInitialState();
    }

    @Override
    public boolean isTransient()
    {
        return _transient;
    }

    @Override
    public void setTransient(boolean aTransient)
    {
        _transient = aTransient;
    }

    /**
     * The type of formatting/parsing to be performed.  Values include:
     * number, currency, and percent.  Default: number.
     * 
     */
    @JSFProperty
    public String getType()
    {
        return _type;
    }

    public void setType(String type)
    {
        if (type != null && type.length() > 0
                && (!"number".equals(type) && !"currency".equals(type) && !"percent".equals(type)))
        {
            throw new FacesException("Uknown type: " + type);
        }
        
        _type = type;
        clearInitialState();
    }

    private DecimalFormatSymbols getDecimalFormatSymbols()
    {
        return new DecimalFormatSymbols(getLocale());
    }
    
    private boolean _initialStateMarked = false;

    @Override
    public void clearInitialState()
    {
        _initialStateMarked = false;
    }

    @Override
    public boolean initialStateMarked()
    {
        return _initialStateMarked;
    }

    @Override
    public void markInitialState()
    {
        _initialStateMarked = true;
    }
}
