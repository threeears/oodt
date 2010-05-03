/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gov.nasa.jpl.oodt.cas.filemgr.structs.mime;

// DOM imports
import org.w3c.dom.Text;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.InputSource;

// JDK imports
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/**
 * A reader for the mime-types DTD compliant XML files.
 * Based on original class written by J. Charron with the 
 * <a href="http://lucene.apache.org/nutch">Apache Nutch</a>project
 * 
 * @author ahart
 * @author bfoster
 */

final class MimeTypesReader {
    
    
    MimeTypesReader() {

    }

    MimeType[] read(String filepath) {
        URL repoUrl = null;
        InputStream is = null;
        
        try {
            repoUrl = new File(filepath).toURL();
            is = repoUrl.openStream();
            
        } catch (MalformedURLException e1) {
            return new MimeType[0];
        } catch (IOException e) {
            //TODO: print we're SOL here
            //TODO: add LOG class to this
        }
        
        return read(is);
       
    }
    
    MimeType[] read(InputStream stream) {
        MimeType[] types = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(stream));
            types = visit(document);
        } catch (Exception e) {
        		
        	//TODO: add message about error while loading mime types
            types = new MimeType[0];
        }
        return types;
    }
    
    /** Scan through the document. */
    private MimeType[] visit(Document document) {
        MimeType[] types = null;
        Element element = document.getDocumentElement();
        if ((element != null) && element.getTagName().equals("mime-types")) {
            types = readMimeTypes(element);
        }
        return (types == null) ? (new MimeType[0]) : types;
    }
    
    /** Read Element named mime-types. */
    private MimeType[] readMimeTypes(Element element) {
        ArrayList<MimeType> types = new ArrayList<MimeType>();
        NodeList nodes = element.getChildNodes();
        for (int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element nodeElement = (Element) node;
                if (nodeElement.getTagName().equals("mime-type")) {
                    MimeType type = readMimeType(nodeElement);
                    if (type != null) { types.add(type); }
                }
            }
        }
        return (MimeType[]) types.toArray(new MimeType[types.size()]);
    }
    
    /** Read Element named mime-type. */
    private MimeType readMimeType(Element element) {
        String name = null;
        String description = null;
        MimeType type = null;
        NamedNodeMap attrs = element.getAttributes();
        for (int i=0; i<attrs.getLength(); i++) {
            Attr attr = (Attr) attrs.item(i);
            if (attr.getName().equals("name")) {
                name = attr.getValue();
            } else if (attr.getName().equals("description")) {
                description = attr.getValue();
            }
        }
        if ((name == null) || (name.trim().equals(""))) {
            return null;
        }
        
        try {
            type = new MimeType(name);
        } catch (MimeTypeException mte) {
            // Mime Type not valid... just ignore it
        	// TODO: add message about ignoring mime type
            return null;
        }
        type.setDescription(description);
        
        NodeList nodes = element.getChildNodes();
        for (int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element nodeElement = (Element) node;
                if (nodeElement.getTagName().equals("ext")) {
                    readExt(nodeElement, type);
                } else if (nodeElement.getTagName().equals("magic")) {
                    readMagic(nodeElement, type);
                }
            }
        }
        return type;
    }
    
    /** Read Element named ext. */
    private void readExt(Element element, MimeType type) {
        NodeList nodes = element.getChildNodes();
        for (int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                type.addExtension(((Text) node).getData());
            }
        }
    }
    
    /** Read Element named magic. */
    private void readMagic(Element element, MimeType mimeType) {
        // element.getValue();
        String offset = null;
        String content = null;
        String type = null;
        NamedNodeMap attrs = element.getAttributes();
        for (int i=0; i<attrs.getLength(); i++) {
            Attr attr = (Attr) attrs.item(i);
            if (attr.getName().equals("offset")) {
                offset = attr.getValue();
            } else if (attr.getName().equals("type")) {
                type = attr.getValue();
            } else if (attr.getName().equals("value")) {
                content = attr.getValue();
            }
        }
        if ((offset != null) && (content != null)) {
            mimeType.addMagic(Integer.parseInt(offset), type, content);
        }
    }

}
