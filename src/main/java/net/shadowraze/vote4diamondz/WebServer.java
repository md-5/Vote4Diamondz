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
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;

/**
 * Simple Java non-blocking NIO webserver.
 *
 * @author md_5
 */
public abstract class WebServer implements Runnable {

    private ByteBuffer buf = ByteBuffer.allocate(2048);
    private Charset charset = Charset.forName("UTF-8");
    private CharsetDecoder decoder = charset.newDecoder();
    private CharsetEncoder encoder = charset.newEncoder();
    private Selector selector = Selector.open();
    private ServerSocketChannel server = ServerSocketChannel.open();
    private boolean isRunning = true;

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
                            // reset our buffer
                            buf.rewind();
                            // read into it
                            client.read(buf);
                            // flip it so we can decode it
                            buf.flip();
                            // decode the bytes, handle it, and write the response
                            client.write(encoder.encode(CharBuffer.wrap("HTTP/1.1 200 OK\r\n\r\n" + handle(client, decoder.decode(buf).toString()) + "\r\n")));
                        }
                    } catch (Exception ex) {
                        System.err.println("Error handling client: " + key.channel());
                        System.err.println(ex);
                        System.err.println("\tat " + ex.getStackTrace()[0]);
                    } finally {
                        if (key.channel() instanceof SocketChannel) {
                            try {
                                key.channel().close();
                            } catch (IOException ex) {
                                // too late to do anything here
                            }
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
    protected abstract String handle(SocketChannel client, String request) throws IOException;

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
        buf = null;
        charset = null;
        decoder = null;
        encoder = null;
        selector = null;
        server = null;
    }
}
