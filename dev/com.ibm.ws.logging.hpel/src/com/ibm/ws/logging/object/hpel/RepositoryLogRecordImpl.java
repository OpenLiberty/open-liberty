/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.object.hpel;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.websphere.logging.hpel.reader.RepositoryPointer;

/**
 * RepositoryLogRecord implementation with a simple set of setters and getters for all
 * fields.
 */
public class RepositoryLogRecordImpl implements RepositoryLogRecord {

	private static final long serialVersionUID = -4091985656048616166L;

	private RepositoryPointer repositoryPointer;
	
	private long internalSeqNumber;
	
	private String messageID;
	private String rawMessage;
	private long millis;
	private int level;      
	private int threadID;
	private long sequence;
	
	private String localizedMessage;	
	private String messageLocale;
	private String loggerName;
	private String rBundle;         
	private String sourceClassName;
	private String sourceMethodName;
	private Object[] params;
	
	//WSLogRecord
	private int localizable;
	private byte[] rawData;
	private String flatStackTrace; 
	
	private final Map<String, String> extensions = new HashMap<String, String>();   
	
	public RepositoryPointer getRepositoryPointer() {
		return repositoryPointer;
	}
	
	/**
	 * Sets repository location of that record.
	 * 
	 * @param repositoryPointer
	 */
	public void setRepositoryPointer(RepositoryPointer repositoryPointer) {
		this.repositoryPointer = repositoryPointer;
	}
	
	/**
	 * @return the internalSeqNumber
	 */
	public long getInternalSeqNumber() {
		return internalSeqNumber;
	}

	/**
	 * @param internalSeqNumber the internalSeqNumber to set
	 */
	public void setInternalSeqNumber(long internalSeqNumber) {
		this.internalSeqNumber = internalSeqNumber;
	}


	public String getLoggerName() {
		return loggerName;
	}

	/**
	 * @param loggerName the loggerName to set
	 */
	public void setLoggerName(String loggerName) {
		this.loggerName = loggerName;
	}
	
	public String getMessageID() {
		return messageID;
	}

	/**
	 * @param messageID the messageID to set
	 */
	public void setMessageID(String messageID) {
		this.messageID = messageID;
	}

	public String getRawMessage() {
		return rawMessage;
	}
	
	
	/**
	 * @param message the rawMessage to set
	 */
	public void setMessage(String message) {
		this.rawMessage = message;
	}

	public long getMillis() {
		return millis;
	}

	/**
	 * @param millis the millis to set
	 */
	public void setMillis(long millis) {
		this.millis = millis;
	}

	public Map<String, String> getExtensions() {
		return extensions;
	}

	/**
	 * @param extensions the extensions to set
	 */
	public void addExtensions(Map<String, String> extensions) {
		this.extensions.putAll(extensions);
	}
	
	public String getExtension(String name) {
		return extensions.get(name);
	}

	/**
	 * Sets one extension value for the log record.
	 * 
	 * @param name extension's key.
	 * @param value extension's value.
	 */
	public void setExtension(String name, String value) {
		this.extensions.put(name, value);
	}

	public String getResourceBundleName() {
		return rBundle;
	}

	/**
	 * @param bundle the rBundle to set
	 */
	public void setResourceBundleName(String bundle) {
		rBundle = bundle;
	}

	public long getSequence() {
		return sequence;
	}

	/**
	 * @param sequence the sequence to set
	 */
	public void setSequence(long sequence) {
		this.sequence = sequence;
	}

	public String getSourceClassName() {
		return sourceClassName;
	}

	/**
	 * @param sourceClassName the sourceClassName to set
	 */
	public void setSourceClassName(String sourceClassName) {
		this.sourceClassName = sourceClassName;
	}

	public String getSourceMethodName() {
		return sourceMethodName;
	}

	/**
	 * @param sourceMethodName the sourceMethodName to set
	 */
	public void setSourceMethodName(String sourceMethodName) {
		this.sourceMethodName = sourceMethodName;
	}

	public int getThreadID() {
		return threadID;
	}

	/**
	 * @param threadID the threadID to set
	 */
	public void setThreadID(int threadID) {
		this.threadID = threadID;
	}

	public String getStackTrace() {
		return flatStackTrace;
	}

	/**
	 * @param flatStackTrace the flatStackTrace to set
	 */
	public void setStackTrace(String flatStackTrace) {
		this.flatStackTrace = flatStackTrace;
	}

	public int getLocalizable() {
		return localizable;
	}

	/**
	 * @param localizable the localizable to set
	 */
	public void setLocalizable(int localizable) {
		this.localizable = localizable;
	}

	public String getFormattedMessage() {
		String result = localizedMessage==null ? rawMessage : localizedMessage;
		if (params != null) {
			try {
				if (result != null)
					return MessageFormat.format(result, params);
			} catch (IllegalArgumentException iae) {
				// In case of problem with positional parameters fall through and
				// return unformatted string.
			}
		}
		return result;
	}
	
	public String getLocalizedMessage() {
		return localizedMessage;
	}

	/**
	 * @param localizedMessage the localizedMessage to set
	 */
	public void setLocalizedMessage(String localizedMessage) {
		this.localizedMessage = localizedMessage;
	}

	public String getMessageLocale() {
		return localizedMessage!=null && messageLocale==null ? Locale.getDefault().toString() : messageLocale;
	}

	/**
	 * @param messageLocale the messageLocale to set
	 */
	public void setMessageLocale(String messageLocale) {
		this.messageLocale = messageLocale;
	}

	public byte[] getRawData() {
		// Return copy since it goes into API user land.
		if (rawData == null) {
			return null;
		}
		byte[] result = new byte[rawData.length];
		System.arraycopy(rawData, 0, result, 0, result.length);
		return result;
	}

