import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class handleClient implements Runnable {

    final Socket s;
    final public static String CRLF = "\r\n";

    String clientRoom = "";

    // Input variables for handling the HTTPRequest
    InputStream input;
    InputStreamReader inputReader;

    // Member Variables
    Scanner scanner;
    String method;
    String filePath;
    String protocol;
    Map<String, String> requestDetails;
    Map<String, String> responseDetails;
    String header;
    String desc;

    // Output variables for handling the HTTPResponse
    OutputStream output;
    PrintWriter responder;
    String contentType;

    File response;
    String responseStatusCode;


    public handleClient(Socket socket) {
        this.s = socket; // Tell the object about the socket that was trying to connect in main()
        // This will construct a client connection which can be used later in the main() function
        // which will allow the use of private functions to handle the I/O...
    }

    private void HTTPRequest() throws IOException {

        // Assign values to the variables that are necessary to handle the HTTPRequest.
        input = s.getInputStream();
        inputReader = new InputStreamReader(input);
        scanner = new Scanner(inputReader);

        // Populate member variables.
        method = scanner.next();
        filePath = scanner.next();
        protocol = scanner.next();
        filePath = filePath.substring(1); // Removes the '/' char for the filepath.

        // Prints out the HTTP Request in the console to make debugging easier.
        System.out.println("REQUEST -----------------------------------------------------------------------------------------------------------------------------------------------" + CRLF);
        System.out.println(method + " " + filePath + " " + protocol + CRLF);

        // Create a map of all the details
        requestDetails = new HashMap<>();
        String line = scanner.nextLine();
        line = scanner.nextLine();

        while (!line.equals("")) {

//                if (line.equals("")) {
//                    break;
//                }
            String[] wordArray = line.split(": ");
            header = wordArray[0];
            desc = wordArray[1];
            requestDetails.put(header, desc);
            System.out.println(header + ": " + desc);
            //System.out.println(protocol + "\n");

            line = scanner.nextLine();
        }

        System.out.println("\n");

        if (isRequestingWebsocketConnection()) {

        }


    }

    private void HTTPResponse() throws IOException, NoSuchAlgorithmException {

        System.out.println("RESPONSE ----------------------------------------------------------------------------------------------------------------------------------------------" + CRLF);

        // Assign values to the necessary variables to handle the HTTP Response.
        output = s.getOutputStream();
        response = new File(filePath);
        responder = new PrintWriter(output);
        responseDetails = new HashMap<>();


        if (!isRequestingWebsocketConnection()) {
            // Check if the filepath is specified and if not, respond with the default webpage, i.e. 'index.html', otherwise continue.
            if (filePath.equals("")) {
                response = new File("index.html");
            }

            // Check if the filepath specified exists, and if not throw a FileNotFoundException and respond with status code '404 Bad Request', otherwise respond with status code '200 OK'.
            if (!response.exists()) {
                responseStatusCode = "404 Bad Request";
                throw new FileNotFoundException("404: Bad Request!");
            } else {
                responseStatusCode = "200 OK";
            }

            // Get Content Type
            contentType = Files.probeContentType(Path.of(filePath));
            responseDetails.put("Content-Type", contentType);
            responseDetails.put("Content-Length", ("" + (int) response.length()));
        }

        // Check if the client is requesting websockets upgrade
        if (isRequestingWebsocketConnection()) {
            handshake(); // Adds the necessary headers to the responseDetails Map.
        }

        // Start writing to the output stream
        responder.write(protocol + " " + responseStatusCode + CRLF); // Respond with the header.
        System.out.print(protocol + " " + responseStatusCode + CRLF);

        // Print all the response headers and descriptions
        for (Map.Entry<String, String> entry : responseDetails.entrySet()) {
            responder.print(entry.getKey() + ": " + entry.getValue() + CRLF);
            System.out.print(entry.getKey() + ": " + entry.getValue() + CRLF);
        }
        responder.print(CRLF);

        //System.out.print(CRLF);
        responder.flush();

        // Respond with bytes to a bufferedOutputStream.
        if (!isRequestingWebsocketConnection()) {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(output);
            byte[] responseByteData = new byte[(int) response.length()];
            FileInputStream fileInputStream = new FileInputStream(response);
            fileInputStream.read(responseByteData);

            System.out.println(responseByteData);
            for (byte responseByteDatum : responseByteData) {
                bufferedOutputStream.write(responseByteDatum);
            }
            bufferedOutputStream.flush();
        } else {
            while (true) {
                WebSocketConnection webSocketConnection = new WebSocketConnection(this);
                System.out.println("WEBSOCKET DATA ---------------------------------------------------------- START ----->");
                webSocketConnection.decodeData();
                webSocketConnection.handleResponse();
//                webSocketConnection.sendMessage(this);
                System.out.println("WEBSOCKET DATA ---------------------------------------------------------- END   ----->");


            }


        }

        responder.write("\r\n\r\n");
        responder.flush();

        // Close the connection with the client that requested this connection.
        if (!isRequestingWebsocketConnection()) {
            s.close();
        }
    }

    private void handshake() throws NoSuchAlgorithmException {
        String value = generateAcceptKey();
        responseStatusCode = "101 Switching Protocols";
        responseDetails.put("Upgrade", "websocket");
        responseDetails.put("Connection", "Upgrade");
        responseDetails.put("Sec-WebSocket-Accept", value);
    }

    private boolean isRequestingWebsocketConnection() {
        for (Map.Entry<String, String> entry : requestDetails.entrySet()) {
            if (entry.getKey().equals("Upgrade") && entry.getValue().equals("websocket")) {
                return true;
            }
        }
        return false;
    }

    private String generateAcceptKey() throws NoSuchAlgorithmException {
        final String MAGIC_KEY = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        final String REQUESTED_WEBSOCKET_KEY = requestDetails.get("Sec-WebSocket-Key");
        String requestKey = REQUESTED_WEBSOCKET_KEY + MAGIC_KEY;
        MessageDigest shaEncoder = MessageDigest.getInstance("SHA-1");

        byte[] encoded = shaEncoder.digest(requestKey.getBytes());
        final String RESPONSE_KEY = Base64.getEncoder().encodeToString(encoded);

        return RESPONSE_KEY;
    }

    @Override
    public void run() {
        //System.out.println("Request handled by thread #" + Thread.currentThread().getId());
        System.out.println("Server handled a request on thread " + Thread.currentThread().getId());
        try { // Try the logic but if it fails move on to catch and handle the exceptions.
            HTTPRequest();
            HTTPResponse();
        } catch (FileNotFoundException fnf) { // Handle the server in case the request file by the user was not found.
            System.out.println("404 File not Found! Bad Request!");

            // Redirected user to the 404 webpage to let them know of the issue.
            this.filePath = "404.html";
            try {
                HTTPResponse();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        } catch (IOException ioe) { // I am not sure what kind of errors would be classified as IOExceptions.
            System.out.println("IOException!");
            ioe.printStackTrace();
        } catch (Exception e) { // Handle any other exceptions and avoid interrupting the webserver.
            e.printStackTrace();
            System.out.println("This is confusing!");
        }
    }
}
