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

import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

public class NcClient implements AutoCloseable {

    private static final String SESSION_NAME = "cfg";
    private Device device = null;

    protected String hostname;
    protected String port;
    protected DataStore datastore = DataStore.running;

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage is: NcClient <hostname> <port> <username> <password>");
            System.exit(1);
        }

        try (NcClient nc = new NcClient(args[0], args[1])) {
            nc.connect(args[2], args[3]);
            nc.interactive();
            // String config = nc.getConfig();
            // System.out.println(config);
        } catch (Throwable t) {
            System.err.println(t.toString());
            System.exit(1);
        }
    }

    protected NcClient(String host, String port) {
        this.hostname = host;
        this.port = port;
    }

    public void interactive() throws NcException, IOException {
        ConsoleReader reader = new ConsoleReader();
        Completer completer = new StringsCompleter("get", "get-config", "exit", "lock", "unlock", "commit",
                "discard-changes");
        reader.addCompleter(completer);

        do {
            try {
                String command = reader.readLine(hostname + "> ");
                command = command.trim();
                switch (command) {
                case "get":
                    System.out.println(get());
                    System.out.println();
                    break;
                case "get-config":
                    System.out.println(getConfig());
                    System.out.println();
                    break;
                case "lock":
                    lock();
                    break;
                case "unlock":
                    unlock();
                    break;
                case "commit":
                    commit();
                    break;
                case "discard-changes":
                    discardChanges();
                    break;
                case "":
                    break;
                case "exit":
                    System.exit(0);
                default:
                    System.err.println("Ohnoes not a command: '" + command + "'");
                }
            } catch (Throwable t) {
                System.err.println(t.toString());
            }
        } while (true);
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

    public String getConfig() throws NcException {

        try {
            int source = datastore.ordinal();
            if (device != null) {
                NetconfSession session = device.getSession(SESSION_NAME);
                if (session != null) {
                    NodeSet nodes = session.getConfig(source);
                    return toXMLDocument(nodes);
                }
            }
        } catch (Throwable e) {
            throw new NcException(e.toString(), e);
        }

        return "";
    }

    public String get() {
        try {
            if (device != null) {
                NetconfSession session = device.getSession(SESSION_NAME);
                if (session != null) {
                    NodeSet nodes = session.get();
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

    public void lock() throws NcException {
        try {
            int source = datastore.ordinal();
            if (device != null) {
                NetconfSession session = device.getSession(SESSION_NAME);
                if (session != null) {
                    session.lock(source);
                }
            }
        } catch (Throwable t) {
            throw new NcException(t.toString(), t);
        }
    }

    public void unlock() throws NcException {
        try {
            int source = datastore.ordinal();
            if (device != null) {
                NetconfSession session = device.getSession(SESSION_NAME);
                if (session != null) {
                    session.unlock(source);
                }
            }
        } catch (Throwable t) {
            throw new NcException(t.toString(), t);
        }
    }

    public void commit() throws NcException {
        try {
            if (device != null) {
                NetconfSession session = device.getSession(SESSION_NAME);
                if (session != null) {
                    session.commit();
                }
            }
        } catch (Throwable t) {
            throw new NcException(t.toString(), t);
        }
    }

    public void discardChanges() throws NcException {
        try {
            if (device != null) {
                NetconfSession session = device.getSession(SESSION_NAME);
                if (session != null) {
                    session.discardChanges();
                }
            }
        } catch (Throwable t) {
            throw new NcException(t.toString(), t);
        }
    }

    public void copyConfig(String config) throws NcException {
        try {
            int target = datastore.ordinal();
            if (device != null) {
                NetconfSession session = device.getSession(SESSION_NAME);
                if (session != null) {
                    XMLParser parser = new XMLParser();
                    Element sourceTree = parser.parse(config);
                    session.copyConfig(sourceTree, target);
                }
            }
        } catch (Throwable t) {
            throw new NcException(t.toString(), t);
        }
    }

    public void editConfig(String config) throws NcException {
        try {
            int target = datastore.ordinal();
            if (device != null) {
                NetconfSession session = device.getSession(SESSION_NAME);
                if (session != null) {
                    XMLParser parser = new XMLParser();
                    Element configTree = parser.parse(config);
                    session.editConfig(target, configTree);
                }
            }
        } catch (Throwable t) {
            throw new NcException(t.toString(), t);
        }
    }

    @Override
    public void close() {
        disconnect();
    }
}
