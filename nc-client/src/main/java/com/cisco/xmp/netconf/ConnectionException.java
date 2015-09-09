package com.cisco.xmp.netconf;

import java.io.IOException;

public class ConnectionException extends IOException {

    private static final long serialVersionUID = -5301330322464542599L;

    public ConnectionException() {
        // TODO Auto-generated constructor stub
    }

    public ConnectionException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public ConnectionException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

}
