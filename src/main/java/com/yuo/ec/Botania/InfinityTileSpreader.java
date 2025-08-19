package com.yuo.ec.Botania;

import com.google.common.base.Predicates;
import com.yuo.ec.ECTileTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.BotaniaAPIClient;
import vazkii.botania.api.block.WandBindable;
import vazkii.botania.api.block.WandHUD;
import vazkii.botania.api.block.Wandable;
import vazkii.botania.api.internal.ManaBurst;
import vazkii.botania.api.internal.VanillaPacketDispatcher;
import vazkii.botania.api.mana.*;
import vazkii.botania.client.core.helper.RenderHelper;
import vazkii.botania.common.block.block_entity.ExposedSimpleInventoryBlockEntity;
import vazkii.botania.common.block.block_entity.mana.ThrottledPacket;
import vazkii.botania.common.entity.ManaBurstEntity;
import vazkii.botania.common.entity.ManaBurstEntity.PositionProperties;
import vazkii.botania.common.handler.BotaniaSounds;
import vazkii.botania.common.handler.ManaNetworkHandler;
import vazkii.botania.common.helper.MathHelper;
import vazkii.botania.common.item.LexicaBotaniaItem;
import vazkii.botania.xplat.BotaniaConfig;
import vazkii.botania.xplat.XplatAbstractions;

import java.util.*;

public class InfinityTileSpreader extends ExposedSimpleInventoryBlockEntity implements WandBindable, KeyLocked, ThrottledPacket, ManaSpreader, Wandable {
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
    public @org.jetbrains.annotations.Nullable DyeColor paddingColor = null;
    private boolean requestsClientUpdate = false;
    private boolean hasReceivedInitialPacket = false;
    private ManaReceiver receiver = null;
    private ManaReceiver receiverLastTick = null;
    private boolean poweredLastTick = true;
    public boolean canShootBurst = true;
    public int lastBurstDeathTick = -1;
    public int burstParticleTick = 0;
    public int pingbackTicks = 0;
    public double lastPingbackX = 0.0;
    public double lastPingbackY = -2.147483648E9;
    public double lastPingbackZ = 0.0;
    private List<ManaBurstEntity.PositionProperties> lastTentativeBurst;
    private boolean invalidTentativeBurst = false;

    public InfinityTileSpreader(BlockPos pos, BlockState state) {
        super(ECTileTypes.INFINITY_SPREADER.get(), pos, state);
    }

    public boolean isFull() {
        return this.mana >= this.getMaxMana();
    }

    public void receiveMana(int mana) {
        this.mana = Math.min(this.mana + mana, this.getMaxMana());
        this.setChanged();
    }

    public void setRemoved() {
        super.setRemoved();
        BotaniaAPI.instance().getManaNetworkInstance().fireManaNetworkEvent(this, ManaBlockType.COLLECTOR, ManaNetworkAction.REMOVE);
    }

