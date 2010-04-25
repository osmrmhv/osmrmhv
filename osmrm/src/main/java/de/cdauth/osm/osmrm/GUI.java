package de.cdauth.osm.osmrm;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

import static de.cdauth.osm.osmrm.I18n.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class GUI
{
	public static final Map<Locale,String> LOCALES = new Hashtable<Locale,String>() {{
		put(Locale.ENGLISH, "English");
		put(Locale.GERMAN, "Deutsch");
		put(new Locale("hu"), "Magyar");
	}};
	
	public static final Charset UTF8 = Charset.forName("UTF-8");

	private String m_description = null;
	private String[] m_javaScripts = null;
	private String m_title = null;
	private String m_bodyClass = null;
	
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
		if(m_title == null)
			return _("OSM Route Manager");
		else
			return sprintf(_("OSM Route Manager: %s"), m_title);
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
			{
				ret.append("\\u");
				if(c > 0xff)
					ret.append('0');
				else if(c > 0xf)
					ret.append("00");
				else
					ret.append("000");
				ret.append(Integer.toHexString(c));
			}
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
	
	public void head(HttpServletRequest a_req, HttpServletResponse a_resp) throws IOException
	{
		PrintWriter out = a_resp.getWriter();

		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
		out.println("\t\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
		out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
		out.println("<!--");
		out.println("\tThis file is part of OSM Route Manager.");
		out.println("");
		out.println("\tOSM Route Manager is free software: you can redistribute it and/or modify");
		out.println("\tit under the terms of the GNU Affero General Public License as published by");
		out.println("\tthe Free Software Foundation, either version 3 of the License, or");
		out.println("\t(at your option) any later version.");
		out.println("");
		out.println("\tOSM Route Manager is distributed in the hope that it will be useful,");
		out.println("\tbut WITHOUT ANY WARRANTY; without even the implied warranty of");
		out.println("\tMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the");
		out.println("\tGNU Affero General Public License for more details.");
		out.println("");
		out.println("\tYou should have received a copy of the GNU Affero General Public License");
		out.println("\talong with OSM Route Manager.  If not, see <http://www.gnu.org/licenses/>.");
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
		Enumeration<String> params = a_req.getParameterNames();
		while(params.hasMoreElements())
		{
			String key = params.nextElement();
			if(key.equals("lang"))
				continue;

			for(String value : a_req.getParameterValues(key))
			{
				try { urlB.append(URLEncoder.encode(key, "UTF-8")); } catch(UnsupportedEncodingException e) {}
				urlB.append('=');
				try { urlB.append(URLEncoder.encode(value, "UTF-8")); } catch(UnsupportedEncodingException e) {}
				urlB.append('&');
			}
		}
		String url = urlB.toString();
		
		out.println("\t\t<ul id=\"switch-lang\">");
		for(Map.Entry<Locale,String> locale : LOCALES.entrySet())
		{
			// FIXME: Skip current locale
			out.println("\t\t\t<li><a href=\""+htmlspecialchars(url+"lang="+locale.getKey().getLanguage())+"\">"+htmlspecialchars(locale.getValue())+"</a></li>");
		}
		out.println("\t\t</ul>");
	}

	public void foot(HttpServletRequest a_req, HttpServletResponse a_resp) throws IOException
	{
		PrintWriter out = a_resp.getWriter();
		out.println("\t\t\t<hr />");
		out.println("\t\t\t<p>All geographic data by <a href=\"http://www.openstreetmap.org/\">OpenStreetMap</a>, available under <a href=\"http://creativecommons.org/licenses/by-sa/2.0/\">cc-by-sa-2.0</a>.</p>");
		out.println("\t\t\t<p>OSM Route Manager is free software: you can redistribute it and/or modify it under the terms of the <a href=\"http://www.gnu.org/licenses/agpl.html\">GNU Affero General Public License</a> as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Get the source code via <a href=\"http://gitorious.org/osmrmhv\">Git</a>.</p>");
		out.println("\t</body>");
		out.println("</html>");
	}
}
