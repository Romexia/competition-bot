package com.gartham.templates.discordbot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
		
		jda.upsertCommand("notifmsg", "Post message to let people know where to subit").queue();

		System.out.println("Connected to Discord. Loading all competition data...");
		var competitionChannel = jda.getTextChannelById(COMPETITION_CHANNEL_ID);
		var dataChannel = jda.getTextChannelById(DATA_CHANNEL_ID);
		var priorVotesChannel = jda.getTextChannelById(PRIOR_VOTES_CHANNEL);

		// Load up all submissions using the data channel, then load up all prior votes
		// using the prior votes channel.

		for (Message m : dataChannel.getIterableHistory()) {
			String[] c = m.getContentRaw().split(" ");
			competitionData.getSubmissions().put(c[0], new Submission(c[1], c[0]));
		}
		
		List<Message> messageList = new ArrayList<>();
		try {
		    messageList = priorVotesChannel.getIterableHistory().takeAsync(1000) // Collect 1000 messages
		            .thenApply(list -> list.stream()
		                // use .filter() here to cut down on the amount of messages
		                .collect(Collectors.toList()))
		            .get();
		} catch (InterruptedException e) {
		    e.printStackTrace();
		} catch (ExecutionException e) {
		    e.printStackTrace();
		}

		Collections.reverse(messageList);

		for (Message m : messageList) {
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
		

		// TODO Reverse history scanning.
//		for (Message m : priorVotesChannel.getIterableHistory().takeAsync(Integer.MAX_VALUE)
//				.thenApply((list -> list.stream().collect(Collectors.toList()).get()))) {
//			String[] c = m.getContentRaw().split(" ");
//			if (c.length == 3) {
//				competitionData.getAuthorVotes().remove(c[1]);
//			} else {
//				// Messages sent in the prior votes channel are Author ID : Ticket #. So if user
//				// with ID 12345 authors a vote for the message with ID 09876, then the message
//				// will say:
//				// 12345 09876
//				competitionData.getAuthorVotes().put(c[0], c[1]);
//			}
//		}

		jda.addEventListener(new EventListener() {
			@Override
			public void onEvent(GenericEvent event) {

				if (event instanceof MessageReceivedEvent) {
					var e = (MessageReceivedEvent) event;
					if (e.getAuthor().isBot())
						return;
					if (e.getChannelType() == ChannelType.PRIVATE) {
						if (competitionData.getSubmissions().containsKey(e.getAuthor().getId())) {
							e.getMessage().reply(
									"You've already submitted a piece for this competition! You can't submit another. :(")
									.queue();
							return;
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
						dataMessage.editMessage(e.getAuthor().getId() + ' ' + competitionMessage.getId()).queue();

						competitionData.getSubmissions().put(e.getAuthor().getId(),
								new Submission(competitionMessage.getId(), e.getAuthor().getId()));
						e.getMessage().reply("Congratulations! Your submission is complete. Check the "
								+ competitionChannel.getAsMention() + '.').queue();
					}
				} else if (event instanceof ButtonInteractionEvent) {
					var e = (ButtonInteractionEvent) event;
					if (e.getComponentId().startsWith("V-") && e.getChannel().getIdLong() == COMPETITION_CHANNEL_ID) {
						var priorVotesChannel = e.getGuild().getTextChannelById(PRIOR_VOTES_CHANNEL);

						if (competitionData.getAuthorVotes().containsKey(e.getUser().getId()) && competitionData
								.getAuthorVotes().get(e.getUser().getId()).equals(e.getComponentId().substring(2))) {
							e.reply("You already voted for this message!!!").setEphemeral(true).queue();
							return;
						}

						// Author ID : Ticket #
						priorVotesChannel.sendMessage(e.getUser().getId() + ' ' + e.getComponentId().substring(2))
								.complete();
						competitionData.getAuthorVotes().put(e.getUser().getId(), e.getComponentId().substring(2));
						e.reply("You've voted for this.").setEphemeral(true).queue();
						return;
					} else if (e.getComponentId().startsWith("R-")
							&& e.getChannel().getIdLong() == COMPETITION_CHANNEL_ID) {
						if (!competitionData.getAuthorVotes().containsKey(e.getUser().getId())) {
							e.reply("You don't have a vote placed anywhere yet...").setEphemeral(true).queue();
							return;
						}
						priorVotesChannel
								.sendMessage("REMOVE: " + e.getUser().getId() + ' ' + e.getComponentId().substring(2))
								.complete();
						e.reply("You rescinded your vote.").setEphemeral(true).queue();
						competitionData.getAuthorVotes().remove(e.getUser().getId());
						return;
					}
					e.reply("That isn't for you!").setEphemeral(true).queue();
				}
			}
		});

	}

}
