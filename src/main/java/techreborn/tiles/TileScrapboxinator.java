/*
 * This file is part of TechReborn, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2017 TechReborn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package techreborn.tiles;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import reborncore.api.tile.IInventoryProvider;
import reborncore.common.IWrenchable;
import reborncore.common.blocks.BlockMachineBase;
import reborncore.common.powerSystem.TilePowerAcceptor;
import reborncore.common.registration.RebornRegistry;
import reborncore.common.registration.impl.ConfigRegistry;
import reborncore.common.util.Inventory;
import techreborn.api.ScrapboxList;
import techreborn.client.container.IContainerProvider;
import techreborn.client.container.builder.BuiltContainer;
import techreborn.client.container.builder.ContainerBuilder;
import techreborn.init.ModBlocks;
import techreborn.init.ModItems;
import techreborn.lib.ModInfo;

import java.util.Random;

@RebornRegistry(modID = ModInfo.MOD_ID)
public class TileScrapboxinator extends TilePowerAcceptor
	implements IWrenchable, IInventoryProvider, ISidedInventory, IContainerProvider {

	@ConfigRegistry(config = "machines", category = "scrapboxinator", key = "ScrapboxinatorMaxInput", comment = "Scrapboxinator Max Input (Value in EU)")
	public static int maxInput = 32;
	@ConfigRegistry(config = "machines", category = "scrapboxinator", key = "ScrapboxinatorMaxEnergy", comment = "Scrapboxinator Max Energy (Value in EU)")
	public static int maxEnergy = 1000;
	@ConfigRegistry(config = "machines", category = "scrapboxinator", key = "ScrapboxinatorEnergyCost", comment = "Scrapboxinator Energy Cost (Value in EU)")
	public static int cost = 20;
	@ConfigRegistry(config = "machines", category = "scrapboxinator", key = "ScrapboxinatorRunTime", comment = "Scrapboxinator Run Time")
	public static int runTime = 200;
	//  @ConfigRegistry(config = "machines", category = "scrapboxinator", key = "ScrapboxinatorWrenchDropRate", comment = "Scrapboxinator Wrench Drop Rate")
	public static float wrenchDropRate = 1.0F;

	public Inventory inventory = new Inventory(6, "TileScrapboxinator", 64, this);

	public int progress;
	public int input1 = 0;
	public int output = 1;

	public TileScrapboxinator() {
		super();
	}

	public int gaugeProgressScaled(final int scale) {
		return this.progress * scale / runTime;
	}

	@Override
	public void updateEntity() {
		final boolean burning = this.isBurning();
		boolean updateInventory = false;
		if (this.getEnergy() <= cost && this.canOpen()) {
			if (this.getEnergy() > cost) {
				updateInventory = true;
			}
		}
		if (this.isBurning() && this.canOpen()) {
			this.updateState();

			this.progress++;
			if (this.progress >= runTime) {
				this.progress = 0;
				this.recycleItems();
				updateInventory = true;
			}
		} else {
			this.progress = 0;
			this.updateState();
		}
		if (burning != this.isBurning()) {
			updateInventory = true;
		}
		if (updateInventory) {
			this.markDirty();
		}
	}

	public void recycleItems() {
		if (this.canOpen() && !this.world.isRemote) {
			final int random = new Random().nextInt(ScrapboxList.stacks.size());
			final ItemStack out = ScrapboxList.stacks.get(random).copy();
			if (this.getStackInSlot(this.output) == null) {
				this.useEnergy(cost);
				this.setInventorySlotContents(this.output, out);
			}

			if (this.getStackInSlot(this.input1).getCount() > 1) {
				this.useEnergy(cost);
				this.decrStackSize(this.input1, 1);
			} else {
				this.useEnergy(cost);
				this.setInventorySlotContents(this.input1, ItemStack.EMPTY);
			}
		}
	}

	public boolean canOpen() {
		return this.getStackInSlot(this.input1) != ItemStack.EMPTY && this.getStackInSlot(this.output) == ItemStack.EMPTY;
	}

	public boolean isBurning() {
		return this.getEnergy() > cost;
	}

	public void updateState() {
		final IBlockState blockState = this.world.getBlockState(this.pos);
		if (blockState.getBlock() instanceof BlockMachineBase) {
			final BlockMachineBase blockMachineBase = (BlockMachineBase) blockState.getBlock();
			if (blockState.getValue(BlockMachineBase.ACTIVE) != this.progress > 0)
				blockMachineBase.setActive(this.progress > 0, this.world, this.pos);
		}
	}

	@Override
	public boolean wrenchCanSetFacing(final EntityPlayer entityPlayer, final EnumFacing side) {
		return false;
	}

	@Override
	public EnumFacing getFacing() {
		return this.getFacingEnum();
	}

	@Override
	public boolean wrenchCanRemove(final EntityPlayer entityPlayer) {
		return entityPlayer.isSneaking();
	}

	@Override
	public float getWrenchDropRate() {
		return wrenchDropRate;
	}

	@Override
	public ItemStack getWrenchDrop(final EntityPlayer entityPlayer) {
		return new ItemStack(ModBlocks.SCRAPBOXINATOR, 1);
	}

	public boolean isComplete() {
		return false;
	}

	// ISidedInventory
	@Override
	public int[] getSlotsForFace(final EnumFacing side) {
		return side == EnumFacing.DOWN ? new int[] { 0, 1, 2 } : new int[] { 0, 1, 2 };
	}

	@Override
	public boolean canInsertItem(final int slotIndex, final ItemStack itemStack, final EnumFacing side) {
		if (slotIndex == 2)
			return false;
		if (slotIndex == 1) {
			if (itemStack.getItem() == ModItems.SCRAP_BOX) {
				return true;
			}
		}
		return this.isItemValidForSlot(slotIndex, itemStack);
	}

	@Override
	public boolean canExtractItem(final int slotIndex, final ItemStack itemStack, final EnumFacing side) {
		return slotIndex == 2;
	}

	@Override
	public double getBaseMaxPower() {
		return maxEnergy;
	}

	@Override
	public boolean canAcceptEnergy(final EnumFacing direction) {
		return true;
	}

	@Override
	public boolean canProvideEnergy(final EnumFacing direction) {
		return false;
	}

	@Override
	public double getBaseMaxOutput() {
		return 0;
	}

	@Override
	public double getBaseMaxInput() {
		return maxInput;
	}

	@Override
	public Inventory getInventory() {
		return this.inventory;
	}

	public int getProgress() {
		return this.progress;
	}

	public void setProgress(final int progress) {
		this.progress = progress;
	}

	@Override
	public BuiltContainer createContainer(final EntityPlayer player) {
		return new ContainerBuilder("scrapboxinator").player(player.inventory).inventory(8, 84).hotbar(8, 142)
			.addInventory().tile(this).filterSlot(0, 56, 34, stack -> stack.getItem() == ModItems.SCRAP_BOX)
			.outputSlot(1, 116, 34).upgradeSlot(2, 152, 8).upgradeSlot(3, 152, 26).upgradeSlot(4, 152, 44)
			.upgradeSlot(5, 152, 62).syncEnergyValue().syncIntegerValue(this::getProgress, this::setProgress)
			.addInventory().create();
	}
}