    public static void commonTick(Level level, BlockPos worldPosition, BlockState state, InfinityTileSpreader self) {
        boolean inNetwork = ManaNetworkHandler.instance.isCollectorIn(level, self);
        if (!inNetwork && !self.isRemoved()) {
            BotaniaAPI.instance().getManaNetworkInstance().fireManaNetworkEvent(self, ManaBlockType.COLLECTOR, ManaNetworkAction.ADD);
        }

        boolean powered = false;
        Direction[] var7 = Direction.values();

        for (Direction dir : var7) {
            BlockPos relPos = worldPosition.relative(dir);
            if (level.hasChunkAt(relPos)) {
                ManaReceiver receiverAt = XplatAbstractions.INSTANCE.findManaReceiver(level, relPos, dir.getOpposite());
                if (receiverAt instanceof ManaPool pool) {
                    if (inNetwork && (pool != self.receiver || self.getVariant() == VariantEC.REDSTONE)) {
                        if (pool instanceof KeyLocked locked) {
                            if (!locked.getOutputKey().equals(self.getInputKey())) {
                                continue;
                            }
                        }

                        int manaInPool = pool.getCurrentMana();
                        if (manaInPool > 0 && !self.isFull()) {
                            int manaMissing = self.getMaxMana() - self.mana;
                            int manaToRemove = Math.min(manaInPool, manaMissing);
                            pool.receiveMana(-manaToRemove);
                            self.receiveMana(manaToRemove);
                        }
                    }
                }

                powered = powered || level.hasSignal(relPos, dir);
            }
        }

        if (self.needsNewBurstSimulation()) {
            self.checkForReceiver();
        }

        if (!self.canShootBurst) {
            if (self.pingbackTicks <= 0) {
                double x = self.lastPingbackX;
                double y = self.lastPingbackY;
                double z = self.lastPingbackZ;
                AABB aabb = (new AABB(x, y, z, x, y, z)).inflate(0.5, 0.5, 0.5);
                List<ThrowableProjectile> bursts = level.getEntitiesOfClass(ThrowableProjectile.class, aabb, Predicates.instanceOf(ManaBurst.class));
                ManaBurst found = null;
                UUID identity = self.getIdentifier();

                for (ThrowableProjectile throwableProjectile : bursts) {
                    ManaBurst burst = (ManaBurst) throwableProjectile;
                    if (burst != null && identity.equals(burst.getShooterUUID())) {
                        found = burst;
                        break;
                    }
                }

                if (found != null) {
                    found.ping();
                } else {
                    self.setCanShoot(true);
                }
            } else {
                --self.pingbackTicks;
            }
        }

        boolean shouldShoot = !powered;
        boolean redstoneSpreader = self.getVariant() == VariantEC.REDSTONE;
        if (redstoneSpreader) {
            shouldShoot = powered && !self.poweredLastTick;
        }

        if (shouldShoot) {
            ManaReceiver var24 = self.receiver;
            if (var24 instanceof KeyLocked locked) {
                shouldShoot = locked.getInputKey().equals(self.getOutputKey());
            }
        }

        ItemStack lens = self.getItemHandler().getItem(0);
        ControlLensItem control = self.getLensController(lens);
        if (control != null) {
            if (redstoneSpreader) {
                if (shouldShoot) {
                    control.onControlledSpreaderPulse(lens, self);
                }
            } else {
                control.onControlledSpreaderTick(lens, self, powered);
            }

            shouldShoot = shouldShoot && control.allowBurstShooting(lens, self, powered);
        }

        if (shouldShoot) {
            self.tryShootBurst();
        }

        if (self.receiverLastTick != self.receiver && !level.isClientSide) {
            self.requestsClientUpdate = true;
            VanillaPacketDispatcher.dispatchTEToNearbyPlayers(self);
        }

        self.poweredLastTick = powered;
        self.receiverLastTick = self.receiver;
    }

    public void writePacketNBT(CompoundTag cmp) {
        super.writePacketNBT(cmp);
        cmp.putUUID("uuid", this.getIdentifier());
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
        cmp.putInt("forceClientBindingX", this.receiver == null ? 0 : this.receiver.getManaReceiverPos().getX());
        cmp.putInt("forceClientBindingY", this.receiver == null ? Integer.MIN_VALUE : this.receiver.getManaReceiverPos().getY());
        cmp.putInt("forceClientBindingZ", this.receiver == null ? 0 : this.receiver.getManaReceiverPos().getZ());
        cmp.putBoolean("mapmakerOverrideEnabled", this.mapmakerOverride);
        cmp.putInt("mmForcedColor", this.mmForcedColor);
        cmp.putInt("mmForcedManaPayload", this.mmForcedManaPayload);
        cmp.putInt("mmForcedTicksBeforeManaLoss", this.mmForcedTicksBeforeManaLoss);
        cmp.putFloat("mmForcedManaLossPerTick", this.mmForcedManaLossPerTick);
        cmp.putFloat("mmForcedGravity", this.mmForcedGravity);
        cmp.putFloat("mmForcedVelocityMultiplier", this.mmForcedVelocityMultiplier);
        this.requestsClientUpdate = false;
    }

