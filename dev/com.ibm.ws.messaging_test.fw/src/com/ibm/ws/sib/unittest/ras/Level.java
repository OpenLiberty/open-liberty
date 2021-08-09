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
package com.ibm.ws.sib.unittest.ras;

/**
 * <p>This class defines the levels to be used with the Logger. This class is
 *   not part of the logger API.
 * </p>
 *
 * <p>SIB build component: mm.logger</p>
 *
 * @author nottinga
 * @version 1.3
 * @since 1.0
 */
public class Level extends java.util.logging.Level
{
  /** <code>serialVersionUID</code> */
  private static final long serialVersionUID = -3787927574903744496L;
  /** A level indicating a method being called */
  public static final Level ENTRY = new Level("ENTRY", 0);
  /** A level indicating a method being left */
  public static final Level EXIT = new Level("EXIT", 1);
  /** A level indicating an exception being thrown */
  public static final Level THROWING = new Level("THROWING", 2);
  /** A level indicating an exception has been caught. */
  public static final Level CAUGHT = new Level("CAUGHT", 3);
  /** A level indicating some information being traced */
  public static final Level INFORMATION = new Level("INFO", 4);
  /** A level indicating an error being generated */
  public static final Level ERROR = new Level("ERROR", 5);
  /** A level indicating an event being generated */
  public static final Level EVENT = new Level("EVENT", 6);
  /** A level indicating an audit event being generated */
  public static final Level AUDIT = new Level("AUDIT", 7);
  /** A level indicating a debug event being generated */
  public static final Level DEBUG = new Level("DEBUG", 8);
  /** A level indicating a fatal error being generated */
  public static final Level FATAL = new Level("FATAL", 9);
  /** A level indicating a service event being generated */
  public static final Level SERVICE = new Level("SERVICE", 10);
  /** A level indicating a warning being generated */
  public static final Level WARNING2 = new Level("WARNING", 11);
  /** A level indicating a dump being traced */
  public static final Level DUMP = new Level("DUMP", 12);
  
  /**
   * The constructor
   * @param name  the level name.
   * @param value the level value.
   * @param resourceBundleName an associated resource bundle.
   */
  protected Level(String name, int value, String resourceBundleName)
  {
    super(name, value, resourceBundleName);
  }

  /**
   * The constructor
   * @param name  The level name.
   * @param value The level value.
   */
  protected Level(String name, int value)
  {
    super(name, value);
  }
}
