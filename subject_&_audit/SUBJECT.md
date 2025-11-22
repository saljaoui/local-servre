## LocalServer

### Overview

Finally, you are going to understand how the internet works from the server side. The Hypertext Transfer Protocol was created in order to ensure a reliable way to communicate on a request/response basis.

This protocol is used by servers and clients (usually browsers) to serve content, and it is the backbone of the World Wide Web. Still, it is also used in many other cases that are far beyond the scope of this exercise.

For this project, you **must** use **Java**.

### Role Play

You are a backend engineer at a startup building a lightweight web server to handle internal APIs and static content with minimal dependencies. Your goal is to deliver a highly available, crash-proof solution that can be extended to support dynamic content via CGI scripts and configured to suit multiple environments.

### Learning Objective

By the end of this project, learners will be able to:

- Design and implement a custom HTTP/1.1-compliant server in Java
- Utilize non-blocking I/O mechanisms
- Parse and construct HTTP requests and responses manually
- Configure server routes, error pages, uploads, and CGI scripts
- Evaluate performance under stress and ensure memory and process safety

Technical skills:

- Socket programming
- Asynchronous I/O
- File and process management
- Configuration parsing

### Instructions

- The project must be written in **Java**.
- Use Java Core Libraries, namely the `java.nio` package for non-blocking I/O and `java.net` for network handling.
- Make use of an event-driven API for handling connections.

> You **cannot** use established server frameworks or asynchronous runtimes (e.g., `Netty`, `Jetty`, `Grizzly`).

#### The Server

Your goal is to write your own HTTP server to serve static web pages to browsers.

It must:

- **Never** crash.
- Timeout long requests.
- Listen on multiple ports and instantiate multiple servers.
- Use only one process and one thread.
- Receive requests and send HTTP/1.1-compliant responses.
- Handle `GET`, `POST`, and `DELETE`.
- Receive file uploads.
- Handle cookies and sessions.
- Provide default error pages for: 400, 403, 404, 405, 413, 500.
- Use an event-driven, non-blocking I/O API.
- Manage chunked and unchunked requests.
- Set the correct HTTP status in responses.

#### The CGI

- Execute one type of CGI (e.g., `.py`) using `ProcessBuilder`.
- Pass the file to process as the first argument.
- Use the `PATH_INFO` environment variable to define full paths.
- Ensure correct relative path handling.

#### Configuration File

Support configuration for:

- Host and multiple ports.
- Default server selection.
- Custom error page paths.
- Client body size limit.
- Routes with:
  - Accepted methods.
  - Redirections.
  - Directory/file roots.
  - Default file for directories.
  - CGI by file extension.
  - Directory listing toggle.
  - Default directory response file.

> No need for regex support.

#### Testing

- Use `siege -b [IP]:[PORT]` for stress testing (target 99.5% availability).
- Write comprehensive tests (redirections, configs, error pages, etc.).
- Test for memory leaks.

#### Bonus Challenges

- Implement a second CGI handler.
- Create an admin dashboard or server metrics endpoint.

### Example Repository Structure

```
/java-server
├── /src
│   ├── Main.java         # Entry point
│   ├── Server.java       # Handles server lifecycle
│   ├── Router.java       # Routes requests
│   ├── CGIHandler.java   # Manages CGI execution
│   ├── ConfigLoader.java # Parses configuration file
│   ├── error.java        # Error responses
│   ├── utils/
│       ├── Session.java  # Session management
│       ├── Cookie.java   # Cookie utilities
├── config.json           # Server configuration
├── README.md             # Documentation
├── error_pages/          # Custom error HTML files
```

### Tips

- Avoid hardcoding; use the config file.
- Validate configs at startup.
- Sanitize inputs for CGI.
- Modularize components.
- Use thread-safe data structures.
- Prevent file descriptor and memory leaks.

### Resources

- [RFC 2616 – HTTP/1.1 Specification](https://www.rfc-editor.org/rfc/rfc9112.html)
- [Java NIO Docs](https://docs.oracle.com/javase/tutorial/essential/io/)
- [CGI Protocol Overview](https://en.wikipedia.org/wiki/Common_Gateway_Interface)
- [siege Load Testing Tool](https://github.com/JoeDog/siege)

### Disclaimer

This project is for educational use only. Using siege or any stress testing tool against a third-party server without explicit permission is illegal and unethical.

\\theta = (X^T X)^{-1} X^T y