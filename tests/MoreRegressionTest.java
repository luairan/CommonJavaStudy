import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import nioSample.nio.sample.filecopy.*;
import org.apache.poi.ss.usermodel.*;

public class MoreRegressionTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    interface Action { void run() throws Exception; }
    static void rejects(Class<? extends Throwable> type, Action action) throws Exception {
        try { action.run(); } catch (Throwable t) { if (type.isInstance(t)) return; throw t; }
        throw new AssertionError("Expected " + type.getName());
    }
    public static void main(String[] args) throws Exception {
        algorithms(); files(); utilities(); excel(); http(); workers(); operations(); echoClient(); servers();
        System.out.println("PASS: remaining algorithms, file integrity, utilities, XLS/XLSX, HTTP, workers, multi-client IO");
    }
    static void algorithms() throws Exception {
        Random random = new Random(7);
        for (int n = 1; n <= 100; n++) {
            int[] input = new int[n];
            for (int i = 0; i < n; i++) input[i] = random.nextInt(21)-10;
            int[] sorted = input.clone(); Arrays.sort(sorted);
            for (int k = 0; k < n; k++) check(new FindPosition().find(input.clone(),k) == sorted[k],"rank");
            int[] radix = input.clone(); new sort.RadixSort().radixSort(radix,10);
            check(Arrays.equals(radix,sorted),"signed radix");
            int[] heap = input.clone(); new sort.HeapSort().heapSort(heap);
            check(Arrays.equals(heap,sorted),"heap");
        }
        int[] extremes = {Integer.MIN_VALUE,Integer.MAX_VALUE,0,-1,1};
        new sort.RadixSort().radixSort(extremes,10);
        check(Arrays.equals(extremes,new int[]{Integer.MIN_VALUE,-1,0,1,Integer.MAX_VALUE}),"radix extremes");
        rejects(IllegalArgumentException.class,() -> new sort.RadixSort().radixSort(new int[]{1000},2));
        rejects(IllegalArgumentException.class,() -> new FindPosition().find(new int[0],0));
        rejects(IllegalArgumentException.class,() -> new FindPosition().find(new int[]{1},-1));
        encode.Base64 decoder = new encode.Base64();
        for (int n = 0; n < 100; n++) {
            byte[] bytes = new byte[n]; random.nextBytes(bytes);
            String encoded = java.util.Base64.getEncoder().encodeToString(bytes);
            check(Arrays.equals(decoder.decode(encoded.toCharArray()),bytes),"Base64 bytes");
            List<Character> chars = decoder.encode(encoded.toCharArray());
            check(chars.size() == bytes.length,"Base64 padding");
            for (int i = 0; i < n; i++) check(chars.get(i) == (char)(bytes[i]&255),"unsigned bytes");
        }
        check(decoder.getpnum("==".toCharArray(),'=') == 2,"padding count");
        rejects(IllegalArgumentException.class,() -> decoder.decode("A==?".toCharArray()));
        ForkJoinPool pool = new ForkJoinPool(2);
        try {
            check(pool.invoke(new thread.forkjoin.CountTask(1,100)) == 5050,"fork join");
            check(pool.invoke(new thread.forkjoin.CountTask(Integer.MAX_VALUE,Integer.MAX_VALUE)) == Integer.MAX_VALUE,"max endpoint");
            check(pool.invoke(new thread.forkjoin.CountTask(Integer.MIN_VALUE,Integer.MIN_VALUE)) == Integer.MIN_VALUE,"min endpoint");
            rejects(ArithmeticException.class,() -> pool.invoke(new thread.forkjoin.CountTask(Integer.MAX_VALUE-1,Integer.MAX_VALUE)));
        } finally { pool.shutdownNow(); }
        rejects(IllegalArgumentException.class,() -> new thread.forkjoin.CountTask(2,1));
    }
    static void files() throws Exception {
        Path directory = Files.createTempDirectory("copy-regression-");
        try {
            Path source = directory.resolve("source"), target = directory.resolve("target");
            for (int length : new int[]{0,1,8191,8192,8193,50000,17*1024*1024+3}) {
                byte[] bytes = new byte[length]; new Random(length).nextBytes(bytes); Files.write(source,bytes);
                for (Class<?> type : new Class<?>[]{StreamFileCopy.class,StreamFileCopyWithBuffer.class,NIOFileCopy.class,MappedFileCopy.class}) {
                    Files.write(target,new byte[]{1,2,3});
                    CommonCopy copy = (CommonCopy) type.getConstructor(String.class,String.class).newInstance(source.toString(),target.toString());
                    Thread worker = new Thread(copy); worker.start(); worker.join(10000);
                    check(!worker.isAlive() && copy.flag && copy.getFailure() == null,"copy completion " + type);
                    check(Files.mismatch(source,target) == -1,"copy data " + type + " / " + length);
                    check(Arrays.equals(bytes,Files.readAllBytes(source)),"source modified");
                }
            }
            NIOFileCopy same = new NIOFileCopy(source.toString(),source.toString()); same.run();
            check(same.getFailure() != null,"same path must fail before truncate");
            Path alias = directory.resolve("alias"); Files.createLink(alias,source);
            same = new NIOFileCopy(source.toString(),alias.toString()); same.run();
            check(same.getFailure() != null,"hard link must fail before truncate");
            CommonCopy missing = new StreamFileCopy(directory.resolve("missing").toString(),target.toString()); missing.run();
            check(missing.flag && missing.getFailure() != null,"copy failure observable");
            byte[] firstBytes = new byte[20001], secondBytes = new byte[34567];
            new Random(1).nextBytes(firstBytes); new Random(2).nextBytes(secondBytes);
            Files.write(source,firstBytes); Files.write(target,secondBytes);
            Path joined = directory.resolve("joined");
            nioSample.nio.sample.gather.FileGathering.main(new String[]{source.toString(),target.toString(),joined.toString()});
            check(Files.size(joined) == 54568,"gather truncation");
            byte[] expected = new byte[54568];
            System.arraycopy(firstBytes,0,expected,0,firstBytes.length);
            System.arraycopy(secondBytes,0,expected,firstBytes.length,secondBytes.length);
            check(Arrays.equals(expected,Files.readAllBytes(joined)),"gather contents/order");
            Path numbers = directory.resolve("numbers");
            Files.writeString(numbers,"3\n-2\n9\n");
            sort.QuickSort.main(new String[]{numbers.toString()});
            sort.MergeSort.main(new String[]{numbers.toString()});
            sort.HeapSort.main(new String[]{numbers.toString()});
            sort.RadixSort.main(new String[]{numbers.toString()});
        } finally { deleteTree(directory); }
    }
    static void deleteTree(Path path) throws IOException {
        try (java.util.stream.Stream<Path> paths = Files.walk(path)) {
            for (Path entry : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(entry);
        }
    }
    public static class Bean {
        private String name;
        public String getName() { return name; }
        public void setName(String value) { name = value; }
        public String getReadOnly() { return "fixed"; }
        public void setWriteOnly(String value) { }
    }
    public static class BrokenBean { public String getValue() { throw new IllegalStateException("getter failed"); } }
    static void utilities() throws Exception {
        Bean bean = (Bean) util.BeanToMapUtil.convertMap(Bean.class,Map.of("name","中文","readOnly","ignore","class","ignore"));
        check(bean.getName().equals("中文"),"bean writable");
        Map<String,Object> map = util.BeanToMapUtil.convertBean(bean);
        check(map.get("name").equals("中文") && map.get("readOnly").equals("fixed") && !map.containsKey("writeOnly"),"bean descriptors");
        rejects(IllegalArgumentException.class,() -> util.BeanToMapUtil.convertBean(new BrokenBean()));
        byte[] image = util.WaterMark.doWaterMark("watermark");
        java.awt.image.BufferedImage png = javax.imageio.ImageIO.read(new ByteArrayInputStream(image));
        check(png != null && png.getWidth() == 200,"missing optional font");
        check(util.MD5Util.MD5("中文").equals("a7bac2239fcdcb3a067903d8077c4a07"),"MD5 UTF8");
        check(util.VelocityUtil.getResult(util.VelocityUtil.getTemplate("你好 $name","test"),Map.of("name","世界")).equals("你好 世界"),"template Unicode");
        rejects(IllegalArgumentException.class,() -> util.VelocityUtil.getTemplate("#if($x)","invalid"));
        ByteArrayOutputStream pathOutput = new ByteArrayOutputStream(); PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(pathOutput,true,StandardCharsets.UTF_8));
            thread.exam.Mytest.main(new String[0]);
        } finally { System.setOut(originalOut); }
        check(pathOutput.toString(StandardCharsets.UTF_8).trim().equals("10\t12"),"tree path contains stale/unused elements");
        util.SQLFormater sql = new util.SQLFormater();
        check(sql.format("(select 1)").contains("select"),"leading parenthesis");
        rejects(IllegalArgumentException.class,() -> sql.format("select '"));
        rejects(IllegalArgumentException.class,() -> sql.format("select \"broken"));
        Path directory = Files.createTempDirectory("bookmark-regression-");
        try {
            Files.writeString(directory.resolve("a.html"),"<A HREF=\"https://one\">one</A>\n<a href='https://one'>duplicate</a>\n<a href='https://two'>two</a>\n");
            Path grep = directory.resolve("grep.txt"); Files.writeString(grep,"first\nlast");
            ByteArrayOutputStream grepOutput = new ByteArrayOutputStream(); PrintStream oldOut = System.out;
            try {
                System.setOut(new PrintStream(grepOutput,true,StandardCharsets.UTF_8));
                nioSample.sample.Grep.main(new String[]{"last",grep.toString()});
            } finally { System.setOut(oldOut); }
            check(grepOutput.toString(StandardCharsets.UTF_8).contains(":2:last"),"grep lost final line");
            Path output = directory.resolve("out.txt");
            util.HTMLTagFilter.main(new String[]{directory.toString(),output.toString()});
            check(Files.readAllLines(output).size() == 2,"bookmark matching");
        } finally { deleteTree(directory); }
    }
    static void excel() throws Exception {
        for (boolean xlsx : new boolean[]{false,true}) {
            byte[] data;
            try (Workbook workbook = xlsx ? new org.apache.poi.xssf.usermodel.XSSFWorkbook() : new org.apache.poi.hssf.usermodel.HSSFWorkbook()) {
                Sheet sheet = workbook.createSheet();
                Row header = sheet.createRow(0); header.createCell(0).setCellValue("name"); header.createCell(3).setCellValue("formula");
                Row row = sheet.createRow(2); row.createCell(0).setCellValue(true); row.createCell(3).setCellFormula("\"hello\"");
                row.createCell(4).setCellFormula("2+3");
                Cell date = row.createCell(5); date.setCellValue(java.sql.Date.valueOf("2020-02-03"));
                CellStyle style = workbook.createCellStyle(); style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd")); date.setCellStyle(style);
                ByteArrayOutputStream out = new ByteArrayOutputStream(); workbook.write(out); data = out.toByteArray();
            }
            AtomicBoolean closed = new AtomicBoolean();
            ByteArrayInputStream input = new ByteArrayInputStream(data) { public void close() { closed.set(true); } };
            check(util.ExcelUtils.readExcelTitle(input,null).length == 4,"sparse header");
            check(!closed.get(),"caller stream ownership");
            List<String[]> rows = util.ExcelUtils.readExcelContent(new ByteArrayInputStream(data),null);
            check(rows.size() == 2 && rows.get(0).length == 6,"sparse row width");
            check(Arrays.stream(rows.get(0)).allMatch(String::isEmpty),"missing row");
            check(rows.get(1)[0].equals("TRUE") && rows.get(1)[3].equals("hello")
                    && rows.get(1)[4].equals("5") && rows.get(1)[5].equals("2020-02-03"),"cell kinds");
            try (Workbook empty = xlsx ? new org.apache.poi.xssf.usermodel.XSSFWorkbook() : new org.apache.poi.hssf.usermodel.HSSFWorkbook()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(); empty.write(out);
                check(util.ExcelUtils.readExcelTitle(new ByteArrayInputStream(out.toByteArray()),null).length == 0,"empty workbook");
            }
        }
    }
    static void http() throws Exception {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("localhost",0),0);
        server.createContext("/",exchange -> {
            byte[] request = exchange.getRequestBody().readAllBytes();
            String value = exchange.getRequestURI() + "|" + new String(request,StandardCharsets.ISO_8859_1);
            byte[] data = value.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200,data.length);
            try (OutputStream out = exchange.getResponseBody()) { out.write(data); } exchange.close();
        }); server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/?x=1#fragment";
            String get = new String(util.HttpUtils.doUrl(url,Map.of("name","中文 &"),util.HttpUtils.Method.GET),StandardCharsets.UTF_8);
            check(get.equals("/?x=1&name=%E4%B8%AD%E6%96%87+%26|"),"legacy HTTP default charset");
            String post = new String(util.HttpUtils.doUrl(url,Map.of("name","é"),"ISO-8859-1",util.HttpUtils.Method.POST),StandardCharsets.UTF_8);
            check(post.equals("/?x=1|name=%E9"),"legacy HTTP explicit charset");
            String raw = new String(util.HttpUtils.doUrl(url,(String)null,util.HttpUtils.Method.POST),StandardCharsets.UTF_8);
            check(raw.equals("/?x=1|"),"null POST");
            check(new nioSample.nio.readpage.BaiduReader().read(URI.create(url)).equals("/?x=1|"),"HTTP page reader");
        } finally { server.stop(0); }
    }
    static void workers() throws Exception {
        nioSample.nio.asyn.multithread.Worker worker = new nioSample.nio.asyn.multithread.Worker();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); PrintStream original = System.out;
        System.setOut(new PrintStream(bytes,true,StandardCharsets.UTF_8));
        Thread thread = new Thread(worker); thread.setDaemon(true);
        try {
            for (int i = 0; i < 3; i++) worker.processData(new nioSample.nio.asyn.multithread.EventData("event"+i));
            thread.start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (!bytes.toString(StandardCharsets.UTF_8).contains("event2") && System.nanoTime() < deadline) Thread.sleep(5);
            check(bytes.toString(StandardCharsets.UTF_8).contains("event2"),"worker backlog");
        } finally { thread.interrupt(); thread.join(3000); System.setOut(original); }
        check(!thread.isAlive(),"worker cancellation");
        Thread echo = new Thread(new nioSample.nio.sample.echo.EchoWorker()); echo.setDaemon(true); echo.start();
        echo.interrupt(); echo.join(3000); check(!echo.isAlive(),"echo worker cancellation");
        Thread response = new Thread(() -> new nioSample.nio.sample.echo.RspHandler().waitForResponse()); response.setDaemon(true); response.start();
        response.interrupt(); response.join(3000); check(!response.isAlive(),"response wait cancellation");
        new nioSample.nio.asyn.AddProcessor().process("not,a-number");
        new nioSample.nio.asyn.MultiProcessor().process("1,");
    }
    static void operations() throws Exception {
        BlockingQueue<String> output = new LinkedBlockingQueue<>();
        try (io.support.LineProcessorServer server = new io.support.LineProcessorServer(new int[]{0,0},
                Arrays.asList(line -> output.add("A:"+line),line -> output.add("B:"+line)))) {
            Thread thread = new Thread(server); thread.setDaemon(true); thread.start();
            try (Socket first = new Socket("localhost",server.getPort(0)); Socket second = new Socket("localhost",server.getPort(0));
                 Socket third = new Socket("localhost",server.getPort(1))) {
                first.getOutputStream().write("1,".getBytes(StandardCharsets.UTF_8)); Thread.sleep(20);
                first.getOutputStream().write("2\n3,4\n".getBytes(StandardCharsets.UTF_8));
                second.getOutputStream().write("5,6\n".getBytes(StandardCharsets.UTF_8));
                third.getOutputStream().write("7,8\r\n".getBytes(StandardCharsets.UTF_8));
                Set<String> got = new HashSet<>();
                for (int i = 0; i < 4; i++) { String line = output.poll(3,TimeUnit.SECONDS); check(line != null,"processor stalled"); got.add(line); }
                check(got.equals(Set.of("A:1,2","A:3,4","A:5,6","B:7,8")),"per-connection processor routing");
            } finally { thread.interrupt(); thread.join(3000); }
            check(!thread.isAlive(),"selector cancellation");
        }
    }
    static void echoClient() throws Exception {
        try (ServerSocket listener = new ServerSocket(0)) {
            byte[] expected = new byte[50000]; new Random(3).nextBytes(expected);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread peer = new Thread(() -> {
                try { for (int request = 0; request < 10; request++) {
                try (Socket socket = listener.accept()) {
                    check(Arrays.equals(socket.getInputStream().readNBytes(expected.length),expected),"echo request");
                    for (int offset = 0; offset < expected.length;) {
                        int count = Math.min(103,expected.length-offset);
                        socket.getOutputStream().write(expected,offset,count); offset += count;
                        if (offset < 500) Thread.sleep(5);
                    }
                }
                } } catch (Throwable e) { failure.set(e); }
            }); peer.setDaemon(true); peer.start();
            nioSample.nio.sample.echo.NioClient client = new nioSample.nio.sample.echo.NioClient(InetAddress.getLoopbackAddress(),listener.getLocalPort());
            Thread reactor = new Thread(client); reactor.setDaemon(true); reactor.start();
            AtomicReference<byte[]> got = new AtomicReference<>(); CountDownLatch done = new CountDownLatch(10);
            try {
                for (int request = 0; request < 10; request++) client.send(expected,new nioSample.nio.sample.echo.RspHandler() {
                    public synchronized boolean handleResponse(byte[] bytes) { got.set(bytes); done.countDown(); return true; }
                    public synchronized void handleFailure(Throwable error) { failure.set(error); done.countDown(); }
                });
                check(done.await(5,TimeUnit.SECONDS),"echo client timed out");
                check(failure.get() == null && Arrays.equals(expected,got.get()),"fragmented echo response " + failure.get());
            } finally { reactor.interrupt(); reactor.join(3000); peer.join(3000); }
            check(!reactor.isAlive(),"echo client cancellation");
        }
    }
    static class JvmServer implements AutoCloseable {
        final int port;
        final Process process;
        final Path log;
        JvmServer(String type) throws Exception {
            try (ServerSocket reservation = new ServerSocket(0)) { port = reservation.getLocalPort(); }
            log = Files.createTempFile("server-regression-",".log");
            process = new ProcessBuilder(Paths.get(System.getProperty("java.home"),"bin","java").toString(),
                    "-cp",System.getProperty("java.class.path"),type,Integer.toString(port))
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
            while (System.nanoTime() < deadline) {
                if (!process.isAlive()) { String output = Files.readString(log); close(); throw new AssertionError(type + ": " + output); }
                try (Socket socket = connect()) { return; }
                catch (IOException e) { Thread.sleep(20); }
            }
            close(); throw new AssertionError("Server did not start: " + type);
        }
        Socket connect() throws IOException {
            Socket socket = new Socket(); socket.connect(new InetSocketAddress("localhost",port),1000); socket.setSoTimeout(5000); return socket;
        }
        public void close() throws Exception { process.destroy(); if (!process.waitFor(3,TimeUnit.SECONDS)) process.destroyForcibly().waitFor(); Files.deleteIfExists(log); }
    }
    static void servers() throws Exception {
        for (String type : new String[]{"io.aio.Socket","luairan.textserver.aio.Socket","luairan.timeserver.aio.Socket",
                "io.nio.UnBlockSingleServer","luairan.timeserver.nio.UnBlockSingleServer",
                "luairan.textserver.bio.Server","luairan.timeserver.bio.Server"}) {
            try (JvmServer server = new JvmServer(type); Socket idle = server.connect(); Socket socket = server.connect()) {
                byte[] message = "  中文  \r\n".getBytes(StandardCharsets.UTF_8);
                socket.getOutputStream().write(message,0,3); Thread.sleep(10);
                socket.getOutputStream().write(message,3,message.length-3);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                check(reader.readLine().endsWith("  中文  "),"line echo " + type);
                String large = "x".repeat(20000);
                socket.getOutputStream().write((large+"\n").getBytes(StandardCharsets.UTF_8));
                check(reader.readLine().endsWith(large),"large echo " + type);
                socket.getOutputStream().write("qu".getBytes(StandardCharsets.UTF_8)); Thread.sleep(10);
                socket.getOutputStream().write("it\n".getBytes(StandardCharsets.UTF_8));
                check("ok".equals(reader.readLine()) && reader.readLine() == null,"quit " + type);
            }
        }
        for (String type : new String[]{"nio.server.Socket","nio.file.Socket","nioSample.Reactor","nioSample.nio.sample.echo.NioServer"}) {
            try (JvmServer server = new JvmServer(type); Socket idle = server.connect(); Socket socket = server.connect()) {
                byte[] bytes = new byte[50000]; new Random(3).nextBytes(bytes);
                socket.getOutputStream().write(bytes); socket.getOutputStream().flush();
                socket.shutdownOutput();
                check(Arrays.equals(bytes,socket.getInputStream().readNBytes(bytes.length)),"raw echo " + type);
                check(socket.getInputStream().read() == -1,"raw half-close " + type);
            }
        }
        try (JvmServer server = new JvmServer("luairan.timeserver.nio.BlockServer"); Socket socket = server.connect()) {
            socket.getOutputStream().write("hello\nquit\n".getBytes(StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
            check("hello".equals(reader.readLine()) && "quit".equals(reader.readLine()) && reader.readLine() == null,"blocking channel echo");
        }
        for (String type : new String[]{"httpServer.HttpServerDemo","luairan.nio.http.server.HttpServerDemo"}) {
            try (JvmServer server = new JvmServer(type)) {
                HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:"+server.port+"/test/").openConnection();
                connection.setReadTimeout(3000); connection.setRequestMethod("POST");
                try { check(connection.getResponseCode() == 405 && "GET".equals(connection.getHeaderField("Allow")),"HTTP method response"); }
                finally { connection.disconnect(); }
            }
        }
        try (JvmServer server = new JvmServer("nioSample.sample.NBTimeServer"); Socket socket = server.connect()) {
            check(new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII)).readLine() != null,"nonblocking daytime");
        }
        Process ping = new ProcessBuilder(Paths.get(System.getProperty("java.home"),"bin","java").toString(),
                "-cp",System.getProperty("java.class.path"),"nioSample.sample.Ping","invalid..host")
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        try { check(ping.waitFor(8,TimeUnit.SECONDS) && ping.exitValue() == 0,"DNS failure must terminate ping"); }
        finally { ping.destroyForcibly(); }
        for (String type : new String[]{"thread.callablefuture.Call","thread.callablefuture.FutureTask","thread.exam.Thread_interrput"}) {
            Process process = new ProcessBuilder(Paths.get(System.getProperty("java.home"),"bin","java").toString(),
                    "-cp",System.getProperty("java.class.path"),type).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            try { check(process.waitFor(5,TimeUnit.SECONDS) && process.exitValue() == 0,"example termination " + type); }
            finally { process.destroyForcibly(); }
        }
    }
}
