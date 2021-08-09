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

import java.lang.reflect.Method;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.ScheduleExpression;

import com.ibm.ejs.container.ContainerProperties;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Temporary storage for timer methods (timeout or automatic timers) prior to
 * creating EJBMethodInfoImpl.
 */
public class TimerMethodData implements Comparable<TimerMethodData>
{
    private static final TraceComponent tc = Tr.register(TimerMethodData.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    /**
     * The method for the data.
     */
    private final Method ivMethod;

    /**
     * The depth of the class containing ivMethod within the class hierarchy of
     * the bean.
     */
    private final int ivDepth;

    /**
     * The method ID. When the corresponding EJBMethodInfoImpl is created, it
     * must be created in the BeanMetaData.timedMethodInfos at this index.
     */
    int ivMethodId = -1;

    /**
     * The list of automatic timers for this method, if any.
     */
    private final List<AutomaticTimer> ivAutomaticTimers = new ArrayList<AutomaticTimer>();

    /**
     * Indicates if the method the timers are calling back into has 1 or 0 parms.
     */
    private final boolean ivMethodHas1Parm; //F743-15870

    /**
     * Creates a new timer method data.
     * 
     * @param method the method
     * @param depth the class hierarchy depth
     * @param methodHas1Parm true if method takes 1 parm, false if it takes 0 parms
     */
    public TimerMethodData(Method method, int depth, boolean methodHas1Parm) //F743-15870
    {
        ivMethod = method;
        ivDepth = depth;
        ivMethodHas1Parm = methodHas1Parm; //F743-15870
    }

    public int compareTo(TimerMethodData timer)
    {
        //F743-15870
        if (ivDepth != timer.ivDepth)
            return ivDepth < timer.ivDepth ? -1 : 1;
        int nameCompare = ivMethod.getName().compareTo(timer.ivMethod.getName());
        if (nameCompare != 0)
            return nameCompare; // compareTo has already determined an ordering
        if (ivMethodHas1Parm != timer.ivMethodHas1Parm)
            return ivMethodHas1Parm ? 1 : -1;
        return 0; // the methods are identical.
    }

    /**
     * Adds an automatic timer to this metadata.
     * 
     * @param timer the automatic timer
     */
    void addAutomaticTimer(AutomaticTimer timer)
    {
        timer.ivMethod = this;
        ivAutomaticTimers.add(timer);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "added automatic timer: " + timer);
    }

    /**
     * Return the method to be called for this data.
     * 
     * @return the method
     */
    public Method getMethod()
    {
        return ivMethod;
    }

    /**
     * Returns the method id for this metadata within the {@link BeanMetaData.timedObjectMethods} array.
     * 
     * @return the method id
     */
    public int getMethodId()
    {
        return ivMethodId;
    }

    /**
     * Returns the automatic timers for this timer method metadata.
     * 
     * @return the automatic timers
     */
    public List<AutomaticTimer> getAutomaticTimers()
    {
        return ivAutomaticTimers;
    }

    public static class AutomaticTimer
    {
        /**
         * True if this timer was specified in the XML deployment descriptor.
         */
        private final boolean ivXML;

        /**
         * The containing TimerMethodData. This value should only be set by {@link addAutomaticTimer}.
         */
        TimerMethodData ivMethod;

        /**
         * True if this timer is persistent.
         */
        private final boolean ivPersistent;

        /**
         * The schedule for this timer. Note that start and end will be unset
         * until the schedule is parsed. At that point, ivStart and ivEnd will
         * be parsed and set on this schedule.
         */
        private final ScheduleExpression ivSchedule;

        /**
         * The unparsed start field for the schedule. This field will be null
         * if the automatic timer metadata was specified in an annotation or if
         * the start was not specified.
         */
        private final String ivStart;

        /**
         * The unparsed end field for the schedule. This field will be null if
         * the automatic timer metadata was specified in an annotation or if the
         * end was not specified.
         */
        private final String ivEnd;

        /**
         * The user-provided info for the timer. This field can be null.
         */
        private final Serializable ivInfo;

        /**
         * Creates a new automatic timer.
         * 
         * @param xml
         * @param persistent
         * @param schedule
         * @param start
         * @param end
         * @param info
         */
        public AutomaticTimer(boolean xml,
                              boolean persistent,
                              ScheduleExpression schedule,
                              String start,
                              String end,
                              Serializable info)
        {
            ivXML = xml;
            ivPersistent = persistent;
            ivSchedule = schedule;
            ivStart = start;
            ivEnd = end;
            ivInfo = info;
        }

        @Override
        public String toString()
        {
            final String nl = ContainerProperties.LineSeparator;
            return super.toString() + nl
                   + "     Method     = " + (ivMethod == null ? null : ivMethod.getMethodId() + " : " + ivMethod.getMethod()) + nl
                   + "     Start      = " + ivStart + nl
                   + "     End        = " + ivEnd + nl
                   + "     Persistent = " + ivPersistent + nl
                   + "     Info       = " + ivInfo + nl;
        }

        /**
         * Returns true if the metadata for this automatic timer was defined in
         * the XML deployment descriptor.
         * 
         * @return true if this automatic timer came from XML
         */
        public boolean isXML()
        {
            return ivXML;
        }

        /**
         * Returns the method metadata containing this automatic timer.
         * 
         * @return the method for this metadata
         */
        public TimerMethodData getMethod()
        {
            return ivMethod;
        }

        /**
         * Returns true if this timer is persistent
         * 
         * @return true if this timer is persistent
         */
        public boolean isPersistent()
        {
            return ivPersistent;
        }

        /**
         * The schedule for this timer with null for start and end.
         * 
         * @return the schedule
         */
        public ScheduleExpression getSchedule()
        {
            return ivSchedule;
        }

        /**
         * Returns the unparsed start field for the schedule, or null if
         * unspecified.
         * 
         * return false;
         * 
         * @return the start field
         */
        public String getStart()
        {
            return ivStart;
        }

        /**
         * Returns the unparsed end field for the schedule, or null if
         * unspecified.
         * 
         * @return the end field
         */
        public String getEnd()
        {
            return ivEnd;
        }

        /**
         * Returns the user data.
         * 
         * @return the user data, or null
         */
        public Serializable getInfo()
        {
            return ivInfo;
        }
    }
}
