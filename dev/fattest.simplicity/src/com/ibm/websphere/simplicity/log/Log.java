/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.simplicity.log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Log {

	private static LogManager manager = LogManager.getLogManager();
	private static HashMap<Class, Logger> logCache = new HashMap<Class, Logger>();
	private static Handler handler = null;
	private static Formatter formatter = null;
	private static HashMap<String, String> levelTranslations = new HashMap<String, String>();
	
	private Log() { }
    
    static {
        initLevelTranslations();
    }
	
	public static LogManager getManager() {
		return manager;
	}
	
	public static void setSpec(String s) {
		setSpecInternal(s);
	}
	
	public static Handler getHandler() {
		return handler;
	}
	
    /**
     * Use this method to set the logging Handler
     * 
     * @param newHandler The Handler to log with
     */
	public static synchronized void setHandler(Handler newHandler) {
		for (Entry<Class, Logger> entry : logCache.entrySet()) {
			for (Handler handler : entry.getValue().getHandlers())
				entry.getValue().removeHandler(handler);
			entry.getValue().addHandler(newHandler);
		}
		handler = newHandler;
	}
    
	public static Formatter getFormatter() {
		return formatter;
	}
	
	public static void setFormatter(Formatter newFormatter) {
		handler.setFormatter(newFormatter);
		formatter = newFormatter;
	}
	
	public static void debug(Class c, String data) {
		getLogger(c).log(Level.FINEST, data);
	}
	
	public static void entering(Class c, String method) {
		getLogger(c).entering(c.getCanonicalName(), method);
	}
	
	public static void entering(Class c, String method, Object value) {
		getLogger(c).entering(c.getCanonicalName(), method, value);
	}
	
	public static void entering(Class c, String method, Object[] values) {
		getLogger(c).entering(c.getCanonicalName(), method, values);
	}
	
	public static void error(Class c, String method, Throwable e) {
		getLogger(c).logp(Level.SEVERE, c.getCanonicalName(), method, null, e);
	}
	
    public static void error(Class c, String method, Throwable e, String mesg) {
        getLogger(c).logp(Level.SEVERE, c.getCanonicalName(), method, mesg, e);
    }
    
    public static void info(Class c, String method, String data) {
        getLogger(c).logp(Level.INFO, c.getCanonicalName(), method, data);
    }
    
    public static void info(Class c, String method, String data, Object value) {
        getLogger(c).logp(Level.INFO, c.getCanonicalName(), method, data, value);
    }
    
    public static void info(Class c, String method, String data, Object[] values) {
        getLogger(c).logp(Level.INFO, c.getCanonicalName(), method, data, values);
    }
	
    public static void finer(Class c, String method, String data) {
        getLogger(c).logp(Level.FINER, c.getCanonicalName(), method, data);
    }
    
    public static void finer(Class c, String method, String data, Object value) {
        getLogger(c).logp(Level.FINER, c.getCanonicalName(), method, data, value);
    }
	
    public static void finer(Class c, String method, String data, Object[] values) {
        getLogger(c).logp(Level.FINER, c.getCanonicalName(), method, data, values);
    }
	
    public static void finest(Class c, String method, String data) {
        getLogger(c).logp(Level.FINEST, c.getCanonicalName(), method, data);
    }
    
    public static void finest(Class c, String method, String data, Object value) {
        getLogger(c).logp(Level.FINEST, c.getCanonicalName(), method, data, value);
    }
    
    public static void finest(Class c, String method, String data, Object[] values) {
        getLogger(c).logp(Level.FINEST, c.getCanonicalName(), method, data, values);
    }
	
	public static void exiting(Class c, String method) {
		getLogger(c).exiting(c.getCanonicalName(), method);
	}
	
	public static void exiting(Class c, String method, Object value) {
		getLogger(c).exiting(c.getCanonicalName(), method, value);
	}
	
	public static void exiting(Class c, String method, Object[] values) {
		getLogger(c).exiting(c.getCanonicalName(), method, values);
	}
	
	public static void warning(Class c, String message) {
		getLogger(c).warning(message);
	}
	
	private static Logger getLogger(Class c) {
		Logger logger = logCache.get(c);
		if (logger == null) {
			logger = Logger.getLogger(c.getCanonicalName());
			if (logger != null) {
				logCache.put(c, logger);
                if(handler != null)
                    logger.addHandler(handler);
			}
		}
		return logger;
	}
	
	private static void setSpecInternal(String fullSpecification) {
		String[] specs = fullSpecification.split(":");
		String property = "";
		for (String spec : specs) {
			// Split at "=", results in parts[0]=spec, parts[1]=level
			String[] parts = spec.split("=");
			// Format the string properly
			parts[0] = parts[0].replace("*", "");
			if (!parts[0].endsWith(".level")) {
				if (!parts[0].endsWith("."))
					parts[0] += ".";
				parts[0] += "level";
			}
			
			// Translate WAS to Java levels
			if (levelTranslations.containsKey(parts[1].toLowerCase()))
				parts[1] = levelTranslations.get(parts[1].toLowerCase());
			parts[1] = parts[1].toUpperCase();
			
			// Add it to property-style string
			property += parts[0]+"="+parts[1]+"\n";
		}

		ByteArrayInputStream is = new ByteArrayInputStream(property.getBytes());
		try {
			manager.readConfiguration(is);
		}
		catch(IOException ioe) {
			// ignore
		}
	}
	
	private static void initLevelTranslations() {
		/*
		 * In order of decreasing detail, these are the WebSphere levels:
		 * 	all, dump, finest, debug, finer, entryExit, fine, event, detail, config, info, audit,
		 *  warning, severe, error, fatal, off
		 * 
		 * In order of decreasing detail, these are the Java levels:
		 *  all, finest, finer, fine, config, info, warning, severe, off
		 *  
		 */
		levelTranslations.put("all", "ALL");
		levelTranslations.put("dump", "FINEST");
		levelTranslations.put("audit", "FINEST");
		levelTranslations.put("debug", "FINEST");
		levelTranslations.put("entryexit", "FINER");
		levelTranslations.put("event", "FINE");
		levelTranslations.put("detail", "FINE");
		levelTranslations.put("audit", "INFO");
		levelTranslations.put("error", "SEVERE");
		levelTranslations.put("fatal", "SEVERE");
	}

	public static class DefaultHandler extends Handler {
		
		private PrintStream out;
		
		public DefaultHandler(PrintStream out) {
			this.out = out;
		}
	
		@Override
		public void close() {
			if (this.out != null)
				this.out.close();
		}
	
		@Override
		public void flush() {
			if (this.out != null)
				this.out.flush();
		}
	
		@Override
		public void publish(LogRecord record) {
			if (!this.isLoggable(record))
				return;

			Formatter f = null;
			synchronized(this) {
				f = this.getFormatter();
				if (f == null) {
					f = new DefaultFormatter();
					this.setFormatter(f);
				}
			}
			String output = f.format(record);
			if (this.out != null)
				this.out.println(output);
			else
				System.out.println(output);
		}
		
	}
	
	public static class DefaultFormatter extends Formatter {
		
		private static final int classNameLength = 13;
		private static final DateFormat dateFormatter = getBasicDateFormatter();
		private static final FieldPosition fieldPosition = new FieldPosition(0);
		private static final String lineSeparator = System.getProperty("line.separator");
		private static final String indent = "                                 ";
		private static final String lineSeparatorPlusIndent = lineSeparator+indent;
		
		@Override
		public String format(LogRecord logRecord) {
			StringBuffer buffer = new StringBuffer();
			writeHeader(logRecord, buffer);
			writeFormattedMessage(logRecord, buffer);
			writeStackTrace(logRecord, buffer);
			return new String(buffer);
		}
		
		private void writeHeader(LogRecord logRecord, StringBuffer buffer) {
			// Clear buffer
			buffer.setLength(0);
			// Add date & time
			writeDateAndTime(logRecord, buffer);
			// Append class name
			writeClassName(logRecord, buffer);
			writeEntryChar(logRecord, buffer);
		}
		
		private void writeDateAndTime(LogRecord logRecord, StringBuffer buffer) {
			buffer.append("[");
			Date date = new Date(logRecord.getMillis());
			dateFormatter.format(date, buffer, fieldPosition);
			buffer.append("] ");
			buffer.append(getThreadId(logRecord));
			buffer.append(' ');
		}
		
		private void writeClassName(LogRecord logRecord, StringBuffer buffer) {
			String name = logRecord.getLoggerName();
			int dot = name.lastIndexOf('.')+1;
			if (dot > 0)
				name = name.substring(dot);
			
			if (name.length() > classNameLength)
				name = name.substring(0, classNameLength);
			else {
				while (name.length() < classNameLength)
					name += " ";
			}
			buffer.append(name);
			buffer.append(" ");
		}
		
		private void writeEntryChar(LogRecord logRecord, StringBuffer buffer) {
			char c = 'Z';
			Level level = logRecord.getLevel();
			if (level == Level.SEVERE)
				c = 'E';
			else if (level == Level.INFO)
				c = 'I';
			else if (level == Level.WARNING)
				c = 'W';
			else if (level == Level.FINE)
				c = '1';
			else if (level == Level.FINER) {
				String msg = logRecord.getMessage();
				if (msg.contains("Entry") || msg.contains("ENTRY"))
					c = '>';
				else if (msg.contains("Exit") || msg.contains("RETURN"))
					c = '<';
				else
					c = '2';
			} else if (level == Level.FINEST)
				c = '3';
			
			buffer.append(" ");
			buffer.append(c);
			buffer.append(" ");
		}
		
		private void writeFormattedMessage(LogRecord record, StringBuffer buffer) {
			String msg = record.getMessage();
			Object[] params = record.getParameters();
	
			try {
				msg = msg.replace("ENTRY", record.getSourceMethodName()+" Entry");
				msg = msg.replace("RETURN", record.getSourceMethodName()+" Exit");
			}
			catch(Exception e) { }
	
			if (params == null || params.length == 0) {
				buffer.append(msg);
				return;
			} else
				// Do NOT modify the original array
				params = params.clone();
	
			// Ensure the placeholders are there for the param list
			if (!msg.contains("{0}")) {
				for (int i=0; i < params.length; i++)
					msg += " {"+i+"}";
			}
			
			// Put each param on a separate line
			msg = msg.replace("{", lineSeparatorPlusIndent+"{");
			
			// Replace empty strings with double-quotes
			for (int i=0; i < params.length; i++) {
				Object o = params[i];
				if (o instanceof String && ((String)o).length() == 0)
					params[i] = "\"\"";
			}
			
			// Format it
			try {
				msg = MessageFormat.format(msg, params);
			}
			catch(IllegalArgumentException e) {
				// Ignore
			}
			buffer.append(msg);
		}
		
		private void writeStackTrace(LogRecord record, StringBuffer buffer) {
			Throwable t = record.getThrown();
			if (t == null)
				return;
	
			buffer.append(lineSeparatorPlusIndent);
			buffer.append(getStackTrace(t));
		}
		
		private String getThreadId(LogRecord logRecord) {
			String tid = Integer.toHexString(logRecord.getThreadID());
			while (tid.length() < 8)
				tid = "0"+tid;
			return tid;
		}
		
		private String getStackTrace(Throwable t) {
			StringWriter s = new StringWriter();
			PrintWriter p = new PrintWriter(s);
			t.printStackTrace(p);
			return s.toString();
		}
		
		private static DateFormat getBasicDateFormatter() {
			// If this proves a mismatch, review FormatSet.getBasicDateFormatter
			// "MM/dd/yyyy kk:mm:ss:SSS zzz"
			return new SimpleDateFormat("yy.MM.dd HH:mm:ss:SSS z");
		}
		
	}
	
}
