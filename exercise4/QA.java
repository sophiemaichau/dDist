import java.io.Serializable;

public class QA implements Serializable {
    private String question;
    private String answer;

    public void setQuestion(String q) {
	     question = q;
    }

    public String getQuestion() {
	return question;
    }

    public void setAnswer(String q) {
	     answer = q;
    }

    public String getAnswer() {
	     return answer;
    }

    	public String toString() {
    		return "question: " + question + ", answer: " + answer;
    	}

}
