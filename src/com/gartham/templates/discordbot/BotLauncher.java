package com.gartham.templates.discordbot;

import java.util.List;

import javax.security.auth.login.LoginException;

import org.alixia.javalibrary.parsers.cli.CLIParams;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
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

		// Data channel we can scan in reverse-chronological order (scrolling UP the
		// channel); this is okay.
		for (Message m : dataChannel.getIterableHistory()) {
			String[] c = m.getContentRaw().split(" ");
			competitionData.getSubmissions().put(c[0], new Submission(c[1], c[0]));
		}

		// priorVotesChannel we need to either store extra data (annoying) or scan DOWN
		// the channel. This was fixed in the previous commit. I have modified the code
		// to be more efficient (and work better):

		// This can be simplified even further, but I decided not to do that so that it
		// would be easier for others to interpret.
		// If you can figure out how to simplify this on your own, then go ahead please
		// because I will be able to work with it fine.
		MessageHistory historyObject = priorVotesChannel.getHistory();// Object we use to retrieve "chunks" of previous
																		// messages.
		while (true) {
			// Retrieve the next 100 messages. This retrieves UP TO 100 messages (it may
			// retrieve less than 100 if there are less than 100 left in the channel. SEE
			// DOCS FOR MORE INFO.)
			// Also, the list is in REVERSE CHRONOLOGICAL order when it's returned (SEE DOCS
			// FOR MORE INFO.)
			// That means that the message at the bottom of the channel (the latest message)
			// is in the first slot of this list (index 0). If you go to the next item in
			// the list, you are scrolling up the channel, seeing older messages.
			List<Message> messages = historyObject.retrievePast(100).complete();

			// If there are no more messages in the channel to obtain (we have scanned all
			// of them already), then the method will return an empty list. (SEE DOCS FOR
			// MORE INFO.)
			if (messages.size() == 0)
				break;

			// "For each message, `m`, inside `messages`...
			for (Message m : messages) {
				// ... split the textual content of `m` by spaces. Store the resulting array of
				// parts into `c`.
				String[] c = m.getContentRaw().split(" ");// Returns an array. If the message, for some reason, has 0
															// spaces, the array will have one element.

				// If the array had two spaces, it SHOULD be a message that looks something like
				// this:
				// "REMOVE: 682427503272525862 1027384349647179827"
				// Notice how there are three parts (separated by spaces): "REMOVE:",
				// "682427503272525862", and "1027384349647179827".
				if (c.length == 3)
					competitionData.getAuthorVotes().remove(c[1]);
				else if (c.length == 2)
					competitionData.getAuthorVotes().put(c[0], c[1]);
			}
		}

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
