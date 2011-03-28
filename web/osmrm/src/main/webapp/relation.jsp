<%--
	This file is part of the OSM Route Manager.

	OSM Route Manager is free software: you can redistribute it and/or modify
	it under the terms of the GNU Affero General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	OSM Route Manager is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Affero General Public License for more details.

	You should have received a copy of the GNU Affero General Public License
	along with this software. If not, see <http://www.gnu.org/licenses/>.

	Copyright © 2010 Candid Dauth
--%>
<%@page import="java.io.PrintWriter"%>
<%@page import="java.io.StringWriter"%>
<%@page import="eu.cdauth.osm.lib.*"%>
<%@page import="eu.cdauth.osm.web.osmrm.*"%>
<%@page import="eu.cdauth.osm.web.common.Cache"%>
<%@page import="eu.cdauth.osm.web.common.Queue"%>
<%@page import="static eu.cdauth.osm.web.osmrm.GUI.*"%>
<%@page import="java.util.*" %>
<%@page import="java.net.URL" %>
<%@page contentType="text/html; charset=UTF-8" buffer="none" session="false"%>
<%!
	private static final Queue queue = Queue.getInstance();

	public void jspInit()
	{
		if(RouteAnalyser.cache == null)
		{
			RouteAnalyser.cache = new Cache<RouteAnalyser>(GUI.getCacheDirectory(getServletContext())+"/osmrm");
			RouteAnalyser.cache.setMaxAge(86400);
		}
	}
%>
<%
	ID relationId = null;
	if(request.getParameter("id") != null)
	{
		try {
			relationId = new ID(request.getParameter("id").replaceFirst("^\\s*#?(.*?)\\s*$", "$1"));
		}
		catch(NumberFormatException e) {
		}
	}

	if(relationId == null)
	{
		response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
		URL thisUrl = new URL(request.getRequestURL().toString());
		response.setHeader("Location", new URL(thisUrl.getProtocol(), thisUrl.getHost(), thisUrl.getPort(), request.getContextPath()).toString());
		return;
	}

	if(request.getParameter("refresh") != null)
	{
		queue.scheduleTask(RouteAnalyser.WORKER, relationId);
		response.setStatus(HttpServletResponse.SC_SEE_OTHER);
		URL thisUrl = new URL(request.getRequestURL().toString());
		response.setHeader("Location", new URL(thisUrl.getProtocol(), thisUrl.getHost(), thisUrl.getPort(), thisUrl.getPath()).toString()+"?id="+GUI.urlencode(request.getParameter("id")));
		return;
	}

	GUI gui = new GUI(request, response);
	gui.setTitle(String.format(gui._("Relation %s"), relationId.toString()));

	boolean render = (request.getParameter("norender") == null);
	if(!render)
		gui.setBodyClass("norender");
	else
		gui.setJavaScripts(new String[] {
			"http://www.openlayers.org/api/OpenLayers.js",
			"http://maps.google.com/maps?file=api&v=2&key=ABQIAAAApZR0PIISH23foUX8nxj4LxS8NUQFcZ1bqJDZDJKH2YPZutbv6BQlk1uBrKkr2JOwOrR4WAupqtWm8g",
			"http://api.maps.yahoo.com/ajaxymap?v=3.0&appid=osmrm",
			"http://api.facilmap.org/facilmap.js",
			"http://api.facilmap.org/osblayer/osblayer.js"
		});

	gui.head();
%>
<ul>
	<li><a href="./"><%=htmlspecialchars(gui._("Back to home page"))%></a></li>
	<li><a href="http://betaplace.emaitie.de/webapps.relation-analyzer/analyze.jsp?relationId=<%=htmlspecialchars(urlencode(relationId.toString()))%>"><%=htmlspecialchars(gui._("Open this in OSM Relation Analyzer"))%></a></li>
	<li><a href="http://www.openstreetmap.org/browse/relation/<%=htmlspecialchars(urlencode(relationId.toString()))%>"><%=htmlspecialchars(gui._("Browse on OpenStreetMap"))%></a></li>
	<li><a href="http://osm.cdauth.eu/history-viewer/blame.jsp?id=<%=htmlspecialchars(urlencode(relationId.toString()))%>"><%=htmlspecialchars(gui._("Blame with History Viewer"))%></a></li>
