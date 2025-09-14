package supersymmetry.mixins.gregtech;

import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.common.metatileentities.multi.electric.MetaTileEntityFluidDrill;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import supersymmetry.api.BedrockFluidRequirementRegistry;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.Style;
import net.minecraftforge.fluids.Fluid;
import gregtech.api.worldgen.bedrockFluids.BedrockFluidVeinHandler;
import gregtech.api.capability.impl.FluidDrillLogic;

@Mixin(value = MetaTileEntityFluidDrill.class, remap = false)
public abstract class MetaTileEntityFluidDrillMixin extends MultiblockWithDisplayBase {

    public MetaTileEntityFluidDrillMixin(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @SideOnly(Side.CLIENT)
    @Inject(method = "addInformation", at = @At("TAIL"))
    public void addInformation(ItemStack stack, @Nullable World player, @NotNull List<String> tooltip, boolean advanced, CallbackInfo ci) {
        super.addInformation(stack, player, tooltip, advanced);
    }

    @Override
    public boolean getIsWeatherOrTerrainResistant() {
        return true;
    }

    @Override
    public boolean isMultiblockPartWeatherResistant(@Nonnull IMultiblockPart part) {
        return true;
    }

    @Shadow private FluidDrillLogic minerLogic;

    @Inject(method = "addWarningText", at = @At("TAIL"))
    private void addRequiredFluidWarning(List<ITextComponent> textList, CallbackInfo ci) {

        if (minerLogic == null) return;

        Fluid drilledFluid = minerLogic.getDrilledFluid();
        if (drilledFluid == null) return;

        BedrockFluidVeinHandler.FluidVeinWorldEntry entry = BedrockFluidVeinHandler.getFluidVeinWorldEntry(((MetaTileEntityFluidDrill) (Object) this).getWorld(), minerLogic.getChunkX(), minerLogic.getChunkZ());
        if (entry != null && BedrockFluidRequirementRegistry.hasRequirement(entry.getDefinition().getDepositName())) {
            textList.add(new TextComponentString("Not :goog:... multi needs fluid input..")
                    .setStyle(new Style().setColor(TextFormatting.RED)));
        }
    }
}
