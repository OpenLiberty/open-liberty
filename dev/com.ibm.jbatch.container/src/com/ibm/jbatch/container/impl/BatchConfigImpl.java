/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.ibm.jbatch.container.impl;

import java.util.Properties;

import com.ibm.jbatch.spi.DatabaseConfigurationBean;
import com.ibm.jbatch.spi.services.IBatchConfig;

public class BatchConfigImpl implements IBatchConfig {
	
	protected boolean j2seMode = false;
	protected DatabaseConfigurationBean databaseConfigBean = null;
	protected Properties configProperties = null;

	@Override
	public boolean isJ2seMode() {
		return j2seMode;
	}

	public void setJ2seMode(boolean j2seMode) {
		this.j2seMode = j2seMode;
	}
	
	@Override
	public DatabaseConfigurationBean getDatabaseConfigurationBean() {
		return databaseConfigBean;
	}

	public void setDatabaseConfigurationBean(DatabaseConfigurationBean databaseConfigBean) {
		this.databaseConfigBean = databaseConfigBean;
	}

	@Override
	public Properties getConfigProperties() {
		return configProperties;
	}
	
    public void setConfigProperties(Properties configProperties) {
		this.configProperties = configProperties;
	}
}
