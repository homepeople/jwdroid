package com.jwdroid.export;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

public class XMLParser {
	
	public static List<Map<String,String>> parse(String elementName, InputStream is) throws ParserConfigurationException, SAXException, IOException {
		List<Map<String,String>> list = null;

        XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        XMLHandler saxHandler = new XMLHandler(elementName);
        xmlReader.setContentHandler(saxHandler);
        xmlReader.parse(new InputSource(is));
        list = saxHandler.getItems();
 
        return list;
	}

}
