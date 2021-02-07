# **分布式ID (A Distributed ID)**
> 借鉴美团Leaf重构, 自增ID读写锁更新号段改为线程通信更新号段, 雪花ID的机器ID生成是通过Zookeeper自动生成改为Redis生成,经过压测性能上比Leaf差一点, 但是重构的方案可以解决雪花模式下的自动部署的问题, Leaf虽然使用了自动增长机器ID的, 我也是这么做的, 但是当当前机器节点下线后, 并没有对下线的机器ID进行处理, 我们知道10Bit最大处理1023个机器ID(包含0), 如果遇到后续服务器迁移, 或者K8S等大规模部署或者重新部署, 那么机器ID将产生不够用的现象并且出现大量无用的机器ID, 当然这个是极端情况下的说明, 可能终你一生都不会碰到, 哈哈, 但是我也这么做了.

>Reference Meituan Leaf refactoring, since the update ID read-write lock them roughly into thread synchronization update them roughly, snow machine ID ID is generated automatically generated by the Zookeeper to Redis, after pressure test performance than the Leaf almost, but the reconstruction scheme can solve the problem of snowflake mode of the automatic deployment, the Leaf, despite the use of automatic growth machine ID, that's what I did, but when the current machine nodes offline, not to deal with offline machine ID,We know that the 10 biggest deal with 1023 - bit machine ID (including zero), if there are any subsequent server migration, or K8S large-scale deployment or redeploy, such as the machine ID will generate enough phenomenon and appear a large number of useless machine ID, of course, this is extreme cases, may all your life will not meet, ha ha, but I did.

环境:
+ JDK1.8
+ SpringBoot 2.2.5
+ Redis 3.2.100
+ MySQL 5.7

*注意 需要自己构建 application.properties/yml 因为每个人的环境不同*
