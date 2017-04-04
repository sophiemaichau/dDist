import java.net.*;
import java.io.*;
import java.io.ObjectInputStream;

/**
*
* A very simple client which will connect to a server, read from a prompt and
* send the text to the server.
*/

public class DDistDemoClient {

  /*
  * Your group should use port number 40HGG, where H is your "hold nummer (1,2 or 3)
  * and GG is gruppe nummer 00, 01, 02, ... So, if you are in group 3 on hold 1 you
  * use the port number 40103. This will avoid the unfortunate situation that you
  * connect to each others servers.
  */
  protected int portNumber = 40404;

  /**
  *
  * Will print out the IP address of the local host on which this client runs.
  */
  protected void printLocalHostAddress() {
    try {
      InetAddress localhost = InetAddress.getLocalHost();
      String localhostAddress = localhost.getHostAddress();
      System.out.println("I'm a client running with IP address " + localhostAddress);
    } catch (UnknownHostException e) {
      System.err.println("Cannot resolve the Internet address of the local host.");
      System.err.println(e);
      System.exit(-1);
    }
  }

  /**
  *
  * Connects to the server on IP address serverName and port number portNumber.
  */
  protected Socket connectToServer(String serverName) {
    Socket res = null;
    try {
      res = new Socket(serverName,portNumber);
    } catch (IOException e) {
      // We return null on IOExceptions
    }
    return res;
  }

  public void run(String serverName) {
    System.out.println("Hello world!");
    System.out.println("Type CTRL-D to shut down the client.");

    printLocalHostAddress();

    Socket socket = connectToServer(serverName);

    if (socket != null) {
      System.out.println("Connected to " + socket);
      try {
        // For reading from standard input - exercise 2
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        //For sending objects to the server
        ObjectOutputStream objOutStream = new ObjectOutputStream(socket.getOutputStream());
        objOutStream.flush();
        ObjectInputStream objInputStream = new ObjectInputStream(socket.getInputStream());
        listenOnServerAnswer(socket, objInputStream);
        listenOnQuestionInput(socket, stdin, objOutStream);

        socket.close();
      } catch (IOException e) {
        System.err.println(e);

      }

    }

    System.out.println("Goodbye world!");
  }

public void listenOnQuestionInput(Socket socket, BufferedReader stdin, ObjectOutputStream objOutStream) throws IOException {
  String s;
  // Read from standard input and send question object to server
  // Ctrl-D terminates the connection
  System.out.print("Type a question and then RETURN> ");
  while ((s = stdin.readLine()) != null) {
    System.out.print("Type a question and then RETURN> ");
    QA qa = new QA();
    qa.setQuestion(s);
    objOutStream.writeObject(qa);
    objOutStream.flush();
  }
}

  //Added in exercise 2
  public void listenOnServerAnswer(Socket socket, ObjectInputStream objInputStream) {
    new Thread(new Runnable() {
      public void run() {
        QA qa;
        try {
          // Read and print what the client is sending
          while ((qa = (QA) objInputStream.readObject()) != null) { // Ctrl-D terminates the connection
            System.out.println("\nAnswer from server: " + qa.getAnswer());
            System.out.print("Type a question and then RETURN> ");
          }

        } catch (IOException e) {
          System.err.println(e);
        } catch (Exception e) {
          System.err.println(e);
        }
      }
    }).start();

  }

  public static void main(String[] args) throws IOException {
    DDistDemoClient client = new DDistDemoClient();
    client.run(args[0]);
  }

}
