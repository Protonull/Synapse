package gjum.minecraft.civ.synapse.mod.integrations;

import journeymap.client.api.*;
import journeymap.client.api.event.ClientEvent;

import static gjum.minecraft.civ.synapse.mod.LiteModSynapse.MOD_NAME;

@ClientPlugin
public class JourneyMapPlugin implements IClientPlugin {
	public static IClientAPI jmApi;

	@Override
	public void initialize(IClientAPI iClientAPI) {
		jmApi = iClientAPI;
	}

	@Override
	public String getModId() {
		return MOD_NAME;
	}

	@Override
	public void onEvent(ClientEvent clientEvent) {
	}
}
