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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

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
public class AutomaticTimerBean {

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

    AutomaticTimerBean(BeanMetaData bmd, List<TimerMethodData> methods) {
        ivBMD = bmd;
        ivMethods = methods;

        int numPersistent = 0;
        int numNonPersistent = 0;

        for (TimerMethodData timerMethod : methods) {
            for (TimerMethodData.AutomaticTimer timer : timerMethod.getAutomaticTimers()) {
                if (timer.isPersistent()) {
                    numPersistent++;
                } else {
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
    public BeanMetaData getBeanMetaData() {
        return ivBMD;
    }

    /**
     * Returns the list of automatic timer methods associated with this bean.
     *
     * @return the automatic timer methods
     */
    public List<TimerMethodData> getMethods() {
        return ivMethods;
    }

    /**
     * Returns the number of persistent automatic timers associated with this
     * bean.
     *
     * @return the number of persistent timers
     */
    public int getNumPersistentTimers() {
        return ivNumPersistent;
    }

    /**
     * Returns the number of non-persistent automatic timers associated with
     * this bean.
     *
     * @return the number of non-persistent timers
     */
    public int getNumNonPersistentTimers() {
        return ivNumNonPersistent;
    }

    /**
     * Gets the partially formed BeanId for this bean. The resulting
     * BeanId will not have a home reference.
     *
     * @return the partially formed BeanId
     */
    public BeanId getBeanId() {
        if (ivBeanId == null) {
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
    public ParsedScheduleExpression parseScheduleExpression(TimerMethodData.AutomaticTimer timer) {
        ScheduleExpression schedule = timer.getSchedule();
        String start = timer.getStart();
        String end = timer.getEnd();

        try {
            if (start != null) {
                schedule.start(parseXSDDateTime("start", start));
            }

            if (end != null) {
                schedule.end(parseXSDDateTime("end", end));
            }

            return ScheduleExpressionParser.parse(schedule);
        } catch (ScheduleExpressionParserException ex) {
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
    private Date parseXSDDateTime(String fieldName, String dateTime) {
        try {
            return new Date(parse(dateTime));
        } catch (IllegalArgumentException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".parseXSDDateTime", "566", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "failed to parse schedule date/time", ex);
            throw new ScheduleExpressionParserException(ScheduleExpressionParserException.Error.VALUE, fieldName, dateTime); // d660135
        }
    }

    static long parse(String dateTime) {
        try {
            return parseJavaTime(dateTime);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static long parseJavaTime(String dateTime) {
        // intentionally fail some input to be consistent with JAXB behavior
        // ccyy-MM-ddThh:mm
        // ccyy-MM-ddThh:mm:ss+hh:mm:ss
        if (Pattern.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}", dateTime) ||
            Pattern.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\+|\\-)\\d{2}:\\d{2}:\\d{2}", dateTime)) {
            throw new IllegalArgumentException(dateTime + " is not a valid xsd:dateTime format");
        }

        // validate input for creating java.time.Instant, minimum pattern to work is ccyy-MM-ddThh:mm:ss
        // so try to pad dateTime with 0s to get to it
        // (c)entury (y)ear (M)onth (d)ay (h)our (m)inute (s)econd .(m)illeseconds Z(UTC) +- Timezone Offset (hh:mm)
        // ccyy
        if (Pattern.matches("\\d{4}", dateTime))
            dateTime += "-01-01T00:00:00";
        // ccyy-MM
        else if (Pattern.matches("\\d{4}-\\d{2}", dateTime))
            dateTime += "-01T00:00:00";
        // can input only 1 digit for seconds but its a ones not a tens, so we insert a 0 in front of it
        // hh:mm:s
        // hh:mm:sZ
        // hh:mm:s(+|-)hh:mm
        // hh:mm:s.(m+)(+|-)hh:mm
        else if (Pattern.matches("\\d{2}:\\d{2}:\\d(\\.\\d+)*(((\\+|\\-)\\d{2}:\\d{2})|Z)?", dateTime)) {
            dateTime = dateTime.substring(0, 6) + "0" + dateTime.substring(6, dateTime.length());
            dateTime = "1970-01-01T" + dateTime;
        }
        // hh:mm:ss
        // hh:mm:ssZ
        // hh:mm:ss(+|-)hh:mm
        // hh:mm:ss.(m+)(+|-)hh:mm
        else if (Pattern.matches("\\d{2}:\\d{2}:\\d{2}(\\.\\d+)*(((\\+|\\-)\\d{2}:\\d{2})|Z)?", dateTime))
            dateTime = "1970-01-01T" + dateTime;
        // ccyy-MM-dd
        else if (Pattern.matches("\\d{4}-\\d{2}-\\d{2}", dateTime))
            dateTime += "T00:00:00";
        // ccyy-MM-ddZ
        else if (Pattern.matches("\\d{4}-\\d{2}-\\d{2}Z", dateTime)) {
            dateTime = dateTime.substring(0, dateTime.length() - 1);
            dateTime += "T00:00:00Z";
        }
        // ccyy-MM-dd(+|-)hh:mm
        else if (Pattern.matches("\\d{4}-\\d{2}-\\d{2}(\\+|\\-)\\d{2}:\\d{2}", dateTime))
            dateTime = dateTime.substring(0, 10) + "T00:00:00" + dateTime.substring(10, dateTime.length());
        // can input only 1 digit for seconds but its a ones not a tens, so we insert a 0 in front of it
        // ccyy-MM-ddThh:mm:s
        // ccyy-MM-ddThh:mm:sZ
        // ccyy-MM-ddThh:mm:s(+-)hh:mm
        // ccyy-MM-ddThh:mm:s.(m+)(+|-)hh:mm
        else if (Pattern.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{1}(\\.\\d+)*(((\\+|\\-)\\d{2}:\\d{2})|Z)?", dateTime)) {
            dateTime = dateTime.substring(0, 17) + "0" + dateTime.substring(17, dateTime.length());
        }

        // if dateTime has >4 .millisecond digits, strip them out
        if (Pattern.matches(".*\\.\\d{4}\\d+.*", dateTime)) {
            String[] split = dateTime.split("\\.", 2);
            String[] split2 = split[1].split("(?<=\\d{4})", 2);
            String[] split3 = split2[1].split("(?!\\d)", 2);
            dateTime = split[0] + "." + split2[0];
            dateTime = split3.length > 1 ? dateTime + split3[1] : dateTime;
        }

        // Finally, take dateTime and parse it using java.time APIs.
        TemporalAccessor accessor = DateTimeFormatter.ofPattern("yyyy[-MM-dd]['T'HH:mm][:ss][.SSSSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S][z]").withZone(ZoneId.systemDefault()).parse(dateTime);
        return Instant.from(accessor).toEpochMilli();
    }
}
