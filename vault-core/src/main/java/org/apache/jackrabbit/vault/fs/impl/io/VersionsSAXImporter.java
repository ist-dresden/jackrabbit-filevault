package org.apache.jackrabbit.vault.fs.impl.io;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.commons.codec.binary.Base64;
import org.apache.jackrabbit.oak.api.Blob;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.identifier.IdentifierManager;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.vault.util.DocViewProperty;
import org.apache.jackrabbit.vault.util.RejectingEntityDefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.version.VersionManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.apache.jackrabbit.JcrConstants.*;
import static org.apache.jackrabbit.commons.NamespaceHelper.NT;
import static org.apache.jackrabbit.oak.plugins.version.VersionConstants.REP_VERSIONSTORAGE;
import static org.apache.jackrabbit.vault.fs.impl.io.DocViewSAXImporter.EMPTY_ATTRIBUTES;
import static org.apache.jackrabbit.vault.util.Constants.VAULT_NS_URI;

public class VersionsSAXImporter extends RejectingEntityDefaultHandler implements NamespaceResolver {

    /**
     * the importing session
     */
    private final Session session;
    private final Node parentNode;

    private NodeBuilder currentVersionHistoryNode;
    private NodeBuilder currentVersionNode;
    private NodeBuilder currentParent;

    private boolean isInVersion;

    public VersionsSAXImporter(Node parentNode) throws RepositoryException {
        this.parentNode = parentNode;
        this.session = parentNode.getSession();
    }

