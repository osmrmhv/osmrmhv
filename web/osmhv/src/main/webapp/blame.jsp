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
<%@page import="java.io.PrintWriter"%>
<%@page import="eu.cdauth.osm.lib.*"%>
<%@page import="eu.cdauth.osm.web.osmhv.*"%>
<%@page import="eu.cdauth.osm.web.common.Cache"%>
<%@page import="eu.cdauth.osm.web.common.Queue"%>
<%@page import="static eu.cdauth.osm.web.osmhv.GUI.*"%>
<%@page import="java.util.*" %>
<%@page import="java.net.URL" %>
<%@page contentType="text/html; charset=UTF-8" buffer="none" session="false"%>
<%!
	private static final Queue queue = Queue.getInstance();

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
			int ret = a_other.changes.compareTo(changes);
			if(ret == 0)
				ret = user.compareTo(a_other.user);
			return ret;
		}

		//@Override
		public boolean equals(Object a_other)
		{
			return a_other instanceof UserChangeNumber && ((UserChangeNumber)a_other).user.equals(user) && ((UserChangeNumber)a_other).changes.equals(changes);
		}
	}

	public void jspInit()
	{
		if(RelationBlame.cache == null)
		{
			RelationBlame.cache = new Cache<RelationBlame>(GUI.getCacheDirectory(getServletContext())+"/osmhv/blame");
			RelationBlame.cache.setMaxAge(86400*7);
		}
	}
