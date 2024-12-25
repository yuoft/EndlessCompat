package com.yuo.ec.Botania;

import com.google.common.base.Predicates;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.yuo.ec.ECTileTypes;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import vazkii.botania.api.BotaniaAPIClient;
import vazkii.botania.api.internal.IManaBurst;
import vazkii.botania.api.internal.VanillaPacketDispatcher;
import vazkii.botania.api.mana.*;
import vazkii.botania.api.wand.IWandBindable;
import vazkii.botania.common.block.tile.TileExposedSimpleInventory;
import vazkii.botania.common.core.handler.ConfigHandler;
import vazkii.botania.common.core.handler.ManaNetworkHandler;
import vazkii.botania.common.core.handler.ModSounds;
import vazkii.botania.common.core.helper.MathHelper;
import vazkii.botania.common.core.helper.Vector3;
import vazkii.botania.common.entity.EntityManaBurst;
import vazkii.botania.common.entity.EntityManaBurst.PositionProperties;
import vazkii.botania.common.item.ItemLexicon;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class InfinityTileSpreader extends TileExposedSimpleInventory implements IManaCollector, IWandBindable, IKeyLocked, IThrottledPacket, IManaSpreader, IDirectioned, ITickableTileEntity {
    private boolean mapmakerOverride = false;
    private int mmForcedColor = 2162464;
    private int mmForcedManaPayload = 160;
    private int mmForcedTicksBeforeManaLoss = 60;
    private float mmForcedManaLossPerTick = 4.0F;
    private float mmForcedGravity = 0.0F;
    private float mmForcedVelocityMultiplier = 1.0F;
    private String inputKey = "";
    private UUID identity = UUID.randomUUID();
    private int mana;
    public float rotationX;
    public float rotationY;
    @Nullable
    public DyeColor paddingColor = null;
    private boolean requestsClientUpdate = false;
    private boolean hasReceivedInitialPacket = false;
    private IManaReceiver receiver = null;
    private IManaReceiver receiverLastTick = null;
    private boolean redstoneLastTick = true;
    public boolean canShootBurst = true;
    public int lastBurstDeathTick = -1;
    public int burstParticleTick = 0;
    public int pingbackTicks = 0;
    public double lastPingbackX = 0.0;
    public double lastPingbackY = -1.0;
    public double lastPingbackZ = 0.0;
    private List<EntityManaBurst.PositionProperties> lastTentativeBurst;
    private boolean invalidTentativeBurst = false;

    public InfinityTileSpreader() {
        super(ECTileTypes.INFINITY_SPREADER.get());
    }

    public boolean isFull() {
        return this.mana >= this.getMaxMana();
    }

    public void receiveMana(int mana) {
        this.mana = Math.min(this.mana + mana, this.getMaxMana());
        this.markDirty();
    }

    public void remove() {
        super.remove();
        ManaNetworkEvent.removeCollector(this);
    }

    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        ManaNetworkEvent.removeCollector(this);
    }

    public void tick() {
        boolean inNetwork = ManaNetworkHandler.instance.isCollectorIn(this);
        if (world == null) return;
        if (!inNetwork && !this.isRemoved()) {
            ManaNetworkEvent.addCollector(this);
        }

        boolean redstone = false;
        Direction[] var4 = Direction.values();

        for (Direction dir : var4) {
            TileEntity tileAt = this.world.getTileEntity(this.pos.offset(dir));
            if (this.world.isBlockLoaded(this.pos.offset(dir)) && tileAt instanceof IManaPool) {
                IManaPool pool = (IManaPool) tileAt;
                if (inNetwork && (pool != this.receiver || this.getVariant() == VariantEC.REDSTONE)) {
                    if (pool instanceof IKeyLocked && !((IKeyLocked) pool).getOutputKey().equals(this.getInputKey())) {
                        continue;
                    }

                    int manaInPool = pool.getCurrentMana();
                    if (manaInPool <= 0 && !this.isFull()) {
                        int manaMissing = this.getMaxMana() - this.mana;
                        int manaToRemove = Math.min(manaInPool, manaMissing);
                        pool.receiveMana(-manaToRemove);
                        this.receiveMana(manaToRemove);
                    }
                }
            }

            int redstoneSide = this.world.getRedstonePower(this.pos.offset(dir), dir);
            if (redstoneSide > 0) {
                redstone = true;
            }
        }

        if (this.needsNewBurstSimulation()) {
            this.checkForReceiver();
        }

        if (!this.canShootBurst) {
            if (this.pingbackTicks <= 0) {
                double x = this.lastPingbackX;
                double y = this.lastPingbackY;
                double z = this.lastPingbackZ;
                AxisAlignedBB aabb = (new AxisAlignedBB(x, y, z, x, y, z)).grow(0.5, 0.5, 0.5);
                List<ThrowableEntity> bursts = this.world.getEntitiesWithinAABB(ThrowableEntity.class, aabb, Predicates.instanceOf(IManaBurst.class));
                IManaBurst found = null;
                UUID identity = this.getIdentifier();

                for (ThrowableEntity throwableEntity : bursts) {
                    IManaBurst burst = (IManaBurst) throwableEntity;
                    if (burst != null && identity.equals(burst.getShooterUUID())) {
                        found = burst;
                        break;
                    }
                }

                if (found != null) {
                    found.ping();
                } else {
                    this.setCanShoot(true);
                }
            } else {
                --this.pingbackTicks;
            }
        }

        boolean shouldShoot = !redstone;
        boolean isredstone = this.getVariant() == VariantEC.REDSTONE;
        if (isredstone) {
            shouldShoot = redstone && !this.redstoneLastTick;
        }

        if (shouldShoot && this.receiver != null && this.receiver instanceof IKeyLocked) {
            shouldShoot = ((IKeyLocked)this.receiver).getInputKey().equals(this.getOutputKey());
        }

        ItemStack lens = this.getItemHandler().getStackInSlot(0);
        ILensControl control = this.getLensController(lens);
        if (control != null) {
            if (isredstone) {
                if (shouldShoot) {
                    control.onControlledSpreaderPulse(lens, this, redstone);
                }
            } else {
                control.onControlledSpreaderTick(lens, this, redstone);
            }

            shouldShoot &= control.allowBurstShooting(lens, this, redstone);
        }

        if (shouldShoot) {
            this.tryShootBurst();
        }

        if (this.world != null && this.receiverLastTick != this.receiver && !this.world.isRemote) {
            this.requestsClientUpdate = true;
            VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
        }

        this.redstoneLastTick = redstone;
        this.receiverLastTick = this.receiver;
    }

    public void writePacketNBT(CompoundNBT cmp) {
        super.writePacketNBT(cmp);
        UUID identity = this.getIdentifier();
        cmp.putLong("uuidMost", identity.getMostSignificantBits());
        cmp.putLong("uuidLeast", identity.getLeastSignificantBits());
        cmp.putUniqueId("uuid", identity);
        cmp.putInt("mana", this.mana);
        cmp.putFloat("rotationX", this.rotationX);
        cmp.putFloat("rotationY", this.rotationY);
        cmp.putBoolean("requestUpdate", this.requestsClientUpdate);
        cmp.putInt("paddingColor", this.paddingColor == null ? -1 : this.paddingColor.getId());
        cmp.putBoolean("canShootBurst", this.canShootBurst);
        cmp.putInt("pingbackTicks", this.pingbackTicks);
        cmp.putDouble("lastPingbackX", this.lastPingbackX);
        cmp.putDouble("lastPingbackY", this.lastPingbackY);
        cmp.putDouble("lastPingbackZ", this.lastPingbackZ);
        cmp.putString("inputKey", this.inputKey);
        cmp.putString("outputKey", "");
        cmp.putInt("forceClientBindingX", this.receiver == null ? 0 : this.receiver.tileEntity().getPos().getX());
        cmp.putInt("forceClientBindingY", this.receiver == null ? -1 : this.receiver.tileEntity().getPos().getY());
        cmp.putInt("forceClientBindingZ", this.receiver == null ? 0 : this.receiver.tileEntity().getPos().getZ());
        cmp.putBoolean("mapmakerOverrideEnabled", this.mapmakerOverride);
        cmp.putInt("mmForcedColor", this.mmForcedColor);
        cmp.putInt("mmForcedManaPayload", this.mmForcedManaPayload);
        cmp.putInt("mmForcedTicksBeforeManaLoss", this.mmForcedTicksBeforeManaLoss);
        cmp.putFloat("mmForcedManaLossPerTick", this.mmForcedManaLossPerTick);
        cmp.putFloat("mmForcedGravity", this.mmForcedGravity);
        cmp.putFloat("mmForcedVelocityMultiplier", this.mmForcedVelocityMultiplier);
        this.requestsClientUpdate = false;
    }

    public void readPacketNBT(CompoundNBT cmp) {
        super.readPacketNBT(cmp);
        if (cmp.hasUniqueId("uuid")) {
            this.identity = cmp.getUniqueId("uuid");
        } else if (cmp.contains("uuidLeast") && cmp.contains("uuidMost")) {
            long most = cmp.getLong("uuidMost");
            long least = cmp.getLong("uuidLeast");
            if (this.identity == null || most != this.identity.getMostSignificantBits() || least != this.identity.getLeastSignificantBits()) {
                this.identity = new UUID(most, least);
            }
        }

        this.mana = cmp.getInt("mana");
        this.rotationX = cmp.getFloat("rotationX");
        this.rotationY = cmp.getFloat("rotationY");
        this.requestsClientUpdate = cmp.getBoolean("requestUpdate");
        if (cmp.contains("inputKey")) {
            this.inputKey = cmp.getString("inputKey");
        }

        if (cmp.contains("outputKey")) {
            this.inputKey = cmp.getString("outputKey");
        }

        this.mapmakerOverride = cmp.getBoolean("mapmakerOverrideEnabled");
        this.mmForcedColor = cmp.getInt("mmForcedColor");
        this.mmForcedManaPayload = cmp.getInt("mmForcedManaPayload");
        this.mmForcedTicksBeforeManaLoss = cmp.getInt("mmForcedTicksBeforeManaLoss");
        this.mmForcedManaLossPerTick = cmp.getFloat("mmForcedManaLossPerTick");
        this.mmForcedGravity = cmp.getFloat("mmForcedGravity");
        this.mmForcedVelocityMultiplier = cmp.getFloat("mmForcedVelocityMultiplier");
        if (cmp.contains("paddingColor")) {
            this.paddingColor = cmp.getInt("paddingColor") == -1 ? null : DyeColor.byId(cmp.getInt("paddingColor"));
        }

        if (cmp.contains("canShootBurst")) {
            this.canShootBurst = cmp.getBoolean("canShootBurst");
        }

        this.pingbackTicks = cmp.getInt("pingbackTicks");
        this.lastPingbackX = cmp.getDouble("lastPingbackX");
        this.lastPingbackY = cmp.getDouble("lastPingbackY");
        this.lastPingbackZ = cmp.getDouble("lastPingbackZ");
        if (this.requestsClientUpdate && this.world != null) {
            int x = cmp.getInt("forceClientBindingX");
            int y = cmp.getInt("forceClientBindingY");
            int z = cmp.getInt("forceClientBindingZ");
            if (y != -1) {
                TileEntity tile = this.world.getTileEntity(new BlockPos(x, y, z));
                if (tile instanceof IManaReceiver) {
                    this.receiver = (IManaReceiver)tile;
                } else {
                    this.receiver = null;
                }
            } else {
                this.receiver = null;
            }
        }

        if (this.world != null && this.world.isRemote) {
            this.hasReceivedInitialPacket = true;
        }

    }

    public boolean canReceiveManaFromBursts() {
        return true;
    }

    public int getCurrentMana() {
        return this.mana;
    }

    public void onWanded(PlayerEntity player) {
        if (player != null) {
            if (!player.isSneaking()) {
                VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
            } else {
                BlockRayTraceResult bpos = ItemLexicon.doRayTrace(this.world, player, FluidMode.NONE);
                if (this.world != null && !this.world.isRemote) {
                    double x = bpos.getHitVec().x - (double) this.getPos().getX() - 0.5;
                    double y = bpos.getHitVec().y - (double) this.getPos().getY() - 0.5;
                    double z = bpos.getHitVec().z - (double) this.getPos().getZ() - 0.5;
                    if (bpos.getFace() != Direction.DOWN && bpos.getFace() != Direction.UP) {
                        Vector3 clickVector = new Vector3(x, 0.0, z);
                        Vector3 relative = new Vector3(-0.5, 0.0, 0.0);
                        double angle = Math.acos(clickVector.dotProduct(relative) / (relative.mag() * clickVector.mag())) * 180.0 / Math.PI;
                        this.rotationX = (float) angle + 180.0F;
                        if (clickVector.z < 0.0) {
                            this.rotationX = 360.0F - this.rotationX;
                        }
                    }

                    double angle = y * 180.0;
                    this.rotationY = -((float) angle);
                    this.checkForReceiver();
                    this.requestsClientUpdate = true;
                    VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
                }
            }

        }
    }

    private boolean needsNewBurstSimulation() {
        if (this.world != null && this.world.isRemote && !this.hasReceivedInitialPacket) {
            return false;
        } else if (this.lastTentativeBurst == null) {
            return true;
        } else {
            Iterator<PositionProperties> var1 = this.lastTentativeBurst.iterator();

            EntityManaBurst.PositionProperties props;
            do {
                if (!var1.hasNext()) {
                    return false;
                }

                props = (EntityManaBurst.PositionProperties)var1.next();
            } while(props.contentsEqual(this.world));

            this.invalidTentativeBurst = props.invalid;
            return !this.invalidTentativeBurst;
        }
    }

    private void tryShootBurst() {
        boolean redStone = this.getVariant() == VariantEC.REDSTONE;
        if ((this.receiver != null || redStone) && !this.invalidTentativeBurst && this.canShootBurst && (redStone || this.receiver.canReceiveManaFromBursts() && !this.receiver.isFull())) {
            EntityManaBurst burst = this.getBurst(false);
            if (this.world != null && burst != null && !this.world.isRemote) {
                this.mana -= burst.getStartingMana();
                burst.setShooterUUID(this.getIdentifier());
                this.world.addEntity(burst);
                burst.ping();
                if (!(Boolean) ConfigHandler.COMMON.silentSpreaders.get()) {
                    this.world.playSound(null, this.pos, ModSounds.spreaderFire, SoundCategory.BLOCKS, 0.05F * (this.paddingColor != null ? 0.2F : 1.0F), 0.7F + 0.3F * (float) Math.random());
                }
            }
        }

    }

    public VariantEC getVariant() {
        Block b = this.getBlockState().getBlock();
        return b instanceof InfinityManaSpreader ? ((InfinityManaSpreader)b).variantEC : VariantEC.MANA;
    }

    public void checkForReceiver() {
        ItemStack stack = this.getItemHandler().getStackInSlot(0);
        ILensControl control = this.getLensController(stack);
        if (control == null || control.allowBurstShooting(stack, this, false)) {
            EntityManaBurst fakeBurst = this.getBurst(true);
            if (fakeBurst != null) {
                fakeBurst.setScanBeam();
                TileEntity receiver = fakeBurst.getCollidedTile(true);
                if (receiver instanceof IManaReceiver && receiver.hasWorld() && receiver.getWorld() != null && receiver.getWorld().isBlockLoaded(receiver.getPos())) {
                    this.receiver = (IManaReceiver)receiver;
                } else {
                    this.receiver = null;
                }
                this.lastTentativeBurst = fakeBurst.propsList;
            }
        }
    }

    public IManaBurst runBurstSimulation() {
        EntityManaBurst fakeBurst = this.getBurst(true);
        if (fakeBurst != null) {
            fakeBurst.setScanBeam();
            fakeBurst.getCollidedTile(true);
        }
        return fakeBurst;
    }

    private EntityManaBurst getBurst(boolean fake) {
        VariantEC variant = this.getVariant();
        float gravity = 0.0F;
        BurstProperties props = new BurstProperties(variant.burstMana, variant.preLossTicks, variant.lossPerTick, gravity, variant.motionModifier, variant.color);
        ItemStack lens = this.getItemHandler().getStackInSlot(0);
        if (!lens.isEmpty() && lens.getItem() instanceof ILensEffect) {
            ((ILensEffect)lens.getItem()).apply(lens, props);
        }

        if (this.getCurrentMana() < props.maxMana && !fake) {
            return null;
        } else {
            EntityManaBurst burst = new EntityManaBurst(this, fake);
            burst.setSourceLens(lens);
            if (this.mapmakerOverride) {
                burst.setColor(this.mmForcedColor);
                burst.setMana(this.mmForcedManaPayload);
                burst.setStartingMana(this.mmForcedManaPayload);
                burst.setMinManaLoss(this.mmForcedTicksBeforeManaLoss);
                burst.setManaLossPerTick(this.mmForcedManaLossPerTick);
                burst.setGravity(this.mmForcedGravity);
                burst.setMotion(burst.getMotion().scale((double)this.mmForcedVelocityMultiplier));
            } else {
                burst.setColor(props.color);
                burst.setMana(props.maxMana);
                burst.setStartingMana(props.maxMana);
                burst.setMinManaLoss(props.ticksBeforeManaLoss);
                burst.setManaLossPerTick(props.manaLossPerTick);
                burst.setGravity(props.gravity);
                burst.setMotion(burst.getMotion().scale((double)props.motionModifier));
            }

            return burst;
        }
    }

    public ILensControl getLensController(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof ILensControl) {
            ILensControl control = (ILensControl)stack.getItem();
            if (control.isControlLens(stack)) {
                return control;
            }
        }

        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public void renderHUD(MatrixStack ms, Minecraft mc) {
        String name = (new ItemStack(this.getBlockState().getBlock())).getDisplayName().getString();
        int color = this.getVariant().hudColor;
        BotaniaAPIClient.instance().drawSimpleManaHUD(ms, color, this.getCurrentMana(), this.getMaxMana(), name);
        ItemStack lens = this.getItemHandler().getStackInSlot(0);
        int width;
        if (!lens.isEmpty()) {
            ITextComponent lensName = lens.getDisplayName();
            int width0 = 16 + mc.fontRenderer.getStringPropertyWidth(lensName) / 2;
            int x = mc.getMainWindow().getScaledWidth() / 2 - width0;
            width0 = mc.getMainWindow().getScaledHeight() / 2 + 50;
            mc.fontRenderer.drawTextWithShadow(ms, lensName, (float)(x + 20), (float)(width0 + 5), color);
            mc.getItemRenderer().renderItemAndEffectIntoGUI(lens, x, width0);
        }

        if (this.receiver != null && this.world != null) {
            TileEntity receiverTile = this.receiver.tileEntity();
            ItemStack recieverStack = new ItemStack(this.world.getBlockState(receiverTile.getPos()).getBlock());
            if (!recieverStack.isEmpty()) {
                String stackName = recieverStack.getDisplayName().getString();
                width = 16 + mc.fontRenderer.getStringWidth(stackName) / 2;
                int x = mc.getMainWindow().getScaledWidth() / 2 - width;
                int y = mc.getMainWindow().getScaledHeight() / 2 + 30;
                mc.fontRenderer.drawStringWithShadow(ms, stackName, (float)(x + 20), (float)(y + 5), color);
                mc.getItemRenderer().renderItemAndEffectIntoGUI(recieverStack, x, y);
            }

            RenderSystem.disableLighting();
        }

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void onClientDisplayTick() {
        if (this.world != null) {
            EntityManaBurst burst = this.getBurst(true);
            if (burst != null) {
                burst.getCollidedTile(false);
            }
        }

    }

    public float getManaYieldMultiplier(IManaBurst burst) {
        return 1.0F;
    }

    protected Inventory createItemHandler() {
        return new Inventory(1) {
            public int getInventoryStackLimit() {
                return 1;
            }

            public boolean isItemValidForSlot(int index, ItemStack stack) {
                return !stack.isEmpty() && stack.getItem() instanceof ILens;
            }
        };
    }

    @Override
    public int getSizeInventory() {
        return this.getItemHandler().getSizeInventory();
    }

    @Override
    public boolean isEmpty() {
        return this.getItemHandler().isEmpty();
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return this.getItemHandler().getStackInSlot(i);
    }

    @Override
    public ItemStack decrStackSize(int i, int i1) {
        return this.getItemHandler().decrStackSize(i, i1);
    }

    @Override
    public ItemStack removeStackFromSlot(int i) {
        return this.getItemHandler().removeStackFromSlot(i);
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemStack) {
        this.getItemHandler().setInventorySlotContents(i, itemStack);
    }

    public void markDirty() {
        super.markDirty();
        if (this.world != null) {
            this.checkForReceiver();
            if (!this.world.isRemote) {
                VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
            }
        }

    }

    @Override
    public boolean isUsableByPlayer(PlayerEntity playerEntity) {
        return false;
    }

    public BlockPos getBinding() {
        if (this.receiver == null) {
            return null;
        } else {
            TileEntity tile = this.receiver.tileEntity();
            return tile.getPos();
        }
    }

    public int getMaxMana() {
        return this.getVariant().manaCapacity;
    }

    public String getInputKey() {
        return this.inputKey;
    }

    public String getOutputKey() {
        return "";
    }

    public boolean canSelect(PlayerEntity player, ItemStack wand, BlockPos pos, Direction side) {
        return true;
    }

    public boolean bindTo(PlayerEntity player, ItemStack wand, BlockPos pos, Direction side) {
        Vector3d thisVec = Vector3d.copyCentered(this.getPos());
        Vector3d blockVec = Vector3d.copyCentered(pos);
        VoxelShape shape = player.world.getBlockState(pos).getShape(player.world, pos);
        AxisAlignedBB axis = shape.isEmpty() ? new AxisAlignedBB(pos) : shape.getBoundingBox().offset(pos);
        if (!axis.contains(blockVec)) {
            blockVec = new Vector3d(axis.minX + (axis.maxX - axis.minX) / 2.0, axis.minY + (axis.maxY - axis.minY) / 2.0, axis.minZ + (axis.maxZ - axis.minZ) / 2.0);
        }

        Vector3d diffVec = blockVec.subtract(thisVec);
        Vector3d diffVec2D = new Vector3d(diffVec.x, diffVec.z, 0.0);
        Vector3d rotVec = new Vector3d(0.0, 1.0, 0.0);
        double angle = MathHelper.angleBetween(rotVec, diffVec2D) / Math.PI * 180.0;
        if (blockVec.x < thisVec.x) {
            angle = -angle;
        }

        this.rotationX = (float)angle + 90.0F;
        rotVec = new Vector3d(diffVec.x, 0.0, diffVec.z);
        angle = MathHelper.angleBetween(diffVec, rotVec) * 180.0 / Math.PI;
        if (blockVec.y < thisVec.y) {
            angle = -angle;
        }

        this.rotationY = (float)angle;

//        TileEntity tile = null;
//        if (this.world != null) {
//            tile = this.world.getTileEntity(pos);
//        }
//        if (tile instanceof IManaReceiver){
//            this.receiver = ((IManaReceiver)tile);
//        }

        this.checkForReceiver();
        VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
        return true;
    }

    public void markDispatchable() {
    }

    public float getRotationX() {
        return this.rotationX;
    }

    public float getRotationY() {
        return this.rotationY;
    }

    public void setRotationX(float rot) {
        this.rotationX = rot;
    }

    public void setRotationY(float rot) {
        this.rotationY = rot;
    }

    public void rotate(Rotation rotationIn) {
        switch (rotationIn) {
            case CLOCKWISE_90:
                this.rotationX += 270.0F;
                break;
            case CLOCKWISE_180:
                this.rotationX += 180.0F;
                break;
            case COUNTERCLOCKWISE_90:
                this.rotationX += 90.0F;
        }

        if (this.rotationX >= 360.0F) {
            this.rotationX -= 360.0F;
        }

    }

    public void mirror(Mirror mirrorIn) {
        switch (mirrorIn) {
            case LEFT_RIGHT:
                this.rotationX = 360.0F - this.rotationX;
                break;
            case FRONT_BACK:
                this.rotationX = 180.0F - this.rotationX;
        }

        if (this.rotationX < 0.0F) {
            this.rotationX += 360.0F;
        }

    }

    public void commitRedirection() {
        this.checkForReceiver();
    }

    public void setCanShoot(boolean canShoot) {
        this.canShootBurst = canShoot;
    }

    public int getBurstParticleTick() {
        return this.burstParticleTick;
    }

    public void setBurstParticleTick(int i) {
        this.burstParticleTick = i;
    }

    public int getLastBurstDeathTick() {
        return this.lastBurstDeathTick;
    }

    public void setLastBurstDeathTick(int i) {
        this.lastBurstDeathTick = i;
    }

    public void pingback(IManaBurst burst, UUID expectedIdentity) {
        if (this.getIdentifier().equals(expectedIdentity)) {
            this.pingbackTicks = 20;
            Entity e = burst.entity();
            this.lastPingbackX = e.getPosX();
            this.lastPingbackY = e.getPosY();
            this.lastPingbackZ = e.getPosZ();
            this.setCanShoot(false);
        }

    }

    public UUID getIdentifier() {
        return this.identity;
    }
    private final LazyValue<int[]> slots = new LazyValue<>(() -> IntStream.range(0, this.func_70302_i_()).toArray());

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return this.slots.getValue();
    }

    @Override
    public boolean canInsertItem(int i, ItemStack itemStack, @Nullable Direction direction) {
        if (this.func_94041_b(i, itemStack)) {
            ItemStack existing = this.func_70301_a(i);
            return existing.getCount() + itemStack.getCount() <= this.func_70297_j_();
        } else {
            return false;
        }
    }

    @Override
    public boolean canExtractItem(int i, ItemStack itemStack, Direction direction) {
        return true;
    }

    @Override
    public void clear() {
        this.getItemHandler().clear();
    }
}
