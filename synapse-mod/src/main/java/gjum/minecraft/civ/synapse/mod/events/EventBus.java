package gjum.minecraft.civ.synapse.mod.events;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.DeadEvent;
import gjum.minecraft.civ.synapse.mod.network.Client;
import java.util.concurrent.Executor;
import org.jetbrains.annotations.NotNull;

public final class EventBus extends AsyncEventBus {
    public EventBus(
        final @NotNull String identifier,
        final @NotNull Executor executor
    ) {
        super(identifier, executor);
    }

    /**
     * Emits an event on the Minecraft thread.
     *
     * @apiNote This override prevents the event bus from emitting another event if there are no handlers registered
     *          for the emitted event.
     */
    @Override
    public void post(
        final @NotNull Object event
    ) {
        if (!(event instanceof DeadEvent)) {
            super.post(event);
        }
    }

    public interface ConnectionEvent {
        @NotNull Client getConnection();
    }
}
