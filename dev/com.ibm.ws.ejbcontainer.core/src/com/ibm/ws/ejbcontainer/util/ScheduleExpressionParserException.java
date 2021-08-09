/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.util;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Indicates that an error has occurred while parsing a schedule expression.
 */
public class ScheduleExpressionParserException
                extends IllegalArgumentException
{
    private static final long serialVersionUID = -5763492536412342693L;

    private static final TraceComponent tc = Tr.register(ScheduleExpressionParserException.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    public static enum Error
    {
        VALUE(
              "AUTOMATIC_TIMER_SCHEDULE_VALUE_CNTR0211E",
              "value not valid"
        ),
        VALUE_RANGE(
                    "AUTOMATIC_TIMER_SCHEDULE_VALUE_RANGE_CNTR0212E",
                    "value is out of range"
        ),
        RANGE_BOUND(
                    "AUTOMATIC_TIMER_SCHEDULE_RANGE_BOUND_CNTR0213E",
                    "range bound value is not valid"
        ),
        INCREMENT_INTERVAL(
                           "AUTOMATIC_TIMER_INVALID_RANGE_VARIABLE_DAY_OF_WEEK_CNTR0214E",
                           "interval value for increment is not valid"
        ),
        LIST_VALUE(
                   "AUTOMATIC_TIMER_INVALID_RANGE_VARIABLE_DAY_OF_WEEK_CNTR0215E",
                   "list value is not valid"
        ),
        UNINCREMENTABLE(
                        "AUTOMATIC_TIMER_INVALID_RANGE_VARIABLE_DAY_OF_WEEK_CNTR0216E",
                        "increment is not valid"
        ),
        MISSING_DAY_OF_WEEK(
                            "AUTOMATIC_TIMER_INVALID_RANGE_VARIABLE_DAY_OF_WEEK_CNTR0217E",
                            "expected day of week after ordinal"
        );

        private String ivMessageId;
        private String ivMessage;

        Error(String messageId, String message)
        {
            ivMessageId = messageId;
            ivMessage = message;
        }

        public String getMessage()
        {
            return ivMessage;
        }

        public String getMessageId()
        {
            return ivMessageId;
        }
    }

    private Error ivError;
    private String ivField;

    public ScheduleExpressionParserException(Error error, String field, String string)
    {
        super(field + ": " + error.getMessage() + " in string: " + string); // d660135
        ivError = error;
        ivField = field;
    }

    /**
     * Logs an error message corresponding to this exception.
     * 
     * @param moduleName the module name
     * @param beanName the bean name
     */
    public void logError(String moduleName, String beanName, String methodName)
    {
        Tr.error(tc, ivError.getMessageId(), new Object[] { beanName, moduleName, methodName, ivField });
    }
}
