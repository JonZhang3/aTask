<p align="center"><img width="100" src="https://jonzhang-3.gitee.io/pics/atask/logo.png" alt="aTask logo"></p>

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/69461d12a70d4b259f74b399f4f5d63f)](https://app.codacy.com/gh/JonZhang3/aTask?utm_source=github.com&utm_medium=referral&utm_content=JonZhang3/aTask&utm_campaign=Badge_Grade)

# ATask

> 一个简单的多用途异步任务执行框架

### 主要特性
1. 操作简便，采用链式调用
2. 多种任务类型：可设置回调的任务，可获取返回结果的任务，任务组
3. 可自定义任务的类型，ID，超时时间等参数
4. 灵活，可阻塞当前线程，亦可设置回调方法
5. 实时获取正在运行任务状态，对已完成任务进行自定义处理

### 使用说明
#### 引入
Maven
```xml
<dependency>
    <groupId>com.github.jonzhang3</groupId>
    <artifactId>aTask</artifactId>
    <version>1.0.1</version>
</dependency>
```
#### 示例
##### 首先创建任务执行的引擎（线程池）
```java
TaskEngine engine = new TaskEngine.Builder()
    .corePoolSize(3)// 设置核心线程池大小，默认为系统 CPU 核数
    .maxPoolSize(100)// 设置线程池的最大大小，默认为 Integer.MAX_VALUE
    .keepAliveSeconds(1000)// 设置线程的最大空闲时间，默认为 60 秒
    // 设置任务等待队列的大小，默认为 Integer.MAX_VALUE
    // 如果设置的为负数，则采用 SynchronousQueue，否则采用 LinkedBlockingQueue
    .queueCapacity(20)
    // 设置线程池的任务拒绝策略
    .rejectedExecutionHandler(RejectedExecutionHandler)
    .completedTaskHandler(handler)// 设置对已完成任务的处理回调
    .build();
// 可通过 getRunningTasks() 方法获取当前正在执行的任务
```

##### 1. 回调处理任务执行结果的任务的使用
```java
Task task = engine.buildTask(ctx -> {
        ctx.onProgress(100);// 设置进度值，将调用进度回调函数
        ctx.onSuccess("1", "2", "3");// 设置任务执行成功，将调用结果回调函数
        ctx.onError(Exception);// 设置任务执行失败，将调用结果回调函数
        // 如果 onSuccess 和 onError 都调用了，则第首先执行的方法将会调用成功
    })
    .type("type")// 设置任务的类型
    .id("id")// 设置任务的 ID
    .progress(progress -> {})// 设置任务的进度回调
    // 设置任务的结果回调
    // 如果任务执行失败，error 则不为 null；如果任务执行成功，error 则为 null
    .end((ctx, error) -> {
        if(error != null) {// 执行成功
            ctx.getResult();// 获取 onSuccess 设置的数据
        } else {// 执行失败
            log.error(error);// 打印错误
        }
    })
    .build();
engine.go(task);// 最后调用 TaskEngine 的 go 方法执行该任务
task.await(); // 该方法会阻塞当前线程，如果你需要等待任务执行完成再继续执行，则调用该方法
```

##### 2. 具有返回结果的任务的使用
```java
ResultTask<String> resultTask = engine.buildResultTask(ctx -> {
        ctx.onProgress(200);
        return "success";// 返回结果数据
    })
    .type("type")
    .id("id")
    .progress(i -> {})
    .build();
engine.go(resultTask);
// 获取返回结果
// 该方法会阻塞当前线程
String result = resultTask.get();
```

##### 3. 任务组的使用
```java
TaskGroup group = engine.prepareGroup();// 创建一个任务组
// 添加并执行一个任务
group.go(group.buildItem(ctx -> {
    ctx.group().incrementCounter();// 计数器 +1（线程安全）
    ctx.group().addData("data");// 设置数据，方便后续使用
}).build());
group.await();// 等待线程组中所有的任务执行完成
group.getCounter();// 获取计数器的结果
Data data = group.getData();// 获取组中任务执行时设置的数据（线程安全）
```