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
<%@page import="java.net.URL" %>
<%@page import="java.net.URLEncoder" %>
<%@page contentType="text/html; charset=UTF-8" buffer="none" session="false"%>
<%!
	protected static final API api = GUI.getAPI();

	protected static final String[] predefinedColours = new String[] {
		"#000",
		"#f00",
		"#0f0",
		"#00f",
		"#ff0",
		"#f0f",
		"#0ff"
	};

	protected static class UserChangeNumber implements Comparable<UserChangeNumber>
	{
		public final User user;
		public final Integer changes;

		public UserChangeNumber(User a_user, int a_changes)
		{
			user = a_user;
			changes = a_changes;
		}

		//@Override
		public int compareTo(UserChangeNumber a_other)
		{
			return a_other.changes.compareTo(changes);
		}
	}
%>
<%
	if(request.getParameter("id") == null)
	{
		response.setStatus(response.SC_MOVED_PERMANENTLY);
		URL thisUrl = new URL(request.getRequestURL().toString());
		response.setHeader("Location", new URL(thisUrl.getProtocol(), thisUrl.getHost(), thisUrl.getPort(), request.getContextPath()).toString());
		return;
	}

	GUI gui = new GUI(request, response);

	ID relationID = new ID(request.getParameter("id").replace("^\\s*#?(.*)\\s*$", "$1"));

	if(relationID != null)
		gui.setTitle(String.format(gui._("Relation %s"), relationID.toString()));
	gui.setJavaScripts(new String[]{
		"http://www.openlayers.org/api/OpenLayers.js",
		"http://maps.google.com/maps?file=api&v=2&key=ABQIAAAApZR0PIISH23foUX8nxj4LxQe8fls808ouw55mfsb9VLPMfxZSBRAxMJl1CWns7WN__O20IuUSiDKng",
		"http://api.maps.yahoo.com/ajaxymap?v=3.0&appid=cdauths-map",
		"http://osm.cdauth.de/map/prototypes.js",
		"http://osm.cdauth.de/map/openstreetbugs.js"
	});

	gui.head();
%>
<ul>
	<li><a href="./"><%=htmlspecialchars(gui._("Back to home page"))%></a></li>
	<li><a href="http://www.openstreetmap.org/browse/relation/<%=htmlspecialchars(urlencode(relationID.toString()))%>"><%=htmlspecialchars(gui._("Browse on OpenStreetMap"))%></a></li>
</ul>
<noscript><p><strong><%=htmlspecialchars(gui._("Note that many features of this page will not work without JavaScript."))%></strong></p></noscript>
<%
	response.getWriter().flush();

	Map<Segment,Changeset> blame = HistoryViewer.blame(api, relationID);

	Map<User,Set<Changeset>> userChangesets = new HashMap<User,Set<Changeset>>();
	Map<Changeset,List<Segment>> changesetSegments = new HashMap<Changeset,List<Segment>>();
	for(Map.Entry<Segment,Changeset> segment : blame.entrySet())
	{
		User user = segment.getValue().getUser();
		Set<Changeset> changesets = userChangesets.get(user);
		if(changesets == null)
		{
			changesets = new HashSet<Changeset>();
			userChangesets.put(user, changesets);
		}
		List<Segment> segments = changesetSegments.get(segment.getValue());
		if(segments == null)
		{
			segments = new ArrayList<Segment>();
			changesetSegments.put(segment.getValue(), segments);
		}

		changesets.add(segment.getValue());
		segments.add(segment.getKey());
	}

	TreeSet<UserChangeNumber> userChangeNumber = new TreeSet<UserChangeNumber>();
	for(Map.Entry<User,Set<Changeset>> user : userChangesets.entrySet())
	{
		int number = 0;
		for(Changeset changeset : user.getValue())
			number += changesetSegments.get(changeset).size();
		userChangeNumber.add(new UserChangeNumber(user.getKey(), number));
	}

	Map<User,String> userColours = new HashMap<User,String>();
	int i = 0;
	for(UserChangeNumber user : userChangeNumber)
	{
		if(i < predefinedColours.length)
			userColours.put(user.user, predefinedColours[i++]);
		else
		{
			long n1 = rand(6, 12);
			long n2 = rand(12-n1, 12);
			long n3 = 30-n1-n2;
			userColours.put(user.user, String.format("#%x%x%x", n1, n2, n3));
		}
	}
%>
<div id="map" class="blame-map"></div>
<div class="blame-changesets">
	<h2><%=htmlspecialchars(gui._("Affecting changesets by user"))%> (<a href="javascript:setGlobalVisibility(false)"><%=htmlspecialchars(gui._("Hide all"))%></a>) (<a href="javascript:setGlobalVisibility(true)"><%=htmlspecialchars(gui._("Show all"))%></a>) (<a href="javascript:map.zoomToExtent(extent)"><%=htmlspecialchars(gui._("Zoom all"))%></a>)</h2>
	<ul>
