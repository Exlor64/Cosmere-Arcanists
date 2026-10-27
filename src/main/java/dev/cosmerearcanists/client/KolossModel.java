package dev.cosmerearcanists.client;
import dev.cosmerearcanists.koloss.KolossEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
/** Supplied Blockbench geometry by Ex, ported to the1.21.1 model API. */
public class KolossModel extends HierarchicalModel<KolossEntity> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("cosmerearcanists", "koloss"), "main");
	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart leftupperarm;
	private final ModelPart leftlowerarm;
	private final ModelPart rightupperarm;
	private final ModelPart rightlowerarm;
	private final ModelPart sword;
	private final ModelPart leftupperleg;
	private final ModelPart leftlowerleg;
	private final ModelPart rightupperleg;
	private final ModelPart rightlowerleg;

	public KolossModel(ModelPart root) {
		this.root = root.getChild("root");
		this.body = this.root.getChild("body");
		this.head = this.body.getChild("head");
		this.leftupperarm = this.body.getChild("leftupperarm");
		this.leftlowerarm = this.leftupperarm.getChild("leftlowerarm");
		this.rightupperarm = this.body.getChild("rightupperarm");
		this.rightlowerarm = this.rightupperarm.getChild("rightlowerarm");
		this.sword = this.rightlowerarm.getChild("sword");
		this.leftupperleg = this.body.getChild("leftupperleg");
		this.leftlowerleg = this.leftupperleg.getChild("leftlowerleg");
		this.rightupperleg = this.body.getChild("rightupperleg");
		this.rightlowerleg = this.rightupperleg.getChild("rightlowerleg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 20.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -18.0F, -8.0F, 16.0F, 8.0F, 16.0F, new CubeDeformation(0.0F))
		.texOffs(56, 48).addBox(-7.0F, -10.0F, -7.0F, 14.0F, 2.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r1 = body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(58, 24).addBox(-7.0F, 0.0F, -3.0F, 14.0F, 6.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -27.0F, 0.0F, -0.1309F, 0.0F, 0.0F));

		PartDefinition cube_r2 = body.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 24).addBox(-9.0F, -7.0F, -5.0F, 18.0F, 13.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -18.0F, 0.0F, -0.1309F, 0.0F, 0.0F));

		PartDefinition cube_r3 = body.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 48).addBox(-7.0F, -8.0F, -7.0F, 14.0F, 6.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -15.0F, 0.0F, -0.1309F, 0.0F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 68).addBox(-4.0F, -15.0F, -5.0F, 8.0F, 8.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(-2, -3).addBox(0.0F, -21.0F, -3.0F, 0.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -18.0F, 0.0F));

		PartDefinition leftupperarm = body.addOrReplaceChild("leftupperarm", CubeListBuilder.create().texOffs(104, 22).addBox(-3.0F, -3.0F, -3.0F, 5.0F, 5.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(86, 97).addBox(-4.0F, 1.0F, -4.0F, 5.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(-9.0F, -23.0F, 0.0F));

		PartDefinition leftlowerarm = leftupperarm.addOrReplaceChild("leftlowerarm", CubeListBuilder.create().texOffs(28, 102).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 12.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 7.0F, 0.0F));

		PartDefinition rightupperarm = body.addOrReplaceChild("rightupperarm", CubeListBuilder.create().texOffs(100, 11).addBox(-2.0F, -3.0F, -3.0F, 5.0F, 5.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(86, 83).addBox(-1.0F, 1.0F, -4.0F, 5.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(9.0F, -23.0F, 0.0F));

		PartDefinition rightlowerarm = rightupperarm.addOrReplaceChild("rightlowerarm", CubeListBuilder.create().texOffs(34, 68).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 12.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, 7.0F, 0.0F));

		PartDefinition sword = rightlowerarm.addOrReplaceChild("sword", CubeListBuilder.create().texOffs(56, 64).addBox(-1.0F, -1.0F, -9.0F, 2.0F, 2.0F, 17.0F, new CubeDeformation(0.0F))
		.texOffs(48, 102).addBox(-2.0F, -2.0F, 7.0F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(64, 0).addBox(-1.0F, -4.0F, -25.0F, 2.0F, 8.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 0.0F));

		PartDefinition cube_r4 = sword.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(60, 83).addBox(-1.0F, -1.0F, -11.0F, 1.0F, 3.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 0.25F, -29.25F, -0.5236F, 0.0F, 0.0F));

		PartDefinition cube_r5 = sword.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 85).addBox(-1.0F, -5.0F, -11.0F, 1.0F, 3.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 0.5F, -29.5F, -0.2182F, 0.0F, 0.0F));

		PartDefinition cube_r6 = sword.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(34, 83).addBox(-1.0F, -5.0F, -11.0F, 1.0F, 7.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 2.0F, -21.0F, -0.2182F, 0.0F, 0.0F));

		PartDefinition leftupperleg = body.addOrReplaceChild("leftupperleg", CubeListBuilder.create().texOffs(100, 0).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 5.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(64, 109).addBox(-2.0F, 4.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, -8.0F, 0.0F));

		PartDefinition leftlowerleg = leftupperleg.addOrReplaceChild("leftlowerleg", CubeListBuilder.create().texOffs(48, 109).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(78, 37).addBox(-3.0F, 4.0F, -6.0F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 6.0F, 0.0F));

		PartDefinition rightupperleg = body.addOrReplaceChild("rightupperleg", CubeListBuilder.create().texOffs(60, 98).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 5.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(94, 76).addBox(-2.0F, 4.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, -8.0F, 0.0F));

		PartDefinition rightlowerleg = rightupperleg.addOrReplaceChild("rightlowerleg", CubeListBuilder.create().texOffs(104, 33).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(56, 37).addBox(-3.0F, 4.0F, -6.0F, 6.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 6.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

    @Override public ModelPart root(){return root;}
    @Override public void setupAnim(KolossEntity entity,float limbSwing,float limbSwingAmount,float ageInTicks,float netHeadYaw,float headPitch){
        root.getAllParts().forEach(ModelPart::resetPose);
        sword.visible=entity.getMainHandItem().is(dev.cosmerearcanists.koloss.KolossContent.SWORD.get());
        head.yRot=netHeadYaw*((float)Math.PI/180);head.xRot=headPitch*((float)Math.PI/180);
        float walk=Mth.cos(limbSwing*.6662f)*limbSwingAmount;
        leftupperleg.xRot=walk;rightupperleg.xRot=-walk;
        leftupperarm.xRot=-walk*.7f;rightupperarm.xRot=walk*.7f;
        if(attackTime>0){float strike=Mth.sin(Mth.sqrt(attackTime)*(float)Math.PI*2)*.8f;rightupperarm.xRot=-1.2f-strike;leftupperarm.xRot=-.5f-strike*.4f;}
    }
}