%>
<%
	ID relationID = null;
	if(request.getParameter("id") != null)
	{
		try {
			relationID = new ID(request.getParameter("id").replaceFirst("^\\s*#?(.*?)\\s*$", "$1"));
		}
		catch(NumberFormatException e) {
		}
	}

	if(relationID == null)
	{
		response.setStatus(response.SC_MOVED_PERMANENTLY);
		URL thisUrl = new URL(request.getRequestURL().toString());
		response.setHeader("Location", new URL(thisUrl.getProtocol(), thisUrl.getHost(), thisUrl.getPort(), request.getContextPath()).toString());
		return;
	}

	if(request.getParameter("refresh") != null)
	{
		queue.scheduleTask(RelationBlame.worker, relationID);
		response.setStatus(HttpServletResponse.SC_SEE_OTHER);
		URL thisUrl = new URL(request.getRequestURL().toString());
		response.setHeader("Location", new URL(thisUrl.getProtocol(), thisUrl.getHost(), thisUrl.getPort(), thisUrl.getPath()).toString()+"?id="+GUI.urlencode(request.getParameter("id")));
		return;
	}

	GUI gui = new GUI(request, response);
	if(relationID != null)
		gui.setTitle(String.format(gui._("Relation %s"), relationID.toString()));
	gui.setJavaScripts(new String[]{
		"http://www.openlayers.org/api/2.11/OpenLayers.js",
		"http://maps.google.com/maps?file=api&v=2&key=ABQIAAAApZR0PIISH23foUX8nxj4LxQThUMYtIDw2iC2eUfTIWgH1b-HoBTSr3REfzI5VMINqd64RXBrAV696w",
		"http://api.maps.yahoo.com/ajaxymap?v=3.0&appid=osmhv",
		"http://api.facilmap.org/facilmap.js",
		"http://api.facilmap.org/osblayer/osblayer.js"
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

	Cache.Entry<RelationBlame> cacheEntry = RelationBlame.cache.getEntry(relationID.toString());
	int queuePosition = queue.getPosition(RelationBlame.worker, relationID);
	if(cacheEntry == null)
	{
		if(queuePosition == 0)
		{
			Queue.Notification notify = queue.scheduleTask(RelationBlame.worker, relationID);
			notify.sleep(20000);
			cacheEntry = RelationBlame.cache.getEntry(relationID.toString());
			queuePosition = queue.getPosition(RelationBlame.worker, relationID);
		}
	}

	if(cacheEntry != null)
	{
		if(cacheEntry.content.exception == null)
		{
%>
<div id="map" class="blame-map"></div>
<%
		}
%>
<p><%=String.format(htmlspecialchars(gui._("The data was last refreshed on %s. The timestamp of the relation is %s.")), gui.formatDate(cacheEntry == null ? null : cacheEntry.date), gui.formatDate(cacheEntry.content.timestamp))%></p>
<%
	}

	if(queuePosition > 0)
	{
%>
<p class="scheduled"><strong><%=htmlspecialchars(String.format(gui._("An analysation of the current version of the relation is scheduled. The position in the queue is %d. Reload this page after a while to see the updated version."), queuePosition))%></strong></p>
<%
	}
	else
	{
%>
<p><%=String.format(htmlspecialchars(gui._("If you think something might have been changed, %sreload the data manually%s.")), "<a href=\"?id="+htmlspecialchars(urlencode(relationID.toString())+"&refresh=1")+"\">", "</a>")%></p>
<%
	}

	if(cacheEntry != null)
	{
		RelationBlame blame = cacheEntry.content;

		if(blame.exception != null)
		{
%>
<p class="error"><strong><%=htmlspecialchars(blame.exception.toString())%></strong></p>
<%
		}
		else
		{
			Map<User,Set<Changeset>> userChangesets = new HashMap<User,Set<Changeset>>();
			Map<Changeset,List<Segment>> changesetSegments = new HashMap<Changeset,List<Segment>>();
			for(Map.Entry<Segment,Changeset> segment : blame.segmentChangeset.entrySet())
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
<div class="blame-changesets">
	<h2><%=htmlspecialchars(gui._("Affecting changesets by user"))%> (<a href="javascript:setGlobalVisibility(false)"><%=htmlspecialchars(gui._("Hide all"))%></a>) (<a href="javascript:setGlobalVisibility(true)"><%=htmlspecialchars(gui._("Show all"))%></a>) (<a href="javascript:map.zoomToExtent(extent)"><%=htmlspecialchars(gui._("Zoom all"))%></a>)</h2>
	<ul>
<%
			for(UserChangeNumber user : userChangeNumber)
			{
%>
		<li><strong class="user-colour" style="color:<%=htmlspecialchars(userColours.get(user.user))%>;"><a href="http://www.openstreetmap.org/user/<%=htmlspecialchars(urlencode(user.user.toString()))%>"><%=htmlspecialchars(user.user.toString())%></a></strong><ul>
<%
				for(Changeset changeset : userChangesets.get(user.user))
				{
					String id = changeset.getID().toString();
					String message = changeset.getTag("comment");
%>
			<li><input type="checkbox" id="checkbox-<%=id%>" onchange="layers[<%=id%>].setVisibility(this.checked);" /><label for="checkbox-<%=id%>"><%=id%>: <%=!message.equals("") ? String.format(gui._("“%s”"), htmlspecialchars(message)) : "<span class=\"nocomment\">"+htmlspecialchars(gui._("No comment"))+"</span>"%></label> (<a href="javascript:map.zoomToExtent(layers['<%=id%>'].getDataExtent())"><%=htmlspecialchars(gui._("Zoom"))%></a>) (<a href="http://www.openstreetmap.org/browse/changeset/<%=htmlspecialchars(urlencode(id))%>"><%=htmlspecialchars(gui._("browse"))%></a>) (<a href="changeset.jsp?id=<%=htmlspecialchars(urlencode(id))%>"><%=htmlspecialchars(gui._("view"))%></a>)</li>
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
	var map = new FacilMap.Map("map");
	map.addAllAvailableLayers();

	window.onresize = function(){ document.getElementById("map").style.height = Math.round(window.innerHeight*.9)+"px"; map.updateSize(); }
	window.onresize();

	var osbLayer = new OpenLayers.Layer.OpenStreetBugs("OpenStreetBugs", { visibility: false, shortName: "osb" });
	map.addLayer(osbLayer);
	osbLayer.setZIndex(500);

	var layerMarkers = new FacilMap.Layer.Markers.LonLat("Markers", { shortName: "m" });
	map.addLayer(layerMarkers);
	var clickControl = new FacilMap.Control.CreateMarker(layerMarkers);
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

	var hashHandler = new FacilMap.Control.URLHashHandler();
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
		}
	}

	gui.foot();
%>
