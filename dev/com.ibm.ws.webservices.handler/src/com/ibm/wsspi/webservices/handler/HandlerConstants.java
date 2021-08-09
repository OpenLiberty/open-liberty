/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webservices.handler;

/**
 * The properties associated with the registered handlers specify what engine type and flow type the handlers will be
 * executed. Valid service properties are listed
 * as constants below with descriptive javadoc.
 */
public class HandlerConstants {

    /**
     * <h4>Service property</h4>
     * 
     * The value of this property should be a boolean: if true, the handler will take effect
     * in Server Side. if false, the handler will not take effect in Server side
     * <ul>
     * <li>true is default value</li>
     * </ul>
     */
    public static final String IS_SERVER_SIDE = "isServerSide";

    /**
     * <h4>Service property</h4>
     * 
     * The value of this property should be a boolean: if true, the handler will take effect
     * in Client Side. if false, the handler will not take effect in Client side
     * <ul>
     * <li>true is default value</li>
     * </ul>
     */
    public static final String IS_CLIENT_SIDE = "isClientSide";

    /**
     * <h4>Service property</h4>
     * 
     * The value of this property determines the flow type the handler is to be performed.
     * Acceptable values are as follows:
     * <ul>
     * <li>{@value #FLOW_TYPE_IN} will take effect in IN Flow</li>
     * <li>{@value #FLOW_TYPE_OUT} will take effect in OUT Flow</li>
     * <li>{@value #FLOW_TYPE_INOUT} will take effect in both in IN and OUT Flow</li>
     * </ul>
     * <em>({@value #FLOW_TYPE_INOUT} is the default setting.)</em>
     */
    public static final String FLOW_TYPE = "flow.type";
    public static final String FLOW_TYPE_IN = "IN";
    public static final String FLOW_TYPE_OUT = "OUT";
    public static final String FLOW_TYPE_INOUT = "INOUT";

    /**
     * <h4>Service property</h4>
     * 
     * The value of this property determines the engine type the handler is to be performed.
     * Acceptable values are as follows:
     * <ul>
     * <li>{@value #ENGINE_TYPE_JAXWS} will take effect in JAXWS Engine</li>
     * <li>{@value #ENGINE_TYPE_JAXRS} will take effect in JAXRS Engine</li>
     * <li>{@value #ENGINE_TYPE_ALL} will take effect in both in JAXWS and JAXRS Engine</li>
     * </ul>
     * <em>({@value #ENGINE_TYPE_ALL} is the default setting.)</em>
     */
    public static final String ENGINE_TYPE = "engine.type";
    public static final String ENGINE_TYPE_JAXWS = "JAX_WS";
    public static final String ENGINE_TYPE_JAXRS = "JAX_RS";
    public static final String ENGINE_TYPE_ALL = "ALL";

}
