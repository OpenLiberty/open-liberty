/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.object.hpel;

import java.util.Arrays;

import com.ibm.websphere.logging.hpel.reader.RepositoryPointer;

/**
 * Implementation of the {@link RepositoryPointer} using file's String ID
 * and record's index in the file.
 */
public class RepositoryPointerImpl implements RepositoryPointer {
	private static final long serialVersionUID = 2840589997089219493L;
	
	//indexes of instanceIds array is expected to hold certain values.  These values are:
	//0 = directory name
	//1 = key used in the map of the ServerInstanceLogRecordList.getChildren()
	private final String[] instanceIds;
	private final String fileId;
	private final long recordOffset;
	
	public RepositoryPointerImpl(String[] instanceIds, String fileId, long recordPosition) {
		this.instanceIds = Arrays.copyOf(instanceIds, instanceIds.length);
		this.fileId = fileId;
		this.recordOffset = recordPosition;
	}

	/**
	 * @return the instanceIds
	 */
	public String[] getInstanceIds() {
		return instanceIds;
	}
	
	/**
	 * @return the fileId
	 */
	public String getFileId() {
		return fileId;
	}

	/**
	 * @return the recordOffset
	 */
	public long getRecordOffset() {
		return recordOffset;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileId == null) ? 0 : fileId.hashCode());
		result = prime * result + Arrays.hashCode(instanceIds);
		result = prime * result + (int) (recordOffset ^ (recordOffset >>> 32));
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
		RepositoryPointerImpl other = (RepositoryPointerImpl) obj;
		if (fileId == null) {
			if (other.fileId != null)
				return false;
		} else if (!fileId.equals(other.fileId))
			return false;
		if (!Arrays.equals(instanceIds, other.instanceIds))
			return false;
		if (recordOffset != other.recordOffset)
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\" file: \"");
		for (String st: instanceIds) {
			sb.append(st);
			sb.append("/");
		}
		sb.append(fileId);
		sb.append("\" offset: ");
		sb.append(Long.toString(recordOffset));
		return sb.toString();
	}

}
