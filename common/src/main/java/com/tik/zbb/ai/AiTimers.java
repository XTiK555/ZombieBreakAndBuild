package com.tik.zbb.ai;

public class AiTimers
{
    private long breakCooldownUntil = Long.MIN_VALUE;
    private long buildCooldownUntil = Long.MIN_VALUE;
    private long goToTargetCooldownUntil = Long.MIN_VALUE;
    private long freezeUntil = Long.MIN_VALUE;
    private long checkPathCooldownUntil = Long.MIN_VALUE;
    private long mitigateDangerousBlocksCooldownUntil = Long.MIN_VALUE;

    public boolean breakCooldownPassed(long now) {return now >= breakCooldownUntil;}

    public boolean buildCooldownPassed(long now) {return now >= buildCooldownUntil;}

    public boolean goToTargetCooldownPassed(long now) {return now >= goToTargetCooldownUntil;}

    public boolean freezePassed(long now) {return now >= freezeUntil;}

    public boolean checkPathCooldownPassed(long now) {return now >= checkPathCooldownUntil;}

    public boolean mitigateDangerousBlocksCooldownPassed(long now) {return now >= mitigateDangerousBlocksCooldownUntil;}


    public void setBreakCooldownUntil(long breakCooldownUntil) {this.breakCooldownUntil = breakCooldownUntil;}

    public void setBuildCooldownUntil(long buildCooldownUntil) {this.buildCooldownUntil = buildCooldownUntil;}

    public void setGoToTargetCooldownUntil(long goToTargetCooldownUntil) {this.goToTargetCooldownUntil = goToTargetCooldownUntil;}

    public void setFreezeUntil(long freezeUntil) {this.freezeUntil = freezeUntil;}

    public void setCheckPathCooldownUntil(long checkPathCooldownUntil) {this.checkPathCooldownUntil = checkPathCooldownUntil;}

    public void setMitigateDangerousBlocksCooldownUntil(long mitigateDangerousBlocksCooldownUntil) {this.mitigateDangerousBlocksCooldownUntil = mitigateDangerousBlocksCooldownUntil;}
}
