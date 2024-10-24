package misa.tiled2misa;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This class is used for loading and parsing TMX files generated by the Tiled map editor.
 * It extracts map attributes, tilesets, layers, and object layers to create a TiledMap object.
 */
@SuppressWarnings("unused")
public class TiledParser
{
    private static final Logger LOGGER = Logger.getLogger(TiledParser.class.getName());

    /**
     * Load a TiledMap object from a TMX file path
     *
     * @param filePath the path for the TMX file
     * @return The parsed TiledMap object
     */
    public TiledMap loadFromTMX(String filePath)
    {
        try
        {
            File xmlFile = new File(filePath);
            Document document = parseXMLFile(xmlFile);
            return createTiledMapFromDocument(document);
        }
        catch (Exception e)
        {
            LOGGER.severe("Failed to load map from TMX: " + filePath);
            return null;
        }
    }

    /**
     * Load a TiledMap object from an InputStream (e.g., resource stream)
     *
     * @param inputStream the InputStream for the TMX file
     * @return The parsed TiledMap object
     */
    public TiledMap loadFromInputStream(InputStream inputStream)
    {
        try
        {
            Document document = parseXMLStream(inputStream);
            return createTiledMapFromDocument(document);
        }
        catch (Exception e)
        {
            LOGGER.severe("Failed to load map from InputStream.");
            return null;
        }
    }

