package raylras.zen.lsp;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Manager implements LanguageClientAware {

    private static ZenScriptLanguageServer server;
    private static LanguageClient client;
    public static final int PORT = 9865;

    public static void main(String[] args) {
        start();
    }

    public static void start() {
        try {
            System.out.println("Waiting client...");
            Socket socket = new ServerSocket(PORT).accept();
            System.out.println("Found a language client from " + socket.getRemoteSocketAddress() + ", starting the language server");
            server = new ZenScriptLanguageServer();
            Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, socket.getInputStream(), socket.getOutputStream());
            client = launcher.getRemoteProxy();
            launcher.startListening();
        } catch (IOException ex) {
            System.out.println("Could not start the language server: " + ex);
        }
    }

    public static ZenScriptLanguageServer getServer() {
        return server;
    }

    public static LanguageClient getClient() {
        return client;
    }

    @Override
    public void connect(LanguageClient client) {
        if (client == null) return;
        Manager.client = client;
        client.logMessage(new MessageParams(MessageType.Info, "Server connected"));
        client.logMessage(new MessageParams(MessageType.Info,"Hello from server"));
    }

}
