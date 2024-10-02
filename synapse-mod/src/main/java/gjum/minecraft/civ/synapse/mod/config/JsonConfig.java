package gjum.minecraft.civ.synapse.mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.util.text.TextFormatting;
import org.jetbrains.annotations.Nullable;

public abstract class JsonConfig {
	private static final TypeAdapter<TextFormatting> typeAdapterTextFormatting = new TypeAdapter<TextFormatting>() {
		@Override
		public void write(JsonWriter out, TextFormatting value) throws IOException {
			if (value == null) {
				out.nullValue();
				return;
			}
			out.value(value.getFriendlyName());
		}

		@Override
		public TextFormatting read(JsonReader in) throws IOException {
			if (in.peek() == JsonToken.NULL) {
				in.nextNull();
				return null;
			}
			return TextFormatting.getValueByName(in.nextString());
		}
	};

	private static final Gson gson = new GsonBuilder()
			.excludeFieldsWithoutExposeAnnotation()
			.setPrettyPrinting()
			.registerTypeAdapter(TextFormatting.class, typeAdapterTextFormatting)
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
		System.out.println("Loading " + this.getClass().getSimpleName() + " from " + saveLocation);
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
			LiteLoaderLogger.info("Saving " + this.getClass().getSimpleName() + " to " + saveLocation);
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
