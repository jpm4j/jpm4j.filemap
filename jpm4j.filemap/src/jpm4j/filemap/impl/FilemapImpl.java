package jpm4j.filemap.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Reference;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;

/**
 * This is an example that uses jpm4j and bndtools together, that is, a Hello
 * World like application. In this case, the application is a command line
 * application that shows a treemap of the file system in the browser. You can
 * use it like:
 * 
 * <pre>
 * filemap / User
 * </pre>
 * 
 * This class is a servlet that registers by default under /filemap. It then
 * provides an html file {@code treemap.html} that provides the file map using
 * javascript and html.It might be horrible slow on larger file systems.
 */

@Component(provide = { Servlet.class, Runnable.class }, properties = {
		"alias=/filemap", "main.thread=true" }, designateFactory = FilemapImpl.Config.class, configurationPolicy=ConfigurationPolicy.optional)
public class FilemapImpl extends HttpServlet implements Runnable {
	private static final long serialVersionUID = 1L;

	// Command line arguments
	private String[] args;

	// Root node for the file map
	private Node root = new Node("root");

	// Simple node class that is serialized to the browser
	public class Node {

		public Node(String name) {
			this.name = name;
		}

		public String name;
		public List<Node> children;
		public long size;
	}

	// Provides metatype configuration to change alias
	interface Config {
		String alias();
	}

	/**
	 * Dispatches to the routine providing JSON or to the resources and applies
	 * compression if possible.
	 */

	public void doGet(HttpServletRequest rq, HttpServletResponse rsp)
			throws IOException {

		String path = rq.getPathInfo();
		if (path != null && path.startsWith("/"))
			path = path.substring(1);

		OutputStream out = rsp.getOutputStream();

		//
		// Check if the caller accepts deflate compression
		//

		String accept = rq.getHeader("Accept-Encoding");
		if (accept.indexOf("deflate") >= 0) {
			out = new DeflaterOutputStream(out);
			rsp.setHeader("Content-Encoding", "deflate");
		}

		//
		// Dispatch between data and resources
		//
		if ("treemap.json".equals(path)) {
			getData(out);
			rsp.setContentType("application/json;charset=utf-8");
		} else {
			InputStream in = getResource(rq.getPathInfo());

			if (in == null)
				rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			else
				IO.copy(in, out);
		}

		//
		// Ensure we close for the optional deflate to be flushed
		//

		out.close();
	}

	/*
	 * Find a resource in our JAR.
	 * 
	 * @param path The path to the resource (based on /static/)
	 * 
	 * @return an Input Stream to the resource or null
	 * 
	 * @throws IOException
	 */
	private InputStream getResource(String path) throws IOException {
		if (path == null || path.isEmpty() || "/".equals(path))
			path = "index.html";
		else
			while (path.startsWith("/"))
				path = path.substring(1);

		URL u = getClass().getResource("/static/" + path);
		if (u == null)
			return null;

		return u.openStream();
	}

	/*
	 * Calculate the JSON stream.
	 */
	private void getData(OutputStream out) {
		try {
			synchronized (root) {
				new JSONCodec().enc().to(out).put(root);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * The bnd launcher provides access to the command line arguments via the
	 * Launcher object. This object is also registered under Object.
	 */

	@Reference
	void setDone(Object done, Map<String, Object> parameters) {
		args = (String[]) parameters.get("launcher.arguments");
	}

	/**
	 * Since we're registered as a Runnable with the main.thread property we get
	 * called when the system is fully initialized.
	 */
	public void run() {
		try {
			//
			// Setup default args if not set
			//

			if (args == null || args.length == 0)
				args = new String[] { "." };

			//
			// Get the URL to browse to
			int port = 8080;
			String p = System.getProperty("org.osgi.http.port");
			if (p != null && p.matches("[0-9]+"))
				port = Integer.parseInt(p);

			open("http://127.0.0.1:" + port + "/filemap/treemap.html");

			// Might add an option to override timeout

			int timeout = 10000000;
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			for (String s : args) {
				File f = IO.getFile(s);
				traverse(root, f);
			}

			System.out.println("done");
			Thread.sleep(timeout);
		} catch (Exception e) {
			// should not happen, but in case ...
			e.printStackTrace();
		}
	}

	/*
	 * Simple routine to traverse the file system from a root. Will create a
	 * Node for each directory.
	 */

	private void traverse(Node parent, File f) {
		if (!f.exists())
			return;

		if (f.isFile()) {
			parent.size += f.length();
			return;
		}

		Node node = new Node(f.getName());
		synchronized (root) {
			if (parent.children == null)
				parent.children = new ArrayList<Node>();
			parent.children.add(node);
		}

		File[] list = f.listFiles();
		if (list != null)
			for (File sub : f.listFiles())
				traverse(node, sub);
	}

	/**
	 * A utility to open a URL on different OS's browsers
	 * 
	 * @param url
	 *            the url to open
	 * @throws IOException
	 */
	void open(String url) throws IOException {
		String os = System.getProperty("os.name").toLowerCase();
		Runtime rt = Runtime.getRuntime();

		if (os.indexOf("mac") >= 0 || os.indexOf("darwin") >= 0) {
			rt.exec("open " + url);
		} else if (os.indexOf("win") >= 0) {
			// this doesn't support showing urls in the form of
			// "page.html#nameLink"
			rt.exec("rundll32 url.dll,FileProtocolHandler " + url);

		} else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {

			// Do a best guess on unix until we get a platform independent way
			// Build a list of browsers to try, in this order.
			String[] browsers = { "epiphany", "firefox", "mozilla",
					"konqueror", "netscape", "opera", "links", "lynx" };

			// Build a command string which looks like
			// "browser1 "url" || browser2 "url" ||..."
			StringBuffer cmd = new StringBuffer();
			for (int i = 0; i < browsers.length; i++)
				cmd.append((i == 0 ? "" : " || ") + browsers[i] + " \"" + url
						+ "\" ");

			rt.exec(new String[] { "sh", "-c", cmd.toString() });

		} else
			System.out.println("Open " + url);
	}
}
