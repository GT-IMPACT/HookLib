package gloomyfolken.hooklib.asm.model;

import gloomyfolken.hooklib.asm.InjectionPoint;
import gloomyfolken.hooklib.asm.Shift;
import lombok.Value;

public @Value
class Anchor {
    InjectionPoint point;

    Shift shift;

    String target;

    int ordinal;
}
