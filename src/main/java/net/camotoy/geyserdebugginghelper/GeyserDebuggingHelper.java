package net.camotoy.geyserdebugginghelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.bedrock.SessionInitializeEvent;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.extension.command.GeyserExtensionCommand;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringArrayParser;

import javax.naming.Name;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

public class GeyserDebuggingHelper implements Extension {
    private Config config;
    private File configPath;
    private final Gson gson = new GsonBuilder().create();

    @Subscribe
    public void onPreInitialize(GeyserPreInitializeEvent event) {
        configPath = this.dataFolder().resolve("config.json").toFile();
        if (configPath.exists()) {
            try (FileReader reader = new FileReader(configPath)) {
                config = gson.fromJson(reader, Config.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (!Files.exists(this.dataFolder())) {
                try {
                    Files.createDirectories(this.dataFolder());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            this.config = new Config();
            this.config.write(gson, configPath);
        }
    }

    @Subscribe
    public void onDefineCommands(GeyserDefineCommandsEvent event) {
        event.register(new GeyserDebuggingCommand(this, "lookup") {
            @Override
            public void execute(CommandContext<GeyserCommandSource> context) {
                randomChance(context.sender());
                LookupType type = context.get("type");
                int id = context.get("id");
                switch (type) {
                    case ITEM -> {
                        Item item = Registries.JAVA_ITEMS.get(id);
                        if (item == null) {
                            context.sender().sendMessage(Component.text("Not found").color(NamedTextColor.RED));
                            return;
                        }
                        context.sender().sendMessage(Component.text(item.javaIdentifier()).color(NamedTextColor.GREEN));
                    }
                    case BLOCKSTATE -> {
                        BlockState blockState = BlockState.of(id);
                        context.sender().sendMessage(Component.text(blockState.toString()).color(NamedTextColor.BLUE));
                    }
                }
            }

            @Override
            public void register(CommandManager<GeyserCommandSource> manager) {
                manager.command(manager.commandBuilder(name())
                        .required("type", EnumParser.enumParser(LookupType.class))
                        .required("id", IntegerParser.integerParser())
                        .handler(this::execute));
            }
        });

        event.register(new GeyserDebuggingCommand(this, "log") {
            @Override
            public void execute(CommandContext<GeyserCommandSource> context) {

            }

            @Override
            public void register(CommandManager<GeyserCommandSource> manager) {
                var base = manager.commandBuilder(name());
                var bedrock = base.literal("bedrock", "b");
                var bedrockSb = bedrock.literal("serverbound", "sb");
                manager.command(bedrockSb
                        .handler(handler -> {
                            for (GeyserSession session : GeyserImpl.getInstance().onlineConnections()) {
                                if (session.isClosed()) {
                                    continue;
                                }
                                Channel channel = session.getUpstream().getSession().getPeer().getChannel();
                                if (channel.pipeline().get(BEDROCK_SERVERBOUND_LOGGER) != null) {
                                    channel.pipeline().remove(BEDROCK_SERVERBOUND_LOGGER);
                                    handler.sender().sendMessage(Component.text("Disabled serverbound logging.")
                                            .color(NamedTextColor.RED));
                                } else {
                                    addBedrockServerboundListener(channel);
                                }
                            }
                            config.bedrock.serverboundEnabled = !config.bedrock.serverboundEnabled;
                            config.write(gson, configPath);
                        }));
                manager.command(bedrockSb.literal("filteradd")
                        .required("packets", StringArrayParser.stringArrayParser())
                        .handler(handler -> {
                            String[] packets = handler.get("packets");
                            for (String packet : packets) {
                                try {
                                    Class.forName("org.cloudburstmc.protocol.bedrock.packet." + packet);
                                } catch (ClassNotFoundException ignored) {
                                    handler.sender().sendMessage(Component.text("Cannot find packet with name " + packet));
                                    continue;
                                }
                                config.bedrock.serverboundPacketFilter.add(packet);
                                handler.sender().sendMessage(Component.text("Added to filter: " + packet));
                            }
                            config.write(gson, configPath);
                        }));
                manager.command(bedrockSb.literal("filterrm")
                        .required("packets", StringArrayParser.stringArrayParser())
                        .handler(handler -> {
                            String[] packets = handler.get("packets");
                            for (String packet : packets) {
                                if (config.bedrock.serverboundPacketFilter.remove(packet)) {
                                    handler.sender().sendMessage(Component.text("Removed from filter: " + packet));
                                } else {
                                    handler.sender().sendMessage(Component.text("Could not find packet " + packet));
                                }
                            }
                            config.write(gson, configPath);
                        }));
                var java = base.literal("java", "j");
                manager.command(java.literal("serverbound", "sb")
                        .handler(handler -> {

                        }));
            }
        });
    }

    private void addBedrockServerboundListener(Channel channel) {
        channel.pipeline().addBefore(BedrockPeer.NAME, BEDROCK_SERVERBOUND_LOGGER, new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof BedrockPacketWrapper wrapper) {
                    String className = wrapper.getPacket().getClass().getSimpleName();
                    if (!config.bedrock.serverboundPacketFilter.contains(className)) {
                        GeyserApi.api().consoleCommandSource()
                                .sendMessage(ChatColor.toANSI(ChatColor.DARK_GREEN) + wrapper.getPacket() + ChatColor.ANSI_RESET);
                    }
                }
                super.channelRead(ctx, msg);
            }
        });
    }

    @Subscribe
    public void onPlayerJoin(SessionInitializeEvent event) {
        if (config.bedrock.serverboundEnabled) {
            addBedrockServerboundListener(((GeyserSession) event.connection()).getUpstream().getSession().getPeer().getChannel());
        }
    }

    private static final String BEDROCK_SERVERBOUND_LOGGER = "bedrock_serverbound_logger";

    private void randomChance(GeyserCommandSource source) {
        if (ThreadLocalRandom.current().nextInt(100) == 42) {
            source.sendMessage(Component.text("You got this! :)").color(NamedTextColor.DARK_GREEN));
        }
    }

    private enum LookupType {
        BLOCKSTATE,
        ITEM
    }
}
