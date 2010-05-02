/*
	This file is part of the OSM Route Manager and History Viewer.

	OSM Route Manager and History Viewer is free software: you can redistribute it and/or modify
	it under the terms of the GNU Affero General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	OSM Route Manager and History Viewer is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Affero General Public License for more details.

	You should have received a copy of the GNU Affero General Public License
	along with this software. If not, see <http://www.gnu.org/licenses/>.
*/

package eu.cdauth.osm.web.common;


import eu.cdauth.osm.lib.ItemCache;
import eu.cdauth.osm.lib.api06.API06API;
import gettext.GettextResource;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * Helper functions for HTML output in JSP. A GUI object represents one HTML document.
 *
 * <p>Usually you set output properties using the setters, then call {@link #head}, then write out your HTML
 * code and then call {@link #foot}.
 *
 * <p>This class additionally contains static functions that help printing text in the HTML page (such as for
 * internationalisation and proper escaping of special characters).
 *
 * @author cdauth
 */

abstract public class GUI
{
	public static final Charset UTF8 = Charset.forName("UTF-8");

	private String m_description = null;
	private String[] m_javaScripts = null;
	private String m_title = null;
	private String m_bodyClass = null;

	private final ResourceBundle m_bundle;

	private final HttpServletRequest m_req;
	private final HttpServletResponse m_resp;

	private static API06API sm_api = null;
	private static Timer sm_apiCleanUp = null;

	/**
	 * Returns an {@link eu.cdauth.osm.lib.API} object to use.
	 * @return The API object to use.
	 */
	public synchronized static API06API getAPI()
	{
		if(sm_api == null)
		{
			DataSource ds = null;
			try {
				ds = (DataSource) ((Context)new InitialContext().lookup("java:comp/env")).lookup("jdbc/osmrmhv");
			} catch(NamingException e) {
			}
			if(ds == null)
			{
				Logger.getLogger(GUI.class.getName()).info("Using API without database cache.");
				sm_api = new eu.cdauth.osm.lib.api06.API06API();
			}
			else
			{
				Logger.getLogger(GUI.class.getName()).info("Using API with database cache.");
				sm_api = new eu.cdauth.osm.lib.api06.API06API(ds);
			}

			new Thread("osmrmhv cache cleanup") {
				@Override public void run() {
					while(true)
					{
						try {
							Thread.sleep(60000);
						} catch(InterruptedException e) {
							break;
						}
						try {
							ItemCache.cleanUpAll();
						} catch(Exception e) {
							Logger.getLogger(GUI.class.getName()).log(Level.WARNING, "Unexpected exception.", e);
						}
					}
				}
			}.start();
		}
		return sm_api;
	}

	/**
	 * Constructs a new GUI object for the given JSP context. Automatically detects the user’s desired language.
	 * @param a_req The JSP page request
	 * @param a_resp The JSP page response
	 */
	public GUI(HttpServletRequest a_req, HttpServletResponse a_resp)
	{
		m_req = a_req;
		m_resp = a_resp;

		ResourceBundle bundle = null;
		if(m_req.getParameter("lang") != null)
		{
			String[] lang = m_req.getParameter("lang").split("_");
			Locale locale;
			if(lang.length >= 2)
				locale = new Locale(lang[0], lang[1]);
			else
				locale = new Locale(lang[0]);
			bundle = ResourceBundle.getBundle(getResourceName(), locale);
			if(!bundle.getLocale().getLanguage().equalsIgnoreCase(lang[0]))
				bundle = null;
			else
			{
				Cookie cookie = new Cookie("lang", m_req.getParameter("lang"));
				cookie.setMaxAge(86400*365*2);
				cookie.setPath(m_req.getContextPath());
				m_resp.addCookie(cookie);
			}
		}

		if(bundle == null)
		{
			Cookie[] cookies = m_req.getCookies();
			if(cookies != null)
			{
				for(Cookie cookie : cookies)
				{
					if(cookie.getName().equals("lang"))
					{
						String[] lang = cookie.getValue().split("_");
						Locale locale;
						if(lang.length >= 2)
							locale = new Locale(lang[0], lang[1]);
						else
							locale = new Locale(lang[0]);
						bundle = ResourceBundle.getBundle(getResourceName(), locale);
						if(!bundle.getLocale().getLanguage().equalsIgnoreCase(lang[0]))
							bundle = null;
					}
				}
			}
		}

		if(bundle == null)
		{
			Enumeration<Locale> locales = a_req.getLocales();
			while(locales.hasMoreElements() && bundle == null)
			{
				Locale locale = locales.nextElement();
				bundle = ResourceBundle.getBundle(getResourceName(), locale);
				if(!bundle.getLocale().getLanguage().equals(locale.getLanguage())) // Chose default locale
					bundle = null;
			}
		}

		if(bundle == null)
			bundle = ResourceBundle.getBundle(getResourceName());
		m_bundle = bundle;
	}

	/**
	 * Returns the gettext resource name to be used by the gettext functions.
	 * @return The gettext resource name.
	 */
	abstract protected String getResourceName();

	/**
	 * Returns the list of locales that translations exist for. These will be selectable by the user.
	 * @return A map with the locale name (such as de_DE) in the key and the description (such as Deutsch) in the value.
	 */
	abstract protected Map<String,String> getLocales();

	/**
	 * Sets the meta description of the HTML page and enables search engine indexing.
	 * @param a_description The meta description of the HTML page.
	 */
	public void setDescription(String a_description)
	{
		m_description = a_description;
	}

	/**
	 * Returns the meta description that will be written in the HTML page.
	 * @return The meta description of the page or null if none has been set.
	 */
	public String getDescription()
	{
		return m_description;
	}

	/**
	 * Sets the list of JavaScript URLs that will be included in the HTML page as &lt;script&gt; elements.
	 * @param a_javaScripts A list of URLs to be included as JavaScript.
	 */
	public void setJavaScripts(String[] a_javaScripts)
	{
		m_javaScripts = a_javaScripts;
	}

	/**
	 * Returns the list of JavaScript URLs that will be included in the HTML page.
	 * @return A list of URLs or null.
	 */
	public String[] getJavaScripts()
	{
		if(m_javaScripts == null)
			return new String[]{ };
		return m_javaScripts;
	}

	/**
	 * Sets the title of this page.
	 * @param a_title The title of this page.
	 */
	public void setTitle(String a_title)
	{
		m_title = a_title;
	}

	/**
	 * Returns the title of this page.
	 * @return The title of this page or null if none has been set
	 */
	public String getTitle()
	{
		return m_title;
	}

	/**
	 * Sets an additional CSS class for the body element.
	 * @param a_bodyClass A CSS class name.
	 */
	public void setBodyClass(String a_bodyClass)
	{
		m_bodyClass = a_bodyClass;
	}

	/**
	 * Returns the additional CSS class name of the body element.
	 * @return A CSS class name or null.
	 */
	public String getBodyClass()
	{
		return m_bodyClass;
	}

	/**
	 * Escapes the four HTML special characters &lt;, &gt;, &quot; and &amp; so that a string can be written into
	 * a HTML page.
	 * @param a_str The string to be escaped or null.
	 * @return The escaped string. An empty string if <code>a_str</code> was null.
	 */
	public static String htmlspecialchars(String a_str)
	{
		if(a_str == null)
			return "";
		StringBuilder ret = new StringBuilder(a_str.length());
		for(int i=0; i<a_str.length(); i++)
		{
			char c = a_str.charAt(i);
			switch(c)
			{
				case '&': ret.append("&amp;"); break;
				case '"': ret.append("&quot;"); break;
				case '<': ret.append("&lt;"); break;
				case '>': ret.append("&gt;"); break;
				default: ret.append(c);
			}
		}
		return ret.toString();
	}

	/**
	 * Creates a JavaScript string out of the specified string. The JavaScript string is enclosed in quotation
	 * marks and all special characters are escaped.
	 * @param a_str The string to escape or null.
	 * @return A JavaScript string (an empty JavaScript string if <code>a_str</code> was null)
	 */
	public static String jsescape(String a_str)
	{
		if(a_str == null)
			return "\"\"";

		StringBuilder ret = new StringBuilder(a_str.length()+2);
		ret.append('"');
		for(int i=0; i<a_str.length(); i++)
		{
			char c = a_str.charAt(i);
			if(c == '"')
				ret.append("\\\"");
			else if(c == '\\')
				ret.append("\\\\");
			else if(c == '\n')
				ret.append("\\n");
			else if(c == '\t')
				ret.append("\\t");
			else if(c == '\r')
				ret.append("\\r");
			else if(c <= 0x1f || c >= 0x7f)
				ret.append("\\u").append(String.format("%04x", (long)c));
			else
				ret.append(c);
		}
		ret.append('"');
		return ret.toString();
	}

	/**
	 * Encodes the string to be used inside a URL.
	 * @param a_str The string to escape or null.
	 * @return The escaped string or an empty string if <code>a_str</code> was null.
	 */
	public static String urlencode(String a_str)
	{
		if(a_str == null)
			return "";
		try {
			return URLEncoder.encode(a_str, "UTF-8");
		} catch(UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a format (to be used with {@link String#format}) that can be used to format the value of an OSM
	 * tag with the key <code>a_key</code>. If the key is <code>url</code> for example, a format is returned
	 * that will turn the first <code>args</code> paramter of {@link String#format} into a HTML link.
	 * @param a_key The key of the tag. Has to be escaped (for example with {@link #htmlspecialchars}).
	 * @return A format to be used with {@link String#format}.
	 */
	public static String getTagFormat(String a_key)
	{
		String keyLower = a_key.toLowerCase();

		if(keyLower.equals("url") || keyLower.startsWith("url:"))
			return "<a href=\"%1$s\">%1$s</a>";
		else if(keyLower.equals("wiki:symbol") || keyLower.startsWith("wiki:symbol:"))
			return "<a href=\"http://wiki.openstreetmap.org/wiki/Image:%1$s\">%1$s</a>";
		else if(keyLower.equals("wiki") || keyLower.startsWith("wiki:"))
			return "<a href=\"http://wiki.openstreetmap.org/wiki/%1$s\">%1$s</a>";
		else
			return "%1$s";
	}

	/**
	 * Returns a formatted HTML version of <code>a_value</code>. <code>a_value</code> is the value of an OSM
	 * tag with the key <code>a_key</code>. If the key is <code>url</code> for example, a HTML link to
	 * <code>a_value</code> is returned. This method also handles multiple values in <code>a_value</code>
	 * (currently separated by a <code>;</code>) correctly.
	 * @param a_key The key of the OSM tag.
	 * @param a_value The value of the OSM tag. Has to be escaped (for example with {@link #htmlspecialchars}).
	 * @return A formatted version of <code>a_value</code> to be printed out in a HTML page.
	 */
	public static String formatTag(String a_key, String a_value)
	{
		String format = getTagFormat(a_key);

		StringBuilder ret = new StringBuilder();
		Matcher delimMatcher = Pattern.compile("\\s*;\\*").matcher(a_value);
		int pos = 0;
		while(delimMatcher.find())
		{
			ret.append(String.format(format, a_value.substring(pos, delimMatcher.start())));
			ret.append(delimMatcher.group());
			pos = delimMatcher.end();
		}
		ret.append(String.format(format, a_value.substring(pos)));
		return ret.toString();
	}

	/**
	 * Glues together the string representation of the <code>a_array</code> objects using the specified delimiter.
	 * @param a_delim The delimiter to use.
	 * @param a_array The objects to join together.
	 * @return The imploded string.
	 */
	public static String implode(String a_delim, Object[] a_array)
	{
		StringBuilder ret = new StringBuilder();
		for(int i=0; i<a_array.length; i++)
		{
			if(i > 0)
				ret.append(a_delim);
			ret.append(a_array[i]);
		}
		return ret.toString();
	}

	/**
	 * @see #implode(String, Object[])
	 */
	public static String implode(String a_delim, List<? extends Object> a_array)
	{
		StringBuilder ret = new StringBuilder();
		for(int i=0; i<a_array.size(); i++)
		{
			if(i > 0)
				ret.append(a_delim);
			ret.append(a_array.get(i));
		}
		return ret.toString();
	}

	/**
	 * Returns a random number between the specified boundaries.
	 * @param a_min The minimum value to return.
	 * @param a_max The maximum value to return.
	 * @return A number greater than or equal <code>a_min</code> and less than or equal <code>a_max</code>.
	 */
	public static long rand(long a_min, long a_max)
	{
		return a_min+(long)((a_max-a_min)*Math.random());
	}

	/**
	 * @see #gettext(String)
	 */
	public String _(String a_message)
	{
		return gettext(a_message);
	}

	/**
	 * Returns the gettext translation for the specified message ID.
	 * @param a_message The message ID
	 * @return The gettext translation
	 * @see #getResourceName
	 */
	public String gettext(String a_message)
	{
		return GettextResource.gettext(m_bundle, a_message);
	}

	/**
	 * Returns the gettext plural translation for the specified message ID and the specified number.
	 * @param a_message The singular message ID
	 * @param a_messagePlural The plural message ID
	 * @param a_number The number
	 * @return The gettext translation
	 * @see #getResourceName
	 */
	public String ngettext(String a_message, String a_messagePlural, long a_number)
	{
		return GettextResource.ngettext(m_bundle, a_message, a_messagePlural, a_number);
	}

	/**
	 * Formats the given number with the user’s locale.
	 * @param a_number The number to format.
	 * @return The formatted number as a string.
	 */
	public String formatNumber(Number a_number)
	{
		return a_number.toString();
	}

	/**
	 * Formats the given number with the user’s locale.
	 * @param a_number The number to format.
	 * @param a_decimals The number of decimals to round to.
	 * @return The formatted number as a string.
	 */
	public String formatNumber(Number a_number, int a_decimals)
	{
		return String.format("%."+a_decimals+"f", a_number);
	}

	/**
	 * Prints out the HTML and page header.
	 * @throws IOException Could not send the output.
	 */
	public void head() throws IOException
	{
		PrintWriter out = m_resp.getWriter();

		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
		out.println("\t\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
		out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
		out.println("<!--");
		out.println("\tThis file is free software: you can redistribute it and/or modify");
		out.println("\tit under the terms of the GNU Affero General Public License as published by");
		out.println("\tthe Free Software Foundation, either version 3 of the License, or");
		out.println("\t(at your option) any later version.");
		out.println("");
		out.println("\tThis software is distributed in the hope that it will be useful,");
		out.println("\tbut WITHOUT ANY WARRANTY; without even the implied warranty of");
		out.println("\tMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the");
		out.println("\tGNU Affero General Public License for more details.");
		out.println("");
		out.println("\tYou should have received a copy of the GNU Affero General Public License");
		out.println("\talong with this software.  If not, see <http://www.gnu.org/licenses/>.");
		out.println("");
		out.println("\tCopyright © 2010 Candid Dauth");
		out.println("");
		out.println("\tObtain the source code from http://gitorious.org/osmrmhv.");
		out.println("-->");
		out.println("\t<head>");
		out.println("\t\t<title>"+htmlspecialchars(getTitle())+"</title>");

		String description = getDescription();
		if(description != null)
		{
			out.println("\t\t<meta name=\"description\" content=\""+htmlspecialchars(description)+"\" />");
			out.println("\t\t<meta name=\"robots\" content=\"index,nofollow\" />");
		}
		else
			out.println("\t\t<meta name=\"robots\" content=\"noindex,nofollow\" />");
		
		out.println("\t\t<link rel=\"stylesheet\" href=\"style.css\" type=\"text/css\" />");

		for(String javaScript : getJavaScripts())
			out.println("\t\t<script type=\"text/javascript\" src=\""+htmlspecialchars(javaScript)+"\"></script>");
		
		out.println("\t</head>");
		
		String bodyClass = getBodyClass();
		if(bodyClass == null)
			bodyClass = "";
		else
			bodyClass = " class=\""+htmlspecialchars(bodyClass)+"\"";
		out.println("\t<body"+bodyClass+">");
		out.println("\t\t<h1>"+htmlspecialchars(getTitle())+"</h1>");
		
		StringBuilder urlB = new StringBuilder("?");
		Enumeration<String> params = m_req.getParameterNames();
		while(params.hasMoreElements())
		{
			String key = params.nextElement();
			if(key.equals("lang"))
				continue;

			for(String value : m_req.getParameterValues(key))
			{
				try { urlB.append(URLEncoder.encode(key, "UTF-8")); } catch(UnsupportedEncodingException e) {}
				urlB.append('=');
				try { urlB.append(URLEncoder.encode(value, "UTF-8")); } catch(UnsupportedEncodingException e) {}
				urlB.append('&');
			}
		}
		String url = urlB.toString();
		
		out.println("\t\t<ul id=\"switch-lang\">");
		for(Map.Entry<String,String> locale : getLocales().entrySet())
		{
			if(m_bundle.getLocale().toString().equals(locale.getKey()))
				continue;
			out.println("\t\t\t<li><a href=\""+htmlspecialchars(url+"lang="+locale.getKey())+"\">"+htmlspecialchars(locale.getValue())+"</a></li>");
		}
		out.println("\t\t</ul>");

		out.flush();
	}

	/**
	 * Writes the page and HTML footer.
	 * @throws IOException Could not send the output.
	 */
	public void foot() throws IOException
	{
		PrintWriter out = m_resp.getWriter();
		out.println("\t\t\t<hr />");
		out.println("\t\t\t<p>All geographic data by <a href=\"http://www.openstreetmap.org/\">OpenStreetMap</a>, available under <a href=\"http://creativecommons.org/licenses/by-sa/2.0/\">cc-by-sa-2.0</a>.</p>");
		out.println("\t\t\t<p>This program is free software: you can redistribute it and/or modify it under the terms of the <a href=\"http://www.gnu.org/licenses/agpl.html\">GNU Affero General Public License</a> as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Get the source code via <a href=\"http://gitorious.org/osmrmhv\">Git</a>.</p>");
		out.println("\t</body>");
		out.println("</html>");
	}
}