    /**
     * Parses the XML file to create a Document object
     *
     * @param file The XML file
     * @return The parsed Document object
     * @throws Exception if parsing fails
     */
    private Document parseXMLFile(File file) throws Exception
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = dbFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(file);
        document.getDocumentElement().normalize();
        return document;
    }

    /**
     * Parses an XML InputStream to create a Document object
     *
     * @param inputStream The InputStream for the XML data
     * @return The parsed Document object
     * @throws Exception if parsing fails
     */
    private Document parseXMLStream(InputStream inputStream) throws Exception
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = dbFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(inputStream);
        document.getDocumentElement().normalize();
        return document;
    }

    /**
     * Core method to create a TiledMap from a parsed XML Document
     *
     * @param document The XML Document object
     * @return The TiledMap object created from the document
     */
    private TiledMap createTiledMapFromDocument(Document document)
    {
        // parse map attributes
        Element mapElement = document.getDocumentElement();
        int mapWidth = getIntAttribute(mapElement, "width");
        int mapHeight = getIntAttribute(mapElement, "height");
        int tileWidth = getIntAttribute(mapElement, "tilewidth");
        int tileHeight = getIntAttribute(mapElement, "tileheight");

        // parse tilesets, layers, and object layers
        List<TiledTileset> tilesets = parseTilesets(document);
        List<TiledLayer> layers = parseLayers(document);
        List<TiledObject> objects = parseObjectLayers(document);

        // use Builder to create the TiledMap
        return new TiledMap.Builder()
                .setWidth(mapWidth)
                .setHeight(mapHeight)
                .setTileWidth(tileWidth)
                .setTileHeight(tileHeight)
                .setLayers(layers)
                .setTilesets(tilesets)
                .setObjects(objects)
                .build();
    }

    /**
     * retrieves integer attributes from an XML element
     *
     * @param element The XML element
     * @param attribute The attribute name
     * @return The integer value of the attribute
     */
    private int getIntAttribute(Element element, String attribute)
    {
        return Integer.parseInt(element.getAttribute(attribute));
    }

    /**
     * parses tilesets from the TMX document
     *
     * @param document The TMX document
     * @return A list of TiledTileset objects
     */
    private List<TiledTileset> parseTilesets(Document document)
    {
        List<TiledTileset> tilesets = new ArrayList<>();
        NodeList tilesetNodes = document.getElementsByTagName("tileset");

        for (int i = 0; i < tilesetNodes.getLength(); i++)
        {
            Element tilesetElement = (Element) tilesetNodes.item(i);
            String source = tilesetElement.getAttribute("source");
            int firstGID = getIntAttribute(tilesetElement, "firstgid");
            tilesets.add(new TiledTileset(source, firstGID));

            LOGGER.info("Loaded tileset: " + source + " (firstGID: " + firstGID + ")");
        }
        return tilesets;
    }

    /**
     * parses tile layers from the TMX document
     *
     * @param document The TMX document
     * @return A list of TiledLayer objects
     */
    private List<TiledLayer> parseLayers(Document document)
    {
        List<TiledLayer> layers = new ArrayList<>();
        NodeList layerNodes = document.getElementsByTagName("layer");

        for (int i = 0; i < layerNodes.getLength(); i++)
        {
            Element layerElement = (Element) layerNodes.item(i);
            String layerName = layerElement.getAttribute("name");
            int layerWidth = getIntAttribute(layerElement, "width");
            int layerHeight = getIntAttribute(layerElement, "height");

            // parse the data based on encoding
            Element dataElement = (Element) layerElement.getElementsByTagName("data").item(0);
            String encoding = dataElement.getAttribute("encoding");
            int[][] tileData;

            if ("csv".equalsIgnoreCase(encoding))
            {
                tileData = decodeCSVTileData(dataElement.getTextContent().trim(),
                        layerWidth, layerHeight);
            }
            else if ("base64".equalsIgnoreCase(encoding))
            {
                tileData = decodeBase64TileData(dataElement.getTextContent().trim(),
                        layerWidth, layerHeight);
            }
            else
            {
                LOGGER.warning("Unsupported encoding: " + encoding);
                continue; // skip layer if the encoding is not supported
            }

            layers.add(new TiledLayer(layerName, layerWidth, layerHeight, tileData));

            LOGGER.info("Loaded layer: " + layerName +
                    " (" + layerWidth + "x" + layerHeight + ")");
        }
        return layers;
    }

    /**
     * decodes CSV tile data from the TMX document
     *
     * @param csvData The CSV data string
     * @param width The width of the layer
     * @param height The height of the layer
     * @return A 2D array of tile IDs
     */
    private int[][] decodeCSVTileData(String csvData, int width, int height)
    {
        int[][] tileData = new int[height][width];
        String[] tokens = csvData.split(",");

        for (int row = 0; row < height; row++)
        {
            for (int col = 0; col < width; col++)
            {
                int index = row * width + col;
                tileData[row][col] = Integer.parseInt(tokens[index].trim());
            }
        }

        LOGGER.info("Successfully decoded CSV tile data for layer (" + width + "x" + height + ")");
        return tileData;
    }

    /**
     * decodes Base64 tile data from the TMX document
     *
     * @param encodedData The Base64 encoded data string
     * @param width The width of the layer
     * @param height The height of the layer
     * @return A 2D array of tile IDs
     */
    private int[][] decodeBase64TileData(String encodedData, int width, int height)
    {
        byte[] decodedData = Base64.getDecoder().decode(encodedData);
        ByteBuffer buffer = ByteBuffer.wrap(decodedData);
        int[][] tileData = new int[height][width];

        for (int row = 0; row < height; row++)
        {
            for (int col = 0; col < width; col++)
            {
                int tileID = buffer.getInt();
                tileData[row][col] = tileID;
            }
        }

        LOGGER.info("Successfully decoded Base64 tile data for layer (" + width + "x" + height + ")");
        return tileData;
    }

    /**
     * parses object layers from the TMX document
     *
     * @param document The TMX document
     * @return A list of TiledObject objects
     */
    private List<TiledObject> parseObjectLayers(Document document)
    {
        List<TiledObject> objects = new ArrayList<>();
        NodeList objectGroupNodes = document.getElementsByTagName("objectgroup");

        for (int i = 0; i < objectGroupNodes.getLength(); i++)
        {
            Element objectGroupElement = (Element) objectGroupNodes.item(i);
            NodeList objectNodes = objectGroupElement.getElementsByTagName("object");

            for (int j = 0; j < objectNodes.getLength(); j++)
            {
                Element objectElement = (Element) objectNodes.item(j);
                TiledObject tiledObject = getObject(objectElement);
                objects.add(tiledObject);
            }

            LOGGER.info("Loaded object group with " + objectNodes.getLength() + " objects.");
        }
        return objects;
    }

    /**
     * parses a single TiledObject from an XML element
     *
     * @param objectElement The XML element representing the object
     * @return The parsed TiledObject
     */
    private static TiledObject getObject(Element objectElement)
    {
        int id = Integer.parseInt(objectElement.getAttribute("id"));
        String name = objectElement.getAttribute("name");
        String type = objectElement.getAttribute("type");
        double x = Double.parseDouble(objectElement.getAttribute("x"));
        double y = Double.parseDouble(objectElement.getAttribute("y"));
        double width = objectElement.hasAttribute("width") ?
                Double.parseDouble(objectElement.getAttribute("width")) : 0;
        double height = objectElement.hasAttribute("height") ?
                Double.parseDouble(objectElement.getAttribute("height")) : 0;

        // parse object properties
        Map<String, String> properties = new HashMap<>();
        NodeList propertyNodes = objectElement.getElementsByTagName("property");
        for (int p = 0; p < propertyNodes.getLength(); p++)
        {
            Element propertyElement = (Element) propertyNodes.item(p);
            String propName = propertyElement.getAttribute("name");
            String propValue = propertyElement.getAttribute("value");
            properties.put(propName, propValue);
        }

        LOGGER.info("Loaded object: " + name + " (Type: " + type + ", ID: " + id + ")");

        return new TiledObject(id, name, type, x, y, width, height, properties);
    }
}
