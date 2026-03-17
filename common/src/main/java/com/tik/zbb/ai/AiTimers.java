package com.tik.zbb.ai;

public class AiTimers
{
    private long breakCooldownUntil = Long.MIN_VALUE;
    private long buildCooldownUntil = Long.MIN_VALUE;
    private long mitigateDangerousBlocksCooldownUntil = Long.MIN_VALUE;

    public boolean breakCooldownPassed(long now) {return now >= breakCooldownUntil;}

    public boolean buildCooldownPassed(long now) {return now >= buildCooldownUntil;}

    public boolean mitigateDangerousBlocksCooldownPassed(long now) {return now >= mitigateDangerousBlocksCooldownUntil;}


    public void setBreakCooldownUntil(long breakCooldownUntil) {this.breakCooldownUntil = breakCooldownUntil;}

    public void setBuildCooldownUntil(long buildCooldownUntil) {this.buildCooldownUntil = buildCooldownUntil;}

    public void setMitigateDangerousBlocksCooldownUntil(long mitigateDangerousBlocksCooldownUntil) {this.mitigateDangerousBlocksCooldownUntil = mitigateDangerousBlocksCooldownUntil;}
}
