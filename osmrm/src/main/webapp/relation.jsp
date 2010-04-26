<%--
    This file is part of OSM Route Manager.

    OSM Route Manager is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OSM Route Manager is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with OSM Route Manager.  If not, see <http://www.gnu.org/licenses/>.
--%>
<%@page import="de.cdauth.osm.lib.*"%>
<%@page import="de.cdauth.osm.osmrm.*"%>
<%@page import="static de.cdauth.osm.osmrm.GUI.*"%>
<%@page import="java.util.*" %>
<%@ page import="java.util.regex.Pattern" %>
<%@page contentType="text/html; charset=UTF-8" buffer="none"%>
<%!
	private static final API api = GUI.getAPI();
%>
<%
	if(request.getParameter("id") == null)
	{
		response.sendError(response.SC_MOVED_PERMANENTLY, ".");
		return;
	}

	GUI gui = new GUI(request, response);

	ID relationId = new ID(request.getParameter("id").trim());
	gui.setTitle(String.format(gui._("Relation %s"), relationId.toString()));

	boolean render = (request.getParameter("norender") == null);
	if(!render)
		gui.setBodyClass("norender");
	else
		gui.setJavaScripts(new String[] {
			"http://www.openlayers.org/api/OpenLayers.js",
			"http://maps.google.com/maps?file=api&v=2&key=ABQIAAAApZR0PIISH23foUX8nxj4LxQe8fls808ouw55mfsb9VLPMfxZSBRAxMJl1CWns7WN__O20IuUSiDKng",
			"http://api.maps.yahoo.com/ajaxymap?v=3.0&appid=cdauths-map",
			"http://osm.cdauth.de/map/prototypes.js",
			"http://osm.cdauth.de/map/openstreetbugs.js"
		});

	gui.head();

	Relation relation = api.getRelationFactory().fetch(relationId); // request.getParameter("refresh") != null
	RelationSegment[] segments = RouteManager.segmentate(relation);

	double totalLength = 0;
	List<Integer>[] connection1 = new List[segments.length];
	List<Integer>[] connection2 = new List[segments.length];
	double[] distance1 = new double[segments.length];
	double[] distance2 = new double[segments.length];

	for(int i=0; i<segments.length; i++)
	{
		connection1[i] = new ArrayList<Integer>();
		connection2[i] = new ArrayList<Integer>();
		distance1[i] = Double.MAX_VALUE;
		distance2[i] = Double.MAX_VALUE;
	}

	// Calculate segment connections and lengthes
	for(int i=0; i<segments.length; i++)
	{
		totalLength += segments[i].getDistance();

		LonLat thisEnd1 = segments[i].getEnd1();
		LonLat thisEnd2 = segments[i].getEnd2();
		for(int j=i+1; j<segments.length; j++)
		{
			LonLat thatEnd1 = segments[j].getEnd1();
			LonLat thatEnd2 = segments[j].getEnd2();

			if(thisEnd1.equals(thatEnd1))
			{
				connection1[i].add(j);
				connection1[j].add(i);
			}
			if(thisEnd1.equals(thatEnd2))
			{
				connection1[i].add(j);
				connection2[j].add(i);
			}
			if(thisEnd2.equals(thatEnd1))
			{
				connection2[i].add(j);
				connection1[j].add(i);
			}
			if(thisEnd2.equals(thatEnd2))
			{
				connection2[i].add(j);
				connection2[j].add(i);
			}

			double it_distance11 = thisEnd1.getDistance(thatEnd1);
			double it_distance12 = thisEnd1.getDistance(thatEnd2);
			double it_distance21 = thisEnd2.getDistance(thatEnd1);
			double it_distance22 = thisEnd2.getDistance(thatEnd2);

			distance1[i] = Math.min(distance1[i], Math.min(it_distance11, it_distance12));
			distance2[i] = Math.min(distance2[i], Math.min(it_distance21, it_distance21));
			distance1[j] = Math.min(distance1[j], Math.min(it_distance11, it_distance21));
			distance2[j] = Math.min(distance2[j], Math.min(it_distance12, it_distance22));
		}
	}
%>
<ul>
	<li><a href="./"><%=htmlspecialchars(gui._("Back to home page"))%></a></li>
	<li><a href="http://betaplace.emaitie.de/webapps.relation-analyzer/analyze.jsp?relationId=<%=htmlspecialchars(urlencode(relationId.toString()))%>"><%=htmlspecialchars(gui._("Open this in OSM Relation Analyzer"))%></a></li>
	<li><a href="http://www.openstreetmap.org/browse/relation/<%=htmlspecialchars(urlencode(relationId.toString()))%>"><%=htmlspecialchars(gui._("Browse on OpenStreetMap"))%></a></li>
</ul>
<noscript><p><strong><%=htmlspecialchars(gui._("Note that many features of this page will not work without JavaScript."))%></strong></p></noscript>
<%--<p><%=String.format(htmlspecialchars(gui._("The data was last refreshed on %s. The timestamp of the relation is %s. If you think one of the members might have been changed, %sreload the data manually%s.")), gmdate("Y-m-d\\TH:i:s\\Z", $segments[0]), $relation->getDOM()->getAttribute("timestamp"), "<a href=\"?id="+htmlspecialchars(urlencode(relationId.toString())+"&refresh=1")."\">", "</a>")%></p>--%>
<h2><%=htmlspecialchars(gui._("Tags"))%></h2>
<dl>
<%
	Map<String,String> tags = relation.getTags();
	for(Map.Entry<String,String> tag : tags.entrySet())
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
	User user = api.getChangesetFactory().fetch(relation.getChangeset()).getUser();
