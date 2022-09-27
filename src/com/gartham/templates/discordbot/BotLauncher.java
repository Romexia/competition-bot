package com.gartham.templates.discordbot;

import javax.security.auth.login.LoginException;

import org.alixia.javalibrary.parsers.cli.CLIParams;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class BotLauncher {

	public static void main(String[] args) throws LoginException {
		BotConfiguration config = new BotConfiguration(new CLIParams(args));

		JDA jda = JDABuilder.createDefault(config.getToken(), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
				.build();

		// TODO Write out whatever code you need your bot to do after it's logged in.
	}

}
