package software.bernie.geckolib.core.animation;
public class RawAnimation {
    public static RawAnimation begin() { return new RawAnimation(); }
    public RawAnimation thenPlayAndHold(String name) { return this; } public RawAnimation thenPlay(String name) { return this; }
    public RawAnimation thenLoop(String name) { return this; }
}
