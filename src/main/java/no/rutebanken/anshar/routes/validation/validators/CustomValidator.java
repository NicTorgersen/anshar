/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package no.rutebanken.anshar.routes.validation.validators;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.helpers.ValidationEventImpl;
import javax.xml.bind.helpers.ValidationEventLocatorImpl;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public abstract class CustomValidator {

    public abstract String getXpath();
    public abstract ValidationEvent isValid(Node node);

    /**
     * General, default categoryname if implementing class does not override method
     * @return
     */
    public String getCategoryName() {
        return this.getClass().getSimpleName().replace("Validator", "");
    }

    protected boolean isValidNsrId(String prefix, String nodeValue) {
        return nodeValue != null && nodeValue.startsWith(prefix);
    }

    protected boolean isValidGenericId(String pattern, String nodeValue) {
        return nodeValue != null && nodeValue.contains(pattern);
    }

    /**
     * Returns the textual content of the provided node - null if it does not exist
     * @param node
     * @return
     */
    protected String getNodeValue(Node node) {
        if (node != null && node.getFirstChild() != null && node.getFirstChild().getNodeValue() != null) {
            return node.getFirstChild().getNodeValue();
        }
        return null;
    }

    /**
     * Returns the textual content of the provided node - null if it does not exist
     * @param node
     * @return
     */
    protected String getNodeAttributeValue(Node node, String attributeName) {
        if (node != null && node.getAttributes() != null) {
            final NamedNodeMap attributes = node.getAttributes();
            return getNodeValue(attributes.getNamedItem(attributeName));
        }
        return null;
    }

    protected String getChildNodeValue(Node node, String name) {
        return getNodeValue(getChildNodeByName(node, name));
    }

    protected Node getChildNodeByName(Node node, String name) {
        if (node != null) {
            final NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node n = childNodes.item(i);
                if (n.hasChildNodes()) {
                    for (int j = 0; j < n.getChildNodes().getLength(); j++) {
                        if (n.getNodeName().equals(name)) {
                            return n;
                        }
                    }
                }
            }
        }
        return null;
    }

    protected List<Node> getChildNodesByName(Node node, String name) {
        List<Node> nodes = new ArrayList<>();
        if (node != null) {
            final NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node n = childNodes.item(i);
                if (n.getNodeName().equals(name)) {
                    nodes.add(n);
                }
            }
        }
        return nodes;
    }


    protected String getSiblingNodeValue(Node node, String name) {
        return getNodeValue(getSiblingNodeByName(node, name));
    }

    private Node getSiblingNodeByName(Node node, String name) {
        final Node parentNode = node.getParentNode();
        if (parentNode != null) {
            final NodeList childNodes = parentNode.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node n = childNodes.item(i);
                if (n.getNodeName().equals(name)) {
                    return n;
                }
            }
        }
        return null;
    }

    protected List<Node> getSiblingNodesByName(Node node, String name) {
        final Node parentNode = node.getParentNode();
        List<Node> nodes = new ArrayList<>();
        if (parentNode != null) {
            final NodeList childNodes = parentNode.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node n = childNodes.item(i);
                if (n.getNodeName().equals(name)) {
                    nodes.add(n);
                }
            }
        }
        return nodes;
    }


    public ValidationEvent verifyNonExistingFields(Node node, String fieldName, String... invalidNodenames) {
        List <String> invalidFields = new ArrayList<>();
        for (String name : invalidNodenames) {
            if (getChildNodeByName(node, name) != null) {
                invalidFields.add(name);
            }
        }
        if (!invalidFields.isEmpty()) {
            return createInvalidFieldEvent(node, fieldName, invalidFields, ValidationEvent.WARNING);
        }
        return null;
    }

    public ValidationEvent verifyRequiredFields(Node node, String fieldName, String... requiredNodenames) {
        List <String> missingFields = new ArrayList<>();
        for (String name : requiredNodenames) {
            if (getChildNodeByName(node, name) == null) {
                missingFields.add(name);
            }
        }
        if (!missingFields.isEmpty()) {
            return createMissingFieldEvent(node, fieldName, missingFields, ValidationEvent.WARNING);
        }
        return null;
    }

    /**
     *
     * @param node Node that is validated
     * @param fieldname Name of attribute that fails validation
     * @param expectedValues Expected value or description of expected value
     * @param actualValue Actual value of node
     * @param severity
     * @return
     */
    protected ValidationEvent createEvent(Node node, String fieldname, Object expectedValues, String actualValue, int severity) {
        String message = MessageFormat.format("Value [{0}] is invalid for field [{1}], expected {2}", actualValue, fieldname, expectedValues);
        return new ValidationEventImpl(severity, message, new ValidationEventLocatorImpl(node));
    }

    /**
     *
     * @param node Node that is validated
     * @param fieldname Name of attribute that fails validation
     * @param missingFields Missing fields
     * @param severity
     * @return
     */
    protected ValidationEvent createMissingFieldEvent(Node node, String fieldname, List<String> missingFields, int severity) {
        ProfileValidationEventOrList eventList = new ProfileValidationEventOrList();
        for (String missingField : missingFields) {
            String message = MessageFormat.format("Missing required attribute: {0} ", missingField);
            eventList.addEvent(new ValidationEventImpl(severity, message, new ValidationEventLocatorImpl(node)));
        }
        return eventList;
    }

    /**
     *
     * @param node Node that is validated
     * @param fieldname Name of attribute that fails validation
     * @param missingFields Missing fields
     * @param severity
     * @return
     */
    private ValidationEvent createInvalidFieldEvent(Node node, String fieldname, List<String> missingFields, int severity) {
        ProfileValidationEventOrList eventList = new ProfileValidationEventOrList();
        for (String missingField : missingFields) {
            String message = MessageFormat.format("Invalid attribute: {0} ", missingField);
            eventList.addEvent(new ValidationEventImpl(severity, message, new ValidationEventLocatorImpl(node)));
        }
        return eventList;
    }

    /**
     *
     * @param node Node that is validated
     * @param message Validation-message
     * @param severity
     * @return
     */
    protected ValidationEvent createCustomFieldEvent(Node node, String message, int severity) {
        return new ValidationEventImpl(severity, message, new ValidationEventLocatorImpl(node));
    }
}
