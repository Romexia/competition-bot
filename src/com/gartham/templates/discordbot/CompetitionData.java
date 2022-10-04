package com.gartham.templates.discordbot;

import java.util.HashMap;
import java.util.Map;

public class CompetitionData {
	/**
	 * Map of Ticket # to {@link Submission}.
	 */
	private final Map<String, Submission> submissions = new HashMap<>();
	/**
	 * Map of Author ID to Ticket #.
	 */
	private final Map<String, String> authorVotes = new HashMap<>();

	public Map<String, Submission> getSubmissions() {
		return submissions;
	}

	public Map<String, String> getAuthorVotes() {
		return authorVotes;
	}

}
