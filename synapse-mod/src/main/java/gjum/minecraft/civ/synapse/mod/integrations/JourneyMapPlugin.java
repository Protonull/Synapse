package gjum.minecraft.civ.synapse.mod.integrations;

import gjum.minecraft.civ.synapse.mod.LiteModSynapse;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;

@journeymap.api.v2.client.JourneyMapPlugin(apiVersion = "2.0.0-SNAPSHOT")
public class JourneyMapPlugin implements IClientPlugin {
    public static IClientAPI jmApi;

    @Override
    public void initialize(IClientAPI iClientAPI) {
        jmApi = iClientAPI;
    }

    @Override
    public String getModId() {
        return LiteModSynapse.MOD_NAME;
    }
}
