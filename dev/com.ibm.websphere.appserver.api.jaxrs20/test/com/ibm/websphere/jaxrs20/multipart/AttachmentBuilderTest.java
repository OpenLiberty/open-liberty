package com.ibm.websphere.jaxrs20.multipart;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import javax.ws.rs.core.MediaType;

@SuppressWarnings("deprecation")
public class AttachmentBuilderTest {

	static {
		System.setProperty("com.ibm.ws.beta.edition", "true");
	}

	@Test
	public void testBasicAttachment() throws Exception {
		IAttachment att = AttachmentBuilder.newBuilder("abc")
				                           .inputStream(new ByteArrayInputStream("Hello".getBytes()))
				                           .build();
		assertEquals("abc", getContentDispositionHeaderValueParam(att, "name"));
		assertEquals("text/plain", att.getHeader("Content-Type"));
		assertEquals("Hello", toString(att.getDataHandler().getInputStream()));
	}

	@Test
	public void testFileBasedAttachment() throws Exception {
		IAttachment att = AttachmentBuilder.newBuilder("foo")
				                           .fileName("xyz.pdf")
				                           .inputStream(new ByteArrayInputStream("Orange".getBytes()))
				                           .build();
		assertEquals("foo", getContentDispositionHeaderValueParam(att, "name"));
		assertEquals("xyz.pdf", getContentDispositionHeaderValueParam(att, "filename"));
		assertEquals("application/octet-stream", att.getHeader("Content-Type"));
		assertEquals("Orange", toString(att.getDataHandler().getInputStream()));
	}

	@Test
	public void testFileBasedAttachment2() throws Exception {
		IAttachment att = AttachmentBuilder.newBuilder("bar")
				                           .inputStream("123.xml", new ByteArrayInputStream("Blue".getBytes()))
				                           .build();
		assertEquals("bar", getContentDispositionHeaderValueParam(att, "name"));
		assertEquals("123.xml", getContentDispositionHeaderValueParam(att, "filename"));
		assertEquals("application/octet-stream", att.getHeader("Content-Type"));
		assertEquals("Blue", toString(att.getDataHandler().getInputStream()));
	}

	@Test
	public void testHeaderInAttachment() throws Exception {
		IAttachment att = AttachmentBuilder.newBuilder("baz")
				                           .inputStream(new ByteArrayInputStream("Red".getBytes()))
				                           .contentType(MediaType.APPLICATION_XML)
				                           .header("MyHeader", "Yellow")
				                           .contentId("12345")
				                           .build();
		assertEquals("baz", getContentDispositionHeaderValueParam(att, "name"));
		assertEquals("application/xml", att.getHeader("Content-Type"));
		assertEquals("Yellow", att.getHeader("MyHeader"));
		assertEquals("12345", att.getHeader("Content-ID"));
		assertEquals("Red", toString(att.getDataHandler().getInputStream()));
	}

	@Test
	public void testStringContentType() throws Exception {
		IAttachment att = AttachmentBuilder.newBuilder("another")
				                           .inputStream(new ByteArrayInputStream("Green".getBytes()))
				                           .contentType("application/json+svg")
				                           .build();
		assertEquals("another", getContentDispositionHeaderValueParam(att, "name"));
		assertEquals("application/json+svg", att.getHeader("Content-Type"));
		assertEquals("Green", toString(att.getDataHandler().getInputStream()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullName() {
		AttachmentBuilder.newBuilder(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullInputStream() {
		AttachmentBuilder.newBuilder("abc").inputStream(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullFileName() {
		AttachmentBuilder.newBuilder("abc").fileName(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullInputStream2() {
		AttachmentBuilder.newBuilder("abc").inputStream("someFile.txt", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullFileName2() {
		AttachmentBuilder.newBuilder("abc").inputStream(null, new ByteArrayInputStream("hello".getBytes()));
	}
	
	@Test(expected = IllegalStateException.class)
	public void noInputStream() {
		AttachmentBuilder.newBuilder("abc").build();
	}

	private String getContentDispositionHeaderValueParam(IAttachment att, String param) {
		String headerValue = att.getHeaders().getFirst("Content-Disposition");
		String searchString = " " + param + "=\"";
		int x = headerValue.indexOf(searchString);
		if (x < 1) return null;
		x += searchString.length();
		int y = headerValue.indexOf("\"", x);
		if (y < 1 || y < x) return null;
		return headerValue.substring(x, y);
	}

	private String toString(InputStream is) throws IOException {
		byte[] buf = new byte[1024];
		int bytesRead = is.read(buf);
		StringBuffer sb = new StringBuffer();
		while (bytesRead > -1) {
			sb.append(new String(buf, 0, bytesRead));
			bytesRead = is.read(buf);
		}
		return sb.toString();
	}
}
