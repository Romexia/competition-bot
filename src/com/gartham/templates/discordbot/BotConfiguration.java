package com.gartham.templates.discordbot;

import org.alixia.javalibrary.parsers.cli.CLIParams;

public class BotConfiguration {

	/*
	 * Add properties to configure the bot on startup here. These are parsed from
	 * command line arguments by the bot.
	 * 
	 * A couple example arguments are provided in the class. You can delete them if
	 * you'd like.
	 */

	public BotConfiguration(CLIParams cmdLineArguments) {
		token = cmdLineArguments.readString("", "--token");
	}

	private final String token;

	public String getToken() {
		return token;
	}

}
