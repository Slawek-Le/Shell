import java.util.Scanner;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import static java.nio.file.StandardCopyOption.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Shell {
	public static String currentDir = "";

	public static void main(String args[]) {
		new GreetingsIntro();
		String shellDef, shellCommand;
		shellDef = "[Shell]";

		String userSpecialChar = "$";
		currentDir = System.getProperty("user.dir");

		Scanner inputScanner = null;

		while (true) {
			System.out.print(shellDef + " " + currentDir + userSpecialChar);
			if (inputScanner == null) {
				inputScanner = new Scanner(System.in);
			}
			shellCommand = inputScanner.nextLine();

			String[] cmdsplit = shellCommand.split(" ");
			String inputString = "";

			try {
				Runtime runtime = Runtime.getRuntime();
				Process process = null;
				process = runtime.exec(cmdsplit, null, new File(currentDir));

				BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

				while ((inputString = stdInput.readLine()) != null) {
					System.out.println(inputString);
				}
			} catch (Exception ex) {
				if (shellCommand.isEmpty())
					continue;
				if (shellCommand.matches("^\\s*$")) {
					continue;
				}

				new CommandInterpreter(cmdsplit, shellCommand);
			}
		}
	}
}

class GreetingsIntro {
	String greetingsLine1 = "Welcome to my own shell";
	String greetingsLine2 = "Available custom commands by typing help?,\nother recognizable commands also work \ne.g. ls -la";
	String greetingsLine3 = "****************************************";

	public GreetingsIntro() {
		System.out.println(greetingsLine3);
		System.out.println(greetingsLine1);
		System.out.println(greetingsLine2);
		System.out.println(greetingsLine3);
		System.out.println();
	}
}

class CommandInterpreter {
	public CommandInterpreter(String[] cmdsplit, String inputCommand) {
		switch (cmdsplit[0]) {
			case "exit":
				if (cmdsplit.length == 1) {
					System.out.println("bye");
					System.exit(0);
				} else {
					System.out.println("Invalid command: " + inputCommand);
				}
				break;
			case "help?":
				if (cmdsplit.length == 1) {
					new Help();
				} else {
					System.out.println("Invalid command: " + inputCommand);
				}
				break;
			case "cd":
				new ChangeDir(cmdsplit);
				break;
			case "credits":
				if (cmdsplit.length == 1) {
					new Credits();
				} else {
					System.out.println("Invalid command: " + inputCommand);
				}
				break;
			case "time":
				if (cmdsplit.length == 1) {
					new ShowTime();
				} else {
					System.out.println("Invalid command: " + inputCommand);
				}
				break;
			case "move":
				if (cmdsplit.length == 3) {
					new FileManipulation(true, cmdsplit);
				} else {
					System.out.println("Invalid command: " + inputCommand);
				}
				break;
			case "copy":
				if (cmdsplit.length == 3) {
					new FileManipulation(false, cmdsplit);
				} else {
					System.out.println("Invalid command: " + inputCommand);
				}
				break;
			case "delete":
				if (cmdsplit.length == 2) {
					new FileManipulation(cmdsplit);
				} else {
					System.out.println("Invalid command: " + inputCommand);
				}
				break;
			default:
				System.out.println("Invalid command: " + inputCommand);
				break;
		}
	}
}

interface Messages {
	public String message1();
}

class Help implements Messages {
	String message2 = "****************************************";

	public String message1() {
		return "Available commands: exit, cd, credits, copy, move, delete, time";
	}

	public Help() {
		System.out.println(message1());
		System.out.println(message2);
		System.out.println();

	}
}

class Credits implements Messages {
	public String message1() {
		return "****************************************\n\n Made by Slawomir Lesniak \n\n****************************************";
	}

	public Credits() {
		System.out.println(message1());
	}
}

class ChangeDir {
	public ChangeDir(String[] cmdsplit) {
		if (cmdsplit.length < 2)
			return;

		if (((cmdsplit[1].equals("..")) || (cmdsplit[1].equals("."))) && (cmdsplit.length == 2)) {
			previousDir();
		} else if (cmdsplit.length == 2) {
			nextDir(cmdsplit[1]);
		}
	}

