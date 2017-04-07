import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

public class QuestionHandler implements Runnable {

  private Socket s;
  private LinkedBlockingQueue<QAWrapper> questionQueue;


  public QuestionHandler(Socket s, LinkedBlockingQueue<QAWrapper> questionQueue) {
    this.s = s;
    this.questionQueue = questionQueue;
  }

  public void run() {
    try {
      listenForNewQuestion(s);
      listenForAnswer();
    } catch (IOException e) {
      System.err.println(e);
    }
  }

  public void listenForAnswer() {
    BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      QAWrapper qaWrapper = null;
      try {
        //blocks until retrieved
        qaWrapper = questionQueue.take();
      } catch (Exception e1) {
        e1.printStackTrace();
      }
      QA qa = qaWrapper.qa;
      Socket socket = qaWrapper.socket;
      System.out.print("Received question: " + qa.getQuestion() +
      "\nType an answer and press ENTER> ");
      try {
        ObjectOutputStream objOutput = qaWrapper.outputStream;
        //wait for input from terminal. blocks.
        String s = stdin.readLine();
        qa.setAnswer(s);
        objOutput.writeObject(qa);
        objOutput.flush();
      } catch (IOException e) {
        System.err.println(e);
      }
    }
  }


  // Adding new questions to the queue
  public void listenForNewQuestion(Socket socket) throws IOException {
    new Thread(new Runnable() {
      public void run(){
        while(true){
          if(socket != null){
            try{
              ObjectInputStream objInput = new ObjectInputStream(socket.getInputStream());
              ObjectOutputStream objOutput = new ObjectOutputStream(socket.getOutputStream());
              QA qa;
              // checks if there is any new questions to be added to the que
              while((qa = (QA) objInput.readObject()) != null){
                QAWrapper qaWrapper = new QAWrapper(qa, socket, objOutput, objInput);
                questionQueue.add(qaWrapper);
              }
            } catch (Exception e) {
              System.out.println("Closing connection with " + socket);
              break;
            }
          } else{
            break;
          }
        }
      }
    }).start();
  }
}
