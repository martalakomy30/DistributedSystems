import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.Thread;

public class ChatClient implements Runnable {

    public Socket socket = null;
    ServerSocket server_socket;

    public static void main(String[] args) {

        int port;
        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage:");
            System.out.println("   ChatClient <port number>");
            return;
        }

        ChatClient client = new ChatClient();
        port = Integer.parseInt(args[0]);
        try {
            Socket client_socket = new Socket("localhost", port);
            client.socket = client_socket;
            System.out.println("client connected");
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    
    System.out.println("start thread...");
    Thread t1 = new Thread(client, "Send");
    t1.start();
    System.out.println("start runR");
    client.runRecv();
    client.close();System.exit(0);
    }

    public void close() {
        try {
            if (socket != null) {
                socket.shutdownOutput();
                socket.close();
            }
            if (server_socket != null) {
                server_socket.close();
            }
        } catch (Exception e) {

        }
    }

    public void runRecv() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter message: ");
            String message;
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            while ((message = reader.readLine()) != null) {
                output.writeUTF(message);
            }
            System.out.println("Closing socket.");
            close();

        } catch (EOFException eofx) {
            System.out.println("EOF encountered; other side shut down");
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }

    public void run() {
        try {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            String message;
            while ((message = input.readUTF()) != null) {
                System.out.println(message);
            }
            close();
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }
}