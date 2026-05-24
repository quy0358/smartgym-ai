package ntu.quy65132908.smartgym_ai.ui.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

public class AIAnalysisLayoutTest {

    @Test
    public void layout_usesCompactTitleStyle() throws Exception {
        Document document = readLayout();
        Element title = findByText(document, "@string/ai_title");

        assertEquals("@style/TextStyle.HeadlineMd", title.getAttribute("android:textAppearance"));
        assertEquals("0dp", title.getAttribute("android:layout_marginTop"));
    }

    @Test
    public void layout_embedsMetricCardsInsideBodyReportCard() throws Exception {
        Document document = readLayout();
        Element firstCard = firstElementByTag(document, "androidx.cardview.widget.CardView");

        assertTrue(containsAndroidId(firstCard, "@+id/stat_body_fat"));
        assertTrue(containsAndroidId(firstCard, "@+id/stat_lean_mass"));
    }

    private Document readLayout() throws Exception {
        File file = new File("src/main/res/layout/fragment_ai_analysis.xml");
        if (!file.exists()) {
            file = new File("app/src/main/res/layout/fragment_ai_analysis.xml");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        Document document = factory.newDocumentBuilder().parse(file);
        document.getDocumentElement().normalize();
        return document;
    }

    private Element findByText(Document document, String text) {
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (text.equals(element.getAttribute("android:text"))) {
                    return element;
                }
            }
        }
        throw new AssertionError("No element with android:text=" + text);
    }

    private Element firstElementByTag(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            throw new AssertionError("No element with tag " + tagName);
        }
        return (Element) nodes.item(0);
    }

    private boolean containsAndroidId(Element root, String id) {
        if (id.equals(root.getAttribute("android:id"))) {
            return true;
        }
        NodeList children = root.getElementsByTagName("*");
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element && id.equals(((Element) node).getAttribute("android:id"))) {
                return true;
            }
        }
        return false;
    }
}
