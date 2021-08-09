package com.ibm.ws.jaxrs.fat.callback;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

public abstract class JaxrsUtil {

	public static final//
	String readFromStream(InputStream stream) throws IOException {
		InputStreamReader isr = new InputStreamReader(stream);
		return readFromReader(isr);
	}

	public static final//
	String readFromReader(Reader reader) throws IOException {
		BufferedReader br = new BufferedReader(reader);
		String entity = br.readLine();
		br.close();
		return entity;
	}
	
	public static final//
	String readFromFile(File file) throws IOException {
		FileReader reader = new FileReader(file);		
		return readFromReader(reader);
	}
	

	public static final <T> //
	String iterableToString(String separator, Iterable<T> collection) {
		if (collection != null)
			return iterableToString(separator, collection.iterator());
		return "";
	}

	public static final <T> //
	String iterableToString(String separator, Iterator<T> iterator) {
		StringBuilder sb = new StringBuilder();
		while (iterator.hasNext()) {
			T item = iterator.next();
			if (item != null) {
				String appendable = item.toString();
				sb.append(appendable);
				if (iterator.hasNext())
					sb.append(separator);
			}
		}
		return sb.toString();
	}

	public static final <T> //
	String enumerationToString(String separator, Enumeration<T> enumeration) {
		StringBuilder sb = new StringBuilder();
		if (enumeration != null)
			while (enumeration.hasMoreElements()) {
				T item = enumeration.nextElement();
				if (item != null) {
					String appendable = item.toString();
					sb.append(appendable);
					if (enumeration.hasMoreElements())
						sb.append(separator);
				}
			}
		return sb.toString();
	}

	public static final <T> //
	String iterableToString(String separator, T... collection) {
		StringBuilder sb = new StringBuilder();
		if (collection != null)
			for (int i = 0; i != collection.length; i++) {
				T item = collection[i];
				if (item != null) {
					String appendable = item.toString();
					sb.append(appendable);
					if (i != collection.length - 1)
						sb.append(separator);
				}
			}
		return sb.toString();
	}
	
	public static final TimeZone findTimeZoneInDate(String date){
		StringBuilder sb = new StringBuilder();
		StringBuilder dateBuilder = new StringBuilder(date.trim()).reverse();
		int index = 0;
		char c;
		while ((c = dateBuilder.charAt(index++)) != ' '){
			sb.append(c);
		}
		TimeZone timezone = TimeZone.getTimeZone(sb.reverse().toString());
		return timezone;
	}
	
	public static final DateFormat createDateFormat(TimeZone timezone){
		SimpleDateFormat sdf = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		sdf.setTimeZone(timezone);
		return sdf;
	}

}