	/**
	 * @param rawData the rawData to set
	 */
	public void setRawData(byte[] rawData) {
		this.rawData = rawData;
	}

	public Level getLevel() {
		return Level.parse(Integer.toString(level));
	}

	/**
	 * @param level the level to set.
	 */
	public void setLevel(int level) {
		this.level = level;
	}

	public Object[] getParameters() {
		// Return copy since it goes into API user land.
		if (params == null) {
			return null;
		}
		Object[] result = new Object[params.length];
		System.arraycopy(params, 0, result, 0, result.length);
		return result;
	}

	/**
	 * @param params the array of Objects to set as parameters.
	 */
	public void setParameters(Object[] params) {
		this.params = params;
	}

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(", extensions=");
        builder.append(extensions);
        builder.append(", flatStackTrace=");
        builder.append(flatStackTrace);
        builder.append(", internalSeqNumber=");
        builder.append(internalSeqNumber);
        builder.append(", level=");
        builder.append(level);
        builder.append(", localizable=");
        builder.append(localizable);
        builder.append(", loggerName=");
        builder.append(loggerName);
        builder.append(", messageID=");
        builder.append(messageID);
        builder.append(", messageLocale=");
        builder.append(messageLocale);
        builder.append(", localizedMessage=");
        builder.append(localizedMessage);
        builder.append(", millis=");
        builder.append(millis);
        builder.append(", params=");
        builder.append(Arrays.toString(params));
        builder.append(", rBundle=");
        builder.append(rBundle);
        builder.append(", rawData=");
        builder.append(Arrays.toString(rawData));
        builder.append(", repositoryPointer=");
        builder.append(repositoryPointer);
        builder.append(", sequence=");
        builder.append(sequence);
        builder.append(", sourceClassName=");
        builder.append(sourceClassName);
        builder.append(", sourceMethodName=");
        builder.append(sourceMethodName);
        builder.append(", threadID=");
        builder.append(threadID);
        builder.append("]");
        return builder.toString();
    }

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((extensions == null) ? 0 : extensions.hashCode());
		result = prime * result
				+ ((flatStackTrace == null) ? 0 : flatStackTrace.hashCode());
		result = prime * result
				+ (int) (internalSeqNumber ^ (internalSeqNumber >>> 32));
		result = prime * result + level;
		result = prime * result + localizable;
		result = prime
				* result
				+ ((localizedMessage == null) ? 0 : localizedMessage.hashCode());
		result = prime * result
				+ ((loggerName == null) ? 0 : loggerName.hashCode());
		result = prime * result
				+ ((messageID == null) ? 0 : messageID.hashCode());
		result = prime * result
				+ ((messageLocale == null) ? 0 : messageLocale.hashCode());
		result = prime * result + (int) (millis ^ (millis >>> 32));
		result = prime * result + Arrays.hashCode(params);
		result = prime * result + ((rBundle == null) ? 0 : rBundle.hashCode());
		result = prime * result + Arrays.hashCode(rawData);
		result = prime * result
				+ ((rawMessage == null) ? 0 : rawMessage.hashCode());
		result = prime
				* result
				+ ((repositoryPointer == null) ? 0 : repositoryPointer
						.hashCode());
		result = prime * result + (int) (sequence ^ (sequence >>> 32));
		result = prime * result
				+ ((sourceClassName == null) ? 0 : sourceClassName.hashCode());
		result = prime
				* result
				+ ((sourceMethodName == null) ? 0 : sourceMethodName.hashCode());
		result = prime * result + threadID;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RepositoryLogRecordImpl other = (RepositoryLogRecordImpl) obj;
		if (extensions == null) {
			if (other.extensions != null)
				return false;
		} else if (!extensions.equals(other.extensions))
			return false;
		if (flatStackTrace == null) {
			if (other.flatStackTrace != null)
				return false;
		} else if (!flatStackTrace.equals(other.flatStackTrace))
			return false;
		if (internalSeqNumber != other.internalSeqNumber)
			return false;
		if (level != other.level)
			return false;
		if (localizable != other.localizable)
			return false;
		if (localizedMessage == null) {
			if (other.localizedMessage != null)
				return false;
		} else if (!localizedMessage.equals(other.localizedMessage))
			return false;
		if (loggerName == null) {
			if (other.loggerName != null)
				return false;
		} else if (!loggerName.equals(other.loggerName))
			return false;
		if (messageID == null) {
			if (other.messageID != null)
				return false;
		} else if (!messageID.equals(other.messageID))
			return false;
		if (messageLocale == null) {
			if (other.messageLocale != null)
				return false;
		} else if (!messageLocale.equals(other.messageLocale))
			return false;
		if (millis != other.millis)
			return false;
		if (!Arrays.equals(params, other.params))
			return false;
		if (rBundle == null) {
			if (other.rBundle != null)
				return false;
		} else if (!rBundle.equals(other.rBundle))
			return false;
		if (!Arrays.equals(rawData, other.rawData))
			return false;
		if (rawMessage == null) {
			if (other.rawMessage != null)
				return false;
		} else if (!rawMessage.equals(other.rawMessage))
			return false;
		if (repositoryPointer == null) {
			if (other.repositoryPointer != null)
				return false;
		} else if (!repositoryPointer.equals(other.repositoryPointer))
			return false;
		if (sequence != other.sequence)
			return false;
		if (sourceClassName == null) {
			if (other.sourceClassName != null)
				return false;
		} else if (!sourceClassName.equals(other.sourceClassName))
			return false;
		if (sourceMethodName == null) {
			if (other.sourceMethodName != null)
				return false;
		} else if (!sourceMethodName.equals(other.sourceMethodName))
			return false;
		if (threadID != other.threadID)
			return false;
		return true;
	}

	
}
