package com.atask;

public enum State {

    UNKNOWN("未知", -1),
    INIT("初始化", 0),
    QUEUED("等待队列中", 1),
    RUNNING("执行中", 2),
    CANCEL("取消", 3),
    SUCCESS("执行成功", 4),
    ERROR("执行失败", 5),
    TIMEOUT("超时", 6);

    private final String name;
    private final int value;

    State(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }
}
