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
mvn test-compile
sh tests/run-all.sh
```

`mvn test-compile` 编译全部主代码和原有测试示例；实际的断言回归测试由
`tests/run-all.sh` 执行，不要把仅编译成功当作回归测试通过。
`JAVA_HOME` 可指定 JDK，`MVN` 可指定 Maven 可执行文件。
测试使用临时文件、本地随机端口和临时 TLS 证书，清理后退出，不请求外部网站。
仅验证第一轮涉及的 JDK 示例时，也可运行无需 Maven 的 `sh tests/run.sh`。

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
请求连接与读取超时为 10 秒，表单参数默认使用 UTF-8，不重置全局 CookieManager。

### 回归覆盖与边界

测试覆盖随机/50 万元素排序、完整 int 范围、排名选择、Base64 二进制往返、
文件长度和内容比对（含 16 MiB 分块边界）、源文件保护、XLS/XLSX 稀疏单元格与公式、
字体缺失、模板中文、HTTP 参数、TLS 不可信证书与域名不匹配、worker 取消和积压、
多客户端分片消息、慢读、半关闭、响应分片、提前 EOF、连接拒绝、停止后提交、
未知主机和线程示例退出。

这些是学习示例，测试覆盖具体回归场景，不代表完整协议实现或已穷尽所有问题。
SQLFormater 是启发式格式化器，不是完整 SQL 解析器。
本轮不访问真实外部 HTTP 服务，也未构造超过 2 GiB 的真实文件做磁盘测试。