    /**
     * {@inheritDoc}
     */
    public String getURI(String prefix) throws NamespaceException {
        try {
            return session.getNamespaceURI(prefix);
        } catch (RepositoryException e) {
            throw new NamespaceException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefix(String uri) throws NamespaceException {
        try {
            return session.getNamespacePrefix(uri);
        } catch (RepositoryException e) {
            throw new NamespaceException(e);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (uri.equals(VAULT_NS_URI) && localName.equals("versions")) {
            final String path = attributes.getValue("", "path");
            System.out.println(path);
        }
        if (uri.equals(NT) && localName.equals("versionHistory")) {
            final String nodename = attributes.getValue(VAULT_NS_URI, "nodename");
            try {
                final NodeBuilder jcrVersionStorage = getVersionStorage();
                final NodeBuilder n1nb = buildIntermediateNode(jcrVersionStorage, nodename.substring(0, 2));
                final NodeBuilder n2nb = buildIntermediateNode(n1nb, nodename.substring(2, 4));
                final NodeBuilder n3nb = buildIntermediateNode(n2nb, nodename.substring(4, 6));
                currentVersionHistoryNode = createVersionHistoryNode(n3nb, /*nodename*/ "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeee");
//                if (currentVersionHistoryNode!=null) {
//                    createRootVersion(currentVersionHistoryNode);
//                }
            } catch (Exception e) {
                throw new SAXException(e);
            }
            isInVersion = false;

        }
        if (uri.equals(NT) && localName.equals("versionLabels")) {
            if (currentVersionHistoryNode!=null) {
                final NodeBuilder versionLabel = createVersionLabel(currentVersionHistoryNode);
                for (int i = 0; i<attributes.getLength();i++) {
                    final String attributeQName = attributes.getQName(i);
                    if (!attributeQName.equals("vlt:nodename") && !attributeQName.equals("jcr:primaryType")) {
                        versionLabel.setProperty(attributeQName, attributes.getValue(i).substring(11), Type.REFERENCE);
                    }
                }
            }
            isInVersion = false;
        }
        if (uri.equals(NT) && localName.equals("version")) {
            final String nodename = attributes.getValue(VAULT_NS_URI, "nodename");
            final String uuid = "a"+attributes.getValue(JCR_UUID).substring(1); //FIXME
            final String created = attributes.getValue(JCR_CREATED).substring(6);
            String predecessors = attributes.getValue(JCR_PREDECESSORS).substring(11);
            String successors = attributes.getValue(JCR_SUCCESSORS).substring(11);
            predecessors = predecessors.substring(1, predecessors.length()-1);
            successors = successors.substring(1, successors.length()-1);
            if (currentVersionHistoryNode!=null) {
                currentVersionNode = createVersion(currentVersionHistoryNode, nodename, uuid, created, predecessors.split(","), successors.split(","));
            }
            isInVersion = true;
            currentParent = currentVersionNode;
        }
        if (uri.equals(NT) && localName.equals(("frozenNode"))) {
            final String nodename = attributes.getValue(VAULT_NS_URI, "nodename");
            if (currentParent != null) {
                currentParent = createNode(currentParent, nodename, attributes);
            }
        }
    }

    private NodeBuilder createNode(NodeBuilder parent, String nodename, Attributes attributes) {
        final NodeBuilder node = parent.child(nodename);
        for (int x = 0; x < attributes.getLength(); x++) {
            final String qName = attributes.getQName(x);
            final String value = attributes.getValue(x);
            if (qName.equals(JCR_UUID)) {
                final String uuid = "abc"+attributes.getValue(JCR_UUID).substring(3); //FIXME
                node.setProperty(JCR_UUID, uuid, Type.STRING);
            } else if (qName.equals(JCR_PRIMARYTYPE)) {
                final String jcrPrimaryType = attributes.getValue(JCR_PRIMARYTYPE);
                node.setProperty(JCR_PRIMARYTYPE, jcrPrimaryType, Type.NAME);
            } else if (qName.equals(JCR_FROZENUUID)) {
                node.setProperty(JCR_FROZENUUID, attributes.getValue(JCR_FROZENUUID), Type.STRING);
            } else if (qName.equals(JCR_FROZENPRIMARYTYPE)) {
                node.setProperty(JCR_FROZENPRIMARYTYPE, attributes.getValue(JCR_FROZENPRIMARYTYPE).substring(6), Type.NAME);
            } else if (qName.equals(JCR_FROZENMIXINTYPES)) {
                if (value != null) {
                    String mixins = value.substring(6);
                    mixins = mixins.substring(1, mixins.length()-1);
                    node.setProperty(JCR_FROZENMIXINTYPES, Arrays.asList(mixins.split(",")), Type.NAMES);
                }
            } else if (qName.equals("vlt:nodename")) {
                // ignore
            } else {
                final DocViewProperty docViewProperty = DocViewProperty.parse(qName, value);
                final Type<?> type1 = Type.fromTag(docViewProperty.type, docViewProperty.isMulti);
                final String[] values = docViewProperty.values;
                if (type1!=Type.UNDEFINED && type1!=Type.UNDEFINEDS) {
                    switch (type1.tag()) {
                        case PropertyType.LONG:
                            if (docViewProperty.isMulti) {
                                final List<Long> longs = Lists.transform(Arrays.asList(values), new Function<String, Long>() {
                                    @Override
                                    public Long apply(String input) {
                                        return Long.valueOf(input);
                                    }
                                });
                                node.setProperty(qName, longs, Type.LONGS);
                            } else {
                                node.setProperty(qName, Long.parseLong(values[0]), Type.LONG);
                            }
                            break;
                        case PropertyType.BOOLEAN:
                            if (docViewProperty.isMulti) {
                                final List<Boolean> booleans = Lists.transform(Arrays.asList(values), new Function<String, Boolean>() {
                                    @Override
                                    public Boolean apply(String input) {
                                        return Boolean.valueOf(input);
                                    }
                                });
                                node.setProperty(qName, booleans, Type.BOOLEANS);
                            } else {
                                node.setProperty(qName, Boolean.parseBoolean(values[0]), Type.BOOLEAN);
                            }
                            break;
                        case PropertyType.DOUBLE:
                            if (docViewProperty.isMulti) {
                                final List<Double> doubles = Lists.transform(Arrays.asList(values), new Function<String, Double>() {
                                    @Override
                                    public Double apply(String input) {
                                        return Double.valueOf(input);
                                    }
                                });
                                node.setProperty(qName, doubles, Type.DOUBLES);
                            } else {
                                node.setProperty(qName, Double.parseDouble(values[0]), Type.DOUBLE);
                            }
                            break;
                        case PropertyType.DATE:
                            if (docViewProperty.isMulti) {
                                final List<String> dates = Arrays.asList(values);
                                node.setProperty(qName, dates, Type.DATES);
                            } else {
                                node.setProperty(qName, values[0], Type.DATE);
                            }
                            break;
                        case PropertyType.DECIMAL:
                            if (docViewProperty.isMulti) {
                                final List<BigDecimal> decimals = Lists.transform(Arrays.asList(values), new Function<String, BigDecimal>() {
                                    @Override
                                    public BigDecimal apply(String input) {
                                        return new BigDecimal(input);
                                    }
                                });
                                node.setProperty(qName, decimals, Type.DECIMALS);
                            } else {
                                node.setProperty(qName, new BigDecimal(values[0]), Type.DECIMAL);
                            }
                            break;
                        case PropertyType.NAME:
                            if (docViewProperty.isMulti) {
                                final List<String> names = Arrays.asList(values);
                                node.setProperty(qName, names, Type.NAMES);
                            } else {
                                node.setProperty(qName, values[0], Type.NAME);
                            }
                            break;
                        case PropertyType.PATH:
                            if (docViewProperty.isMulti) {
                                final List<String> paths = Arrays.asList(values);
                                node.setProperty(qName, paths, Type.PATHS);
                            } else {
                                node.setProperty(qName, values[0], Type.PATH);
                            }
                            break;
                        case PropertyType.URI:
                            if (docViewProperty.isMulti) {
                                final List<String> uris = Arrays.asList(values);
                                node.setProperty(qName, uris, Type.URIS);
                            } else {
                                node.setProperty(qName, values[0], Type.URI);
                            }
                            break;
                        case PropertyType.REFERENCE:
                            if (docViewProperty.isMulti) {
                                final List<String> refs = Arrays.asList(values);
                                node.setProperty(qName, refs, Type.REFERENCES);
                            } else {
                                node.setProperty(qName, values[0], Type.REFERENCE);
                            }
                            break;
                        case PropertyType.WEAKREFERENCE:
                            if (docViewProperty.isMulti) {
                                final List<String> refs = Arrays.asList(values);
                                node.setProperty(qName, refs, Type.WEAKREFERENCES);
                            } else {
                                node.setProperty(qName, values[0], Type.WEAKREFERENCE);
                            }
                            break;
                        case PropertyType.BINARY:
                            class Base64BasedBlob implements Blob {

                                private final byte[] decode;

                                private Base64BasedBlob(String base64) {
                                    final Base64 b = new Base64();
                                    decode = b.decode(base64.getBytes());
                                }

                                @Override
                                public InputStream getNewStream() {
                                    return new ByteArrayInputStream(decode);
                                }

                                @Override
                                public long length() {
                                    return decode.length;
                                }

                                @Override
                                public String getReference() {
                                    return null;
                                }

                                @Override
                                public String getContentIdentity() {
                                    return null;
                                }
                            }
                            Blob blob = new Base64BasedBlob(values[0]);
                            node.setProperty(qName, blob, Type.BINARY);
                    }
                } else {
                    //String
                    if (docViewProperty.isMulti) {
                        node.setProperty(qName, Arrays.asList(values), Type.STRINGS);
                    } else {
                        node.setProperty(qName, value, Type.STRING);
                    }
                }
            }

        }
        System.out.println();

        return node;
    }

    private NodeBuilder buildIntermediateNode(NodeBuilder parent, String name) {
        final NodeBuilder child = parent.child(name);
        if (child.isNew()) {
            child.setProperty(JCR_PRIMARYTYPE, REP_VERSIONSTORAGE, Type.NAME);
        }
        return child;
    }

    private NodeBuilder createVersionHistoryNode(NodeBuilder n3nb, String versionableUuid) {
        final NodeBuilder versionHistoryNode = n3nb.child(versionableUuid);

//        versionHistoryNode.remove();
//        return null;
//
        versionHistoryNode.setProperty(JCR_PRIMARYTYPE, NT_VERSIONHISTORY, Type.NAME);
        versionHistoryNode.setProperty("jcr:versionableUuid", versionableUuid, Type.STRING);
        versionHistoryNode.setProperty(JCR_UUID, IdentifierManager.generateUUID(), Type.STRING);
        return versionHistoryNode;
    }

    private NodeBuilder createVersion(NodeBuilder versionHistoryNode, String versionName, String uuid, String created, String[] predecessors, String[] successors) {
        final NodeBuilder versionNode = versionHistoryNode.child(versionName);
        versionNode.setProperty(JCR_PRIMARYTYPE, NT_VERSION, Type.NAME);
        versionNode.setProperty(JCR_UUID, uuid, Type.STRING);
        versionNode.setProperty(JCR_CREATED, created, Type.DATE);
        versionNode.setProperty(JCR_PREDECESSORS, Arrays.asList(predecessors), Type.REFERENCES);
        versionNode.setProperty(JCR_SUCCESSORS, Arrays.asList(successors), Type.REFERENCES);
//        versionNode.setProperty(JCR_CREATED, ISO8601.format(Calendar.getInstance()), Type.DATE);
        return versionNode;
    }

    private NodeBuilder createVersionLabel(NodeBuilder versionHistoryNode) {
        final NodeBuilder versionLabelsNode = versionHistoryNode.child(JCR_VERSIONLABELS);
        versionLabelsNode.setProperty(JCR_PRIMARYTYPE, NT_VERSIONLABELS, Type.NAME);
        return versionLabelsNode;
    }

    private Object callMethod(Object object, String name, String parameter) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Method method = object.getClass().getMethod(name, String.class);
        final Object o = method.invoke(object, parameter);
        return o;
    }

    private NodeBuilder getVersionStorage() throws RepositoryException, NoSuchFieldException, IllegalAccessException {
        final Workspace workspace = session.getWorkspace();
        final VersionManager versionManager = workspace.getVersionManager();
        final Object versionManagerDelegate = getFieldValue(versionManager, "versionManagerDelegate");
        final Object readWriteVersionManager = getFieldValue(versionManagerDelegate, "versionManager");
        final Object versionStorage = getFieldValue(readWriteVersionManager, "versionStorage");
        //org.apache.jackrabbit.oak.core.MutableRoot
        final Root root = (Root) getFieldValue(versionStorage, "root");
        //org.apache.jackrabbit.oak.plugins.segment.SegmentNodeBuilder
        final NodeBuilder builder = (NodeBuilder) getFieldValue(root, "builder");
        final NodeBuilder jcrSystem = builder.child("jcr:system");
        return jcrSystem.child("jcr:versionStorage");
    }

    private Object getFieldValue(Object object, String name) throws NoSuchFieldException, IllegalAccessException {
        final Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private void createProperty(ContentHandler handler, String name, String value, String type) throws SAXException {
        AttributesImpl attrs;
        attrs = new AttributesImpl();
        attrs.addAttribute(Name.NS_SV_URI, "name", "sv:name", "CDATA", name);
        attrs.addAttribute(Name.NS_SV_URI, "type", "sv:type", "CDATA", type);
        handler.startElement(Name.NS_SV_URI, "property", "sv:property", attrs);
        handler.startElement(Name.NS_SV_URI, "value", "sv:value", EMPTY_ATTRIBUTES);
        handler.characters(value.toCharArray(), 0, value.length());
        handler.endElement(Name.NS_SV_URI, "value", "sv:value");
        handler.endElement(Name.NS_SV_URI, "property", "sv:property");
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    }

    @Override
    public void endDocument() throws SAXException {
        try {
            final Object sessionDelegate = getFieldValue(session, "sd");
            callMethod(sessionDelegate, "setUserData", "import");
            session.save();
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }
}

//                try {
//                    final Field sessionContextField = session.getClass().getDeclaredField("sessionContext");
//                    sessionContextField.setAccessible(true);
//                    final Object o = sessionContextField.get(session);
//                    final Method getProtectedItemImporters = o.getClass().getMethod("getProtectedItemImporters");
//                    final List<ProtectedItemImporter> invoke = (List<ProtectedItemImporter>) getProtectedItemImporters.invoke(o);
//                    System.out.println(invoke);
//                } catch (NoSuchFieldException e) {
//                    e.printStackTrace();
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                } catch (NoSuchMethodException e) {
//                    e.printStackTrace();
//                } catch (InvocationTargetException e) {
//                    e.printStackTrace();
//                }

//                final Node versionStorageNode = session.getNode("/jcr:system/jcr:versionStorage/");
//                final Node n1;
//                if (versionStorageNode.hasNode(nodename.substring(0,2))) {
//                    n1 = versionStorageNode.getNode(nodename.substring(0, 2));
//                } else {
//                    n1 = versionStorageNode.addNode(nodename.substring(0, 2), "rep:versionStorage");
//                }
//                final Node n2;
//                if (n1.hasNode(nodename.substring(2,4))) {
//                    n2 = n1.getNode(nodename.substring(2, 4));
//                } else {
//                    n2 = n1.addNode(nodename.substring(2, 4), "rep:versionStorage");
//                }
//                final Node n3;
//                if (n2.hasNode(nodename.substring(4,6))) {
//                    n3 = n2.getNode(nodename.substring(4, 6));
//                } else {
//                    n3 = n2.addNode(nodename.substring(4, 6), "rep:versionStorage");
//                }



//                try {
//                    final Field dlgField = n3.getClass().getSuperclass().getDeclaredField("dlg");
//                    dlgField.setAccessible(true);
//                    final Object dlg = dlgField.get(n3);
//                    System.out.println(dlg);
//                    final Field treeField = dlg.getClass().getDeclaredField("tree");
//                    treeField.setAccessible(true);
//                    final Tree tree = (Tree) treeField.get(dlg);
//                    final Tree abc = tree.addChild("abc");
//                    session.save();
//                    System.out.println(tree);
//                } catch (NoSuchFieldException e) {
//                    e.printStackTrace();
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                }




//                final ContentHandler handler = session.getImportContentHandler(
//                        n3.getPath(),
//                        ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
//
//                final String uuid = attributes.getValue(JCR, "uuid");
//
//                String[] prefixes = session.getNamespacePrefixes();
//                handler.startDocument();
//                for (String prefix: prefixes) {
//                    handler.startPrefixMapping(prefix, session.getNamespaceURI(prefix));
//                }
//                AttributesImpl attrs = new AttributesImpl();
//                attrs.addAttribute(Name.NS_SV_URI, "name", "sv:name", "CDATA", nodename+"XXX");
//                handler.startElement(Name.NS_SV_URI, "node", "sv:node", attrs);
//
//
//                createProperty(handler, "jcr:primaryType", "nt:versionHistory", PropertyType.TYPENAME_NAME);
//                createProperty(handler, "jcr:uuid", uuid+"XXX", PropertyType.TYPENAME_STRING);
//                // ...
//
//                handler.endElement(Name.NS_SV_URI, "node", "sv:node");
//                handler.endDocument();

//                final Node versionHistoryNode = n3.addNode(nodename, "nt:versionHistory");
//                versionHistoryNode.setProperty("jcr:uuid", uuid);
