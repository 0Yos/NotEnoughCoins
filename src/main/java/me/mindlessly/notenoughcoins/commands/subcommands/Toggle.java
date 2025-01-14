package me.mindlessly.notenoughcoins.commands.subcommands;

import me.mindlessly.notenoughcoins.utils.ApiHandler;
import me.mindlessly.notenoughcoins.utils.ConfigHandler;
import me.mindlessly.notenoughcoins.utils.Reference;
import me.mindlessly.notenoughcoins.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Toggle implements Subcommand {
	// Take initial set of lbins, take second set, and use compared set to identify
	// the biggest
	// gainers/losers
	// namedSet is used to replace internal ids with actual item names
	public static LinkedHashMap<String, Double> initialDataset = new LinkedHashMap<>();
	public static LinkedHashMap<String, Double> secondDataset = new LinkedHashMap<>();
	public static LinkedHashMap<String, Double> namedDataset = new LinkedHashMap<>();
	public static LinkedHashMap<String, Double> avgDataset = new LinkedHashMap<>();
	public static LinkedHashMap<String, Integer> demandDataset = new LinkedHashMap<>();
	public static LinkedHashMap<Integer, Long> updatedDataset = new LinkedHashMap<>();
	public static ArrayList<String> ignoredUUIDs = new ArrayList<>();
	public static double purse;
	public static ArrayList<String> commands = new ArrayList<>();
	public static ArrayList<String> rawNames = new ArrayList<>();
	public static ArrayList<Double> percentageProfit = new ArrayList<>();
	private static int auctionPages = 0;
	private static int flipSpeed = 1;
	public static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(flipSpeed);
	public static boolean alertSound = false;
	public static String UUID = "";

	public Toggle() {
	}

	public static void flip(EntityPlayer sender) {
		if (ConfigHandler.getString(Configuration.CATEGORY_GENERAL, "Flip").equals("true")) {
			Utils.sendMessageWithPrefix("&aFlipper alerts enabled.", sender);
			if (ConfigHandler.hasKey(Configuration.CATEGORY_GENERAL, "alertsound")) {
				alertSound = ConfigHandler.getString(Configuration.CATEGORY_GENERAL, "alertsound").equals("true");
			}
			try {
				ApiHandler.getBins(initialDataset);
				ApiHandler.getAuctionAverages(avgDataset, demandDataset);
				ApiHandler.itemIdsToNames(initialDataset);
				UUID = ApiHandler.getUuid(sender.getName());
			} catch (Exception e) {
				Reference.logger.error(e.getMessage(), e);
			}
			auctionPages = ApiHandler.getNumberOfPages() - 1;
			String name = sender.getName();
			String id = ConfigHandler.getString(Configuration.CATEGORY_GENERAL, "APIKey");
			try {
				ApiHandler.updatePurseCoins(id, name);
			} catch (Exception e) {
				sender.addChatMessage(new ChatComponentText("Could not load purse."));
			}
			for (int i = 0; i < flipSpeed; i++) {
				final int start = i;
				Thread thread = new Thread(() -> flipper(sender, start, flipSpeed));
				thread.start();
			}

			scheduledExecutorService.scheduleAtFixedRate(() -> {
				auctionPages = ApiHandler.getNumberOfPages() - 1;
				try {
					ApiHandler.getBins(initialDataset);
					ApiHandler.getAuctionAverages(avgDataset, demandDataset);
					ApiHandler.itemIdsToNames(initialDataset);
				} catch (Exception e) {
					sender.addChatMessage(new ChatComponentText("Could not load BINs."));
				}
				String name1 = sender.getName();
				String id1 = ConfigHandler.getString(Configuration.CATEGORY_GENERAL, "APIKey");
				try {
					ApiHandler.updatePurseCoins(id1, name1);
				} catch (Exception e) {
					sender.addChatMessage(new ChatComponentText("Could not load purse."));
				}
			}, 60000, 60000, TimeUnit.MILLISECONDS);

		} else {
			Utils.sendMessageWithPrefix("&cFlipper alerts disabled.", sender);
			scheduledExecutorService.shutdownNow();
			scheduledExecutorService = Executors.newScheduledThreadPool(flipSpeed);
		}
	}

	public static void flipper(EntityPlayer sender, int start, int increment) {
		for (int iterate = start; iterate < auctionPages; iterate += increment) {
			final int page = iterate;
			scheduledExecutorService.scheduleAtFixedRate(() -> {
				boolean print = ApiHandler.getFlips(secondDataset, page, ignoredUUIDs);
				if (print) {
					if (namedDataset.size() > 0) {
						purse = Math.round(purse);
						int count = 0;
						int demand = 0;
						boolean noSales = false;
						if (demandDataset.containsKey(rawNames.get(count))) {
							demand = demandDataset.get(rawNames.get(count));
						} else {
							noSales = true;
						}

						for (Map.Entry<String, Double> entry : namedDataset.entrySet()) {
							long profit = Math.abs(entry.getValue().longValue());
							Utils.sendMessageWithPrefix("&e" + entry.getKey() + " "
									+ (profit > 200_000 || purse / 5 < 100_000 ? "&a"
									: profit > 100_000 || purse / 5 < 200_000 ? "&6" : "&e")
									+ "+$" + Utils.formatValue(profit) + " &6" + "PP: "
									+ "&a" + percentageProfit.get(count).intValue() + "% "
									+ "&6"
									+ (!noSales ? "Sales: " + "&a" + demand + "/day"
									: ""), new ClickEvent(ClickEvent.Action.RUN_COMMAND, commands.get(count)), sender);
							if (alertSound) {
								SoundHandler soundHandler = Minecraft.getMinecraft().getSoundHandler();
								if (soundHandler != null && Minecraft.getMinecraft().theWorld != null) {
									soundHandler
											.playSound(PositionedSoundRecord.create(new ResourceLocation("note.pling"),
													(float) Minecraft.getMinecraft().thePlayer.posX,
													(float) Minecraft.getMinecraft().thePlayer.posY,
													(float) Minecraft.getMinecraft().thePlayer.posZ));
								}
							}
							count++;
							noSales = false;
						}
					}
				}
				namedDataset.clear();
				commands.clear();
				rawNames.clear();
				percentageProfit.clear();
			}, 100, 100, TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public String getCommandName() {
		return "toggle";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "";
	}

	@Override
	public boolean processCommand(ICommandSender sender, String[] args) {
		if (ConfigHandler.hasKey(Configuration.CATEGORY_GENERAL, "Flip")) {
			if (ConfigHandler.getString(Configuration.CATEGORY_GENERAL, "Flip").equals("true")) {
				ConfigHandler.writeConfig(Configuration.CATEGORY_GENERAL, "Flip", "false");

			} else if (ConfigHandler.getString(Configuration.CATEGORY_GENERAL, "Flip").equals("false")) {
				ConfigHandler.writeConfig(Configuration.CATEGORY_GENERAL, "Flip", "true");
			}

		} else {
			ConfigHandler.writeConfig(Configuration.CATEGORY_GENERAL, "Flip", "true");
		}

		if (ConfigHandler.hasKey(Configuration.CATEGORY_GENERAL, "FlipSpeed")) {
			scheduledExecutorService.shutdownNow();
			flipSpeed = Integer.parseInt(ConfigHandler.getString(Configuration.CATEGORY_GENERAL, "FlipSpeed"));
			scheduledExecutorService = Executors.newScheduledThreadPool(flipSpeed);
		}
		scheduledExecutorService.schedule(() -> flip((EntityPlayer) sender.getCommandSenderEntity()), 0,
				TimeUnit.SECONDS);
		return true;
	}
}
