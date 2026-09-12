package com.tik.zbb.gametest.fixture;

public final class MutableTestValue<T>
{
    private T value;

    public MutableTestValue() {}

    public MutableTestValue(T value)
    {
        this.value = value;
    }

    public T get()
    {
        return value;
    }

    public void set(T value)
    {
        this.value = value;
    }
}
