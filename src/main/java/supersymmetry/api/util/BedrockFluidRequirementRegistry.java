package supersymmetry.api;

import net.minecraftforge.fluids.Fluid;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class BedrockFluidRequirementRegistry {

    private static final Map<String, List<Fluid>> registry = new HashMap<>();

    public static void addRequirement(String veinName, List<Fluid> requiredFluids) {
        registry.put(veinName, new ArrayList<>(requiredFluids));
    }

    public static boolean hasRequirement(String veinName) {
        return registry.containsKey(veinName) && !registry.get(veinName).isEmpty();
    }

    public static List<Fluid> getRequiredFluids(String veinName) {
        return registry.getOrDefault(veinName, Collections.emptyList());
    }
}