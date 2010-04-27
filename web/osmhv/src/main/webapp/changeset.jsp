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
<%@ page import="java.net.URL" %>
<%@ page import="java.net.URLEncoder" %>
<%@page contentType="text/html; charset=UTF-8" buffer="none" session="false"%>
<%!
	protected static final API api = GUI.getAPI();
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

	ID changesetID = new ID(request.getParameter("id").replace("^\\s*#?(.*)\\s*$", "$1"));

	if(changesetID != null)
		gui.setTitle(String.format(gui._("Changeset %s"), changesetID.toString()));
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
	<li><a href="http://www.openstreetmap.org/browse/changeset/<%=htmlspecialchars(changesetID.toString())%>"><%=htmlspecialchars(gui._("Browse on OpenStreetMap"))%></a></li>
</ul>
<noscript><p><strong><%=htmlspecialchars(gui._("Note that many features of this page will not work without JavaScript."))%></strong></p></noscript>
<%--<p><%=String.format(htmlspecialchars(gui._("This analysation was created on %s.")), gmdate("Y-m-d\\TH:i:s\\Z", $information["analysed"]))%></p>--%>
<p class="introduction"><strong><%=htmlspecialchars(gui._("Everything green on this page will show the status after the changeset was committed, red will be the status before, and things displayed in blue haven’t changed."))%></strong></p>
<%
	response.getWriter().flush();
	Changeset changeset = api.getChangesetFactory().fetch(changesetID);
%>
<h2><%=htmlspecialchars(gui._("Tags"))%></h2>
<dl>
<%
	for(Map.Entry<String,String> tag : changeset.getTags().entrySet())
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
<h2><%=htmlspecialchars(gui._("Details"))%></h2>
<dl>
	<dt><%=htmlspecialchars(gui._("Creation time"))%></dt>
	<dd><%=htmlspecialchars(changeset.getCreationDate().toString())%></dd>

	<dt><%=htmlspecialchars(gui._("Closing time"))%></dt>
	<dd><%=htmlspecialchars(changeset.getClosingDate().toString())%></dd>

	<dt><%=htmlspecialchars(gui._("User"))%></dt>
	<dd><a href="http://www.openstreetmap.org/user/<%=htmlspecialchars(URLEncoder.encode(changeset.getUser().toString(), "UTF-8"))%>"><%=htmlspecialchars(changeset.getUser().toString())%></a></dd>
</dl>
<%
	response.getWriter().flush();
	HistoryViewer.Changes changes = HistoryViewer.getNodeChanges(api, changeset);
%>
<%--<h2><%=htmlspecialchars(gui._("Changed object tags"))%></h2>
<%
	if(!$sql->query("SELECT COUNT(*) FROM changeset_tags_objects WHERE changeset = ".$sql->quote($_GET["id"]).";")->fetchColumn())
	{
%>
<p class="nothing-to-do"><%=htmlspecialchars(gui._("No tags have been changed."))%></p>
<%
	}
	else
	{
%>
<p class="changed-object-tags-note"><%=htmlspecialchars(gui._("Hover the elements to view the changed tags."))%></p>
<ul class="changed-object-tags">
<%
		$old_type = null;
		$old_id = null;
		$tags = $sql->query("SELECT * FROM changeset_tags_objects WHERE changeset = ".$sql->quote($_GET["id"]).";");
		while($line = $tags->fetch())
		{
			if($line["type"] != $old_type || $line["id"] != $old_id)
			{
				if($old_type !== null)
				{
%>
			</tbody>
		</table>
	</li>
<%
				}

				switch($line["type"])
				{
					case 1:
						$type = _("Node");
						$browse = "node";
						break;
					case 2:
						$type = _("Way");
						$browse = "way";
						break;
					case 3:
						$type = _("Relation");
						$browse = "relation";
						break;
				}
%>
	<li><%=htmlspecialchars($type." ".$line["id"])%> (<a href="http://www.openstreetmap.org/browse/<%=htmlspecialchars($browse."/".$line["id"])%>"><%=htmlspecialchars(gui._("browse"))%></a>)
		<table>
			<tbody>
<%
				$old_type = $line["type"];
				$old_id = $line["id"];
			}

			if($line["value1"] == $line["value2"])
			{
				$class1 = "unchanged";
				$class2 = "unchanged";
			}
			else
			{
				$class1 = "old";
				$class2 = "new";
			}

			$values = array();
			foreach(array($line["value1"], $line["value2"]) as $i=>$v)
			{
				if(trim($v) != "")
				{
					if(preg_match("/^url(:|\$)/i", $line["tagname"]))
					{
						$v = explode(";", $v);
						foreach($v as $k=>$v1)
							$v[$k] = "<a href=\"".htmlspecialchars(trim($v1))."\">".htmlspecialchars($v1)."</a>";
						$v = implode(";", $v);
					}
					elseif(preg_match("/^wiki(:.*)?\$/i", $line["tagname"], $m))
					{
						$m[1] = strtolower($m[1]);
						$v = explode(";", $v);
						foreach($v as $k=>$v1)
							$v[$k] = "<a href=\"http://wiki.openstreetmap.org/wiki/".htmlspecialchars(rawurlencode(($m[1] == ":symbol" ? "Image:" : "").$v1))."\">".htmlspecialchars($v1)."</a>";
						$v = implode(";", $v);
					}
					else
						$v = htmlspecialchars($v);
				}
				$values[$i] = $v;
			}
%>
				<tr>
					<th><%=htmlspecialchars($line["tagname"])%></th>
					<td class="<%=htmlspecialchars($class1)%>"><%=$values[0]%></td>
					<td class="<%=htmlspecialchars($class2)%>"><%=$values[1]%></td>
				</tr>
<%
		}

		if($old_type !== null)
		{
%>
			</tbody>
		</table>
	</li>
<%
		}
%>
</ul>
<%
	}
