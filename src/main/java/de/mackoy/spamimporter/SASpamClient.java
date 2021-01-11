package de.mackoy.spamimporter;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SASpamClient {
	
	static Logger LOGGER = LoggerFactory.getLogger(SASpamClient.class);
	
	public SASpamClient() {
		
	}

	public int learnMessageAsSpam(String saToolPath, File file, boolean logOutput) {
		ArrayList<String> commands = new ArrayList<String>();
		commands.add(saToolPath);
		commands.add("--no-sync");
		commands.add("--spam");
		commands.add(file.getAbsolutePath());
		
		LOGGER.debug("Executing: {}", commands);
		
		ProcessBuilder builder = new ProcessBuilder();
		builder.command(commands);
		int exitCode = -1;
		try {
			Process process = builder.start();
			if (logOutput) {
				StreamGobbler streamGobbler = 
						new StreamGobbler(process.getInputStream(), LOGGER::info);
				Executors.newSingleThreadExecutor().submit(streamGobbler);
			}
			exitCode = process.waitFor();
		} catch (Exception e) {
			LOGGER.error("Cannot execute SA Learntool: {}", e.getMessage());
		}
		
		return exitCode;
	}

	public int syncSpamDB(String saToolPath, boolean logOutput) {
		ArrayList<String> commands = new ArrayList<String>();
		commands.add(saToolPath);
		commands.add("--sync");

		ProcessBuilder builder = new ProcessBuilder();
		builder.command(commands);
		int exitCode = -1;
		try {
			Process process = builder.start();
			if (logOutput) {
				StreamGobbler streamGobbler = 
						new StreamGobbler(process.getInputStream(), LOGGER::info);
				Executors.newSingleThreadExecutor().submit(streamGobbler);
			}
			exitCode = process.waitFor();
		} catch (Exception e) {
			LOGGER.error("Cannot execute SA db sync: {}", e.getMessage());
		}
		
		return exitCode;
	}
}