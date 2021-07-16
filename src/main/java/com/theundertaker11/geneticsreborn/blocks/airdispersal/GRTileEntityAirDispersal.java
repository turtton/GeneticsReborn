package com.theundertaker11.geneticsreborn.blocks.airdispersal;

import com.theundertaker11.geneticsreborn.GeneticsReborn;
import com.theundertaker11.geneticsreborn.api.capability.genes.EnumGenes;
import com.theundertaker11.geneticsreborn.potions.GREntityPotion;
import com.theundertaker11.geneticsreborn.potions.ViralSplashPotion;
import com.theundertaker11.geneticsreborn.tile.GRTileEntityBasicEnergyReceiver;
import com.theundertaker11.geneticsreborn.util.CustomEnergyStorage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class GRTileEntityAirDispersal extends GRTileEntityBasicEnergyReceiver implements ITickable {
	public static final int UNLOCKED = 0;
	public static final int LOCKED = 1;

	private int time;
	private int timeLeft;
	private final Item[] lock = new Item[4];
	private int state;

	private ItemStack maskBlock = ItemStack.EMPTY;
	
	
	public GRTileEntityAirDispersal() {
		super();
	}
	
	public GRTileEntityAirDispersal(String name) {
		this(name, false);		
	}
	
	public GRTileEntityAirDispersal(String name, boolean a) {
		super(name);
		state = UNLOCKED;
		storage = new CustomEnergyStorage(1000);
		NUMBER_OF_FIELDS = 5;
	}
	
	public boolean isRunning() {
		return state == LOCKED && world.isBlockPowered(getPos());
	}
	
	public int getState() {
		return state;
	}

	public int timeLeft() {
		return timeLeft;
	}

	public void setTimeLeft(int ticks) {
		timeLeft = ticks;
	}
	
	public int getTimeLeft() {
		return timeLeft;
	}

	public void setTime(int time) {
		this.time = time;
	}
	
	public boolean isLocked() {
		return state == LOCKED;
	}
	
	public void setLocked(boolean l) {
		if (l && (!isPrimed() || !hasPower())) return;
		state = (l) ? LOCKED : UNLOCKED; 
	}

	@Override
	public void update() {
//		if (hasPower())
		world.setBlockState(getPos(), world.getBlockState(getPos()).withProperty(AirDispersal.MASKED, !maskBlock.isEmpty()));

		if (isRunning()) {
			timeLeft--;
			if (timeLeft <= 0) {
				state = UNLOCKED;
				timeLeft = time;
				throwPotion(); //must be after state change
				markDirty();
			}
		}
	}
	
	public boolean hasPower() {
		return storage.getEnergyStored() > 0;
	}
	
	public boolean isPrimed() {
		return guiStackHandler.getStackInSlot(0) != ItemStack.EMPTY;
	}
	
	public Item[] getLock() {
		return lock;
	}
	
	public void setLock(int pos, Item item) {
		lock[pos] = item;
	}
	
	public void throwPotion() {
		if (!isPrimed()) return;
        if (world.isRemote) return;
        
		state = UNLOCKED;
		timeLeft = 0;				
		clearLock();
		storage.extractEnergy(1000, false);

		BlockPos pos = getPos();
		EntityPotion potion;
		ItemStack item = guiStackHandler.extractItem(0, 1, false);		
		if (item.getItem() instanceof ViralSplashPotion) {
			potion = new GREntityPotion(world, pos.getX()+0.5, pos.getY()+1.05, pos.getZ()+0.5, GeneticsReborn.virusRange * 2, item);
			EnumGenes gene = ViralSplashPotion.getGene(item);
			((GREntityPotion)potion).setGene(gene);
		} else {
			potion = new EntityPotion(world, pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5, item);						
		}
        world.playSound((EntityPlayer)null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_SPLASH_POTION_THROW, SoundCategory.PLAYERS, 0.5F, 0.4F);
			
        potion.motionX = 0;
        potion.motionY = 0.6D;
        potion.motionZ = 0;
        world.spawnEntity(potion);		
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setTag("inventory", guiStackHandler.serializeNBT());
		compound.setInteger("state", state);
		compound.setInteger("time", time);
		compound.setInteger("timeleft", timeLeft);
		compound.setString("lock0", (lock[0] != null) ? lock[0].getRegistryName().toString() : "");
		compound.setString("lock1", (lock[1] != null) ? lock[1].getRegistryName().toString() : "");
		compound.setString("lock2", (lock[2] != null) ? lock[2].getRegistryName().toString() : "");
		compound.setString("lock3", (lock[3] != null) ? lock[3].getRegistryName().toString() : "");		
		return compound;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		guiStackHandler.deserializeNBT(compound.getCompoundTag("inventory"));
		state = compound.getInteger("state");
		time = compound.getInteger("time");
		timeLeft = compound.getInteger("timeleft");
		lock[0] = Item.getByNameOrId(compound.getString("lock0"));
		lock[1] = Item.getByNameOrId(compound.getString("lock1"));
		lock[2] = Item.getByNameOrId(compound.getString("lock2"));
		lock[3] = Item.getByNameOrId(compound.getString("lock3"));
	}

	//These are the actual slots, used by GUI, unlimited access
    private ItemStackHandler guiStackHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
        
        @Override
		public int getSlotLimit(int slot) {
        	return 1;
        }
        
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
        	if (state == LOCKED) return ItemStack.EMPTY;
        	return super.extractItem(slot, amount, simulate);
        };
        
    };
    
    
    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
    	if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing == EnumFacing.DOWN) return false;    	
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)  return false;
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
	@Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) guiStackHandler;
        }
        return super.getCapability(capability, facing);
    }
	

	private static final byte TIME_FIELD_ID = 3;
	private static final byte STATE_FIELD_ID = 4;

	@Override
	public int getField(int id) {
		if (id == STATE_FIELD_ID) return this.state;
		if (id == TIME_FIELD_ID) return this.timeLeft;
		return super.getField(id);
	}

	@Override
	public void setField(int id, int value) {
		if (id == STATE_FIELD_ID) this.state = value; 
		else if (id == TIME_FIELD_ID) this.timeLeft = value;
		else super.setField(id, value);		
	}

	public void clearLock() {
		for (int i=0;i<4;i++)
			lock[i] = null;		
	}

	public ItemStack maskBlock() {
		return maskBlock;
	}

	public void setMaskBlock(ItemStack item) {
		maskBlock = item;
	}
}
