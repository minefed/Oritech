package software.bernie.geckolib.animatable;
public interface GeoBlockEntity {
    void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar registrar);
    software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache();
}
