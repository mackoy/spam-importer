package de.mackoy.spamimporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.config.ConfigurationFactory;
import io.bootique.meta.application.CommandMetadata;

public class LearnSpamCommand extends CommandWithMetadata {

	static Logger LOGGER = LoggerFactory.getLogger(LearnSpamCommand.class);


	private static CommandMetadata createMetadata() {
		return CommandMetadata.builder(LearnSpamCommand.class)
				.description("Loads messages from IMAP account and learns it on spam assassin")
				.build();
	}

	@Inject
	private Provider<ConfigurationFactory> configFactory;

	public LearnSpamCommand() {
		super(createMetadata());
	}

	@Override
	public CommandOutcome run(Cli arg0) {
		LearnSpamConfig config = configFactory.get().config(LearnSpamConfig.class, "my");
		LOGGER.info("LearnSpamCommand executed! {}", config.getUsername());

		Session session = Session.getInstance(System.getProperties());
		Store store = null;
		try {
			SASpamClient spamClient = new SASpamClient();

			store = session.getStore("imaps");
			store.connect(config.getHost(), config.getUsername(), config.getPassword());
			Folder inbox = store.getFolder(config.getFolderName());
			inbox.open(Folder.READ_WRITE);

			int msgCount = inbox.getMessageCount();
			LOGGER.info("{} messages in folder {}", msgCount, config.getFolderName());
			int maxBatchCount = config.getMessageBatchCount() != null ? config.getMessageBatchCount() : 100;
			int maxCount = msgCount > maxBatchCount ? maxBatchCount : msgCount;

			int learnedMsgCount = 0;
			for (int i = 0; i<maxCount; i++) {
				int index = i+1;
				MimeMessage msg = (MimeMessage) inbox.getMessage(index);
				if (msg.getFlags().contains(Flags.Flag.DELETED)) {
					LOGGER.info("Message is marked as deleted. Ignoring it.");
				} else {
					String subject = msg.getSubject();
					LOGGER.info("Fetching message #{}: {}", index, subject);
					boolean didLearn = processMessage(config, spamClient, msg);
					
					if (didLearn) {
						if (config.isDeleteMsg()) {
							msg.setFlag(Flag.DELETED, true);
							LOGGER.info("Message successfully learned. Deleted it.");
						} else {
							LOGGER.info("Message successfully learned.");
						}
						learnedMsgCount++;
					} else {
						LOGGER.error("Message could not be learned. Do not delete message.");
					}
					
					if (didLearn) {
						learnedMsgCount++;
					}
				}
			}

			if (learnedMsgCount > 0) {
				spamClient.syncSpamDB(config.getSaToolPath(), config.isLogSAToolOutput());
				LOGGER.info("Syncing DB");
			}

			inbox.close(true);

			LOGGER.info("Finished.");
		} catch (Exception e) {
			LOGGER.error("Cannot get messages from imap: {}", e.getMessage());
			e.printStackTrace();
		} finally {
			if (store != null && store.isConnected()) {
				try {
					store.close();
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}

		return CommandOutcome.succeeded();
	}

	private static File writeStreamToTmpFile(Part part) throws Exception {
		File tmpFile = File.createTempFile("mail-", ".msg");
		FileOutputStream fos = new FileOutputStream(tmpFile);
		part.writeTo(fos);
		fos.close();

		return tmpFile;
	}
	
	private boolean processMessage(LearnSpamConfig config, SASpamClient spamClient, MimeMessage msg) {

		boolean didLearnedSpam = false;

		Object content;
		try {
			content = msg.getContent();
			
			if (!didLearnedSpam && config.isLearnAttachedMsgs()) {
				didLearnedSpam = learnMessagePart(config, spamClient, content);
			}

			if (!didLearnedSpam && config.isLearnMsg()) {
				didLearnedSpam = learnMessage(config, spamClient, msg);
			}
		} catch (MessagingException me) {
			didLearnedSpam = learnMessage(config, spamClient, msg);
		} catch (IOException e) {
			didLearnedSpam = learnMessage(config, spamClient, msg);
		}
		
		return didLearnedSpam;
	}

	private static boolean learnMessagePart(LearnSpamConfig config, SASpamClient spamClient, Object content) {
		boolean didLearnedSpam = false;
		try {
			if (content instanceof Multipart) {
				Multipart multi = ((Multipart)content);
				int parts = multi.getCount();

				for (int j=0; j<parts; j++) {
					MimeBodyPart part = (MimeBodyPart)multi.getBodyPart(j);

					if (part.isMimeType("message/rfc822")) {
						MimeMessage partMessage = (MimeMessage) part.getContent();
						String partSubject = partMessage.getSubject();
						LOGGER.info("Processing attached message #{}: {}", j+1, partSubject);
						File partFile = writeStreamToTmpFile(partMessage);
						spamClient.learnMessageAsSpam(config.getSaToolPath(), partFile, config.isLogSAToolOutput());

						if (config.isDeleteMsg()) {
							partFile.delete();
						}

						didLearnedSpam = true;
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Cannot process message part: {}", e.getMessage());
		}

		return didLearnedSpam;
	}

	private static boolean learnMessage(LearnSpamConfig config, SASpamClient spamClient, MimeMessage msg) {
		boolean didLearnedSpam = false;
		try {
			File msgFile = writeStreamToTmpFile(msg);
			spamClient.learnMessageAsSpam(config.getSaToolPath(), msgFile, config.isLogSAToolOutput());

			if (config.isDeleteMsg()) {
				msgFile.delete();
			}
			didLearnedSpam = true;
		} catch (Exception e) {
			LOGGER.error("Cannot process message: {}", e.getMessage());
		}

		return didLearnedSpam;
	}
}
