import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Thread;

public class ChatClient implements Runnable {

    public Socket socket = null;
    public Socket socketFTP = null;
    ServerSocket server_socketFTP;
    ServerSocket server_socket;
    public String client_name = "xx";
    int port = 6001;
    int portFTP = 6002;
    DataInputStream input;
    DataOutputStream output;


    public static void main(String[] args) {
        int port;
        if (args.length < 1 || args.length > 5) {
            System.out.println("Usage:");
            System.out.println("   ChatClient <port number> <port number> -p <port number>");
            return;
        }
        ChatClient client = new ChatClient();
        client.portFTP = Integer.parseInt(args[1]);
        client.port = Integer.parseInt(args[3]);              
        client.client_name = "client4";
        try {
            Socket client_socket = new Socket("localhost", client.port);
            client.socket = client_socket;
            client.output = new DataOutputStream(client.socket.getOutputStream());
            client.input = new DataInputStream(client.socket.getInputStream());
            System.out.println("client connected");
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.getStackTrace();
            System.exit(0);
        }
        System.out.println("start thread...");
        Thread t1 = new Thread(client, "Send");
        t1.start();
        System.out.println("start runR");
        client.runRecv();
        client.close();
        System.exit(0); 
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
            System.err.println(e);
            e.printStackTrace();
            System.exit(0);
        }
    }


    public void startDownloadFile(String file_name, int port_transfer){
        Runnable download_task = () -> {
            try {
                downloadFile(file_name, port_transfer);
            
            } catch (Exception e) {
                System.exit(0);
            }
            System.err.println("FTP client really done.");
        };
        new Thread(download_task).start();
    }


    public void downloadFile(String file_name, int port_transfer) throws IOException { //file requesting
        FileOutputStream file_out = new FileOutputStream(file_name);
        byte[] buffer = new byte[1500];
        int number_read;
        Socket client_socket = null;
        try {
            client_socket = new Socket("localhost", port_transfer);
            DataInputStream inputFTP = new DataInputStream(client_socket.getInputStream());
            //DataOutputStream output = new DataOutputStream(client_socket.getOutputStream());
            this.socket = client_socket;
            //System.out.println("FTP_Client connected");
            while ((number_read = inputFTP.read(buffer)) != -1) {
                file_out.write(buffer, 0, number_read);
            }
            file_out.close();
            inputFTP.close();
            client_socket.close();
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            file_out.close();
            if (client_socket != null) {
                client_socket.close();
            }
            System.exit(0);

        }
    }


    public void startFTPServer(String file_name){
        System.err.println("Starting FTP Server (Client) " + file_name + ", port: " + portFTP);
        Runnable server_task = () -> {
            try {
                server_socketFTP = new ServerSocket(portFTP);
                transferFile(file_name);
                System.err.println("FTP server closing socket....");

                server_socketFTP.close();
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
                System.exit(0);
            }
            System.err.println("FTP server really done.");

        };
        new Thread(server_task).start();
    }


    public void transferFile(String file_name){
		File file= null;
        try {
            System.err.println("Waiting for connection (transferFile)....");
            socketFTP = server_socketFTP.accept();
            System.err.println("Connected.");
            DataOutputStream outputFTP = new DataOutputStream(socketFTP.getOutputStream());
            // file_name= input.readUTF();
		    file = new File( file_name );
            if ( file.exists() && file.canRead() ){
                FileInputStream file_input= new FileInputStream(file);
		        //System.out.println( "Transmitting file: " + file_name );
                byte[] file_buffer= new byte[1500];
		        int number_read;
                System.err.println("sendfing file data");
		        while( (number_read= file_input.read( file_buffer )) != -1 ){
			        outputFTP.write( file_buffer, 0, number_read );
                }
                System.err.println("sendfing file data done.");

		        file_input.close();
                outputFTP.close();
                // socketFTP.shutdownOutput();
                socketFTP.close();

                System.err.println("FTP server done.");
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
            // System.exit(0);

        }
    }


    public void runRecv() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String message;
           
            System.out.println("Hello! What is your name?");
            if((message = reader.readLine()) != null){
                output.writeUTF(message);
                output.writeInt(portFTP);
            }
            System.out.println(
                "Enter an option ('m', 'f', 'x'): \n \t (m)essage (send) \n \t (f)ile    (request)\n \t e(x)it");
            while ((message = reader.readLine()) != null) {
                if (message.equals("m")) {
                    System.out.println("Enter message:");
                    if ((message = reader.readLine()) != null) {
                        output.writeUTF("m");
                        output.writeUTF(message);
                        System.out.println(
                                "Enter an option ('m', 'f', 'x'): \n \t (m)essage (send) \n \t (f)ile    (request)\n \t e(x)it");
                        continue;
                    }
                } else if (message.equals("f")) {
                    System.out.println("Who do you want the file from?");
                    if ((message = reader.readLine()) != null) {
                        output.writeUTF("f");
                        output.writeUTF(message);
                        System.out.println("Which file do you want?");
                        if ((message = reader.readLine()) != null) {
                            output.writeUTF(message);
                            int port_transfer = 6007;  // input.readInt();
                            System.err.println("got port #" + port_transfer);
                            startDownloadFile(message, port_transfer);
                            System.out.println(
                                "Enter an option ('m', 'f', 'x'): \n \t (m)essage (send) \n \t (f)ile    (request)\n \t e(x)it");
                        }
                    }
                    continue;
                    
                } else if (message.equals("x")) {
                    close();
                    System.out.flush();
                    output.flush();
                    output.close();
                    break;
                }
            }
        
            // System.err.println("End of KBD input - Closing socket.");
            close();
            System.err.println(client_name + ":" + "End of KBD input - Closing socket.");
            System.exit(0);
        } catch (EOFException eofx) {
            eofx.printStackTrace();
            System.out.println("EOF encountered; other side shut down");
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }


    public void run() {
        try {

            String message;
            while ((message = input.readUTF()) != null) {
                if(message.equals("m")){
                    message = input.readUTF();
                    System.out.println(message);
                }
                if(message.equals("f")){
                    message = input.readUTF();
                    //output.writeUTF(String.valueOf(portFTP));
                    //output.flush();
                    //System.err.println("starting FTP server, .... Sent FTP port number");
                    startFTPServer(message);
                }
                
            }
            input.close();
            output.close();
            close();
        } catch (Exception e) {
            // close();
            System.err.println("Participant has left the messenger.");
            System.err.flush();
            System.out.flush();
            //   System.exit(0);
            Runtime.getRuntime().halt(0);
        }
    }
}