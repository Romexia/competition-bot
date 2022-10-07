package com.gartham.templates.discordbot;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.alixia.javalibrary.parsers.cli.CLIParams;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class BotLauncher {

	public static final long COMPETITION_CHANNEL_ID = 1027732441466683463l, DATA_CHANNEL_ID = 1026917114772197457l,
			PRIOR_VOTES_CHANNEL = 1026929153896890408l, UPVOTE_EMOJI_ID = 1026927534769700874l,
			DOWNVOTE_EMOJI_ID = 1026927919773261835l,
			REQUIRED_ROLE_LIST[] = { 693388028118433844l, 710348685116178484l, 682614550679388181l, 931460874328215602l,
					866395351267541022l, 685192919837311033l, 694388915171229706l, 684897225322528820l,
					694387959780212788l, 684875273061269602l, 688318644358610955l, 682614599899676741l,
					687053687365173339l, 714631960718344223l, 683184911867445281l, 694387959780212788l,
					684897225322528820l, 1026924591572074496l };

	private static CompetitionData competitionData = new CompetitionData();

	public static void main(String[] args) throws LoginException, InterruptedException {
		BotConfiguration config = new BotConfiguration(new CLIParams(args));

		JDA jda = JDABuilder.createDefault(config.getToken(), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
				.setMemberCachePolicy(MemberCachePolicy.ALL).build();
		jda.awaitReady();

		jda.upsertCommand("notifmsg", "Post message to let people know where to subit").queue();

		System.out.println("Connected to Discord. Loading all competition data...");
		var competitionChannel = jda.getTextChannelById(COMPETITION_CHANNEL_ID);
		var dataChannel = jda.getTextChannelById(DATA_CHANNEL_ID);
		var priorVotesChannel = jda.getTextChannelById(PRIOR_VOTES_CHANNEL);
		System.out.println("Acquired channels!");

		System.out.println("Loading all users in " + competitionChannel.getGuild().getName());
		var users = competitionChannel.getGuild().loadMembers().get();
		System.out.println(
				"Loaded: " + users.size() + " users!!! Is server loaded? " + competitionChannel.getGuild().isLoaded());

		// Load up all submissions using the data channel, then load up all prior votes
		// using the prior votes channel.

		// Data channel we can scan in reverse-chronological order (scrolling UP the
		// channel); this is okay.

		System.out.println("\nLoading Data Channel (Submissions)");
		for (Message m : dataChannel.getIterableHistory()) {
			String[] c = m.getContentRaw().split(" ");
			competitionData.getSubmissions().put(c[0], new Submission(c[1], c[0], m.getId()));
		}
		System.out.println("\tComplete");

		System.out.println("Loading prior-votes channel...");
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

			for (ListIterator<Message> iterator = messages.listIterator(messages.size()); iterator.hasPrevious();) {
				// ... split the textual content of `m` by spaces. Store the resulting array of
				// parts into `c`.
				String[] c = iterator.previous().getContentRaw().split(" ");// Returns an array. If the message, for
																			// some reason, has 0
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
		System.out.println("\tComplete!");

		System.out.println("Current Rankings: ");
		Map<Submission, Integer> count = new HashMap<>();
		for (var v : competitionData.getSubmissions().entrySet())
			count.put(v.getValue(), 0);
		for (var v : competitionData.getAuthorVotes().entrySet())
			for (var c : competitionData.getSubmissions().entrySet())
				if (c.getValue().getSubmissionDataMsgID().equals(v.getValue()))
					count.put(c.getValue(), count.get(c.getValue()) + 1);
		for (var v : count.entrySet())
			System.out.println("\t<@" + v.getKey().getAuthorID() + ">: " + v.getValue());

		jda.addEventListener(new EventListener() {
			@Override
			public void onEvent(GenericEvent event) {

				if (event instanceof MessageReceivedEvent) {
					var e = (MessageReceivedEvent) event;
					if (e.getAuthor().isBot())
						return;
					if (e.getChannelType() == ChannelType.PRIVATE) {
						Guild g = competitionChannel.getGuild();
						if (!g.isMember(e.getAuthor())) {
							e.getMessage().reply("You have to be in Gartham's Server to be able to run that command.")
									.queue();
							return;
						}
						var roles = g.getMember(e.getAuthor()).getRoles();
						BLK: {
							for (var v : roles)
								for (var c : REQUIRED_ROLE_LIST)
									if (v.getIdLong() == c)
										break BLK;
							e.getMessage().reply(
									"You need to have at least Member to be able to participate in competition events.")
									.queue();
							return;
						}
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
								new Submission(competitionMessage.getId(), e.getAuthor().getId(), dataMessage.getId()));
						e.getMessage().reply("Congratulations! Your submission is complete. Check the "
								+ competitionChannel.getAsMention() + '.').queue();
					}
				} else if (event instanceof ButtonInteractionEvent) {
					var e = (ButtonInteractionEvent) event;
					if (e.getComponentId().startsWith("V-") && e.getChannel().getIdLong() == COMPETITION_CHANNEL_ID) {
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
