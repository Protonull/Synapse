package gjum.minecraft.civ.synapse.mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JsonConfig {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private static final TypeAdapter<ChatFormatting> typeAdapterTextFormatting = new TypeAdapter<>() {
        @Override
        public void write(JsonWriter out, ChatFormatting value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.getName());
        }

        @Override
        public ChatFormatting read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return ChatFormatting.getByName(in.nextString());
        }
    };

    private static final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .registerTypeAdapter(ChatFormatting.class, typeAdapterTextFormatting)
            .create();

    public static long saveLaterTimeout = 300;
    private long lastSaveTime = 0;
    private final ScheduledExecutorService delayedSaveServicePool = Executors.newScheduledThreadPool(1);

    public File saveLocation;

    protected boolean isLoading = false;

    protected abstract Object getData();

    protected abstract void setData(Object data);

    public void load(@Nullable File file) {
        saveLocation = file != null ? file : saveLocation;
        logger.info("Loading {} from {}", getClass().getSimpleName(), saveLocation);
        try {
            try (FileReader reader = new FileReader(saveLocation)) {
                isLoading = true;
                setData(gson.fromJson(reader, getData().getClass()));
            } finally {
                isLoading = false;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void saveLater(@Nullable File file) {
        saveLocation = file != null ? file : saveLocation;
        final long originalSaveRequestTime = System.currentTimeMillis();
        delayedSaveServicePool.schedule(() -> {
            if (lastSaveTime > originalSaveRequestTime) return; // already saved while waiting
            saveNow(saveLocation);
        }, saveLaterTimeout, TimeUnit.MILLISECONDS);
    }

    public void saveNow(@Nullable File file) {
        saveLocation = file != null ? file : saveLocation;
        if (isLoading) throw new ConcurrentModificationException("Cannot save while loading");
        try {
            logger.info("Saving {} to {}", getClass().getSimpleName(), saveLocation);
            lastSaveTime = System.currentTimeMillis();
            String json = gson.toJson(getData());
            FileOutputStream fos = new FileOutputStream(saveLocation);
            fos.write(json.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
