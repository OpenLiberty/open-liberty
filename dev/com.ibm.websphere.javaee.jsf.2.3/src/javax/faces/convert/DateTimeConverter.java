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
package javax.faces.convert;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.faces.component.PartialStateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFConverter;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * This tag associates a date time converter with the nearest parent UIComponent.
 * 
 * Unless otherwise specified, all attributes accept static values or EL expressions.
 * 
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
@JSFConverter(
    name="f:convertDateTime",
    bodyContent="empty",
    tagClass="org.apache.myfaces.taglib.core.ConvertDateTimeTag")
@JSFJspProperty(
    name="binding", 
    returnType = "javax.faces.convert.DateTimeConverter",
    longDesc = "A ValueExpression that evaluates to a DateTimeConverter.")
public class DateTimeConverter
        implements Converter, PartialStateHolder
{

    // API field
    public static final String CONVERTER_ID = "javax.faces.DateTime";
    public static final String DATE_ID = "javax.faces.converter.DateTimeConverter.DATE";
    public static final String DATETIME_ID = "javax.faces.converter.DateTimeConverter.DATETIME";
    public static final String STRING_ID = "javax.faces.converter.STRING";
    public static final String TIME_ID = "javax.faces.converter.DateTimeConverter.TIME";

    // internal constants
    private static final String TYPE_DATE = "date";
    private static final String TYPE_TIME = "time";
    private static final String TYPE_BOTH = "both";
    private static final String TYPE_LOCAL_DATE = "localDate";
    private static final String TYPE_LOCAL_TIME = "localTime";
    private static final String TYPE_LOCAL_DATE_TIME = "localDateTime";
    private static final String TYPE_OFFSET_TIME = "offsetTime";
    private static final String TYPE_OFFSET_DATE_TIME = "offsetDateTime";
    private static final String TYPE_ZONED_DATE_TIME = "zonedDateTime";
            
    private static final String STYLE_DEFAULT = "default";
    private static final String STYLE_MEDIUM = "medium";
    private static final String STYLE_SHORT = "short";
    private static final String STYLE_LONG = "long";
    private static final String STYLE_FULL = "full";
    private static final TimeZone TIMEZONE_DEFAULT = TimeZone.getTimeZone("GMT");

    private String _dateStyle;
    private Locale _locale;
    private String _pattern;
    private String _timeStyle;
    private TimeZone _timeZone;
    private String _type;
    private boolean _transient;

    // CONSTRUCTORS
    public DateTimeConverter()
    {
    }

    // METHODS
    public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String value)
    {
        if (facesContext == null)
        {
            throw new NullPointerException("facesContext");
        }
        if (uiComponent == null)
        {
            throw new NullPointerException("uiComponent");
        }

        if (value != null)
        {
            value = value.trim();
            if (value.length() > 0)
            {
                if (isJava8DateTimeFormatter())
                {
                    DateTimeFormatter format = getDateTimeFormatter();
                    try
                    {
                        TemporalQuery tq = getTemporalQuery();
                        if (tq != null)
                        {
                            return format.parse(value, tq);
                        }
                        else
                        {
                            return format.parse(value);
                        }
                    }
                    catch (Exception e)
                    {
                        String type = getType();
                        TemporalAccessor currentDate;
                        if (TYPE_LOCAL_DATE.equals(type) || TYPE_LOCAL_DATE_TIME.equals(type) 
                                || TYPE_LOCAL_TIME.equals(type))
                        {
                            currentDate = LocalDateTime.now();
                        }
                        else if (TYPE_OFFSET_TIME.equals(type) || TYPE_OFFSET_DATE_TIME.equals(type))
                        {
                            currentDate = OffsetDateTime.now();
                        }
                        else
                        {
                            currentDate = ZonedDateTime.now();
                        }
                        Object[] args = new Object[]{value,
                                format.format(currentDate),_MessageUtils.getLabel(facesContext, uiComponent)};

                        if(type.equals(TYPE_LOCAL_DATE))
                        {
                            throw new ConverterException(_MessageUtils.getErrorMessage(facesContext, DATE_ID, args));
                        }
                        else if (type.equals(TYPE_LOCAL_TIME) || type.equals(TYPE_OFFSET_TIME))
                        {
                            throw new ConverterException(_MessageUtils.getErrorMessage(facesContext, TIME_ID, args));
                        }
                        else if (type.equals(TYPE_LOCAL_DATE_TIME) || type.equals(TYPE_OFFSET_DATE_TIME) 
                                || type.equals(TYPE_ZONED_DATE_TIME))
                        {
                            throw new ConverterException(
                                    _MessageUtils.getErrorMessage(facesContext, DATETIME_ID, args));
                        }
                        else
                        {
                            throw new ConverterException("invalid type '" + _type + "'");
                        }
                    }
                }
                else
                {
                    DateFormat format = getDateFormat();
                    TimeZone tz = getTimeZone();
                    if( tz != null )
                    {
                        format.setTimeZone(tz);
                    }
                    try
                    {
                        return format.parse(value);
                    }
                    catch (ParseException e)
                    {
                        String type = getType();
                        Object[] args = new Object[]{value,
                                format.format(new Date()),_MessageUtils.getLabel(facesContext, uiComponent)};

                        if(type.equals(TYPE_DATE))
                        {
                            throw new ConverterException(_MessageUtils.getErrorMessage(facesContext, DATE_ID, args));
                        }
                        else if (type.equals(TYPE_TIME))
                        {
                            throw new ConverterException(_MessageUtils.getErrorMessage(facesContext, TIME_ID, args));
                        }
                        else if (type.equals(TYPE_BOTH))
                        {
                            throw new ConverterException(
                                    _MessageUtils.getErrorMessage(facesContext, DATETIME_ID, args));
                        }
                        else
                        {
                            throw new ConverterException("invalid type '" + _type + "'");
                        }
                    }
                }
            }
        }
        return null;
    }

    public String getAsString(FacesContext facesContext, UIComponent uiComponent, Object value)
    {
        if (facesContext == null)
        {
            throw new NullPointerException("facesContext");
        }
        if (uiComponent == null)
        {
            throw new NullPointerException("uiComponent");
        }

        if (value == null)
        {
            return "";
        }
        if (value instanceof String)
        {
            return (String)value;
        }

        if (isJava8DateTimeFormatter())
        {
            DateTimeFormatter format = getDateTimeFormatter();
            
            if (value instanceof TemporalAccessor)
            {
                try
                {
                    return format.format((TemporalAccessor) value);
                }
                catch (Exception e)
                {
                    throw new ConverterException(_MessageUtils.getErrorMessage(facesContext, STRING_ID,
                            new Object[]{value,_MessageUtils.getLabel(facesContext, uiComponent)}),e);
                }
            }
            return null;
        }
        else
        {        
            DateFormat format = getDateFormat();
            TimeZone tz = getTimeZone(); 
            if (tz != null)
            {
                format.setTimeZone(tz);
            }
            try
            {
                return format.format(value);
            }
            catch (Exception e)
            {
                throw new ConverterException(_MessageUtils.getErrorMessage(facesContext, STRING_ID,
                        new Object[]{value,_MessageUtils.getLabel(facesContext, uiComponent)}),e);
            }
        }
    }

    private DateFormat getDateFormat()
    {
        String type = getType();
        DateFormat format;
        if (_pattern != null)
        {
            try 
            {
                format = new SimpleDateFormat(_pattern, getLocale());
            } 
                catch (IllegalArgumentException iae)
            {
                throw new ConverterException("Invalid pattern", iae);    
            }
        }
        else if (type.equals(TYPE_DATE))
        {
            format = DateFormat.getDateInstance(calcStyle(getDateStyle()), getLocale());
        }
        else if (type.equals(TYPE_TIME))
        {
            format = DateFormat.getTimeInstance(calcStyle(getTimeStyle()), getLocale());
        }
        else if (type.equals(TYPE_BOTH))
        {
            format = DateFormat.getDateTimeInstance(calcStyle(getDateStyle()),
                                                    calcStyle(getTimeStyle()),
                                                    getLocale());
        }
        else
        {
            throw new ConverterException("invalid type '" + _type + "'");
        }
        
        // format cannot be lenient (JSR-127)
        format.setLenient(false);
        return format;
    }
    
    private DateTimeFormatter getDateTimeFormatter()
    {
        DateTimeFormatter formatter = null;
        String type = getType();
        String pattern = getPattern();
        if (pattern != null && pattern.length() > 0)
        {
            Locale locale = getLocale();
            if (locale == null)
            {
                formatter = DateTimeFormatter.ofPattern(pattern);
            }
            else
            {
                formatter = DateTimeFormatter.ofPattern(pattern, locale);
            }
        }
        else
        {
            if (TYPE_LOCAL_DATE.equals(type))
            {
                formatter = DateTimeFormatter.ofLocalizedDate(calcFormatStyle(getDateStyle()));
            }
            else if (TYPE_LOCAL_DATE_TIME.equals(type) )
            {
                String timeStyle = getTimeStyle();
                if (timeStyle != null && timeStyle.length() > 0)
                {
                    formatter = DateTimeFormatter.ofLocalizedDateTime(
                            calcFormatStyle(getDateStyle()), calcFormatStyle(timeStyle));
                }
                else
                {
                    formatter = DateTimeFormatter.ofLocalizedDateTime(
                            calcFormatStyle(getDateStyle()));
                }
            }
            else if (TYPE_LOCAL_TIME.equals(type) )
            {
                formatter = DateTimeFormatter.ofLocalizedTime(calcFormatStyle(getTimeStyle()));
            }
            else if (TYPE_OFFSET_TIME.equals(type))
            {
                formatter = DateTimeFormatter.ISO_OFFSET_TIME;
            }
            else if (TYPE_OFFSET_DATE_TIME.equals(type))
            {
                formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            }
            else if (TYPE_ZONED_DATE_TIME.equals(type))
            {
                formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
            }
            
            Locale locale = getLocale();
            if (locale != null)
            {
                formatter = formatter.withLocale(locale);
            }
        }
        return formatter;
    }
    
    /**
     * According to java8 api, parse() also receives a TemporalQuery parameter that works as a qualifier to decide
     * how to parse and return the right type of value.
     * 
     * @return 
     */
    private TemporalQuery getTemporalQuery()
    {
        String type = getType();
        if (TYPE_LOCAL_DATE.equals(type))
        {
            return LocalDate::from;
        }
        else if (TYPE_LOCAL_DATE_TIME.equals(type) )
        {
            return LocalDateTime::from;            
        }
        else if (TYPE_LOCAL_TIME.equals(type) )
        {
            return LocalTime::from;
        }
        else if (TYPE_OFFSET_TIME.equals(type))
        {
            return OffsetTime::from;
        }
        else if (TYPE_OFFSET_DATE_TIME.equals(type))
        {
            return OffsetDateTime::from;
        }
        else if (TYPE_ZONED_DATE_TIME.equals(type))
        {
            return ZonedDateTime::from;
        }
        return null;
    }
    
    private FormatStyle calcFormatStyle(String name)
    {
        if (name.equals(STYLE_DEFAULT))
        {
            return FormatStyle.MEDIUM;
        }
        if (name.equals(STYLE_MEDIUM))
        {
            return FormatStyle.MEDIUM;
        }
        if (name.equals(STYLE_SHORT))
        {
            return FormatStyle.SHORT;
        }
        if (name.equals(STYLE_LONG))
        {
            return FormatStyle.LONG;
        }
        if (name.equals(STYLE_FULL))
        {
            return FormatStyle.FULL;
        }
        
        throw new ConverterException("invalid style '" + name + "'");
    }

    private int calcStyle(String name)
    {
        if (name.equals(STYLE_DEFAULT))
        {
            return DateFormat.DEFAULT;
        }
        if (name.equals(STYLE_MEDIUM))
        {
            return DateFormat.MEDIUM;
        }
        if (name.equals(STYLE_SHORT))
        {
            return DateFormat.SHORT;
        }
        if (name.equals(STYLE_LONG))
        {
            return DateFormat.LONG;
        }
        if (name.equals(STYLE_FULL))
        {
            return DateFormat.FULL;
        }

        throw new ConverterException("invalid style '" + name + "'");
    }
    
    private boolean isJava8DateTimeFormatter()
    {
        String type = getType();
        if (type != null)
        {
            return  TYPE_LOCAL_DATE.equals(type) ||
                    TYPE_LOCAL_TIME.equals(type) ||
                    TYPE_LOCAL_DATE_TIME.equals(type) ||
                    TYPE_OFFSET_TIME.equals(type) ||
                    TYPE_OFFSET_DATE_TIME.equals(type) ||
                    TYPE_ZONED_DATE_TIME.equals(type);
        }
        else
        {
            return false;
        }
    }

    // STATE SAVE/RESTORE
    public void restoreState(FacesContext facesContext, Object state)
    {
        if (state != null)
        {
            Object[] values = (Object[])state;
            _dateStyle = (String)values[0];
            _locale = (Locale)values[1];
            _pattern = (String)values[2];
            _timeStyle = (String)values[3];
            _timeZone = (TimeZone)values[4];
            _type = (String)values[5];
        }
    }

    public Object saveState(FacesContext facesContext)
    {
        if (!initialStateMarked())
        {
            Object[] values = new Object[6];
            values[0] = _dateStyle;
            values[1] = _locale;
            values[2] = _pattern;
            values[3] = _timeStyle;
            values[4] = _timeZone;
            values[5] = _type;
            return values;
        }
        return null;
    }

    // GETTER & SETTER
    
    /**
     * The style of the date.  Values include: default, short, medium, 
     * long, and full.
     * 
     */
    @JSFProperty
    public String getDateStyle()
    {
        return _dateStyle != null ? _dateStyle : STYLE_DEFAULT;
    }

    public void setDateStyle(String dateStyle)
    {
        //TODO: validate timeStyle
        _dateStyle = dateStyle;
        clearInitialState();
    }

    /**
     * The name of the locale to be used, instead of the default.
     * 
     */
    @JSFProperty
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

    /**
     * The style of the time.  Values include:  default, short, medium, long, 
     * and full.
     * 
     */
    @JSFProperty
    public String getTimeStyle()
    {
        return _timeStyle != null ? _timeStyle : STYLE_DEFAULT;
    }

    public void setTimeStyle(String timeStyle)
    {
        //TODO: validate timeStyle
        _timeStyle = timeStyle;
        clearInitialState();
    }

    /**
     * The time zone to use instead of GMT (the default timezone). When
     * this value is a value-binding to a TimeZone instance, that
     * timezone is used. Otherwise this value is treated as a String
     * containing a timezone id, ie as the ID parameter of method
     * java.util.TimeZone.getTimeZone(String).
     * 
     */
    @JSFProperty
    public TimeZone getTimeZone()
    {
        return _timeZone != null ? _timeZone : TIMEZONE_DEFAULT;
    }

    public void setTimeZone(TimeZone timeZone)
    {
        _timeZone = timeZone;
        clearInitialState();
    }

    public boolean isTransient()
    {
        return _transient;
    }

    public void setTransient(boolean aTransient)
    {
        _transient = aTransient;
    }

    /**
     * Specifies whether the date, time, or both should be 
     * parsed/formatted.  Values include:  date, time, and both.
     * Default based on setting of timeStyle and dateStyle.
     * 
     */
    @JSFProperty
    public String getType()
    {
        return _type != null ? _type : TYPE_DATE;
    }

    public void setType(String type)
    {
        //TODO: validate type
        _type = type;
        clearInitialState();
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
}
