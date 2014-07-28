package io.hummer.osm.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.w3c.dom.Document;

public class Util {

	public static Document readCompressedXmlFile(File file) {
		try {
			InputStream stream = new FileInputStream(file);

			DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
			InputStream unzipStream = new GzipCompressorInputStream(stream);
			return docBuilder.parse(unzipStream);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static double toRadian(double degrees) {
		return degrees * Math.PI / 180.0;
	}

}
