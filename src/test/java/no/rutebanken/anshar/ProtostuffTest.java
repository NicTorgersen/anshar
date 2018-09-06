package no.rutebanken.anshar;

import com.google.api.client.util.IOUtils;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.junit.Test;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.xml.sax.SAXException;
import uk.org.siri.siri20.Siri;

import javax.xml.bind.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;

import static org.assertj.core.api.Assertions.assertThat;

public class ProtostuffTest {

    @Test
    public void protoStuffTest() throws IOException, JAXBException, XMLStreamException, SAXException {

//        System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");

        Siri siri = unmarshallSiriFile("src/test/resources/siriAfterBaneNorSiriEtRewriting.xml");

        SiriWrapper siriWrapper = new SiriWrapper();
        siriWrapper.siri = siri;

        long schemaStarted = System.currentTimeMillis();
        Schema<SiriWrapper> schema = RuntimeSchema.getSchema(SiriWrapper.class);
        System.out.println("Schema created in "+ (System.currentTimeMillis()-schemaStarted) + " ms");

        long bufferStarted = System.currentTimeMillis();

        LinkedBuffer buffer = LinkedBuffer.allocate(512);

        final byte[] protostuff;
        try {
            protostuff = ProtostuffIOUtil.toByteArray(siriWrapper, schema, buffer);
        } finally {
            buffer.clear();
        }
        System.out.println("Written to protostuff in  "+ (System.currentTimeMillis()-bufferStarted) + " ms");

        System.out.println("The byte array length is " + protostuff.length);
        long beforeWrite = System.currentTimeMillis();
        IOUtils.copy(new ByteArrayInputStream(protostuff), new FileOutputStream("protostuff.file"));
        System.out.println("Wrote protostuff file to disk in " + (System.currentTimeMillis()-beforeWrite) + " ms");

        long serializeBack = System.currentTimeMillis();
        SiriWrapper siriWasProtostuffed = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(protostuff, siriWasProtostuffed, schema);

        System.out.println("Deserialized from protobuf in  "+ (System.currentTimeMillis()-serializeBack) + " ms");

        compareXmlStrings(siriWasProtostuffed.siri, siri);
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
