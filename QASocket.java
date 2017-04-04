import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
// wrapper
public class QASocket{
	public Socket socket;
	public QA qa;
	public ObjectInputStream inputStream;
	public ObjectOutputStream outputStream;

	public QASocket(QA qa, Socket socket, ObjectOutputStream out, ObjectInputStream in){
		this.socket = socket;
		this.qa = qa;
		inputStream = in;
		outputStream = out;
	}
}
