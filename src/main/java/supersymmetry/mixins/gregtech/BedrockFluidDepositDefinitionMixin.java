package supersymmetry.mixins.gregtech;

import supersymmetry.api.BedrockFluidRequirementRegistry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;

import org.jetbrains.annotations.NotNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.WorldProvider;


import gregtech.api.util.GTLog;
import gregtech.api.util.LocalizationUtils;
import gregtech.api.worldgen.bedrockFluids.BedrockFluidVeinHandler;
import gregtech.api.worldgen.config.BedrockFluidDepositDefinition;
import gregtech.api.worldgen.config.WorldConfigUtils;




@Mixin(value = BedrockFluidDepositDefinition.class, remap = false)
public class BedrockFluidDepositDefinitionMixin{


    @Shadow private int weight;
    @Shadow private String assignedName;
    @Shadow private String description;
    @Shadow @Mutable private int[] yields;
    @Shadow private int depletionAmount;
    @Shadow private int depletionChance;
    @Shadow private int depletedYield;
    @Shadow private Fluid storedFluid;
    @Shadow private Function<Biome, Integer> biomeWeightModifier;
    @Shadow private Predicate<WorldProvider> dimensionFilter;
    @Shadow @Mutable private String depositName;


    @Overwrite
    public boolean initializeFromConfig(@NotNull JsonObject configRoot) {
        // the weight value for determining which vein will appear
        this.weight = configRoot.get("weight").getAsInt();
        // the [minimum, maximum) yield of the vein
        this.yields[0] = configRoot.get("yield").getAsJsonObject().get("min").getAsInt();
        this.yields[1] = configRoot.get("yield").getAsJsonObject().get("max").getAsInt();
        // amount of fluid the vein gets depleted by
        this.depletionAmount = configRoot.get("depletion").getAsJsonObject().get("amount").getAsInt();
        // the chance [0, 100] that the vein will deplete by depletionAmount
        this.depletionChance = Math.max(0,
                Math.min(100, configRoot.get("depletion").getAsJsonObject().get("chance").getAsInt()));

        // the fluid which the vein contains
        Fluid fluid = FluidRegistry.getFluid(configRoot.get("fluid").getAsString());
        if (fluid != null) {
            this.storedFluid = fluid;
        } else {
            GTLog.logger.error("Bedrock Fluid Vein {} cannot have a null fluid!", this.depositName,
                    new RuntimeException());
            return false;
        }
        // vein name for JEI display
        if (configRoot.has("name")) {
            this.assignedName = LocalizationUtils.format(configRoot.get("name").getAsString());
        }
        // vein description for JEI display
        if (configRoot.has("description")) {
            this.description = configRoot.get("description").getAsString();
        }
        // yield after the vein is depleted
        if (configRoot.get("depletion").getAsJsonObject().has("depleted_yield")) {
            this.depletedYield = configRoot.get("depletion").getAsJsonObject().get("depleted_yield").getAsInt();
        }
        // additional weighting changes determined by biomes
        if (configRoot.has("biome_modifier")) {
            this.biomeWeightModifier = WorldConfigUtils.createBiomeWeightModifier(configRoot.get("biome_modifier"));
        }
        // filtering of dimensions to determine where the vein can generate
        if (configRoot.has("dimension_filter")) {
            this.dimensionFilter = WorldConfigUtils.createWorldPredicate(configRoot.get("dimension_filter"));
        }
        // ------------------ What the mixin actually adds -----------------
        // adds required fluids to the fluid definition
        if (configRoot.has("required_fluids")) {
            JsonArray array = configRoot.getAsJsonArray("required_fluids");
            List<Fluid> required = new ArrayList<>();
            for (var newInputFluidName : array) {
                Fluid newInputFluid = FluidRegistry.getFluid(newInputFluidName.getAsString());
                if (newInputFluid != null) {
                    required.add(newInputFluid);
                } else {
                   GTLog.logger.warn("Invalid fluid {} in {} definition, skipping.", newInputFluidName.getAsString(), depositName);
                }
            }
            BedrockFluidRequirementRegistry.addRequirement(this.depositName, required);
        }
        BedrockFluidVeinHandler.addFluidDeposit((BedrockFluidDepositDefinition)(Object)this);
        return true;
    }
}