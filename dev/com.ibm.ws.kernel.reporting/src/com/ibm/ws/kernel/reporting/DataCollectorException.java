package com.ibm.ws.kernel.reporting;

import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

@SuppressWarnings("serial")
public class DataCollectorException extends Exception {

	/**
	 * 
	 * @param message
	 * @param cause
	 */
	public DataCollectorException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * 
	 * @param oos
	 * @throws NotSerializableException
	 */
	private void writeObject(ObjectOutputStream oos) throws NotSerializableException {
		throw new NotSerializableException();
	}

}
