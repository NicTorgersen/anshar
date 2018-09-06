package no.rutebanken.anshar;

import com.google.api.client.util.IOUtils;
import io.protostuff.GraphIOUtil;
import io.protostuff.LinkedBuffer;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.junit.Test;
import org.xml.sax.SAXException;
import uk.org.siri.siri20.Siri;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;

import static org.assertj.core.api.Assertions.assertThat;

public class ProtostuffTest {

    @Test
    public void protoStuffTest() throws IOException, JAXBException, XMLStreamException, SAXException {

        // Seems like I have to use GraphIOUtil instead of ProtostuffIOUtil to avoid stack overflow exception with SIRI

        // System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");
        // System.setProperty("protostuff.runtime.morph_non_final_pojos", "true");

        Siri siri = unmarshallSiriFile("src/test/resources/siriAfterBaneNorSiriEtRewriting.xml");


        long schemaStarted = System.currentTimeMillis();
        Schema<Siri> schema = RuntimeSchema.getSchema(Siri.class);
        System.out.println("Schema created in " + (System.currentTimeMillis() - schemaStarted) + " ms");

        long bufferStarted = System.currentTimeMillis();

        LinkedBuffer buffer = LinkedBuffer.allocate(512);

        byte[] protostuff = GraphIOUtil.toByteArray(siri, schema, buffer);

        buffer.clear();
        System.out.println("Written to protostuff in  " + (System.currentTimeMillis() - bufferStarted) + " ms");

        System.out.println("The byte array length is " + protostuff.length);
        long beforeWrite = System.currentTimeMillis();
        IOUtils.copy(new ByteArrayInputStream(protostuff), new FileOutputStream("protostuff.file"));
        System.out.println("Wrote protostuff file to disk in " + (System.currentTimeMillis() - beforeWrite) + " ms");

        long serializeBack = System.currentTimeMillis();

        Siri siriWasProtostuffed = schema.newMessage();
        GraphIOUtil.mergeFrom(protostuff, siriWasProtostuffed, schema);

        System.out.println("Deserialized from protobuf in  " + (System.currentTimeMillis() - serializeBack) + " ms");

        compareXmlStrings(siriWasProtostuffed, siri);
    }

    private void compareXmlStrings(Siri actual, Siri expected) throws JAXBException, IOException, SAXException {
        String actualXml = xmlify(actual);
        String expectedXml = xmlify(expected);

        assertThat(actualXml).isEqualTo(expectedXml);
    }

    private String xmlify(Siri siri) throws JAXBException, IOException, SAXException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Siri.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        jaxbMarshaller.marshal(siri, byteArrayOutputStream);
        return byteArrayOutputStream.toString();
    }


    static Siri unmarshallSiriFile(String filename) throws JAXBException, XMLStreamException, FileNotFoundException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Siri.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        XMLInputFactory xmlif = XMLInputFactory.newInstance();
        FileInputStream xml = new FileInputStream(filename);
        XMLStreamReader xmlsr = xmlif.createXMLStreamReader(xml);
        return (Siri) jaxbUnmarshaller.unmarshal(xmlsr);
    }
}
