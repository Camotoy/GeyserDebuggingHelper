package net.camotoy.geyserdebugginghelper;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.extension.command.GeyserExtensionCommand;

public abstract class GeyserDebuggingCommand extends GeyserExtensionCommand {
    public GeyserDebuggingCommand(@NonNull Extension extension, @NonNull String name) {
        super(extension, name, "You know what this does", "geyserdebugginghelper.all", null, false, false);
    }
}
