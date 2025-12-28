package http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
	private String method;
	private String path;
	private String httpVersion;
	private final Map<String, String> headers = new HashMap<>();
	private final Map<String, String> queryParams = new HashMap<>();
	private byte[] body;

	public HttpRequest() {
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getHttpVersion() {
		return httpVersion;
	}

	public void setHttpVersion(String httpVersion) {
		this.httpVersion = httpVersion;
	}

	public Map<String, String> getHeaders() {
		return Collections.unmodifiableMap(headers);
	}

	public void addHeader(String name, String value) {
		headers.put(name, value);
	}

	public String getHeader(String name) {
		return headers.get(name);
	}

	public Map<String, String> getQueryParams() {
		return Collections.unmodifiableMap(queryParams);
	}

	public void addQueryParam(String name, String value) {
		queryParams.put(name, value);
	}

	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

	public String getBodyAsString() {
		return body == null ? null : new String(body);
	}
}
