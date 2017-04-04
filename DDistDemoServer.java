import java.net.*;
import java.io.*;

/**
*
* A very simple server which will way for a connection from a client and print
* what the client sends. When the client closes the connection, the server is
* ready for the next client.
*/

public class DDistDemoServer {

  /*
  * Your group should use port number 40HGG, where H is your "hold nummer (1,2 or 3)
  * and GG is gruppe nummer 00, 01, 02, ... So, if you are in group 3 on hold 1 you
  * use the port number 40103. This will avoid the unfortunate situation that you
  * connect to each others servers.
  */
  protected int portNumber = 40404;
  protected ServerSocket serverSocket;
  private String toClientText = "Enter an answer: ";

  /**
  *
  * Will print out the IP address of the local host and the port on which this
  * server is accepting connections.
  */
  protected void printLocalHostAddress() {
    try {
      InetAddress localhost = InetAddress.getLocalHost();
      String localhostAddress = localhost.getHostAddress();
      System.out.println("Contact this server on the IP address " + localhostAddress);
    } catch (UnknownHostException e) {
      System.err.println("Cannot resolve the Internet address of the local host.");
      System.err.println(e);
      System.exit(-1);
    }
  }

  /**
  *
  * Will register this server on the port number portNumber. Will not start waiting
  * for connections. For this you should call waitForConnectionFromClient().
  */
  protected void registerOnPort() {
    try {
      serverSocket = new ServerSocket(portNumber);
    } catch (IOException e) {
      serverSocket = null;
      System.err.println("Cannot open server socket on port number" + portNumber);
      System.err.println(e);
      System.exit(-1);
    }
  }

  public void deregisterOnPort() {
    if (serverSocket != null) {
      try {
        serverSocket.close();
        serverSocket = null;
      } catch (IOException e) {
        System.err.println(e);
      }
    }
  }

  /**
  *
  * Waits for the next client to connect on port number portNumber or takes the
  * next one in line in case a client is already trying to connect. Returns the
  * socket of the connection, null if there were any failures.
  */
  protected Socket waitForConnectionFromClient() {
    Socket res = null;
    try {
      res = serverSocket.accept();
    } catch (IOException e) {
      // We return null on IOExceptions
    }
    return res;
  }

  //Added in exercise 2
  public void listenForInputToClient(Socket socket) {

    try {
      // For reading from standard input
      BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
      // For sending text to the server - exercise 2
      PrintWriter toClient = new PrintWriter(socket.getOutputStream(),true);

      new Thread(new Runnable() {
        String s;
        public void run() {
          try {
            while ((s = stdin.readLine()) != null && !toClient.checkError()) {
              toClient.println(s);
            }

          } catch (IOException e) {
            System.err.println(e);
          }
        }
      }).start();
    } catch (IOException e) {
      // We ignore IOExceptions
      System.err.println(e);
    }
  }

  //Added in exercise 3
  public void listenForAnswer(Socket socket, QA qa, ObjectOutputStream objStream) {

      // For reading from standard input
      BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
      // For sending text to the server - exercise 2

      new Thread(new Runnable() {
        String s;
        public void run() {
          try {
            //wait for stdinput. Closes down thread afterwards.
            s = stdin.readLine();
            qa.setAnswer(s);
            objStream.writeObject(qa);
            objStream.flush();
            System.out.println("sent answer!");
          } catch (IOException e) {
            System.err.println(e);
          }
        }
      }).start();
  }

  public void run() {
    System.out.println("Hello world!");

    printLocalHostAddress();

    registerOnPort();

    while (true) {
      Socket socket = waitForConnectionFromClient();

      if (socket != null) {
        System.out.println("Connection from " + socket);
        try {
          //exercise 2
          //listenForInputToClient(socket);
          ObjectOutputStream objStream = new ObjectOutputStream(socket.getOutputStream());
          objStream.flush();
          ObjectInputStream objInput = new ObjectInputStream(socket.getInputStream());
          QA qa;

          //exercise 2
          //BufferedReader fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));

          // Read and print the client's question.
          while ((qa = (QA) objInput.readObject()) != null) { // Ctrl-D terminates the connection
            System.out.println("Received question from client: " + qa.getQuestion());
            System.out.println(toClientText);
            listenForAnswer(socket, qa, objStream);
          }
          socket.close();
        } catch (IOException e) {
          // We report but otherwise ignore IOExceptions
          System.err.println(e);
        } catch (Exception e) {
          System.err.println(e);
        }

        System.out.println("Connection closed by client.");
      } else {
        // We rather agressively terminate the server on the first connection exception
        break;
      }
    }

    deregisterOnPort();

    System.out.println("Goodbuy world!");
  }

  public static void main(String[] args) throws IOException {
    DDistDemoServer server = new DDistDemoServer();
    server.run();
  }

}
