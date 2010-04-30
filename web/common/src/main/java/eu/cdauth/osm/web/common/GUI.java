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
	
	abstract protected String getResourceName();

	abstract protected Map<String,String> getLocales();
	
	public void setDescription(String a_description)
	{
		m_description = a_description;
	}
	
	public String getDescription()
	{
		return m_description;
	}
	
	public void setJavaScripts(String[] a_javaScripts)
	{
		m_javaScripts = a_javaScripts;
	}
	
	public String[] getJavaScripts()
	{
		if(m_javaScripts == null)
			return new String[]{ };
		return m_javaScripts;
	}
	
	public void setTitle(String a_title)
	{
		m_title = a_title;
	}
	
	public String getTitle()
	{
		return m_title;
	}
	
	public void setBodyClass(String a_bodyClass)
	{
		m_bodyClass = a_bodyClass;
	}
	
	public String getBodyClass()
	{
		return m_bodyClass;
	}
	
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
				ret.append("\\u"+String.format("%04x", c));
			else
				ret.append(c);
		}
		ret.append('"');
		return ret.toString();
	}

	public static String urlencode(String a_str)
	{
		try {
			return URLEncoder.encode(a_str, "UTF-8");
		} catch(UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

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

	public static long rand(long a_min, long a_max)
	{
		return a_min+(long)((a_max-a_min)*Math.random());
	}

	public String _(String a_message)
	{
		return gettext(a_message);
	}

	public String gettext(String a_message)
	{
		return GettextResource.gettext(m_bundle, a_message);
	}

	public String ngettext(String a_message, String a_messagePlural, long a_number)
	{
		return GettextResource.ngettext(m_bundle, a_message, a_messagePlural, a_number);
	}

	public String formatNumber(Number a_number)
	{
		return a_number.toString();
	}

	public String formatNumber(Number a_number, int a_decimals)
	{
		return String.format("%."+a_decimals+"f", a_number);
	}

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
