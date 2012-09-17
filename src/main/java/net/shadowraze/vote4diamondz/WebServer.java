package net.shadowraze.vote4diamondz;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;

/**
 * Simple Java non-blocking NIO webserver.
 *
 * @author md_5
 */
public class WebServer implements Runnable {

    private Charset charset = Charset.forName("UTF-8");
    private CharsetEncoder encoder = charset.newEncoder();
    private Selector selector = Selector.open();
    private ServerSocketChannel server = ServerSocketChannel.open();
    private boolean isRunning = true;
    private boolean debug = true;

    /**
     * Create a new server and immediately binds it.
     *
     * @param address the address to bind on
     * @throws IOException if there are any errors creating the server.
     */
    protected WebServer(InetSocketAddress address) throws IOException {
        server.socket().bind(address);
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    /**
     * Core run method. This is not a thread safe method, however it is non
     * blocking. If an exception is encountered it will be thrown wrapped in a
     * RuntimeException, and the server will automatically be {@link #shutDown}
     */
    @Override
    public final void run() {
        if (isRunning) {
            try {
                selector.selectNow();
                Iterator<SelectionKey> i = selector.selectedKeys().iterator();
                while (i.hasNext()) {
                    SelectionKey key = i.next();
                    i.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    try {
                        // get a new connection
                        if (key.isAcceptable()) {
                            // accept them
                            SocketChannel client = server.accept();
                            // non blocking please
                            client.configureBlocking(false);
                            // show out intentions
                            client.register(selector, SelectionKey.OP_READ);
                            // read from the connection
                        } else if (key.isReadable()) {
                            //  get the client
                            SocketChannel client = (SocketChannel) key.channel();
                            // get the session
                            HTTPSession session = (HTTPSession) key.attachment();
                            // create it if it doesnt exist
                            if (session == null) {
                                session = new HTTPSession(client);
                                key.attach(session);
                            }
                            // decode the message
                            String line;
                            while ((line = session.readLine()) != null) {
                                session.writeLine("You said " + line);
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("Error handling client: " + key.channel());
                        if (debug) {
                            ex.printStackTrace();
                        } else {
                            System.err.println(ex);
                            System.err.println("\tat " + ex.getStackTrace()[0]);
                        }
                        if (key.attachment() instanceof HTTPSession) {
                            ((HTTPSession) key.attachment()).close();
                        }
                    }
                }
            } catch (IOException ex) {
                // call it quits
                shutdown();
                // throw it as a runtime exception so that Bukkit can handle it
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Handle a web request.
     *
     * @param client
     * @param request entire request as sent by the client.
     * @return the string to be sent to the client. Must not include HTTP
     * headers.
     */
    protected String handle(SocketChannel client, String request) throws IOException {
        return null;
    }

    /**
     * Shutdown this server, preventing it from handling any more requests.
     */
    public final void shutdown() {
        isRunning = false;
        try {
            selector.close();
            server.close();
        } catch (IOException ex) {
            // do nothing, its game over
        }
        charset = null;
        encoder = null;
        selector = null;
        server = null;
    }

    public final class HTTPSession {

        private final SocketChannel channel;
        private final ByteBuffer buffer = ByteBuffer.allocate(2048);
        private int mark = 0;

        public HTTPSession(SocketChannel channel) {
            this.channel = channel;
        }

        /**
         * Try to read a line.
         */
        public String readLine() throws IOException {
            readData();
            StringBuilder sb = new StringBuilder();
            int l = -1;
            while (buffer.hasRemaining()) {
                char c = (char) buffer.get();
                if (c == '\n' && l == '\r') {
                    mark = buffer.position();
                    return sb.substring(0, sb.length());
                }
                sb.append(c);
                l = c;
            }
            return null;
        }

        /**
         * Get more data from the stream.
         */
        private void readData() throws IOException {
            buffer.limit(buffer.capacity());
            int read = channel.read(buffer);
            if (read == -1) {
                throw new IOException("End of stream");
            }
            buffer.flip();
            buffer.position(mark);
        }

        public void writeLine(String line) throws IOException {
            channel.write(encoder.encode(CharBuffer.wrap(line + "\r\n")));
        }

        public void close() {
            try {
                channel.close();
            } catch (IOException ex) {
            }
        }
    }

    public static void main(String[] args) throws Exception {
        WebServer server = new WebServer(new InetSocketAddress(5555));
        while (true) {
            server.run();
            Thread.sleep(100);
        }
    }
}
