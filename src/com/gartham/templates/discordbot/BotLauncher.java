package com.gartham.templates.discordbot;

import javax.security.auth.login.LoginException;

import org.alixia.javalibrary.parsers.cli.CLIParams;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class BotLauncher {

	public static final long COMPETITION_CHANNEL_ID = 1026917081570099220l, DATA_CHANNEL_ID = 1026917114772197457l,
			PRIOR_VOTES_CHANNEL = 1026929153896890408l, UPVOTE_EMOJI_ID = 1026927534769700874l,
			DOWNVOTE_EMOJI_ID = 1026927919773261835l;

	private static CompetitionData competitionData = new CompetitionData();

	public static void main(String[] args) throws LoginException, InterruptedException {
		BotConfiguration config = new BotConfiguration(new CLIParams(args));

		JDA jda = JDABuilder.createDefault(config.getToken(), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
				.build();
		jda.awaitReady();

		System.out.println("Connected to Discord. Loading all competition data...");
		var competitionChannel = jda.getTextChannelById(COMPETITION_CHANNEL_ID);
		var dataChannel = jda.getTextChannelById(DATA_CHANNEL_ID);
		var priorVotesChannel = jda.getTextChannelById(PRIOR_VOTES_CHANNEL);

		// Load up all submissions using the data channel, then load up all prior votes
		// using the prior votes channel.

		for (Message m : dataChannel.getIterableHistory()) {
			String[] c = m.getContentRaw().split(" ");
			// Message ID in the data channel (m.getId()) is the ticket number. C[1] is the
			// submission ID, and c[0] is the author ID. We picked this format for the data
			// channel's messages.
			competitionData.getSubmissions().put(m.getId(), new Submission(c[1], c[0]));
		}

		for (Message m : priorVotesChannel.getIterableHistory()) {
			String[] c = m.getContentRaw().split(" ");
			if (c.length == 3) {
				competitionData.getAuthorVotes().remove(c[1]);
			} else {
				// Messages sent in the prior votes channel are Author ID : Ticket #. So if user
				// with ID 12345 authors a vote for the message with ID 09876, then the message
				// will say:
				// 12345 09876
				competitionData.getAuthorVotes().put(c[0], c[1]);
			}
		}

		jda.addEventListener(new EventListener() {
			@Override
			public void onEvent(GenericEvent event) {

				if (event instanceof MessageReceivedEvent) {
					var e = (MessageReceivedEvent) event;
					if (e.getChannelType() == ChannelType.PRIVATE) {
						if (competitionData.getSubmissions().containsKey(e.getAuthor().getId())) {
							e.getMessage().reply(
									"You've already submitted a piece for this competition! You can't submit another. :(")
									.queue();
						}
						if (e.getMessage().getAttachments().size() != 1) {
							e.getMessage().reply(
									"You need to send exactly 1 media attachment. (Make sure that you're uploading the attachment, and that it is not a link.)")
									.queue();
							return;
						}

						var dataMessage = dataChannel.sendMessage("TEMPORARY").complete();
						var competitionMessage = competitionChannel
								.sendMessage(e.getMessage().getAttachments().get(0).getProxyUrl())
								.setActionRows(ActionRow.of(Button.success("V-" + dataMessage.getId(), "Upvote!"),
										Button.secondary("R-" + dataMessage.getId(), "Remove Vote")))
								.complete();
						dataMessage.editMessage(e.getAuthor().getAsMention() + ' ' + competitionMessage.getId())
								.queue();

						competitionData.getSubmissions().put(e.getAuthor().getId(),
								new Submission(competitionMessage.getId(), e.getAuthor().getId()));
						e.getMessage().reply("Congratulations! Your submission is complete. Check the "
								+ competitionChannel.getAsMention() + '.').queue();
					}
				} else if (event instanceof ButtonInteractionEvent) {
					var e = (ButtonInteractionEvent) event;
					if (e.getComponentId().startsWith("V-") && e.getChannel().getIdLong() == COMPETITION_CHANNEL_ID) {
						var priorVotesChannel = e.getGuild().getTextChannelById(PRIOR_VOTES_CHANNEL);
						// Author ID : Ticket #
						priorVotesChannel.sendMessage(e.getUser().getId() + ' ' + e.getComponentId().substring(2))
								.complete();
						competitionData.getAuthorVotes().put(e.getUser().getId(), e.getComponentId().substring(2));
					} else if (e.getComponentId().equals("rescind")) {
						if (e.getComponentId().startsWith("R-")
								&& e.getChannel().getIdLong() == COMPETITION_CHANNEL_ID) {
							priorVotesChannel
									.sendMessage(
											"REMOVE: " + e.getUser().getId() + ' ' + e.getComponentId().substring(2))
									.complete();
							competitionData.getAuthorVotes().remove(e.getUser().getId());
						}
					}
				}
			}
		});

	}

}
