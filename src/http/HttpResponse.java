package http;

import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse {
	private int statusCode = 200;
	private String reasonPhrase = "OK";
	private final Map<String, String> headers = new LinkedHashMap<>();
	private byte[] body;

	public HttpResponse() {
		headers.put("Server", "SonicServer");
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatus(int statusCode, String reasonPhrase) {
		this.statusCode = statusCode;
		this.reasonPhrase = reasonPhrase;
	}

	public String getReasonPhrase() {
		return reasonPhrase;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeader(String name, String value) {
		headers.put(name, value);
	}

	public void addHeader(String name, String value) {
		headers.put(name, value);
	}

	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
		if (body != null) {
			headers.put("Content-Length", String.valueOf(body.length));
		}
	}

	public void setBody(String s) {
		if (s == null) {
			setBody((byte[]) null);
		} else {
			setBody(s.getBytes());
		}
	}

	public String getBodyAsString() {
		return body == null ? null : new String(body);
	}
}
