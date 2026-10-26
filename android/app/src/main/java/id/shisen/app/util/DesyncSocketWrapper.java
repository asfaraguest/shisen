package id.shisen.app.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

/**
 * DesyncSocketWrapper wraps a standard TCP socket and intercepts the initial TLS ClientHello
 * write. When desync is enabled for the target host, it splits the ClientHello into two distinct
 * TLS records with a micro-pause, preventing middlebox DPI firewalls from detecting the SNI.
 */
public class DesyncSocketWrapper extends Socket {

    private final Socket delegate;
    private final String host;
    private final boolean desyncEnabled;
    private OutputStream wrappedOut;

    public DesyncSocketWrapper(Socket delegate, String host, boolean desyncEnabled) {
        this.delegate = delegate;
        this.host = host;
        this.desyncEnabled = desyncEnabled;
    }

    @Override
    public synchronized OutputStream getOutputStream() throws IOException {
        if (wrappedOut == null) {
            wrappedOut = new DesyncOutputStream(delegate.getOutputStream(), desyncEnabled);
        }
        return wrappedOut;
    }

    @Override public InputStream getInputStream() throws IOException { return delegate.getInputStream(); }
    @Override public void connect(SocketAddress endpoint) throws IOException { delegate.connect(endpoint); }
    @Override public void connect(SocketAddress endpoint, int timeout) throws IOException { delegate.connect(endpoint, timeout); }
    @Override public void bind(SocketAddress bindpoint) throws IOException { delegate.bind(bindpoint); }
    @Override public InetAddress getInetAddress() { return delegate.getInetAddress(); }
    @Override public InetAddress getLocalAddress() { return delegate.getLocalAddress(); }
    @Override public int getPort() { return delegate.getPort(); }
    @Override public int getLocalPort() { return delegate.getLocalPort(); }
    @Override public SocketAddress getRemoteSocketAddress() { return delegate.getRemoteSocketAddress(); }
    @Override public SocketAddress getLocalSocketAddress() { return delegate.getLocalSocketAddress(); }
    @Override public SocketChannel getChannel() { return delegate.getChannel(); }
    @Override public void setTcpNoDelay(boolean on) throws SocketException { delegate.setTcpNoDelay(on); }
    @Override public boolean getTcpNoDelay() throws SocketException { return delegate.getTcpNoDelay(); }
    @Override public void setSoLinger(boolean on, int linger) throws SocketException { delegate.setSoLinger(on, linger); }
    @Override public int getSoLinger() throws SocketException { return delegate.getSoLinger(); }
    @Override public void sendUrgentData(int data) throws IOException { delegate.sendUrgentData(data); }
    @Override public void setOOBInline(boolean on) throws SocketException { delegate.setOOBInline(on); }
    @Override public boolean getOOBInline() throws SocketException { return delegate.getOOBInline(); }
    @Override public synchronized void setSoTimeout(int timeout) throws SocketException { delegate.setSoTimeout(timeout); }
    @Override public synchronized int getSoTimeout() throws SocketException { return delegate.getSoTimeout(); }
    @Override public synchronized void setSendBufferSize(int size) throws SocketException { delegate.setSendBufferSize(size); }
    @Override public synchronized int getSendBufferSize() throws SocketException { return delegate.getSendBufferSize(); }
    @Override public synchronized void setReceiveBufferSize(int size) throws SocketException { delegate.setReceiveBufferSize(size); }
    @Override public synchronized int getReceiveBufferSize() throws SocketException { return delegate.getReceiveBufferSize(); }
    @Override public void setKeepAlive(boolean on) throws SocketException { delegate.setKeepAlive(on); }
    @Override public boolean getKeepAlive() throws SocketException { return delegate.getKeepAlive(); }
    @Override public void setTrafficClass(int tc) throws SocketException { delegate.setTrafficClass(tc); }
    @Override public int getTrafficClass() throws SocketException { return delegate.getTrafficClass(); }
    @Override public void setReuseAddress(boolean on) throws SocketException { delegate.setReuseAddress(on); }
    @Override public boolean getReuseAddress() throws SocketException { return delegate.getReuseAddress(); }
    @Override public synchronized void close() throws IOException { delegate.close(); }
    @Override public void shutdownInput() throws IOException { delegate.shutdownInput(); }
    @Override public void shutdownOutput() throws IOException { delegate.shutdownOutput(); }
    @Override public String toString() { return delegate.toString(); }
    @Override public boolean isConnected() { return delegate.isConnected(); }
    @Override public boolean isBound() { return delegate.isBound(); }
    @Override public boolean isClosed() { return delegate.isClosed(); }
    @Override public boolean isInputShutdown() { return delegate.isInputShutdown(); }
    @Override public boolean isOutputShutdown() { return delegate.isOutputShutdown(); }

    private static class DesyncOutputStream extends FilterOutputStream {
        private final boolean desyncEnabled;
        private boolean firstWrite = true;

        DesyncOutputStream(OutputStream out, boolean desyncEnabled) {
            super(out);
            this.desyncEnabled = desyncEnabled;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (desyncEnabled && firstWrite && len > 5 && (b[off] & 0xFF) == 0x16) {
                firstWrite = false;
                byte[] clientHello = Arrays.copyOfRange(b, off, off + len);
                byte[][] splitRecords = TlsDesyncHelper.splitClientHello(clientHello);
                if (splitRecords.length == 2) {
                    // Send Record 1 (1 byte of handshake data)
                    out.write(splitRecords[0]);
                    out.flush();
                    try {
                        // 30ms micro-pause ensures packet boundary separation in TCP layer
                        Thread.sleep(30);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    // Send Record 2 (remaining handshake data)
                    out.write(splitRecords[1]);
                    out.flush();
                    return;
                }
            }
            firstWrite = false;
            out.write(b, off, len);
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }
    }
}
