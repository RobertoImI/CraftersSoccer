package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Modelo provisional del balón.
 *
 * La textura utiliza un mapa UV de 128 x 128.
 */
public final class SoccerBallModel {

    public static final ModelLayerLocation MODEL_LAYER =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "soccer_ball"
                    ),
                    "main"
            );

    private SoccerBallModel() {
    }

    /**
     * Crear la geometría.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition =
                new MeshDefinition();

        PartDefinition root =
                meshDefinition.getRoot();

        /*
         * La imagen proporcionada utiliza caras de 32 x 32.
         *
         * Por esa razón la geometría se declara como
         * un cubo de 32 unidades y luego se reduce
         * en el renderer.
         */
        root.addOrReplaceChild(
                "ball",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                -16.0F,
                                -16.0F,
                                -16.0F,
                                32.0F,
                                32.0F,
                                32.0F
                        ),
                PartPose.ZERO
        );

        return LayerDefinition.create(
                meshDefinition,
                128,
                128
        );
    }

    public static ModelPart getBallPart(
            ModelPart bakedRoot
    ) {
        return bakedRoot.getChild("ball");
    }
}