	void previousDir() {
		if (Shell.currentDir.split("/").length > 2) {
			String newPath = "";
			for (int i = 0; i < Shell.currentDir.split("/").length - 1; i++) {
				newPath += i == 0 ? "" : "/" + Shell.currentDir.split("/")[i];
			}

			Shell.currentDir = newPath;
		} else {
			Shell.currentDir = "/";
		}
	}

	void nextDir(String path) {
		if (!path.contains("/"))
			path = "/" + path;

		File file = new File(Shell.currentDir + path);
		if (file.isDirectory())
			Shell.currentDir = Shell.currentDir + path;
		else
			System.out.println("wrong directory");
	}
}

class FileManipulation {
	private String[] cmdSplit;

	public FileManipulation(boolean isMoving, String[] cmdSplit) {
		this.cmdSplit = cmdSplit;
		moveOrCopy(isMoving);
	}

	public FileManipulation(String[] cmdSplit) {
		this.cmdSplit = cmdSplit;
		delete();
	}

	private void moveOrCopy(boolean isMoving) {
		File file = new File(cmdSplit[1]);
		File destination = new File(cmdSplit[2]);

		if (!file.exists()) {
			System.out.println("File does not exist");
			return;
		}
		if (!destination.exists() || !destination.isDirectory()) {
			System.out.println("Destination folder does not exist");
			return;
		}

		String[] splitPath = cmdSplit[1].split("/");
		File splitPathFile = new File(cmdSplit[2] + "/" + splitPath[splitPath.length > 0 ? splitPath.length - 1 : 0]);

		if (isMoving) {
			try {
				Files.move(file.toPath(), splitPathFile.toPath());

			} catch (Exception ex) {
				System.out.println("Cannot move file/folder");
				System.out.println(ex.toString());

				return;
			}
			System.out.println("File/folder moved");

		} else {
			try {
				if (file.isDirectory())
					copyFolder(file, splitPathFile, StandardCopyOption.COPY_ATTRIBUTES);
				else {
					ensureParentFolder(splitPathFile);
					Files.copy(file.toPath(), splitPathFile.toPath());
				}
			} catch (Exception ex) {
				System.out.println("Cannot copy file/folder");
				System.out.println(ex.toString());

				return;
			}
			System.out.println("File/folder copied");
		}
	}

	private void copyFolder(File source, File dest, CopyOption options) throws IOException {
		if (!dest.exists())
			dest.mkdirs();

		File[] contents = source.listFiles();
		if (contents != null) {
			for (File file : contents) {
				File newFile = new File(dest.getAbsolutePath() + File.separator + file.getName());
				if (file.isDirectory())
					copyFolder(file, newFile, options);
				else
					copyFile(file, newFile, options);
			}
		}
	}

	private void copyFile(File source, File dest, CopyOption options) throws IOException {
		Files.copy(source.toPath(), dest.toPath(), options);
	}

	private void ensureParentFolder(File file) {
		File parent = file.getParentFile();
		if (parent != null && !parent.exists())
			parent.mkdirs();
	}

	private void delete() {
		if (cmdSplit.length != 2) {
			System.out.println("invalid command");
			return;
		}
		File fileToDelete = new File(cmdSplit[1]);
		if (!fileToDelete.exists()) {
			System.out.println("File/folder does not exist");
		} else {
			if (fileToDelete.isDirectory()) {
				if (fileToDelete.delete()) {
					System.out.println("Folder deleted");

				} else {
					System.out.println("Check folder/folder not empty");
				}

			} else {
				if (fileToDelete.delete()) {
					System.out.println("File deleted");

				} else {
					System.out.println("Cannot delete file");
				}
			}

		}
	}

}

class ShowTime {
	public ShowTime() {
		try {
			do {
				Thread.sleep(1000);
				for (int i = 0; i < 50; i++) {
					System.out.print(" ");
				}

				for (int j = 0; j < 100; j++) {
					System.out.print("\b");
				}

				final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
				dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

				if (System.in.available() == 0)
					System.out.print(dateFormat.format(new Date()));

			} while (System.in.available() == 0);
		} catch (Exception ex) {
			System.out.println("Error showing time");
			System.out.println(ex.toString());
		}

		System.out.println();
	}

}
