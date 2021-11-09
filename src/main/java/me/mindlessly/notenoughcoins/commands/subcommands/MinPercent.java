package me.mindlessly.notenoughcoins.commands.subcommands;

import me.mindlessly.notenoughcoins.utils.ConfigHandler;
import me.mindlessly.notenoughcoins.utils.Utils;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.common.config.Configuration;

public class MinPercent implements Subcommand {
    public MinPercent() {
    }

    @Override
    public String getCommandName() {
        return "minpercent";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "<minimum percentage>";
    }

    @Override
    public boolean processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0) {
            int percent;
            try {
                percent = Integer.parseInt(args[0]);
            } catch (Exception e) {
                return false;
            }
            ConfigHandler.writeConfig(Configuration.CATEGORY_GENERAL, "MinPercent", percent);
            Utils.sendMessageWithPrefix("&aMinimum profit percentage set to " + percent, sender);
            return true;
        } else return false;
    }
}
