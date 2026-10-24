# commonJavaStudy

## 一些java 源码的探索和实现

## 如果对你有帮助 来杯咖啡：  Alipay donation ： luairan93@gmail.com

 <img src="https://luairan.github.io/DingTalk20170821162050.png" width = "300" alt="打赏" />

### 具体的内容有：

* Array
* code
* GC
* hihocoder
* Lock
* Map
* NIO  NIO通讯例子
* Sort 多种排序算法
* String
* test
* ThreadTest
* Tree
* webserver

### 构建与验证

需要 JDK 17+、Maven 3.9+。首次构建会下载 `pom.xml` 固定版本的依赖。

```sh
mvn test
```

`mvn test` 会编译项目并通过 JUnit/Surefire 执行两组基础回归及独立网络测试，任何断言失败都会使
命令以非零状态退出。基础回归组包含多个实际场景，网络超时和退出场景独立报告测试结果。
测试报告位于 `target/surefire-reports/`，测试源码统一位于 `src/test/java/`。
需要打包时运行 `mvn verify`，同样会先执行测试。

原来的 `sh tests/run-all.sh` 保留为 `mvn test` 的兼容入口；可通过 `MVN` 指定
Maven 可执行文件，额外参数也会传给 Maven，例如：

```sh
sh tests/run-all.sh -Dtest=RegressionSuiteTest#coreRegressions
```

`JAVA_HOME` 可指定 JDK。只编译而不执行测试可运行 `mvn test-compile`。
测试使用临时文件、本地随机端口和临时 TLS 证书，清理后退出，不请求外部网站。
仅验证第一轮涉及的 JDK 示例时，也可运行无需 Maven 的 `sh tests/run.sh`。

### 持续集成

`.github/workflows/java-tests.yml` 会在 `master` 收到提交、面向 `master` 的 PR 创建或
更新，以及手动触发时运行。Ubuntu 与 macOS 都使用 Temurin JDK 17，执行同一条
`mvn --batch-mode --no-transfer-progress test`。Maven 依赖按 `pom.xml` 缓存。

每个任务最多运行 15 分钟，同一 PR 的新提交会取消过时的运行；测试不并行执行，
避免两组测试修改全局 TLS 状态或控制台输出时互相干扰。
成功或失败都会尝试上传 Surefire 报告，保留 7 天。Action 固定到完整提交 SHA，
工作流 token 仅授予仓库内容读取权限。


依赖包括 Apache POI、Velocity、Gson 和 Netty；Netty handler 已适配
`ChannelInboundHandlerAdapter`，Excel 工具已适配 POI 的枚举单元格类型。

### 本轮修复的其他问题

- 文件复制按实际读取长度写出，并循环完成 channel 写入；关闭所有文件句柄。
  映射复制使用只读分块映射，不扩展或改写源文件，支持空文件和超过映射块的文件。
  同路径、软链接或硬链接指向同一文件时，在打开输出前拒绝复制。
  `CommonCopy.flag` 是完成标记，失败原因通过 `getFailure()` 读取。
  gathering 示例会完整、依序拼接两个文件，不再只复制各自第一块。
- `FindPosition.find` 使用零起始的排名，持续缩小搜索范围并拒绝无效排名。
  基数排序支持负数和完整 `int` 范围，`maxbit` 必须在 1–10 且足以表示所有值。
  Base64 正确处理填充、非法输入和大于 127 的字节；旧的 `encode(char[])`
  实际执行解码，兼容保留该方法，并增加名称明确的 `decode(char[])`。
- `HttpUtils` 复用已修复的 HTTPS 实现，保留证书与域名校验，修复默认编码、
  null POST 参数及已有查询串。HTTP 服务对非 GET 请求返回 405 并关闭 exchange。
- AIO 使用非阻塞完成回调，接收新连接后立即继续 accept，缓冲区按连接隔离；
  避免单线程完成池中 `Future.get()` 死锁、部分写入和 EOF 空转。
  重复 NIO 行服务共享已测试的实现，Reactor 骨架补全为原始字节流 echo。
  计算服务按连接保留处理器与未结束行，不再用最后接受的客户端选择处理器；
  非法数字输入不会终止服务线程，worker 会处理已有积压并响应中断。
- NIO echo 客户端累积完整响应，再调用处理器；连接失败会唤醒等待方并清理状态，停止后拒绝新请求（含空请求）。
  服务端跳过已关闭连接的异步事件，支持客户端 shutdownOutput 后收完响应。
  两种 BIO 示例正常处理 EOF，客户端在控制台 EOF 或服务端断开时退出。
- 线程池在异常或中断时也会关闭；中断示例停止循环，避免恢复中断标志后持续抛异常。
  ForkJoin 使用不会溢出的中点和循环下标，拒绝反向区间，以 long 累加中间结果，
  仅在最终结果超出 int 时显式报错。
  树路径仅打印有效长度，深路径可扩展缓冲区。
