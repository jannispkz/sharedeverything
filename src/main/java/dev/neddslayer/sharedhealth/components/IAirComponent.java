package dev.neddslayer.sharedhealth.components;

import org.ladysnake.cca.api.v3.component.ComponentV3;

public interface IAirComponent extends ComponentV3 {
    int getAir();
    void setAir(int air);
    int getMaxAir();
    void setMaxAir(int maxAir);
    int getDrowningTicks();
    void setDrowningTicks(int drowningTicks);
}
