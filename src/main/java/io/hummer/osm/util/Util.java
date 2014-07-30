package io.hummer.osm.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

	public static void write(File file, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(file);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			bw.write(content);
			bw.close();
			fos.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static List<String> readFile(String file) {
		List<String> result = new LinkedList<String>();
		try {
			BufferedReader r = new BufferedReader(
					new InputStreamReader(
					new FileInputStream(file)));
			String tmp = null;
			while((tmp = r.readLine()) != null) {
				result.add(tmp);
			}
			r.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	public static double toRadian(double degrees) {
		return degrees * Math.PI / 180.0;
	}

	public static double getVicinity(final int zoom) {
		MercatorProjection p = new MercatorProjection(zoom);
		return 360.0 / p.getTileCount() / 2.0;
	}

	/* XML handling */
	
	public static String toString(Object jaxbObject) {
		return toString(jaxbObject, false);
	}

	public static String toString(Object jaxbObject, boolean indent) {
		return toString(toElement(jaxbObject), indent);
	}

	public static String toString(Element element) {
		return toString(element, false);
	}
	public static String toString(Element element, boolean indent) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			Transformer tr = TransformerFactory.newInstance().newTransformer();
			tr.setOutputProperty(OutputKeys.METHOD, "xml");
			if(indent) {
				tr.setOutputProperty(
						"{http://xml.apache.org/xslt}indent-amount", "2");
				tr.setOutputProperty(OutputKeys.INDENT, "yes");
			} else {
				tr.setOutputProperty(OutputKeys.INDENT, "no");
			}
			tr.transform(new DOMSource(element), new StreamResult(baos));
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		String string = new String(baos.toByteArray());
		try {
			string = string
					.replaceAll(
							"<\\?xml version=\"1\\.0\" (encoding=\".*\")?( )*\\?>(\n)*",
							"");
		} catch (OutOfMemoryError e) {
			/* swallow */
		}
		return string;
	}
	public static Element toElement(Object jaxbObject) {
		if(jaxbObject == null)
			return null;
		if(jaxbObject instanceof Element)
			return (Element) jaxbObject;
		try {
			if(jaxbObject instanceof String)
				return toElement((String) jaxbObject);
			return toElement(jaxbObject,
					getJaxbContext(jaxbObject.getClass(), true));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Element toElement(Object jaxbObject, JAXBContext ctx)
			throws Exception {
		if(jaxbObject == null)
			return null;
		Element result = newDocument().createElement("result");
		try {
			ctx.createMarshaller().marshal(jaxbObject, result);
		} catch(Exception e) {
			throw e;
		}
		return (Element) result.getFirstChild();
	}

	private static final List<Class<?>> defaultJaxbContextClasses = new LinkedList<Class<?>>();
	private static JAXBContext defaultJaxbContext;

	private static JAXBContext getJaxbContext(Class<?> jaxbClass,
			boolean doCacheContext) {
		try {
			if(doCacheContext) {
				synchronized(defaultJaxbContextClasses) {
					if(!defaultJaxbContextClasses.contains(jaxbClass)) {
						defaultJaxbContextClasses.add(jaxbClass);
						defaultJaxbContext = JAXBContext
								.newInstance(defaultJaxbContextClasses
										.toArray(new Class[0]));
					}
				}
				return defaultJaxbContext;
			} else {
				return JAXBContext.newInstance(jaxbClass);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Document newDocument() throws Exception {
		DocumentBuilder builder = newDocumentBuilder();
		return builder.newDocument();
	}

	public static DocumentBuilder newDocumentBuilder()
			throws ParserConfigurationException {
		DocumentBuilderFactory factory = getDocBuilderFactory();
		DocumentBuilder temp = factory.newDocumentBuilder();
		return temp;
	}

	private static DocumentBuilderFactory getDocBuilderFactory() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		return factory;
	}

}