    public void readPacketNBT(CompoundTag cmp) {
        super.readPacketNBT(cmp);
        String tagUuidMostDeprecated = "uuidMost";
        String tagUuidLeastDeprecated = "uuidLeast";
        if (cmp.hasUUID("uuid")) {
            this.identity = cmp.getUUID("uuid");
        } else if (cmp.contains(tagUuidLeastDeprecated) && cmp.contains(tagUuidMostDeprecated)) {
            long most = cmp.getLong(tagUuidMostDeprecated);
            long least = cmp.getLong(tagUuidLeastDeprecated);
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
        if (this.requestsClientUpdate && this.level != null) {
            int x = cmp.getInt("forceClientBindingX");
            int y = cmp.getInt("forceClientBindingY");
            int z = cmp.getInt("forceClientBindingZ");
            if (y != Integer.MIN_VALUE) {
                BlockPos pos = new BlockPos(x, y, z);
                this.receiver = XplatAbstractions.INSTANCE.findManaReceiver(this.level, pos, (Direction)null);
            } else {
                this.receiver = null;
            }
        }

        if (this.level != null && this.level.isClientSide) {
            this.hasReceivedInitialPacket = true;
        }

    }

    public boolean canReceiveManaFromBursts() {
        return true;
    }

    public Level getManaReceiverLevel() {
        return this.getLevel();
    }

    public BlockPos getManaReceiverPos() {
        return this.getBlockPos();
    }

    public int getCurrentMana() {
        return this.mana;
    }

    public boolean onUsedByWand(@Nullable Player player, ItemStack wand, Direction side) {
        if (player == null || this.level == null) {
            return false;
        } else {
            if (!player.isShiftKeyDown()) {
                VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
            } else {
                BlockHitResult bpos = LexicaBotaniaItem.doRayTrace(this.level, player, Fluid.NONE);
                if (!this.level.isClientSide) {
                    double x = bpos.getLocation().x - (double)this.getBlockPos().getX() - 0.5;
                    double y = bpos.getLocation().y - (double)this.getBlockPos().getY() - 0.5;
                    double z = bpos.getLocation().z - (double)this.getBlockPos().getZ() - 0.5;
                    if (bpos.getDirection() != Direction.DOWN && bpos.getDirection() != Direction.UP) {
                        Vec3 clickVector = new Vec3(x, 0.0, z);
                        Vec3 relative = new Vec3(-0.5, 0.0, 0.0);
                        double angle = Math.acos(clickVector.dot(relative) / (relative.length() * clickVector.length())) * 180.0 / Math.PI;
                        this.rotationX = (float)angle + 180.0F;
                        if (clickVector.z < 0.0) {
                            this.rotationX = 360.0F - this.rotationX;
                        }
                    }

                    double angle = y * 180.0;
                    this.rotationY = -((float)angle);
                    this.setChanged();
                    this.requestsClientUpdate = true;
                }
            }

            return true;
        }
    }

    private boolean needsNewBurstSimulation() {
        if (this.level != null && this.level.isClientSide && !this.hasReceivedInitialPacket) {
            return false;
        } else if (this.lastTentativeBurst == null) {
            return true;
        } else {
            Iterator<PositionProperties> var1 = this.lastTentativeBurst.iterator();

            ManaBurstEntity.PositionProperties props;
            do {
                if (!var1.hasNext()) {
                    return false;
                }

                props = var1.next();
            } while(props.contentsEqual(this.level));

            this.invalidTentativeBurst = props.isInvalidIn(this.level);
            return !this.invalidTentativeBurst;
        }
    }

    private void tryShootBurst() {
        boolean redstone = this.getVariant() == VariantEC.REDSTONE;
        if ((this.receiver != null || redstone) && !this.invalidTentativeBurst && this.canShootBurst && (redstone || this.receiver.canReceiveManaFromBursts() && !this.receiver.isFull())) {
            ManaBurstEntity burst = this.getBurst(false);
            if (this.level != null && burst != null && !this.level.isClientSide) {
                this.receiveMana(-burst.getStartingMana());
                burst.setShooterUUID(this.getIdentifier());
                this.level.addFreshEntity(burst);
                burst.ping();
                if (!BotaniaConfig.common().silentSpreaders()) {
                    this.level.playSound((Player)null, this.worldPosition, BotaniaSounds.spreaderFire, SoundSource.BLOCKS, 0.05F * (this.paddingColor != null ? 0.2F : 1.0F), 0.7F + 0.3F * (float)Math.random());
                }
            }
        }

    }

    public VariantEC getVariant() {
        Block b = this.getBlockState().getBlock();
        if (b instanceof InfinityManaSpreader spreader) {
            return spreader.variantEC;
        } else {
            return VariantEC.MANA;
        }
    }

    public void checkForReceiver() {
        ItemStack stack = this.getItemHandler().getItem(0);
        ControlLensItem control = this.getLensController(stack);
        if (control == null || control.allowBurstShooting(stack, this, false)) {
            ManaBurstEntity fakeBurst = this.getBurst(true);
            if (fakeBurst != null) {
                fakeBurst.setScanBeam();
                ManaReceiver receiver = fakeBurst.getCollidedTile(true);
                if (receiver != null && receiver.getManaReceiverLevel().hasChunkAt(receiver.getManaReceiverPos())) {
                    this.receiver = receiver;
                } else {
                    this.receiver = null;
                }

                this.lastTentativeBurst = fakeBurst.propsList;
            }

        }
    }

    public ManaBurst runBurstSimulation() {
        ManaBurstEntity fakeBurst = this.getBurst(true);
        if (fakeBurst != null) {
            fakeBurst.setScanBeam();
            fakeBurst.getCollidedTile(true);
        }
        return fakeBurst;
    }

    private ManaBurstEntity getBurst(boolean fake) {
        VariantEC variant = this.getVariant();
        float gravity = 0.0F;
        BurstProperties props = new BurstProperties(variant.burstMana, variant.preLossTicks, variant.lossPerTick, gravity, variant.motionModifier, variant.color);
        ItemStack lens = this.getItemHandler().getItem(0);
        if (!lens.isEmpty()) {
            Item var7 = lens.getItem();
            if (var7 instanceof LensEffectItem lensEffectItem) {
                lensEffectItem.apply(lens, props, this.level);
            }
        }

        if (this.getCurrentMana() < props.maxMana && !fake) {
            return null;
        } else {
            ManaBurstEntity burst = new ManaBurstEntity(this.getLevel(), this.getBlockPos(), this.getRotationX(), this.getRotationY(), fake);
            burst.setSourceLens(lens);
            if (this.mapmakerOverride) {
                burst.setColor(this.mmForcedColor);
                burst.setMana(this.mmForcedManaPayload);
                burst.setStartingMana(this.mmForcedManaPayload);
                burst.setMinManaLoss(this.mmForcedTicksBeforeManaLoss);
                burst.setManaLossPerTick(this.mmForcedManaLossPerTick);
                burst.setGravity(this.mmForcedGravity);
                burst.setDeltaMovement(burst.getDeltaMovement().scale((double)this.mmForcedVelocityMultiplier));
            } else {
                burst.setColor(props.color);
                burst.setMana(props.maxMana);
                burst.setStartingMana(props.maxMana);
                burst.setMinManaLoss(props.ticksBeforeManaLoss);
                burst.setManaLossPerTick(props.manaLossPerTick);
                burst.setGravity(props.gravity);
                burst.setDeltaMovement(burst.getDeltaMovement().scale((double)props.motionModifier));
            }

            return burst;
        }
    }

    public ControlLensItem getLensController(ItemStack stack) {
        if (!stack.isEmpty()) {
            Item var3 = stack.getItem();
            if (var3 instanceof ControlLensItem control) {
                if (control.isControlLens(stack)) {
                    return control;
                }
            }
        }

        return null;
    }

    public void onClientDisplayTick() {
        if (this.level != null) {
            ManaBurstEntity burst = this.getBurst(true);
            if (burst != null) {
                burst.getCollidedTile(false);
            }
        }

    }

    public float getManaYieldMultiplier(ManaBurst burst) {
        return 1.0F;
    }

    protected SimpleContainer createItemHandler() {
        return new SimpleContainer(1) {
            public int getMaxStackSize() {
                return 1;
            }

            public boolean canPlaceItem(int index, @NotNull ItemStack stack) {
                return !stack.isEmpty() && stack.getItem() instanceof BasicLensItem;
            }
        };
    }

    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.checkForReceiver();
            VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
        }

    }

    public BlockPos getBinding() {
        return this.receiver == null ? null : this.receiver.getManaReceiverPos();
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

    public boolean canSelect(Player player, ItemStack wand, BlockPos pos, Direction side) {
        return true;
    }

    public boolean bindTo(Player player, ItemStack wand, BlockPos pos, Direction side) {
        VoxelShape shape = player.level().getBlockState(pos).getShape(player.level(), pos);
        AABB axis = shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);
        Vec3 thisVec = Vec3.atCenterOf(this.getBlockPos());
        Vec3 blockVec = new Vec3(axis.minX + (axis.maxX - axis.minX) / 2.0, axis.minY + (axis.maxY - axis.minY) / 2.0, axis.minZ + (axis.maxZ - axis.minZ) / 2.0);
        Vec3 diffVec = blockVec.subtract(thisVec);
        Vec3 diffVec2D = new Vec3(diffVec.x, diffVec.z, 0.0);
        Vec3 rotVec = new Vec3(0.0, 1.0, 0.0);
        double angle = MathHelper.angleBetween(rotVec, diffVec2D) / Math.PI * 180.0;
        if (blockVec.x < thisVec.x) {
            angle = -angle;
        }

        this.rotationX = (float)angle + 90.0F;
        rotVec = new Vec3(diffVec.x, 0.0, diffVec.z);
        angle = MathHelper.angleBetween(diffVec, rotVec) * 180.0 / Math.PI;
        if (blockVec.y < thisVec.y) {
            angle = -angle;
        }

        this.rotationY = (float)angle;
        this.setChanged();
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

    public void rotate(Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_90:
                this.rotationX += 270.0F;
                break;
            case CLOCKWISE_180:
                this.rotationX += 180.0F;
                break;
            case COUNTERCLOCKWISE_90:
                this.rotationX += 90.0F;
            case NONE:
        }

        if (this.rotationX >= 360.0F) {
            this.rotationX -= 360.0F;
        }

    }

    public void mirror(Mirror mirror) {
        switch (mirror) {
            case LEFT_RIGHT:
                this.rotationX = 360.0F - this.rotationX;
                break;
            case FRONT_BACK:
                this.rotationX = 180.0F - this.rotationX;
            case NONE:
        }

        if (this.rotationX < 0.0F) {
            this.rotationX += 360.0F;
        }

    }

    public void commitRedirection() {
        this.setChanged();
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

    public void pingback(ManaBurst burst, UUID expectedIdentity) {
        if (this.getIdentifier().equals(expectedIdentity)) {
            this.pingbackTicks = 20;
            Entity e = burst.entity();
            this.lastPingbackX = e.getX();
            this.lastPingbackY = e.getY();
            this.lastPingbackZ = e.getZ();
            this.setCanShoot(false);
        }

    }

    public UUID getIdentifier() {
        return this.identity;
    }

    public static class WandHud implements WandHUD {
        private final InfinityTileSpreader spreader;

        public WandHud(InfinityTileSpreader spreader) {
            this.spreader = spreader;
        }

        public void renderHUD(GuiGraphics gui, Minecraft mc) {
            String spreaderName = (new ItemStack(this.spreader.getBlockState().getBlock())).getHoverName().getString();
            ItemStack lensStack = this.spreader.getItemHandler().getItem(0);
            ItemStack recieverStack = null;
            if (this.spreader.level != null) {
                recieverStack = this.spreader.receiver == null ? ItemStack.EMPTY : new ItemStack(this.spreader.level.getBlockState(this.spreader.receiver.getManaReceiverPos()).getBlock());
            }
            int width;
            if (recieverStack != null) {
                width = 4 + Collections.max(Arrays.asList(102, mc.font.width(spreaderName), RenderHelper.itemWithNameWidth(mc, lensStack), RenderHelper.itemWithNameWidth(mc, recieverStack)));
                int height = 22 + (lensStack.isEmpty() ? 0 : 18) + (recieverStack.isEmpty() ? 0 : 18);
                int centerX = mc.getWindow().getGuiScaledWidth() / 2;
                int centerY = mc.getWindow().getGuiScaledHeight() / 2;
                RenderHelper.renderHUDBox(gui, centerX - width / 2, centerY + 8, centerX + width / 2, centerY + 8 + height);
                int color = this.spreader.getVariant().hudColor;
                BotaniaAPIClient.instance().drawSimpleManaHUD(gui, color, this.spreader.getCurrentMana(), this.spreader.getMaxMana(), spreaderName);
                RenderHelper.renderItemWithNameCentered(gui, mc, recieverStack, centerY + 30, color);
                RenderHelper.renderItemWithNameCentered(gui, mc, lensStack, centerY + (recieverStack.isEmpty() ? 30 : 48), color);
            }
        }
    }
}
