package com.twotrance.alone.service.segment;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollStreamUtil;

import com.twotrance.alone.model.segment.Alloc;
import com.twotrance.alone.mapper.segment.AllocMapper;

import org.springframework.stereotype.Service;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * AllocService
 *
 * @author trance
 * @description alloc service
 */
@Service
public class AllocService extends ServiceImpl<AllocMapper, Alloc> {

    public List<Alloc> allocs() {
        return CollUtil.emptyIfNull(baseMapper.selectList(null));
    }

    public List<String> bizKeys() {
        List<Alloc> allocs = allocs();
        return allocs.size() < 1 ? ListUtil.empty() : CollStreamUtil.toList(allocs, alloc -> alloc.getBizKey());
    }

    public Alloc alloc(String bizKey) {
        return baseMapper.selectOne(Wrappers.<Alloc>lambdaQuery().eq(Alloc::getBizKey, bizKey));
    }

    public void uMaxId(String bizKey) {
        Alloc alloc = alloc(bizKey);
        if (!ObjectUtil.isEmpty(alloc))
            baseMapper.update(null, Wrappers.<Alloc>lambdaUpdate().eq(Alloc::getBizKey, bizKey).set(Alloc::getMaxId, alloc.getMaxId() + alloc.getStep()));
    }

    public void uMaxIdByCustom(String bizKey, int step) {
        Alloc alloc = alloc(bizKey);
        if (!ObjectUtil.isEmpty(alloc))
            baseMapper.update(null, Wrappers.<Alloc>lambdaUpdate().eq(Alloc::getBizKey, bizKey).set(Alloc::getMaxId, alloc.getMaxId() + step));
    }

}