%>--%>
<h2><%=htmlspecialchars(gui._("Map"))%></h2>
<%
	if(changes.removed.length == 0 && changes.created.length == 0 && changes.unchanged.length == 0)
	{
%>
<p class="nothing-to-do"><%=htmlspecialchars(gui._("No objects were changed in the changeset."))%></p>
<%
	}
	else
	{
%>
<div id="map"></div>
<script type="text/javascript">
// <![CDATA[
	var map = new OpenLayers.Map.cdauth("map");
	map.addAllAvailableLayers();

	window.onresize = function(){ document.getElementById("map").style.height = Math.round(window.innerHeight*.9)+"px"; map.updateSize(); }
	window.onresize();

	var styleMapUnchanged = new OpenLayers.StyleMap({strokeColor: "#0000ff", strokeWidth: 3, strokeOpacity: 0.3});
	var styleMapCreated = new OpenLayers.StyleMap({strokeColor: "#44ff44", strokeWidth: 3, strokeOpacity: 0.5});
	var styleMapRemoved = new OpenLayers.StyleMap({strokeColor: "#ff0000", strokeWidth: 3, strokeOpacity: 0.5});

	var osbLayer = new OpenLayers.Layer.OpenStreetBugs("OpenStreetBugs", { shortName: "osb", visibility: false });
	map.addLayer(osbLayer);
	osbLayer.setZIndex(500);

	var layerMarkers = new OpenLayers.Layer.cdauth.Markers.LonLat("Markers", { shortName: "m" });
	map.addLayer(layerMarkers);
	var clickControl = new OpenLayers.Control.cdauth.CreateMarker(layerMarkers);
	map.addControl(clickControl);
	clickControl.activate();

	var projection = new OpenLayers.Projection("EPSG:4326");
	var layerCreated = new OpenLayers.Layer.PointTrack("(Created)", {
		styleMap: styleMapCreated,
		projection: projection,
		zoomableInLayerSwitcher: true,
		shortName: "created"
	});
	var layerRemoved = new OpenLayers.Layer.PointTrack("(Removed)", {
		styleMap: styleMapRemoved,
		projection: projection,
		zoomableInLayerSwitcher: true,
		shortName: "removed"
	});
	var layerUnchanged = new OpenLayers.Layer.PointTrack("(Unchanged)", {
		styleMap: styleMapUnchanged,
		projection: projection,
		zoomableInLayerSwitcher: true,
		shortName: "unchanged"
	});
<%
		for(Segment segment : changes.removed)
		{
%>
	layerRemoved.addNodes([new OpenLayers.Feature(layerRemoved, new OpenLayers.LonLat(<%=segment.getNode1().getLonLat().getLon()%>, <%=segment.getNode1().getLonLat().getLat()%>).transform(projection, map.getProjectionObject())),new OpenLayers.Feature(layerRemoved, new OpenLayers.LonLat(<%=segment.getNode2().getLonLat().getLon()%>, <%=segment.getNode2().getLonLat().getLat()%>).transform(projection, map.getProjectionObject()))]);
<%
		}

		for(Segment segment : changes.created)
		{
%>
	layerCreated.addNodes([new OpenLayers.Feature(layerRemoved, new OpenLayers.LonLat(<%=segment.getNode1().getLonLat().getLon()%>, <%=segment.getNode1().getLonLat().getLat()%>).transform(projection, map.getProjectionObject())),new OpenLayers.Feature(layerRemoved, new OpenLayers.LonLat(<%=segment.getNode2().getLonLat().getLon()%>, <%=segment.getNode2().getLonLat().getLat()%>).transform(projection, map.getProjectionObject()))]);
<%
		}

		for(Segment segment : changes.unchanged)
		{
%>
	layerUnchanged.addNodes([new OpenLayers.Feature(layerRemoved, new OpenLayers.LonLat(<%=segment.getNode1().getLonLat().getLon()%>, <%=segment.getNode1().getLonLat().getLat()%>).transform(projection, map.getProjectionObject())),new OpenLayers.Feature(layerRemoved, new OpenLayers.LonLat(<%=segment.getNode2().getLonLat().getLon()%>, <%=segment.getNode2().getLonLat().getLat()%>).transform(projection, map.getProjectionObject()))]);
<%
		}
%>

	map.addLayer(layerUnchanged);
	map.addLayer(layerRemoved);
	map.addLayer(layerCreated);

	var extent1 = layerCreated.getDataExtent();
	var extent2 = layerRemoved.getDataExtent();
	var extent3 = layerUnchanged.getDataExtent();

	var extent = extent1;
	if(extent)
	{
		extent.extend(extent2);
		extent.extend(extent3);
	}
	else
	{
		extent = extent2;
		if(extent)
			extent.extend(extent3);
		else
			extent = extent3;
	}

	if(extent)
		map.zoomToExtent(extent);
	else
		map.zoomToMaxExtent();

	var hashHandler = new OpenLayers.Control.cdauth.URLHashHandler();
	map.addControl(hashHandler);
	hashHandler.activate();
// ]]>
</script>
<%
	}

	gui.foot();
%>
