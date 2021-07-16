package com.theundertaker11.geneticsreborn.blocks.airdispersal;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TESRAirDispersal extends TileEntitySpecialRenderer<GRTileEntityAirDispersal> {
	@Override
	public void render(GRTileEntityAirDispersal te, double x, double y, double z, float partialTicks, int destroyStage,	float alpha) {
		ItemStack mask = te.maskBlock();
		if (mask != ItemStack.EMPTY) {
			IBlockState newstate = Block.getBlockFromItem(mask.getItem()).getBlockState().getBaseState();
			if (newstate.getRenderType() == EnumBlockRenderType.MODEL) {
				bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
				GlStateManager.pushMatrix();
				GlStateManager.disableLighting();

				Tessellator tessellator = Tessellator.getInstance();
				BufferBuilder buffer = tessellator.getBuffer();

				buffer.begin(7, DefaultVertexFormats.BLOCK);
				BlockPos pos = te.getPos();
				GlStateManager.translate((float) (x - (double) pos.getX()), (float) (y - (double) pos.getY()), (float) (z - (double) pos.getZ()));
				BlockRendererDispatcher blockRendererDispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
				blockRendererDispatcher.getBlockModelRenderer().renderModel(te.getWorld(), blockRendererDispatcher.getModelForState(newstate), newstate, pos, buffer, false, MathHelper.getPositionRandom(te.getPos()));
				tessellator.draw();
				GlStateManager.enableLighting();
				GlStateManager.popMatrix();
			}

		}
	}
	
}
