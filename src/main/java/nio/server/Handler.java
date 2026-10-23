package nio.server;

/** Raw streaming echo variant; all connection buffers belong to their own session. */
public class Handler extends io.support.AsyncEchoHandler {
    public Handler() { super(false); }
}
