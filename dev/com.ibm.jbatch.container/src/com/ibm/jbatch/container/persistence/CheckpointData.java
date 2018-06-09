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
package com.ibm.jbatch.container.persistence;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.ibm.ws.serialization.DeserializationObjectInputStream;
/**
 * Wrapper to avoid having to put 'null', a perfectly valid return value from
 * checkpointInfo(), into the database.
 */
public class CheckpointData implements Serializable {

	private static final long serialVersionUID = 1074237962635676765L;

	private Serializable readerRestartToken;
	private Serializable writerRestartToken;

	public CheckpointData() {
		super();
	}

	public CheckpointData(byte[] bytesForDB) {

		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(bytesForDB);
			DeserializationObjectInputStream ois = null;
			try {
                                ois = new DeserializationObjectInputStream(bais, Thread.currentThread().getContextClassLoader());
				readerRestartToken = (Serializable) ois.readObject();
				writerRestartToken = (Serializable) ois.readObject();
			} finally {
				ois.close();
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Serializable getReaderCheckpoint() {
		return readerRestartToken;
	}

	public void setReaderCheckpoint(Serializable obj) {
		readerRestartToken = obj;
	}

	public Serializable getWriterCheckpoint() {
		return writerRestartToken;
	}

	public void setWriterCheckpoint(Serializable obj) {
		writerRestartToken = obj;
	}

	@Override
    public String toString() {
		StringBuilder restartString = new StringBuilder();

		String readStr = readerRestartToken == null ? "<null>" : readerRestartToken.toString();

		String writeStr = writerRestartToken == null ? "<null>" : writerRestartToken.toString();

		restartString.append("READER: ").append(readStr).append("\n").append("WRITER: ").append(writeStr);

		return restartString.toString();
	}

	public byte[] getBytesForDB() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(readerRestartToken);
			oos.writeObject(writerRestartToken);
			return baos.toByteArray();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

}

