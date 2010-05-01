<%--
	This file is part of the OSM History Viewer.

	OSM History Viewer is free software: you can redistribute it and/or modify
	it under the terms of the GNU Affero General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	OSM History Viewer is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Affero General Public License for more details.

	You should have received a copy of the GNU Affero General Public License
	along with this software. If not, see <http://www.gnu.org/licenses/>.

	Copyright © 2010 Candid Dauth
--%>
<%@page import="eu.cdauth.osm.lib.*"%>
<%@page import="eu.cdauth.osm.web.osmhv.*"%>
<%@page import="static eu.cdauth.osm.web.osmhv.GUI.*"%>
<%@page import="java.util.*" %>
<%@page contentType="text/html; charset=UTF-8" buffer="none" session="false"%>
<%!
	protected static final API api = GUI.getAPI();
%>
<%
	GUI gui = new GUI(request, response);

	if(!request.getParameterNames().hasMoreElements())
		gui.setDescription(gui._("OSM History Viewer is a debugging tool for Changesets in OpenStreetMap."));
	gui.setJavaScripts(new String[]{ "sortable.js" });

	gui.head();
%>
<form action="changeset.jsp" method="get" id="changeset-form">
	<fieldset>
		<legend><%=htmlspecialchars(gui._("Visualise changeset"))%></legend>
		<dl>
			<dt><label for="i-lookup-id"><%=htmlspecialchars(gui._("Changeset ID"))%></label></dt>
			<dd><input type="text" name="id" id="i-lookup-id" /></dd>
		</dl>
		<button type="submit"><%=htmlspecialchars(gui._("Lookup"))%></button>
	</fieldset>
</form>

<fieldset id="search-form">
	<legend><%=htmlspecialchars(gui._("Relation Blame"))%></legend>
	<form action="blame.jsp" method="get">
		<dl>
			<dt><label for="i-blame"><%=htmlspecialchars(gui._("Relation ID"))%></label></dt>
			<dd><input type="text" id="i-blame" name="id" /></dd>
		</dl>
		<div><input type="submit" value="<%=htmlspecialchars(gui._("Blame"))%>" /></div>
	</form>
	<hr />
<%
	final String searchKey = (request.getParameter("search-key") != null ? request.getParameter("search-key").trim() : null);
	final String searchValue = (request.getParameter("search-value") != null ? request.getParameter("search-value").trim() : null);
	if(searchKey != null && !searchKey.equals("") && searchValue != null && !searchValue.equals(""))
	{
		try
		{
			Set<Relation> results = api.getRelationFactory().search(new HashMap<String,String>(){{ put(searchKey, searchValue); }});
%>
		<table class="result sortable" id="resultTable">
			<thead>
				<tr>
					<th><%=htmlspecialchars(gui._("ID"))%></th>
					<th>type</th>
					<th>route</th>
					<th>network</th>
					<th>ref</th>
					<th>name</th>
					<th class="unsortable"><%=htmlspecialchars(gui._("Open"))%></th>
				</tr>
			</thead>
			<tbody>
<%
			for(Relation result : results)
			{
%>
				<tr>
					<td><%=htmlspecialchars(result.getID().toString())%></td>
					<td><%=htmlspecialchars(result.getTag("type"))%></td>
					<td><%=htmlspecialchars(result.getTag("route"))%></td>
					<td><%=htmlspecialchars(result.getTag("network"))%></td>
					<td><%=htmlspecialchars(result.getTag("ref"))%></td>
					<td><%=htmlspecialchars(result.getTag("name"))%></td>
					<td><a href="blame.jsp?id=<%=htmlspecialchars(urlencode(result.getID().toString()))%>"><%=htmlspecialchars(gui._("Open"))%></a> (<a href="blame.jsp?id=<%=htmlspecialchars(urlencode(result.getID().toString())+"&norender=on")%>"><%=htmlspecialchars(gui._("No map"))%></a>)</td>
				</tr>
<%
			}
%>
			</tbody>
		</table>
<%
		}
		catch(Exception e)
		{
%>
		<p class="error"><%=htmlspecialchars(e.getMessage())%></p>
<%
		}
	}
%>
	<form action="#search-form" method="get">
		<dl>
			<dt><label for="i-search-key"><%=htmlspecialchars(gui._("Key"))%></label></dt>
			<dd><select id="i-search-key" name="search-key">
				<option value="name"<% if("name".equals(request.getParameter("search-key"))){%> selected="selected""<% }%>>name</option>
				<option value="ref"<% if("ref".equals(request.getParameter("search-key"))){%> selected="selected""<% }%>>ref</option>
				<option value="operator"<% if("operator".equals(request.getParameter("search-key"))){%> selected="selected""<% }%>>operator</option>
			</select></dd>

			<dt><label for="i-search-value"><%=htmlspecialchars(gui._("Value"))%></label></dt>
			<dd><input type="text" id="i-search-value" name="search-value" value="<%=htmlspecialchars(request.getParameter("search-value"))%>" /></dd>
		</dl>
		<input type="submit" value="<%=htmlspecialchars(gui._("Search using OSM API"))%>" />
	</form>
</fieldset>
<%
	gui.foot();
%>
