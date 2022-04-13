import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MyHttpServer {

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(8080); // Initialize the server

        while (true) {
            Socket client = server.accept(); // Check if a connection is requested
            //System.out.println("New client connected" + client.getInetAddress().getHostAddress()); // Console output client connection data.

            handleClient clientConnection = new handleClient(client); // Create a client socket connection object that will handle everything in the server such as parsing the request and send the response.

            Thread t = new Thread(clientConnection); // Create a new thread if a connection is requested.
            t.start(); // Then start the thread.
        }
    }
}
