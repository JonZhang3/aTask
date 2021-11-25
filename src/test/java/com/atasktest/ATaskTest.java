package com.atasktest;

import com.atask.ResultTask;
import com.atask.State;
import com.atask.Task;
import com.atask.TaskEngine;
import com.atask.TaskGroup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ATaskTest {

    private static TaskEngine engine;

    @Before
    public void before() {
        engine = new TaskEngine.Builder()
            .build();
    }

    @After
    public void after() {
        engine.shutdown();
//        engine = null;
    }

    @Test
    public void testTask() {
        Task task = engine.buildTask(ctx -> {
            try {
                Thread.sleep(1000 * 5);
            } catch (InterruptedException ignore) {
            }
            ctx.onSuccess("success");
            ctx.onError("error", null);
        })
            .progress(System.out::println)
            .end((ctx, error) -> {
                assertEquals("success", ctx.getResult().getString(0));
                assertEquals(State.SUCCESS, ctx.getState());
            })
            .build();
        engine.go(task);
        assertEquals(1, engine.getRunningTasks().size());
        assertEquals(State.RUNNING, engine.getRunningTasks().get(0).getState());
        task.await();
        assertEquals(State.SUCCESS, task.getState());
    }

    @Test
    public void testResultTask() {
        ResultTask<String> task = engine.buildResultTask(ctx -> {
            try {
                Thread.sleep(1000 * 3);
            } catch (InterruptedException ignore) {
            }
            return "success";
        }).build();
        engine.go(task);
        task.await();
        assertEquals("success", task.get());
        assertEquals(State.SUCCESS, task.getState());

        task = engine.go(ctx -> {
            throw new Exception();
        });
        try {
            task.get();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testTaskGroup() {
        TaskGroup group = engine.prepareGroup();
        for (int i = 0; i < 1000; i++) {
            group.go(group.buildItem(ctx -> {
                ctx.group().incrementCounter();
                ctx.group().addData("a");
            }).build());
        }
        group.whenComplete(() -> {
            assertEquals(1000, group.getCounter());
            assertEquals(1000, group.getData().size());
            assertEquals("a", group.getData().getString(0));
            assertEquals("a", group.getData().getString(999));
        });
        group.await();
        assertEquals(1000, group.getCounter());
        assertEquals(1000, group.getData().size());
        assertEquals("a", group.getData().getString(0));
        assertEquals("a", group.getData().getString(999));
    }

    @Test
    public void testTaskTimeout() {
        Task task = engine.buildTask(ctx -> {
            try {
                Thread.sleep(1000 * 10);
            } catch (InterruptedException ignore) {
            }
        }).build();
        engine.go(task);
        try {
            task.await(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testTaskCancel() {
        Task task = engine.buildTask(ctx -> {
        }).build();
        task.cancel(false);
        assertEquals(State.CANCLE, task.getState());

        task = engine.buildTask(ctx -> {
            try {
                Thread.sleep(1000 * 10);
            } catch (InterruptedException ignore) {
            }
        }).build();
        engine.go(task);
        try {
            Thread.sleep(3 * 1000);
        } catch (InterruptedException ignore) {
        }
        task.cancel(true);
        assertEquals(State.CANCLE, task.getState());
    }

}