</ul>
<noscript><p><strong><%=htmlspecialchars(gui._("Note that many features of this page will not work without JavaScript."))%></strong></p></noscript>
<%
	Cache.Entry<RouteAnalyser> cacheEntry = RouteAnalyser.cache.getEntry(relationId.toString());
	int queuePosition = queue.getPosition(RouteAnalyser.WORKER, relationId);
	if(cacheEntry == null)
	{
		if(queuePosition == 0)
		{
			Queue.Notification notify = queue.scheduleTask(RouteAnalyser.WORKER, relationId);
			notify.sleep(20000);
			cacheEntry = RouteAnalyser.cache.getEntry(relationId.toString());
			queuePosition = queue.getPosition(RouteAnalyser.WORKER, relationId);
		}
	}

	if(cacheEntry != null)
	{
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
<p><%=String.format(htmlspecialchars(gui._("If you think something might have been changed, %sreload the data manually%s.")), "<a href=\"?id="+htmlspecialchars(urlencode(relationId.toString())+"&refresh=1")+"\">", "</a>")%></p>
<%
	}

	if(cacheEntry != null)
	{
		RouteAnalyser route = cacheEntry.content;

		if(route.exception != null)
		{
%>
<p class="error"><strong><%=htmlspecialchars(route.exception.toString())%></strong></p>
<%
		}
		else
		{
%>
<h2><%=htmlspecialchars(gui._("Tags"))%></h2>
<dl>
<%
			for(Map.Entry<String,String> tag : route.tags.entrySet())
			{
%>
	<dt><%=htmlspecialchars(tag.getKey())%></dt>
<%
				String format = getTagFormat(tag.getKey());
				String[] values = tag.getValue().split("\\s*;\\s*");
				for(String value : values)
				{
%>
	<dd><%=String.format(format, htmlspecialchars(value))%></dd>
<%
				}
			}
%>
</dl>
<%
			User user = route.changeset.getUser();
%>
<h2><%=htmlspecialchars(gui._("Details"))%></h2>
<dl>
	<dt><%=htmlspecialchars(gui._("Last changed"))%></dt>
	<dd><%=String.format(gui._("%s by %s"), route.timestamp.toString(), "<a href=\"http://www.openstreetmap.org/user/"+urlencode(user.getName())+"\">"+htmlspecialchars(user.getName())+"</a>")%></dd>

	<dt><%=htmlspecialchars(gui._("Total length"))%></dt>
	<dd><%=gui.formatNumber(route.totalLength, 2)%>&thinsp;km</dd>

	<dt><%=htmlspecialchars(gui._("Sub-relations"))%></dt>
	<dd><ul>
<%
			for(Map.Entry<ID,String> subRelation : route.subRelations.entrySet())
			{
%>
		<li><a href="?id=<%=htmlspecialchars(urlencode(subRelation.getKey().toString()))%>"><%=htmlspecialchars(subRelation.getKey().toString())%> (<%=htmlspecialchars(subRelation.getValue())%>)</a></li>
<%
			}
%>
	</ul></dd>

	<dt><%=htmlspecialchars(gui._("Parent relations"))%></dt>
	<dd><ul>
<%
			for(Map.Entry<ID,String> parentRelation : route.parentRelations.entrySet())
			{
%>
		<li><a href="?id=<%=htmlspecialchars(urlencode(parentRelation.getKey().toString()))%>"><%=htmlspecialchars(parentRelation.getKey().toString())%> (<%=htmlspecialchars(parentRelation.getValue())%>)</a></li>
<%
			}
%>
	</ul></dd>
</dl>
<h2><%=htmlspecialchars(gui._("Segments"))%></h2>
<%
			if(render)
			{
%>
<p><%=htmlspecialchars(gui._("Get GPS coordinates by clicking on the map."))%></p>
<%
			}
%>
<div id="segment-list">
	<table>
		<thead>
			<tr>
				<th><%=htmlspecialchars(gui._("Segment #"))%></th>
				<th><%=htmlspecialchars(gui._("Length"))%></th>
				<th><%=htmlspecialchars(gui._("Distance to next segments"))%></th>
<%
			if(render)
			{
%>
				<th><%=htmlspecialchars(gui._("Visible"))%></th>
				<th><%=htmlspecialchars(gui._("Zoom"))%></th>
<%
			}
%>
				<th><%=htmlspecialchars(gui._("Add to personal route"))%></th>
			</tr>
		</thead>
		<tbody>
<%
			for(int i=0; i<route.segments.length; i++)
			{
				LonLat end1 = route.segments[i].getEnd1();
				LonLat end2 = route.segments[i].getEnd2();
%>
			<tr<% if(render){%> onmouseover="highlightSegment(<%=i%>);" onmouseout="unhighlightSegment(<%=i%>);"<% }%> id="tr-segment-<%=i%>" class="tr-segment-normal">
				<td><%=htmlspecialchars(""+(i+1))%></td>
				<td><%=gui.formatNumber(route.segments[i].getDistance(), 2)%>&thinsp;km</td>
				<td><% if(route.segments.length > 1){%><a href="javascript:zoomToGap(new OpenLayers.LonLat(<%=end1.getLon()%>, <%=end1.getLat()%>), new OpenLayers.LonLat(<%=route.distance1Target[i].getLon()%>, <%=route.distance1Target[i].getLat()%>))"><%=gui.formatNumber(route.distance1[i], 2)%>&thinsp;km</a>, <a href="javascript:zoomToGap(new OpenLayers.LonLat(<%=end2.getLon()%>, <%=end2.getLat()%>), new OpenLayers.LonLat(<%=route.distance2Target[i].getLon()%>, <%=route.distance2Target[i].getLat()%>))"><%=gui.formatNumber(route.distance2[i], 2)%>&thinsp;km</a><% }%></td>
<%
				if(render)
				{
%>
				<td><input type="checkbox" name="select-segment[<%=i%>]" id="select-segment-<%=i%>" checked="checked" onclick="refreshSelected()" /></td>
				<td><a href="javascript:zoomToSegment(<%=i%>);"><%=htmlspecialchars(gui._("Zoom"))%></a></td>
<%
				}
%>
				<td><button disabled="disabled" id="pr-button-<%=i%>" onclick="if(this.osmroutemanager) addPRSegment(this.osmroutemanager.i, this.osmroutemanager.reversed);">+</button></td>
			</tr>
<%
			}
%>
		</tbody>
	</table>
	<h3><%=htmlspecialchars(gui._("Personal route"))%></h3>
	<ol id="personal-route"></ol>
	<ul class="buttons" id="personal-route-buttons" style="display:none;">
		<li><button id="personal-route-pop" onclick="PRPop()">&minus;</button></li>
		<li><button onclick="PRDownloadGPX()"><%=htmlspecialchars(gui._("Download GPX"))%></button></li>
	</ul>
</div>
<%
			if(render)
			{
%>
<div id="map"></div>
<%
			}
%>
<script type="text/javascript">
// <![CDATA[
<%
			if(render)
			{
%>
	var map = new FacilMap.Map("map");
	map.addAllAvailableLayers();

	map.addLayer(new OpenLayers.Layer.OpenStreetBugs("OpenStreetBugs", { visibility: false, shortName: "osb" }));

	window.onresize = function(){ document.getElementById("map").style.height = Math.round(window.innerHeight*.8)+"px"; map.updateSize(); };
	window.onresize();

	var styleMapNormal = new OpenLayers.StyleMap({strokeColor: "#0000ff", strokeWidth: 3, strokeOpacity: 0.5});
	var styleMapHighlight = new OpenLayers.StyleMap({strokeColor: "#ff0080", strokeWidth: 3, strokeOpacity: 0.5});
	var segments = [ ];
	var segments_highlighted = [ ];
	var segments_are_highlighted = [ ];
	var segments_data = [ ];
	var projection = new OpenLayers.Projection("EPSG:4326");
<%
				for(int i=0; i<route.segments.length; i++)
				{
%>
	segments[<%=i%>] = new OpenLayers.Layer.PointTrack(<%=jsescape(String.format(gui._("Segment %s"), i+1))%>, {
		styleMap: styleMapNormal,
		projection: new OpenLayers.Projection("EPSG:4326"),
		displayInLayerSwitcher: false
	});
	segments_data[<%=i%>] = [
<%
					LonLat[] nodes = route.segments[i].getNodes();
					for(int j=0; j<nodes.length; j++)
					{
%>
		new OpenLayers.Feature(segments[<%=i%>], new OpenLayers.LonLat(<%=nodes[j].getLon()%>, <%=nodes[j].getLat()%>).transform(projection, map.getProjectionObject())) <% if(j == nodes.length-1){%> // <% }%>,
<%
					}
%>
	];
	segments[<%=i%>].addNodes(segments_data[<%=i%>]);
<%
				}
%>
	map.addLayers(segments);

	var layerMarkers = new FacilMap.Layer.Markers.LonLat(<%=jsescape(gui._("Markers"))%>, { shortName : "m" });
	map.addLayer(layerMarkers);
	var clickControl = new FacilMap.Control.CreateMarker(layerMarkers);
	map.addControl(clickControl);
	clickControl.activate();

	var permalinkControl = new OpenLayers.Control.Permalink(null, "http://www.openstreetmap.org/");
	map.addControl(permalinkControl);
	permalinkControl.activate();

	var extent;
	if(segments.length > 0)
	{
		extent = segments[0].getDataExtent();
		for(var i=1; i<segments.length; i++)
			extent.extend(segments[i].getDataExtent());
	}
	if(extent)
		map.zoomToExtent(extent);
	else
		map.zoomToMaxExtent();

	var hashHandler = new FacilMap.Control.URLHashHandler();
	hashHandler.updateMapView = function() {
		var ret = FacilMap.Control.URLHashHandler.prototype.updateMapView.apply(this, arguments);
		for(var i=0; i<segments.length; i++)
			document.getElementById("select-segment-"+i).checked = segments[i].getVisibility();
		return ret;
	};
	map.addControl(hashHandler);
	hashHandler.activate();

<%
			}
%>
	var pr_allowed = [
		[
<%
			for(int i=0; i<route.segments.length; i++)
			{
%>
			[ <%=implode(", ", route.connection2[i])%> ]<% if(i == route.segments.length-1){%> // <% }%>,
<%
			}
%>
		],
		[
<%
			for(int i=0; i<route.segments.length; i++)
			{
%>
			[ <%=implode(", ", route.connection1[i])%> ]<% if(i == route.segments.length-1){%> // <% }%>,
<%
			}
%>
		]
	];

	var pr_stack = [ ];

	function addPRSegment(i, reversed)
	{
		pr_stack.push([i, reversed ? 1 : 0]);

		var li = document.createElement("li");
		li.id = "pr-stack-"+(pr_stack.length-1);
		li.onmouseover = function(){highlightSegment(i);};
		li.onmouseout = function(){unhighlightSegment(i);};
		li.appendChild(document.createTextNode((pr_stack.length >= 2 && pr_stack[pr_stack.length-2][0] == i) ? "<%=String.format(gui._("Segment %s (reversed)"), "\"+(i+1)+\"")%>" : "<%=String.format(gui._("Segment %s"), "\"+(i+1)+\"")%>"));
		document.getElementById("personal-route").appendChild(li);

		updatePRButtons();
	}

	function PRPop()
	{
		if(pr_stack.length > 0)
		{
			pr_stack.pop();
			document.getElementById("personal-route").removeChild(document.getElementById("pr-stack-"+(pr_stack.length)));
			updatePRButtons();
		}
	}

	function PRDownloadGPX()
	{
		var segments = [ ];

		for(var i=0; i<pr_stack.length; i++)
			segments[i] = (pr_stack[i][1] ? "-" : "+")+pr_stack[i][0];

		location.href = "gpx.jsp?relation=<%=urlencode(relationId.toString())%>&segments="+encodeURIComponent(segments.join(","));
	}

	function updatePRButtons()
	{
		document.getElementById("personal-route-buttons").style.display = pr_stack.length > 0 ? "" : "none";

		last = pr_stack.length > 0 ? pr_stack[pr_stack.length-1] : null;
		for(var i=0; i<pr_allowed[0].length; i++)
		{
			var allowed,reversed;
			if(last && i == last[0])
			{
				allowed = true;
				reversed = !last[1];
			}
			else if(!last || pr_allowed[last[1]][last[0]].length < 1)
			{
				if(pr_allowed[0][i].length < 1)
				{
					allowed = true;
					reversed = true;
				}
				else if(pr_allowed[1][i].length < 1)
				{
					allowed = true;
					reversed = false;
				}
				else
					allowed = false;
			}
			else
			{
				allowed = false;
				for(var j=0; j<pr_allowed[0][i].length; j++)
				{
					if(pr_allowed[0][i][j] == last[0])
					{
						allowed = true;
						reversed = true;
						break;
					}
				}
				if(!allowed)
				{
					for(var j=0; j<pr_allowed[1][i].length; j++)
					{
						if(pr_allowed[1][i][j] == last[0])
						{
							allowed = true;
							reversed = false;
							break;
						}
					}
				}
			}

			var button = document.getElementById("pr-button-"+i);
			if(!allowed)
			{
				button.disabled = true;
				button.osmroutemanager = null;
			}
			else
			{
				button.disabled = false;
				button.osmroutemanager = { i: i, reversed: reversed };
			}
		}
	}

	updatePRButtons();

<%
			if(render)
			{
%>
	function highlightSegment(i)
	{
		segments_are_highlighted[i] = true;
		if(!segments[i].getVisibility())
			return;

		if(!segments_highlighted[i])
		{
			segments_highlighted[i] = new OpenLayers.Layer.PointTrack(segments[i].name, {
				styleMap: styleMapHighlight,
				projection: new OpenLayers.Projection("EPSG:4326"),
				displayInLayerSwitcher: false,
				shortName: null
			});
			segments_highlighted[i].addNodes(segments_data[i]);
			segments_highlighted[i].setZIndex(100000);
			map.addLayer(segments_highlighted[i]);
		}

		segments[i].fmDefaultVisibility = false;
		segments[i].setVisibility(false);
		segments_highlighted[i].setVisibility(true);
		document.getElementById("tr-segment-"+i).className = "tr-segment-highlight";
	}

	function unhighlightSegment(i)
	{
		segments_are_highlighted[i] = false;
		document.getElementById("tr-segment-"+i).className = "tr-segment-normal";

		if(!segments_highlighted[i] || !segments_highlighted[i].getVisibility())
			return;
		segments_highlighted[i].setVisibility(false);
		segments[i].fmDefaultVisibility = true;
		segments[i].setVisibility(true);
	}

	function refreshSelected()
	{
		for(var i=0; i<segments.length; i++)
		{
			if(document.getElementById("select-segment-"+i).checked && (!segments_highlighted[i] || !segments_highlighted[i].getVisibility()) && !segments[i].getVisibility())
			{
				segments[i].setVisibility(true);
				if(segments_are_highlighted[i])
					highlightSegment(i);
			}
			else if(!document.getElementById("select-segment-"+i).checked)
			{
				segments[i].fmDefaultVisibility = true;
				segments[i].setVisibility(false);
				if(segments_highlighted[i])
					segments_highlighted[i].setVisibility(false);
			}
		}
	}

	refreshSelected();

	function zoomToSegment(i)
	{
		var extent = segments[i].getDataExtent();
		map.zoomToExtent(extent);
	}

	function zoomToGap(lonlat1, lonlat2)
	{
		var bbox = new OpenLayers.Bounds();
		bbox.extend(lonlat1);
		bbox.extend(lonlat2);
		map.zoomToExtent(bbox.transform(new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject()));
	}
<%
			}
%>
// ]]>
</script>
<%
		}
	}

	gui.foot();
%>
