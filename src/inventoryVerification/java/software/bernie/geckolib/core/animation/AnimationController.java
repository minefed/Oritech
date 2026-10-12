package software.bernie.geckolib.core.animation;
import java.util.function.Function;
public class AnimationController<T> {
    public AnimationController(T owner, Function<State<T>, Object> predicate) {}
    public void setAnimation(RawAnimation animation) {} public void forceAnimationReset() {} public boolean hasAnimationFinished() { return true; }
    public AnimationController<T> setAnimationSpeedHandler(Function<T, Double> function) { return this; }
    public AnimationController<T> setSoundKeyframeHandler(Object handler) { return this; }
    public static class State<T> {
        public boolean isCurrentAnimation(RawAnimation animation) { return false; }
        public AnimationController<T> getController() { return new AnimationController<>(null, state -> null); }
        public Object setAndContinue(RawAnimation animation) { return new Object(); }
    }
}
