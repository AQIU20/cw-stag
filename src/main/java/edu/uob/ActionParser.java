package edu.uob;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class ActionParser {

    public GameActionManager parseActions(File actionsFile)
            throws ParserConfigurationException, SAXException, IOException {
        GameActionManager actionManager = new GameActionManager();

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(actionsFile);
        Element root = document.getDocumentElement();
        NodeList actions = root.getChildNodes();

        // Process each action
        for (int i = 0; i < actions.getLength(); i++) {
            if (actions.item(i) instanceof Element) {
                Element actionElement = (Element) actions.item(i);
                GameAction action = parseAction(actionElement);
                actionManager.addAction(action);
            }
        }

        return actionManager;
    }

    private GameAction parseAction(Element actionElement) {
        GameAction action = new GameAction();

        // Parse triggers
        Element triggers = (Element) actionElement.getElementsByTagName("triggers").item(0);
        NodeList keyphrases = triggers.getElementsByTagName("keyphrase");
        for (int i = 0; i < keyphrases.getLength(); i++) {
            String keyphrase = keyphrases.item(i).getTextContent();
            action.addTrigger(keyphrase);
        }

        // Parse subjects
        Element subjects = (Element) actionElement.getElementsByTagName("subjects").item(0);
        if (subjects != null) {
            NodeList subjectEntities = subjects.getElementsByTagName("entity");
            for (int i = 0; i < subjectEntities.getLength(); i++) {
                String subject = subjectEntities.item(i).getTextContent();
                action.addSubject(subject);
            }
        }

        // Parse consumed entities
        Element consumed = (Element) actionElement.getElementsByTagName("consumed").item(0);
        if (consumed != null) {
            NodeList consumedEntities = consumed.getElementsByTagName("entity");
            for (int i = 0; i < consumedEntities.getLength(); i++) {
                String entity = consumedEntities.item(i).getTextContent();
                action.addConsumed(entity);
            }
        }

        // Parse produced entities
        Element produced = (Element) actionElement.getElementsByTagName("produced").item(0);
        if (produced != null) {
            NodeList producedEntities = produced.getElementsByTagName("entity");
            for (int i = 0; i < producedEntities.getLength(); i++) {
                String entity = producedEntities.item(i).getTextContent();
                action.addProduced(entity);
            }
        }

        // Parse narration
        Element narration = (Element) actionElement.getElementsByTagName("narration").item(0);
        if (narration != null) {
            action.setNarration(narration.getTextContent());
        }

        return action;
    }
}