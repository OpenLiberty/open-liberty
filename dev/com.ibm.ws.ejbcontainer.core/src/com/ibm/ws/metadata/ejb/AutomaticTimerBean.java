/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.ejb.ScheduleExpression;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.util.ParsedScheduleExpression;
import com.ibm.ws.ejbcontainer.util.ScheduleExpressionParser;
import com.ibm.ws.ejbcontainer.util.ScheduleExpressionParserException;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Metadata for a bean with automatic timers.
 */
public class AutomaticTimerBean
{

    private static final String CLASS_NAME = AutomaticTimerBean.class.getName();
    private static final TraceComponent tc = Tr.register(AutomaticTimerBean.class, "EJBContainer", "com.ibm.ejs.container.container");
    /**
     * The bean metadata.
     */
    private final BeanMetaData ivBMD;

    /**
     * The list of timer methods for the bean.
     */
    private final List<TimerMethodData> ivMethods;

    /**
     * The number of persistent automatic timers for this bean.
     */
    private final int ivNumPersistent;

    /**
     * The number of non-persistent automatic timers for this bean.
     */
    private final int ivNumNonPersistent;

    /**
     * The partially formed BeanId for this bean. The BeanId will not have
     * a home reference. This value is initialized lazily by {@link getBeanId}.
     */
    private BeanId ivBeanId;

    AutomaticTimerBean(BeanMetaData bmd, List<TimerMethodData> methods)
    {
        ivBMD = bmd;
        ivMethods = methods;

        int numPersistent = 0;
        int numNonPersistent = 0;

        for (TimerMethodData timerMethod : methods)
        {
            for (TimerMethodData.AutomaticTimer timer : timerMethod.getAutomaticTimers())
            {
                if (timer.isPersistent())
                {
                    numPersistent++;
                }
                else
                {
                    numNonPersistent++;
                }
            }
        }

        ivNumPersistent = numPersistent;
        ivNumNonPersistent = numNonPersistent;
    }

    /**
     * Returns the metadata for the bean
     * 
     * @return the metadata for the bean
     */
    public BeanMetaData getBeanMetaData()
    {
        return ivBMD;
    }

    /**
     * Returns the list of automatic timer methods associated with this bean.
     * 
     * @return the automatic timer methods
     */
    public List<TimerMethodData> getMethods()
    {
        return ivMethods;
    }

    /**
     * Returns the number of persistent automatic timers associated with this
     * bean.
     * 
     * @return the number of persistent timers
     */
    public int getNumPersistentTimers()
    {
        return ivNumPersistent;
    }

    /**
     * Returns the number of non-persistent automatic timers associated with
     * this bean.
     * 
     * @return the number of non-persistent timers
     */
    public int getNumNonPersistentTimers()
    {
        return ivNumNonPersistent;
    }

    /**
     * Gets the partially formed BeanId for this bean. The resulting
     * BeanId will not have a home reference.
     * 
     * @return the partially formed BeanId
     */
    public BeanId getBeanId()
    {
        if (ivBeanId == null)
        {
            ivBeanId = new BeanId(ivBMD.j2eeName, null, false);
        }
        return ivBeanId;
    }

    /**
     * Parses the schedule for an automatic timer. If the parsing fails,
     * then a message is logged.
     * 
     * @param timer the automatic timer
     * @throws SchedulerExpressionParserException if the schedule is invalid
     */
    // F743-506, F743-14447 RTC109678
    public ParsedScheduleExpression parseScheduleExpression(TimerMethodData.AutomaticTimer timer)
    {
        ScheduleExpression schedule = timer.getSchedule();
        String start = timer.getStart();
        String end = timer.getEnd();

        try
        {
            if (start != null)
            {
                schedule.start(parseXSDDateTime("start", start));
            }

            if (end != null)
            {
                schedule.end(parseXSDDateTime("end", end));
            }

            return ScheduleExpressionParser.parse(schedule);
        } catch (ScheduleExpressionParserException ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".parseScheduleExpression", "541", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "failed to parse schedule expression", ex);

            // Use the exception to call Tr.error().
            ex.logError(ivBMD.j2eeName.getModule(), ivBMD.j2eeName.getComponent(), timer.getMethod().getMethod().getName());
            throw ex;
        }
    }

    /**
     * Parses a string in xsd:dateTime format into a java.util.Date.
     * 
     * @param dateTime the date/time string
     * @return the parsed Date
     * @throws ScheduleExpressionParserException if the string is invalid
     */
    // F743-506, F743-14447 RTC109678
    private Date parseXSDDateTime(String fieldName, String dateTime)
    {
        try
        {
            return new Date(DateHelper.parse(dateTime));
        } catch (IllegalArgumentException ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".parseXSDDateTime", "566", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "failed to parse schedule date/time", ex);
            throw new ScheduleExpressionParserException(ScheduleExpressionParserException.Error.VALUE, fieldName, dateTime); // d660135
        }
    }

    /**
     * The DatatypeConverter is only guaranteed to work once the JAX-B provider has
     * set it up. The JVM supports this always by using a default DatatypeConverter,
     * but the jaxb-2.2 feature does not. So this causes us to initialize the JAX-B
     * runtime before calling the parse method.
     */
    private static final class DateHelper {
        private static boolean initialized = false;

        public static long parse(String dateTime) {
            try {
                // Reflection is needed here so that ejbcontainer can avoid a dependency on JAX-B, which
                // is being removed from the JDK in Java 9.  If we are JDK <9 we will continue to use JAX-B,
                // and if we are JDK >=9 we will use the java.time APIs that were introduced in JDK 8.
                boolean isJAXBAvailable = System.getProperty("java.version").startsWith("1.");
                if (isJAXBAvailable) {
                    if (!initialized) {
                        // JAXBContext.newInstance(DateHelper.class);
                        Class.forName("javax.xml.bind.JAXBContext").getMethod("newInstance", Class[].class)//
                                        .invoke(null, new Object[] { new Class[] { DateHelper.class } });
                        initialized = true;
                    }
                    // return DatatypeConverter.parseDateTime(dateTime).getTimeInMillis();
                    Calendar calendar = (Calendar) Class.forName("javax.xml.bind.DatatypeConverter").getMethod("parseDateTime", String.class).invoke(null, dateTime);
                    return calendar.getTimeInMillis();
                } else {
                    // TemporalAccessor accessor = DateTimeFormatter
                    // .ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S][z]")
                    // .withZone(ZoneId.systemDefault()).parse(date);
                    // long millis = Instant.from(accessor).toEpochMilli();
                    Class<?> DateTimeFormatter = Class.forName("java.time.format.DateTimeFormatter");
                    Class<?> ZoneId = Class.forName("java.time.ZoneId");
                    Class<?> TemporalAccessor = Class.forName("java.time.temporal.TemporalAccessor");
                    Class<?> Instant = Class.forName("java.time.Instant");
                    Object formatter = DateTimeFormatter.getMethod("ofPattern", String.class)//
                                    .invoke(null,"yyyy-MM-dd'T'HH:mm:ss[.SSSSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S][z]");
                    Object defaultZone = ZoneId.getMethod("systemDefault").invoke(null);
                    formatter = DateTimeFormatter.getMethod("withZone", ZoneId).invoke(formatter, defaultZone);
                    Object accessor = DateTimeFormatter.getMethod("parse", CharSequence.class).invoke(formatter, dateTime);
                    Object instant = Instant.getMethod("from", TemporalAccessor).invoke(null, accessor);
                    return (long) Instant.getMethod("toEpochMilli").invoke(instant);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
