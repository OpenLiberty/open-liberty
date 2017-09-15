/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.commonext;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Component(configurationPid = "com.ibm.ws.javaee.dd.commonext.GlobalTransaction",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class GlobalTransactionComponentImpl implements com.ibm.ws.javaee.dd.commonext.GlobalTransaction {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.commonext.GlobalTransaction delegate;
     protected Boolean send_wsat_context;
     protected Integer transaction_time_out;

     @Activate
     protected void activate(Map<String, Object> config) {
          this.configAdminProperties = config;
          send_wsat_context = (Boolean) config.get("send-wsat-context");
          transaction_time_out = (Integer) config.get("transaction-time-out");
     }

     @Override
     public boolean isSetSendWSATContext() {
          return (send_wsat_context!= null);
     }

     @Override
     public boolean isSendWSATContext() {
          if (delegate == null) {
               return send_wsat_context == null ? false : send_wsat_context;
          } else {
               return send_wsat_context == null ? delegate.isSendWSATContext() : send_wsat_context;
          }
     }

     @Override
     public boolean isSetTransactionTimeOut() {
          return (transaction_time_out!= null);
     }

     @Override
     public int getTransactionTimeOut() {
          if (delegate == null) {
               return transaction_time_out == null ? 0 : transaction_time_out;
          } else {
               return transaction_time_out == null ? delegate.getTransactionTimeOut() : transaction_time_out;
          }
     }
     public Map<String,Object> getConfigAdminProperties() {
          return this.configAdminProperties;
     }

     public void setDelegate(com.ibm.ws.javaee.dd.commonext.GlobalTransaction delegate) {
          this.delegate = delegate;
     }
}
