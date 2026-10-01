package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.CraftersSoccer;
import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;
import org.crafterscr.crafterssoccer.entity.SoccerBallStyle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderizador del balón.
 */
public final class SoccerBallRenderer
        extends EntityRenderer<SoccerBallEntity> {

    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    CraftersSoccer.MOD_ID,
                    "textures/entity/soccer_ball.png"
            );

    private static final ResourceLocation POKEBALL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    CraftersSoccer.MOD_ID,
                    "textures/entity/soccer_ball_pokeball.png"
            );

    private static final ResourceLocation PIKACHU_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    CraftersSoccer.MOD_ID,
                    "textures/entity/soccer_ball_pikachu.png"
            );

    /**
     * Convierte el cubo UV de 32 píxeles en uno
     * visible de aproximadamente 8 píxeles.
     */
    private static final float MODEL_SCALE = 0.25F;

    private final ModelPart ballPart;

    public SoccerBallRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);

        ModelPart bakedRoot =
                context.bakeLayer(
                        SoccerBallModel.MODEL_LAYER
                );

        this.ballPart =
                SoccerBallModel.getBallPart(
                        bakedRoot
                );

        this.shadowRadius =
                SoccerBallEntity.BALL_RADIUS;

        this.shadowStrength = 0.8F;
    }

    @Override
    public void render(
            SoccerBallEntity ball,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();

        /*
         * Centrar el modelo dentro de la hitbox.
         */
        poseStack.translate(
                0.0D,
                SoccerBallEntity.BALL_RADIUS,
                0.0D
        );

        /*
         * Reducirlo al tamaño correcto y corregir orientación.
         */
        poseStack.scale(
                -MODEL_SCALE,
                -MODEL_SCALE,
                MODEL_SCALE
        );

        float heading =
                ball.getRollingHeadingDegrees();

        float roll =
                ball.getInterpolatedRoll(
                        partialTick
                );

        /*
         * Girar visualmente según la dirección recorrida.
         */
        poseStack.mulPose(
                Axis.YP.rotationDegrees(heading)
        );

        poseStack.mulPose(
                Axis.XP.rotationDegrees(roll)
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(-heading)
        );

        VertexConsumer vertexConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCull(
                                getTextureLocation(
                                        ball
                                )
                        )
                );

        this.ballPart.render(
                poseStack,
                vertexConsumer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();

        super.render(
                ball,
                entityYaw,
                partialTick,
                poseStack,
                bufferSource,
                packedLight
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            SoccerBallEntity entity
    ) {
        SoccerBallStyle style =
                entity.getBallStyle();

        return switch (style) {
            case POKEBALL ->
                    POKEBALL_TEXTURE;

            case PIKACHU ->
                    PIKACHU_TEXTURE;

            case DEFAULT ->
                    DEFAULT_TEXTURE;
        };
    }
}