package com.cisco.xmp.netconf;

import java.io.IOException;
import java.util.Iterator;

import com.tailf.jnc.Device;
import com.tailf.jnc.DeviceUser;
import com.tailf.jnc.Element;
import com.tailf.jnc.JNCException;
import com.tailf.jnc.NetconfSession;
import com.tailf.jnc.NodeSet;
import com.tailf.jnc.XMLParser;

public class NcClient implements AutoCloseable {

    private static final String SESSION_NAME = "cfg";
    protected String hostname;
    protected String port;
    private Device device = null;

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage is: NcClient <hostname> <port> <username> <password>");
            System.exit(1);
        }

        try (NcClient nc = new NcClient(args[0], args[1])) {
            nc.connect(args[2], args[3]);
            String config = nc.getConfig(DataStore.running, "");
            System.out.println(config);
        } catch (Throwable t) {
            System.err.println(t.toString());
            System.exit(1);
        }
    }

    protected NcClient(String host, String port) {
        this.hostname = host;
        this.port = port;
    }

    public void connect(String username, String password) throws ConnectionException {

        try {
            int portNum = Integer.parseInt(port);

            DeviceUser user = new DeviceUser(username, username, password);
            device = new Device(hostname, user, hostname, portNum);
            device.connect(username);
            device.newSession(SESSION_NAME);
        } catch (IOException io) {
            throw new ConnectionException(io.getMessage(), io);
        } catch (JNCException jex) {
            throw new ConnectionException(jex.toString(), jex);
        } catch (NumberFormatException nfe) {
            throw new ConnectionException("Failed to read port number", nfe);
        }
    }

    public void disconnect() {
        if (device != null) {
            device.close();
            device = null;
        }
    }

    public boolean isConnected() {
        return device != null;
    }

    public enum DataStore {
        running, startup, candidate
    }

    public String getConfig(DataStore datastore, String filter) throws NcException {

        try {
            int source = datastore.ordinal();
            if (device != null) {
                NetconfSession session = device.getSession(SESSION_NAME);
                if (session != null) {
                    NodeSet nodes = session.getConfig(source, filter);
                    return toXMLDocument(nodes);
                }
            }
        } catch (Throwable e) {
            throw new NcException(e.toString(), e);
        }

        return "";
    }

    public String get(String filter) {
        try {
            if (device != null) {
                NetconfSession session = device.getSession(SESSION_NAME);
                if (session != null) {
                    NodeSet nodes = session.get(filter);
                    return toXMLDocument(nodes);
                }
            }
        } catch (Throwable e) {
            return e.getClass().getName() + e.getMessage();
        }

        return "";
    }

    protected String toXMLDocument(NodeSet nodes) {
        if (nodes.size() == 1) {
            return nodes.toXMLString();
        } else {
            Element root = new Element("http://tail-f.com/ns/rest", "collection");
            Iterator<Element> iterator = nodes.iterator();
            while (iterator.hasNext()) {
                Element e = iterator.next();
                root.addChild(e);
            }
            return root.toXMLString();
        }
    }

    public void lock(DataStore datastore) throws IOException, JNCException {
        int source = datastore.ordinal();
        if (device != null) {
            NetconfSession session = device.getSession(SESSION_NAME);
            if (session != null) {
                session.lock(source);
            }
        }
    }

    public void unlock(DataStore datastore) throws IOException, JNCException {
        int source = datastore.ordinal();
        if (device != null) {
            NetconfSession session = device.getSession(SESSION_NAME);
            if (session != null) {
                session.unlock(source);
            }
        }
    }

    public void commit() throws IOException, JNCException {
        if (device != null) {
            NetconfSession session = device.getSession(SESSION_NAME);
            if (session != null) {
                session.commit();
            }
        }
    }

    public void discardChanges() throws IOException, JNCException {
        if (device != null) {
            NetconfSession session = device.getSession(SESSION_NAME);
            if (session != null) {
                session.discardChanges();
            }
        }
    }

    public void copyConfig(String config, DataStore datastore) throws JNCException, IOException {
        int target = datastore.ordinal();
        if (device != null) {
            NetconfSession session = device.getSession(SESSION_NAME);
            if (session != null) {
                XMLParser parser = new XMLParser();
                Element sourceTree = parser.parse(config);
                session.copyConfig(sourceTree, target);
            }
        }
    }

    public void editConfig(String config, DataStore datastore) throws JNCException, IOException {
        int target = datastore.ordinal();
        if (device != null) {
            NetconfSession session = device.getSession(SESSION_NAME);
            if (session != null) {
                XMLParser parser = new XMLParser();
                Element configTree = parser.parse(config);
                session.editConfig(target, configTree);
            }
        }
    }

    @Override
    public void close() {
        disconnect();
    }
}
