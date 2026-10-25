package id.menatap.app.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocketFactory;

/**
 * DesyncSSLSocketFactory layers TLS ClientHello multi-record desynchronization over raw TCP sockets
 * for target hosts identified as blocked by DPI middleboxes.
 */
public class DesyncSSLSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory delegate;

    public DesyncSSLSocketFactory(SSLSocketFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        boolean shouldDesync = DynamicBlockedHosts.isBlocked(host);
        Socket wrapped = new DesyncSocketWrapper(s, host, shouldDesync);
        return delegate.createSocket(wrapped, host, port, autoClose);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        Socket raw = new Socket(host, port);
        return createSocket(raw, host, port, true);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        Socket raw = new Socket(host, port, localHost, localPort);
        return createSocket(raw, host, port, true);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        String hostName = host.getHostName();
        Socket raw = new Socket(host, port);
        return createSocket(raw, hostName, port, true);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        String hostName = address.getHostName();
        Socket raw = new Socket(address, port, localAddress, localPort);
        return createSocket(raw, hostName, port, true);
    }
}
