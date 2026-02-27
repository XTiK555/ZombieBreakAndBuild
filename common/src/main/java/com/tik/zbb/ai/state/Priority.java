package com.tik.zbb.ai.state;

public enum Priority
{
    High(3),
    Medium(2),
    Low(1);

    private final int weight;

    Priority(int weight)
    {
        this.weight = weight;
    }

    public int weight()
    {
        return weight;
    }
}