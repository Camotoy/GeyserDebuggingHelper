package net.camotoy.geyserdebugginghelper;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Config {
    public Bedrock bedrock = new Bedrock();
    public Java java = new Java();

    public class Bedrock extends PacketFilter {
    }

    public class Java extends PacketFilter {
    }

    abstract class PacketFilter {
        public boolean clientboundEnabled = false;
        public boolean serverboundEnabled = false;

        public Set<String> clientboundPacketFilter = new HashSet<>();
        public Set<String> serverboundPacketFilter = new HashSet<>();
    }

    public void write(Gson gson, File path) {
        synchronized (this) {
            try (FileWriter writer = new FileWriter(path)) {
                gson.toJson(this, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
