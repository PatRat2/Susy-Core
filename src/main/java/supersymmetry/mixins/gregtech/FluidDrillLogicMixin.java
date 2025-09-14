package supersymmetry.mixins.gregtech;

import supersymmetry.api.BedrockFluidRequirementRegistry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;

import gregtech.api.capability.impl.FluidDrillLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.common.metatileentities.multi.electric.MetaTileEntityFluidDrill;
import gregtech.api.worldgen.bedrockFluids.BedrockFluidVeinHandler;
import gregtech.api.worldgen.config.BedrockFluidDepositDefinition;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;



@Mixin(value = FluidDrillLogic.class, remap = false)
public abstract class FluidDrillLogicMixin {

    @Shadow @Final private static int MAX_PROGRESS;
    @Shadow private MetaTileEntityFluidDrill metaTileEntity;
    @Shadow private Fluid veinFluid;
    @Shadow private boolean isWorkingEnabled;
    @Shadow private boolean isInventoryFull;
    @Shadow private boolean isActive;
    @Shadow private int progressTime;

    @Shadow protected abstract boolean checkCanDrain();
    @Shadow protected abstract boolean consumeEnergy(boolean simulate);
    @Shadow protected abstract void setActive(boolean active);
    @Shadow protected abstract void setWasActiveAndNeedsUpdate(boolean state);
    @Shadow protected abstract int getFluidToProduce();
    @Shadow protected abstract void depleteVein();
    @Shadow public abstract int getChunkX();
    @Shadow public abstract int getChunkZ();

    private String veinName;


    @Overwrite
    protected boolean acquireNewFluid() {
        this.veinFluid = BedrockFluidVeinHandler.getFluidInChunk(metaTileEntity.getWorld(), getChunkX(), getChunkZ());
        this.veinName = BedrockFluidVeinHandler.getFluidVeinWorldEntry(metaTileEntity.getWorld(), getChunkX(), getChunkZ()).getDefinition().getDepositName();
        return this.veinFluid != null;
    }

    @Overwrite
    public void performDrilling() {
        if (metaTileEntity.getWorld().isRemote) return;

        // Acquire a new fluid if we have none
        if (veinFluid == null || veinName == null) {
            if (!acquireNewFluid()) return;
        }

        // Drill must be enabled
        if (!this.isWorkingEnabled) return;

        // Check if vein requires input fluid using the interface
        if (veinName != null && BedrockFluidRequirementRegistry.hasRequirement(veinName)) {
            return;
        }

        // Check if drilling is possible
        if (!checkCanDrain()) return;

        // Handle energy and active state
        if (!isInventoryFull) {
            consumeEnergy(false);
            if (!this.isActive) setActive(true);
        } else {
            if (this.isActive) setActive(false);
            return;
        }

        // Progress drilling
        progressTime++;
        if (progressTime % MAX_PROGRESS != 0) return;
        progressTime = 0;

        int amount = getFluidToProduce();

        if (metaTileEntity.fillTanks(new FluidStack(veinFluid, amount), true)) {
            metaTileEntity.fillTanks(new FluidStack(veinFluid, amount), false);
            depleteVein();
        } else {
            isInventoryFull = true;
            setActive(false);
            setWasActiveAndNeedsUpdate(true);
        }
    }
}
