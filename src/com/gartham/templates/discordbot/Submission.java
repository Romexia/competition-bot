package com.gartham.templates.discordbot;

public class Submission {
	private final String submissionMessageID, authorID;
	private int voteCount;

	public Submission(String submissionMessageID, String authorID) {
		this.submissionMessageID = submissionMessageID;
		this.authorID = authorID;
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
