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
<%@page import="eu.cdauth.osm.web.common.Cache"%>
<%@page import="eu.cdauth.osm.web.common.Queue"%>
<%@page import="java.util.*" %>
<%@page import="java.net.URL" %>
<%@page contentType="text/html; charset=UTF-8" buffer="none" session="false"%>
<%!
	protected static final API api = GUI.getAPI();
	private static Cache<ChangesetAnalyser> cache = null;
	private static final Queue queue = Queue.getInstance();

	public void jspInit()
	{
		if(cache == null)
			cache = new Cache<ChangesetAnalyser>(GUI.getCacheDirectory(getServletContext())+"/osmhv/changeset");
		GUI.servletStart();
	}

	public void jspDestroy()
	{
		GUI.servletStop();
	}

	private static final Queue.Worker worker = new Queue.Worker() {
		public void work(ID a_id)
		{
			try
			{
				ChangesetAnalyser route = new ChangesetAnalyser(api, a_id);
				cache.saveEntry(a_id.toString(), route);
			}
			catch(Exception e)
			{
			}
		}
	};
%>
<%
	if(request.getParameter("id") == null)
	{
		response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
		URL thisUrl = new URL(request.getRequestURL().toString());
		response.setHeader("Location", new URL(thisUrl.getProtocol(), thisUrl.getHost(), thisUrl.getPort(), request.getContextPath()).toString());
		return;
	}

	ID changesetID = new ID(request.getParameter("id").replace("^\\s*#?(.*)\\s*$", "$1"));

	GUI gui = new GUI(request, response);
	gui.setTitle(String.format(gui._("Changeset %s"), changesetID.toString()));
	gui.setJavaScripts(new String[]{
		"http://www.openlayers.org/api/OpenLayers.js",
		"http://maps.google.com/maps?file=api&v=2&key=ABQIAAAApZR0PIISH23foUX8nxj4LxQThUMYtIDw2iC2eUfTIWgH1b-HoBTSr3REfzI5VMINqd64RXBrAV696w",
		"http://api.maps.yahoo.com/ajaxymap?v=3.0&appid=osmhv",
		"http://api.facilmap.org/facilmap.js",
		"http://api.facilmap.org/osblayer/osblayer.js"
	});

	gui.head();
%>
<ul>
	<li><a href="./"><%=htmlspecialchars(gui._("Back to home page"))%></a></li>
	<li><a href="http://www.openstreetmap.org/browse/changeset/<%=htmlspecialchars(changesetID.toString())%>"><%=htmlspecialchars(gui._("Browse on OpenStreetMap"))%></a></li>