<%
	for(UserChangeNumber user : userChangeNumber)
	{
%>
		<li><strong class="user-colour" style="color:<%=htmlspecialchars(userColours.get(user.user))%>;"><a href="http://www.openstreetmap.org/user/<%=htmlspecialchars(URLEncoder.encode(user.user.toString()))%>"><%=htmlspecialchars(user.user.toString())%></a></strong><ul>
<%
		for(Changeset changeset : userChangesets.get(user.user))
		{
			String id = changeset.getID().toString();
			String message = changeset.getTag("comment");
%>
			<li><input type="checkbox" id="checkbox-<%=id%>" onchange="layers[<%=id%>].setVisibility(this.checked);" /><%=id%>: <%=message != null ? String.format(gui._("“%s”"), htmlspecialchars(message)) : "<span class=\"nocomment\">"+htmlspecialchars(gui._("No comment"))+"</span>"%> (<a href="javascript:map.zoomToExtent(layers['<%=id%>'].getDataExtent())"><%=htmlspecialchars(gui._("Zoom"))%></a>) (<a href="http://www.openstreetmap.org/browse/changeset/<%=htmlspecialchars(URLEncoder.encode(id))%>"><%=htmlspecialchars(gui._("browse"))%></a>) (<a href="changeset.jsp?id=<%=htmlspecialchars(URLEncoder.encode(id))%>"><%=htmlspecialchars(gui._("view"))%></a>)</li>
<%
	}
%>
		</ul></li>
<%
}
%>
	</ul>
</div>
<script type="text/javascript">
// <![CDATA[
	var map = new OpenLayers.Map.cdauth("map");
	map.addAllAvailableLayers();

	window.onresize = function(){ document.getElementById("map").style.height = Math.round(window.innerHeight*.9)+"px"; map.updateSize(); }
	window.onresize();

	var osbLayer = new OpenLayers.Layer.OpenStreetBugs("OpenStreetBugs", { visibility: false, shortName: "osb" });
	map.addLayer(osbLayer);
	osbLayer.setZIndex(500);

	var layerMarkers = new OpenLayers.Layer.cdauth.Markers.LonLat("Markers", { shortName: "m" });
	map.addLayer(layerMarkers);
	var clickControl = new OpenLayers.Control.cdauth.CreateMarker(layerMarkers);
	map.addControl(clickControl);
	clickControl.activate();

	var projection = new OpenLayers.Projection("EPSG:4326");

	var extent = null;

	var layers = { };
<%
	for(Map.Entry<Changeset,List<Segment>> changeset : changesetSegments.entrySet())
	{
		String id = changeset.getKey().getID().toString();
%>
	layers['<%=id%>'] = new OpenLayers.Layer.PointTrack("Changeset <%=id%>", { styleMap : new OpenLayers.StyleMap({strokeColor: "<%=userColours.get(changeset.getKey().getUser())%>", strokeWidth: 5, strokeOpacity: 0.6, shortName: "<%=id%>"}), projection : projection, zoomableInLayerSwitcher : true });
<%
		for(Segment segment : changeset.getValue())
		{
			LonLat node1 = segment.getNode1().getLonLat();
			LonLat node2 = segment.getNode2().getLonLat();
%>
	layers['<%=id%>'].addNodes([new OpenLayers.Feature(layers['<%=id%>'], new OpenLayers.LonLat(<%=node1.getLon()%>, <%=node1.getLat()%>).transform(projection, map.getProjectionObject())),new OpenLayers.Feature(layers['<%=id%>'], new OpenLayers.LonLat(<%=node2.getLon()%>, <%=node2.getLat()%>).transform(projection, map.getProjectionObject()))]);
<%
		}
%>
	map.addLayer(layers['<%=id%>']);
	if(extent)
		extent.extend(layers['<%=id%>'].getDataExtent());
	else
		extent = layers['<%=id%>'].getDataExtent();

<%
	}
%>
	if(extent)
		map.zoomToExtent(extent);

	var hashHandler = new OpenLayers.Control.cdauth.URLHashHandler();
	map.addControl(hashHandler);
	hashHandler.activate();

	function setGlobalVisibility(visibility)
	{
		for(var i in layers)
			layers[i].setVisibility(visibility);
	}

	function checkLayerVisibility()
	{
		for(var i in layers)
			document.getElementById("checkbox-"+i).checked = layers[i].getVisibility();
	}

	map.events.register("changelayer", null, checkLayerVisibility);
	checkLayerVisibility();
// ]]>
</script>
<%
	gui.foot();
%>
