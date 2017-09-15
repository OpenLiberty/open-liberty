/*
 * Copyright 2013 International Business Machines Corp.
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
package com.ibm.jbatch.spi;

public class DatabaseConfigurationBean {
	
	protected String jndiName = "";
	protected String jdbcDriver = "";
	protected String dbUser = "";
	protected String dbPassword = "";
	protected String jdbcUrl = "";
	protected String schema = "";
	
	public String getSchema() {
		return this.schema;
	}
	
	public void setSchema(String schema) {
		this.schema = schema;
	}
	
	public String getJndiName() {
		return this.jndiName;
	}

	public String getJdbcDriver() {
		return this.jdbcDriver;
	}

	public String getDbUser() {
		return this.dbUser;
	}

	public String getDbPassword() {
		return this.dbPassword;
	}
	
	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
	}

	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}
	
	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}
	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}
	
}
