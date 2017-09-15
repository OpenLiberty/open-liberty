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

import java.util.Date;
import java.util.List;

import javax.ejb.ScheduleExpression;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

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
        static {
            try {
                JAXBContext.newInstance(DateHelper.class);
            } catch (JAXBException e) {
            }
        }

        public static long parse(String dateTime) {
            return DatatypeConverter.parseDateTime(dateTime).getTimeInMillis();
        }
    }
}
