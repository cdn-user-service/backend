package io.ants.common.utils;


import java.io.File;
import java.lang.management.*;

import com.google.common.collect.Maps;
import com.sun.management.OperatingSystemMXBean;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class AntsSystemInfoUtils {
    private static OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    public static Map env()  {
        Map result = Maps.newHashMap();
        try{
            Runtime r =Runtime.getRuntime();
            Properties props =System.getProperties();
            InetAddress addr;
            addr =InetAddress.getLocalHost();
            String ip =addr.getHostAddress();
            Map map =System.getenv();
            // 获取用户名
            Object userName =map.get("USERNAME");
            // 获取计算机名
            Object computerName =map.get("COMPUTERNAME");
            // 获取计算机域名
            Object userDomain =map.get("USERDOMAIN");
            result.put("用户名",userName);
            result.put("计算机名",computerName);
            result.put("计算机域名",userDomain);
            result.put("本地ip地址",ip);
            result.put("本地主机名",addr.getHostName());
            result.put("JVM可以使用的总内存",r.totalMemory());
            result.put("JVM可以使用的剩余内存",r.freeMemory());
            result.put("JVM可以使用的处理器个数",r.availableProcessors());
            result.put("Java的运行环境版本 ",props.getProperty("java.version"));
            result.put("Java的运行环境供应商 ",props.getProperty("java.vendor"));
            result.put("Java供应商的URL ",props.getProperty("java.vendor.url"));
            result.put("Java的安装路径 ",props.getProperty("java.home"));
            result.put("Java的虚拟机规范版本 ",props.getProperty("java.vm.specification.version"));
            result.put("Java的虚拟机规范供应商 ",props.getProperty("java.vm.specification.vendor"));
            result.put("Java的虚拟机规范名称 ",props.getProperty("java.vm.specification.name"));
            result.put("Java的虚拟机实现版本 ",props.getProperty("java.vm.version"));
            result.put("Java的虚拟机实现供应商 ",props.getProperty("java.vm.vendor"));
            result.put("Java的虚拟机实现名称 ",props.getProperty("java.vm.name"));
            result.put("Java运行时环境规范版本 ",props.getProperty("java.specification.version"));
            result.put("Java运行时环境规范供应商 ",props.getProperty("java.specification.vender"));
            result.put("Java运行时环境规范名称 ",props.getProperty("java.specification.name"));
            result.put("Java的类格式版本号 ",props.getProperty("java.class.version"));
            result.put("Java的类路径 ",props.getProperty("java.class.path"));
            result.put("加载库时搜索的路径列表 ",props.getProperty("java.library.path"));
            result.put("默认的临时文件路径 ",props.getProperty("java.io.tmpdir"));
            result.put("一个或多个扩展目录的路径 ",props.getProperty("java.ext.dirs"));
            result.put("操作系统的名称 ",props.getProperty("os.name"));
            result.put("操作系统的构架 ",props.getProperty("os.arch"));
            result.put("操作系统的版本 ",props.getProperty("os.version"));
            result.put("文件分隔符 ",props.getProperty("file.separator"));
            result.put("路径分隔符 ",props.getProperty("path.separator"));
            result.put("行分隔符 ",props.getProperty("line.separator"));
            result.put("用户的账户名称 ",props.getProperty("user.name"));
            result.put("用户的主目录 ",props.getProperty("user.home"));
            result.put("用户的当前工作目录 ",props.getProperty("user.dir"));
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public static Map disk() {
        Map result =Maps.newHashMap();
        try{
            // 磁盘使用情况
            File[]files =File.listRoots();
            for (File file :files) {
                String total =new DecimalFormat("#.#").format(file.getTotalSpace() *1.0 /1024 /1024 /1024) +"G";
                String free =new DecimalFormat("#.#").format(file.getFreeSpace() *1.0 /1024 /1024 /1024) +"G";
                String un =new DecimalFormat("#.#").format(file.getUsableSpace() *1.0 /1024 /1024 /1024) +"G";
                String path =file.getPath();
                Map pathMap =Maps.newHashMap();
                pathMap.put("total",total);
                pathMap.put("un",un);
                pathMap.put("free",free);
                result.put(path,pathMap);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
    public static Map mem() {
        Map result =Maps.newHashMap();
        try{
            OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean mxBean =ManagementFactory.getMemoryMXBean();
            // 堆内存使用情况
            MemoryUsage memoryUsage =mxBean.getHeapMemoryUsage();
            // 初始的总内存
            long initTotalMemorySize =memoryUsage.getInit();
            // 最大可用内存
            long maxMemorySize =memoryUsage.getMax();
            // 已使用的内存
            long usedMemorySize =memoryUsage.getUsed();
            Map memoryUsageMap =Maps.newHashMap();
            memoryUsageMap.put("initTotalMemorySize",initTotalMemorySize);
            memoryUsageMap.put("maxMemorySize",maxMemorySize);
            memoryUsageMap.put("usedMemorySize",usedMemorySize);
            // 总的物理内存
            String totalMemorySize =new DecimalFormat("#.##").format(osmxb.getTotalPhysicalMemorySize() /1024.0 /1024 /1024) +"G";
            // 剩余的物理内存
            String freePhysicalMemorySize =new DecimalFormat("#.##").format(osmxb.getFreePhysicalMemorySize() /1024.0 /1024 /1024) +"G";
            // 已使用的物理内存
            String usedMemory =new DecimalFormat("#.##").format((osmxb.getTotalPhysicalMemorySize() -osmxb.getFreePhysicalMemorySize()) /1024.0 /1024 /1024) +"G";
            memoryUsageMap.put("totalMemorySize",totalMemorySize);
            memoryUsageMap.put("freePhysicalMemorySize",freePhysicalMemorySize);
            memoryUsageMap.put("usedMemory",usedMemory);
            String jvmInitMem =new DecimalFormat("#.#").format(initTotalMemorySize *1.0 /1024 /1024) +"M";
            String jvmMaxMem =new DecimalFormat("#.#").format(maxMemorySize *1.0 /1024 /1024) +"M";
            String jvmUsedMem =new DecimalFormat("#.#").format(usedMemorySize *1.0 /1024 /1024) +"M";
            memoryUsageMap.put("jvmInitMem",jvmInitMem);
            memoryUsageMap.put("jvmMaxMem",jvmMaxMem);
            memoryUsageMap.put("jvmUsedMem",jvmUsedMem);
            result.put("memoryUsageMap",memoryUsageMap);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
    public static Map cpuOshi() {
        Map result =Maps.newHashMap();
        try{
            SystemInfo systemInfo =new SystemInfo();
            result.put("程序启动时间",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(ManagementFactory.getRuntimeMXBean().getStartTime())));
            result.put("程序更新时间",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(ManagementFactory.getRuntimeMXBean().getUptime())));

            CentralProcessor processor =systemInfo.getHardware().getProcessor();
            long[]prevTicks =processor.getSystemCpuLoadTicks();
            long[]ticks =processor.getSystemCpuLoadTicks();
            long nice =ticks[CentralProcessor.TickType.NICE.getIndex()]
                    -prevTicks[CentralProcessor.TickType.NICE.getIndex()];
            long irq =ticks[CentralProcessor.TickType.IRQ.getIndex()]
                    -prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
            long softirq =ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()]
                    -prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
            long steal =ticks[CentralProcessor.TickType.STEAL.getIndex()]
                    -prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
            long cSys =ticks[CentralProcessor.TickType.SYSTEM.getIndex()]
                    -prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
            long user =ticks[CentralProcessor.TickType.USER.getIndex()]
                    -prevTicks[CentralProcessor.TickType.USER.getIndex()];
            long iowait =ticks[CentralProcessor.TickType.IOWAIT.getIndex()]
                    -prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
            long idle =ticks[CentralProcessor.TickType.IDLE.getIndex()]
                    -prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
            long totalCpu =user +nice +cSys +idle +iowait +irq +softirq +steal;
            result.put("cpuLogicalProcessorCount",processor.getLogicalProcessorCount());
            result.put("cpuSys", cSys  +"/"+totalCpu);
            result.put("cpuUser", user +"/"+totalCpu);
            result.put("cpuIoWait", iowait+"/"+ totalCpu);
            result.put("cpuIdle", idle +"/"+ totalCpu);
        }catch (Exception e){
            e.printStackTrace();
        }catch (NoClassDefFoundError error){
            return null;
        }
        return result;
    }

    public static Map cpu(){
        Map result =Maps.newHashMap();
        try{
            double cpuLoad = osmxb.getSystemCpuLoad();
            int percentCpuLoad = (int) (cpuLoad * 100);
            result.put("percentCpuLoad",percentCpuLoad+"%");
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public static Map jvm() {
        Map result =Maps.newHashMap();
        try{
            // 获得线程总数
            ThreadGroup parentThread;
            for (parentThread =Thread.currentThread().getThreadGroup();
                 parentThread.getParent() !=null;
                 parentThread = parentThread.getParent()) {
            }
            int totalThread = parentThread.activeCount();
            result.put("总线程数",totalThread);
            result.put("PID",System.getProperty("PID"));
            result.put("LibraryPath",ManagementFactory.getRuntimeMXBean().getLibraryPath());
            result.put("BootClassPath",ManagementFactory.getRuntimeMXBean().getBootClassPath());
            result.put("ClassPath",ManagementFactory.getRuntimeMXBean().getClassPath());
            result.put("ObjectPendingFinalizationCount",ManagementFactory.getMemoryMXBean().getObjectPendingFinalizationCount());
            result.put("HeapMemoryUsage",ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
            result.put("NonHeapMemoryUsage",ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage());
            result.put("ObjectName",ManagementFactory.getMemoryMXBean().getObjectName());
            result.put("LoadedClassCount",ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());
            result.put("TotalLoadedClassCount",ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount());
            result.put("TotalCompilationTime",ManagementFactory.getCompilationMXBean().getTotalCompilationTime());
            result.put("Compilation",ManagementFactory.getCompilationMXBean().getName());
            result.put("OperatingSystemMXBean",ManagementFactory.getOperatingSystemMXBean().getName());
            result.put("OperatingSystemMXArch",ManagementFactory.getOperatingSystemMXBean().getArch());
            result.put("AvailableProcessors",ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
            result.put("SystemLoadAverage",ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
            Map jvmMemPool =Maps.newHashMap();
            //内存池对象
            List<MemoryPoolMXBean> pools =ManagementFactory.getMemoryPoolMXBeans();
            for (MemoryPoolMXBean pool :pools) {
                jvmMemPool.put(pool.getName(),new HashMap(64) {
                    {
                        put("name",pool.getName());
                        put("Type",pool.getType());
                        put("ObjectName",pool.getObjectName());
                        put("Usage",pool.getUsage().toString());
                        put("PeakUsage",pool.getPeakUsage());
                        put("CollectionUsage",pool.getCollectionUsage());
                    }
                });
            }
            result.put("MemoryPool",jvmMemPool);
            Map garbageCollector =Maps.newHashMap();
            // gc
            List<GarbageCollectorMXBean> gcs =ManagementFactory.getGarbageCollectorMXBeans();
            // ParallelOld("ParallelOld"),
            //    SerialOld("SerialOld"),
            //    PSMarkSweep("PSMarkSweep"),
            //    ParallelScavenge("ParallelScavenge"),
            //    DefNew("DefNew"),
            //    ParNew("ParNew"),
            //    G1New("G1New"),
            //    ConcurrentMarkSweep("ConcurrentMarkSweep"),
            //    G1Old("G1Old"),
            //    GCNameEndSentinel("GCNameEndSentinel");
            for (GarbageCollectorMXBean gc :gcs) {
                garbageCollector.put(gc.getName(),gc);
            }
            result.put("GarbageCollector",garbageCollector);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
}
