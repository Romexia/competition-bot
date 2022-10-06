package com.gartham.templates.discordbot;

public class Submission {
	private final String submissionMessageID, authorID, submissionDataMsgID;
	private int voteCount;

	public Submission(String submissionMessageID, String authorID, String submissionDataMsgID) {
		this.submissionMessageID = submissionMessageID;
		this.authorID = authorID;
		this.submissionDataMsgID = submissionDataMsgID;
	}

	public String getSubmissionDataMsgID() {
		return submissionDataMsgID;
	}

	public int getVoteCount() {
		return voteCount;
	}

	public void setVoteCount(int voteCount) {
		this.voteCount = voteCount;
	}

	public String getSubmissionMessageID() {
		return submissionMessageID;
	}

	public String getAuthorID() {
		return authorID;
	}

}
