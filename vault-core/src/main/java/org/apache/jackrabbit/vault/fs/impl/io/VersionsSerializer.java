package org.apache.jackrabbit.vault.fs.impl.io;

import org.apache.jackrabbit.vault.fs.api.Aggregate;
import org.apache.jackrabbit.vault.fs.api.SerializationType;
import org.apache.jackrabbit.vault.fs.impl.AggregateImpl;
import org.apache.jackrabbit.vault.fs.io.Serializer;
import org.apache.jackrabbit.vault.util.xml.serialize.OutputFormat;
import org.apache.jackrabbit.vault.util.xml.serialize.XMLSerializer;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import java.io.IOException;
import java.io.OutputStream;

import static org.apache.jackrabbit.commons.NamespaceHelper.JCR;
import static org.apache.jackrabbit.commons.NamespaceHelper.MIX;
import static org.apache.jackrabbit.commons.NamespaceHelper.NT;
import static org.apache.jackrabbit.vault.fs.api.SerializationType.XML_GENERIC;
import static org.apache.jackrabbit.vault.util.Constants.VAULT_NS_URI;

public class VersionsSerializer implements Serializer {

    private final AggregateImpl aggregate;

    public VersionsSerializer(Aggregate aggregate) {
        this.aggregate = (AggregateImpl) aggregate;
    }

    @Override
    public void writeContent(OutputStream out) throws IOException, RepositoryException {
        final Session session = aggregate.getNode().getSession();
        final Workspace workspace = session.getWorkspace();
        final VersionManager versionManager = workspace.getVersionManager();
        final VersionHistory versionHistory = versionManager.getVersionHistory(aggregate.getNode().getPath());

        OutputFormat oFmt = new OutputFormat("xml", "UTF-8", true);
        oFmt.setIndent(4);
        oFmt.setLineWidth(0);
        oFmt.setBreakEachAttribute(true);
        XMLSerializer ser = new XMLSerializer(out, oFmt);

        try {
            ser.startDocument();
            ser.startPrefixMapping("jcr", JCR);
            ser.endPrefixMapping("jcr");
            ser.startPrefixMapping("mix", MIX);
            ser.endPrefixMapping("mix");
            ser.startPrefixMapping("nt", NT);
            ser.endPrefixMapping("nt");
            ser.startPrefixMapping("vlt", VAULT_NS_URI);
            ser.endPrefixMapping("vlt");

            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute(VAULT_NS_URI, "path", "", "CDATA", aggregate.getPath());
            ser.startElement(VAULT_NS_URI, "versions", null, attrs);

            writeNode(ser, versionHistory);

            ser.endElement("versions");
            ser.endDocument();
        } catch (SAXException e) {
            throw new IOException(e);
        }

    }

    private void writeNode(XMLSerializer ser, Node node) throws RepositoryException, SAXException {
        final PropertyIterator properties = node.getProperties();
        AttributesImpl pattrs = new AttributesImpl();
        while (properties.hasNext()) {
            final Property property = properties.nextProperty();
            if (property.isMultiple()) {
                final Value[] values = property.getValues();
                StringBuilder result = new StringBuilder();
                for (Value value : values) {
                    result.append(value.getString());
                    result.append(",");
                }
                String mpvalue = "[" + (result.length() > 0 ? result.substring(0, result.length() - 1) : "") + "]";
                pattrs.addAttribute("", property.getName(), "", "CDATA", mpvalue);
            } else {
                pattrs.addAttribute("", property.getName(), "", "CDATA", property.getString());
            }
        }
        pattrs.addAttribute(VAULT_NS_URI, "nodename", "", "CDATA", node.getName());
        ser.startElement("", node.getPrimaryNodeType().getName(), null, pattrs);
        final NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            writeNode(ser, nodes.nextNode());
        }
        ser.endElement(node.getName());
    }

    @Override
    public SerializationType getType() {
        return XML_GENERIC;
    }
}
