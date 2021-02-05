package com.twotrance.alone;

import cn.hutool.core.collection.CollUtil;
import com.twotrance.alone.service.segment.AllocService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class AloneApplicationTests {

    @Resource
    private AllocService allocService;

    @Test
    void contextLoads() {
        System.out.println("allocService.allocs() = " + allocService.allocs());
        System.out.println("allocService.bizKeys() = " + allocService.bizKeys());
    }

    @Test
    void contextLoads2() {
        List l1 = new ArrayList();
        List l2 = new ArrayList();

        l1.add("a");
        l1.add("b");
        l1.add("c");
        l1.add("d");

        l2.add("b");
        l2.add("c");
        l2.add("f");

        System.out.println("CollUtil.subtractToList(l1, l2) = " + CollUtil.subtractToList(l1, l2));
        System.out.println("CollUtil.subtractToList(l2, l1) = " + CollUtil.subtractToList(l2, l1));

    }

}
