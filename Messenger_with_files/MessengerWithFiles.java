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

public class MessengerWithFiles implements Runnable {

    public Socket socket = null;
    public Socket socketFTP = null;
    public String client_name = "xx";
    ServerSocket server_socket;
    ServerSocket server_socketFTP;
    int port = 6001;
    int portFTP = 6002;

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 5) {
            System.out.println("Usage:");
            System.out.println("   MessengerWithFiles Server -l <port number>");
            System.out.println("   MessengerWithFiles Client -l <port number> -p <port number>");
            return;
        }

        MessengerWithFiles M = new MessengerWithFiles();
        if (args.length == 2 && args[0].startsWith("-l")) { //server
            M.port = Integer.parseInt(args[1]);
            M.client_name = "server";
            try {
                ServerSocket server_socket = new ServerSocket(M.port);
                Socket client_socket = server_socket.accept();
                M.socket = client_socket;
                M.server_socket = server_socket;
                System.out.println("server connected");
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
            System.exit(0);

            }

        //uh....
        } else if(args.length == 4 && args[0].startsWith("-l") && args[2].startsWith("-p")){ //File transfer
            M.portFTP = Integer.parseInt(args[1]);
            M.port = Integer.parseInt(args[3]);              
            M.client_name = "client4";
            try {
                M.socket = new Socket("localhost", M.port);
                System.out.println("client connected");


            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
            System.exit(0);

            }
        }
        
        else { // client
            M.client_name = "client-xxx";
            M.port = Integer.parseInt(args[0]);
            try {
                Socket client_socket = new Socket("localhost", M.port);
                M.socket = client_socket;
                System.out.println("client connected");
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
            }
        }
        System.out.println("start thread...");

        Thread t1 = new Thread(M, "Send");
        t1.start();
        
        //starting ftp server thread
        //System.out.println("start serverFTP");
        //M.startFTPServer();        
        System.out.println("start runR");
        M.runRecv();
        M.close();
        System.exit(0);
    }

    public void close() {
        try {
            if (socket != null) {
                socket.shutdownOutput();
                socket.close();
            }
            if(socketFTP != null){
                socketFTP.shutdownOutput();
                socketFTP.close();
            }
            if (server_socket != null) {
                server_socket.close();
            }
            if (server_socketFTP != null) {
                server_socketFTP.close();
            }
        } catch (Exception e) {
            System.err.println("Issue in close.");
            System.exit(0);

        }
    }


    public void startDownloadFile(String file_name){
        Runnable download_task = () -> {
            try {
                downloadFile(file_name);
            
            } catch (Exception e) {
                System.exit(0);
            }
            System.err.println("FTP client really done.");
        };
        new Thread(download_task).start();
    }


    public void downloadFile(String file_name) throws IOException { //file requesting
        FileOutputStream file_out = new FileOutputStream(file_name);
        byte[] buffer = new byte[1500];
        int number_read;
        Socket client_socket = null;
        try {
            client_socket = new Socket("localhost", portFTP);
            DataInputStream input = new DataInputStream(client_socket.getInputStream());
            //DataOutputStream output = new DataOutputStream(client_socket.getOutputStream());
            this.socket = client_socket;
            //System.out.println("FTP_Client connected");
            while ((number_read = input.read(buffer)) != -1) {
                file_out.write(buffer, 0, number_read);
            }
            file_out.close();
            input.close();
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
        Runnable server_task = () -> {
            try {
                server_socketFTP = new ServerSocket(portFTP);
                //boolean running = true;
                //while(running){
                    transferFile(file_name);
                System.err.println("FTP server closing socket....");

                server_socketFTP.close();
                //}
            } catch (Exception e) {
            System.exit(0);
                
            }
            System.err.println("FTP server really done.");

        };
        new Thread(server_task).start();
    }


    public void transferFile(String file_name){
		File file= null;
        try {
            socketFTP = server_socketFTP.accept();
            //DataInputStream input = new DataInputStream(socketFTP.getInputStream());
            DataOutputStream output = new DataOutputStream(socketFTP.getOutputStream());
            // file_name= input.readUTF();
		    file = new File( file_name );
            if ( file.exists() && file.canRead() ){
                FileInputStream file_input= new FileInputStream(file);
		        //System.out.println( "Transmitting file: " + file_name );
                byte[] file_buffer= new byte[1500];
		        int number_read;
		        while( (number_read= file_input.read( file_buffer )) != -1 ){
			        output.write( file_buffer, 0, number_read );
                }
		        file_input.close();
                output.close();
                socketFTP.shutdownOutput();
                socketFTP.close();

                System.err.println("FTP server done.");
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            System.exit(0);

        }
    }
/*
 * ======================================================================================================
 */
    public void runRecv() { 
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println(
                    "Enter an option ('m', 'f', 'x'): \n \t (m)essage (send) \n \t (f)ile    (request)\n \t e(x)it");
            String message;
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
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
                    System.out.println("Which file do you want?");
                    if ((message = reader.readLine()) != null) {
                        output.writeUTF("f");
                        output.writeUTF(message);
                        startDownloadFile(message);
                        System.out.println(
                                "Enter an option ('m', 'f', 'x'): \n \t (m)essage (send) \n \t (f)ile    (request)\n \t e(x)it");
                        continue;
                    }
                } else if (message.equals("x")) {
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
            System.out.println("EOF encountered; other side shut down");
            System.exit(0);

        } catch (IOException e1) {
            e1.printStackTrace();
          //  Runtime.getRuntime().halt(0);

            System.exit(0);

        }

    }

    public void run() {
        try {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            String message;
            while ((message = input.readUTF()) != null) {
                if(message.equals("m")){
                    message = input.readUTF();
                    System.out.println(message);
                }
                if(message.equals("f")){
                    message = input.readUTF();
                    startFTPServer(message);
                }
                
            }
            input.close();
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