</ul>
<noscript><p><strong><%=htmlspecialchars(gui._("Note that many features of this page will not work without JavaScript."))%></strong></p></noscript>
<%
	response.getWriter().flush();

	Cache.Entry<ChangesetAnalyser> cacheEntry = cache.getEntry(changesetID.toString());
	int queuePosition = queue.getPosition(worker, changesetID);
	if(cacheEntry == null)
	{
		if(queuePosition == 0)
		{
			Queue.Notification notify = queue.scheduleTask(worker, changesetID);
			notify.sleep(20000);
			cacheEntry = cache.getEntry(changesetID.toString());
			queuePosition = queue.getPosition(worker, changesetID);
		}
	}

	if(cacheEntry != null)
	{
%>
<p><%=String.format(htmlspecialchars(gui._("This analysation was created on %s.")), gui.formatDate(cacheEntry == null ? null : cacheEntry.date))%></p>
<%
	}

	if(queuePosition > 0)
	{
%>
<p class="scheduled"><strong><%=htmlspecialchars(String.format(gui._("An analysation of this changeset is scheduled. The position in the queue is %d. Reload this page after a while to see the updated version."), queuePosition))%></strong></p>
<%
	}

	if(cacheEntry != null)
	{
		ChangesetAnalyser changes = cacheEntry.content;
%>
<p class="introduction"><strong><%=htmlspecialchars(gui._("Everything green on this page will show the status after the changeset was committed, red will be the status before, and things displayed in blue haven’t changed."))%></strong></p>
<h2><%=htmlspecialchars(gui._("Tags"))%></h2>
<dl>
<%
		for(Map.Entry<String,String> tag : changes.changeset.getTags().entrySet())
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
	<dd><%=htmlspecialchars(changes.changeset.getCreationDate().toString())%></dd>

	<dt><%=htmlspecialchars(gui._("Closing time"))%></dt>
<%
		Date closingDate = changes.changeset.getClosingDate();
		if(closingDate == null)
		{
%>
	<dd><%=htmlspecialchars(gui._("Still open"))%></dd>
<%
		}
		else
		{
%>
	<dd><%=htmlspecialchars(changes.changeset.getClosingDate().toString())%></dd>
<%
		}
%>

	<dt><%=htmlspecialchars(gui._("User"))%></dt>
	<dd><a href="http://www.openstreetmap.org/user/<%=htmlspecialchars(urlencode(changes.changeset.getUser().toString()))%>"><%=htmlspecialchars(changes.changeset.getUser().toString())%></a></dd>
</dl>
<h2><%=htmlspecialchars(gui._("Changed object tags"))%></h2>
<%
		if(changes.tagChanges.length == 0)
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
			for(ChangesetAnalyser.TagChange it : changes.tagChanges)
			{
				String type,browse;
				if(it.type == Node.class)
				{
					type = gui._("Node");
					browse = "node";
				}
				else if(it.type == Way.class)
				{
					type = gui._("Way");
					browse = "way";
				}
				else if(it.type == Relation.class)
				{
					type = gui._("Relation");
					browse = "relation";
				}
				else
					continue;
%>
	<li><%=htmlspecialchars(type+" "+it.id.toString())%> (<a href="http://www.openstreetmap.org/browse/<%=htmlspecialchars(browse+"/"+it.id.toString())%>"><%=htmlspecialchars(gui._("browse"))%></a>)
		<table>
			<tbody>
<%
				Set<String> tags = new HashSet<String>();
				tags.addAll(it.oldTags.keySet());
				tags.addAll(it.newTags.keySet());

				for(String key : tags)
				{
					String valueOld = it.oldTags.get(key);
					String valueNew = it.newTags.get(key);

					if(valueOld == null)
						valueOld = "";
					if(valueNew == null)
						valueNew = "";

					String class1,class2;
					if(valueOld.equals(valueNew))
					{
						class1 = "unchanged";
						class2 = "unchanged";
					}
					else
					{
						class1 = "old";
						class2 = "new";
					}
%>
				<tr>
					<th><%=htmlspecialchars(key)%></th>
					<td class="<%=htmlspecialchars(class1)%>"><%=GUI.formatTag(key, valueOld)%></td>
					<td class="<%=htmlspecialchars(class2)%>"><%=GUI.formatTag(key, valueNew)%></td>
				</tr>
<%
				}
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
%>
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
	var map = new FacilMap.Map("map");
	map.addAllAvailableLayers();

	window.onresize = function(){ document.getElementById("map").style.height = Math.round(window.innerHeight*.9)+"px"; map.updateSize(); };
	window.onresize();

	var styleMapUnchanged = new OpenLayers.StyleMap({strokeColor: "#0000ff", strokeWidth: 3, strokeOpacity: 0.3});
	var styleMapCreated = new OpenLayers.StyleMap({strokeColor: "#44ff44", strokeWidth: 3, strokeOpacity: 0.5});
	var styleMapRemoved = new OpenLayers.StyleMap({strokeColor: "#ff0000", strokeWidth: 3, strokeOpacity: 0.5});

	var osbLayer = new OpenLayers.Layer.OpenStreetBugs("OpenStreetBugs", { shortName: "osb", visibility: false });
	map.addLayer(osbLayer);
	osbLayer.setZIndex(500);

	var layerMarkers = new FacilMap.Layer.Markers.LonLat("Markers", { shortName: "m" });
	map.addLayer(layerMarkers);
	var clickControl = new FacilMap.Control.CreateMarker(layerMarkers);
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

	var hashHandler = new FacilMap.Control.URLHashHandler();
	map.addControl(hashHandler);
	hashHandler.activate();
// ]]>
</script>
<%
		}
	}

	gui.foot();
%>
