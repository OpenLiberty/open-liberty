/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.grpc.internal.monitor;

import com.ibm.websphere.monitor.meters.Meter;

import io.openliberty.grpc.GrpcClientStatsMXBean;

/**
 * This is used to report gRPC Client related statistics. 
 * </br>Statistic reported: 
 * <ul>
 * <li>Total number of RPCs started on the client.
 * <li>Total number of RPCs completed on the client, regardless of success or failure. 
 * <li>Histogram of RPC response latency for completed RPCs, in seconds.
 * <li>Total number of stream messages received from the server. 
 * <li>Total number of stream messages sent by the client.
 * </ul>
 */
public class GrpcClientStats extends Meter implements GrpcClientStatsMXBean {

}
