package send;

public class NotificationSettings {
	
	private int receiveEmails;
	private int receiveTexts;
	
	public NotificationSettings(int receiveEmails, int receiveTexts){
		this.receiveEmails = receiveEmails;
		this.receiveTexts = receiveTexts;
	}

	public NotificationSettings() {}

	public int getReceiveEmails() {
		return receiveEmails;
	}

	public void setReceiveEmails(int receiveEmails) {
		receiveEmails = receiveEmails;
	}

	public int getReceiveTexts() {
		return receiveTexts;
	}

	public void setReceiveTexts(int receiveTexts) {
		receiveTexts = receiveTexts;
	}
	

}
