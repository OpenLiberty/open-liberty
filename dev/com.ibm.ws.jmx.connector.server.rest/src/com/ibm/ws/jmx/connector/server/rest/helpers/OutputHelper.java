/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.helpers;

import java.io.OutputStream;
import java.io.Writer;

import javax.management.AttributeList;
import javax.management.Notification;
import javax.management.NotificationFilter;

import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.converter.NotificationRecord;
import com.ibm.ws.jmx.connector.datatypes.JMXServerInfo;
import com.ibm.ws.jmx.connector.datatypes.MBeanInfoWrapper;
import com.ibm.ws.jmx.connector.datatypes.NotificationArea;
import com.ibm.ws.jmx.connector.datatypes.ObjectInstanceWrapper;
import com.ibm.ws.jmx.connector.datatypes.ServerNotificationRegistration;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.rest.handler.RESTResponse;

public class OutputHelper {

    /**
     * Write a Java Integer to the response
     */
    public static void getIntegerStreamingOutput(final RESTResponse response, final Integer value, final JSONConverter converter) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);

        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();

            //Converter the value
            converter.writeInt(outStream, value);

            //Return converter
            JSONConverter.returnConverter(converter);

            outStream.flush();
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
        }
    }

    /**
     * Write ServerNotificationRegistration to response
     */
    public static void writeServerRegistrationStreamingOutput(final RESTResponse response, final ServerNotificationRegistration value, final JSONConverter converter) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);

        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();

            //Converter the value
            converter.writeServerNotificationRegistration(outStream, value);

            //Return converter
            JSONConverter.returnConverter(converter);

            outStream.flush();
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
        }
    }

    /**
     * Write a StreamingOutput for JMXServerInfo objects
     */
    public static void writeJMXStreamingOutput(final RESTResponse response, final JMXServerInfo value, final JSONConverter converter) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);

        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();

            //Converter the value
            converter.writeJMX(outStream, value);

            //Return converter
            JSONConverter.returnConverter(converter);

            outStream.flush();
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
        }
    }

    /**
     * Write a Java String to the response
     */
    public static void writeStringStreamingOutput(final RESTResponse response, String value, final JSONConverter converter) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);

        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();

            //Converter the value
            converter.writeString(outStream, value);

            //Return converter
            JSONConverter.returnConverter(converter);

            outStream.flush();
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
        }
    }

    /**
     * Write a Java String Array to the response
     */
    public static void writeStringArrayStreamingOutput(final RESTResponse response, final String[] value, final JSONConverter converter) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);

        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();

            //Converter the value
            converter.writeStringArray(outStream, value);

            //Return converter
            JSONConverter.returnConverter(converter);

            outStream.flush();
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
        }
    }

    /**
     * Write NotificationFilter Array to response
     */
    public static void writeNotificationFilterArrayStreamingOutput(final RESTResponse response, final NotificationFilter[] value, final JSONConverter converter) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);

        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();

            //Converter the value
            converter.writeNotificationFilters(outStream, value);

            //Return converter
            JSONConverter.returnConverter(converter);

            outStream.flush();
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
        }
    }

    /**
     * Write an array of ObjectInstanceWrapper objects to response
     */
    public static void writeObjectInstanceArrayOutput(final RESTResponse response, final ObjectInstanceWrapper[] value, final JSONConverter converter) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);

        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();

            //Converter the value
            converter.writeObjectInstanceArray(outStream, value);

            //Return converter
            JSONConverter.returnConverter(converter);

            outStream.flush();
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
        }
    }

    /**
     * Write MBeanInfoWrapper object to the response
     */
    public static void writeMBeanInfoOutput(final RESTResponse response, final MBeanInfoWrapper value, final JSONConverter converter) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);

        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();

            //Converter the value
            converter.writeMBeanInfo(outStream, value);

            //Return converter
            JSONConverter.returnConverter(converter);

            outStream.flush();
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
        }
    }

    /**
     * Write POJO to the response
     */
    public static void writePOJOOutput(final RESTResponse response, final Object value, final JSONConverter converter) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);

        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();

            //Converter the value
            converter.writePOJO(outStream, value);

            //Return converter
            JSONConverter.returnConverter(converter);

            outStream.flush();
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
        }
    }

    public static void writeJsonOutput(final RESTResponse response, final String json) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);
        writeStringOutput(response, json);
    }

    public static void writeTextOutput(final RESTResponse response, final String text) {
        response.setContentType(APIConstants.MEDIA_TYPE_TEXT_PLAIN);
        writeStringOutput(response, text);
    }

    public static void writeStringOutput(final RESTResponse response, final String str) {
        if (str == null) {
            response.setStatus(APIConstants.STATUS_NO_CONTENT);
        } else {
            Writer writer = null;
            try {
                writer = response.getWriter();
                writer.write(str);
                writer.flush();
            } catch (Exception e) {
                throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
            } finally {
                FileUtils.tryToClose(writer);
            }
        }
    }

    /**
     * Write ObjectInstanceWrapper to the response
     */
    public static void writeObjectInstanceOutput(final RESTResponse response, final ObjectInstanceWrapper value, final JSONConverter converter) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);

        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();

            //Converter the value
            converter.writeObjectInstance(outStream, value);

            //Return converter
            JSONConverter.returnConverter(converter);

            outStream.flush();
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
        }
    }

    /**
     * Write AttributeList to the response
     */
    public static void writeAttributeListOutput(final RESTResponse response, final AttributeList value, final JSONConverter converter) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);

        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();

            //Converter the value
            converter.writeAttributeList(outStream, value);

            //Return converter
            JSONConverter.returnConverter(converter);

            outStream.flush();
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
        }
    }

    /**
     * Write NotificationArea to response
     */
    public static void writeNotificationAreaOutput(final RESTResponse response, final NotificationArea value, final JSONConverter converter) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);

        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();

            //Converter the value
            converter.writeNotificationArea(outStream, value);

            //Return converter
            JSONConverter.returnConverter(converter);

            outStream.flush();
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
        }
    }

    /**
     * Write Notification[] to response
     */
    public static void writeNotificationArrayOutput(final RESTResponse response, final Notification[] value, final JSONConverter converter) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);

        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();

            //Converter the value
            converter.writeNotifications(outStream, value);

            //Return converter
            JSONConverter.returnConverter(converter);

            outStream.flush();
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
        }
    }

    /**
     * Write NotificationRecord[] to response
     */
    public static void writeNotificationArrayOutput(final RESTResponse response, final NotificationRecord[] value, final JSONConverter converter) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);

        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();

            //Converter the value
            converter.writeNotificationRecords(outStream, value);

            //Return converter
            JSONConverter.returnConverter(converter);

            outStream.flush();
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
        }
    }

    /**
     * Write a boolean value to response
     */
    public static void writeBooleanOutput(final RESTResponse response, final boolean value, final JSONConverter converter) {
        response.setContentType(APIConstants.MEDIA_TYPE_APPLICATION_JSON);

        OutputStream outStream = null;
        try {
            outStream = response.getOutputStream();

            //Converter the value
            converter.writeBoolean(outStream, value);

            //Return converter
            JSONConverter.returnConverter(converter);

            outStream.flush();
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
        }
    }
}
