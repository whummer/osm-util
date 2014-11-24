package io.hummer.osm.export;

import io.hummer.osm.model.Point.PathPoint;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Export traces to GPX format.
 * @author Waldemar Hummer
 */
public class PathExporter {

	public static String exportGPX(List<PathPoint> list) {
		StringBuilder out = new StringBuilder();
		out.append("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\">");
		out.append("<trk>");
		out.append("<trkseg>\n");
		DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
		DateFormat df2 = new SimpleDateFormat("hh:mm:ss");
		for(PathPoint n : list) {
			out.append("<trkpt lat=\"" + n.y + "\" lon=\"" + n.x + "\" >");
			out.append("<ele>0</ele>"); // TODO
			Date date = new Date((long)(n.time * 1000));
			out.append("<time>" + df1.format(date) + "T" + df2.format(date) + "Z</time>");
			out.append("</trkpt>\n");
		}
		out.append("</trkseg>");
		out.append("</trk>");
		out.append("</gpx>");
		return out.toString();
	}

	public static String exportKML(List<PathPoint> list) {
		StringBuilder out = new StringBuilder();
		out.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">");
		out.append("<Document>");
		DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
		DateFormat df2 = new SimpleDateFormat("hh:mm:ss");
		for(PathPoint n : list) {
			Date date = new Date((long)(n.time * 1000));
			String time = df1.format(date) + "T" + df2.format(date);
			out.append("<Placemark>");
			out.append("<name>" + n.x + "," + n.y + " (" + time + ")</name>");
			out.append("<TimeStamp>");
			out.append("<when>" + time + "Z</when>");
			out.append("</TimeStamp>");
			out.append("<Point>");
			out.append("<coordinates>" + n.x + "," + n.y + ",0</coordinates>");
			out.append("</Point>");
			out.append("</Placemark>\n");
		}

		out.append("<Placemark>");
		out.append("<name>Path</name>");
		out.append("<LineString>");
		out.append("<tessellate>1</tessellate>");
		out.append("<coordinates>");
		for(PathPoint n : list) {
			out.append(n.x + "," + n.y + ",0.0 ");
		}
		out.append("</coordinates>");
		out.append("</LineString>");
		out.append("</Placemark>");

		out.append("</Document>");
		out.append("</kml>");
		return out.toString();
	}

}
