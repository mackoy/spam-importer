package de.mackoy.spamimporter;

public class LearnSpamConfig {
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getFolderName() {
		return folderName;
	}
	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}
	public String getSaToolPath() {
		return saToolPath;
	}
	public void setSaToolPath(String saToolPath) {
		this.saToolPath = saToolPath;
	}
	
	public boolean isDeleteMsg() {
		return deleteMsg;
	}
	public void setDeleteMsg(boolean deleteMsg) {
		this.deleteMsg = deleteMsg;
	}
	public boolean isLearnMsg() {
		return learnMsg;
	}
	public void setLearnMsg(boolean learnMsg) {
		this.learnMsg = learnMsg;
	}
	public boolean isLearnAttachedMsgs() {
		return learnAttachedMsgs;
	}
	public void setLearnAttachedMsgs(boolean learnAttachedMsgs) {
		this.learnAttachedMsgs = learnAttachedMsgs;
	}
	
	public boolean isLogSAToolOutput() {
		return logSAToolOutput;
	}
	public void setLogSAToolOutput(boolean logSAToolOutput) {
		this.logSAToolOutput = logSAToolOutput;
	}
	
	public Integer getMessageBatchCount() {
		return messageBatchCount;
	}
	public void setMessageBatchCount(Integer messageBatchCount) {
		this.messageBatchCount = messageBatchCount;
	}
	
	private String username;
	private String password;
	private String host;
	private String folderName;
	private String saToolPath;
	
	private boolean deleteMsg;
	private boolean learnMsg;
	private boolean learnAttachedMsgs;
	
	private boolean logSAToolOutput;
	
	private Integer messageBatchCount;
}