%>
<h2><%=htmlspecialchars(gui._("Details"))%></h2>
<dl>
	<dt><%=htmlspecialchars(gui._("Last changed"))%></dt>
	<dd><%=String.format(gui._("%s by %s"), relation.getTimestamp().toString(), "<a href=\"http://www.openstreetmap.org/user/"+urlencode(user.getName())+"\">"+htmlspecialchars(user.getName())+"</a>")%></dd>

	<dt><%=htmlspecialchars(gui._("Total length"))%></dt>
	<dd><%=gui.formatNumber(totalLength, 2)%>&thinsp;km</dd>

	<dt><%=htmlspecialchars(gui._("Sub-relations"))%></dt>
	<dd><ul>
<%
	RelationMember[] members = relation.getMembers();
	HashSet<ID> subRelationIDs = new HashSet<ID>();
	for(RelationMember member : members)
	{
		if(member.getType() == Relation.class)
			subRelationIDs.add(member.getReferenceID());
	}
	Map<ID,Relation> subRelations = api.getRelationFactory().fetch(subRelationIDs.toArray(new ID[subRelationIDs.size()]));

	for(Map.Entry<ID,Relation> subRelation : subRelations.entrySet())
	{
%>
		<li><a href="?id=<%=htmlspecialchars(urlencode(subRelation.getKey().toString()))%>"><%=htmlspecialchars(subRelation.getKey().toString())%> (<%=htmlspecialchars(subRelation.getValue().getTag("name"))%>)</a></li>
<%
	}
%>
	</ul></dd>

	<dt><%=htmlspecialchars(gui._("Parent relations"))%></dt>
	<dd><ul>
<%
	Relation[] parentRelations = relation.getContainingRelations();
	for(Relation parentRelation : parentRelations)
	{
%>
		<li><a href="?id=<%=htmlspecialchars(urlencode(parentRelation.getID().toString()))%>"><%=htmlspecialchars(parentRelation.getID().toString())%> (<%=htmlspecialchars(parentRelation.getTag("name"))%>)</a></li>
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
	for(int i=0; i<segments.length; i++)
	{
%>
			<tr<% if(render){%> onmouseover="highlightSegment(<%=i%>);" onmouseout="unhighlightSegment(<%=i%>);"<% }%> id="tr-segment-<%=i%>" class="tr-segment-normal">
				<td><%=htmlspecialchars(""+(i+1))%></td>
				<td><%=gui.formatNumber(segments[i].getDistance(), 2)%>&thinsp;km</td>
				<td><% if(segments.length > 1){%><%=gui.formatNumber(distance1[i], 2)%>&thinsp;km, <%=gui.formatNumber(distance2[i], 2)%>&thinsp;km<% }%></td>
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
	var map = new OpenLayers.Map.cdauth("map");
	map.addAllAvailableLayers();

	map.addLayer(new OpenLayers.Layer.OpenStreetBugs("OpenStreetBugs", { visibility: false, shortName: "osb" }));

	window.onresize = function(){ document.getElementById("map").style.height = Math.round(window.innerHeight*.8)+"px"; map.updateSize(); }
	window.onresize();

	var styleMapNormal = new OpenLayers.StyleMap({strokeColor: "#0000ff", strokeWidth: 3, strokeOpacity: 0.5});
	var styleMapHighlight = new OpenLayers.StyleMap({strokeColor: "#ff0080", strokeWidth: 3, strokeOpacity: 0.5});
	var segments = [ ];
	var segments_highlighted = [ ];
	var segments_are_highlighted = [ ];
	var segments_data = [ ];
	var projection = new OpenLayers.Projection("EPSG:4326");
<%
		for(int i=0; i<segments.length; i++)
		{
%>
	segments[<%=i%>] = new OpenLayers.Layer.PointTrack(<%=jsescape(String.format(gui._("Segment %s"), i+1))%>, {
		styleMap: styleMapNormal,
		projection: new OpenLayers.Projection("EPSG:4326"),
		displayInLayerSwitcher: false
	});
	segments_data[<%=i%>] = [
<%
			LonLat[] nodes = segments[i].getNodes();
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

	var layerMarkers = new OpenLayers.Layer.cdauth.Markers.LonLat(<%=jsescape(gui._("Markers"))%>, { shortName : "m" });
	map.addLayer(layerMarkers);
	var clickControl = new OpenLayers.Control.cdauth.CreateMarker(layerMarkers);
	map.addControl(clickControl);
	clickControl.activate();

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

	var hashHandler = new OpenLayers.Control.cdauth.URLHashHandler();
	hashHandler.updateMapView = function() {
		var ret = OpenLayers.Control.cdauth.URLHashHandler.prototype.updateMapView.apply(this, arguments);
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
	for(int i=0; i<segments.length; i++)
	{
%>
			[ <%=implode(", ", connection2[i])%> ]<% if(i == segments.length-1){%> // <% }%>,
<%
	}
%>
		],
		[
<%
	for(int i=0; i<segments.length; i++)
	{
%>
			[ <%=implode(", ", connection1[i])%> ]<% if(i == segments.length-1){%> // <% }%>,
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
		li.onmouseover = function(){highlightSegment(i);}
		li.onmouseout = function(){unhighlightSegment(i);}
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

		segments[i].cdauthDefaultVisibility = false;
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
		segments[i].cdauthDefaultVisibility = true;
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
				segments[i].cdauthDefaultVisibility = true;
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
<%
	}
%>
// ]]>
</script>
<%
	gui.foot();
%>
