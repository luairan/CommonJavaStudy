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

### 修复后的示例与回归测试

本次修复保留了学习示例的入口，覆盖快速排序退化、归并稳定性、素数边界、
生产者消费者的实例隔离及中断处理、NIO 完整收发和 HTTPS 身份校验。
两处 `HttpsUtils` 使用 JDK 默认信任库，不再接受任意证书或域名；
自签名服务需要显式配置受信任的证书。请求使用 10 秒连接与读取超时，
参数默认采用 UTF-8 表单编码；工具不再重置进程级 CookieManager。

`luairan.textserver.nio.Server` 使用 UTF-8 行协议，每条消息以 LF 或 CRLF 结束，
保留正文首尾空格。`quit` 返回 `ok\n` 后关闭；未结束的最后一行不作为消息处理。
单行上限为 64 KiB（不含 LF，包含可选的 CR），超限关闭连接。
默认监听 localhost:9000，也可通过第一个命令行参数指定端口。

安装 JDK 17 或更新版本后，在仓库根目录运行：

```sh
sh tests/run.sh
```

也可以通过 `JAVA_HOME` 指定 JDK。测试只依赖 JDK，覆盖 50 万元素的有序、逆序、
全相等数组，以及随机排序、素数边界、独立队列、中断时许可回滚、
本地 HTTP 参数、TLS 不可信证书和域名不匹配、NIO 分包/多行/慢读/退出。
测试生成临时证书与编译目录，结束后自动清理。

脚本只编译本次涉及的示例。仓库其他示例依赖 Netty、POI、Velocity 等第三方库，
原仓库未提供统一的 Maven/Gradle 依赖配置，不能据此认为整个仓库已完成构建验证。
