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

package com.ibm.ws.jbatch.wola.internal;

import javax.json.JsonObject;

/**
 * A wrapper for the incoming Json request from the native client, with some
 * utility methods to obtain specific items from the json request
 */
public class BatchWolaRequest {

	/**
	 * createTime field of the json request
	 */
	private String createTime;

	/**
	 * instanceState field of the json request
	 */
	private String instanceState;

	/**
	 * exitStatus field of the json request
	 */
	private String exitStatus;

	/**
	 * page field of the json request
	 */
	private int page;

	/**
	 * pageSize field of the json request
	 */
	private int pageSize;

	/**
	 * purgeJobStoreOnly field of the json request
	 */
	private boolean purgeJobStoreOnly;

	/**
	 * instanceId field of the json request
	 */
	private String instanceId;

	/**
	 * ctor
	 */
	public BatchWolaRequest(JsonObject request) {
		createTime = request.getString("createTime", null);
		instanceState = request.getString("instanceState", null);
		exitStatus = request.getString("exitStatus", null);
		instanceId = request.getString("jobInstanceId", null);
		page = BatchWolaJsonHelper.parseInt(request, "page", 0);
		pageSize = BatchWolaJsonHelper.parseInt(request, "pageSize", 50);

		if (request.toString().contains("purgeJobStoreOnly"))
			purgeJobStoreOnly = true;
		else
			purgeJobStoreOnly = false;
	}

	/**
	 * Get the value specified for the --jobInstanceId or --instanceId argument (it
	 * will get normalized to instanceId before the request is sent from the native
	 * client)
	 * 
	 * @return the instance id value(s) specified, or null if not specified
	 */
	public String getInstanceId() {
		return instanceId;
	}

	/**
	 * Get the value specified for the --page argument
	 * 
	 * @return the --page arg, or 0 if not specified.
	 */
	public int getPage() {
		return page;
	}

	/**
	 * Get the value specified for the --pageSize argument
	 * 
	 * @return the --pageSize arg, or 50 if not specified
	 */
	public int getPageSize() {
		return pageSize;
	}

	/**
	 * Get the value specified for the --createTime argument
	 * 
	 * @return the --createTime arg, or null if not specified
	 */
	public String getCreateTime() {
		return createTime;
	}

	/**
	 * Get the value specified for the --instanceState argument
	 * 
	 * @return the --instanceState arg, or null if not specified
	 */
	public String getInstanceState() {
		return instanceState;
	}

	/**
	 * Get the value specified for the --exitStatus argument
	 * 
	 * @return the --exitStatus arg, or null if not specified
	 */
	public String getExitStatus() {
		return exitStatus;
	}

	/**
	 * Determine whether the --purgeJobStoreOnly argument was specified
	 * 
	 * @return true if --purgeJobStoreOnly was specified, otherwise false
	 */
	public boolean getPurgeJobStoreOnly() {
		return purgeJobStoreOnly;
	}

}
