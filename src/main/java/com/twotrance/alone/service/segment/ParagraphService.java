package com.twotrance.alone.service.segment;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.twotrance.alone.config.ExceptionHandler;
import com.twotrance.alone.model.segment.Paragraph;
import com.twotrance.alone.repositorys.ParagraphRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


/**
 * ParagraphService
 *
 * @author trance
 * @description paragraph service
 */
@Service
public class ParagraphService {

    /**
     * logger
     */
    Log log = LogFactory.get(this.getClass());

    @Resource
    private ParagraphRepository paragraphRepository;

    @Resource
    private ExceptionHandler ex;

    public Set<String> models() {
        return Optional.ofNullable(paragraphRepository.models()).orElse(CollUtil.empty(HashSet.class));
    }

    public void updateOfMax(Long newMax, String model, Long oldMax) {
        paragraphRepository.updateOfMax(newMax, model, oldMax);
    }

    public void updateOfLen(Long len, String model, String phone) {
        Paragraph paragraph = paragraphRepository.findByPhoneAndModel(phone, model);
        if (ObjectUtil.isEmpty(paragraph)) {
            log.error(ex.exception(3001));
            throw ex.exception(3001);
        }
        paragraphRepository.updateOfLen(len, model, phone, paragraph.getLength());
    }

    public Paragraph addModel(Paragraph paragraph) {
        Paragraph exParagraph = paragraphRepository.findByPhoneAndModel(paragraph.getPhone(), paragraph.getModel());
        if (ObjectUtil.isNotEmpty(exParagraph)) {
            log.error(ex.exception(3006));
            throw ex.exception(3005);
        }
        Paragraph saveParagraph;
        try {
            paragraph.setMax(1L);
            paragraph.setDelete(false);
            paragraph.setCreateTime(new Date());
            paragraph.setMax(paragraph.getLength() + 1);
            saveParagraph = paragraphRepository.save(paragraph);
        } catch (Exception e) {
            log.error(ex.exception(3006));
            throw ex.exception(3006);
        }
        return saveParagraph;
    }

    public Paragraph delModel(String model, String phone) {
        Paragraph paragraph = paragraphRepository.findByPhoneAndModel(model, phone);
        if (ObjectUtil.isEmpty(paragraph)) {
            log.error(ex.exception(3001));
            throw ex.exception(3001);
        }
        paragraph.setDelete(true);
        paragraph.setUpdateTime(new Date());
        return paragraphRepository.save(paragraph);
    }

    public Paragraph model(String phone, String model) {
        return paragraphRepository.findByPhoneAndModel(phone, model);
    }

    public boolean hasModel(String phone, String model) {
        return ObjectUtil.isNotEmpty(paragraphRepository.findByPhoneAndModel(phone, model)) ? true : false;
    }

}