- Excel 处理稀疏表头、缺失行、布尔值和字符串/数字公式，关闭 workbook，
  不关闭调用者传入的 InputStream。路径重载自行关闭打开的流。
  Bean 转换跳过只读/只写属性，getter 失败不会被吞掉并伪装为成功。
  可选字体缺失时水印回退到系统逻辑字体；Velocity 不再绕经平台编码的 Blob，
  模板解析失败会报告原因。MD5 字符串输入固定使用 UTF-8。
- 排序、文件读取、书签去重和复制示例使用命令行路径，去除个人电脑路径依赖；
  排序按实际文件行数建立数组。Grep 不遗漏末尾无换行的一行，校验和按块处理文件；
  非阻塞时间服务使用 channel 写入，Ping 的 DNS 失败不会遗留连接线程。

### 网络示例协议

行服务使用 UTF-8，LF 或 CRLF 结束一条消息，保留正文首尾空格。
`quit` 返回 `ok\n` 后关闭。AIO/NIO 行服务单行上限为 64 KiB，超限关闭连接；
未以换行结束的末尾片段不作为完整消息。计算服务每行使用 `A,B` 格式，
worker 队列积压上限为 1024，超限拒绝该连接的新事件。

`nio.server.Socket`、`nio.file.Socket`、`nioSample.Reactor` 和 NIO echo 示例处理
原始字节流。NIO echo 客户端约定响应字节数等于请求字节数，适用于配套 echo 服务。
所有这些示例都可通过首个命令行参数覆盖默认端口；计算服务第二个参数指定乘法端口。
`BlockServer` 保留原有逐行原样回显行为，`quit` 本身也被回显后再关闭。

HTTPS 工具使用 JDK 默认信任库。自签名服务需显式配置受信任证书；
请求连接与读取超时默认均为 10 秒，表单参数默认使用 UTF-8，不重置全局 CookieManager。
两处 HttpsUtils 的六参数原始请求重载可分别配置连接/读取超时（单位毫秒，必须为正数）。
读取超时约束单次等待，不表示整个 HTTP 请求的总时限。

### 网络截止时间与服务退出

NIO echo 客户端默认使用 10 秒的请求总截止时间，也可通过
`new NioClient(address, port, requestTimeoutMillis)` 设置正数毫秒。
截止时间覆盖请求排队、连接、发送和完整响应接收；收到少量字节不会延长它。
到期后以 `SocketTimeoutException` 通知响应处理器、关闭该连接，reactor 仍可处理后续请求。
`close()` 会唤醒事件循环、取消尚未完成的请求，并拒绝后续提交；也支持启动 reactor 前关闭。

`io.nio.UnBlockSingleServer`（及旧包入口）、`LineProcessorServer`、`BlockServer`
和 NIO echo `NioServer` 均支持运行中关闭、重复/并发关闭和中断退出。
退出时停止监听并关闭现有连接；这是取消式停止，不保证发送完尚未完成的响应。
调用者仍应等待服务线程结束；`BlockServer.awaitTermination` 可等待其工作池退出。
NIO echo 服务的外部 EchoWorker 由创建它的调用者负责中断并等待退出。
AIO 的 `AsyncEchoHandler.serve` 可通过中断服务线程停止，并关闭连接和完成线程组。

新增测试使用真实的本地 TCP 服务控制以下行为：

- 接受连接后不回复，或持续缓慢发送少量字节，验证 echo 总截止时间及超时后的后续请求。
- HTTP 响应头不返回、正文只返回一部分，验证两份 HTTP 工具的读取超时。
- 客户端关闭时有挂起请求、启动前有排队请求、排队期间已过期，验证取消与唤醒。
- 五类服务在活动/空闲连接存在时关闭、中断或并发关闭，验证线程结束、客户端断开和原端口重启。
- 关闭后才启动、RST 异常断连后的继续服务，以及 AIO 两种协议的完成线程清理。

可单独运行 `mvn -Dtest=NetworkLifecycleTest,AsyncShutdownTest test`。
测试通过短的可配置时限、锁存器和 Future 限制等待，不依赖外部 IP 制造超时；
未模拟真实网络的 SYN 丢包、DNS 超时或公网故障。

### 回归覆盖与边界

测试覆盖随机/50 万元素排序、完整 int 范围、排名选择、Base64 二进制往返、
文件长度和内容比对（含 16 MiB 分块边界）、源文件保护、XLS/XLSX 稀疏单元格与公式、
字体缺失、模板中文、HTTP 参数、TLS 不可信证书与域名不匹配、worker 取消和积压、
多客户端分片消息、慢读、半关闭、响应分片、提前 EOF、连接拒绝、停止后提交、
未知主机和线程示例退出。

这些是学习示例，测试覆盖具体回归场景，不代表完整协议实现或已穷尽所有问题。
SQLFormater 是启发式格式化器，不是完整 SQL 解析器。
本轮不访问真实外部 HTTP 服务，也未构造超过 2 GiB 的真实文件做磁盘测试。
