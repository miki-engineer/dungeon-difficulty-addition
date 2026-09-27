package com.miki.dungeondifficultyaddition.forge.client;

import com.miki.dungeondifficultyaddition.forge.AnvilSalvageEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public final class AnvilSalvageRenderer extends EntityRenderer<AnvilSalvageEntity> {
    private final ItemRenderer items;
    public AnvilSalvageRenderer(EntityRendererFactory.Context context) {
        super(context);
        items = context.getItemRenderer();
    }
    @Override public void render(AnvilSalvageEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                                 VertexConsumerProvider vertices, int light) {
        matrices.push();
        matrices.translate(0, .04, 0);
        // Minecraft player yaw turns opposite to a positive model-space Y rotation.
        // Keep the top of the laid-flat item pointing away from the placing player.
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
        matrices.scale(.65F, .65F, .65F);
        items.renderItem(entity.stack(), ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV,
                matrices, vertices, entity.getWorld(), entity.getId());
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertices, light);
    }
    @Override public Identifier getTexture(AnvilSalvageEntity entity) { return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE; }
}
