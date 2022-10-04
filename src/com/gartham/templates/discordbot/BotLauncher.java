package com.gartham.templates.discordbot;

import javax.security.auth.login.LoginException;

import org.alixia.javalibrary.parsers.cli.CLIParams;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class BotLauncher {

	public static final long COMPETITION_CHANNEL_ID = 1026917081570099220l, DATA_CHANNEL_ID = 1026917114772197457l,
			UPVOTE_EMOJI_ID = 1026927534769700874l, DOWNVOTE_EMOJI_ID = 1026927919773261835l;

	public static void main(String[] args) throws LoginException, InterruptedException {
		BotConfiguration config = new BotConfiguration(new CLIParams(args));

		JDA jda = JDABuilder.createDefault(config.getToken(), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
				.build();
		jda.awaitReady();

		var chan = jda.getTextChannelById(COMPETITION_CHANNEL_ID);
		chan.sendMessage("Extreme, gaming.")
				.setActionRows(
						ActionRow.of(Button.success("upvote", "Upvote"), Button.secondary("rescind", "Remove Upvote")))
				.queue();

		jda.addEventListener(new EventListener() {
			@Override
			public void onEvent(GenericEvent event) {
				if (event instanceof ButtonInteractionEvent) {
					var e = (ButtonInteractionEvent) event;
					if (e.getComponentId().equals("upvote")) {
						// IMPLEMENT User pressed upvote.
					} else if (e.getComponentId().equals("rescind")) {
						// IMPLEMENT User pressed "remove upvote."
					}
				}
			}
		});

	}

}
