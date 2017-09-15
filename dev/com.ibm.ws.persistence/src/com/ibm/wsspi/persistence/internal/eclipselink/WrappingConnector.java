/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.persistence.internal.eclipselink;

import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Properties;

import org.eclipse.persistence.sessions.Connector;
import org.eclipse.persistence.sessions.Session;

import com.ibm.wsspi.persistence.internal.eclipselink.sql.WrappingConnection;

/**
 * A Connector implementation that delegates all operations to another Connector. The only piece of
 * support that this adds is that it wraps all java.sql.Connection objects with a
 * WrappingConnection.
 */
public class WrappingConnector implements Connector {
     private static final long serialVersionUID = 1L;
     private final Connector _del;

     public WrappingConnector(Connector delegate) {
          _del = delegate;
     }

     public Connector getDelegate() {
          return _del;
     }

     public Connection connect(Properties properties, Session session) {
          return new WrappingConnection(_del.connect(properties, session));
     }

     public void toString(PrintWriter writer) {
          _del.toString(writer);

     }

     public String getConnectionDetails() {
          return _del.getConnectionDetails();
     }

     public Object clone() {
          try {
               return super.clone();
          } catch (CloneNotSupportedException e) {
               throw new RuntimeException(e);
          }
     }
}
